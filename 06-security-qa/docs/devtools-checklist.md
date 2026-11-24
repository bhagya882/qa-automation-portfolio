# Browser DevTools Security Checklist (Chrome/Edge/Firefox)

Target: `http://localhost:7070` (secure) / `http://localhost:7071` (insecure lab). Record the result per mode.

| # | Check | DevTools location | Pass condition | Secure | Lab |
|---|---|---|---|---|---|
| 1 | Security headers on the document | Network > `search?q=x` > Headers > Response | CSP, X-Frame-Options, X-Content-Type-Options, Referrer-Policy, Permissions-Policy, HSTS | ☐ | ☐ |
| 2 | No version disclosure | Same, `Server` header | Absent / generic | ☐ | ☐ |
| 3 | CSP actually enforced | Console: open `/search?q=<img src=x onerror=alert(1)>` | Encoded text, no alert, no CSP violation needed | ☐ | ☐ |
| 4 | Cookie attributes | Application > Cookies > `SESSION` (after a login via the Console `fetch`) | HttpOnly ✓, Secure ✓, SameSite=Strict | ☐ | ☐ |
| 5 | Cookie not readable by JS | Console: `document.cookie` | `SESSION` not listed | ☐ | ☐ |
| 6 | Token not stored in Web Storage | Application > Local/Session Storage | No bearer token stored | ☐ | ☐ |
| 7 | API not cacheable | Network > `/api/products` > `Cache-Control` | `no-store` | ☐ | ☐ |
| 8 | Error responses generic | Console: `fetch('/api/auth/login',{method:'POST',body:'{bad'}).then(r=>r.text()).then(console.log)` | No stack trace or class names | ☐ | ☐ |
| 9 | Clickjacking | Console on any other origin: create an `<iframe src="http://localhost:7070/search">` | Refused (XFO DENY / frame-ancestors) | ☐ | ☐ |
| 10 | Mixed content / HTTP | Security panel | Lab runs HTTP only. Note it as an environment limitation; production must be HTTPS | n/a | n/a |

Console snippet to log in and see the cookie (lab demo account only):
```js
fetch('/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},
  body:JSON.stringify({username:'customer1',password:'<demo password>'})}).then(r=>r.json()).then(console.log)
```
