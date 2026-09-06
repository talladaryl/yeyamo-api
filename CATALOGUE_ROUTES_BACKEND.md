# CATALOGUE EXHAUSTIF DES ROUTES BACKEND — YEYAMO-API

> Ce catalogue recense l'intégralité des **627 routes REST** découvertes par analyse statique dans les microservices de `yeyamo-api`.

---


# SERVICE : `admin-service`


## Contrôleur : `AdminUserController` (`admin-service\src\main\java\com\yeyamo_mobile\api\admin_service\controller\AdminUserController.java`)

### `GET /api/v1/admin/users`
- **Méthode Java** : `list`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<AdminUserResponse>`

### `POST /api/v1/admin/users`
- **Méthode Java** : `create`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `CreateAdminUserRequest` (Validé: True)
- **Response DTO** : `AdminUserResponse`

### `GET /api/v1/admin/users/{id}`
- **Méthode Java** : `get`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `AdminUserResponse`

### `PUT /api/v1/admin/users/{id}`
- **Méthode Java** : `update`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `UpdateAdminUserRequest` (Validé: True)
- **Response DTO** : `AdminUserResponse`


## Contrôleur : `AuditController` (`admin-service\src\main\java\com\yeyamo_mobile\api\admin_service\controller\AuditController.java`)

### `GET /api/v1/admin/audit-logs`
- **Méthode Java** : `list`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<AdminAuditLog>`


## Contrôleur : `GovernanceController` (`admin-service\src\main\java\com\yeyamo_mobile\api\admin_service\governance\GovernanceController.java`)

### `GET /api/v1/admin/feature-flags`
- **Méthode Java** : `flag`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('SUPER_ADMIN')`
- **Paramètres** : PathVars: key
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `FeatureFlagResponse`

### `POST /api/v1/admin/feature-flags`
- **Méthode Java** : `create`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `FeatureFlagRequest` (Validé: True)
- **Response DTO** : `FeatureFlagResponse`

### `GET /api/v1/admin/feature-flags/{key}`
- **Méthode Java** : `flag`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('SUPER_ADMIN')`
- **Paramètres** : PathVars: key
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `FeatureFlagResponse`

### `PUT /api/v1/admin/feature-flags/{key}`
- **Méthode Java** : `update`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('SUPER_ADMIN')`
- **Paramètres** : PathVars: key
- **Request DTO** : `FeatureFlagRequest` (Validé: True)
- **Response DTO** : `FeatureFlagResponse`

### `GET /api/v1/admin/feature-flags/{key}/history`
- **Méthode Java** : `rollbackFlag`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('SUPER_ADMIN')`
- **Paramètres** : PathVars: key, version | QueryParams: reason
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `FeatureFlagResponse`

### `POST /api/v1/admin/feature-flags/{key}/rollback/{version}`
- **Méthode Java** : `rollbackFlag`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('SUPER_ADMIN')`
- **Paramètres** : PathVars: key, version | QueryParams: reason
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `FeatureFlagResponse`

### `GET /api/v1/admin/settings`
- **Méthode Java** : `setting`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('SUPER_ADMIN')`
- **Paramètres** : PathVars: key
- **Request DTO** : `SettingUpdateRequest` (Validé: True)
- **Response DTO** : `SettingResponse`

### `GET /api/v1/admin/settings/history`
- **Méthode Java** : `rollback`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('SUPER_ADMIN')`
- **Paramètres** : PathVars: key, version | QueryParams: reason
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `SettingResponse`

### `PUT /api/v1/admin/settings/{key}`
- **Méthode Java** : `setting`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('SUPER_ADMIN')`
- **Paramètres** : PathVars: key
- **Request DTO** : `SettingUpdateRequest` (Validé: True)
- **Response DTO** : `SettingResponse`

### `POST /api/v1/admin/settings/{key}/rollback/{version}`
- **Méthode Java** : `rollback`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('SUPER_ADMIN')`
- **Paramètres** : PathVars: key, version | QueryParams: reason
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `SettingResponse`


## Contrôleur : `ModerationController` (`admin-service\src\main\java\com\yeyamo_mobile\api\admin_service\controller\ModerationController.java`)

### `GET /api/v1/admin/moderation-actions`
- **Méthode Java** : `history`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : QueryParams: targetType, targetId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<ModerationAction>`

### `POST /api/v1/admin/moderation-actions`
- **Méthode Java** : `apply`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `ModerationActionRequest` (Validé: True)
- **Response DTO** : `ModerationAction`


## Contrôleur : `ReportController` (`admin-service\src\main\java\com\yeyamo_mobile\api\admin_service\controller\ReportController.java`)

### `GET /api/v1/admin/reports`
- **Méthode Java** : `list`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<Report>`

### `POST /api/v1/admin/reports`
- **Méthode Java** : `create`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `ReportRequest` (Validé: True)
- **Response DTO** : `Report`

### `PATCH /api/v1/admin/reports/{id}/resolution`
- **Méthode Java** : `resolve`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `ReportResolutionRequest` (Validé: True)
- **Response DTO** : `Report`


## Contrôleur : `ValidationController` (`admin-service\src\main\java\com\yeyamo_mobile\api\admin_service\controller\ValidationController.java`)

### `GET /api/v1/admin/validations/partners`
- **Méthode Java** : `listPartners`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<PartnerValidation>`

### `POST /api/v1/admin/validations/partners`
- **Méthode Java** : `createPartner`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `PartnerValidationRequest` (Validé: True)
- **Response DTO** : `PartnerValidation`

### `PATCH /api/v1/admin/validations/partners/{id}/review`
- **Méthode Java** : `reviewPartner`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `PartnerReviewRequest` (Validé: True)
- **Response DTO** : `PartnerValidation`

### `GET /api/v1/admin/validations/places`
- **Méthode Java** : `listPlaces`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<PlaceValidation>`

### `POST /api/v1/admin/validations/places`
- **Méthode Java** : `createPlace`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `PlaceValidationRequest` (Validé: True)
- **Response DTO** : `PlaceValidation`

### `PATCH /api/v1/admin/validations/places/{id}/review`
- **Méthode Java** : `reviewPlace`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `PlaceReviewRequest` (Validé: True)
- **Response DTO** : `PlaceValidation`


# SERVICE : `ads-delivery-service`


## Contrôleur : `AdDeliveryController` (`ads-delivery-service\src\main\java\com\yeyamo_mobile\api\ads_delivery_service\interfaces\rest\AdDeliveryController.java`)

### `POST /api/v1/ads/clicks`
- **Méthode Java** : `recordClick`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `ClickRequest` (Validé: True)
- **Response DTO** : `ResponseEntity<Void>`

### `POST /api/v1/ads/conversions`
- **Méthode Java** : `recordConversion`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `ConversionRequest` (Validé: True)
- **Response DTO** : `ResponseEntity<Void>`

### `POST /api/v1/ads/impressions`
- **Méthode Java** : `recordImpression`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `ImpressionRequest` (Validé: True)
- **Response DTO** : `ResponseEntity<Void>`

### `POST /api/v1/ads/select`
- **Méthode Java** : `selectAds`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `AdSelectionRequest` (Validé: True)
- **Response DTO** : `ResponseEntity<List<SponsoredPlacementResponse>>`


# SERVICE : `analytics-service`


## Contrôleur : `AnalyticsController` (`analytics-service\src\main\java\com\yeyamo_mobile\api\analytics_service\controller\AnalyticsController.java`)

### `GET /api/v1/analytics/admin/dashboard`
- **Méthode Java** : `adminDashboard`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `AnalyticsDashboardResponse`

### `GET /api/v1/analytics/event-logs`
- **Méthode Java** : `eventLogs`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<AnalyticsEventLog>`

### `GET /api/v1/analytics/kpis`
- **Méthode Java** : `latestKpis`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<KpiHistory>`

### `GET /api/v1/analytics/kpis/{kpiName}`
- **Méthode Java** : `kpiHistory`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: kpiName
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `KpiPoint>`

### `GET /api/v1/analytics/partners/{partnerId}/dashboard`
- **Méthode Java** : `partnerDashboard`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<PartnerAnalytics>`

### `GET /api/v1/analytics/places/popular`
- **Méthode Java** : `popularPlaces`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<PlacePopularitySummary>`

### `GET /api/v1/analytics/places/{placeId}/popularity`
- **Méthode Java** : `placePopularity`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: placeId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<PlacePopularity>`

### `GET /api/v1/analytics/regions/{regionId}/activity`
- **Méthode Java** : `regionActivity`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: regionId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<RegionActivity>`

### `GET /api/v1/analytics/users/{userId}/engagement`
- **Méthode Java** : `userEngagement`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: userId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<UserEngagement>`


## Contrôleur : `BusinessAnalyticsController` (`analytics-service\src\main\java\com\yeyamo_mobile\api\analytics_service\controller\BusinessAnalyticsController.java`)

### `GET /api/v1/analytics/admin/{scopeType}/{scopeId}`
- **Méthode Java** : `admin`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Metrics>`

### `POST /api/v1/analytics/business/admin/rebuild`
- **Méthode Java** : `rebuild`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `AnalyticsRebuildJob`

### `GET /api/v1/analytics/business/admin/rebuild/{jobId}`
- **Méthode Java** : `rebuildStatus`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: jobId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `AnalyticsRebuildJob`

### `GET /api/v1/analytics/partners/{partnerId}/campaigns/{campaignId}`
- **Méthode Java** : `campaign`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId, campaignId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Metrics>`

### `GET /api/v1/analytics/partners/{partnerId}/ticket-events/{eventId}`
- **Méthode Java** : `ticketing`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId, eventId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Metrics>`

### `GET /api/v1/analytics/partners/{partnerId}/ticket-events/{eventId}/peak-entry`
- **Méthode Java** : `peakEntry`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId, eventId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Metrics>`


## Contrôleur : `CultureAnalyticsController` (`analytics-service\src\main\java\com\yeyamo_mobile\api\analytics_service\presentation\controller\CultureAnalyticsController.java`)

### `GET /api/v1/analytics/culture/artisans/{artisanId}`
- **Méthode Java** : `getArtisanAnalytics`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: artisanId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<Map<String, Object>>`

### `GET /api/v1/analytics/culture/artworks/{artworkId}`
- **Méthode Java** : `getArtworkAnalytics`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: artworkId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<Map<String, Object>>`

### `GET /api/v1/analytics/culture/contributions`
- **Méthode Java** : `getContributionAnalytics`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<Map<String, Object>>`

### `GET /api/v1/analytics/culture/countries/{countryCode}`
- **Méthode Java** : `getCountryAnalytics`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: countryCode
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<Map<String, Object>>`

### `GET /api/v1/analytics/culture/languages/{languageCode}`
- **Méthode Java** : `getLanguageAnalytics`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: languageCode
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<Map<String, Object>>`

### `GET /api/v1/analytics/culture/overview`
- **Méthode Java** : `getCultureOverview`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<CultureOverviewResponse>`

### `GET /api/v1/analytics/culture/trending`
- **Méthode Java** : `getTrendingContent`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<Map<String, Object>>`


## Contrôleur : `TerritorialAnalyticsController` (`analytics-service\src\main\java\com\yeyamo_mobile\api\analytics_service\controller\TerritorialAnalyticsController.java`)

### `GET /api/v1/analytics/countries`
- **Méthode Java** : `countries`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Activity>`

### `GET /api/v1/analytics/countries/{countryCode}`
- **Méthode Java** : `country`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: countryCode
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Activity>`

### `GET /api/v1/analytics/countries/{countryCode}/artisans`
- **Méthode Java** : `artisans`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: countryCode
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ArtisanActivity>`

### `GET /api/v1/analytics/countries/{countryCode}/cities`
- **Méthode Java** : `cities`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: countryCode
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CityActivity>`

### `GET /api/v1/analytics/countries/{countryCode}/culture`
- **Méthode Java** : `culture`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: countryCode
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CultureActivity>`

### `GET /api/v1/analytics/countries/{countryCode}/revenue`
- **Méthode Java** : `revenue`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: countryCode
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Revenue>`


# SERVICE : `api-gateway`


## Contrôleur : `FallbackController` (`api-gateway\src\main\java\com\yeyamo_mobile\api\api_gateway\controller\FallbackController.java`)

### `GET /fallback/{service}`
- **Méthode Java** : `unavailable`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: service
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<Map<String, Object>>`


# SERVICE : `auth-service`


## Contrôleur : `AdminPlatformUserController` (`auth-service\src\main\java\com\yeyamo_mobile\api\auth_service\controller\AdminPlatformUserController.java`)

### `GET /api/v1/admin/platform-users`
- **Méthode Java** : `list`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN','SUPPORT')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<AdminPlatformUserSummary>`

### `GET /api/v1/admin/platform-users/export`
- **Méthode Java** : `export`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<StreamingResponseBody>`

### `GET /api/v1/admin/platform-users/{id}`
- **Méthode Java** : `detail`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN','SUPPORT')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `AdminPlatformUserDetail`

### `PATCH /api/v1/admin/platform-users/{id}/roles`
- **Méthode Java** : `roles`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `AdminUserRolesRequest` (Validé: True)
- **Response DTO** : `AdminPlatformUserDetail`

### `GET /api/v1/admin/platform-users/{id}/sessions`
- **Méthode Java** : `sessions`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN','SUPPORT')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<AdminUserSessionResponse>`

### `POST /api/v1/admin/platform-users/{id}/sessions/revoke`
- **Méthode Java** : `revoke`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `AdminRevokeSessionsRequest` (Validé: True)
- **Response DTO** : `ResponseEntity<Void>`

