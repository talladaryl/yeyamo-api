CREATE TABLE activity_slots(
 id UUID PRIMARY KEY,activity_id VARCHAR(120) NOT NULL,owner_user_id VARCHAR(120) NOT NULL,starts_at TIMESTAMPTZ NOT NULL,ends_at TIMESTAMPTZ NOT NULL,
 capacity INTEGER NOT NULL CHECK(capacity>0),reserved_count INTEGER NOT NULL DEFAULT 0 CHECK(reserved_count>=0),
 unit_price NUMERIC(12,2) NOT NULL CHECK(unit_price>=0),currency VARCHAR(3) NOT NULL,status VARCHAR(20) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,updated_at TIMESTAMPTZ NOT NULL,version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT ck_slot_reservations CHECK(reserved_count<=capacity),CONSTRAINT uk_activity_slot UNIQUE(activity_id,starts_at)
);
CREATE INDEX idx_slots_activity_time ON activity_slots(activity_id,starts_at);

CREATE TABLE bookings(
 id UUID PRIMARY KEY,reference VARCHAR(24) NOT NULL UNIQUE,user_id VARCHAR(120) NOT NULL,activity_id VARCHAR(120) NOT NULL,
 slot_id UUID NOT NULL REFERENCES activity_slots(id),quantity INTEGER NOT NULL CHECK(quantity>0),unit_price NUMERIC(12,2) NOT NULL,
 total_amount NUMERIC(12,2) NOT NULL,currency VARCHAR(3) NOT NULL,status VARCHAR(20) NOT NULL,payment_status VARCHAR(30) NOT NULL,
 cancellation_reason VARCHAR(500),created_at TIMESTAMPTZ NOT NULL,confirmed_at TIMESTAMPTZ,cancelled_at TIMESTAMPTZ,
 completed_at TIMESTAMPTZ,updated_at TIMESTAMPTZ NOT NULL,version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_bookings_user ON bookings(user_id,created_at DESC);
CREATE INDEX idx_bookings_slot_status ON bookings(slot_id,status);

CREATE TABLE booking_payment_sagas(
 id UUID PRIMARY KEY,booking_id UUID NOT NULL UNIQUE REFERENCES bookings(id),status VARCHAR(40) NOT NULL,
 payment_id VARCHAR(120),failure_reason VARCHAR(1000),created_at TIMESTAMPTZ NOT NULL,updated_at TIMESTAMPTZ NOT NULL,version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE booking_command_receipts(
 id UUID PRIMARY KEY,idempotency_key VARCHAR(160) NOT NULL,user_id VARCHAR(120) NOT NULL,operation VARCHAR(80) NOT NULL,
 booking_id UUID NOT NULL REFERENCES bookings(id),created_at TIMESTAMPTZ NOT NULL,CONSTRAINT uk_booking_command UNIQUE(idempotency_key,user_id,operation)
);
CREATE TABLE booking_history(
 id UUID PRIMARY KEY,booking_id UUID NOT NULL,actor_id VARCHAR(120),action VARCHAR(80) NOT NULL,details TEXT NOT NULL,occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_booking_history ON booking_history(booking_id,occurred_at);
CREATE OR REPLACE FUNCTION prevent_booking_history_mutation() RETURNS trigger AS $$ BEGIN RAISE EXCEPTION 'booking_history is append-only'; END; $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_booking_history_immutable BEFORE UPDATE OR DELETE ON booking_history FOR EACH ROW EXECUTE FUNCTION prevent_booking_history_mutation();

CREATE TABLE booking_processed_events(event_id UUID PRIMARY KEY,event_type VARCHAR(140) NOT NULL,processed_at TIMESTAMPTZ NOT NULL);
CREATE TABLE booking_outbox(id UUID PRIMARY KEY,aggregate_id VARCHAR(120) NOT NULL,event_type VARCHAR(140) NOT NULL,target_topic VARCHAR(140) NOT NULL,payload TEXT NOT NULL,occurred_at TIMESTAMPTZ NOT NULL,published_at TIMESTAMPTZ,attempts INTEGER NOT NULL DEFAULT 0,last_error VARCHAR(1000));
CREATE INDEX idx_booking_outbox_pending ON booking_outbox(occurred_at) WHERE published_at IS NULL;
