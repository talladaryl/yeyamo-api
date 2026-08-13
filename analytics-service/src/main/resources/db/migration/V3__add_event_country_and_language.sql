ALTER TABLE analytics_event_store ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
ALTER TABLE analytics_event_store ADD COLUMN IF NOT EXISTS language_code VARCHAR(10);
CREATE INDEX IF NOT EXISTS idx_analytics_event_store_country_time
    ON analytics_event_store(country_code, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_analytics_event_store_language_time
    ON analytics_event_store(language_code, occurred_at DESC);

CREATE TABLE IF NOT EXISTS analytics_country_backfill_audit (
    event_id UUID PRIMARY KEY,
    reason VARCHAR(160) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO analytics_country_backfill_audit(event_id, reason)
SELECT event_id, 'AMBIGUOUS_COUNTRY_NO_RELIABLE_SOURCE'
FROM analytics_event_store WHERE country_code IS NULL
ON CONFLICT (event_id) DO NOTHING;
