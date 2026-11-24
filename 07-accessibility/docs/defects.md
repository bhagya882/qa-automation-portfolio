# Defect Reports – Accessibility

Environment for all defects (unless stated):
- macOS 26 arm64, Chrome 157 (headless and headed), window 1440×900
- https://www.saucedemo.com, user `standard_user`
- Selenium 4.50.0, axe-core 4.10, run on 2026-11-23

Severity scale:
- **Critical**: blocks a task for assistive-technology users
- **High**: major barrier
- **Medium**: workaround exists
- **Low**: best practice

Priority runs from P1 (fix first) to P4.

---

## DEF-A11Y-01 – No visible keyboard focus indicator on interactive controls

| Field | Value |
|---|---|
| Severity / Priority | **High / P1** |
| WCAG | 2.4.7 Focus Visible (AA); 2.4.13 Focus Appearance (AAA, informational) |
| Pages | Login, Inventory (and every page that uses the same stylesheet) |
| Automated test | `KeyboardNavigationTest.loginFocusIndicatorVisible`, `inventoryFocusIndicatorVisible` (group `known-defect`) |

**Steps to reproduce**
1. Open https://www.saucedemo.com/.
2. Press Tab to move focus to the Username field, then the Password field, then the Login button.
3. Log in and keep pressing Tab on the inventory page (menu button, sort dropdown, product links, Add to cart).

**Expected:** every focused element shows a clearly visible indicator (outline or equivalent with at least 3:1 contrast).
**Actual:** no visual change apart from the text caret in inputs. The computed style of the focused element is
`outline-style: none; box-shadow: none`, even though `:focus-visible` matches. Examples:
`BUTTON#react-burger-menu-btn`, `SELECT.product_sort_container`, `A#item_4_title_link`,
`BUTTON#add-to-cart-sauce-labs-backpack`.

**Evidence**
- [docs/evidence/screenshots/A11Y-KB-06_focus_user-name_chrome@desktop.png](evidence/screenshots/A11Y-KB-06_focus_user-name_chrome@desktop.png): focus is on Username with no ring
- [docs/evidence/screenshots/A11Y-KB-07_focus_indicator_chrome@desktop.png](evidence/screenshots/A11Y-KB-07_focus_indicator_chrome@desktop.png): after 12 Tabs, the focused element is not identifiable
- The computed styles are in the test failure message (Allure, `testng-results-full-run.xml`)

**Impact:** sighted keyboard users, including switch and voice-control users, cannot tell where they are on the page.
**Suggested fix:** remove the global `outline: none` and add
`:focus-visible { outline: 3px solid #132322; outline-offset: 2px; }` (or a brand colour with at least 3:1 contrast against the background).

---

## DEF-A11Y-02 – Focus is lost to `<body>` after "Add to cart" / "Remove"

| Field | Value |
|---|---|
| Severity / Priority | **Medium / P2** |
| WCAG | 2.4.3 Focus Order (A) |
| Page | Inventory (the same pattern is on Product details) |
| Automated test | `KeyboardNavigationTest.focusKeptAfterAddToCart` (`known-defect`) |

**Steps to reproduce**
1. Log in. Tab to "Add to cart" for Sauce Labs Backpack and press Enter.
2. Check where focus is (`document.activeElement`), then press Tab once.

**Expected:** focus stays on the same control, which now reads "Remove". The next Tab moves to the next product.
**Actual:** the button is re-rendered with a new id (`remove-sauce-labs-backpack`) and focus falls back to
`<body>`. The next Tab starts again from the top of the page. Keyboard and screen-reader users lose their place
after every cart action.

**Evidence:** [A11Y-KB-03b_focus_lost_after_add_to_cart_chrome@desktop.png](evidence/screenshots/A11Y-KB-03b_focus_lost_after_add_to_cart_chrome@desktop.png); the assertion
`expected "remove-sauce-labs-backpack" but was "body"`.
**Suggested fix:** keep one `<button>` element and toggle its label and `aria-pressed`, or move focus programmatically
to the new button after re-rendering. Announce the cart count change with an `aria-live="polite"` region (WCAG 4.1.3).

---

## DEF-A11Y-03 – All pages have the same title, "Swag Labs"

| Field | Value |
|---|---|
| Severity / Priority | **Medium / P3** |
| WCAG | 2.4.2 Page Titled (A) |
| Automated test | `SemanticStructureTest.pageTitlesAreDescriptive` (`known-defect`) |

**Steps:** visit Login, Inventory, Cart and Checkout step one, and read the browser tab title or `document.title`.
**Expected:** each title describes the page, e.g. "Cart – Swag Labs".
**Actual:** `{LOGIN=Swag Labs, INVENTORY=Swag Labs, CART=Swag Labs, CHECKOUT_STEP_ONE=Swag Labs}`, so there is 1 distinct title for 4 pages.
Screen-reader users hear the same title after every navigation, and the browser history and tabs are indistinguishable.
**Suggested fix:** set `document.title` on each route change, using the format "<Page> – Swag Labs".

---

## DEF-A11Y-04 – Form fields use the placeholder as their only label

| Field | Value |
|---|---|
| Severity / Priority | **Medium / P2** |
| WCAG | 3.3.2 Labels or Instructions (A), 1.3.1 Info and Relationships (A) |
| Pages | Login (`user-name`, `password`), Checkout step one (`first-name`, `last-name`, `postal-code`) |
| Automated test | `SemanticStructureTest.formFieldsHaveVisibleLabels` (`known-defect`) |

**Steps:** open Login, and separately Checkout step one. Type a character in any field.
**Expected:** a persistent visible `<label for=…>` for each field.
**Actual:** no `<label>` elements are present. The placeholder ("Username", "First Name", …) disappears as soon as
the user types, so users with memory or cognitive impairments lose the instruction. axe and Lighthouse do **not**
flag this, because a placeholder counts as an accessible name. This is why the manual and custom checks are needed.
**Evidence:** [A11Y-SEM-05_placeholder_only_fields_chrome@desktop.png](evidence/screenshots/A11Y-SEM-05_placeholder_only_fields_chrome@desktop.png)
**Suggested fix:** add visible `<label>` elements (floating labels are acceptable if they stay visible).

---

## DEF-A11Y-05 – No level-one heading; login content is outside landmarks (best practice)

| Field | Value |
|---|---|
| Severity / Priority | **Low / P3** |
| Rule | axe `page-has-heading-one` (all 7 pages), `region` (login: `.login_logo`) – best-practice; supports 1.3.1 and 2.4.1 |
| Automated test | `AxeWorkflowScanTest.zeroViolations` (`known-defect`); included in `a11y-baseline.json` |

**Steps:** run axe on any page (see [evidence/axe/a11y-report.md](evidence/axe/a11y-report.md)).
**Expected:** each page has one `<h1>` naming the page ("Products", "Your Cart", …), and all content is inside landmarks.
**Actual:** the "Swag Labs" logo and page titles are `<div>`/`<span>` elements, so there is no `<h1>`. On the login page the
logo `div.login_logo` is outside any landmark. Screen-reader users can't jump to the main heading with the `H`/`1` keys.
**Node targets:** `[html]` (page-has-heading-one), `[.login_logo]` (region).
**Suggested fix:** mark the page title up as `<h1>`, and wrap the login content in `<main>`.
