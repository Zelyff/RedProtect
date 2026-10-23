/* Zelyff fork additions, GPL-3.0. Only enumerated administrative configs. */
package br.net.fabiozumbi12.RedProtect.Core.config;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.*;

/** No region/player storage, recursive directory walks, or database access. */
public final class AdministrativeYaml {
  private static final List<String> NAMES =
      List.of("config", "globalflags", "guiconfig", "economy", "textures");
  private static final Pattern DYNAMIC = Pattern.compile("(?m)^\\s*include\\b|\\$\\{");

  private AdministrativeYaml() {}

  /** Stage every candidate before publishing any of them. Originals remain intact. */
  public static synchronized void prepare(Path folder) throws IOException {
    Files.createDirectories(folder);
    Path state = folder.resolve(".zelyff-config-migration");
    rejectLink(state);
    Files.createDirectories(state);
    Path journal = state.resolve("pending.properties");
    recover(folder, journal);
    Path completed = state.resolve("completed.properties");
    boolean previouslySelected = Files.isRegularFile(completed);
    Map<String, Path> sources = new LinkedHashMap<>();
    Map<String, String> sourceHashes = new LinkedHashMap<>();
    Map<String, ConfigurationNode> candidates = new LinkedHashMap<>();
    String language = "EN-US";
    for (String name : NAMES) {
      Path yaml = folder.resolve(name + ".yml");
      Path conf = folder.resolve(name + ".conf");
      if (name.equals("guiconfig") && !Files.exists(conf)) {
        if (!language.matches("[A-Za-z]{2}-[A-Za-z]{2}"))
          throw new IOException("Invalid language path for legacy GUI configuration");
        Path legacy = folder.resolve("guiconfig" + language + ".conf");
        if (Files.isRegularFile(legacy)) conf = legacy;
      }
      rejectLink(yaml);
      rejectLink(conf);
      boolean needsConf =
          Files.isRegularFile(conf) && (!Files.isRegularFile(yaml) || !previouslySelected);
      String sourceHash = needsConf ? hash(conf) : null;
      ConfigurationNode old = needsConf ? readConf(conf) : null;
      if (needsConf && !sourceHash.equals(hash(conf)))
        throw new IOException(name + ": source changed during reading");
      if (Files.isRegularFile(yaml)) {
        ConfigurationNode current = readYaml(yaml, ConfigurationOptions.defaults());
        if (!previouslySelected && old != null && !same(old, current))
          throw new IOException(
              name
                  + ": CONF and YAML differ before initial selection; preserve both and resolve"
                  + " explicitly");
        if (name.equals("config")) language = current.getNode("language").getString(language);
      } else if (old != null) {
        candidates.put(name, old);
        sources.put(name, conf);
        sourceHashes.put(name, sourceHash);
        if (name.equals("config")) language = old.getNode("language").getString(language);
      }
    }
    if (candidates.isEmpty()) return;
    Path transaction = Files.createDirectory(state.resolve("backup-" + UUID.randomUUID()));
    try {
      Files.setPosixFilePermissions(
          transaction, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
    } catch (UnsupportedOperationException ignored) {
      /* Windows inherits the plugin folder ACL. */
    }
    Properties pending = new Properties();
    for (Map.Entry<String, ConfigurationNode> entry : candidates.entrySet()) {
      String name = entry.getKey();
      Path source = sources.get(name);
      if (!sourceHashes.get(name).equals(hash(source)))
        throw new IOException(name + ": source changed during staging");
      Files.copy(source, transaction.resolve(source.getFileName()));
      if (!sourceHashes.get(name).equals(hash(transaction.resolve(source.getFileName()))))
        throw new IOException(name + ": backup changed during copying");
      Path staged = transaction.resolve(name + ".yml");
      writeYaml(staged, entry.getValue());
      if (!same(entry.getValue(), readYaml(staged, ConfigurationOptions.defaults())))
        throw new IOException(name + ": typed YAML roundtrip mismatch; original preserved");
      sync(staged);
      pending.setProperty(name, hash(staged));
    }
    for (String name : candidates.keySet())
      if (!sourceHashes.get(name).equals(hash(sources.get(name))))
        throw new IOException(name + ": source changed before publication");
    // The journal is durable before the first target is published.
    writeProperties(journal, pending);
    try {
      for (String name : candidates.keySet()) {
        Path target = folder.resolve(name + ".yml");
        if (Files.exists(target))
          throw new IOException("Configuration changed while staging: " + name);
        Files.copy(transaction.resolve(name + ".yml"), target);
        sync(target);
      }
      Properties report = new Properties();
      for (String name : candidates.keySet()) {
        report.setProperty(name + ".source-sha256", hash(sources.get(name)));
        report.setProperty(name + ".yaml-sha256", pending.getProperty(name));
      }
      report.setProperty("backup", transaction.getFileName().toString());
      report.setProperty(
          "comments",
          "YAML headers retained where supported; HOCON inline comments may be absent. Values"
              + " verified, originals preserved.");
      writeProperties(completed, report);
      Files.delete(journal);
    } catch (IOException failure) {
      recover(folder, journal);
      throw failure;
    }
  }

  private static void recover(Path folder, Path journal) throws IOException {
    if (!Files.exists(journal)) return;
    Properties pending = new Properties();
    try (InputStream in = Files.newInputStream(journal)) {
      pending.load(in);
    }
    // Check the whole transaction before deleting any generated candidate.
    for (String name : pending.stringPropertyNames()) {
      if (!NAMES.contains(name)) throw new IOException("Invalid config migration journal");
      Path target = folder.resolve(name + ".yml");
      rejectLink(target);
      if (Files.exists(target) && !hash(target).equals(pending.getProperty(name)))
        throw new IOException(
            name
                + ": interrupted migration target was edited; manual recovery required, files"
                + " preserved");
    }
    for (String name : pending.stringPropertyNames())
      Files.deleteIfExists(folder.resolve(name + ".yml"));
    Files.delete(journal);
  }

  public static ConfigurationLoader<ConfigurationNode> loader(File folder, String name) {
    if (!NAMES.contains(name))
      throw new IllegalArgumentException("Not an administrative config: " + name);
    return new AtomicYaml(folder.toPath().resolve(name + ".yml"));
  }

  private static ConfigurationNode readConf(Path path) throws IOException {
    String text = Files.readString(path, StandardCharsets.UTF_8);
    if (DYNAMIC.matcher(text).find())
      throw new IOException(
          path.getFileName()
              + ": HOCON include/substitution needs explicit migration; no file changed");
    ConfigurationNode node = HoconConfigurationLoader.builder().setPath(path).build().load();
    try {
      // Configurate 3.6.1's HOCON visitor loses nested empty lists while building
      // nodes incrementally. Setting the complete typed tree preserves [] and
      // keeps the header/options obtained by the native loader above.
      Object values = ConfigFactory.parseString(text).root().unwrapped();
      node.setValue(values);
      if (!Objects.equals(values, node.getValue()))
        throw new IOException(path.getFileName() + ": unsupported HOCON value; original preserved");
      return node;
    } catch (ConfigException e) {
      throw new IOException(path.getFileName() + ": invalid HOCON; original preserved", e);
    }
  }

  private static YAMLConfigurationLoader yaml(Path path) {
    return YAMLConfigurationLoader.builder().setPath(path).build();
  }

  private static void writeYaml(Path path, ConfigurationNode node) throws IOException {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setIndent(2);
    // Native mod payloads are Strings containing control characters, not byte[].
    options.setNonPrintableStyle(DumperOptions.NonPrintableStyle.ESCAPE);
    try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      String header = node.getOptions().getHeader();
      if (header != null && !header.isEmpty()) {
        for (String line : header.split("\\R", -1)) writer.write("# " + line + "\n");
        writer.write("\n");
      }
      new Yaml(options).dump(node.getValue(), writer);
    }
  }

