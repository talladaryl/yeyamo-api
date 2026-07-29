# Contrat des APIs d'administration YeYamo

Version : 1.0  
Date : 29 juillet 2026  
Point d'entrée : API Gateway, `http://localhost:8083` en développement.

Ce document est le contrat de référence entre `yeyamo-admin` et le backend. Les documents OpenAPI exécutables restent disponibles par service sur `/v3/api-docs` et `/swagger-ui.html`. Lorsqu'un écart existe entre une ancienne implémentation frontend et ce document, la route et le schéma décrits ici sont autoritatifs.

## Conventions communes

### Authentification

Toutes les routes admin exigent `Authorization: Bearer <accessToken YeYamo>`. Le JWT contient :

- `sub` : identifiant de l'acteur ;
- `roles` : `SUPER_ADMIN`, `ADMIN`, `MODERATOR`, `EDITOR`, `SUPPORT`, `COMMERCIAL` ;
- `scope` et `scopes` : scopes OAuth applicatifs ;
- `permissions` : permissions fines.

L'identité de l'acteur n'est jamais acceptée depuis le body. Elle provient du JWT.

### Corrélation

Le client peut envoyer `X-Correlation-Id`. Le Gateway le valide ou le génère, puis le propage. Les réponses et erreurs doivent retourner le même identifiant.

### Idempotence

`Idempotency-Key` est obligatoire pour :

- annulation ou complétion de réservation ;
- remboursement ;
- ajustement ou mouvement de ledger ;
- création financière sensible ;
- soumission d'import.

Une même clé et un même payload retournent le résultat initial. Une même clé avec un payload différent retourne `409 IDEMPOTENCY_CONFLICT`.

### Pagination Spring actuellement utilisée

Les endpoints paginés retournent le contrat Spring Data :

```json
{
  "content": [],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {"sorted": true, "unsorted": false, "empty": false},
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 0,
  "totalPages": 0,
  "last": true,
  "size": 20,
  "number": 0,
  "sort": {"sorted": true, "unsorted": false, "empty": false},
  "numberOfElements": 0,
  "first": true,
  "empty": true
}
```

Paramètres communs :

- `page`, base zéro ;
- `size`, généralement limité à 200 ;
- `sort=field,asc|desc`, répétable.

Le frontend ne doit pas attendre des champs racine `page`; le numéro courant est `number`.

### Erreurs

Contrat cible commun :

```json
{
  "timestamp": "2026-07-29T18:00:00Z",
  "status": 409,
  "code": "INVALID_STATE",
  "message": "Transition impossible",
  "correlationId": "8f6b...",
  "errors": []
}
```

| HTTP | Code | Signification |
|---|---|---|
| 400 | `VALIDATION_ERROR` | body, query ou header invalide |
| 400 | `INVALID_ARGUMENT` | valeur métier invalide |
| 401 | `UNAUTHORIZED` | token absent, expiré ou invalide |
| 403 | `FORBIDDEN` | rôle, permission ou scope insuffisant |
| 404 | `NOT_FOUND` | ressource inexistante |
| 409 | `CONFLICT` | unicité ou concurrence |
| 409 | `INVALID_STATE` | transition métier impossible |
| 409 | `IDEMPOTENCY_CONFLICT` | clé réutilisée avec une autre commande |
| 422 | `BUSINESS_RULE_VIOLATION` | règle métier non satisfaite |
| 429 | `RATE_LIMITED` | limite de débit dépassée |
| 503 | `DEPENDENCY_UNAVAILABLE` | dépendance externe indisponible |

Certains anciens handlers retournent encore RFC 9457 `ProblemDetail`. Cette différence est recensée dans l'audit backend et doit être gérée par `ApiError` jusqu'à convergence.

## Authentification et sessions

| Méthode | Endpoint | Résumé | Request | Response | Autorisation |
|---|---|---|---|---|---|
| POST | `/api/v1/auth/login` | Authentifier un administrateur | `LoginRequest` | tokens YeYamo + utilisateur | public, rate limit |
| POST | `/api/v1/auth/logout` | Révoquer la session courante | refresh/session selon contrat auth | vide | authentifié |
| POST | `/api/v1/auth/refresh` | Rotation du refresh token | `RefreshTokenRequest` | nouveaux tokens | refresh valide |
| GET | `/api/v1/auth/me` | Session courante | — | profil auth et authorities | authentifié |
| GET | `/api/v1/auth/sessions` | Sessions du compte courant | — | `AdminUserSessionResponse[]` | authentifié |
| DELETE | `/api/v1/auth/sessions/{sessionId}` | Révoquer une session | path `sessionId` | `204` | propriétaire |

