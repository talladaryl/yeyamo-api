CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_payments_status_created
    ON payments (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payments_provider_payment
    ON payments (provider_payment_id);
CREATE INDEX IF NOT EXISTS idx_payments_idempotency
    ON payments (idempotency_key);
CREATE INDEX IF NOT EXISTS idx_payments_user_search
    ON payments USING gin (LOWER(user_id) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_payments_provider_payment_search
    ON payments USING gin (LOWER(provider_payment_id) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_payments_idempotency_search
    ON payments USING gin (LOWER(idempotency_key) gin_trgm_ops);
