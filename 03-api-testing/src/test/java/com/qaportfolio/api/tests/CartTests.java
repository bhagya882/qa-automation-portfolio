package com.qaportfolio.api.tests;

import com.qaportfolio.api.client.ApiSpecs;
import com.qaportfolio.api.client.AuthClient;
import com.qaportfolio.api.model.Cart;
import com.qaportfolio.api.model.CartRequest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.notNullValue;

@Epic("REST API")
@Feature("Carts")
public class CartTests {

    @Test(groups = "smoke", description = "TC-API-030 Cart by id matches schema and totals are internally consistent")
    public void cartTotalsAreConsistent() {
        Cart cart = given().spec(ApiSpecs.request()).get("/carts/{id}", 1)
                .then().spec(ApiSpecs.ok())
                .body(matchesJsonSchemaInClasspath("schemas/cart.json"))
                .extract().as(Cart.class);

        BigDecimal sum = BigDecimal.ZERO;
        int qty = 0;
        for (Cart.Line l : cart.products()) {
            assertThat(l.total().doubleValue())
                    .as("line total = price x quantity for product %s", l.id())
                    .isCloseTo(l.price().doubleValue() * l.quantity(), within(0.01));
            sum = sum.add(l.total());
            qty += l.quantity();
        }
        assertThat(cart.total().doubleValue()).isCloseTo(sum.doubleValue(), within(0.01));
        assertThat(cart.totalProducts()).isEqualTo(cart.products().size());
        assertThat(cart.totalQuantity()).isEqualTo(qty);
        assertThat(cart.discountedTotal()).isLessThanOrEqualTo(cart.total());
    }

    @Test(description = "TC-API-031 Carts of the logged-in user all belong to that user")
    public void cartsOfUser() {
        int userId = AuthClient.session().id();
        AuthClient.authenticated().get("/carts/user/{id}", userId)
                .then().spec(ApiSpecs.ok()).body("carts.userId", everyItem(equalTo(userId)));
    }

    @Test(description = "TC-API-032 Carts for unknown user return 404")
    public void cartsOfUnknownUser404() {
        given().spec(ApiSpecs.request()).get("/carts/user/{id}", 99999)
                .then().spec(ApiSpecs.error(404)).body("message", equalTo("User with id '99999' not found"));
    }

    @DataProvider
    public Object[][] newCarts() {
        return new Object[][]{
                {List.of(new CartRequest.Item(1, 1))},
                {List.of(new CartRequest.Item(1, 2), new CartRequest.Item(50, 3))},
                {List.of(new CartRequest.Item(144, 1), new CartRequest.Item(98, 1), new CartRequest.Item(10, 5))},
        };
    }

    @Test(dataProvider = "newCarts", description = "TC-API-033 Add cart computes line totals and quantities")
    public void addCart(List<CartRequest.Item> items) {
        Cart cart = AuthClient.authenticated()
                .body(new CartRequest(AuthClient.session().id(), null, items))
                .post("/carts/add")
                .then().spec(ApiSpecs.ok(201)).extract().as(Cart.class);
        assertThat(cart.products()).hasSize(items.size());
        assertThat(cart.totalQuantity()).isEqualTo(items.stream().mapToInt(CartRequest.Item::quantity).sum());
        for (Cart.Line l : cart.products()) {
            assertThat(l.total().doubleValue()).isCloseTo(l.price().doubleValue() * l.quantity(), within(0.01));
        }
    }

    @Test(description = "TC-API-034 Add cart with no products is rejected with 400")
    public void addEmptyCartRejected() {
        given().spec(ApiSpecs.request()).body(new CartRequest(1, null, List.of()))
                .post("/carts/add").then().spec(ApiSpecs.error(400))
                .body("message", equalTo("products can not be empty"));
    }

    @Test(description = "TC-API-035 Update cart with merge=true keeps existing lines and adds the new one")
    public void updateCartMerge() {
        int before = given().spec(ApiSpecs.request()).get("/carts/{id}", 1).path("totalProducts");
        given().spec(ApiSpecs.request())
                .body(new CartRequest(null, true, List.of(new CartRequest.Item(1, 1))))
                .put("/carts/{id}", 1)
                .then().spec(ApiSpecs.ok())
                .body("id", equalTo(1))
                .body("totalProducts", equalTo(before + 1));
    }

    @Test(description = "TC-API-036 Delete cart returns isDeleted=true")
    public void deleteCart() {
        given().spec(ApiSpecs.request()).delete("/carts/{id}", 1)
                .then().spec(ApiSpecs.ok()).body("isDeleted", equalTo(true)).body("deletedOn", notNullValue());
    }

    @Test(groups = "known-defect", description = "DEF-API-005 Negative quantity must be rejected (400)")
    @Issue("DEF-API-005")
    @Severity(SeverityLevel.CRITICAL)
    public void negativeQuantityRejected() {
        given().spec(ApiSpecs.request()).body(CartRequest.of(1, new CartRequest.Item(1, -3)))
                .post("/carts/add").then().statusCode(400);
    }

    @Test(groups = "known-defect", description = "DEF-API-009 Cart line schema differs between GET (discountedTotal) and add/update (discountedPrice)")
    @Issue("DEF-API-009")
    public void cartLineFieldNamesConsistent() {
        given().spec(ApiSpecs.request()).get("/carts/{id}", 1)
                .then().body("products[0]", hasKey("discountedTotal"));
        given().spec(ApiSpecs.request()).body(CartRequest.of(1, new CartRequest.Item(1, 1)))
                .post("/carts/add")
                .then().statusCode(201).body("products[0]", hasKey("discountedTotal"));
    }

    @Test(description = "TC-API-037 Cart request with wrong JSON type for products is rejected")
    public void malformedCartRejected() {
        int status = given().spec(ApiSpecs.request()).body(Map.of("userId", 1, "products", "not-an-array"))
                .post("/carts/add").statusCode();
        assertThat(status).isBetween(400, 499);
    }
}
