-- Test-only migration used by TransactionRollbackTest: two valid rows, then one that violates ck_products_price.
-- The whole migration must roll back, leaving neither of the first two rows behind.
INSERT INTO products (id, sku, name, price) VALUES (9001, 'SKU-9001', 'Batch item 1', 10.00);
INSERT INTO products (id, sku, name, price) VALUES (9002, 'SKU-9002', 'Batch item 2', 20.00);
INSERT INTO products (id, sku, name, price) VALUES (9003, 'SKU-9003', 'Batch item 3 (invalid)', -1.00);
