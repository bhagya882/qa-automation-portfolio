-- Undo V4: remove migrated data only; legacy_* tables were never modified, so they still hold the source of truth
DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM products;
DELETE FROM legacy_customer_map;
DELETE FROM customers;
DELETE FROM migration_rejects;
DELETE FROM flyway_schema_history WHERE version = '4';