Erreurs spécifiques : `INVALID_CREDENTIALS`, `ACCOUNT_DISABLED`, `TOKEN_EXPIRED`, `REFRESH_TOKEN_REUSED`.

## Utilisateurs plateforme

Service : `auth-service`. Base : PostgreSQL `yeyamo_auth`.

| Méthode | Endpoint | Params/body | Response | Permissions |
|---|---|---|---|---|
| GET | `/api/v1/admin/platform-users` | `search,email,phone,status,role,regionId,createdFrom,createdTo,lastLoginFrom,lastLoginTo,page,size,sort` | `Page<AdminPlatformUserSummary>` | ADMIN, SUPER_ADMIN, SUPPORT |
| GET | `/api/v1/admin/platform-users/{id}` | path `id` | `AdminPlatformUserDetail` | ADMIN, SUPER_ADMIN, SUPPORT |
| PATCH | `/api/v1/admin/platform-users/{id}/status` | `{status,reason}` | `AdminPlatformUserDetail` | ADMIN, SUPER_ADMIN |
| PATCH | `/api/v1/admin/platform-users/{id}/roles` | `{roles,reason}` | `AdminPlatformUserDetail` | SUPER_ADMIN |
| GET | `/api/v1/admin/platform-users/{id}/sessions` | path `id` | `AdminUserSessionResponse[]` sans token | ADMIN, SUPER_ADMIN, SUPPORT |
| POST | `/api/v1/admin/platform-users/{id}/sessions/revoke` | `{sessionId?,reason}` | `204` | ADMIN, SUPER_ADMIN |
| GET | `/api/v1/admin/platform-users/export` | filtres `search,status,role,regionId` | flux `text/csv` | ADMIN, SUPER_ADMIN |

Statuts supportés : `ACTIVE`, `SUSPENDED`, `BLOCKED`, `DEACTIVATED` et statuts historiques du modèle auth. Rôles plateforme limités par `Roles`; l'élévation `SUPER_ADMIN` est refusée par le service utilisateur plateforme.

## Comptes administrateurs

Service : `admin-service`. Base : PostgreSQL `yeyamo_admin`.

| Méthode | Endpoint | Request | Response | Permissions |
|---|---|---|---|---|
| GET | `/api/v1/admin/users` | — | `AdminUserResponse[]` | ADMIN, SUPER_ADMIN |
| GET | `/api/v1/admin/users/{id}` | — | `AdminUserResponse` | ADMIN, SUPER_ADMIN |
| POST | `/api/v1/admin/users` | `CreateAdminUserRequest` | `AdminUserResponse` | SUPER_ADMIN |
| PUT | `/api/v1/admin/users/{id}` | `UpdateAdminUserRequest` | `AdminUserResponse` | SUPER_ADMIN |
| GET | `/api/v1/admin/audit-logs` | `actorId,action,targetType,targetId,service,correlationId,createdFrom,createdTo,page,size,sort` | page d'audit | ADMIN, SUPER_ADMIN |

Protections : email unique, permissions connues, pas de désactivation du dernier `SUPER_ADMIN`, pas d'auto-escalade.

## Partenaires et KYC

Service métier : `partner-service`. Les validations historiques restent dans `admin-service`.

| Méthode | Endpoint | Params/body | Response | Permissions |
|---|---|---|---|---|
| GET | `/api/v1/admin/partners` | `search,status,createdFrom,createdTo,page,size,sort` | `Page<AdminPartnerResponse.Summary>` | SUPPORT, ADMIN, SUPER_ADMIN |
| GET | `/api/v1/admin/partners/{id}` | — | `AdminPartnerResponse.Detail` | SUPPORT, ADMIN, SUPER_ADMIN |
| GET | `/api/v1/admin/partners/{id}/kyc` | — | `Document[]` | ADMIN, SUPER_ADMIN |
| GET | `/api/v1/admin/partners/{id}/validation-history` | — | `History[]` | ADMIN, SUPER_ADMIN |
| GET | `/api/v1/admin/partners/{id}/establishments` | — | page JSON provenant de Place | SUPPORT, ADMIN, SUPER_ADMIN |
| GET | `/api/v1/admin/partners/{partnerId}/kyc/documents/{documentId}` | — | stream sécurisé, `Cache-Control: no-store` | ADMIN, SUPER_ADMIN |
| GET | `/api/v1/admin/validations/partners` | `status` | validations | ADMIN, SUPER_ADMIN |
| PATCH | `/api/v1/admin/validations/partners/{id}/review` | `{decision,reason,comment}` | validation | ADMIN, SUPER_ADMIN |

