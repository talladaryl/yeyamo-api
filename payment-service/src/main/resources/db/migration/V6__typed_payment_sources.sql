ALTER TABLE payments ADD COLUMN IF NOT EXISTS source_type VARCHAR(40);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS source_id UUID;

UPDATE payments SET source_type = 'BOOKING', source_id = booking_id
WHERE source_type IS NULL AND booking_id IS NOT NULL;

ALTER TABLE payments ALTER COLUMN source_type SET NOT NULL;
ALTER TABLE payments ALTER COLUMN source_id SET NOT NULL;
ALTER TABLE payments ALTER COLUMN booking_id DROP NOT NULL;
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_booking_id_key;
CREATE UNIQUE INDEX IF NOT EXISTS uk_payments_source ON payments(source_type, source_id);
