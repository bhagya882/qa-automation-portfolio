package com.qaportfolio.saucedemo.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.qaportfolio.core.util.TestData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Loads demo users from {@code testdata/users.json} once. */
public final class Users {

    private static final List<User> ALL = load();

    private Users() {
    }

    private static List<User> load() {
        JsonNode root = TestData.json("testdata/users.json");
        String password = root.get("password").asText();
        List<User> list = new ArrayList<>();
        for (JsonNode u : root.get("users")) {
            list.add(new User(u.get("key").asText(), u.get("username").asText(), password,
                    u.get("canLogin").asBoolean(), u.path("expectedError").asText(null), u.path("notes").asText("")));
        }
        return Collections.unmodifiableList(list);
    }

    public static List<User> all() {
        return ALL;
    }

    public static User byKey(String key) {
        return ALL.stream().filter(u -> u.key().equals(key)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No user " + key));
    }

    public static User standard() {
        return byKey("standard");
    }

    public static User lockedOut() {
        return byKey("locked_out");
    }

    public static User problem() {
        return byKey("problem");
    }

    public static User error() {
        return byKey("error");
    }

    public static User visual() {
        return byKey("visual");
    }

    public static User performanceGlitch() {
        return byKey("performance_glitch");
    }
}
