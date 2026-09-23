-- A user has at most one row for each toggle interaction.  Feedback service
-- transitions the two feedback values atomically so only one remains ACTIVE.
CREATE UNIQUE INDEX IF NOT EXISTS uk_generic_interaction_toggle_all
    ON generic_interactions(target_type, target_id, user_id, interaction_type)
    WHERE interaction_type IN ('LIKE', 'FAVORITE', 'FOLLOW', 'INTERESTED', 'NOT_INTERESTED');

CREATE INDEX IF NOT EXISTS idx_generic_interaction_user_favorites
    ON generic_interactions(user_id, created_at DESC)
    WHERE interaction_type = 'FAVORITE' AND status = 'ACTIVE';
