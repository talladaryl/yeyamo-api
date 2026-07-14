CREATE TABLE media_assets (
 id UUID PRIMARY KEY, owner_id VARCHAR(100) NOT NULL, type VARCHAR(20) NOT NULL, status VARCHAR(20) NOT NULL,
 thumbnail_status VARCHAR(20) NOT NULL, original_filename VARCHAR(255) NOT NULL, content_type VARCHAR(100) NOT NULL,
 size_bytes BIGINT NOT NULL CHECK(size_bytes > 0), checksum VARCHAR(64) NOT NULL, storage_key VARCHAR(500) NOT NULL UNIQUE,
 thumbnail_key VARCHAR(500), width INTEGER, height INTEGER, duration_ms BIGINT, alt_text VARCHAR(500),
 aggregate_type VARCHAR(80), aggregate_id VARCHAR(100), failure_reason VARCHAR(1000), created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL, deleted_at TIMESTAMPTZ, version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_media_owner_checksum_active ON media_assets(owner_id,checksum) WHERE status <> 'DELETED';
CREATE INDEX idx_media_aggregate ON media_assets(aggregate_type,aggregate_id);
CREATE INDEX idx_media_status_created ON media_assets(status,created_at);

CREATE TABLE media_outbox (
 id UUID PRIMARY KEY, aggregate_id VARCHAR(100) NOT NULL, event_type VARCHAR(100) NOT NULL, payload TEXT NOT NULL,
 occurred_at TIMESTAMPTZ NOT NULL, published_at TIMESTAMPTZ, attempts INTEGER NOT NULL DEFAULT 0, last_error VARCHAR(1000)
);
CREATE INDEX idx_media_outbox_pending ON media_outbox(occurred_at) WHERE published_at IS NULL;
