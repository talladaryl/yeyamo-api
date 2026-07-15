CREATE TABLE mission_definitions (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    title VARCHAR(180) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,
    reward_code VARCHAR(100) NOT NULL,
    reward_title VARCHAR(200) NOT NULL,
    reward_amount INTEGER NOT NULL DEFAULT 1 CHECK (reward_amount > 0),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE mission_objectives (
    id UUID PRIMARY KEY,
    mission_id UUID NOT NULL REFERENCES mission_definitions(id) ON DELETE CASCADE,
    label VARCHAR(200) NOT NULL,
    event_type VARCHAR(140) NOT NULL,
    metric VARCHAR(20) NOT NULL,
    target_value BIGINT NOT NULL CHECK (target_value > 0),
    value_field VARCHAR(100),
    rule_key VARCHAR(100),
    rule_value VARCHAR(200),
    position INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_mission_objective_event ON mission_objectives(event_type);

CREATE TABLE user_missions (
    id UUID PRIMARY KEY,
    user_id VARCHAR(120) NOT NULL,
    mission_id UUID NOT NULL REFERENCES mission_definitions(id),
    status VARCHAR(30) NOT NULL,
    enrolled_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_mission UNIQUE(user_id, mission_id)
);
CREATE INDEX idx_user_missions_user ON user_missions(user_id, updated_at DESC);

CREATE TABLE mission_progress (
    id UUID PRIMARY KEY,
    user_mission_id UUID NOT NULL REFERENCES user_missions(id) ON DELETE CASCADE,
    objective_id UUID NOT NULL REFERENCES mission_objectives(id),
    current_value BIGINT NOT NULL DEFAULT 0 CHECK (current_value >= 0),
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_mission_progress UNIQUE(user_mission_id, objective_id)
);

CREATE TABLE mission_reward_grants (
    id UUID PRIMARY KEY,
    user_mission_id UUID NOT NULL UNIQUE REFERENCES user_missions(id),
    user_id VARCHAR(120) NOT NULL,
    reward_code VARCHAR(100) NOT NULL,
    reward_title VARCHAR(200) NOT NULL,
    amount INTEGER NOT NULL CHECK (amount > 0),
    status VARCHAR(30) NOT NULL,
    granted_at TIMESTAMPTZ,
    failure_reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_reward_grants_user ON mission_reward_grants(user_id, created_at DESC);

CREATE TABLE mission_processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(140) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE mission_outbox (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(140) NOT NULL,
    payload TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000)
);
CREATE INDEX idx_mission_outbox_pending ON mission_outbox(occurred_at) WHERE published_at IS NULL;
