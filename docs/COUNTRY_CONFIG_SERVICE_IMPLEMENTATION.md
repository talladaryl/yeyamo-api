# Country Configuration Service - Documentation d'Implémentation

## 📋 Vue d'ensemble

Le **Country Configuration Service** est un microservice stratégique qui centralise toute la configuration multi-pays, multi-langues et multi-devises de la plateforme YeYamo.

### Objectifs Principaux

1. ✅ **Source de vérité unique** pour les configurations pays
2. ✅ **Stratégie Cameroon-first** : Cameroun LIVE au lancement
3. ✅ **Multi-country awareness** : Tous les services peuvent interroger la configuration
4. ✅ **Lancement progressif** : Contrôle granulaire par statut (DISABLED, COMING_SOON, BETA, LIVE)
5. ✅ **Feature flags** : Activation/désactivation par fonctionnalité et par pays
6. ✅ **Standards ISO** : Respect des normes internationales

---

## 🏗️ Architecture

### Stack Technique

- **Java 21** avec Spring Boot 4.1.0
- **PostgreSQL** pour la persistance
- **Redis** pour le cache (invalidation automatique)
- **Kafka** pour les événements inter-services
- **Flyway** pour les migrations de schéma
- **Spring Cloud** pour la découverte de services (Eureka)
- **OAuth2 JWT** pour la sécurité

### Structure du Service

```
country-config-service/
├── domain/
│   ├── model/
│   │   ├── Country.java                 # Entité principale
│   │   ├── CountryLaunchStatus.java     # Enum: DISABLED, COMING_SOON, BETA, LIVE
│   │   ├── CountryLanguage.java         # Langues par pays
│   │   ├── CountryCurrency.java         # Devises par pays
│   │   └── CountryTimezone.java         # Fuseaux horaires
│   └── repository/                      # Repositories JPA
├── service/
│   └── CountryConfigService.java        # Logique métier + cache
├── controller/
│   ├── CountryController.java           # API publique (lecture)
│   └── CountryAdminController.java      # API admin (écriture)
├── dto/                                 # DTOs pour les API
├── event/
│   └── CountryEventPublisher.java       # Publication Kafka
├── config/
│   ├── SecurityConfig.java              # OAuth2 + RBAC
│   └── CacheConfig.java                 # Redis TTL
└── mapper/
    └── CountryMapper.java               # Conversion entité ↔ DTO
```

---

## 📊 Modèle de Données

### Table `countries`

| Colonne | Type | Description |
|---------|------|-------------|
| `id` | UUID | Identifiant unique |
| `code` | VARCHAR(2) | Code ISO 3166-1 alpha-2 (CM, NG...) |
| `name` | VARCHAR(100) | Nom commun (Cameroon) |
| `official_name` | VARCHAR(200) | Nom officiel (Republic of Cameroon) |
| `continent_code` | VARCHAR(2) | Continent (AF, EU...) |
| `default_language_code` | VARCHAR(10) | Langue par défaut (ISO 639 / BCP 47) |
| `default_currency_code` | VARCHAR(3) | Devise par défaut (ISO 4217) |
| `default_timezone` | VARCHAR(50) | Fuseau horaire (IANA) |
| `phone_country_code` | VARCHAR(5) | Indicatif téléphonique (E.164) |
| `launch_status` | VARCHAR(20) | Statut: DISABLED, COMING_SOON, BETA, LIVE |
| **Feature Flags** | | |
| `registration_enabled` | BOOLEAN | Inscription utilisateurs |
| `content_publishing_enabled` | BOOLEAN | Publication de contenu |
| `partner_onboarding_enabled` | BOOLEAN | Intégration partenaires |
| `payments_enabled` | BOOLEAN | Paiements |
| `booking_enabled` | BOOLEAN | Réservations |
| `ticketing_enabled` | BOOLEAN | Billetterie |
| `artisan_commerce_enabled` | BOOLEAN | Commerce artisanal |
| `culture_module_enabled` | BOOLEAN | Module culturel |
| `version` | BIGINT | Optimistic locking |

### Tables Associées

- **`country_languages`** : Liste des langues par pays (français, anglais pour CM)
- **`country_currencies`** : Devises acceptées (XAF pour CM)
- **`country_timezones`** : Fuseaux horaires (Africa/Douala pour CM)

