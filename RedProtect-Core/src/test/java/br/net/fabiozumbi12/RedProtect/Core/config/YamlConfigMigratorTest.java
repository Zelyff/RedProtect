package br.net.fabiozumbi12.RedProtect.Core.config;

import br.net.fabiozumbi12.RedProtect.Core.config.Category.BlockCategory;
import br.net.fabiozumbi12.RedProtect.Core.config.Category.EconomyCategory;
import br.net.fabiozumbi12.RedProtect.Core.config.Category.FlagGuiCategory;
import br.net.fabiozumbi12.RedProtect.Core.config.Category.GlobalFlagsCategory;
import br.net.fabiozumbi12.RedProtect.Core.config.Category.HeadTexturesCategory;
import br.net.fabiozumbi12.RedProtect.Core.config.Category.MainCategory;
import br.net.fabiozumbi12.RedProtect.Core.config.YamlConfigMigrator.MigrationResult;
import br.net.fabiozumbi12.RedProtect.Core.config.YamlConfigMigrator.MigrationStatus;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.reflect.TypeToken.of;
import static org.junit.jupiter.api.Assertions.*;

class YamlConfigMigratorTest {

    @Test
    void freshInstall_generatesYamlWithHeaderAndSettingComments(@TempDir Path tempDir) throws Exception {
        List<String> warns = new ArrayList<>();
        MigrationResult res = YamlConfigMigrator.resolveAndMigrate(
                tempDir.toFile(), "config", "RedProtect Header", MainCategory.class, null, warns::add, null);

        assertEquals(MigrationStatus.FRESH_YML, res.status());
        assertTrue(warns.isEmpty());

        ConfigurationLoader<ConfigurationNode> loader = YamlConfigMigrator.createYamlLoader(res.targetFile());
        ConfigurationNode node = loader.load(ConfigurationOptions.defaults().setShouldCopyDefaults(true));
        MainCategory category = node.getValue(of(MainCategory.class), new MainCategory(true));
        node.setValue(of(MainCategory.class), category);

        YamlConfigMigrator.saveYamlWithComments(res.targetFile(), node, "RedProtect Header", MainCategory.class);

        String yamlText = Files.readString(res.targetFile().toPath(), StandardCharsets.UTF_8);
        assertTrue(yamlText.contains("# RedProtect Header"));
        assertTrue(yamlText.contains("# Available: DE-DE, EN-US, ES-ES, FR-FR, ID-ID, PL-PL, PT-BR, RU-RU, ZH-CN"));
        assertTrue(yamlText.contains("# Wand claiming & resizing interaction mode."));
        assertTrue(yamlText.contains("wand-mode: GP_DIRECT"));

        ConfigurationNode reloaded = YamlConfigMigrator.createYamlLoader(res.targetFile()).load();
        MainCategory reloadedCat = reloaded.getValue(of(MainCategory.class), new MainCategory(true));
        assertEquals("GP_DIRECT", reloadedCat.region_settings.claim.wand_mode);
        assertEquals("BLOCK", reloadedCat.region_settings.border.mode);
    }

    @Test
    void legacyConfOnly_migratesTreePreservesConfCreatesBackupAndIsIdempotent(@TempDir Path tempDir) throws Exception {
        Path conf = tempDir.resolve("config.conf");
        String hocon = """
                language = "ID-ID"
                online-mode = true
                region-settings {
                  claim {
                    wand-mode = "NATIVE"
                    amount-per-player = 12
                  }
                  border {
                    mode = "HYBRID"
                    time-showing = 15
                    glow-outline = false
                  }
                }
                flags {
                  pvp = false
                  build = true
                }
                """;
        Files.writeString(conf, hocon, StandardCharsets.UTF_8);

        List<String> infos = new ArrayList<>();
        List<String> warns = new ArrayList<>();
        MigrationResult first = YamlConfigMigrator.resolveAndMigrate(
                tempDir.toFile(), "config", "Config Header", MainCategory.class, infos::add, warns::add, null);

        assertEquals(MigrationStatus.MIGRATED, first.status());
        assertNotNull(first.backupFile());
        assertTrue(Files.isRegularFile(first.backupFile().toPath()));
        assertTrue(Files.isRegularFile(conf), "Original .conf must not be deleted");
        assertTrue(warns.isEmpty());
        assertFalse(infos.isEmpty());

        ConfigurationNode loaded = YamlConfigMigrator.createYamlLoader(first.targetFile()).load();
        MainCategory parsed = loaded.getValue(of(MainCategory.class), new MainCategory(true));
        assertEquals("ID-ID", parsed.language);
        assertEquals("NATIVE", parsed.region_settings.claim.wand_mode);
        assertEquals(12, parsed.region_settings.claim.amount_per_player);
        assertEquals("HYBRID", parsed.region_settings.border.mode);
        assertEquals(15, parsed.region_settings.border.time_showing);
        assertFalse(parsed.region_settings.border.glow_outline);
        assertEquals(Boolean.FALSE, parsed.flags.get("pvp"));
        assertEquals(Boolean.TRUE, parsed.flags.get("build"));

        String ymlSnapshot = Files.readString(first.targetFile().toPath(), StandardCharsets.UTF_8);
        assertTrue(ymlSnapshot.contains("# Config Header"));
        assertTrue(ymlSnapshot.contains("# Wand claiming & resizing interaction mode."));

        // Second run must be idempotent with no duplicate backups or conflict spam
        MigrationResult second = YamlConfigMigrator.resolveAndMigrate(
                tempDir.toFile(), "config", "Config Header", MainCategory.class, infos::add, warns::add, null);
        assertEquals(MigrationStatus.ALREADY_MIGRATED, second.status());
        assertTrue(warns.isEmpty());
        assertEquals(ymlSnapshot, Files.readString(first.targetFile().toPath(), StandardCharsets.UTF_8));
    }

