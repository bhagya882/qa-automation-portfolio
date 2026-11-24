# Performance Test Plan: Demo Store (Project 05)

## 1. Objective
Measure response-time percentiles, error rates and throughput of the Demo Store API under load, stress, spike and endurance conditions. Check them against defined SLOs and identify bottlenecks.

## 2. Authorization
Load is only generated against the **owned** Demo Store (`sample-app`), started in-process by the tests or via `monitoring/docker-compose.yml`. `-Dperf.baseUrl` must only point at systems you own or have written permission to load-test. No public or third-party site is load-tested.

## 3. Scope
| In scope | Out of scope |
|---|---|
| REST API: products (list/search/detail), auth login, cart, orders, sales report | Front-end rendering, CDN/network latency |
| Server-side latency, errors and throughput under concurrency | Multi-node scaling, database tuning (the app is in-memory) |
| Bottleneck isolation and analysis | Production capacity sign-off (a dedicated, prod-like environment is needed) |

## 4. Workload model (user journey)
One iteration = one shopping session, with think time between steps (`perf.think.*`):

| # | Sampler label | Request | Notes |
|---|---|---|---|
| 1 | `browse_products` | `GET /api/products?page=${page}&size=10` | page from CSV |
| 2 | `search_products` | `GET /api/products?search=${searchTerm}` | 15 terms incl. no-result term |
| 3 | `view_product` | `GET /api/products/${productId}` | ids 1-60 |
| 4 | `login` | `POST /api/auth/login` | token extracted with JSON extractor |
| 5 | `add_to_cart` | `POST /api/cart/items` | quantity 1-3 |
| 6 | `place_order` | `POST /api/orders` | 201, or 409 when another session of the same demo account already checked out the shared cart |
| 7 | `sales_report` | `GET /api/reports/sales` | back-office action in `perf.reportPct`% of iterations |

Test data: `src/test/resources/data/shoppers.csv`, 300 synthetic rows generated with a fixed seed. No real user data.

## 5. Scenarios
| ID | Scenario | Smoke profile (PR CI) | Full profile (nightly) | Pass criteria |
|---|---|---|---|---|
| PERF-TC-01 | Load | 8 users, 5 s ramp, 20 s hold | 50 users, 60 s ramp, 10 min hold | All SLOs incl. min throughput |
| PERF-TC-02 | Stress (stepped) | 5, 10, 20, 40, 80 users x 6 s | 25-400 users x 90 s | Record breaking point; core endpoints within SLO up to `minSustainableUsers` (20 / 100); errors < 1% |
| PERF-TC-03 | Spike | 3, then 30, then 3 users (1 s ramps) + 5 s recovery | 10, then 200, then 10 users + 60 s recovery | No errors; spike SLO (p95 800 ms, p99 1500 ms); recovery meets normal SLO |
| PERF-TC-04 | Endurance (soak) | 5 users, 25 s | 30 users, 30 min | SLOs + p95 drift (last third vs first third) <= 50% (25% full) + slack |
| PERF-TC-05 | Bottleneck isolation (PERF-001) | 20 users on report + 20 on control | 50 + 50 users | Report SLO under concurrency; `known-defect`, expected to fail |

## 6. SLOs (thresholds)
Defined in `src/test/resources/config/default.properties` and overridable per label (`perf.threshold.p95Ms.<label>`):

| Metric | Default | sales_report |
|---|---|---|
| p95 latency | 300 ms | 500 ms |
| p99 latency | 800 ms | 1000 ms |
| Error rate (overall) | <= 1% | |
| Throughput (load test) | >= 5 req/s smoke, >= 12 req/s full | |

## 7. Environment
| Item | Value |
|---|---|
| SUT | Demo Store 1.0.0 (Javalin 6.7 / Jetty), in-process, port 7080, secure mode |
| Load generator | jmeter-java-dsl 1.29.1 (embedded Apache JMeter 5.6.x), TestNG 7.11 |
| Machine | macOS (Darwin 25.6), OpenJDK 26.0.1. Load generator and SUT share one JVM, so results are relative, not capacity numbers |
| Monitoring (optional) | Prometheus + Grafana via `monitoring/docker-compose.yml` scraping `/metrics` |

## 8. Entry / exit criteria
**Entry:** the app builds and `/health` returns `UP`; functional smoke passes; test data generated; SLOs agreed.
**Exit:** all PR-safe scenarios pass in the smoke profile. Every SLO breach is either fixed or logged as a defect with analysis (PERF-001). JTL + HTML dashboard archived for each scenario.

## 9. Risks / limitations
- Generator and SUT on one host compete for CPU. Absolute numbers are optimistic for the API endpoints and must not be used as production capacity.
- Only 2 demo customer accounts exist, so concurrent threads share carts (409 checkout conflicts, counted separately).
- In-memory data grows during long soaks (orders, sessions). The endurance drift check is designed to catch the resulting slowdown.
