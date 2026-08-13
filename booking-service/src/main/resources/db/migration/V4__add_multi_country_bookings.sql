ALTER TABLE activity_slots ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
CREATE INDEX IF NOT EXISTS idx_bookings_country_created ON bookings(country_code, created_at DESC);
CREATE TABLE IF NOT EXISTS booking_country_migration_audit (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), booking_id UUID NOT NULL, reason VARCHAR(100) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE(booking_id, reason)
);
INSERT INTO booking_country_migration_audit(booking_id, reason)
SELECT id, 'AMBIGUOUS_COUNTRY_LEGACY_BOOKING' FROM bookings WHERE country_code IS NULL
ON CONFLICT (booking_id, reason) DO NOTHING;
