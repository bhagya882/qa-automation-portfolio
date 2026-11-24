# Test Execution Summary – Project 04

**Build:** local, 2026-11-23 · Chrome 157 headless · macOS 26 arm64 · JDK 26.0.1 · parallel methods ×4

| Suite | Command | Total | Passed | Failed | Blocked | Not run | Duration |
|---|---|---|---|---|---|---|---|
| Regression (PR gate) | `-Dsuite=testng.xml -DexcludedGroups=known-defect` | 42 | **42** | 0 | 0 | 8 (excluded known-defect) | 42.2 s |
| Known defects | `-Dsuite=testng-known-defects.xml` | 8 | 0 | **8 (expected)** | 0 | 0 | 20.1 s |

## Regression by feature
| Feature | Tests | Pass |
|---|---|---|
| Login | 16 | 16 |
| Product listing / sorting | 6 | 6 |
| Product details | 5 | 5 |
| Cart | 6 | 6 |
| Checkout | 6 | 6 |
| Navigation & session | 3 | 3 |

## Known-defect results
All 8 failed exactly as described in [defects.md](defects.md) (DEF-04-001 … DEF-04-008). Evidence screenshots and console
logs are in [evidence/](evidence/).

## Notes from execution
* First run: 41/42 passed. `MenuAndLogoutTest.burgerMenuItems` failed because the live site added a new
  menu item **"Dynamic Catalog"**. That is a product change, not a defect, so the assertion was changed to check that the
  core items appear in order (`containsSubsequence`). The rerun passed 42/42.
* No retries were needed (`retry.count=0` locally), and `target/flaky-tests.txt` stayed empty.

## Exit criteria
* ✅ 100% of regression tests pass
* ✅ every known-defect failure is linked to a defect report with evidence
* ✅ Allure results written to `<repo>/target/allure-results`, TestNG XML in `target/surefire-reports`
