# RedProtect Fork Wiki & Admin Guide (`Version by Zelyff`)

Practical setup, player usage, configuration updater/migration, and GitHub release guide for this **RedProtect `8.2.0-SNAPSHOT` (Version by Zelyff)** fork.

---

## 1. Claiming & Resizing Regions (`GP_DIRECT` vs `NATIVE`)

### Player Workflow in `GP_DIRECT` Mode *(Default)*
1. Hold the configured RedProtect admin/claim wand (`wands.adminWandID` in `config.yml`, e.g. `glass_bottle` by default when configured — same wand item as upstream RedProtect).
2. **Claim a new region:**
   - **Right-click** corner 1 (`DIAMOND_BLOCK` fake-block marker appears).
   - **Right-click** the opposite corner 2.
   - RedProtect immediately validates claim limits, block limits, overlaps, world rules, and economy price via `DefineRegionBuilder`, creates the region with an auto-generated name, and renders the claim border (`GLOWSTONE` corners + `GOLD_BLOCK` sides). No `/rp claim` command is required.
3. **Resize an existing region from a corner:**
   - **Right-click** any `(X, Z)` corner of a region you own or have permission to redefine.
   - **Right-click** the new destination corner.
   - RedProtect stretches the 2D `X`/`Z` footprint anchored at the opposite corner via `RedefineRegionBuilder`, validating limits and overlaps while **preserving any custom `minY` / `maxY`** set on that region.
4. **Cancel a pending selection:**
   - `Sneak + Left-click`, switch hotbar slots, change worlds, die, or reconnect.

### Switching Back to Classic `NATIVE` Mode
In `plugins/RedProtect/config.yml`:
```yaml
region-settings:
  claim:
    wand-mode: NATIVE
```
In `NATIVE` mode, left-click sets position 1, right-click sets position 2, and `/rp claim [name]` / `/rp redefine <name>` work identically to upstream RedProtect. Fence/`BLOCK` claiming and `/rp pos1`/`pos2` work in all modes.

---

## 2. Boundary Previews & Java Glow Outline

Configured under `region-settings.border` in `config.yml`:

| Key | Default | Values / Description |
|---|---|---|
| `mode` | `BLOCK` | `BLOCK` (GP client-side fake blocks), `PARTICLE` (classic particles), `HYBRID` (both) |
| `glow-outline` | `true` | Java `1.19.4+` only glowing `BlockDisplay` outlines visible through solid blocks; auto-skipped for Bedrock & older servers |
| `fake-block-step` | `10` | Block interval between wall markers in `BLOCK` / `HYBRID` modes |
| `fake-block-radius` | `75` | Horizontal radius around viewer where border markers render |
| `fake-block-max-elements` | `400` | Max per-viewer client fake blocks sent per preview session |
| `glow-max-entities` | `48` | Max per-viewer glowing `BlockDisplay` entities spawned per preview |
| `time-showing` | `10` | Preview duration in seconds |
| `particle` | `FLAME` | Particle used in `PARTICLE` / `HYBRID` modes |
| `height` | `5` | Vertical particle spread around player Y |
| `intensity` | `NORMAL` | Particle density: `LOW`, `NORMAL`, `HIGH` |

### Fake-Block Visual Palette (`PreviewVisualType`)
| Purpose | Corner Block | Side Block | Trigger |
|---|---|---|---|
| Player Claim / `/rp border` / Info | `GLOWSTONE` | `GOLD_BLOCK` | Claim success, `/rp border`, `/rp info`, `infoWandID` |
| Server / Admin Claim (`#server#`) | `GLOWSTONE` | `PUMPKIN` | Border/info on `#server#` region |
| Initial First Corner | `DIAMOND_BLOCK` | `DIAMOND_BLOCK` | First wand click in `GP_DIRECT` |
| Overlap / Error Highlight | Lit `REDSTONE_ORE` | `NETHERRACK` | When a claim/resize overlaps another region |

All fake blocks use client-side `Player.sendBlockChange` and restore original server blocks automatically when the preview ends or cancels.

---

## 3. Bedrock Forms GUI Setup (Floodgate / Geyser)

1. Install **Geyser** and **Floodgate** on your server (optional — RedProtect runs cleanly without them).
2. Ensure `hooks.bedrock-gui.enabled: true` is set in `plugins/RedProtect/config.yml` (default `true`).
3. When a Bedrock player opens region flags (`/rp flag`, `/rp flag gui-edit`, chest/mob/item flag pickers):
   - Bedrock players receive Cumulus `SimpleForm` / `CustomForm` dialogs automatically.
   - Java players continue seeing classic RedProtect chest GUIs without any changes.
   - Toggle `hooks.bedrock-gui.enabled: false` to disable Cumulus Forms without removing Floodgate.

---

