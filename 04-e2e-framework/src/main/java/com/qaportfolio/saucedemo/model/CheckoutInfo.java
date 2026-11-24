package com.qaportfolio.saucedemo.model;

import com.qaportfolio.core.util.TestData;

/** Shipping details entered on checkout step one. Always synthetic (Datafaker). */
public record CheckoutInfo(String firstName, String lastName, String postalCode) {

    public static CheckoutInfo random() {
        return new CheckoutInfo(TestData.firstName(), TestData.lastName(), TestData.postalCode());
    }

    public CheckoutInfo withFirstName(String v) {
        return new CheckoutInfo(v, lastName, postalCode);
    }

    public CheckoutInfo withLastName(String v) {
        return new CheckoutInfo(firstName, v, postalCode);
    }

    public CheckoutInfo withPostalCode(String v) {
        return new CheckoutInfo(firstName, lastName, v);
    }
}
