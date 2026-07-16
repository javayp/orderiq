CREATE TABLE IF NOT EXISTS orders (
    order_id TEXT PRIMARY KEY,
    customer_id TEXT NOT NULL,
    order_date TEXT NOT NULL,
    amount_usd NUMERIC NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders (customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_order_date ON orders (order_date);

CREATE TABLE IF NOT EXISTS order_dataset_state (
    state_id INTEGER PRIMARY KEY CHECK (state_id = 1),
    revision INTEGER NOT NULL
);

INSERT OR IGNORE INTO order_dataset_state (state_id, revision) VALUES (1, 0);
