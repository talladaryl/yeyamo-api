# Content Service

Service de gestion du contenu utilisateur pour YeYamo.

## Fonctionnalités

### 📝 Posts
- Création et gestion de posts
- Association avec des lieux (places)
- Publication d'événements sur Kafka (`content.events`)

### 📸 Stories (Contenus Éphémères)
- Stories éphémères (24h)
- Vues trackées par utilisateur
- Feed personnalisé basé sur les abonnements
- Expiration automatique via scheduler

## Architecture

- **Framework**: Spring Boot 3.x, Java 21
- **Base de données**: PostgreSQL
- **Pattern**: Hexagonal (Ports & Adapters)
- **Events**: Outbox pattern + Kafka
- **Sécurité**: JWT via Spring Security

## Schéma de Base de Données

### Posts
```sql
content_posts (
  id UUID PRIMARY KEY,
  author_id VARCHAR(255) NOT NULL,
  content TEXT,
  place_id UUID,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
)
```

### Stories
```sql
stories (
  id UUID PRIMARY KEY,
  author_id VARCHAR(255) NOT NULL,
  media_id UUID NOT NULL,
  caption VARCHAR(500),
  duration_seconds INT NOT NULL DEFAULT 15,
  created_at TIMESTAMPTZ NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  deleted_at TIMESTAMPTZ
)

story_views (
  story_id UUID NOT NULL REFERENCES stories(id) ON DELETE CASCADE,
  viewer_id VARCHAR(255) NOT NULL,
  viewed_at TIMESTAMPTZ NOT NULL,
  PRIMARY KEY (story_id, viewer_id)
)
```

## Endpoints REST

### Stories

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/api/v1/stories` | Liste des stories actives des utilisateurs suivis |
| `GET` | `/api/v1/stories/{id}` | Détail d'une story |
| `POST` | `/api/v1/stories/{id}/view` | Enregistrer une vue (idempotent) |
| `POST` | `/api/v1/stories` | Créer une story |
| `DELETE` | `/api/v1/stories/{id}` | Supprimer sa story (soft delete) |

### Posts
Voir `PostController.java` pour la liste complète des endpoints.

## Événements Kafka

### Topic: `content.events`

**Événements Posts:**
- `content.post.created`
- `content.post.updated`
- `content.post.deleted`

**Événements Stories:**
- `content.story.created`
- `content.story.viewed`
- `content.story.deleted`

## Sécurité

- Tous les endpoints protégés par JWT (`@PreAuthorize("isAuthenticated()")`)
- Protection IDOR: vérification propriétaire sur mutations
- Stories privées: 404 (pas 403) pour éviter la fuite d'information

## Configuration

### application.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/content_db
    username: content_user
    password: ${DB_PASSWORD}
  
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

content:
  story:
    expiration-cron: "0 0 * * * *" # Toutes les heures
    
userservice:
  base-url: ${USER_SERVICE_URL:http://localhost:8082}
```

## Intégrations

### User Service
Le service appelle `user-service` pour obtenir la liste des utilisateurs suivis:
```
GET /api/v1/users/social/following
```

### Media Service
Les `mediaId` référencent des médias stockés dans `media-service`.

## Tâches Planifiées

### StoryExpirationScheduler
- **Fréquence**: configurable via `content.story.expiration-cron` (défaut: toutes les heures)
- **Action**: Marque les stories expirées avec `deleted_at`
- **Log**: Nombre de stories marquées à chaque exécution

## Déploiement

1. Variables d'environnement requises:
```bash
DB_PASSWORD=<password>
KAFKA_BROKERS=<kafka-servers>
USER_SERVICE_URL=http://user-service:8082
JWT_SECRET=<secret>
```

2. Migrations Flyway appliquées automatiquement au démarrage

3. Port par défaut: `8083`

## Tests

### Tests unitaires
```bash
mvn test
```

### Coverage
Tests couvrent:
- ✅ Création de stories
- ✅ Récupération feed personnalisé
- ✅ Enregistrement de vues (idempotence)
- ✅ Protection IDOR sur suppression
- ✅ Expiration automatique
- ✅ Stories expirées retournent 404

## Évolutions Futures

- Réactions sur stories (👍 ❤️ 😂)
- Réponses privées aux stories
- Analytics sur taux de complétion des vues
- Highlights (stories permanentes sélectionnées)
