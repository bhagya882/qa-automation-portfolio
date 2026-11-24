package com.qaportfolio.api.client;

import com.qaportfolio.core.config.Config;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;

/** Shared request/response specifications so every test speaks to the API the same way. */
public final class ApiSpecs {

    static {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    private ApiSpecs() {
    }

    public static String baseUrl() {
        return Config.get("api.baseUrl", "https://dummyjson.com");
    }

    public static long maxResponseMs() {
        return Config.getInt("api.maxResponseMs", 3000);
    }

    /** JSON request against the base URL with Allure request/response attachments and 429 back-off. */
    public static RequestSpecification request() {
        return new RequestSpecBuilder()
                .setBaseUri(baseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new RateLimitFilter())
                .addFilter(new AllureRestAssured())
                .build();
    }

    /** Successful JSON response within the response-time SLA. */
    public static ResponseSpecification ok(int status) {
        return new ResponseSpecBuilder()
                .expectStatusCode(status)
                .expectContentType(ContentType.JSON)
                .expectResponseTime(lessThan(maxResponseMs()))
                .build();
    }

    public static ResponseSpecification ok() {
        return ok(200);
    }

    /** Error response: given status, JSON body with a human readable "message". */
    public static ResponseSpecification error(int status) {
        return new ResponseSpecBuilder()
                .expectStatusCode(status)
                .expectContentType(ContentType.JSON)
                .expectBody("message", notNullValue())
                .expectResponseTime(lessThan(maxResponseMs()))
                .build();
    }
}
