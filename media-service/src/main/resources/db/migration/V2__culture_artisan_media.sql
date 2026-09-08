-- V2: Culture & Artisan media support
-- Extends media_assets with usage type, rights, consent and quota fields.
-- All columns are nullable so existing rows are unaffected.

-- -----------------------------------------------------------------------
-- 1. Type & usage
-- -----------------------------------------------------------------------
-- Extend the CHECK constraint on type to include new values.
-- PostgreSQL requires dropping and recreating the constraint.
ALTER TABLE media_assets DROP CONSTRAINT IF EXISTS media_assets_type_check;
ALTER TABLE media_assets
    ADD CONSTRAINT media_assets_type_check
    CHECK (type IN ('IMAGE','VIDEO','AUDIO','DOCUMENT','CERTIFICATE'));

ALTER TABLE media_assets
    ADD COLUMN IF NOT EXISTS usage_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS duration_seconds BIGINT,
    ADD COLUMN IF NOT EXISTS waveform_json    TEXT;

-- -----------------------------------------------------------------------
-- 2. Rights & licensing
-- -----------------------------------------------------------------------
ALTER TABLE media_assets
    ADD COLUMN IF NOT EXISTS copyright_owner   VARCHAR(300),
    ADD COLUMN IF NOT EXISTS license_type      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS usage_permission  VARCHAR(200),
    ADD COLUMN IF NOT EXISTS attribution_required BOOLEAN;

-- -----------------------------------------------------------------------
-- 3. Consent for third-party persons
-- -----------------------------------------------------------------------
ALTER TABLE media_assets
    ADD COLUMN IF NOT EXISTS consent_status    VARCHAR(20),
    ADD COLUMN IF NOT EXISTS consent_record_id VARCHAR(100);

ALTER TABLE media_assets DROP CONSTRAINT IF EXISTS chk_consent_status;
ALTER TABLE media_assets ADD CONSTRAINT chk_consent_status CHECK (
        consent_status IS NULL OR
        consent_status IN ('NOT_REQUIRED','OBTAINED','REFUSED','PENDING')
    );

-- -----------------------------------------------------------------------
-- 4. Signed URL support
-- -----------------------------------------------------------------------
ALTER TABLE media_assets
    ADD COLUMN IF NOT EXISTS signed_url_expires_at TIMESTAMPTZ;

-- -----------------------------------------------------------------------
-- 5. Quota bucket key (for fast per-user usage reporting)
-- -----------------------------------------------------------------------
ALTER TABLE media_assets
    ADD COLUMN IF NOT EXISTS quota_bucket_key VARCHAR(200);

-- -----------------------------------------------------------------------
-- 6. Indexes
-- -----------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_media_usage_type
    ON media_assets (usage_type) WHERE status <> 'DELETED';

CREATE INDEX IF NOT EXISTS idx_media_type_owner
    ON media_assets (type, owner_id, created_at DESC) WHERE status <> 'DELETED';

CREATE INDEX IF NOT EXISTS idx_media_consent
    ON media_assets (consent_status) WHERE consent_status IS NOT NULL;

COMMENT ON COLUMN media_assets.usage_type          IS 'Controlled vocabulary: MediaUsageType enum';
COMMENT ON COLUMN media_assets.copyright_owner     IS 'Name of the copyright holder';
COMMENT ON COLUMN media_assets.license_type        IS 'e.g. CC-BY-4.0, ALL_RIGHTS_RESERVED';
COMMENT ON COLUMN media_assets.usage_permission    IS 'Permitted use description';
COMMENT ON COLUMN media_assets.attribution_required IS 'True if attribution to the author is required';
COMMENT ON COLUMN media_assets.consent_status      IS 'Consent status for third-party persons in the media';
COMMENT ON COLUMN media_assets.consent_record_id   IS 'Reference to external consent management record';
COMMENT ON COLUMN media_assets.waveform_json        IS 'JSON array of amplitude values for audio waveform display';
COMMENT ON COLUMN media_assets.signed_url_expires_at IS 'When the last issued signed URL expires';
