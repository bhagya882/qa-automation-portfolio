package com.qaportfolio.saucedemo.pages;

import com.qaportfolio.saucedemo.model.CheckoutInfo;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

/** Checkout step one: shipping information. */
public class CheckoutInfoPage extends LoggedInPage<CheckoutInfoPage> {

    private static final By FIRST = By.cssSelector("[data-test='firstName']");
    private static final By LAST = By.cssSelector("[data-test='lastName']");
    private static final By ZIP = By.cssSelector("[data-test='postalCode']");
    private static final By CONTINUE = By.cssSelector("[data-test='continue']");
    private static final By CANCEL = By.cssSelector("[data-test='cancel']");
    private static final By ERROR = By.cssSelector("[data-test='error']");

    @Override
    public CheckoutInfoPage waitUntilLoaded() {
        visible(FIRST);
        return this;
    }

    @Step("Fill shipping info {info}")
    public CheckoutInfoPage fill(CheckoutInfo info) {
        type(FIRST, info.firstName());
        type(LAST, info.lastName());
        type(ZIP, info.postalCode());
        return this;
    }

    @Step("Continue to overview")
    public CheckoutOverviewPage continueToOverview() {
        click(CONTINUE);
        return new CheckoutOverviewPage().waitUntilLoaded();
    }

    @Step("Continue expecting validation error")
    public CheckoutInfoPage continueExpectingError() {
        click(CONTINUE);
        visible(ERROR);
        return this;
    }

    public String errorMessage() {
        return text(ERROR);
    }

    public String firstNameValue() {
        return visible(FIRST).getDomProperty("value");
    }

    public String lastNameValue() {
        return visible(LAST).getDomProperty("value");
    }

    public String postalCodeValue() {
        return visible(ZIP).getDomProperty("value");
    }

    @Step("Cancel checkout")
    public CartPage cancel() {
        click(CANCEL);
        return new CartPage().waitUntilLoaded();
    }
}
