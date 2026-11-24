package com.qaportfolio.mobile.support;

import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * After a failed test (and before the session is quit): screenshot, page source (XML) and logcat are saved to
 * {@code target/evidence/} and attached to Allure.
 */
public class MobileEvidenceListener implements IInvokedMethodListener {

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        if (method.isTestMethod() && !result.isSuccess() && result.getStatus() != ITestResult.SKIP && Mobile.active()) {
            capture(result.getTestClass().getRealClass().getSimpleName() + "_" + result.getMethod().getMethodName());
        }
    }

    public static void capture(String name) {
        Path dir = Path.of("target", "evidence");
        try {
            Files.createDirectories(dir);
            byte[] png = Mobile.driver().getScreenshotAs(OutputType.BYTES);
            Files.write(dir.resolve(name + ".png"), png);
            Allure.addAttachment("Device screenshot", "image/png", new ByteArrayInputStream(png), "png");

            String source = Mobile.driver().getPageSource();
            Files.writeString(dir.resolve(name + ".xml"), source);
            Allure.addAttachment("Page source", "text/xml", source, "xml");

            String logcat = Mobile.driver().manage().logs().get("logcat").getAll().stream()
                    .map(Object::toString).collect(Collectors.joining(System.lineSeparator()));
            Files.writeString(dir.resolve(name + ".logcat.txt"), logcat);
            Allure.addAttachment("logcat", "text/plain", logcat);
        } catch (Exception e) {
            System.err.println("[mobile-evidence] capture failed for " + name + ": " + e.getMessage());
        }
    }
}
