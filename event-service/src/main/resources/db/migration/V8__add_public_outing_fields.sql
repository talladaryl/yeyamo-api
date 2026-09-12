ALTER TABLE events ALTER COLUMN place_id DROP NOT NULL;
ALTER TABLE events ADD COLUMN IF NOT EXISTS owner_user_id VARCHAR(120);
ALTER TABLE events ADD COLUMN IF NOT EXISTS location_name VARCHAR(255);
ALTER TABLE events ADD COLUMN IF NOT EXISTS location_address VARCHAR(500);
ALTER TABLE events ADD COLUMN IF NOT EXISTS location_latitude DOUBLE PRECISION;
ALTER TABLE events ADD COLUMN IF NOT EXISTS location_longitude DOUBLE PRECISION;
ALTER TABLE events ADD COLUMN IF NOT EXISTS visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC';
ALTER TABLE events ADD COLUMN IF NOT EXISTS allow_uninvited_participants BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE events ADD COLUMN IF NOT EXISTS comments_participants_only BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE events ADD COLUMN IF NOT EXISTS show_participants BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE events ADD COLUMN IF NOT EXISTS sharing_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE events ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE events ADD CONSTRAINT ck_event_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE'));
ALTER TABLE events ADD CONSTRAINT ck_event_free_location_coordinates CHECK (
    (location_latitude IS NULL AND location_longitude IS NULL)
    OR (location_latitude BETWEEN -90 AND 90 AND location_longitude BETWEEN -180 AND 180)
);
CREATE INDEX IF NOT EXISTS idx_events_owner_created ON events(owner_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_events_public_upcoming ON events(status, visibility, start_at);

CREATE TABLE IF NOT EXISTS event_invitations (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id VARCHAR(120) NOT NULL,
    invited_by VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_event_invitation UNIQUE(event_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_event_invitations_user ON event_invitations(user_id, event_id);
