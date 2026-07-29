# Audit final du backend YeYamo pour `yeyamo-admin`

Date : 29 juillet 2026

## 1. Architecture finale

Monorepo Maven Java 21, Spring Boot 4.1/Spring Cloud 2025.1.2, organisé en microservices DDD/hexagonaux selon maturité du module. L'accès externe passe par Spring Cloud Gateway. Eureka assure la découverte, Config Server centralise la configuration non secrète, PostgreSQL/PostGIS porte les transactions, Redis les caches/rate limits, Cassandra certains journaux, OpenSearch la recherche et Kafka/Redpanda les événements.

Les producteurs critiques utilisent une Transactional Outbox. Les consumers critiques utilisent une inbox persistante par `eventId`, retry borné et DLT. `event-contracts` fournit l'enveloppe versionnée commune.

## 2. État par capacité

### Auth / Security

| Capacité | État | Observation |
|---|---|---|
| login | COMPLETE | password + OAuth, rate limit/Turnstile adaptatif |
| logout | COMPLETE | révocation serveur |
| refresh | COMPLETE | rotation et détection de réutilisation |
| roles | COMPLETE | six rôles admin normalisés |
| permissions | COMPLETE | claims et authorities |
| scopes | COMPLETE | claims `scope/scopes`, campaign couvert |
| JWT | COMPLETE | issuer/audience/secret stricts selon services durcis |
| session revoke | COMPLETE | compte courant et révocation admin plateforme |

### Admin

| Capacité | État | Observation |
|---|---|---|
| platform users | COMPLETE | liste, détail, statut, rôles, sessions, export |
| administrators | COMPLETE | CRUD RBAC et protections dernier super-admin |
| audit logs | COMPLETE | audit central paginé + filtres |

### Partners

| Capacité | État | Observation |
|---|---|---|
| list | PARTIAL | endpoint admin paginé ; filtres KYC/région/ville/catégorie non projetés |
| detail | COMPLETE | vue agrégée limitée |
| KYC | COMPLETE | documents streamés de manière sécurisée |
| validation | COMPLETE | acteur JWT et décisions structurées |
| history | COMPLETE | historique immuable |
| finance | COMPLETE | commerce ledger par partnerId |

La projection des filtres géographiques/KYC nécessite des événements Place/KYC supplémentaires ; elle n'est pas simulée par des appels N+1.

### Places

Toutes les capacités `places`, `regions`, `cities`, `districts`, `categories` et `validation` sont **COMPLETE**. Les coordonnées et cohérences hiérarchiques sont validées. PostGIS reste la source de vérité.

### Catalog

| Capacité | État |
|---|---|
| assets | COMPLETE |
| culture classification | COMPLETE via type/category/tags |
| collections | COMPLETE |
| imports | COMPLETE |
| import errors | COMPLETE |
| retry/cancel | COMPLETE |

### Events

| Capacité | État | Observation |
|---|---|---|
| global admin list | COMPLETE | `/api/v1/admin/events` paginé |
| detail | COMPLETE | vue admin |
| status/publication | COMPLETE | machine d'état |
| ticketing | COMPLETE | ticket types et stats |
| participants | COMPLETE | ticket-service |
| scanner/check-in | COMPLETE | token opaque, anti-replay, acteur authentifié |

### Bookings

Liste, détail, historique, cancel, complete et stats sont **COMPLETE**. Pagination DB, idempotence et transitions concurrentes sont couvertes.

### Payments

| Capacité | État | Observation |
|---|---|---|
| global list | COMPLETE |
| detail | PARTIAL | certains enrichissements externes peuvent être `null` |
| refunds | COMPLETE |
| idempotency | COMPLETE |
| anomalies | PARTIAL | doublons provider/refunds couverts ; rapprochements Booking/Ledger nécessitent projections interservices |
| reconciliation | COMPLETE | job asynchrone |

### Commerce

Promotions, commissions, ledger et adjustments sont **COMPLETE**. Le ledger est append-only et les mutations financières imposent `Idempotency-Key`.

### Moderation

Reports, assignment, decisions, reviews, comments, sanctions, trust et audit sont **COMPLETE**. Les suppressions de contenu sont logiques et synchronisées par événements.

### Gamification

Missions, badges, XP rules, rewards, antifraud et XP ledger sont **COMPLETE**. Le ledger XP admin est maintenant exposé sur `/api/v1/admin/gamification/xp-ledger`, en lecture seule et paginée.

