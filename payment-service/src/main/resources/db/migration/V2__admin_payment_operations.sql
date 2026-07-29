ALTER TABLE payments ADD COLUMN IF NOT EXISTS partner_id VARCHAR(120);
CREATE INDEX IF NOT EXISTS idx_payments_partner_created ON payments(partner_id,created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payments_provider_created ON payments(provider,created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payments_currency_created ON payments(currency,created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payments_booking ON payments(booking_id);
CREATE TABLE payment_reconciliation_jobs(id UUID PRIMARY KEY,status VARCHAR(30) NOT NULL,requested_by VARCHAR(120) NOT NULL,correlation_id VARCHAR(120),created_at TIMESTAMPTZ NOT NULL,completed_at TIMESTAMPTZ,summary JSONB NOT NULL DEFAULT '{}'::jsonb);
