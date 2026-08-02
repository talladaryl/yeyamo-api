# Référence API - Plateforme YeYamo

> Audit statique du code effectué le **22 juillet 2026**. Les routes ci-dessous proviennent des contrôleurs Spring (`@RestController`) présents dans le dépôt, et non d'une liste d'API théorique.

Ce document recense les **235 endpoints REST effectivement implémentés**, répartis dans **23 services applicatifs**. Il précise aussi les règles d'accès, le routage via l'API Gateway et les modules qui ne publient actuellement aucun endpoint métier.

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
| Authentification | `auth-service` | 8082 | 15 |
| Passerelle | `api-gateway` | 8083 | 1 endpoint de fallback |
| Lieux | `place-service` | 8084 | 17 |
| Événements | `event-service` | 8085 | 10 |
| Utilisateurs / graphe social | `user-service` | 8086 | 21 |
| Partenaires | `partner-service` | 8087 | 9 |
| Catalogue / collections | `catalog-service` | 8088 | 24 |
| Ingestion catalogue | `ingestion-service` | 8089 | 2 |
| Contenu | `content-service` | 8090 | 16 |
| Interactions | `interaction-service` | 8091 | 21 |
| Feed | `feed-service` | 8092 | 1 |
| Discovery | `discovery-service` | 8093 | 2 |
| Notifications | `notification-service` | 8094 | 8 |
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
| Gamification | `gamification-service` | 8105 | 10 |
| **Total métier** | **23 services** |  | **235** |

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
| GET | `/blocked` | List profiles blocked by the current user | Privacy settings |
| DELETE | `/followers/{userId}` | Remove one of my followers without blocking | Followers management |
| GET | `/suggestions` | Friend suggestions | Social graph - utilisé par socialApi.getSuggestions |
| GET | `/search` | Search users (excluding blocked) | Search screens - utilisé par socialApi.searchUsers |
| GET | `/activity` | Network activity | Social feed - utilisé par socialApi.getNetworkActivity |

`userId` désigne ici l'UUID public du profil retourné par les DTO utilisateur. Le sujet JWT reste un identifiant d'authentification interne et ne doit pas être envoyé dans ces chemins.

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
| PUT | `/password` | Change password and revoke refresh sessions | Security settings |
| POST | `/account/deactivate` | Temporarily deactivate account | Account settings |
| GET | `/sessions` | List refresh sessions | Session management |
| DELETE | `/sessions/{sessionId}` | Revoke one refresh session | Session management |

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
| PUT | `/interactions/comments/{id}/like` | Like a comment (idempotent) | Comment actions |
| DELETE | `/interactions/comments/{id}/like` | Unlike a comment (idempotent) | Comment actions |
| GET | `/interactions/comments/{id}/likes` | Comment like count and current-user state | Comments section |
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
| GET | `/me` | Events where the current user has a confirmed registration (`limit=50`, max 100) | My events |
| GET | `/{id}` | Event detail | Event detail screen |
| GET | `/{id}/participants?limit=100` | Confirmed participants, maximum 200 rows | Event participants |
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
| GET | `/unread` | My unread notifications (`page=0`, `size=20`, max 50) | Unread notifications |
| GET | `/unread/count` | Unread notification count, response `{ "count": number }` | Notification badge |
| POST | `/{id}/read` | Mark notification as read | Notification tap - utilisé par notificationsApi.markAsRead |
| POST | `/read-all` | Mark all as read | Mark all button - utilisé par notificationsApi.markAllAsRead |
| DELETE | `/{id}` | Delete one notification owned by the current user | Notification management |
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
| GET | `/badges/catalog` | Badge catalog with current user's earned state | Badges catalog |
| GET | `/badges/catalog/{code}` | Badge detail with current user's earned state | Badge detail |
| GET | `/badges/stats` | Badge, XP, level and rank statistics | Badge statistics |
| GET | `/leaderboard?limit=50` | XP leaderboard, maximum 100 rows | Leaderboard |
| GET | `/passport` | My passport stamps | Passport screen |
| GET | `/streaks` | My streaks | Streaks widget |
| GET | `/rewards` | My rewards | Rewards screen |
| POST | `/rewards/{id}/claim` | Claim reward | Claim button |

**Note:** le mobile doit utiliser les chemins ci-dessus sous `/api/v1/me`; les anciens chemins simulés `/badges/user` et `/badges/{id}` ne sont pas des routes backend.

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

La Gateway déclare 24 routes REST avec circuit breaker dans `cloud-conf-yeyamo/api-gateway.properties` et expose un fallback toutes méthodes sur `/fallback/{service}` qui retourne HTTP 503.

Les corrections d'alignement suivantes ont été appliquées le 22 juillet 2026 :

- ajout de `/api/v1/stories/**` vers `content-service`;
- ajout de `/api/v1/collections/**` et des références `regions/cities/categories` vers `catalog-service`;
- remplacement de la route morte `/api/v1/catalog/corrections/**`;
- correction de `/api/v1/reports/**` en `/api/v1/moderation/**`;
- alignement des lectures publiques catalogue, posts publics, interactions publiques et médias avec les règles des services cibles;
- protection explicite de `GET /api/v1/events/me` avant la règle publique des événements.

### Limite WebSocket connue

`messaging-service` expose directement un endpoint STOMP sur `/ws/messaging`, mais `api-gateway` utilise Spring Cloud Gateway **Server Web MVC**. Le proxy WebSocket natif documenté par Spring Cloud Gateway appartient à la variante WebFlux; aucune fausse route `lb://` n'a donc été ajoutée. En attendant une Gateway WebFlux ou un reverse proxy compatible WebSocket, le mobile doit recevoir une URL WebSocket publique distincte pointant vers `messaging-service`.

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

## Écarts mobiles restant sans API backend

Les routes de notifications non lues, de suppression d'une notification et de retrait d'un abonné sont maintenant implémentées et décrites plus haut. Les fonctionnalités suivantes n'ont toujours pas d'endpoint backend correspondant :

### Social Graph
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
- **GET** `/profile/events` - Événements inscrits (utiliser désormais `/api/v1/events/me`)
- **GET** `/profile/reservations` - Réservations (devrait utiliser `/api/v1/bookings/me`)
- **GET** `/profile/reviews` - Avis utilisateur (devrait utiliser `/api/v1/interactions/users/{userId}/reviews`)
- **GET** `/profile/stats` - Stats profil (devrait utiliser `/api/v1/users/social/stats`)

Ces endpoints n'existent pas en tant que tels dans le backend. Le mobile utilise des endpoints fictifs qui devraient être remplacés par les vrais endpoints backend mentionnés ci-dessus.

---

## Complément d'audit pour l'intégration React Native

> Ajout documentaire du **22 juillet 2026**. Cette section complète l'inventaire précédent sans le remplacer. Elle compare les interfaces réellement présentes dans `yeyamo-mobile` avec le contrat backend exposé dans ce document. Les différences de préfixe ou de chemin d'URL (par exemple `/api/...` contre `/api/v1/...`) sont volontairement exclues de la liste des incohérences de contrat ci-dessous.

### État du client mobile observé

- Le projet utilise Expo SDK 54, React Native 0.81, Expo Router, Axios, TanStack Query, Zustand et `expo-secure-store`.
- Le client HTTP central est dans `src/services/api/client.ts` et les variables publiques sont lues dans `src/config/env.ts`.
- Les mocks sont actifs par défaut tant que `EXPO_PUBLIC_USE_MOCKS` n'est pas exactement égal à `false`.
- 11 modules `*.api.ts` existent, mais plusieurs domaines visibles dans l'application restent alimentés directement par des mocks ou par un état local.
- Les écrans d'événements, d'expériences, d'exploration, de paramètres, d'offres et une grande partie du dashboard partenaire n'ont pas encore de couche API complète.

### Alignements backend livrés le 22 juillet 2026

| Besoin mobile | Contrat backend disponible |
|---|---|
| Retirer un abonné | `DELETE /api/v1/users/social/followers/{userId}` |
| Lister les comptes bloqués | `GET /api/v1/users/social/blocked` |
| Notifications non lues | `GET /api/v1/notifications/unread` |
| Compteur non lu | `GET /api/v1/notifications/unread/count` |
| Supprimer une notification | `DELETE /api/v1/notifications/{id}` |
| Événements inscrits | `GET /api/v1/events/me` |

Les commandes sociales acceptent maintenant les UUID publics de profil. Les inscriptions aux événements utilisent le sujet JWT sous forme de chaîne, ce qui les rend compatibles avec l'identifiant numérique émis par `auth-service`.

### Besoins backend encore absents pour des interfaces React Native existantes

Le tableau suivant ne demande pas nécessairement un endpoint séparé pour chaque bouton. Le backend peut regrouper plusieurs opérations si le contrat final couvre explicitement le besoin fonctionnel.

