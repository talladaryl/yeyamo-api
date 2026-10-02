CREATE TABLE interaction_post_views (
    post_id UUID NOT NULL,
    viewer_id VARCHAR(100) NOT NULL,
    viewed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (post_id, viewer_id)
);
CREATE INDEX idx_interaction_post_views_post ON interaction_post_views(post_id);
