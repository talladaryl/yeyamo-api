# Ticket Service - Guide d'Implémentation Complet

## Vue d'Ensemble

Le ticket-service est un microservice critique avec des exigences fortes en:
- **Cohérence**: Pas de survente (inventory management)
- **Sécurité**: QR tokens signés, validation atomique
- **Performance**: Scan rapide (<200ms), support charge (1000+ scans/min)
- **Idempotence**: Commandes, émission, callbacks paiement

---

## Architecture Technique

### Stack
- **Framework**: Spring Boot 4.1.0, Java 21
- **Database**: PostgreSQL avec optimistic locking
- **Cache**: Redis pour hold expiration
- **Messaging**: Kafka avec Transactional Outbox
- **Security**: JWT (JJWT), RSA 2048 pour QR
- **Testing**: JUnit 5, Testcontainers, Awaitility

### Patterns
- **Hexagonal Architecture** (Ports & Adapters)
- **Transactional Outbox** (garantie événements)
- **Optimistic Locking** (éviter survente)
- **Idempotency Keys** (commandes, holds)
- **Event Sourcing léger** (scans audit log)

---

## Composants Clés

### 1. Inventory Management (Anti-Survente)

#### TicketType Entity (avec optimistic locking)
```java
@Entity
@Table(name = "ticket_types")
public class TicketTypeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private Integer quantityTotal;
    private Integer quantityReserved;
    private Integer quantitySold;
    
    @Version  // ← CRITIQUE: Optimistic locking
    private Long version;
    
    // Business validation
    public boolean canReserve(int quantity) {
        int available = quantityTotal - quantityReserved - quantitySold;
        return available >= quantity;
    }
    
    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new InsufficientInventoryException();
        }
        this.quantityReserved += quantity;
    }
    
    public void releaseReservation(int quantity) {
        this.quantityReserved -= quantity;
    }
    
    public void confirmSale(int quantity) {
        this.quantityReserved -= quantity;
        this.quantitySold += quantity;
    }
}
```

#### Inventory Service (Transaction + Locking)
```java
@Service
public class InventoryService {
    
    @Transactional
    public TicketHold createHold(CreateHoldRequest request) {
        // 1. Idempotency check
        Optional<TicketHold> existing = holdRepository
            .findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // 2. Pessimistic lock on ticket type
        TicketTypeEntity type = ticketTypeRepository
            .findByIdForUpdate(request.ticketTypeId())
            .orElseThrow();
        
        // 3. Validate availability
        if (!type.canReserve(request.quantity())) {
            throw new InsufficientInventoryException();
        }
        
        // 4. Reserve inventory
        type.reserve(request.quantity());
        
        // 5. Create hold with expiration
        TicketHold hold = new TicketHold();
        hold.setUserId(request.userId());
        hold.setTicketTypeId(type.getId());
        hold.setQuantity(request.quantity());
        hold.setExpiresAt(Instant.now().plusSeconds(600)); // 10min
        hold.setIdempotencyKey(request.idempotencyKey());
        hold.setStatus("ACTIVE");
        
        return holdRepository.save(hold);
    }
    
    @Transactional
    public void releaseExpiredHolds() {
        List<TicketHold> expired = holdRepository
            .findExpiredHolds(Instant.now());
        
        for (TicketHold hold : expired) {
            TicketTypeEntity type = ticketTypeRepository
                .findByIdForUpdate(hold.getTicketTypeId())
                .orElseThrow();
            
            type.releaseReservation(hold.getQuantity());
            hold.setStatus("EXPIRED");
            
            logger.info("Released hold: {} ({} tickets)", 
                hold.getId(), hold.getQuantity());
        }
    }
}
```

#### Repository avec Locking
```java
@Repository
public interface TicketTypeRepository extends JpaRepository<TicketTypeEntity, UUID> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TicketTypeEntity t WHERE t.id = :id")
    Optional<TicketTypeEntity> findByIdForUpdate(@Param("id") UUID id);
    
    @Query("SELECT t FROM TicketTypeEntity t WHERE t.saleConfigurationId = :configId")
    List<TicketTypeEntity> findBySaleConfiguration(@Param("configId") UUID configId);
}
```

