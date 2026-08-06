CREATE TABLE IF NOT EXISTS orders (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL,
    item_name       VARCHAR(255) NOT NULL,
    amount          NUMERIC(12, 2) NOT NULL,
    currency        VARCHAR(10) NOT NULL DEFAULT 'USD',
    status          VARCHAR(50) NOT NULL,
    payment_id      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders (user_id);
