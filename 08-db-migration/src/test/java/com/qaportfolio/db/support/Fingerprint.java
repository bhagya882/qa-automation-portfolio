package com.qaportfolio.db.support;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HexFormat;

/** Order-stable SHA-256 over every row of a table: detects any change in content, not just counts. */
public final class Fingerprint {

    private Fingerprint() {
    }

    public static String of(Connection c, String table) throws SQLException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        long rows = 0;
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM " + table + " ORDER BY 1")) {
            int n = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                rows++;
                for (int i = 1; i <= n; i++) {
                    Object v = rs.getObject(i);
                    String s = v == null ? "\u0000" : v instanceof BigDecimal b ? b.stripTrailingZeros().toPlainString() : v.toString();
                    md.update(s.getBytes(StandardCharsets.UTF_8));
                    md.update((byte) 0x1F);
                }
                md.update((byte) 0x1E);
            }
        }
        return rows + ":" + HexFormat.of().formatHex(md.digest()).substring(0, 16);
    }
}
