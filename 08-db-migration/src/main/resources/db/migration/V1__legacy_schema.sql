-- Legacy e-commerce schema as it exists in production today: loose types (money/dates as text),
-- denormalised columns (email and product name copied into child tables), no foreign keys, no constraints.
CREATE TABLE legacy_customers (
    id          INTEGER PRIMARY KEY,
    email       VARCHAR(255),
    full_name   VARCHAR(255),
    phone       VARCHAR(50),
    created_at  VARCHAR(30)
);

CREATE TABLE legacy_products (
    id     INTEGER PRIMARY KEY,
    sku    VARCHAR(50),
    name   VARCHAR(255),
    price  VARCHAR(20)
);

CREATE TABLE legacy_orders (
    id              INTEGER PRIMARY KEY,
    customer_id     INTEGER,
    customer_email  VARCHAR(255),
    order_date      VARCHAR(30),
    status          VARCHAR(20),
    total           VARCHAR(20)
);

CREATE TABLE legacy_order_items (
    id            INTEGER PRIMARY KEY,
    order_id      INTEGER,
    product_id    INTEGER,
    product_name  VARCHAR(255),
    quantity      INTEGER,
    unit_price    VARCHAR(20)
);
