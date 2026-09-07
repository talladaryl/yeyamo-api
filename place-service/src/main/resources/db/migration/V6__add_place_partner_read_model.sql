CREATE TABLE IF NOT EXISTS place_partner_read_model (
    partner_id UUID PRIMARY KEY,
    owner_user_id VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