Décisions : `APPROVED`, `REJECTED`, `CORRECTIONS_REQUIRED`. Aucun stockage d'URL KYC publique permanente.

## Lieux et référentiels

Service : `place-service`, PostgreSQL/PostGIS.

| Méthode | Endpoint | Request/params | Response | Permissions |
|---|---|---|---|---|
| GET | `/api/v1/admin/places` | filtres `search,status,categoryId,regionId,cityId,districtId,partnerId,verified,page,size,sort` | page de lieux admin | SUPPORT, EDITOR, ADMIN, SUPER_ADMIN |
| GET | `/api/v1/admin/places/{id}` | — | `AdminPlaceResponse` | idem |
| POST | `/api/v1/admin/places` | DTO lieu | `201 AdminPlaceResponse` | EDITOR, ADMIN, SUPER_ADMIN |
| PUT | `/api/v1/admin/places/{id}` | DTO lieu | `AdminPlaceResponse` | EDITOR, ADMIN, SUPER_ADMIN |
| PATCH | `/api/v1/admin/places/{id}/status` | `{status,reason}` | `AdminPlaceResponse` | ADMIN, SUPER_ADMIN |
| GET | `/api/v1/admin/validations/places` | `status` | validations | ADMIN, SUPER_ADMIN |
| PATCH | `/api/v1/admin/validations/places/{id}/review` | décision structurée | validation | ADMIN, SUPER_ADMIN |
| GET/POST | `/api/v1/regions` | — / `RegionRequest` | liste / ressource | lecture authentifiée, mutation admin |
| PUT | `/api/v1/regions/{id}` | `RegionRequest` | ressource | admin |
| GET/POST | `/api/v1/cities` | filtres / `CityRequest` | liste / ressource | lecture authentifiée, mutation admin |
| GET | `/api/v1/cities/region/{regionId}` | — | `CityResponse[]` | authentifié |
| PUT | `/api/v1/cities/{id}` | `CityRequest` | ressource | admin |
| GET/POST | `/api/v1/districts` | filtres / `DistrictRequest` | liste / ressource | lecture authentifiée, mutation admin |
| GET | `/api/v1/districts/city/{cityId}` | — | `DistrictResponse[]` | authentifié |
| PUT | `/api/v1/districts/{id}` | `DistrictRequest` | ressource | admin |
| GET/POST | `/api/v1/categories` | — / `CategoryRequest` | liste / ressource | lecture authentifiée, mutation admin |
| PUT | `/api/v1/categories/{id}` | `CategoryRequest` | ressource | admin |

Latitude : `[-90,90]`. Longitude : `[-180,180]`. Région, ville et district doivent appartenir à la même hiérarchie.

## Catalogue, collections et ingestion

Services : `catalog-service` et `ingestion-service`.

