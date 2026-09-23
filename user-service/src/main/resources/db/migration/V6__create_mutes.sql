CREATE TABLE mutes (
    muter_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    muted_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (muter_id, muted_id),
    CONSTRAINT chk_no_self_mute CHECK (muter_id <> muted_id)
);
CREATE INDEX idx_mutes_muter ON mutes(muter_id);
