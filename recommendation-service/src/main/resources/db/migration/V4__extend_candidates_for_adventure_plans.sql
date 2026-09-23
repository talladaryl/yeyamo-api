ALTER TABLE recommendation_candidates
    ADD COLUMN IF NOT EXISTS price NUMERIC(19,2),
    ADD COLUMN IF NOT EXISTS currency_code VARCHAR(3),
    ADD COLUMN IF NOT EXISTS image_media_id UUID,
    ADD COLUMN IF NOT EXISTS location_label VARCHAR(300),
    ADD COLUMN IF NOT EXISTS starts_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS ends_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_recommendation_candidates_event_window
    ON recommendation_candidates(kind, country_code, starts_at, ends_at)
    WHERE active = true AND kind = 'EVENT';

CREATE INDEX IF NOT EXISTS idx_recommendation_candidates_budget
    ON recommendation_candidates(country_code, currency_code, price)
    WHERE active = true AND price IS NOT NULL;
