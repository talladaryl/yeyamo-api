CREATE TABLE IF NOT EXISTS payments(
 id UUID PRIMARY KEY,booking_id UUID NOT NULL UNIQUE,saga_id UUID,user_id VARCHAR(120) NOT NULL,
 amount NUMERIC(12,2) NOT NULL CHECK(amount>0),currency VARCHAR(3) NOT NULL,
 status VARCHAR(40) NOT NULL,provider VARCHAR(40) NOT NULL,provider_payment_id VARCHAR(160) UNIQUE,
 idempotency_key VARCHAR(200) NOT NULL UNIQUE,failure_reason VARCHAR(1000),
 created_at TIMESTAMPTZ NOT NULL,updated_at TIMESTAMPTZ NOT NULL,authorized_at TIMESTAMPTZ,
 cancelled_at TIMESTAMPTZ,refunded_at TIMESTAMPTZ,version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_payments_user_created ON payments(user_id,created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payments_status_updated ON payments(status,updated_at);

CREATE TABLE IF NOT EXISTS payment_refunds(
 id UUID PRIMARY KEY,payment_id UUID NOT NULL REFERENCES payments(id),amount NUMERIC(12,2) NOT NULL CHECK(amount>0),
 status VARCHAR(30) NOT NULL,provider_refund_id VARCHAR(160) UNIQUE,idempotency_key VARCHAR(200) NOT NULL UNIQUE,
 failure_reason VARCHAR(1000),created_at TIMESTAMPTZ NOT NULL,updated_at TIMESTAMPTZ NOT NULL,completed_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_payment_refunds_payment ON payment_refunds(payment_id,created_at DESC);

CREATE TABLE IF NOT EXISTS payment_processed_events(
 event_id UUID PRIMARY KEY,event_type VARCHAR(120) NOT NULL,processed_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE IF NOT EXISTS payment_webhook_receipts(
 id UUID PRIMARY KEY,provider VARCHAR(40) NOT NULL,provider_event_id VARCHAR(160) NOT NULL,
 payload_hash VARCHAR(64) NOT NULL,received_at TIMESTAMPTZ NOT NULL,processed_at TIMESTAMPTZ,
 CONSTRAINT uk_payment_webhook_provider_event UNIQUE(provider,provider_event_id)
);
CREATE TABLE IF NOT EXISTS payment_outbox(
 id UUID PRIMARY KEY,aggregate_id VARCHAR(120) NOT NULL,event_type VARCHAR(120) NOT NULL,target_topic VARCHAR(160) NOT NULL,
 payload TEXT NOT NULL,occurred_at TIMESTAMPTZ NOT NULL,published_at TIMESTAMPTZ,attempts INTEGER NOT NULL DEFAULT 0,last_error VARCHAR(1000)
);
CREATE INDEX IF NOT EXISTS idx_payment_outbox_pending ON payment_outbox(occurred_at) WHERE published_at IS NULL;
