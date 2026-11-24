package com.qaportfolio.perf;

import org.apache.http.entity.ContentType;
import us.abstracta.jmeter.javadsl.core.assertions.DslResponseAssertion;
import us.abstracta.jmeter.javadsl.core.threadgroups.BaseThreadGroup.ThreadGroupChild;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static us.abstracta.jmeter.javadsl.JmeterDsl.*;

/**
 * Realistic shopper journey against the Demo Store API. One iteration = one shopping session:
 * browse -> search -> view product -> login -> add to cart -> place order, and ~10% of iterations also
 * open the sales report (back-office action). Data comes from {@code data/shoppers.csv}.
 *
 * <p>Sampler labels are stable keys used for thresholds ({@code perf.threshold.p95Ms.<label>}).
 */
public final class Journey {

    public static final String BROWSE = "browse_products";
    public static final String SEARCH = "search_products";
    public static final String VIEW = "view_product";
    public static final String LOGIN = "login";
    public static final String ADD_TO_CART = "add_to_cart";
    public static final String PLACE_ORDER = "place_order";
    public static final String SALES_REPORT = "sales_report";

    private Journey() {
    }

    /**
     * @param base       e.g. http://localhost:7080
     * @param thinkMin   min think time before each request
     * @param thinkMax   max think time before each request
     * @param reportPct  percentage of iterations that also request the sales report (0-100)
     */
    public static ThreadGroupChild[] shopper(String base, Duration thinkMin, Duration thinkMax, int reportPct) {
        List<ThreadGroupChild> steps = new ArrayList<>();
        if (!thinkMax.isZero()) {
            steps.add(uniformRandomTimer(thinkMin, thinkMax));
        }
        steps.add(httpSampler(BROWSE, base + "/api/products?page=${page}&size=10"));
        steps.add(httpSampler(SEARCH, base + "/api/products?search=${searchTerm}"));
        steps.add(httpSampler(VIEW, base + "/api/products/${productId}"));
        steps.add(httpSampler(LOGIN, base + "/api/auth/login")
                // Demo-lab password from the owned sample-app; override with -Jdemo.password=... in JMeter GUI/CLI
                .post("{\"username\":\"${username}\",\"password\":\"${__P(demo.password," + demoPassword() + ")}\"}",
                        ContentType.APPLICATION_JSON)
                .children(
                        responseAssertion().containsSubstrings("\"token\""),
                        jsonExtractor("TOKEN", "token")));
        steps.add(httpSampler(ADD_TO_CART, base + "/api/cart/items")
                .header("Authorization", "Bearer ${TOKEN}")
                .post("{\"productId\":${productId},\"quantity\":${quantity}}", ContentType.APPLICATION_JSON));
        steps.add(httpSampler(PLACE_ORDER, base + "/api/orders")
                .header("Authorization", "Bearer ${TOKEN}")
                .post("", ContentType.APPLICATION_JSON)
                // The lab app has only 2 customer accounts, so concurrent threads share a cart: when another
                // session of the same account already checked it out, 409 "Cart is empty" is the CORRECT business
                // response. It is accepted here and counted separately (see "checkout conflicts" in summaries).
                .children(responseAssertion("201 created or 409 shared-cart conflict")
                        .fieldToTest(DslResponseAssertion.TargetField.RESPONSE_CODE)
                        .ignoreStatus()
                        .matchesRegexes("201|409")));
        if (reportPct > 0) {
            steps.add(ifController("${__jexl3(${__Random(1,100)} <= " + reportPct + ")}",
                    httpSampler(SALES_REPORT, base + "/api/reports/sales")));
        }
        return steps.toArray(ThreadGroupChild[]::new);
    }

    /** Only the given endpoint, no think time — used to isolate a suspected bottleneck. */
    public static ThreadGroupChild[] single(String label, String url) {
        return new ThreadGroupChild[]{httpSampler(label, url)};
    }

    static String demoPassword() {
        String env = System.getenv("DEMO_CUSTOMER_PASSWORD");
        return env == null || env.isBlank() ? "demo-pass-1" : env;
    }
}
