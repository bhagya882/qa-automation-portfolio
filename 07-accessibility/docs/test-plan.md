# Test Plan – Accessibility and Inclusive UX Audit

| Item | Detail |
|---|---|
| Project | 07 – Accessibility and Inclusive UX Audit |
| Application under test | SauceDemo "Swag Labs" (https://www.saucedemo.com), public automation-practice site |
| Standard | WCAG 2.2 Level AA (plus axe best-practice rules for information) |
| Test window | 18 Oct 2026 – 18 Nov 2026; automated run re-executed 23 Nov 2026 |

## 1. Scope

**In scope:** the end-to-end purchase workflow:

1. Login
2. Inventory (product list, sort, burger menu)
3. Product details
4. Cart
5. Checkout step one (customer information form)
6. Checkout step two (overview)
7. Checkout complete

Quality attributes covered:
- keyboard operability
- focus order and visibility
- labels and accessible names
- colour contrast
- semantic structure (landmarks, headings, `lang`, titles)
- text alternatives
- reflow at 320 CSS px
- screen-reader announcements

**Out of scope:**
- the "About" link, which goes to saucelabs.com
- the other demo user personas (`problem_user` and so on). These are functional fault-injection users.
- native mobile apps
- PDF and other documents
- performance

## 2. Approach

| Layer | Method | Tool |
|---|---|---|
| Automated rules | axe-core scan on every page, compared with a baseline | axe-core 4.10 / Selenium / TestNG |
| Automated behaviour | Keyboard-only journeys, focus-style inspection via computed CSS, DOM semantics | Selenium Actions API + JS |
| Page score | Lighthouse accessibility category | Lighthouse 12 |
| Manual | WCAG 2.2 AA checklist, WAVE visual review, VoiceOver/NVDA script | Browser extensions, screen readers |

Real defects are reproduced by tests in the TestNG group `known-defect`, which is excluded from the PR gate. The gate
fails only on regressions: new axe violations, or loss of keyboard access that works today.

## 3. Assumptions

- SauceDemo is intended for public automation practice. The scans are read-only and low-volume.
- The demo credentials (`standard_user` / `secret_sauce`) are shown on the login page itself, so they are not secrets.
- Desktop Chrome (headless) is the reference browser. Firefox is supported via `-Dbrowser=firefox`.
- The site's markup is stable during the test window.

## 4. Environment

| Component | Version |
|---|---|
| OS | macOS 26 (arm64) |
| Browser | Chrome 157 (headless=new), window 1440×900; 320×800 for the reflow test |
| Java / build | OpenJDK 26 runtime, compiled to release 21; Maven 3.9.9 wrapper |
| Libraries | Selenium 4.50.0, TestNG 7.11.0, axe-core Selenium 4.10.1, Allure 2.35.6 |
| Lighthouse | 12.2.1 via npx (Node 20) |
| Screen readers (manual) | VoiceOver (macOS), NVDA 2024+ with Firefox/Chrome (Windows) |

## 5. Entry criteria

- The target site is reachable (HTTP 200 on `/`).
- qa-core is built and installed, and the module compiles.
- The test data (demo user) can log in.
- The baseline file reflects the last triaged scan.

## 6. Exit criteria

- All seven pages have been scanned by axe, and the report is generated.
- All keyboard and semantics test cases have been executed, with results recorded.
- The PR-gate suite (`-DexcludedGroups=known-defect`) passes: zero new violations and no keyboard regressions.
- Every failure has been triaged into a defect report with evidence, or fixed as a test issue.
- Manual checklist items have each been marked Pass, Fail or N/A.
- The execution summary is published.

## 7. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Third-party site changes markup | Baseline keyed by page and rule; locators use `id`/`data-test` |
| Animation or timing flakiness (burger menu) | Explicit waits for the menu to be fully on-screen; retry analyzer available (`-Dretry.count=1`) |
| Automated tools under-report | Manual checklist and screen-reader script are part of the exit criteria |

## 8. Deliverables

- Test cases ([test-cases.md](test-cases.md))
- Automated suite
- axe/Lighthouse reports
- Defect reports ([defects.md](defects.md))
- WCAG checklist
- Screen-reader script
- Execution summary
