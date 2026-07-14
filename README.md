# YeYamo API — Rapport d'analyse architecturale

> Analyse statique du dépôt et comparaison avec le document technique
> `YeYamo_Document_Technique_Backend_Microservices.pdf`.
>
> Ce document décrit l'état observé du code au 12 juillet 2026. Le terme
> « complet » désigne ici le niveau d'implémentation fonctionnelle visible dans
> le dépôt, et non une validation de mise en production.

## Synthèse

### Mise à jour du socle d'infrastructure

La première version du socle a été implémentée après l'analyse initiale :

- `config-server` utilise le backend natif `cloud-conf-yeyamo`, refuse les
  applications inconnues et expose ses healthchecks Actuator ;
- `registry-service` est configuré comme serveur Eureka standalone, piloté par
  le Config Server et prêt pour les healthchecks de conteneur ;
- `api-gateway` route `auth`, `place`, `event`, `admin` et `analytics` via
  Eureka/Spring Cloud LoadBalancer ;
- le gateway centralise JWT/RBAC, CORS, correlation IDs, rate limiting Redis,
  circuit breakers et réponses de fallback ;
- les trois services disposent désormais d'un Dockerfile avec healthcheck.

Le Config Server doit être démarré avant Eureka et le Gateway. Depuis le dossier
`config-server`, le chemin par défaut `file:../cloud-conf-yeyamo` fonctionne. En
conteneur, monter le dépôt de configuration et définir `CONFIG_REPOSITORY_URI`
avec son chemin `file:` dans le conteneur.

Le dépôt n'est pas un monolithe applicatif classique. Il s'agit d'un **monorepo
contenant 29 applications Spring Boot indépendantes**, correspondant à une
architecture microservices en cours de construction.

Seuls quelques services contiennent actuellement une véritable implémentation
métier. Les autres sont essentiellement des squelettes générés avec une classe
principale Spring Boot et un test de chargement du contexte.

Les services les plus avancés sont :

1. `auth-service`
2. `place-service`
3. `admin-service`
4. `event-service`
5. `analytics-service`

Depuis l'analyse initiale, `user-service` a été implémenté avec PostgreSQL,
Flyway, JWT, profils publics/privés, préférences, soft delete, consommation
Kafka idempotente et outbox transactionnelle. Il rejoint donc les services
métier avancés du dépôt.

`partner-service` est également implémenté : comptes professionnels, workflow
KYC, documents privés, règles d'éligibilité par type d'entreprise, Flyway,
sécurité JWT, outbox Kafka et consommation idempotente des décisions admin.

### Intégration Auth, User et Partner

Les frontières entre ces trois services sont reliées par événements versionnés :

```text
auth-service -- user.created --> user-service
partner-service -- partner.submitted --> admin-service
admin-service -- partner.approved/rejected/... --> partner-service
partner-service -- partner.approved --> auth-service
auth-service -- user.role_added --> autres consommateurs
```

`auth-service` utilise désormais une outbox PostgreSQL pour publier la création
des comptes. `user-service` crée le profil de manière idempotente. Après
validation KYC, `auth-service` consomme `partner.approved`, attribue le rôle
`PARTNER` au propriétaire et enregistre l'événement traité afin d'éviter toute
double application. Les services conservent des bases séparées et ne partagent
aucune entité JPA.

`admin-service` possède sa propre outbox et sa table d'événements traités. Une
soumission KYC crée automatiquement une validation administrative. Les décisions
`UNDER_REVIEW`, `APPROVED`, `REJECTED`, `NEEDS_INFO` et `REQUIRES_CHANGES` sont
publiées sur `admin.events`, puis appliquées idempotemment dans
`partner-service`.

**Aucun service ne peut encore être déclaré totalement terminé ou prêt pour la
production.** Les principales raisons sont l'absence de tests métier et
d'intégration, de documentation OpenAPI, d'un environnement Docker Compose
complet et de validation des interactions entre services.

## État des services

| Service | Volume métier observé | État | Conclusion |
|---|---:|---|---|
| `auth-service` | ~40 classes | Très avancé | Fonctionnellement presque complet |
| `place-service` | ~47 classes | Très avancé | Fonctionnellement presque complet |
| `admin-service` | ~43 classes | Très avancé | Presque complet en isolation |
| `event-service` | ~23 classes | Avancé | Cœur fonctionnel implémenté |
| `analytics-service` | ~20 classes | Avancé mais fragile | Prototype fonctionnel |
| `api-gateway` | 1 classe | Squelette | Non implémenté fonctionnellement |
| `config-server` | 1 classe | Infrastructure minimale | Partiellement prêt |
| `registry-service` | 1 classe | Infrastructure minimale | Partiellement prêt |
| `user-service` | Architecture hexagonale | Avancé | Profil et préférences implémentés |
| `partner-service` | Architecture hexagonale | Avancé | Compte partenaire et KYC implémentés |
| `social-service` | 1 classe | Squelette | Non implémenté |
| `graph-service` | 1 classe | Squelette | Non implémenté |
| `search-service` | 1 classe | Squelette | Non implémenté |
| `messaging-service` | 1 classe | Squelette | Non implémenté |
| `notification-service` | Architecture hexagonale complète | In-app, email, push, préférences, Kafka idempotent, retry/DLQ, JWT et OpenAPI | Prêt V1 |
| `booking-service` | 1 classe | Squelette | Non implémenté |
| `payment-service` | 1 classe | Squelette | Non implémenté |

Chaque projet possède un unique test `contextLoads`. Aucun test métier réel n'a
été trouvé.

## Analyse des services avancés

### Auth Service

`auth-service` est le service fonctionnel le plus complet du dépôt.

Fonctionnalités présentes :

- inscription utilisateur ;
- connexion par identifiant et mot de passe ;
- génération et validation de JWT ;
- filtre d'authentification Spring Security ;
- refresh tokens aléatoires stockés sous forme de hash SHA-256 ;
- rotation et révocation des refresh tokens ;
- rôles et contrôle d'accès RBAC ;
- OAuth Google et Apple ;
- OTP stockés dans Redis ;
- vérification d'adresse e-mail ;
- oubli et réinitialisation du mot de passe ;
- envoi d'e-mails ;
- consultation de l'utilisateur courant ;
- déconnexion ;
- initialisation des rôles au démarrage.

Patterns observés :

- architecture en couches `Controller -> Service -> Repository -> Entity` ;
- Repository Pattern avec Spring Data JPA ;
- Service Layer ;
- DTO Pattern ;
- Dependency Injection ;
- Filter Chain / Intercepting Filter pour JWT ;
- RBAC ;
- Token Rotation ;
- hashing des tokens sensibles ;
- transactions applicatives ;
- Global Exception Handler ;
- Seeder Pattern pour les rôles.

Limites :

- JWT signé avec un secret symétrique alors que le PDF recommande RS256 ou
  ES256 ;
- aucune blacklist Redis visible pour les access tokens ;
- aucun rate limiting ;
- aucune migration Flyway visible ;
- aucun test unitaire ou d'intégration PostgreSQL/Redis ;
- aucune documentation OpenAPI ;
- aucun Dockerfile.

**Estimation : 75 à 85 % fonctionnel, mais non prêt pour la production.**

### Place Service

Fonctionnalités présentes :

- gestion des régions, villes et quartiers ;
- catégories de lieux ;
- création, consultation et modification des lieux ;
- médias et horaires ;
- coordonnées géographiques ;
- recherche à proximité ;
- geohash et génération de slugs ;
- pagination ;
- événements Kafka à la création et à la modification ;
- auditing JPA.

Patterns observés :

- architecture en couches ;
- Repository Pattern ;
- Service Layer ;
- DTO Pattern ;
- transactions Spring ;
- Domain Events ;
- Publish/Subscribe avec Kafka ;
- Entity Auditing ;
- Utility Pattern pour les slugs et geohash ;
- Global Exception Handler ;
- Database per Service dans l'intention.

