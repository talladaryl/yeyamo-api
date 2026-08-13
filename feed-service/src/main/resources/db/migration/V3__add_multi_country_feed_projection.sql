ALTER TABLE feed_posts ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
ALTER TABLE feed_posts ADD COLUMN IF NOT EXISTS language_code VARCHAR(10);
CREATE INDEX IF NOT EXISTS idx_feed_posts_country_language
    ON feed_posts(country_code, language_code, published_at DESC) WHERE available = true;

CREATE TABLE IF NOT EXISTS feed_country_backfill_audit (
    post_id UUID PRIMARY KEY,
    reason VARCHAR(160) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO feed_country_backfill_audit(post_id, reason)
SELECT post_id, 'AMBIGUOUS_COUNTRY_NO_RELIABLE_SOURCE'
FROM feed_posts WHERE country_code IS NULL
ON CONFLICT (post_id) DO NOTHING;
