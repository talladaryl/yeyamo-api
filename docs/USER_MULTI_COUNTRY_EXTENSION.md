# User Multi-Country Extension - Architecture Document

## 📋 Vue d'ensemble

Extension de **auth-service** et **user-service** pour supporter une gestion multi-pays complète avec localisation géographique, préférences linguistiques, et validation pays.

### Objectifs

✅ Chaque utilisateur a un **pays principal** et une **localisation précise**  
✅ Validation pays lors de l'inscription (LIVE, BETA, registrationEnabled)  
✅ Préférences linguistiques et monétaires par pays  
✅ Préférences de découverte de contenu (multi-pays, multi-langues)  
✅ Support complet du format E.164 pour téléphones  
✅ Migration des utilisateurs existants vers CM (Cameroun)  

---

## 🏗️ Architecture

### 1. Modifications auth-service

#### Extension POST /api/v1/auth/register

**Ancienne structure:**
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "phone": "string"
}
```

**Nouvelle structure:**
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "countryCode": "CM",              // ✅ NOUVEAU: Pays (ISO 3166-1 alpha-2)
  "cityId": "uuid",                  // ✅ NOUVEAU: Ville (nullable)
  "preferredLanguageCode": "fr",     // ✅ NOUVEAU: Langue préférée
  "timezone": "Africa/Douala",       // ✅ NOUVEAU: Fuseau horaire (nullable)
  "phone": "+237691234567"           // ✅ NOUVEAU: Format E.164 obligatoire
}
```

#### Validation Pays

Avant inscription, vérifier via **country-config-service**:

1. ✅ Le pays existe (`countryRepository.existsByCode()`)
2. ✅ `registrationEnabled = true`
3. ✅ `launchStatus IN (LIVE, BETA)` ou configuration spéciale

**Règles:**
- Pays `LIVE` → Inscription ouverte si `registrationEnabled = true`
- Pays `BETA` → Inscription possible avec invitation ou whitelist
- Pays `COMING_SOON` → **Inscription bloquée** (sauf admin override)
- Pays `DISABLED` → **Inscription bloquée**

```java
// Pseudo-code de validation
CountryDto country = countryConfigClient.getCountry(request.countryCode());

if (!country.registrationEnabled()) {
    throw new RegistrationNotAllowedException(
        "Registration is not available in " + country.name()
    );
}

if (country.launchStatus() == CountryLaunchStatus.COMING_SOON) {
    throw new CountryNotLiveException(
        "This country is not yet available"
    );
}
```

---

### 2. Modifications user-service

#### Extension du Modèle User

**Nouveaux champs:**

```java
@Entity
@Table(name = "users")
public class User {
    // ... champs existants ...
    
    // ===== Localisation Géographique =====
    @NotBlank
    @Size(min = 2, max = 2)
    @Column(nullable = false, length = 2)
    private String countryCode;
    
    @Column
    private UUID adminLevel1Id;  // Région/State/Province
    
    @Column
    private UUID adminLevel2Id;  // Département/LGA/District
    
    @Column
    private UUID cityId;
    
    @Column
    private UUID localityId;  // Quartier/Ward/Village
    
    // ===== Préférences Linguistiques =====
    @NotBlank
    @Size(max = 10)
    @Column(nullable = false, length = 10)
    private String preferredLanguageCode;  // ISO 639 / BCP 47
    
    @Size(max = 50)
    @Column(length = 50)
    private String timezone;  // IANA timezone
    
    // ===== Préférences Monétaires =====
    @Size(min = 3, max = 3)
    @Column(length = 3)
    private String preferredCurrencyCode;  // ISO 4217
    
    // ===== Préférences de Contenu =====
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Set<String> contentCountries = new HashSet<>();  // Pays à découvrir
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Set<String> contentLanguages = new HashSet<>();  // Langues de contenu
    
    @Column
    private Integer discoveryRadiusKm;  // Rayon de découverte local
    
    @Column(nullable = false)
    private Boolean panAfricanContent = true;  // Contenus africains
}
```

---

## 🔌 API Endpoints

### Auth Service

#### POST /api/v1/auth/register

Enregistrement avec validation pays.

**Request:**
```json
{
  "username": "john_douala",
  "email": "john@example.cm",
  "password": "SecurePass123!",
  "countryCode": "CM",
  "cityId": "douala-uuid",
  "preferredLanguageCode": "fr",
  "timezone": "Africa/Douala",
  "phone": "+237691234567"
}
```

**Response:**
```json
{
  "userId": "uuid",
  "username": "john_douala",
  "email": "john@example.cm",
  "countryCode": "CM",
  "preferredLanguageCode": "fr",
  "accessToken": "jwt-token",
  "refreshToken": "refresh-token"
}
```

