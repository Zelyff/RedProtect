# Verification matrix — test design, not test results

> A checkbox or scenario in this file is NOT proof of passing. Capture runtime, JAR/SHA, server/client versions, test dataset, console logs and reproduction steps.

## Test layers
1. **Source/contract tests:** inspect actual upstream create/redefine/flag/permissions/loaders/event call paths; assert existing public API unchanged.
2. **Unit tests:** intent state transitions, geometry, config conversions/typed compare, form session expiry, input validation.
3. **Integration tests:** Paper MockBukkit *only where representative*, real Paper staged server for events/database, Geyser/Floodgate client.
4. **End-to-end/manual:** two Java clients + two Bedrock clients; before/after snapshots; addon/hook interaction, large claims and rollback drill.

## Detailed functional cases
| ID | Area | Procedure | Expected result |
|---|---|---|---|
| T-001 | Legacy tools | Disable enhancements, create claim with upstream method | Identical RP state/events |
| T-002 | Shovel | Two corners valid, repeat rapid second click | Exactly one claim, correct limits/cost |
| T-003 | Shovel | Attempt to create on protected/disabled world | Reject with same RP denial |
| T-004 | Shovel | Second corner under minimum / beyond border | Reject, original state intact |
| T-005 | Shovel | Cross claim owned by another player | Reject/no partial write |
| T-006 | Resize | Move own corner, confirm updated geometry | All checks/events/storage correct |
| T-007 | Resize | Move someone else's or nested/admin claim | Correct role/priority restriction |
| T-008 | Race | Another player edits/deletes after preview | Stale session rejected safely |
| T-009 | Tool conflict | Offhand dup/rapid input/Bedrock tap | No duplicate claim/charge |
| T-010 | Stick | Inspect current/neighbor claim | Correct owner/info permission and borders |
| T-011 | Lifecycle | Logout/teleport/change world mid-selection | Clear selection/marker, no writes |
| T-012 | Particle | Show/hide border, giant claim, rough terrain | Bounded rendering, no stale particles |
| T-013 | Block | Show gold edge/glowstone corners without glow | Client-only, no world edits, restore on timeout |
| T-014 | Glow | Java glow enabled, marker fully buried behind stone | Verify observed white outline (record screenshot/video); mark FAIL if absent |
| T-015 | Glow | 3D marker culling/chunks/occlusion/shaders | Identify limitations; graceful fallback |
| T-016 | Hybrid | Particle + block marker glow | One border mode, no duplicate entities |
| T-017 | Viewer isolation | Two viewers: only requester asks border | Other client sees no fake blocks/glow entities |
| T-018 | Bedrock render | Bedrock glow option enabled | Fallback still visible; no false through-wall promise |
| T-019 | Bedrock Forms | Owner opens region settings | Only permitted actions visible and usable |
| T-020 | Bedrock Forms | Form stale, role removed, claim deleted | No mutation, explanatory error |
| T-021 | Bedrock Forms | Missing Floodgate | Plugin still enables/protects; original UI works |
| T-022 | Config | Full multi-world HOCON fixture to YAML | Parsed effective typed nodes equal |
| T-023 | Config | YAML scalar string `on`, `001`, `1e3`, text `:` | No type changes |
| T-024 | Config | Duplicate unknown keys, includes, substitution | Preserve semantics or halt migration |
| T-025 | Config | Both YAML & CONF, YAML customized | Do not overwrite YAML on restart |
| T-026 | Config | Invalid/disk-full partial migration | Atomic abort; safe startup; restore works |
| T-027 | Data | Before/after region DB dump/canonical state hash | No unintended change |
| T-028 | API | Load upstream addon/API client | No broken class/method/hook |
| T-029 | Flags | Toggle existing flags and role management | Same logic/defaults/event result |
| T-030 | Scale | 20 viewers inspect large regions | Capped entity count, no runaway load |
| T-031 | Restart | Active render/session during shutdown/reload | No stranded fake blocks/entities/session |
| T-032 | Rollback | Restore `.conf` + original JAR from backup | Original protections & config back |
| T-033 | World | Nether/End/dungeon worlds not claiming-enabled | Honor original per-world settings |
| T-034 | Addons | RedBackups/BuyRentRegion/etc staged | No regression from upstream baseline |

