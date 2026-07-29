ALTER TABLE reviews ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS deletion_reason VARCHAR(1000);
ALTER TABLE interaction_comments ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE interaction_comments ADD COLUMN IF NOT EXISTS deletion_reason VARCHAR(1000);
ALTER TABLE interaction_comments ADD COLUMN IF NOT EXISTS locked BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE content_moderation_audit(id UUID PRIMARY KEY,content_type VARCHAR(20) NOT NULL,content_id UUID NOT NULL,action VARCHAR(60) NOT NULL,actor_id VARCHAR(100) NOT NULL,reason VARCHAR(1000),correlation_id VARCHAR(120),created_at TIMESTAMPTZ NOT NULL);
CREATE INDEX idx_content_moderation_audit_content ON content_moderation_audit(content_type,content_id,created_at DESC);
