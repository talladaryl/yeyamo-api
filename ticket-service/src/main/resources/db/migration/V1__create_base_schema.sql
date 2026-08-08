-- YeYamo Ticket Service - Initial Schema
-- Version 1.0 - Base tables for ticketing system

-- ========================================
-- Sale Configuration
-- ========================================
CREATE TABLE ticket_sale_configurations (
    id VARCHAR(36) PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    partner_id VARCHAR(100) NOT NULL,
    sales_start_at TIMESTAMP NOT NULL,
    sales_end_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    max_tickets_per_buyer INTEGER NOT NULL DEFAULT 10,
    currency VARCHAR(3) NOT NULL DEFAULT 'XOF',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_sales_period CHECK (sales_end_at > sales_start_at),
    CONSTRAINT chk_max_tickets CHECK (max_tickets_per_buyer BETWEEN 1 AND 20),
    CONSTRAINT chk_status CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'ENDED', 'CANCELLED'))
);

CREATE INDEX idx_sale_config_event ON ticket_sale_configurations(event_id);
CREATE INDEX idx_sale_config_partner ON ticket_sale_configurations(partner_id);
CREATE INDEX idx_sale_config_status ON ticket_sale_configurations(status);

COMMENT ON TABLE ticket_sale_configurations IS 'Configuration for ticket sales for specific events';

-- ========================================
-- Ticket Types (Inventory Management)
-- ========================================
CREATE TABLE ticket_types (
    id VARCHAR(36) PRIMARY KEY,
    sale_configuration_id VARCHAR(36) NOT NULL,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    price NUMERIC(12, 2) NOT NULL,
    quantity_total INTEGER NOT NULL,
    quantity_reserved INTEGER NOT NULL DEFAULT 0,
    quantity_sold INTEGER NOT NULL DEFAULT 0,
    sales_start_at TIMESTAMP,
    sales_end_at TIMESTAMP,
    access_zone VARCHAR(100),
    gate_instructions VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_ticket_type_config FOREIGN KEY (sale_configuration_id) 
        REFERENCES ticket_sale_configurations(id) ON DELETE CASCADE,
    CONSTRAINT chk_price CHECK (price >= 0),
    CONSTRAINT chk_quantity_total CHECK (quantity_total >= 1),
    CONSTRAINT chk_quantity_reserved CHECK (quantity_reserved >= 0),
    CONSTRAINT chk_quantity_sold CHECK (quantity_sold >= 0),
    CONSTRAINT chk_inventory_valid CHECK (quantity_reserved + quantity_sold <= quantity_total),
    CONSTRAINT chk_ticket_type_status CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'ENDED', 'CANCELLED'))
);

CREATE INDEX idx_ticket_type_config ON ticket_types(sale_configuration_id);
CREATE UNIQUE INDEX idx_ticket_type_code ON ticket_types(sale_configuration_id, code);

COMMENT ON TABLE ticket_types IS 'Types of tickets with inventory management and optimistic locking';
COMMENT ON COLUMN ticket_types.version IS 'Optimistic lock version for concurrency control';

-- ========================================
-- Ticket Holds (Temporary Reservations)
-- ========================================
CREATE TABLE ticket_holds (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    event_id VARCHAR(100) NOT NULL,
    ticket_type_id VARCHAR(36) NOT NULL,
    quantity INTEGER NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_hold_ticket_type FOREIGN KEY (ticket_type_id) 
        REFERENCES ticket_types(id) ON DELETE CASCADE,
    CONSTRAINT chk_hold_quantity CHECK (quantity BETWEEN 1 AND 20),
    CONSTRAINT chk_hold_status CHECK (status IN ('ACTIVE', 'CONFIRMED', 'RELEASED', 'EXPIRED'))
);

CREATE INDEX idx_hold_user ON ticket_holds(user_id);
CREATE INDEX idx_hold_event ON ticket_holds(event_id);
CREATE INDEX idx_hold_status_expires ON ticket_holds(status, expires_at);
CREATE UNIQUE INDEX idx_hold_idempotency ON ticket_holds(idempotency_key);

