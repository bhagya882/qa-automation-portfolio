package com.qaportfolio.api.client;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/**
 * DummyJSON allows ~100 requests per window and answers 429 beyond that. Rather than produce false failures,
 * wait until the window resets (x-ratelimit-reset, epoch seconds) and retry once.
 */
public class RateLimitFilter implements Filter {

    @Override
    public Response filter(FilterableRequestSpecification req, FilterableResponseSpecification res, FilterContext ctx) {
        Response response = ctx.next(req, res);
        if (response.statusCode() != 429) {
            return response;
        }
        long waitMs = 5_000;
        String reset = response.header("x-ratelimit-reset");
        if (reset != null) {
            try {
                waitMs = Math.max(500, Long.parseLong(reset) * 1000 - System.currentTimeMillis() + 250);
            } catch (NumberFormatException ignored) {
                // keep default
            }
        }
        try {
            Thread.sleep(Math.min(waitMs, 15_000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return ctx.next(req, res);
    }
}
