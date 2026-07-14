CREATE TABLE content_posts (
 id UUID PRIMARY KEY, author_id VARCHAR(100) NOT NULL, caption TEXT, status VARCHAR(20) NOT NULL,
 visibility VARCHAR(20) NOT NULL, catalog_asset_id UUID, created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL, published_at TIMESTAMPTZ, archived_at TIMESTAMPTZ,
 deleted_at TIMESTAMPTZ, version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_content_author_updated ON content_posts(author_id,updated_at DESC) WHERE status <> 'DELETED';
CREATE INDEX idx_content_published ON content_posts(published_at DESC) WHERE status='PUBLISHED' AND visibility='PUBLIC';
CREATE INDEX idx_content_catalog_asset ON content_posts(catalog_asset_id,published_at DESC) WHERE status='PUBLISHED';

CREATE TABLE content_post_media (
 post_id UUID NOT NULL REFERENCES content_posts(id) ON DELETE CASCADE,
 media_id UUID NOT NULL, display_order INTEGER NOT NULL,
 PRIMARY KEY(post_id,display_order), UNIQUE(post_id,media_id)
);
CREATE INDEX idx_content_media_id ON content_post_media(media_id);

CREATE TABLE content_post_hashtags (
 post_id UUID NOT NULL REFERENCES content_posts(id) ON DELETE CASCADE,
 hashtag VARCHAR(50) NOT NULL, PRIMARY KEY(post_id,hashtag)
);
CREATE INDEX idx_content_hashtag ON content_post_hashtags(hashtag);

CREATE TABLE content_outbox (
 id UUID PRIMARY KEY, aggregate_id VARCHAR(100) NOT NULL, event_type VARCHAR(100) NOT NULL,
 payload TEXT NOT NULL, occurred_at TIMESTAMPTZ NOT NULL, published_at TIMESTAMPTZ,
 attempts INTEGER NOT NULL DEFAULT 0, last_error VARCHAR(1000)
);
CREATE INDEX idx_content_outbox_pending ON content_outbox(occurred_at) WHERE published_at IS NULL;
