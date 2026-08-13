CREATE TABLE country_kyc_requirements (
    id UUID PRIMARY KEY,
    country_code VARCHAR(2) NOT NULL,
    partner_type VARCHAR(40) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT TRUE,
    validity_period_days INTEGER,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    configuration_version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_kyc_validity CHECK(validity_period_days IS NULL OR validity_period_days > 0),
    CONSTRAINT uk_country_kyc_requirement UNIQUE(country_code, partner_type, document_type)
);
CREATE INDEX idx_country_kyc_requirements_active ON country_kyc_requirements(country_code, partner_type, active);

CREATE TABLE partner_kyc_configuration_history (
    id UUID PRIMARY KEY,
    partner_id UUID NOT NULL REFERENCES partners(id),
    country_code VARCHAR(2) NOT NULL,
    partner_type VARCHAR(40) NOT NULL,
    configuration_version BIGINT NOT NULL,
    applied_requirements JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
