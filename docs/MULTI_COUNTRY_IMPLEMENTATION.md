# Multi-Country User Management Implementation

## Vue d'ensemble

Cette implémentation étend `auth-service` et `user-service` pour supporter une gestion multi-pays complète, permettant à chaque utilisateur d'avoir une localisation principale et des préférences adaptées à son pays.

## Architecture

### Services impliqués
- **auth-service** : Gère l'enregistrement avec validation pays et champs géographiques
- **user-service** : Gère le profil utilisateur étendu avec préférences multi-pays
- **country-config-service** : Source de vérité pour les pays, villes et configuration

---

## 1. AUTH-SERVICE

### Nouveaux composants

#### RegisterRequest (DTO)
```java
public record RegisterRequest(
    String email,
    String phone,  // Format E.164 (+237XXXXXXXXX)
    String password,
    String displayName,
    String turnstileToken,
    String countryCode,  // ISO 3166-1 alpha-2 (CM, SN, CI...)
    UUID cityId,  // nullable
    String preferredLanguageCode,  // ISO 639-1 ou BCP 47
    String timezone  // nullable, IANA timezone
)
```

#### CountryValidationService
Service dédié à la validation des pays et villes :

**Méthodes principales :**
- `validateCountry(String countryCode)` : Valide l'existence du pays
- `validateCity(String countryCode, UUID cityId)` : Valide que la ville appartient au pays
- `validateRegistrationEligibility(String countryCode, boolean allowComingSoon)` : Valide les règles d'inscription

**Règles de validation :**
1. Le pays doit exister dans country-config-service
2. `registrationEnabled` doit être `true`
3. `launchStatus` ne peut pas être `DISABLED`
4. `COMING_SOON` n'est autorisé que si configuré

#### User (Entity)
Nouveaux champs :
```java
private String countryCode;  // ISO 3166-1 alpha-2
private UUID cityId;  // nullable
private String preferredLanguageCode;  // ISO 639-1
private String timezone;  // IANA timezone
```

#### AuthService
Logique d'enregistrement mise à jour :
1. Valide le pays et l'éligibilité à l'inscription
2. Valide la ville si fournie
3. Utilise les valeurs par défaut du pays si non fournies
4. Sauvegarde l'utilisateur avec les champs géographiques
5. Publie l'événement `user.created` avec les données géographiques

### Migration database
**Fichier** : `V4__add_multi_country_support.sql`

```sql
ALTER TABLE users ADD COLUMN country_code VARCHAR(2);
ALTER TABLE users ADD COLUMN city_id UUID;
ALTER TABLE users ADD COLUMN preferred_language_code VARCHAR(10);
ALTER TABLE users ADD COLUMN timezone VARCHAR(50);

CREATE INDEX idx_users_country ON users(country_code);
CREATE INDEX idx_users_city ON users(city_id);
```

### Endpoint
**POST /api/v1/auth/register**

Exemple de requête :
```json
{
  "email": "user@example.com",
  "phone": "+237699123456",
  "password": "SecurePassword123!",
  "displayName": "John Doe",
  "turnstileToken": "token",
  "countryCode": "CM",
  "cityId": "uuid-optional",
  "preferredLanguageCode": "fr",
  "timezone": "Africa/Douala"
}
```

---

## 2. USER-SERVICE

### Nouveaux composants

#### UserProfile (Domain Model)
Nouveaux champs :
```java
// Geographic location
private String countryCode;
private UUID adminLevel1Id;  // Region, State...
private UUID adminLevel2Id;  // Department, Province...
private UUID cityId;
private UUID localityId;  // Quartier, Neighborhood...

// Language preferences
private String preferredLanguageCode;
private String timezone;
private Set<String> contentLanguages;

// Discovery preferences
private String preferredCurrencyCode;
private Set<String> contentCountries;
private Integer localRadiusKm;
private boolean discoverAfricanContent;
```

#### UserProfileEntity
Mapping JPA avec :
- Colonnes pour champs scalaires
- `@ElementCollection` pour `contentCountries` et `contentLanguages`
- Tables associées : `user_content_countries`, `user_content_languages`

#### UserProfileService
Nouvelles méthodes :
- `createFromIdentityWithLocation()` : Crée profil avec données géographiques
- `updateLocation()` : Met à jour la localisation
- `updateLanguagePreferences()` : Met à jour les langues
- `updateDiscoveryPreferences()` : Met à jour les préférences de découverte

#### UserCreatedEventConsumer
Mis à jour pour extraire les champs géographiques de l'événement `user.created` et initialiser le profil.

### DTOs
- `UpdateLocationRequest` : Mise à jour localisation
- `UpdateLanguageRequest` : Mise à jour langues
- `UpdateDiscoveryPreferencesRequest` : Mise à jour découverte

### Migration database
**Fichier** : `V4__add_multi_country_fields.sql`

