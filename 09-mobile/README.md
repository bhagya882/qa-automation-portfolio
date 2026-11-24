# 09 – Mobile App Quality Engineering Suite (Java + Appium)

This suite tests Sauce Labs' public **My Demo App** for Android
([saucelabs/my-demo-app-android](https://github.com/saucelabs/my-demo-app-android), v2.3.0 build 27), an app built for
test-automation practice.

| Area | Tooling |
|---|---|
| Automation | **Appium 3** server, UiAutomator2 driver, **java-client 10.1.1**, TestNG 7, AssertJ |
| Devices | Android Emulator (verified: Pixel 4a AVD, Android 11 / API 30, arm64); real devices via adb; **BrowserStack** via env vars; iOS XCUITest options (simulator) |
| Network | `mobile: shell` → `svc wifi/data` (needs `appium --relaxed-security`); Charles Proxy for throttling and fault injection ([guide](docs/charles-proxy-setup.md)) |
| Evidence | On failure: screenshot, page-source XML and **logcat** → `target/evidence/` + Allure |

## Setup

```bash
09-mobile/scripts/download-app.sh                    # APK → 09-mobile/apps/ (gitignored, never committed)
npm i -g appium && appium driver install uiautomator2
appium --address 127.0.0.1 --port 4723 --relaxed-security   # relaxed security: needed for the network/rotation shell tests
~/Library/Android/sdk/emulator/emulator -avd Pixel_4a -no-window -no-audio   # or plug in a device
```

## Run

```bash
./mvnw -pl 09-mobile test                                 # full suite (known-defect tests fail on purpose)
./mvnw -pl 09-mobile test -DexcludedGroups=known-defect   # regression gate
./mvnw -pl 09-mobile test -Ddevice.name=emulator-5556 -Dplatform.version=14
./mvnw -pl 09-mobile test -Dplatform=ios                  # needs Xcode simulator + iOS build in apps/MyDemoApp.app
BROWSERSTACK_USERNAME=... BROWSERSTACK_ACCESS_KEY=... BROWSERSTACK_APP=bs://... ./mvnw -pl 09-mobile test
```

If no Appium server is reachable, or the APK is missing, every test is **skipped with a `BLOCKED:` reason** instead of
failing. This keeps CI runners without emulators honest.

## Test inventory

| Class | IDs | Covers |
|---|---|---|
| `InstallLaunchTest` | MOB-01..02 | Installed, launches to MainActivity, terminate/relaunch |
| `NavigationTest` | MOB-10..12 | Catalog → details → back, side menu, empty cart |
| `CoreJourneyTest` | MOB-20..22 | **Core journey**: product → qty 2 → cart → login → address → payment → review → order complete; locked-out user; empty login validation |
| `DeviceConditionsTest` | MOB-30..33 | Device rotation keeps state; background/foreground; offline + recovery; landscape support (known-defect) |
| `PermissionsTest` | MOB-40..41 | Camera deny (known-defect), location grant (`autoGrantPermissions=false`) |
| `AccessibilityTest` | MOB-50..53 | content-desc on key controls; unlabeled clickables, 48 dp targets, input labels (known-defect) |

## Documentation

- [Test plan](docs/test-plan.md)
- [Device compatibility matrix](docs/device-compatibility-matrix.md)
- [Defect reports](docs/defects.md) and [defect template](docs/defect-report-template.md)
- [Charles Proxy setup](docs/charles-proxy-setup.md)
- [Execution summary](docs/execution-summary.md)

## Known limitations

- Only one device has been executed (Pixel 4a emulator, API 30). The other rows of the matrix are planned.
- iOS and BrowserStack code paths compile, but have **not been executed** (no iOS build or BrowserStack account).
- The network test toggles wifi and data with `svc`. Airplane-mode broadcasts and captive portals are not covered.
  Use Charles Proxy for latency and error injection.
- `noReset=false` resets app data for each test. A full reinstall is only done on first run.
- The parent POM manages java-client 9.5.0, which is incompatible with Selenium 4.50 (it references the removed
  `org.openqa.selenium.html5` package). This module overrides the version to 10.1.1.
