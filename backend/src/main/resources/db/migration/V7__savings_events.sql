-- =========================================================================
-- V7: Savings Events Schema
-- =========================================================================

CREATE TABLE IF NOT EXISTS savings_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    event_type VARCHAR(50) NOT NULL DEFAULT 'POTENTIAL',
    selected_price NUMERIC(12, 2),
    baseline_price NUMERIC(12, 2),
    saving_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    merchant_or_provider VARCHAR(100),
    notes VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_savings_events_user_id ON savings_events(user_id);
CREATE INDEX IF NOT EXISTS idx_savings_events_event_type ON savings_events(event_type);
CREATE INDEX IF NOT EXISTS idx_savings_events_created_at ON savings_events(created_at DESC);