| Méthode | Endpoint | Request/params | Response | Permissions |
|---|---|---|---|---|
| GET | `/api/v1/catalog/assets` | `search/q,type,status,regionId/regionCode,categoryId/categoryCode,source,createdFrom,createdTo,page,size,sort` | liste/page selon endpoint public/manage | authentifié |
| GET | `/api/v1/catalog/assets/{id}` | — | asset publié | authentifié |
| GET | `/api/v1/catalog/assets/manage/{id}` | — | asset administrable | EDITOR, ADMIN |
| POST | `/api/v1/catalog/assets` | asset DTO | asset | EDITOR, ADMIN |
| PUT | `/api/v1/catalog/assets/{id}` | asset DTO | asset | EDITOR, ADMIN |
| PATCH | `/api/v1/catalog/assets/{id}/status` | `{status}` | asset | EDITOR, ADMIN |
| DELETE | `/api/v1/catalog/assets/{id}` | — | `204` | ADMIN |
| GET | `/api/v1/collections` | `page,size,sort` | page de collections | authentifié |
| GET | `/api/v1/collections/{id}` | — | collection ordonnée | authentifié |
| POST | `/api/v1/collections` | collection DTO | collection | EDITOR, ADMIN |
| PUT | `/api/v1/collections/{id}` | collection DTO | collection | EDITOR, ADMIN |
| DELETE | `/api/v1/collections/{id}` | — | archive/suppression sûre | ADMIN |
| POST | `/api/v1/catalog/imports` | `Idempotency-Key`, `ImportRequest` | `202 ImportJobResponse` | EDITOR, ADMIN, SUPER_ADMIN |
| GET | `/api/v1/catalog/imports` | `status,source,createdFrom,createdTo,page,size,sort` | `Page<ImportJobResponse>` | EDITOR, ADMIN, SUPER_ADMIN |
| GET | `/api/v1/catalog/imports/{id}` | — | `ImportJobResponse` | authentifié |
| POST | `/api/v1/catalog/imports/{id}/retry` | — | job | EDITOR, ADMIN, SUPER_ADMIN |
| POST | `/api/v1/catalog/imports/{id}/cancel` | — | job | EDITOR, ADMIN, SUPER_ADMIN |
| GET | `/api/v1/catalog/imports/{id}/errors` | — | `{rowNumber,field,code,message}[]` | EDITOR, ADMIN, SUPER_ADMIN |

États import : `PENDING`, `PROCESSING`, `COMPLETED`, `PARTIAL`, `FAILED`, `CANCELLED`.

## Événements et billetterie

Services : `event-service`, `ticket-service`.

| Méthode | Endpoint | Request/params | Response | Permissions |
|---|---|---|---|---|
| GET | `/api/v1/admin/events` | filtres complets + pagination | `Page<AdminEventResponse>` | EDITOR, ADMIN, SUPER_ADMIN |
| GET | `/api/v1/admin/events/{id}` | — | `AdminEventResponse` | idem |
| POST | `/api/v1/admin/events` | `AdminEventRequest` | `201 AdminEventResponse` | idem |
| PUT | `/api/v1/admin/events/{id}` | `AdminEventRequest` | response | idem |
| PATCH | `/api/v1/admin/events/{id}/status?status=...` | `{reason}` | response | idem |
| POST | `/api/v1/admin/events/{id}/publish` | `{reason}` | response | idem |
| POST | `/api/v1/admin/events/{id}/suspend` | `{reason}` | response | idem |
| POST | `/api/v1/admin/events/{id}/reject` | `{reason}` | response | idem |
| POST | `/api/v1/admin/events/{id}/archive` | `{reason}` | response | idem |
| GET | `/api/v1/admin/events/{eventId}/tickets` | — | ticket types | ADMIN, EVENT_SCANNER autorisé |
| GET | `/api/v1/admin/events/{eventId}/participants` | — | participants | ADMIN, EVENT_SCANNER autorisé |
| GET | `/api/v1/admin/events/{eventId}/ticketing-stats` | — | statistiques réelles | ADMIN |
| POST | `/api/v1/tickets/scans` | QR/token opaque + event | résultat scan | scanner autorisé |

États backend : `DRAFT`, `PENDING_REVIEW`, `PUBLISHED`, `SUSPENDED`, `REJECTED`, `ARCHIVED`, `CANCELLED` selon transition réelle.

## Réservations

Service : `booking-service`.

| Méthode | Endpoint | Request/params | Response | Permissions |
|---|---|---|---|---|
| GET | `/api/v1/booking-management/bookings` | filtres définis dans `AdminBookingController`, pagination | `Page<AdminBookingSummary>` | ADMIN, SUPER_ADMIN, SUPPORT |
| GET | `/api/v1/booking-management/bookings/stats` | `from,to` | `AdminBookingStats` | idem |
| GET | `/api/v1/booking-management/bookings/{id}` | — | `AdminBookingDetail` | idem |
| POST | `/api/v1/booking-management/bookings/{id}/cancel` | `Idempotency-Key`, `{reason}` | `BookingView` | ADMIN, SUPER_ADMIN |
| POST | `/api/v1/booking-management/bookings/{id}/complete` | `Idempotency-Key` | `BookingView` | ADMIN, SUPER_ADMIN |
| GET | `/api/v1/bookings/{id}/history` | — | historique immuable | propriétaire ou rôle privilégié |

## Paiements

Service : `payment-service`.

