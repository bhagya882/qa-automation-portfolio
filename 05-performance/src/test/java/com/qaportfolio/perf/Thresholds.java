package com.qaportfolio.perf;

import us.abstracta.jmeter.javadsl.core.TestPlanStats;
import us.abstracta.jmeter.javadsl.core.stats.StatsSummary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Evaluates TestPlanStats against configured SLOs:
 * <ul>
 *   <li>{@code perf.threshold.p95Ms[.<label>]} and {@code perf.threshold.p99Ms[.<label>]} per sampler label</li>
 *   <li>{@code perf.threshold.errorRatePct} overall</li>
 *   <li>{@code perf.threshold.minRps} overall throughput (only when {@code checkThroughput})</li>
 * </ul>
 * The {@code prefix} lets scenarios use their own limits, e.g. {@code perf.spike.p99Ms}.
 */
public final class Thresholds {

    private Thresholds() {
    }

    public static List<String> breaches(TestPlanStats stats, Collection<String> ignoredLabels, boolean checkThroughput) {
        return breaches(stats, ignoredLabels, checkThroughput, "perf.threshold");
    }

    public static List<String> breaches(TestPlanStats stats, Collection<String> ignoredLabels,
                                        boolean checkThroughput, String prefix) {
        List<String> out = new ArrayList<>();
        for (String label : stats.labels()) {
            if (ignoredLabels.contains(label)) {
                continue;
            }
            StatsSummary s = stats.byLabel(label);
            long p95 = s.sampleTime().perc95().toMillis();
            long p99 = s.sampleTime().perc99().toMillis();
            int p95Max = limit(prefix, "p95Ms", label, 300);
            int p99Max = limit(prefix, "p99Ms", label, 800);
            if (p95 > p95Max) {
                out.add(String.format("%s p95 %dms > %dms", label, p95, p95Max));
            }
            if (p99 > p99Max) {
                out.add(String.format("%s p99 %dms > %dms", label, p99, p99Max));
            }
        }
        double errPct = errorRatePct(stats.overall());
        double errMax = PerfSettings.d(prefix + ".errorRatePct", PerfSettings.d("perf.threshold.errorRatePct", 1.0));
        if (errPct > errMax) {
            out.add(String.format("error rate %.2f%% > %.2f%%", errPct, errMax));
        }
        if (checkThroughput) {
            double rps = stats.overall().samples().perSecond();
            double minRps = PerfSettings.d(prefix + ".minRps", PerfSettings.d("perf.threshold.minRps", 0));
            if (rps < minRps) {
                out.add(String.format("throughput %.1f req/s < %.1f req/s", rps, minRps));
            }
        }
        return out;
    }

    public static double errorRatePct(StatsSummary s) {
        long n = s.samplesCount();
        return n == 0 ? 0 : 100.0 * s.errorsCount() / n;
    }

    /** Most specific wins: prefix.metric.label, prefix.metric, perf.threshold.metric.label, perf.threshold.metric. */
    static int limit(String prefix, String metric, String label, int fallback) {
        for (String key : List.of(prefix + "." + metric + "." + label, prefix + "." + metric,
                "perf.threshold." + metric + "." + label, "perf.threshold." + metric)) {
            String v = PerfSettings.get(key);
            if (v != null && !v.isBlank()) {
                return Integer.parseInt(v.trim());
            }
        }
        return fallback;
    }

    public static Set<String> set(String... labels) {
        return Set.of(labels);
    }
}
