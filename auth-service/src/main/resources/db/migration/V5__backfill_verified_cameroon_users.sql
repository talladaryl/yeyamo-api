-- Only infer Cameroon for records whose E.164 number proves the country.  All other
-- records remain unset and are logged for a manual, privacy-safe review.
UPDATE users
SET country_code = 'CM'
WHERE country_code IS NULL
  AND phone ~ '^\\+237[0-9]{6,12}$';

CREATE TABLE IF NOT EXISTS user_country_migration_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL,
    reason VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, reason)
);

INSERT INTO user_country_migration_audit (user_id, reason)
SELECT id, 'AMBIGUOUS_COUNTRY_NO_RELIABLE_E164_SIGNAL'
FROM users
WHERE country_code IS NULL
ON CONFLICT (user_id, reason) DO NOTHING;
