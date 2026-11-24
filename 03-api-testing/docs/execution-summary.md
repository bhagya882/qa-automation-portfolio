# Test Execution Summary — Project 03

**Run date:** 2026-11-22 · **Target:** https://dummyjson.com · **Machine:** macOS 26, JDK 26.0.1, Node 20.19.6
All numbers are copied from real runs. Raw files are in [`evidence/`](evidence/).

## REST Assured + TestNG (`./mvnw -pl 03-api-testing test`)
| Run | Total | Passed | Failed | Blocked/Skipped | Not run |
|---|---|---|---|---|---|
| Full (all groups) | 66 | 53 | 13 | 0 | 0 |
| PR mode (`-DexcludedGroups=known-defect -Dretry.count=1`) | 53 | 53 | 0 | 0 | 12 methods excluded (known-defect) |

All 13 failures in the full run are `known-defect` tests that reproduce DEF-API-001 … DEF-API-011
(DEF-API-004 and DEF-API-010 have 2 invocations each). No unexpected failures and no retries were needed.

## Postman / Newman (`npm test`, `npm run test:defects`)
| Run | Iterations | Requests | Assertions | Failed assertions |
|---|---|---|---|---|
| Regression (Auth, Products, Carts, Users, Negative) | 1 | 32 | 137 | 0 |
| Data-driven (`postman/data/products.csv`) | 4 | 8 | 40 | 0 |
| Known defects folder (expected to fail) | 1 | 8 | 28 | 9 (all known defects reproduced) |

Average response time 455 ms (min 96, max 1418), within the 3000 ms SLA.

## Issues found while building the suite (test-side, fixed)
* **Cookie leakage in Newman:** login sets HttpOnly `accessToken` cookies, and Newman's cookie jar silently
  authenticated the "no token" request (200 instead of 401). The request now unsets the cookies in a pre-request
  script. REST Assured doesn't keep cookies, so it wasn't affected.
* **Wrong test data:** the search term "perfume" matches nothing (products use the word "fragrance"), so the
  CSV row was corrected.
* **JSON Schema draft:** REST Assured's validator implements draft-04 (boolean `exclusiveMinimum`), so the
  schemas were moved to draft-04.

## Defects
11 defects logged: 3 Critical, 3 Major, 4 Minor, 1 Trivial. See [defects/README.md](defects/README.md).

## Exit criteria
| Criterion | Status |
|---|---|
| 100% of non-known-defect tests pass (Newman + REST Assured) | ✅ Met |
| Every known-defect failure linked to a defect report | ✅ Met |
| Critical defects risk-accepted | ✅ Accepted: third-party demo API, no production use |
| Reports archived | ✅ `docs/evidence/` + CI artifacts |
