CREATE TABLE newsletter_campaigns (
  id UUID PRIMARY KEY, name VARCHAR(200) NOT NULL, subject VARCHAR(300) NOT NULL,
  preheader VARCHAR(300), content TEXT NOT NULL, status VARCHAR(30) NOT NULL,
  segment_json TEXT NOT NULL, scheduled_at TIMESTAMPTZ, created_by VARCHAR(120) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, dispatch_key UUID,
  version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_newsletter_status_schedule ON newsletter_campaigns(status, scheduled_at);
CREATE INDEX idx_newsletter_created ON newsletter_campaigns(created_at DESC);
CREATE TABLE newsletter_campaign_stats (
  campaign_id UUID PRIMARY KEY REFERENCES newsletter_campaigns(id), delivered BIGINT NOT NULL DEFAULT 0,
  failed BIGINT NOT NULL DEFAULT 0, opened BIGINT NOT NULL DEFAULT 0, clicked BIGINT NOT NULL DEFAULT 0,
  unsubscribed BIGINT NOT NULL DEFAULT 0, updated_at TIMESTAMPTZ NOT NULL
);
