package com.qaportfolio.security;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

/** OWASP A04:2021 Insecure Design — server-side validation of business rules (boundary values). */
public class A04BusinessLogicTest extends SecurityBase {

    private String token;

    @BeforeClass(alwaysRun = true)
    public void loginOnce() {
        token = login("security.user1");
    }

    @DataProvider
    public Object[][] quantities() {
        return new Object[][]{{0, 400}, {-1, 400}, {21, 400}, {1000, 400}, {1, 201}, {20, 201}};
    }

    @Test(dataProvider = "quantities",
            description = "SEC-TC-15 [A04] Cart quantity is bounded 1..20 server-side (boundary values)")
    public void cartQuantityIsValidated(int qty, int expectedStatus) {
        as(token).body(Map.of("productId", 2, "quantity", qty)).post("/api/cart/items").then().statusCode(expectedStatus);
        as(token).delete("/api/cart");
    }

    @DataProvider
    public Object[][] paging() {
        return new Object[][]{{"0", "10", 400}, {"1", "0", 400}, {"1", "101", 400}, {"1", "100", 200}, {"abc", "10", 400}};
    }

    @Test(dataProvider = "paging", description = "SEC-TC-16 [A04] Paging parameters are bounded (no unbounded result sets)")
    public void pagingIsValidated(String page, String size, int expectedStatus) {
        api().queryParam("page", page).queryParam("size", size).get("/api/products").then().statusCode(expectedStatus);
    }

    @Test(description = "SEC-TC-17 [A04] Cannot add a non-existent product to the cart")
    public void unknownProductRejected() {
        as(token).body(Map.of("productId", 99999, "quantity", 1)).post("/api/cart/items").then().statusCode(404);
    }

    @Test(description = "SEC-TC-18 [A04] Checkout with an empty cart is refused (409)")
    public void emptyCartCheckoutRefused() {
        as(token).delete("/api/cart");
        as(token).post("/api/orders").then().statusCode(409);
    }
}
