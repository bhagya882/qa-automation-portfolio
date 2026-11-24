# Security Findings: Demo Store

| | |
|---|---|
| Target | Demo Store 1.0.0 (`sample-app`). Secure build and the deliberately vulnerable insecure lab (`--insecure`) |
| Test date | 2026-11-23 |
| Environment | Local, in-process (ports 7090/7091), macOS, OpenJDK 26.0.1; curl transcripts on ports 7095/7096 |
| Method | Automated REST Assured/TestNG suite (`testng-insecure-lab.xml`, `testng-known-defects.xml`) + manual curl reproduction |
| Evidence | [`evidence/insecure-lab-results.md`](evidence/insecure-lab-results.md), [`evidence/http-transcripts.md`](evidence/http-transcripts.md), [`evidence/secure-target-results.md`](evidence/secure-target-results.md) |

## Summary

| ID | Title | OWASP 2021 | CWE | Severity (CVSS 3.1) | Priority | Lab | Secure build |
|---|---|---|---|---|---|---|---|
| SEC-001 | IDOR: any user can read any order | A01 Broken Access Control | CWE-639 | **High** (base 6.5, raised*) | P1 | Present | Fixed |
| SEC-002 | Missing security response headers and cache control | A05 Security Misconfiguration | CWE-693, CWE-1021, CWE-525 | Medium 4.3 | P2 | Present | Fixed |
| SEC-003 | Server header discloses product and Java version | A05 | CWE-200 | Low 3.7 | P3 | Present | Fixed |
| SEC-004 | Session cookie without HttpOnly/Secure/SameSite | A05 / A07 | CWE-1004, CWE-614, CWE-1275 | Medium 4.2 | P2 | Present | Fixed |
| SEC-005 | Reflected XSS in `/search?q=` | A03 Injection | CWE-79 | **High** (base 6.1, raised*) | P1 | Present | Fixed |
| SEC-006 | Stack traces returned in error responses | A05 | CWE-209 | Medium 5.3 | P2 | Present | Fixed |
| SEC-007 | Malformed JSON body returns HTTP 500 instead of 400 | A04 Insecure Design / A05 | CWE-755 | Low | P2 | Present | **Open** |
| SEC-008 | No brute-force protection on login | A07 Identification & Auth Failures | CWE-307 | Medium 5.3 | P1 | Present | Fixed |

\* Contextual rating above the base score. IDOR exposes every customer's purchase history through sequential IDs. XSS on the shop page can steal the session, which is amplified by SEC-004.

SEC-001 to SEC-006 and SEC-008 are **seeded** lab vulnerabilities, and the automated suite detected every one of them (13 failing executions in the lab run). **SEC-007 was not seeded**. The suite discovered it in the secure build, so it is a genuine product defect.

---

### SEC-001: IDOR on `GET /api/orders/{id}`
- **Severity / Priority:** High / P1. CVSS 3.1 `AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:N/A:N` (6.5). Raised because order IDs are sequential (1001, 1002, ...), so enumerating all orders is trivial.
- **Affected:** insecure lab. **Detected by:** SEC-TC-03.
- **Steps to reproduce:**
  1. Log in as `customer1` (`POST /api/auth/login`). Add an item (`POST /api/cart/items`), then `POST /api/orders`. Note the `id`, e.g. `1001`.
  2. Log in as `customer2`.
  3. `GET /api/orders/1001` with customer2's bearer token.
- **Expected:** `404 Not Found`, with no data about another user's order.
- **Actual:** `200 OK` with customer1's full order (`"username":"customer1"`, line items, total). See `evidence/http-transcripts.md`.
- **Remediation:** Enforce object-level authorization on every lookup (`order.username == currentUser || role == ADMIN`). Return 404 rather than 403 so IDs can't be probed. Use non-guessable IDs (UUIDs) as defence in depth. Keep one cross-user test per resource type. *Secure mode does this in `DemoStoreApp.getOrder()`.*