| Domaine mobile déjà visible | Besoin backend non trouvé dans les 223 endpoints | État ou conséquence côté mobile |
|---|---|---|
| Sécurité du compte | Changer le mot de passe d'un utilisateur déjà authentifié | L'écran Sécurité renvoie actuellement vers le parcours « mot de passe oublié » de démonstration |
| Sécurité du compte | Lister les sessions/appareils actifs et révoquer une session distante | Les appareils affichés viennent de `MOCK_USER_SETTINGS` et l'action de déconnexion n'appelle aucun backend |
| Sécurité du compte | Activer, confirmer, désactiver et récupérer la configuration 2FA | Le bouton 2FA ne fait que modifier l'état local |
| Sécurité du compte | Vérifier ou modifier le numéro de téléphone | Seule la vérification d'email est documentée dans `auth-service` |
| Confidentialité | Lire et enregistrer la visibilité du compte, le statut en ligne, les autorisations de message/tag, l'affichage de la ville/localisation et la présence dans recherche/suggestions | L'écran Confidentialité est entièrement local; `/users/me/preferences` n'est pas suffisamment décrit pour confirmer qu'il accepte ces champs |
| Préférences | Lire/enregistrer la langue, les catégories préférées et les préférences de contenu | L'interface existe; la structure acceptée par `/users/me/preferences` n'est pas documentée |
| Contacts et suggestions | Importer/synchroniser les contacts, demander le consentement et obtenir des suggestions issues du carnet d'adresses | L'écran « Trouver des amis » existe, mais aucune API de contacts n'est publiée |
| Notifications push | Gérer plusieurs appareils, renouveler et supprimer individuellement les tokens Expo/APNs/FCM | `PUT /notifications/preferences` sait enregistrer un token unique, mais ne modélise pas encore le cycle de vie multi-appareils |
| Favoris de lieux | Ajouter/retirer un lieu favori et lister les lieux favoris | Le backend documente les favoris de posts et les collections, mais pas un favori de lieu explicite alors que l'écran utilise `FavoritePlace` |
| Suggestions de lieux | Soumettre une suggestion de lieu en tant qu'utilisateur standard, suivre sa validation et éventuellement la corriger | L'écran consommateur annonce actuellement un enregistrement « en mode démo »; la création de lieu existante est réservée à `ADMIN`, `SUPER_ADMIN` ou `PARTNER` |
| Événements | Lister les participants, inviter des utilisateurs, accepter/refuser une invitation et gérer « amis proches » | L'interface de création permet les invitations et plusieurs visibilités, mais le contrat publié ne décrit pas ces opérations |
| Offres partenaires | Créer, modifier, soumettre à validation, publier, lister et archiver une offre/promotion/package | L'écran `src/app/(partner)/offer.tsx` est complet visuellement, mais aucun service d'offres n'est présent |
| Dashboard partenaire | Flux consolidé d'activité récente du partenaire | Le dashboard utilise des données locales; aucun endpoint d'agrégation correspondant n'est recensé |
| Messagerie | Supprimer ou archiver une conversation entière | L'interface propose « Supprimer la conversation », mais seuls les messages individuels peuvent être supprimés côté backend documenté |
| Messagerie | Épingler/désépingler un message et lister les messages épinglés | L'écran « Messages épinglés » applique actuellement une règle locale de démonstration |
| Messagerie | Mettre une conversation en sourdine et synchroniser cette préférence | Le réglage est uniquement stocké dans Zustand |
| Messagerie | Rechercher côté serveur dans une conversation et exporter une conversation | La recherche actuelle ne porte que sur les messages déjà chargés; l'export est simulé |
| Appels | Signalisation d'appel audio/vidéo, présence, acceptation/refus, ICE/STUN/TURN et historique | Les écrans audio/vidéo existent, mais aucun service d'appel ni protocole WebRTC n'est documenté |
| Temps réel | Rendre le STOMP existant joignable publiquement et l'aligner avec le client | `/ws/messaging` existe dans `messaging-service`, mais la Gateway MVC ne le proxifie pas et le mobile parle encore Pusher/Reverb |
| Gamification | Catalogue complet des badges, détail d'un badge et statistiques agrégées attendues par l'interface | `/me/badges` ne couvre explicitement que les badges de l'utilisateur; les autres écrans utilisent un contrat différent |
| Gamification sociale | Classement des voyageurs et position de l'utilisateur | L'écran Passeport contient un classement, mais aucun endpoint de leaderboard n'est recensé |
| Paiement utilisateur | Finaliser le parcours d'un vrai prestataire mobile et récupérer son état de traitement | La réservation déclenche déjà l'autorisation par Kafka, mais le fournisseur actif est simulé |

#### Fonctions qui peuvent être couvertes par composition plutôt que par un nouvel endpoint

- Les publications personnelles peuvent utiliser les posts de l'utilisateur courant.
- Les posts sauvegardés peuvent utiliser `saves`.
- Les réservations personnelles peuvent utiliser `bookings/me`.
- Les avis personnels peuvent être obtenus par l'endpoint d'avis d'un utilisateur si l'identifiant courant est connu.
- Les statistiques sociales peuvent utiliser les statistiques du graphe social.
- Les expériences peuvent éventuellement être représentées par des assets catalogue filtrés par type; le backend doit confirmer le type et le DTO.
- Les lieux favoris peuvent éventuellement être modélisés comme une collection système non supprimable; si cette option est retenue, elle doit être explicitée pour éviter deux sources de vérité.

### Informations backend nécessaires pour configurer le `.env`

Les valeurs préfixées par `EXPO_PUBLIC_` sont intégrées au bundle de l'application et sont donc lisibles par l'utilisateur final. **Aucun secret serveur, clé privée, mot de passe, secret OAuth, secret JWT, signature de webhook ou clé d'administration ne doit être placé dans ces variables.** Les secrets restent exclusivement côté backend ou dans le gestionnaire de secrets EAS lorsqu'une valeur n'est pas publique.

#### Variables actuelles et cible d'alignement

| Variable | Information que le backend ou l'infrastructure doit fournir | Exemple de forme, non contractuel |
|---|---|---|
| `EXPO_PUBLIC_API_BASE_URL` | Origine HTTPS publique de l'API Gateway, sans chemin ajouté en double; fournir une valeur distincte pour développement, staging et production | `https://api-staging.example.com` |
| `EXPO_PUBLIC_USE_MOCKS` | `false` pour appeler réellement le backend | `false` |
| `EXPO_PUBLIC_APP_ENV` | Nom de l'environnement ciblé | `development`, `staging` ou `production` |
| `EXPO_PUBLIC_MESSAGING_WS_URL` | URL publique directe du WebSocket STOMP; variable à ajouter au mobile tant que la Gateway MVC ne proxifie pas WebSocket | `wss://messaging-staging.example.com/ws/messaging` |
| `EXPO_PUBLIC_REVERB_HOST` | Hôte temps réel uniquement si le backend supporte réellement le protocole Pusher/Reverb actuellement codé | `realtime.example.com` |
| `EXPO_PUBLIC_REVERB_PORT` | Port public du service temps réel | `443` |
| `EXPO_PUBLIC_REVERB_SCHEME` | Transport public du service temps réel | `wss` en production |

#### Informations à confirmer avant de figer les variables

1. URL publique exacte de la Gateway pour chaque environnement.
2. Disponibilité depuis un téléphone physique; `localhost` sur le téléphone ne désigne pas la machine de développement.
3. Obligation HTTPS/TLS, certificats utilisés et éventuel certificate pinning.
4. Chemin de base déjà inclus ou non dans l'URL fournie afin d'éviter de le concaténer deux fois.
5. URL et protocole temps réel réels : WebSocket natif, STOMP, SSE, Pusher/Reverb ou autre.
6. Endpoint d'authentification des canaux privés, format de l'en-tête/token, noms des canaux et noms des événements.
7. Paramètres heartbeat/ping-pong, reconnexion, reprise après coupure et stratégie de rattrapage des messages.
8. Identifiants OAuth publics nécessaires au mobile pour Google et Apple, avec les redirect URI et schemes autorisés. Les secrets OAuth restent côté serveur.
9. Origines CORS autorisées si la cible Expo Web doit fonctionner; les applications Android/iOS natives ne reposent pas sur CORS de la même manière qu'un navigateur.
10. Taille maximale des requêtes et fichiers, formats MIME, durée vidéo maximale et éventuelle URL CDN publique.
11. Configuration push : projet Expo/EAS attendu, environnements APNs/FCM et endpoint d'enregistrement du token appareil.
12. Durée de vie des access/refresh tokens et marge de renouvellement recommandée.

#### Exemple de fichier local, sans valeur sensible

```dotenv
EXPO_PUBLIC_API_BASE_URL=https://gateway-a-confirmer.example.com
EXPO_PUBLIC_USE_MOCKS=false
EXPO_PUBLIC_APP_ENV=development
EXPO_PUBLIC_MESSAGING_WS_URL=wss://messaging-staging.example.com/ws/messaging

# Variables Reverb à supprimer après migration du client vers STOMP.
```

Le dépôt ignore actuellement `.env*.local`; un fichier tel que `.env.development.local` convient pour une configuration machine non partagée. Une valeur `EXPO_PUBLIC_*` ne devient toutefois pas secrète parce que le fichier est ignoré par Git.

### Contrats immédiatement consommables par le front

#### Authentification

`POST /api/v1/auth/register`, `/login`, `/oauth/google`, `/oauth/apple` et `/refresh` retournent :

```json
{
  "accessToken": "jwt",
  "refreshToken": "opaque-refresh-token",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 42,
    "email": "user@example.com",
    "phone": "+221700000000",
    "status": "ACTIVE",
    "roles": ["USER"],
    "createdAt": "2026-07-22T08:00:00",
    "emailVerifiedAt": "2026-07-22T08:05:00Z"
  }
}
```

Le refresh attend `{ "refreshToken": "..." }`, effectue une rotation et retourne la même enveloppe. Un rejeu de l'ancien refresh token retourne `REFRESH_TOKEN_REUSE_DETECTED` et révoque tous les refresh tokens de l'utilisateur. `POST /auth/logout` révoque également toutes ses sessions backend actuelles.

#### Identifiants sociaux et événements

- L'identifiant `user.id` de `auth-service` est un nombre et devient le sujet JWT sous forme de chaîne.
- L'identifiant `id` des DTO de `user-service` est un UUID public de profil.
- Tous les `{userId}` des routes `/users/social/**` attendent l'UUID public de profil.
- `event-service` utilise directement le sujet JWT pour l'inscription; le front n'envoie aucun identifiant utilisateur dans le body.

#### Pagination des notifications

`GET /notifications`, `GET /notifications/unread` retournent :

```json
{
  "page": 0,
  "size": 20,
  "hasNext": false,
  "items": []
}
```

`GET /notifications/unread/count` retourne `{ "count": 3 }`. Une lecture ou suppression par UUID qui n'appartient pas au sujet JWT retourne volontairement 404 afin de ne pas divulguer l'existence de la notification.

#### Messagerie temps réel STOMP

- URL directe du service : `ws(s)://<messaging-host>/ws/messaging`;
- frame `CONNECT` : en-tête natif `Authorization: Bearer <JWT>`;
- souscription : `/user/queue/messaging`;
- heartbeat client/serveur : 10 secondes;
- limite de souscription : 10 tentatives immédiatement disponibles par utilisateur, puis recharge progressive à 10 par minute (`WEBSOCKET_RATE_LIMIT_SUBSCRIBE` permet de modifier cette valeur);
- reconnexion nécessaire après expiration/rotation du JWT;
- aucune compatibilité Pusher/Reverb implicite.

Chaque message temps réel est enveloppé ainsi :

