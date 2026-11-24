package com.qaportfolio.core.driver;

import org.openqa.selenium.WebDriver;

/** Thread-local holder so TestNG parallel execution gets one browser per thread. */
public final class DriverManager {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();
    private static final ThreadLocal<BrowserSpec> SPEC = new ThreadLocal<>();

    private DriverManager() {
    }

    public static WebDriver start(BrowserSpec spec) {
        quit();
        WebDriver driver = DriverFactory.create(spec);
        DRIVER.set(driver);
        SPEC.set(spec);
        return driver;
    }

    public static WebDriver get() {
        WebDriver d = DRIVER.get();
        if (d == null) {
            throw new IllegalStateException("No WebDriver started on this thread");
        }
        return d;
    }

    public static boolean isStarted() {
        return DRIVER.get() != null;
    }

    public static BrowserSpec spec() {
        return SPEC.get();
    }

    public static void quit() {
        WebDriver d = DRIVER.get();
        if (d != null) {
            try {
                d.quit();
            } finally {
                DRIVER.remove();
                SPEC.remove();
            }
        }
    }
}
