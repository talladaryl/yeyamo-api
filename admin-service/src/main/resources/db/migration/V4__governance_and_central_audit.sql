CREATE TABLE platform_settings (
  setting_key VARCHAR(100) PRIMARY KEY, setting_value TEXT NOT NULL, value_type VARCHAR(30) NOT NULL,
  description VARCHAR(500) NOT NULL, version BIGINT NOT NULL, updated_by VARCHAR(120) NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE platform_setting_history (
  id UUID PRIMARY KEY, setting_key VARCHAR(100) NOT NULL, setting_value TEXT NOT NULL,
  value_type VARCHAR(30) NOT NULL, version BIGINT NOT NULL, updated_by VARCHAR(120) NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL, change_reason VARCHAR(500), UNIQUE(setting_key,version)
);
CREATE INDEX idx_setting_history_key_version ON platform_setting_history(setting_key,version DESC);

CREATE TABLE feature_flags (
  flag_key VARCHAR(100) PRIMARY KEY, description VARCHAR(500) NOT NULL, enabled BOOLEAN NOT NULL,
  environment VARCHAR(30) NOT NULL, rollout_percentage INTEGER, version BIGINT NOT NULL,
  updated_by VARCHAR(120) NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT ck_flag_rollout CHECK (rollout_percentage IS NULL OR rollout_percentage BETWEEN 0 AND 100)
);
CREATE TABLE feature_flag_history (
  id UUID PRIMARY KEY, flag_key VARCHAR(100) NOT NULL, description VARCHAR(500) NOT NULL,
  enabled BOOLEAN NOT NULL, environment VARCHAR(30) NOT NULL, rollout_percentage INTEGER,
  version BIGINT NOT NULL, updated_by VARCHAR(120) NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
  change_reason VARCHAR(500), UNIQUE(flag_key,version)
);
CREATE INDEX idx_flag_history_key_version ON feature_flag_history(flag_key,version DESC);

ALTER TABLE admin_audit_logs ADD COLUMN actor_id VARCHAR(120);
ALTER TABLE admin_audit_logs ADD COLUMN actor_role VARCHAR(80);
ALTER TABLE admin_audit_logs ADD COLUMN service VARCHAR(100) NOT NULL DEFAULT 'admin-service';
ALTER TABLE admin_audit_logs ADD COLUMN target_id_value VARCHAR(160);
CREATE UNIQUE INDEX uk_admin_audit_external_event ON admin_audit_logs(service,correlation_id,action,target_id_value)
  WHERE actor_id IS NOT NULL;
CREATE INDEX idx_admin_audit_actor ON admin_audit_logs(actor_id,created_at DESC);
CREATE INDEX idx_admin_audit_action ON admin_audit_logs(action,created_at DESC);
CREATE INDEX idx_admin_audit_target ON admin_audit_logs(target_type,target_id_value,created_at DESC);
CREATE INDEX idx_admin_audit_service ON admin_audit_logs(service,created_at DESC);
