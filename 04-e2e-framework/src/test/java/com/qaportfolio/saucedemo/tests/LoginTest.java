package com.qaportfolio.saucedemo.tests;

import com.qaportfolio.saucedemo.model.User;
import com.qaportfolio.saucedemo.model.Users;
import com.qaportfolio.saucedemo.pages.InventoryPage;
import com.qaportfolio.saucedemo.pages.LoginPage;
import com.qaportfolio.saucedemo.pages.SauceDemo;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Login")
public class LoginTest extends BaseSauceTest {

    @DataProvider(name = "usersAllowedToLogin", parallel = true)
    public Object[][] usersAllowedToLogin() {
        return Users.all().stream().filter(User::canLogin).map(u -> new Object[]{u}).toArray(Object[][]::new);
    }

    @DataProvider(name = "invalidCredentials", parallel = true)
    public Object[][] invalidCredentials() {
        String pw = Users.standard().password();
        return new Object[][]{
                {"", pw, "Epic sadface: Username is required"},
                {"standard_user", "", "Epic sadface: Password is required"},
                {"standard_user", "wrong_password", "Epic sadface: Username and password do not match any user in this service"},
                {"unknown_user", pw, "Epic sadface: Username and password do not match any user in this service"},
                {"STANDARD_USER", pw, "Epic sadface: Username and password do not match any user in this service"},
                {" standard_user", pw, "Epic sadface: Username and password do not match any user in this service"},
                {"' OR '1'='1", "' OR '1'='1", "Epic sadface: Username and password do not match any user in this service"},
        };
    }

    @Test(dataProvider = "usersAllowedToLogin", groups = {"smoke", "regression"},
            description = "Valid demo users land on the Products page")
    @Story("Successful login")
    @Severity(SeverityLevel.BLOCKER)
    public void validUserCanLogIn(User user) {
        InventoryPage inventory = loginAs(user);
        assertThat(inventory.header().pageTitle()).isEqualTo("Products");
        assertThat(inventory.currentUrl()).endsWith("/inventory.html");
    }

    @Test(groups = "regression", description = "Locked-out user sees a lock message and stays on login")
    @Story("Locked account")
    @Severity(SeverityLevel.CRITICAL)
    public void lockedOutUserIsRejected() {
        User locked = Users.lockedOut();
        LoginPage login = SauceDemo.open().loginExpectingError(locked.username(), locked.password());
        assertThat(login.errorMessage()).isEqualTo(locked.expectedError());
        assertThat(login.isLoaded()).isTrue();
    }

    @Test(dataProvider = "invalidCredentials", groups = "regression",
            description = "Invalid/empty credentials show the correct validation message")
    @Story("Negative login")
    @Severity(SeverityLevel.CRITICAL)
    public void invalidCredentialsShowError(String username, String password, String expectedError) {
        LoginPage login = SauceDemo.open().loginExpectingError(username, password);
        assertThat(login.errorMessage()).isEqualTo(expectedError);
        assertThat(login.usernameMarkedInvalid()).as("username field highlighted").isTrue();
    }

    @Test(groups = "regression", description = "Error banner can be dismissed")
    @Story("Negative login")
    @Severity(SeverityLevel.MINOR)
    public void errorCanBeDismissed() {
        LoginPage login = SauceDemo.open().loginExpectingError("x", "y").dismissError();
        assertThat(login.hasError()).isFalse();
    }

    @Test(groups = "regression", description = "Password input is masked")
    @Story("Login form")
    @Severity(SeverityLevel.NORMAL)
    public void passwordIsMasked() {
        assertThat(SauceDemo.open().passwordFieldType()).isEqualTo("password");
    }

    @Test(groups = {"smoke", "regression"}, description = "Protected pages redirect anonymous users to login")
    @Story("Access control")
    @Severity(SeverityLevel.CRITICAL)
    public void inventoryRequiresLogin() {
        SauceDemo.openPath("/inventory.html");
        LoginPage login = new LoginPage().waitUntilLoaded();
        assertThat(login.errorMessage())
                .isEqualTo("Epic sadface: You can only access '/inventory.html' when you are logged in.");
    }
}