### Campaigns

Route Gateway, list, detail, approve, reject et scopes sont **COMPLETE**. La route campaign est prioritaire sur `/api/v1/admin/**`.

### Analytics

| Capacité | État | Observation |
|---|---|---|
| dashboard | COMPLETE |
| KPIs/history | COMPLETE |
| regions/partners/places/users | COMPLETE |
| event logs | COMPLETE, paginé |
| rebuild | COMPLETE, asynchrone |

Les agrégats absents d'une source retournent un état vide, jamais des données fictives.

### Support

Conversations, messages, assignment, notes, status et SLA sont **COMPLETE** dans `support-service`. Les notes internes ne sont pas exposées par une API utilisateur.

### Newsletter

| Capacité | État | Observation |
|---|---|---|
| campaigns | COMPLETE |
| audiences | PARTIAL | critères stockés ; résolution dépend des projections User/Partner |
| schedule/send | COMPLETE, asynchrone |
| unsubscribe | PARTIAL | compteur présent ; source d'opt-out marketing externe à brancher |
| analytics | COMPLETE selon événements provider |

La dépendance réelle restante est un read model de consentement marketing alimenté par User/Notification preferences.

### Search

Overview, indexes, reindex, synonyms, ranking et zero-results sont **COMPLETE** dans `discovery-service`, avec OpenSearch existant.

### Settings

Platform settings, feature flags, versioning et rollback sont **COMPLETE** dans `admin-service`. Allowlist stricte et rejet des secrets.

### Notifications

Liste admin, unread count, read/unread/read-all et métadonnées de deep link sont **COMPLETE**.

### Infrastructure

| Capacité | État | Observation |
|---|---|---|
| Gateway | COMPLETE | routes prioritaires admin |
| correlation IDs | COMPLETE | génération/propagation |
| error contract | PARTIAL | coexistence JSON commun et `ProblemDetail` |
| Kafka | COMPLETE | enveloppe versionnée |
| outbox | COMPLETE sur flux critiques |
| DLQ/retry | COMPLETE sur consumers critiques |
| idempotency | COMPLETE sur finances, booking, consumers |
| observability | COMPLETE | Actuator, Prometheus, observations Kafka |
| Docker | COMPLETE pour stack locale ; quelques images utilisent build reactor + JRE plutôt que multi-stage autonome |
| migrations | COMPLETE | Flyway par service |
| tests | PARTIAL | forte couverture unitaire/sécurité ; Testcontainers non généralisé à tous les services |

## 3. Services et bases

| Service | Responsabilité admin | Stockage |
|---|---|---|
| api-gateway | routage, sécurité périmétrique, corrélation | Redis |
| auth-service | credentials, platform users, sessions | PostgreSQL + Redis |
| admin-service | admin_users, gouvernance, audit central, validations | PostgreSQL + Redis |
| partner-service | partenaires et KYC | PostgreSQL |
| place-service | lieux/référentiels | PostgreSQL/PostGIS |
| catalog-service | assets/collections | PostgreSQL |
| ingestion-service | imports | PostgreSQL |
| event-service | événements | PostgreSQL |
| ticket-service | inventaire, tickets, scans | PostgreSQL |
| booking-service | réservations | PostgreSQL |
| payment-service | paiements/refunds/reconciliation | PostgreSQL |
| commerce-service | promotions/commissions/ledger | PostgreSQL |
| interaction-service | reviews/comments | PostgreSQL |
| moderation-trust-service | reports/trust/sanctions | PostgreSQL |
| mission-reward-service | missions/définitions/fraude | PostgreSQL |
| gamification-service | XP ledger/profils | PostgreSQL + Redis |
| campaign-service | campagnes | PostgreSQL |
| analytics-service | projections/KPI/logs | PostgreSQL + OpenSearch/Cassandra selon flux |
| support-service | support desk | PostgreSQL |
| notification-service | notifications/newsletters/push | PostgreSQL |
| discovery-service | Search admin/Maps | PostgreSQL + OpenSearch + Redis |

## 4. Permissions et scopes

