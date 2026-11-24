package com.qaportfolio.perf;

import org.testng.annotations.Test;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.qaportfolio.perf.PerfSettings.*;
import static org.assertj.core.api.Assertions.assertThat;
import static us.abstracta.jmeter.javadsl.JmeterDsl.threadGroup;

/**
 * PERF-TC-04 Endurance (soak): steady moderate load for a long period (30 min in the full profile). Besides the
 * normal SLOs, latency must not drift: p95 in the last third of the run may not exceed the first third by more
 * than {@code perf.endurance.maxDriftPct} (plus {@code perf.endurance.driftSlackMs} absolute noise allowance).
 * Drift is the classic symptom of leaks and unbounded growth (sessions, carts, orders kept in memory).
 */
public class EnduranceTest extends PerfBase {

    @Test(groups = "endurance", description = "Steady load for a long period meets SLOs without latency drift")
    public void steadyLoadWithoutDegradation() throws Exception {
        int users = i("perf.endurance.users", 5);
        TestPlanStats stats = run("endurance", base -> threadGroup("soak")
                .rampToAndHold(users, seconds("perf.endurance.rampSeconds", 3), seconds("perf.endurance.holdSeconds", 25))
                .children(Journey.shopper(base, millis("perf.think.minMs", 300), millis("perf.think.maxMs", 800),
                        i("perf.reportPct", 10))));
        assertThat(Thresholds.breaches(stats, Set.of(), false)).as("SLO breaches during soak").isEmpty();

        List<JtlAnalyzer.Sample> samples = JtlAnalyzer.read(Path.of("target", "perf", "endurance", "results.jtl"))
                .stream().filter(s -> !s.label().equals(Journey.SALES_REPORT)).toList();
        List<List<JtlAnalyzer.Sample>> thirds = JtlAnalyzer.windows(samples, 3);
        long first = JtlAnalyzer.percentile(thirds.get(0), 95);
        long last = JtlAnalyzer.percentile(thirds.get(2), 95);
        double maxDrift = d("perf.endurance.maxDriftPct", 50);
        long slack = i("perf.endurance.driftSlackMs", 20);
        String drift = String.format(Locale.ROOT,
                "Endurance drift: p95 first third=%dms (%d samples), last third=%dms (%d samples), allowed <= %dms%n%n",
                first, thirds.get(0).size(), last, thirds.get(2).size(), (long) (first * (1 + maxDrift / 100)) + slack);
        PerfReport.append(Path.of("target", "perf", "summary.md"), drift);
        System.out.print(drift);

        assertThat(last).as(drift).isLessThanOrEqualTo((long) (first * (1 + maxDrift / 100)) + slack);
    }
}
