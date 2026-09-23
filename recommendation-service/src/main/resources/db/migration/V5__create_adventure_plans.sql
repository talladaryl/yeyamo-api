CREATE TABLE adventure_plans (
    id UUID PRIMARY KEY,
    user_id VARCHAR(120) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    party_type VARCHAR(20) NOT NULL,
    budget_tier VARCHAR(20),
    budget_minimum NUMERIC(19,2),
    budget_maximum NUMERIC(19,2),
    currency_code VARCHAR(3),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_adventure_plan_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_adventure_plan_times CHECK (start_time IS NULL OR end_time IS NULL OR end_time > start_time),
    CONSTRAINT chk_adventure_plan_budget_min CHECK (budget_minimum IS NULL OR budget_minimum >= 0),
    CONSTRAINT chk_adventure_plan_budget_max CHECK (budget_maximum IS NULL OR budget_maximum >= 0),
    CONSTRAINT chk_adventure_plan_budget_range CHECK (budget_minimum IS NULL OR budget_maximum IS NULL OR budget_maximum >= budget_minimum)
);
CREATE INDEX idx_adventure_plans_user_created ON adventure_plans(user_id, created_at DESC);

CREATE TABLE adventure_plan_interests (
    plan_id UUID NOT NULL REFERENCES adventure_plans(id) ON DELETE CASCADE,
    interest_code VARCHAR(100) NOT NULL,
    PRIMARY KEY(plan_id, interest_code)
);

CREATE TABLE adventure_plan_days (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL REFERENCES adventure_plans(id) ON DELETE CASCADE,
    plan_date DATE NOT NULL,
    position INTEGER NOT NULL CHECK (position >= 0),
    CONSTRAINT uq_adventure_plan_day_date UNIQUE(plan_id, plan_date),
    CONSTRAINT uq_adventure_plan_day_position UNIQUE(plan_id, position)
);
CREATE INDEX idx_adventure_plan_days_plan_date ON adventure_plan_days(plan_id, plan_date);

CREATE TABLE adventure_plan_items (
    id UUID PRIMARY KEY,
    day_id UUID NOT NULL REFERENCES adventure_plan_days(id) ON DELETE CASCADE,
    recommendation_id UUID NOT NULL UNIQUE,
    source_id VARCHAR(160) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id VARCHAR(120) NOT NULL,
    scheduled_at TIMESTAMPTZ,
    position INTEGER NOT NULL CHECK (position >= 0),
    snapshot_title VARCHAR(300) NOT NULL,
    image_media_id UUID,
    location_label VARCHAR(300),
    starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,
    price NUMERIC(19,2),
    currency_code VARCHAR(3),
    availability_status VARCHAR(20) NOT NULL,
    reason_codes VARCHAR(500),
    skipped_at TIMESTAMPTZ,
    replaced_by_recommendation_id UUID,
    CONSTRAINT uq_adventure_plan_item_position UNIQUE(day_id, position)
);
CREATE INDEX idx_adventure_plan_items_day_position ON adventure_plan_items(day_id, position);
CREATE INDEX idx_adventure_plan_items_target ON adventure_plan_items(target_type, target_id);
