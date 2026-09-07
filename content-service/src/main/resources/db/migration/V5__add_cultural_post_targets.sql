ALTER TABLE content_posts ADD COLUMN target_type VARCHAR(20);
ALTER TABLE content_posts ADD COLUMN target_id UUID;
ALTER TABLE content_posts ADD CONSTRAINT ck_content_posts_cultural_target_pair
    CHECK ((target_type IS NULL AND target_id IS NULL) OR (target_type IN ('PROVERB', 'RECIPE') AND target_id IS NOT NULL));
CREATE INDEX idx_content_posts_cultural_target ON content_posts(target_type, target_id) WHERE target_id IS NOT NULL;
