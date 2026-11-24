package com.qaportfolio.mobile.screens;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

public class CartScreen extends BaseScreen {

    public static final By TITLE = id("productTV");
    public static final By ITEM_TITLE = id("titleTV");
    public static final By ITEMS_COUNT = id("itemsTV");
    public static final By TOTAL = id("totalPriceTV");
    public static final By CHECKOUT = a11y("Confirms products for checkout");
    public static final By REMOVE = id("removeBt");

    public CartScreen waitLoaded() {
        visible(ITEMS_COUNT);
        return this;
    }

    public List<String> itemTitles() {
        return all(ITEM_TITLE).stream().map(WebElement::getText).toList();
    }

    public String itemsCount() {
        return visible(ITEMS_COUNT).getText();
    }

    public String total() {
        return visible(TOTAL).getText();
    }

    /** Proceeds to checkout; lands on Login when not authenticated. */
    public void proceedToCheckout() {
        tap(CHECKOUT);
    }
}
