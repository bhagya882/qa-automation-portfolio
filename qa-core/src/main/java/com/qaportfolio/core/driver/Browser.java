package com.qaportfolio.core.driver;

import java.util.Locale;

public enum Browser {
    CHROME, FIREFOX, EDGE, SAFARI;

    public static Browser from(String name) {
        if (name == null || name.isBlank()) {
            return CHROME;
        }
        return switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "chrome", "chromium" -> CHROME;
            case "firefox", "ff" -> FIREFOX;
            case "edge", "msedge" -> EDGE;
            case "safari", "webkit" -> SAFARI;
            default -> throw new IllegalArgumentException("Unsupported browser: " + name);
        };
    }
}
