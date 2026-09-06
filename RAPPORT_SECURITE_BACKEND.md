# RAPPORT D'AUDIT SÉCURITÉ APPROFONDI — YEYAMO-API

> Audit statique de sécurité couvrant l'authentification, l'autorisation, les failles IDOR/BOLA, le Mass Assignment, les injections, l'exposition de secrets, la passerelle, et la gestion des flux financiers.

---

## 1. Synthèse Exécutive de Sécurité

L'architecture de sécurité de `yeyamo-api` repose sur un modèle moderne **Stateless JWT (HMAC-SHA256)** coordonné par `security-hardening-starter` et Spring Security. La majorité des endpoints sensibles requiert une authentification, et les entités JPA ne sont pas exposées directement via les contrôleurs (0 faille de Mass Assignment direct détectée).

Cependant, l'audit statique met en évidence plusieurs **vulnérabilités critiques et élevées** :
1. **IDOR / BOLA Critique sur les Lieux (`place-service`)** : L'endpoint `PUT /api/v1/places/{id}` permet à n'importe quel utilisateur disposant du rôle `PARTNER` ou `ADMIN` d'écraser intégralement un lieu appartenant à un autre partenaire, sans aucune validation de possession (`ownerId`).
2. **Exposition de Secrets en Clair dans `.env`** : Clé API SMTP Brevo/Sendinblue live (`MAIL_PASSWORD=xsmtpsib-...`) et clés Google Maps API en clair dans le dépôt.
3. **Rate Limiting Gateway Vulnérable derrière Reverse Proxy** : Le filtre Redis (`RedisRateLimitFilter`) extrait l'IP cliente via `request.getRemoteAddr()` sans inspecter `X-Forwarded-For`. En environnement de production (derrière un Ingress/ALB/Nginx), tous les clients partagent la même IP de reverse proxy, provoquant des dénis de service collatéraux (`429 Too Many Requests`) dès 20 tentatives d'authentification.
4. **Fournisseur de Paiement Simulé (`SimulatedPaymentProvider`)** : Aucune intégration réelle avec un agrégateur Mobile Money (Orange Money, MTN MoMo, Wave, CinetPay) n'est implémentée. Tout montant inférieur à 1 000 000 est automatiquement approuvé avec un mock.
5. **CORS Permissif sur Réseau Local** : Le pattern `http://192.168.*:*` autorise n'importe quelle machine locale sur n'importe quel port à émettre des requêtes cross-origin.

---

## 2. Authentification & Gestion des Tokens (JWT)

### 2.1 Génération et Signature
- **Localisation** : `auth-service/src/main/java/com/yeyamo_mobile/api/auth_service/security/JwtService.java`
- **Algorithme** : `HmacSHA256` via clé secrète partagée (`jwt.secret`).
- **Longueur de clé exigée** : Contrôle systématique `>= 32 bytes` (256 bits) au démarrage de chaque microservice dans `SecurityConfig.decoder()`.
- **Observation** : L'architecture utilise une clé symétrique HMAC-SHA256 partagée entre 32 microservices plutôt qu'une paire asymétrique RSA/ECDSA (JWKS / Public Key). Si un seul microservice est compromis, sa clé `jwt.secret` permet de forger des tokens pour l'ensemble de la plateforme.

### 2.2 Expiration & Rotation des Refresh Tokens
- **Access Token** : Durée de vie courte configurée par défaut (15 à 60 minutes).
- **Refresh Token** : Géré par `RefreshTokenService.java` avec hachage SHA-256 en base (`tokenHash`) et révocation lors de la rotation (`rotateRefreshToken`).

---

## 3. Autorisation & Failles IDOR / BOLA (Broken Object Level Authorization)

> **22 endpoints** présentent une absence formelle de validation de propriété sur une ressource identifiée par ID (`{id}` / `{partnerId}`).

