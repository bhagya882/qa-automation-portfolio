package com.qaportfolio.mobile.tests;

import com.qaportfolio.mobile.screens.CartScreen;
import com.qaportfolio.mobile.screens.CatalogScreen;
import com.qaportfolio.mobile.screens.LoginScreen;
import com.qaportfolio.mobile.screens.ProductDetailsScreen;
import com.qaportfolio.mobile.support.BaseMobileTest;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NavigationTest extends BaseMobileTest {

    @Test(description = "MOB-10 Catalog → product details → back returns to catalog")
    public void productDetailsAndBack() {
        CatalogScreen catalog = new CatalogScreen().waitLoaded();
        String first = catalog.visibleProductTitles().get(0);
        ProductDetailsScreen details = catalog.openProduct(0);
        assertThat(details.name()).isEqualTo(first);
        driver().navigate().back();
        assertThat(new CatalogScreen().waitLoaded().isDisplayed()).isTrue();
    }

    @Test(description = "MOB-11 Side menu navigates to Login and back to Catalog")
    public void menuNavigation() {
        LoginScreen login = new CatalogScreen().waitLoaded().openMenu().openLogin();
        assertThat(driver().findElement(LoginScreen.TITLE).getText()).isEqualTo("Login");
        new CatalogScreen().openMenu().openCatalog();
        assertThat(new CatalogScreen().isDisplayed()).isTrue();
    }

    @Test(description = "MOB-12 Empty cart shows no items")
    public void emptyCart() {
        CartScreen cart = new CatalogScreen().waitLoaded().openCart();
        assertThat(cart.itemTitles()).isEmpty();
    }
}
