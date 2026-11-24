package com.qaportfolio.saucedemo.pages;

import com.qaportfolio.core.config.Config;
import com.qaportfolio.core.driver.DriverManager;
import io.qameta.allure.Step;

/** Entry point for fluent navigation: {@code SauceDemo.open().loginAs(user).addToCart(...).openCart()...}. */
public final class SauceDemo {

    private SauceDemo() {
    }

    public static String baseUrl() {
        return Config.get("base.url", "https://www.saucedemo.com").replaceAll("/+$", "");
    }

    @Step("Open SauceDemo login page")
    public static LoginPage open() {
        DriverManager.get().get(baseUrl() + "/");
        return new LoginPage().waitUntilLoaded();
    }

    /** Deep link (used for access-control checks, e.g. inventory without login). */
    @Step("Navigate directly to {path}")
    public static void openPath(String path) {
        DriverManager.get().get(baseUrl() + path);
    }
}
