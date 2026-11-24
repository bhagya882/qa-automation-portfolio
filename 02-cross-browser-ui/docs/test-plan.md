# Test Plan – Project 02: Cross-Browser and Responsive UI Testing

| Item | Value |
|---|---|
| AUT | SauceDemo https://www.saucedemo.com (public automation-practice site) |
| Type | Automated compatibility + responsive layout testing |
| Tools | Java 21, Selenium 4.50 (Selenium Manager), TestNG 7.11 (parameterised `<test>` matrix), AssertJ soft assertions, Allure, Chrome DevTools Protocol |

## 1. Objective
Show that the critical journeys (login, add to cart, checkout, sorting, navigation menu) work and look correct on
supported browsers and screen sizes, and report layout issues, broken controls, accessibility concerns and inconsistent
behaviour with screenshots.

## 2. Scope
**Browsers:** Chrome (Blink), Firefox (Gecko), Edge (Blink), Safari (WebKit).
**Viewports:** 1920x1080 (desktop FHD), 1366x768 (laptop), 768x1024 (tablet portrait), 390x844 (phone).
**Device emulation:** iPhone 14 Pro Max, Pixel 7 (Chrome DevTools emulation).
**Journeys:** login page, purchase journey, sorting, burger menu/logout, image loading, touch-target size, visual baseline
comparison (`visual_user` vs `standard_user`).
**Out of scope:** real mobile devices/native apps (project 09), full WCAG audit (project 07), pixel-perfect visual diffs.

## 3. Approach
* Same test classes, different `<test>` parameters (browser/viewport/device), run in parallel (`parallel="tests"`).
* Rule-based layout oracles: no horizontal overflow; key controls inside the viewport and not covered by
  another element (`elementFromPoint`); menu links on-screen; touch targets ≥ 24 px; per-element bounding-box comparison
  against a baseline user.
* Screenshot at every journey step for visual review (attached to Allure).
* Unavailable browser ⇒ **Blocked** (skip with reason), not Failed.

## 4. Assumptions
Selenium Manager can download drivers and browsers. The site layout is the same for all users except the seeded
`visual_user`/`problem_user` defects. Demo credentials are public.

## 5. Environment
| | Local | CI (GitHub Actions) |
|---|---|---|
| OS | macOS 26 arm64 | ubuntu-latest |
| Chrome | 157.0.8103.79 | stable |
| Firefox | 160.0.1 (downloaded by Selenium Manager) | preinstalled |
| Edge | 158.0.4370.33 (downloaded by Selenium Manager) | preinstalled |
| Safari | installed, remote automation **not enabled** | n/a |

## 6. Entry criteria
Site reachable; project 04 page objects built; at least Chrome available.

## 7. Exit criteria
* PR suite: 100% pass on Chrome + Firefox (desktop and phone).
* Matrix: no failures on available browsers. Blocked combinations are listed with the reason and the follow-up action.
* Every layout/visual defect is documented with screenshots.

## 8. Risks
Animations cause false "outside viewport" results, so the tests wait for the menu transition, fonts and images to finish.
Desktop windows have a minimum size, so viewports are pinned through CDP (Chromium) and the actual viewport is recorded.
