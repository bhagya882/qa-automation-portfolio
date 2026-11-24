# Quality Thresholds, Release Criteria and Branch Protection

## 1. Quality thresholds (enforced by the gate)
Source of truth: [`../quality-gate.properties`](../quality-gate.properties). Changes need a reviewed PR.

| Key | Default | Meaning | Why |
|---|---|---|---|
| `gate.minPassRate` | 100 | passed / (passed + failed) across all modules, in % | Known defects are excluded on PRs, so every remaining test must pass |
| `gate.maxFailuresPerModule` | 0 | Failed tests allowed per module (`gate.maxFailures.<module>=N` overrides) | Zero-tolerance default; any exception is explicit and reviewed |
| `gate.minPassRate.<module>` | not set | Optional per-module pass-rate floor | e.g. a newly added suite during ramp-up |
| `gate.maxSkippedPercent` | 20 | Skipped/blocked share per module | Many skips usually mean broken infrastructure (driver, DB, Appium) and would hide failures |
| `gate.maxFlaky` | 3 | Tests that passed only after retry | Flakiness must stay visible ([policy](flaky-test-policy.md)) |
| `gate.requiredModules` | 02, 03, 04, 05, 06, 07, 08, 10 | Modules that must report results | Prevents "green because nothing ran" |
| `gate.minTestsPerModule` | 1 | Executed tests per required module | Same reason |

Module-specific quality criteria are enforced *inside* the module tests, so they count as failures for the gate:

| Module | Criterion (enforced by its tests) |
|---|---|
| 03 API | Every response < 3000 ms (`api.maxResponseMs`); JSON schema contracts |
| 05 Performance | p95/p99 latency and error-rate thresholds per scenario (see 05 README) |
| 06 Security | No High-risk ZAP alert; required security headers; access-control tests |
| 07 Accessibility | No new axe-core critical/serious violations against the baseline |
| 08 Database | All integrity and reconciliation checks pass; rollback verified |

## 2. Entry criteria (to start a test cycle / merge window)
* `build` job green: all modules compile.
* Test environments reachable: smoke tests in 02/03/04 pass.
* Test data and configuration present. No secrets are needed in the repo.

## 3. Exit / release criteria
A build on `main` is **releasable** when all of these hold:

| # | Criterion | Evidence |
|---|---|---|
| R1 | `Quality gate` check green on the release commit | PR / main run |
| R2 | No open **Critical** or **Major / P1** product defect without a documented risk acceptance from the product owner | Defect register / issues |
| R3 | No tests in `quarantine` for the release-critical flows (login, checkout, payment, data migration) | `grep -r quarantine` + issues |
| R4 | Last nightly run reviewed: matrix, perf-full and dependency scan have no new High/Critical findings | Nightly summary |
| R5 | Performance within agreed thresholds in the latest full run, with no regression > 10% in p95 versus the previous release | 05 report |
| R6 | Test execution summary updated (passed / failed / blocked / not run) and published (Allure on Pages) | Pages URL |
| R7 | Known limitations and accepted risks listed in the release notes | Release PR |

**Not releasable:** any gate breach on the release commit, an unresolved Critical security finding, or data
migration reconciliation differences.

## 4. Branch protection setup (GitHub)
*Settings → Branches → Add branch ruleset* (or *Add rule*) for `main`:
1. **Require a pull request before merging**: 1 approval, dismiss stale approvals on new commits.
2. **Require status checks to pass**: add **`Quality gate`** (the job name in `pr-quality-gate.yml`), and
   optionally `Build & compile all modules`. Tick *Require branches to be up to date before merging*.
3. **Require conversation resolution before merging.**
4. **Block force pushes** and **restrict deletions**.
5. (Optional) **Require linear history** and **Require signed commits**.

Then *Settings → Pages → Build and deployment → Source: **GitHub Actions***, so `publish-report` can deploy the
dashboard. *Settings → Secrets and variables → Actions*: add `NVD_API_KEY` for the nightly dependency scan.

With the CLI (needs admin on the repo):
```bash
gh api -X PUT repos/<owner>/<repo>/branches/main/protection --input - <<'JSON'
{
  "required_status_checks": { "strict": true, "contexts": ["Quality gate"] },
  "enforce_admins": true,
  "required_pull_request_reviews": { "required_approving_review_count": 1, "dismiss_stale_reviews": true },
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false,
  "required_conversation_resolution": true
}
JSON
```
