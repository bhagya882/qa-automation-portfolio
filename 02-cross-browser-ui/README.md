# 02 – Cross-Browser and Responsive UI Testing (Java · Selenium · TestNG)

Validates the key SauceDemo journeys across **browsers × viewports × emulated devices** and reports layout problems,
broken controls and inconsistent behaviour with screenshots. It reuses the page objects from
[project 04](../04-e2e-framework) (Maven dependency `04-e2e-framework`).

## What is checked, per browser/viewport combination
| Test | Checks |
|---|---|
| `loginPageLayout` | no horizontal overflow; username field and login button visible, inside the viewport, not covered |
| `purchaseJourney` | login → add 2 items → cart → checkout → overview (total = items + tax) → confirmation; layout checks + screenshot on every step |
| `sortingByPrice` | price ↑, price ↓ and name Z→A sort behave identically in every browser |
| `burgerMenuReachable` | burger button usable; every menu link fully on-screen after the slide-in; logout works |
| `productImagesLoad` | every `<img>` loaded (`naturalWidth > 0`) |
| `touchTargetsLargeEnough` *(known-defect)* | burger, cart and Add-to-cart targets ≥ 24×24 CSS px (WCAG 2.2 SC 2.5.8) |
| `visualUserLayoutMatchesBaseline` *(known-defect)* | bounding boxes of key elements and image sources for `visual_user` vs `standard_user` baseline (±2 px) |

The layout probes are in [`Layout.java`](src/test/java/com/qaportfolio/crossbrowser/Layout.java). They use plain DOM/JS (`scrollWidth`,
`getBoundingClientRect`, `elementFromPoint`), so they work the same way in every browser.

## Suites
| File | Matrix | Use |
|---|---|---|
| `testng.xml` | Chrome + Firefox × 1366x768 + 390x844 | PR gate (headless) |
| `testng-matrix.xml` | wraps `testng-matrix-parallel.xml` (Chrome/Firefox/Edge × 1920x1080, 1366x768, 768x1024, 390x844 + Chrome emulation of "iPhone 14 Pro Max" and "Pixel 7") **and** `testng-safari.xml` (Safari × 4 viewports, run one after another) | nightly / release |

Each `<test>` passes `browser`, `viewport` and `device` parameters to qa-core `BaseUiTest`.

## How to run (from repo root)
```bash
./mvnw -q -pl qa-core,04-e2e-framework install -DskipTests      # once

# PR suite
./mvnw -pl 02-cross-browser-ui test -Dheadless=true -DexcludedGroups=known-defect

# full matrix (incl. Safari)
./mvnw -pl 02-cross-browser-ui test -Dheadless=true -Dsuite=testng-matrix.xml -DexcludedGroups=known-defect

# visual / touch-target defects (expected to fail)
./mvnw -pl 02-cross-browser-ui test -Dheadless=true -Dgroups=known-defect -DtestFailureIgnore=true
```
Screenshots: `02-cross-browser-ui/target/screenshots/<step>_<browser>@<viewport>.png`. Failure evidence: `target/evidence/`.

### Browser setup
* **Chrome / Firefox / Edge**: Selenium Manager resolves the drivers, and **downloads the browser if it is missing**
  (on this Mac it downloaded Firefox 160.0.1 and Edge 158.0.4370.33 into `~/.cache/selenium`). The first parallel launch can
  race while the download is still running. A browser that can't start marks its tests **BLOCKED** (TestNG skip
  with reason) instead of failing the build.
* **Safari**: macOS only, no headless mode, one session at a time. Enable it once with `sudo safaridriver --enable`.
* **BrowserStack / Selenium Grid**: `-Dgrid.url=https://hub.browserstack.com/wd/hub` with credentials supplied through
  CI secrets/env vars (`GRID_URL`). Never commit credentials. The same suites then run on real devices and browsers.

## Documentation
* [Test plan](docs/test-plan.md) · [Defects](docs/defects.md) · [Execution summary](docs/execution-summary.md) · [Evidence](docs/evidence)

## Known limitations
* Desktop Chrome/Edge won't make a window narrower than 500 px. For Chromium the test pins the exact CSS viewport with
  CDP `Emulation.setDeviceMetricsOverride`. **Firefox can't do this without BiDi**, so "Firefox 390x844" actually renders at
  **500x759** (recorded as the `actual viewport` Allure parameter). Real phone widths in Firefox need BrowserStack or
  qa-core BiDi `setViewport` support.
* Chrome device emulation is not a real iOS/Android browser (no WebKit/Safari engine). Use Safari or BrowserStack devices for that.
* Layout checks are rule-based, not pixel-diffing; subtle colour or font regressions are out of scope.
* SauceDemo is third-party; its layout can change without notice.
