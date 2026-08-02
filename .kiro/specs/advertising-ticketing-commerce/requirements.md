# Requirements Document: Advertising, Ticketing & Commerce Platform

## Introduction

Ce document définit les exigences pour l'extension de la plateforme YeYamo avec des fonctionnalités de campagnes publicitaires, billetterie événementielle et commerce. Cette extension doit être additive et rétrocompatible, sans modification de la logique métier existante.

## Glossaire

- **System**: L'ensemble de la plateforme YeYamo comprenant les quatre nouveaux bounded contexts
- **Campaign_Service**: Service gérant le cycle de vie des campagnes publicitaires
- **Ads_Delivery_Service**: Service responsable de la diffusion et du tracking des publicités
- **Ticket_Service**: Service gérant la billetterie et le contrôle d'accès
- **Commerce_Service**: Service gérant les transactions, paiements et promotions
- **Advertiser**: Partenaire ou utilisateur créant des campagnes publicitaires
- **Campaign**: Ensemble d'annonces publicitaires avec budget, ciblage et période
- **Ad_Impression**: Affichage d'une publicité à un utilisateur
- **Conversion**: Action mesurable résultant d'une publicité (clic, achat, inscription)
- **Ticket**: Billet électronique donnant accès à un événement
- **QR_Code**: Code graphique unique associé à un ticket
- **Agent**: Personnel partenaire autorisé à scanner les tickets
- **Promotion**: Réduction applicable sur un achat
- **Commission**: Frais prélevés par la plateforme sur une transaction
- **Settlement**: Virement des fonds aux partenaires après déduction de la commission
- **Idempotency_Key**: Clé unique garantissant qu'une opération n'est exécutée qu'une seule fois

## Requirements

### Requirement 1: Campaign Lifecycle Management

**User Story:** En tant qu'annonceur, je veux créer et gérer des campagnes publicitaires, afin de promouvoir mes lieux, événements ou contenus sur la plateforme YeYamo.

#### Acceptance Criteria

1. WHEN an advertiser creates a campaign, THE Campaign_Service SHALL validate all required fields (name, budget, dates, targeting criteria, ad creatives)
2. WHEN a campaign is submitted for approval, THE Campaign_Service SHALL publish a campaign.submitted event and change status to PENDING_REVIEW
3. WHEN an admin approves a campaign, THE Campaign_Service SHALL publish a campaign.approved event and allow activation
4. WHEN an admin rejects a campaign, THE Campaign_Service SHALL publish a campaign.rejected event with rejection reason
5. WHEN an advertiser activates an approved campaign, THE Campaign_Service SHALL publish a campaign.activated event and notify Ads_Delivery_Service
6. WHEN a campaign budget is exhausted, THE Campaign_Service SHALL automatically pause the campaign and publish a campaign.budget.exhausted event
7. WHEN a campaign end date is reached, THE Campaign_Service SHALL automatically complete the campaign and publish a campaign.completed event
8. WHEN an advertiser pauses an active campaign, THE Campaign_Service SHALL publish a campaign.paused event and stop ad delivery immediately
9. WHERE campaign data is modified, THE Campaign_Service SHALL track version history for audit purposes
10. WHEN retrieving campaign performance, THE Campaign_Service SHALL aggregate metrics from Ads_Delivery_Service in real-time

### Requirement 2: Ad Targeting & Delivery

**User Story:** En tant que système, je veux diffuser des publicités pertinentes aux utilisateurs, afin de maximiser l'engagement tout en respectant leur expérience.

#### Acceptance Criteria

1. WHEN a user views feed content, THE Ads_Delivery_Service SHALL inject ad placements based on campaign targeting criteria
2. WHEN selecting ads for delivery, THE Ads_Delivery_Service SHALL filter by geographic targeting (region, city, radius in km)
3. WHEN selecting ads for delivery, THE Ads_Delivery_Service SHALL filter by interest targeting (categories, user preferences)
4. WHEN selecting ads for delivery, THE Ads_Delivery_Service SHALL consider campaign budget availability and daily limits
5. WHEN an ad is displayed, THE Ads_Delivery_Service SHALL record an impression with ad.impression.recorded event containing user_id, ad_id, timestamp, context
6. WHEN a user clicks an ad, THE Ads_Delivery_Service SHALL record a click with ad.click.recorded event and redirect to target URL
7. WHEN a conversion is tracked, THE Ads_Delivery_Service SHALL publish ad.conversion.recorded event with conversion type and value
8. IF an ad violates delivery rules, THEN THE Ads_Delivery_Service SHALL publish ad.delivery.rejected event with reason
9. WHEN ad frequency capping is configured, THE Ads_Delivery_Service SHALL limit the number of times an ad is shown to the same user per day
10. THE Ads_Delivery_Service SHALL respect user opt-out preferences for personalized advertising

