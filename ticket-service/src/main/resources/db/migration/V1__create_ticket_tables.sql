-- Ticket Sale Configuration
CREATE TABLE ticket_sale_configurations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) NOT NULL,
    partner_id VARCHAR(255) NOT NULL,
    sales_start_at TIMESTAMP NOT NULL,
    sales_end_at TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    max_tickets_per_buyer INTEGER NOT NULL DEFAULT 10,
    currency VARCHAR(3) NOT NULL DEFAULT 'XOF',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ticket_sale_config_event UNIQUE (event_id)
);

CREATE INDEX idx_ticket_sale_config_partner ON ticket_sale_configurations(partner_id);
CREATE INDEX idx_ticket_sale_config_status ON ticket_sale_configurations(status);

-- Ticket Types
CREATE TABLE ticket_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_configuration_id UUID NOT NULL REFERENCES ticket_sale_configurations(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(15, 2) NOT NULL,
    quantity_total INTEGER NOT NULL,
    quantity_reserved INTEGER NOT NULL DEFAULT 0,
    quantity_sold INTEGER NOT NULL DEFAULT 0,
    sales_start_at TIMESTAMP,
    sales_end_at TIMESTAMP,
    access_zone VARCHAR(100),
    gate_instructions TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ticket_type_code UNIQUE (sale_configuration_id, code),
    CONSTRAINT chk_quantity_positive CHECK (quantity_total >= 0),
    CONSTRAINT chk_reserved_valid CHECK (quantity_reserved >= 0 AND quantity_reserved <= quantity_total),
    CONSTRAINT chk_sold_valid CHECK (quantity_sold >= 0 AND quantity_sold <= quantity_total)
);

CREATE INDEX idx_ticket_type_config ON ticket_types(sale_configuration_id);
CREATE INDEX idx_ticket_type_status ON ticket_types(status);

-- Ticket Holds (for cart reservation)
CREATE TABLE ticket_holds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    event_id VARCHAR(255) NOT NULL,
    ticket_type_id UUID NOT NULL REFERENCES ticket_types(id),
    quantity INTEGER NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    idempotency_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    order_id UUID,
    CONSTRAINT uk_ticket_hold_idempotency UNIQUE (user_id, idempotency_key),
    CONSTRAINT chk_hold_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_ticket_hold_user ON ticket_holds(user_id);
CREATE INDEX idx_ticket_hold_expires ON ticket_holds(expires_at) WHERE status = 'ACTIVE';
CREATE INDEX idx_ticket_hold_type ON ticket_holds(ticket_type_id);

-- Ticket Orders
CREATE TABLE ticket_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference VARCHAR(50) NOT NULL UNIQUE,
    user_id VARCHAR(255) NOT NULL,
    partner_id VARCHAR(255) NOT NULL,
    event_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    subtotal DECIMAL(15, 2) NOT NULL,
    discount_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    service_fee DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    promotion_id VARCHAR(255),
    payment_reference VARCHAR(255),
    payment_provider VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    issued_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    CONSTRAINT chk_order_amounts_positive CHECK (
        subtotal >= 0 AND 
        discount_amount >= 0 AND 
        service_fee >= 0 AND 
        total_amount >= 0
    )
);

CREATE INDEX idx_ticket_order_reference ON ticket_orders(reference);
CREATE INDEX idx_ticket_order_user ON ticket_orders(user_id);
CREATE INDEX idx_ticket_order_event ON ticket_orders(event_id);
CREATE INDEX idx_ticket_order_status ON ticket_orders(status);
CREATE INDEX idx_ticket_order_payment_ref ON ticket_orders(payment_reference);

-- Tickets
CREATE TABLE tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES ticket_orders(id),
    event_id VARCHAR(255) NOT NULL,
    ticket_type_id UUID NOT NULL REFERENCES ticket_types(id),
    owner_user_id VARCHAR(255) NOT NULL,
    serial_number VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_PAYMENT',
    issued_at TIMESTAMP,
    used_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    refunded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_ticket_order ON tickets(order_id);
CREATE INDEX idx_ticket_event ON tickets(event_id);
CREATE INDEX idx_ticket_owner ON tickets(owner_user_id);
CREATE INDEX idx_ticket_serial ON tickets(serial_number);
CREATE INDEX idx_ticket_status ON tickets(status);

-- Ticket QR Credentials (for secure QR tokens)
CREATE TABLE ticket_qr_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    token_id VARCHAR(255) NOT NULL UNIQUE,
    token_hash VARCHAR(512) NOT NULL,
    key_id VARCHAR(50) NOT NULL,
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    CONSTRAINT uk_qr_credential_ticket UNIQUE (ticket_id)
);

CREATE INDEX idx_qr_credential_token_id ON ticket_qr_credentials(token_id);
CREATE INDEX idx_qr_credential_ticket ON ticket_qr_credentials(ticket_id);
CREATE INDEX idx_qr_credential_key ON ticket_qr_credentials(key_id);

-- Ticket Scans (check-in logs)
CREATE TABLE ticket_scans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID REFERENCES tickets(id),
    event_id VARCHAR(255) NOT NULL,
    scanner_user_id VARCHAR(255) NOT NULL,
    staff_assignment_id UUID,
    gate_id VARCHAR(100),
    result VARCHAR(50) NOT NULL,
    reason_code VARCHAR(100),
    scanned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    device_id_hash VARCHAR(255),
    offline_reference VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ticket_scan_ticket ON ticket_scans(ticket_id);
CREATE INDEX idx_ticket_scan_event ON ticket_scans(event_id);
CREATE INDEX idx_ticket_scan_scanner ON ticket_scans(scanner_user_id);
CREATE INDEX idx_ticket_scan_result ON ticket_scans(result);
CREATE INDEX idx_ticket_scan_time ON ticket_scans(scanned_at);
CREATE UNIQUE INDEX uk_ticket_scan_client_reference
    ON ticket_scans(scanner_user_id, offline_reference)
    WHERE offline_reference IS NOT NULL;

-- Event Staff Assignments
CREATE TABLE event_staff_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) NOT NULL,
    partner_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_staff_event_user UNIQUE (event_id, user_id)
);

CREATE INDEX idx_staff_event ON event_staff_assignments(event_id);
CREATE INDEX idx_staff_user ON event_staff_assignments(user_id);
CREATE INDEX idx_staff_status ON event_staff_assignments(status);

-- Outbox for event publishing
CREATE TABLE ticket_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX idx_ticket_outbox_status ON ticket_outbox(status, created_at);
CREATE INDEX idx_ticket_outbox_published ON ticket_outbox(published_at);
