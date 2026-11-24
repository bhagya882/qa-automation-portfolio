# Raw HTTP evidence (curl) — captured 2026-11-23 00:17 IST

Insecure lab: `java -jar sample-app/target/sample-app-1.0.0-all.jar --insecure` on port 7095. Secure build on port 7096 for comparison. Demo-lab accounts only.

## INSECURE LAB (7095)

### SEC-001 IDOR — customer2 reads customer1's order 1001
```
$ curl -i localhost:7095/api/orders/1001 -H 'Authorization: Bearer <customer2-token>'
HTTP/1.1 200 OK
{"id":1001,"username":"customer1","lines":[{"productId":7,"name":"Mini Speaker 7","quantity":1,"unitPrice":69.99,"lineTotal":69.99}],"total":69.99,"createdAt":"2026-11-22T18:47:25.118738Z"}
```
### SEC-002/003 Response headers on /api/products
```
$ curl -s -D - -o /dev/null localhost:7095/api/products
HTTP/1.1 200 OK
Content-Type: application/json
Server: DemoStore/0.1 (Javalin; Java 26.0.1)

```
### SEC-004 Session cookie flags
```
$ curl -si -X POST localhost:7095/api/auth/login -d '{"username":"customer1","password":"<demo>"}' | grep -i set-cookie
Set-Cookie: SESSION=<redacted-token>; Path=/
```
### SEC-005 Reflected XSS on /search
```
$ curl -s 'localhost:7095/search?q=<script>alert(1)</script>' | grep 'Results for'
<p>Results for: <script>alert(1)</script></p>
```
### SEC-006/007 Malformed JSON (stack trace leak / HTTP 500)
```
$ curl -si -X POST localhost:7095/api/auth/login -d '{bad json'
HTTP/1.1 500 Server Error
{"trace":"com.fasterxml.jackson.core.JsonParseException: Unexpected character ('b' (code 98)): was expecting double-quote to start field name\n at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 2]\n  at com.fasterxml.jackson.core.JsonParser._constructReadException(JsonParser.java:2660)\n  at com.fasterxml.jackson.core.base.ParserMinimalBase._reportUne
```
### SEC-008 Brute force — 6 wrong passwords for one username
```
attempt 1 -> HTTP 401
attempt 2 -> HTTP 401
attempt 3 -> HTTP 401
attempt 4 -> HTTP 401
attempt 5 -> HTTP 401
attempt 6 -> HTTP 401
```

## SECURE BUILD (7096)

### SEC-001 IDOR — customer2 reads customer1's order 1001
```
$ curl -i localhost:7096/api/orders/1001 -H 'Authorization: Bearer <customer2-token>'
HTTP/1.1 404 Not Found
{"error":"Order not found"}
```
### SEC-002/003 Response headers on /api/products
```
$ curl -s -D - -o /dev/null localhost:7096/api/products
HTTP/1.1 200 OK
Content-Type: application/json
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Content-Security-Policy: default-src 'self'; frame-ancestors 'none'; form-action 'self'
Strict-Transport-Security: max-age=31536000; includeSubDomains
Permissions-Policy: camera=(), microphone=(), geolocation=()
Cache-Control: no-store

```
### SEC-004 Session cookie flags
```
$ curl -si -X POST localhost:7096/api/auth/login -d '{"username":"customer1","password":"<demo>"}' | grep -i set-cookie
Set-Cookie: SESSION=<redacted-token>; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=1800
```
### SEC-005 Reflected XSS on /search
```
$ curl -s 'localhost:7096/search?q=<script>alert(1)</script>' | grep 'Results for'
<p>Results for: &lt;script&gt;alert(1)&lt;/script&gt;</p>
```
### SEC-006/007 Malformed JSON (stack trace leak / HTTP 500)
```
$ curl -si -X POST localhost:7096/api/auth/login -d '{bad json'
HTTP/1.1 500 Server Error
{"error":"Internal server error"}
```
### SEC-008 Brute force — 6 wrong passwords for one username
```
attempt 1 -> HTTP 401
attempt 2 -> HTTP 401
attempt 3 -> HTTP 401
attempt 4 -> HTTP 401
attempt 5 -> HTTP 401
attempt 6 -> HTTP 429
```
