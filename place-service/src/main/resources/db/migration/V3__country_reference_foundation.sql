CREATE TABLE IF NOT EXISTS countries (
    code VARCHAR(2) PRIMARY KEY CHECK (code ~ '^[A-Z]{2}$'),
    name VARCHAR(120) NOT NULL,
    launch_status VARCHAR(20) NOT NULL CHECK (launch_status IN ('DISABLED','COMING_SOON','BETA','LIVE')),
    default_currency_code VARCHAR(3) NOT NULL CHECK (default_currency_code ~ '^[A-Z]{3}$'),
    default_timezone VARCHAR(80) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS country_languages (
    id BIGSERIAL PRIMARY KEY,
    country_code VARCHAR(2) NOT NULL REFERENCES countries(code),
    language_code VARCHAR(35) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    is_official BOOLEAN NOT NULL DEFAULT FALSE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_country_language UNIQUE(country_code, language_code)
);

INSERT INTO countries(code,name,launch_status,default_currency_code,default_timezone) VALUES
('CM','Cameroun','LIVE','XAF','Africa/Douala'),
('CI','Côte d''Ivoire','COMING_SOON','XOF','Africa/Abidjan'),
('SN','Sénégal','COMING_SOON','XOF','Africa/Dakar'),
('GH','Ghana','COMING_SOON','GHS','Africa/Accra'),
('NG','Nigeria','COMING_SOON','NGN','Africa/Lagos'),
('KE','Kenya','COMING_SOON','KES','Africa/Nairobi'),
('RW','Rwanda','COMING_SOON','RWF','Africa/Kigali'),
('TZ','Tanzanie','COMING_SOON','TZS','Africa/Dar_es_Salaam'),
('ZA','Afrique du Sud','COMING_SOON','ZAR','Africa/Johannesburg'),
('ET','Éthiopie','COMING_SOON','ETB','Africa/Addis_Ababa')
ON CONFLICT (code) DO NOTHING;

INSERT INTO country_languages(country_code,language_code,display_name,is_official,is_primary) VALUES
('CM','fr-CM','Français (Cameroun)',TRUE,TRUE),
('CM','en-CM','English (Cameroon)',TRUE,FALSE)
ON CONFLICT (country_code,language_code) DO NOTHING;

ALTER TABLE regions ADD COLUMN IF NOT EXISTS country_code VARCHAR(2) REFERENCES countries(code);

CREATE TABLE IF NOT EXISTS geographic_migration_issues (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(40) NOT NULL,
    entity_id VARCHAR(80) NOT NULL,
    issue_code VARCHAR(80) NOT NULL,
    details TEXT,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ,
    CONSTRAINT uk_geographic_migration_issue UNIQUE(entity_type, entity_id, issue_code)
);

UPDATE regions SET country_code = 'CM' WHERE country_code IS NULL;

CREATE INDEX IF NOT EXISTS idx_regions_country_active ON regions(country_code, active);
CREATE INDEX IF NOT EXISTS idx_country_languages_country ON country_languages(country_code, is_primary DESC);