---

### 2. QR Token Security

#### Token Structure (JWT)
```json
{
  "jti": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "sub": "ticket-uuid",
  "iss": "ticket-service",
  "iat": 1234567890,
  "exp": 1267567890,
  "kid": "key-2024-q1",
  "evt": "event-123",
  "typ": "TICKET_QR",
  "ver": 1
}
```

#### QR Token Service
```java
@Service
public class QrTokenService {
    
    private final KeyManager keyManager;
    
    public String generateQrToken(Ticket ticket) {
        // 1. Get current signing key
        SigningKey key = keyManager.getCurrentKey();
        
        // 2. Generate unique token ID
        String tokenId = UUID.randomUUID().toString();
        
        // 3. Build JWT
        String token = Jwts.builder()
            .header()
                .add("kid", key.getKeyId())
                .and()
            .id(tokenId)
            .subject(ticket.getId().toString())
            .issuer("ticket-service")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)))
            .claim("evt", ticket.getEventId())
            .claim("typ", "TICKET_QR")
            .claim("ver", 1)
            .signWith(key.getPrivateKey())
            .compact();
        
        // 4. Store credential (hash only)
        TicketQrCredential credential = new TicketQrCredential();
        credential.setTicketId(ticket.getId());
        credential.setTokenId(tokenId);
        credential.setTokenHash(hashToken(token));
        credential.setKeyId(key.getKeyId());
        credential.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        
        credentialRepository.save(credential);
        
        return token;
    }
    
    public QrValidationResult validateQrToken(String token, String eventId) {
        try {
            // 1. Parse JWT (verify signature)
            Jws<Claims> jws = Jwts.parser()
                .verifyWith(keyManager.getPublicKeys())  // Multiple keys support
                .build()
                .parseSignedClaims(token);
            
            Claims claims = jws.getPayload();
            
            // 2. Extract claims
            String tokenId = claims.getId();
            String ticketId = claims.getSubject();
            String tokenEventId = claims.get("evt", String.class);
            
            // 3. Verify event match
            if (!eventId.equals(tokenEventId)) {
                return QrValidationResult.wrongEvent();
            }
            
            // 4. Check revocation
            Optional<TicketQrCredential> credential = 
                credentialRepository.findByTokenId(tokenId);
            
            if (credential.isEmpty() || credential.get().getRevokedAt() != null) {
                return QrValidationResult.revoked();
            }
            
            // 5. Return valid with ticket ID
            return QrValidationResult.valid(ticketId);
            
        } catch (ExpiredJwtException e) {
            return QrValidationResult.expired();
        } catch (JwtException e) {
            return QrValidationResult.invalid();
        }
    }
    
    private String hashToken(String token) {
        return DigestUtils.sha256Hex(token);
    }
}
```

#### Key Manager (Rotation Support)
```java
@Component
public class KeyManager {
    
    private final Map<String, KeyPair> keys = new ConcurrentHashMap<>();
    private volatile String currentKeyId;
    
    @PostConstruct
    public void init() {
        loadKeys();
    }
    
    private void loadKeys() {
        // Load from KeyStore or Vault
        // For now, generate in-memory (dev only)
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        
        String keyId = "key-" + LocalDate.now().toString();
        KeyPair keyPair = generator.generateKeyPair();
        
        keys.put(keyId, keyPair);
        currentKeyId = keyId;
        
        logger.info("Loaded QR signing key: {}", keyId);
    }
    
    public SigningKey getCurrentKey() {
        KeyPair keyPair = keys.get(currentKeyId);
        return new SigningKey(currentKeyId, keyPair.getPrivate());
    }
    
    public Map<String, PublicKey> getPublicKeys() {
        return keys.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().getPublic()
            ));
    }
    
    @Scheduled(cron = "${yeyamo.tickets.qr.rotation-cron:0 0 0 1 */3 *}")
    public void rotateKeys() {
        logger.info("Rotating QR signing keys...");
        
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        
        String newKeyId = "key-" + LocalDate.now().toString();
        KeyPair newKeyPair = generator.generateKeyPair();
        
        keys.put(newKeyId, newKeyPair);
        currentKeyId = newKeyId;
        
        // Keep old keys for 90 days for verification
        Instant cutoff = Instant.now().minus(90, ChronoUnit.DAYS);
        keys.entrySet().removeIf(e -> 
            e.getKey().compareTo("key-" + cutoff.toString()) < 0
        );
        
        logger.info("Key rotation complete. New key: {}", newKeyId);
    }
}
```

