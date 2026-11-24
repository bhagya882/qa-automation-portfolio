# Test Plan — Project 10: Continuous Quality Gate and Test Reporting Platform

## Objective
Build a CI pipeline that runs unit, API, UI and selected security checks on every pull request, publishes
reports and artifacts, fails the build on agreed thresholds, and documents flaky-test handling and release
criteria.

## Scope
| In scope | Out of scope |
|---|---|
| The `QualityGate` CLI (parsing, aggregation, thresholds, reporting) | Content of the other modules' suites, which their own plans cover |
| Workflow design: jobs, artifacts, gate, Pages publishing | Self-hosted runners, paid services (BrowserStack, ReportPortal server) |
| Policies: thresholds, flaky tests, release criteria, branch protection | |

## Assumptions
* GitHub-hosted `ubuntu-latest` runners with Chrome and Firefox preinstalled, and `macos-latest` for Safari nightly.
* Modules follow the repo conventions: TestNG results in `<module>/target/surefire-reports/testng-results.xml`,
  Newman JUnit in `03-api-testing/target/newman/`, and retries recorded in `<module>/target/flaky-tests.txt`.
* GitHub Pages is enabled with "GitHub Actions" as the source.

## Environment
Local: macOS 26, JDK 26.0.1 (release 21 bytecode), Maven wrapper 3.9.9, Node 20.19.6.
CI: Temurin 21, Node 20, `postgres:16` service container.

## Test approach
| Level | What | How |
|---|---|---|
| Unit | Gate parser, aggregation, thresholds, flaky handling, CI artifact layout, JSON output, XXE rejection, token redaction | 12 TestNG tests over **real** fixture files: a TestNG 7.11 run with pass, fail, skip and retry, and real Newman JUnit output from project 03 |
| Integration (local) | Gate over real 03 + 10 results; gate over a simulated CI artifact tree built with `stage-results.sh` | `java -jar …` with exit codes checked |
| Static | Workflow syntax and schema | `@action-validator/cli` (GitHub workflow JSON schema), `bash -n` |
| CI (pending) | Real pipeline run | Only after the repository is pushed to GitHub |

## Entry criteria
Gate module compiles. At least one module has produced results.

## Exit criteria
* All gate unit tests pass.
* The gate exits 0 on a green result set and 1 on a set with failures (both verified with real data).
* Workflows pass schema validation.
* First real GitHub Actions run green, with the dashboard published. This is **pending until push**.
