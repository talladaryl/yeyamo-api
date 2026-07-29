CREATE TABLE IF NOT EXISTS admin_user_profile_projection (
    auth_user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    profile_id UUID UNIQUE,
    display_name VARCHAR(100),
    avatar_url VARCHAR(2048),
    region_id BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email_lower ON users (LOWER(email));
CREATE INDEX IF NOT EXISTS idx_users_phone ON users (phone);
CREATE INDEX IF NOT EXISTS idx_users_status ON users (status);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users (created_at);
CREATE INDEX IF NOT EXISTS idx_users_last_login_at ON users (last_login_at);
CREATE INDEX IF NOT EXISTS idx_admin_user_profile_region ON admin_user_profile_projection (region_id);
