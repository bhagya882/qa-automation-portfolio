package com.qaportfolio.mobile.screens;

import com.qaportfolio.core.config.Config;
import com.qaportfolio.mobile.support.Mobile;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/** Shared helpers for My Demo App screens (resource-id and accessibility-id locators, explicit waits). */
public abstract class BaseScreen {

    protected static final String PKG = "com.saucelabs.mydemoapp.android:id/";

    protected final AppiumDriver driver = Mobile.driver();
    protected final WebDriverWait wait = new WebDriverWait(driver, Config.getSeconds("timeout.explicit", 15));

    public static By id(String resourceId) {
        return AppiumBy.id(PKG + resourceId);
    }

    public static By a11y(String contentDesc) {
        return AppiumBy.accessibilityId(contentDesc);
    }

    public static By text(String text) {
        return AppiumBy.androidUIAutomator("new UiSelector().text(\"" + text + "\")");
    }

    protected WebElement visible(By by) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    protected void tap(By by) {
        wait.until(ExpectedConditions.elementToBeClickable(by)).click();
    }

    protected void type(By by, String value) {
        WebElement el = visible(by);
        el.clear();
        el.sendKeys(value);
    }

    protected boolean present(By by, Duration within) {
        try {
            new WebDriverWait(driver, within).until(ExpectedConditions.presenceOfElementLocated(by));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    protected List<WebElement> all(By by) {
        return driver.findElements(by);
    }

    /** Scrolls the first scrollable container until an element with the resource id is visible. */
    protected WebElement scrollToId(String resourceId) {
        return driver.findElement(AppiumBy.androidUIAutomator(
                "new UiScrollable(new UiSelector().scrollable(true).instance(0))"
                        + ".scrollIntoView(new UiSelector().resourceId(\"" + PKG + resourceId + "\"))"));
    }

    /** Cart badge in the shared header (present on every main screen); "0" when empty. */
    public String cartCount() {
        var badge = all(id("cartTV"));
        return badge.isEmpty() ? "0" : badge.get(0).getText();
    }

    protected void hideKeyboard() {
        try {
            Mobile.android().hideKeyboard();
        } catch (RuntimeException ignored) {
            // keyboard not shown
        }
    }
}
