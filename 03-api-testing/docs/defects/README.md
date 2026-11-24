# Defect Register — Project 03 (DummyJSON API)

Found by exploratory testing on 2026-11-22 and reproduced by automated `known-defect` tests: REST Assured,
plus the Postman folder "Known defects".
Environment for every report: `https://dummyjson.com` (public, production instance). Clients were curl 8.x,
REST Assured 5.5.7 on JDK 26 and Newman 6.2.1 on Node 20.19, all from macOS 26.
DummyJSON is a third-party demo API, so these reports document defects but we can't get them fixed.
In a real project each report would be filed with `.github/ISSUE_TEMPLATE/defect_report.yml`.

Severity: Critical > Major > Minor > Trivial. Priority: P1 (fix now) … P4 (backlog).

| ID | Title | Severity | Priority | Automated test |
|---|---|---|---|---|
| [DEF-API-001](#def-api-001) | Wrong password returns 400 instead of 401 | Minor | P3 | `AuthTests.wrongPasswordShouldReturn401` |
| [DEF-API-002](#def-api-002) | `/auth/me` exposes password, SSN and card number | Critical | P1 | `AuthTests.meMustNotExposeSecrets` |
| [DEF-API-003](#def-api-003) | Add product accepts an empty body | Major | P2 | `ProductWriteTests.addProductRequiresFields` |
| [DEF-API-004](#def-api-004) | Negative / non-numeric price accepted | Critical | P1 | `ProductWriteTests.negativePriceRejected`, `nonNumericPriceRejected` |
| [DEF-API-005](#def-api-005) | Negative cart quantity accepted → negative totals | Critical | P1 | `CartTests.negativeQuantityRejected` |
| [DEF-API-006](#def-api-006) | Unsupported method returns 404 HTML instead of 405 JSON | Minor | P3 | `ProductWriteTests.unsupportedMethodShouldBe405Json` |
| [DEF-API-007](#def-api-007) | Non-numeric product id returns 404 instead of 400 | Trivial | P4 | `ProductReadTests.nonNumericIdShouldBe400` |
| [DEF-API-008](#def-api-008) | Unknown category returns 200 with an empty list | Minor | P4 | `ProductReadTests.unknownCategoryShouldBe404` |
| [DEF-API-009](#def-api-009) | Cart line field differs between GET (`discountedTotal`) and add/update (`discountedPrice`) | Major | P2 | `CartTests.cartLineFieldNamesConsistent` |
| [DEF-API-010](#def-api-010) | Forged JWT causes 500 Internal Server Error | Major | P2 | `AuthTests.forgedJwtShouldBe401` (2 rows) |
| [DEF-API-011](#def-api-011) | Auth cookies have no SameSite attribute | Minor | P3 | `AuthTests.authCookiesDeclareSameSite` |

Evidence: [`../evidence/restassured-full-run.txt`](../evidence/restassured-full-run.txt),
[`../evidence/known-defects-report.html`](../evidence/known-defects-report.html) (Newman HTML, JWTs redacted).

---

### DEF-API-001
**Wrong password returns 400 Bad Request instead of 401 Unauthorized** · Minor · P3
1. `curl -i -X POST https://dummyjson.com/auth/login -H 'Content-Type: application/json' -d '{"username":"emilys","password":"wrong"}'`

* **Expected:** `401 Unauthorized`, so the request was well formed but the credentials are wrong (RFC 9110 §15.5.2).
* **Actual:** `400 Bad Request` with `{"message":"Invalid credentials"}`.
* **Impact:** clients can't tell malformed requests from failed authentication, and generic 401 handling (re-login) doesn't trigger.

### DEF-API-002
**`GET /auth/me` returns the user's password, SSN and full bank card data** · Critical · P1
1. Log in as `emilys` and copy `accessToken`.
2. `curl https://dummyjson.com/auth/me -H "Authorization: Bearer <accessToken>"`

* **Expected:** profile fields only. Secrets and regulated PII are never returned (OWASP API3:2023 Broken Object Property Level Authorization).
* **Actual:** the body has `password` (plain text), `ssn`, `bank.cardNumber`, `bank.cardExpire` and `bank.iban`, among others.
* **Impact:** in a real system this is a direct data breach: any XSS or log capture leaks credentials. The data is fake here, but the pattern is the defect.

### DEF-API-003
**`POST /products/add` with `{}` returns 201 Created** · Major · P2
1. `curl -i -X POST https://dummyjson.com/products/add -H 'Content-Type: application/json' -d '{}'`

* **Expected:** `400` naming the required fields (title, price, category).
* **Actual:** `201` with `{"id":195}`, a product with no title or price.

### DEF-API-004
**Negative and non-numeric prices accepted** · Critical · P1
1. `POST /products/add` with `{"title":"x","price":-5}` gives `201 {"id":195,"title":"x","price":-5}`.
2. `PATCH /products/1` with `{"price":"notanumber"}` gives `200` and the response has `"price":"notanumber"`.

* **Expected:** `400`. Price must be a positive number.
* **Impact:** pricing integrity. Negative prices would let an order total go below zero. Also a type-contract break for clients.

### DEF-API-005
**Cart accepts negative quantity and returns negative totals** · Critical · P1
1. `POST /carts/add` with `{"userId":1,"products":[{"id":1,"quantity":-3}]}`

* **Expected:** `400`. Quantity must be ≥ 1.
* **Actual:** `201` with `"quantity":-3,"total":-29.97,"discountedPrice":-27`.
* **Impact:** a classic business-logic flaw: a negative line reduces the order total (free goods / refunds).

### DEF-API-006
**Unsupported HTTP method returns 404 HTML instead of 405 JSON** · Minor · P3
1. `curl -i -X POST https://dummyjson.com/products/1`

* **Expected:** `405 Method Not Allowed` with an `Allow` header and a JSON error body, consistent with the rest of the API.
* **Actual:** `404` with `Content-Type: text/html` and body `<pre>Cannot POST /products/1</pre>`, which also reveals the Express framework.

### DEF-API-007
**Non-numeric id returns 404 instead of 400** · Trivial · P4
1. `GET /products/abc` returns `404 {"message":"Product with id 'abc' not found"}`.

* **Expected:** `400`, because the id format is invalid. The query-parameter validation (`limit=abc` → 400) handles this case correctly.

### DEF-API-008
**Unknown category returns 200 with an empty list** · Minor · P4
1. `GET /products/category/does-not-exist` returns `200 {"products":[],"total":0,"skip":0,"limit":0}`.

* **Expected:** `404` for a non-existent collection resource, as `/users/0` and `/carts/user/99999` already do. Clients can't tell a typo from an empty category.

### DEF-API-009
**Inconsistent cart line schema between read and write** · Major · P2
1. `GET /carts/1`: each line has `discountedTotal`.
2. `POST /carts/add` or `PUT /carts/1`: each line has `discountedPrice` instead, and that value is rounded to an integer (e.g. `-27`).

* **Expected:** one cart line schema across endpoints.
* **Impact:** clients that deserialise into one model lose the discounted amount after add/update.

### DEF-API-010
**Forged / structurally valid JWT produces 500 Internal Server Error** · Major · P2
1. `curl -i https://dummyjson.com/auth/me -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MX0.invalidsignature'`
2. Also `Bearer a.b.c`.

* **Expected:** `401` with `Invalid/Expired Token!`, the same as for `Bearer garbage`.
* **Actual:** `500 {"message":"invalid signature"}` / `500 {"message":"invalid token"}`.
* **Impact:** an unhandled exception path. It inflates error-rate alerts, leaks library error messages, and a
  client may retry on 5xx.

### DEF-API-011
**Auth cookies are missing a SameSite attribute** · Minor · P3
1. `POST /auth/login` (valid). The response has `Set-Cookie: accessToken=…; Max-Age=3600; Path=/; HttpOnly; Secure`.

* **Expected:** `SameSite=Lax` or `Strict` as CSRF defence in depth. HttpOnly and Secure are already correct (TC-API-007 passes).
