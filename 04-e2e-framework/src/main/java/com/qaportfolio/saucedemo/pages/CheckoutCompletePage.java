package com.qaportfolio.saucedemo.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class CheckoutCompletePage extends LoggedInPage<CheckoutCompletePage> {

    private static final By HEADER = By.cssSelector("[data-test='complete-header']");
    private static final By TEXT = By.cssSelector("[data-test='complete-text']");
    private static final By BACK_HOME = By.cssSelector("[data-test='back-to-products']");

    @Override
    public CheckoutCompletePage waitUntilLoaded() {
        visible(HEADER);
        return this;
    }

    public String confirmationHeader() {
        return text(HEADER);
    }

    public String confirmationText() {
        return text(TEXT);
    }

    @Step("Back home")
    public InventoryPage backHome() {
        click(BACK_HOME);
        return new InventoryPage().waitUntilLoaded();
    }
}
