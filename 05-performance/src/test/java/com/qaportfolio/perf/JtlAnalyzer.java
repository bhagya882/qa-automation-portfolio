package com.qaportfolio.perf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Reads a CSV JTL to compute percentiles over time windows (used to detect latency drift in soak tests). */
public final class JtlAnalyzer {

    public record Sample(long timestamp, long elapsed, String label, String code, boolean success) {
    }

    private JtlAnalyzer() {
    }

    public static List<Sample> read(Path jtl) throws IOException {
        List<String> lines = Files.readAllLines(jtl);
        List<String> header = Arrays.asList(lines.get(0).split(","));
        int ts = header.indexOf("timeStamp");
        int el = header.indexOf("elapsed");
        int lb = header.indexOf("label");
        int ok = header.indexOf("success");
        int rc = header.indexOf("responseCode");
        List<Sample> out = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            // Labels and the fields we need contain no commas; response messages may, but come later
            String[] f = line.split(",", -1);
            if (f.length <= Math.max(Math.max(ts, el), Math.max(lb, ok))) {
                continue;
            }
            out.add(new Sample(Long.parseLong(f[ts]), Long.parseLong(f[el]), f[lb], rc >= 0 ? f[rc] : "", Boolean.parseBoolean(f[ok])));
        }
        return out;
    }

    public static long percentile(List<Sample> samples, double pct) {
        if (samples.isEmpty()) {
            return 0;
        }
        long[] v = samples.stream().mapToLong(Sample::elapsed).sorted().toArray();
        int idx = (int) Math.ceil(pct / 100.0 * v.length) - 1;
        return v[Math.max(0, Math.min(idx, v.length - 1))];
    }

    /** Splits samples into {@code n} equal time windows. */
    public static List<List<Sample>> windows(List<Sample> samples, int n) {
        long min = samples.stream().mapToLong(Sample::timestamp).min().orElse(0);
        long max = samples.stream().mapToLong(Sample::timestamp).max().orElse(0);
        long width = Math.max(1, (max - min + 1) / n + 1);
        List<List<Sample>> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(new ArrayList<>());
        }
        for (Sample s : samples) {
            out.get((int) Math.min(n - 1, (s.timestamp() - min) / width)).add(s);
        }
        return out;
    }
}
