ALTER TABLE countries ADD COLUMN IF NOT EXISTS place_publishing_enabled BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE countries ADD COLUMN IF NOT EXISTS event_feature_enabled BOOLEAN NOT NULL DEFAULT false;

-- Cameroon is already a LIVE country. Preserve the existing launch behaviour by enabling
-- these explicit flags only for Cameroon; all other countries remain fail-closed.
UPDATE countries
SET place_publishing_enabled = true,
    event_feature_enabled = true
WHERE code = 'CM';
