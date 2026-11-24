package com.qaportfolio.mobile.support;

import com.qaportfolio.core.config.Config;
import io.appium.java_client.appmanagement.ApplicationState;

import java.util.List;
import java.util.Map;

/** Device-level operations via UiAutomator2 `mobile:` commands (shell needs Appium --relaxed-security). */
public final class Device {

    private Device() {
    }

    public static String pkg() {
        return Config.get("app.package", "com.saucelabs.mydemoapp.android");
    }

    public static Object shell(String command, String... args) {
        return Mobile.driver().executeScript("mobile: shell", Map.of("command", command, "args", List.of(args)));
    }

    public static void setNetwork(boolean enabled) {
        String state = enabled ? "enable" : "disable";
        shell("svc", "wifi", state);
        shell("svc", "data", state);
    }

    /** True when Android reports an active default network (validated by connectivity service). */
    public static boolean hasActiveNetwork() {
        String out = String.valueOf(shell("dumpsys", "connectivity"));
        return out.contains("Active default network:") && !out.contains("Active default network: none");
    }

    /** Polls the connectivity service until the network state matches, or the timeout passes. */
    public static boolean waitForNetwork(boolean expected, long timeoutMillis) {
        long end = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < end) {
            if (hasActiveNetwork() == expected) {
                return true;
            }
            pause(1000);
        }
        return hasActiveNetwork() == expected;
    }

    public static ApplicationState appState() {
        return Mobile.android().queryAppState(pkg());
    }

    public static double density() {
        Object d = Mobile.driver().executeScript("mobile: getDisplayDensity");
        return ((Number) d).doubleValue() / 160.0;
    }

    public static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
