CREATE TABLE ingestion_jobs (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(200) NOT NULL UNIQUE,
    source_type VARCHAR(20) NOT NULL,
    source_reference VARCHAR(1000),
    input_payload TEXT,
    status VARCHAR(40) NOT NULL,
    total_records INTEGER NOT NULL DEFAULT 0,
    accepted_records INTEGER NOT NULL DEFAULT 0,
    rejected_records INTEGER NOT NULL DEFAULT 0,
    duplicate_records INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_ingestion_jobs_pending ON ingestion_jobs(created_at) WHERE status = 'PENDING';

CREATE TABLE ingestion_records (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES ingestion_jobs(id),
    source VARCHAR(1000) NOT NULL,
    external_id VARCHAR(200),
    fingerprint VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payload TEXT NOT NULL,
    errors TEXT,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_ingestion_records_job ON ingestion_records(job_id);
CREATE UNIQUE INDEX uk_ingestion_accepted_fingerprint ON ingestion_records(fingerprint) WHERE status = 'ACCEPTED';

CREATE TABLE ingestion_outbox (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000)
);
CREATE INDEX idx_ingestion_outbox_pending ON ingestion_outbox(occurred_at) WHERE published_at IS NULL;
