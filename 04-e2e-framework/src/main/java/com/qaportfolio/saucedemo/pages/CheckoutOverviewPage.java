package com.qaportfolio.saucedemo.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;

import java.math.BigDecimal;
import java.util.List;

/** Checkout step two: order summary with item total, tax and total. */
public class CheckoutOverviewPage extends LoggedInPage<CheckoutOverviewPage> {

    private static final By NAME = By.cssSelector("[data-test='inventory-item-name']");
    private static final By PRICE = By.cssSelector("[data-test='inventory-item-price']");
    private static final By SUBTOTAL = By.cssSelector("[data-test='subtotal-label']");
    private static final By TAX = By.cssSelector("[data-test='tax-label']");
    private static final By TOTAL = By.cssSelector("[data-test='total-label']");
    private static final By FINISH = By.cssSelector("[data-test='finish']");
    private static final By CANCEL = By.cssSelector("[data-test='cancel']");

    @Override
    public CheckoutOverviewPage waitUntilLoaded() {
        visible(FINISH);
        return this;
    }

    public List<String> itemNames() {
        return all(NAME).stream().map(e -> e.getText().trim()).toList();
    }

    public List<BigDecimal> itemPrices() {
        return all(PRICE).stream().map(e -> InventoryPage.parsePrice(e.getText())).toList();
    }

    public BigDecimal itemTotal() {
        return InventoryPage.parsePrice(text(SUBTOTAL));
    }

    public BigDecimal tax() {
        return InventoryPage.parsePrice(text(TAX));
    }

    public BigDecimal total() {
        return InventoryPage.parsePrice(text(TOTAL));
    }

    @Step("Finish order")
    public CheckoutCompletePage finish() {
        click(FINISH);
        return new CheckoutCompletePage().waitUntilLoaded();
    }

    @Step("Cancel on overview")
    public InventoryPage cancel() {
        click(CANCEL);
        return new InventoryPage().waitUntilLoaded();
    }
}