COMMENT ON TABLE ticket_holds IS 'Temporary inventory holds with TTL and idempotency';

-- ========================================
-- Ticket Orders
-- ========================================
CREATE TABLE ticket_orders (
    id VARCHAR(36) PRIMARY KEY,
    reference VARCHAR(50) NOT NULL UNIQUE,
    user_id VARCHAR(100) NOT NULL,
    partner_id VARCHAR(100) NOT NULL,
    event_id VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    payment_status VARCHAR(30) NOT NULL DEFAULT 'AWAITING_PAYMENT',
    subtotal NUMERIC(12, 2) NOT NULL,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    service_fee NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    promotion_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    
    CONSTRAINT chk_order_subtotal CHECK (subtotal >= 0),
    CONSTRAINT chk_order_discount CHECK (discount_amount >= 0),
    CONSTRAINT chk_order_service_fee CHECK (service_fee >= 0),
    CONSTRAINT chk_order_total CHECK (total_amount >= 0),
    CONSTRAINT chk_order_status CHECK (status IN ('CREATED', 'AWAITING_PAYMENT', 'PAID', 'ISSUED', 'CANCELLED', 'EXPIRED', 'REFUNDED', 'PARTIALLY_REFUNDED')),
    CONSTRAINT chk_payment_status CHECK (payment_status IN ('CREATED', 'AWAITING_PAYMENT', 'PAID', 'ISSUED', 'CANCELLED', 'EXPIRED', 'REFUNDED', 'PARTIALLY_REFUNDED'))
);

CREATE UNIQUE INDEX idx_order_reference ON ticket_orders(reference);
CREATE INDEX idx_order_user ON ticket_orders(user_id);
CREATE INDEX idx_order_event ON ticket_orders(event_id);
CREATE INDEX idx_order_partner ON ticket_orders(partner_id);
CREATE INDEX idx_order_status ON ticket_orders(status);
CREATE INDEX idx_order_expires ON ticket_orders(expires_at);

COMMENT ON TABLE ticket_orders IS 'Ticket purchase orders with payment lifecycle tracking';

-- ========================================
-- Tickets
-- ========================================
CREATE TABLE tickets (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    event_id VARCHAR(100) NOT NULL,
    ticket_type_id VARCHAR(36) NOT NULL,
    owner_user_id VARCHAR(100) NOT NULL,
    serial_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT',
    issued_at TIMESTAMP,
    used_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    refunded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_ticket_order FOREIGN KEY (order_id) 
        REFERENCES ticket_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_ticket_type FOREIGN KEY (ticket_type_id) 
        REFERENCES ticket_types(id),
    CONSTRAINT chk_ticket_status CHECK (status IN ('PENDING_PAYMENT', 'VALID', 'USED', 'CANCELLED', 'REFUNDED', 'EXPIRED', 'REVOKED'))
);

CREATE INDEX idx_ticket_order ON tickets(order_id);
CREATE INDEX idx_ticket_event ON tickets(event_id);
CREATE INDEX idx_ticket_type ON tickets(ticket_type_id);
CREATE INDEX idx_ticket_owner ON tickets(owner_user_id);
CREATE UNIQUE INDEX idx_ticket_serial ON tickets(serial_number);
CREATE INDEX idx_ticket_status ON tickets(status);

COMMENT ON TABLE tickets IS 'Individual tickets issued to users - contains no sensitive data';

