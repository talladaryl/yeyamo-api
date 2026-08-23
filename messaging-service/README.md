# Messaging Service

Service de messagerie temps réel pour YeYamo (conversations privées et groupes).

## Fonctionnalités

### 💬 Conversations
- Conversations directes (2 participants)
- Groupes (jusqu'à 100 membres)
- Création, gestion membres, métadonnées

### ✉️ Messages
- Envoi messages texte
- Pièces jointes (jusqu'à 10 par message)
- Édition (fenêtre de 15 minutes)
- Suppression logique
- Réponses (reply-to)
- Idempotence client (via `client_message_id`)

### 🔔 Temps Réel (WebSocket)
- Notifications instantanées via STOMP/WebSocket
- Endpoint: `/ws/messaging`
- Authentification JWT obligatoire
- **Autorisation conversation-level** (membre actif uniquement)
- Rate limiting anti-abus
- Heartbeat et timeout inactivité

## Architecture

- **Framework**: Spring Boot 3.x / 4.x, Java 21
- **Base de données**: PostgreSQL 16+ (Spring Data JPA + Flyway)
- **Temps réel**: WebSocket + STOMP
- **Events**: Kafka (`messaging.events`)
- **Sécurité**: JWT + IDOR protection + Rate limiting

## Schéma PostgreSQL

### Tables Principales

```sql
-- Conversations
CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(120),
    owner_id VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_message_id UUID,
    last_message_preview TEXT,
    last_message_at TIMESTAMP WITH TIME ZONE
);

-- Membres
CREATE TABLE conversation_members (
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id VARCHAR(120) NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    left_at TIMESTAMP WITH TIME ZONE,
    last_read_message_id UUID,
    last_read_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (conversation_id, user_id)
);

-- Messages
CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id VARCHAR(120) NOT NULL,
    client_message_id VARCHAR(120) NOT NULL,
    message_type VARCHAR(30) NOT NULL,
    body TEXT,
    reply_to_message_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL,
    edited_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Pièces jointes
CREATE TABLE message_attachments (
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    attachment_id UUID NOT NULL,
    position INTEGER NOT NULL,
    PRIMARY KEY (message_id, position)
);

-- Idempotence
CREATE TABLE message_idempotency (
    sender_id VARCHAR(120) NOT NULL,
    client_message_id VARCHAR(120) NOT NULL,
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (sender_id, client_message_id)
);
```

## Endpoints REST

### Conversations

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/api/v1/messaging/direct` | Créer conversation directe |
| `POST` | `/api/v1/messaging/groups` | Créer groupe |
| `GET` | `/api/v1/messaging/conversations` | Lister mes conversations |
| `GET` | `/api/v1/messaging/conversations/{id}` | Détail conversation |
| `PUT` | `/api/v1/messaging/conversations/{id}` | Modifier métadonnées |
| `DELETE` | `/api/v1/messaging/conversations/{id}` | Quitter conversation |

### Membres

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/api/v1/messaging/conversations/{id}/members` | Ajouter membre |
| `DELETE` | `/api/v1/messaging/conversations/{id}/members/{userId}` | Retirer membre |
| `PUT` | `/api/v1/messaging/conversations/{id}/members/{userId}/role` | Promouvoir membre |

### Messages

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/api/v1/messaging/conversations/{id}/messages` | Envoyer message |
| `GET` | `/api/v1/messaging/conversations/{id}/messages` | Lire messages (paginé) |
| `PUT` | `/api/v1/messaging/messages/{id}` | Éditer message (< 15min) |
| `DELETE` | `/api/v1/messaging/messages/{id}` | Supprimer message |
| `PUT` | `/api/v1/messaging/conversations/{id}/read` | Marquer comme lu |

## WebSocket (Temps Réel)

### Connexion

**Endpoint**: `ws://localhost:8104/ws/messaging`

**Protocole**: STOMP over WebSocket

**Authentification**: Header JWT dans CONNECT
```javascript
const socket = new SockJS('/ws/messaging');
const stompClient = Stomp.over(socket);

stompClient.connect(
  { 'Authorization': 'Bearer ' + jwtToken },
  (frame) => {
    console.log('Connected');
    
    // Souscrire aux notifications
    stompClient.subscribe('/user/queue/messaging', (message) => {
      const payload = JSON.parse(message.body);
      console.log('New message:', payload);
    });
  }
);
```

### Sécurité WebSocket

#### 1. Authentification JWT (CONNECT)
- JWT obligatoire dans header `Authorization: Bearer {token}`
- Validation signature et expiration
- Extraction `userId` et `roles`

#### 2. Autorisation par Conversation (SUBSCRIBE) ✅ **CORRIGÉ**
- Vérification appartenance conversation via PostgreSQL
- Rejet si utilisateur non-membre ou statut != `ACTIVE`
- Message d'erreur générique (`Unauthorized`) sans fuite d'information

**Implémentation** (depuis correctif 2026-07-17):
```java
// Vérifier que user_id est membre ACTIF de conversation_id
Optional<ConversationMemberEntity> member = 
    memberRepository.findByConversationIdAndUserId(conversationId, userId);

if (!member.isPresent() || member.get().getStatus() != MemberStatus.ACTIVE) {
    throw new MessagingException("Unauthorized");
}
```

#### 3. Rate Limiting ✅ **CORRIGÉ**
- Limite: 10 souscriptions par minute par utilisateur (configurable)
- Protection contre énumération d'ID de conversation
- Utilise Guava `RateLimiter`

**Configuration**:
```properties
websocket.rate-limit.subscribe-per-minute=10
```

#### 4. Heartbeat et Timeout ✅ **CORRIGÉ**
- Heartbeat client/server: 10 secondes (configurable)
- Timeout inactivité: 5 minutes (configurable)
- Fermeture automatique connexions zombies

**Configuration**:
```properties
websocket.heartbeat.client-ms=10000
websocket.heartbeat.server-ms=10000
websocket.idle-timeout-ms=300000
```

### Événements Temps Réel

Les utilisateurs reçoivent des notifications sur `/user/queue/messaging` pour:
- `messaging.message.sent` — Nouveau message dans une conversation
- `messaging.message.edited` — Message modifié
- `messaging.message.deleted` — Message supprimé
- `messaging.conversation.updated` — Métadonnées conversation modifiées
- `messaging.member.added` — Nouveau membre ajouté
- `messaging.member.removed` — Membre retiré

**Format payload**:
```json
{
  "eventType": "messaging.message.sent",
  "conversationId": "uuid",
  "messageId": "uuid",
  "senderId": "user-123",
  "body": "Hello!",
  "sentAt": "2026-07-17T14:30:00Z"
}
```

## Sécurité

### REST API
- Tous endpoints protégés par JWT (`@PreAuthorize("isAuthenticated()")`)
- Protection IDOR: vérification appartenance conversation sur toutes mutations
- Validation Jakarta Bean Validation

### WebSocket
- ✅ Authentification JWT au CONNECT
- ✅ Autorisation conversation-level au SUBSCRIBE (correctif 2026-07-17)
- ✅ Rate limiting (10 tentatives/minute)
- ✅ Timeout inactivité (5 minutes)
- ✅ Messages d'erreur génériques (pas de fuite d'information)

### Audit de Sécurité

**Failles corrigées (2026-07-17)**:
1. ❌ → ✅ **Absence d'autorisation conversation-level** (CRITIQUE)
2. ❌ → ✅ **Énumération d'ID de conversation** (MOYEN)
3. ❌ → ✅ **Absence de rate limiting** (MOYEN)
4. ❌ → ✅ **Absence de timeout inactivité** (FAIBLE)

Voir `docs/SECURITY_AUDIT_REPORT.md` pour détails.

## Configuration

### application.properties (via Config Server)
```properties
server.port=8104

spring.datasource.url=jdbc:postgresql://localhost:5432/yeyamo_messaging
spring.datasource.username=postgres
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.flyway.enabled=true

spring.kafka.bootstrap-servers=localhost:9092
yeyamo.kafka.topics.messaging-events=messaging.events

jwt.secret=${JWT_SECRET}

# Limites métier
messaging.direct.max-members=2
messaging.group.max-members=100
messaging.message.max-length=4000
messaging.message.max-attachments=10
messaging.message.edit-window-minutes=15

# WebSocket Security
websocket.rate-limit.subscribe-per-minute=10
websocket.heartbeat.client-ms=10000
websocket.heartbeat.server-ms=10000
websocket.idle-timeout-ms=300000
```

## Déploiement

### Variables d'Environnement

```bash
# PostgreSQL
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/yeyamo_messaging
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# JWT
JWT_SECRET=<secret>

# Server
SERVER_PORT=8104

# WebSocket (optionnel, valeurs par défaut suffisantes)
WEBSOCKET_RATE_LIMIT_SUBSCRIBE=10
WEBSOCKET_HEARTBEAT_CLIENT=10000
WEBSOCKET_HEARTBEAT_SERVER=10000
WEBSOCKET_IDLE_TIMEOUT=300000
```

### Schéma & Migrations Flyway

Le schéma relationnel est géré automatiquement par Flyway au démarrage via les scripts `src/main/resources/db/migration/`.
```bash
spring.flyway.enabled=true
```

### Healthcheck

```bash
curl http://localhost:8104/actuator/health
```

## Tests

### Tests unitaires
```bash
mvn test
```

### Coverage
Tests couvrent:
- ✅ Autorisation WebSocket (membre actif OK, non-membre KO, LEFT/REMOVED KO)
- ✅ Rate limiting (tentatives multiples, isolation users)
- ✅ Protection fuite d'information (messages erreur génériques)
- ✅ CRUD conversations et messages
- ✅ Idempotence messages

## Intégrations

### User Service
Les `userId` référencent des utilisateurs dans `user-service`.

### Media Service
Les `attachment_ids` référencent des médias dans `media-service`.

### Notification Service
Peut consommer les événements `messaging.*` pour notifications push.

## Évolutions Futures

- [ ] Réactions emoji sur messages
- [ ] Messages vocaux
- [ ] Appels audio/vidéo (WebRTC)
- [ ] Chiffrement end-to-end (E2EE)
- [ ] Messages éphémères (auto-destruction)
- [ ] Recherche full-text messages (Elasticsearch)
- [ ] Archivage conversations (retention policy)

## Troubleshooting

### WebSocket ne se connecte pas
- Vérifier que le JWT est valide et non expiré
- Vérifier le header `Authorization: Bearer {token}` dans CONNECT
- Vérifier les CORS (`setAllowedOriginPatterns("*")`)

### Utilisateur ne reçoit pas les messages
- Vérifier qu'il est membre ACTIF de la conversation
- Vérifier qu'il est souscrit à `/user/queue/messaging`
- Vérifier que le WebSocket n'a pas timeout (heartbeat)

### Rate limit dépassé
- Attendre 1 minute avant de réessayer
- Ajuster `websocket.rate-limit.subscribe-per-minute` si nécessaire

### Connexion fermée pour inactivité
- Implémenter heartbeat côté client (ping toutes les 10s)
- Ajuster `websocket.idle-timeout-ms` si besoin de timeout plus long
