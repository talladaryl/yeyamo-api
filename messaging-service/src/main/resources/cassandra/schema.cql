CREATE KEYSPACE IF NOT EXISTS yeyamo_messaging WITH replication = {'class':'SimpleStrategy','replication_factor':1};

CREATE TABLE IF NOT EXISTS yeyamo_messaging.conversations (
 id uuid PRIMARY KEY,type text,title text,owner_id text,created_at timestamp,updated_at timestamp,
 last_message_id uuid,last_message_preview text,last_message_at timestamp
);
CREATE TABLE IF NOT EXISTS yeyamo_messaging.conversation_members (
 conversation_id uuid,user_id text,role text,status text,joined_at timestamp,left_at timestamp,
 last_read_message_id uuid,last_read_at timestamp,PRIMARY KEY(conversation_id,user_id)
);
CREATE TABLE IF NOT EXISTS yeyamo_messaging.conversations_by_user (
 user_id text,conversation_id uuid,role text,status text,title text,conversation_type text,
 updated_at timestamp,last_message_preview text,last_message_at timestamp,
 PRIMARY KEY(user_id,conversation_id)
);
CREATE TABLE IF NOT EXISTS yeyamo_messaging.messages_by_conversation (
 conversation_id uuid,sent_at timestamp,message_id uuid,sender_id text,client_message_id text,
 message_type text,body text,attachment_ids list<uuid>,reply_to_message_id uuid,
 edited_at timestamp,deleted_at timestamp,PRIMARY KEY(conversation_id,sent_at,message_id)
) WITH CLUSTERING ORDER BY(sent_at DESC,message_id ASC);
CREATE TABLE IF NOT EXISTS yeyamo_messaging.messages_by_id (
 message_id uuid PRIMARY KEY,conversation_id uuid,sent_at timestamp,sender_id text,client_message_id text,
 message_type text,body text,attachment_ids list<uuid>,reply_to_message_id uuid,edited_at timestamp,deleted_at timestamp
);
CREATE TABLE IF NOT EXISTS yeyamo_messaging.message_idempotency (
 sender_id text,client_message_id text,message_id uuid,conversation_id uuid,created_at timestamp,
 PRIMARY KEY(sender_id,client_message_id)
);
CREATE TABLE IF NOT EXISTS yeyamo_messaging.direct_conversations (
 participant_pair text PRIMARY KEY,conversation_id uuid,created_at timestamp
);
