package com.qaportfolio.db.tests;

import com.qaportfolio.db.support.Db;
import com.qaportfolio.db.support.DbReport;
import com.qaportfolio.db.support.MigratedDb;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/** DB-10..: profiling of the legacy data and integrity of the migrated data. */
public class DataQualityTest {

    @BeforeClass
    public void migrate() throws SQLException {
        MigratedDb.ensure();
    }

    @DataProvider
    public Object[][] legacyProfile() {
        return new Object[][]{
                {"legacy duplicate emails (case/space-insensitive)", 3L,
                        "SELECT COUNT(*) - COUNT(DISTINCT LOWER(TRIM(email))) FROM legacy_customers WHERE email IS NOT NULL AND TRIM(email) <> ''"},
                {"legacy customers with NULL/blank email", 2L,
                        "SELECT COUNT(*) FROM legacy_customers WHERE email IS NULL OR TRIM(email) = ''"},
                {"legacy orphan order items", 4L,
                        "SELECT COUNT(*) FROM legacy_order_items i WHERE NOT EXISTS (SELECT 1 FROM legacy_orders o WHERE o.id = i.order_id)"},
                {"legacy items with quantity <= 0", 3L,
                        "SELECT COUNT(*) FROM legacy_order_items WHERE quantity <= 0"},
                {"legacy money values with stray whitespace", 4L,
                        "SELECT COUNT(*) FROM legacy_products WHERE price <> TRIM(price)"},
        };
    }

    @Test(dataProvider = "legacyProfile", description = "DB-10 Legacy data profiling finds the seeded dirty rows")
    public void legacyDataProfile(String check, long expected, String sql) throws SQLException {
        try (Connection c = MigratedDb.connect()) {
            long actual = Db.count(c, sql);
            DbReport.record("legacy profiling", check, expected, actual);
            assertThat(actual).as(check).isEqualTo(expected);
        }
    }

    @DataProvider
    public Object[][] integrity() {
        return new Object[][]{
                {"duplicates", "customers with duplicate normalised email",
                        "SELECT COUNT(*) FROM (SELECT email_normalized FROM customers GROUP BY email_normalized HAVING COUNT(*) > 1) d"},
                {"duplicates", "products with duplicate sku",
                        "SELECT COUNT(*) FROM (SELECT sku FROM products GROUP BY sku HAVING COUNT(*) > 1) d"},
                {"duplicates", "duplicate product lines within an order",
                        "SELECT COUNT(*) FROM (SELECT order_id, product_id FROM order_items GROUP BY order_id, product_id HAVING COUNT(*) > 1) d"},
                {"referential integrity", "orders without customer",
                        "SELECT COUNT(*) FROM orders o LEFT JOIN customers c ON c.id = o.customer_id WHERE c.id IS NULL"},
                {"referential integrity", "items without order",
                        "SELECT COUNT(*) FROM order_items i LEFT JOIN orders o ON o.id = i.order_id WHERE o.id IS NULL"},
                {"referential integrity", "items without product",
                        "SELECT COUNT(*) FROM order_items i LEFT JOIN products p ON p.id = i.product_id WHERE p.id IS NULL"},
                {"referential integrity", "orders without any item",
                        "SELECT COUNT(*) FROM orders o WHERE NOT EXISTS (SELECT 1 FROM order_items i WHERE i.order_id = o.id)"},
                {"referential integrity", "legacy customers not mapped and not rejected",
                        "SELECT COUNT(*) FROM legacy_customers c WHERE NOT EXISTS (SELECT 1 FROM legacy_customer_map m WHERE m.legacy_customer_id = c.id) "
                                + "AND NOT EXISTS (SELECT 1 FROM migration_rejects r WHERE r.source_table = 'legacy_customers' AND r.source_id = c.id)"},
                {"data rules", "emails not trimmed",
                        "SELECT COUNT(*) FROM customers WHERE email <> TRIM(email)"},
                {"data rules", "status outside domain",
                        "SELECT COUNT(*) FROM orders WHERE status NOT IN ('NEW', 'PAID', 'SHIPPED', 'CANCELLED')"},
                {"totals", "orders whose total != sum(quantity * unit_price)",
                        "SELECT COUNT(*) FROM orders o WHERE o.total <> (SELECT SUM(i.quantity * i.unit_price) FROM order_items i WHERE i.order_id = o.id)"},
        };
    }

    @Test(dataProvider = "integrity", description = "DB-11 Migrated data has zero integrity violations")
    public void migratedDataIntegrity(String category, String check, String sql) throws SQLException {
        try (Connection c = MigratedDb.connect()) {
            long violations = Db.count(c, sql);
            DbReport.record(category, check, 0, violations);
            assertThat(violations).as(check).isZero();
        }
    }
}