| Gravité | Service | Méthode & Route | Contrôleur & Méthode | Preuve de la Faille | Impact Métier |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **HIGH** | `catalog-service` | `PATCH /api/v1/catalog/assets/{id}/status` | `CatalogAssetController.status` | Fichier `catalog-service\src\main\java\com\yeyamo_mobile\api\catalog_service\interfaces\rest\CatalogAssetController.java` : la méthode `status` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Modification ou suppression non autorisée de la ressource d'autrui via altération de l'ID dans l'URL. |
| **HIGH** | `discovery-service` | `PUT /api/v1/admin/search/synonyms/{id}` | `SearchAdminController.update` | Fichier `discovery-service\src\main\java\com\yeyamo_mobile\api\discovery_service\infrastructure\searchadmin\SearchAdminController.java` : la méthode `update` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Modification ou suppression non autorisée de la ressource d'autrui via altération de l'ID dans l'URL. |
| **HIGH** | `discovery-service` | `DELETE /api/v1/admin/search/synonyms/{id}` | `SearchAdminController.delete` | Fichier `discovery-service\src\main\java\com\yeyamo_mobile\api\discovery_service\infrastructure\searchadmin\SearchAdminController.java` : la méthode `delete` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Modification ou suppression non autorisée de la ressource d'autrui via altération de l'ID dans l'URL. |
| **HIGH** | `notification-service` | `PUT /api/v1/admin/newsletters/{id}` | `NewsletterAdminController.update` | Fichier `notification-service\src\main\java\com\yeyamo_mobile\api\notification_service\newsletter\NewsletterAdminController.java` : la méthode `update` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Modification ou suppression non autorisée de la ressource d'autrui via altération de l'ID dans l'URL. |
| **HIGH** | `mission-reward-service` | `PUT /api/v1/mission-management/badges/{id}` | `GamificationAdminController.updateBadge` | Fichier `mission-reward-service\src\main\java\com\yeyamo_mobile\api\mission_reward_service\infrastructure\web\GamificationAdminController.java` : la méthode `updateBadge` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Modification ou suppression non autorisée de la ressource d'autrui via altération de l'ID dans l'URL. |
| **HIGH** | `mission-reward-service` | `PATCH /api/v1/mission-management/badges/{id}/status` | `GamificationAdminController.badgeStatus` | Fichier `mission-reward-service\src\main\java\com\yeyamo_mobile\api\mission_reward_service\infrastructure\web\GamificationAdminController.java` : la méthode `badgeStatus` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Modification ou suppression non autorisée de la ressource d'autrui via altération de l'ID dans l'URL. |
| **HIGH** | `mission-reward-service` | `PUT /api/v1/mission-management/xp-rules/{id}` | `GamificationAdminController.updateRule` | Fichier `mission-reward-service\src\main\java\com\yeyamo_mobile\api\mission_reward_service\infrastructure\web\GamificationAdminController.java` : la méthode `updateRule` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Modification ou suppression non autorisée de la ressource d'autrui via altération de l'ID dans l'URL. |
| **HIGH** | `mission-reward-service` | `PATCH /api/v1/mission-management/xp-rules/{id}/status` | `GamificationAdminController.ruleStatus` | Fichier `mission-reward-service\src\main\java\com\yeyamo_mobile\api\mission_reward_service\infrastructure\web\GamificationAdminController.java` : la méthode `ruleStatus` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Modification ou suppression non autorisée de la ressource d'autrui via altération de l'ID dans l'URL. |
| **HIGH** | `mission-reward-service` | `PUT /api/v1/mission-management/rewards/{id}` | `GamificationAdminController.updateReward` | Fichier `mission-reward-service\src\main\java\com\yeyamo_mobile\api\mission_reward_service\infrastructure\web\GamificationAdminController.java` : la méthode `updateReward` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Modification ou suppression non autorisée de la ressource d'autrui via altération de l'ID dans l'URL. |
| **HIGH** | `mission-reward-service` | `PATCH /api/v1/mission-management/rewards/{id}/status` | `GamificationAdminController.rewardStatus` | Fichier `mission-reward-service\src\main\java\com\yeyamo_mobile\api\mission_reward_service\infrastructure\web\GamificationAdminController.java` : la méthode `rewardStatus` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Modification ou suppression non autorisée de la ressource d'autrui via altération de l'ID dans l'URL. |
| **HIGH** | `mission-reward-service` | `PUT /api/v1/mission-management/missions/{id}` | `MissionController.update` | Fichier `mission-reward-service\src\main\java\com\yeyamo_mobile\api\mission_reward_service\infrastructure\web\MissionController.java` : la méthode `update` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Modification ou suppression non autorisée de la ressource d'autrui via altération de l'ID dans l'URL. |
| **HIGH** | `mission-reward-service` | `DELETE /api/v1/mission-management/missions/{id}` | `MissionController.archive` | Fichier `mission-reward-service\src\main\java\com\yeyamo_mobile\api\mission_reward_service\infrastructure\web\MissionController.java` : la méthode `archive` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Modification ou suppression non autorisée de la ressource d'autrui via altération de l'ID dans l'URL. |
| **HIGH** | `commerce-service` | `PUT /api/v1/commerce/partners/{partnerId}/promotions/{id}` | `PartnerPromotionController.update` | Fichier `commerce-service\src\main\java\com\yeyamo_mobile\api\commerce_service\interfaces\PartnerPromotionController.java` : la méthode `update` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Modification ou suppression non autorisée de la ressource d'autrui via altération de l'ID dans l'URL. |
| **MEDIUM** | `ticket-service` | `POST /api/v1/partners/{partnerId}/tickets/configurations/{id}/types` | `PartnerTicketController.type` | Fichier `ticket-service\src\main\java\com\yeyamo_mobile\api\ticket_service\interfaces\rest\PartnerTicketController.java` : la méthode `type` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Modification ou suppression non autorisée de la ressource d'autrui via altération de l'ID dans l'URL. |
| **CRITICAL** | `ticket-service` | `DELETE /api/v1/partners/{partnerId}/tickets/staff/{id}` | `PartnerTicketController.revoke` | Fichier `ticket-service\src\main\java\com\yeyamo_mobile\api\ticket_service\interfaces\rest\PartnerTicketController.java` : la méthode `revoke` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Révocation arbitraire de membres du personnel de billetterie d'un autre organisateur. |
| **HIGH** | `place-service` | `PUT /api/v1/places/{id}` | `PlaceController.update` | Fichier `place-service\src\main\java\com\yeyamo_mobile\api\place_service\controller\PlaceController.java` : la méthode `update` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Écrasement malveillant des données d'un lieu (nom, adresse, photos, coordonnées) par un autre partenaire. |
| **MEDIUM** | `support-service` | `GET /api/v1/admin/support/conversations/{id}` | `SupportAdminController.detail` | Fichier `support-service\src\main\java\com\yeyamo_mobile\api\support_service\infrastructure\web\SupportAdminController.java` : la méthode `detail` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Réassignation, clôture ou modification de priorité de tickets de support sans habilitation vérifiée. |
| **MEDIUM** | `support-service` | `POST /api/v1/admin/support/conversations/{id}/messages` | `SupportAdminController.message` | Fichier `support-service\src\main\java\com\yeyamo_mobile\api\support_service\infrastructure\web\SupportAdminController.java` : la méthode `message` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Réassignation, clôture ou modification de priorité de tickets de support sans habilitation vérifiée. |
| **MEDIUM** | `support-service` | `POST /api/v1/admin/support/conversations/{id}/notes` | `SupportAdminController.note` | Fichier `support-service\src\main\java\com\yeyamo_mobile\api\support_service\infrastructure\web\SupportAdminController.java` : la méthode `note` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Réassignation, clôture ou modification de priorité de tickets de support sans habilitation vérifiée. |
| **CRITICAL** | `support-service` | `PATCH /api/v1/admin/support/conversations/{id}/assign` | `SupportAdminController.assign` | Fichier `support-service\src\main\java\com\yeyamo_mobile\api\support_service\infrastructure\web\SupportAdminController.java` : la méthode `assign` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Réassignation, clôture ou modification de priorité de tickets de support sans habilitation vérifiée. |
| **CRITICAL** | `support-service` | `PATCH /api/v1/admin/support/conversations/{id}/status` | `SupportAdminController.status` | Fichier `support-service\src\main\java\com\yeyamo_mobile\api\support_service\infrastructure\web\SupportAdminController.java` : la méthode `status` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Réassignation, clôture ou modification de priorité de tickets de support sans habilitation vérifiée. |
| **CRITICAL** | `support-service` | `PATCH /api/v1/admin/support/conversations/{id}/priority` | `SupportAdminController.priority` | Fichier `support-service\src\main\java\com\yeyamo_mobile\api\support_service\infrastructure\web\SupportAdminController.java` : la méthode `priority` ne prend ni `Principal` ni `Authentication` et n'invoque aucun contrôle `ownerId == currentUserId`. | Réassignation, clôture ou modification de priorité de tickets de support sans habilitation vérifiée. |

