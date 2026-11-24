package com.qaportfolio.gate;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Builds a throw-away repository layout from fixture files (fixtures are real TestNG / Newman outputs) and checks
 * aggregation and threshold evaluation.
 */
public class QualityGateTest {

    private Path repo;

    @BeforeMethod
    public void createRepo() throws IOException {
        repo = Files.createTempDirectory("gate-repo");
    }

    @AfterMethod(alwaysRun = true)
    public void deleteRepo() throws IOException {
        try (Stream<Path> s = Files.walk(repo)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    private void place(String fixture, String relativeTarget) throws IOException {
        Path dest = repo.resolve(relativeTarget);
        Files.createDirectories(dest.getParent());
        try (InputStream in = getClass().getResourceAsStream("/fixtures/" + fixture)) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Properties cfg(String... kv) {
        Properties p = new Properties();
        p.setProperty("gate.minPassRate", "100");
        p.setProperty("gate.maxFailuresPerModule", "0");
        p.setProperty("gate.maxSkippedPercent", "50");
        p.setProperty("gate.maxFlaky", "3");
        for (int i = 0; i < kv.length; i += 2) {
            p.setProperty(kv[i], kv[i + 1]);
        }
        return p;
    }

    @Test(description = "TestNG results: PASS/FAIL/SKIP counted, config methods ignored, retried attempts separated")
    public void parsesTestNgCounts() throws IOException {
        place("testng-mixed.xml", "04-e2e-framework/target/surefire-reports/testng-results.xml");
        ModuleResult m = QualityGate.scan(repo).get("04-e2e-framework");
        assertThat(m.passed()).isEqualTo(2);
        assertThat(m.failed()).isEqualTo(1);
        assertThat(m.skipped()).isEqualTo(1);
        assertThat(m.retried()).isEqualTo(1);
        assertThat(m.failures()).singleElement().asString()
                .startsWith("Sample#fails").contains("expected:<200> but was:<500> second line");
    }

    @Test(description = "Newman JUnit XML: assertions with <failure> are failed, others passed")
    public void parsesNewmanJUnit() throws IOException {
        place("newman-pass.xml", "03-api-testing/target/newman/data-driven-junit.xml");
        place("newman-failures.xml", "03-api-testing/target/newman/known-defects-junit.xml");
        ModuleResult m = QualityGate.scan(repo).get("03-api-testing");
        assertThat(m.failed()).isEqualTo(9);
        assertThat(m.passed()).isEqualTo(10 + 19);
        assertThat(m.sources()).hasSize(2);
    }

    @Test(description = "All green + required modules present -> gate passes")
    public void greenBuildPasses() throws IOException {
        place("testng-all-pass.xml", "04-e2e-framework/target/surefire-reports/testng-results.xml");
        place("newman-pass.xml", "03-api-testing/target/newman/regression.xml");
        QualityGate.Result r = QualityGate.evaluate(repo,
                cfg("gate.requiredModules", "03-api-testing,04-e2e-framework"));
        assertThat(r.breaches()).isEmpty();
        assertThat(r.passed()).isTrue();
        assertThat(r.overallPassRate()).isEqualTo(100.0);
        assertThat(QualityGate.toMarkdown(r)).contains("PASSED").contains("| 04-e2e-framework | 2 | 0 |");
    }

    @Test(description = "Failures above the per-module limit break the gate; per-module override relaxes it")
    public void failuresBreakGateUnlessOverridden() throws IOException {
        place("testng-mixed.xml", "02-cross-browser-ui/target/surefire-reports/testng-results.xml");
        QualityGate.Result strict = QualityGate.evaluate(repo, cfg());
        assertThat(strict.passed()).isFalse();
        assertThat(strict.breaches()).anyMatch(b -> b.contains("`02-cross-browser-ui` has 1 failed"))
                .anyMatch(b -> b.startsWith("Overall pass rate 66.7%"));

        QualityGate.Result relaxed = QualityGate.evaluate(repo,
                cfg("gate.maxFailures.02-cross-browser-ui", "1", "gate.minPassRate", "60"));
        assertThat(relaxed.breaches()).isEmpty();
    }

    @Test(description = "A required module without results fails the gate (no 'green because nothing ran')")
    public void missingRequiredModuleFails() throws IOException {
        place("testng-all-pass.xml", "04-e2e-framework/target/surefire-reports/testng-results.xml");
        QualityGate.Result r = QualityGate.evaluate(repo, cfg("gate.requiredModules", "04-e2e-framework,08-db-migration"));
        assertThat(r.breaches()).containsExactly("Required module `08-db-migration` reported no results");
    }

    @Test(description = "Too many skipped/blocked tests fail the gate")
    public void tooManySkippedFails() throws IOException {
        place("testng-mixed.xml", "09-mobile/target/surefire-reports/testng-results.xml");
        QualityGate.Result r = QualityGate.evaluate(repo,
                cfg("gate.maxSkippedPercent", "10", "gate.maxFailures.09-mobile", "5", "gate.minPassRate", "0"));
        assertThat(r.breaches()).singleElement().asString().contains("skipped/blocked 25.0%");
    }

    @Test(description = "flaky-tests.txt entries are listed and counted against gate.maxFlaky")
    public void flakyTestsAreReportedAndLimited() throws IOException {
        place("testng-all-pass.xml", "04-e2e-framework/target/surefire-reports/testng-results.xml");
        place("flaky-tests.txt", "04-e2e-framework/target/flaky-tests.txt");
        QualityGate.Result tolerated = QualityGate.evaluate(repo, cfg());
        assertThat(tolerated.flaky()).containsExactly(
                "04-e2e-framework: com.example.CheckoutTest#placeOrder",
                "04-e2e-framework: com.example.SearchTest#suggestions");
        assertThat(tolerated.passed()).isTrue();
        assertThat(QualityGate.toMarkdown(tolerated)).contains("Flaky tests");

        QualityGate.Result blocked = QualityGate.evaluate(repo, cfg("gate.maxFlaky", "1"));
        assertThat(blocked.breaches()).containsExactly("2 flaky test(s) (passed only after retry), allowed 1");
    }

    @Test(description = "CI artifact layout: same module from several jobs (browsers) is summed under one module")
    public void aggregatesSameModuleAcrossArtifacts() throws IOException {
        place("testng-all-pass.xml", "results/ui-04-e2e-framework-chrome/04-e2e-framework/target/surefire-reports/testng-results.xml");
        place("testng-mixed.xml", "results/ui-04-e2e-framework-firefox/04-e2e-framework/target/surefire-reports/testng-results.xml");
        place("flaky-tests.txt", "results/ui-04-e2e-framework-firefox/04-e2e-framework/target/flaky-tests.txt");
        QualityGate.Result r = QualityGate.evaluate(repo.resolve("results"), cfg("gate.requiredModules", "04-e2e-framework"));
        assertThat(r.modules()).containsOnlyKeys("04-e2e-framework");
        ModuleResult m = r.modules().get("04-e2e-framework");
        assertThat(m.passed()).isEqualTo(4);
        assertThat(m.failed()).isEqualTo(1);
        assertThat(m.sources()).hasSize(2);
        assertThat(r.flaky()).hasSize(2);
    }

    @Test(description = "Files under src/ and node_modules/ are never scanned")
    public void ignoresSourcesAndDependencies() throws IOException {
        place("testng-mixed.xml", "10-quality-gate/src/test/resources/x/target/surefire-reports/testng-results.xml");
        place("newman-failures.xml", "03-api-testing/node_modules/pkg/target/newman/x.xml");
        assertThat(QualityGate.scan(repo)).isEmpty();
    }

    @Test(description = "JSON summary contains counts and breaches, with quotes escaped")
    public void jsonSummary() throws IOException {
        place("testng-mixed.xml", "04-e2e-framework/target/surefire-reports/testng-results.xml");
        String json = QualityGate.toJson(QualityGate.evaluate(repo, cfg()));
        assertThat(json).contains("\"passed\": false").contains("\"module\": \"04-e2e-framework\"")
                .contains("\"failed\": 1").contains("\"breaches\": [\"`04-e2e-framework` has 1 failed");
        assertThat(QualityGate.toJson(new QualityGate.Result(new java.util.TreeMap<>(),
                java.util.List.of("say \"hi\"\n"), java.util.Set.of(), cfg())))
                .contains("\"say \\\"hi\\\"\\n\"");
    }

    @Test(description = "JWTs in failure messages are redacted before reaching CI summaries")
    public void redactsTokens() {
        String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6MSwidXNlcm5hbWUiOiJ4In0.abcdefghijklmnopqrstuvwxyz012345";
        assertThat(ResultParser.redact("Set-Cookie: accessToken=" + jwt + "; HttpOnly"))
                .isEqualTo("Set-Cookie: accessToken=<redacted-jwt>; HttpOnly");
    }

    @Test(description = "XML with a DOCTYPE is rejected (XXE protection)")
    public void rejectsDoctype() throws IOException {
        Path f = repo.resolve("04-e2e-framework/target/surefire-reports/testng-results.xml");
        Files.createDirectories(f.getParent());
        Files.writeString(f, "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]>"
                + "<testng-results><test-method status=\"PASS\" name=\"&e;\"/></testng-results>");
        assertThatThrownBy(() -> QualityGate.scan(repo)).hasMessageContaining("Cannot parse");
    }
}
