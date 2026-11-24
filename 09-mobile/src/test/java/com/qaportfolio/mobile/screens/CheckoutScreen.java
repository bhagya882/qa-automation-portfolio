package com.qaportfolio.mobile.screens;

import org.openqa.selenium.By;

/** Shipping address → payment → review → complete. */
public class CheckoutScreen extends BaseScreen {

    public static final By TITLE = id("checkoutTitleTV");
    public static final By COMPLETE = id("completeTV");
    public static final By CONTINUE_SHOPPING = id("shoppingBt");

    public CheckoutScreen waitForAddressForm() {
        visible(id("fullNameET"));
        return this;
    }

    public CheckoutScreen fillAddress(String name, String line1, String city, String zip, String country) {
        type(id("fullNameET"), name);
        type(id("address1ET"), line1);
        type(id("cityET"), city);
        type(id("zipET"), zip);
        hideKeyboard();
        scrollToId("countryET");
        type(id("countryET"), country);
        hideKeyboard();
        return this;
    }

    public CheckoutScreen toPayment() {
        scrollToId("paymentBtn").click();
        visible(id("cardNumberET"));
        return this;
    }

    /** Synthetic test card number (Visa test PAN), never a real card. */
    public CheckoutScreen fillPayment(String name, String cardNumber, String expiry, String cvv) {
        type(id("nameET"), name);
        type(id("cardNumberET"), cardNumber);
        type(id("expirationDateET"), expiry);
        type(id("securityCodeET"), cvv);
        hideKeyboard();
        return this;
    }

    public CheckoutScreen reviewOrder() {
        scrollToId("paymentBtn").click();
        visible(a11y("Completes the process of checkout"));
        return this;
    }

    public CheckoutScreen placeOrder() {
        tap(a11y("Completes the process of checkout"));
        return this;
    }

    public String completeHeader() {
        return visible(COMPLETE).getText();
    }
}
