CREATE TABLE partners (
 id UUID PRIMARY KEY, owner_user_id VARCHAR(100) NOT NULL UNIQUE,
 legal_name VARCHAR(160) NOT NULL, trade_name VARCHAR(160), business_type VARCHAR(40) NOT NULL,
 registration_number VARCHAR(100), tax_id VARCHAR(100), contact_email VARCHAR(255) NOT NULL,
 contact_phone VARCHAR(40), website_url VARCHAR(2048), description VARCHAR(1000), status VARCHAR(40) NOT NULL,
 review_comment VARCHAR(1000), risk_score INTEGER, submitted_at TIMESTAMP WITH TIME ZONE,
 verified_at TIMESTAMP WITH TIME ZONE, created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 updated_at TIMESTAMP WITH TIME ZONE NOT NULL, deleted_at TIMESTAMP WITH TIME ZONE, version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_partners_status ON partners(status);
CREATE INDEX idx_partners_trade_name ON partners(LOWER(trade_name));
CREATE TABLE partner_documents (
 id UUID PRIMARY KEY, partner_id UUID NOT NULL REFERENCES partners(id), document_type VARCHAR(50) NOT NULL,
 storage_key VARCHAR(500) NOT NULL UNIQUE, original_filename VARCHAR(255) NOT NULL,
 content_type VARCHAR(100) NOT NULL, size_bytes BIGINT NOT NULL, uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_partner_documents_partner ON partner_documents(partner_id);
CREATE TABLE partner_processed_events (event_id UUID PRIMARY KEY, event_type VARCHAR(100) NOT NULL, processed_at TIMESTAMP WITH TIME ZONE NOT NULL);
CREATE TABLE partner_outbox_events (
 id UUID PRIMARY KEY, aggregate_id UUID NOT NULL, event_type VARCHAR(100) NOT NULL, payload TEXT NOT NULL,
 occurred_at TIMESTAMP WITH TIME ZONE NOT NULL, published_at TIMESTAMP WITH TIME ZONE,
 attempts INTEGER NOT NULL DEFAULT 0, last_error VARCHAR(1000)
);
CREATE INDEX idx_partner_outbox_unpublished ON partner_outbox_events(occurred_at) WHERE published_at IS NULL;
