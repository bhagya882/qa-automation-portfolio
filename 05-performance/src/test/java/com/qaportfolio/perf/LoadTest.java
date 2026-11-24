package com.qaportfolio.perf;

import org.testng.annotations.Test;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;

import java.util.List;
import java.util.Set;

import static com.qaportfolio.perf.PerfSettings.*;
import static org.assertj.core.api.Assertions.assertThat;
import static us.abstracta.jmeter.javadsl.JmeterDsl.threadGroup;

/** PERF-TC-01 Load: expected peak concurrent shoppers with think time; every SLO must hold. */
public class LoadTest extends PerfBase {

    @Test(groups = "load", description = "Expected peak load meets p95/p99, error-rate and throughput SLOs")
    public void expectedPeakLoadMeetsSlos() throws Exception {
        int users = i("perf.load.users", 10);
        TestPlanStats stats = run("load", base -> threadGroup("shoppers")
                .rampToAndHold(users, seconds("perf.load.rampSeconds", 5), seconds("perf.load.holdSeconds", 20))
                .children(Journey.shopper(base, millis("perf.think.minMs", 300), millis("perf.think.maxMs", 800),
                        i("perf.reportPct", 10))));

        List<String> breaches = Thresholds.breaches(stats, Set.of(), true);
        assertThat(breaches).as("SLO breaches under expected load (%d users)", users).isEmpty();
    }
}
