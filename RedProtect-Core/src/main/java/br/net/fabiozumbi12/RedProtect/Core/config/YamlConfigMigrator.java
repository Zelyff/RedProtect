/*
 * Copyright (c) 2012-2026 - @FabioZumbi12
 *
 * This class is provided 'as-is', without any express or implied warranty. In no event will the authors be held liable for any
 * damages arising from the use of this class.
 */

package br.net.fabiozumbi12.RedProtect.Core.config;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.loader.HeaderMode;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.yaml.snakeyaml.DumperOptions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Handles idempotent, non-destructive migration of RedProtect Configurate HOCON ({@code .conf})
 * files into block-style YAML ({@code .yml}) files with header and {@link Setting#comment()} injection.
 */
public final class YamlConfigMigrator {

    private static final DateTimeFormatter BACKUP_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    public enum MigrationStatus {
        FRESH_YML,
        EXISTING_YML,
        MIGRATED,
        ALREADY_MIGRATED,
        CONFLICT_YML_PRECEDENCE
    }

    public record MigrationResult(File targetFile, MigrationStatus status, File backupFile) {
    }

    private YamlConfigMigrator() {
    }

    /**
     * Builds a block-style Configurate {@link YAMLConfigurationLoader} with 2-space indentation.
     */
    public static YAMLConfigurationLoader createYamlLoader(File ymlFile) {
        return YAMLConfigurationLoader.builder()
                .setFile(ymlFile)
                .setIndent(2)
                .setFlowStyle(DumperOptions.FlowStyle.BLOCK)
                .setHeaderMode(HeaderMode.PRESERVE)
                .build();
    }

    /**
     * Resolves the target {@code <baseName>.yml} path, migrating from {@code <baseName>.conf} if needed.
     * <ul>
     *     <li>If both {@code .yml} and {@code .conf} exist and {@code .conf} does not match an existing {@code .conf.bak}
     *         from an earlier migration, keeps both files untouched, logs a conflict warning, and uses {@code .yml}.</li>
     *     <li>If only {@code .conf} exists, validates HOCON parsing first (aborting without writing {@code .yml} on error),
     *         writes a non-destructive {@code .conf.bak} backup, renders YAML with comments to {@code .yml.tmp},
     *         validates by reloading, atomically moves to {@code .yml}, and preserves {@code .conf} untouched.</li>
     *     <li>If neither exists or only {@code .yml} exists, returns {@code .yml} cleanly.</li>
     * </ul>
     */
    public static MigrationResult resolveAndMigrate(
            File folder,
            String baseName,
            String header,
            Class<?> categoryClass,
            Consumer<String> infoLogger,
            Consumer<String> warnLogger,
            Consumer<String> errorLogger) throws IOException {
        if (folder == null) {
            throw new IllegalArgumentException("Config folder must not be null");
        }
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("Could not create config folder: " + folder.getAbsolutePath());
        }

        Path dir = folder.toPath();
        Path confPath = dir.resolve(baseName + ".conf");
        Path ymlPath = dir.resolve(baseName + ".yml");
        Path bakPath = dir.resolve(baseName + ".conf.bak");

        boolean confExists = Files.isRegularFile(confPath);
        boolean ymlExists = Files.isRegularFile(ymlPath);

        if (ymlExists && confExists) {
            if (hasMatchingBackup(dir, baseName, confPath)) {
                return new MigrationResult(ymlPath.toFile(), MigrationStatus.ALREADY_MIGRATED, bakPath.toFile());
            }
            if (warnLogger != null) {
                warnLogger.accept("Both " + baseName + ".conf and " + baseName + ".yml exist in "
                        + folder.getName() + ". Keeping both untouched and loading " + baseName + ".yml.");
            }
            return new MigrationResult(ymlPath.toFile(), MigrationStatus.CONFLICT_YML_PRECEDENCE, null);
        }

        if (ymlExists) {
            return new MigrationResult(ymlPath.toFile(), MigrationStatus.EXISTING_YML, null);
        }

        if (!confExists) {
            return new MigrationResult(ymlPath.toFile(), MigrationStatus.FRESH_YML, null);
        }

        ConfigurationNode hoconNode;
        try {
            ConfigurationLoader<?> hoconLoader = HoconConfigurationLoader.builder().setPath(confPath).build();
            hoconNode = hoconLoader.load();
        } catch (IOException e) {
            String msg = "Failed to parse " + confPath.getFileName() + ": " + e.getMessage()
                    + ". Aborting migration; original .conf preserved untouched and no .yml was written.";
            if (errorLogger != null) {
                errorLogger.accept(msg);
            }
            throw new IOException(msg, e);
        }

        Path createdBackup = createUniqueBackup(confPath, dir, baseName);
        writeYamlAtomically(ymlPath, hoconNode, header, categoryClass);

        if (infoLogger != null) {
            infoLogger.accept("Migrated " + baseName + ".conf -> " + baseName + ".yml (backup: "
                    + createdBackup.getFileName() + ", original .conf preserved).");
        }
        return new MigrationResult(ymlPath.toFile(), MigrationStatus.MIGRATED, createdBackup.toFile());
    }

    /**
     * Serializes a Configurate node as block YAML with file header and {@link Setting#comment()} annotations,
     * validates by reloading, and atomically replaces {@code ymlFile}.
     */
    public static void saveYamlWithComments(
            File ymlFile,
            ConfigurationNode rootNode,
            String header,
            Class<?> categoryClass) throws IOException {
        if (ymlFile == null) {
            throw new IllegalArgumentException("ymlFile must not be null");
        }
        Path ymlPath = ymlFile.toPath();
        Path parent = ymlPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        writeYamlAtomically(ymlPath, rootNode, header, categoryClass);
    }

    private static void writeYamlAtomically(
            Path ymlPath,
            ConfigurationNode rootNode,
            String header,
            Class<?> categoryClass) throws IOException {
        String renderedYaml = renderYamlString(rootNode);
        Map<String, String> comments = collectSettingComments(categoryClass);
        String finalYaml = injectComments(renderedYaml, header, comments);

        Path dir = ymlPath.getParent();
        Path tmpPath = (dir != null)
                ? dir.resolve(ymlPath.getFileName().toString() + ".tmp")
                : Path.of(ymlPath.getFileName().toString() + ".tmp");

        try {
            Files.writeString(tmpPath, finalYaml, StandardCharsets.UTF_8);
            // Structural validation: confirm Configurate can re-parse the generated YAML before replacing target.
            createYamlLoader(tmpPath.toFile()).load();
            moveAtomically(tmpPath, ymlPath);
        } finally {
            try {
                Files.deleteIfExists(tmpPath);
            } catch (IOException ignored) {
            }
        }
    }

    private static String renderYamlString(ConfigurationNode rootNode) throws IOException {
        StringWriter writer = new StringWriter();
        YAMLConfigurationLoader memLoader = YAMLConfigurationLoader.builder()
                .setSink(() -> new BufferedWriter(writer))
                .setIndent(2)
                .setFlowStyle(DumperOptions.FlowStyle.BLOCK)
                .setHeaderMode(HeaderMode.NONE)
                .build();
        memLoader.save(rootNode);
        return writer.toString();
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean hasMatchingBackup(Path dir, String baseName, Path confPath) throws IOException {
        java.util.regex.Pattern backupPattern = java.util.regex.Pattern.compile(
                "^" + java.util.regex.Pattern.quote(baseName) + "\\.conf(?:\\.[^.]+)?\\.bak$");
        try (java.util.stream.Stream<Path> entries = Files.list(dir)) {
            for (Path candidate : (Iterable<Path>) entries::iterator) {
                if (!Files.isRegularFile(candidate)) {
                    continue;
                }
                String fileName = candidate.getFileName().toString();
                if (backupPattern.matcher(fileName).matches() && filesEqual(confPath, candidate)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void validateHoconFile(Path confPath) throws IOException {
        HoconConfigurationLoader.builder().setPath(confPath).build().load();
    }

    private static Path createUniqueBackup(Path confPath, Path dir, String baseName) throws IOException {
        Path primary = dir.resolve(baseName + ".conf.bak");
        if (!Files.exists(primary)) {
            Files.copy(confPath, primary);
            return primary;
        }
        String ts = LocalDateTime.now().format(BACKUP_TS);
        Path candidate = dir.resolve(baseName + ".conf." + ts + ".bak");
        int counter = 1;
        while (Files.exists(candidate)) {
            candidate = dir.resolve(baseName + ".conf." + ts + "-" + counter + ".bak");
            counter++;
        }
        Files.copy(confPath, candidate);
        return candidate;
    }

    private static boolean filesEqual(Path a, Path b) throws IOException {
        if (Files.size(a) != Files.size(b)) {
            return false;
        }
        return Files.mismatch(a, b) == -1L;
    }

    /**
     * Collects dotted config key paths -> {@link Setting#comment()} text across {@code @ConfigSerializable} types.
     * {@link Map} values whose generic parameter is {@code @ConfigSerializable} use {@code *} for dynamic map keys.
     */
    public static Map<String, String> collectSettingComments(Class<?> categoryClass) {
        if (categoryClass == null) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();
        collectRecursive(categoryClass, "", result, new HashSet<>());
        return result;
    }

    private static void collectRecursive(
            Class<?> type,
            String prefix,
            Map<String, String> out,
            Set<Class<?>> activeStack) {
        if (type == null || !activeStack.add(type)) {
            return;
        }
        try {
            List<Field> fields = getAllFields(type);
            for (Field field : fields) {
                int mod = field.getModifiers();
                if (field.isSynthetic() || Modifier.isStatic(mod) || Modifier.isTransient(mod)) {
                    continue;
                }
                Setting setting = field.getAnnotation(Setting.class);
                if (setting == null) {
                    continue;
                }
                String keyName = setting.value().isEmpty() ? field.getName() : setting.value();
                String fullPath = prefix.isEmpty() ? keyName : prefix + "." + keyName;
                if (!setting.comment().isEmpty()) {
                    out.put(fullPath, setting.comment());
                }

                Class<?> fieldType = field.getType();
                if (fieldType.isAnnotationPresent(ConfigSerializable.class)) {
                    collectRecursive(fieldType, fullPath, out, activeStack);
                } else if (Map.class.isAssignableFrom(fieldType)) {
                    Class<?> valueType = resolveMapValueClass(field.getGenericType());
                    if (valueType != null && valueType.isAnnotationPresent(ConfigSerializable.class)) {
                        collectRecursive(valueType, fullPath + ".*", out, activeStack);
                    }
                }
            }
        } finally {
            activeStack.remove(type);
        }
    }

    private static List<Field> getAllFields(Class<?> type) {
        List<Field> all = new ArrayList<>();
        for (Class<?> cur = type; cur != null && cur != Object.class; cur = cur.getSuperclass()) {
            all.addAll(Arrays.asList(cur.getDeclaredFields()));
        }
        return all;
    }

    private static Class<?> resolveMapValueClass(Type genericType) {
        if (!(genericType instanceof ParameterizedType pt)) {
            return null;
        }
        Type[] args = pt.getActualTypeArguments();
        if (args.length != 2) {
            return null;
        }
        if (args[1] instanceof Class<?> cls) {
            return cls;
        }
        return null;
    }

    /**
     * Prepends YAML file header comments and inserts {@code @Setting(comment = ...)} lines before matching keys.
     */
    public static String injectComments(String rawYaml, String header, Map<String, String> commentsByPath) {
        StringBuilder out = new StringBuilder();
        if (header != null && !header.isBlank()) {
            for (String line : header.split("\\r?\\n", -1)) {
                if (line.isBlank()) {
                    out.append("#\n");
                } else {
                    out.append("# ").append(line).append("\n");
                }
            }
            out.append("\n");
        }

        if (rawYaml == null || rawYaml.isEmpty()) {
            return out.toString();
        }

        String[] lines = rawYaml.split("\\r?\\n", -1);
        Deque<String> pathStack = new ArrayDeque<>();
        int blockScalarIndent = -1;
        char openQuote = 0;
        int plainScalarMinIndent = -1;

        for (int idx = 0; idx < lines.length; idx++) {
            String line = lines[idx];
            if (idx == lines.length - 1 && line.isEmpty()) {
                continue;
            }
            String trimmed = line.trim();
            int indent = countLeadingSpaces(line);

            if (openQuote != 0) {
                out.append(line).append("\n");
                openQuote = scanOpenQuote(trimmed, 0, openQuote);
                continue;
            }

            if (blockScalarIndent >= 0) {
                if (!trimmed.isEmpty() && indent <= blockScalarIndent) {
                    blockScalarIndent = -1;
                } else {
                    out.append(line).append("\n");
                    continue;
                }
            }

            if (plainScalarMinIndent >= 0) {
                if (trimmed.isEmpty() || (indent > plainScalarMinIndent && !trimmed.startsWith("#") && !trimmed.startsWith("- ") && !trimmed.equals("-"))) {
                    out.append(line).append("\n");
                    continue;
                }
                plainScalarMinIndent = -1;
            }

            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("- ") || trimmed.equals("-")) {
                out.append(line).append("\n");
                continue;
            }

            int colonIdx = findKeyColonIndex(trimmed);
            if (colonIdx > 0) {
                String rawKey = trimmed.substring(0, colonIdx).trim();
                String key = unquoteKey(rawKey);
                int depth = indent / 2;
                while (pathStack.size() > depth) {
                    pathStack.removeLast();
                }
                pathStack.addLast(key);

                List<String> actualSegments = new ArrayList<>(pathStack);
                String comment = findMatchingComment(actualSegments, commentsByPath);
                if (comment != null && !comment.isBlank()) {
                    String prefix = " ".repeat(indent);
                    for (String commentLine : comment.split("\\r?\\n", -1)) {
                        if (commentLine.isBlank()) {
                            out.append(prefix).append("#\n");
                        } else {
                            out.append(prefix).append("# ").append(commentLine).append("\n");
                        }
                    }
                }

                String remainder = trimmed.substring(colonIdx + 1).trim();
                if (remainder.startsWith("|") || remainder.startsWith(">")) {
                    blockScalarIndent = indent;
                } else if (remainder.startsWith("'") || remainder.startsWith("\"")) {
                    openQuote = scanOpenQuote(remainder, 0, (char) 0);
                } else if (!remainder.isEmpty()
                        && !remainder.startsWith("#")
                        && !remainder.startsWith("[")
                        && !remainder.startsWith("{")
                        && !remainder.startsWith("&")
                        && !remainder.startsWith("*")
                        && !remainder.startsWith("!")) {
                    plainScalarMinIndent = indent;
                }
            }

            out.append(line).append("\n");
        }
        return out.toString();
    }

    private static char scanOpenQuote(String text, int startIdx, char currentQuote) {
        char quote = currentQuote;
        for (int i = startIdx; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote == 0) {
                if (c == '#') {
                    break;
                }
                if (c == '\'' || c == '"') {
                    quote = c;
                }
            } else if (quote == '"') {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    quote = 0;
                }
            } else {
                if (c == '\'') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '\'') {
                        i++;
                    } else {
                        quote = 0;
                    }
                }
            }
        }
        return quote;
    }

    private static int countLeadingSpaces(String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == ' ') {
            i++;
        }
        return i;
    }

    private static int findKeyColonIndex(String trimmed) {
        char quote = 0;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (quote == '"') {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    quote = 0;
                }
                continue;
            }
            if (quote == '\'') {
                if (c == '\'') {
                    if (i + 1 < trimmed.length() && trimmed.charAt(i + 1) == '\'') {
                        i++;
                    } else {
                        quote = 0;
                    }
                }
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
                continue;
            }
            if (c == '#') {
                return -1;
            }
            if (c == ':') {
                if (i == trimmed.length() - 1 || trimmed.charAt(i + 1) == ' ') {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String unquoteKey(String key) {
        if (key.length() >= 2) {
            char first = key.charAt(0);
            char last = key.charAt(key.length() - 1);
            if (first == '\'' && last == '\'') {
                return key.substring(1, key.length() - 1).replace("''", "'");
            }
            if (first == '"' && last == '"') {
                String inner = key.substring(1, key.length() - 1);
                StringBuilder sb = new StringBuilder(inner.length());
                for (int i = 0; i < inner.length(); i++) {
                    char c = inner.charAt(i);
                    if (c == '\\' && i + 1 < inner.length()) {
                        i++;
                        char esc = inner.charAt(i);
                        switch (esc) {
                            case 'n' -> sb.append('\n');
                            case 'r' -> sb.append('\r');
                            case 't' -> sb.append('\t');
                            case '"' -> sb.append('"');
                            case '\\' -> sb.append('\\');
                            default -> sb.append(esc);
                        }
                    } else {
                        sb.append(c);
                    }
                }
                return sb.toString();
            }
        }
        return key;
    }

    private static String findMatchingComment(List<String> actualSegments, Map<String, String> commentsByPath) {
        if (commentsByPath == null || commentsByPath.isEmpty() || actualSegments.isEmpty()) {
            return null;
        }
        String exactPath = String.join(".", actualSegments);
        String direct = commentsByPath.get(exactPath);
        if (direct != null) {
            return direct;
        }
        for (Map.Entry<String, String> entry : commentsByPath.entrySet()) {
            String pattern = entry.getKey();
            if (!pattern.contains("*")) {
                continue;
            }
            String[] patternParts = pattern.split("\\.");
            if (patternParts.length != actualSegments.size()) {
                continue;
            }
            boolean match = true;
            for (int i = 0; i < actualSegments.size(); i++) {
                if (!patternParts[i].equals("*") && !patternParts[i].equals(actualSegments.get(i))) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return entry.getValue();
            }
        }
        return null;
    }
}
