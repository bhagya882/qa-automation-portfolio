# Test Execution Summary: 06 Security QA

| | |
|---|---|
| Date | 2026-11-23 |
| Target | Demo Store 1.0.0, in-process: secure (port 7090) and insecure lab (port 7091) |
| Environment | macOS (Darwin 25.6), OpenJDK 26.0.1, Maven 3.9.9 wrapper, REST Assured 5.5.7, TestNG 7.11 |

| Suite / activity | Total | Passed | Failed | Blocked | Not run | Notes |
|---|---:|---:|---:|---:|---:|---|
| `testng.xml`, secure target (PR gate) | 35 | 35 | 0 | 0 | 0 | 22 test cases (SEC-TC-14 excluded as known-defect), 35 executions with data providers |
| `testng-insecure-lab.xml`, vulnerable lab | 36 | 23 | 13 | 0 | 0 | All 13 failures are expected detections of SEC-001 to SEC-008 |
| `testng-known-defects.xml`, secure target | 1 | 0 | 1 | 0 | 0 | SEC-007 open (500 instead of 400) |
| ZAP baseline (`scripts/zap-baseline.sh`) | 1 | | | 1 | | Docker not installed on the authoring machine |
| Dependency-Check (`scripts/dependency-scan.sh`) | 1 | | | 1 | | Needs NVD database download/API key; not run |
| Manual Burp / DevTools checklists | 2 | | | | 2 | Guides provided; not executed in this run |

**Findings:** 8 (2 High, 4 Medium, 2 Low). 7 are fixed in the secure build; **SEC-007 is open**. See [security-findings.md](security-findings.md).

**Evidence:** [evidence/secure-target-results.md](evidence/secure-target-results.md), [evidence/insecure-lab-results.md](evidence/insecure-lab-results.md), [evidence/http-transcripts.md](evidence/http-transcripts.md). Surefire/TestNG XML is in `06-security-qa/target/surefire-reports/`, plus Allure results.

**Exit criteria:** secure suite 100% ✓ · all seeded vulnerabilities detected ✓ · no open High in the secure build ✓ · ZAP / Dependency-Check gates ✗ not yet executed (blocked, run in CI).
