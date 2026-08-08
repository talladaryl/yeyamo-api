-- V3: Culture & Artisan support
-- Adds:
--   1. culture/artisan columns to discovery_documents (PostGIS adapter)
--   2. search_index_rebuild_jobs for per-index reindex scheduling

-- -------------------------------------------------------------------------
-- 1. Extend discovery_documents with culture/artisan metadata
-- -------------------------------------------------------------------------

ALTER TABLE discovery_documents
    ADD COLUMN IF NOT EXISTS country_code      VARCHAR(2),
    ADD COLUMN IF NOT EXISTS admin_level1_id   VARCHAR(120),
    ADD COLUMN IF NOT EXISTS city_id           VARCHAR(120),
    ADD COLUMN IF NOT EXISTS translated_titles TEXT,        -- JSON map langCode->title
    ADD COLUMN IF NOT EXISTS language_codes    VARCHAR(500), -- comma-separated BCP-47
    ADD COLUMN IF NOT EXISTS community         VARCHAR(300),
    ADD COLUMN IF NOT EXISTS tags              TEXT,         -- comma-separated
    ADD COLUMN IF NOT EXISTS materials         TEXT,         -- comma-separated (artwork)
    ADD COLUMN IF NOT EXISTS techniques        TEXT,         -- comma-separated (artwork)
    ADD COLUMN IF NOT EXISTS artisan_id        VARCHAR(120),
    ADD COLUMN IF NOT EXISTS verification_status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS availability_status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS price_min         NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS price_max         NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS popularity_signal DOUBLE PRECISION NOT NULL DEFAULT 0;

-- Update the tsvector generated column to include new text fields.
-- PostgreSQL does not support ALTER on generated columns; drop and recreate.
ALTER TABLE discovery_documents DROP COLUMN IF EXISTS search_vector;
ALTER TABLE discovery_documents
    ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
        to_tsvector('simple',
            coalesce(title,       '') || ' ' ||
            coalesce(description, '') || ' ' ||
            coalesce(city,        '') || ' ' ||
            coalesce(community,   '') || ' ' ||
            coalesce(tags,        '') || ' ' ||
            coalesce(translated_titles, '')
        )
    ) STORED;

-- Recreate the GIN index on the extended tsvector
DROP INDEX IF EXISTS idx_discovery_search;
CREATE INDEX idx_discovery_search ON discovery_documents USING GIN (search_vector);

-- Additional indexes for culture filters
CREATE INDEX IF NOT EXISTS idx_discovery_country       ON discovery_documents (country_code)      WHERE active = TRUE;
CREATE INDEX IF NOT EXISTS idx_discovery_artisan       ON discovery_documents (artisan_id)        WHERE active = TRUE;
CREATE INDEX IF NOT EXISTS idx_discovery_verification  ON discovery_documents (verification_status) WHERE active = TRUE;
CREATE INDEX IF NOT EXISTS idx_discovery_availability  ON discovery_documents (availability_status) WHERE active = TRUE;
CREATE INDEX IF NOT EXISTS idx_discovery_type_country  ON discovery_documents (type, country_code) WHERE active = TRUE;

COMMENT ON COLUMN discovery_documents.translated_titles  IS 'JSON object: {"fr":"Titre","en":"Title",...}';
COMMENT ON COLUMN discovery_documents.language_codes     IS 'Comma-separated BCP-47 language codes';
COMMENT ON COLUMN discovery_documents.popularity_signal  IS 'Aggregated popularity score from views/interactions';

-- -------------------------------------------------------------------------
-- 2. Per-index rebuild jobs (extends existing search_reindex_jobs)
-- -------------------------------------------------------------------------

ALTER TABLE search_reindex_jobs
    ADD COLUMN IF NOT EXISTS target_index VARCHAR(120),
    ADD COLUMN IF NOT EXISTS progress_pct  SMALLINT DEFAULT 0;

-- Drop old unique constraint on running status so multiple indexes can rebuild
DROP INDEX IF EXISTS uk_search_reindex_running;
CREATE UNIQUE INDEX IF NOT EXISTS uk_search_reindex_running
    ON search_reindex_jobs (status, target_index)
    WHERE status IN ('QUEUED','RUNNING');

COMMENT ON COLUMN search_reindex_jobs.target_index IS 'NULL = all indexes; otherwise the specific index name';
COMMENT ON COLUMN search_reindex_jobs.progress_pct  IS '0–100 percentage of documents processed';
