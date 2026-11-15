# RedProtect (Spigot / Paper Fork `8.2.0-SNAPSHOT` — Version by Zelyff)

An enhanced standalone fork of [FabioZumbi12/RedProtect](https://github.com/FabioZumbi12/RedProtect) maintained by **Zelyff**. It preserves the full RedProtect region engine, protection rules, flags, role hierarchy, commands, integrations, and region database compatibility while modernizing claiming UX, boundary visualization, Bedrock Forms parity, YAML configuration management, and Indonesian (`ID-ID`) localization.

> 📖 Detailed admin, migration, and GitHub release walkthroughs are available in [`WIKI.md`](WIKI.md). Release details live in [`CHANGELOG.md`](CHANGELOG.md).

---

## ✨ Key Fork Enhancements

### 1. GriefPrevention-Style Direct Wand Claiming & Corner Resizing
- Selectable via `region-settings.claim.wand-mode` (`GP_DIRECT` or `NATIVE`; defaults to `GP_DIRECT`).
- **Two right-clicks to claim (`GP_DIRECT`):** Right-click corner 1 (`DIAMOND_BLOCK` marker) then right-click corner 2 to create the region immediately through RedProtect's `DefineRegionBuilder` (respecting area limits, claim limits, world restrictions, overlap checks, economy costs, and auto Y-expansion settings — without requiring `/rp claim`).
- **Corner resizing:** Right-click any corner `(minX/maxX, minZ/maxZ)` of a region you can redefine, then right-click the new corner position. Resizes 2D bounds via `RedefineRegionBuilder` while **preserving custom `minY` / `maxY` vertical bounds**.
- **Safe cancellation:** Cancel any pending claim or resize session via `sneak + left-click`, switching hotbar slots, changing worlds, dying, disconnecting, or reloading.
- **Native mode preserved:** Set `region-settings.claim.wand-mode: NATIVE` (or keep using `/rp pos1`, `/rp pos2`, `/rp claim`, `/rp redefine`, `BLOCK`, and `BOTH` claim modes) at any time.

### 2. GP-Style Fake-Block, Hybrid & Java Glow Boundary Previews
- Selectable via `region-settings.border.mode`:
  - `BLOCK` *(default)* — Client-side GP Legacy v16 fake-block palette (`GLOWSTONE` corners & `GOLD_BLOCK` sides for claims; `PUMPKIN` sides for admin claims; `DIAMOND_BLOCK` for first corner; lit `REDSTONE_ORE` / `NETHERRACK` for overlaps) clinging to visible ground surfaces via `Player.sendBlockChange(...)`. Never modifies real world blocks and restores exact original block data on timeout, replacement, quit, world change, chunk unload, reload, or shutdown.
  - `PARTICLE` — Classic RedProtect border particles (`FLAME` or configured particle, viewer-radius clamped).
  - `HYBRID` — Combines client-side fake blocks and particle outlines together.
- **Optional Java `1.19.4+` Glow-Through-Blocks Outline (`region-settings.border.glow-outline: true`):** Renders per-viewer non-persistent glowing `BlockDisplay` markers (`setVisibleByDefault(false)` + `Player.showEntity(...)`) on corners and stepped border markers, capped by `glow-max-entities` (`48`), safely isolated on pre-`1.19.4` servers, and automatically skipped for Bedrock/Floodgate viewers.
- Integrated consistently across `/rp border`, wand previews, `/rp info`, and `infoWandID`.

### 3. Bedrock Cumulus Forms GUI Parity (Optional Floodgate Hook)
- Controlled by `hooks.bedrock-gui.enabled: true` (defaults to `true`; `floodgate` declared in `softdepend`).
- RedProtect loads and runs normally when **Floodgate/Geyser is absent or disabled**.
- When Floodgate is present and enabled, Bedrock players automatically receive Cumulus Forms equivalents for:
  - Region flag menu (`/rp flag` & GUI toggle)
  - Flag GUI slot layout editor (`/rp flag gui-edit`)
  - Container whitelist/blacklist flag (`chest` / `ChestFlagGui`)
  - Mob spawning flag (`spawn-animals` / `spawn-monsters` / `MobFlagGui`)
  - Material/item list flags (`allow-break`, `deny-break`, `allow-place`, `deny-place`, `allow-enter-items`, `deny-enter-items`, `deny-exit-items` / `ItemFlagGui`) with held-item, inventory dropdown, and manual `Material` input
- All form responses are dispatched back onto the Bukkit main thread and revalidated live against online state, region existence, and current permissions. Java players continue using classic chest GUIs unchanged.

### 4. YAML Configuration Auto-Generation, Schema Updater & `.conf` Migration
- All 7 user-facing runtime configuration files plus packaged `economy.yml` use clean block-style YAML (`.yml`) instead of HOCON `.conf`:
  - `config.yml`, `globalflags.yml`, `guiconfig<LOCALE>.yml` (or `guiconfig.yml`), `economy.yml`, `textures.yml`, `signs.yml`, `blocks.yml`
- **Built-in fresh generator & config schema updater:** Just like upstream RedProtect, default configs (`config.yml`, `globalflags.yml`, `guiconfig*.yml`, `textures.yml`, `signs.yml`, `blocks.yml`) are generated automatically on first startup from `@ConfigSerializable` Java classes (`MainCategory`, `GlobalFlagsCategory`, `FlagGuiCategory`, `HeadTexturesCategory`, `BlockCategory`)—not copied from static template files in `src/main/resources/`. On every startup/reload, new keys are automatically merged into existing `.yml` files with comments while preserving existing user values.
- **Non-destructive automatic `.conf -> .yml` migration (`YamlConfigMigrator`):**
  - If only `.conf` exists: validates HOCON syntax first (never writes partial output on parse failure), creates a `.conf.bak` backup, writes `.yml` atomically via `.yml.tmp` + reload verification, injects section headers & `@Setting` comments, and keeps the original `.conf` untouched.
  - If both `.conf` and `.yml` exist: preserves both and uses `.yml` (never overwrites `.yml`).
  - Region/player data persistence formats are untouched.

### 5. Complete Bahasa Indonesia (`ID-ID`) Localization & Standalone Updater Safeguard
- Added full UTF-8 bundled localizations `assets/redprotect/langID-ID.properties` and `assets/redprotect/guiID-ID.properties`, keeping natural Minecraft/RedProtect gaming terms (`Chest`, `Lever`, `Button`, `Door`, `Sign`, `Fence`, `Potion`, `Level`, `Inventory`, `Region`, `Claim`, `Leader`, `Member`, `Flag`, `Block`, `Item`, etc.) intact while maintaining 100% key parity with `EN-US` (`LocalizationParityTest`).
- Added all 20 new `gp.*` and `bedrock.gui.*` message keys to `langEN-US.properties` and `langID-ID.properties` so `LangCore.updateLang` automatically populates them across existing server installations.
- Disabled SpigotMC auto-updating on startup and in `/rp update` so upstream JARs cannot overwrite this custom **Version by Zelyff** fork.

---

## ⚙️ Notable New Configuration Keys (`config.yml`)

```yaml
language: EN-US # Available: DE-DE, EN-US, ES-ES, FR-FR, ID-ID, PL-PL, PT-BR, RU-RU, ZH-CN
region-settings:
  claim:
    wand-mode: GP_DIRECT # GP_DIRECT | NATIVE
  border:
    mode: BLOCK # PARTICLE | BLOCK | HYBRID
    glow-outline: true # Java 1.19.4+ only; skipped for Bedrock
    fake-block-step: 10
    fake-block-radius: 75
    fake-block-max-elements: 400
    glow-max-entities: 48
hooks:
  bedrock-gui:
    enabled: true
```

---

## 🛠️ Building from Source

Requires **Java 17+** and **Maven 3.8+**:

```bash
mvn -B -nsu "-Dgpg.skip=true" "-Dmaven.javadoc.skip=true" "-Dmaven.source.skip=true" clean test package
```

- **Final ready-to-use server plugin JAR:** `RedProtect-Spigot/target/RedProtect-8.2.0-SNAPSHOT.jar` *(do not use `DONT-DOWNLOAD-Spigot-Reference.jar`, which is unshaded).*
- **Optional add-on JARs:** `add-ons/` (`RedBackups`, `KillerProjectiles`, `RegionChat`, `BuyRentRegion`).

---

## 📦 Developer API (Maven / Gradle)

```xml
<repositories>
    <repository>
        <id>sonatype-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>io.github.fabiozumbi12.RedProtect</groupId>
        <artifactId>RedProtect-Core</artifactId>
        <version>8.2.0-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>io.github.fabiozumbi12.RedProtect</groupId>
        <artifactId>RedProtect-Spigot</artifactId>
        <version>8.2.0-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

```groovy
repositories {
    maven { url = 'https://central.sonatype.com/repository/maven-snapshots/' }
}

dependencies {
    compileOnly 'io.github.fabiozumbi12.RedProtect:RedProtect-Core:8.2.0-SNAPSHOT'
    compileOnly 'io.github.fabiozumbi12.RedProtect:RedProtect-Spigot:8.2.0-SNAPSHOT'
}
```

---

## 📜 License & Credits

* Originally created by **ikillforeyou**.
* Actively maintained and expanded upstream by **FabioZumbi12** (`master` base commit `54342f2e996cccb5963962bb949661a43590251a`).
* **Fork customized & maintained by Zelyff (`Version by Zelyff`)** — standalone server build featuring GP-style direct claiming/resizing, client-side boundary previews & Java glow outlines, Floodgate Bedrock Forms parity, `.conf` to `.yml` migration/schema updating, and `ID-ID` localization.
* Boundary fake-block palette and surface clinging semantics inspired by **GriefPrevention** (`legacy/v16` reference).
* Bedrock Forms integration powered via **Floodgate / Cumulus API** (`provided` soft-dependency).
* Licensed under the [GNU General Public License v3.0 (GPL-3.0)](https://github.com/FabioZumbi12/RedProtect/blob/master/LICENSE).
