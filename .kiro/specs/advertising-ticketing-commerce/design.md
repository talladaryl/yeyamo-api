# Design Document: Advertising, Ticketing & Commerce Platform

## Overview

Cette conception définit l'architecture technique pour l'extension de YeYamo avec quatre nouveaux bounded contexts: Campaign Service, Ads Delivery Service, Ticket Service et Commerce Service. L'approche est additive et rétrocompatible, s'intégrant harmonieusement avec les 26 microservices existants.

### Bounded Contexts

1. **Campaign Service**: Gère le cycle de vie des campagnes publicitaires (création, soumission, approbation, activation, budget)
2. **Ads Delivery Service**: Diffuse les publicités aux utilisateurs selon le ciblage et enregistre les métriques (impressions, clics, conversions)
3. **Ticket Service**: Gère la billetterie avec génération de QR codes et contrôle d'accès par agents
4. **Commerce Service**: Traite les paiements, applique les promotions, calcule les commissions et gère les règlements

### Design Principles

- **Bounded Context Isolation**: Aucun accès direct aux bases de données d'autres services
- **Event-Driven Communication**: Kafka pour la communication asynchrone interservices
- **Idempotency**: Toutes les opérations financières supportent Idempotency-Key
- **Transactional Outbox**: Publication d'événements garantie après commit DB
- **Inbox Pattern**: Déduplication des événements consommés via processed_events
- **Security First**: HTTPS, JWT, rate limiting, audit logs
- **Scalability**: Services stateless, horizontalement scalables
- **Observability**: Correlation-ID, structured logging, metrics Prometheus

## Architecture

### Context Map

```
┌──────────────────────────────────────────────────────────────────┐
│                         API Gateway (8083)                        │
│  - JWT Authentication                                             │
│  - Rate Limiting (Redis)                                          │
│  - CORS Headers                                                   │
│  - Correlation-ID Injection                                       │
└───────────────────────────────┬──────────────────────────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
┌───────▼─────────┐   ┌────────▼─────────┐   ┌────────▼─────────┐
│ Campaign Service│   │ Ads Delivery Svc │   │  Ticket Service  │
│    (Port TBD)   │   │    (Port TBD)    │   │    (Port TBD)    │
│                 │   │                  │   │                  │
│ PostgreSQL      │   │ Redis Cache      │   │ PostgreSQL       │
│ Kafka Producer  │   │ PostgreSQL       │   │ Kafka Producer   │
│                 │   │ Kafka Consumer   │   │                  │
└────────┬────────┘   └──────────┬───────┘   └─────────┬────────┘
         │                       │                      │
         │            ┌──────────▼──────────┐           │
         │            │  Commerce Service   │           │
         │            │    (Port TBD)       │           │
         │            │                     │           │
         │            │  PostgreSQL         │           │
         │            │  Kafka Producer     │           │
         └────────────┤  Kafka Consumer     ├───────────┘
                      │                     │
                      └──────────┬──────────┘
                                 │
        ┌────────────────────────┼────────────────────────┐
        │                        │                        │
┌───────▼─────────┐   ┌─────────▼────────┐   ┌──────────▼─────────┐
│ Payment Service │   │ Notification Svc │   │  Analytics Service │
│   (Existing)    │   │   (Existing)     │   │    (Existing)      │
└─────────────────┘   └──────────────────┘   └────────────────────┘

        ┌────────────────────────────────────────┐
        │   Integration with Existing Services   │
        ├────────────────────────────────────────┤
        │ • Catalog Service (places)             │
        │ • Event Service (events)               │
        │ • User Service (profiles, follow)      │
        │ • Partner Service (partner management) │
        │ • Feed Service (ad placement)          │
        │ • Discovery Service (search ads)       │
        │ • Admin Service (campaign moderation)  │
        │ • Booking Service (capacity reserve)   │
        └────────────────────────────────────────┘
```

### Service Responsibilities

| Service | Responsibilities | External Dependencies |
|---------|------------------|----------------------|
| **campaign-service** | • Campaign CRUD<br>• Budget tracking<br>• Status workflow (draft→submitted→approved→active→paused→completed)<br>• Creative asset management<br>• Targeting configuration<br>• Performance aggregation | • catalog-service (validate places)<br>• event-service (validate events)<br>• partner-service (validate advertiser)<br>• admin-service (approval workflow) |
| **ads-delivery-service** | • Ad selection algorithm<br>• Geographic targeting<br>• Interest-based targeting<br>• Frequency capping<br>• Impression tracking<br>• Click tracking<br>• Conversion tracking<br>• Budget deduction | • feed-service (ad injection)<br>• discovery-service (search ads)<br>• user-service (user preferences)<br>• campaign-service (active campaigns)<br>• analytics-service (metrics push) |
| **ticket-service** | • Ticket type CRUD<br>• Order management<br>• QR code generation<br>• Ticket issuance<br>• Scan validation<br>• Agent access control<br>• Refund processing<br>• PDF ticket generation | • event-service (event validation)<br>• commerce-service (payment status)<br>• notification-service (ticket delivery)<br>• partner-service (agent management) |
| **commerce-service** | • Order creation<br>• Promotion validation & application<br>• Payment orchestration<br>• Commission calculation<br>• Settlement management<br>• Refund processing<br>• Transaction audit | • ticket-service (ticket orders)<br>• payment-service (payment processing)<br>• partner-service (commission rates)<br>• notification-service (order confirmations)<br>• analytics-service (revenue tracking) |



## Components and Interfaces

### Campaign Service

#### REST Endpoints

