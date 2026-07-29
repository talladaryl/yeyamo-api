ALTER TABLE events ADD COLUMN IF NOT EXISTS organizer_id UUID;
ALTER TABLE events ADD COLUMN IF NOT EXISTS partner_id UUID;
ALTER TABLE events ADD COLUMN IF NOT EXISTS region_id BIGINT;
ALTER TABLE events ADD COLUMN IF NOT EXISTS city_id BIGINT;
ALTER TABLE events ADD COLUMN IF NOT EXISTS category_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_events_admin_filters ON events(status,region_id,city_id,start_at);
CREATE INDEX IF NOT EXISTS idx_events_partner_start ON events(partner_id,start_at);
