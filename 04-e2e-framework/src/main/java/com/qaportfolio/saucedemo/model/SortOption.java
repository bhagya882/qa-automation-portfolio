package com.qaportfolio.saucedemo.model;

/** Values of the inventory sort dropdown. */
public enum SortOption {
    NAME_ASC("az", "Name (A to Z)"),
    NAME_DESC("za", "Name (Z to A)"),
    PRICE_ASC("lohi", "Price (low to high)"),
    PRICE_DESC("hilo", "Price (high to low)");

    private final String value;
    private final String label;

    SortOption(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String value() {
        return value;
    }

    public String label() {
        return label;
    }
}
