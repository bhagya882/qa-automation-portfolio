package com.qaportfolio.core.driver;

import com.qaportfolio.core.config.Config;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates WebDriver instances. Browser and viewport can be set per test (TestNG parameters)
 * or globally ({@code -Dbrowser=firefox -Dheadless=true -Dviewport=390x844}).
 * <p>
 * Set {@code -Dgrid.url=...} to run on Selenium Grid or BrowserStack
 * (e.g. {@code https://USER:KEY@hub.browserstack.com/wd/hub}; pass credentials via env vars, never commit them).
 */
public final class DriverFactory {

    private DriverFactory() {
    }

    public static WebDriver create(BrowserSpec spec) {
        MutableCapabilities options = optionsFor(spec);
        String gridUrl = Config.get("grid.url");
        WebDriver driver;
        if (gridUrl != null && !gridUrl.isBlank()) {
            try {
                driver = new RemoteWebDriver(URI.create(gridUrl).toURL(), options);
            } catch (Exception e) {
                throw new IllegalStateException("Cannot connect to grid " + redact(gridUrl), e);
            }
        } else {
            driver = switch (spec.browser()) {
                case CHROME -> new ChromeDriver((ChromeOptions) options);
                case FIREFOX -> new FirefoxDriver((FirefoxOptions) options);
                case EDGE -> new EdgeDriver((EdgeOptions) options);
                case SAFARI -> new SafariDriver((SafariOptions) options);
            };
        }
        driver.manage().timeouts().pageLoadTimeout(Config.getSeconds("timeout.pageLoad", 30));
        if (spec.viewport() != null && spec.mobileDevice() == null) {
            driver.manage().window().setSize(new Dimension(spec.viewport().width(), spec.viewport().height()));
        } else if (spec.mobileDevice() == null && spec.browser() != Browser.SAFARI) {
            driver.manage().window().setSize(new Dimension(1440, 900));
        }
        return driver;
    }

    private static MutableCapabilities optionsFor(BrowserSpec spec) {
        boolean headless = spec.headless();
        return switch (spec.browser()) {
            case CHROME -> {
                ChromeOptions o = new ChromeOptions();
                if (headless) {
                    o.addArguments("--headless=new");
                }
                o.addArguments("--disable-search-engine-choice-screen", "--no-first-run",
                        "--disable-features=PasswordLeakDetection", "--password-store=basic");
                // Disable Chrome's password manager pop-ups that block clicks on demo sites
                Map<String, Object> prefs = new HashMap<>();
                prefs.put("credentials_enable_service", false);
                prefs.put("profile.password_manager_enabled", false);
                prefs.put("profile.password_manager_leak_detection", false);
                o.setExperimentalOption("prefs", prefs);
                if (spec.mobileDevice() != null) {
                    o.setExperimentalOption("mobileEmulation", Map.of("deviceName", spec.mobileDevice()));
                }
                if (System.getenv("CI") != null) {
                    o.addArguments("--no-sandbox", "--disable-dev-shm-usage");
                }
                yield o;
            }
            case FIREFOX -> {
                FirefoxOptions o = new FirefoxOptions();
                if (headless) {
                    o.addArguments("-headless");
                }
                yield o;
            }
            case EDGE -> {
                EdgeOptions o = new EdgeOptions();
                if (headless) {
                    o.addArguments("--headless=new");
                }
                yield o;
            }
            case SAFARI -> new SafariOptions(); // Safari has no headless mode; run `safaridriver --enable` once
        };
    }

    private static String redact(String url) {
        return url.replaceAll("//[^@/]+@", "//***@");
    }
}
