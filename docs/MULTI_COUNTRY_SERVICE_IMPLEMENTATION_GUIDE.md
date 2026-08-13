# Guide d'implémentation Multi-Pays par Service

Ce guide fournit un modèle d'implémentation complet pour étendre un service existant au support multi-pays.

---

## Étape 1 : Dépendances et Configuration

### 1.1 Ajouter shared-lib au pom.xml

```xml
<dependency>
    <groupId>com.yeyamo_mobile.api</groupId>
    <artifactId>shared-lib</artifactId>
    <version>${project.version}</version>
</dependency>

<!-- Resilience4j pour circuit breaker -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>

<!-- Cache -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

### 1.2 Configuration application.properties

```properties
# Country Config Service
yeyamo.services.country-config.url=http://country-config-service:8080

# Resilience4j Circuit Breaker
resilience4j.circuitbreaker.instances.country-config.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.country-config.wait-duration-in-open-state=30s
resilience4j.circuitbreaker.instances.country-config.sliding-window-size=10

# Cache
spring.cache.cache-names=countryConfig
spring.cache.caffeine.spec=maximumSize=500,expireAfterWrite=5m
```

### 1.3 Beans de configuration

```java
@Configuration
@EnableCaching
public class CountryConfigConfiguration {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public CountryConfigClient countryConfigClient(
            RestClient.Builder builder,
            @Value("${yeyamo.services.country-config.url}") String url,
            CircuitBreakerRegistry registry) {
        return new CountryConfigClient(builder, url, registry);
    }
}
```

---

## Étape 2 : Extension du Modèle de Données

### 2.1 Exemple pour Place Entity

**Avant** :
```java
@Entity
@Table(name = "places")
public class Place {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String name;
    private String description;
    private String address;
    
    @Enumerated(EnumType.STRING)
    private PlaceType type;
    
    // ... autres champs
}
```

**Après** :
```java
@Entity
@Table(name = "places", indexes = {
    @Index(name = "idx_places_country", columnList = "country_code"),
    @Index(name = "idx_places_city", columnList = "city_id"),
    @Index(name = "idx_places_location", columnList = "latitude, longitude")
})
public class Place {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String name;
    private String description;
    private String address;
    
    @Enumerated(EnumType.STRING)
    private PlaceType type;
    
    // NOUVEAU : Champs géographiques
    @Embedded
    private GeographicFields geography;
    
    @Column(name = "is_verified")
    private boolean isVerified = false;
    
    // ... autres champs
    
    // Constructor
    public Place(String name, String countryCode) {
        this.name = name;
        this.geography = new GeographicFields(countryCode);
    }
    
    // Business methods
    public void setLocation(UUID cityId, Double latitude, Double longitude) {
        geography.setCityId(cityId);
        geography.setCoordinates(latitude, longitude);
    }
    
    public boolean isInCountry(String countryCode) {
        return geography.getCountryCode().equals(countryCode);
    }
}
```

### 2.2 Migration Flyway

**Fichier** : `src/main/resources/db/migration/V5__add_multi_country_support.sql`

```sql
-- Add geographic fields to places
ALTER TABLE places ADD COLUMN country_code VARCHAR(2);
ALTER TABLE places ADD COLUMN admin_level_1_id UUID;
ALTER TABLE places ADD COLUMN admin_level_2_id UUID;
ALTER TABLE places ADD COLUMN city_id UUID;
ALTER TABLE places ADD COLUMN locality_id UUID;
ALTER TABLE places ADD COLUMN latitude DECIMAL(9,6);
ALTER TABLE places ADD COLUMN longitude DECIMAL(9,6);
ALTER TABLE places ADD COLUMN language_code VARCHAR(10);
ALTER TABLE places ADD COLUMN is_verified BOOLEAN NOT NULL DEFAULT false;

-- Create indexes for geographic queries
CREATE INDEX idx_places_country ON places(country_code);
CREATE INDEX idx_places_city ON places(city_id);
CREATE INDEX idx_places_location ON places USING GIST (
    ll_to_earth(latitude, longitude)
) WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

-- Backfill Cameroon data for existing records
UPDATE places 
SET country_code = 'CM', language_code = 'fr'
WHERE country_code IS NULL;

-- Make country_code NOT NULL after backfill
ALTER TABLE places ALTER COLUMN country_code SET NOT NULL;

-- Log ambiguous profiles for manual review
CREATE TABLE place_migration_log (
    place_id UUID,
    old_address TEXT,
    migration_date TIMESTAMP DEFAULT NOW(),
    needs_review BOOLEAN DEFAULT true
);

INSERT INTO place_migration_log (place_id, old_address)
SELECT id, address 
FROM places 
WHERE city_id IS NULL OR latitude IS NULL;
```

---

## Étape 3 : Service Layer

### 3.1 Injection du CountryConfigClient

```java
@Service
@Transactional
public class PlaceService {
    
