# Changelog

All notable changes in this RedProtect fork (`8.2.0-SNAPSHOT`, branched from upstream `FabioZumbi12/RedProtect` `master` @ `54342f2e996cccb5963962bb949661a43590251a`) are documented in this file.

## [8.2.0-SNAPSHOT-zelyff] - 2026-11-15

### Added

- **Fork Branding & Standalone Safeguards (`Version by Zelyff`):**
  - Added `Zelyff` alongside `FabioZumbi12` in `RedProtect-Spigot/src/main/resources/plugin.yml` (`authors: [ FabioZumbi12, Zelyff ]`, description marked `(Version by Zelyff)`), startup/shutdown console logs, and `/rp update`.
  - Removed `SpigetUpdater` and kept `/rp update` disabled with a custom-build notice so upstream SpigotMC auto-update cannot overwrite this custom fork JAR.
  - Guarded `.github/workflows/build-on-tag.yml` (`Send Discord Embed` step) so standalone fork builds succeed cleanly when `DISCORD_WEBHOOK_URL` secret is unset.

- **GP-Style Direct Claim & Corner Resize (`CLAIM-01`, `RESIZE-01`):**
  - Added `ClaimSessionManager` (`RedProtect-Spigot/.../claim/ClaimSessionManager.java`) and `GpClaimInteractionHandler` (`RedProtect-Spigot/.../claim/GpClaimInteractionHandler.java`) cleanly separated from native wand maps (`firstLocationSelections` / `secondLocationSelections`).
  - Added `region-settings.claim.wand-mode` (`GP_DIRECT` | `NATIVE`, default `GP_DIRECT`) in `MainCategory.claimCat`.
  - In `GP_DIRECT` mode:
    - Right-clicking two corners creates a region immediately through `DefineRegionBuilder` (retaining auto-naming via `RedProtectUtil.nameGen`, claim/area limits, overlap checks, economy pricing, and Y bounds rules).
    - Right-clicking an existing 3D corner (`(x == minX || x == maxX) && (z == minZ || z == maxZ)` with `minY <= y <= maxY`, breaking ties by smallest area) of a region where the player passes `hasRegionPermLeader(player, "redefine", region)` starts a corner-resize session anchored at the opposite corner, resizing via `RedefineRegionBuilder(Player, Region, Location, Location, boolean preserveVerticalBounds)` that preserves custom `minY`/`maxY` bounds and guards child regions.
    - Cancels active selections on `sneak + left-click`, hotbar slot change (`PlayerItemHeldEvent`), world change (`PlayerChangedWorldEvent`), death (`PlayerDeathEvent`), quit (`PlayerQuitEvent`), or plugin reload/shutdown.
- **Boundary Preview Modes & Optional Java Glow Outline (`PREVIEW-01`, `PREVIEW-02`, `INSPECT-01`):**
  - Added `PreviewRenderMode` (`PARTICLE`, `BLOCK`, `HYBRID`), `PreviewVisualType` (GP Legacy v16 palette: `CLAIM` `GLOWSTONE`/`GOLD_BLOCK`, `ADMIN_CLAIM` `GLOWSTONE`/`PUMPKIN`, `SUBDIVISION` `IRON_BLOCK`/`WHITE_WOOL`, `INITIALIZE` `DIAMOND_BLOCK`, `CONFLICT` lit `REDSTONE_ORE`/`NETHERRACK`), `BoundaryGeometryCalculator`, and `RegionPreviewService`.
  - Added `MainCategory.region_settings.border` settings: `mode` (`BLOCK`), `glow-outline` (`true`), `fake-block-step` (`10`), `fake-block-radius` (`75`), `fake-block-max-elements` (`400`), and `glow-max-entities` (`48`).
  - Implemented optional Java `1.19.4+` `GlowOutlineOverlay` (`GlowOverlayBridge`) spawning non-persistent per-viewer glowing `BlockDisplay` markers (`setVisibleByDefault(false)`, `Player.showEntity(...)`), safely isolated from older Bukkit/Spigot APIs and skipped for Floodgate Bedrock viewers.
  - Routed `RedProtectUtil.addBorder` (used by `/rp border`, wand previews, `/rp info`, and `infoWandID`) through `RegionPreviewService`, with guaranteed restoration of exact client-side block data and glow entities on timeout, replacement, quit, world change, chunk unload (`WorldListener`), reload, and shutdown.
