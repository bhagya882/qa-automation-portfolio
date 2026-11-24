package com.qaportfolio.mobile.support;

import com.qaportfolio.core.config.Config;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.MutableCapabilities;
import org.testng.SkipException;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds Android (UiAutomator2), iOS (XCUITest) or BrowserStack sessions from config. Missing infrastructure
 * (no Appium server, no app binary, no iOS config) results in a {@link SkipException} so the run reports the
 * tests as BLOCKED rather than failed.
 */
public final class MobileDriverFactory {

    private MobileDriverFactory() {
    }

    public static AppiumDriver create(boolean autoGrantPermissions) {
        String bsUser = System.getenv("BROWSERSTACK_USERNAME");
        String bsKey = System.getenv("BROWSERSTACK_ACCESS_KEY");
        if (bsUser != null && bsKey != null) {
            return browserStack(bsUser, bsKey, autoGrantPermissions);
        }
        String platform = Config.get("platform", "android");
        URL server = appiumUrl();
        requireReachable(server);
        return "ios".equalsIgnoreCase(platform) ? ios(server) : android(server, autoGrantPermissions);
    }

    public static UiAutomator2Options androidOptions(boolean autoGrantPermissions) {
        File apk = new File(Config.get("app.path", "apps/my-demo-app-android.apk"));
        if (!apk.isFile()) {
            throw new SkipException("BLOCKED: app binary not found at " + apk.getAbsolutePath()
                    + " – run 09-mobile/scripts/download-app.sh");
        }
        UiAutomator2Options o = new UiAutomator2Options()
                .setDeviceName(Config.get("device.name", "emulator-5554"))
                .setApp(apk.getAbsolutePath())
                .setAppPackage(Config.get("app.package"))
                .setAppActivity(Config.get("app.activity"))
                .setAppWaitActivity("*")
                .setAutoGrantPermissions(autoGrantPermissions)
                .setNoReset(Config.getBool("no.reset", false))
                .setFullReset(false)
                .setNewCommandTimeout(Duration.ofSeconds(120));
        String version = Config.get("platform.version");
        if (version != null && !version.isBlank()) {
            o.setPlatformVersion(version);
        }
        return o;
    }

    private static AppiumDriver android(URL server, boolean autoGrantPermissions) {
        return new AndroidDriver(server, androidOptions(autoGrantPermissions));
    }

    private static AppiumDriver ios(URL server) {
        File app = new File(Config.get("ios.app.path", "apps/MyDemoApp.app"));
        if (!app.exists()) {
            throw new SkipException("BLOCKED: iOS build not found at " + app.getAbsolutePath()
                    + " (iOS run needs macOS + Xcode simulator and the My Demo App iOS build)");
        }
        XCUITestOptions o = new XCUITestOptions()
                .setDeviceName(Config.get("ios.device.name", "iPhone 16"))
                .setApp(app.getAbsolutePath())
                .setBundleId(Config.get("ios.bundle.id"))
                .setAutoAcceptAlerts(false)
                .setNewCommandTimeout(Duration.ofSeconds(120));
        String version = Config.get("ios.platform.version");
        if (version != null && !version.isBlank()) {
            o.setPlatformVersion(version);
        }
        return new IOSDriver(server, o);
    }

    private static AppiumDriver browserStack(String user, String key, boolean autoGrantPermissions) {
        String app = System.getenv("BROWSERSTACK_APP");
        if (app == null || app.isBlank()) {
            throw new SkipException("BLOCKED: BROWSERSTACK_APP (bs://… id of the uploaded APK) is not set");
        }
        UiAutomator2Options o = new UiAutomator2Options()
                .setApp(app)
                .setAutoGrantPermissions(autoGrantPermissions);
        o.setCapability("appium:deviceName", Config.get("browserstack.device", "Google Pixel 8"));
        o.setCapability("appium:platformVersion", Config.get("browserstack.os.version", "14.0"));
        Map<String, Object> bs = new HashMap<>();
        bs.put("userName", user);       // read from env vars, never committed
        bs.put("accessKey", key);
        bs.put("projectName", "QA Portfolio – 09 Mobile");
        bs.put("buildName", System.getenv().getOrDefault("GITHUB_RUN_ID", "local"));
        bs.put("deviceLogs", true);
        bs.put("networkLogs", true);
        o.setCapability("bstack:options", bs);
        try {
            return new AndroidDriver(URI.create("https://hub.browserstack.com/wd/hub").toURL(), (MutableCapabilities) o);
        } catch (java.net.MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static URL appiumUrl() {
        try {
            return URI.create(Config.get("appium.url", "http://127.0.0.1:4723")).toURL();
        } catch (Exception e) {
            throw new IllegalArgumentException("Bad appium.url", e);
        }
    }

    private static void requireReachable(URL server) {
        try {
            HttpURLConnection c = (HttpURLConnection) URI.create(server.toString().replaceAll("/$", "") + "/status")
                    .toURL().openConnection();
            c.setConnectTimeout(2000);
            c.setReadTimeout(3000);
            if (c.getResponseCode() != 200) {
                throw new SkipException("BLOCKED: Appium server at " + server + " answered HTTP " + c.getResponseCode());
            }
        } catch (SkipException e) {
            throw e;
        } catch (Exception e) {
            throw new SkipException("BLOCKED: Appium server not reachable at " + server
                    + " – start it with `appium --port 4723` (" + e.getClass().getSimpleName() + ")");
        }
    }
}
