CREATE TABLE IF NOT EXISTS event_place_read_model (
    place_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
