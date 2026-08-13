# Audit Multi-Pays - Domaines de Contenu YeYamo

## Vue d'ensemble

Ce document audite tous les services de contenu YeYamo et définit les extensions nécessaires pour le support multi-pays complet.

---

## Infrastructure Commune

### ✅ Bibliothèque partagée

**Fichier** : `shared-lib/src/main/java/com/yeyamo_mobile/shared/country/`

#### CountryConfigClient
- Client résilient vers country-config-service
- Circuit breaker (seuil 50%, fenêtre 10 requêtes, attente 30s)
- Cache local 5 minutes
- Timeout 2 secondes
- **Pas de fallback silencieux** : échec strict si service indisponible

#### GeographicFields (@Embeddable)
Champs standard pour toutes les entités géographiques :
```java
@Embedded
private GeographicFields geography;
```

Contient :
- `countryCode` (required, VARCHAR(2))
- `adminLevel1Id`, `adminLevel2Id`, `cityId`, `localityId` (UUID)
- `latitude`, `longitude` (DECIMAL(9,6))
- `languageCode` (VARCHAR(10))

---

## Services à Étendre

### 1. CONTENT-SERVICE

#### Entités concernées
- **Place** (lieux)
- **Post** (publications géolocalisées)
- **Story** (stories géolocalisées)

#### Modifications Place

**Modèle actuel à vérifier** :
```bash
# Chercher le modèle Place
```

**Extensions nécessaires** :
```java
@Entity
@Table(name = "places")
public class Place {
    // ... champs existants
    
    @Embedded
    private GeographicFields geography;
    
    @Column(name = "is_verified")
    private boolean isVerified = false;
}
```

**Migration** :
```sql
-- V5__add_multi_country_to_places.sql
ALTER TABLE places ADD COLUMN country_code VARCHAR(2) NOT NULL DEFAULT 'CM';
ALTER TABLE places ADD COLUMN admin_level_1_id UUID;
ALTER TABLE places ADD COLUMN admin_level_2_id UUID;
ALTER TABLE places ADD COLUMN city_id UUID;
ALTER TABLE places ADD COLUMN locality_id UUID;
ALTER TABLE places ADD COLUMN latitude DECIMAL(9,6);
ALTER TABLE places ADD COLUMN longitude DECIMAL(9,6);
ALTER TABLE places ADD COLUMN language_code VARCHAR(10);
ALTER TABLE places ADD COLUMN is_verified BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX idx_places_country ON places(country_code);
CREATE INDEX idx_places_city ON places(city_id);
CREATE INDEX idx_places_location ON places(latitude, longitude);

-- Backfill Cameroon data
UPDATE places SET country_code = 'CM' WHERE country_code = 'CM';
```

**Validation à l'ajout** :
```java
public Place createPlace(CreatePlaceRequest request, String userId) {
    // Validate country feature
    countryConfigClient.validateFeature(
        request.countryCode(), 
        CountryFeature.CONTENT_PUBLISHING
    );
    
    // Validate city if provided
    if (request.cityId() != null) {
        countryConfigClient.validateCity(request.countryCode(), request.cityId());
    }
    
    // Create place...
}
```

**Événements enrichis** :
```json
{
  "eventType": "place.created",
  "payload": {
    "placeId": "uuid",
    "countryCode": "CM",
    "cityId": "uuid",
    "latitude": 4.0511,
    "longitude": 9.7679,
    "languageCode": "fr"
  }
}
```

#### Modifications Post/Story

Ajouter `countryCode` et `cityId` pour les posts géolocalisés :
```sql
ALTER TABLE posts ADD COLUMN country_code VARCHAR(2);
ALTER TABLE posts ADD COLUMN city_id UUID;
ALTER TABLE posts ADD COLUMN latitude DECIMAL(9,6);
ALTER TABLE posts ADD COLUMN longitude DECIMAL(9,6);

ALTER TABLE stories ADD COLUMN country_code VARCHAR(2);
ALTER TABLE stories ADD COLUMN city_id UUID;
```

---

### 2. CATALOG-SERVICE

#### Entités concernées
- **Collection** (collections éditoriales)
- **FeaturedContent** (contenus mis en avant)

#### Extensions nécessaires

