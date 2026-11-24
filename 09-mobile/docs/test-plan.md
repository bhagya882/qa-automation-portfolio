# Test Plan – Mobile App Quality Engineering (09)

| Item | Detail |
|---|---|
| App under test | My Demo App Android 2.3.0 (build 27), `com.saucelabs.mydemoapp.android`, minSdk 21, targetSdk 31 |
| Source | Public Sauce Labs demo app for automation practice (GitHub release) |
| Window | 18 Oct – 18 Nov 2026; executed 23 Nov 2026 |

## 1. Scope

**In scope:**
- installation and launch
- navigation
- core purchase journey
- runtime permissions (camera, location)
- network interruption and recovery
- orientation changes
- background/foreground
- accessibility (content descriptions, touch-target size, input labels)
- device compatibility

**Out of scope:**
- backend/API behaviour
- payment processing (synthetic test card `4111…` only)
- biometrics
- the Virtual USB, Drawing and crash-reporting features
- performance and battery profiling
- localisation

## 2. Approach

| Type | How |
|---|---|
| Automated functional | Appium/UiAutomator2 screen objects (resource-id / accessibility-id locators, explicit waits), fresh session per test |
| Device conditions | `mobile: shell` (svc wifi/data, `settings` rotation), Appium app-management API (terminate/activate/background) |
| Permissions | `autoGrantPermissions=false`; interact with the real `permissioncontroller` dialog; verify the result via `dumpsys package` |
| Accessibility | Inspect `content-desc`, `hint`, `clickable`, and bounds ÷ display density (Android guideline: 48 dp) |
| Network fault injection | Charles Proxy throttling and map-local (manual, [guide](charles-proxy-setup.md)) |
| Exploratory | adb `uiautomator dump` to map screens before automating |

Tests that reproduce real defects are in the `known-defect` group. The regression gate excludes them.

## 3. Assumptions

- The demo credentials (`bod@example.com` / `10203040`) are displayed on the app's own login screen, so they are not secrets.
- The emulator has internet access, and the Google APIs image provides location.
- One Appium session per device, with no parallel sessions on the same emulator.

## 4. Environment (executed)

| Item | Value |
|---|---|
| Host | macOS 26 arm64, OpenJDK 26 (compiled to release 21) |
| Appium | 3.1.2 server, UiAutomator2 driver 6.7.10, java-client 10.1.1, Selenium 4.50.0 |
| Device | Android Emulator AVD `Pixel_4a`: Android 11 (API 30), google_apis arm64-v8a, 1080×2340, density 440 dpi (2.75) |

## 5. Entry criteria

- The APK has been downloaded (`scripts/download-app.sh`) and passes `unzip -t`.
- The Appium server is reachable (`/status` returns 200), and the device is `adb devices` → `device`.
- Screen objects have been validated against the app version under test.

## 6. Exit criteria

- All planned automated cases have been executed on at least one device.
- The regression gate (`-DexcludedGroups=known-defect`) is 100% green.
- Every failure is triaged into a defect with screenshot, logcat and page source.
- The device matrix has been updated with the results.
- The execution summary is published.

## 7. Risks

| Risk | Mitigation |
|---|---|
| Emulator timing (animations, permission dialog) | Explicit waits; wait for dialog entry; connectivity polled instead of fixed sleeps |
| Permission state persisting between tests | `noReset=false` (app data cleared); each permission test starts from a fresh session |
| Device settings left changed (rotation, network) | `@AfterMethod` restores wifi/data and rotation |
| Infra missing in CI | `SkipException("BLOCKED: …")` so the result is reported as blocked, not failed |
