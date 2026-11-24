package com.qaportfolio.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Test data helpers: static JSON fixtures from {@code src/test/resources/testdata/} plus synthetic data
 * (Datafaker) so no real personal data is ever used.
 */
public final class TestData {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Faker FAKER = new Faker(Locale.ENGLISH);

    private TestData() {
    }

    public static JsonNode json(String resourcePath) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("Test data not found on classpath: " + resourcePath);
            }
            return MAPPER.readTree(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> T json(String resourcePath, Class<T> type) {
        return MAPPER.convertValue(json(resourcePath), type);
    }

    public static Faker faker() {
        return FAKER;
    }

    public static String firstName() {
        return FAKER.name().firstName();
    }

    public static String lastName() {
        return FAKER.name().lastName();
    }

    public static String postalCode() {
        return FAKER.address().zipCode();
    }

    public static String uniqueEmail() {
        return "qa+" + System.nanoTime() + "@example.test";
    }
}
