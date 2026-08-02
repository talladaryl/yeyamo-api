-- H2 compatible version for tests
CREATE TABLE campaigns (
    id UUID PRIMARY KEY,
    partner_id VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    objective VARCHAR(50) NOT NULL,
    promoted_entity_type VARCHAR(50) NOT NULL,
    promoted_entity_id VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    billing_model VARCHAR(50) NOT NULL,
    total_budget DECIMAL(19, 4) NOT NULL,
    daily_budget DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    target_configuration CLOB NOT NULL,
    creative_configuration CLOB NOT NULL,
    spent_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    created_by VARCHAR(100) NOT NULL,
    approved_by VARCHAR(100),
    rejection_reason VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_campaigns_partner_id ON campaigns(partner_id);
CREATE INDEX idx_campaigns_status ON campaigns(status);
CREATE INDEX idx_campaigns_partner_id_status ON campaigns(partner_id, status);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload CLOB NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP,
    attempts INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1000)
);

CREATE INDEX idx_outbox_events_occurred_at ON outbox_events(occurred_at);
