CREATE TABLE event_create_idempotency (
    id UUID PRIMARY KEY,
    owner_user_id VARCHAR(120) NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_event_create_idempotency_owner_key UNIQUE (owner_user_id, idempotency_key)
);
