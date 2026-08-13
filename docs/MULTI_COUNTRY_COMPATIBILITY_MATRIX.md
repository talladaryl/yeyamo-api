# Matrice de Compatibilité Multi-Pays - Services YeYamo

## Statut Global

| Service | Statut | Priority | Champs Geo | Feature Check | Events | Tests | Migration |
|---------|--------|----------|------------|---------------|---------|-------|-----------|
| auth-service | ✅ Complete | P0 | ✅ | ✅ | ✅ | ✅ | ✅ |
| user-service | ✅ Complete | P0 | ✅ | N/A | ✅ | ✅ | ✅ |
| country-config-service | ✅ Complete | P0 | N/A | N/A | ✅ | ✅ | ✅ |
| content-service | 🔄 To Do | P1 | ❌ | ❌ | ❌ | ❌ | ❌ |
| catalog-service | 🔄 To Do | P1 | ❌ | ❌ | ❌ | ❌ | ❌ |
| culture-service | 🔄 To Do | P1 | ❌ | ❌ | ❌ | ❌ | ❌ |
| event-service | 🔄 To Do | P1 | ❌ | ❌ | ❌ | ❌ | ❌ |
| partner-service | 🔄 To Do | P1 | ❌ | ❌ | ❌ | ❌ | ❌ |
| booking-service | 🔄 To Do | P2 | ❌ | ❌ | ❌ | ❌ | ❌ |
| recommendation-service | 🔄 To Do | P2 | N/A | N/A | ❌ | ❌ | N/A |
| feed-service | 🔄 To Do | P2 | N/A | N/A | ❌ | ❌ | N/A |
| discovery-service | 🔄 To Do | P2 | ⚠️ Partial | ❌ | ❌ | ❌ | ❌ |
| analytics-service | 🔄 To Do | P3 | ❌ | N/A | N/A | ❌ | ❌ |

**Légende** :
- ✅ Complete : Implémenté et testé
- 🔄 To Do : À implémenter
- ⚠️ Partial : Partiellement implémenté
- ❌ Not Started : Pas commencé
- N/A : Non applicable

---

## Détails par Service

### ✅ P0 : Authentification et Profil (COMPLÉTÉ)

#### auth-service
- ✅ CountryCode, cityId, preferredLanguageCode, timezone
- ✅ Validation pays via CountryValidationService
- ✅ Format E.164 pour téléphone
- ✅ Migration V4__add_multi_country_support.sql
- ✅ Tests : CountryValidationServiceTests, AuthServiceMultiCountryTests
- ✅ Événement user.created enrichi

#### user-service
- ✅ Champs géographiques complets (country, admin levels, city, locality)
- ✅ Content preferences (countries, languages, radius)
- ✅ Endpoints : /me/location, /me/language, /me/discovery-preferences
- ✅ Migration V4__add_multi_country_fields.sql
- ✅ Tests : UserProfileMultiCountryTests
- ✅ Événements : location_updated, language_updated, discovery_preferences_updated

#### country-config-service
- ✅ Modèle Country complet avec feature flags
- ✅ API publique : GET /countries/{code}
- ✅ Validation launchStatus et features
- ✅ Support multi-langue et timezone

---

### 🔄 P1 : Contenu et Découverte (À FAIRE - PRIORITAIRE)

#### content-service

**Ressources concernées** :
- Place (lieux)
- Post (publications géolocalisées)
- Story (stories géolocalisées)

**Implémentation requise** :

| Tâche | Estimation | Assigné |
|-------|------------|---------|
| Ajouter GeographicFields à Place | 2h | - |
| Migration V5__add_multi_country_to_places.sql | 1h | - |
| Intégrer CountryConfigClient | 2h | - |
| Validation CONTENT_PUBLISHING | 1h | - |
| Enrichir événements place.* | 1h | - |
| Tests multi-pays | 3h | - |
| Backfill données CM | 1h | - |
| **Total** | **11h** | - |

**Feature checks** :
- `contentPublishingEnabled` pour création de place
- Validation cityId appartient au pays

**Événements** :
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

---

#### catalog-service

**Ressources concernées** :
- Collection (collections éditoriales)
- FeaturedContent (contenus mis en avant)

**Implémentation requise** :

