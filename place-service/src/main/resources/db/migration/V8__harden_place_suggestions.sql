-- country_code intentionally remains nullable for historical V7 rows. The API now requires it for new submissions.
ALTER TABLE place_suggestions ADD COLUMN IF NOT EXISTS administrative_area_id UUID;
ALTER TABLE place_suggestions ADD COLUMN IF NOT EXISTS country_city_id UUID;
ALTER TABLE place_suggestions ADD COLUMN IF NOT EXISTS locality_id UUID;

CREATE INDEX IF NOT EXISTS idx_place_suggestions_country_status_created
    ON place_suggestions(country_code, status, created_at DESC);

-- Historical suggestions keep a NULL dedupe key. New API submissions populate it,
-- allowing the unique index to protect the check/create race without failing on legacy data.
ALTER TABLE place_suggestions ADD COLUMN IF NOT EXISTS dedupe_key VARCHAR(900);
CREATE UNIQUE INDEX IF NOT EXISTS uq_pending_place_suggestion_dedupe_key
    ON place_suggestions(dedupe_key) WHERE dedupe_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS place_suggestion_media (
    id BIGSERIAL PRIMARY KEY,
    suggestion_id UUID NOT NULL REFERENCES place_suggestions(id) ON DELETE CASCADE,
    media_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    content_type VARCHAR(100),
    content_url VARCHAR(500),
    thumbnail_url VARCHAR(500),
    display_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_place_suggestion_media UNIQUE(suggestion_id, media_id),
    CONSTRAINT uq_place_suggestion_media_order UNIQUE(suggestion_id, display_order)
);

CREATE INDEX IF NOT EXISTS idx_place_suggestion_media_suggestion_order
    ON place_suggestion_media(suggestion_id, display_order);

-- Canonical places keep a media-service identifier in addition to the legacy URL field.
ALTER TABLE place_media ADD COLUMN IF NOT EXISTS media_id UUID;
CREATE UNIQUE INDEX IF NOT EXISTS uq_place_media_media_id
    ON place_media(place_id, media_id) WHERE media_id IS NOT NULL;
