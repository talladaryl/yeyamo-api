# Country Configuration Service

## Vue d'ensemble

Service de configuration centralisé pour le support multi-pays, multi-langues et multi-devises dans la plateforme YeYamo.

**Stratégie Cameroon-first** : Le Cameroun est le seul pays LIVE au lancement. Tous les autres pays africains peuvent exister dans le référentiel avec le statut `COMING_SOON` ou `DISABLED`.

## Fonctionnalités

### Architecture Multi-Pays
- **Source de vérité unique** pour toutes les configurations pays
- **Standards ISO** : ISO 3166-1 (pays), ISO 4217 (devises), ISO 639/BCP 47 (langues)
- **Gestion du lancement progressif** via statuts : DISABLED, COMING_SOON, BETA, LIVE
- **Feature flags granulaires** par pays

### Données Gérées
- Pays et divisions administratives
- Langues par pays (multi-langues)
- Devises acceptées (multi-devises)
- Fuseaux horaires
- Indicatifs téléphoniques (E.164)
- Statuts de lancement
- Feature flags par pays

### Features Contrôlables
- Registration (inscription utilisateurs)
- Content Publishing (publication de contenu)
- Partner Onboarding (intégration partenaires)
- Payments (paiements)
- Booking (réservations)
- Ticketing (billetterie)
- Artisan Commerce (commerce artisanal)
- Culture Module (module culturel)

## API Publique

### Endpoints sans authentification

```http
GET /api/v1/countries
GET /api/v1/countries/available
GET /api/v1/countries/{code}
GET /api/v1/countries/{code}/configuration
GET /api/v1/countries/{code}/features
GET /api/v1/countries/{code}/languages
GET /api/v1/countries/{code}/currencies
GET /api/v1/countries/{code}/timezones
```

### Exemples

#### Obtenir tous les pays disponibles (LIVE ou BETA)
```bash
curl http://localhost:8090/api/v1/countries/available
```

#### Obtenir la configuration complète du Cameroun
```bash
curl http://localhost:8090/api/v1/countries/CM/configuration
```

Réponse :
```json
{
  "country": {
    "code": "CM",
    "name": "Cameroon",
    "officialName": "Republic of Cameroon",
    "continentCode": "AF",
    "defaultLanguageCode": "fr",
    "defaultCurrencyCode": "XAF",
    "defaultTimezone": "Africa/Douala",
    "phoneCountryCode": "+237",
    "launchStatus": "LIVE",
    "registrationEnabled": true,
    "paymentsEnabled": true
  },
  "languages": [
    {
      "languageCode": "fr",
      "name": "Français",
      "isDefault": true,
      "displayOrder": 1
    },
    {
      "languageCode": "en",
      "name": "English",
      "isDefault": false,
      "displayOrder": 2
    }
  ],
  "currencies": [
    {
      "currencyCode": "XAF",
      "name": "Central African CFA franc",
      "symbol": "FCFA",
      "decimalPlaces": 0,
      "isDefault": true
    }
  ],
  "timezones": [
    {
      "timezone": "Africa/Douala",
      "displayName": "West Africa Time (WAT)",
      "isDefault": true
    }
  ]
}
```

## API Admin

### Endpoints avec authentification (JWT + Rôles)

```http
GET    /api/v1/admin/countries           [ADMIN]
GET    /api/v1/admin/countries/{code}    [ADMIN]
PUT    /api/v1/admin/countries/{code}    [ADMIN]
PATCH  /api/v1/admin/countries/{code}/launch-status    [SUPER_ADMIN]
PATCH  /api/v1/admin/countries/{code}/features         [ADMIN]
```

### Exemples Admin

#### Mettre à jour le statut de lancement (SUPER_ADMIN uniquement)
```bash
curl -X PATCH http://localhost:8090/api/v1/admin/countries/NG/launch-status \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "launchStatus": "BETA"
  }'
```

