# Defect Reports – Project 02 (Cross-browser & responsive)

Reproduced with `./mvnw -pl 02-cross-browser-ui test -Dheadless=true -Dgroups=known-defect -DtestFailureIgnore=true`
on 2026-11-23 (Chrome 157 and Firefox 160, 1366x768 and 390x844). Both defects fail the same way in **every** browser and
viewport tested, so they are product defects and not browser-specific rendering differences.

| ID | Title | Severity | Priority | Browsers / viewports |
|---|---|---|---|---|
| DEF-02-001 | visual_user: inventory layout deviates from baseline (cart icon misplaced, wrong image, misaligned titles/buttons, wrong prices) | Major | P2 | Chrome, Firefox × all |
| DEF-02-002 | Burger-menu button touch target is 20×20 px (< 24×24 px) | Minor | P3 | Chrome, Firefox × all |

---

### DEF-02-001 – visual_user: inventory layout deviates from the standard_user baseline
* **Severity / Priority:** Major / P2 (wrong prices are already tracked as Critical DEF-04-003)
* **Environment:** https://www.saucedemo.com, Chrome 157 headless (1366x768 and 390x844), Firefox 160 headless (1366x768 and 390x844 → rendered 500x759), macOS 26
* **Steps:**
  1. Log in as `standard_user` (password `secret_sauce`, public demo account) and capture the Products page.
  2. Log out, log in as `visual_user`, and capture the Products page at the same viewport.
  3. Compare the element positions.
* **Expected:** identical layout. Cart icon top-right inside the header bar; each product shows its own image; titles left-aligned; Add-to-cart buttons aligned.
* **Actual (measured, Chrome 1366x768):**
  * Shopping-cart link moved **186 px left and 29 px down** (x 1306→1120, y 10→39). It now sits on the header's bottom border, above the sort dropdown.
  * The Backpack image is a dog photo (`sl-404` asset), and its width changes from 159 to 262 px.
  * "Sauce Labs Bolt T-Shirt" and "Sauce Labs Fleece Jacket" titles are indented.
  * The last item's "Add to cart" button is shifted 54 px right (x 1018→1072). At 390 px it is also 54 px wider and 286 px higher.
  * Prices differ, so price label widths change (e.g. 60→72 px).
* **Evidence:** baseline [standard 1366](evidence/DEF-02-001_standard_user_chrome@1366x768.png) vs [visual 1366](evidence/DEF-02-001_visual_user_chrome@1366x768.png);
  [standard 390](evidence/DEF-02-001_standard_user_chrome@390x844.png) vs [visual 390](evidence/DEF-02-001_visual_user_chrome@390x844.png);
  Firefox [standard](evidence/DEF-02-001_standard_user_firefox@1366x768.png) vs [visual](evidence/DEF-02-001_visual_user_firefox@1366x768.png).
  The full list of deviations is in the Allure/TestNG failure message of `VisualConsistencyTest.visualUserLayoutMatchesBaseline`.

### DEF-02-002 – Burger-menu button is 20×20 CSS px
* **Severity / Priority:** Minor / P3 (accessibility/usability concern)
* **Environment:** all browsers/viewports tested
* **Steps:** 1. Log in as `standard_user`. 2. Measure `#react-burger-menu-btn` (DevTools → Elements → box model).
* **Expected:** interactive targets are at least 24×24 CSS px. This is the WCAG 2.2 SC 2.5.8 *Target Size (Minimum)*
  guideline, and it matters most on touch screens.
* **Actual:** the button measures 20×20 px. The cart link and the Add-to-cart buttons pass.
* **Note:** SC 2.5.8 has a spacing exception (a target is allowed to be smaller if a 24 px circle around it overlaps no other target).
  The burger probably qualifies for it, so this is logged as Minor/P3 for the UX team rather than as a conformance failure. Project 07 is doing the full accessibility audit.
* **Evidence:** [DEF-02-002_burger_20px_chrome@390x844.png](evidence/DEF-02-002_burger_20px_chrome@390x844.png)

## Inconsistencies investigated and found not to be defects
* **Burger links "outside the viewport" on Firefox 390 px:** this happened because the test measured during the menu's CSS slide-in
  transition. The fix was in the test: wait for the menu to finish arriving (`Header.BurgerMenu.waitUntilOpen`).
* **Chrome "390x844" rendered at 500 px:** desktop Chrome enforces a minimum window width. The fix was in the framework: the exact viewport is now set via CDP.
