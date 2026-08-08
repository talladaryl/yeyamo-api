# Ticket Service - Guide de Déploiement

## Prérequis

- Docker 24+ et Docker Compose
- Java 21+ (pour build local)
- Maven 3.9+ (pour build local)
- PostgreSQL 16+
- Redis 7+
- Kafka 3.6+

## Quick Start avec Docker Compose

### 1. Démarrer l'Infrastructure

```bash
# Depuis la racine du projet yeyamo-api
cd ticket-service

# Démarrer PostgreSQL, Redis, Kafka
docker-compose up -d postgres redis kafka
```

### 2. Vérifier que l'infrastructure est prête

```bash
# PostgreSQL
docker exec -it ticket-service-postgres psql -U yeyamo -d ticket_service_db -c "SELECT 1;"

# Redis
docker exec -it ticket-service-redis redis-cli ping

# Kafka
docker exec -it ticket-service-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### 3. Lancer le Service

**Option A : Avec Docker (recommandé)**
```bash
# Build l'image
docker build -t yeyamo/ticket-service:latest -f Dockerfile ..

# Lancer le service
docker run -d \
  --name ticket-service \
  --network yeyamo-network \
  -p 8093:8093 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/ticket_service_db \
  -e SPRING_DATASOURCE_USERNAME=yeyamo \
  -e SPRING_DATASOURCE_PASSWORD=yeyamo123 \
  -e SPRING_DATA_REDIS_HOST=redis \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092 \
  yeyamo/ticket-service:latest
```

**Option B : Build et Run avec Maven**
```bash
# Build
mvn clean package -DskipTests

# Run
java -jar target/ticket-service-0.0.1-SNAPSHOT.jar
```

**Option C : Development avec hot reload**
```bash
# Utilise Spring Boot DevTools
mvn spring-boot:run
```

## Docker Compose Complet

Créer un fichier `docker-compose.yml` à la racine :

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: ticket-service-postgres
    environment:
      POSTGRES_DB: ticket_service_db
      POSTGRES_USER: yeyamo
      POSTGRES_PASSWORD: yeyamo123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U yeyamo"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: ticket-service-redis
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    container_name: ticket-service-kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: 'CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT'
      KAFKA_ADVERTISED_LISTENERS: 'PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092'
      KAFKA_PROCESS_ROLES: 'broker,controller'
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_CONTROLLER_QUORUM_VOTERS: '1@kafka:29093'
      KAFKA_LISTENERS: 'PLAINTEXT://kafka:29092,CONTROLLER://kafka:29093,PLAINTEXT_HOST://0.0.0.0:9092'
      KAFKA_INTER_BROKER_LISTENER_NAME: 'PLAINTEXT'
      KAFKA_CONTROLLER_LISTENER_NAMES: 'CONTROLLER'
      CLUSTER_ID: 'MkU3OEVBNTcwNTJENDM2Qk'
    healthcheck:
      test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:9092"]
      interval: 10s
      timeout: 10s
      retries: 5

  ticket-service:
    build:
      context: ..
      dockerfile: ticket-service/Dockerfile
    container_name: ticket-service
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      kafka:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/ticket_service_db
      SPRING_DATASOURCE_USERNAME: yeyamo
      SPRING_DATASOURCE_PASSWORD: yeyamo123
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    ports:
      - "8093:8093"
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8093/actuator/health"]
      interval: 30s
      timeout: 3s
      start_period: 40s
      retries: 3

volumes:
  postgres_data:

networks:
  default:
    name: yeyamo-network
```

Démarrer tout :
```bash
docker-compose up -d
```

Voir les logs :
```bash
docker-compose logs -f ticket-service
```

Arrêter tout :
```bash
docker-compose down
```

## Tests

### Tests Unitaires et Intégration

```bash
# Tous les tests
mvn test

# Tests spécifiques
mvn test -Dtest=TicketInventoryConcurrencyTest
mvn test -Dtest=TicketScanDoubleScanTest
mvn test -Dtest=QrTokenSecurityTest
```

### Tests avec Testcontainers

Les tests utilisent automatiquement Testcontainers pour PostgreSQL, Redis, et Kafka :

```bash
# Les conteneurs sont créés et détruits automatiquement
mvn verify
```

### Tests de Charge

Avec Docker Compose test :

```bash
# Démarrer l'environnement de test
docker-compose -f docker-compose.test.yml up -d

# Lancer JMeter ou Gatling
jmeter -n -t load-test-plan.jmx -l results.jtl

# Ou avec Gatling
mvn gatling:test
```

## Vérifications Post-Déploiement

### 1. Health Check

```bash
curl http://localhost:8093/actuator/health

# Réponse attendue:
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"},
    "kafka": {"status": "UP"}
  }
}
```

### 2. API Documentation

Ouvrir dans un navigateur :
```
http://localhost:8093/swagger-ui.html
```

### 3. Metrics

```bash
curl http://localhost:8093/actuator/metrics

# Métrique spécifique
curl http://localhost:8093/actuator/metrics/jvm.memory.used
```

