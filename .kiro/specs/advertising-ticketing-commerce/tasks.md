# Implementation Plan: Advertising, Ticketing & Commerce Platform

## Overview

This implementation plan breaks down the design into discrete coding tasks for creating four new microservices: campaign-service, ads-delivery-service, ticket-service, and commerce-service. The implementation follows an incremental approach, building core functionality first, then adding integrations, and finally implementing advanced features.

**Technology Stack:**
- Java 21
- Spring Boot 4.1.0
- Spring Cloud 2025.1.2
- PostgreSQL 16 with PostGIS
- Redis 7
- Apache Kafka 3.6
- Maven for build
- Docker for containerization

**Architecture Principles:**
- Event-Driven Architecture (Kafka)
- Transactional Outbox Pattern
- Inbox Pattern for idempotency
- CQRS where appropriate
- Domain-Driven Design

## Tasks

### Phase 1: Infrastructure & Foundation

- [ ] 1. Set up Campaign Service project structure
- [ ] 1.1 Create Maven multi-module structure for campaign-service
  - Initialize Spring Boot 4.1.0 application
  - Configure Spring Cloud dependencies (Eureka, Config Server)
  - Add PostgreSQL, Flyway, Spring Data JPA dependencies
  - Add Kafka dependencies (spring-kafka)
  - Set up application.properties with externalized config
  - _Requirements: 1.1, 9.1, 11.1_

- [ ] 1.2 Create database schema and Flyway migrations for campaign-service
  - V1__create_campaigns_table.sql (campaigns table with constraints)
  - V2__create_campaign_creatives_table.sql
  - V3__create_campaign_budget_logs_table.sql
  - V4__create_campaign_outbox_events_table.sql
  - Add indexes for performance (advertiser_id, status, dates)
  - _Requirements: 1.1, 10.7_

- [ ] 1.3 Write property tests for campaign budget constraints
  - **Property 1: Campaign Budget Integrity**
  - **Validates: Requirements 1.6, 1.7**
  - Use jqwik to generate random campaigns and budget deductions
  - Verify budgetSpent never exceeds budgetTotal
  - _Requirements: 1.6, 1.7_


- [ ] 2. Set up Ads Delivery Service project structure
- [ ] 2.1 Create Maven multi-module structure for ads-delivery-service
  - Initialize Spring Boot application with Config Server client
  - Add PostgreSQL, Redis, Kafka dependencies
  - Configure connection pools (HikariCP max 10)
  - Set up application.properties
  - _Requirements: 2.1, 11.1_

- [ ] 2.2 Create database schema and Flyway migrations for ads-delivery-service
  - V1__create_ad_impressions_table.sql (partitioned by month)
  - V2__create_ad_clicks_table.sql (partitioned by month)
  - V3__create_ad_conversions_table.sql (partitioned by month)
  - V4__create_ads_processed_events_table.sql (idempotency)
  - Create first partition tables for current month
  - Add indexes (campaign_id, user_id, created_at)
  - _Requirements: 2.5, 2.6, 2.7, 10.3_

- [ ] 2.3 Write property tests for ad frequency capping
  - **Property 4: Frequency Capping Enforcement**
  - **Validates: Requirements 2.9**
  - Generate random user/campaign pairs
  - Simulate multiple impression requests within 24h
  - Verify impressions never exceed frequency cap
  - _Requirements: 2.9_

- [ ] 3. Set up Ticket Service project structure
- [ ] 3.1 Create Maven multi-module structure for ticket-service
  - Initialize Spring Boot application
  - Add PostgreSQL, Flyway, Spring Data JPA dependencies
  - Add ZXing library for QR code generation
  - Add Apache PDFBox for PDF ticket generation
  - Configure Spring Security for agent authentication
  - _Requirements: 3.1, 3.5, 3.12_


- [ ] 3.2 Create database schema and Flyway migrations for ticket-service
  - V1__create_ticket_types_table.sql
  - V2__create_ticket_orders_table.sql
  - V3__create_ticket_order_items_table.sql
  - V4__create_tickets_table.sql (with unique QR code constraint)
  - V5__create_agents_table.sql
  - V6__create_agent_event_access_table.sql
  - V7__create_ticket_scan_logs_table.sql (audit trail)
  - V8__create_ticket_outbox_events_table.sql
  - _Requirements: 3.1, 3.2, 6.1, 6.2_

- [ ] 3.3 Write property tests for ticket uniqueness
  - **Property 5: Ticket Uniqueness**
  - **Validates: Requirements 3.5**
  - Generate multiple tickets for same event
  - Verify all QR codes are unique
  - _Requirements: 3.5_

- [ ] 3.4 Write property tests for QR code signature verification
  - **Property 7: QR Code Signature Verification**
  - **Validates: Requirements 3.6**
  - Generate random QR payloads
  - Verify signatures with correct key succeed
  - Verify tampered payloads fail verification
  - _Requirements: 3.6_

- [ ] 4. Set up Commerce Service project structure
- [ ] 4.1 Create Maven multi-module structure for commerce-service
  - Initialize Spring Boot application
  - Add PostgreSQL, Kafka, Spring Data JPA dependencies
  - Configure Idempotency-Key filter
  - Set up application.properties
  - _Requirements: 4.1, 4.3, 4.13_

