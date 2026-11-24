# Defect Log – Project 01

Found on 2026-11-23 · Chrome 157.0.8103.79 / macOS 26 arm64 · curl 8.7.1. Format: [defect-template.md](defect-template.md).

| ID | Title | Site | Severity | Priority | Test case | Status |
|---|---|---|---|---|---|---|
| DEF-01-001 | Last Name cannot be entered at checkout (problem_user) | SD | Blocker | P1 | TC-CO-07 | Open |
| DEF-01-002 | Finish does not place the order; console TypeError (error_user) | SD | Blocker | P1 | TC-CO-08 | Open |
| DEF-01-003 | Sorting by price has no effect (problem_user) | SD | Major | P2 | TC-SRCH-08 | Open |
| DEF-01-004 | Clicking a product opens a different product (problem_user) | SD | Critical | P1 | TC-PD-04 | Open |
| DEF-01-005 | Only 3 of 6 products can be added to cart (problem_user, error_user) | SD | Critical | P1 | TC-CART-07/08, TC-DT-02 | Open |
| DEF-01-006 | API returns HTTP 200 for error responses (status only in JSON body) | AE | Major | P2 | TC-LOG-12/13, TC-SRCH-05 | Open |
| DEF-01-007 | Unknown product id returns HTTP 200 with a blank product page | AE | Minor | P3 | TC-PD-06 | Open |
| DEF-01-008 | Mixed-content: Google Fonts stylesheets requested over http are blocked on every page | AE | Minor | P3 | TC-DT-03 | Open |
| DEF-01-009 | Search with no results shows no "no products found" message | AE | Minor | P3 | TC-SRCH-04 | Open |
| DEF-01-010 | /checkout renders full checkout (incl. Place Order) for an anonymous user with an empty cart | AE | Major | P2 | TC-CO-09 | Open |

SauceDemo defects DEF-01-001…005 are the functional view of the automated defects DEF-04-007, DEF-04-008, DEF-04-002,
DEF-04-004 and DEF-04-005/006 in [project 04](../../04-e2e-framework/docs/defects.md), where the full automated evidence is kept.

---

### DEF-01-001 – Last Name cannot be entered at checkout (problem_user)
* **Severity / Priority:** Blocker / P1. The user can't complete a purchase.
* **Environment:** https://www.saucedemo.com · Chrome 157 · macOS 26
* **Preconditions:** logged in as `problem_user` / `secret_sauce` (public demo account), with 1 item in the cart
* **Steps:** 1. Cart → Checkout 2. Type a First Name, a Last Name and a Postal Code
* **Expected:** each field shows what was typed. Continue leads to the overview.
* **Actual:** Last Name stays empty and First Name shows a single character. Continue → "Error: Last Name is required".
* **Evidence:** [screenshot](../../04-e2e-framework/docs/evidence/CheckoutTest_problemUserCanEnterLastName.png)

### DEF-01-002 – Finish does not place the order (error_user)
* **Severity / Priority:** Blocker / P1
* **Preconditions:** `error_user`, on the Checkout: Overview page with 1 item
* **Steps:** 1. Click **Finish**. 2. Open DevTools → Console.
* **Expected:** "Thank you for your order!" page, with no console errors.
* **Actual:** the page stays on Overview. Console: `Uncaught TypeError: Cannot read properties of undefined (reading 'value')`.
* **Evidence:** [screenshot](../../04-e2e-framework/docs/evidence/CheckoutTest_errorUserCanFinishOrder.png) · [console log](../../04-e2e-framework/docs/evidence/CheckoutTest_errorUserCanFinishOrder.console.log)

### DEF-01-003 – Sorting by price has no effect (problem_user)
* **Severity / Priority:** Major / P2
* **Steps:** 1. Log in as `problem_user`. 2. Sort dropdown → "Price (low to high)".
* **Expected:** 7.99, 9.99, 15.99, 15.99, 29.99, 49.99
* **Actual:** unchanged order 29.99, 9.99, 15.99, 49.99, 7.99, 15.99
* **Evidence:** [screenshot](../../04-e2e-framework/docs/evidence/InventoryTest_problemUserCanSortByPrice.png)

### DEF-01-004 – Clicking a product opens a different product (problem_user)
* **Severity / Priority:** Critical / P1. The shopper may buy the wrong item.
* **Steps:** 1. Log in as `problem_user`. 2. Click the title "Sauce Labs Backpack".
* **Expected:** Backpack details. **Actual:** "Sauce Labs Fleece Jacket" details.
* **Evidence:** [screenshot](../../04-e2e-framework/docs/evidence/ProductDetailsTest_problemUserOpensCorrectProduct.png)

