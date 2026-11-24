package com.qaportfolio.a11y.tests;

import com.qaportfolio.a11y.support.Keyboard;
import com.qaportfolio.a11y.support.SauceDemo;
import com.qaportfolio.core.BaseUiTest;
import com.qaportfolio.core.config.Config;
import com.qaportfolio.core.util.Evidence;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** WCAG 2.1.1 Keyboard, 2.1.2 No Keyboard Trap, 2.4.3 Focus Order, 2.4.7 / 2.4.11 Focus Visible/Not Obscured. */
@Feature("Accessibility – keyboard")
public class KeyboardNavigationTest extends BaseUiTest {

    @Test(description = "A11Y-KB-01 Tab order on login is username → password → login button")
    @Description("WCAG 2.4.3 Focus Order")
    public void loginFormTabOrder() {
        SauceDemo.openLogin();
        Keyboard.resetFocusToDocumentStart();
        List<String> order = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Keyboard.tab();
            order.add(Keyboard.activeId());
        }
        assertThat(order).containsExactly("user-name", "password", "login-button");

        Keyboard.shiftTab();
        assertThat(Keyboard.activeId()).as("Shift+Tab moves focus backwards").isEqualTo("password");
    }

    @Test(description = "A11Y-KB-02 Login can be completed with keyboard only (Enter submits)")
    @Description("WCAG 2.1.1 Keyboard")
    public void loginWithKeyboardOnly() {
        SauceDemo.openLogin();
        Keyboard.resetFocusToDocumentStart();
        Keyboard.tab();
        Keyboard.press(Config.get("sauce.user", "standard_user"));
        Keyboard.tab();
        Keyboard.press(Config.get("sauce.password", "secret_sauce"));
        Keyboard.press(Keys.ENTER);
        SauceDemo.waitVisible(SauceDemo.INVENTORY_LIST);
        assertThat(driver().getCurrentUrl()).contains("inventory.html");
    }

    @Test(description = "A11Y-KB-03 Add to cart is reachable and operable with keyboard only")
    @Description("WCAG 2.1.1 Keyboard")
    public void addToCartWithKeyboardOnly() {
        SauceDemo.login();
        Keyboard.resetFocusToDocumentStart();
        int presses = Keyboard.tabUntil("add-to-cart-sauce-labs-backpack", 40);
        assertThat(presses).as("Tab presses to reach 'Add to cart' for the first product").isPositive();
        Keyboard.press(Keys.ENTER);
        WebElement badge = SauceDemo.waitVisible(SauceDemo.CART_BADGE);
        assertThat(badge.getText()).isEqualTo("1");
    }

    @Test(groups = "known-defect",
            description = "A11Y-KB-03b Focus is kept on the Add/Remove toggle after activating it")
    @Description("WCAG 2.4.3 Focus Order — see DEF-A11Y-02")
    public void focusKeptAfterAddToCart() {
        SauceDemo.login();
        Keyboard.resetFocusToDocumentStart();
        Keyboard.tabUntil("add-to-cart-sauce-labs-backpack", 40);
        Keyboard.press(Keys.ENTER);
        SauceDemo.waitVisible(SauceDemo.CART_BADGE);
        String focused = Keyboard.activeId();
        if (!"remove-sauce-labs-backpack".equals(focused)) {
            Evidence.screenshot("A11Y-KB-03b_focus_lost_after_add_to_cart");
        }
        assertThat(focused)
                .as("focus should stay on the toggled control (now 'Remove') instead of being reset to <body>")
                .isEqualTo("remove-sauce-labs-backpack");
        Keyboard.tab();
        assertThat(Keyboard.activeId()).as("next Tab after activation continues from the product, not page top")
                .isNotEqualTo("react-burger-menu-btn");
    }

    @Test(description = "A11Y-KB-04 Burger menu opens with Enter, moves focus into the menu, closes and is not a trap")
    @Description("WCAG 2.1.1 Keyboard, 2.1.2 No Keyboard Trap, 2.4.3 Focus Order")
    public void burgerMenuKeyboard() {
        SauceDemo.login();
        Keyboard.resetFocusToDocumentStart();
        assertThat(Keyboard.tabUntil("react-burger-menu-btn", 10)).as("burger button reachable by Tab").isPositive();
        Keyboard.press(Keys.ENTER);
        new WebDriverWait(driver(), Duration.ofSeconds(5)).until(
                ExpectedConditions.attributeToBe(SauceDemo.MENU_WRAP, "aria-hidden", "false"));
        Keyboard.tab();
        Boolean inMenu = (Boolean) Keyboard.js(
                "return !!document.activeElement.closest('.bm-menu-wrap');");
        assertThat(inMenu).as("first Tab after opening lands inside the menu (focused: %s)", Keyboard.activeId())
                .isTrue();
        // Close using the close button via keyboard
        Keyboard.tabUntil("react-burger-cross-btn", 10);
        Keyboard.press(Keys.ENTER);
        new WebDriverWait(driver(), Duration.ofSeconds(5)).until(
                ExpectedConditions.attributeToBe(SauceDemo.MENU_WRAP, "aria-hidden", "true"));
        // Not trapped: tabbing continues into the page content
        assertThat(Keyboard.tabUntil("add-to-cart-sauce-labs-backpack", 40)).isPositive();
    }

    @Test(description = "A11Y-KB-05 Escape closes the open burger menu (expected dialog/menu behaviour)")
    @Description("WCAG 2.1.1 Keyboard; ARIA APG disclosure/dialog pattern")
    public void burgerMenuClosesWithEscape() {
        SauceDemo.login();
        Keyboard.resetFocusToDocumentStart();
        Keyboard.tabUntil("react-burger-menu-btn", 10);
        Keyboard.press(Keys.ENTER);
        new WebDriverWait(driver(), Duration.ofSeconds(5)).until(
                ExpectedConditions.attributeToBe(SauceDemo.MENU_WRAP, "aria-hidden", "false"));
        waitMenuFullyOpen();
        Keyboard.press(Keys.ESCAPE);
        try {
            new WebDriverWait(driver(), Duration.ofSeconds(3)).until(
                    ExpectedConditions.attributeToBe(SauceDemo.MENU_WRAP, "aria-hidden", "true"));
        } catch (RuntimeException timeout) {
            Evidence.screenshot("A11Y-KB-05_menu_still_open_after_Escape");
            throw new AssertionError("Burger menu is still open after pressing Escape", timeout);
        }
    }

    /** Waits until the slide-in animation has finished (menu fully on-screen). */
    private void waitMenuFullyOpen() {
        new WebDriverWait(driver(), Duration.ofSeconds(5)).until(d -> (Boolean) Keyboard.js(
                "const r=document.querySelector('.bm-menu-wrap').getBoundingClientRect(); return r.left >= 0;"));
    }

    @Test(groups = "known-defect", description = "A11Y-KB-06 Login controls show a visible focus indicator")
    @Description("WCAG 2.4.7 Focus Visible — see DEF-A11Y-01")
    public void loginFocusIndicatorVisible() {
        SauceDemo.openLogin();
        Keyboard.resetFocusToDocumentStart();
        for (String id : List.of("user-name", "password", "login-button")) {
            Keyboard.tab();
            Map<String, Object> style = Keyboard.focusStyle();
            Evidence.screenshot("A11Y-KB-06_focus_" + id);
            assertThat(Keyboard.hasVisibleFocusIndicator(style))
                    .as("visible focus indicator on #%s, computed %s", id, style).isTrue();
        }
    }

    @Test(groups = "known-defect", description = "A11Y-KB-07 Inventory controls show a visible focus indicator")
    @Description("WCAG 2.4.7 Focus Visible — see DEF-A11Y-01")
    public void inventoryFocusIndicatorVisible() {
        SauceDemo.login();
        Keyboard.resetFocusToDocumentStart();
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Keyboard.tab();
            Map<String, Object> style = Keyboard.focusStyle();
            if (!Keyboard.hasVisibleFocusIndicator(style)) {
                missing.add(style.get("tag") + "#" + style.get("id") + " " + style);
            }
        }
        if (!missing.isEmpty()) {
            Evidence.screenshot("A11Y-KB-07_focus_indicator");
        }
        assertThat(missing).as("focused elements without a visible indicator").isEmpty();
    }
}
