-- H2 version of ads tables
CREATE TABLE campaign_projections (
    campaign_id VARCHAR(100) PRIMARY KEY,
    partner_id VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    objective VARCHAR(50) NOT NULL,
    promoted_entity_type VARCHAR(50) NOT NULL,
    promoted_entity_id VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    billing_model VARCHAR(50) NOT NULL,
    bid_amount DECIMAL(19, 4) NOT NULL,
    total_budget DECIMAL(19, 4) NOT NULL,
    daily_budget DECIMAL(19, 4) NOT NULL,
    spent_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    eligible_placements CLOB,
    target_countries CLOB,
    target_regions CLOB,
    target_cities CLOB,
    target_interests CLOB,
    target_categories CLOB,
    min_age VARCHAR(10),
    max_age VARCHAR(10),
    target_languages CLOB,
    creative_json CLOB NOT NULL,
    quality_score INT NOT NULL DEFAULT 50,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    delivery_policy_version VARCHAR(20) DEFAULT '1.0',
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_campaign_active ON campaign_projections(status, start_at, end_at);

CREATE TABLE ad_delivery_records (
    delivery_id VARCHAR(100) PRIMARY KEY,
    campaign_id VARCHAR(100) NOT NULL,
    user_id VARCHAR(100),
    anonymous_session_id VARCHAR(100),
    placement VARCHAR(50) NOT NULL,
    impression_at TIMESTAMP NOT NULL,
    view_duration_ms BIGINT,
    clicked_at TIMESTAMP,
    converted_at TIMESTAMP,
    conversion_type VARCHAR(50),
    conversion_value DECIMAL(19, 4),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_delivery_campaign FOREIGN KEY (campaign_id) REFERENCES campaign_projections(campaign_id)
);

CREATE INDEX idx_delivery_campaign ON ad_delivery_records(campaign_id);

CREATE TABLE delivery_policy_versions (
    version VARCHAR(20) PRIMARY KEY,
    description CLOB,
    algorithm_config CLOB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT false,
    effective_from TIMESTAMP NOT NULL,
    effective_to TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL
);

INSERT INTO delivery_policy_versions (version, description, algorithm_config, is_active, effective_from, created_by)
VALUES ('1.0', 'Initial ad selection algorithm', 
        '{"bidWeight": 0.30, "geoWeight": 0.20}',
        true, CURRENT_TIMESTAMP, 'SYSTEM');
