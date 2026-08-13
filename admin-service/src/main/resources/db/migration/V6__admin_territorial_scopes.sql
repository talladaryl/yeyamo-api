CREATE TABLE admin_scopes (
    id UUID PRIMARY KEY,
    admin_user_id UUID NOT NULL REFERENCES admin_users(id) ON DELETE CASCADE,
    scope_type VARCHAR(20) NOT NULL,
    country_code VARCHAR(2),
    administrative_area_id UUID,
    city_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT ck_admin_scope_shape CHECK (
      (scope_type='GLOBAL' AND country_code IS NULL AND administrative_area_id IS NULL AND city_id IS NULL) OR
      (scope_type='COUNTRY' AND country_code IS NOT NULL) OR
      (scope_type='ADMIN_AREA' AND administrative_area_id IS NOT NULL) OR
      (scope_type='CITY' AND city_id IS NOT NULL)
    )
);
CREATE INDEX idx_admin_scopes_lookup ON admin_scopes(admin_user_id, scope_type, country_code);
ALTER TABLE admin_audit_logs ADD COLUMN IF NOT EXISTS actor_scope JSONB;
ALTER TABLE admin_audit_logs ADD COLUMN IF NOT EXISTS target_country_code VARCHAR(2);
