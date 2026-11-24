package com.qaportfolio.crossbrowser;

import com.qaportfolio.core.driver.DriverManager;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Map;

/** Responsive-layout assertions implemented with small DOM/JS probes (no pixel diffing). */
public final class Layout {

    private Layout() {
    }

    private static JavascriptExecutor js() {
        return (JavascriptExecutor) DriverManager.get();
    }

    /** Actual CSS viewport (innerWidth x innerHeight), which can differ from the requested window size. */
    public static String viewport() {
        Object v = js().executeScript("return window.innerWidth + 'x' + window.innerHeight;");
        return String.valueOf(v);
    }

    /** Returns the horizontal overflow in px (0 = no horizontal scrollbar). */
    @Step("Check page has no horizontal overflow")
    public static long horizontalOverflow() {
        Number n = (Number) js().executeScript(
                "const d=document.documentElement; return Math.max(d.scrollWidth, document.body.scrollWidth) - d.clientWidth;");
        return n.longValue();
    }

    /** Elements whose right edge sticks out of the viewport (common responsive bug), as "selector-ish" labels. */
    public static List<String> elementsOverflowingRight(String cssSelector) {
        @SuppressWarnings("unchecked")
        List<String> out = (List<String>) js().executeScript("""
                const w = document.documentElement.clientWidth;
                return [...document.querySelectorAll(arguments[0])]
                  .filter(e => { const r = e.getBoundingClientRect(); return r.width > 0 && r.right > w + 1; })
                  .map(e => (e.getAttribute('data-test') || e.id || e.className || e.tagName) + ' right=' +
                             Math.round(e.getBoundingClientRect().right) + ' > ' + w);
                """, cssSelector);
        return out;
    }

    /** Scrolls the element into view and checks it is fully inside the viewport and not covered by another element. */
    @Step("Check element is visible, in viewport and clickable (not covered)")
    public static String usabilityProblem(By locator) {
        WebElement el = DriverManager.get().findElement(locator);
        String problem = (String) js().executeScript("""
                const e = arguments[0];
                e.scrollIntoView({block: 'center', inline: 'center'});
                const r = e.getBoundingClientRect();
                if (r.width === 0 || r.height === 0) return 'zero size';
                const vw = document.documentElement.clientWidth, vh = window.innerHeight;
                if (r.left < -1 || r.top < -1 || r.right > vw + 1 || r.bottom > vh + 1)
                  return 'outside viewport rect=' + JSON.stringify(r) + ' viewport=' + vw + 'x' + vh;
                const x = r.left + r.width / 2, y = r.top + r.height / 2;
                const top = document.elementFromPoint(x, y);
                if (top && top !== e && !e.contains(top) && !top.contains(e))
                  return 'covered by <' + top.tagName.toLowerCase() + ' class="' + top.className + '">';
                return null;
                """, el);
        return problem;
    }

    /** Minimum touch-target size (WCAG 2.2 SC 2.5.8 asks for at least 24x24 CSS px). */
    public static Map<String, Object> size(By locator) {
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) js().executeScript(
                "const r=arguments[0].getBoundingClientRect(); return {w: r.width, h: r.height, x: r.left, y: r.top};",
                DriverManager.get().findElement(locator));
        return m;
    }

    /** Bounding boxes for every match, rounded, used to compare layouts between users/browsers. */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> boxes(String cssSelector) {
        return (List<Map<String, Object>>) js().executeScript("""
                return [...document.querySelectorAll(arguments[0])].map(e => {
                  const r = e.getBoundingClientRect();
                  return {id: e.getAttribute('data-test') || e.id || e.className, x: Math.round(r.left + scrollX),
                          y: Math.round(r.top + scrollY), w: Math.round(r.width), h: Math.round(r.height)};
                });""", cssSelector);
    }

    /** Waits until web fonts and all images have finished loading, plus two animation frames, before measuring. */
    public static void waitForRender() {
        js().executeAsyncScript("""
                const done = arguments[arguments.length - 1];
                const imgs = [...document.images].filter(i => !i.complete)
                  .map(i => new Promise(r => { i.onload = i.onerror = r; }));
                Promise.all([document.fonts ? document.fonts.ready : null, ...imgs])
                  .then(() => requestAnimationFrame(() => requestAnimationFrame(() => setTimeout(done, 300))));
                """);
    }

    public static void attach(String name, Object value) {
        Allure.addAttachment(name, "text/plain", String.valueOf(value));
    }
}
