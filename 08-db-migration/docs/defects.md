# Data Defects Found in the Legacy Data (08)

The validation suite profiles the legacy data before migrating it. The issues below are real data-quality defects
**in the source data**. The migration handles each one by rule, and every affected row is kept in
`migration_rejects`, so business owners can review it.

Environment: H2 2.3.232 (PostgreSQL mode); seed 20261018; run on 2026-11-23.

| ID | Severity / Priority | Defect | Rows | Detected by | Handling |
|---|---|---|---|---|---|
| DQ-01 | High / P1 | Duplicate customers whose emails differ only in case or whitespace (e.g. id 41 = id 1 upper-cased; id 42 has leading/trailing spaces) | 3 | DB-10 | R2: merged into the lowest id; orders re-pointed (DB-26) |
| DQ-02 | High / P1 | Customers with NULL or blank email (ids 44, 45), each with an order (ids 4, 5) | 2 customers, 2 orders, their lines | DB-10, DB-22 | R1/R3: quarantined. **Needs a business decision**: contact the customer or purge |
| DQ-03 | High / P2 | Orders containing lines with quantity ≤ 0 (orders 30, 60, 90). Their legacy totals are therefore understated | 3 orders | DB-10 | R4: whole order quarantined; totals reconciled separately (DB-24) |
| DQ-04 | Medium / P2 | Order items referencing non-existent orders 90000–90003 | 4 | DB-10 | R5: quarantined |
| DQ-05 | Low / P3 | Money and SKU values with stray whitespace; lower-case SKUs; mixed-case status (`paid`, `Shipped`, ` cancelled `) | 4 prices, 5 SKUs, most orders | DB-10, DB-25 | C1/C2: trimmed and normalised |

**Reproduction, for every row:**
1. Run `./mvnw -pl 08-db-migration test`.
2. In the report, see the "legacy profiling" and "rejects" sections.
3. To inspect the rows, query `SELECT * FROM mig_main.migration_rejects ORDER BY source_table, reason` (for example from a debugger session or
   on PostgreSQL with `psql`).

**Expected:** the source data satisfies the target constraints.
**Actual:** 5 categories of violations, as above. The quarantine total of €1,047.70 in order value
(DB-24: 50709.85 − 49662.15) must be signed off by Finance before cut-over.

No defects were found in the migration scripts themselves in the final run. The mutation check confirms that the suite
would detect a wrong survivor rule (3 failing checks).
