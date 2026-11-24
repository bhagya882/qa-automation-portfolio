package com.qaportfolio.security;

import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** OWASP A07:2021 Identification and Authentication Failures. */
public class A07AuthSessionTest extends SecurityBase {

    @Test(description = "SEC-TC-19 [A07] Logout invalidates the session token server-side")
    public void logoutInvalidatesToken() {
        String token = login("security.user1");
        as(token).get("/api/cart").then().statusCode(200);
        as(token).post("/api/auth/logout").then().statusCode(204);
        as(token).get("/api/cart").then().statusCode(401);
    }

    @Test(description = "SEC-TC-20 [A07] Session tokens are unique, long and URL-safe random values (CWE-330)")
    public void tokensAreUniqueAndRandomLooking() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            tokens.add(login("security.user2"));
        }
        assertThat(tokens).hasSize(10);
        tokens.forEach(t -> assertThat(t).hasSizeGreaterThanOrEqualTo(32).matches("[A-Za-z0-9_-]+")
                .doesNotContain("customer"));
    }

    @Test(description = "SEC-TC-21 [A07] Brute force: login throttled with 429 after 5 failures (CWE-307)")
    public void bruteForceIsThrottled() {
        String probeUser = "probe-" + System.nanoTime(); // unique user so real accounts are never locked
        for (int i = 1; i <= 5; i++) {
            api().body(Map.of("username", probeUser, "password", "wrong-" + i)).post("/api/auth/login")
                    .then().statusCode(401);
        }
        api().body(Map.of("username", probeUser, "password", "wrong-6")).post("/api/auth/login")
                .then().statusCode(429);
    }

    @Test(description = "SEC-TC-22 [A07] No user enumeration: unknown user and wrong password look identical (CWE-204)")
    public void noUserEnumeration() {
        Response unknown = api().body(Map.of("username", "no-such-user-" + System.nanoTime(), "password", "x"))
                .post("/api/auth/login");
        Response wrongPw = api().body(Map.of("username", "customer2", "password", "definitely-wrong"))
                .post("/api/auth/login");
        assertThat(unknown.statusCode()).isEqualTo(401).isEqualTo(wrongPw.statusCode());
        assertThat(unknown.asString()).isEqualTo(wrongPw.asString());
    }

    @Test(description = "SEC-TC-23 [A07] Missing credentials are rejected with 400")
    public void missingCredentialsRejected() {
        api().body(Map.of("username", "customer1")).post("/api/auth/login").then().statusCode(400);
    }
}
