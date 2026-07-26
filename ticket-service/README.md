# Ticket Service

Microservice autonome gérant la billetterie des événements YeYamo.

## Responsabilités

### Gestion Inventaire
- ✅ Création et configuration des ventes de billets
- ✅ Types de billets avec prix et quantités
- ✅ Réservation temporaire (holds) avec TTL
- ✅ Verrouillage optimiste pour éviter survente
- ✅ Libération automatique des holds expirés

### Gestion Commandes
- ✅ Création de commandes avec idempotence
- ✅ Intégration paiement (payment-service)
- ✅ Émission de billets après paiement confirmé
- ✅ Compensation en cas d'échec
- ✅ Remboursements métier

### QR Code Sécurisé
- ✅ Génération de tokens signés (asymétrique)
- ✅ Rotation des clés (keyId)
- ✅ Expiration et révocation
- ✅ Protection falsification
- ✅ Validation côté serveur uniquement

### Contrôle d'Accès
- ✅ Scan de billets avec validation atomique
- ✅ Détection double-scan
- ✅ Gestion du staff événement
- ✅ Logs d'entrée pour analytics
- ✅ Statistiques de présence

## Architecture

```
ticket-service/
├── domain/
│   ├── model/                  # Entités métier
│   ├── service/                # Logique métier
│   └── port/                   # Interfaces (hexagonal)
├── application/
│   ├── TicketSaleService       # Configuration ventes
│   ├── TicketOrderService      # Gestion commandes
│   ├── TicketIssuanceService   # Émission billets
│   ├── QrTokenService          # Génération QR sécurisés
│   ├── ScanService             # Validation check-in
│   └── StaffManagementService  # Gestion staff
├── infrastructure/
│   ├── persistence/            # JPA repositories
│   ├── messaging/              # Kafka consumers/producers
│   ├── outbox/                 # Transactional outbox
│   ├── crypto/                 # JWT signing, key rotation
│   └── client/                 # Clients externes
└── interfaces/
    └── rest/                   # REST controllers
```

## Entités Principales

### TicketSaleConfiguration
Configuration globale des ventes pour un événement.

### TicketType
Type de billet avec prix, quantité, zones d'accès.
- **Verrouillage optimiste** (`@Version`) pour éviter survente

### TicketHold
Réservation temporaire (panier) avec expiration automatique.
- TTL configurable (défaut: 10 minutes)
- Libération automatique via scheduled job

### TicketOrder
Commande de billets avec statut paiement.
- Idempotence garantie via `idempotencyKey`

### Ticket
Billet émis avec statut et serial number unique.

### TicketQrCredential
Credentials QR sécurisés (JWT signé).
- Stockage du hash du token
- Révocation possible

### TicketScan
Log de chaque scan (succès ou échec).

### EventStaffAssignment
Assignment du staff autorisé à scanner.

## Règles Métier Critiques

### Inventaire
1. **Pas de survente**: `quantity_sold <= quantity_total`
2. **Réservation atomique**: Transaction + optimistic locking
3. **Hold avec TTL**: Expiration automatique après 10min
4. **Compensation**: Libération en cas d'échec paiement

### QR Token (JWT)
```json
{
  "jti": "unique-token-id",
  "sub": "ticket-id",
  "iss": "ticket-service",
  "exp": 1234567890,
  "kid": "key-2024-01",
  "evt": "event-123",
  "typ": "TICKET_QR"
}
```

**Validation**:
1. Vérifier signature avec clé publique (kid)
2. Vérifier expiration
3. Vérifier révocation (table ticket_qr_credentials)
4. Vérifier ticket status = VALID
5. Vérifier event_id match
6. **Transaction atomique**: Update ticket.status = USED

### Scan Atomique
```sql
UPDATE tickets 
SET status = 'USED', used_at = NOW(), version = version + 1
WHERE id = ? AND status = 'VALID' AND version = ?
```

Si `rowsAffected == 0` → déjà utilisé ou invalide.

## Intégrations

