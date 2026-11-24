package com.qaportfolio.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

/**
 * Layered configuration. Lookup order (first match wins):
 * <ol>
 *   <li>JVM system property, e.g. {@code -Dbrowser=firefox}</li>
 *   <li>Environment variable, upper-cased with dots replaced by underscores, e.g. {@code BROWSER}</li>
 *   <li>{@code config/<env>.properties} on the test classpath, where env comes from {@code -Denv=} (default {@code qa})</li>
 *   <li>{@code config/default.properties} on the test classpath</li>
 * </ol>
 * Secrets are never stored in properties files; supply them via env vars or CI secrets.
 */
public final class Config {

    private static final Properties PROPS = load();

    private Config() {
    }

    private static Properties load() {
        Properties merged = new Properties();
        readInto(merged, "config/default.properties");
        String env = System.getProperty("env", System.getenv().getOrDefault("ENV", "qa"));
        readInto(merged, "config/" + env + ".properties");
        return merged;
    }

    private static void readInto(Properties target, String resource) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (in != null) {
                target.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + resource, e);
        }
    }

    public static String get(String key) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) {
            return sys;
        }
        String envVal = System.getenv(key.toUpperCase().replace('.', '_'));
        if (envVal != null && !envVal.isBlank()) {
            return envVal;
        }
        return PROPS.getProperty(key);
    }

    public static String get(String key, String fallback) {
        String v = get(key);
        return v == null ? fallback : v;
    }

    public static String require(String key) {
        String v = get(key);
        if (v == null) {
            throw new IllegalStateException("Missing required config key '" + key + "'");
        }
        return v;
    }

    public static int getInt(String key, int fallback) {
        String v = get(key);
        return v == null ? fallback : Integer.parseInt(v.trim());
    }

    public static boolean getBool(String key, boolean fallback) {
        String v = get(key);
        return v == null ? fallback : Boolean.parseBoolean(v.trim());
    }

    public static Duration getSeconds(String key, int fallbackSeconds) {
        return Duration.ofSeconds(getInt(key, fallbackSeconds));
    }
}
