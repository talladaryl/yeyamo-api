CREATE TABLE moderation_reportable_targets (
    target_key VARCHAR(180) PRIMARY KEY,
    target_type VARCHAR(40) NOT NULL,
    target_id VARCHAR(120) NOT NULL,
    owner_id VARCHAR(120),
    available BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_moderation_reportable_target_lookup ON moderation_reportable_targets(target_type, target_id);
