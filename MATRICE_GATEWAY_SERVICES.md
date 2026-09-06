# MATRICE D'ALIGNEMENT GATEWAY ↔ MICROSERVICES — YEYAMO-API

> Ce document détaille la réconciliation exhaustive entre les **règles de routage configurées dans l'API Gateway** (`cloud-conf-yeyamo/api-gateway.properties`) et les **contrôleurs réels des microservices**.

---

## 1. Synthèse Statistique du Routage Gateway

- **Nombre total de routes REST implémentées dans les services** : 627
- **Routes exposées et correctement acheminées par le Gateway** : 549
- **Routes avec conflit d'acheminement / mauvais service cible (Mismatch d'ordre)** : 13
- **Routes de microservices NON exposées via Gateway (Orphelines Gateway)** : 65
- **Prédicats Gateway sans contrôleur backend correspondant (Routes fantômes)** : 16

---

## 2. Matrice Complète Gateway ↔ Service Cible

| ID Route Gateway | Prédicat Gateway | Service Cible | Contrôleur Détecté | Path Service Cible | Auth Requise | Verdict & Risque |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `ticket-service` (ord: -200) | `/api/v1/tickets/**` | `ticket-service` | PublicTicketTypeController<br>ScanController<br>UserTicketController | `GET /api/v1/tickets/events/{eventId}/types`<br>`POST /api/v1/tickets/scan`<br>`GET /api/v1/tickets/scans/stats/{eventId}`<br>*(+13 routes)* | JWT / Variable | **OK — VALIDE** |
| `ticket-service` (ord: -200) | `/api/v1/partners/*/tickets/**` | `ticket-service` | PartnerTicketController | `PUT /api/v1/partners/{partnerId}/tickets/configuration`<br>`POST /api/v1/partners/{partnerId}/tickets/configurations/{id}/types`<br>`GET /api/v1/partners/{partnerId}/tickets/events/{eventId}/types`<br>*(+5 routes)* | JWT / Variable | **OK — VALIDE** |
| `ticket-service` (ord: -200) | `/api/v1/admin/events/*/tickets` | `ticket-service` | AdminEventTicketController | `GET /api/v1/admin/events/{eventId}/tickets` | JWT / Variable | **OK — VALIDE** |
| `ticket-service` (ord: -200) | `/api/v1/admin/events/*/participants` | `ticket-service` | AdminEventTicketController | `GET /api/v1/admin/events/{eventId}/participants` | JWT / Variable | **OK — VALIDE** |
| `ticket-service` (ord: -200) | `/api/v1/admin/events/*/ticketing-stats` | `ticket-service` | AdminEventTicketController | `GET /api/v1/admin/events/{eventId}/ticketing-stats` | JWT / Variable | **OK — VALIDE** |
| `country-config-cities` (ord: -160) | `/api/v1/cities/*/localities` | `country-config-service` | GeographyController | `GET /api/v1/cities/{id}/localities` | JWT / Variable | **OK — VALIDE** |
| `country-config-cities` (ord: -160) | `/api/v1/cities/*/localities/**` | `country-config-service` | GeographyController | `GET /api/v1/cities/{id}/localities/paged` | JWT / Variable | **OK — VALIDE** |
| `country-config-service` (ord: -150) | `/api/v1/countries/**` | `country-config-service` | CountryController<br>GeographyController | `GET /api/v1/countries/available`<br>`GET /api/v1/countries/{code}`<br>`GET /api/v1/countries/{code}/configuration`<br>*(+11 routes)* | JWT / Variable | **OK — VALIDE** |
| `country-config-service` (ord: -150) | `/api/v1/administrative-areas/**` | `country-config-service` | GeographyController | `GET /api/v1/administrative-areas/{id}`<br>`GET /api/v1/administrative-areas/{id}/children` | JWT / Variable | **OK — VALIDE** |
| `country-config-service` (ord: -150) | `/api/v1/localities/**` | `country-config-service` | GeographyController | `GET /api/v1/localities/{id}` | JWT / Variable | **OK — VALIDE** |
| `country-config-service` (ord: -150) | `/api/v1/admin/countries/**` | `country-config-service` | CountryAdminController | `GET /api/v1/admin/countries/{code}`<br>`PUT /api/v1/admin/countries/{code}`<br>`PATCH /api/v1/admin/countries/{code}/launch-status`<br>*(+1 routes)* | JWT / Variable | **OK — VALIDE** |
| `catalog-artisan-artworks` (ord: -140) | `/api/v1/artisans/*/artworks` | `catalog-service` | ArtworkController | `GET /api/v1/artisans/{artisanId}/artworks` | JWT / Variable | **OK — VALIDE** |
| `catalog-artworks` (ord: -130) | `/api/v1/artworks/**` | `catalog-service` | ArtworkController | `GET /api/v1/artworks/{id}`<br>`GET /api/v1/artworks/{id}/history`<br>`GET /api/v1/artworks/{id}/media`<br>*(+6 routes)* | JWT / Variable | **OK — VALIDE** |
| `partner-artisans` (ord: -130) | `/api/v1/artisans/**` | `partner-service` | ArtisanController | `GET /api/v1/artisans/{id}` | JWT / Variable | **OK — VALIDE** |
| `partner-artisans` (ord: -130) | `/api/v1/admin/artisans/**` | `partner-service` | AdminArtisanController | `PATCH /api/v1/admin/artisans/{id}/verification` | JWT / Variable | **OK — VALIDE** |
| `partner-artisans` (ord: -130) | `/api/v1/artisan-specialties/**` | `partner-service` | *Aucun* | *Aucune route trouvée* | - | **ROUTE FANTÔME** : Prédicat Gateway sans aucun endpoint backend existant! |
| `partner-artisans` (ord: -130) | `/api/v1/partners/me/artisan-profile` | `partner-service` | ArtisanController | `POST /api/v1/partners/me/artisan-profile`<br>`GET /api/v1/partners/me/artisan-profile`<br>`PUT /api/v1/partners/me/artisan-profile` | JWT / Variable | **OK — VALIDE** |
| `commerce-artwork-orders` (ord: -130) | `/api/v1/artwork-orders/**` | `commerce-service` | ArtworkCommerceController | `GET /api/v1/artwork-orders/me`<br>`GET /api/v1/artwork-orders/{id}`<br>`POST /api/v1/artwork-orders/{id}/cancel` | JWT / Variable | **OK — VALIDE** |
| `commerce-artwork-orders` (ord: -130) | `/api/v1/artisan/orders/**` | `commerce-service` | ArtworkCommerceController | `GET /api/v1/artisan/orders/{id}`<br>`PATCH /api/v1/artisan/orders/{id}/status` | JWT / Variable | **OK — VALIDE** |
| `commerce-artwork-offers` (ord: -130) | `/api/v1/artwork-offers/**` | `commerce-service` | ArtworkCommerceController | `GET /api/v1/artwork-offers/{artworkId}`<br>`PUT /api/v1/artwork-offers/{id}`<br>`PATCH /api/v1/artwork-offers/{id}/status` | JWT / Variable | **OK — VALIDE** |
| `culture-languages` (ord: -130) | `/api/v1/languages/**` | `culture-service` | *Aucun* | *Aucune route trouvée* | - | **ROUTE FANTÔME** : Prédicat Gateway sans aucun endpoint backend existant! |
| `culture-traditions` (ord: -130) | `/api/v1/traditions/**` | `culture-service` | *Aucun* | *Aucune route trouvée* | - | **ROUTE FANTÔME** : Prédicat Gateway sans aucun endpoint backend existant! |
| `culture-service` (ord: -120) | `/api/v1/culture/**` | `culture-service` | CultureChallengeController<br>CultureContributionController<br>PublicCultureController | `GET /api/v1/culture/challenges`<br>`GET /api/v1/culture/challenges/{id}`<br>`POST /api/v1/culture/challenges/{id}/join`<br>*(+27 routes)* | JWT / Variable | **OK — VALIDE** |
| `culture-service` (ord: -120) | `/api/v1/admin/culture/**` | `culture-service` | AdminCultureController | `GET /api/v1/admin/culture/contents`<br>`GET /api/v1/admin/culture/contents/{id}`<br>`POST /api/v1/admin/culture/contents`<br>*(+5 routes)* | JWT / Variable | **OK — VALIDE** |
| `graph-service` (ord: -120) | `/api/v1/culture-graph/**` | `graph-service` | CultureGraphController | `GET /api/v1/culture-graph/artworks/{id}/related`<br>`GET /api/v1/culture-graph/cultures/{id}/explore`<br>`GET /api/v1/culture-graph/languages/{code}/related`<br>*(+3 routes)* | JWT / Variable | **OK — VALIDE** |
| `graph-service` (ord: -120) | `/api/v1/admin/culture-graph/**` | `graph-service` | CultureGraphController | `POST /api/v1/admin/culture-graph/rebuild` | JWT / Variable | **OK — VALIDE** |
| `auth-service-admin-platform-users` (ord: -110) | `/api/v1/admin/platform-users/**` | `auth-service` | AdminPlatformUserController | `GET /api/v1/admin/platform-users/{id}`<br>`PATCH /api/v1/admin/platform-users/{id}/status`<br>`PATCH /api/v1/admin/platform-users/{id}/roles`<br>*(+3 routes)* | JWT / Variable | **OK — VALIDE** |
| `auth-service-admin-platform-users` (ord: -110) | `/api/v1/admin/platform-users` | `auth-service` | AdminPlatformUserController | `GET /api/v1/admin/platform-users` | JWT / Variable | **OK — VALIDE** |
| `event-service` (ord: -100) | `/api/v1/events/**` | `event-service` | EventController | `GET /api/v1/events/upcoming`<br>`GET /api/v1/events/me`<br>`GET /api/v1/events/{id}`<br>*(+5 routes)* | JWT / Variable | **OK — VALIDE** |
| `event-service` (ord: -100) | `/api/v1/admin/events/**` | `event-service` | AdminEventController | `GET /api/v1/admin/events/{id}`<br>`PUT /api/v1/admin/events/{id}`<br>`PATCH /api/v1/admin/events/{id}/status`<br>*(+4 routes)* | JWT / Variable | **OK — VALIDE** |
| `event-service` (ord: -100) | `/api/v1/admin/events` | `event-service` | AdminEventController | `GET /api/v1/admin/events`<br>`POST /api/v1/admin/events` | JWT / Variable | **OK — VALIDE** |
| `place-service` (ord: -100) | `/api/v1/places/**` | `place-service` | PlaceController | `GET /api/v1/places/nearby`<br>`GET /api/v1/places/{id}`<br>`PUT /api/v1/places/{id}` | JWT / Variable | **OK — VALIDE** |
| `place-service` (ord: -100) | `/api/v1/regions/**` | `place-service` | RegionController | `GET /api/v1/regions/{slug}`<br>`GET /api/v1/regions/{slug}/places`<br>`PUT /api/v1/regions/{id}`<br>*(+2 routes)* | JWT / Variable | **OK — VALIDE** |
| `place-service` (ord: -100) | `/api/v1/cities/**` | `place-service` | CityController | `GET /api/v1/cities/{id}/places`<br>`GET /api/v1/cities/region/{regionId}`<br>`PUT /api/v1/cities/{id}`<br>*(+2 routes)* | JWT / Variable | **OK — VALIDE** |
| `place-service` (ord: -100) | `/api/v1/districts/**` | `place-service` | DistrictController | `GET /api/v1/districts/city/{cityId}`<br>`PUT /api/v1/districts/{id}`<br>`PATCH /api/v1/districts/{id}/status`<br>*(+1 routes)* | JWT / Variable | **OK — VALIDE** |
| `place-service` (ord: -100) | `/api/v1/categories/**` | `place-service` | CategoryController | `PUT /api/v1/categories/{id}`<br>`PATCH /api/v1/categories/{id}/status`<br>`DELETE /api/v1/categories/{id}` | JWT / Variable | **OK — VALIDE** |
| `place-service` (ord: -100) | `/api/v1/countries/**` | `place-service` | CountryController | `GET /api/v1/countries/{countryCode}`<br>`GET /api/v1/countries/{countryCode}/administrative-areas`<br>`GET /api/v1/countries/{countryCode}/languages` | JWT / Variable | **OK — VALIDE** |
| `place-service` (ord: -100) | `/api/v1/countries` | `place-service` | CountryController | `GET /api/v1/countries` | JWT / Variable | **OK — VALIDE** |
| `place-service` (ord: -100) | `/api/v1/admin/places/**` | `place-service` | AdminPlaceController | `GET /api/v1/admin/places/{id}`<br>`PUT /api/v1/admin/places/{id}`<br>`PATCH /api/v1/admin/places/{id}/status` | JWT / Variable | **OK — VALIDE** |
| `place-service` (ord: -100) | `/api/v1/admin/places` | `place-service` | AdminPlaceController | `GET /api/v1/admin/places`<br>`POST /api/v1/admin/places` | JWT / Variable | **OK — VALIDE** |
| `partner-service` (ord: -100) | `/api/v1/partners/**` | `partner-service` | ArtisanController<br>PartnerController<br>PartnerOnboardingController | `POST /api/v1/partners/me/artisan-profile`<br>`GET /api/v1/partners/me/artisan-profile`<br>`PUT /api/v1/partners/me/artisan-profile`<br>*(+19 routes)* | JWT / Variable | **OK — VALIDE** |
| `partner-service` (ord: -100) | `/api/v1/admin/partners/**` | `partner-service` | AdminPartnerController | `GET /api/v1/admin/partners/{id}`<br>`GET /api/v1/admin/partners/{id}/kyc`<br>`GET /api/v1/admin/partners/{id}/validation-history`<br>*(+2 routes)* | JWT / Variable | **OK — VALIDE** |
| `partner-service` (ord: -100) | `/api/v1/admin/partners` | `partner-service` | AdminPartnerController | `GET /api/v1/admin/partners` | JWT / Variable | **OK — VALIDE** |
| `interaction-service` (ord: -100) | `/api/v1/interactions/**` | `interaction-service` | GenericInteractionController<br>InteractionController | `POST /api/v1/interactions/{targetType}/{targetId}/{type}`<br>`DELETE /api/v1/interactions/{targetType}/{targetId}/{type}`<br>`GET /api/v1/interactions/{targetType}/{targetId}/{type}/status`<br>*(+20 routes)* | JWT / Variable | **OK — VALIDE** |
| `interaction-service` (ord: -100) | `/api/v1/checkins/**` | `interaction-service` | CheckInController | `GET /api/v1/checkins/me` | JWT / Variable | **OK — VALIDE** |
| `interaction-service` (ord: -100) | `/api/v1/saves/**` | `interaction-service` | *Aucun* | *Aucune route trouvée* | - | **ROUTE FANTÔME** : Prédicat Gateway sans aucun endpoint backend existant! |
| `interaction-service` (ord: -100) | `/api/v1/admin/reviews/**` | `interaction-service` | AdminContentController | `GET /api/v1/admin/reviews/{id}`<br>`PATCH /api/v1/admin/reviews/{id}/hide`<br>`PATCH /api/v1/admin/reviews/{id}/restore`<br>*(+1 routes)* | JWT / Variable | **OK — VALIDE** |
| `interaction-service` (ord: -100) | `/api/v1/admin/reviews` | `interaction-service` | AdminContentController | `GET /api/v1/admin/reviews` | JWT / Variable | **OK — VALIDE** |
| `interaction-service` (ord: -100) | `/api/v1/admin/comments/**` | `interaction-service` | AdminContentController | `GET /api/v1/admin/comments/{id}`<br>`PATCH /api/v1/admin/comments/{id}/hide`<br>`PATCH /api/v1/admin/comments/{id}/restore`<br>*(+2 routes)* | JWT / Variable | **OK — VALIDE** |
| `interaction-service` (ord: -100) | `/api/v1/admin/comments` | `interaction-service` | AdminContentController | `GET /api/v1/admin/comments` | JWT / Variable | **OK — VALIDE** |
| `discovery-service` (ord: -100) | `/api/v1/discovery/**` | `discovery-service` | DiscoveryController | `GET /api/v1/discovery/search`<br>`GET /api/v1/discovery/trending` | JWT / Variable | **OK — VALIDE** |
| `discovery-service` (ord: -100) | `/api/v1/maps/**` | `discovery-service` | MapsController | `GET /api/v1/maps/geocode`<br>`GET /api/v1/maps/reverse-geocode`<br>`POST /api/v1/maps/route` | JWT / Variable | **OK — VALIDE** |
| `discovery-service` (ord: -100) | `/api/v1/admin/search/**` | `discovery-service` | SearchAdminController | `GET /api/v1/admin/search/overview`<br>`GET /api/v1/admin/search/indexes`<br>`POST /api/v1/admin/search/reindex`<br>*(+17 routes)* | JWT / Variable | **OK — VALIDE** |
| `gamification-service` (ord: -100) | `/api/v1/me/xp/**` | `gamification-service` | *Aucun* | *Aucune route trouvée* | - | **ROUTE FANTÔME** : Prédicat Gateway sans aucun endpoint backend existant! |
| `gamification-service` (ord: -100) | `/api/v1/me/badges/**` | `gamification-service` | GamificationController | `GET /api/v1/me/badges/catalog`<br>`GET /api/v1/me/badges/catalog/{code}`<br>`GET /api/v1/me/badges/stats` | JWT / Variable | **OK — VALIDE** |
| `gamification-service` (ord: -100) | `/api/v1/me/leaderboard/**` | `gamification-service` | *Aucun* | *Aucune route trouvée* | - | **ROUTE FANTÔME** : Prédicat Gateway sans aucun endpoint backend existant! |
| `gamification-service` (ord: -100) | `/api/v1/me/passport/**` | `gamification-service` | *Aucun* | *Aucune route trouvée* | - | **ROUTE FANTÔME** : Prédicat Gateway sans aucun endpoint backend existant! |
| `gamification-service` (ord: -100) | `/api/v1/me/streaks/**` | `gamification-service` | *Aucun* | *Aucune route trouvée* | - | **ROUTE FANTÔME** : Prédicat Gateway sans aucun endpoint backend existant! |
| `gamification-service` (ord: -100) | `/api/v1/me/rewards/**` | `gamification-service` | GamificationController | `POST /api/v1/me/rewards/{id}/claim` | JWT / Variable | **OK — VALIDE** |
| `gamification-service` (ord: -100) | `/api/v1/admin/gamification/**` | `gamification-service` | GamificationAdminController | `GET /api/v1/admin/gamification/xp-ledger` | JWT / Variable | **OK — VALIDE** |
| `notification-service` (ord: -100) | `/api/v1/notifications/**` | `notification-service` | NotificationController | `GET /api/v1/notifications/unread`<br>`GET /api/v1/notifications/unread/count`<br>`POST /api/v1/notifications/{id}/read`<br>*(+6 routes)* | JWT / Variable | **OK — VALIDE** |
| `notification-service` (ord: -100) | `/api/v1/admin/newsletters/**` | `notification-service` | NewsletterAdminController | `GET /api/v1/admin/newsletters/{id}`<br>`PUT /api/v1/admin/newsletters/{id}`<br>`POST /api/v1/admin/newsletters/{id}/schedule`<br>*(+4 routes)* | JWT / Variable | **OK — VALIDE** |
| `notification-service` (ord: -100) | `/api/v1/admin/notifications/**` | `notification-service` | AdminNotificationController | `GET /api/v1/admin/notifications/unread-count`<br>`PATCH /api/v1/admin/notifications/{id}/read`<br>`PATCH /api/v1/admin/notifications/{id}/unread`<br>*(+1 routes)* | JWT / Variable | **OK — VALIDE** |
| `campaign-service` (ord: -100) | `/api/v1/campaigns/**` | `campaign-service` | CampaignController | `PUT /api/v1/campaigns/{id}`<br>`POST /api/v1/campaigns/{id}/submit`<br>`POST /api/v1/campaigns/{id}/activate`<br>*(+5 routes)* | JWT / Variable | **OK — VALIDE** |
| `campaign-service` (ord: -100) | `/api/v1/admin/campaigns/**` | `campaign-service` | AdminCampaignController | `GET /api/v1/admin/campaigns/{id}`<br>`POST /api/v1/admin/campaigns/{id}/approve`<br>`POST /api/v1/admin/campaigns/{id}/reject` | JWT / Variable | **OK — VALIDE** |
| `support-service` (ord: -100) | `/api/v1/admin/support/**` | `support-service` | SupportAdminController | `GET /api/v1/admin/support/conversations`<br>`GET /api/v1/admin/support/conversations/{id}`<br>`POST /api/v1/admin/support/conversations/{id}/messages`<br>*(+4 routes)* | JWT / Variable | **OK — VALIDE** |
| `auth-service` (ord: 0) | `/api/v1/auth/**` | `auth-service` | AuthController | `POST /api/v1/auth/register`<br>`POST /api/v1/auth/login`<br>`POST /api/v1/auth/oauth/google`<br>*(+12 routes)* | JWT / Variable | **OK — VALIDE** |
| `event-service-place-events` (ord: 0) | `/api/v1/places/*/events` | `event-service` | PlaceEventController | `GET /api/v1/places/{placeId}/events` | JWT / Variable | **OK — VALIDE** |
| `admin-service` (ord: 0) | `/api/v1/admin/**` | `admin-service` | AdminUserController<br>AuditController<br>GovernanceController | `GET /api/v1/admin/users`<br>`GET /api/v1/admin/users/{id}`<br>`POST /api/v1/admin/users`<br>*(+23 routes)* | JWT / Variable | **ATTENTION ORDRE**<br>Intercepte aussi 104 routes de `auth-service` |
| `analytics-service` (ord: 0) | `/api/v1/analytics/**` | `analytics-service` | AnalyticsController<br>BusinessAnalyticsController<br>CultureAnalyticsController | `GET /api/v1/analytics/admin/dashboard`<br>`GET /api/v1/analytics/kpis`<br>`GET /api/v1/analytics/kpis/{kpiName}`<br>*(+25 routes)* | JWT / Variable | **OK — VALIDE** |
| `user-service` (ord: 0) | `/api/v1/users/**` | `user-service` | SocialGraphController<br>UserProfileController | `POST /api/v1/users/social/{userId}/follow`<br>`DELETE /api/v1/users/social/{userId}/follow`<br>`GET /api/v1/users/social/following`<br>*(+22 routes)* | JWT / Variable | **OK — VALIDE** |
| `catalog-service` (ord: 0) | `/api/v1/catalog/assets/**` | `catalog-service` | CatalogAssetController | `GET /api/v1/catalog/assets/manage`<br>`GET /api/v1/catalog/assets/{id}`<br>`GET /api/v1/catalog/assets/manage/{id}`<br>*(+7 routes)* | JWT / Variable | **OK — VALIDE** |
| `catalog-service` (ord: 0) | `/api/v1/catalog/regions/**` | `catalog-service` | *Aucun* | *Aucune route trouvée* | - | **ROUTE FANTÔME** : Prédicat Gateway sans aucun endpoint backend existant! |
| `catalog-service` (ord: 0) | `/api/v1/catalog/cities/**` | `catalog-service` | *Aucun* | *Aucune route trouvée* | - | **ROUTE FANTÔME** : Prédicat Gateway sans aucun endpoint backend existant! |
| `catalog-service` (ord: 0) | `/api/v1/catalog/categories/**` | `catalog-service` | *Aucun* | *Aucune route trouvée* | - | **ROUTE FANTÔME** : Prédicat Gateway sans aucun endpoint backend existant! |
| `catalog-service` (ord: 0) | `/api/v1/collections/**` | `catalog-service` | CollectionController | `GET /api/v1/collections/public`<br>`GET /api/v1/collections/{id}`<br>`GET /api/v1/collections/summaries`<br>*(+5 routes)* | JWT / Variable | **OK — VALIDE** |
| `ingestion-service` (ord: 0) | `/api/v1/catalog/imports/**` | `ingestion-service` | IngestionController | `GET /api/v1/catalog/imports/{id}`<br>`POST /api/v1/catalog/imports/{id}/retry`<br>`POST /api/v1/catalog/imports/{id}/cancel`<br>*(+1 routes)* | JWT / Variable | **OK — VALIDE** |
| `content-service` (ord: 0) | `/api/v1/posts/**` | `content-service` | PostController | `PUT /api/v1/posts/{id}`<br>`POST /api/v1/posts/{id}/publish`<br>`PATCH /api/v1/posts/{id}/visibility`<br>*(+7 routes)* | JWT / Variable | **OK — VALIDE** |
| `content-service` (ord: 0) | `/api/v1/stories/**` | `content-service` | StoryController | `GET /api/v1/stories/{id}`<br>`POST /api/v1/stories/{id}/view`<br>`DELETE /api/v1/stories/{id}` | JWT / Variable | **OK — VALIDE** |
| `feed-service` (ord: 0) | `/api/v1/feed/**` | `feed-service` | *Aucun* | *Aucune route trouvée* | - | **ROUTE FANTÔME** : Prédicat Gateway sans aucun endpoint backend existant! |
| `recommendation-service` (ord: 0) | `/api/v1/recommendations/**` | `recommendation-service` | *Aucun* | *Aucune route trouvée* | - | **ROUTE FANTÔME** : Prédicat Gateway sans aucun endpoint backend existant! |
| `mission-reward-service` (ord: 0) | `/api/v1/missions/**` | `mission-reward-service` | *Aucun* | *Aucune route trouvée* | - | **ROUTE FANTÔME** : Prédicat Gateway sans aucun endpoint backend existant! |
| `mission-reward-service` (ord: 0) | `/api/v1/me/missions/**` | `mission-reward-service` | *Aucun* | *Aucune route trouvée* | - | **ROUTE FANTÔME** : Prédicat Gateway sans aucun endpoint backend existant! |
| `mission-reward-service` (ord: 0) | `/api/v1/me/mission-rewards/**` | `mission-reward-service` | *Aucun* | *Aucune route trouvée* | - | **ROUTE FANTÔME** : Prédicat Gateway sans aucun endpoint backend existant! |
| `mission-reward-service` (ord: 0) | `/api/v1/mission-management/**` | `mission-reward-service` | GamificationAdminController<br>MissionController | `GET /api/v1/mission-management/badges`<br>`GET /api/v1/mission-management/badges/{id}`<br>`POST /api/v1/mission-management/badges`<br>*(+17 routes)* | JWT / Variable | **OK — VALIDE** |
| `referral-service` (ord: 0) | `/api/v1/referrals/**` | `referral-service` | ReferralController | `POST /api/v1/referrals/codes`<br>`POST /api/v1/referrals/invitations`<br>`POST /api/v1/referrals/redeem`<br>*(+1 routes)* | JWT / Variable | **OK — VALIDE** |
| `referral-service` (ord: 0) | `/api/v1/me/referrals/**` | `referral-service` | ReferralController | `GET /api/v1/me/referrals/codes`<br>`GET /api/v1/me/referrals/invitations`<br>`GET /api/v1/me/referrals/attributions`<br>*(+2 routes)* | JWT / Variable | **OK — VALIDE** |
| `moderation-trust-service` (ord: 0) | `/api/v1/trust/**` | `moderation-trust-service` | TrustController<br>TrustSafetyAdminController | `GET /api/v1/trust/{subjectId}`<br>`GET /api/v1/trust/{subjectId}/history`<br>`POST /api/v1/trust/{subjectId}/sanctions` | JWT / Variable | **OK — VALIDE** |
| `moderation-trust-service` (ord: 0) | `/api/v1/moderation/**` | `moderation-trust-service` | AuditController<br>CulturalModerationController<br>ModerationController | `GET /api/v1/moderation/audit`<br>`POST /api/v1/moderation/culture/authenticity/claims`<br>`GET /api/v1/moderation/culture/authenticity/claims/{claimId}`<br>*(+24 routes)* | JWT / Variable | **OK — VALIDE** |
| `media-service` (ord: 0) | `/api/v1/media/**` | `media-service` | MediaController | `POST /api/v1/media/culture`<br>`GET /api/v1/media/{id}`<br>`GET /api/v1/media/{id}/content`<br>*(+3 routes)* | JWT / Variable | **OK — VALIDE** |
| `booking-service` (ord: 0) | `/api/v1/bookings/**` | `booking-service` | BookingController | `GET /api/v1/bookings/me`<br>`GET /api/v1/bookings/{id}`<br>`GET /api/v1/bookings/{id}/history`<br>*(+1 routes)* | JWT / Variable | **OK — VALIDE** |
| `booking-service` (ord: 0) | `/api/v1/activities/*/availability` | `booking-service` | BookingController | `GET /api/v1/activities/{activityId}/availability` | JWT / Variable | **OK — VALIDE** |
| `booking-service` (ord: 0) | `/api/v1/booking-management/**` | `booking-service` | AdminBookingController<br>BookingController | `GET /api/v1/booking-management/bookings`<br>`GET /api/v1/booking-management/bookings/stats`<br>`GET /api/v1/booking-management/bookings/{id}`<br>*(+4 routes)* | JWT / Variable | **OK — VALIDE** |
| `payment-service` (ord: 0) | `/api/v1/payments/**` | `payment-service` | AdminPaymentController<br>PaymentController<br>WebhookController | `GET /api/v1/payments/admin`<br>`GET /api/v1/payments/admin/{id}`<br>`GET /api/v1/payments/admin/anomalies`<br>*(+6 routes)* | JWT / Variable | **OK — VALIDE** |
| `messaging-service` (ord: 0) | `/api/v1/messaging/**` | `messaging-service` | MessagingController | `POST /api/v1/messaging/conversations`<br>`GET /api/v1/messaging/conversations`<br>`GET /api/v1/messaging/conversations/{id}`<br>*(+8 routes)* | JWT / Variable | **OK — VALIDE** |
| `ads-delivery-service` (ord: 0) | `/api/v1/ads/**` | `ads-delivery-service` | AdDeliveryController | `POST /api/v1/ads/select`<br>`POST /api/v1/ads/impressions`<br>`POST /api/v1/ads/clicks`<br>*(+1 routes)* | JWT / Variable | **OK — VALIDE** |
| `commerce-service` (ord: 0) | `/api/v1/commerce/**` | `commerce-service` | AdminCommerceController<br>CommerceController<br>PartnerFinanceController | `GET /api/v1/commerce/admin/promotions`<br>`GET /api/v1/commerce/admin/promotions/{id}`<br>`POST /api/v1/commerce/admin/promotions`<br>*(+20 routes)* | JWT / Variable | **OK — VALIDE** |

