# Database migration validation report

- Generated: 2026-11-23T00:28:54.541047+05:30[Asia/Kolkata]
- Database: jdbc:h2:mem:qa;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1
- Checks: 76, passed: 76, failed: 0

| # | Category | Check | Expected | Actual | Result |
|---|---|---|---|---|---|
| 1 | setup | migrations applied on mig_main | (info) | 5 (target version 5) | PASS |
| 2 | data types | customers.id | integer null=NO | integer null=NO | PASS |
| 3 | data types | customers.email | character varying null=NO | character varying null=NO | PASS |
| 4 | data types | customers.email_normalized | character varying null=NO | character varying null=NO | PASS |
| 5 | data types | customers.created_at | timestamp null=NO | timestamp null=NO | PASS |
| 6 | data types | customers.phone | character varying null=YES | character varying null=YES | PASS |
| 7 | data types | products.price | numeric(10,2) null=NO | numeric(10,2) null=NO | PASS |
| 8 | data types | products.sku | character varying null=NO | character varying null=NO | PASS |
| 9 | data types | orders.customer_id | integer null=NO | integer null=NO | PASS |
| 10 | data types | orders.order_date | timestamp null=NO | timestamp null=NO | PASS |
| 11 | data types | orders.total | numeric(10,2) null=NO | numeric(10,2) null=NO | PASS |
| 12 | data types | orders.status | character varying null=NO | character varying null=NO | PASS |
| 13 | data types | order_items.quantity | integer null=NO | integer null=NO | PASS |
| 14 | data types | order_items.unit_price | numeric(10,2) null=NO | numeric(10,2) null=NO | PASS |
| 15 | data types | migration_rejects.reason | character varying null=NO | character varying null=NO | PASS |
| 16 | constraints | NOT NULL email | rejected | rejected | PASS |
| 17 | constraints | UNIQUE email (case-insensitive) | rejected | rejected | PASS |
| 18 | constraints | CHECK email_normalized = lower(email) | rejected | rejected | PASS |
| 19 | constraints | CHECK email format | rejected | rejected | PASS |
| 20 | constraints | CHECK price >= 0 | rejected | rejected | PASS |
| 21 | constraints | UNIQUE sku | rejected | rejected | PASS |
| 22 | constraints | NUMERIC(10,2) overflow | rejected | rejected | PASS |
| 23 | constraints | FK order -> customer | rejected | rejected | PASS |
| 24 | constraints | CHECK status domain | rejected | rejected | PASS |
| 25 | constraints | CHECK total >= 0 | rejected | rejected | PASS |
| 26 | constraints | CHECK quantity > 0 | rejected | rejected | PASS |
| 27 | constraints | FK item -> order | rejected | rejected | PASS |
| 28 | constraints | FK item -> product | rejected | rejected | PASS |
| 29 | constraints | FK parent delete restricted | rejected | rejected | PASS |
| 30 | constraints | valid customer/order/item accepted (control) | accepted | accepted | PASS |
| 31 | legacy profiling | legacy duplicate emails (case/space-insensitive) | 3 | 3 | PASS |
| 32 | legacy profiling | legacy customers with NULL/blank email | 2 | 2 | PASS |
| 33 | legacy profiling | legacy orphan order items | 4 | 4 | PASS |
| 34 | legacy profiling | legacy items with quantity <= 0 | 3 | 3 | PASS |
| 35 | legacy profiling | legacy money values with stray whitespace | 4 | 4 | PASS |
| 36 | duplicates | customers with duplicate normalised email | 0 | 0 | PASS |
| 37 | duplicates | products with duplicate sku | 0 | 0 | PASS |
| 38 | duplicates | duplicate product lines within an order | 0 | 0 | PASS |
| 39 | referential integrity | orders without customer | 0 | 0 | PASS |
| 40 | referential integrity | items without order | 0 | 0 | PASS |
| 41 | referential integrity | items without product | 0 | 0 | PASS |
| 42 | referential integrity | orders without any item | 0 | 0 | PASS |
| 43 | referential integrity | legacy customers not mapped and not rejected | 0 | 0 | PASS |
| 44 | data rules | emails not trimmed | 0 | 0 | PASS |
| 45 | data rules | status outside domain | 0 | 0 | PASS |
| 46 | totals | orders whose total != sum(quantity * unit_price) | 0 | 0 | PASS |
| 47 | row counts | target counts vs oracle | {customers=40, order_items=283, orders=115, products=20} | {customers=40, order_items=283, orders=115, products=20} | PASS |
| 48 | record level | legacy customer 41 -> customer | 1 | 1 | PASS |
| 49 | record level | orders 1 and 6 re-pointed to customer 1 | 2 | 2 | PASS |
| 50 | totals | legacy grand total = migrated + rejected | 50709.85 | 50709.85 | PASS |
| 51 | totals | migrated grand total vs oracle | 49662.15 | 49662.15 | PASS |
| 52 | rejects | rejects by reason vs oracle | {legacy_customers/DUPLICATE_EMAIL_MERGED=3, legacy_customers/NULL_EMAIL=2, legacy_order_items/ORPHAN_ORDER_ITEM=4, legacy_order_items/PARENT_ORDER_REJECTED=11, legacy_orders/CUSTOMER_NOT_MIGRATED=2, legacy_orders/INVALID_ITEM_QUANTITY=3} | {legacy_customers/DUPLICATE_EMAIL_MERGED=3, legacy_customers/NULL_EMAIL=2, legacy_order_items/ORPHAN_ORDER_ITEM=4, legacy_order_items/PARENT_ORDER_REJECTED=11, legacy_orders/CUSTOMER_NOT_MIGRATED=2, legacy_orders/INVALID_ITEM_QUANTITY=3} | PASS |
| 53 | row counts | legacy_customers = customers (40) + rejects (5) | 45 | 45 | PASS |
| 54 | row counts | legacy_orders = orders (115) + rejects (5) | 120 | 120 | PASS |
| 55 | row counts | legacy_order_items = order_items (283) + rejects (15) | 298 | 298 | PASS |
| 56 | row counts | legacy_products = products (20) + rejects (0) | 20 | 20 | PASS |
| 57 | record level | order 7 (email|date|status|total|lines) | felipe.cummings21@example.test \| 2024-04-17 13:23:00.0 \| SHIPPED \| 436.71 \| 3 | felipe.cummings21@example.test \| 2024-04-17 13:23:00.0 \| SHIPPED \| 436.71 \| 3 | PASS |
| 58 | record level | order 25 (email|date|status|total|lines) | dominique.jaskolski26@example.test \| 2024-05-05 16:21:00.0 \| SHIPPED \| 172.37 \| 2 | dominique.jaskolski26@example.test \| 2024-05-05 16:21:00.0 \| SHIPPED \| 172.37 \| 2 | PASS |
| 59 | record level | order 50 (email|date|status|total|lines) | sheldon.ortiz36@example.test \| 2024-05-30 16:45:00.0 \| PAID \| 462.09 \| 3 | sheldon.ortiz36@example.test \| 2024-05-30 16:45:00.0 \| PAID \| 462.09 \| 3 | PASS |
| 60 | record level | order 77 (email|date|status|total|lines) | kizzie.wyman28@example.test \| 2024-06-26 14:12:00.0 \| PAID \| 786.52 \| 4 | kizzie.wyman28@example.test \| 2024-06-26 14:12:00.0 \| PAID \| 786.52 \| 4 | PASS |
| 61 | record level | order 120 (email|date|status|total|lines) | yuonne.treutel2@example.test \| 2024-08-08 17:11:00.0 \| PAID \| 1122.06 \| 4 | yuonne.treutel2@example.test \| 2024-08-08 17:11:00.0 \| PAID \| 1122.06 \| 4 | PASS |
| 62 | record level | order 1 (email|date|status|total|lines) | tony.sauer1@example.test \| 2024-04-11 18:45:00.0 \| SHIPPED \| 441.04 \| 3 | tony.sauer1@example.test \| 2024-04-11 18:45:00.0 \| SHIPPED \| 441.04 \| 3 | PASS |
| 63 | totals | per-customer order totals (38 customers) | all equal | all equal | PASS |
| 64 | idempotency | migrations executed on re-run | 0 | 0 | PASS |
| 65 | idempotency | table fingerprints unchanged (8 tables) | {customers=40:984134aadef471b0, products=20:663e9c2272dd291e, orders=115:ab398a97fdc9aa22, order_items=283:2dc69cddd4e79ae5, legacy_customer_map=43:502ba18345cf4d7a, legacy_customers=45:5287012aadf5407a, legacy_orders=120:57a835c6947b4d3e, legacy_order_items=298:8bad05c8e4b98c39} | {customers=40:984134aadef471b0, products=20:663e9c2272dd291e, orders=115:ab398a97fdc9aa22, order_items=283:2dc69cddd4e79ae5, legacy_customer_map=43:502ba18345cf4d7a, legacy_customers=45:5287012aadf5407a, legacy_orders=120:57a835c6947b4d3e, legacy_order_items=298:8bad05c8e4b98c39} | PASS |
| 66 | idempotency | flyway validate (checksums) | valid | valid | PASS |
| 67 | rollback | migrations applied V3..V5 | 3 | 3 | PASS |
| 68 | rollback | tables after undo == V2 tables | [legacy_customers, legacy_order_items, legacy_orders, legacy_products] | [legacy_customers, legacy_order_items, legacy_orders, legacy_products] | PASS |
| 69 | rollback | legacy data fingerprints unchanged | {legacy_customers=45:5287012aadf5407a, legacy_products=20:9eade7ea95f08c9f, legacy_orders=120:57a835c6947b4d3e, legacy_order_items=298:8bad05c8e4b98c39} | {legacy_customers=45:5287012aadf5407a, legacy_products=20:9eade7ea95f08c9f, legacy_orders=120:57a835c6947b4d3e, legacy_order_items=298:8bad05c8e4b98c39} | PASS |
| 70 | rollback | flyway current version after undo | 2 | 2 | PASS |
| 71 | rollback | re-migration after undo executes V3..V5 | 3 | 3 | PASS |
| 72 | rollback | re-migration produces identical counts | {customers=40, products=20, orders=115, order_items=283, migration_rejects=25} | {customers=40, products=20, orders=115, order_items=283, migration_rejects=25} | PASS |
| 73 | transactions | failing V6 migration: rows left behind | 0 | 0 | PASS |
| 74 | transactions | history rows recorded for failed V6 before repair (DB-dependent) | (info) | 1 | PASS |
| 75 | transactions | schema version after failed V6 + repair | 5 | 5 | PASS |
| 76 | transactions | JDBC batch with failing 3rd row: rows left behind | 0 | 0 | PASS |
