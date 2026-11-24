package com.qaportfolio.a11y.support;

import com.qaportfolio.core.config.Config;
import com.qaportfolio.core.driver.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Minimal navigation for the SauceDemo practice site (https://www.saucedemo.com), which is published for
 * automation practice. Deliberately independent of the 04 framework so this module stands alone.
 */
public final class SauceDemo {

    public static final By USERNAME = By.id("user-name");
    public static final By PASSWORD = By.id("password");
    public static final By LOGIN_BUTTON = By.id("login-button");
    public static final By INVENTORY_LIST = By.cssSelector("[data-test='inventory-list']");
    public static final By ADD_BACKPACK = By.id("add-to-cart-sauce-labs-backpack");
    public static final By CART_BADGE = By.cssSelector("[data-test='shopping-cart-badge']");
    public static final By CART_LINK = By.cssSelector("[data-test='shopping-cart-link']");
    public static final By CHECKOUT = By.id("checkout");
    public static final By FIRST_NAME = By.id("first-name");
    public static final By LAST_NAME = By.id("last-name");
    public static final By POSTAL_CODE = By.id("postal-code");
    public static final By CONTINUE = By.id("continue");
    public static final By FINISH = By.id("finish");
    public static final By COMPLETE_HEADER = By.cssSelector("[data-test='complete-header']");
    public static final By BURGER = By.id("react-burger-menu-btn");
    public static final By BURGER_CLOSE = By.id("react-burger-cross-btn");
    public static final By MENU_WRAP = By.cssSelector(".bm-menu-wrap");
    public static final By PRODUCT_DETAILS = By.cssSelector("[data-test='inventory-item']");

    /** Pages audited, in user-journey order. */
    public enum Page {
        LOGIN, INVENTORY, PRODUCT_DETAILS, CART, CHECKOUT_STEP_ONE, CHECKOUT_STEP_TWO, CHECKOUT_COMPLETE
    }

    private SauceDemo() {
    }

    private static WebDriver d() {
        return DriverManager.get();
    }

    private static WebDriverWait w() {
        return new WebDriverWait(d(), Config.getSeconds("timeout.explicit", 10));
    }

    public static String baseUrl() {
        return Config.get("base.url", "https://www.saucedemo.com");
    }

    public static void openLogin() {
        d().get(baseUrl() + "/");
        w().until(ExpectedConditions.visibilityOfElementLocated(USERNAME));
        waitForFonts();
    }

    /** Web fonts load late on SauceDemo; wait so evidence screenshots show text. */
    public static void waitForFonts() {
        w().until(dr -> "loaded".equals(((org.openqa.selenium.JavascriptExecutor) dr)
                .executeScript("return document.fonts.status;")));
    }

    /** Logs in from a clean state (SauceDemo keeps the cart in localStorage and the session in a cookie). */
    public static void login() {
        openLogin();
        d().manage().deleteAllCookies();
        ((org.openqa.selenium.JavascriptExecutor) d()).executeScript("localStorage.clear(); sessionStorage.clear();");
        openLogin();
        d().findElement(USERNAME).sendKeys(Config.get("sauce.user", "standard_user"));
        d().findElement(PASSWORD).sendKeys(Config.get("sauce.password", "secret_sauce"));
        d().findElement(LOGIN_BUTTON).click();
        w().until(ExpectedConditions.visibilityOfElementLocated(INVENTORY_LIST));
    }

    public static WebElement waitVisible(By by) {
        return w().until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    /** Navigates from a fresh browser to the given page through the real UI flow. */
    public static void goTo(Page page) {
        switch (page) {
            case LOGIN -> openLogin();
            case INVENTORY -> login();
            case PRODUCT_DETAILS -> {
                login();
                d().findElement(By.cssSelector("[data-test='item-4-title-link']")).click();
                waitVisible(PRODUCT_DETAILS);
            }
            case CART -> {
                login();
                d().findElement(ADD_BACKPACK).click();
                d().findElement(CART_LINK).click();
                waitVisible(CHECKOUT);
            }
            case CHECKOUT_STEP_ONE -> {
                goTo(Page.CART);
                d().findElement(CHECKOUT).click();
                waitVisible(FIRST_NAME);
            }
            case CHECKOUT_STEP_TWO -> {
                goTo(Page.CHECKOUT_STEP_ONE);
                d().findElement(FIRST_NAME).sendKeys("Test");
                d().findElement(LAST_NAME).sendKeys("User");
                d().findElement(POSTAL_CODE).sendKeys("10001");
                d().findElement(CONTINUE).click();
                waitVisible(FINISH);
            }
            case CHECKOUT_COMPLETE -> {
                goTo(Page.CHECKOUT_STEP_TWO);
                d().findElement(FINISH).click();
                waitVisible(COMPLETE_HEADER);
            }
        }
    }
}
