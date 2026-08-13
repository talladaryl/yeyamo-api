-- user-service has no verified phone/country source.  Do not infer CM from a legacy
-- profile alone; retain the data and record profiles requiring user/admin selection.
CREATE TABLE IF NOT EXISTS user_profile_country_migration_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    reason VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (profile_id, reason)
);

INSERT INTO user_profile_country_migration_audit (profile_id, reason)
SELECT id, 'AMBIGUOUS_COUNTRY_NO_VERIFIED_LOCATION'
FROM user_profiles
WHERE country_code IS NULL
ON CONFLICT (profile_id, reason) DO NOTHING;
