package com.qaportfolio.db.tests;

import com.qaportfolio.db.support.Db;
import com.qaportfolio.db.support.DbReport;
import com.qaportfolio.db.support.LegacyOracle;
import com.qaportfolio.db.support.MigratedDb;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/** DB-20..: source-to-target reconciliation against an independent Java oracle. */
public class ReconciliationTest {

    private LegacyOracle oracle;

    @BeforeClass
    public void migrate() throws SQLException {
        oracle = MigratedDb.ensure();
    }

    @DataProvider
    public Object[][] tables() {
        return new Object[][]{{"legacy_customers", "customers"}, {"legacy_orders", "orders"},
                {"legacy_order_items", "order_items"}, {"legacy_products", "products"}};
    }

    @Test(dataProvider = "tables", description = "DB-20 Row counts: legacy = migrated + rejected")
    public void rowCountReconciliation(String legacy, String target) throws SQLException {
        try (Connection c = MigratedDb.connect()) {
            long source = Db.count(c, "SELECT COUNT(*) FROM " + legacy);
            long migrated = Db.count(c, "SELECT COUNT(*) FROM " + target);
            long rejected = Db.count(c, "SELECT COUNT(*) FROM migration_rejects WHERE source_table = ?", legacy);
            DbReport.record("row counts", legacy + " = " + target + " (" + migrated + ") + rejects (" + rejected + ")",
                    source, migrated + rejected);
            assertThat(migrated + rejected).as("%s reconciles", legacy).isEqualTo(source);
        }
    }