```json
{
  "eventId": "uuid",
  "eventType": "messaging.message.sent",
  "eventVersion": 1,
  "occurredAt": "2026-07-22T08:00:00Z",
  "correlationId": "uuid-or-forwarded-value",
  "payload": {}
}
```

Les types actuellement produits comprennent `messaging.conversation.created`, `messaging.member.added`, `messaging.member.removed`, `messaging.member.left`, `messaging.message.sent`, `messaging.message.edited`, `messaging.message.deleted` et `messaging.message.read`.

#### Validation backend de cet alignement

Les suites Maven complètes des cinq modules modifiés ont été exécutées : `user-service` (7 tests), `notification-service` (19), `event-service` (4), `api-gateway` (1) et `messaging-service` (21). Résultat : **52 tests réussis, 0 échec, 0 erreur**. Les assertions statiques de routage confirment aussi la présence des routes Gateway ajoutées et l'absence des anciens chemins morts.

### Informations à fournir au client mobile et à l'agent d'intégration

Cette section constitue la fiche de communication attendue du backend. Elle permet à un développeur ou à un assistant chargé de l'intégration de travailler sans inventer le contrat.

#### Communication avec l'assistant chargé du dépôt

- Pour analyser ou modifier ce dépôt, l'assistant n'a besoin d'aucune clé OpenAI dans le `.env` mobile. Il lui faut la documentation versionnée, les OpenAPI, des exemples anonymisés et les commandes de validation du projet.
- Les comptes de test doivent être transmis par un canal sécurisé ou injectés dans un environnement de test; ils ne doivent jamais être écrits dans ce README, dans un prompt persistant ou dans une variable `EXPO_PUBLIC_*`.
- Toute décision prise avec l'assistant doit être reportée dans une source versionnée : OpenAPI, ADR, changelog ou section de ce README. La conversation seule ne doit pas devenir la source de vérité du contrat.
- Si « communiquer avec l'assistant » signifie ajouter une fonctionnalité IA dans l'application, le mobile ne doit jamais embarquer de clé API OpenAI. Il faut un endpoint backend Yeyamo authentifié jouant le rôle de proxy, avec contrôle d'accès, quotas, journalisation sans données sensibles, modération, timeout et contrat de réponse documenté.
- Dans ce dernier cas, le backend doit fournir au mobile uniquement l'URL de son propre endpoint IA et les capacités autorisées. Le modèle, la clé fournisseur et les instructions système restent côté serveur.

#### Source de vérité requise

- URL OpenAPI `/v3/api-docs` de chaque service et, si disponible, spécification agrégée de la Gateway.
- Fichiers OpenAPI versionnés dans le dépôt ou artefacts CI associés à une version backend précise.
- Environnement de test stable avec comptes de démonstration `USER` et `PARTNER` et données réinitialisables.
- Changelog des ruptures de contrat et politique de compatibilité des versions.
- Contact ou canal technique responsable des décisions de contrat backend/mobile.

#### Pour chaque endpoint consommable par le mobile

Le backend doit documenter :

1. méthode HTTP et niveau d'authentification;
2. rôles autorisés et règles de propriété de la ressource;
3. paramètres de chemin et de requête, type, caractère obligatoire, valeur par défaut et limites;
4. schéma complet du body avec exemples valides;
5. schémas de réponse pour tous les statuts possibles;
6. format d'identifiant : UUID, entier, slug ou identifiant composite;
7. format des dates, timezone et précision;
8. format de pagination : page, offset ou curseur;
9. enveloppe d'erreur stable avec code machine, message utilisateur, erreurs par champ et correlation ID;
10. règles d'idempotence et durée de conservation des clés;
11. limites de débit et en-têtes de retry;
12. stratégie de cache et en-têtes ETag/Last-Modified si applicables;
13. contrat multipart : noms des parts, types MIME, limites et réponse d'upload;
14. exemples anonymisés de requêtes/réponses provenant réellement du backend.

#### Contrat d'authentification indispensable

- Structure exacte de la réponse de connexion/inscription/OAuth.
- Nom des champs `access token`, `refresh token`, type du token et expiration.
- Rotation ou réutilisation du refresh token et comportement en cas de rejeu.
- Body attendu pour le refresh et structure de sa réponse.
- Révocation à la déconnexion : session courante seulement ou toutes les sessions.
- Claims JWT utilisables côté client : identifiant, rôle, permissions, expiration et issuer.
- Comportement attendu pour `401`, `403`, compte suspendu, email non vérifié et partenaire en attente de validation.
- Limitation des tentatives de connexion et codes d'erreur associés.

#### Contrat temps réel indispensable

- Technologie exacte et URL de connexion.
- Authentification initiale et renouvellement du token pendant une connexion ouverte.
- Liste des canaux/topics et règles d'autorisation.
- Noms et payloads versionnés des événements.
- Ordre garanti ou non, identifiant de déduplication et reprise après déconnexion.
- Accusés de réception/lecture, présence, saisie en cours et suppression/modification de message.
- Stratégie mobile lorsque l'application passe en arrière-plan : push, rattrapage REST ou les deux.

#### Contexte client à connaître

- Le projet mobile cible actuellement Expo SDK 54 et React Native 0.81; une migration Expo n'est pas requise pour consommer des API HTTP.
- Le JWT est stocké dans `expo-secure-store` et injecté par un interceptor Axios.
- Le client efface actuellement la session dès le premier `401`; cette logique devra être remplacée lorsque le contrat de refresh sera connu.
- Les entités mobiles utilisent encore majoritairement des identifiants numériques et des champs `snake_case`.
- La pagination mobile actuelle est de forme `data/meta/links`, héritée d'un contrat de type Laravel.
- Le client temps réel actuel imite Pusher/Reverb; il ne doit être conservé que si le backend confirme ce protocole.
- L'intégration doit remplacer progressivement les mocks par domaine afin de garder chaque parcours testable.

### Incohérences de contrat hors différences de routes API

Les différences de chemin, préfixe ou nom de route ne figurent pas dans cette liste, conformément au périmètre de cet audit.

| Sujet | Attente actuelle du mobile | Proposition ou information backend observée | Action nécessaire |
|---|---|---|---|
| Verbe pour l'utilisateur courant | Le client appelle actuellement `me` avec `POST` | Le backend déclare une lecture avec `GET` | Corriger le verbe côté mobile |
| Verbes d'interaction | Le mobile utilise `POST` pour liker/sauvegarder | Le backend utilise `PUT` pour rendre ces mutations idempotentes | Aligner les verbes et tester la répétition de la requête |
| Verbes de notification | Le mobile utilise `PUT` pour marquer comme lu et tout lire | Le backend publie `POST` | Aligner les verbes |
| Identifiants | Les types mobiles utilisent principalement `number` | Le document mentionne des détails par UUID pour plusieurs ressources | Fournir le format par ressource puis migrer les types vers `string` lorsque requis |
| Pagination | Le mobile attend `current_page`, `last_page`, `per_page` et `links` | Les paramètres documentés pour certains services sont `page=0` et `size=20`; le schéma de réponse Spring n'est pas fourni | Définir une enveloppe commune ou ajouter des adaptateurs par service |
| Feed | Le client envoie un curseur, des intérêts et `region_id` | L'unique endpoint feed ne documente ni ces paramètres ni son mode de pagination | Publier le contrat de feed complet |
| Recherche de lieux | Le mobile envoie notamment `city`, `search` et `radius_km` | Le backend documente surtout `lat`, `lng` et `radius` pour la proximité | Normaliser noms, unités, valeurs par défaut et recherche hors proximité |
| Modèle d'utilisateur | Le mobile attend `username`, `display_name`, `avatar_url`, `city`, `is_verified`, `user_type` et `created_at` | Aucun DTO complet n'est fourni | Publier les DTO auth/utilisateur ou introduire un mapping explicite |
| Types de compte | Les types mobiles utilisent `user/partner`, `local/diaspora/partner`, tandis que l'onboarding affiche `explorer/developer` | Les rôles backend utilisent notamment `PARTNER`, `ADMIN`, `SUPER_ADMIN` et `MODERATOR` | Définir séparément rôle de sécurité, type de profil et segment marketing |
| Réponse d'authentification | Le mobile attend exactement `{ token, user }` | Le backend retourne `{ accessToken, refreshToken, tokenType, expiresIn, user }` | Adapter le type et le stockage mobile au contrat documenté ci-dessous |
| Renouvellement JWT | Aucun refresh n'est stocké; un `401` détruit immédiatement la session | Un endpoint de refresh existe | Implémenter stockage, rotation, retry unique et verrou de concurrence |
| Déconnexion | Le mobile efface toujours toutes ses données locales | `POST /auth/logout` révoque actuellement tous les refresh tokens de l'utilisateur | Afficher clairement cette portée et ne supprimer localement que les données de session appropriées |
| Erreurs | Le mobile suppose `message`, `errors` et éventuellement `status` | Aucun schéma d'erreur transversal n'est documenté | Standardiser code machine, message, champs, timestamp et correlation ID |
| Dates et fuseaux | Plusieurs écrans manipulent des chaînes libres et formats français | Le format backend et la timezone ne sont pas précisés | Utiliser ISO 8601 et définir UTC/offset pour stockage et affichage |
| Upload multipart | Le mobile envoie une part `file` et force `multipart/form-data` | Le backend ne documente pas le nom des parts, les limites ni le DTO retourné | Publier le contrat d'upload et laisser le runtime générer correctement la boundary |
| Création de post | Le mobile traite la création comme une publication terminée | Le backend crée d'abord un brouillon puis expose une commande de publication | Adapter le workflow UI : upload, brouillon, modification éventuelle, publication |
| Permissions de suggestion de lieu | L'interface consommateur permet de suggérer un lieu | L'écriture de lieu est réservée aux rôles privilégiés | Ajouter une commande de suggestion utilisateur ou retirer cette capacité aux utilisateurs non autorisés |
| Idempotence | Le client ne génère pas d'`Idempotency-Key` | Le backend l'exige pour plusieurs commandes de réservation, finalisation, remboursement et import | Ajouter une génération persistante par action et réutiliser la clé lors des retries |
| Statuts asynchrones | Le client suppose généralement une réponse immédiatement exploitable | L'import retourne `202`; validation partenaire/lieu/offre et traitements médias peuvent aussi être différés | Documenter polling, événements, statuts terminaux et erreurs asynchrones |
| Temps réel | Le client parle le protocole Pusher/Reverb brut et utilise des canaux `private-*` | Le backend utilise STOMP sur `/ws/messaging` avec `/user/queue/messaging` | Remplacer le client Pusher par STOMP ou décider explicitement d'un changement de protocole backend |
| Authentification de canal | Le mobile place directement le JWT dans le champ `auth` de la souscription Pusher | STOMP attend `Authorization: Bearer <JWT>` dans la frame `CONNECT` | Envoyer l'en-tête STOMP natif et gérer une reconnexion après renouvellement du JWT |
| Notifications mobiles | Le package Expo est configuré mais aucun token appareil n'est créé ou synchronisé | `PUT /notifications/preferences` accepte un `pushToken` unique | Utiliser ce contrat temporairement, puis introduire des ressources appareil pour le multi-device |
| Lecture des messages | Le mobile marque une conversation comme lue avec son seul identifiant | Le backend demande aussi l'identifiant du dernier message lu | Faire remonter le dernier message effectivement affiché et gérer les courses |
| Envoi de message | Le mobile envoie `conversation_id`, `body`, `type` et `media_url` dans un payload générique | Le backend rattache l'envoi à la conversation mais ne publie pas le DTO | Confirmer pièces jointes, types, limites, réponses et idempotence |
| Données de badges | Le mobile attend détail, progression, niveaux et statistiques riches | Le backend expose principalement les badges personnels sans DTO | Définir un modèle commun ou réduire l'interface aux données réellement disponibles |
| Paiement | L'interface de réservation peut nécessiter un paiement utilisateur | `POST /bookings` déclenche une autorisation asynchrone, mais le fournisseur est simulé | Définir le prestataire réel, le challenge mobile, les statuts et la sécurité associée |
| Accès public | Certains services déclarent des lectures publiques | La Gateway est maintenant alignée pour catalogue, posts publics, interactions publiques et médias | Conserver un test de non-régression de la matrice d'accès |
| Rôles et propriété | Les écrans partenaires supposent qu'un partenaire peut gérer ses propres lieux, événements et réservations | Les règles publiées mélangent rôle global et contrôles de propriété non détaillés | Documenter précisément les contrôles propriétaire/établissement/équipe |
| Suppression locale sur `401` | Le client efface token, onboarding et intérêts avec `clearAll` selon le parcours | Une expiration temporaire de token ne signifie pas nécessairement une déconnexion définitive | Séparer credentials, préférences locales et données d'onboarding |