Limites :

- absence d'Outbox Pattern : une transaction PostgreSQL peut réussir alors que
  la publication Kafka échoue ;
- résultat de l'envoi Kafka non contrôlé ;
- erreur de sérialisation seulement journalisée ;
- endpoints non sécurisés dans le service ;
- absence de migration Flyway ;
- absence de tests métier et géographiques ;
- intégration Mapbox absente ;
- OpenAPI et Dockerfile absents.

**Estimation : 75 à 80 % fonctionnel, mais non prêt pour la production.**

### Event Service

Fonctionnalités présentes :

- création et modification d'événements ;
- consultation par identifiant ;
- liste des événements à venir ;
- événements associés à un lieu ;
- changement de statut ;
- inscription et désinscription ;
- publication d'événements Kafka ;
- auditing JPA.

Patterns observés :

- architecture en couches ;
- Repository, Service Layer et DTO Patterns ;
- State Pattern simplifié par des enums ;
- Domain Events et Publish/Subscribe Kafka ;
- transactions ;
- JPA Auditing ;
- Global Exception Handler.

Limites :

- gestion de capacité et des inscriptions simultanées non validée ;
- absence d'Outbox Pattern ;
- endpoints non sécurisés ;
- absence de migrations Flyway ;
- absence de tests métier et de concurrence ;
- OpenAPI et Dockerfile absents.

**Estimation : 65 à 75 % fonctionnel.**

### Admin Service

Fonctionnalités présentes :

- gestion des utilisateurs administrateurs ;
- rôles `ADMIN`, `SUPER_ADMIN` et `MODERATOR` ;
- validation des partenaires et des lieux ;
- création et résolution des signalements ;
- actions de modération ;
- journal d'audit administratif ;
- JWT OAuth2 Resource Server ;
- autorisations avec `@PreAuthorize` ;
- migration Flyway initiale ;
- Actuator, Prometheus et Dockerfile.

Patterns observés :

- architecture en couches ;
- Repository, Service Layer et DTO Patterns ;
- RBAC et Method-Level Security ;
- Audit Log Pattern ;
- workflow / machine à états simplifiée avec enums ;
- Database Migration Pattern avec Flyway ;
- Transaction Management ;
- Global Exception Handler ;
- observabilité avec Actuator et Prometheus.

Limites :

- `partner-service` et `place-service` ne sont pas intégrés au workflow admin ;
- validations enregistrées localement sans orchestration complète ;
- aucune utilisation Kafka visible malgré la dépendance ;
- absence de tests RBAC, de modération et de migration ;
- absence d'OpenAPI et d'idempotence démontrée.

**Estimation : 70 à 80 % en isolation, mais incomplet à l'échelle du système.**

### Analytics Service

Fonctionnalités présentes :

- consommation de plusieurs topics Kafka ;
- journalisation des événements ;
- historique de KPI ;
- activité par région ;
- popularité des lieux ;
- engagement utilisateur ;
- analytics partenaires et dashboard administrateur ;
- publication d'un événement d'audit en cas d'échec ;
- stockage Elasticsearch ;
- Actuator, Prometheus et Dockerfile.

Patterns observés :

- Event-Driven Architecture ;
- Consumer Pattern ;
- Materialized Views ;
- CQRS pragmatique côté lecture ;
- Repository Pattern avec Elasticsearch ;
- Polyglot Persistence ;
- Audit Event Pattern ;
- Service Layer.

Limites critiques :

- idempotence insuffisante : une nouvelle livraison Kafka peut créer des KPI en
  double ;
- aucune contrainte d'unicité démontrée sur `eventId` ;
- une erreur est capturée sans être relancée, ce qui peut conduire Kafka à
  considérer un message échoué comme traité ;
- absence de retry, backoff et dead-letter topic ;
- absence de traitement atomique et de stratégie explicite des offsets ;
- absence de tests de duplication et de reprise ;
- endpoints analytics non sécurisés.

**Estimation : 55 à 65 %, prototype avancé mais non fiable en production.**

## Services d'infrastructure

### Config Server

Le serveur Spring Cloud Config est présent et utilise le profil `native`. Il
recherche les configurations dans :

```text
file:../../conf/cloud-conf-yeyamo
```

Ce répertoire n'est pas présent dans le dépôt analysé. Les services déclarent
l'import Config Server comme optionnel. Ils peuvent donc amorcer leur démarrage
sans le serveur, mais probablement sans les paramètres requis pour PostgreSQL,
Redis, Kafka, Elasticsearch et JWT.

### Registry Service

Le projet Eureka existe, mais sa configuration locale complète dépend du Config
Server. Il constitue un socle d'infrastructure, pas encore une preuve de service
discovery opérationnel de bout en bout.

### API Gateway

Le projet existe, mais aucune implémentation visible ne fournit encore :

- routes vers les services ;
- filtres JWT ;
- politique CORS ;
- rate limiting Redis ;
- correlation ID ;
- journalisation edge ;
- circuit breakers.

L'API Gateway est donc un squelette et non une gateway fonctionnelle.

## Architecture réellement observée

```text
Monorepo YeYamo
├── applications Spring Boot indépendantes
├── API Gateway prévue
├── Config Server
├── Service Registry Eureka
├── PostgreSQL/JPA pour le transactionnel
├── Redis pour OTP et données temporaires
├── Kafka pour certains événements
└── Elasticsearch pour les analytics
```

Il s'agit d'une **architecture microservices partiellement implémentée dans un
monorepo**, et non d'une architecture monolithique traditionnelle.

À l'intérieur des services, l'organisation est principalement une architecture
en couches classique. Le découpage recommandé par le PDF en packages `domain`,
`application`, `infrastructure` et `interfaces` n'est pas appliqué.

Les entités JPA représentent simultanément le domaine et la persistance. Le code
n'implémente donc pas encore une architecture hexagonale, une Clean Architecture
ou un DDD strict.

## Patterns architecturaux identifiés

### Patterns réellement présents

- Layered Architecture ;
- Controller–Service–Repository ;
- Repository Pattern ;
- DTO Pattern ;
- Service Layer ;
- Dependency Injection ;
- Global Exception Handler ;
- Transaction Script ;
- RBAC et Method-Level Security ;
- Filter Chain ;
- Domain Events ;
- Publisher/Subscriber ;
- Event Consumer ;
- JPA Auditing ;
- Database Migration avec Flyway dans `admin-service` ;
- Materialized Read Models ;
- Polyglot Persistence partielle ;
- Service Discovery ;
- Centralized Configuration.

### Patterns absents ou incomplets

- Outbox Pattern ;
- Saga Pattern ;
- Circuit Breaker ;
- Retry avec backoff ;
- Dead Letter Queue ;
- Idempotent Consumer ;
- API Composition ;
- Distributed Tracing ;
- correlation ID global ;
- Hexagonal Architecture ;
- CQRS complet ;
- Contract Testing ;
- Testcontainers ;
- API Gateway filters ;
- Rate Limiting ;
- Schema Versioning Kafka.

## Comparaison avec le document technique

Le code suit déjà plusieurs orientations du PDF :

- Java 21 ;
- séparation par bounded contexts ;
- projets API Gateway, Config Server et Eureka ;
- PostgreSQL pour les données transactionnelles ;
- Redis dans l'authentification ;
- Kafka dans `place-service`, `event-service` et `analytics-service` ;
- Elasticsearch dans `analytics-service` ;
- séparation des DTO et des entités ;
- gestion globale des exceptions ;
- RBAC dans `auth-service` et `admin-service` ;
- Flyway dans `admin-service` ;
- Actuator et Prometheus dans certains services.

### Écarts avec la cible du PDF