| Domaine | Lecture | Mutation |
|---|---|---|
| utilisateurs | SUPPORT/ADMIN | ADMIN/SUPER_ADMIN |
| administrateurs | ADMIN | SUPER_ADMIN |
| KYC | ADMIN | ADMIN/SUPER_ADMIN |
| contenu/catalogue/events | EDITOR/ADMIN | EDITOR/ADMIN |
| finance | SUPPORT/ADMIN | ADMIN/SUPER_ADMIN |
| modération | MODERATOR/ADMIN | MODERATOR/ADMIN |
| support | SUPPORT/ADMIN | SUPPORT/ADMIN |
| newsletter | COMMERCIAL/ADMIN | COMMERCIAL/ADMIN |
| settings/flags | SUPER_ADMIN | SUPER_ADMIN |
| campaign approve | scope `campaign:approve` | même scope |
| campaign reject | scope `campaign:reject` | même scope |

Le backend reste l'autorité finale même si le frontend masque les actions.

## 5. Événements Kafka

Enveloppe : `eventId,eventType,version,producer,occurredAt,correlationId,aggregateId,payload`.

Flux critiques :

- `PaymentSucceeded`, `payment.authorized`, `payment.refunded` ;
- `BookingConfirmed`, annulation/complétion admin ;
- `PartnerApproved`, `PartnerRejected`, corrections KYC ;
- `TicketIssued`, scans validés/rejetés ;
- `AdminAuditEvent` ;
- `FeatureFlagChanged`, `PlatformSettingChanged` ;
- événements moderation, support, campaign et newsletter.

Les consumers critiques enregistrent `eventId` avant acquittement transactionnel. Les DLT sont précréées en local.

## 6. Gateway

Routes admin prioritaires vérifiées :

- platform users → auth-service, ordre `-110` ;
- events, places, partners, interaction, search, notifications/newsletters, support, campaign → services propriétaires, ordre négatif ;
- `/api/v1/admin/**` générique → admin-service après les routes spécialisées ;
- gamification XP ledger → gamification-service, ordre `-100`.

## 7. Divergences avec `yeyamo-admin`

Les quatorze divergences exactes sont détaillées dans `YEYAMO_ADMIN_API_CONTRACT.md`. Elles relèvent désormais principalement du frontend :

- anciennes routes publiques utilisées pour Events, Partners, Payments et Booking detail ;
- adapters Support/Newsletter désactivés malgré les APIs ;
- Search déclaré indisponible ;
- Notifications utilisant le domaine utilisateur ;
- types de pagination et enums Events incomplets ;
- route Analytics rebuild incorrecte.

Le backend ne doit pas créer d'aliases publics moins sécurisés pour masquer ces erreurs frontend.

## 8. OpenAPI

Chaque service expose son document runtime sur `/v3/api-docs`. Les controllers récents incluent `@Operation`, `@Tag` et `@SecurityRequirement` lorsqu'ils ont été ajoutés. Ce document complète les annotations historiques en donnant permissions, pagination, idempotence et correspondance frontend.

Un contrat OpenAPI dédié existe déjà pour platform users dans `docs/openapi/admin-users-api.yaml`. L'étape suivante recommandée est une agrégation CI des `/v3/api-docs` de chaque service derrière un portail, avec détection de breaking changes.

## 9. Tests

Couverture présente :

- 401/403, rôles et scopes ;
- acteur provenant du JWT ;
- transitions Booking/Event/Campaign ;
- idempotence refund, ledger, Kafka, webhook ;
- redelivery consumers et outbox ;
- KYC/modération/audit ;
- ticket QR et inventaire ;
- Gateway campaign et corrélation.

Limites :

- les p95 nécessitent une stack peuplée et `tests/load/admin-api.k6.js` ;
- Testcontainers est présent sur plusieurs services mais pas uniformisé ;
- les tests de contrat frontend doivent être générés depuis OpenAPI plutôt que recopier manuellement les DTO.

## 10. Dette restante

1. Uniformiser tous les handlers d'erreur sur un seul JSON, sans `ProblemDetail` divergent.
2. Construire la projection Partner enrichie pour filtres KYC/géographiques.
3. Construire les projections Payment↔Booking↔Ledger pour toutes les anomalies croisées.
4. Alimenter Newsletter avec une projection de consentement marketing et segmentation.
5. Migrer tous les producteurs Kafka vers `event-contracts`, puis retirer l'alias `eventVersion`.
6. Agréger et versionner les OpenAPI runtime en CI.
7. Corriger les quatorze divergences du frontend plutôt que créer des endpoints backend de compatibilité.

