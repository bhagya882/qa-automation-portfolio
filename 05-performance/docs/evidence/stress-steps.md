### stress steps (profile=smoke)

| Users | req/s | Errors % | p95 all ms | Worst label (p95 ms) | SLO breaches |
|---:|---:|---:|---:|---|---|
| 5 | 42.7 | 0.00 | 26 | sales_report (56) | none |
| 10 | 83.7 | 0.00 | 36 | sales_report (70) | none |
| 20 | 159.8 | 0.00 | 47 | sales_report (132) | none |
| 40 | 264.6 | 0.00 | 292 | sales_report (589) | sales_report p95 589ms > 500ms |
| 80 | 276.2 | 0.00 | 1029 | sales_report (2300) | sales_report p95 2300ms > 500ms; sales_report p99 2336ms > 1000ms |

Breaking point (first level with any SLO breach): 40 users

