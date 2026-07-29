# Audit backend des APIs administratives YeYamo

Date : 29 juillet 2026

## Synthèse d’architecture

- Monorepo Maven Java 21 composé de 32 modules, dont 27 microservices Spring Boot.
- Gateway : Spring Cloud Gateway Server WebMVC, routes externalisées dans `cloud-conf-yeyamo/api-gateway.properties`.
- Sécurité : JWT HMAC partagé, issuer/audience stricts via `security-hardening-starter` dans les services qui déclarent un `JwtDecoder`.
- Persistance principale : PostgreSQL avec Flyway par service.
- Cassandra : analytics event logs. Redis : auth/rate limits/cache selon services. Neo4j est présent mais désactivé dans Compose.
- Kafka : événements métier et Transactional Outbox présents notamment dans admin, auth, campaign, catalog, notification et ticketing.

## Classification des domaines admin

| Domaine | État | Propriétaire | Gateway | Observation |
|---|---|---|---|---|
| Admin users / audit / validations | EXISTING_AND_USABLE | admin-service | routé `/api/v1/admin/**` | rôles convertis explicitement |
| Campaign admin | SECURITY_BLOCKED | campaign-service | route prioritaire existante, order -100 | scopes absents des JWT et method security non activée |
| Analytics admin | EXISTING_AND_USABLE | analytics-service | routé | PostgreSQL + Cassandra selon projection/logs |
| Moderation / trust | EXISTING_AND_USABLE | moderation-trust-service | routé | décisions et acteur via Authentication |
| Catalog / collections | EXISTING_AND_USABLE | catalog-service | routé | PostgreSQL/Flyway |
| Imports | EXISTING_BUT_INCOMPLETE | ingestion-service | routé | création/détail ; liste/retry/cancel admin manquants |
| Missions | EXISTING_AND_USABLE | mission-reward-service | routé | list/create/activate/pause |
| Gamification admin avancée | MISSING | gamification-service | route admin absente | badges/rules/rewards/fraud admin |
| Commerce / ledger / promotions | EXISTING_BUT_INCOMPLETE | commerce-service | routé | historique commissions absent |
| Booking management | EXISTING_BUT_INCOMPLETE | booking-service | routé | index admin et agrégats à confirmer/compléter |
| Payment admin/refunds | EXISTING_AND_USABLE | payment-service | routé | idempotence requise |
| Geography | EXISTING_BUT_INCOMPLETE | place-service | routé | catégories en écriture absentes |
| Events | EXISTING_AND_USABLE | event-service | routé | liste admin limitée |
| Ticketing admin | EXISTING_BUT_INCOMPLETE | ticket-service | routé, order -10 | scans présents, synthèse événement absente |
| Platform users admin | EXISTING_BUT_INCOMPLETE | user/admin services | routes partagées | contrat frontend attendu à aligner avec propriétaire réel |
| Notifications admin personnelles | EXISTING_AND_USABLE | notification-service | routé | list/unread/read/read-all |
| Support desk | MISSING | messaging-service ou futur domaine support | aucune route support | ne pas dupliquer messaging sans décision DDD |
| Newsletter | MISSING | notification-service recommandé | aucune route | réutiliser templates/outbox email |
| Search administration | MISSING | search-service | route admin absente | santé, index, synonyms, ranking, reindex |
| Settings / roles / feature flags | MISSING | admin-service + auth-service | aucune route dédiée | secrets exclus des DTO |

## Cartographie critique existante

### Campaign admin

- Microservice : campaign-service
- Controller : `AdminCampaignController`
- `GET /api/v1/admin/campaigns` → `Page<CampaignResponse>` ; scope approve ou reject
- `GET /api/v1/admin/campaigns/{id}` → `CampaignResponse` ; scope approve ou reject
- `POST /api/v1/admin/campaigns/{id}/approve` → `CampaignResponse` ; scope `campaign:approve`
- `POST /api/v1/admin/campaigns/{id}/reject` ; request `RejectCampaignRequest` ; scope `campaign:reject`
- Acteur : `Authentication.getName()`, donc subject JWT ; aucun actorId frontend.
- Base : PostgreSQL, Flyway `V1__create_campaigns_table.sql`.
- Audit métier : Transactional Outbox contenant actorId et correlationId.
- Gateway : `routes[25]`, `order=-100`, chemin campaign public et admin. La route est déjà prioritaire sur `routes[4]` admin-service.

