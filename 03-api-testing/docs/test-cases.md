# Test Cases — Project 03

Automated in `src/test/java/com/qaportfolio/api/tests/` (IDs appear in each `@Test(description=…)`) and
mirrored in the Postman collection. P = priority (1 = highest).

| ID | Area | Title | Steps / data | Expected | P | Type |
|---|---|---|---|---|---|---|
| TC-API-001 | Auth | Valid login | POST /auth/login `{username, password, expiresInMins:30}` | 200. Matches `login-response.json`. accessToken is a 3-part JWT, refresh ≠ access | 1 | Positive |
| TC-API-002 | Auth | Current user with token | GET /auth/me + Bearer | 200, same id/username as login | 1 | Positive |
| TC-API-003 | Auth | /auth/me with missing / garbage token | No header; `Bearer garbage` | 401 with `Access Token is required` / `Invalid/Expired Token!` | 1 | Negative |
| TC-API-004 | Auth | Refresh token | POST /auth/refresh `{refreshToken}` | 200 with new tokens. New access token works on /auth/me | 2 | Positive |
| TC-API-005 | Auth | Login with missing fields | 4 data rows: none / no password / no username / empty | 400 `Username and password required` | 1 | Validation |
| TC-API-006 | Auth | Wrong password | wrong password | 4xx, no token, `Invalid credentials` | 1 | Negative |
| TC-API-007 | Auth | Auth cookie flags | login | Set-Cookie has HttpOnly + Secure | 2 | Security |
| TC-API-010 | Products | Default list | GET /products | 200, schema, limit 30, 30 items | 1 | Positive |
| TC-API-011 | Products | Headers | GET /products/1 | JSON content type, x-ratelimit-* present | 3 | Contract |
| TC-API-012 | Products | Pagination | 5 rows from `pagination.json` (limit/skip) | Correct slice size, first id = skip+1 | 2 | Data-driven |
| TC-API-013 | Products | Field selection | `select=title,price` | Only id, title, price returned | 3 | Positive |
| TC-API-014 | Products | Sort | `sortBy=price&order=asc&limit=50` | Prices ascending | 2 | Positive |
| TC-API-015 | Products | Search | 4 rows from `search.json`, including a no-match term | total ≥ expected; no-match returns 0 | 2 | Data-driven |
| TC-API-016 | Products | Product by id | GET /products/1 | 200, matches `product.json`, price > 0 | 1 | Contract |
| TC-API-017 | Products | Unknown id | 99999, 0, -1 | 404 `Product with id '<id>' not found` | 2 | Negative |
| TC-API-018 | Products | Categories | /products/category-list, /products/category/laptops | List contains known slugs; every product's category = laptops | 2 | Positive |
| TC-API-019 | Products | Invalid paging | limit=-1, limit=abc, skip=-5 | 400 with specific message | 2 | Validation |
| TC-API-020 | Products | Add product | POST /products/add | 201, echoes title/price, new id | 2 | Positive |
| TC-API-021/022 | Products | Update PUT / PATCH | title / price | 200 with updated values | 2 | Positive |
| TC-API-023 | Products | Delete | DELETE /products/3 | 200, isDeleted=true, deletedOn set | 2 | Positive |
| TC-API-024 | Products | Update unknown | PUT /products/99999 | 404 | 3 | Negative |
| TC-API-025 | Products | Unsupported method | POST /products/1 | 4xx | 3 | Negative |
| TC-API-030 | Carts | Cart totals | GET /carts/1 | Schema. line total = price × qty; cart total = Σ lines; totalQuantity = Σ qty; discounted ≤ total | 1 | Business rule |
| TC-API-031 | Carts | Carts of logged-in user | GET /carts/user/{id} | Every cart has that userId | 2 | Positive |
| TC-API-032 | Carts | Carts of unknown user | /carts/user/99999 | 404 + message | 3 | Negative |
| TC-API-033 | Carts | Add cart | 3 data rows (1–3 lines) | 201. Line totals and quantities computed correctly | 1 | Data-driven |
| TC-API-034 | Carts | Empty cart | products: [] | 400 `products can not be empty` | 2 | Validation |
| TC-API-035 | Carts | Merge update | PUT /carts/1 merge=true | totalProducts + 1 | 2 | Positive |
| TC-API-036 | Carts | Delete cart | DELETE /carts/1 | isDeleted=true | 3 | Positive |
| TC-API-037 | Carts | Malformed body | products is a string | 4xx | 3 | Validation |
| TC-API-040..044 | Users | By id (schema), list+select, filter by nested key, search, unknown id | — | 200 / correct filtering / 404 | 2–3 | Mixed |

Defect-reproduction tests (group `known-defect`) are listed in [defects/](defects/).
