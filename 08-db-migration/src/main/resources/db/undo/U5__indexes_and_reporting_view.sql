-- Undo V5 (Flyway Community has no `undo`; these are applied by the rollback runbook / RollbackTest)
DROP VIEW IF EXISTS v_customer_order_totals;
DROP INDEX IF EXISTS ix_order_items_product;
DROP INDEX IF EXISTS ix_order_items_order;
DROP INDEX IF EXISTS ix_orders_customer;
DELETE FROM flyway_schema_history WHERE version = '5';
