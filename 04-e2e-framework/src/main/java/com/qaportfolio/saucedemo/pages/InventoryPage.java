package com.qaportfolio.saucedemo.pages;

import com.qaportfolio.saucedemo.model.Product;
import com.qaportfolio.saucedemo.model.SortOption;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.math.BigDecimal;
import java.util.List;

public class InventoryPage extends LoggedInPage<InventoryPage> {

    private static final By LIST = By.cssSelector("[data-test='inventory-list']");
    private static final By ITEM = By.cssSelector("[data-test='inventory-item']");
    private static final By NAME = By.cssSelector("[data-test='inventory-item-name']");
    private static final By DESC = By.cssSelector("[data-test='inventory-item-desc']");
    private static final By PRICE = By.cssSelector("[data-test='inventory-item-price']");
    private static final By IMAGE = By.cssSelector("img.inventory_item_img");
    private static final By SORT = By.cssSelector("[data-test='product-sort-container']");
    private static final By ACTION_BUTTON = By.cssSelector("button.btn_inventory");

    @Override
    public InventoryPage waitUntilLoaded() {
        visible(LIST);
        return this;
    }

    public List<Product> products() {
        return all(ITEM).stream().map(InventoryPage::toProduct).toList();
    }

    public List<String> productNames() {
        return all(NAME).stream().map(e -> e.getText().trim()).toList();
    }

    public List<BigDecimal> prices() {
        return all(PRICE).stream().map(e -> parsePrice(e.getText())).toList();
    }

    public List<String> imageSources() {
        return all(IMAGE).stream().map(e -> e.getDomAttribute("src")).toList();
    }

    @Step("Sort products by {option}")
    public InventoryPage sortBy(SortOption option) {
        new Select(visible(SORT)).selectByValue(option.value());
        return this;
    }

    public String activeSortLabel() {
        return text(By.cssSelector("[data-test='active-option']"));
    }

    @Step("Add '{name}' to cart")
    public InventoryPage addToCart(String name) {
        WebElement btn = itemByName(name).findElement(ACTION_BUTTON);
        btn.click();
        return this;
    }

    @Step("Remove '{name}' from cart (inventory page)")
    public InventoryPage removeFromCart(String name) {
        WebElement btn = itemByName(name).findElement(ACTION_BUTTON);
        btn.click();
        return this;
    }

    public String buttonLabel(String name) {
        return itemByName(name).findElement(ACTION_BUTTON).getText().trim();
    }

    @Step("Open product details for '{name}'")
    public ProductDetailsPage openProduct(String name) {
        itemByName(name).findElement(NAME).click();
        return new ProductDetailsPage().waitUntilLoaded();
    }

    @Step("Add {count} products to cart")
    public InventoryPage addFirstProducts(int count) {
        List<String> names = productNames();
        for (int i = 0; i < count; i++) {
            addToCart(names.get(i));
        }
        return this;
    }

    private WebElement itemByName(String name) {
        return wait.until(d -> d.findElements(ITEM).stream()
                .filter(i -> i.findElement(NAME).getText().trim().equals(name))
                .findFirst().orElse(null));
    }

    private static Product toProduct(WebElement item) {
        return new Product(item.findElement(NAME).getText().trim(), item.findElement(DESC).getText().trim(),
                parsePrice(item.findElement(PRICE).getText()));
    }

    static BigDecimal parsePrice(String text) {
        return new BigDecimal(text.replaceAll("[^0-9.]", ""));
    }
}
