package com.qaportfolio.crossbrowser;

import com.qaportfolio.core.util.Evidence;
import com.qaportfolio.saucedemo.model.Users;
import com.qaportfolio.saucedemo.pages.SauceDemo;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.assertj.core.api.SoftAssertions;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * Compares the inventory layout of visual_user / problem_user with the standard_user baseline in the SAME
 * browser+viewport. Differences are real defects seeded in SauceDemo, so these tests are "known-defect".
 */
@Feature("Visual consistency")
public class VisualConsistencyTest extends BaseCrossBrowserTest {

    private static final String KEY_ELEMENTS =
            "[data-test='shopping-cart-link'], #react-burger-menu-btn, button.btn_inventory, img.inventory_item_img, "
                    + "[data-test='inventory-item-price'], [data-test='product-sort-container']";

    @Test(groups = {"known-defect", "cross-browser"},
            description = "DEF-02-001 visual_user: inventory layout differs from standard_user baseline")
    @Issue("DEF-02-001")
    @Story("Visual regression")
    @Severity(SeverityLevel.NORMAL)
    public void visualUserLayoutMatchesBaseline() {
        Snapshot baseline = snapshot("standard");
        Snapshot visual = snapshot("visual");
        compare(baseline, visual, "visual_user");
    }

    @Test(groups = "cross-browser", description = "Product images load (naturalWidth > 0) for standard_user")
    @Story("Images")
    @Severity(SeverityLevel.NORMAL)
    public void productImagesLoad() {
        loginAsStandard();
        assertNoBrokenImages();
    }

    private record Snapshot(List<Map<String, Object>> boxes, List<Object> imageSrcs) {
    }

    private Snapshot snapshot(String userKey) {
        driver().manage().deleteAllCookies();
        SauceDemo.open().loginAs(Users.byKey(userKey));
        Layout.waitForRender();
        Evidence.screenshot("visual-compare-" + userKey);
        @SuppressWarnings("unchecked")
        List<Object> srcs = (List<Object>) ((org.openqa.selenium.JavascriptExecutor) driver()).executeScript(
                "return [...document.querySelectorAll('img.inventory_item_img')].map(i => i.getAttribute('src'));");
        Snapshot s = new Snapshot(Layout.boxes(KEY_ELEMENTS), srcs);
        driver().findElement(By.id("react-burger-menu-btn")).click();
        return s;
    }

    private static void compare(Snapshot expected, Snapshot actual, String who) {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(actual.boxes()).as(who + " element count").hasSameSizeAs(expected.boxes());
        int n = Math.min(expected.boxes().size(), actual.boxes().size());
        for (int i = 0; i < n; i++) {
            Map<String, Object> e = expected.boxes().get(i);
            Map<String, Object> a = actual.boxes().get(i);
            for (String k : List.of("x", "y", "w", "h")) {
                long ev = ((Number) e.get(k)).longValue();
                long av = ((Number) a.get(k)).longValue();
                soft.assertThat(Math.abs(ev - av)).as(who + " " + e.get("id") + " #" + i + " " + k
                        + " (baseline " + ev + ", actual " + av + ")").isLessThanOrEqualTo(2);
            }
        }
        soft.assertThat(actual.imageSrcs()).as(who + " product images").containsExactlyElementsOf(expected.imageSrcs());
        soft.assertAll();
    }

    private void assertNoBrokenImages() {
        @SuppressWarnings("unchecked")
        List<String> broken = (List<String>) ((org.openqa.selenium.JavascriptExecutor) driver()).executeAsyncScript("""
                const done = arguments[arguments.length - 1];
                const imgs = [...document.images];
                Promise.all(imgs.map(i => i.complete ? null : new Promise(r => { i.onload = i.onerror = r; })))
                  .then(() => done(imgs.filter(i => i.naturalWidth === 0).map(i => i.src)));
                """);
        org.assertj.core.api.Assertions.assertThat(broken).as("broken images").isEmpty();
    }
}
