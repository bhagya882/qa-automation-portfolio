package com.qaportfolio.perf;

import org.testng.annotations.Test;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static com.qaportfolio.perf.PerfSettings.*;
import static org.assertj.core.api.Assertions.assertThat;
import static us.abstracta.jmeter.javadsl.JmeterDsl.threadGroup;

/**
 * PERF-TC-05 Bottleneck isolation for defect PERF-001: the same concurrency is applied to the sales report and,
 * as a control, to the product list. The product list meets its SLO; the sales report does not, because every
 * request serialises on one lock (see docs/performance-results.md). Group {@code known-defect}: this test is
 * EXPECTED TO FAIL until PERF-001 is fixed, and is excluded from the PR gate.
 */
public class BottleneckTest extends PerfBase {

    @Test(groups = "known-defect", description = "PERF-001: sales report p95 stays within SLO at moderate concurrency")
    public void salesReportScalesWithConcurrency() throws Exception {
        int users = i("perf.bottleneck.users", 20);
        Duration hold = seconds("perf.bottleneck.holdSeconds", 8);
        TestPlanStats stats = runGroups("bottleneck", base -> List.of(
                threadGroup("report-users").rampToAndHold(users, Duration.ofSeconds(1), hold)
                        .children(Journey.single(Journey.SALES_REPORT, base + "/api/reports/sales")),
                threadGroup("control-users").rampToAndHold(users, Duration.ofSeconds(1), hold)
                        .children(Journey.single(Journey.BROWSE, base + "/api/products?page=1&size=10"))));

        // Control must be healthy, otherwise the environment (not the endpoint) is the problem
        assertThat(Thresholds.breaches(stats, Set.of(Journey.SALES_REPORT), false))
                .as("Control endpoint breached SLO; environment problem, not PERF-001").isEmpty();
        assertThat(Thresholds.breaches(stats, Set.of(Journey.BROWSE), false))
                .as("PERF-001 sales report under %d concurrent users", users).isEmpty();
    }
}