| Recommandation du PDF | État dans le dépôt |
|---|---|
| Spring Boot 3.x | POM configurés en Spring Boot `4.1.0` |
| Docker Compose complet | Absent |
| Dockerfile par service | Présent seulement pour `admin` et `analytics` |
| OpenAPI/Swagger | Absent |
| Tests unitaires métier | Absents |
| Tests Testcontainers | Absents |
| Migrations par service PostgreSQL | Flyway seulement dans `admin-service` |
| Cassandra | Non implémenté |
| Neo4j | Non implémenté |
| OpenSearch pour la recherche | `search-service` vide |
| Mapbox | Non implémenté |
| WebSocket | Non implémenté |
| Rate limiting Redis | Non implémenté |
| Correlation ID global | Très partiel |
| Retry et DLQ Kafka | Non implémentés |
| Idempotence Kafka | Insuffisante |
| Outbox Pattern | Absent |
| CI/CD | Absent |
| Monitoring global | Partiel |
| Secrets et configuration locale reproductible | Configuration externe manquante |
| API Gateway opérationnelle | Non |
| Architecture hexagonale par service | Non |

## Qualité et préparation à la production

Le principal point faible transversal est la validation. Aucun test observé ne
couvre :

- les règles métier ;
- les repositories ;
- les contrôleurs ;
- la sécurité ;
- Kafka ;
- PostgreSQL ;
- Redis ;
- Elasticsearch ;
- les migrations ;
- les communications entre services.

Les versions Maven demandent également une vérification. Les projets déclarent
Spring Boot `4.1.0` et Spring Cloud `2025.1.2`, tandis que le PDF cible Spring
Boot 3. La compatibilité réelle des dépendances et la compilabilité de chaque
service doivent être validées.

L'analyse initiale est restée statique : Maven n'a pas été exécuté afin de ne pas
générer de répertoires `target` pendant la phase demandée en lecture seule. Le
présent rapport ne certifie donc pas que tous les services compilent ou démarrent.

## Risques techniques principaux

1. **Fausse impression de complétude** : la présence de nombreux projets ne
   signifie pas que les services sont implémentés.
2. **Environnement non reproductible** : pas de Docker Compose global et
   configuration externe absente.
3. **Fiabilité Kafka** : pas d'Outbox, d'idempotence robuste, de retry ou de DLQ.
4. **Sécurité hétérogène** : `auth` et `admin` sont sécurisés, mais les autres
   APIs métier ne le sont pas clairement.
5. **Absence de tests** : les régressions et règles métier ne sont pas protégées.
6. **Contrats non documentés** : absence d'OpenAPI et de schémas Kafka versionnés.
7. **Services trop nombreux trop tôt** : une grande partie de la cible existe
   uniquement sous forme de squelette.
8. **Dépendances de versions** : l'alignement Spring Boot / Spring Cloud doit être
   confirmé.

## Ordre de priorité recommandé — analyse initiale désormais historique

### P0 — Stabiliser le socle

1. Valider les versions Spring Boot, Spring Cloud et Java.
2. Créer un Docker Compose reproductible pour le MVP.
3. Fournir des configurations locales sans secrets.
4. Finaliser les routes, la sécurité et le CORS de l'API Gateway.
5. Ajouter Flyway à tous les services PostgreSQL avancés.
6. Ajouter OpenAPI à chaque API fonctionnelle.

### P1 — Sécuriser les services existants

1. Écrire les tests métier de `auth`, `place`, `event` et `admin`.
2. Ajouter des tests d'intégration avec Testcontainers.
3. Sécuriser `place`, `event` et `analytics` comme Resource Servers.
4. Mettre en place correlation IDs, logs structurés et métriques communes.
5. Ajouter Outbox, consommateurs idempotents, retry et DLQ pour Kafka.

### P2 — Compléter le MVP fonctionnel

1. Implémenter `user-service`.
2. Implémenter `partner-service`.
3. Implémenter un `social-service` minimal.
4. Relier les validations admin aux partenaires et aux lieux.
5. `notification-service` : **achevé V1**, à raccorder aux fournisseurs SMTP et push de l'environnement cible.

### P3 — Introduire les composants avancés selon le besoin

1. `search-service` et OpenSearch ;
2. `graph-service` et Neo4j ;
3. Cassandra pour le social à forte charge ;
4. messagerie WebSocket ;
5. réservation ;
6. paiement V2 ;
7. analytics avancé.

## Initialisation des services de l'architecture produit V2

Les modules suivants ont été initialisés le 12 juillet 2026 à partir de la
seconde fiche d'architecture. Ils disposent du socle technique commun, mais
leurs cas d'usage, entités, migrations métier et contrats Kafka restent à
implémenter.

| Service | Port | Stockage initial | Responsabilité cible |
|---|---:|---|---|
| `catalog-service` | 8088 | PostgreSQL + PostGIS | Référentiel touristique canonique |
| `ingestion-service` | 8089 | PostgreSQL | Imports, normalisation et déduplication |
| `content-service` | 8090 | PostgreSQL | Publications sociales et contenu éditorial |
| `interaction-service` | 8091 | PostgreSQL + Redis | Likes, commentaires, sauvegardes et check-ins |
| `feed-service` | 8092 | PostgreSQL + Redis | Construction des fils personnalisés |
| `discovery-service` | 8093 | PostgreSQL + Redis | Recherche et découverte |
| `recommendation-service` | 8095 | PostgreSQL + Redis | Recommandations personnalisées |
| `gamification-service` | 8096 | PostgreSQL + Redis | XP, badges, passeport et séries |
| `mission-reward-service` | 8098 | PostgreSQL | Missions, règles et récompenses |
| `referral-service` | 8099 | PostgreSQL + Redis | Parrainage et attribution |
| `moderation-trust-service` | 8100 | PostgreSQL | Signalements, modération et confiance |
| `media-service` | 8101 | PostgreSQL | Métadonnées et orchestration des médias |

Chaque module utilise Java 21, Spring Boot 4.1, Spring Cloud 2025.1, Config
Client, Eureka Client, Actuator, PostgreSQL, Flyway, Kafka, Bean Validation et
OAuth2 Resource Server. Les services à lecture intensive disposent également
de Redis. `catalog-service` inclut Hibernate Spatial pour préparer PostGIS.

Les configurations externalisées se trouvent dans `cloud-conf-yeyamo`. Les
routes V2 sont déclarées dans `api-gateway.properties`, avec découverte
Eureka, équilibrage de charge et circuit breaker.

Statut exact : **initialisé techniquement, non implémenté fonctionnellement**.
L'ordre conseillé pour la suite est `catalog-service`, `ingestion-service`,
`content-service`, `interaction-service`, puis `feed-service`. Les services
de recommandation, gamification et récompenses doivent être construits après
stabilisation de leurs événements sources.

## Réévaluation complète et ordre d'implémentation — 12 juillet 2026

> Cette section est l'état de référence actuel. Les sections précédentes
> conservent l'historique de l'analyse initiale, mais leurs anciennes priorités
> ne doivent plus être utilisées pour planifier les développements.

### Critères utilisés

Un service est considéré comme **prêt V1** lorsqu'il possède une responsabilité
métier identifiable, des endpoints ou des traitements utilisables, une
persistance maîtrisée, une sécurité cohérente, une configuration externalisée
et un minimum de tests. « Prêt V1 » ne signifie pas « prêt pour la production » :
il manque encore des tests d'intégration bout en bout, de l'observabilité
distribuée, des contrats OpenAPI/AsyncAPI et un environnement Docker Compose
global.

Les statuts employés sont :

- **Prêt infrastructure** : utilisable comme composant technique du socle ;
- **Prêt V1** : flux métier principal implémenté et raccordé ;
- **Partiel à durcir** : métier substantiel, mais garanties de production
  insuffisantes ;
- **Initialisé V2** : socle Spring/configuration créé, aucun métier ;
- **Squelette historique** : ancien module vide dont la place dans la V2 doit
  être confirmée avant développement.

### Inventaire des 29 applications

