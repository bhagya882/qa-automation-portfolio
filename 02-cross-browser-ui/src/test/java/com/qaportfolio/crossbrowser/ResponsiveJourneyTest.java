package com.qaportfolio.crossbrowser;

import com.qaportfolio.core.driver.DriverManager;
import com.qaportfolio.core.util.Evidence;
import com.qaportfolio.saucedemo.components.Header;
import com.qaportfolio.saucedemo.model.CheckoutInfo;
import com.qaportfolio.saucedemo.model.Products;
import com.qaportfolio.saucedemo.model.SortOption;
import com.qaportfolio.saucedemo.pages.CartPage;
import com.qaportfolio.saucedemo.pages.CheckoutCompletePage;
import com.qaportfolio.saucedemo.pages.CheckoutOverviewPage;
import com.qaportfolio.saucedemo.pages.InventoryPage;
import com.qaportfolio.saucedemo.pages.LoginPage;
import com.qaportfolio.saucedemo.pages.SauceDemo;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.assertj.core.api.SoftAssertions;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Key journeys executed once per (browser, viewport/device) combination defined in the suite XML.
 * Each page in the journey is screenshotted (target/screenshots) and checked for responsive-layout problems.
 */
@Feature("Key journeys across browsers and viewports")
public class ResponsiveJourneyTest extends BaseCrossBrowserTest {

    private static final By LOGIN_BUTTON = By.id("login-button");
    private static final By FIRST_ADD_BUTTON = By.cssSelector("[data-test='inventory-item'] button.btn_inventory");
    private static final By CHECKOUT = By.cssSelector("[data-test='checkout']");
    private static final By FINISH = By.cssSelector("[data-test='finish']");

    @Test(groups = {"smoke", "cross-browser"}, description = "Login page renders without overflow; login button usable")
    @Story("Login layout")
    @Severity(SeverityLevel.CRITICAL)
    public void loginPageLayout() {
        SauceDemo.open();
        Layout.attach("actual viewport", Layout.viewport());
        Layout.waitForRender();
        Evidence.screenshot("01-login");
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(Layout.horizontalOverflow()).as("horizontal overflow px").isLessThanOrEqualTo(0);
        soft.assertThat(Layout.usabilityProblem(LOGIN_BUTTON)).as("login button problem").isNull();
        soft.assertThat(Layout.usabilityProblem(By.id("user-name"))).as("username field problem").isNull();
        soft.assertAll();
    }

    @Test(groups = {"smoke", "cross-browser"}, description = "Login → add to cart → checkout → confirmation, layout checked per step")
    @Story("Purchase journey")
    @Severity(SeverityLevel.BLOCKER)
    public void purchaseJourney() {
        SoftAssertions soft = new SoftAssertions();

        InventoryPage inv = loginAsStandard();
        Layout.waitForRender();
        Evidence.screenshot("02-inventory");
        checkPage(soft, "inventory", Header.CART_LINK, Header.BURGER, FIRST_ADD_BUTTON);
        soft.assertThat(Layout.elementsOverflowingRight("[data-test='inventory-item'], .inventory_item_img, button"))
                .as("elements overflowing the right edge on inventory").isEmpty();

        inv.addToCart(Products.BACKPACK).addToCart(Products.BOLT_TSHIRT);
        assertThat(inv.header().cartCount()).isEqualTo(2);

        CartPage cart = inv.header().openCart();
        Layout.waitForRender();
        Evidence.screenshot("03-cart");
        checkPage(soft, "cart", CHECKOUT);

        CheckoutOverviewPage overview = cart.checkout().fill(CheckoutInfo.random()).continueToOverview();
        Layout.waitForRender();
        Evidence.screenshot("04-overview");
        checkPage(soft, "overview", FINISH);
        assertThat(overview.total()).isEqualByComparingTo(overview.itemTotal().add(overview.tax()));

        CheckoutCompletePage done = overview.finish();
        Layout.waitForRender();
        Evidence.screenshot("05-complete");
        assertThat(done.confirmationHeader()).isEqualTo("Thank you for your order!");
        soft.assertAll();
    }

    @Test(groups = "cross-browser", description = "Sorting by price works identically in every browser")
    @Story("Sorting")
    @Severity(SeverityLevel.NORMAL)
    public void sortingByPrice() {
        InventoryPage inv = loginAsStandard();
        List<BigDecimal> asc = inv.sortBy(SortOption.PRICE_ASC).prices();
        assertThat(asc).isSorted();
        List<BigDecimal> desc = inv.sortBy(SortOption.PRICE_DESC).prices();
        assertThat(desc).isSortedAccordingTo(Comparator.reverseOrder());
        List<String> za = inv.sortBy(SortOption.NAME_DESC).productNames();
        assertThat(za).isSortedAccordingTo(Comparator.reverseOrder());
        Layout.waitForRender();
        Evidence.screenshot("06-sorted-name-desc");
    }

    @Test(groups = {"smoke", "cross-browser"}, description = "Burger menu is reachable, its links are inside the viewport, logout works")
    @Story("Navigation menu")
    @Severity(SeverityLevel.CRITICAL)
    public void burgerMenuReachable() {
        InventoryPage inv = loginAsStandard();
        assertThat(Layout.usabilityProblem(Header.BURGER)).as("burger button problem").isNull();
        Header.BurgerMenu menu = inv.header().openMenu();
        Layout.waitForRender();
        Evidence.screenshot("07-menu-open");
        SoftAssertions soft = new SoftAssertions();
        for (By link : List.of(Header.BurgerMenu.ALL_ITEMS, Header.BurgerMenu.ABOUT, Header.BurgerMenu.LOGOUT,
                Header.BurgerMenu.RESET)) {
            soft.assertThat(Layout.usabilityProblem(link)).as("menu link " + link).isNull();
        }
        soft.assertAll();
        LoginPage login = menu.logout();
        assertThat(login.isLoaded()).isTrue();
    }

    @Test(groups = {"known-defect", "cross-browser"},
            description = "DEF-02-002 touch targets are at least 24x24 CSS px (WCAG 2.2 SC 2.5.8)")
    @io.qameta.allure.Issue("DEF-02-002")
    @Story("Touch targets")
    @Severity(SeverityLevel.MINOR)
    public void touchTargetsLargeEnough() {
        loginAsStandard();
        SoftAssertions soft = new SoftAssertions();
        for (By target : List.of(Header.BURGER, Header.CART_LINK, FIRST_ADD_BUTTON)) {
            var box = Layout.size(target);
            double w = ((Number) box.get("w")).doubleValue();
            double h = ((Number) box.get("h")).doubleValue();
            soft.assertThat(Math.min(w, h)).as(target + " size " + w + "x" + h).isGreaterThanOrEqualTo(24);
        }
        soft.assertAll();
    }

    private static void checkPage(SoftAssertions soft, String page, By... mustBeUsable) {
        soft.assertThat(Layout.horizontalOverflow()).as(page + ": horizontal overflow px ("
                + DriverManager.spec().label() + ", viewport " + Layout.viewport() + ")").isLessThanOrEqualTo(0);
        for (By b : mustBeUsable) {
            soft.assertThat(Layout.usabilityProblem(b)).as(page + ": " + b).isNull();
        }
    }
}