### SEC-002: Missing security headers and cache control
- **Severity / Priority:** Medium / P2. CVSS `AV:N/AC:L/PR:N/UI:R/S:U/C:N/I:L/A:N` (4.3).
- **Detected by:** SEC-TC-06 (x3 endpoints), SEC-TC-07.
- **Steps:** `curl -s -D - -o /dev/null http://localhost:7091/api/products`
- **Expected:** `Content-Security-Policy`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy`, `Strict-Transport-Security` (behind TLS), `Permissions-Policy`, and `Cache-Control: no-store` on API responses.
- **Actual:** only `Content-Type` and `Server` are returned.
- **Impact:** clickjacking, MIME sniffing, no CSP to limit XSS (SEC-005), API responses cacheable by proxies or the browser.
- **Remediation:** Set the headers centrally (after-filter or reverse proxy). Start with CSP `default-src 'self'; frame-ancestors 'none'`. Keep ZAP rules 10020/10021/10038 at FAIL so regressions break the build. *Secure mode: `DemoStoreApp.afterRequest()`.*

### SEC-003: Server version disclosure
- **Severity / Priority:** Low / P3. CVSS `AV:N/AC:H/PR:N/UI:N/S:U/C:L/I:N/A:N` (3.7).
- **Detected by:** SEC-TC-08. **Actual:** `Server: DemoStore/0.1 (Javalin; Java 26.0.1)`.
- **Remediation:** Remove the header or make it generic (Jetty `sendServerVersion=false`). Version strings make targeting known CVEs easier.

### SEC-004: Session cookie missing security attributes
- **Severity / Priority:** Medium / P2. CVSS `AV:N/AC:H/PR:N/UI:R/S:U/C:L/I:L/A:N` (4.2).
- **Detected by:** SEC-TC-10. **Actual:** `Set-Cookie: SESSION=<token>; Path=/`. **Expected:** `HttpOnly; Secure; SameSite=Strict; Max-Age=1800`.
- **Impact:** JavaScript (e.g. via SEC-005) can read the session. The cookie travels over plain HTTP and on cross-site requests (CSRF).
- **Remediation:** Set all three flags and a bounded lifetime. *Secure mode: `DemoStoreApp.login()`.*

### SEC-005: Reflected XSS in `/search`
- **Severity / Priority:** High / P1. CVSS `AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N` (6.1). Raised because, with SEC-004, it gives session theft.
- **Detected by:** SEC-TC-11 with 3 payloads (`<script>`, attribute-breaking `"><img onerror>`, `<svg/onload>`).
- **Steps:** open `http://localhost:7091/search?q=<script>alert(1)</script>`.
- **Expected:** the term is rendered as text (`&lt;script&gt;...`).
- **Actual:** `<p>Results for: <script>alert(1)</script></p>`, and the script executes.
- **Remediation:** Context-aware output encoding of all user-controlled values, ideally through an auto-escaping template engine, plus a CSP without `'unsafe-inline'`. *Secure mode: `Html.escape()` in `DemoStoreApp.searchPage()`.*

### SEC-006: Stack trace disclosure in error responses
- **Severity / Priority:** Medium / P2. CVSS `AV:N/AC:L/PR:N/UI:N/S:U/C:L/I:N/A:N` (5.3).
- **Detected by:** SEC-TC-09.
- **Steps:** `curl -si -X POST localhost:7091/api/auth/login -d '{bad json'`
- **Actual:** `500` with `"trace":"com.fasterxml.jackson.core.JsonParseException ... at com.fasterxml.jackson.core.JsonParser._constructReadException(JsonParser.java:2660) ..."`, which reveals frameworks, versions and internal classes.
- **Remediation:** A global exception handler that logs details server-side with a correlation ID and returns a generic body. *Secure mode returns `{"error":"Internal server error"}`.*

### SEC-007: Malformed JSON returns 500 instead of 400 (OPEN in the secure build)
- **Severity / Priority:** Low / P2. No data exposure in the secure build. However, client errors show up as server faults, which pollutes 5xx monitoring and alerting, and anyone can trigger them anonymously.
- **Detected by:** SEC-TC-14 (`known-defect` group; reproduce with `./mvnw -pl 06-security-qa test -Dsuite=testng-known-defects.xml`).
- **Steps:** `POST /api/auth/login` with the body `{not-json`, or an empty body.
- **Expected:** `400 {"error":"Malformed JSON body"}`. The code intends exactly this message.
- **Actual:** `500 {"error":"Internal server error"}` in the secure build. In the lab, a 500 with a stack trace (SEC-006).
- **Root cause:** `DemoStoreApp.parseBody()` catches only `RuntimeException`. Javalin's `bodyAsClass` is Kotlin code that lets Jackson's **checked** `JsonProcessingException` escape undeclared, so it bypasses the catch and reaches the generic 500 handler.
- **Remediation:** Catch `Exception`/`JsonProcessingException` in `parseBody`, or register `app.exception(JsonProcessingException.class, ...)` that maps to 400. Once fixed, move SEC-TC-14 out of `known-defect`. *Owner: sample-app maintainers. Not changed here (test-only scope).*

### SEC-008: No brute-force protection on login
- **Severity / Priority:** Medium / P1. CVSS `AV:N/AC:L/PR:N/UI:N/S:U/C:L/I:N/A:N` (5.3). P1 because the login API is exposed to credential stuffing for every account.
- **Detected by:** SEC-TC-21.
- **Steps:** POST 6 wrong passwords for the same username.
- **Expected:** attempts 1-5 return 401, and attempt 6 returns `429 Too Many Requests`.
- **Actual:** all 6 attempts return 401, with no throttling, lockout or delay.
- **Remediation:** Per account+IP sliding-window throttling, progressive delays, alerting on failure spikes, and MFA for privileged accounts. Generic error messages are already in place (SEC-TC-22). *Secure mode: `LoginThrottle`, 5 failures per 60 s, then 429.*

## Positive results (controls verified in both modes)
Unauthenticated and forged-token requests are rejected (SEC-TC-01/02). Vertical privilege escalation is blocked (TC-04). Injection-style input is treated as data (TC-12/13). Business-rule boundaries are enforced server-side (TC-15 to TC-18). Logout invalidates tokens server-side (TC-19). Tokens are 32-byte `SecureRandom` values (TC-20). There is no user enumeration via login responses (TC-22).
