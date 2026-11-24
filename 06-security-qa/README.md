# 06 — Security-Focused Web Application QA (Java, OWASP Top 10)

Authorized security testing of the **owned** Demo Store ([`sample-app`](../sample-app)). It has two targets:
- **secure mode**, the production-like build. The automated suite must be all green.
- **insecure mode** (`--insecure`), a deliberately vulnerable lab with seeded flaws. The same suite must catch them.

No third-party systems are tested.

## Tools
Java 21+, REST Assured 5.5, TestNG, AssertJ, Allure; OWASP ZAP (Docker, baseline scan); OWASP Dependency-Check (Maven); Burp Suite Community and browser DevTools (manual).

## Layout
| Path | What |
|---|---|
| `src/test/java/com/qaportfolio/security/A01AccessControlTest` | Auth required, forged token, IDOR, privilege escalation |
| `A03InjectionTest` | Reflected XSS (3 payloads), injection-style input, malformed JSON (SEC-007) |
| `A04BusinessLogicTest` | Quantity/paging boundary values, unknown product, empty checkout |
| `A05SecurityConfigTest` | Security headers, cache control, Server header, stack traces, cookie flags |
| `A07AuthSessionTest` | Logout invalidation, token randomness, brute-force throttling, user enumeration |
| `FindingsReportListener` | Writes `target/security/<mode>-results.md` (PASS/FAIL per case) |
| `testng.xml` / `testng-insecure-lab.xml` / `testng-known-defects.xml` | Secure gate / vulnerable lab / open defects |
| `scripts/zap-baseline.sh`, `zap/rules.tsv` | ZAP passive scan with FAIL/WARN rule config |
| `scripts/dependency-scan.sh`, `dependency-check-suppressions.xml` | OWASP Top 10 A06 dependency scan (fails at CVSS >= 7) |
| `docs/` | Test plan, test cases, findings, Burp guide, DevTools checklist, execution summary, evidence |

## How to run (from the repo root)
```bash
./mvnw -pl qa-core,sample-app -am install -DskipTests                              # once
./mvnw -pl 06-security-qa test                                                     # secure target, must pass
./mvnw -pl 06-security-qa test -Dsuite=testng-insecure-lab.xml -DtestFailureIgnore=true   # lab: failures = findings
./mvnw -pl 06-security-qa test -Dsuite=testng-known-defects.xml -DtestFailureIgnore=true  # SEC-007
cat 06-security-qa/target/security/insecure-results.md

# Passive DAST (needs Docker), against a locally started app:
java -jar sample-app/target/sample-app-1.0.0-all.jar --insecure &
06-security-qa/scripts/zap-baseline.sh "http://localhost:7070/search?q=test"      # reports in 06-security-qa/target/zap/

# Dependency scan (needs internet; NVD_API_KEY strongly recommended, kept as a CI secret):
NVD_API_KEY=... 06-security-qa/scripts/dependency-scan.sh                          # target/dependency-check/
```
Demo passwords can be overridden with the `DEMO_CUSTOMER_PASSWORD` / `DEMO_ADMIN_PASSWORD` env vars, which both the app and the tests read.

## Results (2026-11-23)
Secure: **35/35 pass**. Insecure lab: **13 failures across 8 test cases** = every seeded vulnerability detected. A **real defect was found in the secure build**: SEC-007, malformed JSON returns 500 instead of 400. See [docs/security-findings.md](docs/security-findings.md).

## Known limitations
- **Not executed locally:** the ZAP baseline (no Docker) and Dependency-Check (NVD download/API key). The scripts are syntax-checked only, and no output is claimed.
- The ZAP baseline is passive, and the spider finds few links on a JSON API. Seed it with `/search?q=test`, or use `zap-api-scan.py` once an OpenAPI spec exists.
- The lab runs plain HTTP: HSTS and the `Secure` cookie flag are asserted as headers, not exercised over TLS.
- OWASP A08/A09/A10 are not covered (no relevant features in the app), and logging/monitoring was not assessed.
- Seeded vulnerabilities are known in advance, so exploratory manual testing (Burp guide) is still required on real systems.
