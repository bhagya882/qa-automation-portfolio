<!-- Real run 2026-11-22T18:56Z: same command over the 03 full run incl. known-defect tests and the Newman 'Known defects' folder; exit code 1 -->
# Evidence 2: known-defect tests included, the gate blocks the build (exit code 1)

## ❌ Quality gate FAILED

| Module | Passed | Failed | Blocked/Skipped | Retried | Pass rate | Status |
|---|---:|---:|---:|---:|---:|---|
| 03-api-testing | 219 | 22 | 0 | 0 | 90.9% | ❌ |
| 10-quality-gate | 12 | 0 | 0 | 0 | 100.0% | ✅ |
| **Total** | **231** | **22** | **0** | **0** | **91.3%** | |

**Thresholds:** min pass rate 100% · max failures/module 0 · max skipped 20% · max flaky 3

### Breaches
- `03-api-testing` has 22 failed test(s), allowed 0
- Overall pass rate 91.3% below 100.0%

### Failed tests
- 03-api-testing: KnownDefectsDefApi001WrongPasswordShouldBe401#Status is 401
- 03-api-testing: KnownDefectsDefApi002AuthMeMustNotExposePasswordSsn#No password field
- 03-api-testing: KnownDefectsDefApi002AuthMeMustNotExposePasswordSsn#No ssn field
- 03-api-testing: KnownDefectsDefApi003EmptyProductBodyShouldBe400#Status is 400
- 03-api-testing: KnownDefectsDefApi004NegativePriceShouldBe400#Status is 400
- 03-api-testing: KnownDefectsDefApi005NegativeCartQuantityShouldBe400#Status is 400
- 03-api-testing: KnownDefectsDefApi006UnsupportedMethodShouldBe405Json#Status is 405
- 03-api-testing: KnownDefectsDefApi006UnsupportedMethodShouldBe405Json#JSON error body
- 03-api-testing: KnownDefectsDefApi010ForgedJwtShouldBe401Not500#Status is 401
- 03-api-testing: ProductReadTests#nonNumericIdShouldBe400 — 1 expectation failed. Expected status code <400> but was <404>.
- 03-api-testing: ProductReadTests#unknownCategoryShouldBe404 — 1 expectation failed. Expected status code <404> but was <200>.
- 03-api-testing: ProductWriteTests#addProductRequiresFields — 1 expectation failed. Expected status code <400> but was <201>.
- 03-api-testing: ProductWriteTests#negativePriceRejected — 1 expectation failed. Expected status code <400> but was <201>.
- 03-api-testing: ProductWriteTests#nonNumericPriceRejected — 1 expectation failed. Expected status code <400> but was <200>.
- 03-api-testing: ProductWriteTests#unsupportedMethodShouldBe405Json — 1 expectation failed. Expected status code <405> but was <404>.
- 03-api-testing: CartTests#cartLineFieldNamesConsistent — 1 expectation failed. JSON path products[0] doesn't match.
- 03-api-testing: CartTests#negativeQuantityRejected — 1 expectation failed. Expected status code <400> but was <201>.
- 03-api-testing: AuthTests#authCookiesDeclareSameSite — Expecting all elements of: ["accessToken=<redacted-jwt>; Max-Age=1800; Path=/; Expires=Sun, 22 Nov 2026 19:16:24 GMT; HttpOnly; Secure",
- 03-api-testing: AuthTests#forgedJwtShouldBe401 — 1 expectation failed. Expected status code <401> but was <500>.
- 03-api-testing: AuthTests#forgedJwtShouldBe401 — 1 expectation failed. Expected status code <401> but was <500>.
- 03-api-testing: AuthTests#meMustNotExposeSecrets — 1 expectation failed. JSON path $ doesn't match.
- 03-api-testing: AuthTests#wrongPasswordShouldReturn401 — 1 expectation failed. Expected status code <401> but was <400>.
