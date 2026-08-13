-- Add multi-country geographic fields to user_profiles
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS admin_level_1_id UUID;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS admin_level_2_id UUID;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS city_id UUID;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS locality_id UUID;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS preferred_language_code VARCHAR(10);
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS timezone VARCHAR(50);
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS preferred_currency_code VARCHAR(3);
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS local_radius_km INTEGER;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS discover_african_content BOOLEAN NOT NULL DEFAULT true;

-- Create tables for content preferences (many-to-many)
CREATE TABLE IF NOT EXISTS user_content_countries (
    profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    country_code VARCHAR(2) NOT NULL,
    PRIMARY KEY (profile_id, country_code)
);

CREATE TABLE IF NOT EXISTS user_content_languages (
    profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    PRIMARY KEY (profile_id, language_code)
);

-- Create indexes for geographic queries
CREATE INDEX IF NOT EXISTS idx_user_profiles_country ON user_profiles(country_code);
CREATE INDEX IF NOT EXISTS idx_user_profiles_city ON user_profiles(city_id);
CREATE INDEX IF NOT EXISTS idx_user_profiles_admin1 ON user_profiles(admin_level_1_id);

-- Migration note: Existing users without countryCode will need to be updated
-- Set default for Cameroon-based users (if applicable to current context)
-- Example: UPDATE user_profiles SET country_code = 'CM' WHERE country_code IS NULL AND <condition>;
-- Admins should review and update ambiguous profiles manually
