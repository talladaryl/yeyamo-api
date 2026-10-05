-- Historical reconciliation: every outing group owner is exactly one active OWNER member.
-- The conversation/user primary key makes this migration idempotent.
INSERT INTO conversation_members (
    conversation_id,
    user_id,
    role,
    status,
    joined_at,
    left_at,
    last_read_message_id,
    last_read_at
)
SELECT
    link.conversation_id,
    link.owner_user_id,
    'OWNER',
    'ACTIVE',
    link.created_at,
    NULL,
    NULL,
    NULL
FROM outing_group_links link
ON CONFLICT (conversation_id, user_id) DO UPDATE
SET role = 'OWNER',
    status = 'ACTIVE',
    left_at = NULL;
