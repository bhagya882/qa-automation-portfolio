# Browser DevTools Checklist

Run this on every page under test (Chrome DevTools, F12 / ⌥⌘I). Record findings as test-case actual results or defects.

| # | Panel | Check | Pass criterion | Automated? |
|---|---|---|---|---|
| 1 | Console | No errors or uncaught exceptions on load | 0 errors (warnings reviewed) | ✅ `DevToolsHealthCheckTest`, plus console capture on every failed test (qa-core listener) |
| 2 | Console | No errors while doing the action (add to cart, submit form…) | 0 errors | ✅ console log attached to failed tests |
| 3 | Console | No mixed-content warnings (http resources on an https page) | none | ✅ (found DEF-01-008) |
| 4 | Network | No responses ≥ 400 (enable "Preserve log", filter by status) | none | ✅ Resource Timing `responseStatus` |
| 5 | Network | API calls return correct status codes (not 200 for errors) | codes match semantics | ✅ curl checks (found DEF-01-006) |
| 6 | Network | Reasonable payload sizes / no duplicate calls | reviewer judgement | manual |
| 7 | Elements | Images load (no broken image icon) | `naturalWidth > 0` | ✅ |
| 8 | Elements | Form inputs have labels/`name`, `required` and the correct `type` (email, password) | yes | partly (TC-REG-01, TC-LOG-09) |
| 9 | Elements | Stable test hooks (`data-test`, `data-qa`) exist | yes | used by all locators |
| 10 | Application | Session/cart storage: what is stored in cookies/localStorage; nothing sensitive in clear text | no passwords/PII | manual (not executed in this cycle) |
| 11 | Application | Logout clears the session | cookie removed | ✅ TC-LOG-11 |
| 12 | Lighthouse / Device toolbar | Responsive layout at phone/tablet sizes | no horizontal scroll | ✅ project 02 |

## Automated run (2026-11-23)
Command: `./mvnw -pl 04-e2e-framework test -Dheadless=true -Dsuite=testng-devtools.xml`. 7 pages were audited and the
report was written to [evidence/devtools-audit-2026-11-23.md](evidence/devtools-audit-2026-11-23.md).

| Page | Console errors | Failed requests | Broken images |
|---|---|---|---|
| SauceDemo login | 0 | 0 | 0 |
| SauceDemo inventory (standard_user) | 0 | 0 | 0 |
| SauceDemo inventory (problem_user) | 0 | 0 | 0 * |
| SauceDemo inventory (error_user) | 0 | 0 | 0 |
| AutomationExercise home | **3** (mixed content) | 0 | 0 |
| AutomationExercise products | **3** (mixed content) | 0 | 0 |
| AutomationExercise product_details/1 | **3** (mixed content) | 0 | 0 |

\* problem_user's "wrong" images do load (they show a 404-dog picture), so they are not technically broken. The wrong-image
defect is caught by the content check instead (DEF-04-001).
error_user's console errors only appear on interaction (DEF-01-005), which shows why checklist item 2 is needed.
