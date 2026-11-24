# Test Plan — Project 03: REST API Testing and Automated Regression

## 1. Objective
Verify the positive and negative behaviour of a REST e-commerce API: status codes, response schemas,
authentication, input validation and error handling. Automate a regression collection and run it in CI on every
pull request.

## 2. System under test
| Item | Value |
|---|---|
| API | DummyJSON (`https://dummyjson.com`). A public fake REST API published for testing and prototyping |
| Areas | Auth (`/auth/login`, `/auth/me`, `/auth/refresh`), Products, Carts, Users |
| Write behaviour | POST/PUT/PATCH/DELETE are **simulated**: responses echo the change, nothing is persisted |
| Permission | The API is offered publicly for testing. We send low-volume functional traffic only (no load or security scanning), and stay below its rate limit (100 requests per window) |

## 3. Scope
**In scope:** functional and contract testing of the endpoints above; authentication and token handling;
validation of query, path and body input; error format; response headers; response-time SLA (< 3 s per request).

**Out of scope:** load/performance testing (project 05 does this against our own sample app), penetration testing
(project 06), and UI testing.

## 4. Approach
| Layer | Tool | What it covers |
|---|---|---|
| Exploratory | curl, Postman | Probing edge cases (invalid ids, negative limits, wrong methods, forged tokens) to find defects |
| Regression collection | Postman + Newman | 32 requests / 137 assertions plus a 4-row data-driven run. JUnit + HTML reports |
| Code-based regression | Java 21, REST Assured 5, TestNG, JSON Schema Validator, Allure | 66 test invocations: request/response specs, auth helper, POJOs, data providers, schema contracts |
| CI | GitHub Actions (`.github/workflows/pr-quality-gate.yml`, `api` job) | Runs both suites on every PR. The quality gate (project 10) fails the build on threshold breach |

Defects found during exploration become `known-defect` tests. They stay red until DummyJSON fixes the defect.
PR CI excludes them (`-DexcludedGroups=known-defect`) and the nightly job runs them for tracking.

## 5. Test data
* Credentials: the demo account `emilys` published in the DummyJSON docs. It isn't real personal data, and it can
  be overridden with `API_USERNAME`/`API_PASSWORD`.
* Tokens are obtained at runtime and never stored. `npm run evidence` redacts JWTs from saved reports.
* Data files: `src/test/resources/testdata/*.json` (REST Assured) and `postman/data/products.csv` (Newman).

## 6. Environment
macOS 26 / Ubuntu 24.04 (CI) · JDK 21+ (tested on 26) · Maven wrapper 3.9.9 · Node 20 · Newman 6.2.1.

## 7. Entry criteria
* `GET https://dummyjson.com/products/1` returns 200 (smoke).
* The build compiles, and the Postman collection imports without errors.

## 8. Exit criteria
* 100% of non-`known-defect` tests pass in both Newman and REST Assured.
* Every failing `known-defect` test is linked to a defect report in `docs/defects/`.
* No open Critical defect without a documented risk acceptance. For DummyJSON this is accepted, because the
  API is third-party and read-only for us.
* Reports archived (CI artifacts, plus `docs/evidence/` for the submission).

## 9. Risks
| Risk | Mitigation |
|---|---|
| Public API changes or is down | Smoke check first. Failures show the HTTP status, so outages are easy to tell apart from regressions |
| Rate limiting (429) | `RateLimitFilter` waits for `x-ratelimit-reset` and retries once. Small TestNG thread pool. Newman `--delay-request 50` |
| Corporate TLS interception on local machines | Use the OS trust store (`-Djavax.net.ssl.trustStoreType=KeychainStore` on macOS, `NODE_EXTRA_CA_CERTS` for Newman) instead of disabling TLS checks |
