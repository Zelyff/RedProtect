# AGENTS.md — Zelyff's RedProtect fork

## Mission
Conservatively extend FabioZumbi12/RedProtect with GP-like claim/inspect UX, one-for-one `.conf`→`.yml` config migration, three border modes with white through-wall Java glow on BLOCK markers, and optional Bedrock Forms. **Never rewrite working upstream systems merely for aesthetics.**

## Hard constraints (must enforce)
1. **Read** README.md, DECISIONS.md, UPSTREAM-RESEARCH.md, PLAN.md and engineering/PRODUCTION-MIGRATION-AND-ROLLBACK.md before editing.
2. Start only after pinning upstream full SHA, recording original config paths and actual code call sites, and reproducing a clean build. If unable, file BLOCKED with evidence. Never invent classes or keys.
3. Preserve `plugin.yml` public plugin name `RedProtect` and `/rp` alias by default, existing permission inheritance, API binary expectations, region IDs, file/database layout, native protection flags, hooks, and official addons.
4. GP inspiration is *interaction only*. Do not replace RedProtect storage, ownership, world rules, overlap semantics or events.
5. **Only three modes**: PARTICLE, BLOCK, HYBRID. `glow.enabled` is a property for block markers, never a separate mode. Java glow may use display entities; verify isolation, occlusion, culling and cleanup on target runtime. Bedrock through-wall entity glow is NOT promised.
6. Convert config `.conf` to `.yml` *one-to-one* only after inventory. No wholesale config key renaming or splitting. Preserve existing customized values, unknown keys and per-world overrides, and do not touch region databases.
7. Fail safely: on invalid migration do not silently use unsafe defaults; keep `.conf`, log exact failure, prevent unsafe operation until config resolved. No partial migrated state.
8. Java+Bedrock mutation requests always re-check owner, role, world, area/limits, events and overlaps at final commit; never trust client forms.
9. Feature flags: new functionality opt-in or disabled until proved stable; original behavior unchanged with new features off.
10. Every PR must contain source file/line evidence, unit/integration/manual tests, before/after behavior, perf impact, and rollback. Do not say 'tested' without execution evidence.
11. Do not silently delete files or remove comments/license. Keep upstream GPL-3.0 attribution and include `Zelyff` clearly as fork maintainer.
12. Avoid NMS and hard-coded entity packet implementations unless a measured need and versioned compatibility strategy exist. Never spawn world entities for every border block.

## Expected development workflow
- Phase 0: checkout → `git rev-parse HEAD` → dependency/build audit → config/listener/storage call graph → golden behavior snapshots.
- Phase 1: establish YAML parser/migrator with strict typed parity tests and reversible startup selection.
- Phase 2: implement interaction adapter only; call existing RedProtect operations.
- Phase 3: implement renderer strategies/cleanup, client-scoped visuals and counters.
- Phase 4: conditional Floodgate Forms adapter; no forced dependency.
- Phase 5: regression + release gates; GPL/credits.

## Required evidence in status updates
- `VERIFIED`: exact command/test run, input, output, environment and SHA.
- `SOURCE-OBSERVED`: URL/file/class/line from pinned checkout; runtime not verified.
- `RESEARCH`: external docs behavior, not tested on this fork.
- `DESIGN`: proposed mechanism awaiting implementation.
- `BLOCKED`: missing artifact/source/API/conflict with workaround.

## Stop/rollback conditions
- Any region ownership/flag difference with new features disabled.
- Migrator loss or type drift for known keys; unknown keys dropped.
- Missing/incomplete backups; partial writes; two configurations treated as active.
- Players see one another's visualization unintentionally or ghost blocks fail to restore.
- Unbounded entity/particle budget, main-thread I/O, or unsafe async Bukkit world operations.
- Bedrock Forms allowing stale permission/claim state to commit.

## Project communications
Use Indonesian documentation readable by server operators. Keep technical identifiers/code in English. No '100% bug-free' claims. Preserve original changelog separately from fork changes.

## Additional non-negotiable production migration guardrails (v1.1)
- The current operator has approximately a month of live RedProtect claims. **Do not request or import production data in planning**; perform source-first inventory, then gather *redacted* environment facts only when staging rollout is scheduled.
- **Storage schema and serialized region data are frozen in fork v1.** NO new DDL/schema migrations/region format conversion. Audit existing upstream auto-migration and startup write paths; ambiguity is a deployment blocker.
- Never point staging/test instance at live production SQL. Test with a fully isolated clone and prevent accidental network access to production DB.
- Before any production plugin swap, demonstrate consistent OFFLINE snapshot backup of old JAR + original `.conf` + region storage/DB + relevant world state, and prove full restore on isolated staging.
- Never rely on equality of region count alone. Compare canonical region keys, worlds, bounds, owner UUIDs, roles, flags, inheritance, priorities, parent/child and backend writes.
- Failed plugin enable/protection cannot be considered fail-closed if players may still enter. Keep server closed; stop/start properly, never `/reload` into a populated production server.
- Mandatory **GO/NO-GO** report from `templates/PRODUCTION-PREFLIGHT.md`. Any critical FAIL/BLOCKED = NO-GO, no "trial deployment" to live DB.
- Roll back a consistent versioned set, not only the JAR. A rollback to the old snapshot may lose valid activity since cutover; document/agree to freeze and reconciliation before production launch.
- Release includes tests T-035..T-046, including schema/write audits and full restore drill. Update report status only after evidence exists.