---

## 3. Anomalies Critiques d'Acheminement (Conflits d'Ordre & Routage vers Mauvais Service)

Le Spring Cloud Gateway évalue les routes selon leur `order` (les valeurs négatives étant prioritaires). Les conflits suivants ont été démontrés :

### 🔴 `GET /api/v1/admin/campaigns`
- **Implémenté dans le microservice** : `campaign-service` (`AdminCampaignController.listAllCampaigns`)
- **Règle Gateway interceptant la requête** : Route ID `admin-service` avec prédicat `/api/v1/admin/**`
- **Service cible configuré par le Gateway** : `admin-service`
- **Impact** : **404 NOT FOUND ou 500**. La requête HTTP envoyée au Gateway sera transmise au service `admin-service` qui ne possède pas ce contrôleur !

### 🔴 `POST /api/v1/admin/artwork-materials`
- **Implémenté dans le microservice** : `catalog-service` (`ArtworkReferenceController.material`)
- **Règle Gateway interceptant la requête** : Route ID `admin-service` avec prédicat `/api/v1/admin/**`
- **Service cible configuré par le Gateway** : `admin-service`
- **Impact** : **404 NOT FOUND ou 500**. La requête HTTP envoyée au Gateway sera transmise au service `admin-service` qui ne possède pas ce contrôleur !

