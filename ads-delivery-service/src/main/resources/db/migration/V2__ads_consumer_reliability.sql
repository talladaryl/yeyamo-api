CREATE TABLE ads_processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ads_outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    payload TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000)
);
CREATE INDEX idx_ads_outbox_pending
  ON ads_outbox_events(occurred_at) WHERE published_at IS NULL;

ALTER TABLE campaign_projections
    ALTER COLUMN start_at TYPE TIMESTAMPTZ USING start_at AT TIME ZONE 'UTC',
    ALTER COLUMN end_at TYPE TIMESTAMPTZ USING end_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE ad_delivery_records
    ALTER COLUMN impression_at TYPE TIMESTAMPTZ USING impression_at AT TIME ZONE 'UTC',
    ALTER COLUMN clicked_at TYPE TIMESTAMPTZ USING clicked_at AT TIME ZONE 'UTC',
    ALTER COLUMN converted_at TYPE TIMESTAMPTZ USING converted_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
