package com.qaportfolio.mobile.screens;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

public class CatalogScreen extends BaseScreen {

    public static final By TITLE = id("productTV");
    public static final By PRODUCT_LIST = id("productRV");
    public static final By PRODUCT_IMAGE = id("productIV");
    public static final By PRODUCT_TITLE = id("titleTV");
    public static final By SORT = id("sortIV");
    public static final By MENU = id("menuIV");
    public static final By CART = id("cartRL");
    public static final By CART_COUNT = id("cartTV");

    public CatalogScreen waitLoaded() {
        visible(PRODUCT_LIST);
        return this;
    }

    public boolean isDisplayed() {
        return !all(PRODUCT_LIST).isEmpty() && "Products".equals(visible(TITLE).getText());
    }

    public List<String> visibleProductTitles() {
        return all(PRODUCT_TITLE).stream().map(WebElement::getText).toList();
    }

    public ProductDetailsScreen openProduct(int index) {
        waitLoaded();
        all(PRODUCT_IMAGE).get(index).click();
        return new ProductDetailsScreen().waitLoaded();
    }

    public MenuScreen openMenu() {
        tap(MENU);
        return new MenuScreen().waitLoaded();
    }

    public CartScreen openCart() {
        tap(CART);
        return new CartScreen();
    }

}