| Tâche | Estimation | Assigné |
|-------|------------|---------|
| Ajouter targetCountries à Collection | 2h | - |
| Ajouter targetLanguages à Collection | 1h | - |
| Ajouter CollectionScope enum | 1h | - |
| Migration V4__add_multi_country_to_collections.sql | 1h | - |
| Filtres géographiques dans queries | 2h | - |
| Tests multi-pays | 2h | - |
| **Total** | **9h** | - |

**Scopes de collection** :
- `COUNTRY_SPECIFIC` : visible dans pays spécifiques
- `AFRICAN` : tous les pays africains  
- `GLOBAL` : tous les pays

---

#### culture-service

**Ressources concernées** :
- Artwork (œuvres d'art)
- CulturalEvent (événements culturels)
- Artisan (artisans)

**Implémentation requise** :

| Tâche | Estimation | Assigné |
|-------|------------|---------|
| Ajouter GeographicFields à Artwork | 2h | - |
| Ajouter originCountryCode | 1h | - |
| Ajouter culturalOrigin | 1h | - |
| Migration V6__add_multi_country_to_artworks.sql | 1h | - |
| Intégrer CountryConfigClient | 2h | - |
| Validation CULTURE_MODULE / ARTISAN_COMMERCE | 1h | - |
| Enrichir événements artwork.* | 1h | - |
| Tests multi-pays | 3h | - |
| Backfill données CM | 1h | - |
| **Total** | **13h** | - |

**Feature checks** :
- `cultureModuleEnabled` pour œuvres d'art
- `artisanCommerceEnabled` pour œuvres commerciales

---

#### event-service

**Ressources concernées** :
- Event (événements)
- Venue (lieux d'événements)

**Implémentation requise** :

| Tâche | Estimation | Assigné |
|-------|------------|---------|
| Ajouter GeographicFields à Event | 2h | - |
| Ajouter isVirtual flag | 1h | - |
| Ajouter accessibleCountries | 1h | - |
| Migration V5__add_multi_country_to_events.sql | 1h | - |
| Intégrer CountryConfigClient | 2h | - |
| Validation conditionnelle (physique vs virtuel) | 2h | - |
| Enrichir événements event.* | 1h | - |
| Tests multi-pays | 3h | - |
| **Total** | **13h** | - |

**Règles** :
- Événements physiques : countryCode obligatoire
- Événements virtuels : accessibleCountries définit la portée

---

#### partner-service

**Ressources concernées** :
- Partner (partenaires)
- PartnerLocation (établissements)

**Implémentation requise** :

| Tâche | Estimation | Assigné |
|-------|------------|---------|
| Ajouter primaryCountryCode à Partner | 1h | - |
| Ajouter operatingCountries | 1h | - |
| Ajouter GeographicFields à PartnerLocation | 2h | - |
| Migration V7__add_multi_country_to_partners.sql | 1h | - |
| Intégrer CountryConfigClient | 2h | - |
| Validation PARTNER_ONBOARDING | 1h | - |
| Enrichir événements partner.* | 1h | - |
| Tests multi-pays | 3h | - |
| **Total** | **12h** | - |

**Feature checks** :
- `partnerOnboardingEnabled` pour onboarding

---

### 🔄 P2 : Transactions et Recommandations (À FAIRE)

#### booking-service

**Implémentation requise** :

| Tâche | Estimation | Assigné |
|-------|------------|---------|
| Ajouter countryCode à Booking | 1h | - |
| Ajouter currencyCode | 1h | - |
| Migration V4__add_multi_country_to_bookings.sql | 1h | - |
| Intégrer CountryConfigClient | 2h | - |
| Validation BOOKING + PAYMENTS | 2h | - |
| Tests multi-pays | 2h | - |
| **Total** | **9h** | - |

**Feature checks** :
- `bookingEnabled` pour réservations
- `paymentsEnabled` si paiement requis

---

#### recommendation-service

**Implémentation requise** :

| Tâche | Estimation | Assigné |
|-------|------------|---------|
| Consumer profile.location_updated | 2h | - |
| Consumer profile.discovery_preferences_updated | 2h | - |
| Filtres par contentCountries | 3h | - |
| Filtres par localRadiusKm | 3h | - |
| Filtre African content | 2h | - |
| Tests multi-pays | 3h | - |
| **Total** | **15h** | - |

**Pas de champs géographiques** : Service consommateur uniquement

---

#### feed-service

**Implémentation requise** :

| Tâche | Estimation | Assigné |
|-------|------------|---------|
| Filtres par user contentCountries | 3h | - |
| Filtres par user contentLanguages | 2h | - |
| Boost local content (radius) | 3h | - |
| Mix algorithmic + editorial par pays | 3h | - |
| Tests multi-pays | 3h | - |
| **Total** | **14h** | - |

---

#### discovery-service

**État actuel** : ⚠️ V3__culture_artisan_support.sql existe

**À vérifier** :
- [ ] Les tables ont-elles déjà des champs géographiques ?
- [ ] Filtres par pays fonctionnent-ils ?

**Implémentation requise** :

| Tâche | Estimation | Assigné |
|-------|------------|---------|
| Audit tables existantes | 1h | - |
| Filtres par user contentCountries | 2h | - |
| Filtres par user localRadiusKm | 2h | - |
| Tests multi-pays | 3h | - |
| **Total** | **8h** | - |

---

### 🔄 P3 : Analytics (À FAIRE - BASSE PRIORITÉ)

#### analytics-service

**Implémentation requise** :

| Tâche | Estimation | Assigné |
|-------|------------|---------|
| Ajouter countryCode à toutes les métriques | 3h | - |
| Ajouter languageCode aux métriques contenu | 2h | - |
| Migration V6__add_multi_country_to_analytics.sql | 1h | - |
| Dashboards par pays | 4h | - |
| Métriques distribution géographique | 2h | - |
| Tests multi-pays | 2h | - |
| **Total** | **14h** | - |

**Nouvelles métriques** :
- Users par pays
- Content par pays
- Engagement rate par pays
- Language distribution

---

## Estimation Totale

| Priorité | Services | Heures | Jours (8h) |
|----------|----------|--------|------------|
| P0 | 3 | ✅ Complété | ✅ |
| P1 | 5 | 58h | 7.25 jours |
| P2 | 4 | 46h | 5.75 jours |
| P3 | 1 | 14h | 1.75 jours |
| **Total** | **13** | **118h** | **~15 jours** |

**Avec buffer 20%** : ~18 jours ouvrés (3.5 semaines)

---

## Plan de Déploiement

### Semaine 1 : P1 - Core Content
- Lundi-Mardi : content-service (11h)
- Mercredi : catalog-service (9h)
- Jeudi-Vendredi : culture-service (13h)

### Semaine 2 : P1 - Events & Partners
- Lundi-Mardi : event-service (13h)
- Mercredi-Jeudi : partner-service (12h)
- Vendredi : Tests d'intégration P1

### Semaine 3 : P2 - Transactions
- Lundi-Mardi : booking-service (9h)
- Mercredi-Jeudi : recommendation-service (15h)
- Vendredi : feed-service (14h)

### Semaine 4 : P2-P3 + Buffer
- Lundi-Mardi : discovery-service (8h)
- Mercredi : analytics-service (14h)
- Jeudi-Vendredi : Tests d'intégration globaux, fixes, documentation

---

## Risques et Mitigation

| Risque | Impact | Probabilité | Mitigation |
|--------|--------|-------------|------------|
| Service indisponible durant validation | Haut | Moyen | Circuit breaker + cache |
| Données historiques ambiguës | Moyen | Haut | Log pour revue manuelle |
| Performance queries géographiques | Moyen | Moyen | Indexes + PostGIS |
| Tests incomplets | Haut | Moyen | Checklist obligatoire |
| Breaking changes événements | Haut | Faible | Versioning événements |

---

## Success Criteria

Pour chaque service :
- ✅ 6 tests multi-pays passent
- ✅ Circuit breaker configuré
- ✅ Événements Kafka enrichis
- ✅ Migration données complète
- ✅ Monitoring en place
- ✅ Documentation à jour

---

## Prochaines Étapes

1. **Validation architecture** avec équipe
2. **Priorisation finale** des services
3. **Assignment** développeurs par service
4. **Kick-off** : Présentation guide d'implémentation
5. **Développement** selon planning
6. **Review** et tests d'intégration
7. **Déploiement** progressif
8. **Monitoring** et ajustements

---

## Ressources

- 📄 [Guide d'implémentation détaillé](./MULTI_COUNTRY_SERVICE_IMPLEMENTATION_GUIDE.md)
- 📄 [Audit complet des services](./MULTI_COUNTRY_CONTENT_AUDIT.md)
- 📄 [Contrats d'événements](./contracts/multi-country-user-events-v1.md)
- 📄 [Implémentation auth/user](./MULTI_COUNTRY_IMPLEMENTATION.md)