---

## 🌍 Données Initiales (Seed)

### Cameroun - LIVE ✅

```sql
INSERT INTO countries (code, name, launch_status, ...) VALUES
('CM', 'Cameroon', 'LIVE', ...);

-- Toutes les fonctionnalités activées
registration_enabled = true
content_publishing_enabled = true
partner_onboarding_enabled = true
payments_enabled = true
booking_enabled = true
ticketing_enabled = true
artisan_commerce_enabled = true
culture_module_enabled = true
```

**Langues** : Français (défaut), English  
**Devise** : XAF (FCFA, 0 décimales)  
**Timezone** : Africa/Douala

### Autres Pays Africains - COMING_SOON ⏳

35+ pays africains pré-configurés avec statut `COMING_SOON` :
- Nigeria (NG), Ghana (GH), Kenya (KE), South Africa (ZA)
- Sénégal (SN), Côte d'Ivoire (CI), Rwanda (RW)...

**Toutes les fonctionnalités désactivées par défaut.**

---

## 🔌 API Endpoints

### API Publique (Sans Authentification)

#### `GET /api/v1/countries`
Retourne tous les pays (y compris COMING_SOON).

#### `GET /api/v1/countries/available`
Retourne uniquement les pays **LIVE** ou **BETA**.

```json
[
  {
    "code": "CM",
    "name": "Cameroon",
    "launchStatus": "LIVE",
    "registrationEnabled": true,
    "paymentsEnabled": true
  }
]
```

#### `GET /api/v1/countries/{code}`
Détails d'un pays spécifique.

```bash
curl http://localhost:8090/api/v1/countries/CM
```

#### `GET /api/v1/countries/{code}/configuration`
Configuration complète (pays + langues + devises + timezones).

#### `GET /api/v1/countries/{code}/features`
Feature flags uniquement.

```json
{
  "registrationEnabled": true,
  "contentPublishingEnabled": true,
  "paymentsEnabled": true,
  "bookingEnabled": true,
  "ticketingEnabled": true,
  "artisanCommerceEnabled": true,
  "cultureModuleEnabled": true
}
```

#### `GET /api/v1/countries/{code}/languages`
Liste des langues supportées.

#### `GET /api/v1/countries/{code}/currencies`
Liste des devises acceptées.

#### `GET /api/v1/countries/{code}/timezones`
Liste des fuseaux horaires.

---

### API Admin (Authentification JWT Requise)

#### `GET /api/v1/admin/countries` [ADMIN]
Vue administrateur de tous les pays.

#### `PUT /api/v1/admin/countries/{code}` [ADMIN]
Mise à jour de la configuration pays (nom, devise, timezone...).

```bash
curl -X PUT http://localhost:8090/api/v1/admin/countries/CM \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Cameroon",
    "officialName": "Republic of Cameroon",
    "defaultLanguageCode": "fr",
    "defaultCurrencyCode": "XAF",
    "defaultTimezone": "Africa/Douala",
    "phoneCountryCode": "+237"
  }'
```

#### `PATCH /api/v1/admin/countries/{code}/launch-status` [SUPER_ADMIN]
**Opération critique** : Changement du statut de lancement.

```bash
curl -X PATCH http://localhost:8090/api/v1/admin/countries/NG/launch-status \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"launchStatus": "BETA"}'
```

#### `PATCH /api/v1/admin/countries/{code}/features` [ADMIN]
Activation/désactivation des fonctionnalités.

```bash
curl -X PATCH http://localhost:8090/api/v1/admin/countries/CM/features \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "registrationEnabled": true,
    "paymentsEnabled": true,
    "ticketingEnabled": true
  }'
```

---

## 📡 Événements Kafka

### Topic: `country-events`

Le service publie 4 types d'événements :

#### 1. CountryCreated
Émis lors de la création d'un nouveau pays.

#### 2. CountryConfigurationUpdated
Émis lors de la modification des paramètres de base (nom, devise, etc.).

```json
{
  "eventId": "uuid",
  "eventType": "CountryConfigurationUpdated",
  "timestamp": "2026-08-13T10:00:00Z",
  "countryCode": "CM",
  "defaultCurrencyCode": "XAF",
  "defaultLanguageCode": "fr"
}
```