### Requirement 3: Ticketing System

**User Story:** En tant qu'organisateur d'événement, je veux vendre des billets avec QR code, afin de contrôler l'accès et suivre la participation.

#### Acceptance Criteria

1. WHEN creating a ticket type, THE Ticket_Service SHALL validate required fields (event_id, name, price, quantity, sale_start, sale_end)
2. WHEN a ticket type is created, THE Ticket_Service SHALL publish a ticket.type.created event
3. WHEN a user purchases tickets, THE Ticket_Service SHALL create an order with ticket.order.created event
4. WHEN payment is confirmed by Commerce_Service, THE Ticket_Service SHALL generate unique tickets with QR codes and publish ticket.issued events
5. WHEN generating a QR code, THE Ticket_Service SHALL use a cryptographically secure format containing ticket_id, event_id, holder_id, signature
6. WHEN an agent scans a QR code, THE Ticket_Service SHALL validate the ticket signature and status
7. WHEN a valid ticket is scanned for the first time, THE Ticket_Service SHALL publish ticket.scanned and ticket.validated events
8. IF a ticket is scanned twice, THEN THE Ticket_Service SHALL publish ticket.scan.rejected event with reason ALREADY_USED
9. IF a ticket is expired or cancelled, THEN THE Ticket_Service SHALL publish ticket.scan.rejected event with appropriate reason
10. WHEN a user requests a refund, THE Ticket_Service SHALL validate refund policy and publish ticket.refunded event if eligible
11. WHEN a ticket is cancelled, THE Ticket_Service SHALL publish ticket.cancelled event and mark the ticket as invalid
12. THE Ticket_Service SHALL generate PDF tickets with embedded QR codes for download

### Requirement 4: Commerce & Payment Processing

**User Story:** En tant qu'utilisateur, je veux effectuer des achats sécurisés sur la plateforme, afin d'acquérir des billets ou autres services.

#### Acceptance Criteria

1. WHEN a user initiates a purchase, THE Commerce_Service SHALL create an order with order.created event containing items, amounts, currency
2. WHEN an order is created with promotion code, THE Commerce_Service SHALL validate and apply the promotion, publishing promotion.applied event
3. WHEN payment is required, THE Commerce_Service SHALL publish payment.requested event with Idempotency-Key to Payment_Service
4. WHEN Payment_Service confirms payment, THE Commerce_Service SHALL consume payment.confirmed event and update order status to PAID
5. IF payment fails, THEN THE Commerce_Service SHALL consume payment.failed event and allow retry with same Idempotency-Key
6. WHEN an order is completed, THE Commerce_Service SHALL calculate platform commission and publish commission.calculated event
7. WHEN commission is calculated, THE Commerce_Service SHALL create a settlement record with settlement.created event
8. WHEN settlement period ends, THE Commerce_Service SHALL aggregate partner earnings and publish settlement.completed event
9. THE Commerce_Service SHALL enforce minimum transaction amounts (e.g., 500 XAF)
10. THE Commerce_Service SHALL support currency XAF by default and additional currencies configured per region
11. THE Commerce_Service SHALL store all amounts in BigDecimal format to prevent rounding errors
12. THE Commerce_Service SHALL use Transactional Outbox Pattern for all event publications
13. THE Commerce_Service SHALL implement Inbox Pattern with Idempotency-Key for webhook and event consumption
14. WHERE financial operations are performed, THE Commerce_Service SHALL log detailed audit trails with timestamp, actor_id, operation, amounts

### Requirement 5: Promotion Management

**User Story:** En tant que partenaire, je veux créer des codes promotionnels, afin d'attirer des clients et augmenter les ventes.

#### Acceptance Criteria

