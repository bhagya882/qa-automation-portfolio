package com.qaportfolio.a11y.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Writes human-readable Markdown + HTML summaries of all scanned pages to target/a11y/. */
public final class A11yReport {

    private A11yReport() {
    }

    public static void write(Map<SauceDemo.Page, List<AxeScanner.Violation>> byPage) {
        if (byPage.isEmpty()) {
            return;
        }
        StringBuilder md = new StringBuilder("# Accessibility scan report (axe-core)\n\n");
        md.append("Generated: ").append(ZonedDateTime.now()).append("  \n");
        md.append("Target: ").append(SauceDemo.baseUrl()).append("\n\n");
        md.append("| Page | Rule | Impact | WCAG | Nodes | In baseline |\n|---|---|---|---|---|---|\n");
        StringBuilder detail = new StringBuilder("\n## Details\n");
        StringBuilder html = new StringBuilder("""
                <!doctype html><html lang="en"><head><meta charset="utf-8"><title>Accessibility scan report</title>
                <style>body{font-family:system-ui,sans-serif;margin:2rem;color:#111}table{border-collapse:collapse}
                td,th{border:1px solid #888;padding:.35rem .6rem;text-align:left;vertical-align:top}
                .critical{color:#a00;font-weight:700}.serious{color:#b45309;font-weight:700}code{font-size:.85em}</style>
                </head><body><main><h1>Accessibility scan report (axe-core)</h1>
                <table><thead><tr><th scope="col">Page</th><th scope="col">Rule</th><th scope="col">Impact</th>
                <th scope="col">WCAG</th><th scope="col">Nodes (targets)</th><th scope="col">Baseline</th></tr></thead><tbody>
                """);
        int total = 0;
        for (var e : new TreeMap<>(byPage).entrySet()) {
            if (e.getValue().isEmpty()) {
                md.append("| ").append(e.getKey()).append(" | _none_ | | | 0 | |\n");
                html.append("<tr><td>").append(e.getKey()).append("</td><td colspan=\"5\">No violations</td></tr>");
            }
            for (AxeScanner.Violation v : e.getValue()) {
                total++;
                boolean known = Baseline.isKnown(v.page(), v.rule());
                md.append("| ").append(v.page()).append(" | [").append(v.rule()).append("](").append(v.helpUrl())
                        .append(") | ").append(v.impact()).append(" | ").append(String.join(" ", v.wcagTags()))
                        .append(" | ").append(v.nodes().size()).append(" | ").append(known ? "yes" : "**NEW**")
                        .append(" |\n");
                detail.append("\n### ").append(v.page()).append(" – ").append(v.rule()).append(" (")
                        .append(v.impact()).append(")\n").append(v.help()).append("\n\n");
                StringBuilder targets = new StringBuilder();
                for (AxeScanner.NodeInfo n : v.nodes()) {
                    detail.append("- `").append(n.target()).append("` — `")
                            .append(n.html().replace("`", "'")).append("`\n");
                    targets.append("<code>").append(esc(n.target())).append("</code><br>");
                }
                html.append("<tr><td>").append(v.page()).append("</td><td><a href=\"").append(v.helpUrl())
                        .append("\">").append(v.rule()).append("</a></td><td class=\"").append(v.impact()).append("\">")
                        .append(v.impact()).append("</td><td>").append(String.join(" ", v.wcagTags()))
                        .append("</td><td>").append(targets).append("</td><td>").append(known ? "known" : "NEW")
                        .append("</td></tr>");
            }
        }
        md.append("\nTotal violation types: ").append(total).append("\n\n## Needs manual review (axe 'incomplete')\n\n");
        html.append("</tbody></table><h2>Needs manual review (axe incomplete)</h2><ul>");
        for (var e : new TreeMap<>(AxeScanner.needsReview()).entrySet()) {
            if (!e.getValue().isEmpty()) {
                md.append("- ").append(e.getKey()).append(": ").append(String.join(", ", e.getValue())).append('\n');
                html.append("<li>").append(e.getKey()).append(": ").append(esc(String.join(", ", e.getValue())))
                        .append("</li>");
            }
        }
        html.append("</ul>");
        md.append(detail);
        html.append("<p>Total violation types: ").append(total).append("</p></main></body></html>");
        try {
            Files.createDirectories(AxeScanner.OUT);
            Files.writeString(AxeScanner.OUT.resolve("a11y-report.md"), md);
            Files.writeString(AxeScanner.OUT.resolve("a11y-report.html"), html);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
