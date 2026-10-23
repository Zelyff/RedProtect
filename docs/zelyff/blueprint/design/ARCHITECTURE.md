# Architecture and interfaces (design only)

## Principle: one region authority
```
Existing Java commands / wand / fence -----> [ Existing RedProtect pathways ] -----+
                                                                           |
NEW Golden Shovel / Stick --> [ Input & Session Adapter ] --> [ Shared Operations ]
NEW Bedrock Forms ---------> [ Form & Session Adapter ] --> [ Shared Operations ]
                                                                           |
                                                                           v
                                      Existing RedProtect validators/events/region API
                                                                           |
                                        Existing region storage, caches, hooks, flags

Existing /rp border / new inspect -> BorderStrategy { PARTICLE | BLOCK | HYBRID }
                                                   | BLOCK/HYBRID optional Java GLOW
                                                   | Bedrock fallback, per-viewer cleanup
```
The `Shared Operations` box may be **thin calls into upstream services** rather than a new core service. Do not duplicate validation, ownership checks or persistence. Source audit decides correct adapters.

## Component contracts
| Component (logical, NOT a claimed upstream class) | Role | Must not do |
|---|---|---|
| `ClaimInputAdapter` | Translate main-hand interaction to intent; dedupe | Persist claim directly or bypass command guards |
| `ClaimSessionStore` | Per-player `IDLE/SELECTING/RESIZING/PREVIEWING`, expiry | Persist as region data, survive world change |
| `ClaimActionBridge` | Invoke known RedProtect entry points, validate and commit | Reimplement claiming algorithm without evidence |
| `ClaimInspectionAdapter` | Resolve region at location; invoke border | Expose private info beyond upstream rules |
| `BorderStrategy` | Choose one of 3 modes + settings | Leave world-altering marker blocks |
| `JavaBlockMarkerRenderer` | Client-scoped ghost blocks + optional white glowing visual | Spawn markers for all players or entire region perimeter |
| `BedrockFormBridge` | Forms, native input mapping, session tokens, response handling | Grant access from client-supplied region IDs |
| `ConfigurationAdapter` | Load `.yml` from enumerated `.conf` sources, retain public semantics | Convert region storage or silently discard keys |
| `MigrationCoordinator` | Snapshot, translate, validate, atomic publish, report, rollback | Delete original `.conf` or run concurrent writers |

## Strict operation boundary
Use a server-side `RegionActionRequest(actorUuid, world, targetRegionId, operation, selectedBounds, expectedRevision)` only as an *internal* intent carrier if needed. Re-resolve target in current authoritative upstream region manager immediately before mutation. Preserve all upstream events and metadata. Re-check each permission in existing engine, not just in the UI layer.

## Lifecycle rules
`onEnable`: version check → config selection/migration → register legacy upstream behavior → register optional listeners/forms/renderers ONLY on successful prerequisites. `onDisable`: cancel sessions → hide client visuals/release entities → unregister optional hooks; never delete player region data.

## Threading
- Bukkit/Paper world/entity/region-state work on owning server thread (or server-specific scheduler if required); I/O off main thread only when safe; completion returns before mutating live region state.
- No asynchronous mutation of Bukkit Player, Block, World or scoreboard/entity properties.
- Form responses can race after role changes: re-check on owning execution context before commit.

## Data compatibility
Do not alter original region JSON/YAML/SQL schema merely for new presentation features. Session state is ephemeral. If metadata is required, prefer optional namespaced extension storage with explicit migration and audit, but v1 should avoid it.

## Events and integrations
Determine actual event names/classes from the checked-out source. Preserve event order/cancellation for region create, redefine, delete, rename, member changes and flags. Hook into existing public APIs and Vault, WorldGuard, Dynmap, PlaceholderAPI and official addons only through upstream’s existing adapters. No hardcoded assumptions that every hook always exists.

## Failure handling
Invalid action → localized failure message with original claim state untouched. Invalid config → preserve old original config, disable unsafe startup instead of loading permissive guessed defaults. Unsupported Bedrock render effect → documented fallback, never crash. Render failure → clear session and visual markers; does not affect claim state.
