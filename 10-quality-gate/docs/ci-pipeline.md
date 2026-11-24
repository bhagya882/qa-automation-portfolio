# CI Pipeline Design

Two workflows in `.github/workflows/`:

| Workflow | Trigger | Purpose | Blocks merge? |
|---|---|---|---|
| `pr-quality-gate.yml` | every pull request, push to `main`, manual | Fast, deterministic checks on each change, plus publishing the test dashboard | **Yes**: the `Quality gate` job is the required status check |
| `nightly.yml` | 02:00 UTC daily, manual | Slow, infrastructure-heavy or expected-to-fail suites | No (informational, triaged next morning) |

## PR pipeline

```
                 ┌───────── unit (10-quality-gate)
                 ├───────── api  (03: REST Assured + Newman)
                 ├───────── ui   (02, 04) × (chrome, firefox)   ← matrix, 4 jobs
 build ──needs──►├───────── a11y (07: axe-core)
 (compile all)   ├───────── db   (08 + postgres:16 service container)
                 ├───────── security (06 tests + ZAP baseline vs sample-app)
                 └───────── perf (05, smoke profile)
                                  │ each uploads artifact results-<job>
                                  ▼
                           quality-gate ──► publish-report (main only)
                     (aggregate + thresholds)   (Allure → GitHub Pages)
```

**Design decisions**
* **Tests never fail their own job.** Every test job runs with `-DtestFailureIgnore=true`, so it always
  produces and uploads results. The pass/fail decision lives in one place, the `quality-gate` job, and is driven
  by reviewed thresholds in `quality-gate.properties`. That gives one red check with an explanation, instead of
  many red jobs.
* **Missing results are a failure.** `gate.requiredModules` lists every module that must report. A job that
  crashed before writing results can't produce a false green.
* **Consistent artifact layout.** `.github/scripts/stage-results.sh <module>` copies `<module>/target/{surefire-reports,
  newman, evidence, screenshots, zap, …}` and `target/allure-results` into `ci-results/`, minus build outputs.
  The gate job downloads every `results-*` artifact into its own folder. The gate takes the module name from the
  folder directly above `target/`, so the chrome and firefox runs of the same module are summed.
* **PR-safe defaults** in `TEST_FLAGS`: headless browsers, `-Dretry.count=1` (see the [flaky-test policy](flaky-test-policy.md)),
  `-DexcludedGroups=known-defect,quarantine`.
* **Owned targets only.** Security scanning (ZAP) and load (perf) run against `sample-app`, which the pipeline
  itself starts. UI and API jobs send low-volume functional traffic to public sites built for test practice.
* **No stored credentials.** The PostgreSQL service uses `trust` auth inside the throw-away container. Demo
  logins are the ones the practice sites publish. `NVD_API_KEY` is a repository secret. The gate redacts JWTs from
  failure messages before writing the job summary.
* **Dashboard.** `quality-gate` merges all Allure results, generates the report with `allure-commandline`, and
  adds the gate's `summary.md`/`summary.json` under `/quality-gate/`. On `main`, `publish-report` deploys it with
  `actions/deploy-pages`. The URL is shown on the job and under *Settings → Pages*
  (`https://<owner>.github.io/<repo>/`). It deploys even when the gate fails, so the failure can be inspected.

## Nightly pipeline
| Job | What | Notes |
|---|---|---|
| `cross-browser-matrix` | 02 `testng-matrix.xml` on Ubuntu (Chrome, Firefox) and macOS (adds Safari) | `sudo safaridriver --enable` on macOS |
| `known-defects` | `-Dgroups=known-defect` for 02/03/04/07, plus the Newman "Known defects" folder | `continue-on-error`. A test that starts **passing** means the defect was fixed upstream: move it out of the group |
| `perf-full` | 05 with `-Dperf.profile=full` (load, stress, spike, endurance) | 90 min budget |
| `dependency-scan` | `06-security-qa/scripts/dependency-scan.sh` (OWASP Dependency-Check) | Needs the `NVD_API_KEY` secret. The NVD database is cached |
| `mobile` | 09 on an Android 14 emulator (`reactivecircus/android-emulator-runner`) with Appium 2 + UiAutomator2 | `continue-on-error`. Emulators on shared runners are slow |
| `nightly-summary` | Runs the gate in informational mode over the nightly artifacts | Never fails the workflow |

## Running the gate locally
```bash
./mvnw -pl 03-api-testing,10-quality-gate -am verify -DtestFailureIgnore=true   # produce some results
./mvnw -q -pl 10-quality-gate exec:java -Dexec.args="--root ."                 # evaluate (exit code 1 on breach)
# or, with the jar
java -jar 10-quality-gate/target/10-quality-gate-1.0.0.jar --root . --set gate.requiredModules=03-api-testing
```
Output goes to `target/quality-gate/summary.md` and `summary.json`, and is also appended to `$GITHUB_STEP_SUMMARY` in CI.
