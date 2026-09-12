ALTER TABLE activity_slots ADD COLUMN IF NOT EXISTS activity_type VARCHAR(20) NOT NULL DEFAULT 'ACTIVITY';
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS activity_type VARCHAR(20) NOT NULL DEFAULT 'ACTIVITY';
ALTER TABLE bookings ADD CONSTRAINT ck_booking_activity_type CHECK (activity_type IN ('ACTIVITY', 'EXPERIENCE'));
ALTER TABLE activity_slots ADD CONSTRAINT ck_activity_slot_type CHECK (activity_type IN ('ACTIVITY', 'EXPERIENCE'));
CREATE INDEX IF NOT EXISTS idx_activity_slots_type_activity_time ON activity_slots(activity_type, activity_id, starts_at);
