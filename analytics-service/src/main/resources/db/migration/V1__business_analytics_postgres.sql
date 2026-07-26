CREATE TABLE analytics_inbox_processed (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE analytics_event_store (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    partner_id VARCHAR(120),
    campaign_id VARCHAR(120),
    event_entity_id VARCHAR(120),
    payload JSONB NOT NULL,
    stored_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_analytics_event_store_occurred ON analytics_event_store(occurred_at);

CREATE TABLE analytics_reach_fingerprints (
    fingerprint VARCHAR(64) PRIMARY KEY,
    stat_date DATE NOT NULL,
    campaign_id VARCHAR(120) NOT NULL
);

CREATE TABLE analytics_daily_aggregates (
    id UUID PRIMARY KEY,
    stat_date DATE NOT NULL,
    scope_type VARCHAR(30) NOT NULL,
    scope_id VARCHAR(120) NOT NULL,
    partner_id VARCHAR(120),
    dimension_type VARCHAR(30) NOT NULL,
    dimension_value VARCHAR(160) NOT NULL,
    impressions BIGINT NOT NULL DEFAULT 0,
    qualified_impressions BIGINT NOT NULL DEFAULT 0,
    unique_reach BIGINT NOT NULL DEFAULT 0,
    clicks BIGINT NOT NULL DEFAULT 0,
    conversions BIGINT NOT NULL DEFAULT 0,
    tickets_sold BIGINT NOT NULL DEFAULT 0,
    scans BIGINT NOT NULL DEFAULT 0,
    rejected_scans BIGINT NOT NULL DEFAULT 0,
    spend NUMERIC(19,4) NOT NULL DEFAULT 0,
    budget NUMERIC(19,4) NOT NULL DEFAULT 0,
    revenue NUMERIC(19,4) NOT NULL DEFAULT 0,
    commission NUMERIC(19,4) NOT NULL DEFAULT 0,
    refunds NUMERIC(19,4) NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_analytics_daily_dimension UNIQUE
      (stat_date, scope_type, scope_id, dimension_type, dimension_value)
);
CREATE INDEX idx_analytics_daily_partner_date
  ON analytics_daily_aggregates(partner_id, stat_date);
CREATE INDEX idx_analytics_daily_scope_date
  ON analytics_daily_aggregates(scope_type, scope_id, stat_date);
