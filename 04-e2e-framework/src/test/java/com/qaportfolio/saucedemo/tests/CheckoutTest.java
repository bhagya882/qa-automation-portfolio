package com.qaportfolio.saucedemo.tests;

import com.qaportfolio.saucedemo.model.CheckoutInfo;
import com.qaportfolio.saucedemo.model.Products;
import com.qaportfolio.saucedemo.model.Users;
import com.qaportfolio.saucedemo.pages.CheckoutCompletePage;
import com.qaportfolio.saucedemo.pages.CheckoutInfoPage;
import com.qaportfolio.saucedemo.pages.CheckoutOverviewPage;
import com.qaportfolio.saucedemo.pages.InventoryPage;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Checkout")
public class CheckoutTest extends BaseSauceTest {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");

    @Test(groups = {"smoke", "regression"}, description = "E2E: login → add 2 items → checkout → order confirmed")
    @Story("Place order")
    @Severity(SeverityLevel.BLOCKER)
    public void completeCheckoutEndToEnd() {
        CheckoutOverviewPage overview = loginAsStandard()
                .addToCart(Products.BACKPACK)
                .addToCart(Products.BIKE_LIGHT)
                .header().openCart()
                .checkout()
                .fill(CheckoutInfo.random())
                .continueToOverview();

        assertThat(overview.itemNames()).containsExactly(Products.BACKPACK, Products.BIKE_LIGHT);
        BigDecimal sum = overview.itemPrices().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(overview.itemTotal()).isEqualByComparingTo(sum);
        assertThat(overview.tax()).isEqualByComparingTo(sum.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP));
        assertThat(overview.total()).isEqualByComparingTo(overview.itemTotal().add(overview.tax()));

        CheckoutCompletePage done = overview.finish();
        assertThat(done.confirmationHeader()).isEqualTo("Thank you for your order!");
        assertThat(done.header().cartCount()).isZero();
        assertThat(done.backHome().productNames()).hasSize(6);
    }

    @DataProvider(name = "missingFields", parallel = true)
    public Object[][] missingFields() {
        CheckoutInfo ok = CheckoutInfo.random();
        return new Object[][]{
                {ok.withFirstName(""), "Error: First Name is required"},
                {ok.withLastName(""), "Error: Last Name is required"},
                {ok.withPostalCode(""), "Error: Postal Code is required"},
        };
    }

    @Test(dataProvider = "missingFields", groups = "regression", description = "Required shipping fields are validated")
    @Story("Shipping info validation")
    @Severity(SeverityLevel.CRITICAL)
    public void requiredFieldsValidated(CheckoutInfo info, String expectedError) {
        CheckoutInfoPage page = loginAsStandard().addToCart(Products.ONESIE).header().openCart().checkout()
                .fill(info).continueExpectingError();
        assertThat(page.errorMessage()).isEqualTo(expectedError);
    }

    @Test(groups = "regression", description = "Cancel on overview keeps the cart")
    @Story("Cancel checkout")
    @Severity(SeverityLevel.NORMAL)
    public void cancelOnOverviewKeepsCart() {
        InventoryPage inv = loginAsStandard().addToCart(Products.BOLT_TSHIRT).header().openCart().checkout()
                .fill(CheckoutInfo.random()).continueToOverview().cancel();
        assertThat(inv.header().cartCount()).isEqualTo(1);
    }

    @Test(groups = "regression", description = "Cancel on shipping step returns to cart")
    @Story("Cancel checkout")
    @Severity(SeverityLevel.MINOR)
    public void cancelOnInfoReturnsToCart() {
        assertThat(loginAsStandard().addToCart(Products.BACKPACK).header().openCart().checkout().cancel().itemNames())
                .containsExactly(Products.BACKPACK);
    }

    @Test(groups = "known-defect", description = "DEF-04-007 problem_user: Last Name field cannot be filled")
    @Issue("DEF-04-007")
    @Story("Shipping info validation")
    @Severity(SeverityLevel.BLOCKER)
    public void problemUserCanEnterLastName() {
        CheckoutInfo info = CheckoutInfo.random();
        CheckoutInfoPage page = loginAs(Users.problem()).addToCart(Products.BACKPACK).header().openCart().checkout()
                .fill(info);
        assertThat(page.lastNameValue()).as("last name field value").isEqualTo(info.lastName());
        assertThat(page.firstNameValue()).as("first name field value").isEqualTo(info.firstName());
    }

    @Test(groups = "known-defect", description = "DEF-04-008 error_user: Finish does not complete the order")
    @Issue("DEF-04-008")
    @Story("Place order")
    @Severity(SeverityLevel.BLOCKER)
    public void errorUserCanFinishOrder() {
        CheckoutOverviewPage overview = loginAs(Users.error()).addToCart(Products.BACKPACK).header().openCart()
                .checkout().fill(CheckoutInfo.random()).continueToOverview();
        assertThat(overview.finish().confirmationHeader()).isEqualTo("Thank you for your order!");
    }
}
