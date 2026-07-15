CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY,
    place_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(50) NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity >= 0),
    registered_count INTEGER NOT NULL DEFAULT 0 CHECK (registered_count >= 0 AND registered_count <= capacity),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT ck_event_dates CHECK (end_at > start_at)
);
CREATE INDEX IF NOT EXISTS idx_events_place_start ON events(place_id, start_at);
CREATE INDEX IF NOT EXISTS idx_events_status_start ON events(status, start_at);

CREATE TABLE IF NOT EXISTS event_registrations (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    registered_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT uk_event_registration UNIQUE (event_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_event_registrations_user ON event_registrations(user_id, registered_at DESC);

CREATE TABLE IF NOT EXISTS event_outbox (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(80) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000)
);
CREATE INDEX IF NOT EXISTS idx_event_outbox_pending ON event_outbox(occurred_at) WHERE published_at IS NULL;
