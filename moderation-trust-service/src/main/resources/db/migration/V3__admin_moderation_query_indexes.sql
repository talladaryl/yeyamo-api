CREATE INDEX IF NOT EXISTS idx_moderation_status_priority_created
    ON moderation_reports (status, priority, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_moderation_target_status_created
    ON moderation_reports (target_type, target_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_moderation_assignee_priority_created
    ON moderation_reports (assigned_to, priority, created_at DESC);