---

### 3. Scan Service (Atomic Validation)

```java
@Service
public class ScanService {
    
    @Transactional
    public ScanResult scanTicket(ScanRequest request) {
        // 1. Validate QR token
        QrValidationResult validation = qrTokenService
            .validateQrToken(request.qrToken(), request.eventId());
        
        if (!validation.isValid()) {
            return recordFailedScan(request, validation.getReason());
        }
        
        UUID ticketId = validation.getTicketId();
        
        // 2. Load ticket with pessimistic lock
        TicketEntity ticket = ticketRepository
            .findByIdForUpdate(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));
        
        // 3. Validate ticket status
        if (ticket.getStatus() == TicketStatus.USED) {
            ScanResult result = recordFailedScan(
                request, 
                ScanResult.ALREADY_USED,
                ticket
            );
            result.setFirstUsedAt(ticket.getUsedAt());
            result.setFirstScannerName(getFirstScannerName(ticket));
            return result;
        }
        
        if (ticket.getStatus() != TicketStatus.VALID) {
            return recordFailedScan(
                request, 
                mapTicketStatusToScanResult(ticket.getStatus()),
                ticket
            );
        }
        
        // 4. Verify event match
        if (!ticket.getEventId().equals(request.eventId())) {
            return recordFailedScan(request, ScanResult.WRONG_EVENT, ticket);
        }
        
        // 5. Verify staff authorization
        if (!isStaffAuthorized(request.scannerUserId(), request.eventId())) {
            return recordFailedScan(request, ScanResult.ACCESS_DENIED, ticket);
        }
        
        // 6. ATOMIC: Mark as used
        ticket.setStatus(TicketStatus.USED);
        ticket.setUsedAt(Instant.now());
        ticket.incrementVersion();  // Optimistic locking
        
        ticketRepository.save(ticket);
        
        // 7. Record successful scan
        TicketScan scan = new TicketScan();
        scan.setTicketId(ticketId);
        scan.setEventId(request.eventId());
        scan.setScannerUserId(request.scannerUserId());
        scan.setGateId(request.gateId());
        scan.setResult(ScanResult.VALID);
        scan.setScannedAt(Instant.now());
        scan.setDeviceIdHash(hashDeviceId(request.deviceId()));
        
        scanRepository.save(scan);
        
        // 8. Publish event (via outbox)
        outboxService.publish(new TicketScannedEvent(
            ticket.getId(),
            ticket.getEventId(),
            ticket.getOwnerUserId(),
            request.scannerUserId(),
            Instant.now()
        ));
        
        // 9. Build response
        return ScanResult.success(ticket);
    }
    
    private boolean isStaffAuthorized(String userId, String eventId) {
        return staffRepository.existsByUserIdAndEventIdAndStatusActive(
            userId, eventId
        );
    }
    
    private ScanResult recordFailedScan(
            ScanRequest request, 
            ScanResultCode code, 
            TicketEntity ticket) {
        
        TicketScan scan = new TicketScan();
        scan.setTicketId(ticket != null ? ticket.getId() : null);
        scan.setEventId(request.eventId());
        scan.setScannerUserId(request.scannerUserId());
        scan.setResult(code);
        scan.setReasonCode(code.name());
        scan.setScannedAt(Instant.now());
        
        scanRepository.save(scan);
        
        return ScanResult.failure(code, ticket);
    }
}
```

---

### 4. Order & Payment Flow