| Méthode | Endpoint | Request/params | Response | Permissions |
|---|---|---|---|---|
| GET | `/api/v1/payments/admin` | filtres complets + pagination | `Page<AdminPaymentSummary>` | ADMIN, SUPER_ADMIN, SUPPORT |
| GET | `/api/v1/payments/admin/{id}` | — | `AdminPaymentDetail` | idem |
| GET | `/api/v1/payments/admin/anomalies` | — | `PaymentAnomaly[]` | ADMIN, SUPER_ADMIN |
| POST | `/api/v1/payments/admin/reconciliation` | — | `{jobId,status}` | SUPER_ADMIN |
| GET | `/api/v1/payments/{id}/refunds` | — | `RefundView[]` | autorisé |
| POST | `/api/v1/payments/{id}/refunds` | `Idempotency-Key`, `{amount,reason}` | refund | ADMIN, SUPER_ADMIN |

Montants : nombres décimaux sérialisés depuis `BigDecimal`; devise ISO 4217. Aucun secret provider n'est retourné.

## Commerce

Service : `commerce-service`.

| Méthode | Endpoint | Request | Response | Permissions |
|---|---|---|---|---|
| GET/POST | `/api/v1/commerce/admin/promotions` | filtres / promotion | page / promotion | ADMIN, SUPER_ADMIN, COMMERCIAL |
| GET/PUT | `/api/v1/commerce/admin/promotions/{id}` | — / promotion | promotion | idem |
| POST | `/api/v1/commerce/admin/promotions/{id}/disable` | — | promotion | idem |
| GET/POST | `/api/v1/commerce/admin/commissions` | filtres / règle | page / commission | ADMIN, SUPER_ADMIN |
| GET | `/api/v1/commerce/admin/commissions/{id}` | — | commission | ADMIN, SUPER_ADMIN |
| GET | `/api/v1/commerce/admin/ledger/{partnerId}` | pagination | écritures append-only | ADMIN, SUPER_ADMIN, SUPPORT |
| GET | `/api/v1/commerce/admin/ledger/{partnerId}/balance/{currency}` | — | balance | idem |
| POST | `/api/v1/commerce/admin/ledger/{partnerId}/adjustments` | `Idempotency-Key`, montant/devise/raison | écriture compensatoire | SUPER_ADMIN |
| POST | `/api/v1/commerce/admin/ledger/{partnerId}/movements` | `Idempotency-Key`, type/montant/devise/raison | écriture | SUPER_ADMIN |

## Modération et Trust

Services : `moderation-trust-service`, `interaction-service`.

| Méthode | Endpoint | Request/params | Response | Permissions |
|---|---|---|---|---|
| GET | `/api/v1/moderation/reports` | filtres + pagination | `Page<ReportResponse>` | MODERATOR, ADMIN, SUPER_ADMIN |
| GET | `/api/v1/moderation/reports/{id}` | — | report | idem |
| POST | `/api/v1/moderation/reports/{id}/review` | — | report | idem |
| POST | `/api/v1/moderation/reports/{id}/assign` | `{assigneeId}` | report | idem |
| POST | `/api/v1/moderation/reports/{id}/decision` | `{decision,reason,sanction?}` | report | idem |
| GET | `/api/v1/moderation/audit` | filtres + pagination | page d'audit | MODERATOR, ADMIN, SUPER_ADMIN |
| GET | `/api/v1/trust` | pagination | page de scores | MODERATOR, ADMIN, SUPER_ADMIN |
| GET | `/api/v1/trust/{subjectId}` | — | score | sujet ou rôle privilégié |
| GET | `/api/v1/trust/{subjectId}/history` | pagination | sanctions | rôle privilégié |
| POST | `/api/v1/trust/{subjectId}/sanctions` | sanction | sanction créée | ADMIN, SUPER_ADMIN |
| GET | `/api/v1/admin/reviews` | filtres + pagination | page de reviews | MODERATOR, ADMIN |
| GET | `/api/v1/admin/reviews/{id}` | — | détail | idem |
| PATCH | `/api/v1/admin/reviews/{id}/hide` | raison | review | idem |
| PATCH | `/api/v1/admin/reviews/{id}/restore` | raison | review | idem |
| DELETE | `/api/v1/admin/reviews/{id}` | raison | soft delete | ADMIN |
| GET/PATCH/DELETE | `/api/v1/admin/comments...` | mêmes conventions | commentaire admin | MODERATOR, ADMIN |

## Gamification

Services : `mission-reward-service`, `gamification-service`.