### 🔴 `POST /api/v1/admin/artwork-techniques`
- **Implémenté dans le microservice** : `catalog-service` (`ArtworkReferenceController.technique`)
- **Règle Gateway interceptant la requête** : Route ID `admin-service` avec prédicat `/api/v1/admin/**`
- **Service cible configuré par le Gateway** : `admin-service`
- **Impact** : **404 NOT FOUND ou 500**. La requête HTTP envoyée au Gateway sera transmise au service `admin-service` qui ne possède pas ce contrôleur !

### 🔴 `GET /api/v1/admin/countries`
- **Implémenté dans le microservice** : `country-config-service` (`CountryAdminController.getAllCountries`)
- **Règle Gateway interceptant la requête** : Route ID `admin-service` avec prédicat `/api/v1/admin/**`
- **Service cible configuré par le Gateway** : `admin-service`
- **Impact** : **404 NOT FOUND ou 500**. La requête HTTP envoyée au Gateway sera transmise au service `admin-service` qui ne possède pas ce contrôleur !

### 🔴 `GET /api/v1/countries`
- **Implémenté dans le microservice** : `country-config-service` (`CountryController.getAllCountries`)
- **Règle Gateway interceptant la requête** : Route ID `place-service` avec prédicat `/api/v1/countries`
- **Service cible configuré par le Gateway** : `place-service`
- **Impact** : **404 NOT FOUND ou 500**. La requête HTTP envoyée au Gateway sera transmise au service `place-service` qui ne possède pas ce contrôleur !

