# Defect Reports – Project 04 (SauceDemo E2E)

All defects were reproduced by automated tests in group `known-defect`
(`./mvnw -pl 04-e2e-framework test -Dheadless=true -Dsuite=testng-known-defects.xml -DtestFailureIgnore=true`).
SauceDemo seeds these defects deliberately for the `problem_user`, `error_user` and `visual_user` demo accounts.

**Environment (all defects):** https://www.saucedemo.com · Google Chrome 157.0.8103.79 headless · macOS 26 (arm64) ·
Selenium 4.50 · run 2026-11-23 00:10 IST · password for all demo users: `secret_sauce` (public demo account).

| ID | Title | Severity | Priority | User | Automated test |
|---|---|---|---|---|---|
| DEF-04-001 | All product images show the same placeholder | Major | P2 | problem_user | `InventoryTest.problemUserImagesAreDistinct` |
| DEF-04-002 | Sorting by price has no effect | Major | P2 | problem_user | `InventoryTest.problemUserCanSortByPrice` |
| DEF-04-003 | Listing shows wrong (random) prices | Critical | P1 | visual_user | `InventoryTest.visualUserSeesCorrectPrices` |
| DEF-04-004 | Clicking a product opens a different product | Critical | P1 | problem_user | `ProductDetailsTest.problemUserOpensCorrectProduct` |
| DEF-04-005 | Only 3 of 6 products can be added to the cart | Critical | P1 | problem_user | `CartTest.problemUserCanAddAllProducts` |
| DEF-04-006 | "Failed to add item to the cart" JS error for 3 of 6 products | Critical | P1 | error_user | `CartTest.errorUserCanAddAllProducts` |
| DEF-04-007 | Last Name field on checkout cannot be filled | Blocker | P1 | problem_user | `CheckoutTest.problemUserCanEnterLastName` |
| DEF-04-008 | Finish button does not complete the order | Blocker | P1 | error_user | `CheckoutTest.errorUserCanFinishOrder` |

Severity scale: Blocker (journey impossible) > Critical (wrong data/money or core function broken) > Major > Minor.
Priority: P1 fix before release, P2 next sprint, P3 backlog.

---

### DEF-04-001 – problem_user: all product images are identical placeholder
* **Severity / Priority:** Major / P2
* **Steps:** 1. Open https://www.saucedemo.com 2. Log in as `problem_user` 3. Look at the product images on Products.
* **Expected:** each of the 6 products shows its own image (as for `standard_user`).
* **Actual:** all 6 `<img>` use `/assets/sl-404-Cq1a9k9X.jpg` (a "404 dog" image).
* **Evidence:** [InventoryTest_problemUserImagesAreDistinct.png](evidence/InventoryTest_problemUserImagesAreDistinct.png)

### DEF-04-002 – problem_user: sorting by price has no effect
* **Severity / Priority:** Major / P2
* **Steps:** 1. Log in as `problem_user` 2. Choose sort "Price (low to high)".
* **Expected:** prices ascending: 7.99, 9.99, 15.99, 15.99, 29.99, 49.99.
* **Actual:** order unchanged: `[29.99, 9.99, 15.99, 49.99, 7.99, 15.99]`.
* **Evidence:** [InventoryTest_problemUserCanSortByPrice.png](evidence/InventoryTest_problemUserCanSortByPrice.png)

### DEF-04-003 – visual_user: listed prices differ from the catalogue
* **Severity / Priority:** Critical / P1 (customer sees wrong price)
* **Steps:** 1. Log in as `standard_user`, note prices 2. Log out, log in as `visual_user` 3. Compare prices.
* **Expected:** same prices (e.g. Backpack $29.99, Bike Light $9.99, Bolt T-Shirt $15.99).
* **Actual (this run):** Backpack $8.29, Bike Light $59.36, Bolt T-Shirt $92.86, Fleece Jacket $12.01, Onesie $48.65, Red T-Shirt $7.82. Values change on every page load.
* **Evidence:** [InventoryTest_visualUserSeesCorrectPrices.png](evidence/InventoryTest_visualUserSeesCorrectPrices.png)

### DEF-04-004 – problem_user: clicking a product opens a different product
* **Severity / Priority:** Critical / P1
* **Steps:** 1. Log in as `problem_user` 2. Click the name "Sauce Labs Backpack".
* **Expected:** details page for Sauce Labs Backpack.
* **Actual:** details page for "Sauce Labs Fleece Jacket".
* **Evidence:** [ProductDetailsTest_problemUserOpensCorrectProduct.png](evidence/ProductDetailsTest_problemUserOpensCorrectProduct.png)

### DEF-04-005 – problem_user: only 3 of 6 products can be added to the cart
* **Severity / Priority:** Critical / P1
* **Steps:** 1. Log in as `problem_user` 2. Click "Add to cart" on all 6 products.
* **Expected:** cart badge shows 6.
* **Actual:** badge shows 3; the buttons of the other products do nothing.
* **Evidence:** [CartTest_problemUserCanAddAllProducts.png](evidence/CartTest_problemUserCanAddAllProducts.png)

### DEF-04-006 – error_user: JS error "Failed to add item to the cart"
* **Severity / Priority:** Critical / P1
* **Steps:** 1. Log in as `error_user` 2. Click "Add to cart" on all 6 products. 3. Open DevTools → Console.
* **Expected:** badge 6, no console errors.
* **Actual:** badge 3; console: `Uncaught Error: Failed to add item to the cart.` (×3, index-DnLqFcJI.js 572:45830).
* **Evidence:** [screenshot](evidence/CartTest_errorUserCanAddAllProducts.png), [console log](evidence/CartTest_errorUserCanAddAllProducts.console.log)

### DEF-04-007 – problem_user: Last Name cannot be entered on checkout
* **Severity / Priority:** Blocker / P1 (order cannot be placed)
* **Steps:** 1. Log in as `problem_user` 2. Add Backpack → Cart → Checkout 3. Type a first name, last name and postcode.
* **Expected:** each field keeps the typed value.
* **Actual:** Last Name stays empty, and First Name ends up holding a single character (`h` in this run) instead of the typed first name. Typing into Last Name corrupts First Name.
* **Evidence:** [CheckoutTest_problemUserCanEnterLastName.png](evidence/CheckoutTest_problemUserCanEnterLastName.png)

### DEF-04-008 – error_user: Finish does not complete the order
* **Severity / Priority:** Blocker / P1
* **Steps:** 1. Log in as `error_user` 2. Add Backpack → Cart → Checkout → fill info → Continue 3. Click Finish.
* **Expected:** "Thank you for your order!" page.
* **Actual:** stays on the overview page; console: `Uncaught TypeError: Cannot read properties of undefined (reading 'value')` (index-DnLqFcJI.js 572:14839).
* **Evidence:** [screenshot](evidence/CheckoutTest_errorUserCanFinishOrder.png), [console log](evidence/CheckoutTest_errorUserCanFinishOrder.console.log)
