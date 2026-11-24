# Defects — Project 10 (found while building the pipeline; all fixed)

Environment: local macOS 26 / JDK 26 / Node 20, repository at the time of writing.

| ID | Title | Severity | Priority | Status |
|---|---|---|---|---|
| DEF-CI-001 | Same module from several CI jobs overwrote each other's results | Major | P1 | Fixed |
| DEF-CI-002 | Tokens could leak into the CI job summary through failure messages | Critical | P1 | Fixed |
| DEF-CI-003 | Gate counted its own test fixtures as real results | Major | P2 | Fixed |
| DEF-CI-004 | Newman "no token" test was silently authenticated by the cookie jar (false pass risk) | Major | P2 | Fixed (see project 03) |

### DEF-CI-001: results from parallel jobs collided
**Steps:** UI matrix runs `04-e2e-framework` on chrome and firefox. Both artifacts contain
`04-e2e-framework/target/surefire-reports/testng-results.xml`. Download them with `merge-multiple: true`.
**Expected:** both browsers are counted. **Actual (design review):** the last file wins and the other browser's
failures disappear, which is a false green.
**Fix:** artifacts are downloaded one folder each, and the gate takes the module name from the folder above
`target/` and sums the results. Regression test TC-QG-08.

### DEF-CI-002: JWT in failure message would be printed to `$GITHUB_STEP_SUMMARY`
**Steps:** run 03 with known defects. `AuthTests#authCookiesDeclareSameSite` fails, and its AssertJ message includes the
full `Set-Cookie: accessToken=eyJ…` header. Run the gate.
**Expected:** no credentials in CI output. **Actual:** the gate's "Failed tests" list contained the token. The token
was short-lived and for a demo account, but this breaks the "no tokens in logs" rule.
**Fix:** `ResultParser.redact()` masks JWTs in every message, and project 03's `npm run evidence` redacts saved
reports too. Regression test TC-QG-11.

### DEF-CI-003: fixtures picked up as real results
**Steps:** run the gate with `--root .` after `mvn test` in 10-quality-gate.
**Actual:** `target/test-classes/fixtures/flaky-tests.txt` was close to being read as real flaky tests. It was
only missed because of the walk depth.
**Fix:** `src/`, `classes/` and `test-classes/` are ignored, and flaky files must be exactly
`<module>/target/flaky-tests.txt`. Regression test TC-QG-09.

### DEF-CI-004: cookie jar masked an authentication test
See [03 execution summary](../../03-api-testing/docs/execution-summary.md). Login sets HttpOnly token cookies,
and Newman replayed them on the "without token" request. The request now unsets the cookies first.
