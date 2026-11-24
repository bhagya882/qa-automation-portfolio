package com.qaportfolio.a11y.support;

import com.qaportfolio.core.driver.DriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.Map;

/** Keyboard-only interaction helpers (real key events via the Actions API, never element.click()). */
public final class Keyboard {

    private Keyboard() {
    }

    public static void press(CharSequence key) {
        new Actions(DriverManager.get()).sendKeys(key).perform();
    }

    public static void tab() {
        press(Keys.TAB);
    }

    public static void shiftTab() {
        new Actions(DriverManager.get()).keyDown(Keys.SHIFT).sendKeys(Keys.TAB).keyUp(Keys.SHIFT).perform();
    }

    public static WebElement active() {
        return DriverManager.get().switchTo().activeElement();
    }

    /** id, or data-test, or tag name of the focused element. */
    public static String activeId() {
        WebElement a = active();
        String id = a.getAttribute("id");
        if (id != null && !id.isBlank()) {
            return id;
        }
        String dt = a.getAttribute("data-test");
        return dt != null && !dt.isBlank() ? dt : a.getTagName();
    }

    /** Put focus at the very start of the document so the next Tab reaches the first focusable element. */
    public static void resetFocusToDocumentStart() {
        js("document.activeElement && document.activeElement.blur();"
                + "document.body.setAttribute('tabindex','-1');document.body.focus();"
                + "document.body.removeAttribute('tabindex');");
    }

    /** Tabs forward until the element with the given id/data-test has focus; returns presses used or -1. */
    public static int tabUntil(String idOrDataTest, int maxPresses) {
        for (int i = 1; i <= maxPresses; i++) {
            tab();
            if (idOrDataTest.equals(activeId())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Computed focus-indicator style of the focused element: outline style/width/color, box-shadow and
     * whether :focus-visible matches.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> focusStyle() {
        return (Map<String, Object>) js("""
                const el = document.activeElement; const cs = getComputedStyle(el);
                return { tag: el.tagName, id: el.id, outlineStyle: cs.outlineStyle, outlineWidth: cs.outlineWidth,
                         outlineColor: cs.outlineColor, boxShadow: cs.boxShadow,
                         focusVisible: el.matches(':focus-visible') };""");
    }

    public static boolean hasVisibleFocusIndicator(Map<String, Object> s) {
        boolean outline = !"none".equals(s.get("outlineStyle"))
                && parsePx(String.valueOf(s.get("outlineWidth"))) > 0
                && !String.valueOf(s.get("outlineColor")).matches("rgba\\(.*,\\s*0\\)|transparent");
        boolean shadow = s.get("boxShadow") != null && !"none".equals(s.get("boxShadow"));
        return outline || shadow;
    }

    private static double parsePx(String v) {
        try {
            return Double.parseDouble(v.replace("px", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static Object js(String script, Object... args) {
        return ((JavascriptExecutor) DriverManager.get()).executeScript(script, args);
    }
}
