CREATE TABLE user_profiles (
    id UUID PRIMARY KEY,
    auth_user_id VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(2048),
    bio VARCHAR(500),
    language VARCHAR(20) NOT NULL,
    visibility VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    location_sharing_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    preferred_region_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_user_profiles_display_name ON user_profiles(LOWER(display_name));
CREATE INDEX idx_user_profiles_status_visibility ON user_profiles(status, visibility);

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000)
);

CREATE INDEX idx_outbox_events_unpublished ON outbox_events(occurred_at) WHERE published_at IS NULL;
