package br.net.fabiozumbi12.RedProtect.Core.config;

import static org.junit.Assert.*;

import br.net.fabiozumbi12.RedProtect.Core.config.Category.MainCategory;
import com.google.common.reflect.TypeToken;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

public class AdministrativeYamlTest {
  @Rule public TemporaryFolder temp = new TemporaryFolder();

  private Path folder() {
    return temp.getRoot().toPath();
  }

  private void conf(String name, String text) throws IOException {
    Files.writeString(folder().resolve(name + ".conf"), text);
  }

  private ConfigurationNode load(String name) throws IOException {
    return AdministrativeYaml.loader(temp.getRoot(), name).load();
  }

  @Test
  public void typedValuesUnknownKeysAndDottedWorldsSurvive() throws Exception {
    String original =
        "custom { text=\"on\", leading=\"001\", yes=\"yes\", sci=\"1e3\", time=\"12:30\","
            + " unicode=\"AlamNusa 🌿\", big=9223372036854775807, bool=false, ratio=1.25,"
            + " list=[\"a:b\",\"#x\"] }\n"
            + "worlds { \"my.world\" { build=false } }";
    conf("config", original);
    AdministrativeYaml.prepare(folder());
    ConfigurationNode n = load("config");
    assertEquals("on", n.getNode("custom", "text").getValue());
    assertEquals("001", n.getNode("custom", "leading").getValue());
    assertEquals("yes", n.getNode("custom", "yes").getValue());
    assertEquals("1e3", n.getNode("custom", "sci").getValue());
    assertEquals("12:30", n.getNode("custom", "time").getValue());
    assertEquals(Long.MAX_VALUE, n.getNode("custom", "big").getLong());
    assertFalse(n.getNode("worlds", "my.world", "build").getBoolean());
    assertEquals(original, Files.readString(folder().resolve("config.conf")));
  }

  @Test
  public void nativeObjectMappingPreservesUnknownConfigNodes() throws Exception {
    conf(
        "config",
        "language=\"EN-US\"\n"
            + "unknown-root { nested=\"keep\" }\n"
            + "region-settings { custom-addon { nested=\"keep-too\" } }\n"
            + "zelyff-enhancements { enabled=true }");
    AdministrativeYaml.prepare(folder());
    ConfigurationLoader<ConfigurationNode> loader =
        AdministrativeYaml.loader(temp.getRoot(), "config");
    ConfigurationNode node = loader.load();
    MainCategory mapped = node.getValue(TypeToken.of(MainCategory.class), new MainCategory());
    assertTrue(mapped.enhancements.enabled);
    mapped.enhancements.tools.enabled = true;
    node.setValue(TypeToken.of(MainCategory.class), mapped);
    loader.save(node);
    assertEquals("keep", load("config").getNode("unknown-root", "nested").getString());
    assertEquals(
        "keep-too",
        load("config").getNode("region-settings", "custom-addon", "nested").getString());
    assertTrue(load("config").getNode("zelyff-enhancements", "tools", "enabled").getBoolean());
  }

  @Test
  public void allFiveAdministrativeFilesPublishTogetherWithoutLoggingSecrets() throws Exception {
    String[] names = {"config", "globalflags", "guiconfig", "economy", "textures"};
    for (String name : names) conf(name, "custom=\"" + name + "\"");
    conf("config", "mysql { user-pass=\"fixture-secret\" }\ncustom=\"config\"");
    AdministrativeYaml.prepare(folder());
    for (String name : names) {
      assertEquals(name, load(name).getNode("custom").getString());
      assertTrue(Files.exists(folder().resolve(name + ".conf")));
      assertTrue(Files.exists(folder().resolve(name + ".yml")));
    }
    String report =
        Files.readString(folder().resolve(".zelyff-config-migration/completed.properties"));
    assertFalse(report.contains("fixture-secret"));
    assertFalse(Files.exists(folder().resolve(".zelyff-config-migration/pending.properties")));
  }

  @Test
  public void playerAndRegionFilesRemainByteIdentical() throws Exception {
    conf("config", "allowed-claim-worlds=[world]");
    conf("blocks", "players { uuid { added-blocks=123, time=987 } }");
    conf("signs", "\"claim@world\"=[\"world,1,2,3\"]");
    Path data = Files.createDirectories(folder().resolve("data")).resolve("data_world.yml");
    Files.writeString(data, "claim: {owner: uuid, flags: {build: false}}");
    byte[] blocks = Files.readAllBytes(folder().resolve("blocks.conf")),
        signs = Files.readAllBytes(folder().resolve("signs.conf")),
        regions = Files.readAllBytes(data);
    AdministrativeYaml.prepare(folder());
    assertArrayEquals(blocks, Files.readAllBytes(folder().resolve("blocks.conf")));
    assertArrayEquals(signs, Files.readAllBytes(folder().resolve("signs.conf")));
    assertArrayEquals(regions, Files.readAllBytes(data));
    assertFalse(Files.exists(folder().resolve("blocks.yml")));
    assertFalse(Files.exists(folder().resolve("signs.yml")));
  }

