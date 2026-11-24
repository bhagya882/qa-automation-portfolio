package com.qaportfolio.saucedemo.audit;

import com.qaportfolio.core.BaseUiTest;
import com.qaportfolio.core.util.Evidence;
import com.qaportfolio.saucedemo.model.Users;
import com.qaportfolio.saucedemo.pages.SauceDemo;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Automates the "browser DevTools" checklist used in project 01: per page it records console errors (Console tab),
 * failed network requests >= 400 (Network tab, via Resource Timing responseStatus, Chromium) and broken images
 * (naturalWidth == 0). Results are appended to target/devtools-audit.md. Chrome only. Run with:
 * {@code ./mvnw -pl 04-e2e-framework test -Dheadless=true -Dsuite=testng-devtools.xml}
 * <p>This is an audit/report, not a gate: it never fails, findings are written up as defects.
 */
@Epic("DevTools audit")
@Feature("Console / network / images")
public class DevToolsHealthCheckTest extends BaseUiTest {

    private static final Path REPORT = Path.of("target", "devtools-audit.md");

    @DataProvider(name = "pages")
    public Object[][] pages() {
        return new Object[][]{
                {"saucedemo login", "saucedemo", null},
                {"saucedemo inventory standard_user", "saucedemo", "standard"},
                {"saucedemo inventory problem_user", "saucedemo", "problem"},
                {"saucedemo inventory error_user", "saucedemo", "error"},
                {"automationexercise home", "https://automationexercise.com/", null},
                {"automationexercise products", "https://automationexercise.com/products", null},
                {"automationexercise product_details/1", "https://automationexercise.com/product_details/1", null},
        };
    }

    @Test(dataProvider = "pages", groups = "devtools-audit")
    public void audit(String label, String target, String userKey) throws Exception {
        if ("saucedemo".equals(target)) {
            if (userKey == null) {
                SauceDemo.open();
            } else {
                SauceDemo.open().loginAs(Users.byKey(userKey));
            }
        } else {
            driver().get(target);
        }
        JavascriptExecutor js = (JavascriptExecutor) driver();
        // let late images/xhr finish
        js.executeAsyncScript("const d=arguments[arguments.length-1]; setTimeout(d, 1500);");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> failed = (List<Map<String, Object>>) js.executeScript("""
                return performance.getEntriesByType('resource').concat(performance.getEntriesByType('navigation'))
                  .filter(e => e.responseStatus >= 400)
                  .map(e => ({url: e.name, status: e.responseStatus}));""");
        @SuppressWarnings("unchecked")
        List<String> brokenImages = (List<String>) js.executeScript(
                "return [...document.images].filter(i => i.complete && i.naturalWidth === 0).map(i => i.currentSrc || i.src);");
        List<String> console = driver().manage().logs().get(LogType.BROWSER).getAll().stream()
                .filter(e -> e.getLevel().intValue() >= Level.WARNING.intValue())
                .map(LogEntry::getMessage).map(m -> m.length() > 220 ? m.substring(0, 220) + "…" : m).toList();

        Evidence.screenshot("devtools-" + label);
        StringBuilder md = new StringBuilder("\n### ").append(label).append("\n")
                .append("* URL: ").append(driver().getCurrentUrl()).append("\n")
                .append("* Console warnings/errors: ").append(console.size()).append("\n");
        console.forEach(c -> md.append("  * `").append(c.replace('`', '\'')).append("`\n"));
        md.append("* Failed requests (>=400): ").append(failed.size()).append("\n");
        failed.forEach(f -> md.append("  * ").append(f.get("status")).append(" ").append(f.get("url")).append("\n"));
        md.append("* Broken images: ").append(brokenImages.size()).append("\n");
        brokenImages.forEach(b -> md.append("  * ").append(b).append("\n"));
        Allure.addAttachment("DevTools audit", "text/markdown", md.toString());
        append(md.toString());
    }

    private static synchronized void append(String text) throws IOException {
        Files.createDirectories(REPORT.getParent());
        Files.writeString(REPORT, text, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
