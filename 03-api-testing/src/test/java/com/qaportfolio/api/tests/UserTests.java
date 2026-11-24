package com.qaportfolio.api.tests;

import com.qaportfolio.api.client.ApiSpecs;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

@Epic("REST API")
@Feature("Users")
public class UserTests {

    @Test(groups = "smoke", description = "TC-API-040 User by id matches schema")
    public void userById() {
        given().spec(ApiSpecs.request()).get("/users/{id}", 1)
                .then().spec(ApiSpecs.ok())
                .body(matchesJsonSchemaInClasspath("schemas/user.json"))
                .body("id", equalTo(1));
    }

    @Test(description = "TC-API-041 User list honours limit and select")
    public void listUsers() {
        given().spec(ApiSpecs.request()).queryParam("limit", 5).queryParam("select", "firstName,email")
                .get("/users").then().spec(ApiSpecs.ok())
                .body("users", hasSize(5)).body("total", greaterThan(5));
    }

    @Test(description = "TC-API-042 User filter by nested key returns only matching users")
    public void filterUsers() {
        given().spec(ApiSpecs.request()).queryParam("key", "hair.color").queryParam("value", "Brown")
                .get("/users/filter").then().spec(ApiSpecs.ok())
                .body("users.hair.color", everyItem(equalTo("Brown")));
    }

    @Test(description = "TC-API-043 User search matches first/last name")
    public void searchUsers() {
        given().spec(ApiSpecs.request()).queryParam("q", "John")
                .get("/users/search").then().spec(ApiSpecs.ok())
                .body("total", greaterThan(0))
                .body("users[0].lastName", containsStringIgnoringCase("john"));
    }

    @Test(description = "TC-API-044 Unknown user id returns 404")
    public void unknownUser404() {
        given().spec(ApiSpecs.request()).get("/users/{id}", 0)
                .then().spec(ApiSpecs.error(404)).body("message", equalTo("User with id '0' not found"));
    }
}
