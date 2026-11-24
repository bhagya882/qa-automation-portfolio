# Performance Results Summary: smoke profile run, 2026-11-23

All numbers below come from the actual run on 2026-11-23: `./mvnw -pl 05-performance test`, then `-Dsuite=testng-known-defects.xml`. Raw output is in [`evidence/smoke-run-summary.md`](evidence/smoke-run-summary.md), [`evidence/stress-steps.md`](evidence/stress-steps.md) and [`evidence/bottleneck-summary.md`](evidence/bottleneck-summary.md). The JMeter HTML dashboard for the 80-user stress step is in [`evidence/stress-080-users-dashboard/index.html`](evidence/stress-080-users-dashboard/index.html).

## Results by scenario
| Scenario | Users | Samples | Throughput | Error rate | p95 (all) | p99 (all) | Result |
|---|---|---:|---:|---:|---:|---:|---|
| Load | 8 | 320 | 13.3 req/s | 0.00% | 3 ms | 34 ms | **PASS** |
| Stress (5 to 80 users) | stepped | see below | max 276 req/s | 0.00% | 26-1029 ms | | **PASS** (core endpoints within SLO at all levels) |
| Spike | 3, 30, 3 | 531 | 26.8 req/s | 0.00% | 1 ms | 37 ms | **PASS** |
| Spike recovery | 3 | 29 | 5.2 req/s | 0.00% | 2 ms | 2 ms | **PASS** |
| Endurance | 5 | 241 | 8.8 req/s | 0.00% | 1 ms | 19 ms | **PASS** (drift: p95 2 ms, then 2 ms) |
| Bottleneck PERF-001 | 20 + 20 | 227,050 | 23,150 req/s | 0.00% | | | **FAIL as expected** (known-defect) |

PR-safe suite: **4 tests, 4 passed, 0 failed** (130 s). Known-defect suite: **1 test, 1 failed** (expected).

## Stress steps (breaking point)
| Users | req/s | Errors | p95 all | sales_report p95 | Core endpoints p95 | SLO breaches |
|---:|---:|---:|---:|---:|---:|---|
| 5 | 42.7 | 0% | 26 ms | 56 ms | <= 10 ms | none |
| 10 | 83.7 | 0% | 36 ms | 70 ms | <= 10 ms | none |
| 20 | 159.8 | 0% | 47 ms | 132 ms | <= 10 ms | none |
| **40** | 264.6 | 0% | 292 ms | **589 ms** | <= 10 ms | sales_report p95 > 500 ms |
| 80 | 276.2 | 0% | 1029 ms | **2300 ms** | 1-2 ms | sales_report p95 and p99 |

**Breaking point: 40 concurrent users.** Throughput roughly doubles with each step up to 40 users (42.7, 83.7, 159.8, 264.6 req/s). Then it **saturates**: doubling users from 40 to 80 adds only 4% (276 req/s), while the sales report p95 quadruples. No errors occurred, so the system degrades by queueing, not by failing.

An earlier smoke run on the same day put the 40-user step just under the limit (428 ms) and the next run at 571 ms. The breaking point lies around 40 users with run-to-run noise. That is why the 80-user step was added to the smoke profile.

## PERF-001: Sales report serialises all requests (bottleneck)
| | |
|---|---|
| Severity / Priority | Major / P2. Back-office endpoint, but it stalls the whole shopper journey that calls it |
| Endpoint | `GET /api/reports/sales` |
| Reproduce | `./mvnw -pl 05-performance test -Dsuite=testng-known-defects.xml -DtestFailureIgnore=true` |
| Expected | p95 <= 500 ms, p99 <= 1000 ms at 20 concurrent users |
| Actual | p95 **973 ms**, p99 977 ms, mean 834 ms, throughput capped at **21.9 req/s** |
| Control (same run, same 20 users) | `GET /api/products`: 226,835 samples, p95 2 ms, about 23,000 req/s |
| Environment | In-process Demo Store 1.0.0, port 7080, macOS, OpenJDK 26.0.1 |

**Analysis.**
- The report endpoint never exceeds about 22 req/s, regardless of users: 21.9 (bottleneck test), 22.6 (stress step 80). That ceiling matches 1 / 40 ms = 25 req/s for a single-threaded resource, minus overhead. It is a **serialisation point**, not CPU saturation: the control endpoint does about 23k req/s on the same JVM at the same time.
- Little's law confirms queueing. With N = 20 users and X = 21.9 req/s, R = N / X = 0.91 s, which matches the measured mean (834 ms) plus think-free re-arrival.
- Root cause in code: `Store.salesReport()` is `synchronized` (one global lock), and inside the lock it does a 40 ms simulated slow aggregation over all orders. Every caller waits for every earlier caller.
- Knock-on effect: in the stress journey, threads blocked on the report stop issuing other requests, so total throughput plateaus at about 270 req/s even though the other endpoints have huge headroom.

**Recommendations (ordered by impact/effort):**
1. Remove the global lock. Compute the report from an immutable snapshot or a concurrent structure, so readers don't block each other.
2. Cache the report (e.g. 30-60 s TTL) or precompute it incrementally on order placement. Reports rarely need second-level freshness.
3. Move heavy aggregation to an async/background job or a read replica, so it is isolated from the shopper thread pool (bulkhead pattern).
4. Add a per-endpoint SLO alert on `demostore_http_request_duration_seconds` for `/api/reports/sales` (see the Grafana p95 panel).
5. Re-run `BottleneckTest` after the fix, then move it from `known-defect` into the PR-safe suite.

## Other observations
| ID | Observation | Impact | Action |
|---|---|---|---|
| OBS-01 | 409 "Cart is empty" on `place_order` in 6-46% of checkouts (load 15/48, spike 33/71, endurance 3/38) | Correct business behaviour: only 2 demo customer accounts exist, so concurrent sessions share one cart. It is not counted as an error (assertion accepts 201 or 409) but is reported per run | Request seeded load-test accounts (e.g. `loaduser001..500`) in sample-app so every virtual user has its own cart |
| OBS-02 | Generator and SUT in one JVM | Absolute numbers (e.g. 23k req/s) are optimistic | Use the docker-compose stack or a separate host for capacity numbers |
| OBS-03 | No latency drift in the 25 s soak (p95 2 ms, then 2 ms) | Too short to show leaks; the full profile runs 30 min | Run `-Dperf.profile=full` nightly and watch JVM memory in Grafana; orders/sessions are kept in memory without eviction |
