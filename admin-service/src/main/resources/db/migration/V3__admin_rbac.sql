ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS email VARCHAR(320);
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS scopes JSONB NOT NULL DEFAULT '[]'::jsonb;

UPDATE admin_users SET name = 'Administrateur' WHERE name IS NULL;
UPDATE admin_users SET email = CONCAT('admin-', id, '@invalid.yeyamo.local') WHERE email IS NULL;

ALTER TABLE admin_users ALTER COLUMN name SET NOT NULL;
ALTER TABLE admin_users ALTER COLUMN email SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_admin_users_email_lower ON admin_users (LOWER(email));
CREATE INDEX IF NOT EXISTS idx_admin_users_status ON admin_users(status);