#### Order Creation (Idempotent)
```java
@Service
public class OrderService {
    
    @Transactional
    public TicketOrder createOrder(CreateOrderRequest request) {
        // 1. Idempotency check
        String idempotencyKey = request.userId() + ":" + request.holdId();
        Optional<TicketOrder> existing = orderRepository
            .findByIdempotencyKey(idempotencyKey);
        
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // 2. Validate hold
        TicketHold hold = holdRepository.findById(request.holdId())
            .orElseThrow(() -> new HoldNotFoundException());
        
        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new HoldExpiredException();
        }
        
        if (hold.getExpiresAt().isBefore(Instant.now())) {
            throw new HoldExpiredException();
        }
        
        // 3. Calculate amounts
        TicketType ticketType = ticketTypeRepository
            .findById(hold.getTicketTypeId())
            .orElseThrow();
        
        BigDecimal subtotal = ticketType.getPrice()
            .multiply(BigDecimal.valueOf(hold.getQuantity()));
        
        BigDecimal serviceFee = subtotal
            .multiply(BigDecimal.valueOf(0.05));  // 5% fee
        
        BigDecimal total = subtotal.add(serviceFee);
        
        // 4. Create order
        TicketOrder order = new TicketOrder();
        order.setReference(generateOrderReference());
        order.setUserId(request.userId());
        order.setPartnerId(hold.getPartnerId());
        order.setEventId(hold.getEventId());
        order.setStatus(TicketOrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setSubtotal(subtotal);
        order.setServiceFee(serviceFee);
        order.setTotalAmount(total);
        order.setCurrency(ticketType.getCurrency());
        order.setExpiresAt(hold.getExpiresAt());
        
        orderRepository.save(order);
        
        // 5. Mark hold as used
        hold.setStatus(HoldStatus.CONSUMED);
        hold.setOrderId(order.getId());
        
        // 6. Request payment
        outboxService.publish(new PaymentRequestedEvent(
            order.getId().toString(),
            order.getUserId(),
            order.getTotalAmount(),
            order.getCurrency(),
            "TICKET_ORDER",
            Map.of(
                "orderId", order.getId().toString(),
                "eventId", order.getEventId(),
                "quantity", hold.getQuantity()
            )
        ));
        
        return order;
    }
    
    private String generateOrderReference() {
        return "TKT-" + Instant.now().getEpochSecond() + "-" 
            + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
```

#### Payment Confirmation Handler
```java
@Service
public class PaymentEventHandler {
    
    @KafkaListener(topics = "payment-confirmed")
    @Transactional
    public void handlePaymentConfirmed(PaymentConfirmedEvent event) {
        UUID orderId = UUID.fromString(event.aggregateId());
        
        TicketOrder order = orderRepository.findById(orderId)
            .orElseThrow();
        
        if (order.getPaymentStatus() == PaymentStatus.CONFIRMED) {
            logger.warn("Payment already confirmed for order: {}", orderId);
            return;  // Idempotent
        }
        
        // 1. Update order status
        order.setPaymentStatus(PaymentStatus.CONFIRMED);
        order.setStatus(TicketOrderStatus.PAID);
        order.setPaidAt(Instant.now());
        order.setPaymentReference(event.paymentReference());
        order.setPaymentProvider(event.provider());
        
        // 2. Confirm inventory sale
        TicketHold hold = holdRepository.findByOrderId(orderId)
            .orElseThrow();
        
        TicketType ticketType = ticketTypeRepository
            .findByIdForUpdate(hold.getTicketTypeId())
            .orElseThrow();
        
        ticketType.confirmSale(hold.getQuantity());
        
        // 3. Issue tickets
        ticketIssuanceService.issueTickets(order, hold);
        
        logger.info("Payment confirmed and tickets issued for order: {}", orderId);
    }
    
    @KafkaListener(topics = "payment-failed")
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        UUID orderId = UUID.fromString(event.aggregateId());
        
        TicketOrder order = orderRepository.findById(orderId)
            .orElseThrow();
        
        // 1. Update order status
        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setStatus(TicketOrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        
        // 2. Release inventory (compensation)
        TicketHold hold = holdRepository.findByOrderId(orderId)
            .orElseThrow();
        
        TicketType ticketType = ticketTypeRepository
            .findByIdForUpdate(hold.getTicketTypeId())
            .orElseThrow();
        
        ticketType.releaseReservation(hold.getQuantity());
        
        logger.warn("Payment failed, released inventory for order: {}", orderId);
    }
}
```

