# Test Cases – Accessibility

Status column: the result of the automated run on **2026-11-23** (headless Chrome 157). "Manual" cases are executed
with [screen-reader-test-script.md](screen-reader-test-script.md) and the [checklist](wcag-2.2-checklist.md).

| ID | Title | WCAG | Steps (summary) | Test data | Expected | Automated by | Result |
|---|---|---|---|---|---|---|---|
| A11Y-AUTO-01 | No new axe violations (×7 pages) | many | Navigate to page via UI flow → run axe with A/AA + best-practice tags → compare with baseline | standard_user | No violation outside baseline | `AxeWorkflowScanTest.noNewViolations` | Pass (7/7) |
| A11Y-AUTO-02 | Zero axe violations (×7 pages) | many | Same scan, no baseline | standard_user | 0 violations | `AxeWorkflowScanTest.zeroViolations` (known-defect) | Fail (7/7) → DEF-A11Y-05 |
| A11Y-KB-01 | Login tab order | 2.4.3 | Focus document start → Tab ×3 → Shift+Tab | – | username → password → Login; Shift+Tab back to password | `loginFormTabOrder` | Pass |
| A11Y-KB-02 | Keyboard-only login | 2.1.1 | Tab, type user, Tab, type password, Enter | standard_user | Inventory page loads | `loginWithKeyboardOnly` | Pass |
| A11Y-KB-03 | Keyboard-only add to cart | 2.1.1 | Log in → Tab until "Add to cart" (Backpack) → Enter | – | Cart badge shows 1 | `addToCartWithKeyboardOnly` | Pass |
| A11Y-KB-03b | Focus retained after toggle | 2.4.3 | As KB-03, then inspect `document.activeElement` | – | Focus on "Remove" button | `focusKeptAfterAddToCart` (known-defect) | Fail → DEF-A11Y-02 |
| A11Y-KB-04 | Burger menu keyboard + no trap | 2.1.1, 2.1.2 | Tab to menu button → Enter → Tab → Tab to Close → Enter → Tab on | – | Menu opens, focus moves inside, closes, page reachable again | `burgerMenuKeyboard` | Pass |
| A11Y-KB-05 | Escape closes burger menu | 2.1.1 | Open menu by keyboard → Esc | – | Menu `aria-hidden=true` | `burgerMenuClosesWithEscape` | Pass |
| A11Y-KB-06 | Focus indicator on login controls | 2.4.7 | Tab through login controls; read computed outline/box-shadow | – | Visible outline or shadow | `loginFocusIndicatorVisible` (known-defect) | Fail → DEF-A11Y-01 |
| A11Y-KB-07 | Focus indicator on inventory controls | 2.4.7 | Tab 12× on inventory; check each focused element | – | Every element has visible indicator | `inventoryFocusIndicatorVisible` (known-defect) | Fail → DEF-A11Y-01 |
| A11Y-SEM-01 | Page language | 3.1.1 | Read `html[lang]` | – | Valid BCP-47 code | `htmlLangPresent` | Pass (`en`) |
| A11Y-SEM-02 | Page has a title | 2.4.2 | Read `document.title` | – | Non-empty | `pagesHaveTitle` | Pass |
| A11Y-SEM-03 | Titles unique and descriptive | 2.4.2 | Visit login, inventory, cart, checkout; compare titles | – | 4 distinct titles | `pageTitlesAreDescriptive` (known-defect) | Fail → DEF-A11Y-03 |
| A11Y-SEM-04 | Inputs have accessible name | 4.1.2 | `WebElement.getAccessibleName()` on inputs/button | – | Non-empty | `loginInputsHaveAccessibleName` | Pass (name comes from placeholder) |
| A11Y-SEM-05 | Visible labels on form fields | 3.3.2, 1.3.1 | Find inputs without `<label>` on login and checkout | – | None | `formFieldsHaveVisibleLabels` (known-defect) | Fail → DEF-A11Y-04 |
| A11Y-SEM-06 | Product image alt text | 1.1.1 | Read `alt` of every product image | – | Non-empty | `productImagesHaveAltText` | Pass |
| A11Y-SEM-07 | Controls have accessible names | 4.1.2, 2.4.4 | All visible buttons/links → accessible name | – | Non-empty | `interactiveElementsNamed` | Pass |
| A11Y-SEM-08 | Reflow at 320 px | 1.4.10 | Window 320×800 → `scrollWidth - innerWidth` on login & inventory | – | ≤ 1 px | `reflowAt320px` | Pass |
| A11Y-LH-01 | Lighthouse accessibility score | – | `scripts/lighthouse.sh` | login page | ≥ 90 | script | 100 |
| A11Y-MAN-01..12 | Screen-reader script | 1.3.1, 4.1.2, 4.1.3 … | See screen-reader script | standard_user | Announcements as documented | Manual | Not executed in this run |
| A11Y-MAN-13 | Colour contrast of items axe marked "incomplete" | 1.4.3 | Contrast analyser on product names / prices / buttons | – | ≥ 4.5:1 normal text | Manual | Not executed in this run |