### `PATCH /api/v1/admin/platform-users/{id}/status`
- **Méthode Java** : `status`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `AdminUserStatusRequest` (Validé: True)
- **Response DTO** : `AdminPlatformUserDetail`


## Contrôleur : `AuthController` (`auth-service\src\main\java\com\yeyamo_mobile\api\auth_service\controller\AuthController.java`)

### `POST /api/v1/auth/account/deactivate`
- **Méthode Java** : `deactivate`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `DeactivateAccountRequest` (Validé: True)
- **Response DTO** : `void`

### `POST /api/v1/auth/email/verification/confirm`
- **Méthode Java** : `confirmEmailVerification`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `OtpVerificationRequest` (Validé: True)
- **Response DTO** : `MessageResponse`

### `POST /api/v1/auth/email/verification/request`
- **Méthode Java** : `requestEmailVerification`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `EmailRequest` (Validé: True)
- **Response DTO** : `MessageResponse`

### `POST /api/v1/auth/login`
- **Méthode Java** : `login`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : Aucun
- **Request DTO** : `LoginRequest` (Validé: True)
- **Response DTO** : `AuthResponse`

### `POST /api/v1/auth/logout`
- **Méthode Java** : `logout`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `GET /api/v1/auth/me`
- **Méthode Java** : `me`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `UserResponse`

### `POST /api/v1/auth/oauth/apple`
- **Méthode Java** : `apple`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `OAuthLoginRequest` (Validé: True)
- **Response DTO** : `AuthResponse`

### `POST /api/v1/auth/oauth/google`
- **Méthode Java** : `google`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `OAuthLoginRequest` (Validé: True)
- **Response DTO** : `AuthResponse`

### `PUT /api/v1/auth/password`
- **Méthode Java** : `changePassword`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `ChangePasswordRequest` (Validé: True)
- **Response DTO** : `void`

### `POST /api/v1/auth/password/forgot`
- **Méthode Java** : `requestPasswordReset`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `EmailRequest` (Validé: True)
- **Response DTO** : `MessageResponse`

### `POST /api/v1/auth/password/reset`
- **Méthode Java** : `resetPassword`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `PasswordResetRequest` (Validé: True)
- **Response DTO** : `MessageResponse`

### `POST /api/v1/auth/refresh`
- **Méthode Java** : `refresh`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : Aucun
- **Request DTO** : `RefreshTokenRequest` (Validé: True)
- **Response DTO** : `AuthResponse`

### `POST /api/v1/auth/register`
- **Méthode Java** : `register`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : Aucun
- **Request DTO** : `RegisterRequest` (Validé: True)
- **Response DTO** : `AuthResponse`

### `GET /api/v1/auth/sessions`
- **Méthode Java** : `sessions`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<SessionResponse>`

### `DELETE /api/v1/auth/sessions/{sessionId}`
- **Méthode Java** : `revokeSession`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: sessionId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`


# SERVICE : `booking-service`


## Contrôleur : `AdminBookingController` (`booking-service\src\main\java\com\yeyamo_mobile\api\booking_service\infrastructure\web\AdminBookingController.java`)

### `GET /api/v1/booking-management/bookings`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN','SUPPORT')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `GET /api/v1/booking-management/bookings/stats`
- **Méthode Java** : `stats`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN','SUPPORT')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `AdminBookingStats`

### `GET /api/v1/booking-management/bookings/{id}`
- **Méthode Java** : `detail`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN','SUPPORT')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `AdminBookingDetail`

### `POST /api/v1/booking-management/bookings/{id}/cancel`
- **Méthode Java** : `cancel`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `CancelBooking` (Validé: True)
- **Response DTO** : `BookingView`

### `POST /api/v1/booking-management/bookings/{id}/complete`
- **Méthode Java** : `complete`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `BookingView`


## Contrôleur : `BookingController` (`booking-service\src\main\java\com\yeyamo_mobile\api\booking_service\infrastructure\web\BookingController.java`)

### `GET /api/v1/activities/{activityId}/availability`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `CreateBooking` (Validé: True)
- **Response DTO** : `BookingView`

### `POST /api/v1/booking-management/slots`
- **Méthode Java** : `slot`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `CreateSlot` (Validé: True)
- **Response DTO** : `SlotView`

### `POST /api/v1/booking-management/slots/{id}/close`
- **Méthode Java** : `close`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `POST /api/v1/bookings`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `CreateBooking` (Validé: True)
- **Response DTO** : `BookingView`

### `GET /api/v1/bookings/me`
- **Méthode Java** : `get`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `BookingView`

### `GET /api/v1/bookings/{id}`
- **Méthode Java** : `get`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `BookingView`

### `POST /api/v1/bookings/{id}/cancel`
- **Méthode Java** : `cancel`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `CancelBooking` (Validé: True)
- **Response DTO** : `BookingView`

### `GET /api/v1/bookings/{id}/history`
- **Méthode Java** : `cancel`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `CancelBooking` (Validé: True)
- **Response DTO** : `BookingView`


# SERVICE : `campaign-service`


## Contrôleur : `AdminCampaignController` (`campaign-service\src\main\java\com\yeyamo_mobile\api\campaign_service\interfaces\rest\AdminCampaignController.java`)

### `GET /api/v1/admin/campaigns`
- **Méthode Java** : `listAllCampaigns`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<Page<CampaignResponse>>`

### `GET /api/v1/admin/campaigns/{id}`
- **Méthode Java** : `hasAuthority`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `or`

### `POST /api/v1/admin/campaigns/{id}/approve`
- **Méthode Java** : `approveCampaign`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<CampaignResponse>`

### `POST /api/v1/admin/campaigns/{id}/reject`
- **Méthode Java** : `rejectCampaign`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `RejectCampaignRequest` (Validé: True)
- **Response DTO** : `ResponseEntity<CampaignResponse>`


## Contrôleur : `CampaignController` (`campaign-service\src\main\java\com\yeyamo_mobile\api\campaign_service\interfaces\rest\CampaignController.java`)

### `GET /api/v1/campaigns`
- **Méthode Java** : `listCampaigns`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<Page<CampaignResponse>>`

### `POST /api/v1/campaigns`
- **Méthode Java** : `createCampaign`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `CreateCampaignRequest` (Validé: True)
- **Response DTO** : `ResponseEntity<CampaignResponse>`

### `GET /api/v1/campaigns/{id}`
- **Méthode Java** : `getCampaign`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<CampaignResponse>`

### `PUT /api/v1/campaigns/{id}`
- **Méthode Java** : `updateCampaign`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `UpdateCampaignRequest` (Validé: True)
- **Response DTO** : `ResponseEntity<CampaignResponse>`

### `POST /api/v1/campaigns/{id}/activate`
- **Méthode Java** : `activateCampaign`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<CampaignResponse>`

### `POST /api/v1/campaigns/{id}/cancel`
- **Méthode Java** : `cancelCampaign`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<CampaignResponse>`

### `POST /api/v1/campaigns/{id}/complete`
- **Méthode Java** : `completeCampaign`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<CampaignResponse>`

### `POST /api/v1/campaigns/{id}/pause`
- **Méthode Java** : `pauseCampaign`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<CampaignResponse>`

### `POST /api/v1/campaigns/{id}/resume`
- **Méthode Java** : `resumeCampaign`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<CampaignResponse>`

### `POST /api/v1/campaigns/{id}/submit`
- **Méthode Java** : `submitCampaign`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<CampaignResponse>`


# SERVICE : `catalog-service`


## Contrôleur : `ArtworkController` (`catalog-service\src\main\java\com\yeyamo_mobile\api\catalog_service\interfaces\rest\ArtworkController.java`)

### `GET /api/v1/artisans/{artisanId}/artworks`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `ArtworkDtos` (Validé: True)
- **Response DTO** : `Response`

### `GET /api/v1/artworks`
- **Méthode Java** : `detail`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Response`

### `POST /api/v1/artworks`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `ArtworkDtos` (Validé: True)
- **Response DTO** : `Response`

### `DELETE /api/v1/artworks/{id}`
- **Méthode Java** : `delete`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `GET /api/v1/artworks/{id}`
- **Méthode Java** : `detail`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Response`

### `PUT /api/v1/artworks/{id}`
- **Méthode Java** : `delete`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `PATCH /api/v1/artworks/{id}/availability`
- **Méthode Java** : `availability`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `ArtworkDtos` (Validé: True)
- **Response DTO** : `ArtworkEntity`

### `GET /api/v1/artworks/{id}/history`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `POST /api/v1/artworks/{id}/history`
- **Méthode Java** : `addHistory`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `ArtworkDtos` (Validé: True)
- **Response DTO** : `History`

### `PUT /api/v1/artworks/{id}/history/{entryId}`
- **Méthode Java** : `updateHistory`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id, entryId
- **Request DTO** : `ArtworkDtos` (Validé: True)
- **Response DTO** : `History`

### `GET /api/v1/artworks/{id}/media`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `GET /api/v1/artworks/{id}/related`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`


## Contrôleur : `ArtworkReferenceController` (`catalog-service\src\main\java\com\yeyamo_mobile\api\catalog_service\interfaces\rest\ArtworkReferenceController.java`)

### `POST /api/v1/admin/artwork-materials`
- **Méthode Java** : `material`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN','EDITOR')`
- **Paramètres** : Aucun
- **Request DTO** : `ReferenceRequest` (Validé: False)
- **Response DTO** : `Material`

### `POST /api/v1/admin/artwork-techniques`
- **Méthode Java** : `technique`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN','EDITOR')`
- **Paramètres** : Aucun
- **Request DTO** : `ReferenceRequest` (Validé: False)
- **Response DTO** : `Technique`

### `GET /api/v1/artwork-materials`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `GET /api/v1/artwork-materials/{id}/translations`
- **Méthode Java** : `material`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN','EDITOR')`
- **Paramètres** : Aucun
- **Request DTO** : `ReferenceRequest` (Validé: False)
- **Response DTO** : `Material`

### `GET /api/v1/artwork-techniques`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN','EDITOR')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `GET /api/v1/artwork-techniques/{id}/translations`
- **Méthode Java** : `material`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN','EDITOR')`
- **Paramètres** : Aucun
- **Request DTO** : `ReferenceRequest` (Validé: False)
- **Response DTO** : `Material`


## Contrôleur : `CatalogAssetController` (`catalog-service\src\main\java\com\yeyamo_mobile\api\catalog_service\interfaces\rest\CatalogAssetController.java`)

### `POST /api/v1/catalog/assets`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `CatalogAssetRequest` (Validé: True)
- **Response DTO** : `CatalogAssetResponse`

### `GET /api/v1/catalog/assets/!page`
- **Méthode Java** : `search`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<CatalogAssetResponse>`

### `GET /api/v1/catalog/assets/manage`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `GET /api/v1/catalog/assets/manage/{id}`
- **Méthode Java** : `manage`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CatalogAssetResponse`

### `GET /api/v1/catalog/assets/nearby`
- **Méthode Java** : `nearby`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<CatalogAssetResponse>`

### `GET /api/v1/catalog/assets/page`
- **Méthode Java** : `adminSearch`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : QueryParams: page
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<AdminCatalogAssetResponse>`

### `GET /api/v1/catalog/assets/slug/{slug}`
- **Méthode Java** : `bySlug`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: slug
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CatalogAssetResponse`

### `DELETE /api/v1/catalog/assets/{id}`
- **Méthode Java** : `delete`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `GET /api/v1/catalog/assets/{id}`
- **Méthode Java** : `get`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CatalogAssetResponse`

### `PUT /api/v1/catalog/assets/{id}`
- **Méthode Java** : `update`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `CatalogAssetRequest` (Validé: True)
- **Response DTO** : `CatalogAssetResponse`

### `PATCH /api/v1/catalog/assets/{id}/status`
- **Méthode Java** : `status`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `StatusChangeRequest` (Validé: True)
- **Response DTO** : `CatalogAssetResponse`


## Contrôleur : `CatalogReferenceController` (`catalog-service\src\main\java\com\yeyamo_mobile\api\catalog_service\interfaces\rest\CatalogReferenceController.java`)

### `GET /api/v1/catalog/{kind:regions|cities|categories}`
- **Méthode Java** : `list`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: kind
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<CatalogReferenceResponse>`

### `POST /api/v1/catalog/{kind:regions|cities|categories}`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: kind
- **Request DTO** : `CatalogReferenceRequest` (Validé: True)
- **Response DTO** : `CatalogReferenceResponse`

### `DELETE /api/v1/catalog/{kind:regions|cities|categories}/{id}`
- **Méthode Java** : `deactivate`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: kind, id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `GET /api/v1/catalog/{kind:regions|cities|categories}/{id}`
- **Méthode Java** : `get`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: kind, id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CatalogReferenceResponse`

### `PUT /api/v1/catalog/{kind:regions|cities|categories}/{id}`
- **Méthode Java** : `update`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: kind, id
- **Request DTO** : `CatalogReferenceRequest` (Validé: True)
- **Response DTO** : `CatalogReferenceResponse`

### `POST /api/v1/catalog/{kind:regions|cities|categories}/{id}/activate`
- **Méthode Java** : `activate`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: kind, id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CatalogReferenceResponse`


## Contrôleur : `CollectionController` (`catalog-service\src\main\java\com\yeyamo_mobile\api\catalog_service\interfaces\rest\CollectionController.java`)

### `GET /api/v1/collections`
- **Méthode Java** : `getMyCollections`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<CollectionResponse>`

