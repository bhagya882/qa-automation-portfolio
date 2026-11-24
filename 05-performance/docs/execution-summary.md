# Test Execution Summary: 05 Performance

| | |
|---|---|
| Date | 2026-11-23 |
| Build | Demo Store 1.0.0 (secure mode, in-process), jmeter-java-dsl 1.29.1 |
| Profile | `smoke` |
| Environment | macOS (Darwin 25.6), OpenJDK 26.0.1, Maven 3.9.9 wrapper |

| Suite | Total | Passed | Failed | Blocked | Not run |
|---|---:|---:|---:|---:|---:|
| `testng.xml` (PR-safe: load, stress, spike, endurance) | 4 | 4 | 0 | 0 | 0 |
| `testng-known-defects.xml` (PERF-001 bottleneck) | 1 | 0 | 1 (expected) | 0 | 0 |
| Full profile (`-Dperf.profile=full`, ~50 min) | 5 | | | | 5 (nightly only, not run locally) |
| Grafana/Prometheus monitoring stack | 1 | | | 1 (Docker not installed on authoring machine) | |

Defects: **PERF-001** (open, Major/P2). Observations: OBS-01 to OBS-03. See [performance-results.md](performance-results.md).

Exit criteria status: PR-safe scenarios pass. The one SLO breach is analysed and logged as PERF-001. JTL + HTML dashboards were produced for every scenario in `target/perf/`.
