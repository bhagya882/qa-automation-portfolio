package com.qaportfolio.mobile.support;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;

/** Thread-local Appium session holder (one device session per test thread). */
public final class Mobile {

    private static final ThreadLocal<AppiumDriver> DRIVER = new ThreadLocal<>();

    private Mobile() {
    }

    public static void set(AppiumDriver d) {
        DRIVER.set(d);
    }

    public static AppiumDriver driver() {
        AppiumDriver d = DRIVER.get();
        if (d == null) {
            throw new IllegalStateException("No Appium session on this thread");
        }
        return d;
    }

    public static AndroidDriver android() {
        return (AndroidDriver) driver();
    }

    public static boolean active() {
        return DRIVER.get() != null;
    }

    public static void quit() {
        AppiumDriver d = DRIVER.get();
        if (d != null) {
            try {
                d.quit();
            } catch (RuntimeException ignored) {
                // session may already be gone
            } finally {
                DRIVER.remove();
            }
        }
    }
}
