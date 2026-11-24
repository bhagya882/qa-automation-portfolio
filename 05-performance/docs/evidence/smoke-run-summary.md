### load (profile=smoke)

Duration 25s, 320 samples, 13.3 req/s, error rate 0.00%

| Label | Samples | Errors % | Mean ms | Median ms | p90 ms | p95 ms | p99 ms | Max ms | req/s |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| add_to_cart | 51 | 0.00 | 2 | 2 | 3 | 4 | 4 | 5 | 2.3 |
| browse_products | 55 | 0.00 | 2 | 2 | 3 | 7 | 7 | 32 | 2.3 |
| login | 54 | 0.00 | 2 | 2 | 3 | 10 | 10 | 19 | 2.4 |
| place_order | 48 | 0.00 | 2 | 2 | 3 | 4 | 4 | 5 | 2.3 |
| sales_report | 4 | 0.00 | 47 | 46 | 47 | 47 | 47 | 47 | 0.5 |
| search_products | 54 | 0.00 | 2 | 2 | 3 | 3 | 4 | 5 | 2.4 |
| view_product | 54 | 0.00 | 2 | 2 | 3 | 3 | 3 | 4 | 2.4 |
| **TOTAL** | 320 | 0.00 | 2 | 2 | 3 | 3 | 34 | 47 | 13.3 |

Checkout conflicts (409, shared demo account carts): 15 of 48 place_order samples

### stress steps (profile=smoke)

| Users | req/s | Errors % | p95 all ms | Worst label (p95 ms) | SLO breaches |
|---:|---:|---:|---:|---|---|
| 5 | 42.7 | 0.00 | 26 | sales_report (56) | none |
| 10 | 83.7 | 0.00 | 36 | sales_report (70) | none |
| 20 | 159.8 | 0.00 | 47 | sales_report (132) | none |
| 40 | 264.6 | 0.00 | 292 | sales_report (589) | sales_report p95 589ms > 500ms |
| 80 | 276.2 | 0.00 | 1029 | sales_report (2300) | sales_report p95 2300ms > 500ms; sales_report p99 2336ms > 1000ms |

Breaking point (first level with any SLO breach): 40 users

### spike (profile=smoke)

Duration 21s, 531 samples, 26.8 req/s, error rate 0.00%

| Label | Samples | Errors % | Mean ms | Median ms | p90 ms | p95 ms | p99 ms | Max ms | req/s |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| add_to_cart | 76 | 0.00 | 1 | 1 | 1 | 1 | 1 | 2 | 4.5 |
| browse_products | 100 | 0.00 | 1 | 1 | 1 | 2 | 3 | 5 | 5.0 |
| login | 87 | 0.00 | 1 | 1 | 1 | 1 | 2 | 5 | 5.1 |
| place_order | 71 | 0.00 | 1 | 1 | 1 | 1 | 1 | 1 | 4.2 |
| sales_report | 7 | 0.00 | 45 | 44 | 45 | 45 | 45 | 46 | 0.7 |
| search_products | 96 | 0.00 | 1 | 1 | 1 | 1 | 2 | 5 | 5.7 |
| view_product | 94 | 0.00 | 1 | 1 | 1 | 1 | 1 | 3 | 5.6 |
| **TOTAL** | 531 | 0.00 | 1 | 1 | 1 | 1 | 37 | 46 | 26.8 |

Checkout conflicts (409, shared demo account carts): 33 of 71 place_order samples

### spike-recovery (profile=smoke)

Duration 6s, 29 samples, 5.2 req/s, error rate 0.00%

| Label | Samples | Errors % | Mean ms | Median ms | p90 ms | p95 ms | p99 ms | Max ms | req/s |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| add_to_cart | 3 | 0.00 | 2 | 1 | 1 | 1 | 1 | 3 | 4.3 |
| browse_products | 6 | 0.00 | 1 | 1 | 1 | 1 | 1 | 2 | 1.5 |
| login | 5 | 0.00 | 1 | 1 | 2 | 2 | 2 | 2 | 1.3 |
| place_order | 3 | 0.00 | 1 | 1 | 1 | 1 | 1 | 2 | 4.5 |
| search_products | 6 | 0.00 | 1 | 1 | 1 | 1 | 1 | 2 | 1.5 |
| view_product | 6 | 0.00 | 1 | 0 | 0 | 0 | 0 | 1 | 1.5 |
| **TOTAL** | 29 | 0.00 | 1 | 1 | 2 | 2 | 2 | 3 | 5.2 |

Checkout conflicts (409, shared demo account carts): 0 of 3 place_order samples

### endurance (profile=smoke)

Duration 28s, 241 samples, 8.8 req/s, error rate 0.00%

| Label | Samples | Errors % | Mean ms | Median ms | p90 ms | p95 ms | p99 ms | Max ms | req/s |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| add_to_cart | 39 | 0.00 | 1 | 1 | 2 | 2 | 2 | 2 | 1.6 |
| browse_products | 41 | 0.00 | 1 | 1 | 2 | 2 | 2 | 4 | 1.5 |
| login | 40 | 0.00 | 1 | 1 | 1 | 2 | 2 | 2 | 1.6 |
| place_order | 38 | 0.00 | 1 | 1 | 2 | 2 | 2 | 2 | 1.5 |
| sales_report | 3 | 0.00 | 46 | 45 | 45 | 45 | 45 | 48 | 0.2 |
| search_products | 40 | 0.00 | 1 | 1 | 1 | 1 | 1 | 1 | 1.6 |
| view_product | 40 | 0.00 | 1 | 1 | 2 | 2 | 2 | 2 | 1.6 |
| **TOTAL** | 241 | 0.00 | 1 | 1 | 1 | 1 | 19 | 48 | 8.8 |

Checkout conflicts (409, shared demo account carts): 3 of 38 place_order samples

Endurance drift: p95 first third=2ms (71 samples), last third=2ms (83 samples), allowed <= 23ms

### bottleneck (profile=smoke)

Duration 13s, 227050 samples, 23149.5 req/s, error rate 0.00%

| Label | Samples | Errors % | Mean ms | Median ms | p90 ms | p95 ms | p99 ms | Max ms | req/s |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| browse_products | 226835 | 0.00 | 1 | 1 | 1 | 2 | 5 | 93 | 25344.7 |
| sales_report | 215 | 0.00 | 834 | 907 | 964 | 973 | 977 | 979 | 21.9 |
| **TOTAL** | 227050 | 0.00 | 1 | 1 | 1 | 2 | 6 | 979 | 23149.5 |

