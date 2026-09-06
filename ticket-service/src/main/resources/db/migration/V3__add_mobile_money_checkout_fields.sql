ALTER TABLE ticket_orders
    ADD COLUMN IF NOT EXISTS payment_operator VARCHAR(20),
    ADD COLUMN IF NOT EXISTS payment_phone_number VARCHAR(16),
    ADD COLUMN IF NOT EXISTS payment_country_code VARCHAR(2);
