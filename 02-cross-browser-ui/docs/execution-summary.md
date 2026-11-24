# Test Execution Summary – Project 02

Local runs on 2026-11-23, macOS 26 arm64, headless where supported. 5 checks per configuration (known-defect tests excluded,
and reported separately below).

## PR suite (`testng.xml`) – 31.9 s
| Configuration | Passed | Failed | Blocked | Not run |
|---|---|---|---|---|
| Chrome 157 @ 1366x768 | 5 | 0 | 0 | 0 |
| Chrome 157 @ 390x844 | 5 | 0 | 0 | 0 |
| Firefox 160 @ 1366x768 | 5 | 0 | 0 | 0 |
| Firefox 160 @ 390x844 (actual 500x759) | 5 | 0 | 0 | 0 |
| **Total** | **20** | **0** | **0** | 8 known-defect excluded |

## Full matrix (`testng-matrix.xml`) – parallel part 189 s, Safari part 213 s
| Browser | 1920x1080 | 1366x768 | 768x1024 | 390x844 | iPhone 14 Pro Max (emu) | Pixel 7 (emu) |
|---|---|---|---|---|---|---|
| Chrome 157 | 5/5 ✅ | 5/5 ✅ | 5/5 ✅ | 5/5 ✅ | 5/5 ✅ | 5/5 ✅ |
| Firefox 160 | 5/5 ✅ | 5/5 ✅ | 5/5 ✅ | 5/5 ✅ (500 px) | n/a | n/a |
| Edge 158 | 5/5 ✅ | 1 pass + 4 blocked → rerun **5/5 ✅** | 5/5 ✅ | 5/5 ✅ | n/a | n/a |
| Safari | 5 blocked | 5 blocked | 5 blocked | 5 blocked | n/a | n/a |

| | Passed | Failed | Blocked | Not run |
|---|---|---|---|---|
| First matrix run (90 test executions) | 66 | 0 | 24 | 0 |
| After rerunning Edge 1366x768 | 70 | 0 | 20 (Safari) | 0 |

**Blocked reasons**
* Safari × 4 (20 tests): `SessionNotCreatedException`, because Safari remote automation is not enabled on this machine.
  Action: run `sudo safaridriver --enable` once (needs admin), or run on BrowserStack.
* Edge 1366x768 × 4 (first run only): the msedgedriver/Edge download by Selenium Manager was still in progress when the first
  parallel session started. A rerun passed 5/5.

## Known-defect group (`-Dgroups=known-defect`)
| Test | Chrome 1366 | Chrome 390 | Firefox 1366 | Firefox 390 | Defect |
|---|---|---|---|---|---|
| visualUserLayoutMatchesBaseline | ❌ | ❌ | ❌ | ❌ | DEF-02-001 |
| touchTargetsLargeEnough | ❌ | ❌ | ❌ | ❌ | DEF-02-002 |
| **8 executed, 8 failed as expected** | | | | | |

## Issues fixed in the test code during execution
1. Chrome at 390x844 rendered at 500 px (minimum desktop window width). Fixed with CDP viewport emulation.
2. Firefox burger links were measured mid-animation. Fixed by waiting for the slide-in to finish (in the project 04 `Header` component).
3. Screenshots were taken before fonts and images had painted. Fixed with `Layout.waitForRender()`.
4. The suite XML comment contained `--`, which is invalid XML. Fixed.
