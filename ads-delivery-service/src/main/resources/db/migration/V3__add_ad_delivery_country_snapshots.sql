-- Target countries remain a multi-value campaign setting. These fields describe
-- the actual country context of a delivered impression/conversion when known.
ALTER TABLE campaign_projections ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
ALTER TABLE ad_delivery_records ADD COLUMN IF NOT EXISTS viewer_country_code VARCHAR(2);
ALTER TABLE ad_conversions ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);

CREATE INDEX IF NOT EXISTS idx_delivery_viewer_country_impression
    ON ad_delivery_records(viewer_country_code, impression_at DESC);
CREATE INDEX IF NOT EXISTS idx_ad_conversions_country_converted
    ON ad_conversions(country_code, converted_at DESC);

CREATE TABLE IF NOT EXISTS ad_delivery_country_migration_audit (
    aggregate_type VARCHAR(40) NOT NULL,
    aggregate_id VARCHAR(120) NOT NULL,
    reason VARCHAR(160) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (aggregate_type, aggregate_id)
);

INSERT INTO ad_delivery_country_migration_audit(aggregate_type, aggregate_id, reason)
SELECT 'CAMPAIGN', campaign_id, 'AMBIGUOUS_COUNTRY_TARGET_COUNTRIES_REQUIRES_REVIEW'
FROM campaign_projections WHERE country_code IS NULL
ON CONFLICT (aggregate_type, aggregate_id) DO NOTHING;