### Admin service

- Controllers : `AdminUserController`, `ValidationController`, `ReportController`, `ModerationController`, `AuditController`.
- Rôles : ADMIN/SUPER_ADMIN ; MODERATOR ajouté sur modération/reports.
- DTO d’acteur : l’audit utilise le SecurityContext ; les décisions KYC doivent continuer à ignorer tout identifiant acteur externe.
- Base : PostgreSQL, Flyway V1/V2.
- Kafka/outbox : décisions partenaires émises avec actorId/correlationId.

## Constat JWT et authorities

1. auth-service émet `sub`, `roles`, email et téléphone, mais aucun `scope`, `scopes` ou `permissions`.
2. Les rôles disponibles sont USER, PARTNER, ADMIN, SUPER_ADMIN et MODERATOR. EDITOR, SUPPORT et COMMERCIAL manquent.
3. Gateway et admin-service préfixent correctement les rôles en `ROLE_`.
4. Gateway ignore actuellement scopes et permissions.
5. campaign-service utilise `@PreAuthorize(SCOPE_...)` sans SecurityFilterChain locale ni `@EnableMethodSecurity` détecté.
6. Le starter durcit un JwtDecoder existant, mais ne crée ni chaîne de sécurité ni convertisseur d’authorities.

## Plan d’implémentation ordonné

1. Étendre la convention de rôles auth sans renommer les rôles existants.
2. Définir une matrice centralisée rôle → scopes dans auth-service et émettre `scope`, `scopes`, `permissions`.
3. Étendre le convertisseur Gateway pour rôles, scopes et permissions.
4. Ajouter un matcher campaign admin prioritaire dans la sécurité Gateway, distinct du routage déjà correct.
5. Activer OAuth2 Resource Server et method security dans campaign-service.
6. Convertir dans campaign-service rôles, scopes et permissions avec conventions `ROLE_`, `SCOPE_`, `PERMISSION_`.
7. Tester route prioritaire, 401, 403, scope manquant/autorisé et acteur issu du JWT.
8. Tester la non-régression `/api/v1/admin/**` vers admin-service.
9. Compiler et tester security starter, auth-service, api-gateway, campaign-service et admin-service.

## Risques de régression

- Les anciens tokens sans scope restent valides pour les endpoints à rôles, mais seront refusés par campaign-service jusqu’à reconnexion/refresh.
- Ajouter des enum roles nécessite que la colonne PostgreSQL reste textuelle ; c’est le cas du mapping JPA actuel.
- Ne pas modifier l’ordre des indices de routes existants : la route campaign prioritaire existe déjà.
- Ne pas autoriser campaign au Gateway par rôle seulement : campaign-service reste l’autorité finale par scope.
- Ne pas faire confiance à un actorId, adminId ou reviewerId transmis dans un body.

## Bilan après sécurisation prioritaire

- Route Gateway campaign : déjà présente avec `order=-100`, donc conservée et couverte par un test de contrat.
- Gateway : `/api/v1/admin/campaigns/**` est authentifié séparément avant la règle générique admin ; les rôles, scopes et permissions JWT sont convertis en authorities Spring.
- Auth : les rôles `EDITOR`, `SUPPORT` et `COMMERCIAL` sont reconnus ; les JWT contiennent désormais `scope`, `scopes` et `permissions` en plus de `roles`.
- Campaign : OAuth2 Resource Server stateless et method security sont activés ; les endpoints admin restent protégés par `SCOPE_campaign:approve` et `SCOPE_campaign:reject`.
- Acteur : les décisions campaign utilisent le sujet authentifié et non un identifiant fourni par le frontend.
- Audit : le transactional outbox campaign conserve actorId et correlationId sans payload sensible.
- Tests : route prioritaire, authorities, 401, 403, scope autorisé et actorId issu de l'authentification sont couverts.
- Validation : tests Maven et packaging des cinq modules touchés terminés avec `BUILD SUCCESS`.
