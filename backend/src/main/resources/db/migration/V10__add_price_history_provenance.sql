-- Migration V10: Add structured provenance to product_price_history
ALTER TABLE product_price_history ADD COLUMN IF NOT EXISTS data_source VARCHAR(30) DEFAULT 'UNKNOWN';
ALTER TABLE product_price_history ADD COLUMN IF NOT EXISTS is_live BOOLEAN DEFAULT false;
ALTER TABLE product_price_history ADD COLUMN IF NOT EXISTS provenance VARCHAR(50) DEFAULT 'UNKNOWN';

CREATE INDEX IF NOT EXISTS idx_price_history_provenance ON product_price_history (product_id, is_live, data_source);
