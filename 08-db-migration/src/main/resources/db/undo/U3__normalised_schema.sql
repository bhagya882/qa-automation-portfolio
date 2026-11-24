-- Undo V3: drop the target schema (children first)
DROP TABLE IF EXISTS migration_rejects;
DROP TABLE IF EXISTS legacy_customer_map;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS customers;
DELETE FROM flyway_schema_history WHERE version = '3';
