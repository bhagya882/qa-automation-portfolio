package com.qaportfolio.api.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.qaportfolio.api.client.ApiSpecs;
import com.qaportfolio.api.model.Product;
import com.qaportfolio.api.model.ProductPage;
import com.qaportfolio.core.util.TestData;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

@Epic("REST API")
@Feature("Products (read)")
public class ProductReadTests {

    @Test(groups = "smoke", description = "TC-API-010 Product list returns default page of 30 with paging metadata")
    public void listProductsDefaultPage() {
        ProductPage page = given().spec(ApiSpecs.request()).get("/products")
                .then().spec(ApiSpecs.ok())
                .body(matchesJsonSchemaInClasspath("schemas/product-list.json"))
                .extract().as(ProductPage.class);
        assertThat(page.limit()).isEqualTo(30);
        assertThat(page.skip()).isZero();
        assertThat(page.products()).hasSize(30);
        assertThat(page.total()).isGreaterThanOrEqualTo(page.products().size());
    }

    @Test(description = "TC-API-011 Standard response headers: JSON content type, rate-limit headers")
    public void responseHeaders() {
        given().spec(ApiSpecs.request()).get("/products/1")
                .then().spec(ApiSpecs.ok())
                .header("Content-Type", containsString("application/json"))
                .header("x-ratelimit-limit", notNullValue())
                .header("x-ratelimit-remaining", notNullValue());
    }

    @DataProvider
    public Object[][] pages() {
        List<Object[]> rows = new ArrayList<>();
        for (JsonNode n : TestData.json("testdata/pagination.json")) {
            rows.add(new Object[]{n.get("limit").asInt(), n.get("skip").asInt()});
        }
        return rows.toArray(Object[][]::new);
    }

    @Test(dataProvider = "pages", description = "TC-API-012 limit/skip pagination returns the right slice")
    public void pagination(int limit, int skip) {
        ProductPage page = given().spec(ApiSpecs.request())
                .queryParam("limit", limit).queryParam("skip", skip).queryParam("select", "id,title")
                .get("/products").then().spec(ApiSpecs.ok()).extract().as(ProductPage.class);
        int expected = Math.max(0, Math.min(limit, page.total() - skip));
        assertThat(page.products()).hasSize(expected);
        if (expected > 0) {
            assertThat(page.products().get(0).id()).isEqualTo(skip + 1);
        }
    }

    @Test(description = "TC-API-013 select= returns only requested fields (plus id)")
    public void selectLimitsFields() {
        given().spec(ApiSpecs.request()).queryParam("limit", 3).queryParam("select", "title,price")
                .get("/products").then().spec(ApiSpecs.ok())
                .body("products[0].keySet()", equalTo(java.util.Set.of("id", "title", "price")));
    }

    @Test(description = "TC-API-014 sortBy=price&order=asc returns ascending prices")
    public void sortByPriceAscending() {
        List<Float> prices = given().spec(ApiSpecs.request())
                .queryParam("sortBy", "price").queryParam("order", "asc").queryParam("limit", 50)
                .queryParam("select", "price")
                .get("/products").then().spec(ApiSpecs.ok()).extract().jsonPath().getList("products.price", Float.class);
        assertThat(prices).hasSize(50).isSorted();
    }

    @DataProvider
    public Object[][] searches() {
        List<Object[]> rows = new ArrayList<>();
        for (JsonNode n : TestData.json("testdata/search.json")) {
            rows.add(new Object[]{n.get("query").asText(), n.get("expectAtLeast").asInt()});
        }
        return rows.toArray(Object[][]::new);
    }

    @Test(dataProvider = "searches", description = "TC-API-015 Search returns matching products")
    public void search(String query, int expectAtLeast) {
        Response r = given().spec(ApiSpecs.request()).queryParam("q", query).get("/products/search");
        r.then().spec(ApiSpecs.ok()).body(matchesJsonSchemaInClasspath("schemas/product-list.json"));
        int total = r.path("total");
        assertThat(total).isGreaterThanOrEqualTo(expectAtLeast);
        if (expectAtLeast == 0) {
            assertThat(total).isZero();
        }
    }

    @Test(groups = "smoke", description = "TC-API-016 Product by id matches the product schema")
    public void productById() {
        Product p = given().spec(ApiSpecs.request()).get("/products/{id}", 1)
                .then().spec(ApiSpecs.ok())
                .body(matchesJsonSchemaInClasspath("schemas/product.json"))
                .extract().as(Product.class);
        assertThat(p.id()).isEqualTo(1);
        assertThat(p.price()).isGreaterThan(BigDecimal.ZERO);
    }

    @DataProvider
    public Object[][] unknownIds() {
        return new Object[][]{{"99999"}, {"0"}, {"-1"}};
    }

    @Test(dataProvider = "unknownIds", description = "TC-API-017 Unknown product id returns 404 with message")
    public void unknownProduct404(String id) {
        given().spec(ApiSpecs.request()).get("/products/" + id)
                .then().spec(ApiSpecs.error(404))
                .body("message", equalTo("Product with id '" + id + "' not found"));
    }

    @Test(groups = "known-defect", description = "DEF-API-007 Non-numeric product id should be 400 Bad Request, not 404")
    @Issue("DEF-API-007")
    public void nonNumericIdShouldBe400() {
        given().spec(ApiSpecs.request()).get("/products/abc").then().statusCode(400);
    }

    @Test(description = "TC-API-018 Category list contains known categories and category filter is consistent")
    public void categoryFilter() {
        List<String> categories = given().spec(ApiSpecs.request()).get("/products/category-list")
                .then().spec(ApiSpecs.ok()).extract().jsonPath().getList("$", String.class);
        assertThat(categories).contains("beauty", "laptops", "smartphones");

        given().spec(ApiSpecs.request()).get("/products/category/{c}", "laptops")
                .then().spec(ApiSpecs.ok())
                .body("products.category", everyItem(equalTo("laptops")))
                .body("products.id", hasItem(notNullValue()));
    }

    @DataProvider
    public Object[][] badPaging() {
        return new Object[][]{
                {"limit", "-1", "Invalid 'limit' - should be a positive number"},
                {"limit", "abc", "Invalid 'limit' - should be a positive number"},
                {"skip", "-5", "Invalid 'skip' - should be a positive number"},
        };
    }

    @Test(dataProvider = "badPaging", description = "TC-API-019 Invalid paging parameters are rejected with 400")
    public void invalidPagingRejected(String param, String value, String message) {
        given().spec(ApiSpecs.request()).queryParam(param, value).get("/products")
                .then().spec(ApiSpecs.error(400)).body("message", equalTo(message));
    }

    @Test(groups = "known-defect", description = "DEF-API-008 Unknown category should return 404, not 200 with empty list")
    @Issue("DEF-API-008")
    public void unknownCategoryShouldBe404() {
        given().spec(ApiSpecs.request()).get("/products/category/does-not-exist").then().statusCode(404);
    }
}
