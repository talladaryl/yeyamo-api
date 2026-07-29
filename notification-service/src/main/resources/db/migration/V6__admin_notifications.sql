CREATE TABLE admin_notifications (
 id UUID PRIMARY KEY, source_event_id UUID NOT NULL UNIQUE, admin_id VARCHAR(120), target_role VARCHAR(80),
 notification_type VARCHAR(80) NOT NULL, title VARCHAR(300) NOT NULL, message TEXT NOT NULL,
 resource_type VARCHAR(80) NOT NULL, resource_id VARCHAR(160) NOT NULL, created_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ, CONSTRAINT ck_admin_notification_target CHECK (admin_id IS NOT NULL OR target_role IS NOT NULL)
);
CREATE INDEX idx_admin_notification_admin_created ON admin_notifications(admin_id,created_at DESC);
CREATE INDEX idx_admin_notification_role_created ON admin_notifications(target_role,created_at DESC);
CREATE TABLE admin_notification_reads (
 id UUID PRIMARY KEY, notification_id UUID NOT NULL REFERENCES admin_notifications(id) ON DELETE CASCADE,
 admin_id VARCHAR(120) NOT NULL, read_at TIMESTAMPTZ NOT NULL, UNIQUE(notification_id,admin_id)
);
CREATE INDEX idx_admin_notification_read_admin ON admin_notification_reads(admin_id,read_at DESC);
