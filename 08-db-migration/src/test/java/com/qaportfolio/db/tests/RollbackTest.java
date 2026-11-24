package com.qaportfolio.db.tests;

import com.qaportfolio.db.support.Db;
import com.qaportfolio.db.support.DbReport;
import com.qaportfolio.db.support.Fingerprint;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DB-40: the documented rollback (undo scripts U5 → U4 → U3) restores the exact V2 state, and the migration can
 * then be re-applied with identical results. Runs in its own schema.
 */
public class RollbackTest {

    private static final String SCHEMA = "mig_rollback";
    private static final List<String> LEGACY = List.of("legacy_customers", "legacy_products", "legacy_orders",
            "legacy_order_items");

    @Test(description = "DB-40 Undo scripts restore the V2 schema and data; re-migration is repeatable")
    public void undoRestoresV2AndRemigrationIsRepeatable() throws SQLException {
        Db.freshSchema(SCHEMA, "2");
        List<String> v2Tables;
        Map<String, String> v2Data;
        try (Connection c = Db.connect(SCHEMA)) {
            v2Tables = Db.tables(c, SCHEMA);
            v2Data = fingerprints(c);
        }

        var first = Db.flyway(SCHEMA, null).migrate();
        Map<String, Long> firstCounts;
        try (Connection c = Db.connect(SCHEMA)) {
            firstCounts = counts(c);
            for (String undo : List.of("db/undo/U5__indexes_and_reporting_view.sql",
                    "db/undo/U4__migrate_and_cleanse_data.sql", "db/undo/U3__normalised_schema.sql")) {
                Db.runScript(c, undo);
            }
            if (!c.getAutoCommit()) {
                c.commit();
            }
        }

        try (Connection c = Db.connect(SCHEMA)) {
            List<String> afterTables = Db.tables(c, SCHEMA);
            Map<String, String> afterData = fingerprints(c);
            String version = Db.flyway(SCHEMA, null).info().current().getVersion().getVersion();
            DbReport.record("rollback", "migrations applied V3..V5", 3, first.migrationsExecuted);
            DbReport.record("rollback", "tables after undo == V2 tables", v2Tables, afterTables);
            DbReport.record("rollback", "legacy data fingerprints unchanged", v2Data, afterData);
            DbReport.record("rollback", "flyway current version after undo", "2", version);
            assertThat(afterTables).isEqualTo(v2Tables);
            assertThat(afterData).isEqualTo(v2Data);
            assertThat(version).isEqualTo("2");
        }

        int reapplied = Db.flyway(SCHEMA, null).migrate().migrationsExecuted;
        try (Connection c = Db.connect(SCHEMA)) {
            Map<String, Long> secondCounts = counts(c);
            DbReport.record("rollback", "re-migration after undo executes V3..V5", 3, reapplied);
            DbReport.record("rollback", "re-migration produces identical counts", firstCounts, secondCounts);
            assertThat(reapplied).isEqualTo(3);
            assertThat(secondCounts).isEqualTo(firstCounts);
        }
    }

    private static Map<String, String> fingerprints(Connection c) throws SQLException {
        Map<String, String> m = new LinkedHashMap<>();
        for (String t : LEGACY) {
            m.put(t, Fingerprint.of(c, t));
        }
        return m;
    }

    private static Map<String, Long> counts(Connection c) throws SQLException {
        Map<String, Long> m = new LinkedHashMap<>();
        for (String t : List.of("customers", "products", "orders", "order_items", "migration_rejects")) {
            m.put(t, Db.count(c, "SELECT COUNT(*) FROM " + t));
        }
        return m;
    }
}