    @Test
    void realEconomyConfFixture_migratesAllItemsAndEnchantmentsAccurately(@TempDir Path tempDir) throws Exception {
        Path fixtureConf = tempDir.resolve("economy.conf");
        try (InputStream is = getClass().getResourceAsStream("/economy.conf")) {
            assertNotNull(is, "Test fixture /economy.conf must exist");
            Files.copy(is, fixtureConf);
        }

        MigrationResult res = YamlConfigMigrator.resolveAndMigrate(
                tempDir.toFile(), "economy", "Economy Header", EconomyCategory.class, null, null, null);

        assertEquals(MigrationStatus.MIGRATED, res.status());
        ConfigurationNode node = YamlConfigMigrator.createYamlLoader(res.targetFile()).load();
        EconomyCategory eco = node.getValue(of(EconomyCategory.class), new EconomyCategory());

        assertEquals("Coins", eco.economy_name);
        assertEquals("$", eco.economy_symbol);
        assertEquals(100000, eco.max_area_toget_value);
        assertEquals(200000L, eco.items.values.get("BEACON"));
        assertEquals(6000L, eco.enchantments.values.get("PROTECTION_ENVIRONMENTAL"));
        assertEquals(50L, eco.items.values.get("WOOL"));
    }

    @Test
    void packagedEconomyYmlResource_matchesEconomyConfFixture() throws Exception {
        try (InputStream ymlStream = MainCategory.class.getResourceAsStream("/economy.yml");
             InputStream confStream = getClass().getResourceAsStream("/economy.conf")) {
            assertNotNull(ymlStream, "Packaged /economy.yml resource must exist");
            assertNotNull(confStream, "Test fixture /economy.conf must exist");

            EconomyCategory ymlEco = ninja.leaping.configurate.yaml.YAMLConfigurationLoader.builder()
                    .setSource(() -> new java.io.BufferedReader(new java.io.InputStreamReader(ymlStream, StandardCharsets.UTF_8)))
                    .build().load().getValue(of(EconomyCategory.class), new EconomyCategory());
            EconomyCategory confEco = ninja.leaping.configurate.hocon.HoconConfigurationLoader.builder()
                    .setSource(() -> new java.io.BufferedReader(new java.io.InputStreamReader(confStream, StandardCharsets.UTF_8)))
                    .build().load().getValue(of(EconomyCategory.class), new EconomyCategory());

            assertEquals(confEco.economy_name, ymlEco.economy_name);
            assertEquals(confEco.economy_symbol, ymlEco.economy_symbol);
            assertEquals(confEco.max_area_toget_value, ymlEco.max_area_toget_value);
            assertEquals(confEco.items.values, ymlEco.items.values);
            assertEquals(confEco.enchantments.values, ymlEco.enchantments.values);
        }
    }

    @Test
    void conflictBothExistUnmigrated_prefersYamlAndPreservesBothWithoutOverwriting(@TempDir Path tempDir) throws Exception {
        Path conf = tempDir.resolve("blocks.conf");
        Path yml = tempDir.resolve("blocks.yml");
        Files.writeString(conf, "enabled = true\nblocks-to-value = 999\n", StandardCharsets.UTF_8);
        String originalYml = "enabled: false\nblocks-to-value: 50\n";
        Files.writeString(yml, originalYml, StandardCharsets.UTF_8);

        List<String> warns = new ArrayList<>();
        MigrationResult res = YamlConfigMigrator.resolveAndMigrate(
                tempDir.toFile(), "blocks", "Blocks Header", BlockCategory.class, null, warns::add, null);

        assertEquals(MigrationStatus.CONFLICT_YML_PRECEDENCE, res.status());
        assertFalse(warns.isEmpty());
        assertEquals(originalYml, Files.readString(yml, StandardCharsets.UTF_8));
        assertTrue(Files.isRegularFile(conf));

        BlockCategory loaded = YamlConfigMigrator.createYamlLoader(res.targetFile()).load()
                .getValue(of(BlockCategory.class), new BlockCategory());
        assertFalse(loaded.enabled);
        assertEquals(50L, loaded.blocks_to_add);
    }

