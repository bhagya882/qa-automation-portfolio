CREATE INDEX ix_orders_customer ON orders (customer_id);
CREATE INDEX ix_order_items_order ON order_items (order_id);
CREATE INDEX ix_order_items_product ON order_items (product_id);

CREATE VIEW v_customer_order_totals AS
SELECT c.id AS customer_id, c.email_normalized AS email, COUNT(o.id) AS order_count,
       COALESCE(SUM(o.total), 0) AS total_spent
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id
GROUP BY c.id, c.email_normalized;