| Service | Statut | Éléments réellement présents | Décision |
|---|---|---|---|
| `config-server` | **Prêt infrastructure** | Backend natif, healthcheck, configuration centralisée | Conserver |
| `registry-service` | **Prêt infrastructure** | Serveur Eureka standalone, configuration externe | Conserver |
| `api-gateway` | **Prêt infrastructure V1** | Routage Eureka, JWT/RBAC, CORS, correlation ID, rate limit Redis, circuit breakers | Conserver et tester en E2E |
| `auth-service` | **Prêt V1** | Inscription/login, JWT, OTP, OAuth, refresh tokens, PostgreSQL/Flyway, outbox, consommation idempotente de `partner.approved` | Durcir, ne pas réécrire |
| `user-service` | **Prêt V1** | Profils et préférences, architecture hexagonale, Flyway, JWT, outbox, consommation idempotente de `user.created` | Conserver |
| `partner-service` | **Prêt V1** | Comptes partenaires, documents, workflow KYC, ports/adapters, Flyway, outbox et décisions admin idempotentes | Conserver |
| `admin-service` | **Prêt V1 ciblé** | Administration, audit, signalements, validation partenaire, outbox `admin.events`, consommation `partner.submitted` | Conserver ; séparer progressivement la modération |
| `place-service` | **Partiel à durcir** | CRUD géographique riche, catégories, médias, horaires, géohash, événements Kafka | Migrer vers le futur `catalog-service` ; ajouter Flyway, Resource Server, outbox et tests métier |
| `event-service` | **Partiel à durcir** | Événements, inscriptions, statuts, API par lieu, publication Kafka | Ajouter sécurité, Flyway, outbox, idempotence et tests métier |
| `analytics-service` | **Partiel à durcir** | Consommation Kafka, projections KPI et dashboard | Revoir après stabilisation des contrats V2 ; ajouter migrations, idempotence et reprise |
| `catalog-service` | **Prêt V1** | Destinations, lieux, expériences, régions, villes, catégories, recherche PostGIS, Flyway, JWT/RBAC, Outbox et consumers idempotents | Raccorder feed/discovery/recommandation à `catalog.events` |
| `ingestion-service` | **Prêt V1** | Imports CSV/JSON/API, pipeline, validation, déduplication, jobs idempotents, PostgreSQL/Flyway et outbox Kafka | Raccorder le consumer catalog puis tester avec PostgreSQL/Kafka |
| `media-service` | **Prêt V1** | Upload images/vidéos, métadonnées, stockage par port/adapter, miniatures, PostgreSQL/Flyway, Outbox Kafka, JWT et OpenAPI | Ajouter un adapter S3/MinIO pour la production |
| `content-service` | **Prêt V1** | Brouillons, publications, visibilité, hashtags, références media/catalog, soft delete, Flyway, JWT, OpenAPI et Outbox Kafka | Raccorder interaction, feed et modération à `content.events` |
| `interaction-service` | **Prêt V1** | Likes, commentaires, favoris, partages, check-ins, PostgreSQL/Flyway, Redis cache-aside, CQRS léger, idempotence, Outbox Kafka, JWT et OpenAPI | Raccorder feed/recommandation à `interaction.events` |
| `moderation-trust-service` | **Prêt V1** | Signalements, workflow de modération, scores de confiance, audit append-only, Flyway, JWT, Outbox et consumers idempotents | Migrer progressivement les anciens signalements de `admin-service` |
| `feed-service` | **Prêt V1** | Fil personnalisé, CQRS léger, projections PostgreSQL, ranking Strategy, Redis cache-aside, Outbox et consumers idempotents | Enrichir ultérieurement avec les préférences explicites de `user-service` |
| `discovery-service` | **Prêt V1** | Recherche textuelle et PostGIS, tendances, projections Kafka idempotentes, Redis cache-aside, JWT, OpenAPI et adaptateur OpenSearch sélectionnable | Brancher `catalog.events`, `content.events` et `interaction.events` puis valider sur l'infrastructure locale |
| `recommendation-service` | **Prêt V1** | Scoring explicable popularité/proximité/préférences/historique, projections Kafka, PostgreSQL, Redis, Outbox, JWT et OpenAPI | Ajuster les pondérations avec des données métier réelles puis mesurer la qualité du ranking |
| `gamification-service` | **Prêt V1** | Ledger XP append-only, niveaux, badges, séries, passeport, récompenses, PostgreSQL/Flyway, Redis, Outbox, consommateurs Kafka idempotents, JWT et OpenAPI | Valider les flux réels avec Kafka/Redis/PostgreSQL et raccorder le futur producteur `booking-service` |
| `mission-reward-service` | **Prêt V1** | Missions et objectifs configurables, règles COUNT/SUM/MAX, progression automatique, saga légère de récompense, PostgreSQL/Flyway, Outbox, consumers idempotents, JWT et OpenAPI | Valider les flux réels avec PostgreSQL/Kafka et brancher les futurs consommateurs de `mission.events` |
| `referral-service` | **Initialisé V2** | Socle PostgreSQL/Redis/Kafka/Flyway | À lancer après identité, récompenses et attribution |
| `booking-service` | **Squelette historique** | Classe principale et dépendances seulement | À implémenter après catalogue et partenaires |
| `payment-service` | **Squelette historique** | Classe principale et dépendances seulement | À implémenter uniquement après booking |
| `notification-service` | **Prêt V1** | Notifications in-app, email et push, préférences, templates, Flyway, consommation Kafka idempotente, retry/backoff, DLQ, JWT et OpenAPI | Configurer SMTP et le fournisseur push pour les livraisons externes |
| `messaging-service` | **Squelette historique** | Classe principale, dépendances Cassandra/WebSocket | Différer jusqu'à validation du besoin conversationnel |
| `social-service` | **Squelette historique à retirer du plan** | Aucune logique métier | Ne pas implémenter : responsabilité répartie entre content, interaction et feed |
| `search-service` | **Squelette historique à retirer du plan** | Aucune logique métier | Ne pas implémenter en parallèle : absorber dans discovery |
| `graph-service` | **Squelette expérimental** | Aucune logique métier, dépendances Neo4j | Différer ; introduire seulement si les cas d'usage démontrent le besoin |

### Services utilisables aujourd'hui

Le socle de lancement utilisable est :

```text
config-server -> registry-service -> api-gateway
                                   -> auth-service
                                   -> user-service
                                   -> partner-service
                                   -> admin-service
```

Le workflow événementiel le plus complet est :

```text
auth-service
  |-- user.created ----------------------------> user-service
  |
partner-service -- partner.submitted ----------> admin-service
       ^                                             |
       |-- partner.approved/rejected/suspended ------|
                                                  |
                                                     +--> admin.events
partner.approved --------------------------------> auth-service
auth-service -- user.role_added -----------------> consommateurs futurs
```

`place-service`, `event-service` et `analytics-service` peuvent servir de
base fonctionnelle ou de démonstration, mais ne doivent pas être déclarés prêts
pour la production tant que leur sécurité, leurs migrations et leur fiabilité
Kafka ne sont pas alignées sur auth/user/partner/admin.

### Ordre d'implémentation recommandé

#### Étape 0 — stabilisation avant nouveaux domaines

1. **Contrats transverses** : versionner les enveloppes Kafka, les noms de
   topics, les identifiants, les erreurs API et les règles d'idempotence.
2. **`place-service` et `catalog-service`** : décider la migration des données
   et du code utile de place vers catalog, sans maintenir deux référentiels
   concurrents.
3. **`event-service`** : remplacer `ddl-auto=update` par Flyway, ajouter JWT,
   outbox et tests de règles.
4. Ajouter Testcontainers, OpenAPI/AsyncAPI, tracing et un environnement local
   reproductible aux services prêts.

#### Étape 1 — noyau catalogue V2