- [ ] 4.2 Create database schema and Flyway migrations for commerce-service
  - V1__create_orders_table.sql (with idempotency_key unique constraint)
  - V2__create_promotions_table.sql
  - V3__create_promotion_usages_table.sql
  - V4__create_transactions_table.sql (audit log)
  - V5__create_commissions_table.sql
  - V6__create_settlements_table.sql
  - V7__create_settlement_items_table.sql
  - V8__create_commerce_outbox_events_table.sql
  - V9__create_commerce_processed_events_table.sql (inbox)
  - _Requirements: 4.1, 4.2, 5.1, 4.6, 4.8_


- [ ] 4.3 Write property tests for order amount calculation
  - **Property 9: Order Amount Calculation**
  - **Validates: Requirements 4.2**
  - Generate random orders with promotions
  - Verify final_amount = total_amount - discount_amount
  - Verify all amounts non-negative
  - _Requirements: 4.2_

- [ ] 4.4 Write property tests for commission calculation
  - **Property 12: Commission Calculation**
  - **Validates: Requirements 4.6**
  - Generate random orders with different commission rates
  - Verify commission = gross * rate
  - Verify net = gross - commission
  - _Requirements: 4.6_

- [ ] 5. Checkpoint - Ensure all infrastructure is ready
  - Verify all four services start successfully
  - Verify database migrations execute without errors
  - Verify services register with Eureka
  - Verify Config Server serves configuration
  - Ask the user if questions arise

### Phase 2: Campaign Service Core Features

- [ ] 6. Implement Campaign domain model
- [ ] 6.1 Create Campaign aggregate root and value objects
  - Campaign entity with status enum (DRAFT, PENDING_REVIEW, APPROVED, etc.)
  - CampaignTargeting value object (geographic, interests, demographics)
  - CampaignCreative entity
  - Business methods: submit(), approve(), reject(), activate(), pause(), complete()
  - Budget tracking: deductBudget(), isBudgetExhausted()
  - _Requirements: 1.1, 1.2, 1.6_

- [ ] 6.2 Create Campaign repository (Spring Data JPA)
  - CampaignRepository interface
  - Query methods: findByAdvertiserId, findByStatus, findActiveWithBudget
  - Custom query for campaigns expiring soon
  - _Requirements: 1.1, 1.10_


- [ ] 6.3 Write unit tests for Campaign business logic
  - Test state transitions (DRAFT → PENDING_REVIEW → APPROVED → ACTIVE)
  - Test budget deduction and exhaustion
  - Test date validation (end_date > start_date)
  - Test rejection with reason
  - _Requirements: 1.2, 1.3, 1.4, 1.6, 1.7_

- [ ] 6.4 Write property tests for campaign status transitions
  - **Property 2: Campaign Status Transitions**
  - **Validates: Requirements 1.2, 1.3, 1.4, 1.5, 1.8**
  - Generate random status transition sequences
  - Verify only valid transitions are allowed
  - _Requirements: 1.2-1.8_

- [ ] 7. Implement Campaign service layer
- [ ] 7.1 Create CampaignService with business operations
  - createCampaign() - validate and persist
  - updateCampaign() - DRAFT only, optimistic locking
  - submitCampaign() - transition to PENDING_REVIEW, publish event
  - approveCampaign() - ADMIN only, transition to APPROVED
  - rejectCampaign() - ADMIN only, with reason
  - activateCampaign() - transition to ACTIVE, publish event
  - pauseCampaign() - transition to PAUSED
  - _Requirements: 1.1-1.8_

- [ ] 7.2 Implement Transactional Outbox for campaign events
  - OutboxEvent entity
  - OutboxEventRepository
  - OutboxEventPublisher (scheduled job every 5s)
  - Publish to Kafka topic "campaign.events"
  - Mark events as published after successful send
  - _Requirements: 10.1, 4.12_


- [ ] 7.3 Write integration tests for CampaignService with Outbox
  - Test campaign creation publishes campaign.created event
  - Test activation publishes campaign.activated event
  - Test budget exhaustion publishes campaign.budget.exhausted event
  - Verify events are in outbox before Kafka publish
  - _Requirements: 1.1, 1.5, 1.6, 10.1_

- [ ] 8. Implement Campaign REST controllers
- [ ] 8.1 Create CampaignController with CRUD endpoints
  - POST /api/v1/campaigns (create) - PARTNER, ADMIN roles
  - GET /api/v1/campaigns (list with pagination) - filter by status, advertiser
  - GET /api/v1/campaigns/{id} (get details)
  - PUT /api/v1/campaigns/{id} (update) - DRAFT only
  - DELETE /api/v1/campaigns/{id} (delete) - DRAFT only
  - Input validation with @Valid
  - IDOR protection (verify advertiser ownership)
  - _Requirements: 1.1, 9.1, 11.2, 11.6_

