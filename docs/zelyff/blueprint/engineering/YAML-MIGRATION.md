# `.conf` → `.yml` migration — conservative conversion specification

## Objective
Replace only the admin-facing HOCON `.conf` configuration representations with corresponding YAML `.yml` files. **Retain all logical path names, section hierarchy, effective values, types, overrides, comments where representable, and existing behavior.** All actual filenames must be discovered from pinned source/runtime before coding. Do not assume a hypothetical `flags.conf` exists.

## Strict separation
| Type | Action |
|---|---|
| Existing admin config with `.conf` | Convert to same basename `.yml`, preserving semantic structure |
| Existing `.yml` admin configs | Keep as-is; augment only when needed |
| Localization files | Preserve original localization scheme; migrate only if definitely part of target `.conf` admin config inventory |
| Claim region files, DBs, caches, schema | **No migration**, no renaming or deletions |
| Plugin `plugin.yml` | Existing standard Bukkit metadata, **do not** treat as converted admin configuration |
| New Bedrock-only settings | Small optional new file `bedrock.yml` **if warranted**, or subsection in existing config; source audit decides minimal-diff option |
| Border/claim settings | Prefer adding sections under original equivalent config paths rather than splitting existing config |

## Discovery deliverable
Create `evidence/config-inventory.csv` with `sourcePath,generatedPath,loaderClass,readSites,writeSites,reloadSites,defaultResource,worldOverrides,commentHandling,containsSecrets`. Include all HOCON-specific constructs: includes, substitutions `${...}`, duplicate keys, object merges, shorthand booleans, fallbacks and reference semantics. Any unresolved constructs are *blockers*, never silently flattened.

## Load/convert algorithm
1. Discover exact config sources and primary Configurate/other HOCON parser version from checkout. Back up plugin folder config and separately full region/database data before installing fork.
2. Parse `.conf` with **same semantics and default merging behavior as upstream**. Resolve permitted substitutions and includes; detect missing external inputs. Never evaluate arbitrary external paths.
3. Convert loaded typed node tree into YAML using compatible parser/writer; preserve null/bool/int/long/double/string/list/map exactly where possible. Explicitly quote ambiguous YAML scalars (`yes`, `no`, `on`, `off`, timestamps, numerics with leading zeros, UUID-like strings).
4. Write candidate YAML to temporary file *on same filesystem*. Re-read it using the **new actual runtime loader** and compare **effective typed configuration** against original after defaults and per-world overrides.
5. Record unsupported/unknown keys, comments that could not be mapped, and normalized differences in an audit report; **block publication if any material semantic mismatch**. Unknown keys should be kept (unless they cannot be serialized safely), not dropped just because code does not recognize them.
6. Flush/fsync and atomically replace candidate target where supported; otherwise a verified safe publish strategy. Keep `.conf` immutable; store backup & hashes.
7. Set a migration marker only after success. If `.yml` already exists, do **not** overwrite it automatically. If both formats exist, YAML is considered authoritative *only when valid and explicitly selected by the installed fork*, log the coexistence and detect modified old file without importing it again.
8. Subsequent startup loads YAML and merges **missing** defaults without overwriting custom values; save only when necessary. No startup unbounded disk I/O.

## Edge-case fixtures (must test)
- Empty config; nested objects with arrays; arrays containing maps; quotes and special characters; UTF-8; negative numbers, decimals, large integers; duration strings.
- HOCON substitutions `${some.path}`, optional `${?ENV_VAR}`, includes; if unsupported in YAML snapshot, capture **resolved effective value** plus explain lost dynamism or block migration if dynamic semantics are required.
- World names containing punctuation and dots; literal dotted keys; distinct `null` vs missing node.
- YAML typing: `true`/`false`, `on`, `off`, `yes`, `no`, `001`, `1e3`, `12:30`, string colon/hash, multiline MiniMessage placeholders and `%placeholder%`.
- Duplicate keys/conflicts, invalid syntax, read-only disk, disk full, crash between staging and publish, UTF-8 BOM, symlinks, externally modified config.
- Comments: best-effort retention plus migration report for comments lost (comment loss is **not** equivalent to value loss; target retaining admin guidance).
- Per-world config inheritance and fallback order. Unknown keys must survive repeated reload/update.
- Secrets: backups avoid excessive logs; redact passwords/tokens in report; never print raw database credentials.

## Failure/rollback matrix
| Situation | Expected response |
|---|---|
| New YAML invalid | Stop/disable dependent config safely, keep original `.conf`, do not auto-load guessed permissive defaults |
| `.conf` parse invalid | Report errors; keep files untouched; no destructive half-write |
| YAML and CONF both exist | Do not auto-convert; load validated chosen config and warn if diverged |
| File write interrupted | Temporary files cleaned next start, original not overwritten |
| New code unsupported data key | Preserve unknown nodes; report key and owner |
| Default value added upstream | Only add missing key; user values unchanged |
| Need roll back plugin JAR | Restore **matching** original `.conf` backed up; YAML not necessarily readable by upstream |

## Migration command UX — proposal, check collision first
- On fresh server, generate YAML from bundled defaults.
- Existing server, run migration at safe startup with preflight/dry-run and explicit approval/option chosen in deployment protocol; optional admin CLI `config-migrate --dry-run` in development tooling rather than in-game `/rp` command unless source audit validates command compatibility.
- Diagnostics include per-file result, SHA-256 source/target, keys migrated, warnings, semantic-diff summary and timestamp.

## Compatibility validation
Use a **golden fixture** produced by pinned upstream running on a test server: export effective config (with secrets redacted) and region/world behavior snapshot; verify the fork's YAML effective config matches path-by-path under same world/plugin conditions. Run real `/rp reload-config` or matching actual command and compare before/after. Never use regex/string replacements to implement this migration.

## Rollback drill
1. Stop test server, snapshot; install fork and migrate.
2. Change flags or a value through UI/command; restart and verify persistence.
3. Stop; restore upstream matching release JAR, original `.conf` snapshot, and data backup as necessary; verify claims and flags identical.
4. Prove backups can be restored *before* production rollout. No automatic deletion of `.conf` for at least first release; prefer indefinite archival.

## Why this is a high-risk feature
Configuration changes may indirectly change protections/permission defaults. The goal is *representation equivalence*, not cleaner configs. It is acceptable to ship other enhancements first while YAML stays behind a gate if typed parity cannot be demonstrated.

## Useful references
https://github.com/SpongePowered/Configurate
https://github.com/SpongePowered/SpongeDocs/blob/stable/source/plugin/configuration/loaders.rst

## Added v1.1: production server storage guard

Converting administrative `.conf` **never authorizes** touching plugin region/player/SQL persistence. In a populated server, migration must run first on a restored **isolated clone**, with the fork's enhancement flags off. Typed config parity must include effective database connection/provider settings, but reports must redact credentials. If the new YAML points to an unrecognized backend or unexpectedly yields zero claim regions, block startup/cutover rather than inventing an empty default. Follow `engineering/PRODUCTION-MIGRATION-AND-ROLLBACK.md` for backup consistency, source audits of auto-writing, and full version-pair rollback.
