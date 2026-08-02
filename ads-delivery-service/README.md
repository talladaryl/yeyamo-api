# Ads Delivery Service

Service de sélection et de diffusion de publicités sponsorisées pour YeYamo.

## 🎯 Objectif

Sélectionner les campagnes publicitaires éligibles pour un utilisateur et retourner des placements sponsorisés qui peuvent être intégrés progressivement dans différents contextes de l'application.

## ⚠️ Principe Important

**Le service publicitaire ne remplace JAMAIS le classement organique.**  
Il produit une liste séparée de placements sponsorisés que les services appelants peuvent intégrer de manière additive.

## 📍 Placements Supportés (MVP)

- `FEED_CARD` - Carte dans le feed principal
- `DISCOVERY_RESULT` - Résultat dans la découverte
- `SEARCH_RESULT` - Résultat de recherche
- `EVENT_HIGHLIGHT` - Mise en avant d'événement
- `PLACE_HIGHLIGHT` - Mise en avant de lieu
- `MAP_PIN` - Épingle sur la carte
- `PARTNER_PROFILE_BANNER` - Bannière sur profil partenaire

## 🚀 Architecture

### Domain Layer (Pure)
- `AdSelectionContext` - Contexte de sélection d'annonces
- `CampaignProjection` - Projection read-only des campagnes
- `ScoredAd` - Campagne avec score calculé
- `AdDeliveryRecord` - Enregistrement de diffusion (impression/clic/conversion)
- `AdSelectionEngine` - Moteur de sélection et scoring

### Application Layer
- `AdDeliveryService` - Service principal de sélection et tracking
- `SponsoredPlacementProvider` - Interface d'intégration pour autres services

### Infrastructure Layer
- **PostgreSQL** - Projections de campagnes, historique de diffusion
- **Redis** - Frequency caps, réservations de budget, cache
- **Kafka** - Consommation d'événements de campagnes, publication d'événements de tracking

## 🧮 Algorithme de Sélection (v1.0)

```
score = bidScore × 0.30
      + geographicRelevance × 0.20
      + interestAffinity × 0.20
      + contextualRelevance × 0.15
      + qualityScore × 0.10
      + freshnessScore × 0.05
      + exploration (5%)
```

### Filtres d'Éligibilité
1. Campagne ACTIVE
2. Dans la période de validité
3. Budget disponible
4. Correspond au placement
5. Ciblage géographique compatible
6. Langue compatible
7. Pas dans les campagnes exclues

### Reason Codes
- `LOCAL_MATCH` - Correspondance ville/district
- `REGIONAL_MATCH` - Correspondance région
- `INTEREST_MATCH` - Correspondance centres d'intérêt
- `CONTEXTUAL_MATCH` - Correspondance type d'entité
- `HIGH_QUALITY` - Score qualité ≥ 80
- `NEW_CAMPAIGN` - Campagne récente (< 7 jours)
- `GENERAL_RELEVANCE` - Pertinence générale

## 🔐 Sécurité du Tracking

### Tokens Signés
- Format: `PREFIX.deliveryId.campaignId.userId.expiresAt.signature`
- Signature HMAC-SHA256
- Expiration: 5 minutes par défaut
- Protection contre la falsification
- Protection contre le replay

### Anti-Fraude
- Vérification de signature obligatoire
- Expiration courte des tokens
- Déduplication des impressions
- Impression qualifiée: durée de vue ≥ 1 seconde
- Idempotence sur deliveryId

### Privacy
- Pas d'exposition des centres d'intérêt
- Pas de position GPS précise vers les partenaires
- Raisons générales d'affichage (pas de détails sensibles)
- Données minimales conservées

## 💰 Gestion du Budget

### Réservation Atomique
1. Réservation du coût estimé (Redis, TTL 60s)
2. Affichage de l'annonce
3. Confirmation ou libération de la réservation

### Protection Concurrence
- Réservations Redis avec TTL
- Mise à jour atomique du budget dépensé
- Publication `campaign.budget.exhausted` si nécessaire

### Type de Données
- ⚠️ **JAMAIS de Double** pour les montants
- **BigDecimal** obligatoire
- Devise: **XAF** (Franc CFA)

## 🎚️ Feature Flags

Tous les flags sont **désactivés par défaut** :

```properties
yeyamo.ads.feature-flags.ads-delivery-enabled=false
yeyamo.ads.feature-flags.ads-feed-enabled=false
yeyamo.ads.feature-flags.ads-discovery-enabled=false
yeyamo.ads.feature-flags.ads-search-enabled=false
yeyamo.ads.feature-flags.ads-map-enabled=false
yeyamo.ads.feature-flags.ads-event-highlight-enabled=false
yeyamo.ads.feature-flags.ads-place-highlight-enabled=false
yeyamo.ads.feature-flags.ads-partner-profile-banner-enabled=false
```

## 📊 Événements Kafka