- [ ] 8.2 Create CampaignWorkflowController
  - POST /api/v1/campaigns/{id}/submit
  - POST /api/v1/campaigns/{id}/approve (ADMIN only)
  - POST /api/v1/campaigns/{id}/reject (ADMIN only)
  - POST /api/v1/campaigns/{id}/activate
  - POST /api/v1/campaigns/{id}/pause
  - POST /api/v1/campaigns/{id}/resume
  - _Requirements: 1.2-1.8_

- [ ] 8.3 Create CampaignAnalyticsController
  - GET /api/v1/campaigns/{id}/performance (aggregated metrics)
  - GET /api/v1/campaigns/{id}/budget-status
  - Integrate with Analytics Service for real-time data
  - _Requirements: 1.10, 7.1_

- [ ] 8.4 Write integration tests for Campaign REST endpoints
  - Test full workflow: create → submit → approve → activate
  - Test IDOR protection (user cannot access other's campaigns)
  - Test RBAC (only ADMIN can approve)
  - _Requirements: 1.1-1.8, 8.2_


- [ ] 9. Implement Campaign Creative management
- [ ] 9.1 Create CampaignCreativeService
  - uploadCreative() - validate media type (IMAGE, VIDEO, CAROUSEL)
  - Integration with Media Service for file upload
  - Validate creative dimensions and file size
  - Associate creative with campaign
  - _Requirements: 1.1_

- [ ] 9.2 Create CampaignCreativeController
  - POST /api/v1/campaigns/{id}/creatives (multipart/form-data)
  - GET /api/v1/campaigns/{id}/creatives (list)
  - DELETE /api/v1/campaigns/{id}/creatives/{creativeId}
  - _Requirements: 1.1_

- [ ] 10. Checkpoint - Campaign Service complete
  - Run all unit and integration tests
  - Verify campaign lifecycle works end-to-end
  - Verify events are published to Kafka
  - Ask the user if questions arise

### Phase 3: Ads Delivery Service Core Features

- [ ] 11. Implement Ad Delivery domain logic
- [ ] 11.1 Create AdTargetingService
  - matchesGeographicTargeting() - check user location vs campaign targeting
  - matchesInterestTargeting() - check user interests vs campaign interests
  - calculateRelevanceScore() - rank ads by relevance
  - _Requirements: 2.2, 2.3_

- [ ] 11.2 Create FrequencyCappingService
  - checkFrequencyCap() - query Redis for user/campaign impression count
  - incrementImpressionCount() - atomic Redis INCR with 24h TTL
  - Use Redis key pattern: "ad:frequency:{userId}:{campaignId}"
  - _Requirements: 2.9_


- [ ] 11.3 Write property tests for ad targeting consistency
  - **Property 3: Ad Targeting Consistency**
  - **Validates: Requirements 2.2, 2.3**
  - Generate random user locations and interests
  - Generate random campaign targeting configs
  - Verify only matching ads are selected
  - _Requirements: 2.2, 2.3_

- [ ] 12. Implement Ad Delivery service layer
- [ ] 12.1 Create AdDeliveryService
  - requestAds() - select ads for placement based on targeting
  - recordImpression() - save to DB, update Redis frequency cap
  - recordClick() - save to DB, return redirect URL
  - recordConversion() - save to DB with conversion value
  - Budget deduction via Kafka event to Campaign Service
  - _Requirements: 2.1, 2.5, 2.6, 2.7, 2.4_

- [ ] 12.2 Implement Kafka consumer for campaign events
  - Listen to campaign.activated - cache active campaigns in Redis
  - Listen to campaign.paused - remove from Redis cache
  - Listen to campaign.completed - remove from Redis cache
  - Listen to campaign.budget.exhausted - stop delivery immediately
  - Use Inbox Pattern for idempotency (check eventId in processed_events)
  - _Requirements: 10.2, 10.3_

- [ ] 12.3 Write integration tests for Ad Delivery with Kafka
  - Test ad selection filters by targeting
  - Test impression recording updates frequency cap
  - Test campaign.activated event updates active campaigns cache
  - Verify idempotency (duplicate events ignored)
  - _Requirements: 2.1-2.4, 10.3_

- [ ] 13. Implement Ad Delivery REST controllers
- [ ] 13.1 Create AdDeliveryController (Internal APIs)
  - POST /api/v1/ads/request - request ads for feed/discovery placement
  - POST /api/v1/ads/impression - record impression
  - POST /api/v1/ads/click - record click and redirect
  - POST /api/v1/ads/conversion - record conversion
  - Rate limiting: 1000 req/s
  - _Requirements: 2.1, 2.5, 2.6, 2.7, 8.7_


- [ ] 13.2 Write integration tests for Ad Delivery endpoints
  - Test ad request returns relevant ads
  - Test impression increments frequency cap
  - Test click records and redirects
  - Test rate limiting (1001st request rejected)
  - _Requirements: 2.1-2.7, 12.7_

- [ ] 14. Implement Analytics aggregation
- [ ] 14.1 Create scheduled job for analytics aggregation
  - Aggregate impressions, clicks, conversions daily
  - Calculate CTR (clicks/impressions)
  - Calculate conversion rate (conversions/clicks)
  - Publish aggregated metrics to Analytics Service via Kafka
  - Schedule: Daily at 02:00 UTC
  - _Requirements: 7.1, 7.5, 7.7_

- [ ] 15. Checkpoint - Ads Delivery Service complete
  - Run all tests
  - Verify ad targeting works correctly
  - Verify frequency capping prevents over-exposure
  - Ask the user if questions arise

### Phase 4: Ticket Service Core Features

- [ ] 16. Implement Ticket domain model
- [ ] 16.1 Create TicketType aggregate root
  - TicketType entity with price, quantity constraints
  - Business methods: isAvailable(), reserveQuantity(), releaseQuantity()
  - RefundPolicy enum (NO_REFUND, BEFORE_7_DAYS, BEFORE_24_HOURS)
  - _Requirements: 3.1, 3.10_

- [ ] 16.2 Create Ticket aggregate root
  - Ticket entity with status enum (VALID, USED, CANCELLED, REFUNDED)
  - QRCodePayload value object with HMAC-SHA256 signature
  - Business methods: scan(), canBeScanned(), cancel(), refund()
  - _Requirements: 3.4, 3.5, 3.7_

- [ ] 16.3 Create repositories
  - TicketTypeRepository
  - TicketOrderRepository
  - TicketRepository
  - AgentRepository
  - Query methods with pagination
  - _Requirements: 3.1, 3.3_


- [ ] 16.4 Write unit tests for Ticket domain logic
  - Test ticket scanning (first scan succeeds, second fails)
  - Test QR code signature generation and verification
  - Test ticket status transitions
  - Test quantity reservation (atomic update)
  - _Requirements: 3.5, 3.6, 3.7, 3.8_

- [ ] 16.5 Write property tests for ticket scan idempotency
  - **Property 6: Ticket Scan Idempotency**
  - **Validates: Requirements 3.7, 3.8**
  - Generate random tickets
  - Scan multiple times
  - Verify only first scan marks as USED
  - _Requirements: 3.7, 3.8_

- [ ] 17. Implement Ticket service layer
- [ ] 17.1 Create TicketTypeService
  - createTicketType() - validate event exists via Event Service
  - updateTicketType() - only if no tickets sold
  - listTicketTypes() - filter by event
  - _Requirements: 3.1, 3.2, 9.3_

- [ ] 17.2 Create TicketOrderService
  - createOrder() - reserve ticket quantities atomically
  - Publish ticket.order.created event
  - Listen to payment.confirmed - issue tickets with QR codes
  - Listen to payment.failed - release reserved quantities
  - Use Transactional Outbox
  - _Requirements: 3.3, 3.4, 4.4_

- [ ] 17.3 Create TicketScanService
  - scanTicket() - verify QR signature, check status, verify agent permission
  - Log scan attempt to ticket_scan_logs (audit trail)
  - Publish ticket.scanned and ticket.validated events on success
  - Publish ticket.scan.rejected event on failure (ALREADY_USED, INVALID, EXPIRED)
  - Rate limiting: 60 scans/minute per agent
  - _Requirements: 3.6, 3.7, 3.8, 3.9, 6.3, 6.7_


- [ ] 17.4 Write property tests for agent authorization
  - **Property 8: Agent Authorization**
  - **Validates: Requirements 6.3, 6.4**
  - Generate random agents and events
  - Grant/revoke permissions randomly
  - Attempt scans and verify only authorized succeed
  - _Requirements: 6.3, 6.4_

- [ ] 18. Implement QR Code and PDF generation
- [ ] 18.1 Create QRCodeService
  - generateQRCode() - use ZXing library
  - Create QRCodePayload with signature
  - Return Base64-encoded QR image
  - _Requirements: 3.5, 3.6_

- [ ] 18.2 Create PDFTicketService
  - generatePDFTicket() - use Apache PDFBox
  - Embed QR code image
  - Include ticket details (event, holder, date)
  - Add disclaimer and terms
  - _Requirements: 3.12_

- [ ] 19. Implement Ticket REST controllers
- [ ] 19.1 Create TicketTypeController
  - POST /api/v1/ticket-types (PARTNER only)
  - GET /api/v1/ticket-types (filter by event)
  - PUT /api/v1/ticket-types/{id}
  - DELETE /api/v1/ticket-types/{id}
  - _Requirements: 3.1, 3.2_

- [ ] 19.2 Create TicketOrderController
  - POST /api/v1/tickets/orders (create order)
  - GET /api/v1/tickets/orders/{id}
  - GET /api/v1/tickets/my-orders (user's orders)
  - GET /api/v1/tickets/my-tickets (issued tickets)
  - GET /api/v1/tickets/{id}/download (PDF download)
  - _Requirements: 3.3, 3.4, 3.12_


- [ ] 19.3 Create TicketScanController
  - POST /api/v1/tickets/scan (agent scans QR)
  - GET /api/v1/tickets/scan-history (agent's history)
  - Rate limiting: 60 req/min per agent
  - _Requirements: 3.6, 3.7, 6.7_

- [ ] 19.4 Create AgentController
  - POST /api/v1/agents (PARTNER creates agent)
  - GET /api/v1/agents (partner's agents)
  - PUT /api/v1/agents/{id}
  - POST /api/v1/agents/{id}/grant-access (grant event access)
  - DELETE /api/v1/agents/{id}/revoke-access
  - _Requirements: 6.1, 6.2, 6.5_

- [ ] 19.5 Write integration tests for Ticket endpoints
  - Test order creation reserves quantities
  - Test payment confirmation issues tickets
  - Test QR scan with valid/invalid tickets
  - Test agent authorization checks
  - Test PDF download
  - _Requirements: 3.3-3.9, 6.3_

- [ ] 20. Checkpoint - Ticket Service complete
  - Run all tests
  - Verify ticket purchase flow works end-to-end
  - Verify QR scanning with agent authorization
  - Ask the user if questions arise

### Phase 5: Commerce Service Core Features

- [ ] 21. Implement Commerce domain model
- [ ] 21.1 Create Order aggregate root
  - Order entity with status enum (PENDING, PAID, FAILED, CANCELLED, REFUNDED)
  - Business methods: applyPromotion(), confirmPayment(), failPayment(), cancel()
  - Idempotency key support
  - _Requirements: 4.1, 4.2, 4.13_

- [ ] 21.2 Create Promotion aggregate root
  - Promotion entity with discount_type (PERCENTAGE, FIXED_AMOUNT)
  - Business methods: isValid(), calculateDiscount(), incrementUsage()
  - Atomic usage count update with optimistic locking
  - _Requirements: 5.1, 5.2, 5.5_


- [ ] 21.3 Create Commission and Settlement entities
  - Commission calculation logic (commission = gross * rate, net = gross - commission)
  - Settlement aggregation by partner and period
  - _Requirements: 4.6, 4.8_

- [ ] 21.4 Create repositories
  - OrderRepository (with idempotency key query)
  - PromotionRepository
  - CommissionRepository
  - SettlementRepository
  - _Requirements: 4.1, 5.1, 4.6, 4.8_

- [ ] 21.5 Write unit tests for Commerce domain logic
  - Test order amount calculation with promotions
  - Test promotion validation (active, not expired, within limits)
  - Test commission calculation
  - Test settlement aggregation
  - _Requirements: 4.2, 5.2, 4.6, 4.8_

- [ ] 21.6 Write property tests for promotion usage atomicity
  - **Property 11: Promotion Usage Atomicity**
  - **Validates: Requirements 5.5**
  - Simulate concurrent promotion applications
  - Verify usage count never exceeds limit
  - _Requirements: 5.5_

- [ ] 22. Implement Commerce service layer
- [ ] 22.1 Create OrderService
  - createOrder() - validate, apply promotion if provided, save with idempotency key
  - Publish order.created event
  - Request payment via payment.requested event to Payment Service
  - Handle payment.confirmed - update order status, calculate commission
  - Handle payment.failed - update order status, allow retry
  - Use Transactional Outbox and Inbox patterns
  - _Requirements: 4.1-4.5, 4.13, 10.1, 10.2_


- [ ] 22.2 Create PromotionService
  - createPromotion() - validate and save
  - validatePromotion() - check active, dates, usage limits
  - applyPromotion() - calculate discount, increment usage atomically
  - Publish promotion.applied event
  - _Requirements: 5.1-5.5, 5.8_

- [ ] 22.3 Create CommissionService
  - calculateCommission() - apply partner commission rate
  - Fetch commission rate from Partner Service
  - Publish commission.calculated event
  - _Requirements: 4.6_

- [ ] 22.4 Create SettlementService
  - createSettlement() - aggregate commissions by partner and period
  - processSettlement() - initiate payout to partner
  - Publish settlement.created and settlement.completed events
  - Scheduled job: Monthly on 1st day at 00:00 UTC
  - _Requirements: 4.7, 4.8_

- [ ] 22.5 Write integration tests for Commerce Service with Kafka
  - Test order creation with idempotency key
  - Test duplicate order with same key returns same order
  - Test promotion application publishes event
  - Test payment confirmation triggers commission calculation
  - Verify Inbox pattern prevents duplicate event processing
  - _Requirements: 4.1-4.8, 10.2, 10.3_

- [ ] 23. Implement Commerce REST controllers
- [ ] 23.1 Create OrderController
  - POST /api/v1/orders (with Idempotency-Key header)
  - GET /api/v1/orders/{id}
  - GET /api/v1/orders/my-orders
  - POST /api/v1/orders/{id}/cancel
  - _Requirements: 4.1, 4.3, 11.2_


- [ ] 23.2 Create PromotionController
  - POST /api/v1/promotions (PARTNER, ADMIN)
  - GET /api/v1/promotions
  - PUT /api/v1/promotions/{id}
  - DELETE /api/v1/promotions/{id} (deactivate)
  - POST /api/v1/promotions/validate (validate code)
  - _Requirements: 5.1, 5.2_

- [ ] 23.3 Create SettlementController
  - GET /api/v1/settlements (partner's settlements)
  - GET /api/v1/settlements/{id}
  - GET /api/v1/settlements/balance (current balance)
  - _Requirements: 4.8_

- [ ] 23.4 Create AdminCommerceController
  - GET /api/v1/admin/transactions (ADMIN only)
  - GET /api/v1/admin/commissions (commission reports)
  - POST /api/v1/admin/settlements/process (trigger settlement)
  - _Requirements: 4.14_

- [ ] 23.5 Write integration tests for Commerce endpoints
  - Test order creation with Idempotency-Key
  - Test promotion validation and application
  - Test settlement listing for partners
  - Test admin endpoints require ADMIN role
  - _Requirements: 4.1-4.3, 5.1-5.2, 4.8, 8.2_

- [ ] 24. Checkpoint - Commerce Service complete
  - Run all tests
  - Verify order flow with payment integration
  - Verify promotion application works correctly
  - Verify commission calculation is accurate
  - Ask the user if questions arise

### Phase 6: Service Integrations

- [ ] 25. Integrate Campaign Service with existing services
- [ ] 25.1 Add RestTemplate clients for external services
  - CatalogServiceClient - validate place_id
  - EventServiceClient - validate event_id
  - PartnerServiceClient - validate advertiser_id
  - Circuit breaker configuration (Resilience4j)
  - _Requirements: 9.1, 9.2_


- [ ] 25.2 Write integration tests with WireMock
  - Mock Catalog Service responses
  - Mock Event Service responses
  - Test campaign creation with valid/invalid place_id
  - Test circuit breaker opens after 5 failures
  - _Requirements: 9.1, 9.2_

- [ ] 26. Integrate Ads Delivery with Feed Service
- [ ] 26.1 Create Feed Service integration for ad injection
  - FeedServiceClient to request ad placements
  - Ad placement logic (inject 1 ad every 5 posts)
  - Respect user opt-out preferences
  - _Requirements: 2.1, 2.10, 9.4_

- [ ] 26.2 Write integration tests for Feed integration
  - Test ad injection in feed response
  - Test user opt-out respected
  - _Requirements: 2.1, 2.10_

- [ ] 27. Integrate Ticket Service with existing services
- [ ] 27.1 Add RestTemplate clients
  - EventServiceClient - validate event details
  - NotificationServiceClient - send ticket delivery emails
  - _Requirements: 9.3, 9.7_

- [ ] 27.2 Send ticket delivery notification on issuance
  - Listen to ticket.issued event
  - Call Notification Service to send email with PDF attachment
  - _Requirements: 9.7_

- [ ] 28. Integrate Commerce Service with Payment Service
- [ ] 28.1 Implement payment orchestration
  - Publish payment.requested event with Idempotency-Key
  - Consume payment.confirmed event - update order, calculate commission
  - Consume payment.failed event - update order, allow retry
  - _Requirements: 4.3, 4.4, 4.5, 9.6_


- [ ] 28.2 Integrate with Booking Service
  - Publish ticket.order.created event
  - Booking Service reserves capacity
  - _Requirements: 9.5_

- [ ] 28.3 Write integration tests for payment flow
  - Test order → payment request → confirmation → commission
  - Test idempotency on payment retry
  - _Requirements: 4.3-4.6_

- [ ] 29. Checkpoint - All integrations complete
  - Verify all services communicate correctly
  - Verify circuit breakers work
  - Verify Kafka events flow between services
  - Ask the user if questions arise

### Phase 7: Security & RBAC

- [ ] 30. Implement RBAC permissions
- [ ] 30.1 Add permission checks to Campaign Service
  - campaign:create - PARTNER, ADMIN
  - campaign:read - Owner, ADMIN
  - campaign:update - Owner (DRAFT only)
  - campaign:approve - ADMIN only
  - campaign:reject - ADMIN only
  - Use @PreAuthorize annotations
  - _Requirements: 8.2, 11.3_

- [ ] 30.2 Add permission checks to Ticket Service
  - ticket:create - PARTNER
  - ticket:read - Owner, Agent (with permission)
  - ticket:scan - Agent (with event access)
  - ticket:refund - PARTNER, ADMIN
  - _Requirements: 6.3, 6.4, 8.2_

- [ ] 30.3 Add permission checks to Commerce Service
  - order:read - Owner, ADMIN
  - promotion:manage - PARTNER, ADMIN
  - settlement:read - Partner (own settlements), ADMIN
  - _Requirements: 8.2_


- [ ] 30.4 Write security tests for RBAC
  - Test PARTNER cannot approve campaigns
  - Test USER cannot create ticket types
  - Test Agent cannot scan without event permission
  - Test 403 Forbidden responses
  - _Requirements: 8.2, 11.3_

- [ ] 31. Implement rate limiting
- [ ] 31.1 Configure Redis rate limiting at API Gateway
  - Ad request endpoints: 1000 req/min per IP
  - Ticket scan: 60 req/min per agent
  - Order creation: 120 req/min per user
  - _Requirements: 8.7, 12.7_

- [ ] 31.2 Add Idempotency-Key validation filter
  - Validate Idempotency-Key header on financial endpoints
  - Return 400 if missing on POST /api/v1/orders
  - Cache processed keys in Redis (24h TTL)
  - _Requirements: 4.3, 8.3_

- [ ] 32. Implement audit logging
- [ ] 32.1 Add audit logs for financial operations
  - Log all order creation with user_id, amount, timestamp
  - Log all payment confirmations
  - Log all commission calculations
  - Log all settlement processing
  - Use structured logging (JSON format)
  - Store logs for 7 years (compliance)
  - _Requirements: 4.14, 8.8_

- [ ] 32.2 Add audit logs for ticket scanning
  - Log every scan attempt (success and failure)
  - Include agent_id, ticket_id, result, location, timestamp
  - Store in ticket_scan_logs table
  - _Requirements: 6.6_

### Phase 8: Analytics & Reporting

- [ ] 33. Integrate with Analytics Service
- [ ] 33.1 Publish metrics events to Analytics Service
  - Campaign performance metrics (impressions, clicks, CTR, conversions)
  - Ticket sales metrics (tickets sold, revenue, attendance rate)
  - Commerce metrics (total sales, commission earned)
  - Use Kafka topic: analytics.events
  - _Requirements: 7.1, 7.2, 7.3, 9.8_


- [ ] 33.2 Create scheduled aggregation jobs
  - Daily aggregation of ad metrics (02:00 UTC)
  - Weekly aggregation of ticket sales (Mondays 03:00 UTC)
  - Monthly aggregation of commerce metrics (1st day 04:00 UTC)
  - _Requirements: 7.5_

- [ ] 33.3 Add analytics endpoints
  - GET /api/v1/campaigns/{id}/performance (time-series data)
  - GET /api/v1/tickets/analytics (partner dashboard)
  - GET /api/v1/settlements/analytics (commission breakdown)
  - Support filtering by date range, granularity (daily, weekly, monthly)
  - _Requirements: 7.1-7.7_

- [ ] 33.4 Write integration tests for analytics
  - Test metrics aggregation job
  - Test analytics endpoints return correct data
  - Test filtering by date range
  - _Requirements: 7.1-7.7_

### Phase 9: API Gateway Configuration

- [ ] 34. Configure routes in API Gateway
- [ ] 34.1 Add routes for Campaign Service
  - Route /api/v1/campaigns/** to campaign-service
  - Apply JWT authentication filter
  - Apply rate limiting (120 req/min)
  - _Requirements: 9.11_

- [ ] 34.2 Add routes for Ads Delivery Service
  - Route /api/v1/ads/** to ads-delivery-service
  - Apply rate limiting (1000 req/min for ad requests)
  - _Requirements: 9.11_

- [ ] 34.3 Add routes for Ticket Service
  - Route /api/v1/ticket-types/**, /api/v1/tickets/**, /api/v1/agents/** to ticket-service
  - Apply JWT authentication filter
  - Apply rate limiting (60 req/min for scan endpoints)
  - _Requirements: 9.11_

- [ ] 34.4 Add routes for Commerce Service
  - Route /api/v1/orders/**, /api/v1/promotions/**, /api/v1/settlements/** to commerce-service
  - Apply JWT authentication filter
  - Apply Idempotency-Key validation for order creation
  - _Requirements: 9.11_


### Phase 10: Deployment & Observability

- [ ] 35. Create Docker configurations
- [ ] 35.1 Create Dockerfile for campaign-service
  - Multi-stage build (Maven build + JRE runtime)
  - Expose port (assign from existing port matrix, e.g., 8106)
  - Health check endpoint: /actuator/health
  - _Requirements: 11.1_

- [ ] 35.2 Create Dockerfile for ads-delivery-service
  - Multi-stage build
  - Expose port (e.g., 8107)
  - Health check endpoint
  - _Requirements: 11.1_

- [ ] 35.3 Create Dockerfile for ticket-service
  - Multi-stage build
  - Expose port (e.g., 8108)
  - Health check endpoint
  - _Requirements: 11.1_

- [ ] 35.4 Create Dockerfile for commerce-service
  - Multi-stage build
  - Expose port (e.g., 8109)
  - Health check endpoint
  - _Requirements: 11.1_

- [ ] 36. Configure service discovery and config
- [ ] 36.1 Add service configurations to cloud-conf-yeyamo repository
  - campaign-service.properties (DB, Kafka, port)
  - ads-delivery-service.properties (DB, Redis, Kafka, port)
  - ticket-service.properties (DB, Kafka, port)
  - commerce-service.properties (DB, Kafka, port)
  - _Requirements: 9.11_

- [ ] 36.2 Configure Eureka registration
  - Add @EnableDiscoveryClient to all services
  - Configure eureka.client.serviceUrl.defaultZone
  - Configure instance metadata
  - _Requirements: 9.11_


- [ ] 37. Configure observability
- [ ] 37.1 Add Prometheus metrics endpoints
  - Expose /actuator/prometheus on all services
  - Configure Micrometer metrics for:
    - HTTP request duration (P50, P95, P99)
    - Database connection pool usage
    - Kafka consumer lag
    - Redis cache hit rate
  - _Requirements: 12.10_

- [ ] 37.2 Configure structured logging
  - Use Logback with JSON encoder
  - Include correlation-ID in all log messages
  - Log levels: INFO for production, DEBUG for development
  - Mask sensitive data (payment tokens, personal info)
  - _Requirements: 8.6_

- [ ] 37.3 Configure alerts
  - High error rate (>1% of requests)
  - High latency (P95 > 1s)
  - Low database connection pool availability
  - Kafka consumer lag > 1000 messages
  - _Requirements: 12.10_

- [ ] 38. Update docker-compose.yml
- [ ] 38.1 Add new services to docker-compose.yml
  - campaign-service with PostgreSQL dependency
  - ads-delivery-service with PostgreSQL, Redis, Kafka dependencies
  - ticket-service with PostgreSQL dependency
  - commerce-service with PostgreSQL, Kafka dependencies
  - Configure environment variables from .env file
  - _Requirements: 11.1_

- [ ] 39. Create OpenAPI documentation
- [ ] 39.1 Add SpringDoc OpenAPI dependency to all services
  - Configure OpenAPI 3.0 metadata (title, version, description)
  - Add security scheme (Bearer JWT)
  - Annotate controllers with @Operation, @ApiResponse
  - Generate interactive docs at /swagger-ui.html
  - _Requirements: 11.8_


### Phase 11: Testing & Validation

- [ ] 40. Run comprehensive test suite
- [ ] 40.1 Execute all unit tests
  - Run `mvn test` on all four services
  - Verify 80%+ code coverage
  - Fix any failing tests
  - _Requirements: All_

- [ ] 40.2 Execute all property-based tests
  - Run jqwik tests with 100 iterations minimum
  - Verify all 20 correctness properties pass
  - Investigate and fix any failing properties
  - _Requirements: All correctness properties_

- [ ] 40.3 Execute integration tests with Testcontainers
  - Verify PostgreSQL schema migrations
  - Verify Kafka event publication and consumption
  - Verify Redis caching
  - Verify Outbox/Inbox patterns work correctly
  - _Requirements: 10.1-10.10_

- [ ] 40.4 Execute end-to-end tests
  - Test full ticket purchase flow: order → payment → ticket issued → QR scan
  - Test full campaign flow: create → submit → approve → activate → ad delivery
  - Test promotion application on order
  - _Requirements: 1.1-12.10_

- [ ] 40.5 Run performance tests with k6
  - Test ad delivery: 1000 req/s, P95 < 200ms
  - Test ticket scanning: 100 req/s, P95 < 500ms
  - Test order creation: 200 req/s, P95 < 1s
  - Generate performance report
  - _Requirements: 12.1, 12.2, 12.3_

- [ ] 41. Security validation
- [ ] 41.1 Run OWASP Dependency-Check
  - Scan for vulnerable dependencies
  - Update any dependencies with CVSS > 7
  - Document acceptable risks for CVSS < 7
  - _Requirements: 8.1-8.10_


- [ ] 41.2 Validate RBAC implementation
  - Test all permission checks with different roles
  - Verify 403 responses for unauthorized access
  - Test IDOR protection (users cannot access others' resources)
  - _Requirements: 8.2, 11.3_

- [ ] 41.3 Validate Idempotency-Key implementation
  - Test duplicate order requests with same key return same result
  - Test missing Idempotency-Key returns 400
  - Verify no duplicate charges occur
  - _Requirements: 4.3, 8.3_

- [ ] 42. Final checkpoint - System validation
  - All services start successfully and register with Eureka
  - All Kafka topics created with correct configuration
  - All database migrations applied successfully
  - All API endpoints accessible through Gateway
  - All tests passing (unit, integration, property-based, e2e)
  - Performance targets met (P95 latencies)
  - Security validations passed
  - Ask the user if ready for production deployment

## Notes

**Incremental Delivery Strategy:**
- Phase 1-2: Campaign Service MVP (2 weeks)
- Phase 3: Ads Delivery Service MVP (2 weeks)
- Phase 4: Ticket Service MVP (2 weeks)
- Phase 5: Commerce Service MVP (2 weeks)
- Phase 6-7: Integrations & Security (2 weeks)
- Phase 8-11: Analytics, Deployment, Testing (2 weeks)

**Total Estimated Timeline:** 12 weeks (3 months) with 2 backend developers

**Dependencies:**
- PostgreSQL 16 with PostGIS extension
- Redis 7 for caching and rate limiting
- Apache Kafka 3.6 for event streaming
- Existing services: auth-service, payment-service, event-service, catalog-service, notification-service, analytics-service

**Testing Strategy:**
- Unit tests: JUnit 5 + Mockito (80% coverage minimum)
- Property-based tests: jqwik (100 iterations per property) - ALL REQUIRED
- Integration tests: Spring Boot Test + Testcontainers - ALL REQUIRED
- Performance tests: k6 load testing - REQUIRED
- Security tests: OWASP Dependency-Check + manual RBAC validation - REQUIRED

**All Tasks Required:**
- No optional tasks - comprehensive implementation from start
- Total tasks: 42 main tasks with 150+ sub-tasks
- All testing tasks (unit, property, integration, performance, security) are REQUIRED

