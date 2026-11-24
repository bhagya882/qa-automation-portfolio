package com.qaportfolio.db.tests;

import com.qaportfolio.db.support.Db;
import com.qaportfolio.db.support.DbReport;
import com.qaportfolio.db.support.MigratedDb;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** DB-01..: column data types (information_schema) and constraint enforcement. */
public class SchemaValidationTest {

    @BeforeClass
    public void migrate() throws SQLException {
        MigratedDb.ensure();
    }

    @DataProvider
    public Object[][] expectedColumns() {
        // table, column, type family, precision, scale, nullable
        return new Object[][]{
                {"customers", "id", "integer", null, null, "NO"},
                {"customers", "email", "character varying", null, null, "NO"},
                {"customers", "email_normalized", "character varying", null, null, "NO"},
                {"customers", "created_at", "timestamp", null, null, "NO"},
                {"customers", "phone", "character varying", null, null, "YES"},
                {"products", "price", "numeric", 10, 2, "NO"},
                {"products", "sku", "character varying", null, null, "NO"},
                {"orders", "customer_id", "integer", null, null, "NO"},
                {"orders", "order_date", "timestamp", null, null, "NO"},
                {"orders", "total", "numeric", 10, 2, "NO"},
                {"orders", "status", "character varying", null, null, "NO"},
                {"order_items", "quantity", "integer", null, null, "NO"},
                {"order_items", "unit_price", "numeric", 10, 2, "NO"},
                {"migration_rejects", "reason", "character varying", null, null, "NO"},
        };
    }

    @Test(dataProvider = "expectedColumns", description = "DB-01 Column has the expected type, precision and nullability")
    public void columnDataType(String table, String column, String type, Integer precision, Integer scale, String nullable)
            throws SQLException {
        try (Connection c = MigratedDb.connect()) {
            var rows = Db.rows(c, "SELECT data_type, numeric_precision, numeric_scale, is_nullable "
                    + "FROM information_schema.columns WHERE LOWER(table_schema) = ? AND LOWER(table_name) = ? "
                    + "AND LOWER(column_name) = ?", Db.MAIN, table, column);
            assertThat(rows).as("column %s.%s exists", table, column).hasSize(1);
            Map<String, Object> r = rows.get(0);
            String actualType = String.valueOf(r.get("data_type")).toLowerCase(Locale.ROOT);
            String actual = actualType + (precision == null ? "" : "(" + num(r.get("numeric_precision")) + ","
                    + num(r.get("numeric_scale")) + ")") + " null=" + r.get("is_nullable");
            String expected = type + (precision == null ? "" : "(" + precision + "," + scale + ")") + " null=" + nullable;
            // PostgreSQL reports "timestamp without time zone"; H2 reports "timestamp"
            if (actualType.startsWith("timestamp")) {
                actual = actual.replace(actualType, "timestamp");
            }
            DbReport.record("data types", table + "." + column, expected, actual);
            assertThat(actual).isEqualTo(expected);
        }
    }

    private static String num(Object o) {
        return o == null ? "null" : String.valueOf(((Number) o).intValue());
    }

