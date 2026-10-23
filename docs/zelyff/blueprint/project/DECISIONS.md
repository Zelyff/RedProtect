# Architecture decision record — decisions as of 2026-10-22

| ID | Decision | Rationale | Status |
|---|---|---|---|
| ADR-001 | Original RedProtect region engine authoritative | Prevent data/API/flag regression | ACCEPTED |
| ADR-002 | GP shovel/stick *interaction*, not GP storage/model | Keep scope minimal | ACCEPTED |
| ADR-003 | Three border modes only: PARTICLE, BLOCK, HYBRID | Explicit operator request | ACCEPTED |
| ADR-004 | White glow added as overlay to BLOCK and HYBRID | Visual markers through obstacles Java | ACCEPTED DESIGN; NEEDS RUNTIME PROOF |
| ADR-005 | Bedrock fallback for glow limitation | Geyser Java glow unsupported | ACCEPTED |
| ADR-006 | `.conf` admin config → matching `.yml` with same keys/semantics | Easier configuration, conservative update | ACCEPTED; INVENTORY PENDING |
| ADR-007 | No region DB/storage conversion | Avoid corruption & maintain compatibility | ACCEPTED |
| ADR-008 | Native Bedrock Cumulus Forms as optional UI | Mobile UX; no Java regression | ACCEPTED |
| ADR-009 | Fork credit **Zelyff**, retain original authors | Clear identity and GPL attribution | ACCEPTED |
| ADR-010 | Do not change Bukkit plugin public name / aliases just for fork branding | Protect plugin integrations | ACCEPTED |
| ADR-011 | Pin one upstream full SHA before implementation | Prevent moving-target audits | PENDING SOURCE CHECKOUT |
| ADR-012 | New features disabled by default until validated | Safe opt-in deployment | RECOMMENDED RELEASE DEFAULT |
| ADR-013 | Minimal possible source diff and no giant refactor | Merge future upstream changes | ACCEPTED |

## Questions to resolve *from source/tests*, not user preference
1. What is actual active `.conf` file inventory on pinned tag, and how are default config/inheritance applied? Owned by Phase 0.
2. Is Border visualization already controlled by a mode enum or a command? How to add options without breaking `/rp border`? Phase 0–3.
3. Is there a public method for resizing user regions with complete validations? Phase 0–2.
4. Does BlockDisplay glowing remain visible at all required occlusion situations? If not, what measured fallback is feasible? Phase 3.
5. Which Bedrock form API version is compatible with production Geyser/Floodgate and server build? Phase 4.
6. Which exact Minecraft version(s) and Java compile target can be supported with a single JAR? Phase 0/5.

## Change control
Any change to an accepted decision requires a short ADR extension: evidence, impact on preservation, rollback, tests and reason. No 'we found a nicer UI' exceptions that silently modify upstream logic.

## Production safety amendments — accepted 2026-10-22

| ID | Decision | Rationale | Status |
|---|---|---|---|
| ADR-014 | **Fork v1 never converts region data or changes DB schema** | Current production server has month-old player claims | ACCEPTED — SOURCE AUDIT REQUIRED |
| ADR-015 | No install/connection to live production storage until isolated staging and restore drill pass | Avoid irreversible auto-migrate/write | ACCEPTED |
| ADR-016 | Conservative one-for-one CONF→YAML only; preserve database connection semantics | Prevent runtime reconnect to wrong backend | ACCEPTED |
| ADR-017 | Production cutover requires maintenance, latest consistent cold checkpoint, documented GO/NO-GO and one active JAR | Protection cannot be trusted during failed enable | ACCEPTED |
| ADR-018 | Rollback restores consistent original JAR+CONF+storage snapshot; post-cutover edits require explicit reconciliation policy | Prevent false guarantee of lossless downgrade | ACCEPTED |
| ADR-019 | Unknown backend/version/auto-migration behavior blocks rollout; do not infer from current RedProtect release/latest repo | No unverifiable compatibility promise | ACCEPTED |

**Scope freeze:** these requirements govern the release process; they do **not** authorize rewriting core storage. See `engineering/PRODUCTION-MIGRATION-AND-ROLLBACK.md`.
