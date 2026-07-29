CREATE TABLE support_conversations (
  id UUID PRIMARY KEY,
  user_id VARCHAR(120) NOT NULL,
  subject VARCHAR(300) NOT NULL,
  status VARCHAR(30) NOT NULL,
  priority VARCHAR(30) NOT NULL,
  assignee_admin_id VARCHAR(120),
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  first_response_at TIMESTAMPTZ,
  resolved_at TIMESTAMPTZ,
  version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_support_conversation_status_updated ON support_conversations(status, updated_at DESC);
CREATE INDEX idx_support_conversation_assignee ON support_conversations(assignee_admin_id, status);
CREATE INDEX idx_support_conversation_user ON support_conversations(user_id, created_at DESC);

CREATE TABLE support_messages (
  id UUID PRIMARY KEY,
  conversation_id UUID NOT NULL REFERENCES support_conversations(id),
  sender_type VARCHAR(20) NOT NULL,
  sender_id VARCHAR(120) NOT NULL,
  content TEXT NOT NULL,
  attachment_media_ids TEXT NOT NULL DEFAULT '[]',
  created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_support_message_conversation ON support_messages(conversation_id, created_at);

CREATE TABLE support_internal_notes (
  id UUID PRIMARY KEY,
  conversation_id UUID NOT NULL REFERENCES support_conversations(id),
  author_admin_id VARCHAR(120) NOT NULL,
  content TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_support_note_conversation ON support_internal_notes(conversation_id, created_at);

CREATE TABLE support_audit_log (
  id UUID PRIMARY KEY,
  conversation_id UUID NOT NULL,
  actor_id VARCHAR(120) NOT NULL,
  action VARCHAR(80) NOT NULL,
  metadata TEXT NOT NULL DEFAULT '{}',
  correlation_id VARCHAR(120),
  created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_support_audit_conversation ON support_audit_log(conversation_id, created_at);
