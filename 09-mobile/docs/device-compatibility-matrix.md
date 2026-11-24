# Device Compatibility Matrix (09)

Legend:
- **Executed**: results recorded
- **Planned**: configuration ready, not run
- **N/A**: not supported

| # | Device / AVD | OS (API) | Form factor | Density | Where | Status | Result |
|---|---|---|---|---|---|---|---|
| 1 | Pixel 4a (AVD `Pixel_4a`) | Android 11 (30) | Phone 6.2" | 440 dpi | Local emulator | **Executed 2026-11-23** | Regression 13/13 pass; 5 known defects |
| 2 | Pixel (AVD `Pixel`) | Android 8.0 (26) | Phone 5" | 420 dpi | Local emulator | Planned | – |
| 3 | Pixel 5 (AVD `Pixel_5_API_36`) | Android 16 (36) | Phone 6" | 440 dpi | Local emulator | Planned | – |
| 4 | Google Pixel 8 | Android 14 (34) | Phone | 420 dpi | BrowserStack (`browserstack.device`) | Planned (needs account) | – |
| 5 | Samsung Galaxy S23 | Android 13 (33) | Phone | 480 dpi | BrowserStack | Planned | – |
| 6 | Samsung Galaxy Tab S8 | Android 12 (31) | Tablet 11" | 274 dpi | BrowserStack | Planned – check portrait lock on tablets (DEF-MOB-02) | – |
| 7 | iPhone 16 Simulator | iOS 18 | Phone | @3x | Local Xcode (`-Dplatform=ios`) | Planned (needs the iOS build of My Demo App) | – |
| 8 | Android 4.x (API < 21) | – | – | – | – | N/A: below the app's minSdk 21 | – |

To run a row:
- **Emulator:** `-Ddevice.name=<adb serial> -Dplatform.version=<x>`
- **BrowserStack:** set `BROWSERSTACK_USERNAME` / `BROWSERSTACK_ACCESS_KEY` / `BROWSERSTACK_APP`, plus
  `-Dbrowserstack.device="Samsung Galaxy S23" -Dbrowserstack.os.version=13.0`
