ALTER TABLE partners ADD COLUMN IF NOT EXISTS primary_country_code VARCHAR(2);
CREATE TABLE IF NOT EXISTS partner_operating_countries (
  partner_id UUID NOT NULL REFERENCES partners(id) ON DELETE CASCADE,
  country_code VARCHAR(2) NOT NULL,
  PRIMARY KEY (partner_id, country_code)
);
CREATE INDEX IF NOT EXISTS idx_partners_primary_country ON partners(primary_country_code);
CREATE TABLE IF NOT EXISTS partner_country_migration_audit (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), partner_id UUID NOT NULL, reason VARCHAR(100) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE(partner_id, reason)
);
INSERT INTO partner_country_migration_audit(partner_id, reason)
SELECT id, 'AMBIGUOUS_COUNTRY_NO_VERIFIED_LOCATION' FROM partners WHERE primary_country_code IS NULL
ON CONFLICT (partner_id, reason) DO NOTHING;
