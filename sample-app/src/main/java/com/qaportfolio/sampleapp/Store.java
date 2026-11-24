package com.qaportfolio.sampleapp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** In-memory data store. Synthetic data only. */
final class Store {

    record Account(String password, String role) {
    }

    record Product(int id, String name, String category, BigDecimal price, int stock) {
    }

    record CartLine(int productId, String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    record Order(int id, String username, List<CartLine> lines, BigDecimal total, String createdAt) {
    }

    /**
     * Demo-only accounts for a local lab app. Passwords can be overridden with env vars; they are not real
     * credentials and protect nothing outside this process.
     */
    static final Map<String, Account> USERS = Map.of(
            "customer1", new Account(env("DEMO_CUSTOMER_PASSWORD", "demo-pass-1"), "USER"),
            "customer2", new Account(env("DEMO_CUSTOMER_PASSWORD", "demo-pass-1"), "USER"),
            "admin", new Account(env("DEMO_ADMIN_PASSWORD", "demo-admin-pass"), "ADMIN"));

    private static final String[] CATEGORIES = {"backpacks", "apparel", "electronics", "home", "toys"};
    private static final String[] ADJ = {"Classic", "Pro", "Lite", "Ultra", "Eco", "Smart", "Travel", "Mini"};
    private static final String[] NOUN = {"Backpack", "T-Shirt", "Headphones", "Lamp", "Robot", "Jacket", "Bottle",
            "Speaker", "Mug", "Watch"};

    private final Map<Integer, Product> products = new LinkedHashMap<>();
    private final Map<String, Map<Integer, Integer>> carts = new ConcurrentHashMap<>();
    private final Map<Integer, Order> orders = new ConcurrentHashMap<>();
    private final AtomicInteger orderSeq = new AtomicInteger(1000);

    Store() {
        for (int i = 1; i <= 60; i++) {
            String name = ADJ[i % ADJ.length] + " " + NOUN[i % NOUN.length] + " " + i;
            BigDecimal price = BigDecimal.valueOf(5 + (i * 37 % 195) + 0.99).setScale(2, RoundingMode.HALF_UP);
            products.put(i, new Product(i, name, CATEGORIES[i % CATEGORIES.length], price, 10 + i % 25));
        }
    }

    List<Product> search(String q) {
        if (q == null || q.isBlank()) {
            return new ArrayList<>(products.values());
        }
        String needle = q.toLowerCase(Locale.ROOT);
        return products.values().stream()
                .filter(p -> p.name().toLowerCase(Locale.ROOT).contains(needle) || p.category().contains(needle))
                .toList();
    }

    Product product(int id) {
        return products.get(id);
    }

    List<CartLine> cart(String user) {
        Map<Integer, Integer> c = carts.getOrDefault(user, Map.of());
        List<CartLine> lines = new ArrayList<>();
        c.forEach((pid, qty) -> {
            Product p = products.get(pid);
            lines.add(new CartLine(pid, p.name(), qty, p.price(), p.price().multiply(BigDecimal.valueOf(qty))));
        });
        return lines;
    }

    List<CartLine> addToCart(String user, int productId, int qty) {
        carts.computeIfAbsent(user, k -> new ConcurrentHashMap<>()).merge(productId, qty, Integer::sum);
        return cart(user);
    }

    void clearCart(String user) {
        carts.remove(user);
    }

    Order checkout(String user) {
        List<CartLine> lines = cart(user);
        if (lines.isEmpty()) {
            return null;
        }
        BigDecimal total = lines.stream().map(CartLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        Order o = new Order(orderSeq.incrementAndGet(), user, List.copyOf(lines), total, Instant.now().toString());
        orders.put(o.id(), o);
        carts.remove(user);
        return o;
    }

    Order order(int id) {
        return orders.get(id);
    }

    List<Order> ordersFor(String user) {
        return orders.values().stream().filter(o -> o.username().equals(user)).toList();
    }

    /**
     * Deliberate bottleneck for the performance project: a global lock plus simulated slow aggregation query.
     * Under concurrency requests queue behind each other, so p95 grows linearly with users.
     */
    synchronized Map<String, Object> salesReport() {
        try {
            Thread.sleep(40);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        for (Order o : orders.values()) {
            for (CartLine l : o.lines()) {
                byCategory.merge(products.get(l.productId()).category(), l.lineTotal(), BigDecimal::add);
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("orders", orders.size());
        res.put("revenueByCategory", Collections.unmodifiableMap(byCategory));
        return res;
    }

    private static String env(String key, String fallback) {
        return Objects.requireNonNullElse(System.getenv(key), fallback);
    }
}
