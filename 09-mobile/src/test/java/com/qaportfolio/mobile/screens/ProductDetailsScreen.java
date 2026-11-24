package com.qaportfolio.mobile.screens;

import org.openqa.selenium.By;

public class ProductDetailsScreen extends BaseScreen {

    public static final By NAME = id("productTV");
    public static final By PRICE = id("priceTV");
    public static final By PLUS = id("plusIV");
    public static final By QUANTITY = id("noTV");
    public static final By ADD_TO_CART = id("cartBt");

    public ProductDetailsScreen waitLoaded() {
        visible(a11y("Displays selected product"));
        return this;
    }

    public String name() {
        return scrollToId("productTV").getText();
    }

    public String price() {
        return visible(PRICE).getText();
    }

    public ProductDetailsScreen increaseQuantity() {
        scrollToId("plusIV").click();
        return this;
    }

    public String quantity() {
        return scrollToId("noTV").getText();
    }

    /** Adds to cart; the app stays on the product details screen. */
    public ProductDetailsScreen addToCart() {
        scrollToId("cartBt").click();
        return this;
    }

    public CartScreen openCart() {
        tap(id("cartRL"));
        return new CartScreen();
    }
}