### `POST /api/v1/collections`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `CollectionRequest` (Validé: True)
- **Response DTO** : `CollectionResponse`

### `POST /api/v1/collections/places`
- **Méthode Java** : `addPlace`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `AddPlaceRequest` (Validé: True)
- **Response DTO** : `void`

### `GET /api/v1/collections/public`
- **Méthode Java** : `getPublicCollections`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<CollectionResponse>`

### `GET /api/v1/collections/summaries`
- **Méthode Java** : `getSummaries`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<CollectionSummaryResponse>`

### `DELETE /api/v1/collections/{collectionId}/places/{assetId}`
- **Méthode Java** : `removePlace`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: collectionId, assetId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `PATCH /api/v1/collections/{collectionId}/places/{assetId}`
- **Méthode Java** : `updatePlace`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: collectionId, assetId
- **Request DTO** : `UpdateCollectionPlaceRequest` (Validé: True)
- **Response DTO** : `void`

### `DELETE /api/v1/collections/{id}`
- **Méthode Java** : `delete`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `GET /api/v1/collections/{id}`
- **Méthode Java** : `getCollection`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CollectionResponse`

### `PUT /api/v1/collections/{id}`
- **Méthode Java** : `update`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `CollectionRequest` (Validé: True)
- **Response DTO** : `CollectionResponse`


# SERVICE : `commerce-service`


## Contrôleur : `AdminCommerceController` (`commerce-service\src\main\java\com\yeyamo_mobile\api\commerce_service\interfaces\AdminCommerceController.java`)

### `GET /api/v1/commerce/admin/commissions`
- **Méthode Java** : `commission`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CommissionRule`

### `POST /api/v1/commerce/admin/commissions`
- **Méthode Java** : `commission`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `CommissionCommand` (Validé: False)
- **Response DTO** : `CommissionRule`

### `GET /api/v1/commerce/admin/commissions/{id}`
- **Méthode Java** : `commission`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CommissionRule`

### `GET /api/v1/commerce/admin/promotions`
- **Méthode Java** : `promotion`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Promotion`

### `POST /api/v1/commerce/admin/promotions`
- **Méthode Java** : `create`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `PromotionCommand` (Validé: False)
- **Response DTO** : `Promotion`

### `GET /api/v1/commerce/admin/promotions/{id}`
- **Méthode Java** : `promotion`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Promotion`

### `PUT /api/v1/commerce/admin/promotions/{id}`
- **Méthode Java** : `update`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `PromotionCommand` (Validé: False)
- **Response DTO** : `Promotion`

### `POST /api/v1/commerce/admin/promotions/{id}/disable`
- **Méthode Java** : `disable`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Promotion`


## Contrôleur : `ArtworkCommerceController` (`commerce-service\src\main\java\com\yeyamo_mobile\api\commerce_service\interfaces\ArtworkCommerceController.java`)

### `GET /api/v1/artisan/orders`
- **Méthode Java** : `artisan`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<ArtworkOrder>`

### `GET /api/v1/artisan/orders/{id}`
- **Méthode Java** : `artisanDetail`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ArtworkOrder`

### `PATCH /api/v1/artisan/orders/{id}/status`
- **Méthode Java** : `artisanStatus`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `StatusRequest` (Validé: True)
- **Response DTO** : `ArtworkOrder`

### `POST /api/v1/artwork-offers`
- **Méthode Java** : `createOffer`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `OfferRequest` (Validé: True)
- **Response DTO** : `ArtworkOffer`

### `GET /api/v1/artwork-offers/{artworkId}`
- **Méthode Java** : `offer`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: artworkId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ArtworkOffer`

### `PUT /api/v1/artwork-offers/{id}`
- **Méthode Java** : `updateOffer`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `OfferRequest` (Validé: True)
- **Response DTO** : `ArtworkOffer`

### `PATCH /api/v1/artwork-offers/{id}/status`
- **Méthode Java** : `offerStatus`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `OfferStatus` (Validé: True)
- **Response DTO** : `ArtworkOffer`

### `POST /api/v1/artwork-orders`
- **Méthode Java** : `order`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `OrderRequest` (Validé: True)
- **Response DTO** : `ArtworkOrder`

### `GET /api/v1/artwork-orders/me`
- **Méthode Java** : `mine`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<ArtworkOrder>`

### `GET /api/v1/artwork-orders/{id}`
- **Méthode Java** : `detail`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ArtworkOrder`

### `POST /api/v1/artwork-orders/{id}/cancel`
- **Méthode Java** : `cancel`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `CancelRequest` (Validé: True)
- **Response DTO** : `ArtworkOrder`


## Contrôleur : `CommerceController` (`commerce-service\src\main\java\com\yeyamo_mobile\api\commerce_service\interfaces\CommerceController.java`)

### `GET /api/v1/commerce/admin/ledger/{partnerId}`
- **Méthode Java** : `AdjustmentRequest`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `record`

### `POST /api/v1/commerce/admin/ledger/{partnerId}/adjustments`
- **Méthode Java** : `adjustment`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: partnerId
- **Request DTO** : `AdjustmentRequest` (Validé: True)
- **Response DTO** : `LedgerEntry`

### `GET /api/v1/commerce/admin/ledger/{partnerId}/balance/{currency}`
- **Méthode Java** : `AdjustmentRequest`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `record`

### `POST /api/v1/commerce/admin/ledger/{partnerId}/movements`
- **Méthode Java** : `movement`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: partnerId
- **Request DTO** : `MovementRequest` (Validé: True)
- **Response DTO** : `LedgerEntry`

### `POST /api/v1/commerce/orders`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `CreateRequest` (Validé: True)
- **Response DTO** : `CommerceOrder`

### `GET /api/v1/commerce/orders/me`
- **Méthode Java** : `RefundRequest`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `record`

### `POST /api/v1/commerce/orders/{orderId}/refunds`
- **Méthode Java** : `refund`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: orderId
- **Request DTO** : `RefundRequest` (Validé: True)
- **Response DTO** : `CommerceRefund`


## Contrôleur : `PartnerFinanceController` (`commerce-service\src\main\java\com\yeyamo_mobile\api\commerce_service\interfaces\PartnerFinanceController.java`)

### `GET /api/v1/commerce/partners/{partnerId}/finance/summary`
- **Méthode Java** : `summary`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Summary`

### `GET /api/v1/commerce/partners/{partnerId}/finance/transactions`
- **Méthode Java** : `transactions`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<LedgerEntry>`

### `GET /api/v1/commerce/partners/{partnerId}/finance/transactions/{id}`
- **Méthode Java** : `transaction`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId, id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `LedgerEntry`


## Contrôleur : `PartnerPromotionController` (`commerce-service\src\main\java\com\yeyamo_mobile\api\commerce_service\interfaces\PartnerPromotionController.java`)

### `GET /api/v1/commerce/partners/{partnerId}/promotions`
- **Méthode Java** : `one`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId, id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Promotion`

### `POST /api/v1/commerce/partners/{partnerId}/promotions`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId
- **Request DTO** : `Request` (Validé: True)
- **Response DTO** : `Promotion`

### `GET /api/v1/commerce/partners/{partnerId}/promotions/{id}`
- **Méthode Java** : `one`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId, id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Promotion`

### `PUT /api/v1/commerce/partners/{partnerId}/promotions/{id}`
- **Méthode Java** : `update`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId, id
- **Request DTO** : `Request` (Validé: True)
- **Response DTO** : `Promotion`

### `POST /api/v1/commerce/partners/{partnerId}/promotions/{id}/disable`
- **Méthode Java** : `disable`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId, id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Promotion`


# SERVICE : `content-service`


## Contrôleur : `PostController` (`content-service\src\main\java\com\yeyamo_mobile\api\content_service\interfaces\rest\PostController.java`)

### `POST /api/v1/posts`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `PostRequest` (Validé: True)
- **Response DTO** : `PostResponse`

### `GET /api/v1/posts/catalog/{assetId}`
- **Méthode Java** : `catalog`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: assetId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<PostResponse>`

### `GET /api/v1/posts/hashtags/{tag}`
- **Méthode Java** : `hashtag`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: tag
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<PostResponse>`

### `GET /api/v1/posts/me`
- **Méthode Java** : `mine`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<PostResponse>`

### `GET /api/v1/posts/me/{id}`
- **Méthode Java** : `mine`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `PostResponse`

### `DELETE /api/v1/posts/{id}`
- **Méthode Java** : `delete`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `GET /api/v1/posts/{id}`
- **Méthode Java** : `publicPost`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `PostResponse`

### `PUT /api/v1/posts/{id}`
- **Méthode Java** : `update`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `PostRequest` (Validé: True)
- **Response DTO** : `PostResponse`

### `POST /api/v1/posts/{id}/archive`
- **Méthode Java** : `archive`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `PostResponse`

### `POST /api/v1/posts/{id}/publish`
- **Méthode Java** : `publish`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `PostResponse`

### `PATCH /api/v1/posts/{id}/visibility`
- **Méthode Java** : `visibility`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `VisibilityRequest` (Validé: True)
- **Response DTO** : `PostResponse`


## Contrôleur : `StoryController` (`content-service\src\main\java\com\yeyamo_mobile\api\content_service\interfaces\rest\StoryController.java`)

### `GET /api/v1/stories`
- **Méthode Java** : `getActiveStories`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<StoryResponse>`

### `POST /api/v1/stories`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `StoryRequest` (Validé: True)
- **Response DTO** : `StoryResponse`

### `DELETE /api/v1/stories/{id}`
- **Méthode Java** : `delete`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `GET /api/v1/stories/{id}`
- **Méthode Java** : `getStory`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `StoryResponse`

### `POST /api/v1/stories/{id}/view`
- **Méthode Java** : `recordView`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`


# SERVICE : `country-config-service`


## Contrôleur : `CountryAdminController` (`country-config-service\src\main\java\com\yeyamo_mobile\api\country_config_service\controller\CountryAdminController.java`)

### `GET /api/v1/admin/countries`
- **Méthode Java** : `getAllCountries`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<List<CountryDto>>`

### `GET /api/v1/admin/countries/{code}`
- **Méthode Java** : `getCountryByCode`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('ADMIN')`
- **Paramètres** : PathVars: code
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<CountryDto>`

### `PUT /api/v1/admin/countries/{code}`
- **Méthode Java** : `updateCountry`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('ADMIN')`
- **Paramètres** : PathVars: code
- **Request DTO** : `UpdateCountryRequest` (Validé: True)
- **Response DTO** : `ResponseEntity<CountryDto>`

### `PATCH /api/v1/admin/countries/{code}/features`
- **Méthode Java** : `updateFeatures`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('ADMIN')`
- **Paramètres** : PathVars: code
- **Request DTO** : `UpdateFeaturesRequest` (Validé: True)
- **Response DTO** : `ResponseEntity<CountryDto>`

### `PATCH /api/v1/admin/countries/{code}/launch-status`
- **Méthode Java** : `updateLaunchStatus`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('SUPER_ADMIN')`
- **Paramètres** : PathVars: code
- **Request DTO** : `UpdateLaunchStatusRequest` (Validé: True)
- **Response DTO** : `ResponseEntity<CountryDto>`


## Contrôleur : `CountryController` (`country-config-service\src\main\java\com\yeyamo_mobile\api\country_config_service\controller\CountryController.java`)

### `GET /api/v1/countries`
- **Méthode Java** : `getAllCountries`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<List<CountryDto>>`

### `GET /api/v1/countries/available`
- **Méthode Java** : `getAvailableCountries`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<List<CountryDto>>`

### `GET /api/v1/countries/{code}`
- **Méthode Java** : `getCountryByCode`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: code
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<CountryDto>`

### `GET /api/v1/countries/{code}/configuration`
- **Méthode Java** : `getCountryConfiguration`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: code
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<CountryConfigurationDto>`

### `GET /api/v1/countries/{code}/currencies`
- **Méthode Java** : `getCountryCurrencies`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: code
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<List<CurrencyDto>>`

### `GET /api/v1/countries/{code}/features`
- **Méthode Java** : `getCountryFeatures`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: code
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<FeatureFlagsDto>`

### `GET /api/v1/countries/{code}/languages`
- **Méthode Java** : `getCountryLanguages`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: code
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<List<LanguageDto>>`

### `GET /api/v1/countries/{code}/timezones`
- **Méthode Java** : `getCountryTimezones`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: code
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<List<TimezoneDto>>`


## Contrôleur : `GeographyController` (`country-config-service\src\main\java\com\yeyamo_mobile\api\country_config_service\controller\GeographyController.java`)

### `GET /api/v1/administrative-areas/{id}`
- **Méthode Java** : `getAdministrativeArea`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<AdministrativeAreaDto>`

### `GET /api/v1/administrative-areas/{id}/children`
- **Méthode Java** : `getChildren`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<List<AdministrativeAreaDto>>`

### `GET /api/v1/cities/{id}`
- **Méthode Java** : `getCity`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<CityDto>`

### `GET /api/v1/cities/{id}/localities`
- **Méthode Java** : `getLocalitiesByCity`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<List<LocalityDto>>`

### `GET /api/v1/cities/{id}/localities/paged`
- **Méthode Java** : `getLocalitiesByCityPaged`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<Page<LocalityDto>>`

### `GET /api/v1/countries/{code}/administrative-areas`
- **Méthode Java** : `getAdministrativeAreas`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: code
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<List<AdministrativeAreaDto>>`

### `GET /api/v1/countries/{code}/administrative-areas/paged`
- **Méthode Java** : `getAdministrativeAreasPaged`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: code
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<Page<AdministrativeAreaDto>>`