```
# Campaign Management
POST   /api/v1/campaigns                    # Create campaign (PARTNER, ADMIN)
GET    /api/v1/campaigns                    # List campaigns (paginated, filtered)
GET    /api/v1/campaigns/{id}               # Get campaign details
PUT    /api/v1/campaigns/{id}               # Update campaign (DRAFT only)
DELETE /api/v1/campaigns/{id}               # Delete campaign (DRAFT only)

# Campaign Workflow
POST   /api/v1/campaigns/{id}/submit        # Submit for approval
POST   /api/v1/campaigns/{id}/approve       # Approve campaign (ADMIN)
POST   /api/v1/campaigns/{id}/reject        # Reject campaign (ADMIN)
POST   /api/v1/campaigns/{id}/activate      # Activate approved campaign
POST   /api/v1/campaigns/{id}/pause         # Pause active campaign
POST   /api/v1/campaigns/{id}/resume        # Resume paused campaign

# Analytics
GET    /api/v1/campaigns/{id}/performance   # Get performance metrics
GET    /api/v1/campaigns/{id}/budget-status # Get budget utilization

# Creative Assets
POST   /api/v1/campaigns/{id}/creatives     # Upload creative asset
GET    /api/v1/campaigns/{id}/creatives     # List creatives
DELETE /api/v1/campaigns/{id}/creatives/{creativeId} # Delete creative
```

#### Database Schema (PostgreSQL)

```sql
-- Campaigns
CREATE TABLE campaigns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    advertiser_id VARCHAR(255) NOT NULL,  -- Partner or User ID
    advertiser_type VARCHAR(50) NOT NULL, -- PARTNER, USER
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,          -- DRAFT, PENDING_REVIEW, APPROVED, REJECTED, ACTIVE, PAUSED, COMPLETED
    budget_total DECIMAL(19,4) NOT NULL,
    budget_spent DECIMAL(19,4) DEFAULT 0,
    budget_daily DECIMAL(19,4),
    currency VARCHAR(3) DEFAULT 'XAF',
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    targeting JSONB NOT NULL,             -- Geographic + Interest targeting
    objective VARCHAR(50),                -- AWARENESS, TRAFFIC, CONVERSIONS
    rejection_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version INT DEFAULT 1,
    CHECK (budget_total > 0),
    CHECK (budget_spent >= 0),
    CHECK (budget_spent <= budget_total),
    CHECK (end_date > start_date)
);

CREATE INDEX idx_campaigns_advertiser ON campaigns(advertiser_id);
CREATE INDEX idx_campaigns_status ON campaigns(status);
CREATE INDEX idx_campaigns_dates ON campaigns(start_date, end_date);

-- Creative Assets
CREATE TABLE campaign_creatives (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,            -- IMAGE, VIDEO, CAROUSEL
    media_url VARCHAR(500) NOT NULL,
    title VARCHAR(255),
    description TEXT,
    call_to_action VARCHAR(50),           -- LEARN_MORE, BOOK_NOW, VISIT, CALL
    target_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_creatives_campaign ON campaign_creatives(campaign_id);

-- Targeting Configuration (embedded in campaigns.targeting JSONB)
-- {
--   "geographic": {
--     "type": "RADIUS" | "CITY" | "REGION",
--     "center": {"lat": 3.848, "lng": 11.502},
--     "radius_km": 10,
--     "city_ids": ["uuid1", "uuid2"],
--     "region_ids": ["uuid3"]
--   },
--   "interests": ["RESTAURANTS", "NIGHTLIFE", "CULTURE"],
--   "demographics": {
--     "age_min": 18,
--     "age_max": 65
--   }
-- }

-- Budget Tracking
CREATE TABLE campaign_budget_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    amount DECIMAL(19,4) NOT NULL,
    event_type VARCHAR(50) NOT NULL,     -- IMPRESSION, CLICK, CONVERSION
    event_id UUID NOT NULL,
    occurred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_budget_logs_campaign ON campaign_budget_logs(campaign_id, occurred_at);

-- Outbox for Event Sourcing
CREATE TABLE campaign_outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_campaign_outbox_unpublished ON campaign_outbox_events(published, created_at) WHERE published = FALSE;
```

#### Kafka Events Produced

```yaml
# campaign.events topic
- campaign.created
- campaign.updated
- campaign.submitted
- campaign.approved
- campaign.rejected
- campaign.activated
- campaign.paused
- campaign.resumed
- campaign.completed
- campaign.budget.exhausted
- campaign.creative.uploaded

# Event Envelope Example
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "campaign.activated",
  "eventVersion": 1,
  "occurredAt": "2026-08-02T10:30:00Z",
  "producer": "campaign-service",
  "aggregateType": "Campaign",
  "aggregateId": "123e4567-e89b-12d3-a456-426614174000",
  "correlationId": "abc-123-def",
  "causationId": "previous-event-id",
  "actorId": "user:partner123",
  "payload": {
    "campaignId": "123e4567-e89b-12d3-a456-426614174000",
    "advertiserId": "partner123",
    "name": "Summer Promo 2026",
    "budgetTotal": 500000.00,
    "currency": "XAF",
    "startDate": "2026-08-05T00:00:00Z",
    "endDate": "2026-08-31T23:59:59Z",
    "targeting": { /* targeting config */ }
  }
}
```

### Ads Delivery Service

#### REST Endpoints

```
# Ad Delivery (Internal - called by Feed/Discovery)
POST   /api/v1/ads/request                 # Request ads for placement
POST   /api/v1/ads/impression              # Record impression
POST   /api/v1/ads/click                   # Record click & get redirect URL
POST   /api/v1/ads/conversion              # Record conversion

# Configuration (Admin)
GET    /api/v1/ads/config                  # Get delivery config
PUT    /api/v1/ads/config                  # Update delivery config
```

#### Database Schema (PostgreSQL + Redis)