| Méthode | Endpoint | Response/action | Permissions |
|---|---|---|---|
| GET/POST | `/api/v1/missions`, `/api/v1/mission-management/missions` | liste/création mission | ADMIN, SUPER_ADMIN |
| GET/PUT/DELETE | `/api/v1/mission-management/missions/{id}` | détail, mise à jour, archive | ADMIN, SUPER_ADMIN |
| POST | `/api/v1/mission-management/missions/{id}/activate` | activation | ADMIN, SUPER_ADMIN |
| POST | `/api/v1/mission-management/missions/{id}/pause` | pause | ADMIN, SUPER_ADMIN |
| CRUD/status | `/api/v1/mission-management/badges` | badges | ADMIN, SUPER_ADMIN |
| CRUD/status | `/api/v1/mission-management/xp-rules` | règles XP | ADMIN, SUPER_ADMIN |
| CRUD/status | `/api/v1/mission-management/rewards` | récompenses | ADMIN, SUPER_ADMIN |
| GET | `/api/v1/mission-management/fraud-alerts` | page filtrée | ADMIN, SUPER_ADMIN |
| GET | `/api/v1/admin/gamification/xp-ledger` | `userId?,page,size,sort` → page append-only | ADMIN, SUPER_ADMIN |

## Campagnes

Service : `campaign-service`. Route Gateway prioritaire.

| Méthode | Endpoint | Request/params | Response | Scope |
|---|---|---|---|---|
| GET | `/api/v1/admin/campaigns` | filtres + pagination | page campagnes | `campaign:approve` ou `campaign:reject` |
| GET | `/api/v1/admin/campaigns/{id}` | — | détail | idem |
| POST | `/api/v1/admin/campaigns/{id}/approve` | — | campagne | `campaign:approve` |
| POST | `/api/v1/admin/campaigns/{id}/reject` | `{rejectionReason}` | campagne | `campaign:reject` |

## Analytics

Service : `analytics-service`.

| Méthode | Endpoint | Params | Response | Permissions |
|---|---|---|---|---|
| GET | `/api/v1/analytics/admin/dashboard` | `from,to,timezone` selon support | dashboard | ADMIN, SUPER_ADMIN |
| GET | `/api/v1/analytics/kpis` | période | KPIs | ADMIN, SUPER_ADMIN |
| GET | `/api/v1/analytics/kpis/{kpiName}` | `from,to,granularity` | historique | ADMIN, SUPER_ADMIN |
| GET | `/api/v1/analytics/event-logs` | filtres + pagination | page event logs | ADMIN, SUPER_ADMIN |
| GET | `/api/v1/analytics/regions/{regionId}/activity` | période | activité région | authentifié |
| GET | `/api/v1/analytics/partners/{partnerId}/dashboard` | période | dashboard partenaire | propriétaire ou admin |
| GET | `/api/v1/analytics/places/popular` | période/limit | lieux populaires | authentifié |
| GET | `/api/v1/analytics/places/{placeId}/popularity` | période | popularité | authentifié |
| GET | `/api/v1/analytics/users/{userId}/engagement` | période | engagement | utilisateur ou admin |
| POST | `/api/v1/analytics/business/admin/rebuild` | — | `{jobId,status}` | ADMIN, SUPER_ADMIN |
| GET | `/api/v1/analytics/business/admin/rebuild/{jobId}` | — | statut job | ADMIN, SUPER_ADMIN |

## Support

Service : `support-service`.

| Méthode | Endpoint | Request/params | Response | Permissions |
|---|---|---|---|---|
| GET | `/api/v1/admin/support/conversations` | filtres + pagination | page conversations avec SLA | SUPPORT, ADMIN, SUPER_ADMIN |
| GET | `/api/v1/admin/support/conversations/{id}` | — | détail, messages, notes internes | idem |
| POST | `/api/v1/admin/support/conversations/{id}/messages` | `{content,attachmentMediaIds}` | message | idem |
| POST | `/api/v1/admin/support/conversations/{id}/notes` | `{content}` | note interne | idem |
| PATCH | `/api/v1/admin/support/conversations/{id}/assign` | `{assigneeId}` | conversation | idem |
| PATCH | `/api/v1/admin/support/conversations/{id}/status` | `{status}` | conversation | idem |
| PATCH | `/api/v1/admin/support/conversations/{id}/priority` | `{priority}` | conversation | idem |

## Newsletter

