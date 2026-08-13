-- Payment country is a snapshot of the order/booking context, never a currency inference.
ALTER TABLE payments ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
ALTER TABLE payment_refunds ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
ALTER TABLE payment_webhook_receipts ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);

CREATE INDEX IF NOT EXISTS idx_payments_country_created
    ON payments(country_code, created_at DESC);

CREATE TABLE IF NOT EXISTS payment_country_migration_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(40) NOT NULL,
    aggregate_id VARCHAR(120) NOT NULL,
    reason VARCHAR(160) NOT NULL,
    evidence_type VARCHAR(80),
    evidence_id VARCHAR(120),
    migration_batch VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (aggregate_type, aggregate_id, reason)
);

INSERT INTO payment_country_migration_audit(aggregate_type, aggregate_id, reason)
SELECT 'PAYMENT', id::text, 'AMBIGUOUS_COUNTRY_NO_AUTHORITATIVE_BOOKING_SNAPSHOT'
FROM payments WHERE country_code IS NULL
ON CONFLICT (aggregate_type, aggregate_id, reason) DO NOTHING;