### `GET /api/v1/countries/{code}/administrative-areas/top-level`
- **Méthode Java** : `getTopLevelAreas`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: code
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<List<AdministrativeAreaDto>>`

### `GET /api/v1/countries/{code}/administrative-labels`
- **Méthode Java** : `getLevelLabels`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: code
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<List<AdministrativeLevelLabelDto>>`

### `GET /api/v1/countries/{code}/cities`
- **Méthode Java** : `getCities`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: code
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<List<CityDto>>`

### `GET /api/v1/countries/{code}/cities/paged`
- **Méthode Java** : `getCitiesPaged`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: code
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<Page<CityDto>>`

### `GET /api/v1/countries/{code}/cities/{id}`
- **Méthode Java** : `getCityForCountry`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: code, id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<CityDto>`

### `GET /api/v1/localities/{id}`
- **Méthode Java** : `getLocality`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<LocalityDto>`


# SERVICE : `culture-service`


## Contrôleur : `AdminCultureController` (`culture-service\src\main\java\com\yeyamo_mobile\api\culture_service\interfaces\rest\AdminCultureController.java`)

### `GET /api/v1/admin/culture/contents`
- **Méthode Java** : `list`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<ContentResponse>`

### `POST /api/v1/admin/culture/contents`
- **Méthode Java** : `create`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `ContentRequest` (Validé: True)
- **Response DTO** : `ContentResponse`

### `GET /api/v1/admin/culture/contents/{id}`
- **Méthode Java** : `get`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ContentResponse`

### `PUT /api/v1/admin/culture/contents/{id}`
- **Méthode Java** : `update`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `ContentRequest` (Validé: True)
- **Response DTO** : `ContentResponse`

### `PATCH /api/v1/admin/culture/contents/{id}/status`
- **Méthode Java** : `status`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `StatusRequest` (Validé: True)
- **Response DTO** : `ContentResponse`

### `GET /api/v1/admin/culture/contributions`
- **Méthode Java** : `contributions`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<ContentResponse>`

### `POST /api/v1/admin/culture/contributions/{id}/review`
- **Méthode Java** : `review`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `ReviewRequest` (Validé: True)
- **Response DTO** : `ContentResponse`

### `PATCH /api/v1/admin/culture/translations/{id}/verify`
- **Méthode Java** : `verifyTranslation`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `TranslationResponse`


## Contrôleur : `CultureChallengeController` (`culture-service\src\main\java\com\yeyamo_mobile\api\culture_service\interfaces\rest\CultureChallengeController.java`)

### `DELETE /api/v1/culture/challenge-submissions/{id}`
- **Méthode Java** : `withdraw`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `GET /api/v1/culture/challenges`
- **Méthode Java** : `list`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<ChallengeResponse>`

### `GET /api/v1/culture/challenges/me`
- **Méthode Java** : `mine`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<ParticipationResponse>`

### `GET /api/v1/culture/challenges/{id}`
- **Méthode Java** : `get`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ChallengeResponse`

### `POST /api/v1/culture/challenges/{id}/join`
- **Méthode Java** : `join`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ParticipationResponse`

### `GET /api/v1/culture/challenges/{id}/submissions`
- **Méthode Java** : `submissions`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<SubmissionResponse>`

### `POST /api/v1/culture/challenges/{id}/submissions`
- **Méthode Java** : `submit`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `SubmissionRequest` (Validé: False)
- **Response DTO** : `SubmissionResponse`


## Contrôleur : `CultureContributionController` (`culture-service\src\main\java\com\yeyamo_mobile\api\culture_service\interfaces\rest\CultureContributionController.java`)

### `POST /api/v1/culture/contributions`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `ContentRequest` (Validé: True)
- **Response DTO** : `ContentResponse`

### `GET /api/v1/culture/contributions/me`
- **Méthode Java** : `mine`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<ContentResponse>`

### `DELETE /api/v1/culture/contributions/{id}`
- **Méthode Java** : `delete`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `GET /api/v1/culture/contributions/{id}`
- **Méthode Java** : `get`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ContentResponse`

### `PUT /api/v1/culture/contributions/{id}`
- **Méthode Java** : `update`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `ContentRequest` (Validé: True)
- **Response DTO** : `ContentResponse`

### `POST /api/v1/culture/contributions/{id}/submit`
- **Méthode Java** : `submit`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ContentResponse`


## Contrôleur : `PublicCultureController` (`culture-service\src\main\java\com\yeyamo_mobile\api\culture_service\interfaces\rest\PublicCultureController.java`)

### `GET /api/v1/culture/categories`
- **Méthode Java** : `categories`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<ContentType>`

### `GET /api/v1/culture/contents`
- **Méthode Java** : `contents`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<ContentResponse>`

### `GET /api/v1/culture/contents/{id}`
- **Méthode Java** : `content`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ContentResponse`

### `GET /api/v1/culture/contents/{id}/translations`
- **Méthode Java** : `translations`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<TranslationResponse>`

### `GET /api/v1/culture/daily`
- **Méthode Java** : `daily`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<ContentResponse>`

### `GET /api/v1/culture/daily-word`
- **Méthode Java** : `dailyWord`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : QueryParams: countryCode
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ContentResponse`

### `GET /api/v1/culture/language-lessons/{id}`
- **Méthode Java** : `lesson`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `LessonDetail`

### `POST /api/v1/culture/language-lessons/{id}/attempts`
- **Méthode Java** : `attempt`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `AttemptResponse`

### `POST /api/v1/culture/language-lessons/{id}/complete`
- **Méthode Java** : `complete`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ProgressResponse`

### `POST /api/v1/culture/language-lessons/{id}/start`
- **Méthode Java** : `start`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ProgressResponse`

### `GET /api/v1/culture/language-progress/me`
- **Méthode Java** : `progress`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<ProgressResponse>`

### `GET /api/v1/culture/language-progress/me/{languageCode}`
- **Méthode Java** : `progress`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: languageCode
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<ProgressResponse>`

### `GET /api/v1/culture/languages`
- **Méthode Java** : `languages`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<LanguageResponse>`

### `GET /api/v1/culture/languages/{code}`
- **Méthode Java** : `language`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: code
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `LanguageResponse`

### `GET /api/v1/culture/languages/{code}/content`
- **Méthode Java** : `languageContent`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: code
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<ContentResponse>`

### `GET /api/v1/culture/languages/{code}/lessons`
- **Méthode Java** : `lessons`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: code
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<LessonSummary>`

### `GET /api/v1/culture/trending`
- **Méthode Java** : `trending`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<ContentResponse>`


# SERVICE : `discovery-service`


## Contrôleur : `DiscoveryController` (`discovery-service\src\main\java\com\yeyamo_mobile\api\discovery_service\infrastructure\web\DiscoveryController.java`)

### `GET /api/v1/discovery/search`
- **Méthode Java** : `search`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `DiscoveryPage`

### `GET /api/v1/discovery/trending`
- **Méthode Java** : `trending`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `DiscoveryPage`


## Contrôleur : `MapsController` (`discovery-service\src\main\java\com\yeyamo_mobile\api\discovery_service\infrastructure\web\MapsController.java`)

### `GET /api/v1/maps/geocode`
- **Méthode Java** : `geocode`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : QueryParams: address
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `GeocodeResult`

### `GET /api/v1/maps/reverse-geocode`
- **Méthode Java** : `reverse`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : QueryParams: latitude, longitude
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `GeocodeResult`

### `POST /api/v1/maps/route`
- **Méthode Java** : `route`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `RouteRequest` (Validé: True)
- **Response DTO** : `RouteResult`


## Contrôleur : `SearchAdminController` (`discovery-service\src\main\java\com\yeyamo_mobile\api\discovery_service\infrastructure\searchadmin\SearchAdminController.java`)

### `GET /api/v1/admin/search/indexes`
- **Méthode Java** : `indexes`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<IndexInfo>`

### `GET /api/v1/admin/search/indexes`
- **Méthode Java** : `indexes`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `IndexInfo>>`

### `POST /api/v1/admin/search/indexes/culture/ensure`
- **Méthode Java** : `ensureCultureIndexes`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<Void>`

### `GET /api/v1/admin/search/overview`
- **Méthode Java** : `overview`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Overview`

### `GET /api/v1/admin/search/overview`
- **Méthode Java** : `overview`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Overview>`

### `GET /api/v1/admin/search/ranking`
- **Méthode Java** : `ranking`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `RankingResponse`

### `GET /api/v1/admin/search/ranking`
- **Méthode Java** : `ranking`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `RankingResponse>`

### `POST /api/v1/admin/search/ranking`
- **Méthode Java** : `ranking`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `SearchAdminDtos` (Validé: False)
- **Response DTO** : `RankingResponse>`

### `PUT /api/v1/admin/search/ranking`
- **Méthode Java** : `ranking`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `RankingRequest` (Validé: True)
- **Response DTO** : `RankingResponse`

### `POST /api/v1/admin/search/reindex`
- **Méthode Java** : `reindex`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ReindexResponse`

### `POST /api/v1/admin/search/reindex`
- **Méthode Java** : `reindex`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ReindexResponse>`

### `POST /api/v1/admin/search/reindex/{indexName}`
- **Méthode Java** : `reindexCultureIndex`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: indexName
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<Void>`

### `GET /api/v1/admin/search/synonyms`
- **Méthode Java** : `synonyms`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<SynonymResponse>`

### `GET /api/v1/admin/search/synonyms`
- **Méthode Java** : `synonyms`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `SynonymResponse>>`

### `POST /api/v1/admin/search/synonyms`
- **Méthode Java** : `create`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `SynonymRequest` (Validé: True)
- **Response DTO** : `SynonymResponse`

### `POST /api/v1/admin/search/synonyms`
- **Méthode Java** : `createSynonym`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `SearchAdminDtos` (Validé: False)
- **Response DTO** : `SynonymResponse>`

### `DELETE /api/v1/admin/search/synonyms/{id}`
- **Méthode Java** : `delete`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `PUT /api/v1/admin/search/synonyms/{id}`
- **Méthode Java** : `update`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `SynonymRequest` (Validé: True)
- **Response DTO** : `SynonymResponse`

### `GET /api/v1/admin/search/zero-results`
- **Méthode Java** : `zero`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<ZeroResult>`

### `GET /api/v1/admin/search/zero-results`
- **Méthode Java** : `zeroResults`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<?>`


# SERVICE : `event-service`


## Contrôleur : `AdminEventController` (`event-service\src\main\java\com\yeyamo_mobile\api\event_service\controller\AdminEventController.java`)

### `GET /api/v1/admin/events`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `POST /api/v1/admin/events`
- **Méthode Java** : `create`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `AdminEventRequest` (Validé: True)
- **Response DTO** : `AdminEventResponse`

### `GET /api/v1/admin/events/{id}`
- **Méthode Java** : `get`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `AdminEventResponse`

### `PUT /api/v1/admin/events/{id}`
- **Méthode Java** : `update`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `AdminEventRequest` (Validé: True)
- **Response DTO** : `AdminEventResponse`

### `POST /api/v1/admin/events/{id}/archive`
- **Méthode Java** : `archive`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `AdminEventActionRequest` (Validé: True)
- **Response DTO** : `AdminEventResponse`

### `POST /api/v1/admin/events/{id}/publish`
- **Méthode Java** : `publish`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `AdminEventActionRequest` (Validé: True)
- **Response DTO** : `AdminEventResponse`

### `POST /api/v1/admin/events/{id}/reject`
- **Méthode Java** : `reject`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `AdminEventActionRequest` (Validé: True)
- **Response DTO** : `AdminEventResponse`

### `PATCH /api/v1/admin/events/{id}/status`
- **Méthode Java** : `status`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id | QueryParams: status
- **Request DTO** : `AdminEventActionRequest` (Validé: True)
- **Response DTO** : `AdminEventResponse`

### `POST /api/v1/admin/events/{id}/suspend`
- **Méthode Java** : `suspend`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `AdminEventActionRequest` (Validé: True)
- **Response DTO** : `AdminEventResponse`


## Contrôleur : `EventController` (`event-service\src\main\java\com\yeyamo_mobile\api\event_service\controller\EventController.java`)

### `GET /api/v1/events`
- **Méthode Java** : `byPlace`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : QueryParams: placeId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<EventSummaryResponse>`

### `POST /api/v1/events`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `EventRequest` (Validé: True)
- **Response DTO** : `EventResponse`

### `GET /api/v1/events/me`
- **Méthode Java** : `mine`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<EventSummaryResponse>`

### `GET /api/v1/events/upcoming`
- **Méthode Java** : `upcoming`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<EventSummaryResponse>`

### `GET /api/v1/events/{id}`
- **Méthode Java** : `getById`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `EventResponse`

### `PUT /api/v1/events/{id}`
- **Méthode Java** : `update`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `EventUpdateRequest` (Validé: True)
- **Response DTO** : `EventResponse`

### `GET /api/v1/events/{id}/participants`
- **Méthode Java** : `participants`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<EventParticipantResponse>`

### `POST /api/v1/events/{id}/register`
- **Méthode Java** : `register`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `EventResponse`

### `PATCH /api/v1/events/{id}/status`
- **Méthode Java** : `updateStatus`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `EventStatusRequest` (Validé: True)
- **Response DTO** : `EventResponse`

### `DELETE /api/v1/events/{id}/unregister`
- **Méthode Java** : `unregister`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `EventResponse`


## Contrôleur : `PlaceEventController` (`event-service\src\main\java\com\yeyamo_mobile\api\event_service\controller\PlaceEventController.java`)

### `GET /api/v1/places/{placeId}/events`
- **Méthode Java** : `findByPlace`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: placeId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<EventSummaryResponse>`


