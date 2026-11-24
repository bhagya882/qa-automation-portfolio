# Security Test Plan: Demo Store (Project 06)

## 1. Authorization statement
All testing in this project targets the **Demo Store** application in [`sample-app/`](../../sample-app), which belongs to this repository and runs locally (or in a CI container) for the duration of a test run. No third-party system is scanned, proxied or attacked. `scripts/zap-baseline.sh` refuses non-localhost targets unless `ZAP_AUTHORISED_TARGET=1` is set, and you should only set it with written permission from the system owner. Test accounts are synthetic demo-lab accounts that exist only inside the app process.

## 2. Objective
Check access control, session handling, input validation, security headers and dependency risk against the OWASP Top 10 (2021). Show that the automated checks detect real vulnerabilities, and document findings with remediation guidance.

## 3. Scope
| In scope | Out of scope |
|---|---|
| Demo Store REST API (`/api/**`), `/search` HTML page, `/health`, `/metrics` | Any externally hosted site (saucedemo.com, dummyjson.com, ...) |
| Secure mode (production-like build) and insecure mode (deliberately vulnerable lab) | Infrastructure/OS hardening, TLS configuration (the lab runs plain HTTP) |
| Third-party Java dependencies of all modules (A06) | Denial-of-service and volumetric testing (that belongs to the perf project, on our own app only) |
| Manual verification with Burp Suite Community and browser DevTools | Social engineering, physical security |

## 4. Approach
| Layer | Tool | What it covers | Where |
|---|---|---|---|
| Automated functional security tests | Java 21, REST Assured, TestNG | A01, A03, A04, A05, A07 (23 test cases, 35 executions incl. data-driven) | `src/test/java/com/qaportfolio/security` |
| Passive DAST | OWASP ZAP baseline (Docker) | Headers, cookies, info disclosure, error disclosure | `scripts/zap-baseline.sh`, `zap/rules.tsv` |
| SCA (vulnerable components) | OWASP Dependency-Check Maven plugin | A06: CVEs in dependencies, fail at CVSS >= 7 | `scripts/dependency-scan.sh` |
| Manual | Burp Suite Community, Chrome DevTools | Exploratory IDOR/auth testing, header and cookie inspection | `docs/burp-suite-guide.md`, `docs/devtools-checklist.md` |

**Two-target strategy.** The same test classes run against:
1. **Secure mode** (`testng.xml`): every test must pass. This is the PR gate.
2. **Insecure lab** (`testng-insecure-lab.xml`): failures are expected, and each failure is a detected finding. This proves the tests have teeth (a security test that cannot fail is worthless).

Open defects in the secure build are tagged with the `known-defect` group. They are excluded from the gate and reproduced by `testng-known-defects.xml`.

## 5. Test environment
| Item | Value |
|---|---|
| Application | Demo Store 1.0.0 (Javalin 6.7, Jetty), in-process on ports 7090 (secure) and 7091 (insecure) |
| Test runtime | OpenJDK 26.0.1 (bytecode target 21), Maven 3.9.9 wrapper, macOS (Darwin 25.6) |
| Libraries | REST Assured 5.5.7, TestNG 7.11.0, AssertJ 3.27.3 |
| Scanners | ZAP `ghcr.io/zaproxy/zaproxy:stable`, Dependency-Check 12.1.9 (both need Docker or NVD access) |
| Test data | Demo accounts `customer1`, `customer2` (USER) and `admin` (ADMIN); passwords via env `DEMO_CUSTOMER_PASSWORD` / `DEMO_ADMIN_PASSWORD`, with lab defaults built into the sample app |

## 6. Entry criteria
- `sample-app` builds and `/health` returns `UP`.
- Written scope and authorization (section 1) agreed.
- Test accounts available; no production data in the environment.

## 7. Exit criteria
- `testng.xml` (secure target): 100% pass.
- Insecure lab: every seeded vulnerability is detected by at least one failing test.
- No open finding rated **High** or above in the secure build without an accepted risk or fix date.
- Dependency-Check: no dependency with CVSS >= 7 unless suppressed with justification and review date.
- ZAP baseline: no FAIL-level rule triggered on the secure build.
- Findings documented with severity, reproduction steps, evidence and remediation.

## 8. Severity and priority
Severity follows a simplified CVSS 3.1 base score: Critical 9.0+, High 7.0-8.9, Medium 4.0-6.9, Low 0.1-3.9. Priority (P1-P3) also weighs exploitability in this app's context: P1 = fix before release, P2 = fix in the next sprint, P3 = backlog.

## 9. Risks and limitations
- The insecure lab's vulnerabilities are seeded and known. Real applications need exploratory testing too (see the Burp guide).
- The ZAP baseline is passive only. Active scanning (`zap-full-scan.py`) should only be run against the lab, never a shared environment.
- No TLS in the lab, so HSTS is asserted as a header only.
- Docker and NVD are unavailable on the authoring machine, so the ZAP and Dependency-Check scripts are provided but **were not executed there** (see the execution summary).
