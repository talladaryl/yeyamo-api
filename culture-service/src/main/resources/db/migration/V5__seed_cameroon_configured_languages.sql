-- These codes and names are the existing Cameroon Country Config references
-- (country-config V2). Culture keeps its own language catalogue because
-- /api/v1/culture/languages is the source used by the contribution workflow.
INSERT INTO languages (
    code, name, native_name, country_codes, writing_system, status,
    description, speaker_estimate, verified
) VALUES
    ('fr', 'Français', 'Français', 'CM', 'Latin', 'ACTIVE', NULL, NULL, FALSE),
    ('en', 'English', 'English', 'CM', 'Latin', 'ACTIVE', NULL, NULL, FALSE)
ON CONFLICT (code) DO NOTHING;
