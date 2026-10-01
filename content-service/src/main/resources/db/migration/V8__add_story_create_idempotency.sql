CREATE TABLE story_create_idempotency (
    id UUID PRIMARY KEY,
    author_id VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    story_id UUID NOT NULL REFERENCES stories(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_story_create_idempotency_author_key UNIQUE (author_id, idempotency_key)
);
