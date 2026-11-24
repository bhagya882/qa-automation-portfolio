# Master Test Plan: QA Automation Portfolio

| Item | Value |
|---|---|
| Programme | Software Testing & Quality Assurance – Project Assignments 01–10 |
| Planned window | 18.10.2026 – 18.11.2026 |
| Primary language | Java 21 (Maven multi-module), with JavaScript only where the tool demands it (Postman/Newman scripts) |
| Core stack | Selenium 4, TestNG 7, REST Assured 5, Allure 2, Appium java-client 9, axe-core Selenium, jmeter-java-dsl, Flyway, GitHub Actions |

Each project folder has its own detailed plan in `<project>/docs/`. This document defines what they all share.

## 1. Scope

| # | Project | System under test | Type |
|---|---|---|---|
| 01 | E-commerce functional suite | saucedemo.com, automationexercise.com | Manual test design and execution |
| 02 | Cross-browser and responsive UI | saucedemo.com | Selenium matrix (browser × viewport × device) |
| 03 | REST API and regression | dummyjson.com | Postman/Newman and REST Assured |
| 04 | E2E automation framework | saucedemo.com | Selenium + TestNG + POM + Allure |
| 05 | Performance and load | `sample-app` (owned) | JMeter (Java DSL), Prometheus/Grafana |
| 06 | Security-focused QA | `sample-app` (owned, with vulnerable lab mode) | REST Assured security tests, OWASP ZAP, dependency scan, Burp guide |
| 07 | Accessibility audit | saucedemo.com | axe-core, keyboard automation, Lighthouse, manual screen reader |
| 08 | DB integrity and migration | Local H2 / PostgreSQL | Flyway, JDBC, TestNG |
| 09 | Mobile quality | Sauce Labs *My Demo App* (Android) | Appium + UiAutomator2 |
| 10 | Continuous quality gate | This repository | GitHub Actions, Java quality-gate CLI, Allure on GitHub Pages |

**Out of scope:** testing any system without permission, production data, load or security testing of third-party sites, and native iOS execution (no macOS CI runner budget; config only).

## 2. Authorization and test-target policy

- **UI and functional:** saucedemo.com and automationexercise.com are public sites published specifically for test-automation practice. Tests run at human-like rates. No load or security scanning is done against them.
- **API:** dummyjson.com is a public fake REST API for testing and prototyping. Functional regression runs at low volume only.
- **Load and security:** run **only** against `sample-app`, which this repository owns and runs on localhost or in CI.
- **Mobile:** an open-source demo app installed on a local emulator.
- **Data:** synthetic only (Datafaker or fixtures). The credentials in this repo are either published public demo accounts of the practice sites or local-only defaults of `sample-app`. Real secrets (BrowserStack keys, NVD API key) are supplied through environment variables or GitHub Secrets and are never committed.

## 3. Assumptions

- Practice sites stay available and keep their current behaviour. Their intentionally buggy users (e.g. `problem_user`) are used as a source of real, reproducible defects.
- Java 21+ and network access to Maven Central are available. Docker is available in CI (GitHub-hosted Ubuntu runners) even where it is missing locally.
- Firefox, Edge and an Android emulator may not exist on every workstation. Tests that need missing infrastructure are reported as **Blocked** (TestNG *skipped*) rather than silently passing.

## 4. Test environment

| Layer | Local | CI (GitHub Actions) |
|---|---|---|
| OS | macOS (Apple Silicon) | ubuntu-latest |
| JDK | 21+ (verified on 26) | Temurin 21 |
| Browsers | Chrome, Safari | Chrome, Firefox (preinstalled on runner) |
| DB | H2 (PostgreSQL mode) | PostgreSQL 16 service container |
| Mobile | Android emulator + Appium 2 (if installed) | android-emulator-runner (nightly, non-blocking) |
| Security tooling | REST Assured security suite | + OWASP ZAP baseline (Docker), OWASP dependency-check |

## 5. Entry criteria

- Code compiles: `./mvnw -B install -DskipTests` succeeds.
- The target environment is reachable (health or ping check). Otherwise the suite reports Blocked.
- Test data and fixtures exist, and no real personal data is present.
- For load and security runs, the target is confirmed to be the owned `sample-app`.

## 6. Exit criteria (release criteria)

- 100 % pass rate and 0 failures per module across PR-gate suites. Tests in the `known-defect` group are excluded because they document open defects.
- No more than 20 % of a module's tests blocked (skipped), and every required module reports results.
- 0 failing tests in the `smoke`/critical-journey groups.
- No new accessibility violations beyond the recorded baseline, and 0 *critical* ones.
- Performance smoke: p95 and error-rate thresholds met for every non-bottleneck endpoint.
- Security suite all green in secure mode, ZAP baseline without FAIL-level alerts, no dependency with CVSS ≥ 7 unless waived.
- Every open defect has severity, priority, reproduction steps and evidence.
- Flaky tests (passed only on retry) are listed in the quality-gate summary and stay within the threshold.

Thresholds are configured in `10-quality-gate/quality-gate.properties`.

## 7. Defect management

- **Severity:** S1 Critical (blocks a core journey, data loss, security breach), S2 Major (core feature wrong, no workaround), S3 Minor (workaround exists, cosmetic-functional), S4 Trivial (cosmetic).
- **Priority:** P1 fix now, P2 next release, P3 backlog.
- **Template:** `.github/ISSUE_TEMPLATE/defect_report.yml`. Each project's `docs/defects` holds the filed examples.
- Automated tests that reproduce an open defect are tagged `known-defect`. They run nightly as evidence and do not block PRs. When the defect is fixed, the tag is removed.

## 8. Reporting

- **Per module:** TestNG/Surefire XML (`target/surefire-reports`), screenshots, page source and console logs on failure (`target/evidence`).
- **Aggregated:** Allure results (`target/allure-results` at the repo root). The report is published to GitHub Pages from `main`.
- **Gate:** `10-quality-gate` writes `summary.md` / `summary.json` and the GitHub job summary.
- **Execution summaries:** passed, failed, blocked and not-run counts, kept in each project's `docs/execution-summary.md`.

## 9. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Public practice site changes or goes down | Health checks before suites, results marked Blocked, stable `data-test` locators |
| Flaky UI timing | Explicit waits only, one retry in CI, flaky list in the gate summary, `quarantine` group |
| Rate limiting by public APIs | Low-volume functional tests only; load goes to the owned app |
| Missing local tooling (Docker, Firefox, emulator) | CI covers it; tests report Blocked with a reason |
