# Matrice d’intégration YeYamo Admin

Date de vérification : 2026-07-29

## Règles de statut

- **FRONT_COMPLETE** : l’écran, ses états et mutations existent.
- **BACKEND_COMPLETE** : le contrôleur et le contrat sont présents.
- **CONNECTED** : le client frontend appelle la route réelle via `/api/backend`.
- **E2E_TESTED** : un scénario navigateur → Gateway → microservice a été exécuté avec une stack active.

## Matrice

| Route frontend | Feature | Méthode / endpoint backend principal | Microservice | Autorisation | Front | Backend | Connected | E2E |
|---|---|---|---|---|---|---|---|---|
| `/admin/users` | Platform Users | `GET /api/v1/admin/platform-users` | admin-service | SUPPORT/ADMIN/SUPER_ADMIN | YES | YES | YES | NO |
| `/admin/administrators` | Administrateurs | `GET/POST /api/v1/admin/users` | admin-service | SUPER_ADMIN | YES | YES | YES | NO |
| `/admin/partners` | Partenaires | `GET /api/v1/admin/partners` | partner-service | SUPPORT/ADMIN/SUPER_ADMIN | YES | YES | YES | NO |
| `/admin/partners/[id]` | KYC partenaire | `GET /api/v1/admin/partners/{id}`, `/kyc`, `/validation-history` | partner-service | ADMIN/SUPER_ADMIN | YES | YES | YES | NO |
| `/admin/places` | Lieux | `GET /api/v1/admin/places` | place-service | ADMIN/SUPER_ADMIN/EDITOR | YES | YES | YES | NO |
| `/admin/regions` | Régions | `/api/v1/regions` | place-service | ADMIN/SUPER_ADMIN/EDITOR | YES | YES | YES | NO |
| `/admin/cities` | Villes | `/api/v1/cities` | place-service | ADMIN/SUPER_ADMIN/EDITOR | YES | YES | YES | NO |
| `/admin/districts` | Districts | `/api/v1/districts` | place-service | ADMIN/SUPER_ADMIN/EDITOR | YES | YES | YES | NO |
| `/admin/place-categories` | Catégories | `/api/v1/categories` | place-service | ADMIN/SUPER_ADMIN/EDITOR | YES | YES | YES | NO |
| `/admin/catalog` | Catalogue | `/api/v1/catalog/assets` | catalog-service | EDITOR/ADMIN/SUPER_ADMIN | YES | YES | YES | NO |
| `/admin/culture` | Culture | `GET /api/v1/catalog/assets` avec classification | catalog-service | EDITOR/ADMIN/SUPER_ADMIN | YES | YES | YES | NO |
| `/admin/collections` | Collections | `/api/v1/collections` | catalog-service | EDITOR/ADMIN/SUPER_ADMIN | YES | YES | YES | NO |
| `/admin/catalog/imports` | Imports | `/api/v1/catalog/imports` | ingestion-service | EDITOR/ADMIN/SUPER_ADMIN | YES | YES | YES | NO |
| `/admin/events` | Événements | `GET /api/v1/admin/events` | event-service | EDITOR/ADMIN/SUPER_ADMIN | YES | YES | YES | NO |
| `/admin/events/[id]` | Billetterie événement | `/api/v1/admin/events/{id}/participants`, `/tickets`, `/ticketing-stats` | event-service/ticket-service | ADMIN/SUPER_ADMIN | PARTIAL | YES | PARTIAL | NO |
| `/admin/reservations` | Réservations | `GET /api/v1/booking-management/bookings` | booking-service | SUPPORT/ADMIN/SUPER_ADMIN | YES | YES | YES | NO |
| `/admin/reservations/[id]` | Réservation détail/actions | `GET/POST /api/v1/booking-management/bookings/{id}/...` | booking-service | SUPPORT/ADMIN/SUPER_ADMIN | YES | YES | YES | NO |
| `/admin/payments` | Paiements | `GET /api/v1/payments/admin` | payment-service | SUPPORT/ADMIN/SUPER_ADMIN | YES | YES | YES | NO |
| `/admin/payments/[id]` | Paiement/remboursement | `GET /api/v1/payments/admin/{id}`, `POST /api/v1/payments/{id}/refunds` | payment-service | ADMIN/SUPER_ADMIN | PARTIAL | YES | PARTIAL | NO |
| `/admin/promotions` | Promotions | `/api/v1/commerce/admin/promotions` | commerce-service | COMMERCIAL/ADMIN/SUPER_ADMIN | YES | YES | YES | NO |
| `/admin/commissions` | Commissions | `/api/v1/commerce/admin/commissions` | commerce-service | ADMIN/SUPER_ADMIN | YES | YES | YES | NO |
| `/admin/ledger` | Ledger | `/api/v1/commerce/admin/ledger/{partnerId}` | commerce-service | ADMIN/SUPER_ADMIN | YES | YES | YES | NO |
| `/admin/moderation` | Modération | `/api/v1/moderation/reports` | moderation-trust-service | MODERATOR/ADMIN/SUPER_ADMIN | YES | YES | YES | NO |
| `/admin/reviews` | Avis | `/api/v1/admin/reviews` | interaction-service | MODERATOR/ADMIN/SUPER_ADMIN | NO | YES | NO | NO |
| `/admin/comments` | Commentaires | `/api/v1/admin/comments` | interaction-service | MODERATOR/ADMIN/SUPER_ADMIN | NO | YES | NO | NO |
| `/admin/trust` | Trust | `/api/v1/trust` | moderation-trust-service | MODERATOR/ADMIN/SUPER_ADMIN | PARTIAL | YES | PARTIAL | NO |
| `/admin/gamification/*` | Gamification | `/api/v1/mission-management/**` | mission-reward-service | ADMIN/SUPER_ADMIN | PARTIAL | YES | PARTIAL | NO |
| `/admin/campaigns` | Campagnes | `/api/v1/admin/campaigns` | campaign-service | ADMIN/SUPER_ADMIN + scopes | YES | YES | YES | NO |
| `/admin/analytics/*` | Analytics | `/api/v1/analytics/**` | analytics-service | ADMIN/SUPER_ADMIN | YES | YES | YES | NO |
| `/admin/messages` | Support | `/api/v1/admin/support/conversations` | support-service | SUPPORT/ADMIN/SUPER_ADMIN | YES | YES | YES | NO |
| `/admin/newsletter` | Newsletter | `/api/v1/admin/newsletters` | notification-service | COMMERCIAL/ADMIN/SUPER_ADMIN | YES | YES | YES | NO |
| `/admin/search-discovery/*` | Search admin | `/api/v1/admin/search/**` | search-service | ADMIN/SUPER_ADMIN | NO | YES | NO | NO |
| `/admin/settings/general` | Settings | `/api/v1/admin/settings` | admin-service | SUPER_ADMIN | NO | YES | NO | NO |
| `/admin/settings/feature-flags` | Feature flags | `/api/v1/admin/feature-flags` | admin-service | SUPER_ADMIN | NO | YES | NO | NO |
| `/admin/notifications` | Notifications admin | `/api/v1/admin/notifications` | notification-service | rôles admin | YES | YES | YES | NO |
| `/admin/settings/audit-logs` | Audit | `/api/v1/admin/audit-logs` | admin-service | ADMIN/SUPER_ADMIN | PARTIAL | YES | YES | NO |

