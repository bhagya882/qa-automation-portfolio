# QA Automation Portfolio (Java)

Ten Software Testing & QA project assignments (window 18.10.2026 – 18.11.2026) in one Maven multi-module repository. The automation is written in **Java 21** with Selenium 4, TestNG, REST Assured, Appium, axe-core, the JMeter Java DSL and Flyway, and every module is wired into one GitHub Actions quality gate.

| # | Project | Folder | Stack | Target |
|---|---|---|---|---|
| 01 | E-Commerce Functional Testing Suite | [01-ecommerce-functional](01-ecommerce-functional/) | Test design, CSV for Excel/Sheets, GitHub Issues template, DevTools | saucedemo.com, automationexercise.com |
| 02 | Cross-Browser & Responsive UI Testing | [02-cross-browser-ui](02-cross-browser-ui/) | Selenium, Chrome/Firefox/Edge/Safari, device emulation, BrowserStack-ready | saucedemo.com |
| 03 | REST API Testing & Automated Regression | [03-api-testing](03-api-testing/) | Postman + Newman, REST Assured, JSON Schema | dummyjson.com |
| 04 | End-to-End Test Automation Framework | [04-e2e-framework](04-e2e-framework/) + [qa-core](qa-core/) | Selenium, TestNG, Page Object Model, parallel runs, Allure | saucedemo.com |
| 05 | Performance & Load Testing | [05-performance](05-performance/) | JMeter (jmeter-java-dsl, .jmx exports), Prometheus, Grafana, Docker | [sample-app](sample-app/) (owned) |
| 06 | Security-Focused Web App QA | [06-security-qa](06-security-qa/) | REST Assured security suite, OWASP ZAP, dependency-check, Burp guide | sample-app (owned) |
| 07 | Accessibility & Inclusive UX Audit | [07-accessibility](07-accessibility/) | axe-core Selenium, keyboard automation, Lighthouse, WAVE, screen reader | saucedemo.com |
| 08 | Database Integrity & Migration Validation | [08-db-migration](08-db-migration/) | SQL, Flyway, JDBC, TestNG, H2 / PostgreSQL, Docker | local DB |
| 09 | Mobile App Quality Engineering | [09-mobile](09-mobile/) | Appium + UiAutomator2, Android emulator, XCUITest/BrowserStack config, Charles Proxy | Sauce Labs *My Demo App* |
| 10 | Continuous Quality Gate & Reporting | [10-quality-gate](10-quality-gate/) + [.github/workflows](.github/workflows/) | GitHub Actions, Java gate CLI, Allure report on GitHub Pages | this repo |

Start with the **[master test plan](docs/master-test-plan.md)**, which covers scope, authorization policy, environments, entry and exit criteria, and severity/priority definitions. Each project folder then has its own `README.md` and `docs/` folder containing the test plan, test cases, defect reports, execution summary and evidence.

## Submission checklist → where to find it

| Required | Location |
|---|---|
| Test plan (scope, assumptions, environment, entry/exit) | `docs/master-test-plan.md` and `<project>/docs/test-plan*.md` |
| Test cases / automated scripts | `<project>/docs/test-cases*`, `01-…/docs/test-cases.csv`, `src/test/java`, Postman collection |
| Defect reports (severity/priority, steps, evidence, environment) | `<project>/docs/defects*`, `06-…/docs/security-findings.md`, `.github/ISSUE_TEMPLATE/` |
| Execution summary (passed / failed / blocked / not run) | `<project>/docs/execution-summary.md` |
| Evidence (reports, screenshots, logs) | `<project>/docs/evidence/` |
| Live test dashboard | Allure report on GitHub Pages, published by `pr-quality-gate.yml` from `main` |

## Prerequisites

- JDK 21+. Maven is **not** needed because the repo ships the Maven wrapper (`./mvnw`).
- Google Chrome. Selenium Manager downloads drivers, and Firefox/Edge if they are missing.
- Node 20+ only for Newman (03) and Lighthouse (07).
- Optional: Docker (Prometheus/Grafana, ZAP, PostgreSQL), and Android SDK + Appium 2/3 (09).

> **Corporate network note:** if HTTPS calls fail with `PKIX path building failed`, your proxy re-signs TLS. On macOS, add `-Djavax.net.ssl.trustStoreType=KeychainStore` to Maven commands and set `NODE_EXTRA_CA_CERTS` for Newman. GitHub runners don't need this.

## Run

```bash
# Build everything (no tests)
./mvnw install -DskipTests

# The PR quality gate locally: every suite except known-defect tests.
# 09-mobile is left out because it needs a running emulator and Appium (see 09-mobile/README.md)
./mvnw install -pl '!09-mobile' -Dheadless=true -Dretry.count=1 -DexcludedGroups=known-defect,quarantine -DtestFailureIgnore=true

# Aggregate the results and apply the thresholds (exit code 1 = gate failed)
./mvnw -q -pl 10-quality-gate exec:java -Dexec.args="--root ."

# Single project, e.g. the E2E framework in Firefox
./mvnw -pl 04-e2e-framework -am verify -Dbrowser=firefox -Dheadless=true

# Allure report from all modules' results
npx allure-commandline@2.32.0 serve target/allure-results
```

Project-specific commands (browser matrix, perf profiles, the insecure security lab, PostgreSQL, the emulator) are in each project's README.

## Conventions

- **`known-defect` group:** tests that reproduce real, documented defects in the system under test. They are excluded from the PR gate and run nightly as evidence. A known-defect test that starts passing means the defect was fixed.
- **Blocked = skipped:** when infrastructure is missing (no Safari driver, no Appium server, no Docker), tests throw `SkipException("BLOCKED: …")` instead of passing silently.
- **Failure evidence:** a screenshot, page source and console log go to `target/evidence/` and are attached to Allure.
- **Flaky handling:** one retry in CI, every retry is logged to `target/flaky-tests.txt`, and the gate reports flaky tests. See [the flaky-test policy](10-quality-gate/docs/flaky-test-policy.md).
- **Config:** `-Dkey=value`, then the `KEY` environment variable, then `config/<env>.properties`, then `config/default.properties`. Secrets come only from environment variables or GitHub Secrets.

## Security and permission

- UI and API tests target public sites built for automation practice, at low volume.
- **Load and security testing runs only against `sample-app`**, a demo-store API owned by this repo. It has a secure mode and a deliberately vulnerable `--insecure` lab mode.
- No real user data, passwords or tokens are committed. The accounts used are the public demo accounts printed on the practice sites, or local-only defaults of sample-app.

## Known limitations

- PostgreSQL (08), ZAP (06), the Grafana stack (05) and the NVD dependency scan were configured but **not executed locally** (no Docker or NVD key on the authoring machine). They run in CI.
- Safari was blocked locally (needs `sudo safaridriver --enable`). In CI it runs on the nightly macOS job.
- iOS (XCUITest) and BrowserStack are configured but not executed (they need a macOS simulator or an account).
- The public practice sites can change without notice. Assertions use stable `data-test` attributes, but content changes can still break tests.
