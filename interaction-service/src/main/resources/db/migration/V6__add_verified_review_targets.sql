ALTER TABLE reviews ADD COLUMN IF NOT EXISTS target_type VARCHAR(20) NOT NULL DEFAULT 'PLACE';
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS evidence_reference VARCHAR(160);
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS reported_at TIMESTAMPTZ;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS reported_by VARCHAR(120);
ALTER TABLE reviews DROP CONSTRAINT IF EXISTS uk_review_user_place;
ALTER TABLE reviews ADD CONSTRAINT ck_review_target_type CHECK (target_type IN ('PLACE', 'EXPERIENCE', 'ARTISAN', 'EVENT'));
ALTER TABLE reviews ADD CONSTRAINT ck_review_status CHECK (status IN ('ACTIVE', 'HIDDEN', 'REPORTED', 'DELETED'));
CREATE INDEX IF NOT EXISTS idx_reviews_target_status_created ON reviews(target_type, place_id, status, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uk_review_verified_transaction ON reviews(user_id, target_type, evidence_reference) WHERE evidence_reference IS NOT NULL;