### Consommés (topic: campaign-events)
- `CampaignActivated` - Synchroniser la projection
- `CampaignPaused` - Désactiver la diffusion
- `CampaignCancelled` - Retirer la campagne
- `CampaignBudgetExhausted` - Arrêter la diffusion
- `CampaignCompleted` - Archiver

### Publiés (topic: ad-events)
- `ad.impression.recorded` - Impression comptabilisée
- `ad.click.recorded` - Clic enregistré
- `ad.conversion.recorded` - Conversion trackée
- `ad.delivery.rejected` - Diffusion rejetée

## 🔌 API Endpoints

### POST /api/v1/ads/select
Sélectionner des placements sponsorisés

**Request:**
```json
{
  "userId": "optional",
  "anonymousSessionId": "optional",
  "placement": "FEED_CARD",
  "countryCode": "CM",
  "regionId": "CM-CE",
  "cityId": "YAO",
  "latitude": 3.8667,
  "longitude": 11.5167,
  "interestIds": ["INT1", "INT2"],
  "language": "fr",
  "deviceType": "MOBILE",
  "requestTimestamp": "2026-08-02T10:00:00Z",
  "limit": 3
}
```

**Response:**
```json
[
  {
    "deliveryId": "uuid",
    "campaignId": "CAMP123",
    "promotedEntityType": "PLACE",
    "promotedEntityId": "PLACE456",
    "placement": "FEED_CARD",
    "creative": { ... },
    "disclosureLabel": "Sponsorisé",
    "reasonCodes": ["LOCAL_MATCH", "INTEREST_MATCH"],
    "impressionTrackingToken": "signed-token",
    "clickTrackingToken": "signed-token",
    "expiresAt": "2026-08-02T10:05:00Z",
    "rankPosition": 1,
    "deliveryPolicyVersion": "1.0"
  }
]
```

### POST /api/v1/ads/impressions
Enregistrer une impression

### POST /api/v1/ads/clicks
Enregistrer un clic

### POST /api/v1/ads/conversions
Enregistrer une conversion

## 🧪 Tests

### Tests Unitaires
```bash
mvn test
```

### Tests d'Intégration (Testcontainers)
```bash
mvn verify
```

### Scénarios Testés
- ✅ Ciblage géographique
- ✅ Filtrage par dates
- ✅ Vérification budget
- ✅ Frequency caps
- ✅ Gestion de la concurrence
- ✅ Validation des tokens
- ✅ Anti-replay
- ✅ Idempotence
- ✅ Projection Kafka
- ✅ Campagne suspendue
- ✅ Privacy (pas d'exposition données sensibles)
- ✅ Feature flags

## 🚦 Configuration Requise

### PostgreSQL
- Base: `yeyamo_ads`
- Tables: `campaign_projections`, `ad_delivery_records`, `ad_conversions`, `delivery_policy_versions`

### Redis
- Frequency caps (TTL: 1h/24h)
- Budget reservations (TTL: 60s)
- Cache de campagnes éligibles

### Kafka
- Topics: `campaign-events`, `ad-events`
- Consumer group: `ads-delivery-service`

## 🔧 Variables d'Environnement

```bash
# Obligatoire en production
ADS_TOKEN_SECRET=your-secure-secret-key-here

# Optionnel
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/yeyamo_ads
SPRING_REDIS_HOST=localhost
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

## 📈 Métriques

Exposées via Actuator + Prometheus:
- `ads_selection_total` - Nombre de sélections
- `ads_impression_total` - Nombre d'impressions
- `ads_click_total` - Nombre de clics
- `ads_conversion_total` - Nombre de conversions
- `ads_budget_reserved_total` - Budget réservé
- `ads_selection_duration_seconds` - Durée de sélection

## 🔄 Intégration Additive

Pour intégrer dans un service existant (feed, discovery, search):

```java
@Service
public class FeedService {
    
    private final SponsoredPlacementProvider adsProvider;
    
    public FeedResponse getFeed(FeedRequest request) {
        // 1. Obtenir le contenu organique (INCHANGÉ)
        List<FeedItem> organicItems = getOrganicFeed(request);
        
        // 2. Obtenir les placements sponsorisés (ADDITIF)
        List<SponsoredPlacement> sponsored = adsProvider.getSponsoredPlacements(
            buildAdRequest(request)
        );
        
        // 3. Mélanger si feature flag activé (OPTIONNEL)
        if (featureFlags.isEnabled("ads_feed_enabled")) {
            return mergeSponsoredContent(organicItems, sponsored);
        }
        
        return new FeedResponse(organicItems);
    }
}
```

## ⚠️ Important

- Ne JAMAIS connecter les placements au feed en production sans validation explicite
- Feature flags désactivés par défaut
- Pas de modification du ranking organique
- Échec silencieux (retourne liste vide en cas d'erreur)
- Respect de la privacy (RGPD-compliant)

## 📝 Prochaines Étapes

1. ✅ Implémentation MVP complète
2. ⏳ Tests d'intégration complets
3. ⏳ Load testing et optimisations
4. ⏳ Intégration progressive par placement
5. ⏳ Monitoring et alertes
6. ⏳ A/B testing de l'algorithme
