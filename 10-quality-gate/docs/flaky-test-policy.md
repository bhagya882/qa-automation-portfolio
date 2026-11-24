# Flaky-Test Handling Policy

A **flaky test** gives different results on the same code: it fails, then passes on retry with nothing changed.
Flaky tests destroy trust in the pipeline ("just re-run it"), so we detect them, make them visible and fix
them. We don't hide them.

## 1. Detection: retry once, but record every retry
* `qa-core` `RetryAnalyzer` (registered through `RetryTransformer` in every suite XML) retries a failed test
  `retry.count` times. **Local default 0, CI 1.** Tests in group `known-defect` are never retried, because a known
  defect must fail every time.
* Each retry is appended to `<module>/target/flaky-tests.txt`. TestNG also marks the first attempt as `retried`
  in `testng-results.xml`.
* The quality gate lists every flaky test in the job summary under **"Flaky tests (passed after retry)"** and
  counts them against `gate.maxFlaky` (default 3). Above that the gate fails, even if every test eventually passed.

## 2. Triage (same working day)
The person who first sees a flaky test in a PR or nightly run:
1. Opens an issue from the *Defect report* template with title `[FLAKY] Class#method`, labels `flaky` + area,
   and attaches the failing attempt's evidence (screenshot, page source, console log and Allure attachments are
   captured automatically on failure).
2. Classifies the cause:

| Cause | Typical fix |
|---|---|
| Timing / synchronisation | Replace implicit timing with explicit waits on a state (`BasePage` helpers). `Thread.sleep` is never acceptable |
| Shared or leaked test data | Generate unique data (`TestData` / Datafaker), clean up in `@AfterMethod`, avoid order dependence |
| Environment (third-party site slow/down, rate limit 429) | Back-off filter (`RateLimitFilter`), smoke check first, raise timeouts only with evidence |
| Parallelism | Thread-local state (`DriverManager`), no static mutable fields |
| Real product race condition | Not a test problem: file it as a product defect |

## 3. Quarantine (only if the fix takes longer than one day)
* Add the test to TestNG group **`quarantine`**, keeping any existing groups, and add `@Issue("<issue id>")`.
* PR suites exclude it: `-DexcludedGroups=known-defect,quarantine`. Nightly still runs it, so we see when it's stable.
* A quarantined test must have an **owner** (issue assignee) and a **due date**.

| Severity of the covered flow | Fix-or-delete SLA |
|---|---|
| Checkout, login, payment, data integrity | 2 working days |
| Other user journeys | 5 working days |
| Low-value / duplicated coverage | 10 working days, then delete with a note in the test plan |

* At most **5 tests** may be in quarantine across the repo. Beyond that, new feature work pauses until the number drops.

## 4. Exit from quarantine
A quarantined test leaves the group when it has **passed 10 consecutive nightly runs** after the fix. The PR
that removes the group links the issue and closes it.

## 5. What we don't do
* Raise `retry.count` above 1 to get green builds.
* Delete or `@Ignore` a failing test without an issue.
* Re-run the whole pipeline until it passes. If you re-run, note the flaky test in the PR.
