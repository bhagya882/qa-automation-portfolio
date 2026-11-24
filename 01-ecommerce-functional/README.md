# 01 – E-Commerce Website Functional Testing Suite

Manual and scripted functional test design for an e-commerce shop: registration, login, search, product details, cart,
checkout and order history. The cases are written as test design artefacts (scenarios, cases, defects). Where possible they
were **executed by the automated suite of [project 04](../04-e2e-framework) or by scripted HTTP/DevTools checks**,
and each case records how it was executed.

## Systems under test (public practice sites only)
| Site | Why | Covers |
|---|---|---|
| [SauceDemo](https://www.saucedemo.com) | Sauce Labs demo shop built for test practice; documents its own seeded-defect users | login, sorting, product details, cart, checkout, order confirmation |
| [AutomationExercise](https://automationexercise.com) | Practice site that publishes test cases and an API list for automation practice | registration, search, product details, cart, checkout entry, API checks |

SauceDemo has no registration, search or order history, so those areas use AutomationExercise. **Neither** site has an
order-history page, so that case is marked Blocked and is covered at API level in project 03.

## Contents
| File | What |
|---|---|
| [docs/test-plan.md](docs/test-plan.md) | Scope, assumptions, environment, approach, entry/exit criteria, risks |
| [docs/test-scenarios.md](docs/test-scenarios.md) | High-level scenarios per module (traceability to cases) |
| [docs/test-cases.csv](docs/test-cases.csv) | **60 test cases**: ID, module, target site, title, preconditions, steps, data, expected, actual, status, linked defect, execution method. Opens directly in Excel / Google Sheets (File → Import) |
| [docs/defects.md](docs/defects.md) | 10 defect reports with severity/priority, steps, expected/actual, environment and evidence |
| [docs/defect-template.md](docs/defect-template.md) | Jira / GitHub Issues defect template (+ [GitHub issue form YAML](docs/github-issue-form-defect.yml)) |
| [docs/devtools-checklist.md](docs/devtools-checklist.md) | Browser DevTools checklist (Console, Network, Elements, Application) and how it was automated |
| [docs/execution-summary.md](docs/execution-summary.md) | Passed / failed / blocked / not-run per module |
| [docs/evidence/](docs/evidence) | DevTools audit report, HTTP check log, screenshots (SauceDemo screenshots are in [04 evidence](../04-e2e-framework/docs/evidence)) |

## How to reproduce the results
```bash
# automated SauceDemo cases (from repo root)
./mvnw -q -pl qa-core install -DskipTests
./mvnw -pl 04-e2e-framework test -Dheadless=true -DexcludedGroups=known-defect                       # TC-LOG/PD/CART/CO passes
./mvnw -pl 04-e2e-framework test -Dheadless=true -Dsuite=testng-known-defects.xml -DtestFailureIgnore=true  # failing cases

# DevTools audit (TC-DT-*) -> 04-e2e-framework/target/devtools-audit.md
./mvnw -pl 04-e2e-framework test -Dheadless=true -Dsuite=testng-devtools.xml
```
The AutomationExercise HTTP checks are listed command by command in [docs/evidence/automationexercise-http-checks.txt](docs/evidence/automationexercise-http-checks.txt).
Rerun them with the same `curl` commands.

## Tools
Test design in CSV/Google Sheets · GitHub Issues/Jira defect template · Chrome DevTools (Console, Network, Elements) ·
Selenium-driven DevTools audit · curl.

## Known limitations
* Account creation, UI login and order placement on AutomationExercise were **not executed**, because they would create real data on a
  third-party site. They are marked Not Run and are meant to be run manually with a synthetic account that is deleted afterwards.
* "Actual" results come from the 2026-11-23 run. Both sites are public and can change.
* Credentials used are the public demo accounts printed on SauceDemo's login page. There is no real personal data anywhere.
