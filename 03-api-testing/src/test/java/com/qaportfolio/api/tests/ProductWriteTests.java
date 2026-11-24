package com.qaportfolio.api.tests;

import com.qaportfolio.api.client.ApiSpecs;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * DummyJSON simulates writes: responses echo the change but nothing is persisted, so these tests are
 * repeatable and leave no data behind.
 */
@Epic("REST API")
@Feature("Products (write, simulated)")
public class ProductWriteTests {

    @Test(groups = "smoke", description = "TC-API-020 Add product returns 201 and echoes the payload with a new id")
    public void addProduct() {
        given().spec(ApiSpecs.request())
                .body(Map.of("title", "QA Test Backpack", "price", 49.99, "category", "bags"))
                .post("/products/add")
                .then().spec(ApiSpecs.ok(201))
                .body("id", notNullValue())
                .body("title", equalTo("QA Test Backpack"))
                .body("price", equalTo(49.99f));
    }

    @Test(description = "TC-API-021 PUT updates a product title")
    public void updateProductPut() {
        given().spec(ApiSpecs.request()).body(Map.of("title", "Renamed by QA"))
                .put("/products/{id}", 1)
                .then().spec(ApiSpecs.ok()).body("id", equalTo(1)).body("title", equalTo("Renamed by QA"));
    }

    @Test(description = "TC-API-022 PATCH updates a product price")
    public void updateProductPatch() {
        given().spec(ApiSpecs.request()).body(Map.of("price", 12.5))
                .patch("/products/{id}", 2)
                .then().spec(ApiSpecs.ok()).body("id", equalTo(2)).body("price", equalTo(12.5f));
    }

    @Test(description = "TC-API-023 DELETE marks product deleted with timestamp")
    public void deleteProduct() {
        given().spec(ApiSpecs.request()).delete("/products/{id}", 3)
                .then().spec(ApiSpecs.ok())
                .body("id", equalTo(3)).body("isDeleted", equalTo(true)).body("deletedOn", notNullValue());
    }

    @Test(description = "TC-API-024 Update of unknown product returns 404")
    public void updateUnknownProduct404() {
        given().spec(ApiSpecs.request()).body(Map.of("title", "x"))
                .put("/products/{id}", 99999).then().spec(ApiSpecs.error(404));
    }

    @Test(groups = "known-defect", description = "DEF-API-003 Add product with empty body should be rejected (400)")
    @Issue("DEF-API-003")
    @Severity(SeverityLevel.NORMAL)
    public void addProductRequiresFields() {
        given().spec(ApiSpecs.request()).body("{}").post("/products/add").then().statusCode(400);
    }

    @Test(groups = "known-defect", description = "DEF-API-004 Negative price should be rejected (400)")
    @Issue("DEF-API-004")
    @Severity(SeverityLevel.CRITICAL)
    public void negativePriceRejected() {
        given().spec(ApiSpecs.request()).body(Map.of("title", "Bad", "price", -5))
                .post("/products/add").then().statusCode(400);
    }

    @Test(groups = "known-defect", description = "DEF-API-004 Non-numeric price on PATCH should be rejected (400)")
    @Issue("DEF-API-004")
    public void nonNumericPriceRejected() {
        given().spec(ApiSpecs.request()).body(Map.of("price", "notanumber"))
                .patch("/products/{id}", 1).then().statusCode(400);
    }

    @Test(description = "TC-API-025 Unsupported method is not accepted (any 4xx)")
    public void unsupportedMethodNotAccepted() {
        given().spec(ApiSpecs.request()).post("/products/{id}", 1)
                .then().statusCode(org.hamcrest.Matchers.both(org.hamcrest.Matchers.greaterThanOrEqualTo(400))
                        .and(org.hamcrest.Matchers.lessThan(500)));
    }

    @Test(groups = "known-defect", description = "DEF-API-006 Unsupported method should return 405 JSON, not 404 HTML")
    @Issue("DEF-API-006")
    public void unsupportedMethodShouldBe405Json() {
        given().spec(ApiSpecs.request()).post("/products/{id}", 1)
                .then().statusCode(405).header("Content-Type", containsString("application/json"));
    }
}
