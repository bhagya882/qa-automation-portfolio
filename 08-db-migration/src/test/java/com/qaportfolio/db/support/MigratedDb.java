package com.qaportfolio.db.support;

import org.flywaydb.core.api.output.MigrateResult;

import java.sql.Connection;
import java.sql.SQLException;

/** Lazily migrates the shared, read-only validation schema exactly once per JVM. */
public final class MigratedDb {

    private static LegacyOracle oracle;
    private static MigrateResult result;

    private MigratedDb() {
    }

    public static synchronized LegacyOracle ensure() throws SQLException {
        if (oracle == null) {
            var flyway = Db.flyway(Db.MAIN, null);
            flyway.clean();
            result = flyway.migrate();
            try (Connection c = Db.connect(Db.MAIN)) {
                oracle = LegacyOracle.compute(c);
            }
            DbReport.info("setup", "migrations applied on " + Db.MAIN, result.migrationsExecuted
                    + " (target version " + result.targetSchemaVersion + ")");
        }
        return oracle;
    }

    public static Connection connect() throws SQLException {
        ensure();
        return Db.connect(Db.MAIN);
    }
}
