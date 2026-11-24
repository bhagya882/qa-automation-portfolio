package com.qaportfolio.saucedemo.components;

import com.qaportfolio.core.pages.BasePage;
import com.qaportfolio.saucedemo.pages.CartPage;
import com.qaportfolio.saucedemo.pages.LoginPage;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** Top app header shared by all logged-in pages: title, cart badge, burger menu. */
public class Header extends BasePage {

    public static final By BURGER = By.id("react-burger-menu-btn");
    public static final By CART_LINK = By.cssSelector("[data-test='shopping-cart-link']");
    private static final By CART_BADGE = By.cssSelector("[data-test='shopping-cart-badge']");
    private static final By TITLE = By.cssSelector("[data-test='title']");

    public String pageTitle() {
        return text(TITLE);
    }

    /** 0 when the badge is absent. */
    public int cartCount() {
        return all(CART_BADGE).isEmpty() ? 0 : Integer.parseInt(all(CART_BADGE).get(0).getText().trim());
    }

    @Step("Open cart")
    public CartPage openCart() {
        click(CART_LINK);
        return new CartPage().waitUntilLoaded();
    }

    @Step("Open burger menu")
    public BurgerMenu openMenu() {
        click(BURGER);
        BurgerMenu menu = new BurgerMenu();
        menu.waitUntilOpen();
        return menu;
    }

    public static class BurgerMenu extends BasePage {
        public static final By ALL_ITEMS = By.id("inventory_sidebar_link");
        public static final By ABOUT = By.id("about_sidebar_link");
        public static final By LOGOUT = By.id("logout_sidebar_link");
        public static final By RESET = By.id("reset_sidebar_link");
        public static final By CLOSE = By.id("react-burger-cross-btn");
        private static final By WRAP = By.cssSelector(".bm-menu-wrap");

        void waitUntilOpen() {
            wait.until(ExpectedConditions.attributeToBe(WRAP, "aria-hidden", "false"));
            visible(LOGOUT);
            // The menu slides in with a CSS transition; wait until it has fully arrived
            wait.until(d -> Boolean.TRUE.equals(js(
                    "return arguments[0].getBoundingClientRect().left >= 0;", d.findElement(LOGOUT))));
        }

        public boolean isOpen() {
            return "false".equals(driver.findElement(WRAP).getDomAttribute("aria-hidden"));
        }

        public java.util.List<String> itemLabels() {
            return all(By.cssSelector(".bm-item-list a")).stream().map(e -> e.getText().trim()).toList();
        }

        @Step("Log out via burger menu")
        public LoginPage logout() {
            click(LOGOUT);
            return new LoginPage().waitUntilLoaded();
        }

        @Step("Reset app state via burger menu")
        public BurgerMenu resetAppState() {
            click(RESET);
            return this;
        }

        public String aboutHref() {
            return visible(ABOUT).getDomProperty("href");
        }

        @Step("Close burger menu")
        public void close() {
            click(CLOSE);
            wait.until(ExpectedConditions.attributeToBe(WRAP, "aria-hidden", "true"));
        }
    }
}