## Property-based / invariant tests
- Rectangles produced by two corners are normalized `min<=max` and world-consistent; never integer overflow on area/perimeter.
- Failed claim mutation leaves `regions`, payments and region cache unchanged (atomicity or rollback).
- `role_before`, `role_after` and permission check at commit time agree with canonical RP authority.
- Config conversion roundtrip: `typedEffective(CONF) == typedEffective(YAML)`, including fallback and world inheritance.
- New features disabled ⇒ functional equivalence on golden fixtures (not necessarily byte-for-byte config file serialization).
- Same viewer's repeated border refresh never grows outstanding marker count unboundedly.

## Performance & observation
Record TPS/MSPT using consistent test conditions, p95 claim operation latency, config load time, total session/marker counters, packets/particles rate where observable, and client FPS/visibility for Java and Bedrock. Establish baseline distributions before numeric pass threshold; one arbitrary TPS snapshot is insufficient.

## Security tests
Try crafted form responses, admin-only flags, authorization race, owner renamed, UUID collisions, invalid world, oversized text/name, reload during operation, unexpected plugin disable, overlarge claim with edge coords near world border, double payment and null/invalid material in config.

## Evidence log format
`CASE | SHA | Server | JavaVersion | Geyser/Floodgate | Client | Preconditions | Steps | Expected | Actual | PASS/FAIL/BLOCKED | Logs/Screenshot | Tester | Date`.

## Production migration / database hardening cases (added in v1.1)

| ID | Area | Procedure | Expected result |
|---|---|---|---|
| T-035 | Production version | Inventory actual installed old JAR/build/driver and compare to source pinned fork | Exact version/hash/backend recorded; unknown => BLOCKED |
| T-036 | Backend | Inventory every DB/file/region loader, serializer and startup auto-upgrade hook | No unknown writer or unreviewed schema upgrade path |
| T-037 | Isolation | Restore clone; assert staging cannot connect to prod SQL/backend (including negative network attempt) | Production untouched; no shared data |
| T-038 | Backup | Cold full checkpoint A: JAR + config + storage/DB + relevant worlds, validate hashes | Complete recoverable set |
| T-039 | Parity | Compare canonical region keys/owner UUID/member/flags/bounds/priority/parent-child old vs fork | Exact semantic equality with enhancements disabled |
| T-040 | SQL / file writes | Capture DB schema and DML/file changes on fork staging startup with no user action | All observed writes explained/equivalent; no unplanned changes |
| T-041 | Wrong backend | YAML storage provider/credential/DB name differs from old effective config | Cutover blocked, never create/use silent empty storage |
| T-042 | Boot failure | Force DB offline/invalid config/plugin startup failure while admission closed | Players remain blocked; no unprotected world access |
| T-043 | Version drift | Mismatch real production backend/version vs tested staging | Explicit NO-GO and re-run audit, not auto-upgrade |
| T-044 | Restore drill | Restore original JAR + `.conf` + DB/region snapshot onto isolated staging, check sample blocks | Claims/flags/owners/interactions restored identically |
| T-045 | Cutover | Make new checkpoint B immediately before upgrade, stop server, install only one RedProtect JAR | Immutable checkpoint, no dual-plugin conflict, access still closed |
| T-046 | Post-cutover rollback | Simulate a legitimate new claim after fork update then restore checkpoint B | Document that post-checkpoint changes are not retained without manual reconciliation; return to consistent upstream state |

Evidence fields include DB engine/version (redacted), schema/file manifest before/after, staged SQL query diffs when applicable, canonical region hashes and restore logs. NEVER run these destructive/failure tests on active production; T-045 is an operator maintenance rehearsal and final deployment checklist, not permission to experiment with real player data.