**Erreurs:**
```json
// 400 - Pays non disponible
{
  "status": 400,
  "title": "Registration Not Allowed",
  "detail": "Registration is not available in Nigeria"
}

// 400 - Pays COMING_SOON
{
  "status": 400,
  "title": "Country Not Live",
  "detail": "This country is not yet available"
}

// 400 - Téléphone invalide
{
  "status": 400,
  "title": "Invalid Phone Format",
  "detail": "Phone must be in E.164 format (+237XXXXXXXXX)"
}
```

---

### User Service

#### GET /api/v1/users/me

Profil utilisateur avec localisation complète.

**Response:**
```json
{
  "id": "uuid",
  "username": "john_douala",
  "email": "john@example.cm",
  "countryCode": "CM",
  "adminLevel1Id": "littoral-uuid",
  "adminLevel2Id": "wouri-uuid",
  "cityId": "douala-uuid",
  "localityId": "akwa-uuid",
  "preferredLanguageCode": "fr",
  "timezone": "Africa/Douala",
  "preferredCurrencyCode": "XAF",
  "contentCountries": ["CM", "NG", "SN"],
  "contentLanguages": ["fr", "en"],
  "discoveryRadiusKm": 50,
  "panAfricanContent": true
}
```

#### PATCH /api/v1/users/me/location

Mise à jour de la localisation géographique.

**Request:**
```json
{
  "countryCode": "CM",
  "adminLevel1Id": "littoral-uuid",
  "adminLevel2Id": "wouri-uuid",
  "cityId": "douala-uuid",
  "localityId": "bonanjo-uuid",
  "timezone": "Africa/Douala"
}
```

**Validation:**
- ✅ Pays existe
- ✅ Ville appartient au pays
- ✅ Hiérarchie cohérente (locality → city → admin2 → admin1 → country)

#### PATCH /api/v1/users/me/language

Mise à jour de la langue préférée.

**Request:**
```json
{
  "preferredLanguageCode": "en",
  "contentLanguages": ["en", "fr", "sw"]
}
```

**Validation:**
- ✅ Langue supportée dans le pays

#### PATCH /api/v1/users/me/discovery-preferences

Préférences de découverte de contenu.

**Request:**
```json
{
  "contentCountries": ["CM", "NG", "GH", "SN"],
  "contentLanguages": ["fr", "en"],
  "discoveryRadiusKm": 100,
  "panAfricanContent": true
}
```

---

## 📡 Events Kafka

### Topic: `user-events`

#### UserCountrySelected
```json
{
  "eventId": "uuid",
  "eventType": "UserCountrySelected",
  "timestamp": "2026-08-13T10:00:00Z",
  "userId": "uuid",
  "countryCode": "CM",
  "previousCountryCode": null
}
```

#### UserLocationUpdated
```json
{
  "eventId": "uuid",
  "eventType": "UserLocationUpdated",
  "timestamp": "2026-08-13T10:00:00Z",
  "userId": "uuid",
  "countryCode": "CM",
  "cityId": "douala-uuid",
  "localityId": "akwa-uuid"
}
```

#### UserLanguageUpdated
```json
{
  "eventId": "uuid",
  "eventType": "UserLanguageUpdated",
  "timestamp": "2026-08-13T10:00:00Z",
  "userId": "uuid",
  "preferredLanguageCode": "fr",
  "contentLanguages": ["fr", "en"]
}
```

#### UserDiscoveryPreferencesUpdated
```json
{
  "eventId": "uuid",
  "eventType": "UserDiscoveryPreferencesUpdated",
  "timestamp": "2026-08-13T10:00:00Z",
  "userId": "uuid",
  "contentCountries": ["CM", "NG", "SN"],
  "discoveryRadiusKm": 50,
  "panAfricanContent": true
}
```

---

## 🔄 Migration des Utilisateurs Existants

### Stratégie

Pour les utilisateurs existants **sans pays défini**:

1. ✅ Attribuer `countryCode = 'CM'` (Cameroun) par défaut
2. ✅ `preferredLanguageCode = 'fr'` (français)
3. ✅ `timezone = 'Africa/Douala'`
4. ✅ `preferredCurrencyCode = 'XAF'`
5. ✅ Journaliser les profils ambigus pour revue manuelle

### Migration SQL

