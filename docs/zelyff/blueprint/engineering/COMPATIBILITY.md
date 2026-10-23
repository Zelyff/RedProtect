# Compatibility preservation contract

## Keep exactly, unless source evidence forces an isolated fix
- **Identity:** `RedProtect` plugin name, `/rp` and command aliases, existing package identifiers, dependent plugin lookup/API expectations.
- **Region state:** world identifiers, names, UUID ownership, roles/members, flags/default/inheritance, priority, parent-child, 2D/3D bounds, claim quotas, currency costs, cache and DB schema.
- **APIs:** public class/method signatures, events and cancellation, listener order, result types, plugin metadata/soft dependencies.
- **Existing UI:** command, GUI, wand/fence claiming, visualization, translations; no feature replaced by Bedrock-only Forms.
- **Hooks:** PlaceholderAPI, WorldGuard/WorldEdit, Vault, Dynmap, GP migration integrations, official addons, optional mod listeners, and other upstream-supported hooks.
- **Server matrix:** Paper target plus production AdvancedSlimePaper; don't claim compatible with Folia/old Spigot without running tests.

## Known evidence from upstream metadata
`RedProtect-Spigot/src/main/resources/plugin.yml` publicly declares main class, alias `/rp`, default permissions for claim/border/wand/info/rename etc and a broad softdepend list. Treat that as *candidate metadata for current master*, not a guarantee the release tag is identical.

## Feature-off equivalence methodology
1. Record **upstream golden output** on test server from exact pinned JAR/version/world/data: `/rp ?`, permissions, config effective values, world rules, claim/resize result, flags, interaction protection, backup checksums.
2. Run fork with *all new features disabled* and equivalent data. Compare: complete logical region state, permissions, storage bytes or canonical records, key emitted events, user commands/errors and protection decisions.
3. If any deviation, stop and investigate; no passing a regression by relaxing expected result.

## Official addons
Root POM declares addons (RedBackups, KillerProjectiles, RegionChat, BuyRentRegion) and upstream README describes their function. Smoke test each with original and fork builds; API and metadata compatibility gates are mandatory.

## Graceful plugin dependency handling
- Floodgate absent → Forms unavailable but all RedProtect protection stays functional.
- Vault or optional hooks absent → match original RP hook behavior, not silent enablement.
- WorldEditCUI absent → maintain original in-game border fallback.
- Server supports `Display` API at compile but not runtime → detect capabilities and disable glow only, not whole land plugin.

## Integration traps
- TAB/scoreboard nametag plugins: avoid scoreboard team edits for glow colors.
- GriefPrevention on same test instance: golden shovel/stick collisions and overlapping protection; migration done on separate staging server.
- Server world managers: region load/unload and world name changes are upstream responsibility.
- Dynmap/other map visualizations: ephemeral inspection borders should not persist as map markers unless original plugin does so.

## Test environments
- Minimum agreed target: 1.21+ (pin exact minor in the build; do not pretend one JAR automatically supports all targets).
- Latest desired target: 26.3+ where a compatible API/runtime exists; verify Java version, Paper API, and NMS avoidance.
- Production-specific: AdvancedSlimePaper build used by operator; capture full build string, Java version, Geyser/Floodgate versions and test on copy only.

## Plugin naming and author
Add `Zelyff` as fork creator/contributor in fork build metadata and README; do not discard `FabioZumbi12` or `ikillforeyou`. Avoid unnecessary renamed package/project artifacts that may break `getPlugin("RedProtect")` and dependent API access.

## Existing populated server: storage compatibility is a deploy gate

Do not infer storage compatibility from the repository's newest tag. The server's actual old RedProtect JAR and backend must be identified near cutover and compared against the pinned fork source. Preserve the original serializer/storage format and startup migration behavior unless changes are explicitly proven safe and separately approved. A successful plugin enable or equal claim count is insufficient: verify canonical region content, flags, owner UUIDs, hierarchy and permission decisions on an isolated clone. See `PRODUCTION-MIGRATION-AND-ROLLBACK.md`; any ambiguity = NO-GO.
