CREATE TABLE admin_processed_events (
 event_id UUID PRIMARY KEY,
 event_type VARCHAR(100) NOT NULL,
 processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE admin_outbox_events (
 id UUID PRIMARY KEY,
 aggregate_id UUID NOT NULL,
 event_type VARCHAR(100) NOT NULL,
 payload TEXT NOT NULL,
 occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
 published_at TIMESTAMP WITH TIME ZONE,
 attempts INTEGER NOT NULL DEFAULT 0,
 last_error VARCHAR(1000)
);
CREATE INDEX idx_admin_outbox_unpublished ON admin_outbox_events(occurred_at) WHERE published_at IS NULL;
