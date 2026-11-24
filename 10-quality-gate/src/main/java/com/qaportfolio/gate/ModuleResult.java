package com.qaportfolio.gate;

import java.util.ArrayList;
import java.util.List;

/** Aggregated counts for one module (top-level folder of the repository). */
public final class ModuleResult {

    private final String module;
    int passed;
    int failed;
    int skipped;
    int retried;
    final List<String> failures = new ArrayList<>();
    final List<String> sources = new ArrayList<>();

    public ModuleResult(String module) {
        this.module = module;
    }

    public String module() {
        return module;
    }

    public int passed() {
        return passed;
    }

    public int failed() {
        return failed;
    }

    /** Skipped = blocked (SkipException for missing infrastructure, dependency failures). */
    public int skipped() {
        return skipped;
    }

    /** Attempts that TestNG retried (RetryAnalyzer). Not counted as skipped. */
    public int retried() {
        return retried;
    }

    public int executed() {
        return passed + failed;
    }

    public int total() {
        return passed + failed + skipped;
    }

    public List<String> failures() {
        return failures;
    }

    public List<String> sources() {
        return sources;
    }

    public double passRate() {
        return executed() == 0 ? 0 : 100.0 * passed / executed();
    }

    public double skippedPercent() {
        return total() == 0 ? 0 : 100.0 * skipped / total();
    }
}
