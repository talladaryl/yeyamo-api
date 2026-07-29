CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_bookings_reference_search
    ON bookings USING gin (LOWER(reference) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_bookings_activity_search
    ON bookings USING gin (LOWER(activity_id) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_bookings_user_search
    ON bookings USING gin (LOWER(user_id) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_bookings_slot
    ON bookings (slot_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status_payment_created
    ON bookings (status, payment_status, created_at DESC);
