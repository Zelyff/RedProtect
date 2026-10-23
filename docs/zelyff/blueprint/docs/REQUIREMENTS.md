# Product requirements — agreed scope

## Vision
A conservative fork of RedProtect authored by **Zelyff**, making land protection easier for Java/Bedrock users without changing existing server/world/region behavior.

## Functional requirements
| ID | Requirement | Priority | Acceptance summary |
|---|---|---|---|
| FR-01 | Golden shovel 2-corner claim as optional interaction | P0 | Valid claim matches command create results/limits; existing tools still work |
| FR-02 | Golden shovel region corner resize | P0 | Existing RedProtect resize/validation logic used; illegal resize rejected |
| FR-03 | Stick inspect owner/details and boundaries | P0 | No mutation; permissions/info privacy honored |
| FR-04 | Sneak-stick nearby claims inspect | P1 | Limited radius/count, no expensive scans |
| FR-05 | Three border modes `PARTICLE/BLOCK/HYBRID` | P0 | No new fourth renderer mode; existing mode behavior preserved |
| FR-06 | White glowing outline on virtual gold/glowstone marker entities | P0 Java | Glow is optional subfeature of BLOCK/HYBRID; through-wall validated in-game |
| FR-07 | Bedrock fallback for unsupported Java glow | P0 | No Java-only glow promised; fallback rendering usable |
| FR-08 | One-to-one `.conf` → `.yml` conversion of actual existing config files | P0 | Typed semantic equality; source backup; safe startup on failure |
| FR-09 | Incremental config schema with no renames/section reshuffling | P0 | Original keys/defaults & custom values preserved |
| FR-10 | Floodgate native Forms for Bedrock player region management | P0 optional | Form access protected by platform check and revalidation |
| FR-11 | Java existing GUI and command UX retained | P0 | Behavior and permission parity with baseline |
| FR-12 | Optional feature toggles and platform fallback | P0 | With extras disabled, observable behavior equals upstream |
| FR-13 | Proper fork attribution `Zelyff`, original credits/license | P0 | No removal of GPL notices/creator names |
| FR-14 | Reproducible Maven build & compatibility matrix | P0 | Baseline and fork builds reproducibly on CI |

## Non-functional requirements
- Safety: new UX cannot bypass any legacy region validation; no silent downgrade to permissive defaults.
- Performance: no per-tick global region scans; no permanent display entities for markers; cap concurrent markers/particles and optimize by viewer.
- Data: preserve region UUID/ID, world name, ownership, flags, nesting, priorities, storage schema and backups.
- Availability: missing Floodgate disables forms only; it must **not** disable RedProtect protections.
- UX: Indonesian-friendly messages can be added as optional translations, preserving all upstream language bundles.
- Logs: clear warning with cause + remediation, not a stack trace loop; per-player inspect actions are not logged as critical security events.
- Maintenance: minimize diff against upstream so future changes can be cherry-picked/merged.

## Explicit exclusions (v1)
- Replacing RedProtect engine with GriefPrevention or importing entire GP Java codebase.
- New land shop, rent, economy currencies, factions, town systems, or claim-block currency model.
- Changes to region data format/databases, region coordinate policy, or flag semantics without necessity.
- New rendering mode `GLOW`; glow is an option on BLOCK/HYBRID.
- Through-wall glowing promise for Bedrock.
- Rebranding plugin name/package/commands automatically.
- Silent auto-conversion of all files in plugin directory; only *enumerated admin configurations*.

## Permissions and roles
Derive exact permissions from pinned upstream plugin.yml and command handlers. Map each new action to existing matching authority; only add dedicated `redprotect.enhancements.*` permissions if no suitable upstream permission exists. Do not grant elevated powers via simple ownership checks if RedProtect has an additional restrictions layer.

## Metrics for internal validation (provisional budgets)
- No *measurable* regression in p95 region lookup or claim operations with enhancements disabled under same synthetic load.
- Visualization must stop at configured duration and release all temporary entities/packets after quit/teleport/world change/disable.
- 100% tested config fixture semantic parity, including atypical HOCON syntax and per-world overrides.
- Zero newly introduced bypasses found in defined negative/security test cases.
The numeric performance threshold must be established using baseline measurement, not fabricated.
