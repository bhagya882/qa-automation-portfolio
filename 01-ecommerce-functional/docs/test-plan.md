# Test Plan – Project 01: E-Commerce Website Functional Testing

## 1. Introduction
Functional verification of the end-user shopping workflow: registration, login, search, product details, cart,
checkout and order history.

## 2. Test items
| ID | Item | URL |
|---|---|---|
| SD | SauceDemo (Swag Labs) – practice shop by Sauce Labs | https://www.saucedemo.com |
| AE | AutomationExercise – practice shop with public test cases and API list | https://automationexercise.com |

## 3. Scope
| Module | In scope | Site |
|---|---|---|
| Registration | signup form validation, account creation, duplicate email, delete account | AE |
| Login | valid/invalid/locked/empty credentials, masking, access control, logout | SD (+ AE API) |
| Search | keyword, case-insensitivity, no results, API search, reflected-input encoding, sorting | AE (+ SD sort) |
| Product details | content matches listing, add/remove, navigation, invalid id | SD, AE |
| Cart | add, remove, persistence, empty state, reset | SD (+ AE empty cart) |
| Checkout | E2E with price/tax maths, required fields, cancel paths, access without login | SD, AE |
| Order history | confirmation, invoice, order list | SD, AE |
| DevTools hygiene | console errors, failed requests, broken images | SD, AE |

**Out of scope:** payments with real cards, performance, security scanning, accessibility audit, cross-browser
(projects 02, 05, 06, 07). Also out of scope: any action that creates data on the third-party site without cleaning it up.

## 4. Assumptions
* Both sites are intended for public test practice. Testing is limited to normal user behaviour: no load, no attacks, no scraping.
* SauceDemo users `problem_user`, `error_user` and `visual_user` intentionally contain defects. Reporting them is the point of the exercise.
* All test data is synthetic (`@example.test` emails, Datafaker names). Demo passwords are the public ones shown on the site.

## 5. Approach
* Design techniques: equivalence partitioning, boundary values, state transition (cart/login state), error guessing,
  calculation oracle for totals (8% tax).
* Execution: SauceDemo cases are automated in project 04 (Selenium/TestNG). AutomationExercise read-only cases were run as
  scripted HTTP checks (curl). Console/network/image hygiene was checked by a Selenium-driven DevTools audit. Cases that would
  write data to AE are Not Run.
* Defects are logged with the [defect template](defect-template.md). Severity and priority follow the definitions in that template.

## 6. Test environment
| Component | Value |
|---|---|
| OS | macOS 26 (Darwin 25.6) arm64 |
| Browser | Google Chrome 157.0.8103.79 (headless for automation) |
| Automation | Java 26 runtime / Selenium 4.50 / TestNG 7.11 |
| HTTP checks | curl 8.7.1 |
| Test management | CSV → Google Sheets/Excel; defects as Markdown (Jira/GitHub Issues format) |

## 7. Entry criteria
Test items reachable (HTTP 200 on home pages, verified 2026-11-23); test cases reviewed; environment ready.

## 8. Exit criteria
* 100% of in-scope cases executed, or marked Not Run/Blocked with a reason.
* No open Blocker/Critical defect in the `standard_user` happy path.
* All failed cases linked to a defect report with reproduction steps and evidence.
* Execution summary published.

## 9. Suspension criteria
Site unavailable for more than 15 minutes, or a Blocker on login that prevents all other modules.

## 10. Deliverables
Test plan, [scenarios](test-scenarios.md), [test cases](test-cases.csv), [defects](defects.md),
[execution summary](execution-summary.md), [evidence](evidence/).

## 11. Risks
| Risk | Impact | Mitigation |
|---|---|---|
| Public site changes | Cases become outdated | Record the run date; use `data-test`/`data-qa` attributes |
| Writing data on 3rd-party site | Etiquette/permission | Read-only checks only; write flows left for manual execution with cleanup |
| No order-history feature | Coverage gap | Mark Blocked; cover via the API project (sample-app `/api/orders`) |
