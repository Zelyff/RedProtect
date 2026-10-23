# Master execution plan (WBS) — Zelyff RedProtect Fork

## Scope boundary
**Product:** RedProtect with added GP-like UX, three optional border modes (white glow specifically on BLOCK/HYBRID), YAML config equivalence, and optional Bedrock native Forms. **Method:** source-informed conservative extension; preserve upstream behavior. This plan prioritizes dependency gates over aggressive timelines. Never jump into feature coding before proving the upstream source and build.

## Phase 0 — Forensic baseline and environment (mandatory before feature code)
**Deliverables:** `evidence/SOURCE-MANIFEST.md`, `evidence/CALL-GRAPH.md`, `evidence/CONFIG-INVENTORY.csv`, `evidence/UPSTREAM-BUILD.log`, `evidence/BASELINE-RESULTS.md`.

| Task | Step | Completion / proof |
|---|---|---|
| 0.1 | Clone/tag lock upstream; print full commit SHA/branch and checksums | Full SHA and matching JAR/source; no dirty state |
| 0.2 | Reproduce Maven build with exact JDK and deps; document systemPath JARs and legal acquisition | Clean reproducible build or precise BLOCKED report |
| 0.3 | Inventory `.conf` and other config file paths, resources, parser, writer, reload | Every active config mapped to class/method references |
| 0.4 | Map region create/redefine/resize/claim permissions/area validation/payment/serialization/event execution | Accurate call graph and extension insertion points |
| 0.5 | Map current border visualization, marker strategy, WorldEditCUI integration | Existing behavior snapshot and refactor boundary |
| 0.6 | Inspect plugin.yml and plugin API/add-ons; note exact version differences from master | Compatibility baseline lists |
| 0.7 | Run vanilla upstream on isolated server using sample worlds/regions | Golden snapshot of configs, flags, ownership, commands and data |
| 0.8 | Capture target server matrix (Paper, AdvancedSlimePaper, Java, Geyser/Floodgate) | Precise tested environment inventory |

**Gate G0:** no feature code until source pinned, baseline is reproducible and extension call graph has verified classes/methods. If a missing optional binary blocks build, isolate build logistics (not protection semantics) and record gap.

## Phase 1 — YAML representation and safe migration
**Goal:** Convert exact original `.conf` admin configurations to `.yml`, one-to-one.

| Task | Implementation | Proof |
|---|---|---|
| 1.1 | Add YAML loader adapter at original config load boundary | Same runtime effective data tree fixture |
| 1.2 | List every existing key, inheritance, fallback, custom values and unknown keys | Parsed fixture snapshots |
| 1.3 | Conversion utility: HOCON typed nodes→YAML temp→read-back typed nodes | Roundtrip semantic parity |
| 1.4 | Preserve old file backups, detect dual-config conflicts, version marker, atomic writes | Repeated start/update never overwrites admin YAML |
| 1.5 | Handle HOCON includes/substitutions; generate redacted detailed report | Unmappable dynamic semantics block or documented strategy |
| 1.6 | Reload and default-merge tests; invalid YAML fail-safe | No permissive defaults on error |
| 1.7 | Restore upstream jar and original conf snapshot as drill | Existing claims/flags behave identically |

**Gate G1:** all migration fixtures pass; any missing dynamic HOCON semantic is a blocker. **No claim database conversion.**

## Phase 2 — Golden shovel/stick adapters
| Task | Implementation | Proof |
|---|---|---|
| 2.1 | Add feature flags + configurable materials/permission gates, disabled by default during staging | Feature-off behavior equals original |
| 2.2 | Per-player selection state, main-hand input, right-click dedupe, timeout/cancel | State machine unit tests |
| 2.3 | Two-corner claim request uses original RedProtect create/validation method | Region, payment, event and flags parity |
| 2.4 | Corner select/resize request uses original redefine geometry/validation method | Parent/child/overlap permissions correct |
| 2.5 | Stick inspect, sneak-nearby with bounded lookup and info rights | Read-only, bounded, correct roles |
| 2.6 | Bedrock translated interaction checks (before Forms) | No duplicate action or unexpected block break |
| 2.7 | Regression on original `/rp` commands, fence/wand, addons | No changed legacy behavior |

**Gate G2:** two clients, ownership/overlap/limit negative tests and legacy equality pass. Reject any direct database manipulation from input adapter.

## Phase 3 — Border rendering without fourth mode
| Task | Implementation | Proof |
|---|---|---|
| 3.1 | Wrap original border function (rather than replace unless required) in 3-mode strategy | Existing PARTICLE preserved |
| 3.2 | Client-scoped virtual GOLD_BLOCK/GLOWSTONE markers | No actual block/world edits, clean restore |
| 3.3 | Java BlockDisplay or equivalent glow overlay on markers (BLOCK/HYBRID) | White outline visibly penetrates chosen occluder test walls |
| 3.4 | Viewer isolation, render ownership, expiry, cleanup on quit/reload/teleport | Zero leaks and stale fake blocks |
| 3.5 | Bound samples, entities, chunk access, particle budget | Load scenario TPS/MSPT/player FPS evidence |
| 3.6 | Bedrock fallback (no glowing promise), test relevant translated packets | Bedrock visuals usable, graceful behavior |

**Gate G3:** Java glow criterion verified in screenshots/video. If not feasible, keep feature experimental/off and report exact limitation, do not claim gate passed.