### Ordre recommandé de clarification

1. Figer OpenAPI, DTO, erreurs, identifiants et pagination.
2. Figer l'authentification complète, notamment refresh et rôles.
3. Fournir les URLs d'environnement et rendre les services nécessaires accessibles via la Gateway.
4. Valider une tranche verticale : connexion, utilisateur courant, feed et détail d'un lieu.
5. Ajouter uploads, interactions, profil, événements et réservations.
6. Définir ensuite temps réel, push, appels et paiements, qui nécessitent des contrats supplémentaires.
7. Remplacer enfin les mocks des dashboards et fonctions secondaires domaine par domaine.

---

## Résumé

- **223 endpoints REST métier** documentés dans **23 services** possédant au moins un contrôleur.
- **1 endpoint technique** supplémentaire dans l'API Gateway : `/fallback/{service}`.
- **6 modules sans endpoint métier** : `graph-service`, `search-service`, `social-service`, `config-server`, `registry-service` et `security-hardening-starter`.
- Les routes REST auparavant manquantes dans la Gateway (stories, collections, références catalogue et modération) sont maintenant corrigées; le WebSocket STOMP reste exposé directement par `messaging-service`.
- Les endpoints sont majoritairement protégés par JWT; les exceptions publiques et les restrictions de rôles sont détaillées plus haut.

---

**Date de dernière vérification:** 2026-07-22  
**Version backend:** 1.0.0-SNAPSHOT  
**Version mobile:** En développement

---

## Addendum final — audit recroisé du client mobile et des endpoints

> Audit du **22 juillet 2026** réalisé depuis le dépôt `yeyamo-mobile` après les derniers ajouts backend documentés. Cet addendum est placé volontairement en fin de fichier. En cas de contradiction avec une note plus ancienne du document, les constats de cet addendum prévalent. L'audit combine inspection statique du mobile, recomptage des tableaux du présent README, contrôle TypeScript et requêtes HTTP de lecture sur l'environnement local disponible. Aucun endpoint de mutation n'a été appelé.

### Périmètre réellement contrôlé

- **278 fichiers** sous `src` et **97 écrans** Expo Router inspectés.
- **11 modules API** contenant **60 fonctions de consommation** recensées.
- **15 fichiers de mocks**; de nombreux écrans importent encore directement des données simulées.
- **223 lignes d'endpoints REST métier** recomptées dans les tableaux du présent document, réparties sur **23 services**; le total affiché dans la vue d'ensemble est cohérent avec les tableaux.
- `npx.cmd tsc --noEmit` réussit sans erreur sur le projet mobile actuel.
- Le dépôt mobile ne contient pas les sources Spring ni leurs OpenAPI versionnées; les affirmations relatives aux contrôleurs et aux 52 tests backend ne peuvent donc pas être reproduites depuis ce seul workspace.

### Errata sur les sections précédentes du README

Certaines notes antérieures sont devenues fausses après les ajouts backend décrits plus loin dans le document :

1. La note sous `notification-service` affirmant que `/unread`, `/unread/count` et `DELETE /{id}` n'existent pas est obsolète : ces opérations figurent désormais dans le tableau du service.
2. Le retrait d'un abonné et la liste des comptes bloqués ne sont plus des endpoints manquants : ils sont maintenant publiés par `user-service`.
3. La liste des événements de l'utilisateur courant n'est plus manquante : `event-service` publie désormais `GET /events/me`.
4. Le protocole temps réel n'est plus inconnu : le document précise maintenant STOMP, l'URL de handshake, la destination utilisateur, le heartbeat et les types d'événements.
5. L'enregistrement push n'est plus totalement absent : `PUT /notifications/preferences` accepte actuellement un `pushToken` unique. Ce contrat reste insuffisant pour gérer proprement plusieurs appareils par utilisateur.
6. Les lignes précédentes qui classent encore ces cinq éléments comme « endpoint backend absent » doivent être lues comme un historique, pas comme l'état courant.

### État des 60 fonctions API déjà écrites dans le mobile

| Module mobile | Fonctions | Couverture backend constatée | Adaptations obligatoires avant usage réel |
|---|---:|---|---|
| `auth.api.ts` | 4 | Équivalents backend présents | `me` utilise le mauvais verbe; réponse `{ token, user }` incompatible avec `{ accessToken, refreshToken, tokenType, expiresIn, user }`; aucun refresh mobile |
| `chat.api.ts` | 5 | Équivalents REST présents | Envoi et lecture ne respectent pas le contrat conversation/message; DTO, pagination et types d'identifiants à confirmer; temps réel mobile incompatible avec STOMP |
| `collections.api.ts` | 9 | Les 9 opérations principales existent | Confirmer UUID/entier, `assetId` contre `place_id`, visibilité `friends`, enveloppes et prise en charge de `note`/`is_priority` |
| `feed.api.ts` | 6 | Feed et détail de post présents; interactions présentes ailleurs | Like/save utilisent de mauvais verbes et une sémantique différente; paramètres du feed non documentés |
| `notifications.api.ts` | 6 | Les 6 besoins sont maintenant présents | Verbes de lecture incorrects, UUID attendu, pagination `items/page/size/hasNext` non gérée, compteur sans enveloppe `data` |
| `places.api.ts` | 2 | Détail présent | La liste générique appelée par le mobile n'existe pas sous cette forme; choisir proximité, catalogue, région/ville ou discovery; paramètres incompatibles |
| `post.api.ts` | 4 | Équivalents média/post/story présents | Contrat multipart absent; identifiants à confirmer; création de post = brouillon puis publication séparée |
| `profile.api.ts` | 6 | Données généralement composables depuis plusieurs services | Les 6 alias mobiles n'existent pas; remplacer par posts personnels, saves, events/me, bookings/me, reviews utilisateur et stats sociales |
| `social.api.ts` | 11 | 8 opérations ont un équivalent direct | UUID public requis; filtres de recherche différents; friend-suggestions par contacts et settings sociaux restent absents |
| `badges.api.ts` | 4 | Un équivalent partiel pour les badges personnels | Catalogue, détail et statistiques riches non couverts par le contrat publié |
| `story.api.ts` | 3 | Les 3 opérations existent | Confirmer identifiants, DTO et enveloppes; la Gateway est annoncée corrigée mais doit être testée après authentification |

**Conclusion sur ces modules : aucune des 11 couches API ne peut être considérée comme directement compatible sans adaptation.** Cela ne signifie pas que les 60 besoins exigent 60 nouveaux endpoints : une grande partie possède déjà un équivalent backend, mais le client doit être réaligné.

### Endpoints encore réellement manquants pour des interfaces mobiles existantes

Cette liste retire les éléments désormais implémentés par le backend et conserve seulement les besoins sans couverture claire.

