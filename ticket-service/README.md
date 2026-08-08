# YeYamo Ticket Service

Microservice autonome pour la gestion de la billetterie événementielle sur la plateforme YeYamo.

## Fonctionnalités

### Pour les Partenaires
- ✅ Activer la billetterie pour un événement
- ✅ Créer plusieurs types de billets avec prix et quantités
- ✅ Gérer le personnel d'événement (staff)
- ✅ Scanner les QR codes pour contrôler l'accès
- ✅ Consulter les statistiques de vente et d'entrée

### Pour les Utilisateurs
- ✅ Parcourir les types de billets disponibles
- ✅ Créer une commande de billets
- ✅ Payer via integration avec payment-service
- ✅ Recevoir des billets avec QR code sécurisé
- ✅ Afficher et gérer ses billets
- ✅ Recevoir des notifications

## Architecture

### Responsabilités
- Gestion de l'inventaire avec verrouillage distribué (Redis/Redisson)
- Réservations temporaires (holds) avec TTL
- Création et validation de commandes
- Émission de billets avec QR codes signés (JWT RSA)
- Validation atomique des scans (prévention double-entrée)
- Gestion du personnel événementiel
- Publication d'événements via Transactional Outbox Pattern

### Sécurité QR Code
- **Signatures asymétriques** : RS256 (RSA-SHA256)
- **Rotation des clés** : tous les 90 jours
- **Token opaque** : aucune donnée personnelle dans le QR
- **Hash stocké** : SHA-256 du token pour révocation
- **Validation serveur** : vérification signature + statut
- **Protection falsification** : impossible de créer un faux QR

### Concurrence et Intégrité
- **Verrouillage optimiste** : version sur TicketType
- **Verrouillage pessimiste** : PESSIMISTIC_WRITE pour scan
- **Locks distribués** : Redisson pour les opérations d'inventaire
- **Idempotence** : clés d'idempotence sur holds et ordres
- **Atomic operations** : Transaction ACID pour scan

## Technologies

- **Java 21** & **Spring Boot 4.1.0**
- **PostgreSQL** : base de données principale
- **Redis/Redisson** : verrouillage distribué
- **Kafka** : événements asynchrones
- **JWT (JJWT)** : signature des QR codes
- **Flyway** : migrations de schéma
- **Testcontainers** : tests d'intégration

## Prérequis

- Java 21+
- Maven 3.9+
- PostgreSQL 16+
- Redis 7+
- Kafka 3.6+

## Configuration

### Variables d'environnement

```bash
# Database
DB_USERNAME=yeyamo
DB_PASSWORD=your_password

# Redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# OAuth2
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:8081/realms/yeyamo
```

### Base de données

Créer la base de données :
```sql
CREATE DATABASE ticket_service_db;
CREATE USER yeyamo WITH PASSWORD 'yeyamo123';
GRANT ALL PRIVILEGES ON DATABASE ticket_service_db TO yeyamo;
```

## Démarrage

### Développement local

```bash
# Démarrer les dépendances
docker-compose up -d postgres redis kafka

# Lancer le service
cd ticket-service
mvn spring-boot:run
```

### Avec Docker

```bash
# Build
docker build -t yeyamo/ticket-service:latest .

# Run
docker run -p 8093:8093 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/ticket_service_db \
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  yeyamo/ticket-service:latest
```

## Tests

### Tests unitaires et d'intégration
```bash
mvn test
```

### Tests avec Testcontainers
```bash
# Les tests démarrent automatiquement PostgreSQL, Redis, Kafka
mvn verify
```

### Tests de charge sur scan
```bash
docker-compose -f docker-compose.test.yml up
```

## API Documentation

Documentation OpenAPI disponible à : `http://localhost:8093/swagger-ui.html`

### Endpoints principaux

#### Utilisateurs
- `POST /api/v1/tickets/orders` - Créer une commande
- `GET /api/v1/tickets/orders/{orderId}` - Détails commande
- `GET /api/v1/tickets/events/{eventId}` - Mes billets pour un événement
- `GET /api/v1/tickets/{ticketId}/qr` - Obtenir QR code

#### Partenaires
- `POST /api/v1/partner/tickets/configurations` - Configurer billetterie
- `POST /api/v1/partner/tickets/configurations/{id}/activate` - Activer ventes
- `POST /api/v1/partner/tickets/staff` - Assigner personnel
- `POST /api/v1/partner/tickets/scan` - Scanner un billet
- `GET /api/v1/partner/tickets/events/{eventId}/scan-stats` - Statistiques

## Événements Kafka

### Publiés
- `ticket.order.created` - Commande créée
- `ticket.payment.confirmed` - Paiement confirmé
- `ticket.issued` - Billet émis
- `ticket.used` - Billet utilisé (scan)
- `ticket.cancelled` - Billet annulé
- `ticket.refunded` - Billet remboursé

### Consommés
- `payment.confirmed` - Confirmation de paiement
- `payment.failed` - Échec de paiement
- `event.updated` - Mise à jour événement

## Monitoring

### Actuator Endpoints
- `/actuator/health` - Santé du service
- `/actuator/metrics` - Métriques
- `/actuator/prometheus` - Métriques Prometheus

### Métriques clés
- Taux de réussite des scans
- Temps de réponse scan (cible < 200ms)
- Inventaire disponible par événement
- Billets vendus par type
- Tentatives de scan échouées

## Maintenance

### Nettoyage des holds expirés
Automatique toutes les 5 minutes via scheduler.

### Nettoyage des événements outbox
Automatique quotidiennement à 2h du matin.

### Rotation des clés QR
Automatique tous les 90 jours (configurable).

## Troubleshooting

### Le scan est lent
- Vérifier les connexions Redis (locks distribués)
- Vérifier les indexes PostgreSQL
- Augmenter le pool de connexions

### Overselling (survente)
- Vérifier que Redis fonctionne (locks)
- Vérifier les versions optimistes (TicketType.version)
- Consulter les logs de conflits

### QR invalide
- Vérifier la rotation des clés
- Vérifier que le token n'est pas expiré
- Vérifier que le credential n'est pas révoqué

## Sécurité

### Checklist production
- [ ] Changer les mots de passe par défaut
- [ ] Activer SSL/TLS pour PostgreSQL
- [ ] Activer SSL/TLS pour Redis
- [ ] Configurer Kafka SASL
- [ ] Limiter les accès réseau (firewall)
- [ ] Activer audit logging
- [ ] Configurer backup automatique
- [ ] Tester la procédure de restauration
- [ ] Configurer alertes monitoring
- [ ] Review des clés de signature

## Licence

Propriétaire - YeYamo © 2024
