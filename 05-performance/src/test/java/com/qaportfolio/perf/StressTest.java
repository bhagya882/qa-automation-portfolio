package com.qaportfolio.perf;

import org.testng.annotations.Test;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;
import us.abstracta.jmeter.javadsl.core.threadgroups.DslDefaultThreadGroup;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.qaportfolio.perf.PerfSettings.*;
import static org.assertj.core.api.Assertions.assertThat;
import static us.abstracta.jmeter.javadsl.JmeterDsl.threadGroup;

/**
 * PERF-TC-02 Stress: step the user count up ({@code perf.stress.levels}) with short think time until SLOs
 * break, and record the breaking point and which endpoint breaks first.
 *
 * <p>Pass criteria: no errors above the error-rate SLO at any level, and every endpoint except those in
 * {@code perf.stress.excludeLabels} (the known bottleneck, tracked as PERF-001 by {@link BottleneckTest}) stays within
 * SLO up to {@code perf.stress.minSustainableUsers}.
 */
public class StressTest extends PerfBase {

    @Test(groups = "stress", description = "Step load up to find the breaking point; core endpoints sustain minimum capacity")
    public void findBreakingPoint() throws Exception {
        List<Integer> levels = ints("perf.stress.levels", "5,10,20,40");
        int minSustainable = i("perf.stress.minSustainableUsers", 20);
        Set<String> excluded = new HashSet<>(list("perf.stress.excludeLabels", Journey.SALES_REPORT));

        StringBuilder table = new StringBuilder("### stress steps (profile=" + profile() + ")\n\n"
                + "| Users | req/s | Errors % | p95 all ms | Worst label (p95 ms) | SLO breaches |\n"
                + "|---:|---:|---:|---:|---|---|\n");
        Integer breakingPoint = null;
        List<String> failures = new ArrayList<>();

        // One stepped plan (all levels in sequence) for opening/running in the JMeter GUI
        exportJmx("stress", base -> {
            DslDefaultThreadGroup tg = threadGroup("stress-stepped");
            for (int users : levels) {
                tg = tg.rampTo(users, seconds("perf.stress.rampSeconds", 2)).holdFor(seconds("perf.stress.stepSeconds", 8));
            }
            return List.of(tg.children(Journey.shopper(base, millis("perf.stress.thinkMinMs", 50),
                    millis("perf.stress.thinkMaxMs", 150), i("perf.stress.reportPct", 50))));
        });

        for (int users : levels) {
            TestPlanStats stats = run(String.format("stress/step-%03d-users", users), base -> threadGroup("stress")
                    .rampToAndHold(users, seconds("perf.stress.rampSeconds", 2), seconds("perf.stress.stepSeconds", 8))
                    .children(Journey.shopper(base, millis("perf.stress.thinkMinMs", 50),
                            millis("perf.stress.thinkMaxMs", 150), i("perf.stress.reportPct", 50))));

            List<String> all = Thresholds.breaches(stats, Set.of(), false);
            List<String> core = Thresholds.breaches(stats, excluded, false);
            String worst = stats.labels().stream()
                    .max((a, b) -> stats.byLabel(a).sampleTime().perc95().compareTo(stats.byLabel(b).sampleTime().perc95()))
                    .map(l -> l + " (" + stats.byLabel(l).sampleTime().perc95().toMillis() + ")").orElse("-");
            table.append(String.format(Locale.ROOT, "| %d | %.1f | %.2f | %d | %s | %s |%n", users,
                    stats.overall().samples().perSecond(), Thresholds.errorRatePct(stats.overall()),
                    stats.overall().sampleTime().perc95().toMillis(), worst, all.isEmpty() ? "none" : String.join("; ", all)));

            if (breakingPoint == null && !all.isEmpty()) {
                breakingPoint = users;
            }
            if (users <= minSustainable && !core.isEmpty()) {
                failures.add(users + " users: " + core);
            }
            if (Thresholds.errorRatePct(stats.overall()) > 50) {
                break; // system is down, no point going higher
            }
        }
        table.append(String.format("%nBreaking point (first level with any SLO breach): %s%n%n",
                breakingPoint == null ? "not reached up to " + levels.get(levels.size() - 1) + " users" : breakingPoint + " users"));
        PerfReport.append(Path.of("target", "perf", "stress", "stress-steps.md"), table.toString());
        PerfReport.append(Path.of("target", "perf", "summary.md"), table.toString());
        System.out.println(table);
        io.qameta.allure.Allure.addAttachment("stress steps", "text/markdown", table.toString(), "md");

        assertThat(failures).as("Core endpoints breached SLO at or below %d users", minSustainable).isEmpty();
    }
}