```sql
-- Ad Impressions (Time-series data, partitioned by month)
CREATE TABLE ad_impressions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID NOT NULL,
    creative_id UUID NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    session_id VARCHAR(255),
    context VARCHAR(50) NOT NULL,        -- FEED, DISCOVERY, STORY
    placement VARCHAR(50),               -- TOP, MIDDLE, SIDEBAR
    device_type VARCHAR(50),             -- MOBILE, TABLET, DESKTOP
    user_location JSONB,                 -- {lat, lng, city, region}
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

CREATE TABLE ad_impressions_2026_08 PARTITION OF ad_impressions
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');

CREATE INDEX idx_impressions_campaign ON ad_impressions_2026_08(campaign_id, created_at);
CREATE INDEX idx_impressions_user ON ad_impressions_2026_08(user_id, created_at);

-- Ad Clicks
CREATE TABLE ad_clicks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    impression_id UUID REFERENCES ad_impressions(id),
    campaign_id UUID NOT NULL,
    creative_id UUID NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    target_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

CREATE TABLE ad_clicks_2026_08 PARTITION OF ad_clicks
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');

CREATE INDEX idx_clicks_campaign ON ad_clicks_2026_08(campaign_id, created_at);

-- Ad Conversions
CREATE TABLE ad_conversions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    click_id UUID REFERENCES ad_clicks(id),
    campaign_id UUID NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    conversion_type VARCHAR(50) NOT NULL, -- PURCHASE, SIGNUP, BOOKING, VISIT
    conversion_value DECIMAL(19,4),
    currency VARCHAR(3),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

CREATE TABLE ad_conversions_2026_08 PARTITION OF ad_conversions
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');

-- Frequency Capping (Redis)
-- Key: "ad:frequency:{user_id}:{campaign_id}"
-- Value: impression count
-- TTL: 24 hours

-- Active Campaigns Cache (Redis)
-- Key: "campaigns:active"
-- Value: JSON array of active campaign IDs
-- TTL: 5 minutes

-- Processed Events (Idempotency)
CREATE TABLE ads_processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ads_processed_event_type ON ads_processed_events(event_type, processed_at);
```

#### Kafka Events

**Consumed:**
- `campaign.events`: campaign.activated, campaign.paused, campaign.completed, campaign.budget.exhausted

**Produced:**
- `ad.events`: ad.impression.recorded, ad.click.recorded, ad.conversion.recorded, ad.delivery.rejected

```yaml
# ad.impression.recorded
{
  "eventId": "uuid",
  "eventType": "ad.impression.recorded",
  "eventVersion": 1,
  "occurredAt": "2026-08-02T10:35:00Z",
  "producer": "ads-delivery-service",
  "aggregateType": "AdImpression",
  "aggregateId": "impression-uuid",
  "correlationId": "request-correlation-id",
  "actorId": "user:user123",
  "payload": {
    "impressionId": "impression-uuid",
    "campaignId": "campaign-uuid",
    "creativeId": "creative-uuid",
    "userId": "user123",
    "context": "FEED",
    "placement": "TOP",
    "deviceType": "MOBILE",
    "userLocation": {"lat": 3.848, "lng": 11.502, "city": "Yaounde"}
  }
}
```

### Ticket Service

#### REST Endpoints

```
# Ticket Type Management
POST   /api/v1/ticket-types                # Create ticket type (PARTNER)
GET    /api/v1/ticket-types                # List ticket types (filtered by event)
GET    /api/v1/ticket-types/{id}           # Get ticket type details
PUT    /api/v1/ticket-types/{id}           # Update ticket type
DELETE /api/v1/ticket-types/{id}           # Delete ticket type (if no sales)

# Ticket Purchase (User flow)
POST   /api/v1/tickets/orders              # Create ticket order
GET    /api/v1/tickets/orders/{id}         # Get order status
GET    /api/v1/tickets/my-orders           # List user's orders
GET    /api/v1/tickets/my-tickets          # List user's tickets
GET    /api/v1/tickets/{id}                # Get ticket details
GET    /api/v1/tickets/{id}/download       # Download PDF ticket
POST   /api/v1/tickets/{id}/transfer       # Transfer ticket to another user

# Ticket Scanning (Agent flow)
POST   /api/v1/tickets/scan                # Scan QR code
GET    /api/v1/tickets/scan-history        # Agent's scan history

# Agent Management (Partner)
POST   /api/v1/agents                      # Create agent account
GET    /api/v1/agents                      # List partner's agents
PUT    /api/v1/agents/{id}                 # Update agent
DELETE /api/v1/agents/{id}                 # Deactivate agent
POST   /api/v1/agents/{id}/grant-access   # Grant event access
DELETE /api/v1/agents/{id}/revoke-access  # Revoke event access

# Refunds (User/Partner)
POST   /api/v1/tickets/{id}/refund-request # Request refund
GET    /api/v1/tickets/refund-requests     # List refund requests (Partner/Admin)
POST   /api/v1/tickets/{id}/refund-approve # Approve refund (Partner/Admin)
```

#### Database Schema (PostgreSQL)

