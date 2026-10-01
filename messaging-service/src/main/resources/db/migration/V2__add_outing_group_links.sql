CREATE TABLE outing_group_links (
    outing_id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    owner_user_id VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_outing_group_links_conversation UNIQUE (conversation_id)
);
