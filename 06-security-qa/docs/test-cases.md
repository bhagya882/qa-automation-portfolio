# Security Test Cases

Automated in `src/test/java/com/qaportfolio/security`. Every case runs against both targets. "Secure" is the expected result on the secure build; "Lab" is the observed result on the insecure lab (2026-11-23 run).

| ID | OWASP | Title | Steps (summary) | Expected (secure) | Secure | Lab |
|---|---|---|---|---|---|---|
| SEC-TC-01 | A01 | Unauthenticated cart/order access | `GET /api/cart`, `POST /api/orders` with no token | 401 | PASS | PASS |
| SEC-TC-02 | A01 | Forged token | Call `/api/cart` with a random bearer token | 401 | PASS | PASS |
| SEC-TC-03 | A01 | IDOR on orders (CWE-639) | customer1 places an order; customer2 does `GET /api/orders/{id}` | Owner gets 200; other user gets 404, no order data | PASS | **FAIL** (SEC-001) |
| SEC-TC-04 | A01 | Vertical privilege escalation | USER calls `GET /api/admin/users` | 403 | PASS | PASS |
| SEC-TC-05 | A01 | Admin positive control | ADMIN calls `GET /api/admin/users` | 200 | PASS | PASS |
| SEC-TC-06 | A05 | Security headers (x3 endpoints) | GET `/health`, `/api/products`, `/search` | CSP, X-Frame-Options DENY, nosniff, Referrer-Policy, HSTS, Permissions-Policy present | PASS | **FAIL x3** (SEC-002) |
| SEC-TC-07 | A05 | API not cacheable (CWE-525) | GET `/api/products` | `Cache-Control: no-store` | PASS | **FAIL** (SEC-002) |
| SEC-TC-08 | A05 | Server version disclosure (CWE-200) | GET `/health`, inspect `Server` | Absent or without version | PASS | **FAIL** (SEC-003) |
| SEC-TC-09 | A05 | Stack trace in errors (CWE-209) | POST malformed JSON to `/api/auth/login` | Generic error body, no trace | PASS | **FAIL** (SEC-006) |
| SEC-TC-10 | A05/A07 | Session cookie flags | Login, inspect `Set-Cookie` | HttpOnly; Secure; SameSite=Strict | PASS | **FAIL** (SEC-004) |
| SEC-TC-11 | A03 | Reflected XSS (x3 payloads, CWE-79) | `GET /search?q=<payload>` | Payload HTML-encoded, never raw | PASS | **FAIL x3** (SEC-005) |
| SEC-TC-12 | A03 | Injection-style path id | `GET /api/products/1' OR '1'='1` | 400, no exception text | PASS | PASS |
| SEC-TC-13 | A03 | Injection-style search | `GET /api/products?search=' OR 1=1 --` | 200, total = 0 (treated as data) | PASS | PASS |
| SEC-TC-14 | A03/A04 | Malformed JSON gives 400 (CWE-755) | POST `{not-json` and empty body to login | 400 | **FAIL** (SEC-007, known-defect) | **FAIL** |
| SEC-TC-15 | A04 | Cart quantity boundaries (x6) | qty 0, -1, 21, 1000 / 1, 20 | 400 / 201 | PASS | PASS |
| SEC-TC-16 | A04 | Paging bounds (x5) | page 0, size 0, size 101, page abc / size 100 | 400 / 200 | PASS | PASS |
| SEC-TC-17 | A04 | Unknown product to cart | productId 99999 | 404 | PASS | PASS |
| SEC-TC-18 | A04 | Empty cart checkout | Clear cart, `POST /api/orders` | 409 | PASS | PASS |
| SEC-TC-19 | A07 | Logout invalidates token | Login, logout, reuse token | 401 after logout | PASS | PASS |
| SEC-TC-20 | A07 | Token randomness (CWE-330) | 10 logins | 10 unique tokens, >= 32 chars, URL-safe, no username | PASS | PASS |
| SEC-TC-21 | A07 | Brute-force throttling (CWE-307) | 6 wrong passwords for one username | Attempts 1-5 give 401, attempt 6 gives 429 | PASS | **FAIL** (SEC-008) |
| SEC-TC-22 | A07 | No user enumeration (CWE-204) | Unknown user vs wrong password | Identical status and body | PASS | PASS |
| SEC-TC-23 | A07 | Missing credentials | Login without password | 400 | PASS | PASS |

**A06 Vulnerable components** is covered by `scripts/dependency-scan.sh`. **A08/A09/A10** (integrity, logging, SSRF) are not applicable to this app's feature set (no deserialisation of untrusted objects, no outbound URL fetch) or are out of scope. They are listed as gaps in the README.