### 🔴 `GET /api/v1/cities/{id}`
- **Implémenté dans le microservice** : `country-config-service` (`GeographyController.getCity`)
- **Règle Gateway interceptant la requête** : Route ID `place-service` avec prédicat `/api/v1/cities/**`
- **Service cible configuré par le Gateway** : `place-service`
- **Impact** : **404 NOT FOUND ou 500**. La requête HTTP envoyée au Gateway sera transmise au service `place-service` qui ne possède pas ce contrôleur !

### 🔴 `GET /api/v1/admin/notifications`
- **Implémenté dans le microservice** : `notification-service` (`AdminNotificationController.unread`)
- **Règle Gateway interceptant la requête** : Route ID `admin-service` avec prédicat `/api/v1/admin/**`
- **Service cible configuré par le Gateway** : `admin-service`
- **Impact** : **404 NOT FOUND ou 500**. La requête HTTP envoyée au Gateway sera transmise au service `admin-service` qui ne possède pas ce contrôleur !

### 🔴 `GET /api/v1/admin/newsletters`
- **Implémenté dans le microservice** : `notification-service` (`NewsletterAdminController.list`)
- **Règle Gateway interceptant la requête** : Route ID `admin-service` avec prédicat `/api/v1/admin/**`
- **Service cible configuré par le Gateway** : `admin-service`
- **Impact** : **404 NOT FOUND ou 500**. La requête HTTP envoyée au Gateway sera transmise au service `admin-service` qui ne possède pas ce contrôleur !