#### 3. CountryLaunchStatusChanged
Émis lors d'un changement de statut (ex: COMING_SOON → BETA).

```json
{
  "eventId": "uuid",
  "eventType": "CountryLaunchStatusChanged",
  "countryCode": "NG",
  "oldStatus": "COMING_SOON",
  "newStatus": "BETA"
}
```

#### 4. CountryFeatureChanged
Émis lors de l'activation/désactivation de fonctionnalités.

```json
{
  "eventId": "uuid",
  "eventType": "CountryFeatureChanged",
  "countryCode": "CM",
  "features": {
    "paymentsEnabled": true,
    "ticketingEnabled": true
  }
}
```

### Consommation par d'autres services

Les autres microservices peuvent s'abonner pour :
- Invalider leurs propres caches
- Adapter leurs fonctionnalités selon le pays
- Logger les changements critiques

---

## 💾 Cache Redis

### Stratégie de Cache

| Cache | TTL | Invalidation |
|-------|-----|--------------|
| `countries` (liste) | 1 heure | Automatique sur UPDATE |
| `country:{code}` | 1 heure | Automatique sur UPDATE |
| `country-config:{code}` | 1 heure | Automatique sur UPDATE |
| `country-features:{code}` | 30 minutes | Automatique sur UPDATE |

### Configuration

```java
@Cacheable(value = "country", key = "#code")
public CountryDto getCountryByCode(String code) { ... }

@CacheEvict(value = {"country", "countries"}, allEntries = true)
public CountryDto updateCountry(String code, ...) { ... }
```

---

## 🔐 Sécurité

### Endpoints Publics
- Lecture seule sur `/api/v1/countries/**`
- Pas d'authentification requise
- Idéal pour sélecteurs de pays dans les apps mobiles

### Endpoints Admin
- JWT OAuth2 requis
- Rôle **ADMIN** pour les modifications standards
- Rôle **SUPER_ADMIN** pour les changements de `launch_status`

### Optimistic Locking
- Champ `version` sur l'entité `Country`
- Empêche les mises à jour concurrentes

---

## 🧪 Tests

### Tests Unitaires
```bash
./mvnw test -Dtest=CountryConfigServiceTest
```

Scénarios testés :
- ✅ Cameroun est LIVE avec toutes les fonctionnalités
- ✅ Nigeria est COMING_SOON avec fonctionnalités désactivées
- ✅ Mise à jour du statut de lancement
- ✅ Changement de feature flags
- ✅ Gestion des exceptions (pays introuvable)
- ✅ Optimistic locking

### Tests d'Intégration
```bash
./mvnw test -Dtest=CountryControllerIntegrationTest
```

Endpoints testés :
- ✅ GET /api/v1/countries
- ✅ GET /api/v1/countries/available (retourne uniquement CM)
- ✅ GET /api/v1/countries/CM (200 OK)
- ✅ GET /api/v1/countries/XX (404 Not Found)
- ✅ GET /api/v1/countries/CM/features

---

## 🚀 Déploiement

### Configuration Docker

```dockerfile
FROM eclipse-temurin:21-jre-alpine
EXPOSE 8090
HEALTHCHECK http://localhost:8090/actuator/health
```

### Variables d'Environnement

```bash
DB_HOST=postgres
DB_PORT=5432
DB_USERNAME=postgres
DB_PASSWORD=<secret>

REDIS_HOST=redis
REDIS_PORT=6379

KAFKA_BOOTSTRAP_SERVERS=kafka:9092

JWT_ISSUER_URI=http://keycloak:8080/auth/realms/yeyamo
EUREKA_SERVER=http://registry-service:8761/eureka/
```

### Docker Compose

```yaml
country-config-service:
  image: yeyamo/country-config-service:latest
  ports:
    - "8090:8090"
  environment:
    - DB_HOST=postgres
    - REDIS_HOST=redis
    - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
  depends_on:
    - postgres
    - redis
    - kafka
    - registry-service
```

---

## 🔗 Intégration avec les Autres Services

### Services Consommateurs

