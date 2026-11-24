<!-- Real run 2026-11-22T18:56Z: java -jar 10-quality-gate/target/10-quality-gate-1.0.0.jar --root <results-dir> --config 10-quality-gate/quality-gate.properties --set gate.requiredModules=03-api-testing,10-quality-gate ; exit code 0 -->
# Evidence 1: PR-mode results for 03-api-testing + 10-quality-gate (exit code 0)

## ✅ Quality gate PASSED

| Module | Passed | Failed | Blocked/Skipped | Retried | Pass rate | Status |
|---|---:|---:|---:|---:|---:|---|
| 03-api-testing | 200 | 0 | 0 | 0 | 100.0% | ✅ |
| 10-quality-gate | 12 | 0 | 0 | 0 | 100.0% | ✅ |
| **Total** | **212** | **0** | **0** | **0** | **100.0%** | |

**Thresholds:** min pass rate 100% · max failures/module 0 · max skipped 20% · max flaky 3

