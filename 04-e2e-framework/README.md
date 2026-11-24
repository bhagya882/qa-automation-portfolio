# 04 – End-to-End Test Automation Framework (Java · Selenium · TestNG · Allure)

A maintainable UI automation framework for the public practice shop **[SauceDemo](https://www.saucedemo.com)**
(built by Sauce Labs for automation practice).

| Concern | Where / how |
|---|---|
| Reusable driver, waits, config, listeners | [`qa-core`](../qa-core) (`DriverFactory`, `DriverManager` (ThreadLocal), `BasePage`, `Config`, `FailureEvidenceListener`, `RetryAnalyzer`) |
| Page Object Model | [`src/main/java/.../saucedemo/pages`](src/main/java/com/qaportfolio/saucedemo/pages) – `LoginPage`, `InventoryPage`, `ProductDetailsPage`, `CartPage`, `CheckoutInfoPage`, `CheckoutOverviewPage`, `CheckoutCompletePage` |
| Reusable components | [`components/Header`](src/main/java/com/qaportfolio/saucedemo/components/Header.java) (title, cart badge, burger menu) |
| Fluent navigation | `SauceDemo.open().loginAs(user).addToCart(..).header().openCart().checkout().fill(info).continueToOverview().finish()` |
| Test data management | [`testdata/users.json`](src/main/resources/testdata/users.json) fixture → `Users`; synthetic checkout data via Datafaker (`CheckoutInfo.random()`) |
| Environment config | `src/test/resources/config/default.properties` + `<env>.properties` (`-Denv=qa` default, `-Denv=ci`); any key can be overridden with `-Dkey=` or env var |
| Parallel execution | `testng.xml` → `parallel="methods" thread-count="4"`, parallel data providers; one browser per thread |
| Screenshots / page source / console log on failure | qa-core `FailureEvidenceListener` → `target/evidence/` + Allure attachments |
| Flaky-test handling | `RetryTransformer` registered in the suite; `-Dretry.count=1` in CI; every retry logged to `target/flaky-tests.txt`; `known-defect` tests are never retried |
| Reports | Allure (`@Epic/@Feature/@Story/@Severity/@Issue`, `@Step` on every page action) + TestNG/Surefire XML |
| CI | GitHub Actions workflow in `.github/` (repo root) runs this module headless on every PR |

## Prerequisites
* JDK 21+ (tested on JDK 26) – Maven is provided via the wrapper (`./mvnw`)
* Google Chrome (Selenium Manager downloads the matching chromedriver automatically)
* Optional: Allure CLI for a local HTML report (`brew install allure`) – or use `./mvnw allure:serve`

## How to run (from the repository root)
```bash
# first time: build shared module
./mvnw -q -pl qa-core install -DskipTests

# PR-safe regression suite (42 tests), headless, known product defects excluded
./mvnw -pl 04-e2e-framework test -Dheadless=true -DexcludedGroups=known-defect

# smoke only
./mvnw -pl 04-e2e-framework test -Dheadless=true -Dsuite=testng-smoke.xml

# reproduce the documented product defects (8 tests, EXPECTED to fail)
./mvnw -pl 04-e2e-framework test -Dheadless=true -Dsuite=testng-known-defects.xml -DtestFailureIgnore=true

# DevTools audit (console errors, failed requests, broken images) -> target/devtools-audit.md (used by project 01)
./mvnw -pl 04-e2e-framework test -Dheadless=true -Dsuite=testng-devtools.xml

# other browser / environment
./mvnw -pl 04-e2e-framework test -Dbrowser=firefox -Denv=ci

# Allure HTML report (results are aggregated in <repo>/target/allure-results)
allure serve target/allure-results
```

## Test inventory
| Class | Tests | Covers |
|---|---|---|
| `LoginTest` | 16 | data-driven valid users (5), locked-out user, 7 invalid/empty/injection credential rows, dismiss error, masked password, protected-page redirect |
| `InventoryTest` | 6 + 3 known-defect | catalogue content, distinct images, default + 3 sort orders |
| `ProductDetailsTest` | 5 + 1 known-defect | details match listing (data-driven), add/remove from details, back navigation |
| `CartTest` | 6 + 2 known-defect | add, remove (cart + listing), persistence across reload, empty cart, reset app state |
| `CheckoutTest` | 6 + 2 known-defect | full E2E with price/tax/total maths, 3 required-field validations, cancel paths |
| `MenuAndLogoutTest` | 3 | burger menu items, logout ends session, Back after logout |

## Documentation
* [Test plan](docs/test-plan.md)
* [Defect reports](docs/defects.md) (8 reproduced defects with screenshots in [docs/evidence](docs/evidence))
* [Execution summary](docs/execution-summary.md)

## Known limitations
* SauceDemo is a third-party public demo; content can change (e.g. a "Dynamic Catalog" menu item appeared in Nov 2026). Assertions avoid brittle exact lists where the site is expected to evolve.
* The demo has no back-end API we can use for setup/teardown, so state is reset by starting a fresh browser per test.
* `visual_user` prices are randomised on each load, so DEF-04-003 shows different values on every run (the defect itself is stable).
* Allure logs "Could not update test case" warnings because `Allure.parameter` is called in `@BeforeMethod` in qa-core; the report is still generated (tracked as a framework improvement).
* Credentials used are the public demo accounts printed on the SauceDemo login page – not secrets.
