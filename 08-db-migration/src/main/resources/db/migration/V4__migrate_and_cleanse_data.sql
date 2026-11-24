-- Data migration legacy_* -> normalised tables. Rules (see docs/test-plan.md):
--   R1 customers with NULL/blank email          -> rejected  (NULL_EMAIL)
--   R2 duplicate emails (case/whitespace)        -> merged into lowest id, duplicates logged (DUPLICATE_EMAIL_MERGED)
--   R3 orders whose customer was rejected/missing-> rejected  (CUSTOMER_NOT_MIGRATED)
--   R4 orders containing a line with qty <= 0    -> rejected  (INVALID_ITEM_QUANTITY), with all their lines
--   R5 order items without a legacy order        -> rejected  (ORPHAN_ORDER_ITEM)
--   R6 items of a rejected order                 -> rejected  (PARENT_ORDER_REJECTED)
--   Cleansing: TRIM text, LOWER email key, UPPER status, text money -> NUMERIC(10,2), text dates -> TIMESTAMP

-- R1
INSERT INTO migration_rejects (source_table, source_id, reason, payload)
SELECT 'legacy_customers', id, 'NULL_EMAIL', 'full_name=' || COALESCE(full_name, '')
FROM legacy_customers
WHERE email IS NULL OR TRIM(email) = '';

-- Survivors: lowest id per normalised email
INSERT INTO customers (id, email, email_normalized, full_name, phone, created_at)
SELECT c.id, TRIM(c.email), LOWER(TRIM(c.email)), TRIM(c.full_name), TRIM(c.phone), CAST(TRIM(c.created_at) AS TIMESTAMP)
FROM legacy_customers c
WHERE c.email IS NOT NULL AND TRIM(c.email) <> ''
  AND c.id = (SELECT MIN(c2.id) FROM legacy_customers c2 WHERE LOWER(TRIM(c2.email)) = LOWER(TRIM(c.email)));

-- R2
INSERT INTO migration_rejects (source_table, source_id, reason, payload)
SELECT 'legacy_customers', c.id, 'DUPLICATE_EMAIL_MERGED', 'merged_into=' || CAST(s.id AS VARCHAR(20)) || ';email=' || c.email
FROM legacy_customers c
JOIN customers s ON s.email_normalized = LOWER(TRIM(c.email))
WHERE c.id <> s.id;

INSERT INTO legacy_customer_map (legacy_customer_id, customer_id)
SELECT c.id, s.id
FROM legacy_customers c
JOIN customers s ON s.email_normalized = LOWER(TRIM(c.email));

INSERT INTO products (id, sku, name, price)
SELECT id, UPPER(TRIM(sku)), TRIM(name), CAST(TRIM(price) AS NUMERIC(10,2))
FROM legacy_products;

-- R3
INSERT INTO migration_rejects (source_table, source_id, reason, payload)
SELECT 'legacy_orders', o.id, 'CUSTOMER_NOT_MIGRATED', 'customer_id=' || COALESCE(CAST(o.customer_id AS VARCHAR(20)), 'null')
FROM legacy_orders o
WHERE NOT EXISTS (SELECT 1 FROM legacy_customer_map m WHERE m.legacy_customer_id = o.customer_id);

-- R4
INSERT INTO migration_rejects (source_table, source_id, reason, payload)
SELECT 'legacy_orders', o.id, 'INVALID_ITEM_QUANTITY', 'total=' || o.total
FROM legacy_orders o
WHERE EXISTS (SELECT 1 FROM legacy_order_items i WHERE i.order_id = o.id AND (i.quantity IS NULL OR i.quantity <= 0))
  AND NOT EXISTS (SELECT 1 FROM migration_rejects r WHERE r.source_table = 'legacy_orders' AND r.source_id = o.id);

INSERT INTO orders (id, customer_id, order_date, status, total)
SELECT o.id, m.customer_id, CAST(TRIM(o.order_date) AS TIMESTAMP), UPPER(TRIM(o.status)), CAST(TRIM(o.total) AS NUMERIC(10,2))
FROM legacy_orders o
JOIN legacy_customer_map m ON m.legacy_customer_id = o.customer_id
WHERE NOT EXISTS (SELECT 1 FROM migration_rejects r WHERE r.source_table = 'legacy_orders' AND r.source_id = o.id);

-- R5
INSERT INTO migration_rejects (source_table, source_id, reason, payload)
SELECT 'legacy_order_items', i.id, 'ORPHAN_ORDER_ITEM', 'order_id=' || COALESCE(CAST(i.order_id AS VARCHAR(20)), 'null')
FROM legacy_order_items i
WHERE NOT EXISTS (SELECT 1 FROM legacy_orders o WHERE o.id = i.order_id);

-- R6
INSERT INTO migration_rejects (source_table, source_id, reason, payload)
SELECT 'legacy_order_items', i.id, 'PARENT_ORDER_REJECTED', 'order_id=' || CAST(i.order_id AS VARCHAR(20))
FROM legacy_order_items i
WHERE EXISTS (SELECT 1 FROM legacy_orders o WHERE o.id = i.order_id)
  AND NOT EXISTS (SELECT 1 FROM orders n WHERE n.id = i.order_id);

INSERT INTO order_items (id, order_id, product_id, quantity, unit_price)
SELECT i.id, i.order_id, i.product_id, i.quantity, CAST(TRIM(i.unit_price) AS NUMERIC(10,2))
FROM legacy_order_items i
WHERE EXISTS (SELECT 1 FROM orders n WHERE n.id = i.order_id);