    @Test(description = "DB-21 Row counts match the independent oracle")
    public void countsMatchOracle() throws SQLException {
        try (Connection c = MigratedDb.connect()) {
            Map<String, Long> actual = new TreeMap<>(Map.of(
                    "customers", Db.count(c, "SELECT COUNT(*) FROM customers"),
                    "orders", Db.count(c, "SELECT COUNT(*) FROM orders"),
                    "order_items", Db.count(c, "SELECT COUNT(*) FROM order_items"),
                    "products", Db.count(c, "SELECT COUNT(*) FROM products")));
            Map<String, Long> expected = new TreeMap<>(Map.of("customers", oracle.expectedCustomers,
                    "orders", oracle.expectedOrders, "order_items", oracle.expectedItems,
                    "products", oracle.legacyProducts));
            DbReport.record("row counts", "target counts vs oracle", expected, actual);
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Test(description = "DB-22 Rejects per table/reason match the oracle")
    public void rejectsMatchOracle() throws SQLException {
        try (Connection c = MigratedDb.connect()) {
            Map<String, Long> actual = new TreeMap<>();
            for (var r : Db.rows(c, "SELECT source_table, reason, COUNT(*) AS n FROM migration_rejects GROUP BY source_table, reason")) {
                actual.put(r.get("source_table") + "/" + r.get("reason"), ((Number) r.get("n")).longValue());
            }
            DbReport.record("rejects", "rejects by reason vs oracle", oracle.rejectsByReason, actual);
            assertThat(actual).isEqualTo(oracle.rejectsByReason);
        }
    }

    @Test(description = "DB-23 Sum of order totals per customer is unchanged (duplicates merged)")
    public void totalsPerCustomerReconcile() throws SQLException {
        try (Connection c = MigratedDb.connect()) {
            Map<String, BigDecimal> actual = new TreeMap<>();
            for (var r : Db.rows(c, "SELECT email, total_spent FROM v_customer_order_totals WHERE order_count > 0")) {
                actual.put((String) r.get("email"), ((BigDecimal) r.get("total_spent")).stripTrailingZeros());
            }
            Map<String, BigDecimal> expected = new TreeMap<>();
            oracle.totalsByEmail.forEach((k, v) -> expected.put(k, v.stripTrailingZeros()));
            boolean ok = DbReport.record("totals", "per-customer order totals (" + expected.size() + " customers)",
                    "all equal", expected.equals(actual) ? "all equal" : "MISMATCH " + diff(expected, actual));
            assertThat(ok).as("per-customer totals: %s", diff(expected, actual)).isTrue();
        }
    }

    @Test(description = "DB-24 Grand total: legacy = migrated + rejected order totals")
    public void grandTotalReconciles() throws SQLException {
        try (Connection c = MigratedDb.connect()) {
            BigDecimal migrated = (BigDecimal) Db.scalar(c, "SELECT SUM(total) FROM orders");
            BigDecimal rejected = BigDecimal.ZERO;
            for (var r : Db.rows(c, "SELECT o.total FROM legacy_orders o JOIN migration_rejects r "
                    + "ON r.source_table = 'legacy_orders' AND r.source_id = o.id")) {
                rejected = rejected.add(new BigDecimal(((String) r.get("total")).trim()));
            }
            DbReport.record("totals", "legacy grand total = migrated + rejected",
                    oracle.legacyGrandTotal.stripTrailingZeros().toPlainString(),
                    migrated.add(rejected).stripTrailingZeros().toPlainString());
            DbReport.record("totals", "migrated grand total vs oracle",
                    oracle.expectedMigratedGrandTotal.stripTrailingZeros().toPlainString(),
                    migrated.stripTrailingZeros().toPlainString());
            assertThat(migrated.add(rejected)).isEqualByComparingTo(oracle.legacyGrandTotal);
            assertThat(migrated).isEqualByComparingTo(oracle.expectedMigratedGrandTotal);
        }
    }

    @DataProvider
    public Object[][] sampleOrders() {
        return new Object[][]{{7}, {25}, {50}, {77}, {120}, {1}};
    }

    @Test(dataProvider = "sampleOrders", description = "DB-25 Record-level comparison of sampled orders")
    public void sampledOrderMatchesLegacy(int orderId) throws SQLException {
        try (Connection c = MigratedDb.connect()) {
            var legacy = Db.rows(c, "SELECT o.order_date, o.status, o.total, LOWER(TRIM(c.email)) AS email, "
                    + "(SELECT COUNT(*) FROM legacy_order_items i WHERE i.order_id = o.id) AS lines "
                    + "FROM legacy_orders o JOIN legacy_customers c ON c.id = o.customer_id WHERE o.id = ?", orderId).get(0);
            var target = Db.rows(c, "SELECT o.order_date, o.status, o.total, c.email_normalized AS email, "
                    + "(SELECT COUNT(*) FROM order_items i WHERE i.order_id = o.id) AS lines "
                    + "FROM orders o JOIN customers c ON c.id = o.customer_id WHERE o.id = ?", orderId).get(0);
            String exp = String.join(" | ", (String) legacy.get("email"),
                    Timestamp.valueOf(((String) legacy.get("order_date")).trim()).toString(),
                    ((String) legacy.get("status")).trim().toUpperCase(Locale.ROOT),
                    new BigDecimal(((String) legacy.get("total")).trim()).stripTrailingZeros().toPlainString(),
                    String.valueOf(((Number) legacy.get("lines")).longValue()));
            String act = String.join(" | ", (String) target.get("email"), target.get("order_date").toString(),
                    (String) target.get("status"),
                    ((BigDecimal) target.get("total")).stripTrailingZeros().toPlainString(),
                    String.valueOf(((Number) target.get("lines")).longValue()));
            DbReport.record("record level", "order " + orderId + " (email|date|status|total|lines)", exp, act);
            assertThat(act).isEqualTo(exp);
        }
    }

    @Test(description = "DB-26 Duplicate customer 41 is merged into survivor 1 and its orders follow")
    public void duplicateCustomerMerged() throws SQLException {
        try (Connection c = MigratedDb.connect()) {
            long mappedTo = Db.count(c, "SELECT customer_id FROM legacy_customer_map WHERE legacy_customer_id = 41");
            long ordersMoved = Db.count(c, "SELECT COUNT(*) FROM orders WHERE id IN (1, 6) AND customer_id = 1");
            DbReport.record("record level", "legacy customer 41 -> customer", 1, mappedTo);
            DbReport.record("record level", "orders 1 and 6 re-pointed to customer 1", 2, ordersMoved);
            assertThat(mappedTo).isEqualTo(1);
            assertThat(ordersMoved).isEqualTo(2);
        }
    }

    private static String diff(Map<String, BigDecimal> e, Map<String, BigDecimal> a) {
        StringBuilder sb = new StringBuilder();
        e.forEach((k, v) -> {
            if (!v.equals(a.get(k))) {
                sb.append(k).append(": expected ").append(v).append(" got ").append(a.get(k)).append("; ");
            }
        });
        a.keySet().stream().filter(k -> !e.containsKey(k)).forEach(k -> sb.append(k).append(": unexpected; "));
        return sb.toString();
    }
}