| Service | Usage |
|---------|-------|
| **user-service** | Vérifier si `registrationEnabled` avant inscription |
| **payment-service** | Vérifier si `paymentsEnabled` + devise par défaut |
| **booking-service** | Vérifier si `bookingEnabled` |
| **ticket-service** | Vérifier si `ticketingEnabled` |
| **commerce-service** | Vérifier si `artisanCommerceEnabled` |
| **culture-service** | Vérifier si `cultureModuleEnabled` |
| **partner-service** | Vérifier si `partnerOnboardingEnabled` |
| **content-service** | Vérifier si `contentPublishingEnabled` |
| **notification-service** | Adapter langues et fuseaux horaires |

### Exemple d'Utilisation par un Service

```java
// Dans user-service
@Service
public class RegistrationService {
    
    private final RestTemplate restTemplate;
    
    public void registerUser(UserRegistrationRequest request) {
        // Appel au country-config-service
        CountryDto country = restTemplate.getForObject(
            "http://country-config-service/api/v1/countries/" + request.countryCode(),
            CountryDto.class
        );
        
        // Vérification
        if (!country.registrationEnabled()) {
            throw new RegistrationNotAvailableException(
                "Registration is not available in " + country.name()
            );
        }
        
        // Continuer l'inscription...
    }
}
```

---

## 📈 Monitoring

### Métriques Prometheus

```
http://localhost:8090/actuator/prometheus
```

Métriques personnalisées :
- `country_config_cache_hit_ratio`
- `country_config_update_total`
- `country_launch_status_change_total`

### Health Checks

```bash
curl http://localhost:8090/actuator/health
```

Retour :
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

### Logs Structurés

```
2026-08-13 10:00:00 INFO  CountryConfigService - Updating launch status for country CM to LIVE
2026-08-13 10:00:01 INFO  CountryEventPublisher - Published CountryLaunchStatusChanged event for country: CM
```

---

## 🗺️ Roadmap Futures Évolutions

### Phase 2 - Divisions Administratives
- [ ] Régions, départements, villes
- [ ] Support multi-niveaux (ex: Cameroun → Littoral → Douala)

### Phase 3 - Business Rules
- [ ] Providers de paiement par pays (MTN Mobile Money, Orange Money...)
- [ ] Exigences KYC spécifiques
- [ ] Règles fiscales (TVA, taxes locales)
- [ ] Jours fériés et calendriers culturels

### Phase 4 - Preferences Culturelles
- [ ] Formats de date/heure préférés
- [ ] Systèmes de mesure (métrique vs impérial)
- [ ] Symboles de devise et position

### Phase 5 - Advanced Features
- [ ] API GraphQL en plus de REST
- [ ] Webhooks pour notifications de changements
- [ ] Versioning des configurations
- [ ] Historique des modifications

---

## 📚 Standards Utilisés

| Standard | Application | Exemple |
|----------|-------------|---------|
| **ISO 3166-1 alpha-2** | Codes pays | CM (Cameroon), NG (Nigeria) |
| **ISO 4217** | Codes devises | XAF, NGN, USD |
| **ISO 639-1 / BCP 47** | Codes langues | fr, en, fr-CM |
| **IANA** | Fuseaux horaires | Africa/Douala, Africa/Lagos |
| **E.164** | Indicatifs téléphoniques | +237, +234 |

---

## ✅ Checklist de Livraison

- [x] Entités JPA avec validation
- [x] Repositories Spring Data
- [x] Service métier avec cache Redis
- [x] API REST publique (lecture)
- [x] API REST admin (écriture)
- [x] Publication d'événements Kafka
- [x] Sécurité OAuth2 + RBAC
- [x] Migrations Flyway (V1: schéma, V2: seed Cameroun + Afrique)
- [x] Tests unitaires
- [x] Tests d'intégration
- [x] Configuration Spring Cloud Config
- [x] Dockerfile multi-stage
- [x] Route API Gateway
- [x] Documentation README
- [x] Documentation OpenAPI/Swagger
- [x] Health checks Actuator
- [x] Métriques Prometheus

---

## 📞 Support

Pour toute question sur le country-config-service :
- **Architecture** : Équipe Backend Architecture
- **Déploiement** : Équipe DevOps
- **Features** : Product Management

---

**Version** : 1.0.0  
**Date** : 13 août 2026  
**Auteur** : YeYamo Backend Team
