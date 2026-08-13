-- Expand phase for the legacy region/city/district model.  The existing region
-- country backfill must be provenance-validated before it is used as evidence.
ALTER TABLE cities ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
ALTER TABLE districts ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
ALTER TABLE places ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);

CREATE INDEX IF NOT EXISTS idx_cities_country_region ON cities(country_code, region_id);
CREATE INDEX IF NOT EXISTS idx_districts_country_city ON districts(country_code, city_id);
CREATE INDEX IF NOT EXISTS idx_places_country_city_status ON places(country_code, city_id, status);

CREATE TABLE IF NOT EXISTS place_country_migration_audit (
    entity_type VARCHAR(40) NOT NULL,
    entity_id VARCHAR(120) NOT NULL,
    reason VARCHAR(160) NOT NULL,
    evidence_type VARCHAR(80),
    evidence_id VARCHAR(120),
    migration_batch VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (entity_type, entity_id, reason)
);

INSERT INTO place_country_migration_audit(entity_type, entity_id, reason)
SELECT 'PLACE', id::text, 'AMBIGUOUS_COUNTRY_LEGACY_GEOGRAPHY_REQUIRES_PROVENANCE_REVIEW'
FROM places WHERE country_code IS NULL
ON CONFLICT (entity_type, entity_id, reason) DO NOTHING;
