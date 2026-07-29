ALTER TABLE moderation_reports ADD COLUMN IF NOT EXISTS priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL';

CREATE INDEX IF NOT EXISTS idx_moderation_target ON moderation_reports(target_type,target_id);
CREATE INDEX IF NOT EXISTS idx_moderation_assignee ON moderation_reports(assigned_to,status,created_at DESC);
CREATE INDEX IF NOT EXISTS idx_moderation_reason ON moderation_reports(reason,created_at DESC);

CREATE TABLE trust_sanctions (
 id UUID PRIMARY KEY,
 subject_id VARCHAR(120) NOT NULL,
 type VARCHAR(40) NOT NULL,
 reason VARCHAR(2000) NOT NULL,
 start_at TIMESTAMPTZ NOT NULL,
 end_at TIMESTAMPTZ,
 actor_id VARCHAR(120) NOT NULL,
 report_id UUID REFERENCES moderation_reports(id),
 created_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT ck_trust_sanction_type CHECK(type IN('WARNING','CONTENT_RESTRICTION','TEMPORARY_SUSPENSION','PERMANENT_SUSPENSION')),
 CONSTRAINT ck_trust_sanction_dates CHECK(end_at IS NULL OR end_at > start_at)
);
CREATE INDEX idx_trust_sanctions_subject ON trust_sanctions(subject_id,created_at DESC);
