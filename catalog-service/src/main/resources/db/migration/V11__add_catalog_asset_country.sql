ALTER TABLE catalog_assets ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
CREATE INDEX IF NOT EXISTS idx_catalog_assets_country ON catalog_assets(country_code, status);

CREATE TABLE IF NOT EXISTS catalog_asset_country_backfill_audit (
    asset_id UUID PRIMARY KEY,
    reason VARCHAR(160) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO catalog_asset_country_backfill_audit(asset_id, reason)
SELECT id, 'AMBIGUOUS_COUNTRY_NO_RELIABLE_SOURCE'
FROM catalog_assets WHERE country_code IS NULL
ON CONFLICT (asset_id) DO NOTHING;
