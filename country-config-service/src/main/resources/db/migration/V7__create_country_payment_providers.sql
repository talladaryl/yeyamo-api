CREATE TABLE country_payment_providers (
    id UUID PRIMARY KEY,
    country_code VARCHAR(2) NOT NULL REFERENCES countries(code),
    provider_code VARCHAR(64) NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    priority INTEGER NOT NULL DEFAULT 100,
    min_amount NUMERIC(19,4),
    max_amount NUMERIC(19,4),
    configuration_reference VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_country_payment_provider_amounts CHECK (min_amount IS NULL OR max_amount IS NULL OR min_amount <= max_amount),
    CONSTRAINT uk_country_payment_provider UNIQUE(country_code, provider_code, payment_method, currency_code)
);
CREATE INDEX idx_country_payment_provider_resolution
    ON country_payment_providers(country_code, currency_code, payment_method, enabled, priority);

-- Providers are deliberately not seeded: availability must be confirmed and credentials remain external.
