# Test Execution Summary — Project 10

**Date:** 2026-11-22 · **Machine:** macOS 26, JDK 26.0.1, Maven 3.9.9 (wrapper), Node 20.19.6

## Unit tests (`./mvnw -pl 10-quality-gate verify`)
| Total | Passed | Failed | Blocked/Skipped | Not run |
|---|---|---|---|---|
| 12 | 12 | 0 | 0 | 0 |

## Integration runs of the gate (real data)
| Run | Input | Exit code | Result |
|---|---|---|---|
| PR mode | 03 (REST Assured 53 + Newman 147 assertions) + 10 (12) | 0 | PASSED, 212/212 ([summary](evidence/summary-pr-mode-pass.md), [json](evidence/summary-pr-mode-pass.json)) |
| Known defects included | 03 full run + Newman "Known defects" folder + 10 | 1 | FAILED: 22 failures, 91.3% pass rate ([summary](evidence/summary-known-defects-blocked.md)) |
| Simulated CI artifacts | `stage-results.sh` output, one folder per artifact | 0 | 2 modules detected, Allure results merged |

The gate counts each Newman *assertion* as a test case: 137 regression + 10 distinct data-driven assertions.
Newman's JUnit reporter merges iterations.

## Static validation
| Check | Result |
|---|---|
| `npx @action-validator/cli` on `pr-quality-gate.yml`, `nightly.yml` | 0 errors |
| YAML parse of `ISSUE_TEMPLATE/defect_report.yml` (js-yaml) | OK |
| `bash -n .github/scripts/stage-results.sh` | OK |

## Not yet verified
* **The GitHub Actions workflows have not run yet**, because the repository isn't pushed. Schema validation
  catches syntax and structure errors, not runtime problems (runner images, browser versions, the emulator,
  other modules' commands). The first push to GitHub is the real test.
* The GitHub Pages deployment (live dashboard URL) depends on Pages being enabled in the repository settings.

## Defects
4 pipeline defects found and fixed. See [defects.md](defects.md).
