CREATE TABLE recommendation_feedback (
    id UUID PRIMARY KEY,
    user_id VARCHAR(120) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id VARCHAR(120) NOT NULL,
    feedback_type VARCHAR(20) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_recommendation_feedback UNIQUE(user_id, target_type, target_id),
    CONSTRAINT ck_recommendation_feedback_type CHECK(feedback_type IN ('INTERESTED', 'NOT_INTERESTED'))
);
CREATE INDEX idx_recommendation_feedback_user_target ON recommendation_feedback(user_id, target_id);
