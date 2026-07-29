CREATE INDEX IF NOT EXISTS idx_bookings_status_created ON bookings(status,created_at DESC);
CREATE INDEX IF NOT EXISTS idx_bookings_payment_status ON bookings(payment_status,created_at DESC);
CREATE INDEX IF NOT EXISTS idx_bookings_activity ON bookings(activity_id,created_at DESC);
CREATE INDEX IF NOT EXISTS idx_slots_owner_start ON activity_slots(owner_user_id,starts_at DESC);