1. **`catalog-service`** — priorité absolue. Il devient la source canonique des
   destinations, lieux, expériences, catégories, zones et attributs
   géographiques. Patterns : architecture hexagonale, DDD/Aggregate, Repository,
   Specification pour les recherches, Strategy pour les types d'actifs,
   Transactional Outbox et consommateurs idempotents.
2. **`ingestion-service`** — importe et normalise les données vers le catalogue.
   Patterns : Pipeline, Template Method, Strategy par fournisseur, Adapter,
   Anti-Corruption Layer, jobs idempotents et état de traitement persistant.
3. **`media-service`** — métadonnées, stockage objet et traitements asynchrones.
   Patterns : Ports & Adapters, Strategy par fournisseur de stockage,
   Factory pour les traitements, Saga légère par événements et Outbox.

#### Étape 2 — noyau social V2

4. **`content-service`** — posts, brouillons, publication, visibilité et
   références média/catalogue. Patterns : hexagonal, Aggregate, State,
   Repository, Outbox et soft delete.
5. **`interaction-service`** — likes, commentaires, sauvegardes, partages et
   check-ins. Patterns : commandes idempotentes, CQRS léger, cache-aside Redis,
   Specification et Outbox.
6. **`moderation-trust-service`** — signalements et décisions de confiance.
   Patterns : State Machine, Chain of Responsibility pour les contrôles,
   Strategy pour les politiques, consommateurs idempotents et audit append-only.
7. **`feed-service`** — projections de contenu et fils personnalisés. Patterns :
   CQRS, materialized views, fan-out hybride, Strategy de ranking, cache-aside
   et reconstruction par replay d'événements.

#### Étape 3 — découverte et engagement

8. **`discovery-service`** — recherche textuelle/géographique et tendances.
   Patterns : CQRS/projections, Specification, Adapter de moteur de recherche,
   cache-aside. L'introduction d'OpenSearch doit être motivée par le volume.
9. **`notification-service`** — notifications in-app, email et push issues des
   événements métier. Patterns : événementiel, Strategy par canal, Template
   Method, préférences utilisateur, retry/DLQ et idempotence.
10. **`recommendation-service`** — recommandations à partir des projections et
    signaux stabilisés. Patterns : Strategy de ranking, Pipeline, feature
    adapters, cache-aside et fallback déterministe.

#### Étape 4 — progression et croissance

11. **`gamification-service`** — ledger XP, badges, passeport et séries.
    Patterns : Event Sourcing ciblé ou ledger append-only, State, Specification,
    Strategy de calcul et idempotence stricte.
12. **`mission-reward-service`** — missions, conditions et récompenses.
    Patterns : State Machine, Specification/Rules Engine, Saga, Outbox et
    compensation.
13. **`referral-service`** — codes, attribution et anti-fraude.
    Patterns : Aggregate, Specification, State, idempotency key et Saga avec les
    récompenses.

#### Étape 5 — transactions et communication

14. **`booking-service`** — seulement lorsque catalog et partner exposent des
    contrats stables. Patterns : State Machine, Saga orchestrée, réservation
    temporaire, idempotency key et Outbox.
15. **`payment-service`** — après booking. Patterns : Adapter par prestataire,
    Strategy, Saga/compensation, ledger immuable, webhook idempotent et Outbox.
16. **`messaging-service`** — uniquement si le besoin de conversation temps réel
    est prioritaire. Patterns : WebSocket gateway, conversation aggregate,
    event log, fan-out et idempotence.
17. **`analytics-service`** — finaliser les projections produit après
    stabilisation des événements V2. Patterns : CQRS, materialized views,
    consumer idempotent, checkpoint/replay et traitement par fenêtres.

### Modules à ne pas implémenter tels quels

- **`social-service`** ferait doublon avec content + interaction + feed ;
- **`search-service`** ferait doublon avec discovery ;
- **`graph-service`** ne doit pas imposer Neo4j avant qu'un besoin de graphe
  mesurable ne le justifie ;
- l'ancien domaine de **`place-service`** doit converger vers catalog plutôt
  que créer deux sources de vérité.

### Architecture cible du workflow V2

```text
Sources externes -> ingestion -> catalog -----------------> discovery
                              |       |                         |
                              |       +--> content <-> media    |
                              |                |                |
auth -> user -----------------+----------> interaction ---------+
  |                                            |
  |                                            +--> moderation-trust
  |                                            +--> feed
  |                                            +--> recommendation
  |                                            +--> gamification
partner -> admin                                   |
  |                                               +--> mission-reward
  +----------> catalog/booking --------------------+--> referral
                         |
                         +--> payment

Tous les événements utiles -> notification
Tous les événements versionnés -> analytics
```

La règle structurante reste : chaque service possède sa base, échange par API
pour les besoins synchrones et par événements versionnés pour la propagation.
Les opérations qui modifient une base puis publient un événement utilisent une
Transactional Outbox ; les consommateurs conservent les identifiants traités.

### Bilan

À la date de cette réévaluation :

- **3 services d'infrastructure** sont prêts pour le socle V1 ;
- **4 services métier** sont prêts fonctionnellement en V1 ;
- **3 services métier** sont substantiels mais à durcir ;
- **12 services V2** sont correctement initialisés mais sans métier ;
- **7 anciens modules** sont encore des squelettes, dont trois ne doivent pas
  être développés tels quels.

La prochaine implémentation recommandée est donc **`catalog-service`**, en
réutilisant et migrant les concepts valides de `place-service`, puis
`ingestion-service` et `media-service`. Cette séquence réduit les doublons et
fournit les données stables dont dépend tout le social V2.

### Implémentation de catalog-service

`catalog-service` est la source canonique V2 des destinations, lieux,
expériences et référentiels géographiques. `place-service` reste accepté comme
source de migration par événement, mais ne partage ni table ni entité JPA avec
le catalogue.

Architecture et règles :

- architecture hexagonale : domaine, cas d'usage, ports puis adapters JPA,
  PostGIS, Kafka et REST ;
- actifs typés `DESTINATION`, `PLACE`, `EXPERIENCE` ou `EVENT` avec workflow
  `DRAFT → IN_REVIEW → PUBLISHED → ARCHIVED` et suppression logique ;
- régions, villes et catégories gérées comme référentiels versionnés et
  désactivables ; une ville exige une région active et les catégories peuvent
  être hiérarchiques ;
- recherche textuelle et filtres par type, région et catégorie ;
- recherche de proximité PostGIS avec `ST_DWithin`, tri par `ST_Distance` et
  index GiST sur `geometry(Point, 4326)` ;
- Flyway crée PostGIS, les contraintes, index géographiques/plein texte,
  l'Outbox et la table des événements consommés ;
- JWT Resource Server stateless : lectures publiées publiques, écritures et
  lectures de gestion réservées à `ADMIN`, `SUPER_ADMIN` ou `PARTNER` ;
- Outbox transactionnelle vers `catalog.events` pour les actifs et les
  référentiels ;
- consommateurs idempotents de `place.events` et
  `catalog.ingestion.events`, avec retries puis DLT ;
- OpenAPI et Swagger UI.

Endpoints principaux :

```text
GET    /api/v1/catalog/assets
GET    /api/v1/catalog/assets/nearby
GET    /api/v1/catalog/assets/{id}
GET    /api/v1/catalog/assets/slug/{slug}
GET    /api/v1/catalog/assets/manage/{id}
POST   /api/v1/catalog/assets
PUT    /api/v1/catalog/assets/{id}
PATCH  /api/v1/catalog/assets/{id}/status
DELETE /api/v1/catalog/assets/{id}

GET|POST /api/v1/catalog/regions
GET|PUT|DELETE /api/v1/catalog/regions/{id}
GET|POST /api/v1/catalog/cities
GET|PUT|DELETE /api/v1/catalog/cities/{id}
GET|POST /api/v1/catalog/categories
GET|PUT|DELETE /api/v1/catalog/categories/{id}
POST /api/v1/catalog/{regions|cities|categories}/{id}/activate

GET /v3/api-docs
GET /swagger-ui.html
```

