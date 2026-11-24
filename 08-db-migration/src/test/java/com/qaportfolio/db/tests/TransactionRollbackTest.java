package com.qaportfolio.db.tests;

import com.qaportfolio.db.support.Db;
import com.qaportfolio.db.support.DbReport;
import org.flywaydb.core.api.FlywayException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** DB-41/42: a batch that fails part-way leaves no partial data (JDBC transaction and Flyway migration). */
public class TransactionRollbackTest {

    private static final String SCHEMA = "mig_txn";

    @BeforeClass
    public void migrate() {
        Db.freshSchema(SCHEMA, null);
    }

    @Test(description = "DB-41 JDBC batch: failure on row 3 rolls back rows 1-2")
    public void failedJdbcBatchLeavesNoPartialData() throws SQLException {
        try (Connection c = Db.connect(SCHEMA)) {
            long before = Db.count(c, "SELECT COUNT(*) FROM products");
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO products (id, sku, name, price) VALUES (?, ?, ?, ?)")) {
                Object[][] rows = {{8001, "SKU-8001", "ok 1", 1.00}, {8002, "SKU-8002", "ok 2", 2.00},
                        {8003, "SKU-8003", "bad", -3.00}};
                for (Object[] r : rows) {
                    ps.setInt(1, (Integer) r[0]);
                    ps.setString(2, (String) r[1]);
                    ps.setString(3, (String) r[2]);
                    ps.setDouble(4, (Double) r[3]);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException expected) {
                c.rollback();
            }
            c.setAutoCommit(true);
            long after = Db.count(c, "SELECT COUNT(*) FROM products");
            long partial = Db.count(c, "SELECT COUNT(*) FROM products WHERE id IN (8001, 8002, 8003)");
            DbReport.record("transactions", "JDBC batch with failing 3rd row: rows left behind", 0, partial);
            assertThat(partial).isZero();
            assertThat(after).isEqualTo(before);
        }
    }

    @Test(description = "DB-42 Failing Flyway migration (V6) is rolled back as a unit")
    public void failedFlywayMigrationLeavesNoPartialData() throws SQLException {
        var flyway = Db.flyway(SCHEMA, null, "classpath:db/failing");
        assertThatThrownBy(flyway::migrate).isInstanceOf(FlywayException.class);
        try (Connection c = Db.connect(SCHEMA)) {
            long partial = Db.count(c, "SELECT COUNT(*) FROM products WHERE id IN (9001, 9002, 9003)");
            long failedHistory = Db.count(c, "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '6'");
            DbReport.record("transactions", "failing V6 migration: rows left behind", 0, partial);
            DbReport.info("transactions", "history rows recorded for failed V6 before repair (DB-dependent)",
                    failedHistory);
            assertThat(partial).isZero();
        }
        flyway.repair(); // remove any failed history entry so the schema is clean
        String current = Db.flyway(SCHEMA, null).info().current().getVersion().getVersion();
        DbReport.record("transactions", "schema version after failed V6 + repair", "5", current);
        assertThat(current).isEqualTo("5");
    }
}
