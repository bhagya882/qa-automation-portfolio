package com.qaportfolio.core.listeners;

import com.qaportfolio.core.config.Config;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Retries a failed test up to {@code retry.count} times (default 0, CI uses 1).
 * Every retry is appended to {@code target/flaky-tests.txt} so the quality gate can report flaky tests
 * instead of silently hiding them. Tests in the {@code known-defect} group are never retried.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private int attempts;

    @Override
    public boolean retry(ITestResult result) {
        int max = Config.getInt("retry.count", 0);
        for (String g : result.getMethod().getGroups()) {
            if ("known-defect".equals(g)) {
                return false;
            }
        }
        if (attempts < max) {
            attempts++;
            record(result);
            return true;
        }
        return false;
    }

    private void record(ITestResult result) {
        String line = result.getTestClass().getName() + "#" + result.getMethod().getMethodName()
                + " retry=" + attempts + " cause=" + String.valueOf(result.getThrowable()).lines().findFirst().orElse("")
                + System.lineSeparator();
        try {
            Path file = Path.of("target", "flaky-tests.txt");
            Files.createDirectories(file.getParent());
            Files.writeString(file, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // best effort
        }
    }
}
