CREATE TABLE IF NOT EXISTS place_suggestions (
    id UUID PRIMARY KEY,
    submitter_user_id VARCHAR(120) NOT NULL,
    name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    normalized_address VARCHAR(500) NOT NULL,
    description TEXT,
    category_label VARCHAR(120),
    place_type VARCHAR(120),
    region_label VARCHAR(120),
    country_code VARCHAR(2),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    canonical_place_id UUID,
    moderation_reason VARCHAR(1000),
    reviewed_by VARCHAR(120),
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_place_suggestion_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_place_suggestion_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_place_suggestion_longitude CHECK (longitude BETWEEN -180 AND 180)
);
CREATE INDEX IF NOT EXISTS idx_place_suggestions_submitter_created ON place_suggestions(submitter_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_place_suggestions_status_created ON place_suggestions(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_place_suggestions_duplicate ON place_suggestions(status, normalized_name, normalized_address);
