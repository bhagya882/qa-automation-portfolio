package com.qaportfolio.security;

import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Writes {@code target/security/<mode>-results.md}: one row per test with OWASP category, result and the first
 * line of the failure. Against the insecure lab, every FAIL row is a detected finding (evidence).
 */
public class FindingsReportListener implements ISuiteListener {

    @Override
    public void onFinish(ISuite suite) {
        List<ITestResult> all = new ArrayList<>();
        suite.getResults().values().forEach(r -> {
            all.addAll(r.getTestContext().getPassedTests().getAllResults());
            all.addAll(r.getTestContext().getFailedTests().getAllResults());
            all.addAll(r.getTestContext().getSkippedTests().getAllResults());
        });
        all.sort(Comparator.comparing((ITestResult r) -> String.valueOf(r.getMethod().getDescription()))
                .thenComparing(r -> java.util.Arrays.toString(r.getParameters())));

        String mode = SecurityBase.mode == null ? "secure" : SecurityBase.mode;
        long passed = all.stream().filter(r -> r.getStatus() == ITestResult.SUCCESS).count();
        long failed = all.stream().filter(r -> r.getStatus() == ITestResult.FAILURE).count();
        long skipped = all.stream().filter(r -> r.getStatus() == ITestResult.SKIP).count();

        StringBuilder sb = new StringBuilder();
        sb.append("# Security test results — target mode: ").append(mode).append("\n\n");
        sb.append("Generated ").append(ZonedDateTime.now()).append(" by FindingsReportListener (suite: ")
                .append(suite.getName()).append(")\n\n");
        sb.append(String.format("Total %d | Passed %d | Failed %d | Skipped %d%n%n", all.size(), passed, failed, skipped));
        sb.append("| Test case | Data | Result | Failure detail (first lines) |\n|---|---|---|---|\n");
        for (ITestResult r : all) {
            String status = switch (r.getStatus()) {
                case ITestResult.SUCCESS -> "PASS";
                case ITestResult.FAILURE -> "**FAIL**";
                default -> "SKIP";
            };
            String detail = r.getThrowable() == null ? "" : firstLine(r.getThrowable().getMessage());
            sb.append("| ").append(r.getMethod().getDescription()).append(" | ")
                    .append(r.getParameters().length == 0 ? "" : esc(java.util.Arrays.toString(r.getParameters())))
                    .append(" | ").append(status).append(" | ").append(esc(detail)).append(" |\n");
        }
        Path out = Path.of("target", "security", mode + "-results.md");
        try {
            Files.createDirectories(out.getParent());
            Files.writeString(out, sb.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        System.out.println("[security] results written to " + out.toAbsolutePath());
    }

    private static String firstLine(String s) {
        if (s == null) {
            return "";
        }
        // First two non-blank lines: "[description]" + "Expecting/Expected ... but was ..."
        String line = String.join(" ", s.strip().lines().map(String::strip).filter(l -> !l.isBlank()).limit(2).toList());
        return line.length() > 220 ? line.substring(0, 220) + "…" : line;
    }

    private static String esc(String s) {
        return s.replace("|", "\\|").replace("<", "&lt;").replace(">", "&gt;");
    }
}