# SERVICE : `feed-service`


## Contrôleur : `FeedController` (`feed-service\src\main\java\com\yeyamo_mobile\api\feed_service\interfaces\rest\FeedController.java`)

### `GET /api/v1/feed`
- **Méthode Java** : `feed`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `FeedPage`


# SERVICE : `gamification-service`


## Contrôleur : `GamificationAdminController` (`gamification-service\src\main\java\com\yeyamo_mobile\api\gamification_service\infrastructure\web\GamificationAdminController.java`)

### `GET /api/v1/admin/gamification/xp-ledger`
- **Méthode Java** : `ledger`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<XpLedgerResponse>`


## Contrôleur : `GamificationController` (`gamification-service\src\main\java\com\yeyamo_mobile\api\gamification_service\infrastructure\web\GamificationController.java`)

### `GET /api/v1/me/badges`
- **Méthode Java** : `badges`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<Badge>`

### `GET /api/v1/me/badges/catalog`
- **Méthode Java** : `badgeCatalog`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<BadgeCatalogEntry>`

### `GET /api/v1/me/badges/catalog/{code}`
- **Méthode Java** : `badge`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: code
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `BadgeCatalogEntry`

### `GET /api/v1/me/badges/stats`
- **Méthode Java** : `badgeStats`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `BadgeStats`

### `GET /api/v1/me/leaderboard`
- **Méthode Java** : `leaderboard`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<LeaderboardEntry>`

### `GET /api/v1/me/passport`
- **Méthode Java** : `passport`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<PassportStamp>`

### `GET /api/v1/me/rewards`
- **Méthode Java** : `rewards`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<Reward>`

### `POST /api/v1/me/rewards/{id}/claim`
- **Méthode Java** : `claim`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Reward`

### `GET /api/v1/me/streaks`
- **Méthode Java** : `streaks`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `StreakResponse`

### `GET /api/v1/me/xp`
- **Méthode Java** : `xp`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Progress`


# SERVICE : `graph-service`


## Contrôleur : `CultureGraphController` (`graph-service\src\main\java\com\yeyamo_mobile\api\graph_service\interfaces\CultureGraphController.java`)

### `POST /api/v1/admin/culture-graph/rebuild`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `GET /api/v1/culture-graph/artisans/{id}/related`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `GET /api/v1/culture-graph/artworks/{id}/related`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `GET /api/v1/culture-graph/cultures/{id}/explore`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `GET /api/v1/culture-graph/discover`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `GET /api/v1/culture-graph/languages/{code}/related`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `GET /api/v1/culture-graph/places/{id}/culture`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`


# SERVICE : `ingestion-service`


## Contrôleur : `IngestionController` (`ingestion-service\src\main\java\com\yeyamo_mobile\api\ingestion_service\interfaces\rest\IngestionController.java`)

### `GET /api/v1/catalog/imports`
- **Méthode Java** : `retry`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ImportJobResponse`

### `POST /api/v1/catalog/imports`
- **Méthode Java** : `submit`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ImportJobResponse`

### `GET /api/v1/catalog/imports/{id}`
- **Méthode Java** : `get`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ImportJobResponse`

### `POST /api/v1/catalog/imports/{id}/cancel`
- **Méthode Java** : `cancel`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ImportJobResponse`

### `GET /api/v1/catalog/imports/{id}/errors`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `POST /api/v1/catalog/imports/{id}/retry`
- **Méthode Java** : `retry`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ImportJobResponse`


# SERVICE : `interaction-service`


## Contrôleur : `AdminContentController` (`interaction-service\src\main\java\com\yeyamo_mobile\api\interaction_service\interfaces\rest\AdminContentController.java`)

### `GET /api/v1/admin/comments`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `DELETE /api/v1/admin/comments/{id}`
- **Méthode Java** : `deleteComment`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `ActionRequest` (Validé: False)
- **Response DTO** : `CommentView`

### `GET /api/v1/admin/comments/{id}`
- **Méthode Java** : `comment`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CommentView`

### `PATCH /api/v1/admin/comments/{id}/hide`
- **Méthode Java** : `hideComment`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `ActionRequest` (Validé: False)
- **Response DTO** : `CommentView`

### `POST /api/v1/admin/comments/{id}/lock`
- **Méthode Java** : `lock`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `ActionRequest` (Validé: False)
- **Response DTO** : `CommentView`

### `PATCH /api/v1/admin/comments/{id}/restore`
- **Méthode Java** : `restoreComment`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `ActionRequest` (Validé: False)
- **Response DTO** : `CommentView`

### `GET /api/v1/admin/reviews`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `DELETE /api/v1/admin/reviews/{id}`
- **Méthode Java** : `deleteReview`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `ActionRequest` (Validé: False)
- **Response DTO** : `ReviewView`

### `GET /api/v1/admin/reviews/{id}`
- **Méthode Java** : `review`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ReviewView`

### `PATCH /api/v1/admin/reviews/{id}/hide`
- **Méthode Java** : `hideReview`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `ActionRequest` (Validé: False)
- **Response DTO** : `ReviewView`

### `PATCH /api/v1/admin/reviews/{id}/restore`
- **Méthode Java** : `restoreReview`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `ActionRequest` (Validé: False)
- **Response DTO** : `ReviewView`


## Contrôleur : `CheckInController` (`interaction-service\src\main\java\com\yeyamo_mobile\api\interaction_service\interfaces\rest\CheckInController.java`)

### `POST /api/v1/checkins`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `CheckInRequest` (Validé: True)
- **Response DTO** : `CheckInResponse`

### `GET /api/v1/checkins/me`
- **Méthode Java** : `mine`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<CheckInResponse>`


## Contrôleur : `GenericInteractionController` (`interaction-service\src\main\java\com\yeyamo_mobile\api\interaction_service\interfaces\rest\GenericInteractionController.java`)

### `GET /api/v1/interactions/{targetType}/{targetId}/comments`
- **Méthode Java** : `comments`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: targetId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<GenericInteractionEntity>`

### `DELETE /api/v1/interactions/{targetType}/{targetId}/{type}`
- **Méthode Java** : `remove`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: targetId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `POST /api/v1/interactions/{targetType}/{targetId}/{type}`
- **Méthode Java** : `add`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: targetId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `GenericInteractionEntity`

### `GET /api/v1/interactions/{targetType}/{targetId}/{type}/status`
- **Méthode Java** : `status`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: targetId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `GenericInteractionEntity`

### `GET /api/v1/interactions/{targetType}/{type}/me`
- **Méthode Java** : `mine`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<GenericInteractionEntity>`


## Contrôleur : `InteractionController` (`interaction-service\src\main\java\com\yeyamo_mobile\api\interaction_service\interfaces\rest\InteractionController.java`)

### `DELETE /api/v1/interactions/comments/{id}`
- **Méthode Java** : `delete`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `PUT /api/v1/interactions/comments/{id}`
- **Méthode Java** : `edit`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `CommentRequest` (Validé: True)
- **Response DTO** : `CommentResponse`

### `DELETE /api/v1/interactions/comments/{id}/like`
- **Méthode Java** : `unlikeComment`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CommentLikeResponse`

### `PUT /api/v1/interactions/comments/{id}/like`
- **Méthode Java** : `likeComment`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CommentLikeResponse`

### `GET /api/v1/interactions/comments/{id}/likes`
- **Méthode Java** : `commentLikeStatus`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CommentLikeResponse`

### `GET /api/v1/interactions/places/{placeId}/reviews`
- **Méthode Java** : `placeReviews`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: placeId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ReviewResponse>`

### `POST /api/v1/interactions/places/{placeId}/reviews`
- **Méthode Java** : `createReview`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: placeId
- **Request DTO** : `None` (Validé: True)
- **Response DTO** : `ReviewResponse`

### `GET /api/v1/interactions/posts/{postId}/comments`
- **Méthode Java** : `comments`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: postId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<CommentResponse>`

### `POST /api/v1/interactions/posts/{postId}/comments`
- **Méthode Java** : `comment`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: postId
- **Request DTO** : `CommentRequest` (Validé: True)
- **Response DTO** : `CommentResponse`

### `DELETE /api/v1/interactions/posts/{postId}/favorite`
- **Méthode Java** : `unfavorite`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: postId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CommandResponse`

### `PUT /api/v1/interactions/posts/{postId}/favorite`
- **Méthode Java** : `favorite`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: postId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CommandResponse`

### `DELETE /api/v1/interactions/posts/{postId}/like`
- **Méthode Java** : `unlike`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: postId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CommandResponse`

### `PUT /api/v1/interactions/posts/{postId}/like`
- **Méthode Java** : `like`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: postId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CommandResponse`

### `POST /api/v1/interactions/posts/{postId}/shares`
- **Méthode Java** : `share`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: postId
- **Request DTO** : `ShareRequest` (Validé: True)
- **Response DTO** : `ShareResponse`

### `GET /api/v1/interactions/posts/{postId}/summary`
- **Méthode Java** : `summary`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: postId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `InteractionSummary`

### `DELETE /api/v1/interactions/reviews/{id}`
- **Méthode Java** : `deleteReview`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `PUT /api/v1/interactions/reviews/{id}`
- **Méthode Java** : `updateReview`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: True)
- **Response DTO** : `ReviewResponse`

### `GET /api/v1/interactions/users/{userId}/reviews`
- **Méthode Java** : `userReviews`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: userId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ReviewResponse>`


## Contrôleur : `SavedPostController` (`interaction-service\src\main\java\com\yeyamo_mobile\api\interaction_service\interfaces\rest\SavedPostController.java`)

### `GET /api/v1/saves`
- **Méthode Java** : `mine`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<FavoriteResponse>`


# SERVICE : `media-service`


## Contrôleur : `MediaController` (`media-service\src\main\java\com\yeyamo_mobile\api\media_service\interfaces\rest\MediaController.java`)

### `POST /api/v1/media`
- **Méthode Java** : `upload`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `MediaResponse`

### `POST /api/v1/media/culture`
- **Méthode Java** : `uploadCulture`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `MediaResponse`

### `DELETE /api/v1/media/{id}`
- **Méthode Java** : `delete`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `GET /api/v1/media/{id}`
- **Méthode Java** : `metadata`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `MediaResponse`

### `GET /api/v1/media/{id}/content`
- **Méthode Java** : `content`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<InputStreamResource>`

### `POST /api/v1/media/{id}/signed-url`
- **Méthode Java** : `signedUrl`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Map<String,String>`

### `GET /api/v1/media/{id}/thumbnail`
- **Méthode Java** : `thumbnail`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<InputStreamResource>`


# SERVICE : `messaging-service`


## Contrôleur : `MessagingController` (`messaging-service\src\main\java\com\yeyamo_mobile\api\messaging_service\infrastructure\web\MessagingController.java`)

### `GET /api/v1/messaging/conversations`
- **Méthode Java** : `get`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ConversationView`

### `POST /api/v1/messaging/conversations`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `CreateConversationRequest` (Validé: True)
- **Response DTO** : `ConversationView`

### `GET /api/v1/messaging/conversations/{id}`
- **Méthode Java** : `get`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ConversationView`

### `POST /api/v1/messaging/conversations/{id}/leave`
- **Méthode Java** : `leave`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `POST /api/v1/messaging/conversations/{id}/members`
- **Méthode Java** : `add`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `MemberRequest` (Validé: True)
- **Response DTO** : `ConversationView`

### `DELETE /api/v1/messaging/conversations/{id}/members/{userId}`
- **Méthode Java** : `remove`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id, userId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `GET /api/v1/messaging/conversations/{id}/messages`
- **Méthode Java** : `messages`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `MessageSlice`

### `POST /api/v1/messaging/conversations/{id}/messages`
- **Méthode Java** : `send`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `SendMessageRequest` (Validé: True)
- **Response DTO** : `MessageView`

### `POST /api/v1/messaging/conversations/{id}/read/{messageId}`
- **Méthode Java** : `read`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id, messageId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `DELETE /api/v1/messaging/messages/{messageId}`
- **Méthode Java** : `delete`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: messageId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `PATCH /api/v1/messaging/messages/{messageId}`
- **Méthode Java** : `edit`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: messageId
- **Request DTO** : `EditMessageRequest` (Validé: True)
- **Response DTO** : `MessageView`


# SERVICE : `mission-reward-service`


## Contrôleur : `GamificationAdminController` (`mission-reward-service\src\main\java\com\yeyamo_mobile\api\mission_reward_service\infrastructure\web\GamificationAdminController.java`)

### `GET /api/v1/mission-management/badges`
- **Méthode Java** : `badges`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<BadgeDefinitionEntity>`

### `POST /api/v1/mission-management/badges`
- **Méthode Java** : `createBadge`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `BadgeCommand` (Validé: True)
- **Response DTO** : `BadgeDefinitionEntity`

### `GET /api/v1/mission-management/badges/{id}`
- **Méthode Java** : `badge`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `BadgeDefinitionEntity`

### `PUT /api/v1/mission-management/badges/{id}`
- **Méthode Java** : `updateBadge`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `BadgeCommand` (Validé: True)
- **Response DTO** : `BadgeDefinitionEntity`

### `PATCH /api/v1/mission-management/badges/{id}/status`
- **Méthode Java** : `badgeStatus`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `StatusCommand` (Validé: False)
- **Response DTO** : `BadgeDefinitionEntity`

### `GET /api/v1/mission-management/fraud-alerts`
- **Méthode Java** : `fraud`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<FraudAlertEntity>`

