# Execution Summary – Accessibility (07)

| Item | Value |
|---|---|
| Run date | 2026-11-23 |
| Build | `./mvnw -pl 07-accessibility test` (full) and `-DexcludedGroups=known-defect` (PR gate) |
| Environment | macOS 26 arm64 · Chrome 157 headless · Selenium 4.50.0 · axe-core 4.10 · TestNG 7.11 |
| Target | https://www.saucedemo.com (7 pages of the purchase journey) |

## Automated results (full run, all groups)

Source: [evidence/testng-results-full-run.xml](evidence/testng-results-full-run.xml)

| Total | Passed | Failed | Blocked/Skipped | Not run |
|---|---|---|---|---|
| 30 | 18 | 12 | 0 | 0 |

All 12 failures are in the `known-defect` group and reproduce documented defects:

| Failing test | Count | Defect |
|---|---|---|
| `zeroViolations` (one per page) | 7 | DEF-A11Y-05 |
| `loginFocusIndicatorVisible`, `inventoryFocusIndicatorVisible` | 2 | DEF-A11Y-01 |
| `focusKeptAfterAddToCart` | 1 | DEF-A11Y-02 |
| `pageTitlesAreDescriptive` | 1 | DEF-A11Y-03 |
| `formFieldsHaveVisibleLabels` | 1 | DEF-A11Y-04 |

## PR-gate run (`-DexcludedGroups=known-defect`)

| Total executed | Passed | Failed |
|---|---|---|
| 18 | 18 | 0 |

The build was green. There are no new axe violations compared with the baseline, and no keyboard regressions.

## Tool results

| Tool | Result |
|---|---|
| axe-core, WCAG 2.0/2.1/2.2 A+AA tags | **0 violations** on all 7 pages; `color-contrast` incomplete (needs review) on 6 pages |
| axe-core, best-practice | `page-has-heading-one` on 7 pages, `region` on login (baselined) |
| Lighthouse 12.2.1 (login page) | Accessibility score **100** ([report](evidence/lighthouse/www_saucedemo_com.report.html)) |
| Custom keyboard/semantic tests | 4 defects that axe and Lighthouse did not detect (focus visible, focus loss, titles, labels) |

## Manual

| Activity | Status |
|---|---|
| WCAG 2.2 checklist | Completed from automated evidence; 8 items still need manual execution |
| Screen-reader script (SR-01..12) | Prepared, not executed |
| WAVE review | Instructions prepared, not executed |

## Defects by severity

| Critical | High | Medium | Low |
|---|---|---|---|
| 0 | 1 | 3 | 1 |

## Conclusion

The journey is keyboard-operable, but it does **not** conform to WCAG 2.2 AA. The main issue is the missing focus
indicator (2.4.7), followed by focus loss, generic titles and placeholder-only labels. The tools reported a clean result
(0 axe A/AA violations, Lighthouse 100) while real barriers exist, which shows that behaviour-level tests and manual
review are needed.
