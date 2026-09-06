# MATRICE D'ALIGNEMENT DES CAPACITÉS : YEYAMO MOBILE ↔ BACKEND YEYAMO-API

> Analyse comparative approfondie entre les parcours utilisateurs et interfaces de **Yeyamo Mobile** et les capacités réelles implémentées dans les microservices de **yeyamo-api**.

---

## 1. Synthèse Globale d'Alignement

| Métrique | Valeur |
| :--- | :---: |
| **Besoins Mobile Audités** | 18 domaines |
| **Capacités Totalement Supportées par le Backend** | 9 |
| **Capacités Partiellement Supportées (Écarts DTO ou Mock)** | 5 |
| **Capacités Sans Backend / Routes Absentes** | 4 |
| **Taux d'Alignement Fonctionnel Immédiat** | **50.0 %** |

---

## 2. Matrice Exhaustive des Capacités Mobile ↔ Backend

| Feature / Besoin Yeyamo Mobile | Service Backend | Route Backend Réelle | DTO Utilisé | Support Complet ? | Écart Identifié & Risque Bloquant | Verdict |
| :--- | :--- | :--- | :--- | :---: | :--- | :---: |
| **Place → Réserver** (Réservation directe d'un lieu) | `place-service` & `booking-service` | `POST /api/v1/bookings` (booking) | `CreateBookingRequest` | ❌ NON | **Aucune liaison Place ↔ Booking**. `place-service` n'a pas d'entité `Activity` ni créneaux. `booking-service` attend un `activityId` abstrait. | **ROUTE ABSENTE** |
| **Event → Mobile Money** (Achat billet par Orange/MTN/Wave) | `ticket-service` & `payment-service` | `POST /api/v1/tickets/orders` (ticket) | `CreateOrderRequest` | ⚠️ PARTIEL | Contrat enum `MOBILE_MONEY` présent, mais le backend utilise `SimulatedPaymentProvider` (mock local). Aucun agrégateur réel connecté. | **MOCK SIMULÉ** |
| **Artisan → Conversation** (Messagerie directe avec un artisan) | `partner-service` & `messaging-service` | `POST /api/v1/messaging/conversations` | `CreateConversationRequest` | ❌ NON | `ArtisanDtos.Response` ne renvoie que `partnerId` et cache le `userId`. `messaging-service` exige le `userId`. Chat impossible. | **DTO INSUFFISANT** |
| **Proverb → Feed** (Proverbes dans le feed social) | `culture-service` & `feed-service` | `GET /api/v1/feed` | `FeedPage` | ❌ NON | `feed-service` n'écoute pas les événements Kafka de `culture-service`. Les proverbes ne sont jamais projetés dans le fil. | **CAPACITÉ DÉCONNECTÉE** |
| **Recipe → Feed** (Recettes dans le feed social) | `culture-service` & `feed-service` | `GET /api/v1/feed` | `FeedPage` | ❌ NON | Aucun pipeline d'ingestion d'événements culturels dans le feed. De plus, `Recipe` n'a pas de structure dédiée (texte brut). | **CAPACITÉ DÉCONNECTÉE** |
| **Experience Detail** (Fiche détaillée d'expérience) | `catalog-service` / `place-service` | `GET /api/v1/catalog/assets/{id}` | `CatalogAssetResponse` | ⚠️ PARTIEL | Pas d'entité `Experience` dédiée. Utilisent des assets génériques sans champs d'expérience (horaires, guide, équipement). | **DTO INSUFFISANT** |
| **Partner Create Place** (Création de lieu par partenaire) | `place-service` | `POST /api/v1/places` | `PlaceRequest` | ⚠️ PARTIEL | Conflit de type d'ID : `PlaceRequest` attend un `Long cityId`, alors que `country-config-service` fournit des `UUID` ! | **INCOMPATIBILITÉ TYPE** |
| **Partner Create Event** (Création événement par partenaire) | `event-service` | `POST /api/v1/events` | `EventRequest` | ⚠️ PARTIEL | `@NotNull UUID placeId` obligatoire. Impossible de créer un événement sans lieu physique préalablement enregistré. | **CONTRAINTE FORTE** |
| **Country Features** (Flags fonctionnalités par pays) | `country-config-service` | `GET /api/v1/countries/{code}/features` | `FeatureFlagsDto` | ⚠️ PARTIEL | Renvoie 404 si le pays n'est pas en base (aucun fallback par défaut), provoquant des crashes sur l'écran Create mobile en mode démo. | **ABSENCE FALLBACK** |
| **Billetterie & Scan** (Validation QR code organisateur) | `ticket-service` | `POST /api/v1/partners/{partnerId}/tickets/scan` | `ScanTicketRequest` | ✅ OUI | Route et logique complètes : vérification de validité, statut `USED`, idempotence et log d'audit. | **TOTALEMENT SUPPORTÉ** |
| **Recherche & Découverte** (Recherche textuelle et carte) | `discovery-service` | `GET /api/v1/discovery/search` | `DiscoverySearchResult` | ✅ OUI | Recherche multi-critères, pagination, filtrage géographique par rayon (`radiusKm`) et catégories. | **TOTALEMENT SUPPORTÉ** |
| **Explorer Lieux & Catégories** (Navigation catalogue) | `place-service` | `GET /api/v1/places`, `GET /api/v1/categories` | `PlaceResponse`, `CategoryDto` | ✅ OUI | Endpoints publics complets avec filtres par pays, région, ville et pagination Spring Data. | **TOTALEMENT SUPPORTÉ** |
| **Événements à proximité** (Agenda culturel) | `event-service` | `GET /api/v1/events` | `EventResponse` | ✅ OUI | Filtrage par dates (`startAt`, `endAt`), ville, pays et statut `PUBLISHED`. | **TOTALEMENT SUPPORTÉ** |
| **Gamification & XP** (Progression et badges) | `gamification-service` | `GET /api/v1/gamification/profile` | `GamificationProfileDto` | ⚠️ PARTIEL | Logique backend existante, mais le routage Gateway `/api/v1/me/xp/**` est orphelin (incohérence d'URL). | **ROUTE GATEWAY ERRÉE** |
| **Missions & Récompenses** (Défis culturels) | `mission-reward-service` | `GET /api/v1/missions` | `MissionResponse` | ✅ OUI | Modèle complet avec conditions de succès, octroi de points XP et badges automatiques. | **TOTALEMENT SUPPORTÉ** |
| **Profil & Authentification** (Inscription, Connexion, OTP) | `auth-service` & `user-service` | `POST /api/v1/auth/login`, `POST /api/v1/auth/register` | `AuthRequest`, `AuthResponse` | ✅ OUI | JWT complet, refresh tokens avec hachage SHA-256 et réinitialisation de mot de passe par code OTP. | **TOTALEMENT SUPPORTÉ** |
| **Upload de Photos/Médias** (Médias utilisateur et partenaires) | `media-service` | `POST /api/v1/media` | `MediaUploadResponse` | ⚠️ PARTIEL | Implémenté, mais non exposé correctement sur le Gateway sans slash (`/api/v1/media` échoue en 404). | **GATEWAY 404** |
| **Contenu & Stories** (Publications sociales des utilisateurs) | `content-service` | `POST /api/v1/posts`, `GET /api/v1/stories` | `PostResponse`, `StoryResponse` | ✅ OUI | Support des médias multiples, expiration des stories à 24h et publication immédiate vers Kafka. | **TOTALEMENT SUPPORTÉ** |

---

## 3. Détail des Bloquants Majeurs pour Yeyamo Mobile

### 🔴 Bloquant 1 : Rupture du tunnel "Place → Réserver"
- **Constat** : Le bouton "Réserver" sur l'écran détail d'un Lieu du mobile ne peut fonctionner.
- **Raison** : `place-service` n'a pas de table d'activités, de sessions ou de tarifs. `booking-service` est un microservice indépendant qui gère des `activity_slots` identifiés par un `activityId` qui ne correspond à aucune clé primaire de `place-service`.

### 🔴 Bloquant 2 : Impossibilité de contacter un Artisan depuis son profil
- **Constat** : L'écran Profil Artisan mobile ne peut pas ouvrir une messagerie instantanée.
- **Raison** : `ArtisanDtos.Response` renvoie `partnerId` (ex: `a1b2c3d4-...`), mais `MessagingController.create` exige une liste de `participantIds` contenant des `userId`. Le mobile n'a aucun moyen de convertir `partnerId` en `userId`.

### 🔴 Bloquant 3 : Incompatibilité de Type sur la Création de Lieux (`cityId`)
- **Constat** : Le formulaire de création de lieu par un partenaire crash ou échoue à la soumission.
- **Raison** : Le sélecteur de ville du mobile consomme `country-config-service` (`GET /api/v1/cities`) qui renvoie un identifiant `UUID`. Mais `PlaceRequest` dans `place-service` type `cityId` en `Long` (`private Long cityId;`). Jackson échoue avec une exception de désérialisation HTTP 400 (`Cannot deserialize UUID to Long`).

### 🔴 Bloquant 4 : Cloisonnement du Flux Social ("Feed")
- **Constat** : Les proverbes et recettes publiés par les contributeurs culturels restent invisibles dans le fil d'accueil.
- **Raison** : `feed-service` n'ingère que les événements émis par `content-service` et `interaction-service`. Le microservice `culture-service` n'émet pas vers `content.events`, privant le fil d'actualité de toute richesse culturelle.
