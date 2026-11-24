package com.qaportfolio.security;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;

/** OWASP A03:2021 Injection (reflected XSS, injection-style input, malformed input). */
public class A03InjectionTest extends SecurityBase {

    @DataProvider
    public Object[][] xssPayloads() {
        return new Object[][]{
                {"<script>alert(1)</script>"},
                {"\"><img src=x onerror=alert(1)>"},
                {"<svg/onload=alert(document.domain)>"},
        };
    }

    @Test(dataProvider = "xssPayloads",
            description = "SEC-TC-11 [A03] Reflected XSS: search term is HTML-encoded in /search (CWE-79)")
    public void searchTermIsEncoded(String payload) {
        String html = api().accept("text/html").queryParam("q", payload).get("/search").then()
                .statusCode(200).extract().asString();
        assertThat(html).as("raw payload must not be reflected").doesNotContain(payload);
        assertThat(html).contains("&lt;");
    }

    @Test(description = "SEC-TC-12 [A03] SQL-injection style product id is rejected as 400, not 500")
    public void injectionStyleIdIsRejected() {
        api().get("/api/products/1' OR '1'='1").then().statusCode(400).body(not(containsString("Exception")));
    }

    @Test(description = "SEC-TC-13 [A03] Injection-style search term is treated as data (no extra results, no error)")
    public void injectionStyleSearchIsData() {
        api().queryParam("search", "' OR 1=1 --").get("/api/products").then().statusCode(200).body("total", equalTo(0));
    }

    @Test(groups = "known-defect",
            description = "SEC-TC-14 [A03/A04] Malformed JSON body is rejected with 400 (SEC-007: returns 500, CWE-755)")
    public void malformedJsonIsRejectedWith400() {
        api().body("{not-json").post("/api/auth/login").then().statusCode(400);
        api().body("").post("/api/auth/login").then().statusCode(400);
    }
}
