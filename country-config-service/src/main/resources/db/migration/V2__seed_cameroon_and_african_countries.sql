-- Seed Cameroon as LIVE country (Cameroon-first strategy)
-- Other African countries as COMING_SOON

-- CAMEROON - LIVE (Priority country at launch)
INSERT INTO countries (
    code, name, official_name, continent_code,
    default_language_code, default_currency_code, default_timezone, phone_country_code,
    launch_status,
    registration_enabled, content_publishing_enabled, partner_onboarding_enabled,
    payments_enabled, booking_enabled, ticketing_enabled,
    artisan_commerce_enabled, culture_module_enabled
) VALUES (
    'CM', 'Cameroon', 'Republic of Cameroon', 'AF',
    'fr', 'XAF', 'Africa/Douala', '+237',
    'LIVE',
    true, true, true, true, true, true, true, true
);

-- Get Cameroon ID for foreign key references
DO $$
DECLARE
    cameroon_id UUID;
BEGIN
    SELECT id INTO cameroon_id FROM countries WHERE code = 'CM';
    
    -- Cameroon languages (French and English)
    INSERT INTO country_languages (country_id, language_code, name, is_default, display_order) VALUES
        (cameroon_id, 'fr', 'Français', true, 1),
        (cameroon_id, 'en', 'English', false, 2);
    
    -- Cameroon currency (XAF - Central African CFA franc)
    INSERT INTO country_currencies (country_id, currency_code, name, symbol, decimal_places, is_default) VALUES
        (cameroon_id, 'XAF', 'Central African CFA franc', 'FCFA', 0, true);
    
    -- Cameroon timezone
    INSERT INTO country_timezones (country_id, timezone, display_name, is_default) VALUES
        (cameroon_id, 'Africa/Douala', 'West Africa Time (WAT)', true);
END $$;

-- OTHER AFRICAN COUNTRIES - COMING_SOON
-- Central Africa
INSERT INTO countries (code, name, official_name, continent_code, default_language_code, default_currency_code, default_timezone, phone_country_code, launch_status) VALUES
    ('CF', 'Central African Republic', 'Central African Republic', 'AF', 'fr', 'XAF', 'Africa/Bangui', '+236', 'COMING_SOON'),
    ('TD', 'Chad', 'Republic of Chad', 'AF', 'fr', 'XAF', 'Africa/Ndjamena', '+235', 'COMING_SOON'),
    ('CG', 'Congo', 'Republic of the Congo', 'AF', 'fr', 'XAF', 'Africa/Brazzaville', '+242', 'COMING_SOON'),
    ('GA', 'Gabon', 'Gabonese Republic', 'AF', 'fr', 'XAF', 'Africa/Libreville', '+241', 'COMING_SOON'),
    ('GQ', 'Equatorial Guinea', 'Republic of Equatorial Guinea', 'AF', 'es', 'XAF', 'Africa/Malabo', '+240', 'COMING_SOON');

-- West Africa
INSERT INTO countries (code, name, official_name, continent_code, default_language_code, default_currency_code, default_timezone, phone_country_code, launch_status) VALUES
    ('BJ', 'Benin', 'Republic of Benin', 'AF', 'fr', 'XOF', 'Africa/Porto-Novo', '+229', 'COMING_SOON'),
    ('BF', 'Burkina Faso', 'Burkina Faso', 'AF', 'fr', 'XOF', 'Africa/Ouagadougou', '+226', 'COMING_SOON'),
    ('CI', 'Côte d''Ivoire', 'Republic of Côte d''Ivoire', 'AF', 'fr', 'XOF', 'Africa/Abidjan', '+225', 'COMING_SOON'),
    ('GH', 'Ghana', 'Republic of Ghana', 'AF', 'en', 'GHS', 'Africa/Accra', '+233', 'COMING_SOON'),
    ('GN', 'Guinea', 'Republic of Guinea', 'AF', 'fr', 'GNF', 'Africa/Conakry', '+224', 'COMING_SOON'),
    ('ML', 'Mali', 'Republic of Mali', 'AF', 'fr', 'XOF', 'Africa/Bamako', '+223', 'COMING_SOON'),
    ('NE', 'Niger', 'Republic of Niger', 'AF', 'fr', 'XOF', 'Africa/Niamey', '+227', 'COMING_SOON'),
    ('NG', 'Nigeria', 'Federal Republic of Nigeria', 'AF', 'en', 'NGN', 'Africa/Lagos', '+234', 'COMING_SOON'),
    ('SN', 'Senegal', 'Republic of Senegal', 'AF', 'fr', 'XOF', 'Africa/Dakar', '+221', 'COMING_SOON'),
    ('TG', 'Togo', 'Togolese Republic', 'AF', 'fr', 'XOF', 'Africa/Lome', '+228', 'COMING_SOON');

