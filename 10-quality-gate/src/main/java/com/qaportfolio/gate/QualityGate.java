package com.qaportfolio.gate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Continuous quality gate.
 *
 * <pre>
 * java -jar 10-quality-gate/target/10-quality-gate-1.0.0.jar --root . [--config file] [--out dir] [--set k=v]...
 * ./mvnw -q -pl 10-quality-gate exec:java -Dexec.args="--root ."
 * </pre>
 * Scans {@code <root>/<module>/target/surefire-reports/testng-results.xml}, {@code <root>/<module>/target/newman/*.xml}
 * and {@code <root>/<module>/target/flaky-tests.txt}; aggregates per module; applies thresholds; writes
 * {@code summary.md} + {@code summary.json}; appends the markdown to {@code $GITHUB_STEP_SUMMARY}; exits 1 on breach.
 */
public final class QualityGate {

    /** Outcome of one evaluation. */
    public record Result(Map<String, ModuleResult> modules, List<String> breaches, Set<String> flaky,
                         Properties config) {

        public boolean passed() {
            return breaches.isEmpty();
        }

        public int sum(java.util.function.ToIntFunction<ModuleResult> f) {
            return modules.values().stream().mapToInt(f).sum();
        }

        public double overallPassRate() {
            int executed = sum(ModuleResult::executed);
            return executed == 0 ? 0 : 100.0 * sum(ModuleResult::passed) / executed;
        }
    }

    private QualityGate() {
    }

    public static void main(String[] args) {
        Path root = Path.of(".");
        Path config = null;
        Path out = null;
        Map<String, String> overrides = new TreeMap<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--root" -> root = Path.of(args[++i]);
                case "--config" -> config = Path.of(args[++i]);
                case "--out" -> out = Path.of(args[++i]);
                case "--set" -> {
                    String[] kv = args[++i].split("=", 2);
                    overrides.put(kv[0], kv.length > 1 ? kv[1] : "");
                }
                case "-h", "--help" -> {
                    System.out.println("Usage: QualityGate --root <repo> [--config <properties>] [--out <dir>] [--set key=value]");
                    return;
                }
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    System.exit(2);
                }
            }
        }
        root = root.toAbsolutePath().normalize();
        if (config == null) {
            config = root.resolve("10-quality-gate/quality-gate.properties");
        }
        if (out == null) {
            out = root.resolve("target/quality-gate");
        }
        Properties props = loadConfig(config);
        overrides.forEach(props::setProperty);

        Result result = evaluate(root, props);
        String md = toMarkdown(result);
        write(out, result, md);
        System.out.println(md);
        String stepSummary = System.getenv("GITHUB_STEP_SUMMARY");
        if (stepSummary != null && !stepSummary.isBlank()) {
            try {
                Files.writeString(Path.of(stepSummary), md + System.lineSeparator(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                System.err.println("Could not write GITHUB_STEP_SUMMARY: " + e.getMessage());
            }
        }
        if (!result.passed()) {
            System.err.println("QUALITY GATE FAILED: " + result.breaches().size() + " breach(es)");
            System.exit(1);
        }
        System.out.println("QUALITY GATE PASSED");
    }

    // ------------------------------------------------------------------ evaluation

    public static Result evaluate(Path root, Properties cfg) {
        Map<String, ModuleResult> modules = scan(root);
        Set<String> flaky = readFlaky(root);
        List<String> breaches = new ArrayList<>();

        double minPassRate = dbl(cfg, "gate.minPassRate", 100);
        int maxFailures = integer(cfg, "gate.maxFailuresPerModule", 0);
        double maxSkippedPct = dbl(cfg, "gate.maxSkippedPercent", 20);
        int maxFlaky = integer(cfg, "gate.maxFlaky", 3);
        int minTests = integer(cfg, "gate.minTestsPerModule", 1);

        for (String required : list(cfg.getProperty("gate.requiredModules", ""))) {
            ModuleResult m = modules.get(required);
            if (m == null) {
                breaches.add("Required module `" + required + "` reported no results");
            } else if (m.executed() < minTests) {
                breaches.add("Required module `" + required + "` executed " + m.executed() + " test(s), minimum is " + minTests);
            }
        }
        for (ModuleResult m : modules.values()) {
            int allowed = integer(cfg, "gate.maxFailures." + m.module(), maxFailures);
            if (m.failed() > allowed) {
                breaches.add("`" + m.module() + "` has " + m.failed() + " failed test(s), allowed " + allowed);
            }
            double modMinRate = dbl(cfg, "gate.minPassRate." + m.module(), -1);
            if (modMinRate >= 0 && m.executed() > 0 && m.passRate() < modMinRate) {
                breaches.add("`" + m.module() + "` pass rate " + pct(m.passRate()) + " below " + pct(modMinRate));
            }
            if (m.total() > 0 && m.skippedPercent() > maxSkippedPct) {
                breaches.add("`" + m.module() + "` skipped/blocked " + pct(m.skippedPercent())
                        + " of tests, allowed " + pct(maxSkippedPct) + " (infrastructure problem?)");
            }
        }
        Result partial = new Result(modules, breaches, flaky, cfg);
        if (partial.sum(ModuleResult::executed) > 0 && partial.overallPassRate() < minPassRate) {
            breaches.add("Overall pass rate " + pct(partial.overallPassRate()) + " below " + pct(minPassRate));
        }
        if (flaky.size() > maxFlaky) {
            breaches.add(flaky.size() + " flaky test(s) (passed only after retry), allowed " + maxFlaky);
        }
        return partial;
    }

    /** Matches .../<module>/target/... and captures the module (the folder that owns target/). */
    private static final java.util.regex.Pattern MODULE = java.util.regex.Pattern.compile("(?:^|.*/)([^/]+)/target/(.+)$");

    /**
     * Module = folder directly above {@code target/}. This works both for a checked-out repo
     * ({@code 04-e2e-framework/target/...}) and for downloaded CI artifacts, where each artifact sits in its own
     * folder ({@code results/ui-04-e2e-framework-firefox/04-e2e-framework/target/...}). Results of the same module
     * from several jobs (e.g. one per browser) are summed.
     */
    static Map<String, ModuleResult> scan(Path root) {
        Map<String, ModuleResult> modules = new TreeMap<>();
        try (Stream<Path> files = Files.walk(root, 12)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> !ignored(root, p))
                    .sorted()
                    .forEach(p -> {
                        java.util.regex.Matcher m = MODULE.matcher(root.relativize(p).toString().replace('\\', '/'));
                        if (!m.matches()) {
                            return;
                        }
                        String module = m.group(1);
                        String inTarget = m.group(2);
                        if (inTarget.equals("surefire-reports/testng-results.xml")) {
                            ResultParser.parseTestNg(p, modules.computeIfAbsent(module, ModuleResult::new));
                        } else if (inTarget.matches("newman/[^/]+\\.xml")) {
                            ResultParser.parseJUnit(p, modules.computeIfAbsent(module, ModuleResult::new));
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return modules;
    }

    static Set<String> readFlaky(Path root) {
        Set<String> flaky = new LinkedHashSet<>();
        try (Stream<Path> files = Files.walk(root, 12)) {
            for (Path p : files.filter(Files::isRegularFile).filter(f -> !ignored(root, f)).sorted().toList()) {
                java.util.regex.Matcher m = MODULE.matcher(root.relativize(p).toString().replace('\\', '/'));
                if (!m.matches() || !m.group(2).equals("flaky-tests.txt")) {
                    continue;
                }
                for (String line : Files.readAllLines(p)) {
                    if (!line.isBlank()) {
                        flaky.add(m.group(1) + ": " + line.split(" retry=")[0]);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return flaky;
    }

    /** Skip dependencies, VCS data, sources and compiled resources (e.g. this module's own test fixtures). */
    private static boolean ignored(Path root, Path p) {
        String rel = "/" + root.relativize(p).toString().replace('\\', '/');
        return rel.contains("/node_modules/") || rel.contains("/.git/") || rel.contains("/src/")
                || rel.contains("/test-classes/") || rel.contains("/classes/");
    }

    // ------------------------------------------------------------------ reporting

    public static String toMarkdown(Result r) {
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(r.passed() ? "✅ Quality gate PASSED" : "❌ Quality gate FAILED").append("\n\n");
        sb.append("| Module | Passed | Failed | Blocked/Skipped | Retried | Pass rate | Status |\n");
        sb.append("|---|---:|---:|---:|---:|---:|---|\n");
        for (ModuleResult m : r.modules().values()) {
            boolean bad = r.breaches().stream().anyMatch(b -> b.contains("`" + m.module() + "`"));
            sb.append("| ").append(m.module()).append(" | ").append(m.passed()).append(" | ").append(m.failed())
                    .append(" | ").append(m.skipped()).append(" | ").append(m.retried()).append(" | ")
                    .append(m.executed() == 0 ? "n/a" : pct(m.passRate())).append(" | ")
                    .append(bad ? "❌" : "✅").append(" |\n");
        }
        sb.append("| **Total** | **").append(r.sum(ModuleResult::passed)).append("** | **")
                .append(r.sum(ModuleResult::failed)).append("** | **").append(r.sum(ModuleResult::skipped))
                .append("** | **").append(r.sum(ModuleResult::retried)).append("** | **")
                .append(pct(r.overallPassRate())).append("** | |\n\n");

        sb.append("**Thresholds:** min pass rate ").append(r.config().getProperty("gate.minPassRate", "100"))
                .append("% · max failures/module ").append(r.config().getProperty("gate.maxFailuresPerModule", "0"))
                .append(" · max skipped ").append(r.config().getProperty("gate.maxSkippedPercent", "20"))
                .append("% · max flaky ").append(r.config().getProperty("gate.maxFlaky", "3")).append("\n\n");

        if (!r.breaches().isEmpty()) {
            sb.append("### Breaches\n");
            r.breaches().forEach(b -> sb.append("- ").append(b).append('\n'));
            sb.append('\n');
        }
        if (!r.flaky().isEmpty()) {
            sb.append("### Flaky tests (passed after retry — fix or quarantine)\n");
            r.flaky().forEach(f -> sb.append("- ").append(f).append('\n'));
            sb.append('\n');
        }
        List<String> failures = new ArrayList<>();
        r.modules().values().forEach(m -> m.failures().forEach(f -> failures.add(m.module() + ": " + f)));
        if (!failures.isEmpty()) {
            sb.append("### Failed tests\n");
            failures.stream().limit(25).forEach(f -> sb.append("- ").append(f.replace("|", "\\|")).append('\n'));
            if (failures.size() > 25) {
                sb.append("- … and ").append(failures.size() - 25).append(" more\n");
            }
        }
        return sb.toString();
    }

    public static String toJson(Result r) {
        StringBuilder sb = new StringBuilder("{\n");
        sb.append("  \"passed\": ").append(r.passed()).append(",\n");
        sb.append("  \"overallPassRate\": ").append(String.format(Locale.ROOT, "%.2f", r.overallPassRate())).append(",\n");
        sb.append("  \"modules\": [\n");
        List<String> rows = new ArrayList<>();
        for (ModuleResult m : r.modules().values()) {
            rows.add(String.format(Locale.ROOT,
                    "    {\"module\": %s, \"passed\": %d, \"failed\": %d, \"skipped\": %d, \"retried\": %d, \"passRate\": %.2f}",
                    q(m.module()), m.passed(), m.failed(), m.skipped(), m.retried(), m.passRate()));
        }
        sb.append(String.join(",\n", rows)).append("\n  ],\n");
        sb.append("  \"breaches\": [").append(String.join(", ", r.breaches().stream().map(QualityGate::q).toList())).append("],\n");
        sb.append("  \"flaky\": [").append(String.join(", ", r.flaky().stream().map(QualityGate::q).toList())).append("]\n");
        return sb.append("}\n").toString();
    }

    private static void write(Path out, Result r, String md) {
        try {
            Files.createDirectories(out);
            Files.writeString(out.resolve("summary.md"), md, StandardCharsets.UTF_8);
            Files.writeString(out.resolve("summary.json"), toJson(r), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ------------------------------------------------------------------ helpers

    static Properties loadConfig(Path file) {
        Properties p = new Properties();
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            System.err.println("Config " + file + " not found, using defaults");
        }
        return p;
    }

    private static List<String> list(String csv) {
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static int integer(Properties p, String key, int fallback) {
        String v = p.getProperty(key);
        return v == null || v.isBlank() ? fallback : Integer.parseInt(v.trim());
    }

    private static double dbl(Properties p, String key, double fallback) {
        String v = p.getProperty(key);
        return v == null || v.isBlank() ? fallback : Double.parseDouble(v.trim());
    }

    private static String pct(double v) {
        return String.format(Locale.ROOT, "%.1f%%", v);
    }

    private static String q(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
