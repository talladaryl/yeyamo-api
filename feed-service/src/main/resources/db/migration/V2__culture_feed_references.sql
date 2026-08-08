ALTER TABLE feed_posts ADD COLUMN reference_type VARCHAR(32) NOT NULL DEFAULT 'NONE';
ALTER TABLE feed_posts ADD COLUMN reference_id VARCHAR(100);
CREATE INDEX idx_feed_posts_reference ON feed_posts(reference_type,reference_id) WHERE available=true AND reference_type<>'NONE';
