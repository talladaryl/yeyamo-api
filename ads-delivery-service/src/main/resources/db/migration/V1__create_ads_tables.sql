-- Campaign projections table (read-only projection for ad selection)
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
    eligible_placements TEXT,
    target_countries TEXT,
    target_regions TEXT,
    target_cities TEXT,
    target_interests TEXT,
    target_categories TEXT,
    min_age VARCHAR(10),
    max_age VARCHAR(10),
    target_languages TEXT,
    creative_json TEXT NOT NULL,
    quality_score INT NOT NULL DEFAULT 50,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    delivery_policy_version VARCHAR(20) DEFAULT '1.0',
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_campaign_active ON campaign_projections(status, start_at, end_at);
CREATE INDEX idx_campaign_partner ON campaign_projections(partner_id);
CREATE INDEX idx_campaign_entity ON campaign_projections(promoted_entity_type, promoted_entity_id);

-- Ad delivery records table (tracking impressions, clicks, conversions)
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
CREATE INDEX idx_delivery_user ON ad_delivery_records(user_id);
CREATE INDEX idx_delivery_impression ON ad_delivery_records(impression_at);
CREATE INDEX idx_delivery_click ON ad_delivery_records(clicked_at) WHERE clicked_at IS NOT NULL;
CREATE INDEX idx_delivery_conversion ON ad_delivery_records(converted_at) WHERE converted_at IS NOT NULL;

-- Ad conversions table (detailed conversion tracking)
CREATE TABLE ad_conversions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id VARCHAR(100) NOT NULL,
    campaign_id VARCHAR(100) NOT NULL,
    conversion_type VARCHAR(50) NOT NULL,
    conversion_value DECIMAL(19, 4),
    currency VARCHAR(3),
    converted_at TIMESTAMP NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_conversion_delivery FOREIGN KEY (delivery_id) REFERENCES ad_delivery_records(delivery_id),
    CONSTRAINT fk_conversion_campaign FOREIGN KEY (campaign_id) REFERENCES campaign_projections(campaign_id)
);

CREATE INDEX idx_conversion_delivery ON ad_conversions(delivery_id);
CREATE INDEX idx_conversion_campaign ON ad_conversions(campaign_id);
CREATE INDEX idx_conversion_type ON ad_conversions(conversion_type);

-- Delivery policy versions table (for A/B testing and algorithm versioning)
CREATE TABLE delivery_policy_versions (
    version VARCHAR(20) PRIMARY KEY,
    description TEXT,
    algorithm_config JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT false,
    effective_from TIMESTAMP NOT NULL,
    effective_to TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL
);

-- Insert default policy version
INSERT INTO delivery_policy_versions (version, description, algorithm_config, is_active, effective_from, created_by)
VALUES ('1.0', 'Initial ad selection algorithm', 
        '{"bidWeight": 0.30, "geoWeight": 0.20, "interestWeight": 0.20, "contextWeight": 0.15, "qualityWeight": 0.10, "freshnessWeight": 0.05, "explorationRate": 0.05}',
        true, NOW(), 'SYSTEM');