### 🔴 `POST /api/v1/admin/newsletters`
- **Implémenté dans le microservice** : `notification-service` (`NewsletterAdminController.create`)
- **Règle Gateway interceptant la requête** : Route ID `admin-service` avec prédicat `/api/v1/admin/**`
- **Service cible configuré par le Gateway** : `admin-service`
- **Impact** : **404 NOT FOUND ou 500**. La requête HTTP envoyée au Gateway sera transmise au service `admin-service` qui ne possède pas ce contrôleur !

### 🔴 `GET /api/v1/places/{placeId}/events`
- **Implémenté dans le microservice** : `event-service` (`PlaceEventController.findByPlace`)
- **Règle Gateway interceptant la requête** : Route ID `place-service` avec prédicat `/api/v1/places/**`
- **Service cible configuré par le Gateway** : `place-service`
- **Impact** : **404 NOT FOUND ou 500**. La requête HTTP envoyée au Gateway sera transmise au service `place-service` qui ne possède pas ce contrôleur !

### 🔴 `GET /api/v1/countries/{countryCode}`
- **Implémenté dans le microservice** : `place-service` (`CountryController.get`)
- **Règle Gateway interceptant la requête** : Route ID `country-config-service` avec prédicat `/api/v1/countries/**`
- **Service cible configuré par le Gateway** : `country-config-service`
- **Impact** : **404 NOT FOUND ou 500**. La requête HTTP envoyée au Gateway sera transmise au service `country-config-service` qui ne possède pas ce contrôleur !