### Event Service
- **API interne**: `GET /internal/events/{id}` (vérification existence)
- **Kafka**: Consomme `event.published`, `event.cancelled`

### Payment Service
- **Publish**: `payment.requested` (avec order details)
- **Consume**: `payment.confirmed`, `payment.failed`

### Notification Service
- **Publish**: `ticket.issued`, `ticket.cancelled`, `ticket.reminder`

### Analytics Service
- **Publish**: `ticket.sold`, `ticket.scanned`, `revenue.recorded`

## APIs Principales

### Partner APIs

```
POST   /api/v1/tickets/sales/configure
GET    /api/v1/tickets/sales/{eventId}
POST   /api/v1/tickets/sales/{eventId}/types
PUT    /api/v1/tickets/sales/types/{id}

POST   /api/v1/tickets/staff
GET    /api/v1/tickets/staff/event/{eventId}
DELETE /api/v1/tickets/staff/{id}

GET    /api/v1/tickets/orders/event/{eventId}
GET    /api/v1/tickets/analytics/event/{eventId}
```

### User APIs

```
POST   /api/v1/tickets/hold
POST   /api/v1/tickets/orders
GET    /api/v1/tickets/orders/{id}
GET    /api/v1/tickets/my-tickets

GET    /api/v1/tickets/{id}/qr
```

### Scanner APIs

```
POST   /api/v1/tickets/scan
GET    /api/v1/tickets/scans/event/{eventId}
GET    /api/v1/tickets/scans/stats/{eventId}
```

## Tests

### Tests Unitaires
- Service layer avec mocks
- Validation business rules

### Tests d'Intégration (Testcontainers)
- PostgreSQL container
- Test transactions concurrentes
- Test double scan
- Test QR falsifié
- Test hold expiration

### Tests de Charge
- JMeter ou Gatling
- Scénarios:
  - Achat concurrent (100 users, 10 tickets restants)
  - Scan concurrent (même ticket scanné 2x simultanément)

## Sécurité

### QR Token Keys
- **RSA 2048** pour signature
- Clés stockées dans KeyStore ou Vault
- Rotation tous les 90 jours
- Multiple keys actives (old + new)

### Staff Authentication
- JWT utilisateur vérifié
- Scope: `ticket:scan`
- Vérification assignment dans `event_staff_assignments`

### PII Protection
- QR ne contient **aucune donnée personnelle**
- Uniquement token opaque signé
- Données retrieved côté serveur après validation

## Monitoring

### Métriques
```
ticket_sales_total{event_id, ticket_type}
ticket_holds_active
ticket_holds_expired_total
ticket_scans_total{result, event_id}
ticket_qr_generation_duration
ticket_scan_validation_duration
```

### Alerts
- Survente détectée (should never happen)
- Scan failure rate > 5%
- Hold expiration rate > 50%
- QR validation failures

## Configuration

### Environnement
```properties
# Database
spring.datasource.url=jdbc:postgresql://postgres:5432/ticket_db

# Kafka
spring.kafka.bootstrap-servers=kafka:9092

# Business Rules
yeyamo.tickets.hold-ttl-minutes=10
yeyamo.tickets.max-per-buyer=10
yeyamo.tickets.service-fee-percent=5

# QR Security
yeyamo.tickets.qr.key-rotation-days=90
yeyamo.tickets.qr.token-ttl-days=30
yeyamo.tickets.qr.algorithm=RS256

# Scheduler
yeyamo.tickets.scheduler.release-holds-cron=0 */5 * * * *
```

## Déploiement

```bash
# Build
mvn clean package

# Docker
docker build -t yeyamo/ticket-service:latest .

# Run
docker-compose up -d ticket-service
```

## Future Enhancements (Phase 2)

- [ ] Mode offline scan avec sync
- [ ] Transfert de billets (peer-to-peer)
- [ ] Upgrade de billets
- [ ] Attente list (waitlist)
- [ ] Dynamic pricing
- [ ] Promotions et discount codes
- [ ] Group bookings
- [ ] Season passes
- [ ] Recurring events support
