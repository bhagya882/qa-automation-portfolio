package com.qaportfolio.perf;

import com.qaportfolio.core.config.Config;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Performance settings. Lookup: -D system property, then {@code perf/<profile>.properties}
 * (profile from {@code -Dperf.profile}, default {@code smoke}), then {@code config/default.properties} via
 * {@link Config}.
 */
public final class PerfSettings {

    private static final String PROFILE = System.getProperty("perf.profile", "smoke");
    private static final Properties PROFILE_PROPS = load("perf/" + PROFILE + ".properties");

    private PerfSettings() {
    }

    private static Properties load(String resource) {
        Properties p = new Properties();
        try (InputStream in = PerfSettings.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Unknown perf profile, missing " + resource);
            }
            p.load(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return p;
    }

    public static String profile() {
        return PROFILE;
    }

    public static String get(String key) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) {
            return sys;
        }
        String prof = PROFILE_PROPS.getProperty(key);
        return prof != null ? prof : Config.get(key);
    }

    public static String get(String key, String fallback) {
        String v = get(key);
        return v == null || v.isBlank() ? fallback : v.trim();
    }

    public static int i(String key, int fallback) {
        return Integer.parseInt(get(key, String.valueOf(fallback)));
    }

    public static double d(String key, double fallback) {
        return Double.parseDouble(get(key, String.valueOf(fallback)));
    }

    public static Duration seconds(String key, int fallback) {
        return Duration.ofSeconds(i(key, fallback));
    }

    public static Duration millis(String key, int fallback) {
        return Duration.ofMillis(i(key, fallback));
    }

    public static List<Integer> ints(String key, String fallback) {
        return Arrays.stream(get(key, fallback).split(",")).map(String::trim).map(Integer::parseInt).toList();
    }

    public static List<String> list(String key, String fallback) {
        return Arrays.stream(get(key, fallback).split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
