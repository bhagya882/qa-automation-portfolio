package com.qaportfolio.sampleapp;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Opaque random bearer tokens with a 30 minute idle timeout. */
final class Sessions {

    record User(String username, String role) {
    }

    private record Session(User user, Instant expires) {
    }

    private static final Duration TTL = Duration.ofMinutes(30);
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> byToken = new ConcurrentHashMap<>();

    String create(String username, String role) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        byToken.put(token, new Session(new User(username, role), Instant.now().plus(TTL)));
        return token;
    }

    User find(String token) {
        Session s = byToken.get(token);
        if (s == null) {
            return null;
        }
        if (Instant.now().isAfter(s.expires())) {
            byToken.remove(token);
            return null;
        }
        byToken.put(token, new Session(s.user(), Instant.now().plus(TTL)));
        return s.user();
    }

    void invalidate(String token) {
        byToken.remove(token);
    }
}
