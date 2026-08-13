ALTER TABLE analytics_event_store ADD COLUMN IF NOT EXISTS admin_level1_id VARCHAR(120);
ALTER TABLE analytics_event_store ADD COLUMN IF NOT EXISTS city_id VARCHAR(120);
ALTER TABLE analytics_event_store ADD COLUMN IF NOT EXISTS user_country_code VARCHAR(2);
ALTER TABLE analytics_event_store ADD COLUMN IF NOT EXISTS content_country_code VARCHAR(2);
ALTER TABLE analytics_event_store ADD COLUMN IF NOT EXISTS currency_code VARCHAR(3);

CREATE TABLE country_activity_daily (
    id UUID PRIMARY KEY, stat_date DATE NOT NULL, country_code VARCHAR(2) NOT NULL, event_type VARCHAR(120) NOT NULL,
    events BIGINT NOT NULL DEFAULT 0, views BIGINT NOT NULL DEFAULT 0, interactions BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), UNIQUE(stat_date,country_code,event_type));
CREATE TABLE city_activity_daily (
    id UUID PRIMARY KEY, stat_date DATE NOT NULL, country_code VARCHAR(2) NOT NULL, city_id VARCHAR(120) NOT NULL, event_type VARCHAR(120) NOT NULL,
    events BIGINT NOT NULL DEFAULT 0, views BIGINT NOT NULL DEFAULT 0, interactions BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), UNIQUE(stat_date,country_code,city_id,event_type));
CREATE TABLE country_revenue_daily (
    id UUID PRIMARY KEY, stat_date DATE NOT NULL, country_code VARCHAR(2) NOT NULL, currency_code VARCHAR(3) NOT NULL,
    payments BIGINT NOT NULL DEFAULT 0, revenue NUMERIC(19,4) NOT NULL DEFAULT 0, refunds NUMERIC(19,4) NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), UNIQUE(stat_date,country_code,currency_code));
CREATE TABLE culture_country_daily (
    id UUID PRIMARY KEY, stat_date DATE NOT NULL, country_code VARCHAR(2) NOT NULL, content_type VARCHAR(80) NOT NULL,
    events BIGINT NOT NULL DEFAULT 0, views BIGINT NOT NULL DEFAULT 0, interactions BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), UNIQUE(stat_date,country_code,content_type));
CREATE TABLE artisan_country_daily (
    id UUID PRIMARY KEY, stat_date DATE NOT NULL, country_code VARCHAR(2) NOT NULL, events BIGINT NOT NULL DEFAULT 0,
    views BIGINT NOT NULL DEFAULT 0, interactions BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), UNIQUE(stat_date,country_code));