- **Bedrock Forms GUI Parity (`BEDROCK-01`, `BEDROCK-02`):**
  - Added `org.geysermc.floodgate:api:2.2.5-SNAPSHOT` (`scope=provided` from `https://repo.opencollab.dev/main/`) in `RedProtect-Spigot/pom.xml` and `floodgate` in `RedProtect-Spigot/src/main/resources/plugin.yml` `softdepend`.
  - Added `hooks.bedrock-gui.enabled` (default `true`) in `MainCategory.hooksCat` and safe runtime detection in `HooksManager` (`checkFloodgate()`, `isBedrockPlayer`, `isBedrockGuiEnabled`).
  - Added `BedrockGuiForms` and `BedrockFormStateHelper` implementing Cumulus Forms parity for `FlagGui` (including `gui-edit` slot editor), `ChestFlagGui` (`getContainerMaterials()`), `MobFlagGui` (`getMobEntities(String)`), and `ItemFlagGui` (registered items `SimpleForm` + held/inventory/text material `CustomForm`), revalidating region existence and permissions synchronously on the Bukkit main thread.
- **Configuration Migration `.conf -> .yml` (`YAML-01`, `YAML-02`):**
  - Added `YamlConfigMigrator` (`RedProtect-Core/.../config/YamlConfigMigrator.java`) with block-style `YAMLConfigurationLoader`, atomic `.yml.tmp` write + reload check, `.conf.bak` backup creation, `.yml` conflict precedence (never overwriting an existing `.yml`), parse-error abort without partial files, idempotence, and `@Setting(comment = ...)` + header comment injection.
  - Migrated `CoreConfigManager`, `ConfigManager`, and `BlockConfig` across all 7 runtime config files (`config.yml`, `globalflags.yml`, `guiconfig<LOCALE>.yml`/`guiconfig.yml`, `economy.yml`, `textures.yml`, `signs.yml`, `blocks.yml`), and converted bundled resource `economy.conf` to `economy.yml`.
- **Localization & Unit Test Suite (`RELEASE-01`):**
  - Added 20 new `gp.*` and `bedrock.gui.*` message keys to `langEN-US.properties`.
  - Created full Bahasa Indonesia translations `assets/redprotect/langID-ID.properties` and `assets/redprotect/guiID-ID.properties`, preserving common Minecraft and RedProtect English gaming terms (`Chest`, `Container`, `Door`, `Lever`, `Button`, `Pressure Plate`, `Sign`, `Fence`, `Potion`, `Level`, `Inventory`, `Region`, `Claim`, `Leader`, `Member`, `Flag`, `Block`, `Item`, etc.) for natural readability.
  - Added JUnit 5 + Mockito test suites across `RedProtect-Core` (`YamlConfigMigratorTest`, `LocalizationParityTest`, `CoreUtilTest`) and `RedProtect-Spigot` (`RegionCreationSupportTest`, `ClaimSessionManagerTest`, `RedefineVerticalBoundsTest`, `WorldRegionManagerTest`, `BedrockFormStateHelperTest`, `PermissionHandlerTest`, `HooksManagerBedrockTest`, `BoundaryGeometryCalculatorTest`, `RegionPreviewServiceTest`).

### Changed

- Updated `MainCategory.java` textures comment to reference `textures.yml`.
- Removed `SpigetUpdater` and disabled `/rp update` on this independent **Version by Zelyff** fork to prevent upstream builds from overwriting custom features.
- Routed `/rp flag`, `/rp flag gui-edit`, and `CommandHandlers` flag GUI openers to `BedrockGuiForms` for Floodgate Bedrock players when `hooks.bedrock-gui.enabled` is `true`, leaving Java chest inventories unchanged.
- Refactored container/mob item lists in `ChestFlagGui` and `MobFlagGui` into shared `getContainerMaterials()` and `getMobEntities(String)` helpers reused by both Java and Bedrock GUIs.

### Fixed

- Guarded `PlayerListener.onPlayerInteract` wand and info-wand triggers with main-hand (`EquipmentSlot.HAND`) and cancelled-block-click checks (`event.useInteractedBlock() == Event.Result.DENY`) so off-hand events or protected interactions in other plugins do not trigger duplicate wand clicks.
- Added explicit preview and claim-session cleanup hooks in `PlayerListener` (`PlayerQuitEvent`, `PlayerChangedWorldEvent`, `PlayerItemHeldEvent`, `PlayerDeathEvent`), `WorldListener` (`ChunkUnloadEvent`), and `RedProtect` reload/shutdown lifecycle paths so fake blocks and glowing `BlockDisplay` entities cannot leak.
- Removed legacy `.yml` -> `*_BKP.yml` rename blocks from `ConfigManager.java` so existing `.yml` configs are loaded natively rather than backed up away during migration/reload.
- Preserved RedProtect's region database schema (`data/*.yml`), flags, role hierarchy, commands, `BLOCK`/`BOTH` claim modes, and Java GUI behavior without breaking changes.
