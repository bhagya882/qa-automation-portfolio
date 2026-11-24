package com.qaportfolio.perf;

import com.qaportfolio.sampleapp.DemoStoreApp;
import io.javalin.Javalin;
import io.qameta.allure.Allure;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import us.abstracta.jmeter.javadsl.core.DslTestPlan;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;
import us.abstracta.jmeter.javadsl.core.configs.DslCsvDataSet;
import us.abstracta.jmeter.javadsl.core.threadgroups.DslDefaultThreadGroup;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static us.abstracta.jmeter.javadsl.JmeterDsl.*;

/**
 * Starts the OWNED Demo Store in-process (unless {@code -Dperf.baseUrl} points at an external instance you are
 * authorised to load-test), and provides helpers to run a plan with JTL + HTML dashboard output and to export
 * the same plan as a .jmx for the JMeter GUI.
 */
public abstract class PerfBase {

    protected static String baseUrl;
    private static Javalin app;

    @BeforeSuite(alwaysRun = true)
    public void startTarget() throws Exception {
        if (baseUrl != null) {
            return; // already started for this suite
        }
        String external = PerfSettings.get("perf.baseUrl");
        if (external != null && !external.isBlank()) {
            baseUrl = external.replaceAll("/$", "");
        } else {
            int port = PerfSettings.i("perf.port", 7080);
            app = new DemoStoreApp(false).start(port);
            baseUrl = "http://localhost:" + port;
        }
        HttpResponse<String> health = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/health")).build(), HttpResponse.BodyHandlers.ofString());
        if (health.statusCode() != 200) {
            throw new IllegalStateException("Target not healthy: " + baseUrl + " -> " + health.statusCode());
        }
        System.out.printf("[perf] profile=%s target=%s%n", PerfSettings.profile(), baseUrl);
    }

    @AfterSuite(alwaysRun = true)
    public void stopTarget() {
        if (app != null) {
            app.stop();
            app = null;
        }
        baseUrl = null;
    }

    /**
     * Runs {@code threadGroupFor(baseUrl)} with CSV data + JTL + HTML dashboard into target/perf/&lt;name&gt;/, and saves
     * a portable copy (base http://localhost:7070, JTL only) as 05-performance/jmeter/&lt;name&gt;.jmx.
     */
    protected TestPlanStats run(String name, Function<String, DslDefaultThreadGroup> threadGroupFor) throws IOException {
        return runGroups(name, b -> List.of(threadGroupFor.apply(b)));
    }

    /** Same as {@link #run} but with several thread groups running concurrently. */
    protected TestPlanStats runGroups(String name, Function<String, List<DslDefaultThreadGroup>> groupsFor)
            throws IOException {
        Path out = Path.of("target", "perf", name);
        deleteRecursively(out);
        Files.createDirectories(out);

        List<DslTestPlan.TestPlanChild> children = new ArrayList<>();
        children.add(csvDataSet(testResource("data/shoppers.csv")).sharedIn(DslCsvDataSet.Sharing.ALL_THREADS));
        children.addAll(groupsFor.apply(baseUrl));
        children.add(jtlWriter(out.toString(), "results.jtl"));
        children.add(htmlReporter(out.resolve("html").toString()));
        DslTestPlan plan = testPlan(children.toArray(DslTestPlan.TestPlanChild[]::new));
        if (!name.contains("/")) { // sub-runs (e.g. stress steps) export one combined plan themselves
            exportJmx(name, groupsFor);
        }
        TestPlanStats stats = plan.run();

        long conflicts = JtlAnalyzer.read(out.resolve("results.jtl")).stream()
                .filter(x -> x.label().equals(Journey.PLACE_ORDER) && x.code().equals("409")).count();
        String summary = PerfReport.write(name, out, stats, conflicts);
        Allure.addAttachment(name + " summary", "text/markdown", summary, "md");
        System.out.println(summary);
        return stats;
    }

    protected void exportJmx(String name, Function<String, List<DslDefaultThreadGroup>> groupsFor) throws IOException {
        Path file = Path.of("jmeter", name.replace('/', '_') + ".jmx");
        Files.createDirectories(file.getParent());
        List<DslTestPlan.TestPlanChild> children = new ArrayList<>();
        // Relative to the .jmx location, which is how JMeter resolves CSV paths
        children.add(csvDataSet("../src/test/resources/data/shoppers.csv"));
        children.addAll(groupsFor.apply("http://localhost:7070"));
        children.add(jtlWriter("results", name.replace('/', '_') + ".jtl"));
        testPlan(children.toArray(DslTestPlan.TestPlanChild[]::new)).saveAsJmx(file.toString());
    }

    protected static void deleteRecursively(Path p) {
        if (!Files.exists(p)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(p)) {
            walk.sorted(Comparator.reverseOrder()).forEach(f -> {
                try {
                    Files.delete(f);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
