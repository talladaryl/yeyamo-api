ALTER TABLE recommendation_candidates ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
ALTER TABLE recommendation_candidates ADD COLUMN IF NOT EXISTS language_code VARCHAR(10);
CREATE INDEX IF NOT EXISTS idx_recommendation_candidates_country_language
    ON recommendation_candidates(country_code, language_code, popularity DESC) WHERE active = true;

CREATE TABLE IF NOT EXISTS recommendation_country_backfill_audit (
    source_id VARCHAR(160) PRIMARY KEY,
    reason VARCHAR(160) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO recommendation_country_backfill_audit(source_id, reason)
SELECT source_id, 'AMBIGUOUS_COUNTRY_NO_RELIABLE_SOURCE'
FROM recommendation_candidates WHERE country_code IS NULL
ON CONFLICT (source_id) DO NOTHING;
