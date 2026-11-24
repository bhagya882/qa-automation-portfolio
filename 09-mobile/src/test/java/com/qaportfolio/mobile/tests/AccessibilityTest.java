package com.qaportfolio.mobile.tests;

import com.qaportfolio.mobile.screens.BaseScreen;
import com.qaportfolio.mobile.screens.CatalogScreen;
import com.qaportfolio.mobile.screens.LoginScreen;
import com.qaportfolio.mobile.support.BaseMobileTest;
import com.qaportfolio.mobile.support.Device;
import com.qaportfolio.mobile.support.MobileEvidenceListener;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** TalkBack-relevant checks: content descriptions, labels, touch-target size (Android guideline 48dp). */
public class AccessibilityTest extends BaseMobileTest {

    @Test(description = "MOB-50 Key controls expose a content-description")
    public void keyControlsHaveContentDescription() {
        new CatalogScreen().waitLoaded();
        List<String> missing = new ArrayList<>();
        for (String id : List.of("menuIV", "sortIV", "cartRL", "productIV", "productRV")) {
            WebElement el = driver().findElement(BaseScreen.id(id));
            String desc = el.getAttribute("content-desc");
            if (blank(desc)) {
                missing.add(id);
            }
        }
        assertThat(missing).as("controls without content-desc").isEmpty();
    }

    @Test(groups = "known-defect", description = "MOB-51 Every clickable element has a content-desc or text")
    public void clickableElementsAreLabelled() {
        new CatalogScreen().waitLoaded();
        List<String> unlabeled = unlabeledClickables();
        if (!unlabeled.isEmpty()) {
            MobileEvidenceListener.capture("MOB-51_unlabeled_clickables");
        }
        assertThat(unlabeled).as("clickable elements TalkBack can only announce as 'button'").isEmpty();
    }

    @Test(groups = "known-defect", description = "MOB-52 Clickable targets are at least 48x48 dp")
    public void touchTargetsAtLeast48dp() {
        new CatalogScreen().waitLoaded();
        double density = Device.density();
        List<String> small = new ArrayList<>();
        for (WebElement el : driver().findElements(By.xpath("//*[@clickable='true']"))) {
            var r = el.getRect();
            double w = r.getWidth() / density;
            double h = r.getHeight() / density;
            if (r.getWidth() > 0 && (w < 48 || h < 48)) {
                small.add(el.getAttribute("resource-id").replace("com.saucelabs.mydemoapp.android:id/", "")
                        + String.format(" %.0fx%.0fdp", w, h));
            }
        }
        assertThat(small).as("touch targets below 48dp (density %.2f)", density).isEmpty();
    }

    @Test(groups = "known-defect", description = "MOB-53 Login inputs have an accessible label (hint/content-desc)")
    public void loginInputsLabelled() {
        new CatalogScreen().waitLoaded().openMenu().openLogin();
        List<String> unlabeled = new ArrayList<>();
        for (By by : List.of(LoginScreen.USERNAME, LoginScreen.PASSWORD)) {
            WebElement el = driver().findElement(by);
            String desc = el.getAttribute("content-desc");
            String hint = el.getAttribute("hint");
            if (blank(desc) && blank(hint)) {
                unlabeled.add(el.getAttribute("resource-id"));
            }
        }
        assertThat(unlabeled).as("EditTexts without content-desc or hint").isEmpty();
    }

    /** UiAutomator2 returns the string "null" for attributes that are not set. */
    private static boolean blank(String s) {
        return s == null || s.isBlank() || "null".equals(s);
    }

    private List<String> unlabeledClickables() {
        List<String> out = new ArrayList<>();
        for (WebElement el : driver().findElements(By.xpath("//*[@clickable='true']"))) {
            String desc = el.getAttribute("content-desc");
            String text = el.getText();
            if (blank(desc) && blank(text)) {
                out.add(el.getAttribute("resource-id").replace("com.saucelabs.mydemoapp.android:id/", ""));
            }
        }
        return out;
    }
}
