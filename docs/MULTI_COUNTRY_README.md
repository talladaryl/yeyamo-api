# Multi-Country Implementation - Guide de Démarrage

## 🎯 Vue d'ensemble

Ce dossier contient toute la documentation nécessaire pour l'extension multi-pays de la plateforme YeYamo.

**Statut actuel** : Phase 1 ✅ | Phase 2 🔄

---

## 📚 Documentation Disponible

### 🏁 Commencez ici
1. **[MULTI_COUNTRY_ROLLOUT_SUMMARY.md](./MULTI_COUNTRY_ROLLOUT_SUMMARY.md)**  
   📄 Résumé exécutif : vue d'ensemble, statut, planning

### 📖 Documentation détaillée
2. **[MULTI_COUNTRY_IMPLEMENTATION.md](./MULTI_COUNTRY_IMPLEMENTATION.md)**  
   📄 Implémentation Phase 1 (auth-service + user-service)

3. **[MULTI_COUNTRY_CONTENT_AUDIT.md](./MULTI_COUNTRY_CONTENT_AUDIT.md)**  
   📄 Audit complet des 10 services de contenu à étendre

4. **[MULTI_COUNTRY_SERVICE_IMPLEMENTATION_GUIDE.md](./MULTI_COUNTRY_SERVICE_IMPLEMENTATION_GUIDE.md)**  
   📄 Guide pas-à-pas pour implémenter un service

5. **[MULTI_COUNTRY_COMPATIBILITY_MATRIX.md](./MULTI_COUNTRY_COMPATIBILITY_MATRIX.md)**  
   📄 Matrice de compatibilité et planning détaillé

### 📋 Contrats
6. **[contracts/multi-country-user-events-v1.md](./contracts/multi-country-user-events-v1.md)**  
   📄 Schéma des événements Kafka enrichis

---

## 🚀 Quick Start

### Pour Product Managers
Lire : [MULTI_COUNTRY_ROLLOUT_SUMMARY.md](./MULTI_COUNTRY_ROLLOUT_SUMMARY.md)
- Vue business
- Timeline
- Success metrics

### Pour Développeurs - Nouveau service
Lire : [MULTI_COUNTRY_SERVICE_IMPLEMENTATION_GUIDE.md](./MULTI_COUNTRY_SERVICE_IMPLEMENTATION_GUIDE.md)
1. Ajouter shared-lib au pom.xml
2. Configurer CountryConfigClient
3. Étendre entités avec @Embedded GeographicFields
4. Créer migration Flyway
5. Ajouter validation feature
6. Enrichir événements Kafka
7. Écrire tests (6 scénarios)

### Pour Développeurs - Référence
Voir implémentation complète :
- `auth-service` : Validation pays lors inscription
- `user-service` : Profil multi-pays avec préférences

### Pour DevOps
Configuration requise :
```properties
yeyamo.services.country-config.url=http://country-config-service:8080
resilience4j.circuitbreaker.instances.country-config.*
spring.cache.cache-names=countryConfig
```

---

## 📦 Composants Réutilisables

### CountryConfigClient
**Localisation** : `shared-lib/src/main/java/com/yeyamo_mobile/shared/country/`

**Utilisation** :
```java
@Autowired
private CountryConfigClient countryConfigClient;

// Validate country and feature
countryConfigClient.validateFeature("CM", CountryFeature.CONTENT_PUBLISHING);

// Validate city
countryConfigClient.validateCity("CM", cityId);

// Get country config
CountryConfig config = countryConfigClient.getCountry("CM");
```

**Caractéristiques** :
- ✅ Circuit breaker (50% threshold, 30s wait)
- ✅ Local cache (5 min TTL)
- ✅ Timeout (2s)
- ✅ NO silent fallback

---

### GeographicFields
**Localisation** : `shared-lib/src/main/java/com/yeyamo_mobile/shared/geography/`