  @Test
  public void brokenSecondFilePublishesNothing() throws Exception {
    conf("config", "language=\"EN-US\"");
    conf("globalflags", "broken = [");
    try {
      AdministrativeYaml.prepare(folder());
      fail("Must reject invalid HOCON");
    } catch (IOException expected) {
    }
    assertFalse(Files.exists(folder().resolve("config.yml")));
    assertFalse(Files.exists(folder().resolve("globalflags.yml")));
  }

  @Test
  public void dynamicHoconIsBlockedWithoutModification() throws Exception {
    conf("config", "secret=${?ENV_VAR}");
    try {
      AdministrativeYaml.prepare(folder());
      fail("Must block dynamic semantics");
    } catch (IOException expected) {
    }
    assertFalse(Files.exists(folder().resolve("config.yml")));
    assertEquals("secret=${?ENV_VAR}", Files.readString(folder().resolve("config.conf")));
  }

  @Test
  public void editedYamlWinsOnRepeatedStartup() throws Exception {
    conf("config", "custom=\"before\"");
    AdministrativeYaml.prepare(folder());
    Files.writeString(folder().resolve("config.yml"), "custom: after\nunknown: keep\n");
    AdministrativeYaml.prepare(folder());
    assertEquals("after", load("config").getNode("custom").getString());
    assertEquals("keep", load("config").getNode("unknown").getString());
  }

  @Test
  public void firstStartupConflictsAreRejected() throws Exception {
    conf("config", "custom=\"conf\"");
    Files.writeString(folder().resolve("config.yml"), "custom: yaml\n");
    try {
      AdministrativeYaml.prepare(folder());
      fail("Must not silently select a conflicting old file");
    } catch (IOException expected) {
    }
    assertEquals("custom: yaml\n", Files.readString(folder().resolve("config.yml")));
  }

  @Test
  public void duplicateYamlKeysAreRejected() throws Exception {
    Files.writeString(folder().resolve("config.yml"), "build: true\nbuild: false\n");
    try {
      AdministrativeYaml.prepare(folder());
      fail("Duplicate key must be rejected");
    } catch (IOException expected) {
    }
  }

  @Test
  public void atomicSaveRefusesExternalEdit() throws Exception {
    conf("config", "custom=\"first\"");
    AdministrativeYaml.prepare(folder());
    ConfigurationLoader<ConfigurationNode> loader =
        AdministrativeYaml.loader(temp.getRoot(), "config");
    ConfigurationNode node = loader.load();
    node.getNode("custom").setValue("plugin");
    Files.writeString(folder().resolve("config.yml"), "custom: admin\n");
    try {
      loader.save(node);
      fail("Admin edit must be retained");
    } catch (IOException expected) {
    }
    assertEquals("custom: admin\n", Files.readString(folder().resolve("config.yml")));
  }

  @Test
  public void interruptedPublicationRecoversAndRetriesWholeSet() throws Exception {
    conf("config", "custom=\"native\"");
    Files.writeString(folder().resolve("config.yml"), "custom: native\n");
    Path state = Files.createDirectories(folder().resolve(".zelyff-config-migration"));
    Properties pending = new Properties();
    pending.setProperty("config", sha(folder().resolve("config.yml")));
    try (OutputStream out = Files.newOutputStream(state.resolve("pending.properties"))) {
      pending.store(out, "");
    }
    AdministrativeYaml.prepare(folder());
    assertEquals("native", load("config").getNode("custom").getString());
    assertFalse(Files.exists(state.resolve("pending.properties")));
  }

  @Test
  public void interruptedPublicationRetainsAdminEditedTarget() throws Exception {
    conf("config", "custom=\"native\"");
    Files.writeString(folder().resolve("config.yml"), "custom: generated\n");
    Path state = Files.createDirectories(folder().resolve(".zelyff-config-migration"));
    Properties pending = new Properties();
    pending.setProperty("config", sha(folder().resolve("config.yml")));
    try (OutputStream out = Files.newOutputStream(state.resolve("pending.properties"))) {
      pending.store(out, "");
    }
    Files.writeString(folder().resolve("config.yml"), "custom: admin\n");
    try {
      AdministrativeYaml.prepare(folder());
      fail("Manual recovery required");
    } catch (IOException expected) {
    }
    assertEquals("custom: admin\n", Files.readString(folder().resolve("config.yml")));
  }

  @Test
  public void onlyAllowlistedAdministrativeNamesCanBeLoaded() {
    try {
      AdministrativeYaml.loader(temp.getRoot(), "../data/world");
      fail("Not administrative");
    } catch (IllegalArgumentException expected) {
    }
  }

  private String sha(Path path) throws Exception {
    StringBuilder s = new StringBuilder();
    for (byte b : MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)))
      s.append(String.format("%02x", b & 255));
    return s.toString();
  }
}
