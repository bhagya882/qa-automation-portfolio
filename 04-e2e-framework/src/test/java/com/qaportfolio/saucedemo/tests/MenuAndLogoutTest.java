package com.qaportfolio.saucedemo.tests;

import com.qaportfolio.saucedemo.components.Header;
import com.qaportfolio.saucedemo.pages.LoginPage;
import com.qaportfolio.saucedemo.pages.SauceDemo;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Navigation & session")
public class MenuAndLogoutTest extends BaseSauceTest {

    @Test(groups = "regression", description = "Burger menu contains All Items, About, Logout, Reset App State")
    @Story("Burger menu")
    @Severity(SeverityLevel.NORMAL)
    public void burgerMenuItems() {
        Header.BurgerMenu menu = loginAsStandard().header().openMenu();
        // Live site added "Dynamic Catalog" (Nov 2026); assert the core items in order, tolerate additions
        assertThat(menu.itemLabels()).containsSubsequence("All Items", "About", "Logout", "Reset App State");
        assertThat(menu.aboutHref()).startsWith("https://saucelabs.com");
        menu.close();
        assertThat(menu.isOpen()).isFalse();
    }

    @Test(groups = {"smoke", "regression"}, description = "Logout returns to login and ends the session")
    @Story("Logout")
    @Severity(SeverityLevel.CRITICAL)
    public void logoutEndsSession() {
        LoginPage login = loginAsStandard().header().openMenu().logout();
        assertThat(login.isLoaded()).isTrue();
        SauceDemo.openPath("/inventory.html");
        assertThat(new LoginPage().waitUntilLoaded().errorMessage()).contains("when you are logged in");
    }

    @Test(groups = "regression", description = "Browser Back after logout does not reveal the inventory")
    @Story("Logout")
    @Severity(SeverityLevel.CRITICAL)
    public void backAfterLogoutStaysLoggedOut() {
        loginAsStandard().header().openMenu().logout();
        driver().navigate().back();
        assertThat(new LoginPage().waitUntilLoaded().isLoaded()).isTrue();
    }
}