    @Test
    void malformedConf_abortsMigrationWithoutWritingYamlOrTempFiles(@TempDir Path tempDir) throws Exception {
        Path conf = tempDir.resolve("textures.conf");
        Files.writeString(conf, "mob-textures {\n  ZOMBIE = \"abc\"\n  UNCLOSED_BRACE = {\n", StandardCharsets.UTF_8);

        List<String> errors = new ArrayList<>();
        assertThrows(IOException.class, () -> YamlConfigMigrator.resolveAndMigrate(
                tempDir.toFile(), "textures", "Textures", HeadTexturesCategory.class, null, null, errors::add));

        assertFalse(errors.isEmpty());
        assertTrue(Files.isRegularFile(conf));
        assertFalse(Files.exists(tempDir.resolve("textures.yml")));
        assertFalse(Files.exists(tempDir.resolve("textures.yml.tmp")));
        assertFalse(Files.exists(tempDir.resolve("textures.conf.bak")));
    }

    @Test
    void existingBackupFile_isNeverOverwrittenAndGetsTimestampedBackup(@TempDir Path tempDir) throws Exception {
        Path conf = tempDir.resolve("guiconfig.conf");
        Path preExistingBak = tempDir.resolve("guiconfig.conf.bak");
        Files.writeString(conf, "gui-separator { data = 7, material = \"STONE\" }\n", StandardCharsets.UTF_8);
        Files.writeString(preExistingBak, "previous-backup-content", StandardCharsets.UTF_8);

        MigrationResult res = YamlConfigMigrator.resolveAndMigrate(
                tempDir.toFile(), "guiconfig", "GUI Header", FlagGuiCategory.class, null, null, null);

        assertEquals(MigrationStatus.MIGRATED, res.status());
        assertNotNull(res.backupFile());
        assertNotEquals(preExistingBak.toFile(), res.backupFile());
        assertTrue(Files.isRegularFile(res.backupFile().toPath()));
        assertEquals("previous-backup-content", Files.readString(preExistingBak, StandardCharsets.UTF_8));
    }

    @Test
    void dynamicWorldWildcardComments_injectCommentsForEveryWorldKey(@TempDir Path tempDir) throws Exception {
        Path conf = tempDir.resolve("globalflags.conf");
        Files.writeString(conf, "worlds {\n  world_nether {\n    build = false\n    pvp = true\n  }\n}\n", StandardCharsets.UTF_8);

        MigrationResult res = YamlConfigMigrator.resolveAndMigrate(
                tempDir.toFile(), "globalflags", "Global Flags", GlobalFlagsCategory.class, null, null, null);

        String yamlText = Files.readString(res.targetFile().toPath(), StandardCharsets.UTF_8);
        assertTrue(yamlText.contains("# Players can build in this world?"));
        assertTrue(yamlText.contains("# Allow pvp?"));
    }

    @Test
    void injectComments_ignoresFakeKeysInsideMultilineScalarsAndSupportsDottedMapKeys() {
        String rawYaml = """
                worlds:
                  "world.nether":
                    welcome-message: |-
                      Hello traveler
                      build: fake inside block scalar
                    quoted-note: "line one
                      build: fake inside double quote"
                    plain-desc: first line
                      pvp: fake inside plain continuation
                    build: false
                    pvp: true
                """;
        java.util.Map<String, String> comments = new java.util.LinkedHashMap<>();
        comments.put("worlds.*.build", "Players can build in this world?");
        comments.put("worlds.*.pvp", "Allow pvp?");

        String result = YamlConfigMigrator.injectComments(rawYaml, null, comments);
        assertEquals(1, countOccurrences(result, "# Players can build in this world?"));
        assertEquals(1, countOccurrences(result, "# Allow pvp?"));
    }

    @Test
    void timestampedBackup_matchingOriginalConfIsIdempotent(@TempDir Path tempDir) throws Exception {
        Path conf = tempDir.resolve("config.conf");
        String hocon = "language = \"EN-US\"\n";
        Files.writeString(conf, hocon, StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("config.conf.bak"), "older-different-backup", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("config.conf.20261115-101010.bak"), hocon, StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("config.yml"), "language: EN-US\n", StandardCharsets.UTF_8);

        MigrationResult res = YamlConfigMigrator.resolveAndMigrate(
                tempDir.toFile(), "config", null, MainCategory.class, null, null, null);
        assertEquals(MigrationStatus.ALREADY_MIGRATED, res.status());
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
