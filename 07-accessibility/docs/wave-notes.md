# WAVE Usage Notes

WAVE (https://wave.webaim.org) runs in the browser on the rendered DOM, so it works behind the SauceDemo login.

1. Install the **WAVE Evaluation Tool** extension for Chrome or Firefox.
2. Log in to SauceDemo, then open each page from the [scope](test-plan.md#1-scope) and click the WAVE icon.
3. On each page, review the following panels:
   - **Errors** (red). Compare them with the axe report; WAVE and axe use different rule sets.
   - **Contrast errors**. Use this to resolve the `color-contrast` items that axe marked *incomplete*.
   - **Alerts**: "Missing first level heading" (expected: DEF-A11Y-05), "No page regions", and "Redundant title text".
   - **Structure** tab: confirm the heading outline and landmarks.
   - **Order** tab: shows the tab sequence. Cross-check it with `loginFormTabOrder`.
4. Toggle **"No Styles"** to check the reading order without CSS (1.3.2 Meaningful Sequence).
5. Save evidence: a screenshot of the WAVE sidebar with its counts, saved to `docs/evidence/wave/<page>.png`.

Expected based on the automated run: no WCAG A/AA errors; alerts for missing headings and regions; possible
contrast flags on the grey description text. Placeholder-only inputs show as an alert ("Missing form label"), not an error.