**Collection** :
```java
@Entity
public class Collection {
    // ... champs existants
    
    @ElementCollection
    @CollectionTable(name = "collection_countries")
    @Column(name = "country_code")
    private Set<String> targetCountries = new HashSet<>();
    
    @ElementCollection
    @CollectionTable(name = "collection_languages")
    @Column(name = "language_code")
    private Set<String> targetLanguages = new HashSet<>();
    
    @Enumerated(EnumType.STRING)
    private CollectionScope scope = CollectionScope.COUNTRY_SPECIFIC;
}

enum CollectionScope {
    COUNTRY_SPECIFIC,  // Visible dans pays spécifiques
    AFRICAN,          // Tous les pays africains
    GLOBAL            // Tous les pays
}
```

**Migration** :
```sql
-- V4__add_multi_country_to_collections.sql
CREATE TABLE collection_countries (
    collection_id UUID REFERENCES collections(id) ON DELETE CASCADE,
    country_code VARCHAR(2) NOT NULL,
    PRIMARY KEY (collection_id, country_code)
);

CREATE TABLE collection_languages (
    collection_id UUID REFERENCES collections(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    PRIMARY KEY (collection_id, language_code)
);

ALTER TABLE collections ADD COLUMN scope VARCHAR(30) NOT NULL DEFAULT 'COUNTRY_SPECIFIC';
```

---

### 3. CULTURE-SERVICE

