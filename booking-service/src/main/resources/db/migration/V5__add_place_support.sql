ALTER TABLE activity_slots ADD COLUMN IF NOT EXISTS place_id UUID;
CREATE INDEX IF NOT EXISTS idx_slots_place ON activity_slots(place_id);

CREATE TABLE IF NOT EXISTS place_read_model (
    place_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_place_read_model_active ON place_read_model(is_active);