| Domaine | Besoin affiché dans l'application | Endpoint/contrat encore absent |
|---|---|---|
| Compte | Désactivation temporaire puis réactivation à la connexion | Commandes de désactivation/réactivation distinctes de la suppression définitive |
| Sécurité | Changement de mot de passe authentifié | Endpoint exigeant ancien mot de passe, nouveau mot de passe et révocation éventuelle des sessions |
| Sécurité | Liste des sessions/appareils et révocation d'une session | Ressources de session utilisateur |
| Sécurité | Activation, confirmation, désactivation et récupération 2FA | Workflow 2FA complet et codes de récupération |
| Sécurité | Vérification/modification du téléphone | Workflow téléphone distinct de la vérification email |
| Confidentialité | Visibilité du profil, statut en ligne, autorisations message/tag, recherche et suggestions | DTO confirmé pour lire et enregistrer ces réglages; `/users/me/preferences` n'est pas suffisamment détaillé |
| Social | Suggestions issues des contacts du téléphone | Import haché/consenti des contacts et endpoint de suggestions |
| Social | Paramètres sociaux lus et modifiés par `socialApi` | GET/PUT de paramètres sociaux ou intégration explicite dans les préférences utilisateur |
| Lieux | Suggestion de lieu par un utilisateur non partenaire | Commande de suggestion avec statut de validation; la création actuelle est réservée aux rôles privilégiés |
| Favoris | Favori de lieu explicite et écran « Mes favoris » | API dédiée ou décision contractuelle d'utiliser une collection système |
| Collections | Modifier `note` et `is_priority` d'un élément déjà ajouté | Mise à jour de l'élément de collection; l'écran ne fait aujourd'hui qu'une modification locale |
| Événements | Participants, invitations, acceptation/refus et groupes « amis proches » | Endpoints de participants et d'invitations correspondant à l'écran de création |
| Événements/expériences | Enregistrer/retirer un événement ou une expérience | API de sauvegarde ou règle documentée d'utilisation des collections/favoris |
| Offres partenaires | CRUD, soumission, validation, publication et archivage d'une offre | Aucun service d'offres/promotions n'est inventorié |
| Messagerie | Supprimer/archiver une conversation | Seule la suppression d'un message individuel est documentée |
| Messagerie | Épingler/désépingler un message | Commandes et liste des messages épinglés |
| Messagerie | Mettre une conversation en sourdine | Préférence synchronisée côté serveur |
| Messagerie | Recherche serveur et export d'une conversation | La recherche actuelle ne porte que sur les messages déjà chargés et l'export est simulé |
| Appels | Appels audio/vidéo | Signalisation, présence, historique, STUN/TURN et contrat WebRTC |
| Commentaires | Réponses imbriquées, like/unlike de commentaire | Les types et l'interface les prévoient, mais l'inventaire ne publie que création/édition/suppression de commentaire |
| Gamification | Catalogue/détail des badges, statistiques riches et leaderboard | Les écrans dépassent le contrat `/me/badges`, `/me/xp` et `/me/passport` actuellement décrit |
| Notifications push | Plusieurs appareils par utilisateur | Enregistrer, actualiser et révoquer chaque appareil séparément; `pushToken` unique dans preferences est une solution transitoire |
| Paiement mobile | Challenge/confirmation utilisateur du moyen de paiement réel | Le document indique une autorisation déclenchée par la réservation, mais le fournisseur est simulé et aucun flux mobile réel n'est défini |

### Endpoints backend existants mais encore non branchés aux interfaces correspondantes

Le problème est ici côté mobile, pas une absence backend :

- confirmation et renvoi de vérification email;
- mot de passe oublié et réinitialisation;
- OAuth Google et Apple;
- refresh token;
- profil utilisateur courant, modification du profil, préférences et suppression du compte;
- régions, villes, quartiers, catégories, catalogue et discovery;
- commentaires, partages, résumés d'interaction, avis et check-ins;
- création, modification, publication, archivage et visibilité des posts;
- événements, inscription/désinscription et événements par lieu;
- profil/documentation/validation partenaire;
- disponibilités, réservations, annulation, historique et management partenaire;
- paiements en lecture;
- gestion des membres, modification et suppression de messages;
- préférences de notifications;
- XP, passeport, séries, récompenses, missions, parrainage et recommandations;
- modération utilisateur et trust score;
- analytics partenaire.

Les écrans correspondants utilisent actuellement des mocks, des `TODO`, des `Alert` de démonstration ou uniquement un état Zustand/local.

### Incohérences d'endpoints et de contrats encore actives

| Sujet | Mobile actuel | Backend documenté | Conséquence |
|---|---|---|---|
| Base HTTP | Ajoute automatiquement `/api` à `EXPO_PUBLIC_API_BASE_URL` | Contrats publiés sous une version d'API | Toutes les URL finales doivent être recalculées une seule fois, sans double préfixe |
| Utilisateur courant auth | `POST` | `GET` | Requête rejetée ou non routée |
| Likes/favoris de post | `POST` pour créer | `PUT` idempotent | Mutation incompatible |
| Notifications lues | `PUT` | `POST` | Mutation incompatible |
| Auth response | `{ token, user }` | `{ accessToken, refreshToken, tokenType, expiresIn, user }` | Login/register cassent dès la désactivation des mocks |
| Refresh | Non stocké/non appelé | Rotation obligatoire et détection de rejeu | Expiration du token provoque une déconnexion et peut supprimer des préférences locales |
| Identité utilisateur | `id: number` utilisé partout | ID auth numérique, mais UUID public pour user-service/social | Profil, follow, block et listes sociales utilisent le mauvais identifiant |
| Nommage DTO | Majoritairement `snake_case` | Exemple auth en `camelCase` | Mapping explicite indispensable |
| Notification ID | `number` | UUID | Lecture/suppression incompatibles |
| Notification list | Attend `{ data: Notification[] }` | Retourne `{ page, size, hasNext, items }` | Les listes ne peuvent pas se rendre correctement |
| Notification count | Attend `{ data: { count } }` | Retourne `{ count }` | Extraction du compteur incorrecte |
| Pagination générale | `data/meta/links`, parfois curseur | Notifications en page; discovery en `page/size`; autres schémas non fournis | Un seul type `PaginatedResponse` ne suffit pas |
| Feed | Envoie `cursor`, `interests`, `region_id` | Paramètres non décrits | Filtres potentiellement ignorés ou rejetés |
| Lieux | Envoie `city`, `search`, `radius_km` | Proximité décrite avec `lat`, `lng`, `radius`; discovery avec `radiusKm` | Noms et unité du rayon incohérents |
| Upload | Part `file`, `Content-Type` forcé | Part, limites et réponse non documentées | Risque de boundary incorrecte et de 400/415 |
| Post | Le bouton « publier » suppose une création finale | Le backend crée un brouillon puis exige une publication | Post créé mais non visible |
| Message envoyé | Body avec `conversation_id`, `type`, `media_url` | Conversation portée par la ressource; DTO non publié | Body probablement incompatible |
| Lecture message | Seulement `conversationId` | Nécessite aussi `messageId` | Accusé de lecture impossible |
| Temps réel | Client Pusher/Reverb brut | STOMP `/ws/messaging`, destination `/user/queue/messaging` | Aucun message temps réel reçu sans remplacement du client |
| Login UI | Placeholder « email ou téléphone », type et validation `email` | Contrat documenté avec email/password | Connexion par téléphone promise par l'UI mais non supportée par le client |
| Inscription partenaire | Un formulaire monolithique simulé | Création auth, profil partenaire, documents puis soumission sont séparés | Workflow, rollback et statuts à concevoir |
| Suppression compte | UI demande le mot de passe puis appelle seulement `logout` | `DELETE /users/me` documenté sans contrat de confirmation | L'interface affiche un succès sans supprimer le compte |
| Accès public | README annonce plusieurs lectures publiques | Toutes les lectures publiques testées localement ont répondu `401` | Impossible de valider les parcours anonymes sur l'instance actuelle |
| Documentation notifications | Une note affirme encore que trois endpoints sont absents | Le tableau et les contrats suivants les déclarent présents | Contradiction documentaire à ne pas propager dans le code |

### Informations encore manquantes pour configurer correctement la consommation

#### Bloquants d'environnement

1. **URL Gateway réellement utilisable** pour développement, staging et production. La valeur par défaut `https://api.yeyamo.com` ne possède actuellement aucun enregistrement DNS.
2. **URL WebSocket STOMP publique**. `EXPO_PUBLIC_MESSAGING_WS_URL` est documentée mais absente de `src/config/env.ts`; le port direct local `8104` n'était pas accessible lors du contrôle.
3. **Fichier d'environnement mobile**. Aucun `.env`, `.env.local` ou `.env.<environnement>.local` n'est présent; les mocks restent donc actifs par défaut.
4. **Comptes de test non sensibles** pour les rôles `USER` et `PARTNER`, avec états email vérifié/non vérifié et partenaire en attente/validé.
5. **Disponibilité cohérente des services**. Lors du contrôle local, `auth-service` (8082), `catalog-service` (8088), `content-service` (8090), `admin-service` (8096) et `messaging-service` (8104) n'étaient pas joignables sur leurs ports documentés.
6. **Politique d'accès public réellement déployée**. Les GET de `regions`, `categories`, `events/upcoming` et `catalog/assets` ont tous retourné `401` via la Gateway locale malgré la matrice publique annoncée.

#### Bloquants de contrat

1. OpenAPI agrégée ou fichiers OpenAPI versionnés et accessibles au mobile/CI; `/v3/api-docs` n'a pas pu être récupéré anonymement de manière exploitable lors du contrôle local.
2. DTO complets requête/réponse pour tous les domaines, pas seulement auth, notifications et enveloppe STOMP.
3. Format d'identifiant de chaque ressource : auth ID, profil UUID, post, média, story, lieu/asset, collection, événement, réservation, notification, conversation et message.
4. Formats de pagination par service et règle commune éventuelle.
5. Enveloppe d'erreur stable : code machine, message, erreurs par champ, timestamp, correlation ID et statuts possibles.
6. Règles exactes des rôles, propriété des ressources et état de validation partenaire.
7. Contrat multipart : noms des parts, types MIME, tailles, durée vidéo, checksum éventuel, traitement asynchrone et URLs CDN.
8. Durée de validité et portée d'une `Idempotency-Key`, réponse en cas de rejeu et stratégie de retry réseau.
9. Rate limits REST, en-têtes de retry, timeout recommandé et politique de circuit breaker visible par le mobile.
10. Redirect URI, schemes et identifiants clients publics Google/Apple; aucun secret OAuth ne doit être placé dans `EXPO_PUBLIC_*`.
11. Stratégie push multi-device, retrait du token à la déconnexion et distinction développement/production APNs/FCM/Expo.
12. Contrat STOMP complémentaire : STOMP natif ou SockJS, version, ACK mode, receipts, taille maximale, compression et rattrapage REST après déconnexion.
13. Fournisseur de paiement réel, statuts, challenge utilisateur, devise, montants en unité mineure et reprise après interruption.
14. Formats de date/timezone, langue des messages, devise et normalisation des numéros de téléphone.
15. Politique de cache, ETag et invalidation pour catalogue, feed, profils et médias.

### Contrôle d'exécution effectué pendant cet audit