```sql
-- Ticket Types
CREATE TABLE ticket_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,              -- Foreign key to event-service
    partner_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'XAF',
    quantity_total INT NOT NULL,
    quantity_sold INT DEFAULT 0,
    sale_start TIMESTAMP NOT NULL,
    sale_end TIMESTAMP NOT NULL,
    refund_policy VARCHAR(50) DEFAULT 'NO_REFUND', -- NO_REFUND, BEFORE_7_DAYS, BEFORE_24_HOURS
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CHECK (price >= 0),
    CHECK (quantity_total > 0),
    CHECK (quantity_sold >= 0),
    CHECK (quantity_sold <= quantity_total),
    CHECK (sale_end > sale_start)
);

CREATE INDEX idx_ticket_types_event ON ticket_types(event_id);
CREATE INDEX idx_ticket_types_partner ON ticket_types(partner_id);

-- Ticket Orders
CREATE TABLE ticket_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    event_id UUID NOT NULL,
    total_amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'XAF',
    status VARCHAR(50) NOT NULL,         -- PENDING, PAID, CANCELLED, REFUNDED
    payment_id UUID,                     -- Reference to payment-service
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_user ON ticket_orders(user_id, created_at DESC);
CREATE INDEX idx_orders_event ON ticket_orders(event_id);

-- Order Items
CREATE TABLE ticket_order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES ticket_orders(id) ON DELETE CASCADE,
    ticket_type_id UUID NOT NULL REFERENCES ticket_types(id),
    quantity INT NOT NULL,
    unit_price DECIMAL(19,4) NOT NULL,
    subtotal DECIMAL(19,4) NOT NULL,
    CHECK (quantity > 0),
    CHECK (unit_price >= 0),
    CHECK (subtotal = quantity * unit_price)
);

CREATE INDEX idx_order_items_order ON ticket_order_items(order_id);

-- Tickets (issued after payment)
CREATE TABLE tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES ticket_orders(id),
    ticket_type_id UUID NOT NULL REFERENCES ticket_types(id),
    holder_id VARCHAR(255) NOT NULL,     -- User who owns the ticket
    event_id UUID NOT NULL,
    qr_code_data TEXT NOT NULL UNIQUE,   -- Encrypted QR payload
    status VARCHAR(50) NOT NULL,         -- VALID, USED, CANCELLED, REFUNDED
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    scanned_at TIMESTAMP,
    scanned_by VARCHAR(255),             -- Agent ID who scanned
    CHECK (qr_code_data <> '')
);

CREATE INDEX idx_tickets_order ON tickets(order_id);
CREATE INDEX idx_tickets_holder ON tickets(holder_id);
CREATE INDEX idx_tickets_event ON tickets(event_id);
CREATE UNIQUE INDEX idx_tickets_qr ON tickets(qr_code_data);

-- Agents
CREATE TABLE agents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_agents_partner ON agents(partner_id);

-- Agent Event Access
CREATE TABLE agent_event_access (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id UUID NOT NULL REFERENCES agents(id) ON DELETE CASCADE,
    event_id UUID NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    UNIQUE(agent_id, event_id)
);

CREATE INDEX idx_agent_access_agent ON agent_event_access(agent_id);
CREATE INDEX idx_agent_access_event ON agent_event_access(event_id);

-- Scan Logs (Audit trail)
CREATE TABLE ticket_scan_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES tickets(id),
    agent_id UUID NOT NULL REFERENCES agents(id),
    event_id UUID NOT NULL,
    scan_result VARCHAR(50) NOT NULL,    -- SUCCESS, ALREADY_USED, INVALID, EXPIRED
    device_info JSONB,
    location JSONB,
    scanned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_scan_logs_ticket ON ticket_scan_logs(ticket_id, scanned_at);
CREATE INDEX idx_scan_logs_agent ON ticket_scan_logs(agent_id, scanned_at);
CREATE INDEX idx_scan_logs_event ON ticket_scan_logs(event_id, scanned_at);

-- Outbox
CREATE TABLE ticket_outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ticket_outbox_unpublished ON ticket_outbox_events(published, created_at) WHERE published = FALSE;
```

#### QR Code Format

```json
{
  "v": 1,                              // Version
  "t": "ticket-uuid",                  // Ticket ID
  "e": "event-uuid",                   // Event ID
  "h": "user-uuid",                    // Holder ID
  "i": "2026-08-02T10:00:00Z",         // Issued at
  "s": "HMAC-SHA256-signature"         // Signature
}
```

**Signature Calculation:**
```
data = "v=1&t={ticket_id}&e={event_id}&h={holder_id}&i={issued_at}"
signature = HMAC-SHA256(data, SECRET_KEY)
qr_code_data = Base64(json_payload)
```

#### Kafka Events

**Consumed:**
- `payment.events`: payment.confirmed, payment.failed

**Produced:**
- `ticket.events`: ticket.type.created, ticket.order.created, ticket.issued, ticket.scanned, ticket.validated, ticket.scan.rejected, ticket.cancelled, ticket.refunded

### Commerce Service

#### REST Endpoints

```
# Order Management
POST   /api/v1/orders                      # Create order
GET    /api/v1/orders/{id}                 # Get order details
GET    /api/v1/orders/my-orders            # List user's orders
POST   /api/v1/orders/{id}/cancel          # Cancel order

# Promotions
POST   /api/v1/promotions                  # Create promotion (PARTNER, ADMIN)
GET    /api/v1/promotions                  # List promotions
GET    /api/v1/promotions/{id}             # Get promotion details
PUT    /api/v1/promotions/{id}             # Update promotion
DELETE /api/v1/promotions/{id}             # Deactivate promotion
POST   /api/v1/promotions/validate         # Validate promotion code

# Settlements (Partner)
GET    /api/v1/settlements                 # List settlements
GET    /api/v1/settlements/{id}            # Get settlement details
GET    /api/v1/settlements/balance         # Get current balance

# Admin
GET    /api/v1/admin/transactions          # List all transactions
GET    /api/v1/admin/commissions           # Commission reports
POST   /api/v1/admin/settlements/process   # Process settlements
```

#### Database Schema (PostgreSQL)

