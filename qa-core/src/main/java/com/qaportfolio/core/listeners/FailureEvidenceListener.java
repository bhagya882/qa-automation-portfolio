package com.qaportfolio.core.listeners;

import com.qaportfolio.core.driver.DriverManager;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * After every test method (before TestNG notifies result listeners, so the Allure test case is still open):
 * records the browser label as an Allure parameter and, on failure, saves a screenshot, the page source and
 * (Chromium only) the browser console log to {@code target/evidence/} and attaches them to Allure.
 * No-op for tests without a browser.
 */
public class FailureEvidenceListener implements IInvokedMethodListener {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        if (!method.isTestMethod() || !DriverManager.isStarted()) {
            return;
        }
        if (DriverManager.spec() != null) {
            Allure.parameter("browser", DriverManager.spec().label());
        }
        if (!result.isSuccess()) {
            capture(result, "FAILED");
        }
    }

    public static void capture(ITestResult result, String prefix) {
        if (!DriverManager.isStarted()) {
            return;
        }
        WebDriver driver = DriverManager.get();
        String spec = DriverManager.spec() == null ? "browser" : DriverManager.spec().label();
        String name = String.format("%s_%s_%s_%s", prefix, result.getTestClass().getRealClass().getSimpleName(),
                        result.getMethod().getMethodName(), spec)
                .replaceAll("[^A-Za-z0-9_.@-]", "_") + "_" + LocalDateTime.now().format(TS);
        Path dir = Path.of("target", "evidence");
        try {
            Files.createDirectories(dir);
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Files.write(dir.resolve(name + ".png"), png);
            Allure.addAttachment("Screenshot on failure", "image/png", new ByteArrayInputStream(png), "png");

            String html = driver.getPageSource();
            Files.writeString(dir.resolve(name + ".html"), html);
            Allure.addAttachment("Page source", "text/html", html, "html");

            Allure.addAttachment("URL", "text/plain", driver.getCurrentUrl());

            String console = browserConsole(driver);
            if (!console.isEmpty()) {
                Files.writeString(dir.resolve(name + ".console.log"), console, StandardCharsets.UTF_8);
                Allure.addAttachment("Browser console", "text/plain", console);
            }
        } catch (IOException | RuntimeException e) {
            // Evidence capture must never mask the real failure
            System.err.println("[evidence] could not capture for " + name + ": " + e.getMessage());
        }
    }

    private static String browserConsole(WebDriver driver) {
        try {
            return driver.manage().logs().get(LogType.BROWSER).getAll().stream()
                    .map(LogEntry::toString)
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (RuntimeException unsupported) {
            return ""; // Firefox/Safari don't expose the legacy logging API
        }
    }
}
