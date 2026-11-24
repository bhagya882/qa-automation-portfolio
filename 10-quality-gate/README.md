# Project 10 — Continuous Quality Gate and Test Reporting Platform

A GitHub Actions pipeline that runs unit, API, UI, accessibility, database, security and performance checks on
every pull request. It publishes the reports and fails the build on agreed thresholds. The thresholds are
enforced by a small Java CLI, `QualityGate`, so there is one place that decides "is this mergeable?".

| Piece | Location |
|---|---|
| PR pipeline (required check: **Quality gate**) | [`.github/workflows/pr-quality-gate.yml`](../.github/workflows/pr-quality-gate.yml) |
| Nightly pipeline (matrix, known defects, full perf, dependency scan, mobile) | [`.github/workflows/nightly.yml`](../.github/workflows/nightly.yml) |
| Result staging for artifacts | [`.github/scripts/stage-results.sh`](../.github/scripts/stage-results.sh) |
| Gate CLI | `src/main/java/com/qaportfolio/gate/` (`QualityGate`, `ResultParser`, `ModuleResult`) |
| Thresholds | [`quality-gate.properties`](quality-gate.properties) |
| Defect issue form / PR checklist | [`.github/ISSUE_TEMPLATE/defect_report.yml`](../.github/ISSUE_TEMPLATE/defect_report.yml), [`.github/pull_request_template.md`](../.github/pull_request_template.md) |
| Live dashboard | Allure report + gate summary on GitHub Pages, `https://<owner>.github.io/<repo>/` after the first push to `main` |

## What the gate does
1. Finds `<module>/target/surefire-reports/testng-results.xml` and `<module>/target/newman/*.xml` (JUnit)
   anywhere under `--root`. In CI that's the downloaded artifacts; locally it's the repo.
2. Aggregates passed / failed / skipped (blocked) / retried per module. Results of one module from several jobs
   (e.g. chrome + firefox) are summed.
3. Reads `<module>/target/flaky-tests.txt` (written by `qa-core` `RetryAnalyzer`) and lists the flaky tests.
4. Applies the thresholds: overall pass rate, failures per module, skipped share, flaky count, and required modules
   present.
5. Writes `target/quality-gate/summary.md` + `summary.json`, appends the markdown to `$GITHUB_STEP_SUMMARY`,
   and **exits 1 on any breach** (2 on bad arguments).

It has no runtime dependencies (plain `java -jar`). XML parsing refuses DOCTYPEs (XXE-safe), and JWTs in failure
messages are redacted before anything is printed.

## How to run
```bash
# unit tests of the gate
./mvnw -pl 10-quality-gate -am verify

# evaluate whatever results exist in the repo
./mvnw -q -pl 10-quality-gate exec:java -Dexec.args="--root ."

# jar, with overrides (e.g. only the modules you ran)
./mvnw -q -pl 10-quality-gate -am package -DskipTests
java -jar 10-quality-gate/target/10-quality-gate-1.0.0.jar --root . \
     --set gate.requiredModules=03-api-testing,10-quality-gate --set gate.maxFlaky=0
```
Options: `--root <dir>`, `--config <properties>` (default `<root>/10-quality-gate/quality-gate.properties`),
`--out <dir>` (default `<root>/target/quality-gate`), `--set key=value` (repeatable).

Example output (real run, see [evidence](docs/evidence/summary-pr-mode-pass.md)):
```
## ✅ Quality gate PASSED
| Module          | Passed | Failed | Blocked/Skipped | Retried | Pass rate | Status |
| 03-api-testing  |    200 |      0 |               0 |       0 |    100.0% | ✅     |
| 10-quality-gate |     12 |      0 |               0 |       0 |    100.0% | ✅     |
```

## GitHub setup (one-time)
1. Push the repository and open a PR to trigger `pr-quality-gate.yml`.
2. *Settings → Pages → Source: GitHub Actions* to publish the dashboard from `main`.
3. *Settings → Branches*: require the **Quality gate** check ([steps](docs/release-criteria.md#4-branch-protection-setup-github)).
4. *Settings → Secrets → Actions*: `NVD_API_KEY` (free) for the nightly dependency scan.

## Docs
* [CI pipeline design](docs/ci-pipeline.md)
* [Thresholds, release / exit criteria, branch protection](docs/release-criteria.md)
* [Flaky-test handling policy](docs/flaky-test-policy.md)
* [Test plan](docs/test-plan.md) · [Test cases](docs/test-cases.md) · [Defects](docs/defects.md) · [Execution summary](docs/execution-summary.md)

## Known limitations
* **The workflows are schema-validated but haven't run on GitHub yet** (nothing pushed). Expect the first run to
  need small fixes, e.g. runner image changes, or module commands that other projects change.
* ReportPortal isn't used. Allure on GitHub Pages is the dashboard, so there's no server to host.
* The Allure report has no history trend: Pages deployments don't keep previous reports. Adding a `gh-pages`
  branch with `allure-history` would enable it.
* Mobile (09) runs nightly only, on an emulator, as non-blocking.
