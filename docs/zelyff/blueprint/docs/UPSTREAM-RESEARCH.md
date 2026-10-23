# Upstream research and evidence register

**Research snapshot:** 2026-10-22. Evidence grades: `OBSERVED` = actually visible in public webpage/repository file; `DESIGN` = unimplemented proposal; `UNVERIFIED` = requires full checkout + tests. Links may change.

## Verified public facts
| ID | Status | Finding | Source |
|---|---|---|---|
| E-01 | OBSERVED | Main upstream repository has `RedProtect-Core`, `RedProtect-Spigot`, and official addons, GPL-3.0, original creator `ikillforeyou`, upstream developer `FabioZumbi12`. | https://github.com/FabioZumbi12/RedProtect |
| E-02 | OBSERVED | Latest GitHub release page encountered points at tag `v8.1.4` and b11 JAR; master README advertises `8.2.0-SNAPSHOT` APIs. Lock a single checkout. | https://github.com/FabioZumbi12/RedProtect/releases/latest ; https://github.com/FabioZumbi12/RedProtect |
| E-03 | OBSERVED | Root POM declares modules Core, Spigot, and addons. | https://github.com/FabioZumbi12/RedProtect/blob/master/pom.xml |
| E-04 | OBSERVED | Spigot POM includes local `systemPath` JAR dependencies, including GriefPrevention and MyChunk, and compiler source/target 16. They may block reproducible builds. | https://github.com/FabioZumbi12/RedProtect/blob/master/RedProtect-Spigot/pom.xml |
| E-05 | OBSERVED | `plugin.yml` names the plugin `RedProtect`, main class `br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect`, `/rp` alias, various default permission sets, and soft-dependencies. | https://raw.githubusercontent.com/FabioZumbi12/RedProtect/master/RedProtect-Spigot/src/main/resources/plugin.yml |
| E-06 | DOCUMENTED | RedProtect supports legacy wand corners and `/rp claim`; wiki references `config.conf`, world-types, and old claim tools. Wiki may refer to older releases; verify exact checkout. | https://github-wiki-see.page/m/FabioZumbi12/RedProtect/wiki/How-to-use |
| E-07 | DOCUMENTED | GriefPrevention uses golden shovel for creation/resize, stick for investigation, nearby visualisation; adapt concept only. | https://github.com/GriefPrevention/GriefPrevention-Docs/blob/master/configuration.md ; https://docs.griefprevention.com/permissions/ |
| E-08 | DOCUMENTED | Paper Display API includes a configurable glow color for displays. This does **not** prove each buried/cull scenario works. | https://jd.papermc.io/paper/1.21.10/org/bukkit/entity/Display.html |
| E-09 | DOCUMENTED | Geyser limitation: Java glowing effect cannot be translated as-is for Bedrock. Validate fallback behavior. | https://geysermc.org/wiki/geyser/current-limitations/ |
| E-10 | DOCUMENTED | Floodgate forms can be sent using Floodgate API and Cumulus. | https://geysermc.org/wiki/floodgate/api/ ; https://github.com/GeyserMC/Floodgate/wiki/Forms |
| E-11 | DOCUMENTED | Configurate supports HOCON and YAML nodes/loaders; parse and serialize, not rename file. | https://github.com/SpongePowered/Configurate ; https://github.com/SpongePowered/SpongeDocs/blob/stable/source/plugin/configuration/loaders.rst |
| E-12 | DOCUMENTED | Hangar listing (2026-10-13) advertises RedProtect 8.1.4 for Paper 1.19–26.3; NOT proof this fork supports every version. | https://hangar.papermc.io/AreaZ12/RedProtectAntigrief/versions/8.1.4 |

## Important nuance: runtime version and source pin
A currently displayed GitHub release and current `master` commit are different artifacts. GitHub page shows tag `v8.1.4`, short commit `b097dfe`; a **full SHA is still mandatory**. Do not use b097dfe alone as reproducible provenance. Choose one target:
- `R1` pinned release tag `v8.1.4` with full SHA and matching release JAR for behavior baseline; OR
- `R2` pinned master snapshot with full SHA if a required upstream fix exists, separately recording reasons and tests.
**Default recommendation:** `R1` if reproducible; do not assume R2 is safer just because newer.

## Unverified/source audit backlog (Phase 0 mandatory)
1. Full list and locations of generated/config bundled `.conf` files, naming, HOCON library version, load/write/merge hooks, reload behavior, generated comments, per-world config and language files.
2. Source entry points for claim create/define/redefine/resize, fence/block create, world checks, permission limits, parent-child, economy checks, anti-exploit, and persistence.
3. Existing border packet/particle renderer: exact class, packets, cleanup, WorldEditCUI branch and how events are emitted.
4. Bedrock/Geyser behavior on target server with Floodgate enabled/disabled.
5. Database implementations and transactional semantics; events API used by third-party addons; backup/restore ability.
6. Whether any `.yml` already exists for *region data* — do NOT convert those as user config and do not mistake them for `.conf`.
7. Maven local dependencies and license obligations of all shipped libraries.
8. Compatibility with **AdvancedSlimePaper** and Java runtime in production; published Paper range is not sufficient.

## Research limitation
The public web allowed examination of README, root POM, Spigot POM, and `plugin.yml`, but a full source clone in the working environment failed due to DNS/network restriction. These documents therefore **do not claim** a forensic method-by-method source audit. Use `templates/AUDIT-REPORT.md` when source is available and replace all `UNVERIFIED` markers with grounded file/line findings.

## Baseline evidence to capture before first code change
```text
git remote -v
git rev-parse HEAD
git describe --tags --always --dirty
java -version
mvn -version
mvn -B -ntp -DskipTests=false verify
find RedProtect-Core RedProtect-Spigot -type f | sort
```
Record output, environment and checksums under `evidence/` in the actual fork; DO NOT add real player/database credentials or production config secrets.
