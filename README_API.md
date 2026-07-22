# Référence API - Plateforme YeYamo

> Audit statique du code effectué le **22 juillet 2026**. Les routes ci-dessous proviennent des contrôleurs Spring (`@RestController`) présents dans le dépôt, et non d'une liste d'API théorique.

Ce document recense les **217 endpoints REST effectivement implémentés**, répartis dans **23 services applicatifs**. Il précise aussi les règles d'accès, le routage via l'API Gateway et les modules qui ne publient actuellement aucun endpoint métier.

---

## Lecture rapide

- [Vue d'ensemble](#vue-densemble)
- [Authentification et conventions](#authentification-et-conventions)
- [Inventaire complet par service](#inventaire-complet-par-service)
- [Routage API Gateway et écarts](#routage-api-gateway-et-écarts)
- [Modules sans endpoint métier](#modules-sans-endpoint-métier)
- [Écarts avec le client mobile](#interfaces-mobiles-sans-api-backend)

## Vue d'ensemble

| Domaine | Service | Port par défaut | Endpoints |
|---|---|---:|---:|
| Authentification | `auth-service` | 8082 | 11 |
| Passerelle | `api-gateway` | 8083 | 1 endpoint de fallback |
| Lieux | `place-service` | 8084 | 17 |
| Événements | `event-service` | 8085 | 8 |
| Utilisateurs / graphe social | `user-service` | 8086 | 19 |
| Partenaires | `partner-service` | 8087 | 9 |
| Catalogue / collections | `catalog-service` | 8088 | 24 |
| Ingestion catalogue | `ingestion-service` | 8089 | 2 |
| Contenu | `content-service` | 8090 | 16 |
| Interactions | `interaction-service` | 8091 | 18 |
| Feed | `feed-service` | 8092 | 1 |
| Discovery | `discovery-service` | 8093 | 2 |
| Notifications | `notification-service` | 8094 | 5 |
| Recommandations | `recommendation-service` | 8095 | 1 |
| Administration | `admin-service` | 8096 | 16 |
| Analytics | `analytics-service` | 8097 | 9 |
| Missions | `mission-reward-service` | 8098 | 6 |
| Parrainage | `referral-service` | 8099 | 9 |
| Modération / confiance | `moderation-trust-service` | 8100 | 8 |
| Médias | `media-service` | 8101 | 5 |
| Réservations | `booking-service` | 8102 | 9 |
| Paiements | `payment-service` | 8103 | 5 |
| Messagerie | `messaging-service` | 8104 | 11 |
| Gamification | `gamification-service` | 8105 | 6 |
| **Total métier** | **23 services** |  | **217** |

Les ports viennent de `cloud-conf-yeyamo/*.properties`. `config-server` utilise le port 8080 et `registry-service` le port 8761, mais ils n'exposent pas de contrôleur métier.

## Authentification et conventions

- Le point d'entrée normal est l'API Gateway : `http://localhost:8083` en local.
- Sauf mention contraire, les routes nécessitent `Authorization: Bearer <JWT>`.
- Passages autorisés sans JWT au niveau Gateway : `/api/v1/auth/**`, le webhook de paiement, les lectures de lieux/régions/villes/quartiers/catégories/événements, ainsi que la liste et le détail UUID des utilisateurs et partenaires. Le service cible peut rester plus strict; c'est notamment le cas de `/auth/me` et `/auth/logout`.
- Les routes `/api/v1/admin/**` et `/api/v1/analytics/**` exigent au niveau Gateway un rôle `ADMIN`, `SUPER_ADMIN` ou `MODERATOR`; les services appliquent parfois une restriction plus forte.
- `Idempotency-Key` est obligatoire pour la création/annulation de réservation, la finalisation d'une réservation, les remboursements manuels et la soumission d'un import.
- `X-Correlation-Id` est accepté sur plusieurs commandes; la casse `X-Correlation-ID` est autorisée par la configuration CORS de la Gateway.
- La documentation OpenAPI, lorsqu'elle est activée par le service, est disponible directement sur `/v3/api-docs` et l'interface Swagger sur `/swagger-ui.html`.

### Règles de sécurité propres aux services

| Services / routes | Règle appliquée par le service cible |
|---|---|
| `auth-service` | Routes de connexion, inscription, OAuth, refresh, vérification et mot de passe publiques; `/me` et `/logout` authentifiées |
| `place-service` | Toutes les lectures publiques; écritures réservées à `ADMIN`, `SUPER_ADMIN` ou `PARTNER` |
| `event-service` | Lectures publiques; inscription/désinscription authentifiées; autres écritures réservées à `ADMIN`, `SUPER_ADMIN` ou `PARTNER` |
| `user-service` | Liste/recherche et détail UUID publics; profil personnel et graphe social authentifiés |
| `partner-service` | Liste et détail UUID publics; gestion du profil et des documents authentifiée |
| `catalog-service` | Lectures catalogue publiques sauf `/assets/manage/**`; écritures catalogue réservées à `ADMIN`, `SUPER_ADMIN` ou `PARTNER`; collections authentifiées |
| `content-service` | Détail public d'un post UUID et lectures par hashtag/catalogue publiques; autres posts et toutes les stories authentifiés |
| `interaction-service` | Lectures sous `/interactions/posts/**` publiques; mutations, avis, check-ins et sauvegardes authentifiés |
| `media-service` | Lectures publiques; upload et suppression authentifiés |
| `booking-service` | Disponibilité et réservations authentifiées; management réservé à `ADMIN` ou `PARTNER` |
| `payment-service` | Webhooks publics; lectures authentifiées; remboursement manuel contrôlé `ADMIN` dans le contrôleur |
| `ingestion-service` | `ADMIN` ou `PARTNER` |
| `mission-reward-service` | Catalogue/progression authentifiés; management réservé à `ADMIN` |
| `moderation-trust-service` | Création et liste personnelle authentifiées; file/audit/décisions réservés à `ADMIN`, `SUPER_ADMIN` ou `MODERATOR`; trust score personnel ou rôle privilégié |
| `admin-service` | Accès global `ADMIN`, `MODERATOR` ou `SUPER_ADMIN`, puis restrictions méthode par méthode; création/modification d'admin = `SUPER_ADMIN` |
| `analytics-service` | Dashboard admin, KPI et logs = `ADMIN`/`SUPER_ADMIN`; dashboard partenaire et engagement utilisateur soumis à un contrôle d'accès dédié |
| Autres services métier | JWT obligatoire |

## Inventaire complet par service

Dans les tableaux existants ci-dessous, le chemin affiché est relatif au **Base Path**, sauf lorsqu'il commence déjà par `/api/v1`.

---

## user-service

**Base Path:** `/api/v1/users`

### Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/me` | Get my profile | Profile screens |
| PUT | `/me` | Update my profile | Profile settings |
| PATCH | `/me/preferences` | Update preferences | Settings |
| DELETE | `/me` | Delete my account | Settings |
| GET | `/{id}` | Get public profile by ID | User profile views |
| GET | `/` (with query param `q`) | Search users | Search screens |

### Social Graph Endpoints

**Base Path:** `/api/v1/users/social`

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| POST | `/{userId}/follow` | Follow a user | Social graph screens - utilisé par socialApi.followUser |
| DELETE | `/{userId}/follow` | Unfollow a user | Social graph screens - utilisé par socialApi.unfollowUser |
| GET | `/following` | My following list | Social graph - FollowingScreen - utilisé par socialApi.getFollowing |
| GET | `/followers` | My followers list | Social graph - FollowersScreen - utilisé par socialApi.getFollowers |
| GET | `/{userId}/following` | User's following list | Public profile - FollowingScreen |
| GET | `/{userId}/followers` | User's followers list | Public profile - FollowersScreen |
| GET | `/stats` | My social stats | Profile stats |
| GET | `/{userId}/stats` | User's social stats | Public profile stats |
| POST | `/{userId}/block` | Block a user | Social settings |
| DELETE | `/{userId}/block` | Unblock a user | Social settings |
| GET | `/suggestions` | Friend suggestions | Social graph - utilisé par socialApi.getSuggestions |
| GET | `/search` | Search users (excluding blocked) | Search screens - utilisé par socialApi.searchUsers |
| GET | `/activity` | Network activity | Social feed - utilisé par socialApi.getNetworkActivity |

---

## auth-service

**Base Path:** `/api/v1/auth`

### Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| POST | `/register` | Register new user | Login/Register - utilisé par authApi.register |
| POST | `/login` | Login with email/password | Login - utilisé par authApi.login |
| POST | `/oauth/google` | Login with Google | OAuth login |
| POST | `/oauth/apple` | Login with Apple | OAuth login |
| POST | `/refresh` | Refresh access token | Token management |
| POST | `/email/verification/request` | Request email verification code | Email verification |
| POST | `/email/verification/confirm` | Confirm email with code | Email verification |
| POST | `/password/forgot` | Request password reset | Password reset |
| POST | `/password/reset` | Reset password with code | Password reset |
| GET | `/me` | Get current user info | Auth state - utilisé par authApi.me |
| POST | `/logout` | Logout | Logout button - utilisé par authApi.logout |

---

## place-service

**Base Path:** `/api/v1/places`

### Places Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/nearby` | Find places nearby (lat, lng, radius) | Map view, Explore - utilisé par placesApi.getPlaces |
| GET | `/{id}` | Get place by ID | Place detail screen - utilisé par placesApi.getPlace |
| POST | `/` | Create a place | Partner dashboard |
| PUT | `/{id}` | Update a place | Partner dashboard |

### Regions Endpoints

**Base Path:** `/api/v1/regions`

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/` | List all regions | Region selector, Explore |
| GET | `/{slug}` | Get region by slug | Region detail page |
| GET | `/{slug}/places` | Places in a region | Region explore |
| POST | `/` | Create region | Admin panel |
| PUT | `/{id}` | Update region | Admin panel |

### Cities Endpoints

**Base Path:** `/api/v1/cities`

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/{id}/places` | Places in a city | City explore - utilisé par placesApi.getPlaces |
| GET | `/region/{regionId}` | Cities in a region | City selector |
| POST | `/` | Create city | Admin panel |
| PUT | `/{id}` | Update city | Admin panel |

### Districts Endpoints

**Base Path:** `/api/v1/districts`

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/city/{cityId}` | Districts in a city | District selector |
| POST | `/` | Create district | Admin panel |
| PUT | `/{id}` | Update district | Admin panel |

### Categories Endpoints

**Base Path:** `/api/v1/categories`

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/` | List all categories | Category filter, Explore |

---

## catalog-service

**Base Path:** `/api/v1/catalog`

### Catalog Assets Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/assets/{id}` | Get asset by ID | Asset detail |
| GET | `/assets/manage/{id}` | Get asset for management | Partner dashboard |
| GET | `/assets/slug/{slug}` | Get asset by slug | Deep linking |
| GET | `/assets` | Search assets (type, region, category, q) | Search, Explore |
| GET | `/assets/nearby` | Nearby assets (lat, lng, radius) | Map, Explore nearby |
| POST | `/assets` | Create catalog asset | Partner/Admin |
| PUT | `/assets/{id}` | Update asset | Partner/Admin |
| PATCH | `/assets/{id}/status` | Change workflow status | Partner/Admin |
| DELETE | `/assets/{id}` | Soft-delete asset | Partner/Admin |

### Catalog References Endpoints

**Base Path:** `/api/v1/catalog` (`kind` = `regions`, `cities` ou `categories`)

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/{kind}` | List references (with filters) | Reference selectors |
| GET | `/{kind}/{id}` | Get reference by ID | Reference detail |
| POST | `/{kind}` | Create reference | Admin panel |
| PUT | `/{kind}/{id}` | Update reference | Admin panel |
| DELETE | `/{kind}/{id}` | Deactivate reference | Admin panel |
| POST | `/{kind}/{id}/activate` | Reactivate reference | Admin panel |

### Collections Endpoints

**Base Path:** `/api/v1/collections`

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/` | My collections | Collections screen - utilisé par collectionsApi.getUserCollections |
| GET | `/public` | Public collections | Explore collections - utilisé par collectionsApi.getPublicCollections |
| GET | `/{id}` | Collection detail with assets | Collection detail - utilisé par collectionsApi.getCollection |
| GET | `/summaries` | Collection summaries | Collection selector - utilisé par collectionsApi.getCollectionSummaries |
| POST | `/` | Create collection | New collection - utilisé par collectionsApi.createCollection |
| PUT | `/{id}` | Update collection | Edit collection - utilisé par collectionsApi.updateCollection |
| DELETE | `/{id}` | Delete collection | Delete collection - utilisé par collectionsApi.deleteCollection |
| POST | `/places` | Add place to collection | Add to collection - utilisé par collectionsApi.addPlaceToCollection |
| DELETE | `/{collectionId}/places/{assetId}` | Remove place from collection | Collection management - utilisé par collectionsApi.removePlaceFromCollection |

---

## content-service

**Base Path:** `/api/v1`

### Posts Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| POST | `/posts` | Create draft post | Create post - utilisé par postApi.createPost |
| PUT | `/posts/{id}` | Update draft | Edit draft |
| POST | `/posts/{id}/publish` | Publish draft | Publish action |
| PATCH | `/posts/{id}/visibility` | Change visibility | Post settings |
| POST | `/posts/{id}/archive` | Archive post | Post management |
| DELETE | `/posts/{id}` | Delete post | Delete post - utilisé par postApi.deletePost |
| GET | `/posts/{id}` | Get public post | Post detail - utilisé par feedApi.getPost |
| GET | `/posts/me` | My posts | Profile publications - utilisé indirectement par profileApi.getUserPublications |
| GET | `/posts/me/{id}` | Get my post (any status) | Edit my post |
| GET | `/posts/hashtags/{tag}` | Posts by hashtag | Hashtag feed |
| GET | `/posts/catalog/{assetId}` | Posts linked to asset | Asset posts |

### Stories Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/stories` | Active stories | Stories feed - utilisé par storyApi.getStories |
| GET | `/stories/{id}` | Story detail | Story viewer - utilisé par storyApi.getStory |
| POST | `/stories/{id}/view` | Record story view | Story tracking - utilisé par storyApi.markViewed |
| POST | `/stories` | Create story | Create story - utilisé par postApi.createStory |
| DELETE | `/stories/{id}` | Delete story | Delete story |

---

## interaction-service

**Base Path:** `/api/v1`

### Interactions Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| PUT | `/interactions/posts/{postId}/like` | Like a post | Like button - utilisé par feedApi.likePost |
| DELETE | `/interactions/posts/{postId}/like` | Unlike a post | Unlike button - utilisé par feedApi.unlikePost |
| PUT | `/interactions/posts/{postId}/favorite` | Save a post | Save button - utilisé par feedApi.savePost |
| DELETE | `/interactions/posts/{postId}/favorite` | Unsave a post | Unsave button - utilisé par feedApi.unsavePost |
| POST | `/interactions/posts/{postId}/comments` | Comment on a post | Comment form |
| PUT | `/interactions/comments/{id}` | Edit comment | Edit comment |
| DELETE | `/interactions/comments/{id}` | Delete comment | Delete comment |
| POST | `/interactions/posts/{postId}/shares` | Share a post | Share menu |
| GET | `/interactions/posts/{postId}/summary` | Interaction counts | Post detail |
| GET | `/interactions/posts/{postId}/comments` | List comments | Comments section |
| POST | `/interactions/places/{placeId}/reviews` | Create review | Review form - utilisé indirectement par profileApi.getUserReviews |
| PUT | `/interactions/reviews/{id}` | Update review | Edit review |
| DELETE | `/interactions/reviews/{id}` | Delete review | Delete review |
| GET | `/interactions/places/{placeId}/reviews` | Reviews for place | Place reviews |
| GET | `/interactions/users/{userId}/reviews` | User's reviews | User reviews |

### Check-ins Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| POST | `/checkins` | Check in at location | Check-in action |
| GET | `/checkins/me` | My check-ins | Profile check-ins |

### Saved Posts Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/saves` | My saved posts | Saved posts screen - utilisé indirectement par profileApi.getUserFavorites |

---

## media-service

**Base Path:** `/api/v1/media`

### Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| POST | `/` | Upload image or video | Media upload - utilisé par postApi.uploadMedia |
| GET | `/{id}` | Get media metadata | Media info |
| GET | `/{id}/content` | Download original content | Media viewer |
| GET | `/{id}/thumbnail` | Download thumbnail | Thumbnails in lists |
| DELETE | `/{id}` | Delete media | Media management |

---

## feed-service

**Base Path:** `/api/v1/feed`

### Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/` | Personalized feed | Feed screen - utilisé par feedApi.getFeed |

---

## discovery-service

**Base Path:** `/api/v1/discovery`  
**Accès:** JWT obligatoire

### Endpoints

| Method | Path | Description | Paramètres principaux |
|--------|------|-------------|------------------------|
| GET | `/search` | Recherche unifiée des lieux et contenus découvrables | `q`, `type`, `categoryCode`, `regionCode`, `lat`, `lng`, `radiusKm`, `page=0`, `size=20` |
| GET | `/trending` | Liste les lieux et contenus tendance | `type`, `regionCode`, `page=0`, `size=20` |

---

## event-service

**Base Path:** `/api/v1/events`

### Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| POST | `/` | Create event | Create event (Partner) |
| GET | `/upcoming` | Upcoming events | Events explore |
| GET | `/{id}` | Event detail | Event detail screen |
| PUT | `/{id}` | Update event | Edit event |
| PATCH | `/{id}/status` | Update status | Event management |
| POST | `/{id}/register` | Register for event | Event registration - utilisé indirectement par profileApi.getUserEvents |
| DELETE | `/{id}/unregister` | Unregister from event | Event management |

### Place Events Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/api/v1/places/{placeId}/events` | Events at a place | Place detail events |

---

## partner-service

**Base Path:** `/api/v1/partners`

### Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| POST | `/` | Create partner profile | Partner onboarding |
| GET | `/me` | My partner profile | Partner dashboard |
| PUT | `/me` | Update my profile | Partner settings |
| GET | `/me/documents` | My documents | Partner documents |
| POST | `/me/documents` | Upload document | Document upload |
| DELETE | `/me/documents/{id}` | Remove document | Document management |
| POST | `/me/submit` | Submit for validation | Partner submission |
| GET | `/{id}` | Public partner profile | Partner profile view |
| GET | `/` | Search partners | Partner search |

---

## booking-service

**Base Path:** `/api/v1`

Les commandes `POST /bookings`, `POST /bookings/{id}/cancel` et `POST /booking-management/bookings/{id}/complete` exigent l'en-tête `Idempotency-Key`.

### Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/activities/{activityId}/availability` | Check slot availability | Booking flow |
| POST | `/bookings` | Create booking | Booking confirmation - utilisé indirectement par profileApi.getUserReservations |
| GET | `/bookings/me` | My bookings | Reservations screen |
| GET | `/bookings/{id}` | Booking detail | Booking detail |
| GET | `/bookings/{id}/history` | Booking history | Booking tracking |
| POST | `/bookings/{id}/cancel` | Cancel booking | Cancel reservation |

### Booking Management Endpoints

**Base Path:** `/api/v1/booking-management`

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| POST | `/slots` | Create slot | Partner dashboard |
| POST | `/slots/{id}/close` | Close slot | Partner dashboard |
| POST | `/bookings/{id}/complete` | Mark booking complete | Partner dashboard |

---

## payment-service

**Base Path:** `/api/v1/payments`

Le remboursement manuel exige `Idempotency-Key` et un utilisateur ayant le rôle `ADMIN`. Le webhook est public au niveau Gateway et service, exige `X-Payment-Timestamp` et `X-Payment-Signature`, puis valide la signature du prestataire.

### Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/{id}` | Get payment details | Payment detail |
| GET | `/mine` | My payments | Payments history |
| GET | `/{id}/refunds` | Payment refunds | Refund history |
| POST | `/{id}/refunds` | Request refund (admin) | Admin panel |

### Webhooks Endpoints

**Base Path:** `/api/v1/payments/webhooks`

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| POST | `/{provider}` | Receive payment webhook | N/A (backend only) |

---

## messaging-service

**Base Path:** `/api/v1/messaging`

### Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| POST | `/conversations` | Create conversation | New conversation - utilisé par chatApi.createConversation |
| GET | `/conversations` | My conversations | Chat list - utilisé par chatApi.getConversations |
| GET | `/conversations/{id}` | Conversation detail | Chat screen |
| POST | `/conversations/{id}/members` | Add member | Group management |
| DELETE | `/conversations/{id}/members/{userId}` | Remove member | Group management |
| POST | `/conversations/{id}/leave` | Leave conversation | Leave group |
| POST | `/conversations/{id}/messages` | Send message | Chat input - utilisé par chatApi.sendMessage |
| GET | `/conversations/{id}/messages` | Get messages | Chat messages - utilisé par chatApi.getMessages |
| PATCH | `/messages/{messageId}` | Edit message | Edit message |
| DELETE | `/messages/{messageId}` | Delete message | Delete message |
| POST | `/conversations/{id}/read/{messageId}` | Mark as read | Read receipts - utilisé par chatApi.markRead |

---

## notification-service

**Base Path:** `/api/v1/notifications`

### Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/` | My notifications | Notifications screen - utilisé par notificationsApi.getNotifications |
| POST | `/{id}/read` | Mark notification as read | Notification tap - utilisé par notificationsApi.markAsRead |
| POST | `/read-all` | Mark all as read | Mark all button - utilisé par notificationsApi.markAllAsRead |
| GET | `/preferences` | Notification preferences | Settings |
| PUT | `/preferences` | Update preferences | Notification settings |

**Note:** Les endpoints `/unread`, `/unread/count` et `DELETE /{id}` mentionnés dans le client mobile n'existent pas dans le backend actuel.

---

## gamification-service

**Base Path:** `/api/v1/me`

### Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/xp` | My XP and level | Profile gamification |
| GET | `/badges` | My badges | Badges screen - utilisé indirectement par badgesApi.getUserBadges |
| GET | `/passport` | My passport stamps | Passport screen |
| GET | `/streaks` | My streaks | Streaks widget |
| GET | `/rewards` | My rewards | Rewards screen |
| POST | `/rewards/{id}/claim` | Claim reward | Claim button |

**Note:** Les endpoints `/badges/user`, `/badges/{id}`, `/badges/stats`, `/badges` du client mobile ne correspondent pas au service gamification actuel.

---

## mission-reward-service

**Base Path:** `/api/v1`

### Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/missions` | Mission catalog | Missions explore |
| GET | `/me/missions` | My mission progress | My missions |
| GET | `/me/mission-rewards` | My mission rewards | Mission rewards |

### Mission Management Endpoints

**Base Path:** `/api/v1/mission-management`

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| POST | `/missions` | Create mission | Admin panel |
| POST | `/missions/{id}/activate` | Activate mission | Admin panel |
| POST | `/missions/{id}/pause` | Pause mission | Admin panel |

---

## referral-service

**Base Path:** `/api/v1`

### Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| POST | `/referrals/codes` | Create referral code | Referral screen |
| POST | `/referrals/invitations` | Send invitation | Invite friends |
| POST | `/referrals/redeem` | Redeem referral code | Onboarding |
| POST | `/referrals/codes/{id}/disable` | Disable code | Code management |
| GET | `/me/referrals/codes` | My referral codes | Referral dashboard |
| GET | `/me/referrals/invitations` | My invitations | Invitations sent |
| GET | `/me/referrals/attributions` | My attributions | Referral success |
| GET | `/me/referrals/rewards` | Referral rewards | Rewards earned |
| GET | `/me/referrals/attributions/{id}/history` | Attribution history | Attribution detail |

---

## recommendation-service

**Base Path:** `/api/v1/recommendations`

### Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/` | Personalized recommendations | Recommendations screen |

---

## moderation-trust-service

**Base Path:** `/api/v1`

### Moderation Reports Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| POST | `/moderation/reports` | Report content/account | Report button |
| GET | `/moderation/reports/me` | My reports | My reports |
| GET | `/moderation/reports` | Moderation queue | Moderator panel |
| GET | `/moderation/reports/{id}` | Report detail | Report detail |
| POST | `/moderation/reports/{id}/review` | Start review | Moderator action |
| POST | `/moderation/reports/{id}/decision` | Approve/reject report | Moderator action |

### Trust Scores Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/trust/{subjectId}` | Get trust score | Trust badge |

### Moderation Audit Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/moderation/audit` | Latest audit entries | Admin audit log |

---

## analytics-service

**Base Path:** `/api/v1/analytics`

### Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/admin/dashboard` | Admin dashboard | Admin analytics (web) |
| GET | `/kpis` | Latest KPIs | Admin KPIs |
| GET | `/kpis/{kpiName}` | KPI history | Admin charts |
| GET | `/event-logs` | Event logs | Admin logs |
| GET | `/regions/{regionId}/activity` | Region activity | Region analytics |
| GET | `/partners/{partnerId}/dashboard` | Partner analytics | Partner dashboard |
| GET | `/places/popular` | Popular places | Trending places |
| GET | `/places/{placeId}/popularity` | Place popularity | Place analytics |
| GET | `/users/{userId}/engagement` | User engagement | User analytics (admin) |

---

## admin-service

**Base Path:** `/api/v1/admin`

### Admin Users Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/users` | List admin users | Admin panel |
| GET | `/users/{id}` | Get admin user | Admin detail |
| POST | `/users` | Create admin user | Admin creation |
| PUT | `/users/{id}` | Update admin user | Admin edit |

### Audit Logs Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/audit-logs` | Audit logs | Admin audit |

### Moderation Actions Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/moderation-actions` | Moderation history | Admin moderation |
| POST | `/moderation-actions` | Apply moderation | Admin action |

### Reports Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/reports` | List reports | Admin reports |
| POST | `/reports` | Create report | Report submission |
| PATCH | `/reports/{id}/resolution` | Resolve report | Admin resolution |

### Validations Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| GET | `/validations/partners` | Partner validations | Admin validation |
| POST | `/validations/partners` | Create partner validation | Validation request |
| PATCH | `/validations/partners/{id}/review` | Review partner | Admin review |
| GET | `/validations/places` | Place validations | Admin validation |
| POST | `/validations/places` | Create place validation | Validation request |
| PATCH | `/validations/places/{id}/review` | Review place | Admin review |

---

## ingestion-service

**Base Path:** `/api/v1/catalog/imports`

Accès `ADMIN` ou `PARTNER`. La soumission exige `Idempotency-Key` et retourne HTTP 202.

### Endpoints

| Method | Path | Description | Interface Mobile |
|--------|------|-------------|------------------|
| POST | `/` | Submit import job | N/A (admin tool) |
| GET | `/{id}` | Get import job status | N/A (admin tool) |

---

## Routage API Gateway et écarts

La Gateway déclare 24 routes avec circuit breaker dans `cloud-conf-yeyamo/api-gateway.properties` et expose un fallback toutes méthodes sur `/fallback/{service}` qui retourne HTTP 503. La plupart des chemins métier sont correctement routés, mais l'audit a relevé les écarts suivants :

| Écart | Conséquence via le port 8083 |
|---|---|
| `/api/v1/stories/**` absent de la route `content-service` | Les 5 endpoints stories existent dans le service mais ne sont pas joignables via la Gateway |
| `/api/v1/collections/**` absent de la route `catalog-service` | Les 9 endpoints collections ne sont pas joignables via la Gateway |
| `/api/v1/catalog/regions/**`, `/cities/**`, `/categories/**` absents de la route catalogue | Les 6 formes génériques d'endpoints de référence ne sont pas joignables via la Gateway |
| La Gateway route `/api/v1/reports/**`, alors que le service implémente `/api/v1/moderation/**` | Les 7 endpoints de rapports/audit de modération ne sont pas joignables via la Gateway; le chemin routé ne correspond à aucun contrôleur |
| `/api/v1/catalog/corrections/**` est routé mais aucun contrôleur correspondant n'existe | Route morte / réservée à une future implémentation |
| Les lectures publiques catalogue, posts, interactions et médias ne sont pas dans la liste publique de la Gateway | Elles sont publiques en accès direct au service mais demandent actuellement un JWT via la Gateway |

Ces écarts sont documentés ici; aucune configuration de routage n'a été modifiée dans le cadre de cette mise à jour documentaire.

## Modules sans endpoint métier

| Module | État observé |
|---|---|
| `graph-service` | Application Spring présente, aucun `@RestController`, aucune route Gateway/config cloud dédiée |
| `search-service` | Application Spring présente, aucun `@RestController`, aucune route Gateway/config cloud dédiée |
| `social-service` | Application Spring présente, aucun `@RestController`; le graphe social réel est implémenté dans `user-service` |
| `config-server` | Infrastructure Spring Cloud Config, port 8080; pas d'endpoint métier déclaré par le projet |
| `registry-service` | Registre Eureka, port 8761; pas d'endpoint métier déclaré par le projet |
| `security-hardening-starter` | Bibliothèque partagée de durcissement sécurité, pas une application REST |

---

## APIs sans interface mobile

Ces endpoints backend n'ont actuellement aucune interface mobile correspondante :

### payment-service
- **POST** `/api/v1/payments/webhooks/{provider}` - Webhooks de paiement (backend seulement)

### ingestion-service
- **POST** `/api/v1/catalog/imports` - Soumettre un job d'import
- **GET** `/api/v1/catalog/imports/{id}` - Statut du job d'import

### admin-service (tous les endpoints)
- Administration des utilisateurs admin
- Actions de modération admin
- Résolution des rapports
- Validations des partenaires et lieux
- Audit logs

Les endpoints admin sont destinés à une interface d'administration web, pas à l'application mobile.

---

## Interfaces mobiles sans API backend

Ces fonctionnalités mobiles n'ont pas d'endpoint backend correspondant :

### Notifications
- **GET** `/notifications/unread` - Liste des notifications non lues (endpoint mentionné dans `notificationsApi` mais non implémenté)
- **GET** `/notifications/unread/count` - Compteur de notifications non lues (endpoint mentionné dans `notificationsApi` mais non implémenté)
- **DELETE** `/notifications/{id}` - Supprimer une notification (endpoint mentionné dans `notificationsApi` mais non implémenté)

### Social Graph
- **DELETE** `/social/followers/{userId}` - Retirer un abonné (endpoint mentionné dans `socialApi` mais non implémenté)
- **GET** `/social/friend-suggestions` - Suggestions basées sur contacts (endpoint mentionné dans `socialApi` mais non implémenté)
- **GET/PUT** `/social/settings` - Paramètres sociaux (endpoint mentionné dans `socialApi` mais non implémenté)

### Badges & Gamification
Les endpoints suivants du client mobile (`badgesApi`) ne correspondent à aucun service backend :
- **GET** `/badges/user` - Badges utilisateur
- **GET** `/badges/{id}` - Détail d'un badge
- **GET** `/badges/stats` - Stats des badges
- **GET** `/badges` - Tous les badges disponibles

Ces endpoints devraient probablement être mappés vers **gamification-service** (`/api/v1/me/badges`), mais la structure des données diffère.

### Profile Endpoints
Les endpoints suivants du client mobile (`profileApi`) sont des alias/agrégations :
- **GET** `/profile/publications` - Posts de l'utilisateur (devrait utiliser `/api/v1/posts/me`)
- **GET** `/profile/favorites` - Posts sauvegardés (devrait utiliser `/api/v1/saves`)
- **GET** `/profile/events` - Événements inscrits (devrait utiliser `/api/v1/events` avec filtre)
- **GET** `/profile/reservations` - Réservations (devrait utiliser `/api/v1/bookings/me`)
- **GET** `/profile/reviews` - Avis utilisateur (devrait utiliser `/api/v1/interactions/users/{userId}/reviews`)
- **GET** `/profile/stats` - Stats profil (devrait utiliser `/api/v1/users/social/stats`)

Ces endpoints n'existent pas en tant que tels dans le backend. Le mobile utilise des endpoints fictifs qui devraient être remplacés par les vrais endpoints backend mentionnés ci-dessus.

---

## Résumé

- **217 endpoints REST métier** documentés dans **23 services** possédant au moins un contrôleur.
- **1 endpoint technique** supplémentaire dans l'API Gateway : `/fallback/{service}`.
- **6 modules sans endpoint métier** : `graph-service`, `search-service`, `social-service`, `config-server`, `registry-service` et `security-hardening-starter`.
- **4 familles de routes implémentées mais non exposées correctement par la Gateway** : stories, collections, références catalogue et modération.
- Les endpoints sont majoritairement protégés par JWT; les exceptions publiques et les restrictions de rôles sont détaillées plus haut.

---

**Date de dernière vérification:** 2026-07-22  
**Version backend:** 1.0.0-SNAPSHOT  
**Version mobile:** En développement
