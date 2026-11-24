package com.qaportfolio.db.tests;

import com.qaportfolio.db.support.Db;
import com.qaportfolio.db.support.DbReport;
import com.qaportfolio.db.support.Fingerprint;
import com.qaportfolio.db.support.MigratedDb;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** DB-30: re-running migrate is a no-op; checksums of applied migrations still validate. */
public class IdempotencyTest {

    private static final List<String> TABLES = List.of("customers", "products", "orders", "order_items",
            "legacy_customer_map", "legacy_customers", "legacy_orders", "legacy_order_items");

    @Test(description = "DB-30 Second migrate executes 0 migrations and changes no data")
    public void migrateTwiceIsNoOp() throws SQLException {
        MigratedDb.ensure();
        Map<String, String> before = snapshot();
        var flyway = Db.flyway(Db.MAIN, null);
        int executed = flyway.migrate().migrationsExecuted;
        flyway.validate(); // throws if any applied script was edited after being applied
        Map<String, String> after = snapshot();
        DbReport.record("idempotency", "migrations executed on re-run", 0, executed);
        DbReport.record("idempotency", "table fingerprints unchanged (" + TABLES.size() + " tables)", before, after);
        DbReport.record("idempotency", "flyway validate (checksums)", "valid", "valid");
        assertThat(executed).isZero();
        assertThat(after).isEqualTo(before);
    }

    private Map<String, String> snapshot() throws SQLException {
        Map<String, String> m = new LinkedHashMap<>();
        try (Connection c = MigratedDb.connect()) {
            for (String t : TABLES) {
                m.put(t, Fingerprint.of(c, t));
            }
        }
        return m;
    }
}