### 4. Test Création Configuration

```bash
curl -X POST http://localhost:8093/api/v1/partner/tickets/configurations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "eventId": "event-123",
    "salesStartAt": "2024-06-01T00:00:00Z",
    "salesEndAt": "2024-06-30T23:59:59Z",
    "maxTicketsPerBuyer": 10,
    "currency": "XOF",
    "ticketTypes": [{
      "code": "VIP",
      "name": "VIP Ticket",
      "description": "Access to VIP area",
      "price": 50000,
      "quantityTotal": 100,
      "accessZone": "VIP Lounge"
    }]
  }'
```

## Production

### Variables d'Environnement Recommandées

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db.internal:5432/ticket_service_db
SPRING_DATASOURCE_USERNAME=${DB_USER}
SPRING_DATASOURCE_PASSWORD=${DB_PASS}
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=50

# Redis (avec password)
SPRING_DATA_REDIS_HOST=prod-redis.internal
SPRING_DATA_REDIS_PASSWORD=${REDIS_PASSWORD}
SPRING_DATA_REDIS_SSL=true

# Kafka (avec SSL/SASL)
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka-1:9093,kafka-2:9093,kafka-3:9093
SPRING_KAFKA_SECURITY_PROTOCOL=SASL_SSL
SPRING_KAFKA_SASL_MECHANISM=SCRAM-SHA-512
SPRING_KAFKA_SASL_JAAS_CONFIG=${KAFKA_JAAS}

# OAuth2
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=${KEYCLOAK_URL}

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_YEYAMO=INFO

# JVM Options
JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC
```

### Scaling Horizontal

```bash
# Kubernetes
kubectl scale deployment ticket-service --replicas=5

# Docker Swarm
docker service scale yeyamo_ticket-service=5
```

### Backup Base de Données

```bash
# Backup
pg_dump -h postgres -U yeyamo ticket_service_db > ticket_service_backup.sql

# Restore
psql -h postgres -U yeyamo ticket_service_db < ticket_service_backup.sql
```

### Monitoring

**Prometheus** :
```yaml
scrape_configs:
  - job_name: 'ticket-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['ticket-service:8093']
```

**Grafana Dashboard** :
- Import dashboard ID: Custom (créer avec métriques)
- Panels recommandés :
  - Request rate
  - Error rate
  - Response time (P50, P95, P99)
  - Active scans
  - Inventory levels
  - QR validation success rate

## Troubleshooting

### Service ne démarre pas

```bash
# Vérifier les logs
docker logs ticket-service

# Vérifier la connectivité DB
docker exec -it ticket-service-postgres psql -U yeyamo -d ticket_service_db -c "\dt"

# Vérifier Redis
docker exec -it ticket-service-redis redis-cli ping

# Vérifier Kafka
docker exec -it ticket-service-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Scans lents

```bash
# Vérifier Redis latency
redis-cli --latency

# Vérifier les connexions DB
SELECT count(*) FROM pg_stat_activity WHERE datname = 'ticket_service_db';

# Vérifier les locks
SELECT * FROM pg_locks WHERE NOT granted;
```

### Overselling détecté

```bash
# Vérifier l'inventaire
SELECT id, code, quantity_total, quantity_reserved, quantity_sold, version 
FROM ticket_types 
WHERE id = 'TICKET_TYPE_ID';

# Vérifier les holds
SELECT count(*), status FROM ticket_holds GROUP BY status;

# Vérifier Redis
redis-cli KEYS "ticket:inventory:*"
```

### QR validation échoue

```bash
# Vérifier les clés de signature
SELECT * FROM qr_signing_keys WHERE expires_at > NOW();

# Vérifier les credentials
SELECT ticket_id, revoked_at FROM ticket_qr_credentials WHERE ticket_id = 'TICKET_ID';

# Vérifier les scans récents
SELECT * FROM ticket_scans WHERE ticket_id = 'TICKET_ID' ORDER BY scanned_at DESC LIMIT 10;
```

## Commandes Utiles

```bash
# Rebuild from scratch
docker-compose down -v
docker-compose build --no-cache
docker-compose up -d

# Voir les stats Redis
docker exec -it ticket-service-redis redis-cli INFO stats

# Voir les topics Kafka
docker exec -it ticket-service-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Consumer group lag
docker exec -it ticket-service-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group ticket-service \
  --describe

# Database migrations status
docker exec -it ticket-service-postgres psql -U yeyamo -d ticket_service_db \
  -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"

# Restart service only
docker-compose restart ticket-service

# View real-time logs
docker-compose logs -f --tail=100 ticket-service
```

## Support

Pour toute question ou problème :
- Email: tech@yeyamo.com
- Documentation: https://docs.yeyamo.com
- Issues: https://github.com/yeyamo/yeyamo-api/issues

---

**Version**: 1.0.0  
**Last Updated**: 2024  
**Status**: Production Ready ✅
