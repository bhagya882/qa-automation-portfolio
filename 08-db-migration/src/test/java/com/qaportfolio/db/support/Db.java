package com.qaportfolio.db.support;

import com.qaportfolio.core.config.Config;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DB access for the suite. Each test area works in its own schema so destructive tests (rollback) never
 * interfere with read-only validation. Works on H2 (default, PostgreSQL mode) and real PostgreSQL.
 */
public final class Db {

    public static final String MAIN = "mig_main";

    private Db() {
    }

    public static String url() {
        return Config.require("db.url");
    }

    public static boolean isPostgres() {
        return url().startsWith("jdbc:postgresql");
    }

    public static Connection connect(String schema) throws SQLException {
        Connection c = DriverManager.getConnection(url(), Config.get("db.user", "sa"), Config.get("db.password", ""));
        if (schema != null) {
            c.setSchema(schema);
        }
        return c;
    }

    public static Flyway flyway(String schema, String target, String... extraLocations) {
        List<String> locations = new ArrayList<>(List.of("classpath:db/migration"));
        locations.addAll(List.of(extraLocations));
        FluentConfiguration cfg = Flyway.configure()
                .dataSource(url(), Config.get("db.user", "sa"), Config.get("db.password", ""))
                .schemas(schema)
                .defaultSchema(schema)
                .locations(locations.toArray(String[]::new))
                .cleanDisabled(false);
        if (target != null) {
            cfg.target(target);
        }
        return cfg.load();
    }

    /** Fresh schema migrated to {@code target} (null = latest). Cleans first so re-runs on PostgreSQL are repeatable. */
    public static Flyway freshSchema(String schema, String target) {
        Flyway f = flyway(schema, target);
        f.clean();
        f.migrate();
        return f;
    }

    /** Runs a classpath SQL script statement by statement (scripts must not contain ';' inside literals). */
    public static void runScript(Connection c, String resource) throws SQLException {
        String sql;
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing script " + resource);
            }
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        StringBuilder cleaned = new StringBuilder();
        for (String line : sql.split("\n")) {
            if (!line.trim().startsWith("--")) {
                cleaned.append(line).append('\n');
            }
        }
        try (Statement st = c.createStatement()) {
            for (String stmt : cleaned.toString().split(";")) {
                if (!stmt.isBlank()) {
                    st.execute(stmt.trim());
                }
            }
        }
    }

    public static long count(Connection c, String sql, Object... params) throws SQLException {
        Object v = scalar(c, sql, params);
        return v == null ? 0 : ((Number) v).longValue();
    }

    public static Object scalar(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject(1) : null;
            }
        }
    }

    public static List<Map<String, Object>> rows(Connection c, String sql, Object... params) throws SQLException {
        List<Map<String, Object>> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                int n = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= n; i++) {
                        row.put(rs.getMetaData().getColumnLabel(i).toLowerCase(), rs.getObject(i));
                    }
                    out.add(row);
                }
            }
        }
        return out;
    }

    /** Base tables in a schema (lower-case names), excluding Flyway's history table. */
    public static List<String> tables(Connection c, String schema) throws SQLException {
        List<String> names = new ArrayList<>();
        for (Map<String, Object> r : rows(c, "SELECT table_name FROM information_schema.tables "
                + "WHERE LOWER(table_schema) = ? AND table_type IN ('BASE TABLE', 'TABLE') ORDER BY table_name", schema)) {
            String t = String.valueOf(r.get("table_name")).toLowerCase();
            if (!t.equals("flyway_schema_history")) {
                names.add(t);
            }
        }
        return names.stream().sorted().toList();
    }
}
