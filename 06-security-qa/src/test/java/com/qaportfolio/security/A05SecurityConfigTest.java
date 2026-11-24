package com.qaportfolio.security;

import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** OWASP A05:2021 Security Misconfiguration (+ A02 transport hints like HSTS). */
public class A05SecurityConfigTest extends SecurityBase {

    private static final Map<String, String> REQUIRED_HEADERS = Map.of(
            "X-Content-Type-Options", "nosniff",
            "X-Frame-Options", "DENY",
            "Content-Security-Policy", "default-src 'self'",
            "Referrer-Policy", "no-referrer",
            "Strict-Transport-Security", "max-age=",
            "Permissions-Policy", "camera=()");

    @DataProvider
    public Object[][] endpoints() {
        return new Object[][]{{"/health"}, {"/api/products"}, {"/search?q=lamp"}};
    }

    @Test(dataProvider = "endpoints",
            description = "SEC-TC-06 [A05] Security response headers present on every response type")
    public void securityHeadersPresent(String path) {
        Response r = api().get(path);
        assertThat(r.statusCode()).isEqualTo(200);
        REQUIRED_HEADERS.forEach((name, expected) ->
                assertThat(r.header(name)).as("header %s on %s", name, path).isNotNull().contains(expected));
    }

    @Test(description = "SEC-TC-07 [A05] API responses are not cacheable (Cache-Control: no-store)")
    public void apiResponsesAreNotCacheable() {
        assertThat(api().get("/api/products").header("Cache-Control")).as("Cache-Control").isEqualTo("no-store");
    }

    @Test(description = "SEC-TC-08 [A05] Server header does not disclose product/version (CWE-200)")
    public void serverHeaderDoesNotDiscloseVersion() {
        String server = api().get("/health").header("Server");
        assertThat(server == null || !server.matches(".*\\d.*"))
                .as("Server header should be absent or version-free but was: %s", server).isTrue();
    }

    @Test(description = "SEC-TC-09 [A05] Error responses do not leak stack traces (CWE-209)")
    public void errorsDoNotLeakStackTraces() {
        String body = api().body("{not-json").post("/api/auth/login").asString();
        assertThat(body).as("error body").doesNotContain("trace").doesNotContain("Exception")
                .doesNotContainPattern("\\bat [a-z]+\\.[a-z]+\\.");
    }

    @Test(description = "SEC-TC-10 [A05/A07] Session cookie has HttpOnly, Secure and SameSite=Strict")
    public void sessionCookieHasSecureFlags() {
        String setCookie = api().body(Map.of("username", "customer1", "password", passwordFor("customer1")))
                .post("/api/auth/login").then().statusCode(200).extract().header("Set-Cookie");
        assertThat(setCookie).as("Set-Cookie").isNotNull()
                .containsIgnoringCase("HttpOnly").containsIgnoringCase("Secure").containsIgnoringCase("SameSite=Strict");
    }
}
