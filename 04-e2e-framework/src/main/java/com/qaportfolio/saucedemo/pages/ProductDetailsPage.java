package com.qaportfolio.saucedemo.pages;

import com.qaportfolio.saucedemo.model.Product;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class ProductDetailsPage extends LoggedInPage<ProductDetailsPage> {

    private static final By NAME = By.cssSelector("[data-test='inventory-item-name']");
    private static final By DESC = By.cssSelector("[data-test='inventory-item-desc']");
    private static final By PRICE = By.cssSelector("[data-test='inventory-item-price']");
    private static final By IMAGE = By.cssSelector("img.inventory_details_img");
    private static final By ADD = By.cssSelector("[data-test='add-to-cart']");
    private static final By REMOVE = By.cssSelector("[data-test='remove']");
    private static final By BACK = By.cssSelector("[data-test='back-to-products']");

    @Override
    public ProductDetailsPage waitUntilLoaded() {
        visible(BACK);
        return this;
    }

    public Product product() {
        return new Product(text(NAME).trim(), text(DESC).trim(), InventoryPage.parsePrice(text(PRICE)));
    }

    public String imageSource() {
        return visible(IMAGE).getDomAttribute("src");
    }

    @Step("Add product to cart from details page")
    public ProductDetailsPage addToCart() {
        click(ADD);
        visible(REMOVE);
        return this;
    }

    @Step("Remove product from cart on details page")
    public ProductDetailsPage remove() {
        click(REMOVE);
        visible(ADD);
        return this;
    }

    public boolean canRemove() {
        return isDisplayed(REMOVE);
    }

    @Step("Back to products")
    public InventoryPage backToProducts() {
        click(BACK);
        return new InventoryPage().waitUntilLoaded();
    }
}
