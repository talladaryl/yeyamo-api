CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_users_email_search
    ON users USING gin (LOWER(email) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_users_phone_search
    ON users USING gin (phone gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_users_status_created
    ON users (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_user
    ON user_roles (role_id, user_id);
