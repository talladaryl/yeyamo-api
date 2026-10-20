ALTER TABLE users ADD COLUMN IF NOT EXISTS account_type VARCHAR(20) NOT NULL DEFAULT 'EXPLORER';

UPDATE users u
SET account_type = 'PARTNER'
WHERE EXISTS (
    SELECT 1
    FROM user_roles ur
    JOIN roles r ON r.id = ur.role_id
    WHERE ur.user_id = u.id AND r.code = 'PARTNER'
);

ALTER TABLE users ADD CONSTRAINT chk_users_account_type
    CHECK (account_type IN ('EXPLORER', 'PARTNER'));
