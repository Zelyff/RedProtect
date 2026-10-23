# Golden shovel and stick — UX protocol

## Modes/state machine
```
IDLE --(shovel click empty valid ground)--> SELECTING(cornerA)
SELECTING --(second corner click)--> VALIDATING --> COMMIT existing RP method --> IDLE
SELECTING --(invalid area)--> SELECTING(cornerA) [keep first corner, show reason]
IDLE --(shovel click owned claim corner)--> RESIZING(claimId,cornerId,revision)
RESIZING --(new corner)--> VALIDATING --> COMMIT existing RP redefine --> IDLE
ANY --(cancel/timeout/world change/quit/tool loss*)--> IDLE and cleanup
IDLE --(stick click)--> INSPECT (read only) --> IDLE
IDLE --(sneak+stick)--> NEARBY INSPECT (read only) --> IDLE
```
`*` Tool loss behavior is configurable, with explicit instructions to avoid accidental cancellation on Bedrock.

## Gesture design
- Right-click block with **main-hand** golden shovel: pick two opposite corners; in empty unclaimed land, make a rectangle. Favor on-block clicks to prevent accidental claims from air interactions.
- Existing RedProtect region operations supply vertical semantics (auto-expand/cuboid); **do not presume GP 2D height model**.
- If clicking an editable claim's corner, present/enter resize intent; corner recognition must be unambiguous and handle adjacent/nested regions. If multiple regions match, require selection rather than silently choosing one.
- Right-click block with stick: region info and border. Sneak+click: bounded nearby inspect. Preserve upstream visibility/permission policy for private details.
- Shovel gestures must be opt-in with config and permission. On unsupported/offline worlds, return original upstream denial reasons.
- Block break/use semantics: cancel relevant interaction to avoid digging with the shovel or using a block; preserve *unrelated* RedProtect event handler execution and priorities.
- Bedrock: inspect/claim gestures may differ due to translated inputs; provide explicit Form-assisted select-corner workflow if needed. Do not assume SHIFT+right-click means the same client packet.

## Ambiguities that require source/runtime validation
1. Native RedProtect 3D selection minY/maxY and any pre-existing wand selection state. Is it per-player? Can it be reused? 
2. Region priorities/child overlaps and when parent owners may resize.
3. Player default permissions (`redprotect.command.claim`, `redprotect.command.admin.redefine`, etc.) vs whether user resize exists.
4. Whether current claim operation triggers economy payment, claim count, claim block/balance and external hooks.
5. Native cancel/reset command and whether adding one is safe or command collision occurs.
6. Whether `GOLDEN_SHOVEL` or stick conflicts with other installed plugins; tools must be configurable.

## Checks performed at EACH commit, not only selection time
- Actor online/identity/world allowed, correct tool and current ability/permission.
- Region current existence, ownership/role, target ID (not merely name), parent/child and priority.
- New rectangle coordinates, min/max bounds, distance, minimum size, area/cost/claim count, world border and source engine world-height behavior.
- Overlap and containment rules, compatibility with concurrent region changes, admin protections.
- Existing RP validation logic and cancellable events; on failure no partial payment/region mutation.

## Proposed safeguards
- Per-player session TTL 90s (design starting point), reset on quit/world change; notify before timeout only if reasonable.
- Debounce duplicate interaction within 150–250ms **only after measuring** to avoid suppressing real quick taps.
- Cap nearby inspect regions (design starting point 3) and scanning radius (e.g., 48–96 blocks); use spatial index/region lookup API, not a full scan.
- Any confirmation UI holds an opaque server-issued session identifier, with owner UUID + expiry + target ID + expected revision.
- Resize requires visual preview; if region data changed between selection and commit, abort/restart rather than overwrite.

## Scenarios
| Case | Expected |
|---|---|
| Two valid corners | Claim created once, player sees claim name and border; original listeners/events fire |
| Small region, out-of-world, protected area, insufficient budget | No write/charge; explanatory message; may retry second corner |
| Click a region not owned | Inspect only or explicit denial; never enter resize |
| Multiple overlapping candidate regions | Explicit choose/cancel (not silently alter) |
| Shift+stick empty land | Nearby bounded visualization or 'none found' |
| Left/right/offhand duplication | Exactly one business operation |
| Player moves worlds or logs out | Session and temporary render cleaned |
| `/rp claim` after shovel selection | Existing native selection and manual commands remain coherent |
| GP simultaneously installed | Detect tool conflict; avoid two protection owners; migration should occur in test server |
