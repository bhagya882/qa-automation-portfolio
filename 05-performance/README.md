# 05 — Performance and Load Testing (Java + Apache JMeter)

Load, stress, spike and endurance tests for the **owned** Demo Store API ([`sample-app`](../sample-app)). They are written in Java with [jmeter-java-dsl](https://abstracta.github.io/jmeter-java-dsl/), which runs Apache JMeter embedded, so no JMeter install is needed. Each plan is also exported as a `.jmx` file for the JMeter GUI/CLI, and an optional Prometheus + Grafana stack is included for live monitoring.

## Tools
Java 21+, Maven (wrapper), TestNG, jmeter-java-dsl 1.29.1 (Apache JMeter 5.6), AssertJ, Allure; Docker + Prometheus + Grafana (optional).

## Layout
| Path | What |
|---|---|
| `src/test/java/com/qaportfolio/perf/Journey.java` | Shopper journey (7 labelled samplers, think time, token extraction, CSV data) |
| `LoadTest`, `StressTest`, `SpikeTest`, `EnduranceTest` | PR-safe scenarios (`testng.xml`) |
| `BottleneckTest` | PERF-001 reproduction, group `known-defect` (`testng-known-defects.xml`) |
| `Thresholds.java` | p95/p99 per label, error rate and throughput SLO evaluation |
| `src/test/resources/config/default.properties` | SLOs and target |
| `src/test/resources/perf/{smoke,full}.properties` | Load profiles (users, durations, think time) |
| `src/test/resources/data/shoppers.csv` | Synthetic test data |
| `jmeter/*.jmx` | Generated JMeter plans (target `http://localhost:7070`), refreshed on every run |
| `monitoring/` | docker-compose: Demo Store + Prometheus + Grafana with a provisioned dashboard |
| `docs/` | Test plan, results and bottleneck analysis, execution summary, evidence |

## How to run (from the repo root)
```bash
./mvnw -pl qa-core,sample-app -am install -DskipTests        # once
./mvnw -pl 05-performance test                               # smoke profile, ~2.5 min
./mvnw -pl 05-performance test -Dperf.profile=full           # nightly profile, ~50 min
./mvnw -pl 05-performance test -Dtest=StressTest             # single scenario
./mvnw -pl 05-performance test -Dsuite=testng-known-defects.xml -DtestFailureIgnore=true   # PERF-001
./mvnw -pl 05-performance test -Dperf.baseUrl=http://localhost:7070   # external owned instance
./mvnw -pl 05-performance test -Dperf.threshold.p95Ms=200    # override any SLO
```
Outputs per scenario: `05-performance/target/perf/<scenario>/results.jtl`, `html/index.html` (JMeter dashboard) and `summary.md`. There is also a combined `target/perf/summary.md`, and results go to the shared Allure report.

**JMeter GUI/CLI:** start the app (`java -jar sample-app/target/sample-app-1.0.0-all.jar`), then open `05-performance/jmeter/load.jmx` in JMeter, or run `jmeter -n -t 05-performance/jmeter/load.jmx`.

**Monitoring (Grafana):**
```bash
./mvnw -pl sample-app -am package -DskipTests
docker compose -f 05-performance/monitoring/docker-compose.yml up -d --build
./mvnw -pl 05-performance test -Dperf.baseUrl=http://localhost:7070
# Grafana http://localhost:3000 -> Performance / "Demo Store - Load Test"; Prometheus http://localhost:9090
```

## Results (2026-11-23, smoke)
4/4 PR-safe scenarios pass with 0% errors. The stress breaking point is **40 users**, caused by **PERF-001**: `/api/reports/sales` serialises on a global lock, capping it at about 22 req/s with p95 973 ms at 20 users, while other endpoints stay at a p95 of 2 ms. Details: [docs/performance-results.md](docs/performance-results.md).

## Known limitations
- Load generator and SUT share one JVM and host, so the numbers are relative and are not capacity figures.
- Only two demo customer accounts exist, so concurrent users share carts and get 409 checkout conflicts. These are accepted and counted (OBS-01).
- **Not verified locally:** the `monitoring/` Docker stack (Docker is not installed on the authoring machine) and the `full` profile (it takes ~50 min and is intended for nightly CI).
- `htmlReporter` needs an empty output directory. Each test deletes `target/perf/<scenario>` before running.