**Utilisation** :
```java
@Entity
public class Place {
    @Embedded
    private GeographicFields geography;
    
    public Place(String name, String countryCode) {
        this.geography = new GeographicFields(countryCode);
    }
    
    public void setLocation(UUID cityId, Double lat, Double lon) {
        geography.setCityId(cityId);
        geography.setCoordinates(lat, lon);
    }
}
```

**Champs inclus** :
- countryCode (required)
- adminLevel1Id, adminLevel2Id, cityId, localityId
- latitude, longitude
- languageCode

---

## 🧪 Tests Obligatoires

Pour chaque service, implémenter ces 6 scénarios :

```java
@Test void create_inLiveCountry_succeeds()
@Test void create_inDisabledCountry_throwsException()
@Test void create_whenFeatureDisabled_throwsException()
@Test void create_inComingSoonCountry_throwsException()
@Test void create_withInvalidCity_throwsException()
@Test void create_whenServiceDown_throwsException()
@Test void publishEvent_includesGeographicFields()
```

---

## 📊 Statut Services

| Service | Statut | Docs | Tests | Migration |
|---------|--------|------|-------|-----------|
| ✅ auth-service | Complete | ✅ | ✅ | ✅ |
| ✅ user-service | Complete | ✅ | ✅ | ✅ |
| ✅ country-config | Complete | ✅ | ✅ | ✅ |
| 🔄 content-service | To Do | 📄 | ❌ | ❌ |
| 🔄 catalog-service | To Do | 📄 | ❌ | ❌ |
| 🔄 culture-service | To Do | 📄 | ❌ | ❌ |
| 🔄 event-service | To Do | 📄 | ❌ | ❌ |
| 🔄 partner-service | To Do | 📄 | ❌ | ❌ |
| 🔄 booking-service | To Do | 📄 | ❌ | ❌ |
| 🔄 recommendation | To Do | 📄 | ❌ | N/A |
| 🔄 feed-service | To Do | 📄 | ❌ | N/A |
| 🔄 discovery | To Do | 📄 | ❌ | ❌ |
| 🔄 analytics | To Do | 📄 | ❌ | ❌ |

---

## 🎯 Roadmap

### ✅ Phase 1 : Infrastructure (COMPLÉTÉ)
- country-config-service
- auth-service
- user-service
- Shared library (CountryConfigClient, GeographicFields)

### 🔄 Phase 2 : Content (EN COURS - 3.5 semaines)

**Semaine 1** : Core Content
- content-service (places, posts)
- catalog-service (collections)
- culture-service (artworks, artisans)

**Semaine 2** : Events & Partners
- event-service
- partner-service

**Semaine 3** : Transactions
- booking-service
- recommendation-service
- feed-service

**Semaine 4** : Analytics + Buffer
- discovery-service
- analytics-service
- Tests d'intégration globaux

---

## 🔗 Liens Utiles

### Code
- [auth-service](../auth-service/) : Exemple complet
- [user-service](../user-service/) : Exemple complet
- [shared-lib](../shared-lib/) : Composants communs
- [country-config-service](../country-config-service/) : API configuration

### Documentation
- [Architecture générale](../architecture-yeyamo-complet.md)
- [Modèle géographique générique](./GENERIC_GEOGRAPHY_MODEL.md)

### APIs
- Country Config API : `http://country-config-service:8080/api/v1/countries`
- Swagger UI : `http://localhost:8080/swagger-ui.html` (chaque service)

---

## 💡 Patterns Communs

### Pattern 1 : Validation à la création
```java
public Resource createResource(CreateRequest request) {
    // 1. Validate country feature
    countryConfigClient.validateFeature(
        request.countryCode(), 
        CountryFeature.RELEVANT_FEATURE
    );
    
    // 2. Validate city if provided
    if (request.cityId() != null) {
        countryConfigClient.validateCity(request.countryCode(), request.cityId());
    }
    
    // 3. Create resource
    Resource resource = new Resource(request.name(), request.countryCode());
    
    // 4. Save
    Resource saved = repository.save(resource);
    
    // 5. Publish event with geographic fields
    eventPublisher.resourceCreated(saved);
    
    return saved;
}
```

