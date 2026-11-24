# Manual Testing with Burp Suite Community: Demo Store lab

> Use Burp only against the local Demo Store, i.e. your own lab. Never point it at third-party sites.

## 1. Setup
1. Start both targets from the repo root:
   ```bash
   PORT=7070 java -jar sample-app/target/sample-app-1.0.0-all.jar              # secure
   PORT=7071 java -jar sample-app/target/sample-app-1.0.0-all.jar --insecure   # vulnerable lab
   ```
2. Burp Suite Community: **Proxy > Proxy settings**. Confirm the listener is on `127.0.0.1:8080`.
3. Use Burp's built-in Chromium (**Proxy > Intercept > Open browser**), or send API calls through the proxy with curl:
   `curl -x http://127.0.0.1:8080 http://localhost:7071/api/products`
4. **Target > Scope**: add `http://localhost:7070` and `http://localhost:7071`. Tick *"Show only in-scope items"* in the HTTP history filter.

## 2. Repeater: IDOR test (SEC-001)
1. Through the proxy, log in as `customer1`:
   `POST /api/auth/login` with body `{"username":"customer1","password":"<demo password>"}`
2. Add an item to the cart and place an order (`POST /api/cart/items`, then `POST /api/orders`). Note `"id": 1001`.
3. Log in as `customer2` and copy its `token`.
4. In HTTP history, right-click the customer1 `GET /api/orders/1001` request and choose **Send to Repeater**.
5. In Repeater, replace the `Authorization: Bearer ...` value with **customer2's** token and send.
   - Lab (7071): `200` with customer1's order, so **IDOR confirmed**.
   - Secure (7070): `404 {"error":"Order not found"}`.
6. Change the id to 1000, 1002, ... to show the IDs are enumerable.

## 3. "Intruder-lite": brute-force/throttling (SEC-008)
Community Edition's Intruder is rate-limited, but that is enough for a small attempt list.
1. Send `POST /api/auth/login` to **Intruder**. Mark the password value as the payload position (`§wrong§`).
2. Payloads: simple list `wrong1` to `wrong8`. Attack type: *Sniper*.
3. Start the attack and compare status codes:
   - Lab: all `401`, so there is **no throttling**.
   - Secure: `401` x5, then `429 Too Many Requests`.
4. Use an unknown username such as `probe-xyz` so no real account gets locked.

## 4. Other quick checks
| Check | How | Expected (secure) |
|---|---|---|
| Reflected XSS (SEC-005) | Repeater: `GET /search?q=<svg/onload=alert(1)>`; then **Show response in browser** | Payload is HTML-encoded |
| Headers (SEC-002/003) | Inspect any response in Proxy > HTTP history > Response > Headers | CSP, XFO, nosniff, HSTS, Referrer-Policy present; no versioned `Server` |
| Cookie flags (SEC-004) | Login response `Set-Cookie` | `HttpOnly; Secure; SameSite=Strict` |
| Error handling (SEC-006/007) | Repeater: send the login body `{bad` | Generic body (secure); expected 400, currently 500 (SEC-007) |
| Token reuse after logout | Repeater: `POST /api/auth/logout`, then replay `GET /api/cart` | 401 |
| Privilege escalation | Repeater: `GET /api/admin/users` with a customer token | 403 |

## 5. Recording evidence
Save each issue with Burp's **Save item** (request + response), or copy the raw request/response into the finding. Redact bearer tokens before committing (`Bearer <redacted>`).
