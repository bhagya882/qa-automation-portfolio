# Execution Summary – Database Migration (08)

| Item | Value |
|---|---|
| Date | 2026-11-23 |
| Command | `./mvnw -pl 08-db-migration clean test` |
| Database | H2 2.3.232 in PostgreSQL mode (in-memory) |
| Evidence | [db-validation-report.md](evidence/db-validation-report.md), [testng-results-h2.xml](evidence/testng-results-h2.xml) |

| Total test invocations | Passed | Failed | Blocked | Not run |
|---|---|---|---|---|
| 64 | 64 | 0 | 0 | 0 |

| Report checks | Passed | Failed |
|---|---|---|
| 76 | 76 | 0 |

| Area | Checks | Result |
|---|---|---|
| Data types | 14 | Pass |
| Constraints (negative + control) | 15 | Pass |
| Legacy profiling | 5 | 5 data defects confirmed (see [defects.md](defects.md)) |
| Integrity (duplicates, RI, rules, totals) | 11 | Pass, 0 violations |
| Reconciliation (counts, rejects, money, record level) | 15 | Pass |
| Idempotency | 3 | Pass |
| Rollback + re-migration | 6 | Pass |
| Transactions | 3 (+1 info) | Pass |

**Key numbers:**
- customers 45 → 40 migrated + 5 rejected
- orders 120 → 115 + 5
- items 298 → 283 + 15
- products 20 → 20
- Grand total: 50709.85 = 49662.15 migrated + 1047.70 quarantined

**Mutation check:** with V4's survivor rule changed to `MAX(id)`, 3 checks failed (DB-02 unique email, DB-26 ×2).
The suite detects real migration defects.

**Not run:** the PostgreSQL 16 target, because Docker isn't available on the test machine. This is **blocked** locally and must be
executed in CI or on a Docker host before release.
