package com.qaportfolio.mobile.tests;

import com.qaportfolio.core.config.Config;
import com.qaportfolio.mobile.screens.CartScreen;
import com.qaportfolio.mobile.screens.CatalogScreen;
import com.qaportfolio.mobile.screens.CheckoutScreen;
import com.qaportfolio.mobile.screens.LoginScreen;
import com.qaportfolio.mobile.screens.ProductDetailsScreen;
import com.qaportfolio.mobile.support.BaseMobileTest;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Automated core journey: browse → add to cart → login → checkout → order complete. */
public class CoreJourneyTest extends BaseMobileTest {

    @Test(description = "MOB-20 Core purchase journey completes")
    public void purchaseJourney() {
        CatalogScreen catalog = new CatalogScreen().waitLoaded();
        ProductDetailsScreen details = catalog.openProduct(0);
        String product = details.name();
        details.increaseQuantity();
        assertThat(details.quantity()).isEqualTo("2");
        details.addToCart();
        assertThat(details.cartCount()).isEqualTo("2");

        CartScreen cart = details.openCart().waitLoaded();
        assertThat(cart.itemTitles()).containsExactly(product);
        assertThat(cart.itemsCount()).isEqualTo("2 Items");
        cart.proceedToCheckout();

        new LoginScreen().waitLoaded().login(Config.get("mda.user"), Config.get("mda.password"));

        CheckoutScreen checkout = new CheckoutScreen().waitForAddressForm()
                .fillAddress("Test User", "1 Main Street", "Testville", "12345", "Testland")
                .toPayment()
                .fillPayment("Test User", "4111111111111111", "0330", "123")
                .reviewOrder()
                .placeOrder();
        assertThat(checkout.completeHeader()).isEqualToIgnoringCase("Checkout Complete");
    }

    @Test(description = "MOB-21 Locked-out user cannot log in and sees an error")
    public void lockedOutUser() {
        LoginScreen login = new CatalogScreen().waitLoaded().openMenu().openLogin();
        login.login("alice@example.com", Config.get("mda.password"));
        assertThat(driver().findElement(LoginScreen.PASSWORD_ERROR).getText()).containsIgnoringCase("locked");
    }

    @Test(description = "MOB-22 Empty login shows field validation")
    public void emptyLoginValidation() {
        LoginScreen login = new CatalogScreen().waitLoaded().openMenu().openLogin();
        driver().findElement(LoginScreen.LOGIN).click();
        assertThat(driver().findElement(LoginScreen.USERNAME_ERROR).getText()).containsIgnoringCase("required");
    }
}