Événements sortants : `catalog.asset.created`, `catalog.asset.updated`,
`catalog.asset.status_changed`, `catalog.asset.deleted`,
`catalog.asset.synchronized`, `catalog.reference.created`,
`catalog.reference.updated`, `catalog.reference.activated` et
`catalog.reference.deactivated`.

Lancement Docker local après construction du jar :

```powershell
cd catalog-service
..\config-server\mvnw.cmd clean package
docker compose up --build
```

Le Compose démarre PostGIS sur le port hôte `5438`, Kafka/Redpanda sur `19092`
et le service sur `8088`. Le secret JWT fourni dans le Compose est uniquement
destiné au développement local.

### Implémentation d'ingestion-service

`ingestion-service` est désormais un service fonctionnel V1 pour alimenter le
catalogue à partir de données externes.

Fonctionnalités :

- création de jobs avec l'en-tête obligatoire `Idempotency-Key` ;
- stratégies d'extraction CSV, JSON et API ;
- API distante protégée par une liste blanche d'hôtes afin de limiter le SSRF ;
- pipeline commun de normalisation, validation et déduplication ;
- fingerprint SHA-256 stable ;
- persistance PostgreSQL de chaque job et de chaque ligne acceptée, rejetée ou
  détectée comme doublon ;
- worker asynchrone persistant et protégé par verrouillage optimiste ;
- outbox transactionnelle vers `catalog.ingestion.events` ;
- enveloppe `catalog.asset.ingested` conforme au contrat événementiel YeYamo ;
- Flyway, JWT/RBAC, Eureka, Config Client, Actuator et tests H2.

Endpoints :

```text
POST /api/v1/catalog/imports
GET  /api/v1/catalog/imports/{jobId}
```

Le POST est réservé à `ADMIN` et `PARTNER` et retourne `202 Accepted`.
Exemple JSON :

```http
POST /api/v1/catalog/imports
Idempotency-Key: partner-42-places-2026-07-13
Authorization: Bearer <token>
Content-Type: application/json

{
  "sourceType": "CSV",
  "sourceReference": "partner-42",
  "payload": "external_id,name,type,latitude,longitude\\nP1,Musée National,PLACE,3.87,11.52"
}
```

Pour une source API, `sourceReference` contient l'URL et son hôte doit être
présent dans `INGESTION_API_ALLOWED_HOSTS`. Les tableaux JSON sont acceptés
directement ou sous la forme `{"data": [...]}`.

Variables principales :

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/yeyamo_ingestion
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=...
JWT_SECRET=...
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
INGESTION_API_ALLOWED_HOSTS=data.example.org,partner.example.org
```

### Implémentation de media-service

`media-service` gère désormais les originaux, les métadonnées et les
miniatures sans exposer le fournisseur de stockage au domaine.

Architecture :

- `ObjectStoragePort` isole le stockage objet ;
- `LocalObjectStorageAdapter` fournit un stockage atomique local pour le
  développement et bloque les traversées de chemins ;
- `ThumbnailStrategy` sélectionne le traitement image ou vidéo ;
- les images sont redimensionnées et converties en JPEG avec Java2D ;
- les vidéos utilisent FFmpeg avec un timeout configurable ;
- PostgreSQL conserve les métadonnées et le soft delete ;
- une outbox transactionnelle publie `media.uploaded`, `media.ready`,
  `media.thumbnail_failed` et `media.deleted` sur `media.events`.

Endpoints :

```text
POST   /api/v1/media
GET    /api/v1/media/{id}
GET    /api/v1/media/{id}/content
GET    /api/v1/media/{id}/thumbnail
DELETE /api/v1/media/{id}
GET    /v3/api-docs
GET    /swagger-ui.html
```

L'upload et la suppression exigent un JWT. La lecture d'un média au statut
`READY` est publique. La suppression est limitée au propriétaire, avec une
exception pour `ADMIN` et `SUPER_ADMIN`.

Formats acceptés : JPEG, PNG, WEBP, MP4, WEBM et MOV. Le service vérifie la
signature binaire en plus du type MIME, calcule un SHA-256 et refuse un doublon
actif pour le même propriétaire.

Variables principales :

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/yeyamo_media
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=...
JWT_SECRET=...
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
MEDIA_STORAGE_ROOT=./storage/media
FFMPEG_PATH=ffmpeg
```

Pour la production, remplacer l'adapter local par S3/MinIO sans modifier le
domaine ni les cas d'usage. Le stockage local ne doit pas être utilisé avec
plusieurs réplicas du service.

### Implémentation de content-service

`content-service` est désormais le propriétaire du cycle de vie des
publications sociales. Il conserve uniquement les identifiants des médias et
des actifs catalogue ; les données de `media-service` et `catalog-service`
ne sont ni copiées ni partagées.

Règles principales :

- une publication est créée au statut `DRAFT` ;
- seul un brouillon peut être édité ou publié ;
- la publication exige un texte ou au moins un média ;
- les visibilités disponibles sont `PUBLIC`, `FOLLOWERS` et `PRIVATE` ;
- les recherches anonymes retournent uniquement `PUBLISHED + PUBLIC` ;
- un post accepte au maximum 10 médias et 20 hashtags ;
- les hashtags sont normalisés en minuscules sans le caractère `#` ;
- la suppression est logique et conserve l'historique événementiel ;
- auteur, administrateur, super-administrateur ou modérateur peuvent appliquer
  les actions autorisées selon leur rôle.

Endpoints :

```text
POST   /api/v1/posts
PUT    /api/v1/posts/{id}
POST   /api/v1/posts/{id}/publish
PATCH  /api/v1/posts/{id}/visibility
POST   /api/v1/posts/{id}/archive
DELETE /api/v1/posts/{id}
GET    /api/v1/posts/{id}
GET    /api/v1/posts/me
GET    /api/v1/posts/me/{id}
GET    /api/v1/posts/hashtags/{tag}
GET    /api/v1/posts/catalog/{catalogAssetId}
GET    /v3/api-docs
GET    /swagger-ui.html
```

L'outbox publie les événements `content.post.created`,
`content.post.updated`, `content.post.published`,
`content.post.visibility_changed`, `content.post.archived` et
`content.post.deleted` sur `content.events`.

### Implémentation de interaction-service

`interaction-service` porte désormais les signaux sociaux demandés par les
architectures V1 et V2 sans recopier les publications ni le catalogue. Les
références `postId` et `catalogAssetId` restent des identifiants externes vers
`content-service` et `catalog-service`.

Architecture et garanties :

- architecture hexagonale avec domaine et ports indépendants de JPA, Redis et Kafka ;
- CQRS léger : `InteractionCommandService` sépare les mutations de
  `InteractionQueryService` et de ses projections de lecture ;
- likes et favoris uniques par couple utilisateur/publication ;
- commentaires hiérarchiques, modifiables par leur auteur ou un rôle de
  modération, avec suppression logique et verrouillage optimiste ;
- commandes idempotentes via `Idempotency-Key` et reçus PostgreSQL uniques ;
- compteurs lus en cache-aside Redis avec TTL et repli PostgreSQL si Redis est
  indisponible ;
- Outbox transactionnelle PostgreSQL puis publication asynchrone sur
  `interaction.events` ;
- JWT Resource Server stateless et documentation OpenAPI.

Endpoints :

```text
PUT    /api/v1/interactions/posts/{postId}/like
DELETE /api/v1/interactions/posts/{postId}/like
PUT    /api/v1/interactions/posts/{postId}/favorite
DELETE /api/v1/interactions/posts/{postId}/favorite
POST   /api/v1/interactions/posts/{postId}/comments
GET    /api/v1/interactions/posts/{postId}/comments
PUT    /api/v1/interactions/comments/{commentId}
DELETE /api/v1/interactions/comments/{commentId}
POST   /api/v1/interactions/posts/{postId}/shares
GET    /api/v1/interactions/posts/{postId}/summary
GET    /api/v1/saves
POST   /api/v1/checkins
GET    /api/v1/checkins/me
GET    /v3/api-docs
GET    /swagger-ui.html
```