-- East Africa
INSERT INTO countries (code, name, official_name, continent_code, default_language_code, default_currency_code, default_timezone, phone_country_code, launch_status) VALUES
    ('KE', 'Kenya', 'Republic of Kenya', 'AF', 'sw', 'KES', 'Africa/Nairobi', '+254', 'COMING_SOON'),
    ('TZ', 'Tanzania', 'United Republic of Tanzania', 'AF', 'sw', 'TZS', 'Africa/Dar_es_Salaam', '+255', 'COMING_SOON'),
    ('UG', 'Uganda', 'Republic of Uganda', 'AF', 'en', 'UGX', 'Africa/Kampala', '+256', 'COMING_SOON'),
    ('RW', 'Rwanda', 'Republic of Rwanda', 'AF', 'rw', 'RWF', 'Africa/Kigali', '+250', 'COMING_SOON'),
    ('ET', 'Ethiopia', 'Federal Democratic Republic of Ethiopia', 'AF', 'am', 'ETB', 'Africa/Addis_Ababa', '+251', 'COMING_SOON');

-- Southern Africa
INSERT INTO countries (code, name, official_name, continent_code, default_language_code, default_currency_code, default_timezone, phone_country_code, launch_status) VALUES
    ('ZA', 'South Africa', 'Republic of South Africa', 'AF', 'en', 'ZAR', 'Africa/Johannesburg', '+27', 'COMING_SOON'),
    ('ZW', 'Zimbabwe', 'Republic of Zimbabwe', 'AF', 'en', 'USD', 'Africa/Harare', '+263', 'COMING_SOON'),
    ('ZM', 'Zambia', 'Republic of Zambia', 'AF', 'en', 'ZMW', 'Africa/Lusaka', '+260', 'COMING_SOON'),
    ('BW', 'Botswana', 'Republic of Botswana', 'AF', 'en', 'BWP', 'Africa/Gaborone', '+267', 'COMING_SOON'),
    ('MZ', 'Mozambique', 'Republic of Mozambique', 'AF', 'pt', 'MZN', 'Africa/Maputo', '+258', 'COMING_SOON');

-- North Africa
INSERT INTO countries (code, name, official_name, continent_code, default_language_code, default_currency_code, default_timezone, phone_country_code, launch_status) VALUES
    ('DZ', 'Algeria', 'People''s Democratic Republic of Algeria', 'AF', 'ar', 'DZD', 'Africa/Algiers', '+213', 'COMING_SOON'),
    ('MA', 'Morocco', 'Kingdom of Morocco', 'AF', 'ar', 'MAD', 'Africa/Casablanca', '+212', 'COMING_SOON'),
    ('TN', 'Tunisia', 'Republic of Tunisia', 'AF', 'ar', 'TND', 'Africa/Tunis', '+216', 'COMING_SOON'),
    ('EG', 'Egypt', 'Arab Republic of Egypt', 'AF', 'ar', 'EGP', 'Africa/Cairo', '+20', 'COMING_SOON');

COMMENT ON TABLE countries IS 'Cameroon is LIVE. All other countries are COMING_SOON and features disabled by default.';