#### Activer des fonctionnalités
```bash
curl -X PATCH http://localhost:8090/api/v1/admin/countries/CM/features \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "registrationEnabled": true,
    "contentPublishingEnabled": true,
    "partnerOnboardingEnabled": true,
    "paymentsEnabled": true,
    "bookingEnabled": true,
    "ticketingEnabled": true,
    "artisanCommerceEnabled": true,
    "cultureModuleEnabled": true
  }'
```

## Événements Kafka

Le service publie des événements sur le topic `country-events` :

- `CountryCreated`
- `CountryConfigurationUpdated`
- `CountryLaunchStatusChanged`
- `CountryFeatureChanged`

Les autres microservices peuvent s'abonner pour rester synchronisés.

## Cache Redis

Les données pays sont mises en cache avec différents TTL :
- **Listes de pays** : 1 heure
- **Pays individuel** : 1 heure
- **Configuration complète** : 1 heure
- **Feature flags** : 30 minutes

Le cache est invalidé automatiquement lors des mises à jour.

## Base de Données

### Schema PostgreSQL

```
countries
├── country_languages
├── country_currencies
└── country_timezones
```

### Migrations Flyway

- `V1__create_country_schema.sql` : Tables de base
- `V2__seed_cameroon_and_african_countries.sql` : Données initiales (Cameroun LIVE + pays africains COMING_SOON)

## Configuration

### Variables d'environnement

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# OAuth2
JWT_ISSUER_URI=http://localhost:8080/auth/realms/yeyamo

# Eureka
EUREKA_SERVER=http://localhost:8761/eureka/
```

## Démarrage Local

### Prérequis
- Java 21
- PostgreSQL 15+
- Redis 7+
- Kafka 3+

### Démarrage

```bash
# Créer la base de données
psql -U postgres -c "CREATE DATABASE yeyamo_country_config;"

# Démarrer le service
./mvnw spring-boot:run
```

### Accès

- **API** : http://localhost:8090
- **Swagger UI** : http://localhost:8090/swagger-ui.html
- **Actuator** : http://localhost:8090/actuator/health

## Docker

```bash
# Build
docker build -t yeyamo/country-config-service:latest .

# Run
docker run -p 8090:8090 \
  -e DB_HOST=postgres \
  -e REDIS_HOST=redis \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  yeyamo/country-config-service:latest
```

## Tests

```bash
# Tests unitaires et intégration
./mvnw test

# Coverage
./mvnw verify
```

## Sécurité

- **Public API** : Pas d'authentification requise (lecture seule)
- **Admin API** : JWT OAuth2 + rôles (ADMIN, SUPER_ADMIN)
- **Launch Status** : Changement réservé aux SUPER_ADMIN
- **Optimistic Locking** : Protection contre les mises à jour concurrentes

## Standards Utilisés

| Standard | Usage |
|----------|-------|
| ISO 3166-1 alpha-2 | Codes pays (CM, NG, etc.) |
| ISO 4217 | Codes devises (XAF, NGN, etc.) |
| ISO 639-1 / BCP 47 | Codes langues (fr, en, fr-CM) |
| IANA Timezone | Fuseaux horaires (Africa/Douala) |
| E.164 | Indicatifs téléphoniques (+237) |

## Monitoring

### Métriques Prometheus

```
http://localhost:8090/actuator/prometheus
```

### Health Checks

```bash
curl http://localhost:8090/actuator/health
```

## Architecture

Ce service suit **Domain-Driven Design** avec une architecture hexagonale :

```
domain/
  model/         # Entités métier
  repository/    # Ports de sortie
service/         # Logique métier
controller/      # API REST
dto/             # Objets de transfert
event/           # Publication Kafka
config/          # Configuration Spring
```

## Roadmap

- [ ] Support des divisions administratives (régions, départements)
- [ ] Providers de paiement par pays
- [ ] Exigences KYC par pays
- [ ] Règles commerciales configurables
- [ ] Options culturelles et préférences
- [ ] API GraphQL
- [ ] Webhooks pour les changements de configuration

## License

Proprietary - YeYamo Platform
