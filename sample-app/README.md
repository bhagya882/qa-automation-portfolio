# sample-app: Demo Store API (owned test target)

A small e-commerce REST API written in Java with Javalin. It exists so the load tests (project 05) and security tests (project 06) run against a system **this repository owns**. Running them against third-party sites without permission is not allowed.

## Run

```bash
./mvnw -pl sample-app -am package -DskipTests
java -jar sample-app/target/sample-app-1.0.0-all.jar              # secure mode, http://localhost:7070
java -jar sample-app/target/sample-app-1.0.0-all.jar --insecure   # vulnerable lab mode
PORT=8080 java -jar sample-app/target/sample-app-1.0.0-all.jar    # different port
docker build -t demo-store sample-app && docker run -p 7070:7070 demo-store
```

## Endpoints

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/health` | – | status and mode |
| GET | `/metrics` | – | Prometheus text format (request counter and latency histogram per route) |
| POST | `/api/auth/login` | – | `{"username","password"}` → `{token,...}`; also sets the `SESSION` cookie |
| POST | `/api/auth/logout` | Bearer | invalidates the token |
| GET | `/api/products?search=&page=&size=` | – | paginated; size 1–100 |
| GET | `/api/products/{id}` | – | 404 if unknown |
| GET / POST / DELETE | `/api/cart`, `/api/cart/items` | Bearer | quantity 1–20 |
| POST / GET | `/api/orders`, `/api/orders/{id}` | Bearer | users see only their own orders (secure mode) |
| GET | `/api/admin/users` | Bearer (ADMIN) | 403 for USER |
| GET | `/api/reports/sales` | – | **deliberate bottleneck** (global lock plus 40 ms) for perf analysis |
| GET | `/search?q=` | – | HTML page; reflects the query |

Demo accounts are synthetic and only work inside this local process: `customer1`, `customer2` (USER) and `admin` (ADMIN). Default passwords are in `Store.java` and can be overridden with `DEMO_CUSTOMER_PASSWORD` / `DEMO_ADMIN_PASSWORD`.

## Seeded vulnerabilities (`--insecure` only)

| # | Flaw | OWASP 2021 | Secure-mode behaviour |
|---|---|---|---|
| V1 | IDOR: any user can read any order | A01 | 404 unless owner or admin |
| V2 | Missing security headers (CSP, XFO, nosniff, HSTS, Referrer-Policy) | A05 | headers set on every response |
| V3 | `Server` header discloses framework and Java version | A05 | header absent |
| V4 | Reflected XSS on `/search?q=` | A03 | output HTML-escaped |
| V5 | Stack traces in 500 responses | A05 | generic error body |
| V6 | No login throttling (brute force) | A07 | 429 after 5 failures/minute |
| V7 | Session cookie without HttpOnly/Secure/SameSite | A07 | all flags set |

Never expose insecure mode outside localhost or CI.