| Vérification | Résultat |
|---|---|
| TypeScript mobile | Réussi, 0 erreur |
| Fichiers `.env*` | Aucun |
| DNS `api.yeyamo.com` | Inexistant (`NXDOMAIN`) |
| Gateway locale `127.0.0.1:8083` | Port accessible pendant le contrôle |
| WebSocket direct `127.0.0.1:8104` | Non accessible |
| Lectures publiques via Gateway | `401` pour régions, catégories, événements à venir et catalogue |
| Endpoint protégé `/auth/me` sans JWT | `401`, comportement attendu |
| OpenAPI locale | Non récupérable anonymement de façon exploitable sur l'instance contrôlée |

Ces résultats décrivent l'instance locale au moment du contrôle; ils ne remplacent pas un test de staging et peuvent nécessiter un redémarrage des services après changement de configuration.

### Verdict de préparation à la consommation

**La consommation complète ne doit pas être lancée en désactivant simplement les mocks.** Elle échouerait dès l'authentification, les réponses ne correspondent pas aux types mobiles, le domaine par défaut est inexistant et le temps réel utilise le mauvais protocole.

**Une consommation progressive peut commencer**, mais uniquement sous forme de tranche d'intégration contrôlée, après fourniture d'une URL Gateway fonctionnelle et d'un compte de test :

1. réaligner le client HTTP et les variables d'environnement;
2. implémenter login, stockage access/refresh token, rotation et `GET auth/me`;
3. créer les adaptateurs d'identité numérique/UUID et d'erreurs;
4. valider une lecture authentifiée simple et les lectures publiques;
5. brancher notifications avec leur pagination réelle;
6. intégrer feed/détail de lieu avec DTO confirmés;
7. poursuivre domaine par domaine en maintenant les mocks pour les parcours non migrés;
8. traiter STOMP séparément après les flux REST.

**Décision finale : prêt pour commencer le travail d'intégration, mais pas prêt pour activer la consommation réelle globale ni pour une recette de bout en bout.**

---

## Prompt à transmettre au backend avant l'intégration mobile

Le prompt ci-dessous peut être transmis tel quel à l'équipe backend ou à un agent travaillant dans le dépôt backend. Son objectif est d'obtenir un contrat vérifiable et toutes les configurations publiques nécessaires, sans exposer de secret dans le client Expo.

```text
Tu travailles sur le backend Spring de la plateforme Yeyamo. Ta mission est de rendre le backend réellement intégrable par le projet mobile `yeyamo-mobile`, puis de fournir les contrats et configurations exacts permettant au client Expo/React Native de consommer les API sans inventer de route, de DTO ou de comportement.

CONTEXTE MOBILE À RESPECTER

- Application : Expo SDK 54, React Native 0.81, Expo Router, Axios, TanStack Query, Zustand et expo-secure-store.
- Le mobile contient 97 écrans, 11 modules API et 60 fonctions réseau, mais de nombreux parcours utilisent encore des mocks.
- Deux connexions de démonstration doivent impérativement continuer à fonctionner sans backend :
  1. utilisateur démo, alimenté par `MOCK_USER` et les mockData consommateur;
  2. partenaire démo, alimenté par `MOCK_PARTNER_USER` et les mockData partenaire.
- Ne demande jamais de supprimer ces deux modes démo. L'intégration réelle doit coexister avec eux et être activée par configuration ou par action explicite de l'utilisateur.
- Le mobile utilise actuellement des champs majoritairement en snake_case, des identifiants souvent numériques et une pagination `data/meta/links`; le backend documenté utilise aussi des UUID, du camelCase et plusieurs formes de pagination. Ces différences doivent être décrites précisément.
- Le client temps réel actuel imite Pusher/Reverb, alors que le backend annonce STOMP. Le contrat STOMP doit être complet pour permettre son remplacement.

RÈGLES IMPÉRATIVES

1. Audite le code backend réel : contrôleurs, DTO, validation, sécurité, Gateway, configuration cloud, WebSocket, événements, migrations et tests. Ne réponds pas à partir d'une architecture théorique.
2. N'invente aucun endpoint. Pour chaque opération annoncée, indique le contrôleur, la méthode, le service propriétaire et le test qui prouvent son existence.
3. Si une fonctionnalité demandée n'existe pas, marque-la `ABSENTE`, propose le contrat minimal nécessaire et implémente-la seulement si tu as l'autorisation de modifier le backend.
4. Ne déclare jamais une fonctionnalité « prête » uniquement parce que le code compile. Démarre les services concernés et effectue des tests HTTP/WebSocket représentatifs.
5. Ne place aucun secret, mot de passe, secret OAuth, clé SMTP, clé JWT, clé fournisseur de cartes, clé privée, credential TURN permanent ou signature webhook dans README, OpenAPI, `.env.example` public ou réponse de conversation.
6. Les variables destinées au mobile et préfixées `EXPO_PUBLIC_` sont publiques. Elles ne peuvent contenir que des URL, identifiants clients publics et options non sensibles.
7. Tous les secrets restent côté serveur dans un gestionnaire de secrets ou dans des variables d'environnement backend non versionnées.
8. Ne change pas silencieusement une route, un verbe, un statut HTTP ou un DTO déjà publié. Signale toute rupture et fournis une stratégie de migration/versionnement.
9. La Gateway doit être le point d'entrée REST normal. Si une capacité ne peut pas traverser la Gateway, fournis une URL publique séparée, justifie-la et documente sa sécurité.
10. Les comptes et tokens de test doivent être transmis par un canal sécurisé, jamais écrits dans la documentation versionnée.

LIVRABLES OBLIGATOIRES

A. SPÉCIFICATIONS OPENAPI

- Fournis une OpenAPI 3.0/3.1 agrégée de la Gateway ou une spécification versionnée par microservice.
- Donne l'URL réelle de chaque `/v3/api-docs` et `/swagger-ui.html` en développement/staging.
- L'OpenAPI doit inclure : sécurité Bearer, rôles, paramètres, validations, exemples, tous les statuts, erreurs, pagination, multipart et en-têtes d'idempotence/corrélation.
- Vérifie que le nombre d'opérations OpenAPI correspond aux contrôleurs réellement déployés.
- Fournis une commande CI qui valide l'OpenAPI et détecte les breaking changes.

B. CONFIGURATION MOBILE PUBLIQUE

Fournis un fichier `.env.mobile.example` sans secret avec des valeurs réelles ou clairement marquées à remplacer :

EXPO_PUBLIC_API_BASE_URL=<origine HTTPS publique de la Gateway>
EXPO_PUBLIC_MESSAGING_WS_URL=<URL WSS publique STOMP>
EXPO_PUBLIC_USE_MOCKS=false
EXPO_PUBLIC_APP_ENV=development|staging|production
EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID=<identifiant public si requis par Expo AuthSession>
EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID=<identifiant public si requis>
EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID=<identifiant public si requis>
EXPO_PUBLIC_MAP_PROVIDER=<provider retenu>
EXPO_PUBLIC_MAP_STYLE_URL=<URL publique éventuelle, sans secret>
EXPO_PUBLIC_ROUTING_BASE_URL=<URL publique OSRM ou proxy Yeyamo>

Pour chaque variable, indique : environnement, propriétaire, valeur attendue, caractère public/sensible, plateforme concernée et procédure de rotation. N'ajoute pas une variable si elle n'est pas nécessaire.

C. MATRICE COMPLÈTE DES ENDPOINTS MOBILES

Pour chaque endpoint que le mobile peut consommer, fournis un tableau avec :

- domaine et cas d'usage mobile;
- méthode HTTP et chemin complet via Gateway;
- service et contrôleur propriétaires;
- public ou authentifié;
- rôles autorisés;
- règle de propriété de la ressource;
- type de chaque identifiant : entier, UUID, slug ou autre;
- query params et valeurs par défaut;
- body exact;
- réponse exacte;
- statuts succès et erreurs;
- pagination;
- rate limit;
- cache/ETag;
- Idempotency-Key requise ou non;
- X-Correlation-Id supporté ou non;
- état : `PRÊT`, `PARTIEL`, `ABSENT`, `BACKEND-ONLY`;
- test automatisé et test runtime associés.

D. CONTRAT D'ERREUR TRANSVERSAL

Définis une enveloppe unique ou documente chaque exception :

- `code` machine stable;
- `message` destiné à l'utilisateur ou clé i18n;
- `fieldErrors` par champ;
- `status` HTTP;
- `timestamp` ISO 8601 UTC;
- `correlationId`;
- `retryable`;
- `retryAfter` si applicable.

Fournis des exemples pour 400, 401, 403, 404, 409, 422, 429, 500 et 503. Précise quand un 404 masque volontairement une ressource non possédée.

AUTHENTIFICATION ET IDENTITÉ

1. Confirme le DTO exact de register, login, OAuth et refresh : accessToken, refreshToken, tokenType, expiresIn et user.
2. Confirme la rotation du refresh token, la détection de rejeu, la révocation et le comportement en concurrence lorsque plusieurs requêtes reçoivent 401.
3. Documente la portée de logout : session courante ou toutes les sessions.
4. Documente tous les claims JWT : subject, auth user ID, profile UUID, rôles, permissions, issuer, audience, issuedAt et expiration.
5. Explique clairement la relation entre l'identifiant auth numérique et l'UUID public user-service. Le mobile ne doit pas deviner lequel envoyer.
6. Fournis les DTO complets de l'utilisateur authentifié et du profil public.
7. Confirme si la connexion accepte seulement l'email ou également le téléphone. Si le téléphone est accepté, fournis format E.164, validation, ambiguïtés et DTO.
8. Fournis des comptes de test USER/PARTNER par canal sécurisé et précise leurs états de vérification/validation.

EMAIL, MOT DE PASSE ET VÉRIFICATION

Le backend doit couvrir et documenter :

- demande et confirmation de vérification email;
- renvoi du code avec cooldown et limite de tentatives;
- mot de passe oublié et reset par code/lien;
- changement de mot de passe en session avec ancien mot de passe;
- email de confirmation après changement de mot de passe;
- alerte de sécurité après changement sensible;
- révocation configurable des autres sessions après changement;
- expiration, usage unique et stockage haché des codes/tokens;
- protection anti-énumération de comptes;
- rate limits par IP et par compte;
- deep links mobiles de retour vers le scheme `yeyamo://`;
- templates français et anglais;
- fournisseur email, adresse From/Reply-To, SPF/DKIM/DMARC et environnement sandbox;
- boîte de test ou mécanisme MailHog/Mailpit en local.