Toutes les mutations exigent un JWT et l'en-tête `Idempotency-Key`. La lecture
des commentaires et des compteurs est publique ; les favoris et check-ins sont
privés. Les événements produits sont :

```text
interaction.like.added
interaction.like.removed
interaction.favorite.added
interaction.favorite.removed
interaction.comment.created
interaction.comment.updated
interaction.comment.deleted
interaction.post.shared
interaction.checkin.created
```

Variables principales :

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/yeyamo_interactions
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=...
JWT_SECRET=... # au moins 32 octets et identique à auth-service
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
REDIS_HOST=localhost
REDIS_PORT=6379
INTERACTION_EVENTS_TOPIC=interaction.events
INTERACTION_CACHE_TTL_SECONDS=60
```

### Implémentation de moderation-trust-service

`moderation-trust-service` devient le propriétaire V2 des signalements, des
décisions de modération et des scores de confiance. La modération historique
de `admin-service` peut être migrée progressivement par événements sans partage
de base de données.

Règles et architecture :

- architecture hexagonale avec agrégats `ModerationReport` et `TrustScore` ;
- cibles supportées : publication, commentaire, média, utilisateur, partenaire
  et actif catalogue ;
- raisons normalisées : spam, harcèlement, haine, violence, nudité, fraude,
  désinformation, copyright et autre ;
- workflow strict `OPEN → REVIEW → APPROVED|REJECTED` ;
- un seul dossier `OPEN/REVIEW` par signalant et cible ;
- score de confiance borné entre 0 et 100, initialisé à 50 ;
- un signalement approuvé diminue le score du propriétaire ciblé et valorise
  légèrement le signalant ; un signalement rejeté pénalise légèrement le
  signalant ;
- audit append-only : l'application expose uniquement l'ajout et PostgreSQL
  interdit physiquement les `UPDATE` et `DELETE` par trigger ;
- Outbox transactionnelle vers `moderation.events` ;
- consommateurs idempotents de `content.events`, `interaction.events` et
  `admin.events`, avec reçus PostgreSQL, trois retries et DLT ;
- JWT stateless avec rôles `ADMIN`, `SUPER_ADMIN` et `MODERATOR` pour la file
  de modération ;
- OpenAPI et Swagger UI.

Endpoints :

```text
POST /api/v1/moderation/reports
GET  /api/v1/moderation/reports/me
GET  /api/v1/moderation/reports
GET  /api/v1/moderation/reports/{id}
POST /api/v1/moderation/reports/{id}/review
POST /api/v1/moderation/reports/{id}/decision
GET  /api/v1/moderation/audit
GET  /api/v1/trust/{subjectId}
GET  /v3/api-docs
GET  /swagger-ui.html
```

Événements sortants :

```text
moderation.report.created
moderation.report.review_started
moderation.report.approved
moderation.report.rejected
trust.score.changed
```

Variables principales :

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/yeyamo_moderation
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=...
JWT_SECRET=... # au moins 32 octets
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
MODERATION_EVENTS_TOPIC=moderation.events
CONTENT_EVENTS_TOPIC=content.events
INTERACTION_EVENTS_TOPIC=interaction.events
ADMIN_EVENTS_TOPIC=admin.events
```

### Implémentation de feed-service

`feed-service` construit un fil personnalisé sans appel synchrone à
`content-service` ou `interaction-service`. Il matérialise localement les
événements nécessaires puis répond depuis PostgreSQL et Redis.

Architecture et comportement :

- CQRS léger avec `FeedProjectionCommandService` pour les événements et
  `FeedQueryService` pour les lectures ;
- trois projections matérialisées : publications, métriques d'engagement et
  signaux utilisateur/publication ;
- consommation idempotente de `content.events` et `interaction.events` avec
  reçus PostgreSQL, trois retries et DLT ;
- les métriques et signaux peuvent être reçus avant la publication : ils sont
  conservés puis automatiquement exploitables quand le contenu arrive ;
- seules les publications `PUBLISHED + PUBLIC` apparaissent dans le fil ;
- `PersonalizedRankingStrategy` combine fraîcheur exponentielle, engagement
  pondéré et affinité avec l'auteur ;
- l'interface `RankingStrategy` permet d'ajouter ultérieurement une stratégie
  exploration/diversité ou ML sans modifier le cas d'usage ;
- pagination bornée à 50 éléments et sélection d'un ensemble de candidats
  récent avant ranking ;
- cache-aside Redis avec TTL, repli PostgreSQL et invalidation O(1) par version
  globale ;
- Outbox transactionnelle vers `feed.events` ;
- JWT Resource Server et documentation OpenAPI.

Endpoint :

```text
GET /api/v1/feed?page=0&size=20
GET /v3/api-docs
GET /swagger-ui.html
```

Événements sortants :

```text
feed.post.projected
feed.metrics.updated
```

Variables principales :

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/yeyamo_feed
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=...
JWT_SECRET=... # au moins 32 octets
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
REDIS_HOST=localhost
REDIS_PORT=6379
CONTENT_EVENTS_TOPIC=content.events
INTERACTION_EVENTS_TOPIC=interaction.events
FEED_EVENTS_TOPIC=feed.events
FEED_CACHE_TTL_SECONDS=60
```

### Implémentation de notification-service

`notification-service` matérialise localement les notifications issues des
événements métier et ne dépend d'aucun appel synchrone vers les producteurs.

Architecture et fiabilité :

- architecture hexagonale avec ports de persistance, templates et canaux ;
- canaux `IN_APP`, `EMAIL` et `PUSH` sélectionnés par les préférences utilisateur ;
- templates PostgreSQL par type d'événement, canal et langue, avec fallback ;
- consommation de `auth.events`, `partner-events` et `moderation.events` ;
- idempotence persistante par `eventId` et contrainte source/destinataire ;
- retry Kafka avec backoff exponentiel et publication dans `<topic>.DLT` ;
- livraisons email/push persistées avec backoff exponentiel, nombre maximal de
  tentatives et état final `DEAD_LETTER` ;
- verrou pessimiste sur les livraisons pour empêcher deux instances de traiter
  simultanément la même tentative ;
- SMTP via `JavaMailSender` et push via un adapter HTTP configurable ;
- Flyway, JWT Resource Server, OpenAPI et route API Gateway.

Endpoints :

```text
GET  /api/v1/notifications?page=0&size=20
POST /api/v1/notifications/{id}/read
POST /api/v1/notifications/read-all
GET  /api/v1/notifications/preferences
PUT  /api/v1/notifications/preferences
GET  /v3/api-docs
GET  /swagger-ui.html
```

Variables principales :

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/yeyamo_notification
JWT_SECRET=... # au moins 32 octets
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
SMTP_HOST=localhost
SMTP_PORT=1025
NOTIFICATION_EMAIL_FROM=no-reply@yeyamo.app
PUSH_PROVIDER_ENDPOINT=https://provider.example/send
PUSH_PROVIDER_API_KEY=...
NOTIFICATION_MAX_ATTEMPTS=5
NOTIFICATION_BASE_DELAY_SECONDS=30
```

### Implémentation de recommendation-service

`recommendation-service` construit ses recommandations depuis ses propres
projections PostgreSQL, sans appel synchrone aux services producteurs.

Architecture et comportement :

- architecture hexagonale avec ports de projection, cache et Outbox ;
- consommation de `catalog.events`, `content.events`, `interaction.events`,
  `feed.events` et `user.events` ;
- compatibilité transitoire avec le topic historique `user-events` ;
- candidats unifiés pour destinations, lieux, expériences, événements et contenus ;
- quatre stratégies de scoring composables : popularité, proximité,
  préférences et historique ;
