# Manual Screen-Reader Test Script

**Readers:**
- VoiceOver + Safari (macOS). Toggle with Cmd+F5. "VO" means Ctrl+Option.
- NVDA + Firefox or Chrome (Windows). Insert is the NVDA key.

**Data:** `standard_user` / `secret_sauce` (public demo credentials shown on the site).

Record Pass/Fail for each step, with a short transcript of what was announced. Attach a VoiceOver caption-panel
screenshot or an NVDA Speech Viewer log as evidence.

| ID | Page | Action (VoiceOver / NVDA) | Expected announcement | Related |
|---|---|---|---|---|
| SR-01 | Login | Load page | "Swag Labs" (title), then web content. *Expected per WCAG 2.4.2: a descriptive title such as "Login – Swag Labs"* | DEF-A11Y-03 |
| SR-02 | Login | VO+U → Headings / NVDA `H` | A level-1 heading such as "Swag Labs" or "Log in". *Currently no headings are found* | DEF-A11Y-05 |
| SR-03 | Login | Tab to Username | "Username, edit text" (NVDA: "Username edit blank"). The name comes from the placeholder | DEF-A11Y-04 |
| SR-04 | Login | Type a value, Shift+Tab, Tab | Field is still announced as "Username" with its value. Check that the label isn't lost | DEF-A11Y-04 |
| SR-05 | Login | Submit an empty form | Error "Epic sadface: Username is required" is announced automatically (live region) or on focus | 3.3.1, 4.1.3 |
| SR-06 | Inventory | VO+U → Landmarks / NVDA `D` | Main, navigation (menu), contentinfo (footer) | 1.3.1, 2.4.1 |
| SR-07 | Inventory | Tab to "Add to cart" for Backpack, then press Enter | "Add to cart, button" → after activation, "Remove, button" and the cart count is announced. *Currently focus is lost and nothing is announced* | DEF-A11Y-02, 4.1.3 |
| SR-08 | Inventory | Tab to the sort select, open it with arrow keys | "Name (A to Z), popup button / combo box", and each option announced | 4.1.2 |
| SR-09 | Inventory | Activate "Open Menu" | "Open Menu, button" → menu opens, and the first item "All Items, link" is announced | 4.1.2 |
| SR-10 | Inventory | Images: VO+Cmd+G / NVDA `G` | Each product image is announced by its product name (alt text) | 1.1.1 |
| SR-11 | Checkout 1 | Tab through the fields | "First Name", "Last Name", "Zip/Postal Code" edit text. Submit empty → error announced | DEF-A11Y-04, 3.3.1 |
| SR-12 | Complete | After Finish | "Thank you for your order!" heading is announced, and focus/reading starts at the confirmation | 2.4.3 |

**Exit:** every row has a result. Raise a defect (template in [defects.md](defects.md)) for each unexpected announcement.

> Status 2026-11-23: script prepared; manual execution not yet recorded.
