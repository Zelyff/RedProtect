# Acceptance gates and release readiness

## Gate A — Upstream pinned and reproduced
- [ ] Full SHA pinned with tag, checksums and provenance; no release/snapshot mixing.
- [ ] Maven build reproducible on documented Java/OS setup.
- [ ] All actual HOCON config file paths, parsers, generated defaults, mutable load/write sites inventoried.
- [ ] Claim creation/resize/overlap/economy/role/region events traced to source.
- [ ] Golden upstream test world/fixtures and API/addon smoke baseline saved.

## Gate B — YAML parity
- [ ] One-to-one conversion of every enumerated admin `.conf`; no speculative renames/splits.
- [ ] No migration of databases, region state, player data or unrelated `.yml`.
- [ ] Typed effective-config parity tests across default, custom, and world configs.
- [ ] Atomic save, original backup, conflict policy, migration report, and real rollback test.
- [ ] Fail safely on parse/validation error; no silently permissive fallback.

## Gate C — Tools
- [ ] Shovel claim and resize reuse upstream validation/mutation paths.
- [ ] Stick/nearby inspect read-only and bounded; no information/permission leaks.
- [ ] Legacy fence/wand/command interfaces unchanged with feature disabled.
- [ ] Negative scenarios T-002..T-011 pass on real staging server.

## Gate D — Visuals
- [ ] Exactly `PARTICLE`, `BLOCK`, `HYBRID` modes.
- [ ] Gold/glowstone virtual markers remain intact in BLOCK; glow is optional property inherited by HYBRID.
- [ ] Java buried-marker white outline reproducible on target builds (or clearly documented feature is experimental/disabled, **not** declared passed).
- [ ] Bedrock renderer fallback visibly functional without claiming wall-penetrating glow.
- [ ] No persistent world changes, leaked display entities or stale fake blocks after lifecycle changes.
- [ ] Rendering budgets observed under player/large-claim load.

## Gate E — Forms & integrations
- [ ] Java legacy UI unchanged.
- [ ] Floodgate optional: absence doesn't break plugin enable or region protection.
- [ ] Forms never bypass permissions; validation re-run on submit; stale sessions rejected.
- [ ] Official addon hooks/supported integration smoke tests pass.

## Gate F — Release
- [ ] All 46 tests in TEST-PLAN have status & evidence, no unresolved P0 failures.
- [ ] Version/build provenance, GPL and Zelyff/upstream credits included.
- [ ] Changelog, user guide, admin migration/rollback and known limitations current.
- [ ] Staging restore successful; explicit go/no-go approved before production deployment.

**Release rule:** gate not passed => remain `design`, `dev`, or `RC`, never claim a complete production-ready plugin.

## Gate G7 — Already-running production server (mandatory; default NO-GO)
- [ ] Actual current upstream production JAR/version/SHA known; no guessing from GitHub latest.
- [ ] Actual backend/config/schema inventoried with source-proven read/write/auto-migrate paths.
- [ ] **No** fork database/region schema migration; startup mutations are audited, justified and compared on staging.
- [ ] Staging SQL/storage clone is isolated from production (verified connection policy).
- [ ] Cold/consistent complete production backup includes old JAR, `.conf`, region data/DB and relevant world snapshot.
- [ ] Full **restore drill** using upstream JAR on staging passes, including real protection checks.
- [ ] Feature-off fork has equal canonical region state (not only count), flags, permissions, config and hooks.
- [ ] Production checkpoint B immediately before swap; only one RedProtect JAR enabled and server in maintenance.
- [ ] Failure path denies **player access**, including if plugin fails to enable (not just a logger error).
- [ ] Post-cutover writes are tracked, and rollback/reconciliation trade-off acknowledged.
- [ ] `templates/PRODUCTION-PREFLIGHT.md` final verdict is `GO`; every critical item PASS with evidence.

A G5/G6 green status does **not** override G7. Target fork is not deemed safe for the populated server until G7 passes.
