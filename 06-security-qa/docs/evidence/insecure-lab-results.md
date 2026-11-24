# Security test results — target mode: insecure

Generated 2026-11-23T00:17:03.578567+05:30[Asia/Kolkata] by FindingsReportListener (suite: 06 Security QA - insecure lab)

Total 36 | Passed 23 | Failed 13 | Skipped 0

| Test case | Data | Result | Failure detail (first lines) |
|---|---|---|---|
| SEC-TC-01 [A01] Unauthenticated request to cart is rejected with 401 |  | PASS |  |
| SEC-TC-02 [A01] Forged/unknown bearer token is rejected with 401 |  | PASS |  |
| SEC-TC-03 [A01] IDOR: a customer cannot read another customer's order (CWE-639) |  | **FAIL** | 1 expectation failed. Expected status code &lt;404&gt; but was &lt;200&gt;. |
| SEC-TC-04 [A01] Vertical privilege escalation: USER role cannot call admin endpoint |  | PASS |  |
| SEC-TC-05 [A01] Positive control: ADMIN role can call admin endpoint |  | PASS |  |
| SEC-TC-06 [A05] Security response headers present on every response type | [/api/products] | **FAIL** | [header Content-Security-Policy on /api/products] Expecting actual not to be null |
| SEC-TC-06 [A05] Security response headers present on every response type | [/health] | **FAIL** | [header Content-Security-Policy on /health] Expecting actual not to be null |
| SEC-TC-06 [A05] Security response headers present on every response type | [/search?q=lamp] | **FAIL** | [header Content-Security-Policy on /search?q=lamp] Expecting actual not to be null |
| SEC-TC-07 [A05] API responses are not cacheable (Cache-Control: no-store) |  | **FAIL** | [Cache-Control] expected: "no-store" |
| SEC-TC-08 [A05] Server header does not disclose product/version (CWE-200) |  | **FAIL** | [Server header should be absent or version-free but was: DemoStore/0.1 (Javalin; Java 26.0.1)] Expecting value to be true but was false |
| SEC-TC-09 [A05] Error responses do not leak stack traces (CWE-209) |  | **FAIL** | [error body] Expecting actual: |
| SEC-TC-10 [A05/A07] Session cookie has HttpOnly, Secure and SameSite=Strict |  | **FAIL** | [Set-Cookie] Expecting actual: |
| SEC-TC-11 [A03] Reflected XSS: search term is HTML-encoded in /search (CWE-79) | ["&gt;&lt;img src=x onerror=alert(1)&gt;] | **FAIL** | [raw payload must not be reflected] Expecting actual: |
| SEC-TC-11 [A03] Reflected XSS: search term is HTML-encoded in /search (CWE-79) | [&lt;script&gt;alert(1)&lt;/script&gt;] | **FAIL** | [raw payload must not be reflected] Expecting actual: |
| SEC-TC-11 [A03] Reflected XSS: search term is HTML-encoded in /search (CWE-79) | [&lt;svg/onload=alert(document.domain)&gt;] | **FAIL** | [raw payload must not be reflected] Expecting actual: |
| SEC-TC-12 [A03] SQL-injection style product id is rejected as 400, not 500 |  | PASS |  |
| SEC-TC-13 [A03] Injection-style search term is treated as data (no extra results, no error) |  | PASS |  |
| SEC-TC-14 [A03/A04] Malformed JSON body is rejected with 400 (SEC-007: returns 500, CWE-755) |  | **FAIL** | 1 expectation failed. Expected status code &lt;400&gt; but was &lt;500&gt;. |
| SEC-TC-15 [A04] Cart quantity is bounded 1..20 server-side (boundary values) | [-1, 400] | PASS |  |
| SEC-TC-15 [A04] Cart quantity is bounded 1..20 server-side (boundary values) | [0, 400] | PASS |  |
| SEC-TC-15 [A04] Cart quantity is bounded 1..20 server-side (boundary values) | [1, 201] | PASS |  |
| SEC-TC-15 [A04] Cart quantity is bounded 1..20 server-side (boundary values) | [1000, 400] | PASS |  |
| SEC-TC-15 [A04] Cart quantity is bounded 1..20 server-side (boundary values) | [20, 201] | PASS |  |
| SEC-TC-15 [A04] Cart quantity is bounded 1..20 server-side (boundary values) | [21, 400] | PASS |  |
| SEC-TC-16 [A04] Paging parameters are bounded (no unbounded result sets) | [0, 10, 400] | PASS |  |
| SEC-TC-16 [A04] Paging parameters are bounded (no unbounded result sets) | [1, 0, 400] | PASS |  |
| SEC-TC-16 [A04] Paging parameters are bounded (no unbounded result sets) | [1, 100, 200] | PASS |  |
| SEC-TC-16 [A04] Paging parameters are bounded (no unbounded result sets) | [1, 101, 400] | PASS |  |
| SEC-TC-16 [A04] Paging parameters are bounded (no unbounded result sets) | [abc, 10, 400] | PASS |  |
| SEC-TC-17 [A04] Cannot add a non-existent product to the cart |  | PASS |  |
| SEC-TC-18 [A04] Checkout with an empty cart is refused (409) |  | PASS |  |
| SEC-TC-19 [A07] Logout invalidates the session token server-side |  | PASS |  |
| SEC-TC-20 [A07] Session tokens are unique, long and URL-safe random values (CWE-330) |  | PASS |  |
| SEC-TC-21 [A07] Brute force: login throttled with 429 after 5 failures (CWE-307) |  | **FAIL** | 1 expectation failed. Expected status code &lt;429&gt; but was &lt;401&gt;. |
| SEC-TC-22 [A07] No user enumeration: unknown user and wrong password look identical (CWE-204) |  | PASS |  |
| SEC-TC-23 [A07] Missing credentials are rejected with 400 |  | PASS |  |