    private final PlaceRepository repository;
    private final CountryConfigClient countryConfigClient;
    private final PlaceEventPublisher eventPublisher;
    
    public PlaceService(
            PlaceRepository repository,
            CountryConfigClient countryConfigClient,
            PlaceEventPublisher eventPublisher) {
        this.repository = repository;
        this.countryConfigClient = countryConfigClient;
        this.eventPublisher = eventPublisher;
    }
    
    // ... methods
}
```

### 3.2 Validation lors de la création

```java
public Place createPlace(CreatePlaceRequest request, String userId) {
    // 1. Validate country and feature
    countryConfigClient.validateFeature(
        request.countryCode(),
        CountryFeature.CONTENT_PUBLISHING
    );
    
    // 2. Validate city if provided
    if (request.cityId() != null) {
        countryConfigClient.validateCity(
            request.countryCode(),
            request.cityId()
        );
    }
    
    // 3. Create place
    Place place = new Place(request.name(), request.countryCode());
    place.setDescription(request.description());
    place.setLocation(
        request.cityId(),
        request.latitude(),
        request.longitude()
    );
    
    if (request.languageCode() != null) {
        place.getGeography().setLanguageCode(request.languageCode());
    } else {
        // Use country default
        CountryConfig country = countryConfigClient.getCountry(request.countryCode());
        place.getGeography().setLanguageCode(country.defaultLanguageCode());
    }
    
    // 4. Save
    Place saved = repository.save(place);
    
    // 5. Publish event
    eventPublisher.placeCreated(saved, userId);
    
    return saved;
}
```

### 3.3 Gestion des erreurs

```java
@RestControllerAdvice
public class CountryValidationExceptionHandler {
    
    @ExceptionHandler(CountryConfigClient.CountryConfigException.class)
    public ResponseEntity<ErrorResponse> handleCountryConfigException(
            CountryConfigClient.CountryConfigException ex) {
        
        ErrorResponse error = new ErrorResponse(
            "COUNTRY_CONFIG_ERROR",
            ex.getMessage(),
            Instant.now()
        );
        
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(error);
    }
}
```

---

## Étape 4 : DTOs et Validation

### 4.1 Request DTO

```java
public record CreatePlaceRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 1000) String description,
    @NotBlank @Pattern(regexp = "[A-Z]{2}") String countryCode,
    UUID cityId,
    @DecimalMin("-90") @DecimalMax("90") Double latitude,
    @DecimalMin("-180") @DecimalMax("180") Double longitude,
    @Size(max = 10) String languageCode,
    @NotNull PlaceType type
) {
    public CreatePlaceRequest {
        if (latitude != null && longitude == null) {
            throw new IllegalArgumentException("Longitude required when latitude provided");
        }
        if (longitude != null && latitude == null) {
            throw new IllegalArgumentException("Latitude required when longitude provided");
        }
    }
}
```

### 4.2 Response DTO

```java
public record PlaceResponse(
    UUID id,
    String name,
    String description,
    GeographicInfo geography,
    PlaceType type,
    boolean isVerified,
    Instant createdAt
) {
    public static PlaceResponse from(Place place) {
        return new PlaceResponse(
            place.getId(),
            place.getName(),
            place.getDescription(),
            GeographicInfo.from(place.getGeography()),
            place.getType(),
            place.isVerified(),
            place.getCreatedAt()
        );
    }
}

public record GeographicInfo(
    String countryCode,
    UUID cityId,
    Double latitude,
    Double longitude,
    String languageCode
) {
    public static GeographicInfo from(GeographicFields fields) {
        return new GeographicInfo(
            fields.getCountryCode(),
            fields.getCityId(),
            fields.getLatitude(),
            fields.getLongitude(),
            fields.getLanguageCode()
        );
    }
}
```

---

## Étape 5 : Événements Kafka

### 5.1 Event Publisher

```java
@Component
public class PlaceEventPublisher {
    
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    
    public void placeCreated(Place place, String userId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("placeId", place.getId().toString());
        payload.put("name", place.getName());
        payload.put("type", place.getType().name());
        
        // IMPORTANT : Geographic fields
        payload.put("countryCode", place.getGeography().getCountryCode());
        payload.put("cityId", place.getGeography().getCityId() != null 
            ? place.getGeography().getCityId().toString() 
            : null);
        payload.put("latitude", place.getGeography().getLatitude());
        payload.put("longitude", place.getGeography().getLongitude());
        payload.put("languageCode", place.getGeography().getLanguageCode());
        
        publishEvent("place.created", place.getId(), userId, payload);
    }
    
