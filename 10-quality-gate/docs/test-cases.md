# Test Cases — Project 10 (QualityGate CLI)

Automated in `src/test/java/com/qaportfolio/gate/QualityGateTest.java`. Fixtures in `src/test/resources/fixtures/`
are real tool outputs. `testng-mixed.xml` was produced by TestNG 7.11 from a class with one passing, one failing,
one skipped and one fail-then-pass-on-retry test; the Newman files come from project 03 runs.

| ID | Test | Input | Expected |
|---|---|---|---|
| TC-QG-01 | `parsesTestNgCounts` | testng-mixed.xml | passed 2, failed 1, skipped 1, retried 1. Config methods ignored. Failure line keeps the assertion message |
| TC-QG-02 | `parsesNewmanJUnit` | Newman data-driven + known-defects JUnit | 9 failed, 29 passed, 2 sources |
| TC-QG-03 | `greenBuildPasses` | all-pass TestNG + passing Newman, both modules required | No breaches, pass rate 100%, markdown row correct |
| TC-QG-04 | `failuresBreakGateUnlessOverridden` | 1 failure | Breach "has 1 failed" and "Overall pass rate 66.7%". Passes when `gate.maxFailures.<module>=1` and `minPassRate=60` |
| TC-QG-05 | `missingRequiredModuleFails` | required module without results | Exactly one breach: "reported no results" |
| TC-QG-06 | `tooManySkippedFails` | 25% skipped, limit 10% | Breach "skipped/blocked 25.0%" |
| TC-QG-07 | `flakyTestsAreReportedAndLimited` | flaky-tests.txt with 2 entries | Listed and tolerated at max 3. Breach at max 1 |
| TC-QG-08 | `aggregatesSameModuleAcrossArtifacts` | Same module from chrome + firefox artifacts | One module, counts summed (4 passed / 1 failed), flaky found inside artifacts |
| TC-QG-09 | `ignoresSourcesAndDependencies` | Results under `src/` and `node_modules/` | Nothing scanned |
| TC-QG-10 | `jsonSummary` | mixed result | JSON has passed=false, module counts and breaches, and escapes quotes/newlines |
| TC-QG-11 | `redactsTokens` | Failure message containing a JWT | Replaced with `<redacted-jwt>` |
| TC-QG-12 | `rejectsDoctype` | XML with an external-entity DOCTYPE | Parsing refused (XXE protection) |

## Integration checks (manual, recorded in evidence/)
| ID | Check | Expected | Result |
|---|---|---|---|
| IT-QG-01 | Gate over real PR-mode results of 03 + 10 | exit 0 | ✅ exit 0 ([summary](evidence/summary-pr-mode-pass.md)) |
| IT-QG-02 | Gate over real 03 full run, including known defects | exit 1, 22 failures listed | ✅ exit 1 ([summary](evidence/summary-known-defects-blocked.md)) |
| IT-QG-03 | `stage-results.sh` + artifact-per-folder layout, then gate | Modules detected, Allure results mergeable | ✅ exit 0, 2 modules, Allure files merged |
| IT-QG-04 | `@action-validator/cli` on both workflows | 0 errors | ✅ 0 errors |
