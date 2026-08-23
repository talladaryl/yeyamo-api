CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(120),
    owner_id VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_message_id UUID,
    last_message_preview TEXT,
    last_message_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_conversations_updated_at ON conversations(updated_at DESC);

CREATE TABLE conversation_members (
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id VARCHAR(120) NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    left_at TIMESTAMP WITH TIME ZONE,
    last_read_message_id UUID,
    last_read_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (conversation_id, user_id)
);

CREATE INDEX idx_conv_members_user_status ON conversation_members(user_id, status);

CREATE TABLE direct_conversations (
    participant_pair VARCHAR(255) PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id VARCHAR(120) NOT NULL,
    client_message_id VARCHAR(120) NOT NULL,
    message_type VARCHAR(30) NOT NULL,
    body TEXT,
    reply_to_message_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL,
    edited_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_messages_conv_sent_at ON messages(conversation_id, sent_at DESC, id DESC);
CREATE INDEX idx_messages_sender_client ON messages(sender_id, client_message_id);

CREATE TABLE message_attachments (
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    attachment_id UUID NOT NULL,
    position INTEGER NOT NULL,
    PRIMARY KEY (message_id, position)
);

CREATE INDEX idx_message_attachments_msg_id ON message_attachments(message_id);

CREATE TABLE message_idempotency (
    sender_id VARCHAR(120) NOT NULL,
    client_message_id VARCHAR(120) NOT NULL,
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (sender_id, client_message_id)
);
