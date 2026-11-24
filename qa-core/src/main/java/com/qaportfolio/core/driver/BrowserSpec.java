package com.qaportfolio.core.driver;

import com.qaportfolio.core.config.Config;

/**
 * What to launch: browser + headless flag + optional viewport (e.g. 390x844) or Chrome device-emulation name
 * (e.g. "Pixel 7", "iPhone 14 Pro Max").
 */
public record BrowserSpec(Browser browser, boolean headless, Viewport viewport, String mobileDevice) {

    public static BrowserSpec fromConfig() {
        return of(Config.get("browser", "chrome"), Config.get("viewport"), Config.get("device"));
    }

    /** Null/blank arguments fall back to config values. */
    public static BrowserSpec of(String browser, String viewport, String device) {
        String b = blank(browser) ? Config.get("browser", "chrome") : browser;
        String v = blank(viewport) ? Config.get("viewport") : viewport;
        String d = blank(device) ? Config.get("device") : device;
        return new BrowserSpec(Browser.from(b), Config.getBool("headless", false),
                blank(v) ? null : Viewport.parse(v), blank(d) ? null : d);
    }

    public String label() {
        String size = mobileDevice != null ? mobileDevice : viewport != null ? viewport.toString() : "desktop";
        return browser.name().toLowerCase() + "@" + size;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    public record Viewport(int width, int height) {
        public static Viewport parse(String s) {
            String[] parts = s.toLowerCase().split("x");
            return new Viewport(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
        }

        @Override
        public String toString() {
            return width + "x" + height;
        }
    }
}
