-- Expand phase only. No country is inferred from XAF, language or a legacy partner id.
ALTER TABLE commerce_promotions ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
ALTER TABLE commerce_commission_rules ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
ALTER TABLE commerce_orders ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
ALTER TABLE commerce_ledger ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
ALTER TABLE commerce_refunds ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
ALTER TABLE commerce_invoices ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);

CREATE INDEX IF NOT EXISTS idx_commerce_orders_country_created
    ON commerce_orders(country_code, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_commerce_ledger_country_occurred
    ON commerce_ledger(country_code, occurred_at DESC);

CREATE TABLE IF NOT EXISTS commerce_country_migration_audit (
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

INSERT INTO commerce_country_migration_audit(aggregate_type, aggregate_id, reason)
SELECT 'ORDER', id::text, 'AMBIGUOUS_COUNTRY_NO_AUTHORITATIVE_ORDER_SNAPSHOT'
FROM commerce_orders WHERE country_code IS NULL
ON CONFLICT (aggregate_type, aggregate_id, reason) DO NOTHING;
