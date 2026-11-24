package com.qaportfolio.db.support;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Independent test oracle: re-implements the migration rules in Java directly from the legacy tables, so the
 * expected results are NOT derived from the SQL under test.
 */
public final class LegacyOracle {

    public final Map<String, Long> rejectsByReason = new TreeMap<>();
    public final Map<Integer, Integer> customerMap = new HashMap<>();   // legacy id -> surviving id
    public final Map<String, BigDecimal> totalsByEmail = new TreeMap<>();
    public final Set<Integer> migratedOrders = new HashSet<>();
    public long legacyCustomers;
    public long legacyOrders;
    public long legacyItems;
    public long legacyProducts;
    public long expectedCustomers;
    public long expectedOrders;
    public long expectedItems;
    public BigDecimal legacyGrandTotal = BigDecimal.ZERO;
    public BigDecimal expectedMigratedGrandTotal = BigDecimal.ZERO;

    public static LegacyOracle compute(Connection c) throws SQLException {
        LegacyOracle o = new LegacyOracle();
        Map<String, Integer> survivorByEmail = new HashMap<>();
        Map<Integer, String> emailById = new HashMap<>();
        var customers = Db.rows(c, "SELECT id, email FROM legacy_customers ORDER BY id");
        o.legacyCustomers = customers.size();
        for (var r : customers) {
            int id = ((Number) r.get("id")).intValue();
            String email = (String) r.get("email");
            if (email == null || email.isBlank()) {
                o.reject("legacy_customers/NULL_EMAIL");
                continue;
            }
            String key = email.trim().toLowerCase(Locale.ROOT);
            Integer survivor = survivorByEmail.putIfAbsent(key, id);
            if (survivor != null) {
                o.reject("legacy_customers/DUPLICATE_EMAIL_MERGED");
            }
            o.customerMap.put(id, survivor == null ? id : survivor);
            emailById.put(id, key);
        }
        o.expectedCustomers = survivorByEmail.size();
        o.legacyProducts = Db.count(c, "SELECT COUNT(*) FROM legacy_products");

        Set<Integer> badQtyOrders = new HashSet<>();
        Map<Integer, Integer> itemsPerOrder = new HashMap<>();
        var items = Db.rows(c, "SELECT order_id, quantity FROM legacy_order_items");
        o.legacyItems = items.size();
        for (var r : items) {
            int orderId = ((Number) r.get("order_id")).intValue();
            Number q = (Number) r.get("quantity");
            if (q == null || q.intValue() <= 0) {
                badQtyOrders.add(orderId);
            }
            itemsPerOrder.merge(orderId, 1, Integer::sum);
        }

        var orders = Db.rows(c, "SELECT id, customer_id, total FROM legacy_orders ORDER BY id");
        o.legacyOrders = orders.size();
        Set<Integer> legacyOrderIds = new HashSet<>();
        for (var r : orders) {
            int id = ((Number) r.get("id")).intValue();
            legacyOrderIds.add(id);
            BigDecimal total = new BigDecimal(((String) r.get("total")).trim());
            o.legacyGrandTotal = o.legacyGrandTotal.add(total);
            Number cust = (Number) r.get("customer_id");
            if (cust == null || !o.customerMap.containsKey(cust.intValue())) {
                o.reject("legacy_orders/CUSTOMER_NOT_MIGRATED");
            } else if (badQtyOrders.contains(id)) {
                o.reject("legacy_orders/INVALID_ITEM_QUANTITY");
            } else {
                o.migratedOrders.add(id);
                o.expectedMigratedGrandTotal = o.expectedMigratedGrandTotal.add(total);
                o.totalsByEmail.merge(emailById.get(cust.intValue()), total, BigDecimal::add);
            }
        }
        o.expectedOrders = o.migratedOrders.size();
        for (var e : itemsPerOrder.entrySet()) {
            if (!legacyOrderIds.contains(e.getKey())) {
                o.reject("legacy_order_items/ORPHAN_ORDER_ITEM", e.getValue());
            } else if (!o.migratedOrders.contains(e.getKey())) {
                o.reject("legacy_order_items/PARENT_ORDER_REJECTED", e.getValue());
            } else {
                o.expectedItems += e.getValue();
            }
        }
        return o;
    }

    private void reject(String key) {
        reject(key, 1);
    }

    private void reject(String key, long n) {
        rejectsByReason.merge(key, n, Long::sum);
    }
}
