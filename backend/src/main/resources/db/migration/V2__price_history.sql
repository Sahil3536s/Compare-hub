-- Flyway Migration: V2__price_history.sql
-- Table: product_price_history

CREATE TABLE IF NOT EXISTS product_price_history (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    merchant VARCHAR(100) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_price_history_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_price_history_prod_time ON product_price_history (product_id, recorded_at DESC);
CREATE INDEX IF NOT EXISTS idx_price_history_prod_merchant ON product_price_history (product_id, merchant);
