package com.qaportfolio.perf;

import us.abstracta.jmeter.javadsl.core.TestPlanStats;
import us.abstracta.jmeter.javadsl.core.stats.StatsSummary;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.TreeSet;

/** Markdown summary per scenario, also appended to target/perf/summary.md for the whole run. */
public final class PerfReport {

    private PerfReport() {
    }

    public static String write(String scenario, Path dir, TestPlanStats stats, long checkoutConflicts) {
        StringBuilder sb = new StringBuilder();
        sb.append("### ").append(scenario).append(" (profile=").append(PerfSettings.profile()).append(")\n\n");
        sb.append(String.format(Locale.ROOT, "Duration %ds, %d samples, %.1f req/s, error rate %.2f%%%n%n",
                stats.duration().toSeconds(), stats.overall().samplesCount(), stats.overall().samples().perSecond(),
                Thresholds.errorRatePct(stats.overall())));
        sb.append("| Label | Samples | Errors % | Mean ms | Median ms | p90 ms | p95 ms | p99 ms | Max ms | req/s |\n");
        sb.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for (String label : new TreeSet<>(stats.labels())) {
            row(sb, label, stats.byLabel(label));
        }
        row(sb, "**TOTAL**", stats.overall());
        if (stats.labels().contains(Journey.PLACE_ORDER)) {
            sb.append(String.format(Locale.ROOT, "%nCheckout conflicts (409, shared demo account carts): %d of %d place_order samples%n",
                    checkoutConflicts, stats.byLabel(Journey.PLACE_ORDER).samplesCount()));
        }
        sb.append('\n');
        String md = sb.toString();
        try {
            Files.writeString(dir.resolve("summary.md"), md);
            Files.writeString(dir.getParent().resolve("summary.md"), md, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return md;
    }

    private static void row(StringBuilder sb, String label, StatsSummary s) {
        sb.append(String.format(Locale.ROOT, "| %s | %d | %.2f | %d | %d | %d | %d | %d | %d | %.1f |%n",
                label, s.samplesCount(), Thresholds.errorRatePct(s), s.sampleTime().mean().toMillis(),
                s.sampleTime().median().toMillis(), s.sampleTime().perc90().toMillis(),
                s.sampleTime().perc95().toMillis(), s.sampleTime().perc99().toMillis(),
                s.sampleTime().max().toMillis(), s.samples().perSecond()));
    }

    public static void append(Path file, String text) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