Les identifiants SMTP/API du fournisseur email restent exclusivement côté backend. Le mobile ne doit recevoir aucun secret d'envoi de mail.

OAUTH GOOGLE ET APPLE

1. Confirme le flux choisi : Authorization Code + PKCE recommandé pour le mobile.
2. Fournis les client IDs publics par plateforme, redirect URI exactes et schemes autorisés.
3. Valide côté backend l'issuer, audience, nonce, expiration et signature du token fournisseur.
4. Décris la liaison de compte, les collisions d'email, compte existant, compte suspendu et email privé Apple.
5. Fournis le DTO envoyé par le mobile et la réponse Yeyamo finale.
6. N'expose jamais le client secret Google/Apple au mobile.
7. Ajoute des tests pour succès, nonce invalide, audience invalide, token expiré et compte déjà lié.

PROFIL, SOCIAL ET PRÉFÉRENCES

- Fournis les DTO de profil, modification, préférences, confidentialité et suppression.
- Confirme les endpoints pour suivre, ne plus suivre, retirer un abonné, bloquer, débloquer, lister les bloqués, suggestions, recherche et activité.
- Fournis un contrat pour les paramètres sociaux ou confirme leur intégration dans `/users/me/preferences`.
- Décide si les suggestions basées sur les contacts sont supportées. Si oui, définis consentement, hachage côté appareil, rétention et suppression RGPD.
- Ajoute les opérations de désactivation/réactivation, sessions actives, révocation de session, 2FA et téléphone si elles doivent rester dans l'interface mobile.

CATALOGUE, LIEUX, DISCOVERY ET CARTE

1. Clarifie la différence entre `place-service` et les assets de `catalog-service` et indique l'identifiant canonique utilisé par posts, collections, avis, événements et réservations.
2. Fournis les DTO de région, ville, quartier, catégorie, lieu/asset, équipements, horaires, médias, coordonnées et statut de validation.
3. Définis les paramètres exacts de proximité et discovery : lat, lng, rayon, unité, bornes, page/size, tri et filtres.
4. Fournis ou confirme un endpoint de suggestion de lieu pour un utilisateur standard, distinct de la création partenaire/admin.
5. Décide comment modéliser les lieux favoris : endpoint dédié ou collection système.
6. Pour la carte, précise le fournisseur de tuiles, géocodage, géocodage inverse et itinéraires.
7. Si OSRM reste utilisé, fournis l'URL autorisée, les limites, la politique d'usage et un proxy backend si nécessaire. Le serveur public de démonstration OSRM ne doit pas être considéré comme un SLA de production.
8. Si une clé cartographique est nécessaire, utilise une clé publique restreinte par bundle/package lorsque le fournisseur le permet; conserve toute clé secrète côté backend.
9. Documente les coordonnées WGS84, précision, ordre latitude/longitude et comportement sans permission de localisation.
10. Ajoute des tests sur rayon nul/maximal, coordonnées invalides, pagination et absence de résultat.

CONTENU, MÉDIAS, STORIES ET INTERACTIONS

- Documente le workflow complet : upload média, création brouillon, modification, publication, visibilité, archivage et suppression.
- Fournis le contrat multipart exact : noms des parts, fichiers multiples, MIME, taille, dimensions, durée, checksum, statuts 413/415 et réponse.
- Confirme si les IDs média/post/story sont UUID ou numériques.
- Fournis les DTO feed, post, story, interaction summary, comments, shares, saves, reviews et check-ins.
- Documente la pagination réelle du feed et les paramètres cursor/interests/region.
- Décide si réponses imbriquées et likes de commentaires sont supportés; sinon demande au mobile de masquer ces actions.
- Fournis les URLs CDN, règles d'expiration, thumbnails et traitement vidéo asynchrone.

ÉVÉNEMENTS, EXPÉRIENCES, RÉSERVATIONS ET PAIEMENTS

- Fournis les DTO complets des événements et expériences, leurs statuts, visibilité, participants et organisateur.
- Confirme liste personnelle, inscription, désinscription, participants, invitations et amis proches.
- Décide comment sauvegarder un événement ou une expérience.
- Documente disponibilités, slots, création/annulation/finalisation et historique des réservations.
- Définis la génération, durée et réutilisation d'Idempotency-Key.
- Fournis montants en unité mineure, devise, taxes/frais, fournisseur de paiement, statuts et reprise après interruption.
- Le paiement simulé ne peut pas être marqué production-ready. Fournis le flux réel et le challenge mobile éventuel.
- Les webhooks restent backend-only et doivent vérifier timestamp, signature, rejeu et idempotence.

PARTENAIRES ET OFFRES

- Documente le workflow réel : auth, création profil partenaire, upload des documents, soumission, validation/rejet et corrections.
- Fournis statuts et raisons de rejet lisibles par le mobile.
- Documente propriété et droits sur lieux, événements, réservations, avis et analytics.
- L'interface mobile contient la création d'offres/promotions. Si aucun service n'existe, marque la fonctionnalité `ABSENTE` et propose CRUD, soumission, validation, publication et archivage.
- Ne fais pas passer une offre simulée pour une fonctionnalité backend disponible.

NOTIFICATIONS ET PUSH

- Confirme la pagination `page/size/hasNext/items`, UUID des notifications, compteur et verbes de lecture/suppression.
- Fournis le DTO des préférences.
- Remplace à terme le `pushToken` unique par des ressources appareil : deviceId, platform, provider, token, appVersion, locale, enabled, lastSeenAt.
- Documente création, rotation et suppression du token Expo/APNs/FCM, notamment au logout et à la réinstallation.
- Précise quelles notifications sont envoyées par push, email, in-app et STOMP.
- Fournis les deep links et payloads versionnés associés.

MESSAGERIE TEMPS RÉEL

1. Confirme STOMP natif ou SockJS, versions et sous-protocoles WebSocket.
2. Fournis l'URL WSS publique réelle; le port 8104 direct local n'est pas une configuration production.
3. Documente CONNECT avec Bearer, destination `/user/queue/messaging`, heartbeat, ACK mode, receipts, limites et reconnexion.
4. Fournis les DTO REST des conversations, membres, messages, pièces jointes et lecture avec messageId.
5. Fournis les schémas complets de tous les événements `messaging.*`, eventId, version, ordre, déduplication et correlationId.
6. Décris le rattrapage REST après déconnexion et la reconnexion après rotation JWT.
7. Décide si suppression/archivage de conversation, messages épinglés, mute, recherche et export sont supportés.
8. Ajoute des tests multi-utilisateurs empêchant l'accès à une conversation non autorisée.

APPELS AUDIO ET VIDÉO

Les écrans d'appel existent dans le mobile, mais aucun contrat complet n'est actuellement publié. Fournis :

- signalisation via STOMP/WebSocket : invite, ringing, accept, reject, busy, cancel, hangup et timeout;
- callId UUID, participants, type audio/vidéo, timestamps et raison de fin;
- présence et gestion multi-device;
- serveurs STUN/TURN, génération de credentials TURN temporaires côté backend et durée de validité;
- stratégie WebRTC, ICE restart, réseau mobile, passage arrière-plan et permissions;
- notification push pour appel entrant lorsque l'application est suspendue;
- politique d'historique d'appels et confidentialité;
- comportement si le service d'appel n'est pas disponible;
- tests à deux utilisateurs sur Android/iOS et scénarios refus, absence de réponse, coupure réseau et token expiré.

Aucun secret TURN permanent ne doit être placé dans le `.env` Expo.

GAMIFICATION, MISSIONS, PARRAINAGE ET MODÉRATION

- Fournis les DTO de XP, badges, passport, streaks, rewards, missions et referral.
- Décide si le catalogue des badges, détail, progression riche et leaderboard visibles dans le mobile sont supportés.
- Fournis les règles de calcul ou au minimum les champs de lecture nécessaires; le mobile ne doit pas recalculer une vérité métier serveur.
- Documente report, suivi d'un report, trust score et actions autorisées à un utilisateur standard.

SÉCURITÉ, CORS ET EXPLOITATION

- Vérifie la matrice public/authentifié via la Gateway. Les lectures regions, categories, events/upcoming et catalog/assets ont retourné 401 lors du dernier test local alors qu'elles sont documentées publiques.
- Fournis tests de non-régression de sécurité pour chaque famille publique/protégée.
- Configure CORS pour Expo Web avec origines explicites; n'utilise pas `*` avec credentials.
- Fournis TLS valide, health/readiness endpoints, métriques, logs avec correlationId et traces interservices.
- Documente 429/503, Retry-After, timeouts et circuit breakers.
- Masque PII, tokens, codes de vérification et documents dans les logs.
- Fournis politique de suppression/export des données et rétention.

PREUVES D'EXÉCUTION À FOURNIR

1. Commandes exactes utilisées pour démarrer la Gateway et tous les services requis.
2. Résultat des tests unitaires/intégration avec nombre de tests par service.
3. Smoke tests via la Gateway pour au minimum : public region/category/event/catalog, register/login/refresh/me/logout, profil, feed, notification, upload, story, collection, événement et réservation.
4. Test STOMP réel avec deux utilisateurs et preuve d'un message envoyé, reçu, édité, supprimé et lu.
5. Test d'email local/sandbox : vérification, reset et confirmation de changement de mot de passe.
6. Test OAuth avec clients de développement valides sans publier les secrets.
7. Test push sur un appareil réel.
8. Test carte/proximité/itinéraire avec les limites du fournisseur.
9. Test d'appel audio puis vidéo entre deux appareils si la fonction est déclarée prête.
10. Tableau final des échecs connus, fonctions simulées et travaux restants.

FORMAT DE TA RÉPONSE OBLIGATOIRE

Réponds dans cet ordre :

1. Résumé exécutif et verdict `PRÊT / PARTIEL / BLOQUÉ`.
2. Version/commit backend audité.
3. Services démarrés et URLs réellement accessibles.
4. `.env.mobile.example` sans secret.
5. Liens ou fichiers OpenAPI.
6. Matrice des endpoints mobiles.
7. Contrats auth/email/OAuth.
8. Contrats map/média/push/STOMP/appels/paiement.
9. Endpoints nouvellement implémentés.
10. Endpoints encore absents.
11. Incohérences ou breaking changes.
12. Tests exécutés et résultats.
13. Secrets/configurations à transmettre par canal sécurisé.
14. Checklist de démarrage de l'intégration mobile.

