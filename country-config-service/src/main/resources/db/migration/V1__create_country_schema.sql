-- Country Configuration Service - Base Schema
-- Multi-country, multi-language, multi-currency support
-- ISO standards: ISO 3166-1 (countries), ISO 4217 (currencies), ISO 639/BCP 47 (languages)

-- Countries table
CREATE TABLE countries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(2) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    official_name VARCHAR(200),
    continent_code VARCHAR(2) NOT NULL,
    default_language_code VARCHAR(10) NOT NULL,
    default_currency_code VARCHAR(3) NOT NULL,
    default_timezone VARCHAR(50) NOT NULL,
    phone_country_code VARCHAR(5) NOT NULL,
    launch_status VARCHAR(20) NOT NULL,
    
    -- Feature flags
    registration_enabled BOOLEAN NOT NULL DEFAULT false,
    content_publishing_enabled BOOLEAN NOT NULL DEFAULT false,
    partner_onboarding_enabled BOOLEAN NOT NULL DEFAULT false,
    payments_enabled BOOLEAN NOT NULL DEFAULT false,
    booking_enabled BOOLEAN NOT NULL DEFAULT false,
    ticketing_enabled BOOLEAN NOT NULL DEFAULT false,
    artisan_commerce_enabled BOOLEAN NOT NULL DEFAULT false,
    culture_module_enabled BOOLEAN NOT NULL DEFAULT false,
    
    -- Auditing
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    
    -- Constraints
    CONSTRAINT chk_country_code CHECK (code ~ '^[A-Z]{2}$'),
    CONSTRAINT chk_currency_code CHECK (default_currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_phone_code CHECK (phone_country_code ~ '^\+\d{1,4}$'),
    CONSTRAINT chk_launch_status CHECK (launch_status IN ('DISABLED', 'COMING_SOON', 'BETA', 'LIVE'))
);

CREATE INDEX idx_country_code ON countries(code);
CREATE INDEX idx_country_launch_status ON countries(launch_status);

COMMENT ON TABLE countries IS 'Central source of truth for country configuration';
COMMENT ON COLUMN countries.code IS 'ISO 3166-1 alpha-2 country code';
COMMENT ON COLUMN countries.default_currency_code IS 'ISO 4217 currency code';
COMMENT ON COLUMN countries.default_language_code IS 'ISO 639-1 or BCP 47 language code';
COMMENT ON COLUMN countries.default_timezone IS 'IANA timezone identifier';
COMMENT ON COLUMN countries.phone_country_code IS 'E.164 phone country code';

-- Country languages table
CREATE TABLE country_languages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_id UUID NOT NULL REFERENCES countries(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    name VARCHAR(50) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT false,
    display_order INTEGER NOT NULL DEFAULT 0,
    
    CONSTRAINT uq_country_language UNIQUE (country_id, language_code)
);

CREATE INDEX idx_country_lang_country_id ON country_languages(country_id);
CREATE INDEX idx_country_lang_code ON country_languages(language_code);

COMMENT ON TABLE country_languages IS 'Languages supported in each country';
COMMENT ON COLUMN country_languages.language_code IS 'ISO 639-1 or BCP 47 language code';

-- Country currencies table
CREATE TABLE country_currencies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_id UUID NOT NULL REFERENCES countries(id) ON DELETE CASCADE,
    currency_code VARCHAR(3) NOT NULL,
    name VARCHAR(100) NOT NULL,
    symbol VARCHAR(10),
    decimal_places INTEGER NOT NULL DEFAULT 2,
    is_default BOOLEAN NOT NULL DEFAULT false,
    
    CONSTRAINT chk_currency_code_fmt CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_decimal_places CHECK (decimal_places >= 0 AND decimal_places <= 4),
    CONSTRAINT uq_country_currency UNIQUE (country_id, currency_code)
);

CREATE INDEX idx_country_curr_country_id ON country_currencies(country_id);
CREATE INDEX idx_country_curr_code ON country_currencies(currency_code);

COMMENT ON TABLE country_currencies IS 'Currencies accepted in each country';
COMMENT ON COLUMN country_currencies.currency_code IS 'ISO 4217 currency code';

-- Country timezones table
CREATE TABLE country_timezones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_id UUID NOT NULL REFERENCES countries(id) ON DELETE CASCADE,
    timezone VARCHAR(50) NOT NULL,
    display_name VARCHAR(100),
    is_default BOOLEAN NOT NULL DEFAULT false,
    
    CONSTRAINT uq_country_timezone UNIQUE (country_id, timezone)
);

CREATE INDEX idx_country_tz_country_id ON country_timezones(country_id);

COMMENT ON TABLE country_timezones IS 'Timezones for countries spanning multiple zones';
COMMENT ON COLUMN country_timezones.timezone IS 'IANA timezone identifier';
