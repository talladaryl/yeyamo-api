CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE discovery_documents (
    id UUID PRIMARY KEY,
    source_id VARCHAR(120) NOT NULL UNIQUE,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    category_code VARCHAR(100),
    region_code VARCHAR(40),
    city VARCHAR(160),
    latitude DOUBLE PRECISION CHECK (latitude BETWEEN -90 AND 90),
    longitude DOUBLE PRECISION CHECK (longitude BETWEEN -180 AND 180),
    location geometry(Point, 4326) GENERATED ALWAYS AS (
        CASE WHEN latitude IS NULL OR longitude IS NULL THEN NULL
             ELSE ST_SetSRID(ST_MakePoint(longitude, latitude), 4326) END
    ) STORED,
    search_vector tsvector GENERATED ALWAYS AS (
        to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(description, '') || ' ' || coalesce(city, ''))
    ) STORED,
    author_id VARCHAR(120),
    trend_score DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (trend_score >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    published_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_discovery_search ON discovery_documents USING GIN (search_vector);
CREATE INDEX idx_discovery_location ON discovery_documents USING GIST (location);
CREATE INDEX idx_discovery_active_trend ON discovery_documents (active, trend_score DESC, published_at DESC);
CREATE INDEX idx_discovery_filters ON discovery_documents (type, category_code, region_code) WHERE active = TRUE;

CREATE TABLE discovery_pending_trends (
    source_id VARCHAR(120) PRIMARY KEY,
    score DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (score >= 0),
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE discovery_processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);
