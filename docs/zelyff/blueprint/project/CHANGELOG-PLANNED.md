# Planned changelog — NO features implemented yet

## `8.1.4-zelyff.1` (proposed; subject to baseline pin)
### Planned enhancements
- Optional GP-inspired golden shovel claim/resize and stick inspect/nearby inspect.
- Three border rendering modes (PARTICLE, BLOCK, HYBRID) with gold/glowstone virtual markers; optional white glowing outline overlay on Java BLOCK/HYBRID.
- Bedrock fallback for renderer and optional native Cumulus Forms.
- One-to-one `.conf` admin configuration conversion to YAML with parity verification and backups.
- Zelyff fork credit while retaining RedProtect original attribution.

### Intentionally unchanged
Existing region engine, storage/databases, protections, flags, commands, public APIs, permissions, and installed integrations.

### Known design limitation
Bedrock via Geyser does not support Java's entity glowing effect as an equivalent through-wall outline. Exact buried-marker Java effect requires staging test.

### Status
**DESIGN ONLY — no release date, no runtime claim, no JAR attached.**

## Documentation package v1.1 — 2026-10-22 (design only)
- Add production-safe migration and rollback protocol for a populated RedProtect server (~1 month existing claims).
- Explicit **no region/DB schema migration** contract, source-first auto-write audit, isolated staging, cold snapshots A/B, canonical region parity and mandatory restore rehearsal.
- Add GO/NO-GO template, redacted production inventory template and tests T-035..T-046.
- Update agent instructions, release gates, risks, roadmap and traceability. **No plugin/database changes were executed.**
