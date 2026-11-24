package com.qaportfolio.saucedemo.pages;

import com.qaportfolio.saucedemo.model.Product;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

public class CartPage extends LoggedInPage<CartPage> {

    private static final By CART_LIST = By.cssSelector("[data-test='cart-list']");
    private static final By ITEM = By.cssSelector("[data-test='inventory-item']");
    private static final By NAME = By.cssSelector("[data-test='inventory-item-name']");
    private static final By DESC = By.cssSelector("[data-test='inventory-item-desc']");
    private static final By PRICE = By.cssSelector("[data-test='inventory-item-price']");
    private static final By QTY = By.cssSelector("[data-test='item-quantity']");
    private static final By REMOVE = By.cssSelector("button.cart_button");
    private static final By CHECKOUT = By.cssSelector("[data-test='checkout']");
    private static final By CONTINUE = By.cssSelector("[data-test='continue-shopping']");

    @Override
    public CartPage waitUntilLoaded() {
        visible(CART_LIST);
        return this;
    }

    public List<Product> items() {
        return all(ITEM).stream().map(i -> new Product(i.findElement(NAME).getText().trim(),
                i.findElement(DESC).getText().trim(), InventoryPage.parsePrice(i.findElement(PRICE).getText()))).toList();
    }

    public List<String> itemNames() {
        return items().stream().map(Product::name).toList();
    }

    public List<Integer> quantities() {
        return all(QTY).stream().map(e -> Integer.parseInt(e.getText().trim())).toList();
    }

    @Step("Remove '{name}' from cart")
    public CartPage remove(String name) {
        WebElement item = all(ITEM).stream().filter(i -> i.findElement(NAME).getText().trim().equals(name))
                .findFirst().orElseThrow(() -> new AssertionError(name + " is not in the cart"));
        item.findElement(REMOVE).click();
        wait.until(d -> itemNames().stream().noneMatch(name::equals));
        return this;
    }

    @Step("Proceed to checkout")
    public CheckoutInfoPage checkout() {
        click(CHECKOUT);
        return new CheckoutInfoPage().waitUntilLoaded();
    }

    @Step("Continue shopping")
    public InventoryPage continueShopping() {
        click(CONTINUE);
        return new InventoryPage().waitUntilLoaded();
    }
}