### `GET /api/v1/mission-management/rewards`
- **Méthode Java** : `rewards`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<RewardDefinitionEntity>`

### `POST /api/v1/mission-management/rewards`
- **Méthode Java** : `createReward`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `RewardCommand` (Validé: False)
- **Response DTO** : `RewardDefinitionEntity`

### `PUT /api/v1/mission-management/rewards/{id}`
- **Méthode Java** : `updateReward`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `RewardCommand` (Validé: False)
- **Response DTO** : `RewardDefinitionEntity`

### `PATCH /api/v1/mission-management/rewards/{id}/status`
- **Méthode Java** : `rewardStatus`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `StatusCommand` (Validé: False)
- **Response DTO** : `RewardDefinitionEntity`

### `GET /api/v1/mission-management/xp-rules`
- **Méthode Java** : `rules`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<XpRuleEntity>`

### `POST /api/v1/mission-management/xp-rules`
- **Méthode Java** : `createRule`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `XpRuleCommand` (Validé: False)
- **Response DTO** : `XpRuleEntity`

### `PUT /api/v1/mission-management/xp-rules/{id}`
- **Méthode Java** : `updateRule`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `XpRuleCommand` (Validé: False)
- **Response DTO** : `XpRuleEntity`

### `PATCH /api/v1/mission-management/xp-rules/{id}/status`
- **Méthode Java** : `ruleStatus`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `StatusCommand` (Validé: False)
- **Response DTO** : `XpRuleEntity`


## Contrôleur : `MissionController` (`mission-reward-service\src\main\java\com\yeyamo_mobile\api\mission_reward_service\infrastructure\web\MissionController.java`)

### `GET /api/v1/me/mission-rewards`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `CreateMission` (Validé: True)
- **Response DTO** : `MissionView`

### `GET /api/v1/me/missions`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `CreateMission` (Validé: True)
- **Response DTO** : `MissionView`

### `POST /api/v1/mission-management/missions`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `CreateMission` (Validé: True)
- **Response DTO** : `MissionView`

### `DELETE /api/v1/mission-management/missions/{id}`
- **Méthode Java** : `archive`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `MissionView`

### `GET /api/v1/mission-management/missions/{id}`
- **Méthode Java** : `get`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `MissionView`

### `PUT /api/v1/mission-management/missions/{id}`
- **Méthode Java** : `update`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `UpdateMission` (Validé: True)
- **Response DTO** : `MissionView`

### `POST /api/v1/mission-management/missions/{id}/activate`
- **Méthode Java** : `activate`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `MissionView`

### `POST /api/v1/mission-management/missions/{id}/pause`
- **Méthode Java** : `pause`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `MissionView`

### `GET /api/v1/missions`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`


# SERVICE : `moderation-trust-service`


## Contrôleur : `AuditController` (`moderation-trust-service\src\main\java\com\yeyamo_mobile\api\moderation_trust_service\interfaces\rest\AuditController.java`)

### `GET /api/v1/moderation/audit`
- **Méthode Java** : `list`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<AuditEntryEntity>`


## Contrôleur : `CulturalModerationController` (`moderation-trust-service\src\main\java\com\yeyamo_mobile\api\moderation_trust_service\interfaces\rest\CulturalModerationController.java`)

### `POST /api/v1/moderation/culture/authenticity/claims`
- **Méthode Java** : `declareClaim`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `AuthenticityClaimRequest` (Validé: True)
- **Response DTO** : `AuthenticityClaim`

### `GET /api/v1/moderation/culture/authenticity/claims/{claimId}`
- **Méthode Java** : `getClaim`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: claimId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `AuthenticityClaim`

### `POST /api/v1/moderation/culture/authenticity/claims/{claimId}/assign`
- **Méthode Java** : `assignForReview`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: claimId
- **Request DTO** : `AssignReviewerRequest` (Validé: True)
- **Response DTO** : `void`

### `GET /api/v1/moderation/culture/authenticity/claims/{claimId}/evidence`
- **Méthode Java** : `getEvidence`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: claimId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<AuthenticityEvidence>`

### `POST /api/v1/moderation/culture/authenticity/claims/{claimId}/evidence`
- **Méthode Java** : `submitEvidence`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: claimId
- **Request DTO** : `EvidenceRequest` (Validé: True)
- **Response DTO** : `AuthenticityEvidence`

### `POST /api/v1/moderation/culture/authenticity/claims/{claimId}/reject`
- **Méthode Java** : `rejectClaim`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: claimId
- **Request DTO** : `ReviewDecisionRequest` (Validé: True)
- **Response DTO** : `AuthenticityClaim`

### `POST /api/v1/moderation/culture/authenticity/claims/{claimId}/revoke`
- **Méthode Java** : `revoke`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: claimId
- **Request DTO** : `ReviewDecisionRequest` (Validé: True)
- **Response DTO** : `AuthenticityClaim`

### `POST /api/v1/moderation/culture/authenticity/claims/{claimId}/verify`
- **Méthode Java** : `verify`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: claimId
- **Request DTO** : `ReviewDecisionRequest` (Validé: True)
- **Response DTO** : `AuthenticityClaim`

### `POST /api/v1/moderation/culture/copyright/claims`
- **Méthode Java** : `fileCopyrightClaim`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `CopyrightClaimRequest` (Validé: True)
- **Response DTO** : `CopyrightClaim`

### `GET /api/v1/moderation/culture/copyright/claims/{claimId}`
- **Méthode Java** : `getCopyrightClaim`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: claimId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CopyrightClaim`

### `POST /api/v1/moderation/culture/copyright/claims/{claimId}/dismiss`
- **Méthode Java** : `dismiss`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: claimId
- **Request DTO** : `ReviewDecisionRequest` (Validé: True)
- **Response DTO** : `CopyrightClaim`

### `POST /api/v1/moderation/culture/copyright/claims/{claimId}/respond`
- **Méthode Java** : `respond`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: claimId
- **Request DTO** : `CreatorResponseRequest` (Validé: True)
- **Response DTO** : `CopyrightClaim`

### `POST /api/v1/moderation/culture/copyright/claims/{claimId}/uphold`
- **Méthode Java** : `uphold`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: claimId
- **Request DTO** : `ReviewDecisionRequest` (Validé: True)
- **Response DTO** : `CopyrightClaim`

### `GET /api/v1/moderation/culture/reviewers/eligible`
- **Méthode Java** : `eligibleReviewers`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : QueryParams: contentType
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<CulturalReviewer>`

### `GET /api/v1/moderation/culture/sensitive-content/check`
- **Méthode Java** : `isBlocked`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : QueryParams: targetType, targetId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `BlockedStatusResponse`

### `POST /api/v1/moderation/culture/sensitive-content/flags`
- **Méthode Java** : `flagContent`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `SensitiveFlagRequest` (Validé: True)
- **Response DTO** : `SensitiveContentFlag`

### `POST /api/v1/moderation/culture/sensitive-content/flags/{flagId}/approve`
- **Méthode Java** : `approveFlag`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: flagId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `SensitiveContentFlag`


## Contrôleur : `ModerationController` (`moderation-trust-service\src\main\java\com\yeyamo_mobile\api\moderation_trust_service\interfaces\rest\ModerationController.java`)

### `GET /api/v1/moderation/reports`
- **Méthode Java** : `list`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<ReportResponse>`

### `POST /api/v1/moderation/reports`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `ReportRequest` (Validé: True)
- **Response DTO** : `ReportResponse`

### `GET /api/v1/moderation/reports/me`
- **Méthode Java** : `mine`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<ReportResponse>`

### `GET /api/v1/moderation/reports/{id}`
- **Méthode Java** : `get`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ReportResponse`

### `POST /api/v1/moderation/reports/{id}/decision`
- **Méthode Java** : `decide`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `DecisionRequest` (Validé: True)
- **Response DTO** : `ReportResponse`

### `POST /api/v1/moderation/reports/{id}/review`
- **Méthode Java** : `review`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ReportResponse`


## Contrôleur : `TrustController` (`moderation-trust-service\src\main\java\com\yeyamo_mobile\api\moderation_trust_service\interfaces\rest\TrustController.java`)

### `GET /api/v1/trust/{subjectId}`
- **Méthode Java** : `get`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: subjectId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `TrustScore`


## Contrôleur : `TrustSafetyAdminController` (`moderation-trust-service\src\main\java\com\yeyamo_mobile\api\moderation_trust_service\interfaces\rest\TrustSafetyAdminController.java`)

### `GET /api/v1/moderation/admin/audit`
- **Méthode Java** : `audit`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<AuditEntryEntity>`

### `GET /api/v1/moderation/admin/reports`
- **Méthode Java** : `reports`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<ModerationReportEntity>`

### `POST /api/v1/moderation/reports/{id}/assign`
- **Méthode Java** : `assign`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `AssignRequest` (Validé: True)
- **Response DTO** : `ModerationReportEntity`

### `GET /api/v1/trust`
- **Méthode Java** : `trust`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<TrustScoreEntity>`

### `GET /api/v1/trust/{subjectId}/history`
- **Méthode Java** : `history`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: subjectId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<SanctionEntity>`

### `POST /api/v1/trust/{subjectId}/sanctions`
- **Méthode Java** : `sanction`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: subjectId
- **Request DTO** : `SanctionRequest` (Validé: True)
- **Response DTO** : `SanctionEntity`


# SERVICE : `notification-service`


## Contrôleur : `AdminNotificationController` (`notification-service\src\main\java\com\yeyamo_mobile\api\notification_service\admin\AdminNotificationController.java`)

### `GET /api/v1/admin/notifications`
- **Méthode Java** : `unread`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `UnreadCount`

### `POST /api/v1/admin/notifications/read-all`
- **Méthode Java** : `all`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Updated`

### `GET /api/v1/admin/notifications/unread-count`
- **Méthode Java** : `unread`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `UnreadCount`

### `PATCH /api/v1/admin/notifications/{id}/read`
- **Méthode Java** : `read`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Response`

### `PATCH /api/v1/admin/notifications/{id}/unread`
- **Méthode Java** : `unread`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Response`


## Contrôleur : `NewsletterAdminController` (`notification-service\src\main\java\com\yeyamo_mobile\api\notification_service\newsletter\NewsletterAdminController.java`)

### `GET /api/v1/admin/newsletters`
- **Méthode Java** : `list`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<CampaignResponse>`

### `POST /api/v1/admin/newsletters`
- **Méthode Java** : `create`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `CampaignRequest` (Validé: True)
- **Response DTO** : `CampaignResponse`

### `GET /api/v1/admin/newsletters/{id}`
- **Méthode Java** : `get`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CampaignResponse`

### `PUT /api/v1/admin/newsletters/{id}`
- **Méthode Java** : `update`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `CampaignRequest` (Validé: True)
- **Response DTO** : `CampaignResponse`

### `POST /api/v1/admin/newsletters/{id}/cancel`
- **Méthode Java** : `cancel`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CampaignResponse`

### `POST /api/v1/admin/newsletters/{id}/pause`
- **Méthode Java** : `pause`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CampaignResponse`

### `POST /api/v1/admin/newsletters/{id}/schedule`
- **Méthode Java** : `schedule`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `ScheduleRequest` (Validé: True)
- **Response DTO** : `CampaignResponse`

### `POST /api/v1/admin/newsletters/{id}/send`
- **Méthode Java** : `send`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CampaignResponse`

### `GET /api/v1/admin/newsletters/{id}/stats`
- **Méthode Java** : `stats`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `StatsResponse`


## Contrôleur : `NotificationController` (`notification-service\src\main\java\com\yeyamo_mobile\api\notification_service\infrastructure\web\NotificationController.java`)

### `GET /api/v1/notifications`
- **Méthode Java** : `mine`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `NotificationSlice`

### `POST /api/v1/notifications/devices/push-token`
- **Méthode Java** : `registerPushToken`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `PushTokenRequest` (Validé: True)
- **Response DTO** : `void`

### `DELETE /api/v1/notifications/devices/push-token/{deviceId}`
- **Méthode Java** : `unregisterPushToken`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: deviceId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `GET /api/v1/notifications/preferences`
- **Méthode Java** : `preferences`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `NotificationPreference`

### `PUT /api/v1/notifications/preferences`
- **Méthode Java** : `preferences`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `PreferenceRequest` (Validé: True)
- **Response DTO** : `NotificationPreference`

### `POST /api/v1/notifications/read-all`
- **Méthode Java** : `readAll`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ReadAllResponse`

### `GET /api/v1/notifications/unread`
- **Méthode Java** : `unread`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `NotificationSlice`

### `GET /api/v1/notifications/unread/count`
- **Méthode Java** : `unreadCount`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `UnreadCountResponse`

### `DELETE /api/v1/notifications/{id}`
- **Méthode Java** : `delete`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `POST /api/v1/notifications/{id}/read`
- **Méthode Java** : `read`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Notification`


# SERVICE : `partner-service`


## Contrôleur : `AdminArtisanController` (`partner-service\src\main\java\com\yeyamo_mobile\api\partner_service\interfaces\rest\AdminArtisanController.java`)

### `PATCH /api/v1/admin/artisans/{id}/verification`
- **Méthode Java** : `verify`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `VerificationRequest` (Validé: True)
- **Response DTO** : `Response`


## Contrôleur : `AdminPartnerController` (`partner-service\src\main\java\com\yeyamo_mobile\api\partner_service\interfaces\rest\AdminPartnerController.java`)

