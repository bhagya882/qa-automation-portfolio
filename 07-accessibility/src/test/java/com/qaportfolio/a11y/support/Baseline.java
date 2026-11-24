package com.qaportfolio.a11y.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.qaportfolio.core.util.TestData;

import java.util.HashSet;
import java.util.Set;

/**
 * Known, already-reported violations ({@code src/test/resources/a11y-baseline.json}). Each entry links to a
 * defect report. New violations not listed here fail the regression test.
 */
public final class Baseline {

    private static final Set<String> KNOWN = load();

    private Baseline() {
    }

    private static Set<String> load() {
        Set<String> keys = new HashSet<>();
        for (JsonNode n : TestData.json("a11y-baseline.json").path("knownViolations")) {
            keys.add(key(n.path("page").asText(), n.path("rule").asText()));
        }
        return keys;
    }

    public static boolean isKnown(String page, String rule) {
        return KNOWN.contains(key(page, rule));
    }

    private static String key(String page, String rule) {
        return page + "|" + rule;
    }
}
