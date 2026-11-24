# Charles Proxy with the Android Emulator

Charles Proxy lets you inspect, throttle and fault-inject the app's HTTP(S) traffic. Use it to cover what `svc wifi`
can't: slow networks, timeouts and server errors.

## 1. Point the emulator at Charles

1. Start Charles (it listens on port 8888 by default). Open *Proxy → Proxy Settings* and enable *HTTP proxy*.
2. Start the emulator with the proxy set:
   ```bash
   ~/Library/Android/sdk/emulator/emulator -avd Pixel_4a -http-proxy http://127.0.0.1:8888
   ```
   Alternatively, on a running emulator: *Extended controls (…) → Settings → Proxy → Manual: 127.0.0.1:8888*. Or use adb:
   `adb shell settings put global http_proxy 10.0.2.2:8888` (10.0.2.2 is the host as seen from the emulator).
   To undo: `adb shell settings put global http_proxy :0`.

## 2. Trust the Charles root certificate (HTTPS)

1. Charles: *Help → SSL Proxying → Save Charles Root Certificate…* → `charles.pem`.
2. `adb push charles.pem /sdcard/Download/`, then on the device go to *Settings → Security → Encryption & credentials → Install a
   certificate → CA certificate*.
3. Android 7+ apps trust user CAs only if their `network_security_config` allows it. If the app's release build doesn't
   allow user CAs, use a debug build, or a rooted/`google_apis` image with the certificate installed as a system CA.
   Never do this on a personal device.
4. Charles: *Proxy → SSL Proxying Settings → Add* the hosts to decrypt (e.g. `*.saucelabs.com`, plus the WebView target host).

## 3. Throttling (slow networks)

*Proxy → Throttle Settings* → enable, preset **3G** or a custom profile (e.g. 400 ms latency, 256 kbps, 5% packet loss).

Re-run the journey, `./mvnw -pl 09-mobile test -Dtest=CoreJourneyTest`, and record:
- whether spinners or loading states appear
- that there are no ANRs (`adb logcat | grep ANR`)
- that no timeouts happen below the agreed threshold

## 4. Fault injection

| Tool | Use |
|---|---|
| *Tools → Map Local* | Serve a local JSON file (e.g. an empty or malformed product list) for an endpoint. Check that the app shows an error state, not a crash |
| *Tools → Rewrite* | Change the status to `500`/`503`, or strip headers |
| *Tools → Breakpoints* | Pause a request so you can test cancellation and timeouts |
| *Tools → Block List* | Simulate an unreachable host |

## 5. Evidence

Save the session (*File → Save Session* → `.chls`) and attach it to the defect, together with logcat and a screenshot. Remove
cookies and tokens before sharing. Never commit sessions that contain real credentials.

> Status: guide prepared. Charles was not used in the 2026-11-23 run. Network interruption was covered by the automated
> `svc wifi/data` test (MOB-32).