```sql
-- Orders
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    order_type VARCHAR(50) NOT NULL,     -- TICKET_PURCHASE, SERVICE_BOOKING
    total_amount DECIMAL(19,4) NOT NULL,
    discount_amount DECIMAL(19,4) DEFAULT 0,
    final_amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'XAF',
    status VARCHAR(50) NOT NULL,         -- PENDING, PAID, FAILED, CANCELLED, REFUNDED
    promotion_code VARCHAR(50),
    idempotency_key VARCHAR(255) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CHECK (total_amount >= 0),
    CHECK (discount_amount >= 0),
    CHECK (final_amount >= 0),
    CHECK (final_amount = total_amount - discount_amount)
);

CREATE INDEX idx_orders_user ON orders(user_id, created_at DESC);
CREATE INDEX idx_orders_status ON orders(status);
CREATE UNIQUE INDEX idx_orders_idempotency ON orders(idempotency_key) WHERE idempotency_key IS NOT NULL;

-- Promotions
CREATE TABLE promotions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    partner_id VARCHAR(255),             -- NULL = platform-wide
    discount_type VARCHAR(50) NOT NULL,  -- PERCENTAGE, FIXED_AMOUNT
    discount_value DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'XAF',
    min_purchase_amount DECIMAL(19,4),
    max_discount_amount DECIMAL(19,4),
    usage_limit INT,
    usage_count INT DEFAULT 0,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    targeting JSONB,                     -- Event types, categories, specific events
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CHECK (discount_value > 0),
    CHECK (usage_count >= 0),
    CHECK (usage_limit IS NULL OR usage_count <= usage_limit),
    CHECK (end_date > start_date)
);

CREATE UNIQUE INDEX idx_promotions_code ON promotions(code);
CREATE INDEX idx_promotions_active ON promotions(is_active, start_date, end_date);

-- Promotion Usage
CREATE TABLE promotion_usages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    promotion_id UUID NOT NULL REFERENCES promotions(id),
    order_id UUID NOT NULL REFERENCES orders(id),
    user_id VARCHAR(255) NOT NULL,
    discount_applied DECIMAL(19,4) NOT NULL,
    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_promo_usage_promotion ON promotion_usages(promotion_id);
CREATE INDEX idx_promo_usage_user ON promotion_usages(user_id);

-- Transactions (Audit log)
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id),
    payment_id UUID,                     -- From payment-service
    transaction_type VARCHAR(50) NOT NULL, -- CHARGE, REFUND, COMMISSION
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'XAF',
    status VARCHAR(50) NOT NULL,         -- SUCCESS, FAILED, PENDING
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transactions_order ON transactions(order_id);
CREATE INDEX idx_transactions_payment ON transactions(payment_id);

-- Commissions
CREATE TABLE commissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id),
    partner_id VARCHAR(255) NOT NULL,
    gross_amount DECIMAL(19,4) NOT NULL,
    commission_rate DECIMAL(5,4) NOT NULL, -- e.g., 0.15 for 15%
    commission_amount DECIMAL(19,4) NOT NULL,
    net_amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'XAF',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CHECK (commission_rate >= 0 AND commission_rate <= 1),
    CHECK (commission_amount = gross_amount * commission_rate),
    CHECK (net_amount = gross_amount - commission_amount)
);

CREATE INDEX idx_commissions_partner ON commissions(partner_id, created_at);
CREATE INDEX idx_commissions_order ON commissions(order_id);

-- Settlements
CREATE TABLE settlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id VARCHAR(255) NOT NULL,
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    total_commissions DECIMAL(19,4) NOT NULL,
    total_net_amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'XAF',
    status VARCHAR(50) NOT NULL,         -- PENDING, PROCESSING, COMPLETED, FAILED
    payout_reference VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_settlements_partner ON settlements(partner_id, created_at DESC);
CREATE INDEX idx_settlements_status ON settlements(status);

-- Settlement Line Items
CREATE TABLE settlement_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    settlement_id UUID NOT NULL REFERENCES settlements(id) ON DELETE CASCADE,
    commission_id UUID NOT NULL REFERENCES commissions(id),
    order_id UUID NOT NULL,
    gross_amount DECIMAL(19,4) NOT NULL,
    commission_amount DECIMAL(19,4) NOT NULL,
    net_amount DECIMAL(19,4) NOT NULL
);

CREATE INDEX idx_settlement_items_settlement ON settlement_items(settlement_id);

-- Outbox
CREATE TABLE commerce_outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_commerce_outbox_unpublished ON commerce_outbox_events(published, created_at) WHERE published = FALSE;

-- Inbox (Idempotency for consumed events)
CREATE TABLE commerce_processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Kafka Events

**Consumed:**
- `ticket.events`: ticket.order.created
- `payment.events`: payment.confirmed, payment.failed

**Produced:**
- `commerce.events`: order.created, payment.requested, payment.confirmed, payment.failed, commission.calculated, settlement.created, settlement.completed, promotion.applied



## Data Models

### Campaign Domain

```java
// Campaign.java (Aggregate Root)
@Entity
@Table(name = "campaigns")
public class Campaign {
    @Id
    private UUID id;
    private String advertiserId;
    
    @Enumerated(EnumType.STRING)
    private AdvertiserType advertiserType; // PARTNER, USER
    
    private String name;
    private String description;
    
    @Enumerated(EnumType.STRING)
    private CampaignStatus status; // DRAFT, PENDING_REVIEW, APPROVED, REJECTED, ACTIVE, PAUSED, COMPLETED
    
    @Column(precision = 19, scale = 4)
    private BigDecimal budgetTotal;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal budgetSpent;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal budgetDaily;
    
    private String currency; // Default: "XAF"
    
    private Instant startDate;
    private Instant endDate;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private CampaignTargeting targeting;
    
    @Enumerated(EnumType.STRING)
    private CampaignObjective objective; // AWARENESS, TRAFFIC, CONVERSIONS
    
    private String rejectionReason;
    
    private Instant createdAt;
    private Instant updatedAt;
    
    @Version
    private Integer version;
    
    // Business methods
    public void submit() { /* transition to PENDING_REVIEW */ }
    public void approve() { /* transition to APPROVED */ }
    public void reject(String reason) { /* transition to REJECTED */ }
    public void activate() { /* transition to ACTIVE */ }
    public void pause() { /* transition to PAUSED */ }
    public void resume() { /* transition to ACTIVE */ }
    public void complete() { /* transition to COMPLETED */ }
    public void deductBudget(BigDecimal amount) { /* update budgetSpent */ }
    public boolean isBudgetExhausted() { /* check budget */ }
}

// CampaignTargeting.java (Value Object)
public class CampaignTargeting {
    private GeographicTargeting geographic;
    private List<String> interests;
    private DemographicTargeting demographics;
}

public class GeographicTargeting {
    private TargetingType type; // RADIUS, CITY, REGION
    private GeoPoint center;
    private Double radiusKm;
    private List<UUID> cityIds;
    private List<UUID> regionIds;
}
```

### Ticket Domain

```java
// TicketType.java (Aggregate Root)
@Entity
@Table(name = "ticket_types")
public class TicketType {
    @Id
    private UUID id;
    private UUID eventId;
    private String partnerId;
    private String name;
    private String description;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal price;
    
