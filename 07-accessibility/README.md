# 07 – Accessibility and Inclusive UX Audit (Java)

An automated and manual accessibility audit of the purchase journey on **SauceDemo**
(https://www.saucedemo.com), a public site published for test-automation practice. The audit covers login,
inventory, product details, cart, and checkout steps one, two and complete.

| Area | Tooling |
|---|---|
| Rule-based WCAG scan | **axe-core** via `com.deque.html.axe-core:selenium`, tags `wcag2a wcag2aa wcag21a wcag21aa wcag22aa best-practice` |
| Keyboard / focus / semantics | **Selenium 4 + TestNG** (Java 21), real key events through the Actions API |
| Page score | **Lighthouse** accessibility category (`scripts/lighthouse.sh`) |
| Manual | WAVE browser extension, VoiceOver (macOS) / NVDA (Windows), WCAG 2.2 AA checklist |
| Reporting | Allure (shared `target/allure-results`), `target/a11y/a11y-report.{md,html}`, raw axe JSON |

## How to run

```bash
# from the repo root
./mvnw -pl 07-accessibility test                                  # everything (known-defect tests fail on purpose)
./mvnw -pl 07-accessibility test -DexcludedGroups=known-defect    # PR gate: must be green
./mvnw -pl 07-accessibility test -Dheadless=false                 # watch it run
./mvnw -pl 07-accessibility test -Dbrowser=firefox                # other browser
07-accessibility/scripts/lighthouse.sh https://www.saucedemo.com/ # Lighthouse (needs Node 18+ and Chrome)
```

The parent POM sets up shared tooling (qa-core, Allure). If qa-core isn't installed yet, run
`./mvnw -pl qa-core install -DskipTests` once.

Outputs:
- `target/a11y/a11y-report.md` and `.html`: violations per page, WCAG tags, node targets, and baseline status
- `target/a11y/<PAGE>.axe.json`: raw axe results
- `target/screenshots/`, `target/evidence/`: deliberate evidence, plus screenshots and DOM captured on failure
- Allure: `./mvnw allure:serve` from the repo root

## How the baseline works

`src/test/resources/a11y-baseline.json` lists the violations already reported (page + rule + defect ID).

- `AxeWorkflowScanTest.noNewViolations` is the regression gate. It fails only on violations that are **not** in the baseline.
- `AxeWorkflowScanTest.zeroViolations` (group `known-defect`) asserts the target state of zero violations. It fails today and documents the open defects.

When a defect is fixed, remove its baseline entry so it can't come back unnoticed.

## Test design

| Class | What it checks |
|---|---|
| `AxeWorkflowScanTest` | axe scan of 7 pages (data-driven) against the baseline |
| `KeyboardNavigationTest` | tab order, keyboard-only login and add-to-cart, burger menu open/close/Escape/no trap, focus visibility, focus retention |
| `SemanticStructureTest` | `lang`, page titles, accessible names, visible labels, image alt text, reflow at 320 px |

## Documentation

- [Test plan](docs/test-plan.md)
- [Test cases](docs/test-cases.md)
- [WCAG 2.2 AA checklist mapped to findings](docs/wcag-2.2-checklist.md)
- [Defect reports](docs/defects.md)
- [Manual screen-reader test script](docs/screen-reader-test-script.md)
- [WAVE usage notes](docs/wave-notes.md)
- [Execution summary](docs/execution-summary.md)
- Evidence: [docs/evidence/](docs/evidence/)

## Known limitations

- Automated tools catch roughly 30–40% of WCAG issues. axe found **no** WCAG A/AA rule violations and Lighthouse
  scored **100**, yet the keyboard tests found four real defects. The manual checklist and screen-reader script are required.
- axe reports `color-contrast` as *incomplete* (text over images and gradients). This needs a manual check with a
  contrast analyser; it is listed under "Needs manual review" in the report.
- The screen-reader steps are manual and documented, not automated.
- The target is a third-party practice site. Its markup can change without notice, and the baseline may then need updating.
- Safari needs `safaridriver --enable` once (it requires admin rights).