Service : `notification-service`.

| Méthode | Endpoint | Request | Response | Permissions |
|---|---|---|---|---|
| GET/POST | `/api/v1/admin/newsletters` | filtres / `CampaignRequest` | page / campagne | COMMERCIAL, ADMIN, SUPER_ADMIN |
| GET/PUT | `/api/v1/admin/newsletters/{id}` | — / `CampaignRequest` | campagne | idem |
| POST | `/api/v1/admin/newsletters/{id}/schedule` | `{scheduledAt}` | campagne | idem |
| POST | `/api/v1/admin/newsletters/{id}/send` | — | campagne | idem |
| POST | `/api/v1/admin/newsletters/{id}/pause` | — | campagne | idem |
| POST | `/api/v1/admin/newsletters/{id}/cancel` | — | campagne | idem |
| GET | `/api/v1/admin/newsletters/{id}/stats` | — | delivered/failed/opened/clicked/unsubscribed | idem |

La résolution réelle d'audience et l'opt-out dépendent des projections utilisateurs/partenaires. Aucun envoi ne doit ignorer les préférences marketing.

## Search & Discovery

Service : `discovery-service`, moteur OpenSearch.

| Méthode | Endpoint | Request | Response | Permissions |
|---|---|---|---|---|
| GET | `/api/v1/admin/search/overview` | — | santé, compteurs, latence | ADMIN, SUPER_ADMIN |
| GET | `/api/v1/admin/search/indexes` | — | index | ADMIN, SUPER_ADMIN |
| POST | `/api/v1/admin/search/reindex` | — | `202 {jobId,status}` | ADMIN, SUPER_ADMIN |
| GET/POST | `/api/v1/admin/search/synonyms` | — / groupe | liste / groupe | lecture admin, mutation SUPER_ADMIN |
| PUT/DELETE | `/api/v1/admin/search/synonyms/{id}` | groupe / — | groupe / `204` | SUPER_ADMIN |
| GET/PUT | `/api/v1/admin/search/ranking` | — / config versionnée | config | lecture admin, mutation SUPER_ADMIN |
| GET | `/api/v1/admin/search/zero-results` | pagination | page anonymisée | ADMIN, SUPER_ADMIN |

## Gouvernance

Service : `admin-service`.

| Méthode | Endpoint | Request | Response | Permissions |
|---|---|---|---|---|
| GET | `/api/v1/admin/settings` | — | allowlist settings | SUPER_ADMIN |
| PUT | `/api/v1/admin/settings/{key}` | `{value,type,reason}` | setting versionné | SUPER_ADMIN |
| GET | `/api/v1/admin/settings/history` | — | historique | SUPER_ADMIN |
| POST | `/api/v1/admin/settings/{key}/rollback/{version}` | query `reason` | nouvelle version | SUPER_ADMIN |
| GET | `/api/v1/admin/feature-flags` | — | flags | SUPER_ADMIN |
| GET | `/api/v1/admin/feature-flags/{key}` | — | flag | SUPER_ADMIN |
| POST | `/api/v1/admin/feature-flags` | flag | flag | SUPER_ADMIN |
| PUT | `/api/v1/admin/feature-flags/{key}` | flag | flag versionné | SUPER_ADMIN |
| GET | `/api/v1/admin/feature-flags/{key}/history` | — | historique | SUPER_ADMIN |
| POST | `/api/v1/admin/feature-flags/{key}/rollback/{version}` | query `reason` | nouvelle version | SUPER_ADMIN |

Les clés contenant ou désignant un secret sont refusées.

## Notifications administratives

Service : `notification-service`.

| Méthode | Endpoint | Request | Response | Permissions |
|---|---|---|---|---|
| GET | `/api/v1/admin/notifications` | `type,page,size,sort` | page | rôle admin ciblé |
| GET | `/api/v1/admin/notifications/unread-count` | — | `{count}` | rôle admin ciblé |
| PATCH | `/api/v1/admin/notifications/{id}/read` | — | notification | destinataire/rôle |
| PATCH | `/api/v1/admin/notifications/{id}/unread` | — | notification | destinataire/rôle |
| POST | `/api/v1/admin/notifications/read-all` | — | `{updated}` | destinataire/rôle |

Le backend retourne `resourceType` et `resourceId`, jamais une URL frontend arbitraire.

## Matrice frontend

