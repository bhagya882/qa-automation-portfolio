package com.qaportfolio.a11y.support;

import com.deque.html.axecore.results.CheckedNode;
import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import com.deque.html.axecore.selenium.AxeBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.qaportfolio.core.config.Config;
import com.qaportfolio.core.driver.DriverManager;
import io.qameta.allure.Allure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs axe-core with the configured WCAG tags, stores raw JSON per page in {@code target/a11y/} and keeps a
 * per-page cache so the baseline test and the known-defect test don't rescan the same page.
 */
public final class AxeScanner {

    public record Violation(String page, String rule, String impact, String help, String helpUrl,
                            List<String> wcagTags, List<NodeInfo> nodes) {
    }

    public record NodeInfo(String target, String html, String failureSummary) {
    }

    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final Map<SauceDemo.Page, List<Violation>> CACHE = new ConcurrentHashMap<>();
    /** axe "incomplete" results: rules axe could not decide (e.g. contrast over images) — need manual review. */
    private static final Map<String, List<String>> NEEDS_REVIEW = new ConcurrentHashMap<>();
    public static final Path OUT = Path.of("target", "a11y");

    private AxeScanner() {
    }

    public static List<Violation> scanCached(SauceDemo.Page page) {
        return CACHE.computeIfAbsent(page, p -> {
            SauceDemo.goTo(p);
            return scan(p.name());
        });
    }

    public static Map<SauceDemo.Page, List<Violation>> all() {
        return CACHE;
    }

    public static Map<String, List<String>> needsReview() {
        return NEEDS_REVIEW;
    }

    public static List<Violation> scan(String pageName) {
        List<String> tags = Arrays.stream(Config.get("axe.tags", "wcag2a,wcag2aa").split(","))
                .map(String::trim).toList();
        Results results = new AxeBuilder().withTags(tags).analyze(DriverManager.get());
        try {
            Files.createDirectories(OUT);
            String raw = JSON.writeValueAsString(results);
            Files.writeString(OUT.resolve(pageName + ".axe.json"), raw);
            Allure.addAttachment("axe results " + pageName, "application/json", raw, "json");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<String> review = new ArrayList<>();
        for (Rule r : results.getIncomplete()) {
            review.add(r.getId() + " (" + r.getImpact() + ", " + r.getNodes().size() + " nodes)");
        }
        NEEDS_REVIEW.put(pageName, review);
        List<Violation> out = new ArrayList<>();
        for (Rule r : results.getViolations()) {
            List<NodeInfo> nodes = new ArrayList<>();
            for (CheckedNode n : r.getNodes()) {
                nodes.add(new NodeInfo(String.valueOf(n.getTarget()), n.getHtml(), n.getFailureSummary()));
            }
            List<String> wcag = r.getTags().stream()
                    .filter(t -> t.startsWith("wcag") || t.equals("best-practice")).toList();
            out.add(new Violation(pageName, r.getId(), r.getImpact(), r.getHelp(), r.getHelpUrl(), wcag, nodes));
        }
        return out;
    }
}
