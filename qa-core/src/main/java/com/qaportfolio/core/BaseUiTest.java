package com.qaportfolio.core;

import com.qaportfolio.core.driver.BrowserSpec;
import com.qaportfolio.core.driver.DriverManager;
import com.qaportfolio.core.listeners.FailureEvidenceListener;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

/**
 * Starts a fresh browser per test method and always quits it.
 * TestNG suite parameters {@code browser}, {@code viewport}, {@code device} override config, which lets one
 * suite XML fan out the same tests over a browser/viewport matrix.
 */
@Listeners(FailureEvidenceListener.class)
public abstract class BaseUiTest {

    @Parameters({"browser", "viewport", "device"})
    @BeforeMethod(alwaysRun = true)
    public void startBrowser(@Optional String browser, @Optional String viewport, @Optional String device) {
        BrowserSpec spec = BrowserSpec.of(browser, viewport, device);
        DriverManager.start(spec);
    }

    @AfterMethod(alwaysRun = true)
    public void stopBrowser() {
        // Evidence is captured by FailureEvidenceListener before this runs
        DriverManager.quit();
    }

    protected WebDriver driver() {
        return DriverManager.get();
    }
}