1. WHEN creating a promotion, THE Commerce_Service SHALL validate required fields (code, discount_type, discount_value, start_date, end_date, max_uses)
2. WHEN a promotion code is applied, THE Commerce_Service SHALL verify the code is active, not expired, and within usage limits
3. WHEN a promotion code is valid, THE Commerce_Service SHALL apply the discount (percentage or fixed amount) to the order
4. IF a promotion code is invalid, THEN THE Commerce_Service SHALL reject with clear error message (expired, max uses reached, invalid code)
5. WHEN a promotion is used, THE Commerce_Service SHALL increment usage count atomically to prevent race conditions
6. WHERE promotion targeting is configured, THE Commerce_Service SHALL restrict application to specific events, categories, or user segments
7. THE Commerce_Service SHALL prevent stacking of multiple promotion codes on the same order
8. WHEN a promotion expires, THE Commerce_Service SHALL automatically deactivate it

### Requirement 6: Agent Access Control

**User Story:** En tant que partenaire, je veux autoriser mes agents à scanner les billets, afin de contrôler l'accès aux événements.

#### Acceptance Criteria

1. WHEN a partner creates an agent account, THE Ticket_Service SHALL validate agent credentials (name, email, phone, partner_id)
2. WHEN an agent is granted access to an event, THE Ticket_Service SHALL create an access permission with expiration date
3. WHEN an agent scans a ticket, THE Ticket_Service SHALL verify the agent has active permission for the associated event
4. IF an agent lacks permission, THEN THE Ticket_Service SHALL reject the scan with UNAUTHORIZED error
5. WHEN an agent permission expires, THE Ticket_Service SHALL automatically revoke scanning access
6. WHEN an agent scans a ticket, THE Ticket_Service SHALL log the scan with agent_id, ticket_id, timestamp, location for audit
7. THE Ticket_Service SHALL rate-limit scanning operations to prevent abuse (e.g., 60 scans per minute per agent)

### Requirement 7: Analytics & Reporting

**User Story:** En tant qu'annonceur ou partenaire, je veux consulter des rapports détaillés, afin de mesurer les performances et le ROI.

#### Acceptance Criteria

1. WHEN retrieving campaign analytics, THE Analytics_Service SHALL provide metrics including impressions, clicks, CTR, conversions, cost
2. WHEN retrieving ticket sales analytics, THE Analytics_Service SHALL provide metrics including tickets sold, revenue, attendance rate
3. WHEN retrieving commerce analytics, THE Analytics_Service SHALL provide metrics including total sales, commission earned, settlement status
4. THE Analytics_Service SHALL consume all tracking events (impressions, clicks, conversions, ticket scans) for aggregation
5. THE Analytics_Service SHALL provide time-series data with daily, weekly, and monthly granularity
6. THE Analytics_Service SHALL allow filtering analytics by date range, campaign, event, region, category
7. THE Analytics_Service SHALL calculate derived metrics (CTR = clicks/impressions, conversion rate = conversions/clicks)
8. WHEN exporting analytics data, THE Analytics_Service SHALL support CSV and JSON formats

### Requirement 8: Security & Compliance

**User Story:** En tant que système, je veux garantir la sécurité des transactions financières, afin de protéger les utilisateurs et partenaires.

#### Acceptance Criteria

1. THE System SHALL enforce HTTPS for all API communications
2. THE System SHALL require JWT authentication for all protected endpoints
3. WHEN processing financial operations, THE System SHALL require Idempotency-Key header to prevent duplicate charges
4. WHEN receiving webhooks, THE System SHALL verify HMAC signatures before processing
5. THE System SHALL encrypt sensitive data at rest (payment tokens, personal information)
6. THE System SHALL mask sensitive data in logs (credit card numbers, tokens, passwords)
7. THE System SHALL implement rate limiting on all public endpoints (e.g., 120 req/min for authenticated users, 20 req/min for auth endpoints)
8. THE System SHALL audit all financial transactions with immutable logs stored for 7 years minimum
9. WHEN detecting suspicious patterns, THE System SHALL trigger alerts to admin dashboard
10. THE Commerce_Service SHALL comply with PCI DSS requirements by never storing raw payment card data

### Requirement 9: Integration with Existing Services

**User Story:** En tant que système, je veux m'intégrer harmonieusement avec les services existants, afin de maintenir la cohérence de l'architecture.

#### Acceptance Criteria

