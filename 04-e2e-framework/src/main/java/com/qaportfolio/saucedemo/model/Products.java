package com.qaportfolio.saucedemo.model;

/** Stable product names from the SauceDemo catalogue. */
public final class Products {
    public static final String BACKPACK = "Sauce Labs Backpack";
    public static final String BIKE_LIGHT = "Sauce Labs Bike Light";
    public static final String BOLT_TSHIRT = "Sauce Labs Bolt T-Shirt";
    public static final String FLEECE_JACKET = "Sauce Labs Fleece Jacket";
    public static final String ONESIE = "Sauce Labs Onesie";
    public static final String RED_TSHIRT = "Test.allTheThings() T-Shirt (Red)";

    private Products() {
    }

    /** data-test slug used in button ids, e.g. "sauce-labs-backpack". */
    public static String slug(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9.()]+", "-").replaceAll("-+$", "");
    }
}
