# Interaction Service

Service de gestion des interactions utilisateurs pour YeYamo.

## Fonctionnalités

### ❤️ Relations (Likes & Favorites)
- Likes sur posts
- Favoris (saved posts)
- Gestion idempotente via `Idempotency-Key`

### 💬 Commentaires
- Commentaires sur posts
- Réponses imbriquées (threads)
- Soft delete
- Modération (admin/moderator)

### 📤 Partages
- Partages multi-canaux (social, messaging, etc.)

### 📍 Check-ins
- Check-ins géolocalisés sur lieux
- Visibilité publique/privée

### ⭐ Reviews (Avis et Notes)
- Avis utilisateurs sur les lieux (places)
- Note de 1 à 5 étoiles
- Commentaire textuel optionnel
- Un seul avis par utilisateur par lieu
- Modification/suppression par auteur ou modérateur

## Architecture

- **Framework**: Spring Boot 3.x, Java 21
- **Base de données**: PostgreSQL
- **Pattern**: Hexagonal (Ports & Adapters)
- **Events**: Outbox pattern + Kafka
- **Sécurité**: JWT via Spring Security
- **Cache**: Redis (compteurs d'interactions)
- **Idempotence**: Command receipts

## Schéma de Base de Données

### Relations (Likes/Favorites)
```sql
interaction_relations (
  id UUID PRIMARY KEY,
  post_id UUID NOT NULL,
  user_id VARCHAR(100) NOT NULL,
  type VARCHAR(20) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  UNIQUE (post_id, user_id, type)
)
```

### Commentaires
```sql
interaction_comments (
  id UUID PRIMARY KEY,
  post_id UUID NOT NULL,
  parent_id UUID,
  author_id VARCHAR(100) NOT NULL,
  body TEXT,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  deleted_at TIMESTAMPTZ,
  version BIGINT NOT NULL DEFAULT 0
)
```

### Check-ins
```sql
interaction_checkins (
  id UUID PRIMARY KEY,
  catalog_asset_id UUID NOT NULL,
  user_id VARCHAR(100) NOT NULL,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  visible BOOLEAN NOT NULL,
  occurred_at TIMESTAMPTZ NOT NULL
)
```

### Reviews
```sql
reviews (
  id UUID PRIMARY KEY,
  user_id VARCHAR(100) NOT NULL,
  place_id UUID NOT NULL,
  rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
  comment TEXT,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  UNIQUE (user_id, place_id)
)
```

## Endpoints REST

### Likes & Favorites

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `PUT` | `/api/v1/interactions/posts/{postId}/like` | Liker un post |
| `DELETE` | `/api/v1/interactions/posts/{postId}/like` | Retirer un like |
| `PUT` | `/api/v1/interactions/posts/{postId}/favorite` | Sauvegarder un post |
| `DELETE` | `/api/v1/interactions/posts/{postId}/favorite` | Retirer des favoris |

### Commentaires

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/api/v1/interactions/posts/{postId}/comments` | Commenter un post |
| `PUT` | `/api/v1/interactions/comments/{id}` | Modifier son commentaire |
| `DELETE` | `/api/v1/interactions/comments/{id}` | Supprimer commentaire (auteur ou modérateur) |
| `GET` | `/api/v1/interactions/posts/{postId}/comments` | Lire commentaires actifs |

### Partages

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/api/v1/interactions/posts/{postId}/shares` | Partager un post |

### Résumé

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/api/v1/interactions/posts/{postId}/summary` | Compteurs d'interactions (likes, comments, shares) |

### Reviews

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/api/v1/interactions/places/{placeId}/reviews` | Créer un avis (409 si dupliqué) |
| `PUT` | `/api/v1/interactions/reviews/{id}` | Modifier son avis (auteur uniquement) |
| `DELETE` | `/api/v1/interactions/reviews/{id}` | Supprimer avis (auteur ou modérateur) |
| `GET` | `/api/v1/interactions/places/{placeId}/reviews` | Lister avis d'un lieu (paginé) |
| `GET` | `/api/v1/interactions/users/{userId}/reviews` | Lister avis d'un utilisateur (paginé) |

## Événements Kafka

### Topic: `interaction.events`

**Événements Relations:**
- `interaction.like.added`
- `interaction.like.removed`
- `interaction.favorite.added`
- `interaction.favorite.removed`

**Événements Commentaires:**
- `interaction.comment.created`
- `interaction.comment.updated`
- `interaction.comment.deleted`

**Événements Partages:**
- `interaction.post.shared`

**Événements Check-ins:**
- `interaction.checkin.created`

**Événements Reviews:**
- `interaction.review.created`
- `interaction.review.updated`
- `interaction.review.deleted`

## Sécurité

- Tous les endpoints (sauf GET public) protégés par JWT
- Protection IDOR sur modifications (auteur uniquement)
- Modérateurs (ROLE_MODERATOR, ROLE_ADMIN) peuvent supprimer commentaires/reviews
- Idempotence via `Idempotency-Key` header (obligatoire sur mutations)

### Gestion Reviews

| Règle | Implémentation |
|-------|----------------|
| **Un avis par lieu** | UNIQUE constraint (user_id, place_id) |
| **409 Conflict** | Si POST sur lieu déjà reviewé → Invite à utiliser PUT |
| **403 Forbidden** | Si utilisateur B tente de modifier l'avis de A |
| **Suppression modération** | ROLE_MODERATOR/ADMIN peut DELETE n'importe quel avis |

## Configuration

### application.properties
```properties
server.port=8091

spring.datasource.url=jdbc:postgresql://localhost:5432/yeyamo_interactions
spring.datasource.username=postgres
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}

spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
yeyamo.kafka.topics.interaction-events=interaction.events

spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=6379

jwt.secret=${JWT_SECRET}
```

## Idempotence

Toutes les commandes de mutation utilisent un `Idempotency-Key` header obligatoire.

**Principe**: Si la même clé est rejouée, le service renvoie le résultat déjà enregistré sans réexécuter l'opération.

```bash
POST /api/v1/interactions/places/{id}/reviews
Idempotency-Key: key-12345
Authorization: Bearer {JWT}

# Réexécution avec même clé → Renvoie le même résultat
```

## Cache Redis

Les compteurs d'interactions (likes, comments, shares) sont mis en cache pour optimiser les lectures.

**Invalidation**: Cache évincé automatiquement lors de mutations.

## Tests

### Tests unitaires
```bash
mvn test
```

### Coverage
Tests couvrent:
- ✅ Création reviews (nominal, 409 sur doublon, idempotence)
- ✅ Modification reviews (auteur OK, 403 non-auteur, 404 review inexistante)
- ✅ Suppression reviews (auteur OK, modérateur OK, 403 non-autorisé)
- ✅ Requêtes reviews (pagination, limite cap à 100)
- ✅ Relations likes/favorites (idempotence)
- ✅ Commentaires (nested replies, soft delete, modération)

## Déploiement

### Variables d'Environnement

```bash
# Base de données
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/yeyamo_interactions
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<password>

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# JWT
JWT_SECRET=<secret>

# Server
SERVER_PORT=8091
```

### Migrations Flyway

Les migrations s'exécutent automatiquement au démarrage:
1. `V1__create_interaction_schema.sql` — Relations, comments, shares, check-ins, outbox
2. `V2__create_reviews.sql` — ✅ **Reviews**

### Healthcheck

```bash
curl http://localhost:8091/actuator/health
```

## Intégrations

### Content Service
Les `post_id` référencent des posts dans `content-service`.

### Catalog Service
- `catalog_asset_id` (check-ins et reviews) référence des places dans `catalog-service`

### Feed Service
Peut consommer les événements d'interactions pour construire les feeds personnalisés.

### Recommendation Service
Peut consommer les événements reviews pour calculer notes moyennes et recommandations.

## Évolutions Futures

### Reviews
- [ ] Note moyenne par lieu (agrégation côté catalog ou recommendation-service)
- [ ] Filtres sur reviews (par note, date)
- [ ] Modération semi-automatique (détection spam/toxicité)
- [ ] Photos dans reviews (via media-service)
- [ ] Votes utiles ("helpful" reviews)

### Général
- [ ] Reactions emoji sur posts (au-delà de like/favorite)
- [ ] Mentions (@user) dans commentaires
- [ ] Notifications temps réel via WebSocket