1. WHEN a campaign targets a place, THE Campaign_Service SHALL validate place_id against Catalog_Service
2. WHEN a campaign targets an event, THE Campaign_Service SHALL validate event_id against Event_Service
3. WHEN tickets are sold for an event, THE Ticket_Service SHALL validate event details against Event_Service
4. WHEN displaying ads in feed, THE Ads_Delivery_Service SHALL coordinate with Feed_Service for placement
5. WHEN a ticket purchase occurs, THE Commerce_Service SHALL notify Booking_Service to reserve capacity
6. WHEN payment is processed, THE Commerce_Service SHALL integrate with existing Payment_Service using established patterns
7. WHEN sending notifications, THE System SHALL use Notification_Service for order confirmations, ticket delivery, campaign status
8. WHEN tracking analytics, THE System SHALL publish metrics to Analytics_Service via Kafka events
9. WHEN admin reviews campaigns, THE Admin_Service SHALL expose campaign moderation endpoints
10. WHEN partners manage campaigns, THE Partner_Service SHALL provide access control and dashboards
11. THE System SHALL respect existing event schema for domain events (eventId, eventType, eventVersion, occurredAt, producer, aggregateId, correlationId, causationId, payload)
12. THE System SHALL use existing Kafka topics where appropriate or create new topics following naming conventions

### Requirement 10: Data Consistency & Reliability

**User Story:** En tant que système, je veux garantir la cohérence des données, afin d'éviter les incohérences et pertes de données.

#### Acceptance Criteria

1. THE System SHALL use Transactional Outbox Pattern for all database writes followed by event publication
2. THE System SHALL use Inbox Pattern with processed_events table for idempotent event consumption
3. WHEN consuming Kafka events, THE System SHALL check eventId in processed_events table before processing
4. WHEN an event is processed successfully, THE System SHALL store eventId in processed_events with processing timestamp
5. IF event processing fails, THEN THE System SHALL retry with exponential backoff up to 5 attempts
6. IF event processing fails after max retries, THEN THE System SHALL publish to Dead Letter Topic (.DLT suffix)
7. THE System SHALL use UUID for all entity identifiers to ensure global uniqueness
8. THE System SHALL store all timestamps in UTC timezone
9. WHEN handling concurrent updates, THE System SHALL use optimistic locking with version columns
10. THE System SHALL implement database connection pooling with appropriate limits (max 10 connections per service)

### Requirement 11: API Design & Versioning

**User Story:** En tant que développeur d'API, je veux des endpoints REST cohérents, afin de faciliter l'intégration et la maintenance.

#### Acceptance Criteria

1. THE System SHALL expose all REST endpoints under /api/v1 prefix
2. THE System SHALL use RESTful conventions (GET for read, POST for create, PUT for update, DELETE for remove)
3. THE System SHALL return appropriate HTTP status codes (200 OK, 201 Created, 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 409 Conflict, 500 Internal Server Error)
4. THE System SHALL include pagination support for list endpoints using page, size, sort parameters
5. THE System SHALL include HATEOAS links in responses where appropriate (self, next, prev)
6. THE System SHALL validate all input using Bean Validation annotations (@Valid, @NotNull, @Size, @Min, @Max)
7. THE System SHALL return consistent error response format {timestamp, status, error, message, path}
8. THE System SHALL document all endpoints using OpenAPI 3.0 specification
9. THE System SHALL version breaking changes by incrementing API version (/api/v2)
10. THE System SHALL support JSON as primary content type (application/json)

### Requirement 12: Performance & Scalability

**User Story:** En tant que système, je veux gérer des charges élevées, afin de maintenir une expérience utilisateur fluide lors des pics de trafic.

#### Acceptance Criteria

1. THE Ads_Delivery_Service SHALL respond to ad requests within 200ms at P95
2. THE Ticket_Service SHALL generate QR codes and issue tickets within 5 seconds of payment confirmation
3. THE System SHALL support at least 1000 concurrent users per service instance
4. THE System SHALL horizontally scale by deploying multiple service replicas behind a load balancer
5. THE System SHALL use caching (Redis) for frequently accessed data (campaigns, ticket types, promotions)
6. THE System SHALL set appropriate cache TTL (Time To Live) values to balance freshness and performance
7. THE System SHALL use database indexes on frequently queried columns (user_id, event_id, campaign_id, created_at)
8. THE System SHALL implement query optimization to avoid N+1 problems and full table scans
9. THE System SHALL use asynchronous processing for non-critical operations (email notifications, analytics aggregation)
10. THE System SHALL monitor and alert on high latency (P95 > 1s), high error rates (>1%), and resource exhaustion

