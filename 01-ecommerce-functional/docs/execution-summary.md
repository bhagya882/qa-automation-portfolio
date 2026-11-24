# Test Execution Summary – Project 01

**Cycle:** 2026-11-23 · Chrome 157 / macOS 26 · Sources: project 04 automated run, curl HTTP checks, DevTools audit.

## Overall
| Total | Passed | Failed | Blocked | Not run | Pass rate (executed) |
|---|---|---|---|---|---|
| 60 | 37 | 14 | 1 | 8 | 72.5% (37/51) |

## By module
| Module | Total | Pass | Fail | Blocked | Not run |
|---|---|---|---|---|---|
| Registration (AE) | 6 | 1 | 0 | 0 | 5 |
| Login | 14 | 11 | 2 | 0 | 1 |
| Search / sort | 8 | 5 | 3 | 0 | 0 |
| Product details | 6 | 4 | 2 | 0 | 0 |
| Cart | 10 | 7 | 2 | 0 | 1 |
| Checkout | 9 | 6 | 3 | 0 | 0 |
| Order history | 3 | 1 | 0 | 1 | 1 |
| DevTools hygiene | 4 | 2 | 2 | 0 | 0 |

## By site
| Site | Total | Pass | Fail | Blocked | Not run |
|---|---|---|---|---|---|
| SauceDemo | 36 | 29 | 7 (all seeded-defect users) | 0 | 0 |
| AutomationExercise | 23 | 8 | 7 | 0 | 8 |
| Both (order history list) | 1 | 0 | 0 | 1 | 0 |

## Defects
10 logged ([defects.md](defects.md)): 2 Blocker, 2 Critical, 3 Major, 3 Minor. All are open, because these are third-party practice sites.

## Not run / blocked: reasons
* **TC-REG-02…06, TC-LOG-14, TC-CART-10, TC-OH-02 (Not Run):** these steps write data (account, cart, order) on a third-party site.
  They are meant to be run manually with a synthetic `@example.test` account that is deleted afterwards (TC-REG-06).
* **TC-OH-03 (Blocked):** neither site has an order-history page. API-level coverage is in project 03.

## Exit criteria
* ✅ every case executed or given a documented reason
* ✅ no defects in the SauceDemo `standard_user` happy path
* ✅ every failure linked to a defect with evidence
* ⚠️ AE write-flows still need a manual session before sign-off