#### Entités concernées
- **Artwork** (œuvres d'art)
- **CulturalEvent** (événements culturels)
- **Artisan** (artisans)

#### Extensions Artwork

```java
@Entity
@Table(name = "artworks")
public class Artwork {
    // ... champs existants
    
    @Embedded
    private GeographicFields geography;
    
    @Column(name = "origin_country_code", length = 2)
    private String originCountryCode;
    
    @Column(name = "cultural_origin")
    private String culturalOrigin; // e.g., "Bamoun", "Bamiléké"
}
```

**Migration** :
```sql
-- V6__add_multi_country_to_artworks.sql
ALTER TABLE artworks ADD COLUMN country_code VARCHAR(2) NOT NULL DEFAULT 'CM';
ALTER TABLE artworks ADD COLUMN city_id UUID;
ALTER TABLE artworks ADD COLUMN language_code VARCHAR(10);
ALTER TABLE artworks ADD COLUMN origin_country_code VARCHAR(2);
ALTER TABLE artworks ADD COLUMN cultural_origin VARCHAR(100);

CREATE INDEX idx_artworks_country ON artworks(country_code);
CREATE INDEX idx_artworks_origin ON artworks(origin_country_code);

UPDATE artworks SET country_code = 'CM', origin_country_code = 'CM' 
WHERE country_code = 'CM';
```

**Validation** :
```java
public Artwork createArtwork(CreateArtworkRequest request) {
    countryConfigClient.validateFeature(
        request.countryCode(),
        CountryFeature.CULTURE_MODULE
    );
    // or ARTISAN_COMMERCE for commercial artworks
}
```

#### Extensions Artisan

```java
@Entity
public class Artisan {
    // ... champs existants
    
    @Embedded
    private GeographicFields geography;
    
    @Column(name = "serves_countries")
    private String servesCountries; // JSON array of country codes
}
```

---

### 4. EVENT-SERVICE

#### Entités concernées
- **Event** (événements)
- **Venue** (lieux d'événements)

#### Extensions Event

```java
@Entity
@Table(name = "events")
public class Event {
    // ... champs existants
    
    @Embedded
    private GeographicFields geography;
    
    @Column(name = "is_virtual")
    private boolean isVirtual = false;
    
    @ElementCollection
    @CollectionTable(name = "event_accessible_countries")
    private Set<String> accessibleCountries = new HashSet<>();
}
```

**Migration** :
```sql
-- V5__add_multi_country_to_events.sql
ALTER TABLE events ADD COLUMN country_code VARCHAR(2);
ALTER TABLE events ADD COLUMN city_id UUID;
ALTER TABLE events ADD COLUMN latitude DECIMAL(9,6);
ALTER TABLE events ADD COLUMN longitude DECIMAL(9,6);
ALTER TABLE events ADD COLUMN is_virtual BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE event_accessible_countries (
    event_id UUID REFERENCES events(id) ON DELETE CASCADE,
    country_code VARCHAR(2) NOT NULL,
    PRIMARY KEY (event_id, country_code)
);

CREATE INDEX idx_events_country ON events(country_code);
CREATE INDEX idx_events_city ON events(city_id);

-- Physical events require country
UPDATE events SET country_code = 'CM' 
WHERE country_code IS NULL AND is_virtual = false;
```

**Validation** :
```java
public Event createEvent(CreateEventRequest request) {
    if (!request.isVirtual()) {
        // Physical event requires country
        countryConfigClient.validateFeature(
            request.countryCode(),
            CountryFeature.CONTENT_PUBLISHING
        );
    }
}
```

---

### 5. PARTNER-SERVICE

#### Entités concernées
- **Partner** (partenaires)
- **PartnerLocation** (établissements)

#### Extensions Partner

```java
@Entity
public class Partner {
    // ... champs existants
    
    @Column(name = "primary_country_code", nullable = false)
    private String primaryCountryCode;
    
    @ElementCollection
    @CollectionTable(name = "partner_operating_countries")
    private Set<String> operatingCountries = new HashSet<>();
}

@Entity
public class PartnerLocation {
    // ... champs existants
    
    @Embedded
    private GeographicFields geography;
}
```

**Migration** :
```sql
-- V7__add_multi_country_to_partners.sql
ALTER TABLE partners ADD COLUMN primary_country_code VARCHAR(2) NOT NULL DEFAULT 'CM';

CREATE TABLE partner_operating_countries (
    partner_id UUID REFERENCES partners(id) ON DELETE CASCADE,
    country_code VARCHAR(2) NOT NULL,
    PRIMARY KEY (partner_id, country_code)
);

ALTER TABLE partner_locations ADD COLUMN country_code VARCHAR(2) NOT NULL DEFAULT 'CM';
ALTER TABLE partner_locations ADD COLUMN city_id UUID;
ALTER TABLE partner_locations ADD COLUMN latitude DECIMAL(9,6);
ALTER TABLE partner_locations ADD COLUMN longitude DECIMAL(9,6);

CREATE INDEX idx_partner_locations_country ON partner_locations(country_code);

UPDATE partners SET primary_country_code = 'CM';
UPDATE partner_locations SET country_code = 'CM';
```

**Validation onboarding** :
```java
public Partner onboardPartner(OnboardPartnerRequest request) {
    countryConfigClient.validateFeature(
        request.countryCode(),
        CountryFeature.PARTNER_ONBOARDING
    );
}
```

---

### 6. BOOKING-SERVICE

#### Entités concernées
- **Booking** (réservations)

#### Extensions Booking

```java
@Entity
public class Booking {
    // ... champs existants
    
    @Column(name = "country_code", nullable = false)
    private String countryCode;
    
    @Column(name = "currency_code", length = 3)
    private String currencyCode;
}
```

**Migration** :
```sql
-- V4__add_multi_country_to_bookings.sql
ALTER TABLE bookings ADD COLUMN country_code VARCHAR(2) NOT NULL DEFAULT 'CM';
ALTER TABLE bookings ADD COLUMN currency_code VARCHAR(3) NOT NULL DEFAULT 'XAF';

CREATE INDEX idx_bookings_country ON bookings(country_code);

UPDATE bookings SET country_code = 'CM', currency_code = 'XAF';
```

**Validation** :
```java
public Booking createBooking(CreateBookingRequest request) {
    countryConfigClient.validateFeature(
        request.countryCode(),
        CountryFeature.BOOKING
    );
    
    // Validate payment if required
    if (request.requiresPayment()) {
        countryConfigClient.validateFeature(
            request.countryCode(),
            CountryFeature.PAYMENTS
        );
    }
}
```

---

### 7. RECOMMENDATION-SERVICE

#### Modifications nécessaires

**Requêtes de recommandation** :
```java
public List<Recommendation> getRecommendations(String userId) {
    UserProfile profile = getUserProfile(userId);
    
    // Filter by user's content countries
    Set<String> countries = profile.getContentCountries();
    if (countries.isEmpty()) {
        countries = Set.of(profile.getCountryCode());
    }
    
    // Apply African content filter if enabled
    if (profile.isDiscoverAfricanContent()) {
        countries = getAfricanCountries();
    }
    
    // Apply local radius if set
    if (profile.getLocalRadiusKm() != null && profile.hasCoordinates()) {
        // Geo-spatial query
    }
}
```

**Consumer d'événements** :
```java
@KafkaListener(topics = "user-events")
public void handleUserEvents(String event) {
    // Listen to:
    // - profile.location_updated
    // - profile.discovery_preferences_updated
    // Update recommendation cache
}
```

---

### 8. FEED-SERVICE

#### Modifications nécessaires

**Algorithme de feed** :
```java
public Feed generateFeed(String userId) {
    UserProfile profile = getUserProfile(userId);
    
    // 1. Filter by content countries
    Set<String> countries = profile.getContentCountries();
    
    // 2. Filter by content languages
    Set<String> languages = profile.getContentLanguages();
    
    // 3. Local content boost
    if (profile.getLocalRadiusKm() != null) {
        // Boost content within radius
    }
    
    // 4. Mix algorithmic + editorial
}
```

---

### 9. DISCOVERY-SERVICE

#### Modifications nécessaires

**Filtres de recherche** :
```java
public SearchResults search(SearchRequest request, String userId) {
    UserProfile profile = getUserProfile(userId);
    
    // Apply user's country filters
    SearchCriteria criteria = SearchCriteria.builder()
        .countries(request.countries() != null 
            ? request.countries() 
            : profile.getContentCountries())
        .languages(profile.getContentLanguages())
        .build();
        
    // Execute search
}
```

**Déjà implémenté** : Culture et artisan discovery
- ✅ V3__culture_artisan_support.sql existe

**À vérifier** : Si les tables ont déjà les champs géographiques

---

### 10. ANALYTICS-SERVICE

#### Nouvelles métriques

**Par pays** :
- Total users par pays
- Total places par pays
- Total events par pays
- Engagement rate par pays

**Dimensions analytiques** :
```java
@Entity
public class ContentAnalytics {
    // ... champs existants
    
    @Column(name = "country_code")
    private String countryCode;
    
    @Column(name = "language_code")
    private String languageCode;
}
```

**Migration** :
```sql
-- V6__add_multi_country_to_analytics.sql
ALTER TABLE content_analytics ADD COLUMN country_code VARCHAR(2);
ALTER TABLE content_analytics ADD COLUMN language_code VARCHAR(10);
ALTER TABLE user_engagement_daily ADD COLUMN country_code VARCHAR(2);

CREATE INDEX idx_analytics_country ON content_analytics(country_code);
CREATE INDEX idx_engagement_country ON user_engagement_daily(country_code);
```

---

## Événements Kafka Enrichis

### Format standard

Tous les événements doivent inclure :
```json
{
  "eventId": "uuid",
  "eventType": "resource.action",
  "eventVersion": 1,
  "occurredAt": "2026-08-13T10:30:00Z",
  "producer": "service-name",
  "correlationId": "uuid",
  "actorId": "userId",
  "payload": {
    "resourceId": "uuid",
    "countryCode": "CM",
    "languageCode": "fr",
    "cityId": "uuid",
    "latitude": 4.0511,
    "longitude": 9.7679
  }
}
```

### Événements à enrichir

- ✅ `place.created`, `place.updated`
- ✅ `artwork.created`, `artwork.updated`
- ✅ `event.created`, `event.updated`
- ✅ `partner.created`, `partner.location_added`
- ✅ `booking.created`
- ✅ `post.created` (si géolocalisé)

---

## Tests Multi-Pays

### Scénarios de test obligatoires

Pour chaque service :

#### 1. Pays actif (LIVE)
```java
@Test
void createResource_inLiveCountry_succeeds() {
    when(countryConfigClient.getCountry("CM"))
        .thenReturn(new CountryConfig("CM", "LIVE", true, ...));
    
    assertDoesNotThrow(() -> service.createResource(request));
}
```

#### 2. Pays COMING_SOON
```java
@Test
void createResource_inComingSoonCountry_throwsException() {
    when(countryConfigClient.getCountry("SN"))
        .thenReturn(new CountryConfig("SN", "COMING_SOON", false, ...));
    
    assertThrows(CountryConfigException.class, 
        () -> service.createResource(request));
}
```

#### 3. Feature désactivée
```java
@Test
void createPlace_whenContentPublishingDisabled_throwsException() {
    when(countryConfigClient.getCountry("CI"))
        .thenReturn(new CountryConfig("CI", "LIVE", false, ...));
    
    assertThrows(CountryConfigException.class, 
        () -> service.createPlace(request));
}
```

#### 4. Service indisponible
```java
@Test
void createResource_whenCountryServiceDown_throwsException() {
    when(countryConfigClient.getCountry(any()))
        .thenThrow(new CountryConfigException("Service unavailable"));
    
    assertThrows(CountryConfigException.class, 
        () -> service.createResource(request));
}
```

#### 5. Ville invalide
```java
@Test
void createPlace_withInvalidCity_throwsException() {
    doThrow(new CountryConfigException("City not found"))
        .when(countryConfigClient).validateCity("CM", invalidCityId);
    
    assertThrows(CountryConfigException.class, 
        () -> service.createPlace(request));
}
```

#### 6. Événement Kafka enrichi
```java
@Test
void publishEvent_includesGeographicFields() {
    service.createPlace(request);
    
    verify(eventPublisher).publish(argThat(event -> 
        event.getPayload().get("countryCode").equals("CM") &&
        event.getPayload().get("cityId") != null
    ));
}
```

---

## Migration Données Existantes

### Script de migration automatique

```sql
-- content-service
UPDATE places 
SET country_code = 'CM', language_code = 'fr' 
WHERE country_code IS NULL;

-- culture-service
UPDATE artworks 
SET country_code = 'CM', origin_country_code = 'CM' 
WHERE country_code IS NULL;

UPDATE artisans 
SET country_code = 'CM' 
WHERE country_code IS NULL;

-- event-service
UPDATE events 
SET country_code = 'CM' 
WHERE country_code IS NULL AND is_virtual = false;

-- partner-service
UPDATE partners 
SET primary_country_code = 'CM' 
WHERE primary_country_code IS NULL;

UPDATE partner_locations 
SET country_code = 'CM' 
WHERE country_code IS NULL;

-- booking-service
UPDATE bookings 
SET country_code = 'CM', currency_code = 'XAF' 
WHERE country_code IS NULL;
```

### Log des profils ambigus

```sql
-- Identifier les ressources sans localisation claire
SELECT 'places' as table_name, id, created_at 
FROM places 
WHERE country_code IS NULL
UNION ALL
SELECT 'events', id, created_at 
FROM events 
WHERE country_code IS NULL AND is_virtual = false
ORDER BY created_at DESC;
```

---

## Checklist de Déploiement

### Par service

- [ ] Ajouter dépendance `shared-lib` avec `CountryConfigClient`
- [ ] Créer bean `RestClient.Builder`
- [ ] Configurer `yeyamo.services.country-config.url`
- [ ] Ajouter champs géographiques aux entités
- [ ] Créer migrations Flyway
- [ ] Ajouter validation feature dans création ressources
- [ ] Enrichir événements Kafka
- [ ] Écrire tests multi-pays
- [ ] Exécuter migration données
- [ ] Déployer
- [ ] Monitorer logs erreurs

### Ordre de déploiement

1. ✅ country-config-service (déjà déployé)
2. ✅ auth-service + user-service (déjà fait)
3. content-service
4. catalog-service
5. culture-service
6. event-service
7. partner-service
8. booking-service
9. recommendation-service
10. feed-service
11. discovery-service
12. analytics-service

---

## Résumé

Cette extension multi-pays touche **10 services** et nécessite :
- ✅ Client commun résilient (CountryConfigClient)
- ✅ Champs géographiques standardisés (GeographicFields)
- 🔄 ~25 migrations database
- 🔄 Validation feature par pays pour chaque création
- 🔄 Enrichissement de tous les événements Kafka
- 🔄 Tests multi-pays pour tous les services
- 🔄 Migration données historiques vers Cameroun

**Estimation** : 2-3 semaines pour compléter tous les services avec tests.
