# Border rendering — exactly three modes

## Non-negotiable model
| Mode | Base rendering | Optional white glow |
|---|---|---|
| `PARTICLE` | Particle boundary line from existing or extended RP visualizer | No block glow; no extra mode |
| `BLOCK` | Temporary *client-side* virtual GOLD_BLOCK edges / GLOWSTONE corners | Yes, apply entity glow *overlay* at sampled block markers (Java only) |
| `HYBRID` | Particle line + virtual blocks | Yes, inherits BLOCK overlay config |

`glow.enabled: true` is **NOT** a fourth border mode. Implement only as additive enhancement to block marker renderer.

## How the white-through-walls effect works (Java design)
A regular Minecraft block cannot itself be given Java `Glowing`. Render virtual markers as gold/glowstone using the appropriate existing fake-block/client update mechanism and **separately** render small `BlockDisplay` (or other supported display entity) overlays co-located with the markers, configured glowing plus `setGlowColorOverride(Color.WHITE)`. The hoped-for effect is a white entity outline visible even when the marker is visually occluded.

**This is a hypothesis requiring in-game validation**: client culling/frustum, display dimensions/transforms, marker inside opaque blocks, glow strength with shaders, Bedrock packet translation, chunk unload, and entity tracker behavior are not guaranteed by the Paper API alone. If `BlockDisplay` overlay is inconsistent, trial alternative lightweight glowing entities with no collision, or opt out while preserving BLOCK. Do not promise every buried-block case before seeing actual footage/tests.

## Per-viewer visibility & lifecycle
- Scope temporary virtual blocks and glow overlays only to requesting viewer, using Paper entity show/hide or viewer-only packet mechanism when it works reliably; **avoid spawning entity visible to all viewers even momentarily**.
- Never change world block data or place permanent glowstone/gold blocks.
- Track each visualization session by `(viewerUuid, worldId, regionIds, renderGeneration)`; cleanup on timer, mode change, world switch, teleport, quit, plugin disable, chunk invalidation and reload.
- Virtual fake blocks must restore the *current authoritative client block state* when removed (not a cached stale material if the underlying world changed).
- Deduplicate shared border corners and segment samples; use capped pooled ephemeral markers when possible; no unbounded per-block entity spawns.
- Avoid scoreboard teams unless absolutely necessary; prevent conflicts with TAB and nametag plugins. Display color override is preferred over modifying player/team state.

## Geometry
- 2D rectangle corners computed from upstream region bounds; vertical/surface sampling reuses engine semantics and world-height rules. Do not generate infinite vertical columns.
- `SURFACE` uses sampled highest suitable *loaded* block (with caps), not synchronously loading many chunks. `PLAYER_Y` paints on one plane. Respect cuboid region bounds when relevant.
- Large claims use sparse sampled markers + particle lines, not one marker per meter across a huge perimeter.
- If a vertical wall blocks line of sight, through-wall glow applies to Java marker overlays; **particles themselves are not through-wall**.

## Bedrock decision
Geyser lists Java glowing as unsupported. Bedrock mode remains BLOCK/HYBRID but with ghost block/particle-compatible fallback; documented explicitly as **not providing through-wall white entity outline**. Bedrock Forms can display coordinates/size/owner instead of pretending x-ray exists. Test Geyser protocol translations of particles, block updates, and corner visibility.

## Proposed config semantics (actual path subject to upstream config inventory)
- `border.mode`: `PARTICLE | BLOCK | HYBRID` (existing border option reused if possible)
- `border.block.corner-material`: `GLOWSTONE`
- `border.block.edge-material`: `GOLD_BLOCK`
- `border.block.glow.enabled`: `false` initially until verified and benchmarks passed; then product default decision
- `border.block.glow.color`: `#FFFFFF`
- `border.block.glow.java-only`: `true`
- `border.block.glow.corner-only`: default `true` to protect performance; optional edge samples
- `border.duration-seconds`: restore original default if one exists; proposed only otherwise
- `border.max-glow-markers-per-viewer`, `border.max-active-viewers`, `border.max-radius`, `border.max-particles-per-second`: measured budgets, not magical constants

## Test geometry & expected behavior
1. Single exposed marker; confirm white outline around virtual gold/glowstone, with no double-block appearance.
2. Fully buried marker (stone 1/2/5 thick); from angles including behind walls, see if outline appears.
3. Several marks behind wall; ensure outline represents each marker, not an unrelated rectangle.
4. 3D boundary height at cliffs, caves, underwater, nether ceiling, unloaded chunks.
5. 1/5/20 simultaneous viewers and large claims; entity counts, packet rate, TPS/MSPT, frame rate.
6. Viewers outside render group see no entity (not even briefly). Quit/kick/teleport/disable restores virtual blocks and removes all entities.
7. Java with/without resource pack, shader variants; Bedrock phone and desktop with current Geyser/Floodgate.

## Source references
- Paper Display API: https://jd.papermc.io/paper/1.21.10/org/bukkit/entity/Display.html
- Geyser limitations: https://geysermc.org/wiki/geyser/current-limitations/
