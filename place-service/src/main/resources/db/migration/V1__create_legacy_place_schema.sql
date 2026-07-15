CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS regions (
    id BIGSERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE, code VARCHAR(10) NOT NULL UNIQUE,
    description TEXT, cover_image VARCHAR(255)
);
CREATE TABLE IF NOT EXISTS cities (
    id BIGSERIAL PRIMARY KEY, region_id BIGINT NOT NULL REFERENCES regions(id),
    name VARCHAR(255) NOT NULL, slug VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION, longitude DOUBLE PRECISION,
    CONSTRAINT uk_city_region_slug UNIQUE(region_id,slug)
);
CREATE TABLE IF NOT EXISTS districts (
    id BIGSERIAL PRIMARY KEY, city_id BIGINT NOT NULL REFERENCES cities(id),
    name VARCHAR(255) NOT NULL, latitude DOUBLE PRECISION, longitude DOUBLE PRECISION,
    CONSTRAINT uk_district_city_name UNIQUE(city_id,name)
);
CREATE TABLE IF NOT EXISTS place_categories (
    id BIGSERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE, icon VARCHAR(255),
    parent_id BIGINT REFERENCES place_categories(id)
);
CREATE TABLE IF NOT EXISTS places (
    id UUID PRIMARY KEY, partner_id UUID,
    category_id BIGINT REFERENCES place_categories(id),
    region_id BIGINT NOT NULL REFERENCES regions(id),
    city_id BIGINT NOT NULL REFERENCES cities(id),
    district_id BIGINT REFERENCES districts(id),
    name VARCHAR(255) NOT NULL, slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT, latitude DOUBLE PRECISION NOT NULL CHECK(latitude BETWEEN -90 AND 90),
    longitude DOUBLE PRECISION NOT NULL CHECK(longitude BETWEEN -180 AND 180),
    location geometry(Point,4326), geohash VARCHAR(255), address VARCHAR(255),
    phone VARCHAR(255), website VARCHAR(255), status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_places_location ON places USING GIST(location);
CREATE INDEX IF NOT EXISTS idx_places_region_status ON places(region_id,status);
CREATE INDEX IF NOT EXISTS idx_places_city_status ON places(city_id,status);
CREATE TABLE IF NOT EXISTS place_media (
    id BIGSERIAL PRIMARY KEY, place_id UUID NOT NULL REFERENCES places(id) ON DELETE CASCADE,
    url VARCHAR(255) NOT NULL, type VARCHAR(50) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE IF NOT EXISTS place_schedules (
    id BIGSERIAL PRIMARY KEY, place_id UUID NOT NULL REFERENCES places(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL CHECK(day_of_week BETWEEN 1 AND 7),
    open_time TIME NOT NULL, close_time TIME NOT NULL
);
CREATE TABLE IF NOT EXISTS place_outbox (
    id UUID PRIMARY KEY, aggregate_id VARCHAR(80) NOT NULL,
    event_type VARCHAR(100) NOT NULL, payload TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL, published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0, last_error VARCHAR(1000)
);
CREATE INDEX IF NOT EXISTS idx_place_outbox_pending ON place_outbox(occurred_at) WHERE published_at IS NULL;
