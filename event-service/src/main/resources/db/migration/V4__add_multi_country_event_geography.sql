ALTER TABLE events ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
ALTER TABLE events ADD COLUMN IF NOT EXISTS admin_level_1_id UUID;
ALTER TABLE events ADD COLUMN IF NOT EXISTS admin_level_2_id UUID;
ALTER TABLE events ADD COLUMN IF NOT EXISTS country_city_id UUID;
ALTER TABLE events ADD COLUMN IF NOT EXISTS locality_id UUID;
ALTER TABLE events ADD COLUMN IF NOT EXISTS latitude DECIMAL(9,6);
ALTER TABLE events ADD COLUMN IF NOT EXISTS longitude DECIMAL(9,6);
ALTER TABLE events ADD COLUMN IF NOT EXISTS language_code VARCHAR(10);
ALTER TABLE events ADD COLUMN IF NOT EXISTS is_virtual BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE events ALTER COLUMN place_id DROP NOT NULL;
CREATE TABLE IF NOT EXISTS event_accessible_countries (
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    country_code VARCHAR(2) NOT NULL,
    PRIMARY KEY (event_id, country_code)
);
CREATE INDEX IF NOT EXISTS idx_events_country_city ON events(country_code, country_city_id);

CREATE TABLE IF NOT EXISTS event_country_migration_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), event_id UUID NOT NULL,
    reason VARCHAR(100) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (event_id, reason)
);
INSERT INTO event_country_migration_audit (event_id, reason)
SELECT id, 'AMBIGUOUS_COUNTRY_LEGACY_EVENT' FROM events WHERE country_code IS NULL
ON CONFLICT (event_id, reason) DO NOTHING;
