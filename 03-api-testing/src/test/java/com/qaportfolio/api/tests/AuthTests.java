package com.qaportfolio.api.tests;

import com.qaportfolio.api.client.ApiSpecs;
import com.qaportfolio.api.client.AuthClient;
import com.qaportfolio.api.model.LoginRequest;
import com.qaportfolio.api.model.LoginResponse;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;

@Epic("REST API")
@Feature("Authentication")
public class AuthTests {

    @Test(groups = "smoke", description = "TC-API-001 Valid login returns JWT access + refresh tokens")
    @Severity(SeverityLevel.BLOCKER)
    public void validLoginReturnsTokens() {
        LoginResponse res = AuthClient.login(AuthClient.defaultCredentials())
                .then().spec(ApiSpecs.ok())
                .body(matchesJsonSchemaInClasspath("schemas/login-response.json"))
                .extract().as(LoginResponse.class);

        assertThat(res.username()).isEqualTo(AuthClient.defaultCredentials().username());
        assertThat(res.accessToken().split("\\.")).hasSize(3);
        assertThat(res.refreshToken()).isNotEqualTo(res.accessToken());
    }

    @Test(groups = "smoke", description = "TC-API-002 GET /auth/me with a valid bearer token returns the logged-in user")
    public void meWithTokenReturnsCurrentUser() {
        AuthClient.authenticated().get("/auth/me")
                .then().spec(ApiSpecs.ok())
                .body("username", equalTo(AuthClient.session().username()))
                .body("id", equalTo(AuthClient.session().id()));
    }

    @DataProvider
    public Object[][] badTokens() {
        return new Object[][]{
                {null, "Access Token is required"},
                {"garbage", "Invalid/Expired Token!"},
        };
    }

    @Test(dataProvider = "badTokens", description = "TC-API-003 /auth/me rejects missing / malformed / forged tokens with 401")
    public void meRejectsBadTokens(String token, String expectedMessage) {
        var req = given().spec(ApiSpecs.request());
        if (token != null) {
            req.auth().oauth2(token);
        }
        req.get("/auth/me").then().spec(ApiSpecs.error(401)).body("message", equalTo(expectedMessage));
    }

    @Test(description = "TC-API-004 Refresh token issues new tokens that authenticate /auth/me")
    public void refreshTokenIssuesWorkingAccessToken() {
        String refresh = AuthClient.session().refreshToken();
        Response r = given().spec(ApiSpecs.request())
                .body(Map.of("refreshToken", refresh, "expiresInMins", 30))
                .post("/auth/refresh");
        r.then().spec(ApiSpecs.ok()).body("$", hasKey("accessToken")).body("$", hasKey("refreshToken"));

        given().spec(ApiSpecs.request()).auth().oauth2(r.path("accessToken"))
                .get("/auth/me").then().spec(ApiSpecs.ok())
                .body("username", equalTo(AuthClient.session().username()));
    }

    @DataProvider
    public Object[][] missingFields() {
        return new Object[][]{
                {new LoginRequest(null, null, null)},
                {new LoginRequest("emilys", null, null)},
                {new LoginRequest(null, "emilyspass", null)},
                {new LoginRequest("", "", null)},
        };
    }

    @Test(dataProvider = "missingFields", description = "TC-API-005 Login without username/password is rejected with 400")
    public void loginRequiresUsernameAndPassword(LoginRequest body) {
        AuthClient.login(body).then().spec(ApiSpecs.error(400))
                .body(matchesJsonSchemaInClasspath("schemas/error.json"))
                .body("message", equalTo("Username and password required"));
    }

    @Test(description = "TC-API-006 Wrong password is rejected and no token is issued")
    public void wrongPasswordIssuesNoToken() {
        Response r = AuthClient.login(new LoginRequest("emilys", "definitely-wrong", null));
        assertThat(r.statusCode()).isBetween(400, 499);
        r.then().body("$", not(hasKey("accessToken"))).body("message", equalTo("Invalid credentials"));
    }

    @Test(groups = "known-defect", description = "DEF-API-001 Wrong password should return 401 Unauthorized, not 400")
    @Issue("DEF-API-001")
    @Severity(SeverityLevel.MINOR)
    public void wrongPasswordShouldReturn401() {
        AuthClient.login(new LoginRequest("emilys", "definitely-wrong", null)).then().statusCode(401);
    }

    @DataProvider
    public Object[][] forgedJwts() {
        return new Object[][]{{"eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MX0.invalidsignature"}, {"a.b.c"}};
    }

    @Test(groups = "known-defect", dataProvider = "forgedJwts",
            description = "DEF-API-010 Forged/structurally-valid JWT must give 401, not 500 Internal Server Error")
    @Issue("DEF-API-010")
    @Severity(SeverityLevel.CRITICAL)
    public void forgedJwtShouldBe401(String token) {
        given().spec(ApiSpecs.request()).auth().oauth2(token).get("/auth/me").then().spec(ApiSpecs.error(401));
    }

    @Test(description = "TC-API-007 Auth cookies are HttpOnly and Secure")
    public void authCookiesAreHttpOnlyAndSecure() {
        java.util.List<String> cookies = AuthClient.login(AuthClient.defaultCredentials())
                .then().statusCode(200).extract().headers().getValues("Set-Cookie");
        assertThat(cookies).isNotEmpty().allSatisfy(c -> assertThat(c).contains("HttpOnly").contains("Secure"));
    }

    @Test(groups = "known-defect", description = "DEF-API-011 Auth cookies should declare SameSite (CSRF defence in depth)")
    @Issue("DEF-API-011")
    @Severity(SeverityLevel.MINOR)
    public void authCookiesDeclareSameSite() {
        java.util.List<String> cookies = AuthClient.login(AuthClient.defaultCredentials())
                .then().statusCode(200).extract().headers().getValues("Set-Cookie");
        assertThat(cookies).allSatisfy(c -> assertThat(c).containsIgnoringCase("SameSite="));
    }

    @Test(groups = "known-defect", description = "DEF-API-002 /auth/me must not expose password, SSN or card data")
    @Issue("DEF-API-002")
    @Severity(SeverityLevel.CRITICAL)
    public void meMustNotExposeSecrets() {
        AuthClient.authenticated().get("/auth/me").then().spec(ApiSpecs.ok())
                .body("$", not(hasKey("password")))
                .body("$", not(hasKey("ssn")))
                .body("bank", not(hasKey("cardNumber")));
    }
}
