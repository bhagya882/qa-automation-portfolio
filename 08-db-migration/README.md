# 08 – Database Integrity and Data Migration Validation (Java)

This module validates a migration from a loosely typed **legacy e-commerce schema** to a **normalised,
constrained schema**. The tooling is Flyway, JDBC and TestNG. Every expected value comes from an independent Java
oracle, not from the SQL under test.

| Area | Tooling |
|---|---|
| Migrations | Flyway 10 (SQL V1, V3–V5; Java V2 seeder) |
| Undo / rollback | Hand-written `db/undo/U5..U3` scripts (Flyway Community has no `undo`), verified by `RollbackTest` |
| Test data | Datafaker with fixed seed `20261018`: deterministic, synthetic, `.test` email domain only |
| Databases | H2 2.3 in PostgreSQL mode (default, in-memory) · PostgreSQL 16 (`docker-compose.yml` / CI service) |
| Tests / report | TestNG 7 + AssertJ → `target/db-validation-report.md` (every check with expected vs actual) |

## The scenario

The migration runs in these steps:

1. **V1** creates the legacy tables `legacy_customers`, `legacy_products`, `legacy_orders` and `legacy_order_items`. Money and dates
   are stored as text, emails are copied into orders, and there are no FKs or constraints.
2. **V2** is a Java seeder. It inserts 45 customers, 20 products, 120 orders, and 298 order items. The data is
   deliberately dirty:
   - 3 duplicate emails that differ only in case or whitespace
   - 2 NULL or blank emails
   - 3 orders with a quantity ≤ 0
   - 4 orphan order items
   - money values padded with whitespace
   - status values in mixed case
3. **V3** creates the normalised schema with `NOT NULL`, `CHECK`, `UNIQUE` (case-insensitive email via `email_normalized`), FKs,
   `NUMERIC(10,2)` money and `TIMESTAMP` dates. It also creates a `legacy_customer_map` table and a `migration_rejects` quarantine table.
4. **V4** cleanses and migrates the data under rules R1–R6 (see [docs/test-plan.md](docs/test-plan.md#migration-rules)).
5. **V5** adds indexes and the `v_customer_order_totals` reporting view.

## How to run

```bash
# H2 (default) – runs anywhere, no Docker needed
./mvnw -pl 08-db-migration test

# PostgreSQL 16
docker compose -f 08-db-migration/docker-compose.yml up -d
DB_URL=jdbc:postgresql://localhost:5432/qa DB_USER=qa DB_PASSWORD=qa_local_only ./mvnw -pl 08-db-migration test
#   (or -Ddb.url=... -Ddb.user=... -Ddb.password=...; CI injects these from a service container / secrets)
```

The report is written to `08-db-migration/target/db-validation-report.md`. The latest committed copy is in
[docs/evidence/](docs/evidence/db-validation-report.md).

## Test classes

| Class | IDs | Checks |
|---|---|---|
| `SchemaValidationTest` | DB-01..03 | Column types, precision and nullability via `information_schema`; 14 constraint-violation inserts are rejected; positive control |
| `DataQualityTest` | DB-10..11 | Profiling of the legacy dirty data; zero duplicates, orphans and rule violations after migration; order total = Σ lines |
| `ReconciliationTest` | DB-20..26 | Row counts legacy = migrated + rejects; counts and reject reasons vs the oracle; per-customer and grand totals; record-level sample; duplicate merge |
| `IdempotencyTest` | DB-30 | A second `migrate` runs 0 migrations; data fingerprints are unchanged; `flyway validate` passes |
| `RollbackTest` | DB-40 | U5→U4→U3 restore the exact V2 tables and data (SHA-256 fingerprints); re-migration gives identical counts |
| `TransactionRollbackTest` | DB-41..42 | A JDBC batch failing on row 3, and a failing Flyway V6, leave no partial rows |

Each area uses its own schema (`mig_main`, `mig_rollback`, `mig_txn`), so destructive tests can't affect the
read-only validations. Re-runs on PostgreSQL are repeatable, because each run cleans its schemas first.

## Documentation

- [Test plan](docs/test-plan.md)
- [Test cases](docs/test-cases.md)
- [Data defects found in the legacy data](docs/defects.md)
- [Rollback runbook](docs/rollback-runbook.md)
- [Execution summary](docs/execution-summary.md)

## Known limitations

- **PostgreSQL has not been verified on this machine**, because Docker isn't installed. Only H2 (PostgreSQL mode) has been executed. The SQL
  is written to be portable, and the H2-vs-PG differences (type names, failed-migration history rows) are handled in
  the tests. Run it against `postgres:16` in CI before relying on it.
- H2 commits DDL implicitly. Transactional-rollback tests therefore cover DML only. On PostgreSQL, DDL would also roll back.
- `Db.runScript` splits scripts on `;`, so undo scripts must not contain semicolons inside string literals.
- The data volume is small (~500 rows), so this suite tests correctness, not migration performance.
