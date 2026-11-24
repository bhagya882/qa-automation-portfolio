package com.qaportfolio.security;

import org.testng.annotations.Test;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;

/** OWASP A01:2021 Broken Access Control. */
public class A01AccessControlTest extends SecurityBase {

    @Test(description = "SEC-TC-01 [A01] Unauthenticated request to cart is rejected with 401")
    public void unauthenticatedCartRequestIsRejected() {
        api().get("/api/cart").then().statusCode(401);
        api().post("/api/orders").then().statusCode(401);
    }

    @Test(description = "SEC-TC-02 [A01] Forged/unknown bearer token is rejected with 401")
    public void forgedTokenIsRejected() {
        as("forged-token-AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").get("/api/cart").then().statusCode(401);
    }

    @Test(description = "SEC-TC-03 [A01] IDOR: a customer cannot read another customer's order (CWE-639)")
    public void customerCannotReadAnotherCustomersOrder() {
        String victim = login("security.user1");
        String attacker = login("security.user2");
        int orderId = placeOrder(victim);

        as(victim).get("/api/orders/" + orderId).then().statusCode(200); // positive control
        as(attacker).get("/api/orders/" + orderId).then()
                .statusCode(404)
                .body(not(containsString("\"lines\"")));
    }

    @Test(description = "SEC-TC-04 [A01] Vertical privilege escalation: USER role cannot call admin endpoint")
    public void customerCannotUseAdminEndpoint() {
        as(login("security.user1")).get("/api/admin/users").then().statusCode(403);
    }

    @Test(description = "SEC-TC-05 [A01] Positive control: ADMIN role can call admin endpoint")
    public void adminCanUseAdminEndpoint() {
        as(login("security.admin")).get("/api/admin/users").then().statusCode(200);
    }
}