### DEF-01-005 – Only 3 of 6 products can be added to the cart
* **Severity / Priority:** Critical / P1
* **Steps:** 1. Log in as `problem_user` (repeat with `error_user`). 2. Click "Add to cart" on all six products.
* **Expected:** cart badge shows 6, and every button changes to "Remove".
* **Actual:** the badge shows 3 for both users. For `error_user` the console shows `Uncaught Error: Failed to add item to the cart.` three times.
* **Evidence:** [problem_user](../../04-e2e-framework/docs/evidence/CartTest_problemUserCanAddAllProducts.png) · [error_user](../../04-e2e-framework/docs/evidence/CartTest_errorUserCanAddAllProducts.png) · [console](../../04-e2e-framework/docs/evidence/CartTest_errorUserCanAddAllProducts.console.log)

### DEF-01-006 – AutomationExercise API returns HTTP 200 for errors
* **Severity / Priority:** Major / P2. Clients and monitoring can't detect failures from the status code.
* **Environment:** https://automationexercise.com/api/* · curl 8.7.1
* **Steps:**
  1. `curl -i -X POST https://automationexercise.com/api/verifyLogin -d email=nobody@example.test -d password=wrong`
  2. `curl -i -X POST https://automationexercise.com/api/searchProduct` (no parameter)
  3. `curl -i -X DELETE https://automationexercise.com/api/verifyLogin`
* **Expected:** HTTP 404, 400 and 405 respectively, matching the documented response codes.
* **Actual:** HTTP **200** every time. The real code appears only inside the body (`{"responseCode": 404, "message": "User not found!"}`, `400`, `405`).
* **Evidence:** [automationexercise-http-checks.txt](evidence/automationexercise-http-checks.txt)

### DEF-01-007 – Unknown product id returns HTTP 200 with a blank product
* **Severity / Priority:** Minor / P3
* **Steps:** open https://automationexercise.com/product_details/9999
* **Expected:** HTTP 404 with a "product not found" message.
* **Actual:** HTTP 200 and a product page with an empty name (`<h2></h2>`) and no price.
* **Evidence:** [automationexercise-http-checks.txt](evidence/automationexercise-http-checks.txt)

### DEF-01-008 – Mixed-content errors: Google Fonts stylesheets blocked
* **Severity / Priority:** Minor / P3. Fonts fall back to defaults, and the console is noisy on every page.
* **Steps:** 1. Open https://automationexercise.com/ (also /products and /product_details/1). 2. DevTools → Console.
* **Expected:** no console errors.
* **Actual:** 3 errors per page: *"Mixed Content: The page … was loaded over HTTPS, but requested an insecure stylesheet
  'http://fonts.googleapis.com/css?family=Roboto…' / 'Open+Sans…' / 'Abel'. This request has been blocked"*.
* **Fix:** use `https://` (or protocol-relative) font URLs.
* **Evidence:** [devtools-audit-2026-11-23.md](evidence/devtools-audit-2026-11-23.md) · [screenshot](evidence/devtools-automationexercise-home.png)

### DEF-01-009 – Search with no results has no empty-state message
* **Severity / Priority:** Minor / P3 (UX)
* **Steps:** Products → search `zzzxqq`
* **Expected:** a message such as "No products found for 'zzzxqq'".
* **Actual:** the heading "Searched Products" appears with an empty grid (0 product cards) and no message.
* **Evidence:** [automationexercise-http-checks.txt](evidence/automationexercise-http-checks.txt) (0 `productinfo` cards)

### DEF-01-010 – Checkout page accessible anonymously with an empty cart
* **Severity / Priority:** Major / P2. This is broken access control/validation for the checkout flow (OWASP A01).
* **Steps:** in a fresh session that is not logged in and has an empty cart, open https://automationexercise.com/checkout directly.
* **Expected:** a redirect to Login or Cart. In the normal flow the site shows a "Register / Login account to proceed on checkout" modal.
* **Actual:** the full checkout page is rendered ("Address Details", "Review Your Order", "Total Amount", **"Place Order"**).
  Place Order was deliberately not clicked, to avoid creating data on the third-party site.
* **Evidence:** [automationexercise-http-checks.txt](evidence/automationexercise-http-checks.txt)
