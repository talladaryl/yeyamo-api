ALTER TABLE content_posts ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
ALTER TABLE content_posts ADD COLUMN IF NOT EXISTS admin_level_1_id UUID;
ALTER TABLE content_posts ADD COLUMN IF NOT EXISTS admin_level_2_id UUID;
ALTER TABLE content_posts ADD COLUMN IF NOT EXISTS city_id UUID;
ALTER TABLE content_posts ADD COLUMN IF NOT EXISTS locality_id UUID;
ALTER TABLE content_posts ADD COLUMN IF NOT EXISTS latitude DECIMAL(9,6);
ALTER TABLE content_posts ADD COLUMN IF NOT EXISTS longitude DECIMAL(9,6);
ALTER TABLE content_posts ADD COLUMN IF NOT EXISTS language_code VARCHAR(10);
ALTER TABLE stories ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
ALTER TABLE stories ADD COLUMN IF NOT EXISTS admin_level_1_id UUID;
ALTER TABLE stories ADD COLUMN IF NOT EXISTS admin_level_2_id UUID;
ALTER TABLE stories ADD COLUMN IF NOT EXISTS city_id UUID;
ALTER TABLE stories ADD COLUMN IF NOT EXISTS locality_id UUID;
ALTER TABLE stories ADD COLUMN IF NOT EXISTS latitude DECIMAL(9,6);
ALTER TABLE stories ADD COLUMN IF NOT EXISTS longitude DECIMAL(9,6);
ALTER TABLE stories ADD COLUMN IF NOT EXISTS language_code VARCHAR(10);
CREATE INDEX IF NOT EXISTS idx_content_posts_country_city ON content_posts(country_code, city_id);
CREATE INDEX IF NOT EXISTS idx_stories_country_city ON stories(country_code, city_id);

CREATE TABLE IF NOT EXISTS content_country_migration_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), aggregate_type VARCHAR(20) NOT NULL,
    aggregate_id UUID NOT NULL, reason VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (aggregate_type, aggregate_id, reason)
);
INSERT INTO content_country_migration_audit (aggregate_type, aggregate_id, reason)
SELECT 'POST', id, 'AMBIGUOUS_COUNTRY_NO_VERIFIED_LOCATION' FROM content_posts WHERE country_code IS NULL
ON CONFLICT (aggregate_type, aggregate_id, reason) DO NOTHING;