    private String currency;
    private Integer quantityTotal;
    private Integer quantitySold;
    private Instant saleStart;
    private Instant saleEnd;
    
    @Enumerated(EnumType.STRING)
    private RefundPolicy refundPolicy; // NO_REFUND, BEFORE_7_DAYS, BEFORE_24_HOURS
    
    private Boolean isActive;
    
    // Business methods
    public boolean isAvailable() { /* check quantity, dates */ }
    public void reserveQuantity(int quantity) { /* atomic increment */ }
    public void releaseQuantity(int quantity) { /* atomic decrement */ }
}

// Ticket.java (Aggregate Root)
@Entity
@Table(name = "tickets")
public class Ticket {
    @Id
    private UUID id;
    private UUID orderId;
    private UUID ticketTypeId;
    private String holderId;
    private UUID eventId;
    private String qrCodeData;
    
    @Enumerated(EnumType.STRING)
    private TicketStatus status; // VALID, USED, CANCELLED, REFUNDED
    
    private Instant issuedAt;
    private Instant scannedAt;
    private String scannedBy;
    
    // Business methods
    public void scan(String agentId) { /* mark as USED */ }
    public boolean canBeScanned() { /* check status */ }
    public void cancel() { /* mark as CANCELLED */ }
    public void refund() { /* mark as REFUNDED */ }
}

// QRCodePayload.java (Value Object)
public class QRCodePayload {
    private Integer version;
    private UUID ticketId;
    private UUID eventId;
    private String holderId;
    private Instant issuedAt;
    private String signature;
    
    public static QRCodePayload generate(Ticket ticket, String secretKey) { /* ... */ }
    public boolean verify(String secretKey) { /* HMAC verification */ }
    public String toBase64() { /* serialize to QR code */ }
    public static QRCodePayload fromBase64(String data) { /* deserialize */ }
}
```

### Commerce Domain

```java
// Order.java (Aggregate Root)
@Entity
@Table(name = "orders")
public class Order {
    @Id
    private UUID id;
    private String userId;
    
    @Enumerated(EnumType.STRING)
    private OrderType orderType; // TICKET_PURCHASE, SERVICE_BOOKING
    
    @Column(precision = 19, scale = 4)
    private BigDecimal totalAmount;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal discountAmount;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal finalAmount;
    
    private String currency;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status; // PENDING, PAID, FAILED, CANCELLED, REFUNDED
    
    private String promotionCode;
    private String idempotencyKey;
    
    private Instant createdAt;
    private Instant updatedAt;
    
    // Business methods
    public void applyPromotion(Promotion promotion) { /* calculate discount */ }
    public void confirmPayment(UUID paymentId) { /* update status */ }
    public void failPayment() { /* update status */ }
    public void cancel() { /* update status */ }
}

// Promotion.java (Aggregate Root)
@Entity
@Table(name = "promotions")
public class Promotion {
    @Id
    private UUID id;
    private String code;
    private String partnerId;
    
    @Enumerated(EnumType.STRING)
    private DiscountType discountType; // PERCENTAGE, FIXED_AMOUNT
    
    @Column(precision = 19, scale = 4)
    private BigDecimal discountValue;
    
    private String currency;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal minPurchaseAmount;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal maxDiscountAmount;
    
    private Integer usageLimit;
    private Integer usageCount;
    private Instant startDate;
    private Instant endDate;
    private Boolean isActive;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private PromotionTargeting targeting;
    
    // Business methods
    public boolean isValid() { /* check active, dates, usage */ }
    public BigDecimal calculateDiscount(BigDecimal orderAmount) { /* apply discount */ }
    public void incrementUsage() { /* atomic increment */ }
}

// Commission.java (Entity)
@Entity
@Table(name = "commissions")
public class Commission {
    @Id
    private UUID id;
    private UUID orderId;
    private String partnerId;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal grossAmount;
    
    @Column(precision = 5, scale = 4)
    private BigDecimal commissionRate;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal commissionAmount;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal netAmount;
    
    private String currency;
    private Instant createdAt;
    