```sql
-- V10__add_multi_country_support_to_users.sql

-- Ajouter les nouvelles colonnes
ALTER TABLE users ADD COLUMN country_code VARCHAR(2);
ALTER TABLE users ADD COLUMN admin_level1_id UUID;
ALTER TABLE users ADD COLUMN admin_level2_id UUID;
ALTER TABLE users ADD COLUMN city_id UUID;
ALTER TABLE users ADD COLUMN locality_id UUID;
ALTER TABLE users ADD COLUMN preferred_language_code VARCHAR(10);
ALTER TABLE users ADD COLUMN timezone VARCHAR(50);
ALTER TABLE users ADD COLUMN preferred_currency_code VARCHAR(3);
ALTER TABLE users ADD COLUMN content_countries JSONB;
ALTER TABLE users ADD COLUMN content_languages JSONB;
ALTER TABLE users ADD COLUMN discovery_radius_km INTEGER;
ALTER TABLE users ADD COLUMN pan_african_content BOOLEAN DEFAULT true;

-- Migrer les utilisateurs existants vers Cameroun
UPDATE users 
SET 
    country_code = 'CM',
    preferred_language_code = 'fr',
    timezone = 'Africa/Douala',
    preferred_currency_code = 'XAF',
    content_countries = '["CM"]'::jsonb,
    content_languages = '["fr"]'::jsonb,
    discovery_radius_km = 50,
    pan_african_content = true
WHERE country_code IS NULL;

-- Rendre country_code obligatoire
ALTER TABLE users ALTER COLUMN country_code SET NOT NULL;
ALTER TABLE users ALTER COLUMN preferred_language_code SET NOT NULL;

-- Ajouter contraintes
ALTER TABLE users ADD CONSTRAINT chk_country_code CHECK (country_code ~ '^[A-Z]{2}$');
ALTER TABLE users ADD CONSTRAINT chk_currency_code CHECK (preferred_currency_code ~ '^[A-Z]{3}$' OR preferred_currency_code IS NULL);

-- Index pour les requêtes par pays
CREATE INDEX idx_users_country ON users(country_code);
CREATE INDEX idx_users_city ON users(city_id);

-- Journaliser les utilisateurs migrés
CREATE TABLE user_migration_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    migration_type VARCHAR(50) NOT NULL,
    previous_values JSONB,
    new_values JSONB,
    migrated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT
);

INSERT INTO user_migration_log (user_id, migration_type, new_values, notes)
SELECT 
    id,
    'country_default',
    jsonb_build_object(
        'country_code', 'CM',
        'preferred_language_code', 'fr',
        'timezone', 'Africa/Douala'
    ),
    'Auto-migrated to Cameroon during multi-country deployment'
FROM users;
```

---

## 🧪 Tests

### Scénarios de Test

#### 1. Registration - Pays LIVE
```java
@Test
void shouldRegisterUserInLiveCountry() {
    // Cameroun est LIVE avec registrationEnabled = true
    RegisterRequest request = new RegisterRequest(
        "user_cm", "user@cm.com", "Pass123!",
        "CM", doualaId, "fr", "Africa/Douala", "+237691234567"
    );
    
    RegisterResponse response = authService.register(request);
    
    assertThat(response.countryCode()).isEqualTo("CM");
    assertThat(response.preferredLanguageCode()).isEqualTo("fr");
}
```

#### 2. Registration - Pays COMING_SOON
```java
@Test
void shouldRejectRegistrationInComingSoonCountry() {
    // Nigeria est COMING_SOON
    RegisterRequest request = new RegisterRequest(
        "user_ng", "user@ng.com", "Pass123!",
        "NG", lagosId, "en", "Africa/Lagos", "+2348012345678"
    );
    
    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(CountryNotLiveException.class)
        .hasMessageContaining("not yet available");
}
```

#### 3. Registration - Pays DISABLED
```java
@Test
void shouldRejectRegistrationInDisabledCountry() {
    RegisterRequest request = new RegisterRequest(
        "user", "user@example.com", "Pass123!",
        "XX", null, "en", null, "+1234567890"
    );
    
    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(CountryNotFoundException.class);
}
```

#### 4. Validation E.164
```java
@Test
void shouldValidateE164PhoneFormat() {
    RegisterRequest request = new RegisterRequest(
        "user", "user@cm.com", "Pass123!",
        "CM", doualaId, "fr", "Africa/Douala", "0691234567"  // ❌ Pas E.164
    );
    
    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(InvalidPhoneFormatException.class)
        .hasMessageContaining("E.164 format");
}
```