---

### 5. Ticket Issuance

```java
@Service
public class TicketIssuanceService {
    
    @Transactional
    public List<Ticket> issueTickets(TicketOrder order, TicketHold hold) {
        List<Ticket> tickets = new ArrayList<>();
        
        for (int i = 0; i < hold.getQuantity(); i++) {
            Ticket ticket = new Ticket();
            ticket.setOrderId(order.getId());
            ticket.setEventId(order.getEventId());
            ticket.setTicketTypeId(hold.getTicketTypeId());
            ticket.setOwnerUserId(order.getUserId());
            ticket.setSerialNumber(generateSerialNumber(order, i));
            ticket.setStatus(TicketStatus.VALID);
            ticket.setIssuedAt(Instant.now());
            
            ticketRepository.save(ticket);
            
            // Generate QR token
            String qrToken = qrTokenService.generateQrToken(ticket);
            ticket.setQrToken(qrToken);  // Store for retrieval
            
            tickets.add(ticket);
        }
        
        // Update order status
        order.setStatus(TicketOrderStatus.ISSUED);
        order.setIssuedAt(Instant.now());
        
        // Publish events
        outboxService.publish(new TicketsIssuedEvent(
            order.getId(),
            order.getUserId(),
            order.getEventId(),
            tickets.stream().map(Ticket::getId).toList(),
            Instant.now()
        ));
        
        logger.info("Issued {} tickets for order: {}", tickets.size(), order.getId());
        
        return tickets;
    }
    
    private String generateSerialNumber(TicketOrder order, int index) {
        return String.format("%s-%03d", 
            order.getReference(), 
            index + 1
        );
    }
}
```

---

## Tests Critiques

### Test 1: Concurrent Purchase (No Overselling)

```java
@SpringBootTest
@Testcontainers
class ConcurrentPurchaseTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");
    
    @Test
    void shouldNotOversell_whenConcurrentPurchases() throws Exception {
        // Setup: 10 tickets available
        TicketType type = createTicketType(10);
        
        // Act: 20 concurrent purchase attempts
        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        
        List<Future<Boolean>> results = new ArrayList<>();
        
        for (int i = 0; i < threads; i++) {
            String userId = "user-" + i;
            results.add(executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();  // Synchronize start
                    
                    // Attempt to purchase 1 ticket
                    holdService.createHold(new CreateHoldRequest(
                        userId, 
                        type.getId(), 
                        1, 
                        UUID.randomUUID().toString()
                    ));
                    return true;
                } catch (InsufficientInventoryException e) {
                    return false;
                }
            }));
        }
        
        // Assert: Exactly 10 successes, 10 failures
        long successes = results.stream()
            .map(f -> f.get())
            .filter(success -> success)
            .count();
        
        assertEquals(10, successes);
        
        // Verify database state
        TicketType updated = ticketTypeRepository.findById(type.getId()).get();
        assertEquals(10, updated.getQuantityReserved());
        assertEquals(0, updated.getQuantitySold());
    }
}
```

### Test 2: Double Scan Detection

