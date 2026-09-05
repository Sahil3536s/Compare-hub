-- =========================================================================
-- V6: User Payment Preferences Schema
-- =========================================================================

CREATE TABLE IF NOT EXISTS user_payment_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    preferred_bank VARCHAR(100) NOT NULL DEFAULT 'ALL',
    preferred_card_type VARCHAR(50) NOT NULL DEFAULT 'ALL',
    has_upi BOOLEAN NOT NULL DEFAULT TRUE,
    has_wallet BOOLEAN NOT NULL DEFAULT FALSE,
    preferred_wallet VARCHAR(100) NOT NULL DEFAULT 'NONE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_payment_prefs_user_id ON user_payment_preferences(user_id);
