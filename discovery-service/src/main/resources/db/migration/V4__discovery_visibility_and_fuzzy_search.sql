-- Additive search projection improvements. Existing rows remain public by default.
ALTER TABLE discovery_documents
    ADD COLUMN IF NOT EXISTS launch_visibility VARCHAR(30) NOT NULL DEFAULT 'PUBLIC',
    ADD COLUMN IF NOT EXISTS aliases TEXT;

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_discovery_visibility
    ON discovery_documents (launch_visibility, active)
    WHERE active = TRUE;

CREATE INDEX IF NOT EXISTS idx_discovery_title_trgm
    ON discovery_documents USING GIN (title gin_trgm_ops)
    WHERE active = TRUE;