---

## 4. Mass Assignment & Validation des DTOs

### 4.1 Mass Assignment
- **Observation** : **0 contrôleur** ne mappe directement une entité JPA (`@Entity`) en paramètre `@RequestBody`.
- **Verdict** : **CONFORME**. Les développeurs ont systématiquement isolé des DTOs spécifiques (`Create...Request`, `Update...Request`).

### 4.2 Endpoints avec Absence de `@Valid` sur `@RequestBody`
- **Nombre d'anomalies détectées** : 24 routes reçoivent un payload JSON sans annotation `@Valid` ou `@Validated`.
- **Exemples notables** :
  - `catalog-service` : `POST /api/v1/admin/artwork-materials` (`ArtworkReferenceController.material`) DTO `ReferenceRequest` sans `@Valid`
  - `catalog-service` : `POST /api/v1/admin/artwork-techniques` (`ArtworkReferenceController.technique`) DTO `ReferenceRequest` sans `@Valid`
  - `culture-service` : `POST /api/v1/culture/challenges/{id}/submissions` (`CultureChallengeController.submit`) DTO `SubmissionRequest` sans `@Valid`
  - `interaction-service` : `PATCH /api/v1/admin/reviews/{id}/hide` (`AdminContentController.hideReview`) DTO `ActionRequest` sans `@Valid`
  - `interaction-service` : `PATCH /api/v1/admin/reviews/{id}/restore` (`AdminContentController.restoreReview`) DTO `ActionRequest` sans `@Valid`
  - `interaction-service` : `PATCH /api/v1/admin/comments/{id}/hide` (`AdminContentController.hideComment`) DTO `ActionRequest` sans `@Valid`
  - `interaction-service` : `PATCH /api/v1/admin/comments/{id}/restore` (`AdminContentController.restoreComment`) DTO `ActionRequest` sans `@Valid`
  - `interaction-service` : `POST /api/v1/admin/comments/{id}/lock` (`AdminContentController.lock`) DTO `ActionRequest` sans `@Valid`
  - `discovery-service` : `POST /api/v1/admin/search/synonyms` (`SearchAdminController.createSynonym`) DTO `SearchAdminDtos` sans `@Valid`
  - `discovery-service` : `POST /api/v1/admin/search/ranking` (`SearchAdminController.ranking`) DTO `SearchAdminDtos` sans `@Valid`