## Phase 4 — Floodgate Bedrock UI
| Task | Implementation | Proof |
|---|---|---|
| 4.1 | Soft-dependency bridge, API compatibility check, platform selection | Server starts both with and without Floodgate |
| 4.2 | SimpleForm menu, claim summary and paginated claim list | Read-only displays match RedProtect state |
| 4.3 | Role/member and flags editing through same core action methods | Live permission check, admin-only flags protected |
| 4.4 | Rename/delete/resize confirmation, nonce/revision/session TTL | Replayed/stale Forms rejected |
| 4.5 | Localization, navigation and mobile ergonomics; graceful close/back | Bedrock user test result |
| 4.6 | Java UI regression; Bedrock/no-Floodgate fallback | No forced UI switch for Java |

**Gate G4:** Form and command outcomes equal; security negative tests pass.

## Phase 5 — Integration, regression, security, performance
| Task | Coverage | Proof |
|---|---|---|
| 5.1 | Permission, flag, storage, hooks, addon API compatibility | Upstream golden snapshots and addon smoke tests |
| 5.2 | All T-001..T-034 test cases | Dated, reproducible result matrix |
| 5.3 | Staged load test: 1/5/20 viewers, big and nested regions | Profiling and bounded resource counters |
| 5.4 | Migration crash/failure and full restore drills | Demonstrated rollback not just a README promise |
| 5.5 | Cross-platform versions 1.21+ through chosen newest target | Per-release compatibility matrix, not speculative green check |
| 5.6 | Security review of crafted Form response/concurrent edits | No P0 bypass unresolved |

**Gate G5:** zero unresolved P0 failures; any unresolved experimental glow or version limitation explicitly separated from stable scope.

## Phase 6 — Documentation, release, upstream sync
- Update in-game command/help messages (where altered) and public wiki; preserve upstream language bundles.
- Update `README`, `LICENSE` notices, contributor credits `Zelyff`, `NOTICE`, changelog with fork vs upstream separation.
- Build release candidate with source/tag checksums and system requirements; include configuration migration/rollback document.
- Canary in staging; then operational go/no-go decision. Maintain a downstream patch list to ease future upstream merges.

## Critical path
`Phase 0` → `Phase 1` (YAML) and `Phase 2` (tools) may share analysis but don't merge risky features together → `Phase 3` rendering after existing border audit → `Phase 4` Forms after safe actions exist → `Phase 5` acceptance → `Phase 6` release.

## Suggested PR slicing
`PR-00: upstream pin/build & tests only` → `PR-01: read-only config inventory/fixtures` → `PR-02: YAML adapter (not enabled)` → `PR-03: YAML migrator gated` → `PR-04: shovel inspect read-only` → `PR-05: shovel claim` → `PR-06: resize` → `PR-07: border block markers` → `PR-08: glow` → `PR-09: forms read-only` → `PR-10: forms mutations` → `PR-11: staging hardening/docs`.

## Change control and freeze
- Upstream bugfix is allowed only if test shows a blocker; separate PR and explain source regression.
- Feature expansion (economy redesign, claims-block accounting rewrite, admin web dashboard) remains post-v1 backlog, not included.
- If upgrading upstream midstream, update source pin, rerun G0/G1 and relevant snapshot suites first.

## Evidence notebook requirements
Each completed task records `pinned SHA, environment, changed files, problem, root cause, invariant, tests run, test output, known limitations`. Report pending tasks as pending; avoid “all fixed” without runtime.

## Phase 7 — Populated-server migration (must pass before production release)

**Context:** Server has approximately one month of existing RedProtect regions. This phase does not need access to live data *now*. It becomes mandatory near deployment.

| Task | Work | Deliverable/evidence |
|---|---|---|
| 7.1 | Inventory the **actual** old production JAR version, SHA, config files, storage backend/schema/driver, worlds and hooks (no credentials in docs) | `data/PRODUCTION-INVENTORY.template.json` completed privately |
| 7.2 | Audit upstream startup auto-migration/writers, fork DB initialization, region serializer, read/write rules | Source references and SQL/file mutation audit |
| 7.3 | Take immutable consistent offline backup of original JAR, config, storage/DB and related world data | Checkpoint A manifest + SHA-256 |
| 7.4 | Restore backup into *isolated* staging, verify it cannot reach prod DB, run original JAR to create golden baseline | Staging old-upstream restore evidence |
| 7.5 | Swap fork **in staging only**, one-to-one YAML migration with all enhancement flags OFF; compare region canonical states, config effective values, SQL/file writes | Complete diff & T-035..T-043 outcomes |
| 7.6 | Perform full **restore rehearsal** of original JAR + CONF + data snapshot; prove claim protections after restore | T-044 PASS |
| 7.7 | Put production in maintenance, stop server, take new consistent checkpoint B, verify identical preflight version/backend | GO/NO-GO worksheet signed |
| 7.8 | Controlled offline JAR cutover, server access still restricted, validate startup, claims, flags, hooks and no storage mutation | T-045, T-046, approval |
| 7.9 | Activate new features one by one only after baseline stable; monitor, preserve checkpoint B | Operators' monitoring record |

**Gate G7:** evidence-backed `GO` for every critical condition in `templates/PRODUCTION-PREFLIGHT.md`. Any missing restore test, unexpected storage write/schema mutation, region parity difference, plugin enable failure or backend ambiguity => **NO-GO and hold maintenance/restore**. No 'replace JAR on production and see' procedure.

**Rollback rule:** stop production, keep it closed; restore a *matching set* of JAR original + `.conf` original + consistent DB/file state, together with world state when required. Document that legitimate edits/claims since checkpoint B are not automatically retained when restoring B. See detailed runbook.
