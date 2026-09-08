CREATE TABLE ticket_processed_events(event_id UUID PRIMARY KEY,event_type VARCHAR(120) NOT NULL,processed_at TIMESTAMP WITH TIME ZONE NOT NULL);
ALTER TABLE ticket_outbox ADD COLUMN IF NOT EXISTS attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE ticket_outbox ADD COLUMN IF NOT EXISTS last_error VARCHAR(1000);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ticket_scan_client_ref ON ticket_scans(scanner_user_id, offline_reference);