    private void publishEvent(String eventType, UUID aggregateId, 
            String actorId, Map<String, Object> payload) {
        try {
            OutboxEvent event = new OutboxEvent();
            event.setId(UUID.randomUUID());
            event.setAggregateType("place");
            event.setAggregateId(aggregateId.toString());
            event.setEventType(eventType);
            event.setActorId(actorId);
            event.setOccurredAt(Instant.now());
            
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("eventId", event.getId());
            envelope.put("eventType", eventType);
            envelope.put("eventVersion", 1);
            envelope.put("occurredAt", event.getOccurredAt());
            envelope.put("producer", "content-service");
            envelope.put("correlationId", UUID.randomUUID());
            envelope.put("actorId", actorId);
            envelope.put("payload", payload);
            
            event.setPayload(objectMapper.writeValueAsString(envelope));
            outboxRepository.save(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish event", e);
        }
    }
}
```

---

## Étape 6 : Requêtes avec Filtres Géographiques

### 6.1 Repository avec filtres pays

```java
public interface PlaceRepository extends JpaRepository<Place, UUID> {
    
    @Query("SELECT p FROM Place p WHERE p.geography.countryCode = :countryCode")
    Page<Place> findByCountry(
        @Param("countryCode") String countryCode, 
        Pageable pageable
    );
    
    @Query("SELECT p FROM Place p WHERE p.geography.countryCode IN :countries")
    Page<Place> findByCountries(
        @Param("countries") Set<String> countries, 
        Pageable pageable
    );
    
    @Query("""
        SELECT p FROM Place p 
        WHERE p.geography.countryCode = :countryCode
        AND p.geography.cityId = :cityId
        """)
    Page<Place> findByCountryAndCity(
        @Param("countryCode") String countryCode,
        @Param("cityId") UUID cityId,
        Pageable pageable
    );
    
    // Geo-spatial query (requires PostGIS)
    @Query(value = """
        SELECT * FROM places 
        WHERE country_code = :countryCode
        AND earth_distance(
            ll_to_earth(latitude, longitude),
            ll_to_earth(:lat, :lon)
        ) <= :radiusMeters
        """, nativeQuery = true)
    List<Place> findNearby(
        @Param("countryCode") String countryCode,
        @Param("lat") Double latitude,
        @Param("lon") Double longitude,
        @Param("radiusMeters") Double radiusMeters
    );
}
```

### 6.2 Service de recherche

```java
public Page<Place> searchPlaces(PlaceSearchRequest request, String userId) {
    // Get user's country preferences
    UserProfile user = userProfileClient.getProfile(userId);
    
    // Determine countries to search
    Set<String> countries = request.countries() != null 
        ? request.countries()
        : user.getContentCountries();
    
    if (countries.isEmpty()) {
        countries = Set.of(user.getCountryCode());
    }
    
    // Apply filters
    if (request.cityId() != null) {
        return repository.findByCountryAndCity(
            request.countryCode(),
            request.cityId(),
            request.pageable()
        );
    }
    
    if (request.nearLatitude() != null && request.nearLongitude() != null) {
        // Nearby search
        return repository.findNearby(
            request.countryCode(),
            request.nearLatitude(),
            request.nearLongitude(),
            request.radiusKm() * 1000, // Convert to meters
            request.pageable()
        );
    }
    
    return repository.findByCountries(countries, request.pageable());
}
```

---

## Étape 7 : Tests

### 7.1 Test avec pays actif

```java
@SpringBootTest
@AutoConfigureMockMvc
class PlaceServiceMultiCountryTests {
    
    @Autowired
    private PlaceService placeService;
    
    @MockBean
    private CountryConfigClient countryConfigClient;
    
    @Test
    void createPlace_inLiveCountryWithFeatureEnabled_succeeds() {
        // Given
        CountryConfig cameroon = new CountryConfig(
            "CM", "Cameroon", "LIVE",
            true, true, // registrationEnabled, contentPublishingEnabled
            false, false, false, false, false,
            "fr", "Africa/Douala", "XAF"
        );
        
        when(countryConfigClient.getCountry("CM")).thenReturn(cameroon);
        doNothing().when(countryConfigClient).validateFeature(any(), any());
        
        CreatePlaceRequest request = new CreatePlaceRequest(
            "Douala Grand Mall",
            "Centre commercial",
            "CM",
            null,
            4.0511,
            9.7679,
            "fr",
            PlaceType.SHOPPING
        );
        
        // When
        Place place = placeService.createPlace(request, "user123");
        
        // Then
        assertNotNull(place.getId());
        assertEquals("CM", place.getGeography().getCountryCode());
        assertEquals(4.0511, place.getGeography().getLatitude());
        assertEquals("fr", place.getGeography().getLanguageCode());
        
        verify(countryConfigClient).validateFeature("CM", CountryFeature.CONTENT_PUBLISHING);
    }
    