```sql
ALTER TABLE user_profiles ADD COLUMN country_code VARCHAR(2);
ALTER TABLE user_profiles ADD COLUMN admin_level_1_id UUID;
ALTER TABLE user_profiles ADD COLUMN admin_level_2_id UUID;
ALTER TABLE user_profiles ADD COLUMN city_id UUID;
ALTER TABLE user_profiles ADD COLUMN locality_id UUID;
ALTER TABLE user_profiles ADD COLUMN preferred_language_code VARCHAR(10);
ALTER TABLE user_profiles ADD COLUMN timezone VARCHAR(50);
ALTER TABLE user_profiles ADD COLUMN preferred_currency_code VARCHAR(3);
ALTER TABLE user_profiles ADD COLUMN local_radius_km INTEGER;
ALTER TABLE user_profiles ADD COLUMN discover_african_content BOOLEAN DEFAULT true;

CREATE TABLE user_content_countries (
    profile_id UUID REFERENCES user_profiles(id) ON DELETE CASCADE,
    country_code VARCHAR(2),
    PRIMARY KEY (profile_id, country_code)
);

CREATE TABLE user_content_languages (
    profile_id UUID REFERENCES user_profiles(id) ON DELETE CASCADE,
    language_code VARCHAR(10),
    PRIMARY KEY (profile_id, language_code)
);
```

### Endpoints

#### PATCH /api/v1/users/me/location
Mise à jour de la localisation utilisateur.

**Requête :**
```json
{
  "countryCode": "SN",
  "adminLevel1Id": "uuid",
  "cityId": "uuid",
  "timezone": "Africa/Dakar"
}
```

#### PATCH /api/v1/users/me/language
Mise à jour des préférences linguistiques.

**Requête :**
```json
{
  "preferredLanguageCode": "fr",
  "contentLanguages": ["fr", "en", "wolof"]
}
```

#### PATCH /api/v1/users/me/discovery-preferences
Mise à jour des préférences de découverte.

**Requête :**
```json
{
  "contentCountries": ["CM", "SN", "CI"],
  "localRadiusKm": 50,
  "discoverAfricanContent": true,
  "preferredCurrencyCode": "XAF"
}
```

---

## 3. ÉVÉNEMENTS

### Nouveaux événements (Topic: `user-events`)

#### profile.location_updated
```json
{
  "eventType": "profile.location_updated",
  "payload": {
    "profileId": "uuid",
    "authUserId": "userId",
    "countryCode": "CM",
    "adminLevel1Id": "uuid",
    "cityId": "uuid",
    "timezone": "Africa/Douala"
  }
}
```

#### profile.language_updated
```json
{
  "eventType": "profile.language_updated",
  "payload": {
    "profileId": "uuid",
    "authUserId": "userId",
    "preferredLanguageCode": "fr",
    "contentLanguages": ["fr", "en"]
  }
}
```

#### profile.discovery_preferences_updated
```json
{
  "eventType": "profile.discovery_preferences_updated",
  "payload": {
    "profileId": "uuid",
    "authUserId": "userId",
    "contentCountries": ["CM", "SN"],
    "localRadiusKm": 50,
    "discoverAfricanContent": true,
    "preferredCurrencyCode": "XAF"
  }
}
```

### Événement modifié

#### user.created (auth-service)
Enrichi avec champs géographiques :
```json
{
  "eventType": "user.created",
  "payload": {
    "userId": "42",
    "email": "user@example.com",
    "countryCode": "CM",
    "cityId": "uuid",
    "preferredLanguageCode": "fr",
    "timezone": "Africa/Douala"
  }
}
```

---

## 4. VALIDATION ET RÈGLES

### Format téléphone
- **Format E.164 requis** : `+[country][number]`
- Regex : `\\+[1-9]\\d{1,14}`
- Exemples valides :
  - Cameroun : `+237699123456`
  - Sénégal : `+221771234567`
  - Côte d'Ivoire : `+2250707123456`

### Code pays
- **ISO 3166-1 alpha-2** : 2 lettres majuscules
- Exemples : `CM`, `SN`, `CI`, `BF`, `ML`

### Code langue
- **ISO 639-1** (2 lettres) ou **BCP 47** (avec région)
- Exemples : `fr`, `en`, `fr-CM`, `wolof`, `bam`

### Timezone
- **IANA timezone database**
- Exemples : `Africa/Douala`, `Africa/Dakar`, `Africa/Abidjan`

### Code devise
- **ISO 4217** : 3 lettres majuscules
- Exemples : `XAF`, `XOF`, `GHS`, `NGN`

---

## 5. MIGRATION DES DONNÉES EXISTANTES

### Pour les utilisateurs existants