### `GET /api/v1/admin/partners`
- **Méthode Java** : `list`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('SUPPORT','ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Summary>`

### `GET /api/v1/admin/partners/{id}`
- **Méthode Java** : `detail`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('SUPPORT','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Detail`

### `GET /api/v1/admin/partners/{id}/establishments`
- **Méthode Java** : `establishments`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('SUPPORT','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `JsonNode`

### `GET /api/v1/admin/partners/{id}/kyc`
- **Méthode Java** : `establishments`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `JsonNode`

### `GET /api/v1/admin/partners/{id}/validation-history`
- **Méthode Java** : `establishments`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `JsonNode`

### `GET /api/v1/admin/partners/{partnerId}/kyc/documents/{documentId}`
- **Méthode Java** : `document`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: partnerId, documentId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<InputStreamResource>`


## Contrôleur : `ArtisanController` (`partner-service\src\main\java\com\yeyamo_mobile\api\partner_service\interfaces\rest\ArtisanController.java`)

### `GET /api/v1/artisan-specialties`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `GET /api/v1/artisans`
- **Méthode Java** : `search`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<Response>`

### `GET /api/v1/artisans/{id}`
- **Méthode Java** : `publicProfile`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Response`

### `GET /api/v1/partners/me/artisan-profile`
- **Méthode Java** : `me`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Response`

### `POST /api/v1/partners/me/artisan-profile`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `Request` (Validé: True)
- **Response DTO** : `Response`

### `PUT /api/v1/partners/me/artisan-profile`
- **Méthode Java** : `update`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `Request` (Validé: True)
- **Response DTO** : `Response`


## Contrôleur : `PartnerController` (`partner-service\src\main\java\com\yeyamo_mobile\api\partner_service\interfaces\rest\PartnerController.java`)

### `GET /api/v1/partners`
- **Méthode Java** : `search`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<PublicPartnerResponse>`

### `POST /api/v1/partners`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `PartnerRequest` (Validé: True)
- **Response DTO** : `ResponseEntity<PartnerResponse>`

### `GET /api/v1/partners/me`
- **Méthode Java** : `me`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `PartnerResponse`

### `PUT /api/v1/partners/me`
- **Méthode Java** : `update`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `PartnerRequest` (Validé: True)
- **Response DTO** : `PartnerResponse`

### `GET /api/v1/partners/me/documents`
- **Méthode Java** : `documents`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<DocumentResponse>`

### `POST /api/v1/partners/me/documents`
- **Méthode Java** : `upload`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : QueryParams: type
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<DocumentResponse>`

### `DELETE /api/v1/partners/me/documents/{id}`
- **Méthode Java** : `remove`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<Void>`

### `POST /api/v1/partners/me/submit`
- **Méthode Java** : `submit`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `PartnerResponse`

### `GET /api/v1/partners/{id}`
- **Méthode Java** : `byId`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `PublicPartnerResponse`


## Contrôleur : `PartnerOnboardingController` (`partner-service\src\main\java\com\yeyamo_mobile\api\partner_service\interfaces\rest\PartnerOnboardingController.java`)

### `GET /api/v1/partners/onboarding/requirements`
- **Méthode Java** : `requirements`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : QueryParams: countryCode, partnerType
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Requirements`


## Contrôleur : `PartnerOperationsController` (`partner-service\src\main\java\com\yeyamo_mobile\api\partner_service\interfaces\rest\PartnerOperationsController.java`)

### `GET /api/v1/partners/{partnerId}/operations/advertising-credits`
- **Méthode Java** : `credits`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Object`

### `GET /api/v1/partners/{partnerId}/operations/billing-preferences`
- **Méthode Java** : `billing`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `PartnerBillingPreferences`

### `PUT /api/v1/partners/{partnerId}/operations/billing-preferences`
- **Méthode Java** : `billing`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId
- **Request DTO** : `BillingRequest` (Validé: True)
- **Response DTO** : `PartnerBillingPreferences`

### `GET /api/v1/partners/{partnerId}/operations/commercial-statistics`
- **Méthode Java** : `stats`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Object`


## Contrôleur : `PartnerStaffController` (`partner-service\src\main\java\com\yeyamo_mobile\api\partner_service\interfaces\rest\PartnerStaffController.java`)

### `GET /api/v1/partners/{partnerId}/staff/audit`
- **Méthode Java** : `audit`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<PartnerStaffAuditEntity>`

### `POST /api/v1/partners/{partnerId}/staff/invitations`
- **Méthode Java** : `accept`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId
- **Request DTO** : `AcceptRequest` (Validé: True)
- **Response DTO** : `PartnerMembershipEntity`

### `POST /api/v1/partners/{partnerId}/staff/invitations/accept`
- **Méthode Java** : `accept`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId
- **Request DTO** : `AcceptRequest` (Validé: True)
- **Response DTO** : `PartnerMembershipEntity`

### `GET /api/v1/partners/{partnerId}/staff/permissions/{permission}`
- **Méthode Java** : `permission`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId, permission
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Map<String,Boolean>`

### `POST /api/v1/partners/{partnerId}/staff/roles`
- **Méthode Java** : `role`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId
- **Request DTO** : `RoleRequest` (Validé: True)
- **Response DTO** : `PartnerRoleEntity`

### `DELETE /api/v1/partners/{partnerId}/staff/{userId}`
- **Méthode Java** : `revoke`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId, userId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `PUT /api/v1/partners/{partnerId}/staff/{userId}/role/{roleId}`
- **Méthode Java** : `role`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId, userId, roleId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`


# SERVICE : `payment-service`


## Contrôleur : `AdminPaymentController` (`payment-service\src\main\java\com\yeyamo_mobile\api\payment_service\infrastructure\web\AdminPaymentController.java`)

### `GET /api/v1/payments/admin`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN','SUPPORT')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `GET /api/v1/payments/admin/anomalies`
- **Méthode Java** : `reconciliation`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ReconciliationResponse`

### `POST /api/v1/payments/admin/reconciliation`
- **Méthode Java** : `reconciliation`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ReconciliationResponse`

### `GET /api/v1/payments/admin/{id}`
- **Méthode Java** : `detail`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN','SUPPORT')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `AdminPaymentDetail`


## Contrôleur : `PaymentController` (`payment-service\src\main\java\com\yeyamo_mobile\api\payment_service\infrastructure\web\PaymentController.java`)

### `GET /api/v1/payments/mine`
- **Méthode Java** : `mine`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<PaymentView>`

### `GET /api/v1/payments/{id}`
- **Méthode Java** : `get`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `PaymentView`

### `GET /api/v1/payments/{id}/refunds`
- **Méthode Java** : `refunds`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<RefundView>`

### `POST /api/v1/payments/{id}/refunds`
- **Méthode Java** : `refund`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `ManualRefundRequest` (Validé: True)
- **Response DTO** : `RefundView`


## Contrôleur : `WebhookController` (`payment-service\src\main\java\com\yeyamo_mobile\api\payment_service\infrastructure\web\WebhookController.java`)

### `POST /api/v1/payments/webhooks/{provider}`
- **Méthode Java** : `receive`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`


# SERVICE : `place-service`


## Contrôleur : `AdminPlaceController` (`place-service\src\main\java\com\yeyamo_mobile\api\place_service\controller\AdminPlaceController.java`)

### `GET /api/v1/admin/places`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `POST /api/v1/admin/places`
- **Méthode Java** : `create`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `PlaceRequest` (Validé: True)
- **Response DTO** : `PlaceResponse`

### `GET /api/v1/admin/places/{id}`
- **Méthode Java** : `detail`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `AdminPlaceResponse`

### `PUT /api/v1/admin/places/{id}`
- **Méthode Java** : `update`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `PlaceRequest` (Validé: True)
- **Response DTO** : `PlaceResponse`

### `PATCH /api/v1/admin/places/{id}/status`
- **Méthode Java** : `status`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `PlaceStatusRequest` (Validé: True)
- **Response DTO** : `AdminPlaceResponse`


## Contrôleur : `CategoryController` (`place-service\src\main\java\com\yeyamo_mobile\api\place_service\controller\CategoryController.java`)

### `GET /api/v1/categories`
- **Méthode Java** : `listCategories`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<CategoryResponse>`

### `POST /api/v1/categories`
- **Méthode Java** : `create`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `CategoryRequest` (Validé: True)
- **Response DTO** : `CategoryResponse`

### `DELETE /api/v1/categories/{id}`
- **Méthode Java** : `delete`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `PUT /api/v1/categories/{id}`
- **Méthode Java** : `update`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `CategoryRequest` (Validé: True)
- **Response DTO** : `CategoryResponse`

### `PATCH /api/v1/categories/{id}/status`
- **Méthode Java** : `status`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `ReferenceStatusRequest` (Validé: False)
- **Response DTO** : `CategoryResponse`


## Contrôleur : `CityController` (`place-service\src\main\java\com\yeyamo_mobile\api\place_service\controller\CityController.java`)

### `POST /api/v1/cities`
- **Méthode Java** : `create`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `CityRequest` (Validé: True)
- **Response DTO** : `CityResponse`

### `GET /api/v1/cities/region/{regionId}`
- **Méthode Java** : `listByRegion`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: regionId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<CityResponse>`

### `DELETE /api/v1/cities/{id}`
- **Méthode Java** : `delete`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `PUT /api/v1/cities/{id}`
- **Méthode Java** : `update`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `CityRequest` (Validé: True)
- **Response DTO** : `CityResponse`

### `GET /api/v1/cities/{id}/places`
- **Méthode Java** : `placesByCity`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<PlaceSummaryResponse>`

### `PATCH /api/v1/cities/{id}/status`
- **Méthode Java** : `status`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `ReferenceStatusRequest` (Validé: False)
- **Response DTO** : `CityResponse`


## Contrôleur : `CountryController` (`place-service\src\main\java\com\yeyamo_mobile\api\place_service\controller\CountryController.java`)

### `GET /api/v1/countries`
- **Méthode Java** : `list`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<CountryResponse>`

### `GET /api/v1/countries/{countryCode}`
- **Méthode Java** : `get`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: countryCode
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `CountryResponse`

### `GET /api/v1/countries/{countryCode}/administrative-areas`
- **Méthode Java** : `administrativeAreas`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: countryCode
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<RegionResponse>`

### `GET /api/v1/countries/{countryCode}/languages`
- **Méthode Java** : `languages`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: countryCode
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<CountryLanguageResponse>`


## Contrôleur : `DistrictController` (`place-service\src\main\java\com\yeyamo_mobile\api\place_service\controller\DistrictController.java`)

### `POST /api/v1/districts`
- **Méthode Java** : `create`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `DistrictRequest` (Validé: True)
- **Response DTO** : `DistrictResponse`

### `GET /api/v1/districts/city/{cityId}`
- **Méthode Java** : `listByCity`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: cityId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<DistrictResponse>`

### `DELETE /api/v1/districts/{id}`
- **Méthode Java** : `delete`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `PUT /api/v1/districts/{id}`
- **Méthode Java** : `update`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `DistrictRequest` (Validé: True)
- **Response DTO** : `DistrictResponse`

### `PATCH /api/v1/districts/{id}/status`
- **Méthode Java** : `status`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `ReferenceStatusRequest` (Validé: False)
- **Response DTO** : `DistrictResponse`


## Contrôleur : `PlaceController` (`place-service\src\main\java\com\yeyamo_mobile\api\place_service\controller\PlaceController.java`)

### `POST /api/v1/places`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `PlaceRequest` (Validé: True)
- **Response DTO** : `PlaceResponse`

### `GET /api/v1/places/nearby`
- **Méthode Java** : `nearby`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<PlaceSummaryResponse>`

### `GET /api/v1/places/{id}`
- **Méthode Java** : `getById`
- **Niveau Auth** : `PUBLIC`
- **Rôles / Permissions** : `None`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `PlaceResponse`

### `PUT /api/v1/places/{id}`
- **Méthode Java** : `update`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `PlaceRequest` (Validé: True)
- **Response DTO** : `PlaceResponse`


## Contrôleur : `RegionController` (`place-service\src\main\java\com\yeyamo_mobile\api\place_service\controller\RegionController.java`)

### `GET /api/v1/regions`
- **Méthode Java** : `listRegions`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<RegionResponse>`

### `POST /api/v1/regions`
- **Méthode Java** : `create`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `RegionRequest` (Validé: True)
- **Response DTO** : `RegionResponse`

### `DELETE /api/v1/regions/{id}`
- **Méthode Java** : `delete`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasRole('SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `PUT /api/v1/regions/{id}`
- **Méthode Java** : `update`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `RegionRequest` (Validé: True)
- **Response DTO** : `RegionResponse`

### `PATCH /api/v1/regions/{id}/status`
- **Méthode Java** : `status`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: id
- **Request DTO** : `ReferenceStatusRequest` (Validé: False)
- **Response DTO** : `RegionResponse`

### `GET /api/v1/regions/{slug}`
- **Méthode Java** : `getBySlug`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: slug
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `RegionResponse`

### `GET /api/v1/regions/{slug}/places`
- **Méthode Java** : `placesByRegion`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: slug
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<PlaceSummaryResponse>`


# SERVICE : `recommendation-service`


## Contrôleur : `RecommendationController` (`recommendation-service\src\main\java\com\yeyamo_mobile\api\recommendation_service\infrastructure\web\RecommendationController.java`)

### `GET /api/v1/recommendations`
- **Méthode Java** : `recommendations`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `RecommendationPage`


# SERVICE : `referral-service`


## Contrôleur : `ReferralController` (`referral-service\src\main\java\com\yeyamo_mobile\api\referral_service\infrastructure\web\ReferralController.java`)

### `GET /api/v1/me/referrals/attributions`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `GET /api/v1/me/referrals/attributions/{id}/history`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `GET /api/v1/me/referrals/codes`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `GET /api/v1/me/referrals/invitations`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `GET /api/v1/me/referrals/rewards`
- **Méthode Java** : `unknown`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `unknown`

### `POST /api/v1/referrals/codes`
- **Méthode Java** : `create`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `CreateCode` (Validé: True)
- **Response DTO** : `CodeView`

### `POST /api/v1/referrals/codes/{id}/disable`
- **Méthode Java** : `disable`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `POST /api/v1/referrals/invitations`
- **Méthode Java** : `invite`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `Invite` (Validé: True)
- **Response DTO** : `InvitationView`

### `POST /api/v1/referrals/redeem`
- **Méthode Java** : `redeem`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `Redeem` (Validé: True)
- **Response DTO** : `AttributionView`


# SERVICE : `support-service`


## Contrôleur : `SupportAdminController` (`support-service\src\main\java\com\yeyamo_mobile\api\support_service\infrastructure\web\SupportAdminController.java`)

### `GET /api/v1/admin/support/conversations`
- **Méthode Java** : `list`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<ConversationSummary>`

### `GET /api/v1/admin/support/conversations/{id}`
- **Méthode Java** : `detail`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ConversationDetail`

### `PATCH /api/v1/admin/support/conversations/{id}/assign`
- **Méthode Java** : `assign`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `AssignRequest` (Validé: True)
- **Response DTO** : `ConversationSummary`

### `POST /api/v1/admin/support/conversations/{id}/messages`
- **Méthode Java** : `message`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `MessageRequest` (Validé: True)
- **Response DTO** : `MessageResponse`

### `POST /api/v1/admin/support/conversations/{id}/notes`
- **Méthode Java** : `note`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `NoteRequest` (Validé: True)
- **Response DTO** : `NoteResponse`

### `PATCH /api/v1/admin/support/conversations/{id}/priority`
- **Méthode Java** : `priority`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `PriorityRequest` (Validé: True)
- **Response DTO** : `ConversationSummary`

### `PATCH /api/v1/admin/support/conversations/{id}/status`
- **Méthode Java** : `status`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `ROLE_ADMIN / ROLE_SUPER_ADMIN`
- **Paramètres** : PathVars: id
- **Request DTO** : `StatusRequest` (Validé: True)
- **Response DTO** : `ConversationSummary`


# SERVICE : `ticket-service`


## Contrôleur : `AdminEventTicketController` (`ticket-service\src\main\java\com\yeyamo_mobile\api\ticket_service\interfaces\rest\AdminEventTicketController.java`)

### `GET /api/v1/admin/events/{eventId}/participants`
- **Méthode Java** : `stats`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: eventId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `TicketingStats`

### `GET /api/v1/admin/events/{eventId}/ticketing-stats`
- **Méthode Java** : `stats`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : PathVars: eventId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `TicketingStats`

### `GET /api/v1/admin/events/{eventId}/tickets`
- **Méthode Java** : `TicketTypeView`
- **Niveau Auth** : `ADMIN`
- **Rôles / Permissions** : `hasAnyRole('ADMIN','SUPER_ADMIN')`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `>new`


## Contrôleur : `PartnerTicketController` (`ticket-service\src\main\java\com\yeyamo_mobile\api\ticket_service\presentation\controller\PartnerTicketController.java`)

### `POST /api/v1/partner/tickets/configurations`
- **Méthode Java** : `createSaleConfiguration`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `CreateSaleConfigurationRequest` (Validé: True)
- **Response DTO** : `ResponseEntity<TicketSaleConfiguration>`

### `POST /api/v1/partner/tickets/configurations/{configId}/activate`
- **Méthode Java** : `activateSales`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: configId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<Void>`

### `GET /api/v1/partner/tickets/events/{eventId}/scan-stats`
- **Méthode Java** : `getScanStats`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: eventId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<Map<String, Object>>`

### `GET /api/v1/partner/tickets/events/{eventId}/staff`
- **Méthode Java** : `getEventStaff`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: eventId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<List<EventStaffAssignment>>`

### `POST /api/v1/partner/tickets/scan`
- **Méthode Java** : `scanTicket`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `TicketScanRequest` (Validé: True)
- **Response DTO** : `ResponseEntity<TicketScanResponse>`

### `POST /api/v1/partner/tickets/staff`
- **Méthode Java** : `createStaffAssignment`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `CreateStaffAssignmentRequest` (Validé: True)
- **Response DTO** : `ResponseEntity<EventStaffAssignment>`

### `DELETE /api/v1/partner/tickets/staff/{assignmentId}`
- **Méthode Java** : `revokeStaffAssignment`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: assignmentId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<Void>`

### `PUT /api/v1/partners/{partnerId}/tickets/configuration`
- **Méthode Java** : `config`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `TicketSaleConfigurationEntity`

### `POST /api/v1/partners/{partnerId}/tickets/configurations/{id}/types`
- **Méthode Java** : `type`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId, id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `TicketTypeEntity`

### `GET /api/v1/partners/{partnerId}/tickets/events/{eventId}/staff`
- **Méthode Java** : `StaffRoleRequest`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `record`

### `DELETE /api/v1/partners/{partnerId}/tickets/events/{eventId}/staff/{assignmentId}`
- **Méthode Java** : `revokeEventStaff`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId, eventId, assignmentId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `PUT /api/v1/partners/{partnerId}/tickets/events/{eventId}/staff/{assignmentId}/role`
- **Méthode Java** : `staffRole`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId, eventId, assignmentId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `EventStaffAssignmentEntity`

### `GET /api/v1/partners/{partnerId}/tickets/events/{eventId}/types`
- **Méthode Java** : `staff`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `EventStaffAssignmentEntity`

### `POST /api/v1/partners/{partnerId}/tickets/staff`
- **Méthode Java** : `staff`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `EventStaffAssignmentEntity`

### `DELETE /api/v1/partners/{partnerId}/tickets/staff/{id}`
- **Méthode Java** : `revoke`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: partnerId, id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`


## Contrôleur : `PublicTicketTypeController` (`ticket-service\src\main\java\com\yeyamo_mobile\api\ticket_service\interfaces\rest\PublicTicketTypeController.java`)

### `GET /api/v1/tickets/events/{eventId}/types`
- **Méthode Java** : `available`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: eventId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `AvailableTicketTypesResponse`


## Contrôleur : `ScanController` (`ticket-service\src\main\java\com\yeyamo_mobile\api\ticket_service\interfaces\rest\ScanController.java`)

### `POST /api/v1/tickets/scan`
- **Méthode Java** : `scanTicket`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `ScanRequestDto` (Validé: True)
- **Response DTO** : `ResponseEntity<ScanResponseDto>`

### `GET /api/v1/tickets/scans/stats/{eventId}`
- **Méthode Java** : `getScanStatistics`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: eventId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<ScanStatisticsDto>`


## Contrôleur : `UserTicketController` (`ticket-service\src\main\java\com\yeyamo_mobile\api\ticket_service\presentation\controller\UserTicketController.java`)

### `GET /api/v1/tickets`
- **Méthode Java** : `getMyTickets`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<List<Ticket>>`

### `GET /api/v1/tickets/events/{eventId}`
- **Méthode Java** : `getMyTicketsForEvent`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: eventId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<List<TicketResponse>>`

### `POST /api/v1/tickets/hold`
- **Méthode Java** : `createHold`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `CreateHoldRequest` (Validé: True)
- **Response DTO** : `ResponseEntity<HoldResponse>`

### `DELETE /api/v1/tickets/hold/{holdId}`
- **Méthode Java** : `releaseHold`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: holdId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<Void>`

### `GET /api/v1/tickets/my-orders`
- **Méthode Java** : `getMyOrders`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<List<OrderResponse>>`

### `GET /api/v1/tickets/my-tickets`
- **Méthode Java** : `getMyTickets`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<List<TicketSummary>>`

### `GET /api/v1/tickets/orders`
- **Méthode Java** : `getMyOrders`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<List<TicketOrder>>`

### `POST /api/v1/tickets/orders`
- **Méthode Java** : `createOrder`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `CreateOrderRequest` (Validé: True)
- **Response DTO** : `ResponseEntity<OrderResponse>`

### `POST /api/v1/tickets/orders`
- **Méthode Java** : `createOrder`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `CreateTicketOrderRequest` (Validé: True)
- **Response DTO** : `ResponseEntity<TicketOrderResponse>`

### `GET /api/v1/tickets/orders/{orderId}`
- **Méthode Java** : `getOrder`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: orderId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<OrderResponse>`

### `GET /api/v1/tickets/orders/{orderId}`
- **Méthode Java** : `getOrder`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: orderId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<TicketOrder>`

### `GET /api/v1/tickets/{ticketId}`
- **Méthode Java** : `getTicket`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: ticketId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<TicketDetailResponse>`

### `GET /api/v1/tickets/{ticketId}/qr`
- **Méthode Java** : `getTicketQr`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: ticketId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<TicketQrResponse>`

### `GET /api/v1/tickets/{ticketId}/qr`
- **Méthode Java** : `getTicketQr`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: ticketId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<QrCodeResponse>`


# SERVICE : `user-service`


## Contrôleur : `SocialGraphController` (`user-service\src\main\java\com\yeyamo_mobile\api\user_service\interfaces\rest\SocialGraphController.java`)

### `GET /api/v1/users/social/activity`
- **Méthode Java** : `getNetworkActivity`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<NetworkActivityResponse>`

### `GET /api/v1/users/social/blocked`
- **Méthode Java** : `getBlockedUsers`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<UserProfileSummaryResponse>`

### `GET /api/v1/users/social/followers`
- **Méthode Java** : `getFollowers`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<UserProfileSummaryResponse>`

### `DELETE /api/v1/users/social/followers/{userId}`
- **Méthode Java** : `removeFollower`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: userId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `GET /api/v1/users/social/following`
- **Méthode Java** : `getFollowing`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<UserProfileSummaryResponse>`

### `GET /api/v1/users/social/search`
- **Méthode Java** : `searchUsers`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : QueryParams: query
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<UserProfileSummaryResponse>`

### `GET /api/v1/users/social/settings`
- **Méthode Java** : `getSettings`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `SocialSettingsResponse`

### `PUT /api/v1/users/social/settings`
- **Méthode Java** : `updateSettings`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `SocialSettingsRequest` (Validé: True)
- **Response DTO** : `SocialSettingsResponse`

### `GET /api/v1/users/social/stats`
- **Méthode Java** : `getMyStats`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `SocialStatsResponse`

### `GET /api/v1/users/social/suggestions`
- **Méthode Java** : `getSuggestions`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `List<UserProfileSummaryResponse>`

### `DELETE /api/v1/users/social/{userId}/block`
- **Méthode Java** : `unblock`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: userId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `POST /api/v1/users/social/{userId}/block`
- **Méthode Java** : `block`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: userId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `DELETE /api/v1/users/social/{userId}/follow`
- **Méthode Java** : `unfollow`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: userId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `POST /api/v1/users/social/{userId}/follow`
- **Méthode Java** : `follow`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: userId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `void`

### `GET /api/v1/users/social/{userId}/followers`
- **Méthode Java** : `getUserFollowers`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: userId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<UserProfileSummaryResponse>`

### `GET /api/v1/users/social/{userId}/following`
- **Méthode Java** : `getUserFollowing`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: userId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<UserProfileSummaryResponse>`

### `GET /api/v1/users/social/{userId}/stats`
- **Méthode Java** : `getUserStats`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: userId
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `SocialStatsResponse`


## Contrôleur : `UserProfileController` (`user-service\src\main\java\com\yeyamo_mobile\api\user_service\interfaces\rest\UserProfileController.java`)

### `GET /api/v1/users`
- **Méthode Java** : `search`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `Page<PublicProfileResponse>`

### `DELETE /api/v1/users/me`
- **Méthode Java** : `delete`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `ResponseEntity<Void>`

### `GET /api/v1/users/me`
- **Méthode Java** : `me`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `MyProfileResponse`

### `PUT /api/v1/users/me`
- **Méthode Java** : `update`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `UpdateProfileRequest` (Validé: True)
- **Response DTO** : `MyProfileResponse`

### `PATCH /api/v1/users/me/discovery-preferences`
- **Méthode Java** : `updateDiscoveryPreferences`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `UpdateDiscoveryPreferencesRequest` (Validé: True)
- **Response DTO** : `MyProfileResponse`

### `PATCH /api/v1/users/me/language`
- **Méthode Java** : `updateLanguage`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `UpdateLanguageRequest` (Validé: True)
- **Response DTO** : `MyProfileResponse`

### `PATCH /api/v1/users/me/location`
- **Méthode Java** : `updateLocation`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `UpdateLocationRequest` (Validé: True)
- **Response DTO** : `MyProfileResponse`

### `PATCH /api/v1/users/me/preferences`
- **Méthode Java** : `preferences`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : Aucun
- **Request DTO** : `UpdatePreferencesRequest` (Validé: True)
- **Response DTO** : `MyProfileResponse`

### `GET /api/v1/users/{id}`
- **Méthode Java** : `byId`
- **Niveau Auth** : `AUTHENTICATED`
- **Rôles / Permissions** : `Bearer JWT`
- **Paramètres** : PathVars: id
- **Request DTO** : `None` (Validé: False)
- **Response DTO** : `PublicProfileResponse`
