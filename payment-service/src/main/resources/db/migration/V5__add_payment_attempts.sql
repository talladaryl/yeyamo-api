CREATE TABLE IF NOT EXISTS payment_attempts (
    id UUID PRIMARY KEY,
    reference VARCHAR(160) NOT NULL UNIQUE,
    transaction_id VARCHAR(160),
    source_service VARCHAR(60) NOT NULL,
    source_id UUID NOT NULL,
    status VARCHAR(40) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    failure_reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payment_attempts_reference ON payment_attempts(reference);
CREATE INDEX IF NOT EXISTS idx_payment_attempts_source ON payment_attempts(source_service, source_id);
CREATE INDEX IF NOT EXISTS idx_payment_attempts_idempotency ON payment_attempts(idempotency_key);