  private static ConfigurationNode readYaml(Path path, ConfigurationOptions options)
      throws IOException {
    if (Files.exists(path)) {
      try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
        validate(new Yaml().compose(reader), Collections.newSetFromMap(new IdentityHashMap<>()));
      } catch (RuntimeException e) {
        throw new IOException(path.getFileName() + ": invalid YAML", e);
      }
    }
    return yaml(path).load(options);
  }

  private static void validate(Node node, Set<Node> active) throws IOException {
    if (node == null) return;
    if (!active.add(node))
      throw new IOException("Recursive YAML aliases are not administrative values");
    if (node instanceof MappingNode mapping) {
      Set<String> keys = new HashSet<>();
      for (NodeTuple pair : mapping.getValue()) {
        if (!(pair.getKeyNode() instanceof ScalarNode key)
            || !Tag.STR.equals(key.getTag())
            || !keys.add(key.getValue()))
          throw new IOException("Duplicate or non-string YAML mapping key");
        validate(pair.getValueNode(), active);
      }
    } else if (node instanceof SequenceNode sequence) {
      for (Node child : sequence.getValue()) validate(child, active);
    } else if (!Set.of(Tag.STR, Tag.INT, Tag.FLOAT, Tag.BOOL, Tag.NULL).contains(node.getTag())) {
      throw new IOException("Unsupported YAML value tag: " + node.getTag().getValue());
    }
    active.remove(node);
  }

  public static boolean same(ConfigurationNode a, ConfigurationNode b) {
    return Objects.equals(a.getValue(), b.getValue());
  }

  private static void rejectLink(Path path) throws IOException {
    if (Files.isSymbolicLink(path))
      throw new IOException("Symlink not allowed for config migration: " + path.getFileName());
  }

  private static void sync(Path path) throws IOException {
    try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
      channel.force(true);
    }
  }

  private static String hash(Path path) throws IOException {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
      StringBuilder result = new StringBuilder();
      for (byte b : digest) result.append(String.format("%02x", b & 255));
      return result.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void replace(Path temp, Path target) throws IOException {
    try {
      Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException e) {
      throw new IOException("Atomic config publication unsupported; original retained", e);
    }
  }

  private static void writeProperties(Path target, Properties properties) throws IOException {
    Path temp = Files.createTempFile(target.getParent(), ".migration-", ".tmp");
    try {
      try (OutputStream out = Files.newOutputStream(temp)) {
        properties.store(out, "Zelyff administrative config migration; no credentials");
      }
      sync(temp);
      replace(temp, target);
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  private static final class AtomicYaml implements ConfigurationLoader<ConfigurationNode> {
    private final Path target;
    private String loadedHash;

    AtomicYaml(Path target) {
      this.target = target;
    }

    @Override
    public ConfigurationOptions getDefaultOptions() {
      return yaml(target).getDefaultOptions();
    }

    @Override
    public ConfigurationNode createEmptyNode(ConfigurationOptions options) {
      return yaml(target).createEmptyNode(options);
    }

    @Override
    public ConfigurationNode load(ConfigurationOptions options) throws IOException {
      rejectLink(target);
      ConfigurationNode result = readYaml(target, options);
      loadedHash = Files.exists(target) ? hash(target) : null;
      return result;
    }

    @Override
    public void save(ConfigurationNode node) throws IOException {
      rejectLink(target);
      if (!Objects.equals(loadedHash, Files.exists(target) ? hash(target) : null))
        throw new IOException(target.getFileName() + ": externally edited; reload before saving");
      Path temp = Files.createTempFile(target.getParent(), "." + target.getFileName(), ".tmp");
      try {
        writeYaml(temp, node);
        if (!same(node, readYaml(temp, node.getOptions())))
          throw new IOException(target.getFileName() + ": save roundtrip mismatch");
        sync(temp);
        replace(temp, target);
        loadedHash = hash(target);
      } finally {
        Files.deleteIfExists(temp);
      }
    }
  }
}