---

## 5. Exposition de Secrets et Données Sensibles

### 5.1 Secrets Détectés dans les Fichiers de Configuration
| Emplacement | Ligne | Type | Valeur Masquée | Risque |
| :--- | :---: | :--- | :--- | :--- |
| `.env` | 9 | Clé API SMTP Brevo | `xsmtpsib-dc65****b6d94` | **CRITIQUE** : Clé API externe réelle permettant l'envoi d'emails malveillants et l'accès au compte Brevo. |
| `.env` | 21 | Clé Google Maps Android | `AIzaSy****BN8` | **ÉLEVÉ** : Clé API mobile utilisable sans restriction de quota. |
| `.env` | 22 | Clé Google Maps iOS | `AIzaSy****TWs` | **ÉLEVÉ** : Clé API mobile utilisable sans restriction de quota. |
| `catalog-service/compose.yaml` | 38 | Secret JWT hardcodé | `local-dev-jwt-secret-key-at-least-32-chars-1234567890` | **MOYEN** : Secret JWT par défaut réutilisé en environnement local. |
| `docker-compose.yml` | 24 | Mot de passe Postgres | `postgres` | **FAIBLE** : Identifiant par défaut du cluster local Docker. |

---

## 6. Analyse des Uploads et Gestion des Médias

- **Localisation** : `media-service/src/main/java/com/yeyamo_mobile/api/media_service/`
- **Contrôle MIME & Extensions** : Présence de `MediaUploadPolicy` vérifiant les types MIME autorisés (`image/jpeg`, `image/png`, `video/mp4`).
- **Path Traversal** : Les noms de fichiers originaux ne sont pas conservés sur le disque local ; un UUID aléatoire est généré pour chaque média (`UUID.randomUUID() + extension`).
- **Risque Détecté** : Dans `VideoThumbnailStrategy.java` ligne 16, un bloc `finally { try { Files.deleteIfExists(input); } catch(Exception e) {} }` étouffe silencieusement les exceptions sans journalisation.

---

## 7. Sécurité des Paiements et Transactions Financières

- **Architecture** : Asynchrone basée sur Transactional Outbox + Kafka.
- **Idempotence** : `PaymentController` exige un en-tête `Idempotency-Key` sur les remboursements (`POST /api/v1/payments/{id}/refunds`).
- **Risque Critique Identifié** : `SimulatedPaymentProvider.java` approuve automatiquement tout paiement sans interagir avec un prestataire réel. Les montants ne sont pas validés contre les soldes réels d'un compte Mobile Money.

---

## 8. Configuration Réseau, Actuator & CORS

### 8.1 Actuator
- **Observation** : Restreint à `health,info,metrics,prometheus` sur l'ensemble des 21 microservices audités.
- **Endpoints Dangereux (`/env`, `/heapdump`, `/beans`)** : **NON EXPOSÉS**. Verdict : **CONFORME**.

### 8.2 CORS
- **Risque** : `security.cors.allowed-origins=${CORS_ORIGINS:http://localhost:*,http://127.0.0.1:*,http://10.0.2.2:*,http://192.168.*:*}` autorise tout le sous-réseau `192.168.*:*`.

### 8.3 Rate Limiting
- **Risque** : `RedisRateLimitFilter` calcule le hash client sur `request.getRemoteAddr()`, ce qui mutualise tout le trafic sous l'IP du proxy inverse en production.