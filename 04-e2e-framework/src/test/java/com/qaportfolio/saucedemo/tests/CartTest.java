package com.qaportfolio.saucedemo.tests;

import com.qaportfolio.saucedemo.model.Products;
import com.qaportfolio.saucedemo.model.Users;
import com.qaportfolio.saucedemo.pages.CartPage;
import com.qaportfolio.saucedemo.pages.InventoryPage;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Cart")
public class CartTest extends BaseSauceTest {

    @Test(groups = {"smoke", "regression"}, description = "Added products appear in the cart with quantity 1")
    @Story("Add to cart")
    @Severity(SeverityLevel.BLOCKER)
    public void addedProductsAppearInCart() {
        InventoryPage inv = loginAsStandard().addToCart(Products.BACKPACK).addToCart(Products.BOLT_TSHIRT);
        assertThat(inv.header().cartCount()).isEqualTo(2);
        assertThat(inv.buttonLabel(Products.BACKPACK)).isEqualTo("Remove");
        CartPage cart = inv.header().openCart();
        assertThat(cart.itemNames()).containsExactly(Products.BACKPACK, Products.BOLT_TSHIRT);
        assertThat(cart.quantities()).containsOnly(1);
    }

    @Test(groups = "regression", description = "Removing an item in the cart updates list and badge")
    @Story("Remove from cart")
    @Severity(SeverityLevel.CRITICAL)
    public void removeFromCart() {
        CartPage cart = loginAsStandard().addToCart(Products.BACKPACK).addToCart(Products.ONESIE).header().openCart();
        cart.remove(Products.BACKPACK);
        assertThat(cart.itemNames()).containsExactly(Products.ONESIE);
        assertThat(cart.header().cartCount()).isEqualTo(1);
    }

    @Test(groups = "regression", description = "Removing from the listing toggles the button back to Add to cart")
    @Story("Remove from cart")
    @Severity(SeverityLevel.NORMAL)
    public void removeFromListing() {
        InventoryPage inv = loginAsStandard().addToCart(Products.BIKE_LIGHT).removeFromCart(Products.BIKE_LIGHT);
        assertThat(inv.header().cartCount()).isZero();
        assertThat(inv.buttonLabel(Products.BIKE_LIGHT)).isEqualTo("Add to cart");
    }

    @Test(groups = "regression", description = "Cart persists after continue shopping and page reload")
    @Story("Cart persistence")
    @Severity(SeverityLevel.NORMAL)
    public void cartPersists() {
        InventoryPage inv = loginAsStandard().addToCart(Products.FLEECE_JACKET).header().openCart().continueShopping();
        driver().navigate().refresh();
        assertThat(new InventoryPage().waitUntilLoaded().header().cartCount()).isEqualTo(1);
    }

    @Test(groups = "regression", description = "Empty cart shows no items")
    @Story("Cart")
    @Severity(SeverityLevel.MINOR)
    public void emptyCart() {
        CartPage cart = loginAsStandard().header().openCart();
        assertThat(cart.itemNames()).isEmpty();
    }

    @Test(groups = "regression", description = "Reset App State empties the cart")
    @Story("Cart")
    @Severity(SeverityLevel.NORMAL)
    public void resetAppStateClearsCart() {
        InventoryPage inv = loginAsStandard().addFirstProducts(3);
        assertThat(inv.header().cartCount()).isEqualTo(3);
        inv.header().openMenu().resetAppState();
        assertThat(inv.header().cartCount()).isZero();
    }

    @Test(groups = "known-defect", description = "DEF-04-005 problem_user: some products cannot be added to the cart")
    @Issue("DEF-04-005")
    @Story("Add to cart")
    @Severity(SeverityLevel.CRITICAL)
    public void problemUserCanAddAllProducts() {
        InventoryPage inv = loginAs(Users.problem()).addFirstProducts(6);
        assertThat(inv.header().cartCount()).as("cart badge after adding all 6 products").isEqualTo(6);
    }

    @Test(groups = "known-defect", description = "DEF-04-006 error_user: some products cannot be added/removed")
    @Issue("DEF-04-006")
    @Story("Add to cart")
    @Severity(SeverityLevel.CRITICAL)
    public void errorUserCanAddAllProducts() {
        InventoryPage inv = loginAs(Users.error()).addFirstProducts(6);
        assertThat(inv.header().cartCount()).as("cart badge after adding all 6 products").isEqualTo(6);
    }
}
