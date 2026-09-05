-- =========================================================================
-- V5: User Ranking Preferences Schema
-- =========================================================================

CREATE TABLE IF NOT EXISTS user_ranking_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    preset VARCHAR(50) NOT NULL DEFAULT 'BALANCED',
    
    -- Product Weights (must sum to 100)
    product_price INT NOT NULL DEFAULT 40,
    product_rating INT NOT NULL DEFAULT 20,
    product_discount INT NOT NULL DEFAULT 15,
    product_delivery INT NOT NULL DEFAULT 15,
    product_reliability INT NOT NULL DEFAULT 10,

    -- Flight Weights
    flight_price INT NOT NULL DEFAULT 50,
    flight_duration INT NOT NULL DEFAULT 30,
    flight_stops INT NOT NULL DEFAULT 20,

    -- Ride Weights
    ride_fare INT NOT NULL DEFAULT 60,
    ride_eta INT NOT NULL DEFAULT 40,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_ranking_prefs_user_id ON user_ranking_preferences(user_id);
