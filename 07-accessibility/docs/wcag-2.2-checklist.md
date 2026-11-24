# WCAG 2.2 Level A/AA Checklist – SauceDemo purchase journey

Legend:
- **Pass**: verified, conforms
- **Fail**: defect raised
- **Review**: needs a human decision
- **Manual**: manual check, not executed in this run
- **N/A**: not applicable

"Evidence" names the automated test, tool or defect. Only criteria relevant to the audited pages are listed. The
time-based-media criteria (1.2.x) are N/A because the site has no audio or video.

| SC | Level | Criterion | Result | Evidence / notes |
|---|---|---|---|---|
| 1.1.1 | A | Non-text Content | Pass | `productImagesHaveAltText`; axe `image-alt` passes |
| 1.2.x | A/AA | Time-based media | N/A | No audio/video |
| 1.3.1 | A | Info and Relationships | **Fail** | DEF-A11Y-04 (no `<label>`), DEF-A11Y-05 (no h1 / region) |
| 1.3.2 | A | Meaningful Sequence | Pass | DOM order matches visual order (tab-order tests) |
| 1.3.3 | A | Sensory Characteristics | Manual | Check that no instructions rely on shape, colour or position only |
| 1.3.4 | AA | Orientation | Pass | No orientation lock; the page renders at 320×800 (portrait) and 1440×900 (landscape) in the automated run |
| 1.3.5 | AA | Identify Input Purpose | **Review** | Checkout fields lack `autocomplete="given-name"/"family-name"/"postal-code"` – recommend adding |
| 1.4.1 | A | Use of Color | Manual | Check that error states use text/icons, not colour alone |
| 1.4.3 | AA | Contrast (Minimum) | **Review** | axe marks `color-contrast` "incomplete" (4–8 nodes per page); check with a contrast analyser |
| 1.4.4 | AA | Resize Text | Manual | Zoom to 200% and check for loss of content |
| 1.4.10 | AA | Reflow | Pass | `reflowAt320px`: 0 px horizontal overflow on login and inventory |
| 1.4.11 | AA | Non-text Contrast | **Fail** | No focus indicator at all (DEF-A11Y-01); the input underline is low contrast (review) |
| 1.4.12 | AA | Text Spacing | Manual | Apply a text-spacing bookmarklet |
| 1.4.13 | AA | Content on Hover or Focus | N/A | No tooltips |
| 2.1.1 | A | Keyboard | Pass | `loginWithKeyboardOnly`, `addToCartWithKeyboardOnly`, `burgerMenuKeyboard`, `burgerMenuClosesWithEscape` |
| 2.1.2 | A | No Keyboard Trap | Pass | `burgerMenuKeyboard` tabs back into page content after closing |
| 2.1.4 | A | Character Key Shortcuts | N/A | None |
| 2.2.1 | A | Timing Adjustable | Manual | No time-outs hit during automated runs (each < 15 s); a long-idle test is still to do |
| 2.3.1 | A | Three Flashes | Pass | No animated content apart from the menu slide-in |
| 2.4.1 | A | Bypass Blocks | **Review** | No skip link; landmarks are partial (DEF-A11Y-05) |
| 2.4.2 | A | Page Titled | **Fail** | DEF-A11Y-03 |
| 2.4.3 | A | Focus Order | **Fail** | Order is correct (`loginFormTabOrder`), but focus is lost after a cart toggle (DEF-A11Y-02) |
| 2.4.4 | A | Link Purpose (In Context) | Pass | `interactiveElementsNamed`; product links use product names |
| 2.4.6 | AA | Headings and Labels | **Fail** | DEF-A11Y-04, DEF-A11Y-05 |
| 2.4.7 | AA | Focus Visible | **Fail** | DEF-A11Y-01 |
| 2.4.11 | AA | Focus Not Obscured (Minimum) *(new in 2.2)* | Manual | Can't be judged while no focus indicator exists (DEF-A11Y-01); re-test after the fix |
| 2.5.1 | A | Pointer Gestures | N/A | No multi-point gestures |
| 2.5.2 | A | Pointer Cancellation | Manual | Check that actions fire on the up-event |
| 2.5.3 | A | Label in Name | Pass | Visible button text equals the accessible name (`Login`, `Add to cart`) |
| 2.5.4 | A | Motion Actuation | N/A | – |
| 2.5.7 | AA | Dragging Movements *(new in 2.2)* | N/A | No drag interactions |
| 2.5.8 | AA | Target Size (Minimum) *(new in 2.2)* | Pass | Buttons ≥ 24×24 CSS px (axe `target-size` passes) |
| 3.1.1 | A | Language of Page | Pass | `htmlLangPresent` (`en`) |
| 3.2.1 / 3.2.2 | A | On Focus / On Input | Pass | Tabbing through every control (KB tests) caused no navigation; the sort select is manual |
| 3.2.6 | A | Consistent Help *(new in 2.2)* | N/A | No help mechanism |
| 3.3.1 | A | Error Identification | Manual | Submit empty login/checkout forms and check the text error (`[data-test=error]`) |
| 3.3.2 | A | Labels or Instructions | **Fail** | DEF-A11Y-04 |
| 3.3.7 | A | Redundant Entry *(new in 2.2)* | Pass | The checkout flow (automated) asks for customer info once |
| 3.3.8 | AA | Accessible Authentication (Minimum) *(new in 2.2)* | Pass | Plain username/password with no puzzle; credentials are typed by the automation (paste not blocked: `type=password`, no `onpaste` handler – manual confirm) |
| 4.1.2 | A | Name, Role, Value | Pass | `loginInputsHaveAccessibleName`, `interactiveElementsNamed`; the burger menu exposes `aria-hidden` state |
| 4.1.3 | AA | Status Messages | **Review** | The cart badge change isn't announced (no `aria-live`). Verify with the screen-reader script SR-07 |

**Summary:** 7 criteria failed (1.3.1, 1.4.11, 2.4.2, 2.4.3, 2.4.6, 2.4.7, 3.3.2), mapped to 5 defects; 4 need review (1.3.5, 1.4.3, 2.4.1, 4.1.3); 8 manual checks are pending.