    @DataProvider
    public Object[][] invalidRows() {
        return new Object[][]{
                {"NOT NULL email", "INSERT INTO customers (id, email, email_normalized, full_name, created_at) "
                        + "VALUES (9001, NULL, 'x@example.test', 'X', CURRENT_TIMESTAMP)"},
                {"UNIQUE email (case-insensitive)", "INSERT INTO customers (id, email, email_normalized, full_name, created_at) "
                        + "SELECT 9002, UPPER(email), email_normalized, 'Dup', CURRENT_TIMESTAMP FROM customers WHERE id = 1"},
                {"CHECK email_normalized = lower(email)", "INSERT INTO customers (id, email, email_normalized, full_name, created_at) "
                        + "VALUES (9003, 'Mixed@Example.test', 'Mixed@Example.test', 'X', CURRENT_TIMESTAMP)"},
                {"CHECK email format", "INSERT INTO customers (id, email, email_normalized, full_name, created_at) "
                        + "VALUES (9004, 'not-an-email', 'not-an-email', 'X', CURRENT_TIMESTAMP)"},
                {"CHECK price >= 0", "INSERT INTO products (id, sku, name, price) VALUES (9005, 'SKU-9005', 'Neg', -0.01)"},
                {"UNIQUE sku", "INSERT INTO products (id, sku, name, price) SELECT 9006, sku, 'Dup', 1 FROM products WHERE id = 1"},
                {"NUMERIC(10,2) overflow", "INSERT INTO products (id, sku, name, price) VALUES (9007, 'SKU-9007', 'Big', 123456789.99)"},
                {"FK order -> customer", "INSERT INTO orders (id, customer_id, order_date, status, total) "
                        + "VALUES (9008, 999999, CURRENT_TIMESTAMP, 'NEW', 1)"},
                {"CHECK status domain", "INSERT INTO orders (id, customer_id, order_date, status, total) "
                        + "VALUES (9009, 1, CURRENT_TIMESTAMP, 'paid', 1)"},
                {"CHECK total >= 0", "INSERT INTO orders (id, customer_id, order_date, status, total) "
                        + "VALUES (9010, 1, CURRENT_TIMESTAMP, 'NEW', -5)"},
                {"CHECK quantity > 0", "INSERT INTO order_items (id, order_id, product_id, quantity, unit_price) "
                        + "SELECT 9011, MIN(id), 1, 0, 1 FROM orders"},
                {"FK item -> order", "INSERT INTO order_items (id, order_id, product_id, quantity, unit_price) "
                        + "VALUES (9012, 999999, 1, 1, 1)"},
                {"FK item -> product", "INSERT INTO order_items (id, order_id, product_id, quantity, unit_price) "
                        + "SELECT 9013, MIN(id), 999999, 1, 1 FROM orders"},
                {"FK parent delete restricted", "DELETE FROM customers WHERE id = (SELECT MIN(customer_id) FROM orders)"},
        };
    }

    @Test(dataProvider = "invalidRows", description = "DB-02 Constraint rejects invalid data")
    public void constraintRejectsInvalidRow(String constraint, String sql) throws SQLException {
        try (Connection c = MigratedDb.connect()) {
            c.setAutoCommit(false);
            try (Statement st = c.createStatement()) {
                Throwable thrown = null;
                try {
                    st.executeUpdate(sql);
                } catch (SQLException e) {
                    thrown = e;
                }
                DbReport.record("constraints", constraint, "rejected", thrown == null ? "ACCEPTED" : "rejected");
                assertThat(thrown).as("'%s' must be rejected by the database", constraint).isNotNull();
            } finally {
                c.rollback();
            }
        }
    }

    @Test(description = "DB-03 Positive control: a fully valid customer/order/item is accepted")
    public void validRowsAccepted() throws SQLException {
        try (Connection c = MigratedDb.connect()) {
            c.setAutoCommit(false);
            try (Statement st = c.createStatement()) {
                st.executeUpdate("INSERT INTO customers (id, email, email_normalized, full_name, created_at) "
                        + "VALUES (9100, 'New.User@example.test', 'new.user@example.test', 'New User', CURRENT_TIMESTAMP)");
                st.executeUpdate("INSERT INTO orders (id, customer_id, order_date, status, total) "
                        + "VALUES (9100, 9100, CURRENT_TIMESTAMP, 'NEW', 12.34)");
                st.executeUpdate("INSERT INTO order_items (id, order_id, product_id, quantity, unit_price) "
                        + "VALUES (9100, 9100, 1, 2, 6.17)");
                DbReport.record("constraints", "valid customer/order/item accepted (control)", "accepted", "accepted");
            } finally {
                c.rollback();
            }
            assertThatThrownBy(() -> Db.count(c, "SELECT COUNT(*) FROM nonexistent_table"))
                    .as("sanity: SQL errors surface as SQLException").isInstanceOf(SQLException.class);
        }
    }
}
