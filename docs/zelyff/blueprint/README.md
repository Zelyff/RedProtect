# RedProtect — Zelyff Fork Blueprint

> **Status:** Design baseline v1.1 (2026-10-22) — **NOT implemented, NOT compiled, NOT runtime-tested**.
>
> **Fork author:** **Zelyff**. Original creator: **ikillforeyou**. Upstream maintainer: **FabioZumbi12**. Preserve upstream GPL-3.0 notices.

## Core promise

**Extend, do not rewrite.** The fork keeps RedProtect's region model, protection listeners, storage, public APIs, commands, permissions, flags, integrations, legacy claim paths and add-on expectations, unless a narrowly scoped compatibility fix is demonstrably necessary and regression-tested.

### Precisely agreed product scope

1. **GP-inspired interaction:** optional two-corner golden shovel claim and corner resize, stick investigation and nearby inspection; all writes reuse the original RedProtect validation/creation/update/persistence paths. Original fences, wand, and commands remain available.
2. **Three border modes ONLY:** `PARTICLE`, `BLOCK`, `HYBRID`. In `BLOCK` and `HYBRID`, optionally give gold/glowstone markers **white entity-glowing outline** visible through obstacles in Java. No fourth glow mode. The virtual blocks themselves are *not* modified; supported renderers require in-game verification. Bedrock fallback does not promise through-wall glow.
3. **YAML configuration:** convert original user-facing `.conf` config files into logically equivalent `.yml` counterparts **one for one**. Retain keys, sections, types, values, default resolution and per-world behavior. Keep original `.conf` backups, do not migrate claims/databases, and add only minimal new settings for enhancements. Do not invent seven replacement config files.
4. **Bedrock optional UX:** detect Floodgate players and use Cumulus Forms as an extra management UI, routed through the same permission/region service as Java. Vanilla Java and no-Floodgate operation stay functional.
5. **Attribution:** fork credit `Zelyff`, with original credits/license untouched. Do not replace plugin's public `name`, commands or package just for branding.

## Where to start

| File | Purpose |
|---|---|
| [AGENTS.md](AGENTS.md) | Non-negotiable instructions for coding agents |
| [project/PLAN.md](project/PLAN.md) | Implementation stages, gates, tasks, dependencies |
| [project/DECISIONS.md](project/DECISIONS.md) | Accepted design decisions and unverified matters |
| [docs/UPSTREAM-RESEARCH.md](docs/UPSTREAM-RESEARCH.md) | Evidence, sources, limitations, upstream pinning |
| [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) | Product requirements and explicit exclusions |
| [design/ARCHITECTURE.md](design/ARCHITECTURE.md) | Architecture and integration contracts |
| [design/CLAIM-INTERACTION.md](design/CLAIM-INTERACTION.md) | Input flows/state machine and edge cases |
| [design/BORDER-RENDERING.md](design/BORDER-RENDERING.md) | Three modes, Java white glow, Bedrock fallback |
| [design/BEDROCK-FORMS.md](design/BEDROCK-FORMS.md) | Forms and permission/validation design |
| [engineering/YAML-MIGRATION.md](engineering/YAML-MIGRATION.md) | Migrator, rollback, parity, validation |
| [engineering/PRODUCTION-MIGRATION-AND-ROLLBACK.md](engineering/PRODUCTION-MIGRATION-AND-ROLLBACK.md) | **CRITICAL:** Existing server upgrade, DB safety, staging, cold backup, cutover, recovery |
| [engineering/COMPATIBILITY.md](engineering/COMPATIBILITY.md) | API/storage/hook/add-on compatibility |
| [engineering/BUILD-AND-RELEASE.md](engineering/BUILD-AND-RELEASE.md) | Maven baseline, quality gates, release |
| [qa/TEST-PLAN.md](qa/TEST-PLAN.md) | Test matrix and destructive scenarios |
| [qa/ACCEPTANCE.md](qa/ACCEPTANCE.md) | Definition of done per feature |
| [project/RISKS.md](project/RISKS.md) | Risk register, rollback plan |
| [project/CHANGELOG-PLANNED.md](project/CHANGELOG-PLANNED.md) | Planned release notes (not claims of shipping) |
| [templates/IMPLEMENTATION-PROMPT.md](templates/IMPLEMENTATION-PROMPT.md) | Copyable agent kickoff prompt |
| [templates/PR-TEMPLATE.md](templates/PR-TEMPLATE.md) | Pull-request checks |
| [templates/AUDIT-REPORT.md](templates/AUDIT-REPORT.md) | Evidence-first audit report template |
| [templates/PRODUCTION-PREFLIGHT.md](templates/PRODUCTION-PREFLIGHT.md) | Operator checklist and GO/NO-GO |
| [data/PRODUCTION-INVENTORY.template.json](data/PRODUCTION-INVENTORY.template.json) | Empty inventory template (no production data) |
| [examples/CONFIG-ADDITIONS.yml](examples/CONFIG-ADDITIONS.yml) | **Illustrative** enhancement keys, NOT deployable as-is |
| [examples/BEDROCK-OPTIONAL.yml](examples/BEDROCK-OPTIONAL.yml) | **Illustrative** optional Bedrock keys |
| [data/TRACEABILITY.csv](data/TRACEABILITY.csv) | Requirement → validation coverage |
| [data/BASELINE-MANIFEST.json](data/BASELINE-MANIFEST.json) | Pinned source candidates and verification state |
| [docs/CREDITS-LICENSE.md](docs/CREDITS-LICENSE.md) | Required attribution, legal checks |

## Decision on upstream

Latest GitHub *release page* verified: `v8.1.4`, asset `RedProtect-8.1.4-b11-release.jar` (the repository `master` advertises `8.2.0-SNAPSHOT` API). Do **not** mix snapshot sources with a release JAR; implementation phase must pin a single full Git commit SHA, capture build inputs, and prove vanilla upstream runs before coding. See [research](docs/UPSTREAM-RESEARCH.md).

## Honest limits of this pack

This is a documentation/planning pack, **not a source fork**, test suite implementation, migrator, or deployable config. Live repository ZIP/source could not be cloned into this workspace (network access unavailable); browsed README, `plugin.yml`, POM and official pages were used, but a comprehensive file-by-file forensic audit remains an explicit Phase 0 gate. Example configs are contracts/proposals only; do not merge them into production without matching actual upstream configuration hierarchy.

## Safety rule

No transition from planning to deploying on a populated Minecraft server without a full filesystem/database backup, a verified restore rehearsal, snapshot tests of claim data/permissions, and a non-production shadow test. **For the currently populated server (approximately one month of RedProtect use), migration is strictly NO-GO until every storage/version/backend difference is audited and a consistent cutover checkpoint passes. Do not connect staging to production DB.**

## v1.1 production migration addendum (2026-10-22)

The operator already runs RedProtect on a populated server. This pack **does not request database access** or a production ZIP now. A deployment migration protocol is now a *mandatory release gate*, not optional documentation. Read [PRODUCTION-MIGRATION-AND-ROLLBACK.md](engineering/PRODUCTION-MIGRATION-AND-ROLLBACK.md) **before touching production**, complete [preflight](templates/PRODUCTION-PREFLIGHT.md), and run T-035..T-046. Phase 7 adds staging/cutover with explicit GO/NO-GO. No DB schema/data migration is authorized in v1. If required, stop and escalate as a separate ADR and versioned project.