    public static Commission calculate(UUID orderId, String partnerId, BigDecimal gross, BigDecimal rate) {
        BigDecimal commission = gross.multiply(rate);
        BigDecimal net = gross.subtract(commission);
        return new Commission(orderId, partnerId, gross, rate, commission, net);
    }
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Campaign Budget Integrity

*For any* campaign, the sum of all budget deductions (from impressions, clicks, conversions) should always equal the budgetSpent field, and budgetSpent should never exceed budgetTotal.

**Validates: Requirements 1.6, 1.7**

### Property 2: Campaign Status Transitions

*For any* campaign state transition, the new status should be reachable from the current status according to the defined workflow (DRAFT → PENDING_REVIEW → APPROVED → ACTIVE → {PAUSED, COMPLETED}).

**Validates: Requirements 1.2, 1.3, 1.4, 1.5, 1.8**

### Property 3: Ad Targeting Consistency

*For any* ad impression recorded, the user's location and interests should match the campaign's targeting criteria within configured tolerance (e.g., radius, interest overlap).

**Validates: Requirements 2.2, 2.3**

### Property 4: Frequency Capping Enforcement

*For any* user and campaign, the number of ad impressions shown to that user in a 24-hour period should not exceed the configured frequency cap.

**Validates: Requirements 2.9**

### Property 5: Ticket Uniqueness

*For any* two tickets, their QR code data should be unique, ensuring no duplicate tickets exist in the system.

**Validates: Requirements 3.5**

### Property 6: Ticket Scan Idempotency

*For any* valid ticket, scanning it multiple times should only mark it as USED once, and subsequent scans should be rejected with ALREADY_USED reason.

**Validates: Requirements 3.7, 3.8**

### Property 7: QR Code Signature Verification

*For any* QR code scanned, the signature should verify correctly using the secret key, and tampering should result in rejection.

**Validates: Requirements 3.6**

### Property 8: Agent Authorization

*For any* ticket scan attempt, the agent should have active permission for the event associated with that ticket, otherwise the scan should be rejected with UNAUTHORIZED.

**Validates: Requirements 6.3, 6.4**

### Property 9: Order Amount Calculation

*For any* order, the final_amount should always equal total_amount minus discount_amount, and all amounts should be non-negative.

**Validates: Requirements 4.2**

### Property 10: Promotion Validation

*For any* promotion code applied to an order, the promotion should be active, not expired, within usage limits, and meet minimum purchase requirements.

**Validates: Requirements 5.2, 5.4**

### Property 11: Promotion Usage Atomicity

*For any* promotion with usage limits, concurrent applications should not exceed the max usage count due to race conditions.

**Validates: Requirements 5.5**

### Property 12: Commission Calculation

*For any* paid order, the commission amount should equal gross_amount multiplied by commission_rate, and net_amount should equal gross_amount minus commission_amount.

**Validates: Requirements 4.6**

### Property 13: Settlement Integrity

*For any* settlement, the total net amount should equal the sum of all commission net amounts for that partner during the settlement period.

**Validates: Requirements 4.8**

### Property 14: Payment Idempotency

*For any* order with an idempotency key, retrying the payment request with the same key should not create duplicate charges.

**Validates: Requirements 4.3, 4.13**

### Property 15: Event Idempotency

*For any* Kafka event consumed, processing it multiple times (due to retries) should only trigger domain logic once, verified by eventId in processed_events table.

**Validates: Requirements 10.3, 10.4**

### Property 16: Outbox Atomicity

*For any* database write that produces events, the event should be stored in the outbox table within the same transaction, ensuring no events are lost.

**Validates: Requirements 10.1, 4.12**

### Property 17: Timestamp Consistency

*For any* entity created, the createdAt timestamp should be stored in UTC and be less than or equal to the current time.

**Validates: Requirements 10.8**

### Property 18: Currency Consistency

*For any* financial transaction, all amounts involved (order, payment, commission) should use the same currency.

**Validates: Requirements 4.10**

### Property 19: Ticket Quantity Reservation

*For any* ticket type, the quantity_sold should never exceed quantity_total, even under concurrent purchase attempts.

**Validates: Requirements 3.2**

### Property 20: Refund Eligibility

*For any* ticket refund request, the refund should only be approved if the ticket status is VALID and the refund policy allows refunds before the current time.

**Validates: Requirements 3.10**

## Error Handling

### Error Response Format

All services return consistent error responses following the format:

```json
{
  "timestamp": "2026-08-02T10:45:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid campaign budget: budget must be positive",
  "path": "/api/v1/campaigns",
  "correlationId": "abc-123-def"
}
```

### Error Categories

| HTTP Status | Category | Examples |
|------------|----------|----------|
| 400 | Bad Request | Invalid input, validation errors |
| 401 | Unauthorized | Missing or invalid JWT token |
| 403 | Forbidden | Insufficient permissions (wrong role) |
| 404 | Not Found | Resource does not exist |
| 409 | Conflict | Duplicate key, optimistic lock failure, promotion already used |
| 422 | Unprocessable Entity | Business rule violation (budget exhausted, ticket sold out) |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Unexpected errors, caught and logged |
| 503 | Service Unavailable | Downstream service timeout, circuit breaker open |

### Retry Strategy

**Transient Errors (5xx, network failures):**
- Exponential backoff: 1s, 2s, 4s, 8s, 16s
- Max retries: 5
- Jitter: ±20% to prevent thundering herd

**Non-Transient Errors (4xx):**
- No retry
- Return error to caller immediately

**Kafka Event Processing:**
- Max retries: 5
- Backoff: Exponential (1s to 32s)
- Dead Letter Topic: Publish to `{topic}.DLT` after max retries

### Circuit Breaker Pattern

Applied to external service calls (catalog-service, event-service, payment-service):

```yaml
circuitBreaker:
  failureRateThreshold: 50%        # Open if >50% of calls fail
  slowCallRateThreshold: 100%      # Open if >100% of calls are slow
  slowCallDurationThreshold: 2s    # Call is "slow" if >2s
  waitDurationInOpenState: 10s     # Stay open for 10s before half-open
  permittedNumberOfCallsInHalfOpen: 3 # Test with 3 calls in half-open
  slidingWindowSize: 10            # Track last 10 calls
```

## Testing Strategy

### Unit Testing

**Framework:** JUnit 5 + Mockito + AssertJ  
**Coverage Target:** 80% minimum

**Focus Areas:**
- Domain logic (campaign state transitions, budget calculations, commission formulas)
- Value objects (QRCodePayload signature generation/verification)
- Input validation (Bean Validation constraints)
- Business rules (promotion eligibility, ticket availability)

**Example Unit Tests:**
```java
@Test
void campaignBudgetDeduction_shouldNotExceedTotal() {
    Campaign campaign = new Campaign(budgetTotal: 10000.00);
    campaign.deductBudget(5000.00);
    campaign.deductBudget(5000.00);
    
    assertThat(campaign.getBudgetSpent()).isEqualTo(new BigDecimal("10000.00"));
    assertThat(campaign.isBudgetExhausted()).isTrue();
    
    assertThatThrownBy(() -> campaign.deductBudget(1.00))
        .isInstanceOf(BudgetExhaustedException.class);
}

@Test
void qrCodeSignature_shouldVerifyCorrectly() {
    Ticket ticket = new Ticket(/* ... */);
    QRCodePayload payload = QRCodePayload.generate(ticket, SECRET_KEY);
    
    assertThat(payload.verify(SECRET_KEY)).isTrue();
    assertThat(payload.verify("wrong-key")).isFalse();
}
```

### Property-Based Testing

**Framework:** jqwik (Java Property-Based Testing)  
**Configuration:** Minimum 100 iterations per test

**Each property from the Correctness Properties section will be implemented as a property-based test.**

**Example Property Tests:**

```java
// Property 1: Campaign Budget Integrity
@Property
@Label("Feature: advertising-ticketing-commerce, Property 1: Campaign Budget Integrity")
void campaignBudgetIntegrity(@ForAll("campaigns") Campaign campaign,
                              @ForAll("budgetDeductions") List<BigDecimal> deductions) {
    BigDecimal totalDeducted = BigDecimal.ZERO;
    
    for (BigDecimal amount : deductions) {
        if (campaign.getBudgetSpent().add(amount).compareTo(campaign.getBudgetTotal()) <= 0) {
            campaign.deductBudget(amount);
            totalDeducted = totalDeducted.add(amount);
        }
    }
    
    assertThat(campaign.getBudgetSpent()).isEqualTo(totalDeducted);
    assertThat(campaign.getBudgetSpent()).isLessThanOrEqualTo(campaign.getBudgetTotal());
}

// Property 6: Ticket Scan Idempotency
@Property
@Label("Feature: advertising-ticketing-commerce, Property 6: Ticket Scan Idempotency")
void ticketScanIdempotency(@ForAll("validTickets") Ticket ticket,
                            @ForAll("agents") String agentId) {
    // First scan should succeed
    ScanResult firstScan = ticketService.scanTicket(ticket.getQrCodeData(), agentId);
    assertThat(firstScan.isSuccess()).isTrue();
    assertThat(firstScan.getReason()).isEqualTo(ScanResult.SUCCESS);
    
    // Second scan should be rejected
    ScanResult secondScan = ticketService.scanTicket(ticket.getQrCodeData(), agentId);
    assertThat(secondScan.isSuccess()).isFalse();
    assertThat(secondScan.getReason()).isEqualTo(ScanResult.ALREADY_USED);
    
    // Third scan (and beyond) should also be rejected
    ScanResult thirdScan = ticketService.scanTicket(ticket.getQrCodeData(), agentId);
    assertThat(thirdScan.isSuccess()).isFalse();
    assertThat(thirdScan.getReason()).isEqualTo(ScanResult.ALREADY_USED);
}

// Property 11: Promotion Usage Atomicity
@Property(tries = 1000)
@Label("Feature: advertising-ticketing-commerce, Property 11: Promotion Usage Atomicity")
void promotionUsageAtomicity(@ForAll("promotionsWithLimits") Promotion promotion) {
    int usageLimit = promotion.getUsageLimit();
    int concurrentUsers = usageLimit + 10; // Attempt to exceed limit
    
    ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
    CountDownLatch latch = new CountDownLatch(concurrentUsers);
    AtomicInteger successCount = new AtomicInteger(0);
    
    for (int i = 0; i < concurrentUsers; i++) {
        executor.submit(() -> {
            try {
                promotionService.applyPromotion(promotion.getCode(), createOrder());
                successCount.incrementAndGet();
            } catch (PromotionLimitExceededException e) {
                // Expected for some threads
            } finally {
                latch.countDown();
            }
        });
    }
    
    latch.await();
    executor.shutdown();
    
    assertThat(successCount.get()).isLessThanOrEqualTo(usageLimit);
    assertThat(promotion.getUsageCount()).isEqualTo(successCount.get());
}

// Generators
@Provide
Arbitrary<Campaign> campaigns() {
    return Combinators.combine(
        Arbitraries.strings().ofMinLength(1).ofMaxLength(100),
        Arbitraries.bigDecimals().between(BigDecimal.valueOf(1000), BigDecimal.valueOf(1000000))
    ).as((name, budget) -> new Campaign(name, budget));
}

@Provide
Arbitrary<BigDecimal> budgetDeductions() {
    return Arbitraries.bigDecimals()
        .between(BigDecimal.valueOf(1), BigDecimal.valueOf(1000))
        .list().ofMinSize(1).ofMaxSize(20);
}
```

### Integration Testing

**Framework:** Spring Boot Test + Testcontainers (PostgreSQL, Kafka, Redis)  
**Scope:** Service layer + Repository layer + Kafka producers/consumers

**Focus Areas:**
- Database transactions and rollback
- Outbox pattern (event publication after commit)
- Inbox pattern (event deduplication)
- Kafka event production and consumption
- REST endpoint integration

**Example Integration Test:**
```java
@SpringBootTest
@Testcontainers
class CampaignServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));
    
    @Autowired
    private CampaignService campaignService;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Test
    void activateCampaign_shouldPublishEventAndUpdateStatus() {
        // Given
        Campaign campaign = campaignService.createCampaign(createRequest());
        campaignService.submitCampaign(campaign.getId());
        campaignService.approveCampaign(campaign.getId());
        
        // When
        campaignService.activateCampaign(campaign.getId());
        
        // Then
        Campaign activated = campaignService.getCampaign(campaign.getId());
        assertThat(activated.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
        
        // Verify event published
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, "campaign.events");
        assertThat(record.value()).contains("campaign.activated");
    }
}
```

### End-to-End Testing

**Framework:** REST Assured + WireMock (for external services)  
**Scope:** Full request-response cycle through API Gateway

**Scenarios:**
- User purchases ticket → payment confirmed → ticket issued with QR code
- Partner creates campaign → admin approves → ads delivered in feed
- User applies promotion code → discount calculated → order total updated

### Performance Testing

**Framework:** k6 (load testing)  
**Targets:**
- Ads Delivery: 1000 req/s, P95 < 200ms
- Ticket Scanning: 100 req/s, P95 < 500ms
- Order Creation: 200 req/s, P95 < 1s

**Scenarios:**
```javascript
import http from 'k6/http';
import { check } from 'k6';

export let options = {
  stages: [
    { duration: '2m', target: 100 },  // Ramp up
    { duration: '5m', target: 1000 }, // Sustained load
    { duration: '2m', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<200'], // 95% of requests < 200ms
  },
};

export default function () {
  let res = http.post('https://api.yeyamo.com/api/v1/ads/request', JSON.stringify({
    context: 'FEED',
    userId: 'user123',
    location: { lat: 3.848, lng: 11.502 }
  }), {
    headers: { 'Content-Type': 'application/json' },
  });
  
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 200ms': (r) => r.timings.duration < 200,
  });
}
```

