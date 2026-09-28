-- =========================================================================
-- Flyway Migration V9: Ranking & Query Performance Indexes
-- =========================================================================

-- 1. Composite index for merchant-isolated price history queries ordered by time
CREATE INDEX IF NOT EXISTS idx_price_history_prod_merch_time 
    ON product_price_history (product_id, merchant, recorded_at DESC);

-- 2. Fast lookup for active price alerts per user and per product
CREATE INDEX IF NOT EXISTS idx_price_alerts_user_active 
    ON price_alerts (user_id, active);

CREATE INDEX IF NOT EXISTS idx_price_alerts_prod_active 
    ON price_alerts (product_id, active);

-- 3. Composite index for brand & category faceted product queries
CREATE INDEX IF NOT EXISTS idx_products_brand_category 
    ON products (brand, category);
