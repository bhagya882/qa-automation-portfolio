# Test Cases – Database Migration (08)

Result = the H2 run on 2026-11-23 (see [evidence/db-validation-report.md](evidence/db-validation-report.md)).

| ID | Title | Data / preconditions | Steps | Expected | Result |
|---|---|---|---|---|---|
| DB-01 (×14) | Column types | Migrated `mig_main` | Query `information_schema.columns` | e.g. `products.price numeric(10,2) NOT NULL`, dates `timestamp` | Pass |
| DB-02 (×14) | Constraint enforcement | Migrated | Insert/delete a violating row in a txn, then roll back | Each statement rejected (NOT NULL, UNIQUE ci-email, CHECKs, FKs, NUMERIC overflow, FK restrict on delete) | Pass |
| DB-03 | Positive control | Migrated | Insert a valid customer, order and item | Accepted | Pass |
| DB-10 (×5) | Legacy profiling | Seeded V2 | Count the dirty rows | 3 dup emails, 2 null emails, 4 orphans, 3 qty ≤ 0, 4 padded prices | Pass |
| DB-11 (×11) | Post-migration integrity | Migrated | Duplicate / orphan / domain / total queries | 0 violations each | Pass |
| DB-20 (×4) | Count reconciliation | Migrated | legacy vs target + rejects | Equal (customers 45 = 40 + 5; orders 120 = 115 + 5; items 298 = 283 + 15; products 20 = 20) | Pass |
| DB-21 | Counts vs oracle | Migrated | Compare with `LegacyOracle` | Equal | Pass |
| DB-22 | Rejects by reason | Migrated | Group `migration_rejects` | Equal to oracle (3/2/4/11/2/3) | Pass |
| DB-23 | Per-customer totals | Migrated | View vs oracle, 38 customers | All equal (duplicates' orders merged) | Pass |
| DB-24 | Grand total | Migrated | Σ legacy = Σ migrated + Σ rejected | 50709.85 = 49662.15 + 1047.70 | Pass |
| DB-25 (×6) | Record-level sample | Orders 1, 7, 25, 50, 77, 120 | Compare email, date, status, total, line count | Identical after cleansing | Pass |
| DB-26 | Duplicate merge | Legacy customer 41 | Check map and orders 1, 6 | 41 → 1; both orders on customer 1 | Pass |
| DB-30 | Idempotency | Migrated | `migrate` again, then `validate` | 0 executed; fingerprints unchanged | Pass |
| DB-40 | Rollback | Fresh `mig_rollback` | V2 → snapshot → V5 → U5, U4, U3 → compare → re-migrate | V2 tables and fingerprints restored; version 2; re-migration identical | Pass |
| DB-41 | JDBC atomicity | `mig_txn` | Batch with an invalid 3rd row, then roll back | 0 rows left | Pass |
| DB-42 | Flyway atomicity | `mig_txn` | Apply the test-only V6 (2 valid rows + 1 invalid) | FlywayException; 0 rows left; version 5 after repair | Pass |
