package com.qaportfolio.sampleapp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Blocks a client+username key after N failed logins within a window. */
final class LoginThrottle {

    private record Window(int failures, long startedAt) {
    }

    private final int maxFailures;
    private final long windowMillis;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    LoginThrottle(int maxFailures, long windowMillis) {
        this.maxFailures = maxFailures;
        this.windowMillis = windowMillis;
    }

    boolean isBlocked(String key) {
        Window w = windows.get(key);
        if (w == null) {
            return false;
        }
        if (System.currentTimeMillis() - w.startedAt() > windowMillis) {
            windows.remove(key);
            return false;
        }
        return w.failures() >= maxFailures;
    }

    void recordFailure(String key) {
        long now = System.currentTimeMillis();
        windows.merge(key, new Window(1, now), (old, n) ->
                now - old.startedAt() > windowMillis ? n : new Window(old.failures() + 1, old.startedAt()));
    }

    void reset(String key) {
        windows.remove(key);
    }
}
