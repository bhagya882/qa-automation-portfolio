package com.qaportfolio.saucedemo.pages;

import com.qaportfolio.core.pages.BasePage;
import com.qaportfolio.saucedemo.model.User;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class LoginPage extends BasePage {

    private static final By USERNAME = By.id("user-name");
    private static final By PASSWORD = By.id("password");
    private static final By LOGIN = By.id("login-button");
    private static final By ERROR = By.cssSelector("[data-test='error']");
    private static final By ERROR_CLOSE = By.cssSelector("[data-test='error-button']");

    public LoginPage waitUntilLoaded() {
        visible(LOGIN);
        return this;
    }

    @Step("Log in as {user}")
    public InventoryPage loginAs(User user) {
        submit(user.username(), user.password());
        return new InventoryPage().waitUntilLoaded();
    }

    @Step("Submit credentials username='{username}'")
    public LoginPage submit(String username, String password) {
        type(USERNAME, username);
        type(PASSWORD, password);
        click(LOGIN);
        return this;
    }

    @Step("Attempt invalid login username='{username}'")
    public LoginPage loginExpectingError(String username, String password) {
        submit(username, password);
        visible(ERROR);
        return this;
    }

    public String errorMessage() {
        return text(ERROR);
    }

    public boolean hasError() {
        return isDisplayed(ERROR);
    }

    @Step("Dismiss error message")
    public LoginPage dismissError() {
        click(ERROR_CLOSE);
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(LOGIN);
    }

    public String passwordFieldType() {
        return visible(PASSWORD).getDomProperty("type");
    }

    /** Field highlighted red after failed validation. */
    public boolean usernameMarkedInvalid() {
        String cls = visible(USERNAME).getDomAttribute("class");
        return cls != null && cls.contains("error");
    }
}
