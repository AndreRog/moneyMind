# Persistence test harness (Testcontainers + Postgres)

## What to build

Stand up a reusable integration-test harness in test scope only. It boots a disposable Postgres via Testcontainers, applies the existing Flyway migrations from `src/main/resources/db/migration/`, and exposes a jOOQ `DSLContext` plus a small seeding helper for `bank_transaction` rows. A single tagged smoke test proves the harness end-to-end: insert a row, read it back, assert equality.

Data isolation between tests is achieved by truncating between runs, not by restarting the container. The container is reused across the suite for speed. Integration tests are tagged/grouped so they can be run or excluded independently from the fast unit tests (`SummaryEngineTest`, `GuestImportTest`, `UpdateTransactionsTest`).

No production code changes. No schema changes. The harness is the prior art every subsequent persistence PRD (starting with the `TransactionQuery` issue) copies from.

## Acceptance criteria

- [x] A Testcontainers-backed Postgres starts for integration tests using the same Postgres image version as production
- [x] All existing Flyway migrations from `src/main/resources/db/migration/` are applied to the container automatically
- [x] A jOOQ `DSLContext` bound to the container is available to any test that uses the harness
- [x] A seeding helper allows inserting `bank_transaction` rows in a few lines
- [x] Each test (or class) runs against clean data — no state leaks between tests
- [x] The container is reused across the suite (not restarted per test)
- [x] Integration tests are tagged or named so they can be run separately from fast unit tests
- [x] A smoke test inserts a row via the harness `DSLContext` and reads it back, asserting equality — this is the prior art for PRD 0001's integration tests
- [x] The harness lives in test scope only — nothing added to the production artifact
- [x] All 13 existing unit tests still pass after the harness is added

## Setup notes

**Build command (run from module root, Windows, JDK 21 required, Docker must be running):**
```
./mvnw.cmd test -Pjooq
```
`JAVA_HOME` must point at JDK 21 (e.g. `C:\Program Files\Java\jdk-21`). Without `-Pjooq` the generated jOOQ classes are absent and compilation fails.

## Blocked by

None — can start immediately