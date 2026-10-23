# Risk register & mitigation

| ID | Severity | Risk | Mitigation & evidence gate |
|---|---|---|---|
| R-01 | CRITICAL | YAML parser changes effective protection/flags/defaults | Typed effective parity + real worlds fixture; fail closed on mismatch |
| R-02 | CRITICAL | Shovel bypasses region overlap/ownership/limits/payments | Reuse original commit API; negative tests and event parity |
| R-03 | HIGH | Damage/loss of region data on update | Do not change DB schema; staging snapshot; restore drill |
| R-04 | HIGH | Glow marker leaks entities or shows to other players | Per-viewer design/packet audit; lifecycle cleanup, caps and 2-client test |
| R-05 | MEDIUM | Glow invisible when buried, shaders or view culling | Client measurements; experimental until proven; safe no-glow fallback |
| R-06 | HIGH | Floodgate absence or API mismatch disables RedProtect | Optional bridge, guarded classloading, compatibility smoke tests |
| R-07 | HIGH | Bedrock Form response uses stale permissions/region | Re-fetch/authorize at submit, nonce/revision/expiry |
| R-08 | HIGH | Fork changes hook/API/permissions and breaks addons | Original test JAR API check, addon matrix, leave identity stable |
| R-09 | MEDIUM | Golden shovel/stick conflict with other plugins | Configurable tools, listener priority analysis, main-hand dedupe |
| R-10 | HIGH | Maven local systemPath libs unavailable/licensing unclear | Record exact missing deps, legal replacements, reproducible build procedure |
| R-11 | MEDIUM | Renderer impacts client FPS and server MSPT | Capped samples, per-viewer budgets, profiling and fallback |
| R-12 | HIGH | YAML and CONF coexist and overwrite admin edits | Migration state marker, immutable backup, policy for existing YAML |
| R-13 | MEDIUM | Unsupported Minecraft 26.3 runtime features | Test exact API builds, not blanket support claims |
| R-14 | HIGH | GPL credits/source availability omitted | Preserve LICENSE, original headers, source publishing procedure and review |
| R-15 | MEDIUM | Feature creep leads to invasive refactor | ADR guardrail, out-of-scope list and PR change budget review |

## Blockers before implementation
Full checkout unavailable in current artifact environment; thus source callgraph and exact config paths unknown. The baseline clone/build/inventory is a **mandatory first implementation task**, not something this pack claims to have completed.

## Rollback invariant
A rollback plan is acceptable only if the original matching RedProtect JAR + `.conf` + region DB backups can be restored to verified operation, including all claim/permission/flag state. A YAML file alone is not enough for reverting to upstream.

## Production deployment-specific risks (v1.1)

| ID | Severity | Risk | Mitigation / STOP condition |
|---|---|---|---|
| R-16 | CRITICAL | Production may use a different RedProtect storage version/backend than research sources | Pin actual JAR/schema/driver; unknown format => NO-GO |
| R-17 | CRITICAL | Plugin startup or upstream path auto-writes/migrates DB unexpectedly | Audit source & SQL/file changes; any unplanned DDL/DML => NO-GO |
| R-18 | CRITICAL | Staging accidentally targets live DB | Separate credentials/host/schema and network isolation; cannot prove isolation => NO-GO |
| R-19 | CRITICAL | Backup incomplete/inconsistent or only JAR downgraded | Cold consistent checkpoint + restore rehearsal of **JAR/CONF/data** together |
| R-20 | CRITICAL | Failed enable exposes claim areas while players remain online | Maintenance/closed access until protection healthcheck PASS; otherwise STOP |
| R-21 | HIGH | Counts equal but one region/owner/member/flag corrupted | Canonical per-region semantic compare + persistent-store diff |
| R-22 | HIGH | Return to checkpoint discards valid activity since cutover | Freeze window; clearly document conflict/reconciliation before production |
| R-23 | HIGH | YAML changes database connector/defaults causing blank/new storage | Config typed parity, backend connection verification; zero regions unexplained => NO-GO |

See `engineering/PRODUCTION-MIGRATION-AND-ROLLBACK.md` for execution details.
