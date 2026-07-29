CREATE TABLE partner_validation_history (
 id UUID PRIMARY KEY,
 partner_id UUID NOT NULL REFERENCES partners(id),
 actor_id VARCHAR(100) NOT NULL,
 decision VARCHAR(50) NOT NULL,
 reason VARCHAR(1000),
 comment VARCHAR(1000),
 correlation_id VARCHAR(100),
 created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_partner_validation_history_partner_created ON partner_validation_history(partner_id, created_at DESC);
CREATE INDEX idx_partners_created_at ON partners(created_at);
CREATE INDEX idx_partners_contact_email ON partners(LOWER(contact_email));