    @Test
    void createPlace_whenContentPublishingDisabled_throwsException() {
        // Given
        doThrow(new CountryConfigException("Feature not enabled"))
            .when(countryConfigClient)
            .validateFeature("SN", CountryFeature.CONTENT_PUBLISHING);
        
        CreatePlaceRequest request = new CreatePlaceRequest(
            "Dakar Plaza", "", "SN", null, null, null, null, PlaceType.SHOPPING
        );
        
        // When & Then
        assertThrows(CountryConfigException.class, 
            () -> placeService.createPlace(request, "user123"));
    }
    
    @Test
    void createPlace_withInvalidCity_throwsException() {
        // Given
        UUID invalidCityId = UUID.randomUUID();
        
        when(countryConfigClient.getCountry("CM"))
            .thenReturn(new CountryConfig("CM", "Cameroon", "LIVE", true, true, 
                false, false, false, false, false, "fr", "Africa/Douala", "XAF"));
        
        doThrow(new CountryConfigException("City not found"))
            .when(countryConfigClient)
            .validateCity("CM", invalidCityId);
        
        CreatePlaceRequest request = new CreatePlaceRequest(
            "Test Place", "", "CM", invalidCityId, null, null, null, PlaceType.RESTAURANT
        );
        
        // When & Then
        assertThrows(CountryConfigException.class,
            () -> placeService.createPlace(request, "user123"));
    }
    
    @Test
    void createPlace_whenCountryServiceUnavailable_throwsException() {
        // Given
        when(countryConfigClient.getCountry(any()))
            .thenThrow(new CountryConfigException("Service unavailable"));
        
        CreatePlaceRequest request = new CreatePlaceRequest(
            "Test", "", "CM", null, null, null, null, PlaceType.PARK
        );
        
        // When & Then
        assertThrows(CountryConfigException.class,
            () -> placeService.createPlace(request, "user123"));
    }
    
    @Test
    void publishEvent_includesGeographicFields() {
        // Given
        Place place = new Place("Test Place", "CM");
        place.setLocation(UUID.randomUUID(), 4.05, 9.76);
        place.getGeography().setLanguageCode("fr");
        
        // When
        placeEventPublisher.placeCreated(place, "user123");
        
        // Then
        verify(outboxRepository).save(argThat(event -> {
            try {
                Map<String, Object> envelope = objectMapper.readValue(
                    event.getPayload(),
                    new TypeReference<Map<String, Object>>() {}
                );
                Map<String, Object> payload = (Map<String, Object>) envelope.get("payload");
                
                return payload.get("countryCode").equals("CM") &&
                       payload.get("latitude") != null &&
                       payload.get("languageCode").equals("fr");
            } catch (Exception e) {
                return false;
            }
        }));
    }
}
```

---

## Étape 8 : Monitoring et Logs

### 8.1 Logs structurés

```java
@Slf4j
@Service
public class PlaceService {
    
    public Place createPlace(CreatePlaceRequest request, String userId) {
        log.info("Creating place in country {}, city {}", 
            request.countryCode(), request.cityId());
        
        try {
            countryConfigClient.validateFeature(
                request.countryCode(),
                CountryFeature.CONTENT_PUBLISHING
            );
        } catch (CountryConfigException e) {
            log.warn("Country validation failed for {}: {}", 
                request.countryCode(), e.getMessage());
            throw e;
        }
        
        // ... création
        
        log.info("Place created: id={}, country={}", 
            place.getId(), place.getGeography().getCountryCode());
        
        return place;
    }
}
```

### 8.2 Métriques

```java
@Component
public class CountryMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordPlaceCreation(String countryCode) {
        meterRegistry.counter("places.created", "country", countryCode).increment();
    }
    
    public void recordCountryValidationFailure(String countryCode, String reason) {
        meterRegistry.counter("country.validation.failed", 
            "country", countryCode,
            "reason", reason
        ).increment();
    }
}
```

---

## Checklist par Service

- [ ] Ajouter shared-lib au pom.xml
- [ ] Configurer CountryConfigClient
- [ ] Étendre entités avec GeographicFields
- [ ] Créer migration Flyway
- [ ] Ajouter validation feature dans service
- [ ] Enrichir événements Kafka
- [ ] Ajouter filtres géographiques aux requêtes
- [ ] Écrire tests multi-pays (6 scénarios minimum)
- [ ] Ajouter logs structurés
- [ ] Configurer métriques
- [ ] Backfill données historiques
- [ ] Déployer et monitorer

---

## Résumé

Cette implémentation fournit :
- ✅ Validation stricte pays et features
- ✅ Champs géographiques standardisés
- ✅ Résilience (circuit breaker, cache, timeout)
- ✅ Événements Kafka enrichis
- ✅ Filtres géographiques pour requêtes
- ✅ Tests complets multi-pays
- ✅ Migration données historiques
- ✅ Monitoring et observabilité
