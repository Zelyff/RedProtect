# Maven build, CI and release specification

## Phase 0 clean baseline
1. Clone upstream at pinned tag/full SHA. Record repository URL, SHA, tag, license and every external artifact/checksum.
2. `mvn -B -ntp -DskipTests=false verify` with matching JDK, Maven and OS; capture actual console output, effective POM, dependency tree and toolchain information.
3. Upstream Spigot POM observed with local `systemPath` deps (e.g. `libs/GriefPrevention.jar`, `libs/MyChunk.jar`) and compiler target/source `16`; reproduce first, then document deterministic solutions for absent legal artifacts. Do not simply drop optional integration code to make compilation green.
4. Preserve root modules and plugin metadata. If using Java 21+ at runtime, compile compatibility and bytecode release need explicit choices, not blind upgrades.

## Build policy
- Maven is the selected tool. Keep upstream parent/modules and build as close as possible until baseline exists.
- No secret tokens, paid JAR redistribution or production config copied to repository.
- Pin dependencies; avoid snapshots unless necessary and documented. Use `mvn dependency:tree` and checksum significant JARs.
- Static quality: Checkstyle/SpotBugs as feasible without high-cost unrelated refactors; tests for new logic and contract fixtures; CI executes vanilla baseline target and fork tests.
- Optional external plugin APIs as `provided`/`compileOnly` concepts if framework supports; avoid shading duplicate plugin libraries into the final jar.

## Pull request gates
1. Source pin and relevant upstream methods cited; minimal diff demonstrated.
2. New setting has defaults, migration rules and documentation.
3. Positive + negative tests plus specific feature-off parity tests.
4. Profiling counters or benchmark for visualization and bulk config conversion.
5. No user data/storage schema changes without separate approved migration.
6. Public API/command/permission compatibility review and GPL attributions.
7. Release staging restore drill documented.

## Versioning (proposal)
`8.1.4-zelyff.1` only when pinned on the release branch and passed all gates. Start with `-dev` or `-rc1` for staging. Never use a stable release tag until tested. Include commit SHA and upstream pin in build manifest. Changelog separates original upstream fixes and fork-only features.

## Deployment protocol
- Back up entire `plugins/RedProtect`, database dumps, server/world state and plugin JAR; validate restore on staging.
- Clean staging server with same jar, Java, Geyser/Floodgate and world data snapshot.
- Migrate config in preflight, run feature-off regression and enable features individually.
- Canary with staff test accounts (Java and Bedrock), inspect metrics. No direct update on a populated server.
- Rollback = stop server, install original pinned jar and corresponding original `.conf` backup, restore needed DB snapshot, verify region hashes/flags.