-- ========================================
-- QR Credentials (Security)
-- ========================================
CREATE TABLE ticket_qr_credentials (
    id VARCHAR(36) PRIMARY KEY,
    ticket_id VARCHAR(36) NOT NULL UNIQUE,
    token_id VARCHAR(100) NOT NULL UNIQUE,
    token_hash VARCHAR(64) NOT NULL,
    key_id VARCHAR(50) NOT NULL,
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    
    CONSTRAINT fk_qr_ticket FOREIGN KEY (ticket_id) 
        REFERENCES tickets(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_qr_ticket ON ticket_qr_credentials(ticket_id);
CREATE UNIQUE INDEX idx_qr_token_id ON ticket_qr_credentials(token_id);
CREATE INDEX idx_qr_key ON ticket_qr_credentials(key_id);
CREATE INDEX idx_qr_revoked ON ticket_qr_credentials(revoked_at);

COMMENT ON TABLE ticket_qr_credentials IS 'Secure QR token metadata with asymmetric signatures';
COMMENT ON COLUMN ticket_qr_credentials.token_hash IS 'SHA-256 hash of token for validation without storing token';

-- ========================================
-- Event Staff Assignments
-- ========================================
CREATE TABLE event_staff_assignments (
    id VARCHAR(36) PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    partner_id VARCHAR(100) NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_staff_role CHECK (role IN ('EVENT_MANAGER', 'ACCESS_CONTROLLER', 'CASHIER', 'SUPERVISOR')),
    CONSTRAINT chk_staff_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED')),
    CONSTRAINT chk_staff_period CHECK (valid_until > valid_from)
);

CREATE INDEX idx_staff_event ON event_staff_assignments(event_id);
CREATE INDEX idx_staff_partner ON event_staff_assignments(partner_id);
CREATE INDEX idx_staff_user ON event_staff_assignments(user_id);
CREATE INDEX idx_staff_status ON event_staff_assignments(status);
CREATE UNIQUE INDEX idx_staff_event_user ON event_staff_assignments(event_id, user_id);

COMMENT ON TABLE event_staff_assignments IS 'Staff members assigned to events with roles and permissions';

-- ========================================
-- Ticket Scans (Audit Log)
-- ========================================
CREATE TABLE ticket_scans (
    id VARCHAR(36) PRIMARY KEY,
    ticket_id VARCHAR(36),
    event_id VARCHAR(100) NOT NULL,
    scanner_user_id VARCHAR(100) NOT NULL,
    staff_assignment_id VARCHAR(36),
    gate_id VARCHAR(100),
    result VARCHAR(30) NOT NULL,
    reason_code VARCHAR(100),
    scanned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    device_id_hash VARCHAR(64),
    offline_reference VARCHAR(100),
    
    CONSTRAINT fk_scan_ticket FOREIGN KEY (ticket_id) 
        REFERENCES tickets(id) ON DELETE SET NULL,
    CONSTRAINT fk_scan_staff FOREIGN KEY (staff_assignment_id) 
        REFERENCES event_staff_assignments(id) ON DELETE SET NULL,
    CONSTRAINT chk_scan_result CHECK (result IN ('VALID', 'ALREADY_USED', 'INVALID', 'EXPIRED', 'CANCELLED', 'REFUNDED', 'WRONG_EVENT', 'WRONG_GATE', 'NOT_YET_VALID', 'ACCESS_DENIED'))
);

CREATE INDEX idx_scan_ticket ON ticket_scans(ticket_id);
CREATE INDEX idx_scan_event ON ticket_scans(event_id);
CREATE INDEX idx_scan_scanner ON ticket_scans(scanner_user_id);
CREATE INDEX idx_scan_result ON ticket_scans(result);
CREATE INDEX idx_scan_time ON ticket_scans(scanned_at);
CREATE INDEX idx_scan_gate ON ticket_scans(gate_id);

COMMENT ON TABLE ticket_scans IS 'Audit log of all scan attempts for security and analytics';

-- ========================================
-- Outbox Events (Transactional Outbox Pattern)
-- ========================================
CREATE TABLE outbox_events (
    id VARCHAR(36) PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_outbox_published ON outbox_events(published);
CREATE INDEX idx_outbox_created ON outbox_events(created_at);
CREATE INDEX idx_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);

COMMENT ON TABLE outbox_events IS 'Transactional outbox for guaranteed Kafka event publishing';
