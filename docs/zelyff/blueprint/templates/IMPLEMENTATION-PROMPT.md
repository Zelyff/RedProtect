# Prompt for a future coding agent

> Copy the following into the coding-agent conversation after adding the **actual upstream RedProtect repository** and this blueprint pack to the workspace.

---

You are implementing an independent, conservative fork of RedProtect maintained by **Zelyff**. The workspace contains an upstream RedProtect Maven repository plus `RedProtect_Zelyff_Blueprint_v1.1` documents. **Read** `README.md`, `AGENTS.md`, `project/DECISIONS.md`, `project/PLAN.md`, `docs/UPSTREAM-RESEARCH.md`, and `engineering/PRODUCTION-MIGRATION-AND-ROLLBACK.md` before editing, plus each feature specification.

**Absolute intent:** KEEP EVERY EXISTING RedProtect feature/claim method/database/API/flag/command/permission/integration; add only the approved enhancements. No rewrite of region engine. The desired fork improvements are: (1) GP-like golden shovel two-corner claim and resize plus stick inspect; (2) border modes exactly PARTICLE, BLOCK, HYBRID with optional white glowing **block marker overlay** on Java BLOCK/HYBRID so gold/glowstone corner/edge markers are visible behind blocks, subject to honest runtime verification; (3) one-to-one HOCON `.conf` admin configuration migration to `.yml` preserving logical key hierarchy, defaults and all customized values, excluding region/player/database data; and (4) optional Bedrock Forms using Floodgate, backed by original RedProtect authority. Fork author name `Zelyff`; preserve upstream credits and GPL-3.0.

**FIRST TASK — Phase 0 ONLY**
1. Identify exact upstream remote/tag/commit SHA and current Maven build; capture baseline evidence and run real build/tests.
2. Deep-audit concrete code and resources: all `.conf` paths and load/write/merge/reload sites; region create/resize/permissions/events/storage call graph; border visualizer; plugin.yml/hooks; build dependencies.
3. Write evidence-backed Phase-0 report in `evidence/`, with file/class/method/line references, observed outputs, unresolved blockers, and precise extension points.
4. Compare live project to blueprint assumptions; update the documentation/ADR if findings require correction, **do not fabricate any file path or behavior**.
5. Present a PR-by-PR execution order and implement only after G0 passes. If G0 fails, report blocker and minimal solution with no unrelated rewrites.

**Implementation after G0**
- Do Phase 1 YAML with typed semantic parity, transactional staging, immutable backups, idempotence and safe rollback; never blindly rename `.conf` or overwrite edited YAML.
- Do Phase 2 shovel/stick with per-player session and original RP create/redefine guards; avoid offhand duplicates.
- Do Phase 3 three-mode visuals and through-wall Java block glow overlay with viewer-only visibility, bounded entity counts and cleanup; validate real Java/Bedrock behavior, no fourth mode.
- Do Phase 4 Bedrock optional forms with complete reauthorization/revision check at submit; missing Floodgate must be harmless.
- Do Phase 5 integration, regression, profiling, restore drill; ship RC only when QA gates pass.

**Deliverables in actual repo:** code changes, preserved original source, Maven build output, tests, docs/wiki, config migration examples, changelog, attribution and explicit known limits. **Do not** state “works”, “validated” or “final” unless build/runtime/tests were performed and their outputs included.

When finished with each PR/phase, summarize: files changed, evidence, technical decisions, tests executed with exact outcome, uncovered issues, next gated tasks. Do not start an unrelated feature. Never delete/rewrite original config or data without validated backup and explicit reason.

---

**ADDITIONAL MANDATORY PRODUCTION RULE (v1.1):** The operator already has month-old active RedProtect claims. Do not ask for server database or claim ZIP in planning. Do **not** change DB schema or serialized region storage in v1. Before any production deployment, source-audit the real installed old RedProtect version and backend, capture actual startup storage writers/auto-migrations, verify read-only staging isolation from production DB, take full consistent version-paired checkpoints, compare canonical region data and effective config with feature flags OFF, and demonstrate a full restore to old JAR + original CONF + matching snapshot. Run T-035..T-046; use `templates/PRODUCTION-PREFLIGHT.md` to issue a GO/NO-GO. Any failure or ambiguity => NO-GO; never "try" a fork JAR against live database. If plugin cannot enable, close the server to players; plugin disable by itself does NOT secure their land. Explain that rollback from old checkpoint cannot retain post-cutover changes without explicit reconciliation.