- détail des composantes du score retourné pour rendre le ranking explicable ;
- respect du consentement de partage de localisation avant tout calcul de proximité ;
- affinités de catégorie apprises depuis l'historique d'interaction ;
- conservation des signaux de popularité reçus avant le candidat ;
- consommateurs Kafka idempotents avec reçus PostgreSQL, trois retries et DLT ;
- cache-aside Redis versionné avec fallback PostgreSQL ;
- Outbox transactionnelle vers `recommendation.events` ;
- Flyway, JWT Resource Server, OpenAPI et route API Gateway.

Endpoint :

```text
GET /api/v1/recommendations?lat=4.05&lng=9.70&page=0&size=20
GET /v3/api-docs
GET /swagger-ui.html
```

Variables principales :

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/yeyamo_recommendation
JWT_SECRET=... # au moins 32 octets
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
REDIS_HOST=localhost
REDIS_PORT=6379
CATALOG_EVENTS_TOPIC=catalog.events
CONTENT_EVENTS_TOPIC=content.events
INTERACTION_EVENTS_TOPIC=interaction.events
FEED_EVENTS_TOPIC=feed.events
USER_V2_EVENTS_TOPIC=user.events
RECOMMENDATION_EVENTS_TOPIC=recommendation.events
RECOMMENDATION_CACHE_TTL_SECONDS=120
```

## Implémentation de `gamification-service` — 14 juillet 2026

Le service de gamification est désormais **prêt V1** sur le port `8096`. Il
utilise une architecture hexagonale : le domaine de progression ne dépend ni
de Spring, ni de PostgreSQL, ni de Kafka. Les adapters JPA, Redis, Kafka et HTTP
implémentent les ports définis par la couche application.

Fonctionnalités livrées :

- ledger XP append-only, protégé dans PostgreSQL contre les mises à jour et les
  suppressions par trigger ;
- calcul déterministe des niveaux et du prochain seuil XP ;
- badges attribués par règles indépendantes (`Strategy`) ;
- séries journalières calculées en UTC ;
- passeport voyageur alimenté par les check-ins, avec unicité par lieu ;
- récompenses de niveau et de badge, réclamables une seule fois ;
- lectures mises en cache dans Redis selon le pattern cache-aside ;
- publication fiable sur `gamification.events` par Transactional Outbox ;
- consommation idempotente de `user.events`, `user-events`, `content.events`,
  `interaction.events` et `booking.events`, avec retry puis DLT ;
- sécurité JWT Resource Server, OpenAPI et migrations Flyway.

Les événements reconnus dans cette première version sont `profile.created`,
`content.post.published`, `interaction.like.added`,
`interaction.favorite.added`, `interaction.comment.created`,
`interaction.post.shared`, `interaction.checkin.created`,
`booking.confirmed` et `booking.completed`. Pour une réservation, la clé
fonctionnelle empêche `confirmed` et `completed` de créditer deux fois le même
gain.

Endpoints authentifiés :

```text
GET  /api/v1/me/xp
GET  /api/v1/me/badges
GET  /api/v1/me/passport
GET  /api/v1/me/streaks
GET  /api/v1/me/rewards
POST /api/v1/me/rewards/{rewardId}/claim
GET  /v3/api-docs
GET  /swagger-ui.html
```

Variables principales :

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/yeyamo_gamification
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
JWT_SECRET=... # au moins 32 octets, identique à l'émetteur des JWT
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
REDIS_HOST=localhost
REDIS_PORT=6379
GAMIFICATION_EVENTS_TOPIC=gamification.events
GAMIFICATION_CACHE_TTL_SECONDS=120
```

La suite automatisée contient 14 tests et couvre le calcul de progression, les
règles de badges, l'orchestration métier, la politique XP, l'idempotence du
consumer et le démarrage du contexte Spring. Elle passe sous Java 21. Les
intégrations réelles PostgreSQL/Kafka/Redis restent à valider dans
l'environnement d'exécution. `booking-service` étant encore un squelette, le
consumer est prêt pour ses contrats V2, mais aucun producteur de réservation
n'est encore disponible dans le dépôt.

## Implémentation de `mission-reward-service` — 14 juillet 2026

Le service est désormais **prêt V1** sur le port `8098`. Il gère le cycle de
vie des définitions de mission (`DRAFT`, `ACTIVE`, `PAUSED`, `ENDED`),
l'inscription automatique d'un utilisateur au premier événement éligible, la
progression de chaque objectif, la validation automatique de la mission et
l'attribution d'une récompense.

Le moteur de règles utilise une Strategy déterministe par métrique :

- `COUNT` compte les événements correspondants ;
- `SUM` additionne un champ numérique du payload, par exemple `points` ;
- `MAX` conserve la plus grande valeur observée, par exemple `level` ;
- `ruleKey` et `ruleValue` permettent de filtrer un objectif sur une propriété
  métier sans coder une classe spécifique pour chaque mission.

La saga légère de récompense suit les états `PENDING`, `GRANTED` et `FAILED`.
La complétion, la création du grant et les messages Outbox sont enregistrés
atomiquement. Après publication réussie de `mission.reward.granted`, le grant
et la mission utilisateur passent à l'état final. Après dix échecs de
publication, le grant est marqué `FAILED` en conservant la cause. Les
publications restent rejouables et les consumers utilisent une table de reçus
persistante pour l'idempotence.

Événements consommés :

```text
gamification.events
interaction.events
```

Les objectifs peuvent notamment cibler `gamification.xp.awarded`,
`gamification.level.changed`, `gamification.badge.earned`,
`interaction.like.added`, `interaction.favorite.added`,
`interaction.comment.created`, `interaction.post.shared` et
`interaction.checkin.created`. Le service publie ses événements versionnés sur
`mission.events`, notamment `mission.created`, `mission.activated`,
`mission.completed` et `mission.reward.granted`.

Endpoints JWT :

```text
GET  /api/v1/missions
GET  /api/v1/me/missions
GET  /api/v1/me/mission-rewards
POST /api/v1/mission-management/missions
POST /api/v1/mission-management/missions/{id}/activate
POST /api/v1/mission-management/missions/{id}/pause
GET  /v3/api-docs
GET  /swagger-ui.html
```

Les routes `/api/v1/mission-management/**` exigent le rôle `ADMIN`. Cette
frontière évite également tout conflit avec `/api/v1/admin/**`, déjà attribué à
`admin-service` dans la Gateway.

Variables principales :

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/yeyamo_missions
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
JWT_SECRET=... # au moins 32 octets, identique à l'émetteur des JWT
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
GAMIFICATION_EVENTS_TOPIC=gamification.events
INTERACTION_EVENTS_TOPIC=interaction.events
MISSION_EVENTS_TOPIC=mission.events
MISSION_OUTBOX_DELAY_MS=1000
```

La suite contient 14 tests : moteur de règles, filtres, orchestration de
mission, complétion automatique, démarrage de saga, mapping des contrats,
consumer idempotent et chargement complet du contexte Spring. Tous passent
sous Java 21. Les tests automatisés utilisent H2 ou des doubles ; une
validation d'intégration avec PostgreSQL et Kafka réels reste nécessaire avant
la production.

## Conclusion

YeYamo possède un socle microservices opérationnel et quatre domaines métier
V1 cohérents : auth, profils utilisateur, partenaires et administration. La
V2 dispose maintenant d'un noyau métier concret pour le catalogue, l'ingestion,
les médias, le contenu et les interactions. Les autres modules V2 restent à
implémenter ou à durcir selon leur état indiqué plus haut.

La priorité est de stabiliser les contrats transverses, puis d'implémenter
`catalog-service` comme source de vérité en faisant converger
`place-service`. L'ordre de construction à retenir est ensuite ingestion,
media, content, interaction, moderation-trust, feed et discovery. Les domaines
de recommandation, progression, transaction et analytics arrivent après, car
ils dépendent des événements produits par ce noyau.
