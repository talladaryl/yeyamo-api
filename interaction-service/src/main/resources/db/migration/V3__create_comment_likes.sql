CREATE TABLE interaction_comment_likes (
    id UUID PRIMARY KEY,
    comment_id UUID NOT NULL REFERENCES interaction_comments(id) ON DELETE CASCADE,
    user_id VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_comment_like_comment_user UNIQUE (comment_id, user_id)
);

CREATE INDEX idx_comment_likes_comment ON interaction_comment_likes(comment_id);
CREATE INDEX idx_comment_likes_user ON interaction_comment_likes(user_id);