#### 5. Update Location - Hiérarchie Valide
```java
@Test
void shouldUpdateLocationWithValidHierarchy() {
    UpdateLocationRequest request = new UpdateLocationRequest(
        "CM",
        littoralId,  // Région Littoral
        wouriId,     // Département Wouri (parent = Littoral)
        doualaId,    // Ville Douala (parent = Wouri)
        akwaId       // Quartier Akwa (parent = Douala)
    );
    
    userService.updateLocation(userId, request);
    
    User user = userRepository.findById(userId).get();
    assertThat(user.getCountryCode()).isEqualTo("CM");
    assertThat(user.getCityId()).isEqualTo(doualaId);
}
```

#### 6. Update Location - Ville Hors Pays
```java
@Test
void shouldRejectCityOutsideCountry() {
    UpdateLocationRequest request = new UpdateLocationRequest(
        "CM",
        null,
        null,
        lagosId,  // ❌ Lagos (NG) dans profil CM
        null
    );
    
    assertThatThrownBy(() -> userService.updateLocation(userId, request))
        .isInstanceOf(InvalidLocationException.class)
        .hasMessageContaining("does not belong to country");
}
```

#### 7. Migration - Utilisateurs Existants
```java
@Test
void shouldMigrateExistingUsersToDefaultCountry() {
    // Créer utilisateur sans pays (ancien modèle)
    User oldUser = new User("old_user", "old@example.com");
    userRepository.save(oldUser);
    
    // Exécuter migration
    migrationService.migrateToMultiCountry();
    
    // Vérifier
    User migrated = userRepository.findById(oldUser.getId()).get();
    assertThat(migrated.getCountryCode()).isEqualTo("CM");
    assertThat(migrated.getPreferredLanguageCode()).isEqualTo("fr");
    assertThat(migrated.getTimezone()).isEqualTo("Africa/Douala");
}
```

---

## 📊 Impact sur les Autres Services

### Services Consommateurs

| Service | Usage | Action Requise |
|---------|-------|----------------|
| **content-service** | Filtrer par pays/langue | Ajouter `countryCode` filter |
| **discovery-service** | Rayon local + pays préférés | Utiliser `discoveryRadiusKm` + `contentCountries` |
| **recommendation-service** | Contenu adapté au pays | Interroger pays + langues utilisateur |
| **feed-service** | Feed personnalisé | Filtrer par `contentCountries` + `contentLanguages` |
| **notification-service** | Langue des notifications | Utiliser `preferredLanguageCode` |
| **payment-service** | Devise par défaut | Utiliser `preferredCurrencyCode` |
| **booking-service** | Disponibilité locale | Vérifier `countryCode` + feature flags |
| **event-service** | Événements locaux | Filtrer par `cityId` + `discoveryRadiusKm` |

### Exemple d'Intégration

```java
// Dans discovery-service
@Service
public class ContentDiscoveryService {
    
    private final UserServiceClient userClient;
    private final CountryConfigClient countryClient;
    
    public List<ContentDto> discoverContent(UUID userId) {
        // 1. Récupérer profil utilisateur
        UserProfileDto user = userClient.getUser(userId);
        
        // 2. Filtrer par pays préférés
        Set<String> countries = user.panAfricanContent() 
            ? getAllAfricanCountries() 
            : user.contentCountries();
        
        // 3. Filtrer par langues
        Set<String> languages = user.contentLanguages();
        
        // 4. Rayon géographique
        Integer radiusKm = user.discoveryRadiusKm();
        
        // 5. Requête de contenu
        return contentRepository.findByCountriesAndLanguages(
            countries, languages, user.cityId(), radiusKm
        );
    }
}
```

---

## 🚀 Déploiement

### Phase 1: Préparation
- [x] Créer migrations database
- [x] Étendre modèles User
- [x] Ajouter validation pays
- [x] Tests complets

### Phase 2: Déploiement
1. Déployer **country-config-service** (déjà fait)
2. Déployer **user-service** avec nouvelles colonnes
3. Exécuter migration `V10__add_multi_country_support_to_users.sql`
4. Déployer **auth-service** avec validation pays
5. Mettre à jour **API Gateway** routes

### Phase 3: Migration Utilisateurs
1. Exécuter migration automatique (CM par défaut)
2. Analyser logs de migration
3. Revue manuelle des profils ambigus
4. Communication utilisateurs (mise à jour profil)

### Phase 4: Services Consommateurs
1. Mettre à jour discovery-service
2. Mettre à jour recommendation-service
3. Mettre à jour feed-service
4. Mettre à jour notification-service

---

## 📖 Documentation API

Swagger UI:
- Auth Service: `http://localhost:8080/swagger-ui.html`
- User Service: `http://localhost:8081/swagger-ui.html`

---

**Version**: 1.0.0  
**Date**: 13 août 2026  
**Auteur**: YeYamo Backend Team
