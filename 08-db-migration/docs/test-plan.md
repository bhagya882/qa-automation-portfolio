# Test Plan – Database Integrity and Data Migration Validation

## 1. Scope

**In scope:**
- schema migration V1→V5
- data type and constraint verification
- data cleansing rules
- source-to-target reconciliation (counts, money totals, record level)
- quarantine of rejected rows
- idempotency
- rollback (undo scripts)
- transactional atomicity

**Out of scope:**
- migration performance at production volume
- application-level (API/UI) behaviour
- backup/restore tooling
- vendor-specific features (partitioning, functional indexes)

## 2. Migration rules

| Rule | Condition | Action | Reject reason |
|---|---|---|---|
| R1 | Customer email NULL/blank | Quarantine | `NULL_EMAIL` |
| R2 | Email duplicates another (case/whitespace-insensitive) | Merge into the lowest id; map legacy→survivor | `DUPLICATE_EMAIL_MERGED` |
| R3 | Order's customer not migrated or missing | Quarantine order and its lines | `CUSTOMER_NOT_MIGRATED` |
| R4 | Order has a line with quantity ≤ 0 | Quarantine order and its lines | `INVALID_ITEM_QUANTITY` |
| R5 | Order item without legacy order | Quarantine | `ORPHAN_ORDER_ITEM` |
| R6 | Item of a quarantined order | Quarantine | `PARENT_ORDER_REJECTED` |
| C1 | Text cleansing | TRIM all text, upper-case SKU and status, lower-case email key | – |
| C2 | Type conversion | text money → `NUMERIC(10,2)`, text date → `TIMESTAMP` | – |

## 3. Approach

- **Independent oracle:** `LegacyOracle` reads the raw legacy rows and applies R1–R6 in Java. The tests compare the
  database against this oracle, never against the migration SQL itself.
- **Deterministic data:** the seeder uses a fixed Datafaker seed, so every run (H2 or PG) produces identical rows. Expected
  counts such as "3 duplicates" are stable.
- **Fingerprints:** SHA-256 over all rows (ordered by primary key) proves that data is byte-identical, not just the same count.
- **Mutation check:** the suite was run against a deliberately broken V4 (survivor chosen by `MAX(id)` instead of `MIN(id)`),
  and 3 checks failed as expected. Restoring V4 brought it back to green. This shows the tests can detect real defects.

## 4. Assumptions

- Legacy tables are read-only during migration and are kept afterwards for reconciliation and rollback.
- "Duplicate" means equal after `LOWER(TRIM(email))`.
- An order with any invalid line is quarantined as a whole. Partial orders would corrupt totals.

## 5. Environment

| Item | Value |
|---|---|
| DB (executed) | H2 2.3.232, `MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`, in-memory |
| DB (supported, unverified locally) | PostgreSQL 16 via `docker-compose.yml` or a CI service container |
| Runtime | OpenJDK 26 (compiled to release 21), Maven 3.9.9 wrapper |
| Libraries | Flyway 10.22.0, TestNG 7.11.0, AssertJ 3.27.3, Datafaker 2.4.2 |

## 6. Entry criteria

- Migration scripts are reviewed and committed.
- The DB is reachable with the configured credentials (from env/CI secrets).
- The module compiles.

## 7. Exit criteria

- 100% of the checks in `db-validation-report.md` pass.
- legacy = migrated + rejected for every table.
- Money totals reconcile to the cent.
- The rollback restores the V2 fingerprints exactly.
- Re-migration is idempotent.
- The suite has been executed on the production engine (PostgreSQL) at least once before release.

## 8. Risks

| Risk | Mitigation |
|---|---|
| H2 behaves differently from PostgreSQL | Portable SQL only; type-name normalisation in tests; run on PG in CI |
| Rule misunderstanding | Rules are tabulated above and implemented twice (SQL and oracle) |
| Destructive tests interfere | Separate schema per test area |
