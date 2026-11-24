package db.migration;

import net.datafaker.Faker;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;

/**
 * Seeds the legacy schema with deterministic synthetic data (Datafaker, fixed seed 20261018), so every run, on
 * H2 or PostgreSQL, produces identical rows. No real personal data: emails use the reserved {@code .test} TLD.
 *
 * <p>Deliberately dirty rows, each covered by a migration rule (see V4):
 * <ul>
 *   <li>3 customers duplicating an earlier email with different case/whitespace (ids 41-43)</li>
 *   <li>2 customers with NULL / blank email (ids 44-45), each with one order</li>
 *   <li>3 orders containing a line with quantity &lt;= 0</li>
 *   <li>4 order items pointing to a non-existent order (orphans)</li>
 *   <li>Money with stray whitespace, mixed-case status values</li>
 * </ul>
 */
public class V2__seed_legacy_data extends BaseJavaMigration {

    public static final long SEED = 20261018L;
    public static final int CLEAN_CUSTOMERS = 40;
    public static final int PRODUCTS = 20;
    public static final int ORDERS = 120;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String[] STATUS = {"paid", "PAID", "Shipped", "new", " cancelled ", "shipped"};

    @Override
    public void migrate(Context context) throws Exception {
        Connection c = context.getConnection();
        Random rnd = new Random(SEED);
        Faker faker = new Faker(Locale.ENGLISH, new Random(SEED));
        LocalDateTime base = LocalDateTime.of(2024, 1, 1, 9, 0);

        String[] emails = new String[CLEAN_CUSTOMERS + 6];
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO legacy_customers (id, email, full_name, phone, created_at) VALUES (?, ?, ?, ?, ?)")) {
            for (int id = 1; id <= CLEAN_CUSTOMERS; id++) {
                String first = faker.name().firstName();
                String last = faker.name().lastName();
                String email = (first + "." + last + id).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.]", "")
                        + "@example.test";
                emails[id] = email;
                customer(ps, id, email, first + " " + last, faker.phoneNumber().cellPhone(),
                        base.plusDays(id).format(TS));
            }
            // Dirty: duplicates of customers 1..3 differing only in case / whitespace
            customer(ps, 41, emails[1].toUpperCase(Locale.ROOT), "Duplicate One", null, base.plusDays(60).format(TS));
            customer(ps, 42, "  " + capitalize(emails[2]) + " ", "Duplicate Two", null, base.plusDays(61).format(TS));
            customer(ps, 43, emails[3].replace("example.test", "Example.TEST"), "Duplicate Three", null,
                    base.plusDays(62).format(TS));
            // Dirty: missing email
            customer(ps, 44, null, "No Email", null, base.plusDays(63).format(TS));
            customer(ps, 45, "   ", "Blank Email", null, base.plusDays(64).format(TS));
            ps.executeBatch();
        }

        BigDecimal[] prices = new BigDecimal[PRODUCTS + 1];
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO legacy_products (id, sku, name, price) VALUES (?, ?, ?, ?)")) {
            for (int id = 1; id <= PRODUCTS; id++) {
                prices[id] = BigDecimal.valueOf(199 + rnd.nextInt(9800)).movePointLeft(2);
                ps.setInt(1, id);
                ps.setString(2, (id % 4 == 0 ? " sku-" : "SKU-") + String.format("%04d", id));
                ps.setString(3, faker.commerce().productName());
                // Dirty: some prices stored with surrounding whitespace
                ps.setString(4, id % 5 == 0 ? " " + prices[id].toPlainString() + " " : prices[id].toPlainString());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        int itemId = 1;
        try (PreparedStatement po = c.prepareStatement(
                "INSERT INTO legacy_orders (id, customer_id, customer_email, order_date, status, total) VALUES (?, ?, ?, ?, ?, ?)");
             PreparedStatement pi = c.prepareStatement(
                     "INSERT INTO legacy_order_items (id, order_id, product_id, product_name, quantity, unit_price) VALUES (?, ?, ?, ?, ?, ?)")) {
            for (int id = 1; id <= ORDERS; id++) {
                // Orders 1-6 belong to the dirty customers: duplicates 41,42,43 (+41 again), email-less 44,45
                int customer = id <= 6 ? new int[]{41, 42, 43, 44, 45, 41}[id - 1] : 1 + rnd.nextInt(CLEAN_CUSTOMERS);
                boolean invalidQty = id == 30 || id == 60 || id == 90;
                int lines = 1 + rnd.nextInt(4);
                BigDecimal total = BigDecimal.ZERO;
                int firstProduct = 1 + rnd.nextInt(PRODUCTS);
                for (int l = 0; l < lines; l++) {
                    int product = 1 + (firstProduct + l * 3) % PRODUCTS;  // distinct products per order
                    int qty = (invalidQty && l == 0) ? -1 * (1 + rnd.nextInt(2)) : 1 + rnd.nextInt(5);
                    total = total.add(prices[product].multiply(BigDecimal.valueOf(qty)));
                    item(pi, itemId++, id, product, "Product " + product, qty, prices[product].toPlainString());
                }
                po.setInt(1, id);
                po.setInt(2, customer);
                po.setString(3, customer <= CLEAN_CUSTOMERS ? emails[customer] : null);
                po.setString(4, base.plusDays(100 + id).plusMinutes(rnd.nextInt(600)).format(TS));
                po.setString(5, STATUS[rnd.nextInt(STATUS.length)]);
                String t = total.setScale(2, RoundingMode.HALF_UP).toPlainString();
                po.setString(6, id % 7 == 0 ? " " + t : t);
                po.addBatch();
            }
            // Dirty: orphan items referencing orders that don't exist
            for (int k = 0; k < 4; k++) {
                item(pi, itemId++, 90_000 + k, 1 + k, "Ghost", 1, prices[1 + k].toPlainString());
            }
            po.executeBatch();
            pi.executeBatch();
        }
    }

    private static void customer(PreparedStatement ps, int id, String email, String name, String phone, String created)
            throws SQLException {
        ps.setInt(1, id);
        if (email == null) {
            ps.setNull(2, Types.VARCHAR);
        } else {
            ps.setString(2, email);
        }
        ps.setString(3, name);
        ps.setString(4, phone);
        ps.setString(5, created);
        ps.addBatch();
    }

    private static void item(PreparedStatement ps, int id, int order, int product, String name, int qty, String price)
            throws SQLException {
        ps.setInt(1, id);
        ps.setInt(2, order);
        ps.setInt(3, product);
        ps.setString(4, name);
        ps.setInt(5, qty);
        ps.setString(6, price);
        ps.addBatch();
    }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