### Pattern 2 : Requêtes géographiques
```java
public Page<Resource> search(SearchRequest request, String userId) {
    // Get user preferences
    UserProfile user = userService.getProfile(userId);
    
    // Determine countries to search
    Set<String> countries = request.countries() != null
        ? request.countries()
        : user.getContentCountries();
    
    // Query with geographic filters
    return repository.findByCountriesAndCity(
        countries,
        request.cityId(),
        request.pageable()
    );
}
```

### Pattern 3 : Événements enrichis
```java
private void publishEvent(String type, Resource resource) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("resourceId", resource.getId().toString());
    // ... other fields
    
    // CRITICAL : Geographic fields
    payload.put("countryCode", resource.getGeography().getCountryCode());
    payload.put("cityId", resource.getGeography().getCityId());
    payload.put("latitude", resource.getGeography().getLatitude());
    payload.put("longitude", resource.getGeography().getLongitude());
    payload.put("languageCode", resource.getGeography().getLanguageCode());
    
    outbox.publish(type, resource.getId(), payload);
}
```

---

## ❓ FAQ

### Q: Que faire si country-config-service est down ?
**R:** Le circuit breaker ouvrira après 50% d'échecs. Toutes les créations échoueront avec exception explicite. Pas de fallback silencieux.

### Q: Comment gérer les données historiques ?
**R:** Utiliser les migrations SQL pour backfill `country_code = 'CM'` pour données certaines. Logger les profils ambigus pour revue manuelle.

### Q: Faut-il toujours valider la ville ?
**R:** Oui, si fournie. Utiliser `countryConfigClient.validateCity(countryCode, cityId)`.

### Q: Comment tester localement ?
**R:** Utiliser les mocks dans tests. Pour intégration, démarrer country-config-service en local.

### Q: Les événements virtuels ont-ils besoin d'un pays ?
**R:** Non pour événements 100% virtuels. Utiliser `accessibleCountries` pour définir la portée.

---

## 🆘 Support

### Problèmes techniques
1. Vérifier logs : `countryCode` doit apparaître
2. Vérifier circuit breaker : `resilience4j.circuitbreaker.instances.country-config`
3. Vérifier cache : `spring.cache.cache-names=countryConfig`

### Questions architecture
Consulter : [MULTI_COUNTRY_CONTENT_AUDIT.md](./MULTI_COUNTRY_CONTENT_AUDIT.md)

### Questions implémentation
Consulter : [MULTI_COUNTRY_SERVICE_IMPLEMENTATION_GUIDE.md](./MULTI_COUNTRY_SERVICE_IMPLEMENTATION_GUIDE.md)

---

## ✅ Checklist Déploiement

Avant de déployer un service :
- [ ] Dépendance shared-lib ajoutée
- [ ] CountryConfigClient configuré
- [ ] @Embedded GeographicFields ajouté aux entités
- [ ] Migration Flyway créée et testée
- [ ] Validation feature ajoutée
- [ ] Événements Kafka enrichis
- [ ] 6 tests multi-pays passent
- [ ] Backfill données historiques
- [ ] Documentation à jour
- [ ] Monitoring configuré

---

## 📝 Changelog

### 2026-08-13
- ✅ Phase 1 complète (auth, user, country-config)
- ✅ Documentation complète créée
- ✅ Composants shared-lib livrés
- 🔄 Phase 2 démarrée (content services)

---

**Ready to implement? Start with [MULTI_COUNTRY_SERVICE_IMPLEMENTATION_GUIDE.md](./MULTI_COUNTRY_SERVICE_IMPLEMENTATION_GUIDE.md)** 🚀
