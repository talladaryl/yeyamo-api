-- Add multi-country support fields to users table
-- ISO 3166-1 alpha-2 country code (e.g., CM, SN, CI)
ALTER TABLE users ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);

-- City ID from country-config-service (nullable)
ALTER TABLE users ADD COLUMN IF NOT EXISTS city_id UUID;

-- Preferred language code (ISO 639-1 or BCP 47)
ALTER TABLE users ADD COLUMN IF NOT EXISTS preferred_language_code VARCHAR(10);

-- IANA timezone (e.g., Africa/Douala, Africa/Dakar)
ALTER TABLE users ADD COLUMN IF NOT EXISTS timezone VARCHAR(50);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_users_country ON users(country_code);
CREATE INDEX IF NOT EXISTS idx_users_city ON users(city_id);

-- Migration note: Existing users without countryCode will need to be updated
-- For Cameroon-based users (if applicable to current context):
-- UPDATE users SET country_code = 'CM' WHERE country_code IS NULL AND <condition>;
-- Admins should review and update ambiguous profiles manually
