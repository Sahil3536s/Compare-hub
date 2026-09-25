-- =========================================================================
-- Flyway Migration V8: Saved Products Pricing & Activity Telemetry Schema
-- =========================================================================

-- 1. Add saved price and merchant columns to saved_products
ALTER TABLE saved_products ADD COLUMN IF NOT EXISTS saved_price NUMERIC(12, 2);
ALTER TABLE saved_products ADD COLUMN IF NOT EXISTS saved_merchant VARCHAR(100);

-- 2. Add activity metadata and redirect url to search_histories
ALTER TABLE search_histories ADD COLUMN IF NOT EXISTS details VARCHAR(500);
ALTER TABLE search_histories ADD COLUMN IF NOT EXISTS target_url VARCHAR(500);

-- 3. Performance indexes for user-isolated queries
CREATE INDEX IF NOT EXISTS idx_saved_products_user_created ON saved_products(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_search_histories_user_created ON search_histories(user_id, created_at DESC);
