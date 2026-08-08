CREATE TABLE artwork_offers (
 id UUID PRIMARY KEY, artwork_id UUID NOT NULL UNIQUE, artisan_partner_id VARCHAR(255) NOT NULL,
 sale_type VARCHAR(32) NOT NULL, amount NUMERIC(19,4), currency_code VARCHAR(3),
 available_quantity INTEGER NOT NULL, reserved_quantity INTEGER NOT NULL DEFAULT 0,
 country_code VARCHAR(2) NOT NULL, international_shipping BOOLEAN NOT NULL DEFAULT FALSE,
 custom_order_allowed BOOLEAN NOT NULL DEFAULT FALSE, status VARCHAR(32) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 CHECK (available_quantity >= 0), CHECK (reserved_quantity >= 0),
 CHECK (sale_type <> 'FIXED_PRICE' OR (amount > 0 AND currency_code IS NOT NULL)),
 CHECK (sale_type <> 'AUCTION_FUTURE' OR status = 'DISABLED')
);
CREATE TABLE artwork_orders (
 id UUID PRIMARY KEY, reference VARCHAR(60) NOT NULL UNIQUE, idempotency_key VARCHAR(255) NOT NULL UNIQUE,
 buyer_user_id VARCHAR(255) NOT NULL, artisan_partner_id VARCHAR(255) NOT NULL,
 offer_id UUID NOT NULL REFERENCES artwork_offers(id), commerce_order_id UUID REFERENCES commerce_orders(id),
 status VARCHAR(40) NOT NULL, delivery_type VARCHAR(32) NOT NULL, quantity INTEGER NOT NULL,
 gross_amount NUMERIC(19,4) NOT NULL, commission_amount NUMERIC(19,4) NOT NULL,
 artisan_amount NUMERIC(19,4) NOT NULL, currency_code VARCHAR(3) NOT NULL,
 cancellation_reason TEXT, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_artwork_orders_buyer ON artwork_orders(buyer_user_id,created_at DESC);
CREATE INDEX idx_artwork_orders_artisan ON artwork_orders(artisan_partner_id,created_at DESC);
CREATE TABLE artwork_order_items (id UUID PRIMARY KEY, order_id UUID NOT NULL REFERENCES artwork_orders(id), artwork_id UUID NOT NULL, quantity INTEGER NOT NULL, unit_price NUMERIC(19,4) NOT NULL, currency_code VARCHAR(3) NOT NULL);
CREATE TABLE artwork_order_status_history (id UUID PRIMARY KEY, order_id UUID NOT NULL REFERENCES artwork_orders(id), previous_status VARCHAR(40), new_status VARCHAR(40) NOT NULL, reason TEXT, actor_id VARCHAR(255) NOT NULL, created_at TIMESTAMPTZ NOT NULL);