```java
@Test
void shouldRejectDoubleScan_whenScannedTwiceConcurrently() throws Exception {
    // Setup: Valid ticket
    Ticket ticket = createValidTicket();
    String qrToken = qrTokenService.generateQrToken(ticket);
    
    // Act: 2 concurrent scans
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch latch = new CountDownLatch(2);
    
    Future<ScanResult> scan1 = executor.submit(() -> {
        latch.countDown();
        latch.await();
        return scanService.scanTicket(new ScanRequest(
            qrToken, 
            ticket.getEventId(), 
            "scanner-1",
            null,
            null
        ));
    });
    
    Future<ScanResult> scan2 = executor.submit(() -> {
        latch.countDown();
        latch.await();
        return scanService.scanTicket(new ScanRequest(
            qrToken, 
            ticket.getEventId(), 
            "scanner-2",
            null,
            null
        ));
    });
    
    // Assert: One VALID, one ALREADY_USED
    ScanResult result1 = scan1.get();
    ScanResult result2 = scan2.get();
    
    assertTrue(
        (result1.getResult() == ScanResultCode.VALID && 
         result2.getResult() == ScanResultCode.ALREADY_USED) ||
        (result1.getResult() == ScanResultCode.ALREADY_USED && 
         result2.getResult() == ScanResultCode.VALID)
    );
    
    // Verify ticket used only once
    Ticket updated = ticketRepository.findById(ticket.getId()).get();
    assertEquals(TicketStatus.USED, updated.getStatus());
    assertNotNull(updated.getUsedAt());
}
```

### Test 3: QR Token Forgery

```java
@Test
void shouldRejectFakeQrToken() {
    // Fake token with wrong signature
    String fakeToken = Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(UUID.randomUUID().toString())
        .issuer("ticket-service")
        .claim("evt", "event-123")
        .signWith(generateFakeKey())  // Wrong key!
        .compact();
    
    QrValidationResult result = qrTokenService.validateQrToken(
        fakeToken, 
        "event-123"
    );
    
    assertEquals(QrValidationResultCode.INVALID, result.getCode());
}
```

---

## Configuration Cloud

```properties
# ticket-service.properties

# Database
spring.datasource.url=jdbc:postgresql://postgres:5432/ticket_db
spring.datasource.username=${TICKET_DB_USERNAME}
spring.datasource.password=${TICKET_DB_PASSWORD}

# JPA
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# Redis
spring.data.redis.host=${REDIS_HOST:redis}
spring.data.redis.port=6379

# Kafka
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:kafka:9092}
spring.kafka.consumer.group-id=ticket-service
spring.kafka.consumer.auto-offset-reset=earliest

# Eureka
eureka.client.service-url.defaultZone=${EUREKA_SERVER_URL:http://discovery-service:8761/eureka/}
eureka.instance.prefer-ip-address=true

# Business Rules
yeyamo.tickets.hold-ttl-minutes=10
yeyamo.tickets.max-per-buyer=10
yeyamo.tickets.service-fee-percent=5.0

# QR Security
yeyamo.tickets.qr.token-ttl-days=30
yeyamo.tickets.qr.key-rotation-days=90
yeyamo.tickets.qr.algorithm=RS256

# Scheduler
yeyamo.tickets.scheduler.release-holds-cron=0 */5 * * * *
yeyamo.tickets.scheduler.cleanup-expired-cron=0 0 2 * * *

# Actuator
management.endpoints.web.exposure.include=health,info,metrics,prometheus

# Security
spring.security.oauth2.resourceserver.jwt.issuer-uri=${JWT_ISSUER_URI:http://auth-service:8080}

# Logging
logging.level.com.yeyamo_mobile.api.ticket_service=INFO
logging.level.com.yeyamo_mobile.api.ticket_service.domain=DEBUG
```

---

## Commandes Docker

```bash
# Build
cd ticket-service
mvn clean package -DskipTests

# Docker image
docker build -t yeyamo/ticket-service:latest .

# Run tests (avec Testcontainers)
mvn test

# Tests d'intégration
mvn verify -P integration-tests

# Run service
docker-compose up -d ticket-service
```

---

## Endpoints Swagger

http://localhost:8086/swagger-ui.html

---

## Métriques Prometheus

http://localhost:8086/actuator/prometheus

```promql
# Sales
ticket_holds_created_total
ticket_orders_created_total
ticket_issued_total{event_id}

# Scans
ticket_scans_total{result, event_id}
ticket_scan_duration_seconds

# Inventory
ticket_inventory_available{ticket_type_id}
ticket_inventory_reserved{ticket_type_id}
```

---

**Statut**: ✅ Prêt pour implémentation complète
**Complexité**: ÉLEVÉE
**Criticité**: MAXIMALE (money + security)
