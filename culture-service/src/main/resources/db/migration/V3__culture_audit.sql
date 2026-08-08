CREATE TABLE culture_audit (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_culture_audit_correlation ON culture_audit(correlation_id, occurred_at DESC);
