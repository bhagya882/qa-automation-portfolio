package com.qaportfolio.a11y.tests;

import com.qaportfolio.a11y.support.A11yReport;
import com.qaportfolio.a11y.support.AxeScanner;
import com.qaportfolio.a11y.support.AxeScanner.Violation;
import com.qaportfolio.a11y.support.Baseline;
import com.qaportfolio.a11y.support.SauceDemo.Page;
import com.qaportfolio.core.BaseUiTest;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Automated WCAG 2.x A/AA scan of every page in the purchase journey.
 * <ul>
 *   <li>{@code noNewViolations}: regression gate — fails only on violations not in the baseline.</li>
 *   <li>{@code zeroViolations} (group known-defect): the "true" target state; fails today and documents defects.</li>
 * </ul>
 */
@Feature("Accessibility – axe-core WCAG scan")
public class AxeWorkflowScanTest extends BaseUiTest {

    @DataProvider
    public Object[][] pages() {
        return Arrays.stream(Page.values()).map(p -> new Object[]{p}).toArray(Object[][]::new);
    }

    @Test(dataProvider = "pages", description = "No new axe violations compared with the accepted baseline")
    @Description("A11Y-AUTO-01: axe-core WCAG 2.0/2.1/2.2 A+AA scan, compared with a11y-baseline.json")
    public void noNewViolations(Page page) {
        List<Violation> found = AxeScanner.scanCached(page);
        List<Violation> fresh = found.stream().filter(v -> !Baseline.isKnown(v.page(), v.rule())).toList();
        assertThat(fresh)
                .as("New accessibility violations on %s (not in baseline): %s", page, describe(fresh))
                .isEmpty();
    }

    @Test(dataProvider = "pages", groups = "known-defect",
            description = "Target state: page has zero axe violations (fails while baseline defects are open)")
    @Description("A11Y-AUTO-02: documents open accessibility defects; excluded from the PR gate")
    public void zeroViolations(Page page) {
        List<Violation> found = AxeScanner.scanCached(page);
        assertThat(found).as("axe violations on %s: %s", page, describe(found)).isEmpty();
    }

    @AfterClass(alwaysRun = true)
    public void writeReport() {
        A11yReport.write(AxeScanner.all());
    }

    private static String describe(List<Violation> vs) {
        return vs.stream()
                .map(v -> v.rule() + "[" + v.impact() + "] x" + v.nodes().size() + " e.g. " + v.nodes().get(0).target())
                .collect(Collectors.joining("; "));
    }
}