CRITÈRES DE SORTIE

Ne conclus `PRÊT` que si :

- une URL Gateway joignable est fournie;
- auth-service et les services de la première tranche sont démarrés;
- OpenAPI et DTO sont accessibles;
- login/refresh/me fonctionnent réellement;
- les lectures publiques respectent la matrice annoncée;
- un compte USER et un compte PARTNER de test sont disponibles par canal sécurisé;
- les contrats d'identifiants, erreurs et pagination sont figés;
- l'URL STOMP publique est fournie ou la messagerie temps réel est explicitement exclue de la première tranche;
- aucun secret n'est requis dans une variable EXPO_PUBLIC;
- les fonctions encore simulées sont explicitement listées.

Si un seul de ces critères manque, conclus `PARTIEL` ou `BLOQUÉ` et indique exactement ce qui empêche l'intégration. Ne masque pas un manque de contrat derrière une valeur supposée ou un exemple fictif.
```

---

## Mise à jour du 24 juillet 2026 — vérification des API signalées absentes

Cette section est le bilan de référence après comparaison du rapport frontend avec le code backend. Elle remplace les affirmations d'absence devenues obsolètes dans les sections historiques du rapport.

### Endpoints ajoutés

Tous les appels passent par `http://localhost:8083/api/v1` et nécessitent
`Authorization: Bearer <accessToken>`.

#### Compte et sessions (`auth-service`)

| Méthode | Chemin | Requête | Réponse |
|---|---|---|---|
| POST | `/auth/account/deactivate` | `{ "currentPassword": "..." }` | `204 No Content` |
| GET | `/auth/sessions` | aucune | tableau `{ id, expiresAt, revokedAt, active }` |
| DELETE | `/auth/sessions/{sessionId}` | aucune | `204 No Content`; `404 SESSION_NOT_FOUND` si la session n'appartient pas à l'utilisateur |

La réactivation n'utilise volontairement pas une route publique séparée: une connexion
`POST /auth/login` avec le bon mot de passe réactive un compte `INACTIVE`, puis émet
de nouveaux tokens. Un compte `PENDING` retourne maintenant explicitement
`403 EMAIL_NOT_VERIFIED` avant l'authentification, au lieu d'un `401` ambigu.
La désactivation et le changement de mot de passe révoquent tous les refresh tokens.
Un access token déjà émis reste cependant valable jusqu'à son expiration, car les JWT
sont stateless et il n'existe pas encore de denylist distribuée.

La liste représente des **sessions de refresh token**. Le schéma actuel ne conserve pas
encore le nom de l'appareil, l'adresse IP ni le user-agent.

#### Participants d'événement (`event-service`)

| Méthode | Chemin | Réponse |
|---|---|---|
| GET | `/events/{eventId}/participants?limit=100` | tableau `{ registrationId, userId, status, registeredAt }` |

Seules les inscriptions `CONFIRMED` sont retournées. `limit` vaut `100` par défaut et
est borné entre `1` et `200`. La route exige un JWT et retourne
`404 EVENT_NOT_FOUND` pour un événement inconnu.

#### Réponses et likes de commentaires (`interaction-service`)

Les réponses imbriquées étaient déjà prises en charge par
`POST /interactions/posts/{postId}/comments` avec le champ optionnel `parentId`.
Les routes manquantes de like sont maintenant:

| Méthode | Chemin | Réponse |
|---|---|---|
| PUT | `/interactions/comments/{commentId}/like` | `{ commentId, likeCount, liked: true }` |
| DELETE | `/interactions/comments/{commentId}/like` | `{ commentId, likeCount, liked: false }` |
| GET | `/interactions/comments/{commentId}/likes` | `{ commentId, likeCount, liked }` |

`PUT` et `DELETE` sont idempotents. Un commentaire absent ou supprimé retourne
`404 COMMENT_NOT_FOUND`. La migration Flyway `V3__create_comment_likes.sql` impose
une unicité `(comment_id, user_id)` et supprime les likes en cascade avec le commentaire.

#### Badges et classement (`gamification-service`)

| Méthode | Chemin | Réponse |
|---|---|---|
| GET | `/me/badges/catalog` | catalogue `{ code, name, description, earned, earnedAt }[]` |
| GET | `/me/badges/catalog/{code}` | détail d'un badge et état de gain de l'utilisateur |
| GET | `/me/badges/stats` | `{ earnedBadges, totalBadges, totalXp, level, rank }` |
| GET | `/me/leaderboard?limit=50` | `{ rank, userId, totalXp, level }[]`, maximum 100 |

Le catalogue contient les sept règles réellement attribuées par le service:
`FIRST_POST`, `EXPLORER`, `TRAVELER_5`, `SOCIAL_10`, `FIRST_BOOKING`,
`STREAK_7` et `LEVEL_5`. Le leaderboard ne fabrique ni nom, ni avatar: il retourne
le sujet utilisateur canonique; le front doit résoudre le profil via `user-service`.
La gateway a été étendue pour router `/api/v1/me/leaderboard/**`.

### État exhaustif des 23 besoins du rapport frontend

| # | Besoin signalé | État vérifié | Décision/contrat |
|---:|---|---|---|
| 1 | Désactivation/réactivation | **Implémenté** | désactivation authentifiée; réactivation à la connexion valide |
| 2 | Changement de mot de passe | **Déjà présent** | `PUT /auth/password`, ancien et nouveau mot de passe, révocation des refresh tokens |
| 3 | Sessions/appareils | **Partiel implémenté** | liste et révocation des sessions; métadonnées appareil/IP/user-agent encore absentes |
| 4 | Workflow 2FA | **Absent** | nécessite stockage des secrets, chiffrement, QR/TOTP, recovery codes et politique de challenge |
| 5 | Vérification/changement de téléphone | **Absent** | aucun fournisseur SMS ni contrat OTP téléphone n'est configuré |
| 6 | Confidentialité détaillée | **Partiel déjà présent** | `GET/PUT /users/settings` couvre visibilité, activité, abonnés, notifications, suggestions et messages inconnus; statut en ligne, tags et réglages de recherche restent à définir |
| 7 | Suggestions par contacts | **Absent** | `/users/suggestions` couvre les amis d'amis, pas l'import consenti et haché du carnet d'adresses |
| 8 | Paramètres sociaux | **Déjà présent** | `GET/PUT /users/settings` avec DTO `privacy`, `notifications`, `preferences` |
| 9 | Suggestion de lieu utilisateur | **Absent** | workflow de modération et modèle de brouillon à définir avant d'exposer une écriture publique |
| 10 | Favoris de lieux | **Absent comme API dédiée** | les collections existent, mais aucune collection système « favoris » n'est contractualisée |
| 11 | Note/priorité d'une collection | **Déjà présent** | `PATCH /collections/{collectionId}/places/{assetId}` |
| 12 | Participants/invitations/amis proches | **Partiel implémenté** | participants confirmés ajoutés; invitations, acceptation/refus et groupes proches restent absents |
| 13 | Sauvegarde événement/expérience | **Absent** | aucune règle canonique entre collections, favoris et événements n'est figée |
| 14 | Offres partenaires | **Absent** | aucun agrégat/service d'offre, cycle de validation ou schéma n'existe |
| 15 | Supprimer/archiver conversation | **Absent** | le modèle Cassandra actuel ne définit pas d'archivage par participant |
| 16 | Messages épinglés | **Absent** | schéma et règles d'autorisation à ajouter à la messagerie |
| 17 | Conversation en sourdine | **Absent** | préférence par participant non modélisée |
| 18 | Recherche/export conversation | **Absent** | index de recherche, pagination d'export et politique de confidentialité non définis |
| 19 | Appels audio/vidéo | **Absent** | nécessite signalisation, historique, présence et infrastructure STUN/TURN/WebRTC |
| 20 | Réponses/likes commentaire | **Implémenté** | `parentId` existait; like/unlike/statut ajoutés |
| 21 | Catalogue/stats/leaderboard badges | **Implémenté** | quatre routes `/me` ajoutées avec données persistées réelles |
| 22 | Push multi-appareil | **Absent** | le `pushToken` unique reste transitoire; registre d'appareils et révocation par appareil requis |
| 23 | Challenge de paiement mobile réel | **Absent** | le fournisseur de paiement est simulé; aucun SDK/provider ni webhook réel n'est configuré |

**Bilan:** les 23 lignes ne sont pas toutes implémentées. Six familles sont désormais
complètement couvertes (`1`, `2`, `8`, `11`, `20`, `21`), trois sont partiellement
couvertes (`3`, `6`, `12`) et quatorze restent absentes faute de contrat de domaine,
de schéma persistant ou de fournisseur externe. Aucun endpoint factice n'a été ajouté
pour masquer ces dépendances.

### Build et validation

- Suites et tests ciblés réussis: `auth-service` **10 tests**, `interaction-service`
  **27 tests**, `gamification-service` **15 tests**, `event-service` **5 tests**;
  aucun échec ni erreur.
- Validation ciblée des nouveaux comportements: **11 tests réussis**.
- JAR Spring Boot reconstruits pour `auth-service`, `event-service`,
  `interaction-service` et `gamification-service`.
- Images Docker locales reconstruites sans téléchargement externe à partir de
  `yeyamo-api-java21-runtime:local`.
- Services recréés avec la gateway afin de charger le routage du leaderboard.
- État Docker final des services modifiés et de la gateway:
  `auth-service`, `event-service`, `interaction-service`, `gamification-service`
  et `api-gateway` sont tous `healthy` et enregistrés dans Eureka.
- Smoke tests authentifiés via `http://localhost:8083`: login `200`, sessions
  `200`, révocation d'une session `204`, catalogue de badges `200` (7 entrées),
  statistiques `200`, leaderboard `200`; les UUID inconnus sur participants
  et likes retournent bien le `404` métier du service cible.
- Cycle compte vérifié sur l'instance reconstruite: désactivation `204`, puis
  connexion avec le bon mot de passe `200` et compte réactivé.
