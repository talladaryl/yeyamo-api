CREATE TABLE device_push_tokens (
  id UUID PRIMARY KEY,
  user_id VARCHAR(120) NOT NULL,
  token VARCHAR(500) NOT NULL UNIQUE,
  platform VARCHAR(20) NOT NULL,
  device_id VARCHAR(120) NOT NULL,
  app_version VARCHAR(50),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  last_seen_at TIMESTAMPTZ NOT NULL,
  invalidated_at TIMESTAMPTZ,
  CONSTRAINT uk_device_push_user_device UNIQUE(user_id, device_id)
);
CREATE INDEX idx_device_push_user_enabled ON device_push_tokens(user_id, enabled);

ALTER TABLE notification_deliveries DROP CONSTRAINT uk_notification_delivery;
ALTER TABLE notification_deliveries ADD CONSTRAINT uk_notification_delivery UNIQUE(notification_id, channel, destination);

CREATE TABLE push_tickets (
  id UUID PRIMARY KEY,
  delivery_id UUID NOT NULL UNIQUE REFERENCES notification_deliveries(id) ON DELETE CASCADE,
  device_token_id UUID NOT NULL REFERENCES device_push_tokens(id),
  expo_ticket_id VARCHAR(100) UNIQUE,
  status VARCHAR(30) NOT NULL,
  error_code VARCHAR(100),
  error_message VARCHAR(500),
  created_at TIMESTAMPTZ NOT NULL,
  receipt_checked_at TIMESTAMPTZ
);
CREATE INDEX idx_push_ticket_receipts ON push_tickets(status, receipt_checked_at, created_at);