**Option 1 : Migration automatique (si contexte connu)**
```sql
-- Si tous les utilisateurs actuels sont camerounais
UPDATE users 
SET country_code = 'CM', 
    preferred_language_code = 'fr',
    timezone = 'Africa/Douala'
WHERE country_code IS NULL;

UPDATE user_profiles 
SET country_code = 'CM',
    preferred_language_code = 'fr',
    timezone = 'Africa/Douala',
    discover_african_content = true
WHERE country_code IS NULL;
```

**Option 2 : Migration manuelle**
- Identifier les utilisateurs sans `countryCode`
- Analyser les données disponibles (numéro de téléphone, adresse IP...)
- Demander aux utilisateurs de mettre à jour leur profil

**Option 3 : Journalisation pour revue**
```sql
-- Créer un log des profils ambigus
SELECT id, auth_user_id, display_name, created_at
FROM user_profiles
WHERE country_code IS NULL
ORDER BY created_at DESC;
```

---

## 6. TESTS

### Tests unitaires auth-service
- ✅ `CountryValidationServiceTests` : Validation pays et villes
- ✅ `AuthServiceMultiCountryTests` : Enregistrement multi-pays

**Scénarios testés :**
- Pays LIVE avec registration enabled
- Pays DISABLED (rejet)
- Pays COMING_SOON (avec/sans flag)
- Validation ville hors pays
- Format E.164 du téléphone
- Valeurs par défaut du pays

### Tests unitaires user-service
- ✅ `UserProfileMultiCountryTests` : Préférences multi-pays

**Scénarios testés :**
- Création profil avec localisation
- Mise à jour localisation
- Mise à jour langues
- Mise à jour découverte
- Valeurs nullables

---

## 7. INTÉGRATION AVEC AUTRES SERVICES

### Recommendation Service
- Consommer `profile.location_updated` pour ajuster les recommandations locales
- Consommer `profile.discovery_preferences_updated` pour filtrer les contenus par pays
- Utiliser `localRadiusKm` pour les recommandations de proximité

### Discovery Service
- Filtrer contenus par `contentCountries`
- Appliquer filtre africain si `discoverAfricanContent = true`

### Analytics Service
- Tracker distribution géographique des utilisateurs
- Analyser préférences linguistiques
- Métriques par pays

### Notification Service
- Utiliser `preferredLanguageCode` pour les notifications
- Utiliser `timezone` pour l'envoi optimal

---

## 8. CONFIGURATION

### auth-service application.properties
```properties
# Country Config Service URL
yeyamo.services.country-config.url=http://country-config-service:8080

# Allow COMING_SOON countries (default: false)
yeyamo.registration.allow-coming-soon=false
```

### RestClient Bean
```java
@Bean
public RestClient.Builder restClientBuilder() {
    return RestClient.builder();
}
```

---

## 9. SÉCURITÉ

### Validation d'entrée
- ✅ Format E.164 pour téléphone
- ✅ ISO 3166-1 alpha-2 pour pays
- ✅ Validation pays via country-config-service
- ✅ Validation ville appartenance au pays

### Protection contre abus
- ✅ Turnstile token obligatoire
- ✅ Pays disabled bloqués
- ✅ Registration disabled bloqué

---

## 10. DOCUMENTATION API

### OpenAPI/Swagger
Mettre à jour les spécifications OpenAPI pour inclure :
- Nouveaux champs dans `/auth/register`
- Nouveaux endpoints dans `/users/me/*`
- Exemples de requêtes par pays

---

## 11. CHECKLIST DE DÉPLOIEMENT

- [ ] Déployer country-config-service avec données pays
- [ ] Exécuter migrations auth-service V4
- [ ] Exécuter migrations user-service V4
- [ ] Configurer URL country-config-service
- [ ] Tester registration pour chaque pays LIVE
- [ ] Migrer utilisateurs existants (si applicable)
- [ ] Mettre à jour documentation API
- [ ] Informer services consommateurs des nouveaux événements
- [ ] Surveiller logs pour erreurs de validation pays

---

## 12. POINTS D'ATTENTION

### Performance
- Les appels à country-config-service sont synchrones
- Considérer un cache pour les configurations pays
- Monitoring des latences de validation

### Cohérence des données
- Les IDs de villes doivent correspondre au pays
- Les timezones doivent être valides
- Les codes langues doivent être reconnus

### Évolution future
- Support multi-localisation (résidences multiples)
- Historique des changements de pays
- Préférences par service (langue notifications vs contenu)

---

## Résumé

Cette implémentation fournit une base solide pour la gestion multi-pays :
- ✅ Enregistrement avec sélection pays obligatoire
- ✅ Validation complète via country-config-service
- ✅ Profil utilisateur enrichi avec préférences géographiques
- ✅ Endpoints dédiés pour mise à jour granulaire
- ✅ Événements pour propagation aux autres services
- ✅ Tests couvrant les scénarios principaux
- ✅ Migration documentée pour utilisateurs existants