| Page frontend | Service | Endpoint principal | Permissions | État |
|---|---|---|---|---|
| `/admin/users` | auth-service | `/api/v1/admin/platform-users` | ADMIN/SUPPORT | CONNECTÉ |
| `/admin/administrators` | admin-service | `/api/v1/admin/users` | ADMIN | CONNECTÉ |
| `/admin/partners` | partner-service | `/api/v1/admin/partners` | SUPPORT/ADMIN | FRONTEND À ALIGNER |
| `/admin/places` | place-service | `/api/v1/admin/places` | EDITOR/ADMIN | PARTIELLEMENT ALIGNÉ |
| `/admin/catalog` | catalog-service | `/api/v1/catalog/assets` | EDITOR/ADMIN | CONNECTÉ |
| `/admin/catalog/imports` | ingestion-service | `/api/v1/catalog/imports` | EDITOR/ADMIN | PARTIEL |
| `/admin/events` | event-service | `/api/v1/admin/events` | EDITOR/ADMIN | FRONTEND À ALIGNER |
| `/admin/reservations` | booking-service | `/api/v1/booking-management/bookings` | SUPPORT/ADMIN | LISTE CONNECTÉE |
| `/admin/payments` | payment-service | `/api/v1/payments/admin` | SUPPORT/ADMIN | LISTE CONNECTÉE |
| `/admin/moderation` | moderation-trust-service | `/api/v1/moderation/reports` | MODERATOR | TYPE PAGE À ALIGNER |
| `/admin/gamification` | mission/gamification | `/api/v1/mission-management/*` | ADMIN | PARTIEL |
| `/admin/campaigns` | campaign-service | `/api/v1/admin/campaigns` | scopes campaign | CONNECTÉ |
| `/admin/analytics` | analytics-service | `/api/v1/analytics/*` | ADMIN | PARTIEL |
| `/admin/messages` | support-service | `/api/v1/admin/support/conversations` | SUPPORT | ADAPTER FRONTEND DÉSACTIVÉ |
| `/admin/newsletter` | notification-service | `/api/v1/admin/newsletters` | COMMERCIAL | ADAPTER FRONTEND DÉSACTIVÉ |
| `/admin/search-discovery` | discovery-service | `/api/v1/admin/search/*` | ADMIN | FRONTEND MARQUE À TORT INDISPONIBLE |
| `/admin/settings` | admin-service | `/api/v1/admin/settings` | SUPER_ADMIN | FRONTEND NON BRANCHÉ |
| `/admin/notifications` | notification-service | `/api/v1/admin/notifications` | rôles admin | FRONTEND UTILISE ROUTE UTILISATEUR |

## Divergences frontend vérifiées

1. Events utilise `/api/v1/events` au lieu de `/api/v1/admin/events`; les enums frontend omettent `DRAFT`, `PENDING_REVIEW`, `SUSPENDED`, `REJECTED`, `ARCHIVED`.
2. Partners utilise `/api/v1/partners` au lieu de `/api/v1/admin/partners`.
3. Notifications admin utilise `/api/v1/notifications` et POST read au lieu de `/api/v1/admin/notifications` et PATCH.
4. Support et Newsletter déclarent encore `*AdminAvailable=false` alors que les APIs existent.
5. Search affiche « API indisponible » alors que `/api/v1/admin/search/*` est routé.
6. Settings ne consomme que sessions/audit et ignore settings/feature flags.
7. Moderation attend `Report[]` avec `limit`; le backend retourne `Page<ReportResponse>` avec `page/size`.
8. Analytics `events()` attend `EventLog[]`; le backend est paginé.
9. Analytics frontend appelle `/api/v1/analytics/admin/rebuild`; la route réelle est `/api/v1/analytics/business/admin/rebuild`.
10. Payment detail frontend appelle `/api/v1/payments/{id}`; la vue admin enrichie est `/api/v1/payments/admin/{id}`.
11. Reservation detail/cancel utilise les routes propriétaire `/api/v1/bookings/{id}`; la console doit utiliser `/api/v1/booking-management/bookings/{id}` et son cancel admin.
12. Gamification admin lit badges/rewards via `/api/v1/me/**`; les définitions administrables sont sous `/api/v1/mission-management/**`.
13. Event status frontend envoie `{status}` alors que le backend exige `status` en query et `{reason}` dans le body.
14. Le type de page Events frontend utilise `number`, correct pour Spring; d'autres types internes utilisent parfois `page`, à normaliser.

