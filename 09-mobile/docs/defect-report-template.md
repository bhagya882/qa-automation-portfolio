# Defect Report Template (mobile)

| Field | Value |
|---|---|
| ID | DEF-MOB-NN |
| Title | <what is wrong, where> |
| Severity / Priority | Critical / High / Medium / Low · P1–P4 |
| App build | My Demo App x.y.z (build N), APK SHA-256 |
| Device / OS | e.g. Pixel 4a emulator, Android 11 (API 30), 1080×2340 @ 440 dpi |
| Network | wifi / cellular / offline / Charles throttled (profile) |
| Automated test | Class#method (group) or "manual" |
| Preconditions | logged in? cart state? permissions granted? |

**Steps to reproduce**
1. …

**Expected result:** …
**Actual result:** …
**Frequency:** always / intermittent (x of y)

**Evidence:** screenshot, screen recording (`adb shell screenrecord`), logcat excerpt (with timestamps), page source XML,
Charles session (.chls) if network-related.
**Notes / suspected cause:** …
