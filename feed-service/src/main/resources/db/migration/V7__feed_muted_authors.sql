CREATE TABLE feed_muted_authors (
    id UUID PRIMARY KEY,
    viewer_id VARCHAR(120) NOT NULL,
    author_id VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_feed_muted_author UNIQUE(viewer_id, author_id)
);
CREATE INDEX idx_feed_muted_authors_viewer ON feed_muted_authors(viewer_id);
