CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE catalog_assets (
    id UUID PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    owner_id UUID,
    source VARCHAR(80) NOT NULL,
    external_id VARCHAR(160),
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL UNIQUE,
    description TEXT,
    category_code VARCHAR(100),
    region_code VARCHAR(40),
    city VARCHAR(160),
    district VARCHAR(160),
    address VARCHAR(300),
    latitude DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    location geometry(Point,4326) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_catalog_source_external UNIQUE (source, external_id)
);
CREATE INDEX idx_catalog_status_type ON catalog_assets(status, type);
CREATE INDEX idx_catalog_region_category ON catalog_assets(region_code, category_code);
CREATE INDEX idx_catalog_location ON catalog_assets USING GIST(location);
CREATE INDEX idx_catalog_name_search ON catalog_assets USING GIN(to_tsvector('simple', name || ' ' || coalesce(description, '')));

CREATE TABLE catalog_outbox (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(80) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000)
);
CREATE INDEX idx_catalog_outbox_pending ON catalog_outbox(occurred_at) WHERE published_at IS NULL;

CREATE TABLE catalog_processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);
