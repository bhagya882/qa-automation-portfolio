# Execution Summary – Mobile (09)

| Item | Value |
|---|---|
| Date | 2026-11-23 |
| Device | Pixel 4a emulator, Android 11 (API 30), headless (`-no-window`) |
| Stack | Appium 3.1.2 · UiAutomator2 6.7.10 · java-client 10.1.1 · TestNG 7.11 |
| App | My Demo App Android 2.3.0 (27) |

## Full run (all groups)

Source: [testng-results-emulator-full.xml](evidence/testng-results-emulator-full.xml)

| Total | Passed | Failed | Blocked | Not run |
|---|---|---|---|---|
| 18 | 13 | 5 | 0 | 0 |

All 5 failures are `known-defect` tests:

| Test | Defect |
|---|---|
| `denyCameraPermission` | DEF-MOB-01 |
| `appSupportsLandscape` | DEF-MOB-02 |
| `clickableElementsAreLabelled` | DEF-MOB-03 |
| `touchTargetsAtLeast48dp` | DEF-MOB-04 |
| `loginInputsLabelled` | DEF-MOB-05 |

## Regression gate (`-DexcludedGroups=known-defect`)

Source: [testng-results-emulator-prgate.xml](evidence/testng-results-emulator-prgate.xml)

| Executed | Passed | Failed |
|---|---|---|
| 13 | 13 | 0 |

Covered by the passing tests:
- install/launch
- terminate/relaunch
- navigation (3)
- **core purchase journey through to order complete**
- locked-out user
- empty-login validation
- device rotation keeps state
- background/foreground
- offline + network recovery
- location permission grant
- content-desc on key controls

## Infrastructure-missing run (Appium stopped)

Source: [testng-results-no-appium-blocked.xml](evidence/testng-results-no-appium-blocked.xml)

| Total | Skipped (blocked) |
|---|---|
| 18 | 18 |

Each test was skipped with the reason `BLOCKED: Appium server not reachable at http://127.0.0.1:4723`.

## Stabilisation notes (test issues fixed, not product defects)

- `addToCart` stays on the details screen. The screen model was corrected.
- UiAutomator2 returns the string `"null"` for unset attributes. The accessibility checks were corrected; before the fix they gave a false pass.
- The permission dialog ignores taps during its entry animation. The test now waits for it to be clickable.
- A single ping after re-enabling the network was flaky (1 of 4 runs). It was replaced with connectivity-service polling, which passed 3 of 3 times.

## Not executed

- iOS / XCUITest (no iOS build)
- BrowserStack (no account)
- Additional AVDs (see the [device matrix](device-compatibility-matrix.md))
- TalkBack manual pass
- Charles Proxy throttling and fault injection
