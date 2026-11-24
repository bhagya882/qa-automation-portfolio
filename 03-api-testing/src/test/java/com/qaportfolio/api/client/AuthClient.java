package com.qaportfolio.api.client;

import com.qaportfolio.api.model.LoginRequest;
import com.qaportfolio.api.model.LoginResponse;
import com.qaportfolio.core.config.Config;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/** Logs in once per thread and hands out authenticated request specs. Tokens are never persisted. */
public final class AuthClient {

    private static final ThreadLocal<LoginResponse> SESSION = new ThreadLocal<>();

    private AuthClient() {
    }

    public static LoginRequest defaultCredentials() {
        return new LoginRequest(Config.require("api.username"), Config.require("api.password"), 30);
    }

    @Step("POST /auth/login as {credentials.username}")
    public static Response login(LoginRequest credentials) {
        return given().spec(ApiSpecs.request()).body(credentials).post("/auth/login");
    }

    public static LoginResponse session() {
        LoginResponse s = SESSION.get();
        if (s == null) {
            s = login(defaultCredentials()).then().spec(ApiSpecs.ok()).extract().as(LoginResponse.class);
            SESSION.set(s);
        }
        return s;
    }

    public static RequestSpecification authenticated() {
        return given().spec(ApiSpecs.request()).auth().oauth2(session().accessToken());
    }
}
