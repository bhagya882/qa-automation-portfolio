package com.qaportfolio.mobile.support;

import io.appium.java_client.AppiumDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

/** Fresh app session per test (app reinstalled/reset by UiAutomator2 because noReset=false). */
@Listeners(MobileEvidenceListener.class)
public abstract class BaseMobileTest {

    /** Override to false in tests that exercise runtime-permission prompts. */
    protected boolean autoGrantPermissions() {
        return true;
    }

    @BeforeMethod(alwaysRun = true)
    public void startSession() {
        Mobile.set(MobileDriverFactory.create(autoGrantPermissions()));
    }

    @AfterMethod(alwaysRun = true)
    public void endSession() {
        Mobile.quit();
    }

    protected AppiumDriver driver() {
        return Mobile.driver();
    }
}
