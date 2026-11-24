# Defect Reports – Mobile (09)

Common environment:
- My Demo App Android 2.3.0 (build 27)
- Pixel 4a emulator, Android 11 (API 30), 1080×2340 @ 440 dpi (density 2.75)
- Appium 3.1.2 + UiAutomator2 6.7.10
- Run on 2026-11-23

---

## DEF-MOB-01 – Camera permission prompt re-appears after "Deny" (QR Code Scanner)

| Field | Value |
|---|---|
| Severity / Priority | **High / P2** |
| Test | `PermissionsTest#denyCameraPermission` (known-defect) |
| Frequency | Always on a fresh install (Appium run, plus 3/3 consecutive prompts in a manual adb repro) |

**Preconditions:** fresh app data (camera permission never decided).

**Steps**
1. Open the menu and select **QR Code Scanner**.
2. The system dialog "Allow My Demo App to take pictures and record video?" appears. Tap **Deny**.

**Expected:** the dialog closes, and the app explains that the camera is required, or returns to the previous screen.
Android must not be asked again in a loop.

**Actual:** logcat shows the denial being recorded
(`GrantPermissionsActivity: Permission grant result … permission=android.permission.CAMERA … result=6`), but the dialog
is displayed again straight away. In the manual repro, the prompt came back after each of 3 consecutive Deny taps.
`dumpsys activity` showed `GrantPermissionsActivity` still resumed, and the app was in the background
(`queryAppState = RUNNING_IN_BACKGROUND`). The user can only escape with the "Deny & don't ask again" variant or with Back.

**Evidence**
- [screenshots/MOB-40_camera_permission_dialog.png](evidence/screenshots/MOB-40_camera_permission_dialog.png)
- [screenshots/MOB-40_after_deny.png](evidence/screenshots/MOB-40_after_deny.png): the dialog is back 3 s after Deny
- [logs/MOB-40_after_deny.logcat.tail.txt](evidence/logs/MOB-40_after_deny.logcat.tail.txt)

**Suspected cause:** `requestPermissions()` is called again in `onResume()` without checking
`shouldShowRequestPermissionRationale()`.

---

## DEF-MOB-02 – App is locked to portrait; landscape is refused

| Field | Value |
|---|---|
| Severity / Priority | **Medium / P3** |
| Test | `DeviceConditionsTest#appSupportsLandscape` (known-defect) |
| Standard | WCAG 2.1 SC 1.3.4 Orientation (AA); Android large-screen quality guidelines |

**Steps:** with the app on the catalog, request landscape (Appium `rotate(LANDSCAPE)`), or rotate the device with auto-rotate on.
**Expected:** the layout adapts to landscape, or portrait is only used where essential.
**Actual:** `Screen rotation cannot be changed to ROTATION_270 after 2000ms. Is it locked programmatically?` The manifest
declares `android:screenOrientation="portrait"` (0x1) on `MainActivity`, `DebugCrashActivity` and `VirtualUsbActivity`
(checked with `aapt dump xmltree`). Users who mount their device in landscape (wheelchair mounts, tablets) can't use the app.
**Positive note:** state survives a device rotation (`deviceRotationKeepsState` passes: quantity 2 and the product are kept).
**Evidence:** [screenshots/MOB-30_device_rotated_landscape.png](evidence/screenshots/MOB-30_device_rotated_landscape.png): the device is rotated and the app stays in portrait.

---

## DEF-MOB-03 – Rating-star buttons have no accessible name

| Field | Value |
|---|---|
| Severity / Priority | **Medium / P2** |
| Test | `AccessibilityTest#clickableElementsAreLabelled` (known-defect) |

**Steps:** catalog screen; inspect the clickable elements (or turn TalkBack on and swipe to the stars below a product).
**Expected:** each star announces e.g. "Rate 3 of 5 stars".
**Actual:** `start1IV`…`start5IV` are `clickable=true`, with an empty `text` and **no `content-desc`** (page source:
[logs/catalog_page_source.xml](evidence/logs/catalog_page_source.xml)). TalkBack can only say "button", or nothing at all.
**Evidence:** [screenshots/MOB-51_unlabeled_clickables.png](evidence/screenshots/MOB-51_unlabeled_clickables.png)

---

## DEF-MOB-04 – Touch targets smaller than 48 × 48 dp

| Field | Value |
|---|---|
| Severity / Priority | **Medium / P3** |
| Test | `AccessibilityTest#touchTargetsAtLeast48dp` (known-defect) |
| Standard | Android accessibility guideline (48 dp); WCAG 2.2 SC 2.5.8 (24 CSS px minimum) |

**Actual (bounds ÷ density 2.75):**

| Control | Size |
|---|---|
| `menuIV` | 30×30 dp |
| `cartRL` | 30×30 dp |
| `sortIV` | **24×24 dp** |
| rating stars `start1IV..5IV` | **20×20 dp** (below even the WCAG 2.5.8 minimum of 24) |

**Impact:** mis-taps for users with motor impairments; the stars are adjacent with no spacing.
**Evidence:** [screenshots/AccessibilityTest_touchTargetsAtLeast48dp.png](evidence/screenshots/AccessibilityTest_touchTargetsAtLeast48dp.png)

---

## DEF-MOB-05 – Login input fields expose no hint or content description

| Field | Value |
|---|---|
| Severity / Priority | **Low / P3** (needs TalkBack confirmation) |
| Test | `AccessibilityTest#loginInputsLabelled` (known-defect) |

**Actual:** `nameET` and `passwordET` have neither `content-desc` nor `hint`. The visible "Username" and "Password" labels are
separate TextViews. If they are not linked with `android:labelFor`, TalkBack announces only "Edit box". `labelFor` is not
visible through UiAutomator2, so confirm manually with TalkBack. If the labels are linked, close this as not a defect.
**Evidence:** [screenshots/AccessibilityTest_loginInputsLabelled.png](evidence/screenshots/AccessibilityTest_loginInputsLabelled.png)
