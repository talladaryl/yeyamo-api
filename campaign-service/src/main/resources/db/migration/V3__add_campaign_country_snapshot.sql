-- `target_configuration` can target several countries; `country_code` is the
-- campaign owner/market snapshot when it is explicitly known.
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
CREATE INDEX IF NOT EXISTS idx_campaigns_country_status ON campaigns(country_code, status);

CREATE TABLE IF NOT EXISTS campaign_country_migration_audit (
    campaign_id UUID PRIMARY KEY,
    reason VARCHAR(160) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO campaign_country_migration_audit(campaign_id, reason)
SELECT id, 'AMBIGUOUS_COUNTRY_TARGET_CONFIGURATION_REQUIRES_REVIEW'
FROM campaigns WHERE country_code IS NULL
ON CONFLICT (campaign_id) DO NOTHING;
