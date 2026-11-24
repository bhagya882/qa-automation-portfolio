package com.qaportfolio.crossbrowser;

import com.qaportfolio.core.driver.Browser;
import com.qaportfolio.core.driver.BrowserSpec;
import com.qaportfolio.saucedemo.pages.InventoryPage;
import com.qaportfolio.saucedemo.pages.SauceDemo;
import com.qaportfolio.saucedemo.model.User;
import com.qaportfolio.saucedemo.model.Users;
import com.qaportfolio.core.BaseUiTest;
import com.qaportfolio.core.driver.DriverManager;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import org.openqa.selenium.chromium.HasCdp;

import java.util.Map;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

/**
 * Same as BaseUiTest, but a browser that cannot be started on this machine (not installed, Safari remote automation
 * disabled, ...) marks the tests BLOCKED (TestNG skip with reason) instead of erroring the whole run.
 */
@Epic("Cross-browser & responsive")
public abstract class BaseCrossBrowserTest extends BaseUiTest {

    @Override
    @Parameters({"browser", "viewport", "device"})
    @BeforeMethod(alwaysRun = true)
    public void startBrowser(@Optional String browser, @Optional String viewport, @Optional String device) {
        BrowserSpec spec = BrowserSpec.of(browser, viewport, device);
        if (spec.mobileDevice() != null && spec.browser() != Browser.CHROME && spec.browser() != Browser.EDGE) {
            throw new SkipException("NOT APPLICABLE: device emulation is Chromium-only (" + spec.label() + ")");
        }
        try {
            super.startBrowser(browser, viewport, device);
        } catch (RuntimeException e) {
            throw new SkipException(Blocked.reason(spec, e));
        }
        emulateExactViewport(spec);
        Allure.parameter("actual viewport", Layout.viewport());
    }

    /**
     * Desktop Chrome/Edge refuse to shrink a window below ~500px wide, so a "390x844" window is really 500px.
     * For Chromium we pin the exact CSS viewport through CDP (Emulation.setDeviceMetricsOverride).
     * Firefox/Safari keep the OS window size; the actual viewport is recorded on every test for transparency.
     */
    private static void emulateExactViewport(BrowserSpec spec) {
        if (spec.viewport() == null || spec.mobileDevice() != null) {
            return;
        }
        if (DriverManager.get() instanceof HasCdp cdp) {
            int w = spec.viewport().width();
            int h = spec.viewport().height();
            cdp.executeCdpCommand("Emulation.setDeviceMetricsOverride",
                    Map.of("width", w, "height", h, "deviceScaleFactor", 1, "mobile", w < 768));
        }
    }

    protected InventoryPage loginAs(User user) {
        return SauceDemo.open().loginAs(user);
    }

    protected InventoryPage loginAsStandard() {
        return loginAs(Users.standard());
    }
}