### 🔴 `GET /api/v1/countries/{countryCode}/administrative-areas`
- **Implémenté dans le microservice** : `place-service` (`CountryController.administrativeAreas`)
- **Règle Gateway interceptant la requête** : Route ID `country-config-service` avec prédicat `/api/v1/countries/**`
- **Service cible configuré par le Gateway** : `country-config-service`
- **Impact** : **404 NOT FOUND ou 500**. La requête HTTP envoyée au Gateway sera transmise au service `country-config-service` qui ne possède pas ce contrôleur !

### 🔴 `GET /api/v1/countries/{countryCode}/languages`
- **Implémenté dans le microservice** : `place-service` (`CountryController.languages`)
- **Règle Gateway interceptant la requête** : Route ID `country-config-service` avec prédicat `/api/v1/countries/**`
- **Service cible configuré par le Gateway** : `country-config-service`
- **Impact** : **404 NOT FOUND ou 500**. La requête HTTP envoyée au Gateway sera transmise au service `country-config-service` qui ne possède pas ce contrôleur !

---

## 4. Routes Microservices NON Exposées via Gateway

> Ces routes sont implémentées dans les contrôleurs Spring Boot mais sont inaccessibles depuis le Gateway car aucun prédicat `Path=` ne correspond exactement (notamment dû au piège AntPathMatcher `/path/**` qui ne matche pas `/path` sans slash, ou routes d'administration oubliées).

| Service | Méthode | Path Contrôleur | Raison de Non-Exposition |
| :--- | :--- | :--- | :--- |
| `api-gateway` | `GET` | `/fallback/{service}` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `user-service` | `GET` | `/api/v1/users` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `partner-service` | `GET` | `/api/v1/artisans` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `partner-service` | `GET` | `/api/v1/artisan-specialties` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `partner-service` | `POST` | `/api/v1/partners` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `partner-service` | `GET` | `/api/v1/partners` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `campaign-service` | `POST` | `/api/v1/campaigns` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `campaign-service` | `GET` | `/api/v1/campaigns` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `catalog-service` | `GET` | `/api/v1/artworks` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `catalog-service` | `POST` | `/api/v1/artworks` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `catalog-service` | `GET` | `/api/v1/artwork-materials` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `catalog-service` | `GET` | `/api/v1/artwork-techniques` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `catalog-service` | `GET` | `/api/v1/artwork-materials/{id}/translations` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `catalog-service` | `GET` | `/api/v1/artwork-techniques/{id}/translations` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `catalog-service` | `POST` | `/api/v1/catalog/assets` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `catalog-service` | `GET` | `/api/v1/catalog/{kind:regions|cities|categories}` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `catalog-service` | `GET` | `/api/v1/catalog/{kind:regions|cities|categories}/{id}` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `catalog-service` | `POST` | `/api/v1/catalog/{kind:regions|cities|categories}` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `catalog-service` | `PUT` | `/api/v1/catalog/{kind:regions|cities|categories}/{id}` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `catalog-service` | `DELETE` | `/api/v1/catalog/{kind:regions|cities|categories}/{id}` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `catalog-service` | `POST` | `/api/v1/catalog/{kind:regions|cities|categories}/{id}/activate` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `catalog-service` | `GET` | `/api/v1/collections` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `catalog-service` | `POST` | `/api/v1/collections` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `ingestion-service` | `POST` | `/api/v1/catalog/imports` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `ingestion-service` | `GET` | `/api/v1/catalog/imports` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `media-service` | `POST` | `/api/v1/media` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `content-service` | `POST` | `/api/v1/posts` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `content-service` | `GET` | `/api/v1/stories` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `content-service` | `POST` | `/api/v1/stories` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |
| `interaction-service` | `POST` | `/api/v1/checkins` | Piège AntMatcher : prédicat `/**` ne matchant pas la racine |