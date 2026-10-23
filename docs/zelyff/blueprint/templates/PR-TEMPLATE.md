# PR: [brief change]

## Why / ticket

## Upstream commit & evidence
`commit SHA:`
`source paths/methods/lines:`

## Scope changed

## Scope deliberately unchanged
- [ ] Region storage/API/permissions/flags/commands unchanged, or precise documented exception

## Compatibility / config
- [ ] No old keys renamed or dropped
- [ ] Config migration impact described (if applicable)
- [ ] Java & Bedrock behavior described
- [ ] With new feature OFF, upstream equivalence preserved

## Tests (real output, not assertions)
| Test ID | Environment | Command/steps | Expected | Actual | PASS/FAIL |
|---|---|---|---|---|---|

## Performance / security / rollback

## Attribution and GPL

## Known limitations & follow-up

## Populated-server storage/regression impact (mandatory)
- [ ] No new schema migration, database table/column updates, or region serialization changes.
- [ ] Actual old production JAR/backend uncertainty recorded; no latest-version assumption.
- [ ] Startup/read/write/auto-migrate paths audited or explicitly BLOCKED.
- [ ] Does change affect config/provider/region persistence? If yes, attach typed parity and canonical region diff on isolated staging.
- [ ] Release ticket links `PRODUCTION-PREFLIGHT.md`, restore drill and GO/NO-GO before production; this PR approval alone is **not** approval to deploy.
