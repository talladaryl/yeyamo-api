-- Repair migration for installations where the original V2 seed was recorded
-- by Flyway but Cameroon was not present in the country configuration database.
--
-- Cameroon is the launch country and auth-service validates registration against
-- this table.  Keep this migration insert-only: an administrator's existing
-- country configuration must never be overwritten by a deployment.

INSERT INTO countries (
    code, name, official_name, continent_code,
    default_language_code, default_currency_code, default_timezone, phone_country_code,
    launch_status,
    registration_enabled, content_publishing_enabled, partner_onboarding_enabled,
    payments_enabled, booking_enabled, ticketing_enabled,
    artisan_commerce_enabled, culture_module_enabled,
    place_publishing_enabled, event_feature_enabled
) VALUES (
    'CM', 'Cameroon', 'Republic of Cameroon', 'AF',
    'fr', 'XAF', 'Africa/Douala', '+237',
    'LIVE',
    true, true, true,
    true, true, true,
    true, true,
    true, true
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO country_languages (country_id, language_code, name, is_default, display_order)
SELECT id, 'fr', 'Français', true, 1
FROM countries
WHERE code = 'CM'
ON CONFLICT (country_id, language_code) DO NOTHING;

INSERT INTO country_languages (country_id, language_code, name, is_default, display_order)
SELECT id, 'en', 'English', false, 2
FROM countries
WHERE code = 'CM'
ON CONFLICT (country_id, language_code) DO NOTHING;

INSERT INTO country_currencies (country_id, currency_code, name, symbol, decimal_places, is_default)
SELECT id, 'XAF', 'Central African CFA franc', 'FCFA', 0, true
FROM countries
WHERE code = 'CM'
ON CONFLICT (country_id, currency_code) DO NOTHING;

INSERT INTO country_timezones (country_id, timezone, display_name, is_default)
SELECT id, 'Africa/Douala', 'West Africa Time (WAT)', true
FROM countries
WHERE code = 'CM'
ON CONFLICT (country_id, timezone) DO NOTHING;
