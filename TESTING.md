# EnthusiaVotes testing

EnthusiaVotes uses JUnit 5, MockK, MockBukkit, Exposed and a local SQLite test dependency. Run the automated suite with:

```bash
./gradlew test --no-daemon
```

For a clean verification pass:

```bash
./gradlew clean test --no-daemon
```

Gradle test reports are written under `build/reports/tests/test/` and XML results under `build/test-results/test/`.

## Automated coverage currently protecting

The repository now has direct behavioral regression evidence for:

- streak reward multiplier thresholds and mining-multiplier cache behavior;
- VoteParty restore, vote counting, threshold activation, scheduled deactivation and persistence calls;
- SQLite vote totals, streak continuation, same-day voting, top-voter ordering and today's service set;
- offline gold accumulation and clearing;
- persisted VoteParty snapshots, including the transient `justActivated` boundary;
- architecture layer rules.

`FullFeatureCoverageContractTest` is an inventory guard. It ensures established behavioral suites are not silently removed or moved; it is not a replacement for assertions.

## Important remaining coverage gaps

This is the first broad behavioral harness for the repository, not exhaustive coverage. High-value remaining areas include:

- `VoteService.processVote` online/offline delivery, all-sites bonus and broadcast behavior;
- `VoteCommand`, `/votetop`, `/votesites` and admin command formatting/authorization;
- `MiningListener` reward multiplication and block eligibility;
- offline-login payout behavior and proxied delivery;
- NuVotifier listener input handling;
- Bedrock/Geyser form behavior;
- PlaceholderAPI expansion output;
- config loading/validation and vote-site preset mapping;
- plugin enable/disable wiring and real Paper integration;
- MariaDB-backed repository compatibility.

Prefer deterministic unit tests for application/domain policy and temporary SQLite databases for persistence. Use MockBukkit only where Bukkit behavior is actually part of the contract. Do not connect tests to production MariaDB or use live credentials.

## Change workflow

When behavior changes:

1. add or update a regression test that fails without the intended behavior;
2. run `./gradlew clean test --no-daemon` and inspect the failing test report;
3. add a new feature family to `FullFeatureCoverageContractTest` only after real behavioral evidence exists;
4. keep credentials, production databases and live server state out of tests;
5. keep release publishing separate from pull-request test validation.