## Corrections réalisées

- Events utilise désormais `/api/v1/admin/events`, le DTO enrichi et la pagination Spring.
- Partners utilise `/api/v1/admin/partners`; le mapper conserve les composants existants sans inventer de données.
- Réservations utilise les routes administrateur pour le détail et l’annulation.
- Notifications utilise `/api/v1/admin/notifications`, `PATCH read/unread` et le modèle `resourceType/resourceId`.
- Support consomme réellement support-service avec liste, détail, réponse, note interne, statut et priorité.
- Newsletter consomme réellement notification-service avec CRUD, envoi, pause et annulation.
- Les erreurs continuent de passer par `ApiError` avec `code`, `message`, `correlationId` et `details`.
- Les mutations financières déjà branchées transmettent `Idempotency-Key`.

## Restes frontend identifiés

1. Search & Discovery possède encore une interface statique malgré les contrôleurs backend présents.
2. Settings et Feature Flags restent statiques malgré les APIs admin disponibles.
3. Reviews et Comments restent des placeholders malgré les routes interaction-service.
4. XP Rules, anti-fraud et analytics Gamification restent partiels.
5. Payment detail doit finir son adaptation au DTO enveloppé `AdminPaymentDetail`; anomalies doivent être branchées.
6. Event list conserve encore une adaptation de compatibilité et doit passer tous ses filtres au serveur.
7. Audit Logs filtre encore en mémoire au lieu d’utiliser la pagination serveur.

## Verdict

- Fonctionnalités totalement connectées dans cette matrice : **25**
- Fonctionnalités partiellement connectées : **7**
- Fonctionnalités backend disponibles mais frontend non connecté : **6**
- Scénarios E2E exécutés contre une stack active : **0**

Le verdict global est **NOT READY** pour une validation de production. La compilation seule ne vaut pas test E2E; une stack saine, des comptes par rôle et des données de test contrôlées sont nécessaires.