## 4. YAML Auto-Generation, Schema Updater, `.conf` Migration & Rollback

Starting from this version, all 7 configuration files in `plugins/RedProtect/` use `.yml`:
- `config.yml` (from `config.conf`)
- `globalflags.yml` (from `globalflags.conf`)
- `guiconfig.yml` / `guiconfig<LOCALE>.yml` (from `guiconfig*.conf`)
- `economy.yml` (from `economy.conf`)
- `textures.yml` (from `textures.conf`)
- `signs.yml` (from `signs.conf`)
- `blocks.yml` (from `blocks.conf`)

### Why Config Files Are Generated Instead of Static Files in `src/main/resources/`
Only `economy.yml` exists as a static resource template under `RedProtect-Core/src/main/resources/`. The other configs (`config.yml`, `globalflags.yml`, `guiconfig*.yml`, `textures.yml`, `signs.yml`, `blocks.yml`) are built dynamically by Configurate ObjectMapper from Java schema classes (`MainCategory`, `GlobalFlagsCategory`, `FlagGuiCategory`, `HeadTexturesCategory`, `BlockCategory`):
- **Fresh server without old `.conf`:** Startup automatically creates clean `.yml` files with all new defaults (`wand-mode: GP_DIRECT`, `border.mode: BLOCK`, `glow-outline: true`, `hooks.bedrock-gui.enabled: true`) and `@Setting` comments.
- **Future plugin updates (Config Schema Updater):** Whenever new config fields are added or `.yml` is saved on reload/startup, existing keys are kept and any newly added fields are automatically inserted into your `.yml` files.

### How Automatic `.conf -> .yml` Migration Works
- **Upgrade from old `.conf` server:** On startup, `YamlConfigMigrator` reads each `.conf`, verifies valid HOCON syntax, creates a backup `<name>.conf.bak` (using unique suffixes if a backup already exists), writes `<name>.yml.tmp` atomically, verifies the generated YAML parses cleanly, renames it to `<name>.yml`, and injects section/comment headers. The original `.conf` file is kept intact (not deleted).
- **Both `.conf` and `.yml` exist:** RedProtect treats `.yml` as authoritative, logs a conflict notice, and **never overwrites existing `.yml`**.
- **Parse error in `.conf`:** Migration aborts safely without writing a partial `.yml` so you can fix the `.conf` syntax and restart.
- **Note:** Region & player databases under `plugins/RedProtect/data/` are unaffected.

### Manual Rollback & Standalone Update Safety
1. `SpigetUpdater` has been removed and `/rp update` is **disabled** in this **Version by Zelyff** fork so your custom JAR is never overwritten accidentally.
2. To downgrade back to an upstream JAR that reads `.conf`, stop the server and restore `.conf` (or copy `<name>.conf.bak` back to `<name>.conf`).

---

## 5. Bahasa Indonesia Localization (`ID-ID`)

This fork bundles complete Indonesian translations for both chat/command messages and GUI flag descriptions, keeping familiar Minecraft/RedProtect English terms (`Chest`, `Door`, `Lever`, `Button`, `Sign`, `Fence`, `Potion`, `Level`, `Inventory`, `Region`, `Claim`, `Leader`, `Member`, `Flag`, `Block`, `Item`, etc.) intact:
- `assets/redprotect/langID-ID.properties`
- `assets/redprotect/guiID-ID.properties`

To enable Bahasa Indonesia in `plugins/RedProtect/config.yml`:
```yaml
language: ID-ID
```
Then run `/rp reload` or restart the server. Existing subcommand names (`/rp claim`, `/rp border`, `/rp flag`, `/rp wand`) remain unchanged under `cmdmanager.translation.*` so existing player guides and command muscle memory keep working seamlessly.

---

## 6. Pushing to Your GitHub Fork & GitHub Actions Build

1. Ensure `origin` points to your fork (`https://github.com/Zelyff/RedProtect.git`) and `upstream` points to `https://github.com/FabioZumbi12/RedProtect.git`:
   ```bash
   git remote -v
   ```
2. Check status (`AGENTS.md`, `docs/`, `research/`, `target/`, and `*.jar` are already ignored by `.gitignore`) and commit:
   ```bash
   git status
   git add -A
   git commit -m "feat: RedProtect GP claiming/previews, Bedrock Forms, YAML migration, ID-ID locale (Version by Zelyff)"
   ```
3. Push to a new branch or update `master`, plus optional release tag (`.github/workflows/build-on-tag.yml` skips Discord notifications cleanly when `DISCORD_WEBHOOK_URL` secret is not set):
   - Safe new branch push + tag:
     ```bash
     git push -u origin master:zelyff-8.2.0
     git tag v8.2.0-zelyff.1
     git push origin v8.2.0-zelyff.1
     ```
