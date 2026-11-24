package com.qaportfolio.saucedemo.tests;

import com.qaportfolio.saucedemo.model.Product;
import com.qaportfolio.saucedemo.model.Products;
import com.qaportfolio.saucedemo.model.Users;
import com.qaportfolio.saucedemo.pages.InventoryPage;
import com.qaportfolio.saucedemo.pages.ProductDetailsPage;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Product details")
public class ProductDetailsTest extends BaseSauceTest {

    @DataProvider(name = "products", parallel = true)
    public Object[][] products() {
        return new Object[][]{{Products.BACKPACK}, {Products.FLEECE_JACKET}, {Products.RED_TSHIRT}};
    }

    @Test(dataProvider = "products", groups = "regression",
            description = "Details page matches the listing (name, description, price)")
    @Story("View details")
    @Severity(SeverityLevel.CRITICAL)
    public void detailsMatchListing(String name) {
        InventoryPage inv = loginAsStandard();
        Product listed = inv.products().stream().filter(p -> p.name().equals(name)).findFirst().orElseThrow();
        Product details = inv.openProduct(name).product();
        assertThat(details).isEqualTo(listed);
    }

    @Test(groups = {"smoke", "regression"}, description = "Add and remove from details page updates cart badge")
    @Story("Add to cart from details")
    @Severity(SeverityLevel.CRITICAL)
    public void addAndRemoveFromDetails() {
        ProductDetailsPage details = loginAsStandard().openProduct(Products.BIKE_LIGHT).addToCart();
        assertThat(details.header().cartCount()).isEqualTo(1);
        details.remove();
        assertThat(details.header().cartCount()).isZero();
    }

    @Test(groups = "regression", description = "Back to products returns to the listing")
    @Story("Navigation")
    @Severity(SeverityLevel.MINOR)
    public void backToProducts() {
        InventoryPage inv = loginAsStandard().openProduct(Products.ONESIE).backToProducts();
        assertThat(inv.productNames()).hasSize(6);
    }

    @Test(groups = "known-defect", description = "DEF-04-004 problem_user: clicking a product opens a different product")
    @Issue("DEF-04-004")
    @Story("View details")
    @Severity(SeverityLevel.CRITICAL)
    public void problemUserOpensCorrectProduct() {
        Product details = loginAs(Users.problem()).openProduct(Products.BACKPACK).product();
        assertThat(details.name()).isEqualTo(Products.BACKPACK);
    }
}
