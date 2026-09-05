-- Flyway Migration: V4__performance_indexes.sql
-- Optimizing indexes for high-frequency queries and joins

-- 1. Product Search & Categorization Indexes
CREATE INDEX IF NOT EXISTS idx_products_category_name ON products (category, name);
CREATE INDEX IF NOT EXISTS idx_products_brand ON products (brand);

-- 2. Merchant Offer Sorting & Joins
CREATE INDEX IF NOT EXISTS idx_merchant_offers_product_price ON merchant_offers (product_id, price ASC);
CREATE INDEX IF NOT EXISTS idx_merchant_offers_merchant ON merchant_offers (merchant);

-- 3. Notifications Unread Counter & Timeline
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON notifications (user_id, read, created_at DESC);

-- 4. User Personalization & History
CREATE INDEX IF NOT EXISTS idx_saved_products_user_created ON saved_products (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_search_history_user_time ON search_history (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_flight_searches_user_time ON flight_searches (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ride_searches_user_time ON ride_searches (user_id, created_at DESC);
