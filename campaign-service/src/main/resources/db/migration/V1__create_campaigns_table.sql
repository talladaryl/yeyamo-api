-- Create campaigns table
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
    start_at TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at TIMESTAMP WITH TIME ZONE NOT NULL,
    target_configuration JSONB NOT NULL,
    creative_configuration JSONB NOT NULL,
    spent_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    created_by VARCHAR(100) NOT NULL,
    approved_by VARCHAR(100),
    rejection_reason VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

-- Create indexes
CREATE INDEX idx_campaigns_partner_id ON campaigns(partner_id);
CREATE INDEX idx_campaigns_status ON campaigns(status);
CREATE INDEX idx_campaigns_partner_id_status ON campaigns(partner_id, status);
CREATE INDEX idx_campaigns_promoted_entity ON campaigns(promoted_entity_type, promoted_entity_id);
CREATE INDEX idx_campaigns_start_at ON campaigns(start_at);
CREATE INDEX idx_campaigns_end_at ON campaigns(end_at);

-- Create outbox_events table
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    attempts INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1000)
);

-- Create indexes for outbox
CREATE INDEX idx_outbox_events_published_at ON outbox_events(published_at) WHERE published_at IS NULL;
CREATE INDEX idx_outbox_events_occurred_at ON outbox_events(occurred_at);
