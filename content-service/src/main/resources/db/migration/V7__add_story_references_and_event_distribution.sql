ALTER TABLE stories ADD COLUMN IF NOT EXISTS reference_type VARCHAR(32) NOT NULL DEFAULT 'NONE';
ALTER TABLE stories ADD COLUMN IF NOT EXISTS reference_id VARCHAR(100);
CREATE INDEX IF NOT EXISTS idx_stories_reference
    ON stories(reference_type, reference_id) WHERE reference_type <> 'NONE';

CREATE TABLE IF NOT EXISTS content_event_social_distributions (
    event_id UUID PRIMARY KEY,
    source_event_id UUID NOT NULL UNIQUE,
    organizer_user_id VARCHAR(100) NOT NULL,
    feed_requested BOOLEAN NOT NULL,
    story_requested BOOLEAN NOT NULL,
    feed_status VARCHAR(40) NOT NULL,
    story_status VARCHAR(40) NOT NULL,
    post_id UUID,
    story_id UUID,
    feed_error VARCHAR(1000),
    story_error VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_content_event_social_distribution_post
    ON content_event_social_distributions(post_id) WHERE post_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_content_event_social_distribution_story
    ON content_event_social_distributions(story_id) WHERE story_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS content_processed_event_receipts (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);
