# Guide d'Implémentation - YeYamo Ticket Service

Ce document détaille l'implémentation complète du service de billetterie.

## Table des Matières

1. [Architecture](#architecture)
2. [Gestion de l'Inventaire](#gestion-de-linventaire)
3. [Sécurité QR Code](#sécurité-qr-code)
4. [Processus de Scan](#processus-de-scan)
5. [Transactional Outbox](#transactional-outbox)
6. [Tests](#tests)
7. [Déploiement](#déploiement)

## Architecture

### Vue d'ensemble

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   Client    │─────▶│  API Gateway │─────▶│   Ticket    │
│   Mobile    │      │              │      │   Service   │
└─────────────┘      └──────────────┘      └─────────────┘
                                                   │
                     ┌─────────────────────────────┼──────────────┐
                     │                             │              │
                     ▼                             ▼              ▼
              ┌──────────────┐            ┌──────────────┐  ┌─────────┐
              │  PostgreSQL  │            │    Redis     │  │  Kafka  │
              │  (Data)      │            │  (Locks)     │  │ (Events)│
              └──────────────┘            └──────────────┘  └─────────┘
```

### Entités Principales

1. **TicketSaleConfiguration** : Configuration de vente pour un événement
2. **TicketType** : Types de billets avec inventaire
3. **TicketHold** : Réservations temporaires
4. **TicketOrder** : Commandes de billets
5. **Ticket** : Billets individuels émis
6. **TicketQrCredential** : Credentials de sécurité QR
7. **EventStaffAssignment** : Affectation du personnel
8. **TicketScan** : Journal d'audit des scans

## Gestion de l'Inventaire

### Problème : Survente (Overselling)

Sans contrôle de concurrence, plusieurs utilisateurs peuvent réserver le dernier billet simultanément.

### Solution : Verrouillage Multi-Niveaux

#### 1. Verrouillage Distribué (Redis/Redisson)

```java
String lockKey = "ticket:inventory:" + ticketTypeId;
RLock lock = redissonClient.getLock(lockKey);

try {
    lock.lock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    
    // Operations critiques sur l'inventaire
    
} finally {
    lock.unlock();
}
```

**Avantages** :
- Fonctionne en multi-instance
- Timeout pour éviter les deadlocks
- Libération automatique si le service crash

#### 2. Verrouillage Optimiste (JPA @Version)

```java
@Entity
public class TicketType {
    @Version
    private Long version;  // Incrémenté à chaque modification
    
    private Integer quantityReserved;
    private Integer quantitySold;
}
```

**Fonctionnement** :
- JPA vérifie que la version n'a pas changé avant UPDATE
- Si changée → OptimisticLockException
- Retry automatique ou manuel

#### 3. Verrouillage Pessimiste (Pour les scans)

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT t FROM Ticket t WHERE t.id = :id")
Optional<Ticket> findByIdWithLock(String id);
```

**Usage** :
- Empêche les lectures concurrentes
- Garantit l'unicité du scan
- Libéré à la fin de la transaction

### Workflow Complet : Création d'un Hold

```java
@Transactional
public TicketHold createHold(...) {
    // 1. Vérifier idempotence
    Optional<TicketHold> existing = holdRepository.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
        return existing.get();
    }
    
    // 2. Acquérir lock distribué
    RLock lock = redissonClient.getLock("ticket:inventory:" + ticketTypeId);
    lock.lock();
    
    try {
        // 3. Charger avec lock pessimiste
        TicketType ticketType = ticketTypeRepository.findByIdWithLock(ticketTypeId);
        
        // 4. Vérifier disponibilité
        if (!ticketType.hasAvailableTickets(quantity)) {
            throw new IllegalStateException("Insufficient inventory");
        }
        
        // 5. Réserver
        ticketType.reserveTickets(quantity);
        ticketTypeRepository.save(ticketType);  // Version check here
        
        // 6. Créer hold
        TicketHold hold = TicketHold.builder()
            .userId(userId)
            .ticketTypeId(ticketTypeId)
            .quantity(quantity)
            .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
            .idempotencyKey(idempotencyKey)
            .build();
        
        return holdRepository.save(hold);
        
    } finally {
        lock.unlock();
    }
}
```

### Expiration Automatique des Holds

```java
@Scheduled(fixedDelay = 300000)  // Toutes les 5 minutes
@Transactional
public int expireOldHolds() {
    List<TicketHold> expired = holdRepository.findExpiredHolds(Instant.now());
    
    for (TicketHold hold : expired) {
        releaseHold(hold.getId());  // Libère l'inventaire
    }
    
    return expired.size();
}
```

## Sécurité QR Code

### Exigences

1. ❌ **PAS de données personnelles** dans le QR
2. ✅ **Signature cryptographique** (RSA)
3. ✅ **Rotation des clés** régulière
4. ✅ **Révocation** possible
5. ✅ **Validation serveur** obligatoire

### Architecture JWT

Le QR contient un JWT signé avec RSA-256 :

```json
{
  "header": {
    "alg": "RS256",
    "kid": "key-1234567890"
  },
  "payload": {
    "jti": "unique-token-id",
    "sub": "ticket-id",
    "eventId": "event-123",
    "iat": 1234567890,
    "exp": 1265567890
  },
  "signature": "..."
}
```

### Génération du Token

```java
public QrTokenData generateToken(String ticketId, String eventId, int expiryDays) {
    String tokenId = UUID.randomUUID().toString();
    String keyId = keyManager.getCurrentKeyId();
    
    String token = Jwts.builder()
        .setId(tokenId)                    // Unique ID pour révocation
        .setSubject(ticketId)              // Référence ticket
        .claim("eventId", eventId)         // Événement
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(expiry))
        .setHeaderParam("kid", keyId)      // ID de la clé
        .signWith(privateKey, RS256)       // Signature asymétrique
        .compact();
    
    String hash = sha256(token);           // Hash pour stockage
    
    return new QrTokenData(token, tokenId, hash, keyId, now, expiry);
}
```

### Validation du Token

```java
public TokenValidationResult validateToken(String token) {
    try {
        // 1. Parser header pour obtenir kid
        String keyId = extractKeyId(token);
        
        // 2. Vérifier que la clé existe et est valide
        if (!keyManager.isKeyIdValid(keyId)) {
            return invalid("Invalid signing key");
        }
        
        // 3. Valider signature avec clé publique
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(keyManager.getPublicKey(keyId))
            .build()
            .parseClaimsJws(token)
            .getBody();
        
        // 4. Extraire claims
        String ticketId = claims.getSubject();
        String eventId = claims.get("eventId", String.class);
        
        return valid(ticketId, eventId, ...);
        
    } catch (ExpiredJwtException e) {
        return invalid("Token expired");
    } catch (SignatureException e) {
        return invalid("Invalid signature");
    }
}
```

### Rotation des Clés

```java
@Scheduled(cron = "0 0 0 */90 * *")  // Tous les 90 jours
public void rotateKeysIfNeeded() {
    if (keyManager.shouldRotateKey()) {
        keyManager.rotateKeys();
        log.info("Signing keys rotated");
    }
}
```

**Processus** :
1. Génération d'une nouvelle paire de clés
2. Nouveaux tokens signés avec la nouvelle clé
3. Anciens tokens validés avec leur clé originale (via kid)
4. Anciennes clés conservées 180 jours puis supprimées

### Stockage Sécurisé

```java
@Entity
public class TicketQrCredential {
    private String ticketId;
    private String tokenId;        // jti du JWT
    private String tokenHash;      // SHA-256 du token complet
    private String keyId;          // kid pour rotation
    private Instant expiresAt;
    private Instant revokedAt;     // NULL = non révoqué
}
```

**Pourquoi stocker le hash ?**
- Permet la révocation sans stocker le token
- Détection de replay attacks
- Audit trail

## Processus de Scan

### Exigences

1. **Atomicité** : 2 scans concurrents → 1 seul réussit
2. **Performance** : < 200ms par scan
3. **Audit** : Tous les scans loggés
4. **Sécurité** : Validation multicouches

### Workflow Détaillé

```java
@Transactional
public TicketScanResponse scanTicket(String scannerId, TicketScanRequest request) {
    // 1. Vérifier autorisation scanner
    EventStaffAssignment staff = staffRepository
        .findActiveAssignment(request.getEventId(), scannerId, Instant.now())
        .orElse(null);
    
    if (staff == null || !staff.canScanTickets()) {
        return denied("Scanner not authorized");
    }
    
    // 2. Valider signature QR
    TokenValidationResult tokenValidation = qrTokenService.validateToken(request.getQrToken());
    
    if (!tokenValidation.isValid()) {
        return denied(tokenValidation.getErrorMessage());
    }
    
    // 3. Vérifier événement
    if (!request.getEventId().equals(tokenValidation.getEventId())) {
        return denied("Wrong event");
    }
    
    // 4. Vérifier révocation
    TicketQrCredential credential = qrCredentialRepository
        .findByTokenId(tokenValidation.getTokenId())
        .orElse(null);
    
    if (credential == null || credential.isRevoked()) {
        return denied("Token revoked");
    }
    
    // 5. LOCK PESSIMISTE : Charger ticket
    Ticket ticket = ticketRepository
        .findByIdWithLock(tokenValidation.getTicketId())
        .orElseThrow();
    
    // 6. Vérifier statut
    if (ticket.getStatus() == TicketStatus.USED) {
        return denied("Already used");
    }
    
    if (ticket.getStatus() != TicketStatus.VALID) {
        return denied("Ticket not valid");
    }
    
    // 7. ATOMIC : Marquer comme utilisé
    ticket.markAsUsed();
    ticketRepository.save(ticket);
    
    // 8. Enregistrer scan
    TicketScan scan = recordScan(ticket, scannerId, staff, ScanResult.VALID);
    
    // 9. Publier événement
    outboxService.publishTicketUsed(ticket, scan);
    
    return success(ticket);
}
```

### Prévention Double-Scan

**Mécanisme 1 : Lock Pessimiste**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT t FROM Ticket t WHERE t.id = :id")
Optional<Ticket> findByIdWithLock(String id);
```

- Thread 1 acquiert le lock
- Thread 2 attend
- Thread 1 marque USED et commit
- Thread 2 voit le statut USED → refuse

**Mécanisme 2 : Isolation Transaction**
```properties
spring.jpa.properties.hibernate.connection.isolation=2
# REPEATABLE_READ
```

### Performance

**Optimisations** :
1. Index sur `ticket_id` dans scans
2. Index composite `(status, event_id)` sur tickets
3. Connection pool sized pour charge
4. Query plan optimisé

**Benchmark cible** :
- P50: 50ms
- P95: 150ms
- P99: 200ms

## Transactional Outbox

### Problème

Sans outbox, si Kafka échoue après commit DB → événement perdu.

### Solution : Outbox Pattern

```
Transaction {
    1. UPDATE tickets SET status = 'USED'
    2. INSERT INTO outbox_events (...)
    COMMIT
}

Scheduler (separate) {
    3. SELECT * FROM outbox_events WHERE published = false
    4. PUBLISH to Kafka
    5. UPDATE outbox_events SET published = true
}
```

### Implémentation

**Enregistrement d'événement** :
```java
@Transactional
public void publishTicketUsed(Ticket ticket, TicketScan scan) {
    Map<String, Object> payload = Map.of(
        "ticketId", ticket.getId(),
        "eventId", ticket.getEventId(),
        "usedAt", ticket.getUsedAt()
    );
    
    String payloadJson = objectMapper.writeValueAsString(payload);
    
    OutboxEvent event = OutboxEvent.builder()
        .aggregateType("Ticket")
        .aggregateId(ticket.getId())
        .eventType("ticket.used")
        .payload(payloadJson)
        .published(false)
        .build();
    
    outboxRepository.save(event);
    // Commit avec la transaction principale
}
```

**Publication asynchrone** :
```java
@Scheduled(fixedDelay = 5000)  // Toutes les 5 secondes
@Transactional
public void publishPendingEvents() {
    List<OutboxEvent> unpublished = outboxRepository.findUnpublishedEvents();
    
    for (OutboxEvent event : unpublished) {
        try {
            String topic = "yeyamo.ticket." + event.getEventType();
            kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload());
            
            event.markAsPublished();
            outboxRepository.save(event);
            
        } catch (Exception e) {
            log.error("Failed to publish event", e);
            // Retry on next run
        }
    }
}
```

### Garanties

- **At-least-once delivery** : événements publiés au moins une fois
- **Ordering** : ordre préservé par aggregate ID (Kafka partition key)
- **No data loss** : événements survit aux crashs

## Tests

### 1. Test Concurrent Inventory

```java
@Test
void testConcurrentHoldsDoNotOversell() {
    // 10 tickets disponibles
    // 20 threads essaient d'en prendre 1
    // Résultat attendu : exactement 10 succès
    
    ExecutorService executor = Executors.newFixedThreadPool(20);
    CountDownLatch latch = new CountDownLatch(20);
    
    List<Future<Boolean>> results = new ArrayList<>();
    
    for (int i = 0; i < 20; i++) {
        results.add(executor.submit(() -> {
            latch.countDown();
            latch.await();  // Synchronize start
            
            try {
                inventoryService.createHold(...);
                return true;
            } catch (Exception e) {
                return false;
            }
        }));
    }
    
    long successCount = results.stream()
        .map(f -> f.get())
        .filter(success -> success)
        .count();
    
    assertThat(successCount).isEqualTo(10);
}
```

### 2. Test Double Scan

```java
@Test
void testConcurrentScansOnlyOneSucceeds() {
    // 10 threads scannent le même ticket simultanément
    // Résultat attendu : 1 VALID, 9 ALREADY_USED
    
    AtomicInteger validCount = new AtomicInteger(0);
    AtomicInteger alreadyUsedCount = new AtomicInteger(0);
    
    ExecutorService executor = Executors.newFixedThreadPool(10);
    CountDownLatch latch = new CountDownLatch(10);
    
    for (int i = 0; i < 10; i++) {
        executor.submit(() -> {
            latch.countDown();
            latch.await();
            
            TicketScanResponse response = scanService.scanTicket(scannerId, request);
            
            if (response.getResult() == ScanResult.VALID) {
                validCount.incrementAndGet();
            } else if (response.getResult() == ScanResult.ALREADY_USED) {
                alreadyUsedCount.incrementAndGet();
            }
        });
    }
    
    executor.shutdown();
    executor.awaitTermination(30, TimeUnit.SECONDS);
    
    assertThat(validCount.get()).isEqualTo(1);
    assertThat(alreadyUsedCount.get()).isEqualTo(9);
}
```

### 3. Test QR Falsifié

```java
@Test
void testTamperedQrIsRejected() {
    QrTokenData tokenData = qrTokenService.generateToken(ticketId, eventId, 365);
    
    // Modifier le token
    String tamperedToken = tokenData.token()
        .substring(0, tokenData.token().length() - 10) + "XXXXXXXXXX";
    
    TokenValidationResult result = qrTokenService.validateToken(tamperedToken);
    
    assertThat(result.isValid()).isFalse();
}
```

## Déploiement

### Variables d'Environnement Production

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/ticket_service_db
SPRING_DATASOURCE_USERNAME=${DB_USER}
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=50

# Redis
SPRING_DATA_REDIS_HOST=prod-redis
SPRING_DATA_REDIS_PASSWORD=${REDIS_PASSWORD}

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka-1:9092,kafka-2:9092,kafka-3:9092
SPRING_KAFKA_SECURITY_PROTOCOL=SASL_SSL

# Security
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=${KEYCLOAK_URL}

# Performance
TICKET_SCAN_MAX_CONCURRENT_REQUESTS=2000
```

### Commandes Docker

```bash
# Build
docker build -t yeyamo/ticket-service:1.0.0 .

# Tag
docker tag yeyamo/ticket-service:1.0.0 registry.yeyamo.com/ticket-service:1.0.0

# Push
docker push registry.yeyamo.com/ticket-service:1.0.0

# Run
docker run -d \
  --name ticket-service \
  -p 8093:8093 \
  --env-file .env.production \
  registry.yeyamo.com/ticket-service:1.0.0
```

### Health Checks

```bash
# Liveness
curl http://localhost:8093/actuator/health/liveness

# Readiness
curl http://localhost:8093/actuator/health/readiness

# Metrics
curl http://localhost:8093/actuator/metrics
```

## Conclusion

Le Ticket Service est maintenant opérationnel avec :
- ✅ Gestion d'inventaire sans survente
- ✅ QR codes sécurisés et infalsifiables
- ✅ Validation atomique des scans
- ✅ Tests de concurrence complets
- ✅ Outbox pattern pour événements fiables
- ✅ Documentation complète
- ✅ Prêt pour la production
