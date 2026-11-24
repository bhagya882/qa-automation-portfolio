package com.qaportfolio.db.support;

import org.testng.ISuite;
import org.testng.ISuiteListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Collects every validation check (expected vs actual) and writes {@code target/db-validation-report.md} when the
 * suite finishes. Register in testng.xml as a listener.
 */
public class DbReport implements ISuiteListener {

    public record Check(String category, String name, String expected, String actual, boolean pass) {
    }

    private static final List<Check> CHECKS = Collections.synchronizedList(new ArrayList<>());

    /** Records a check and returns whether it passed; callers assert on the result. */
    public static boolean record(String category, String name, Object expected, Object actual) {
        boolean pass = Objects.equals(String.valueOf(expected), String.valueOf(actual));
        CHECKS.add(new Check(category, name, String.valueOf(expected), String.valueOf(actual), pass));
        return pass;
    }

    public static void info(String category, String name, Object value) {
        CHECKS.add(new Check(category, name, "(info)", String.valueOf(value), true));
    }

    @Override
    public void onFinish(ISuite suite) {
        StringBuilder md = new StringBuilder("# Database migration validation report\n\n");
        md.append("- Generated: ").append(ZonedDateTime.now()).append('\n');
        md.append("- Database: ").append(Db.url().replaceAll("password=[^;&]*", "password=***")).append('\n');
        long failed = CHECKS.stream().filter(c -> !c.pass()).count();
        md.append("- Checks: ").append(CHECKS.size()).append(", passed: ").append(CHECKS.size() - failed)
                .append(", failed: ").append(failed).append("\n\n");
        md.append("| # | Category | Check | Expected | Actual | Result |\n|---|---|---|---|---|---|\n");
        int i = 1;
        synchronized (CHECKS) {
            for (Check c : CHECKS) {
                md.append("| ").append(i++).append(" | ").append(c.category()).append(" | ").append(c.name())
                        .append(" | ").append(esc(c.expected())).append(" | ").append(esc(c.actual())).append(" | ")
                        .append(c.pass() ? "PASS" : "**FAIL**").append(" |\n");
            }
        }
        try {
            Path out = Path.of("target", "db-validation-report.md");
            Files.createDirectories(out.getParent());
            Files.writeString(out, md);
            System.out.println("[db-report] written " + out.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String esc(String s) {
        return s.replace("|", "\\|").replace("\n", " ");
    }
}
