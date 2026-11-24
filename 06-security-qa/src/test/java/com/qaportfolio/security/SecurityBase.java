package com.qaportfolio.security;

import com.qaportfolio.core.config.Config;
import com.qaportfolio.sampleapp.DemoStoreApp;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import java.util.Map;

/**
 * Authorised security tests against the OWNED Demo Store only.
 * Suite parameter {@code mode} selects the target: {@code secure} (default, must be all green) or
 * {@code insecure} (the deliberately vulnerable lab; failures there are the detected findings).
 */
@Listeners(FindingsReportListener.class)
public abstract class SecurityBase {

    protected static String baseUrl;
    protected static String mode;
    private static Javalin app;

    @Parameters("mode")
    @BeforeSuite(alwaysRun = true)
    public void startTarget(@Optional("secure") String suiteMode) {
        if (baseUrl != null) {
            return;
        }
        mode = System.getProperty("security.mode", suiteMode);
        String external = Config.get("security.baseUrl");
        if (external != null && !external.isBlank()) {
            baseUrl = external.replaceAll("/$", "");
        } else {
            boolean insecure = "insecure".equalsIgnoreCase(mode);
            int port = Config.getInt(insecure ? "security.port.insecure" : "security.port.secure", insecure ? 7091 : 7090);
            app = new DemoStoreApp(insecure).start(port);
            baseUrl = "http://localhost:" + port;
        }
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        System.out.printf("[security] mode=%s target=%s%n", mode, baseUrl);
    }

    @AfterSuite(alwaysRun = true)
    public void stopTarget() {
        if (app != null) {
            app.stop();
            app = null;
        }
        baseUrl = null;
    }

    protected static RequestSpecification api() {
        return RestAssured.given().baseUri(baseUrl).contentType(ContentType.JSON).accept(ContentType.JSON);
    }

    protected static RequestSpecification as(String token) {
        return api().header("Authorization", "Bearer " + token);
    }

    protected static String login(String userKey) {
        String username = Config.require(userKey);
        return api().body(Map.of("username", username, "password", passwordFor(username)))
                .post("/api/auth/login").then().statusCode(200).extract().path("token");
    }

    protected static String passwordFor(String username) {
        String envKey = "admin".equals(username) ? "DEMO_ADMIN_PASSWORD" : "DEMO_CUSTOMER_PASSWORD";
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return "admin".equals(username) ? "demo-admin-pass" : "demo-pass-1";
    }

    /** Creates an order for the user and returns its id. */
    protected static int placeOrder(String token) {
        as(token).body(Map.of("productId", 1, "quantity", 1)).post("/api/cart/items").then().statusCode(201);
        return as(token).post("/api/orders").then().statusCode(201).extract().path("id");
    }
}
