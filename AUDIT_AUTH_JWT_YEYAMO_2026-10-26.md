# Audit forensique de l’authentification JWT et des sessions Yeyamo

**Date :** 2026-10-26<br>
**Périmètre :** dépôt local `yeyamo-api` et dépôt mobile local `yeyamo-mobile`<br>
**Mode :** analyse statique, lecture seule<br>
**Statut :** aucun correctif appliqué

## A. Résumé exécutif

### Conclusion principale

**CONFIRMÉ sous réserve de l’inventaire de production fourni :** l’Auth Service et la Gateway sont associés au groupe de secret JWT A, tandis que les microservices métier sont associés au groupe B. Si ces empreintes correspondent aux valeurs effectivement injectées dans les conteneurs, la Gateway peut accepter le JWT puis le microservice métier le rejette lors de sa propre vérification HMAC. Cette chaîne explique directement un login réussi suivi de `401` sur des fonctionnalités privées.

Le dépôt local ne contient pas les valeurs effectives des secrets de production. Il ne permet donc pas de recalculer les empreintes ni de déclarer que les valeurs locales sont identiques à celles déployées. La divergence de production est une **preuve fournie**, pas une mesure indépendante réalisée dans ce dépôt.

### Autres conclusions

- **CONFIRMÉ :** l’OTP implémenté est un OTP de vérification d’adresse e-mail et de réinitialisation de mot de passe. Il n’existe pas de second facteur OTP de connexion dans le parcours backend inspecté.
- **CONFIRMÉ :** après confirmation de l’e-mail, le mobile revient volontairement à l’écran de login ; le login suivant émet alors les tokens. L’absence d’un nouvel OTP est donc normale pour le contrat actuel.
- **CONFIRMÉ :** la Gateway et les services métier valident les JWT indépendamment ; la Gateway ne transmet pas une identité de confiance à la place du JWT.
- **CONFIRMÉ :** le mobile ajoute le Bearer depuis SecureStore sur les requêtes privées et dispose d’un refresh automatique sur `401`.
- **PROBABLE :** une divergence de secret provoque ensuite des refreshs répétés : chaque requête métier échoue encore, puis son interceptor tente un refresh qui réussit. Cela peut expliquer `RATE_LIMITED`.
- **CONFIRMÉ dans la configuration :** plusieurs services déclarent un mode OAuth2 issuer/JWK distinct du JWT HMAC produit par l’Auth Service. La compatibilité effective dépend des surcharges d’environnement et du BeanPostProcessor du starter commun.
- **PROBABLE :** `ORIGIN_NOT_ALLOWED` sur `/ws/messaging` vient d’une origine WebSocket native non présente dans `CORS_ALLOWED_ORIGINS`, et non d’un échec JWT.
- **NON CONFIRMÉ :** une incompatibilité intrinsèque entre Spring Boot `4.1.0` et Spring Cloud `2025.1.2`. Les versions sont déclarées, mais aucune résolution Maven ni exécution d’intégration n’a été lancée pendant cet audit en lecture seule.

### Réponse courte à la question finale

Yeyamo peut terminer le login parce que l’Auth Service signe le token avec son secret et la Gateway le valide avec le même groupe A. Les fonctionnalités privées échouent ensuite lorsque le microservice cible vérifie lui-même ce token avec un secret du groupe B, ou avec une configuration issuer/JWK incompatible. Le mobile interprète alors les `401` comme une expiration et lance des refreshs ; le refresh peut répondre `200`, sans réparer la validation du token par le service métier. La correction minimale est d’aligner la configuration effective de validation sur la configuration de signature, puis de tester la chaîne complète Gateway → service cible, sans désactiver la validation.

## B. Architecture réelle

### Parcours d’une requête privée

```text
Mobile
  -> POST /api/v1/auth/login
  -> Auth Service : authentifie, signe un access JWT, crée un refresh token opaque
  -> Mobile : stocke les deux valeurs dans Expo SecureStore
  -> GET /api/v1/<fonctionnalité> avec Authorization: Bearer <access JWT>
  -> API Gateway : vérifie signature, iss, aud, timestamps et éligibilité selon le decoder effectif
  -> Gateway MVC : route vers lb://<service> via Eureka/LoadBalancer
  -> Microservice : vérifie à nouveau le JWT et applique ses rôles/permissions
  -> Réponse
```

### Composants et preuves

| Composant | Fichier / classe | Rôle observé | Risque principal |
|---|---|---|---|
| Contrôleur Auth | `auth-service/src/main/java/com/yeyamo_mobile/api/auth_service/controller/AuthController.java:52-84`, `AuthController` | Expose login, refresh, confirmation e-mail et sessions | Le endpoint OTP ne finalise pas une session |
| Service Auth | `auth-service/src/main/java/com/yeyamo_mobile/api/auth_service/service/AuthService.java:176-253`, `AuthService` | Authentifie, applique l’éligibilité, génère/renouvelle les tokens | Le refresh ne peut pas corriger une clé aval divergente |
| Signature JWT | `auth-service/src/main/java/com/yeyamo_mobile/api/auth_service/security/JwtService.java:62-91`, `JwtService` | Produit un JWT HMAC HS256 | Secret partagé nécessaire avec chaque validateur |
| Validation Auth | `auth-service/src/main/java/com/yeyamo_mobile/api/auth_service/security/JwtAuthenticationFilter.java:28-61`, `JwtAuthenticationFilter` | Vérifie le Bearer pour les routes protégées d’Auth | Validation custom distincte du resource server |
| Refresh | `auth-service/src/main/java/com/yeyamo_mobile/api/auth_service/service/RefreshTokenService.java:86-106`, `RefreshTokenService` | Rotation transactionnelle, hash SHA-256, détection de réutilisation | Une réutilisation concurrente peut révoquer toutes les sessions |
| OTP | `auth-service/src/main/java/com/yeyamo_mobile/api/auth_service/service/OtpService.java:61-116`, `OtpService` | Hash Redis, expiration, tentatives atomiques, consommation unique | Redis est nécessaire au parcours OTP |
| Gateway Security | `api-gateway/src/main/java/com/yeyamo_mobile/api/api_gateway/config/SecurityConfig.java:45-121`, `SecurityConfig` | Resource server HMAC, règles publiques/privées, CORS | Validation réussie à l’entrée mais échec possible en aval |
| Gateway routes | `cloud-conf-yeyamo/api-gateway.properties:25-347` | Routes `lb://`, CircuitBreaker, fallback | Le circuit breaker ne résout pas une erreur d’authentification aval |
| Hardening commun | `security-hardening-starter/src/main/java/com/yeyamo/security/hardening/HardeningAutoConfiguration.java:20-70` | Remplace les `JwtDecoder`, ajoute filtrage CORS/rate-limit | Peut changer le decoder effectivement utilisé |
| Decoder strict | `security-hardening-starter/src/main/java/com/yeyamo/security/hardening/StrictJwtDecoders.java:27-87` | HMAC ou JWK, rotation, issuer, audience, horloge, éligibilité | Un mauvais secret/issuer/audience produit un `401` |
| Client mobile | `E:\Daryl\yeyamo-mobile\src\services\api\client.ts` | Ajoute le Bearer, déduplique les refreshs simultanés, rejoue une requête | Les `401` avals peuvent provoquer des refreshs répétés |
| Session mobile | `E:\Daryl\yeyamo-mobile\src\features\auth\auth.service.ts` | Persiste/restaure les tokens, connecte le WebSocket | Une session cache peut masquer un échec réseau |

### Qui vérifie réellement la signature ?

1. `JwtService` vérifie la signature dans l’Auth Service via JJWT et la clé courante ou précédente (`JwtService.java:130-145`).
2. La Gateway déclare un `NimbusJwtDecoder` HMAC (`SecurityConfig.java:114-120`), ensuite potentiellement remplacé par `StrictJwtDecoders`.
3. Les microservices métier déclarent pour la plupart leur propre `NimbusJwtDecoder` HMAC et le starter commun peut le remplacer après initialisation.
4. Le routage `lb://` ne valide pas le JWT et Eureka ne valide pas le JWT ; ils ne font que résoudre/acheminer l’instance.

## C. Matrice JWT

### Génération

| Élément | Constat |
|---|---|
| Algorithme | HS256, via JJWT (`JwtService.java:90`) |
| Clé | Octets UTF-8 de `jwt.secret`, minimum 32 octets (`JwtService.java:148-154`) |
| Source Auth | `jwt.secret=${JWT_SECRET}` dans `cloud-conf-yeyamo/auth-service.properties:32` |
| Claims obligatoires générés | `sub`, `iss`, `aud`, `iat`, `exp` |
| Claims métier | `token_type=access`, e-mail, téléphone, pays, `email_verified`, `account_status`, rôles, scopes, permissions |
| Claims absents observés | Pas de `nbf`, pas de `jti` |
| Expiration access | `jwt.access-token-expiration=1800000`, soit 30 minutes (`auth-service.properties:37`) |
| Expiration refresh | `jwt.refresh-token-expiration=7776000000`, soit 90 jours (`auth-service.properties:38`) |
| Rotation | Liste `jwt.previous-secrets` supportée par l’Auth Service et le starter strict |

### Validation

`JwtService.extractAllClaims` exige la signature, `iss` et `aud`. La validation métier exige également un sujet numérique, un type `access` si présent et un compte `ACTIVE` avec e-mail vérifié lorsque l’e-mail existe (`JwtService.java:98-145`).

Le starter commun ajoute :

- une tolérance d’horloge de 60 secondes ;
- un issuer obligatoire ;
- une audience obligatoire ;
- la validation de l’éligibilité du compte ;
- la rotation sur `jwt.previous-secrets` ;
- un mode JWK si `jwt.jwk-set-uri` est effectivement renseigné (`StrictJwtDecoders.java:27-80`).

### Réponse obligatoire sur les groupes A/B

Si l’Auth Service signe avec le secret du groupe A et qu’un microservice métier valide avec le secret du groupe B :

1. le JWT arrive au microservice avec sa signature HS256 intacte ;
2. le decoder HMAC du microservice calcule une vérification avec B ;
3. la signature ne correspond pas ;
4. le resource server lève une erreur de token invalide ;
5. la SecurityFilterChain refuse la requête, normalement en `401` ;
6. le contrôleur métier n’est pas exécuté.

La Gateway peut néanmoins avoir accepté le même JWT avec A. Le code ne contient pas de mécanisme qui transforme le JWT ou qui remplace `Authorization` par une identité interne. Cette divergence est donc suffisante pour expliquer le pattern « login réussi, fonctionnalités privées en `401` », si l’inventaire de production fourni est exact.

### Access token versus refresh token

Le refresh token n’est pas un JWT : `RefreshTokenService.randomToken()` produit 64 octets aléatoires encodés Base64 URL-safe (`RefreshTokenService.java:112-115`) et la base ne conserve que son hash (`:40-44`, `:118-123`).

Un refresh token présenté comme Bearer est rejeté par `JwtService` ou par le resource server ; il ne peut pas être utilisé comme access token. Le mobile envoie correctement le refresh dans le corps de `POST /auth/refresh`, sans le mettre dans `Authorization`.

## D. Analyse des erreurs HTTP 401

| Cause | Niveau de preuve | Effet attendu | Conclusion |
|---|---|---|---|
| Secret de signature A différent du secret de validation B | **CONFIRMÉ sous réserve de l’inventaire de production fourni** | Gateway `200`/route puis service métier `401` | Cause racine prioritaire |
| `iss` ou `aud` différent entre Auth, Gateway et services | **PROBABLE / à vérifier dans l’environnement effectif** | Decoder strict `401` | Les valeurs communes existent, mais les surcharges ne sont pas prouvées |
| Mode JWK/issuer sur certains services | **CONFIRMÉ dans les fichiers de configuration ; effet production non confirmé** | Échec de récupération de clés, démarrage dégradé ou JWT HMAC refusé | Risque de configuration incohérente |
| Suppression de `Authorization` par la Gateway | **NON CONFIRMÉ, contredit par le code inspecté** | Service aval `401` malgré Gateway valide | Aucun filtre local inspecté ne le supprime |
| Expiration réelle de l’access token | **NON CONFIRMÉ** | Mobile refresh | À distinguer d’un rejet de signature par les logs avals |
| Route publique mal classée | **PROBABLE pour certaines URL exactes** | `401` à la Gateway avant routage | Vérifier l’URL exacte, notamment stats sociales |
| Circuit breaker ou LoadBalancer | **NON CAUSE D’UN 401** | `503`, timeout ou fallback | Ne transforme pas normalement une signature valide en `401` |

### Routes signalées

- `/api/v1/users/me` est privée dans la Gateway et dans le service utilisateur ; elle est compatible avec une divergence de secret aval.
- `/api/v1/posts/me`, `/api/v1/stories`, `/api/v1/messaging/conversations` et `/api/v1/notifications/unread/count` sont des routes privées dans les configurations inspectées ; elles dépendent du Bearer valide en Gateway puis en aval.
- `/api/v1/users/social/{uuid}/stats` est explicitement publique pour un `GET` dans la Gateway (`SecurityConfig.java:90-91`) et dans le service utilisateur. Le symptôme fourni omet toutefois l’UUID. Si l’URL réellement appelée est littéralement `/api/v1/users/social/stats`, elle ne correspond pas à cette règle et peut être protégée. L’URL complète et le `X-Correlation-ID` sont nécessaires pour trancher.

### Distinction Gateway / aval

Le filtre de corrélation conserve ou génère `X-Correlation-ID` (`api-gateway/src/main/java/com/yeyamo_mobile/api/api_gateway/filter/CorrelationIdFilter.java:24-52`). Le fallback journalise le type d’exception et la corrélation, sans journaliser le Bearer. Il faut exploiter cet identifiant dans les logs :

- `401` sans trace de routage : refus Gateway/hardening ;
- trace de route puis `401` du service cible : validation aval ou autorisation métier ;
- `503`/fallback : LoadBalancer, timeout ou CircuitBreaker, pas une preuve de JWT invalide.

## E. Audit de la Gateway, Eureka et LoadBalancer

### Routage

Les routes métier sont en `lb://<service>` et les routes WebSocket utilisent `lb:ws://messaging-service` (`cloud-conf-yeyamo/api-gateway.properties:25-347`). La Gateway déclare Eureka Client, Spring Cloud LoadBalancer et Gateway Server Web MVC dans `api-gateway/pom.xml`.

Le code statique ne montre pas d’adresse IP fixe ni de réécriture de `Authorization`. Eureka fournit les instances ; LoadBalancer choisit l’instance ; CircuitBreaker/fallback traite les erreurs de disponibilité. Aucun de ces composants ne peut rendre compatible un JWT signé avec A et vérifié avec B.

### Timeouts et résilience

La configuration centrale indique :

- `resilience4j.timelimiter.configs.default.timeout-duration=3s` ;
- fenêtre CircuitBreaker de 20 appels ;
- minimum 10 appels ;
- seuil 50 % ;
- attente en état ouvert de 15 secondes.

Ces paramètres concernent les erreurs de disponibilité et de latence. Un `401` rapide doit être séparé d’un `503` après trois secondes. Le fallback ne doit jamais être utilisé comme preuve d’un problème de clé JWT.

### Versions

`api-gateway/pom.xml` déclare Spring Boot `4.1.0` et Spring Cloud `2025.1.2`, avec Gateway Server Web MVC, Eureka Client, LoadBalancer et CircuitBreaker Resilience4j. Les versions n’ont pas été modifiées. Aucun test de résolution Maven n’a été exécuté pendant cet audit ; la compatibilité opérationnelle complète reste donc **NON CONFIRMÉE**, même si le code et les tests présents montrent une intégration déjà structurée.

## F. Analyse des refresh tokens

### Comportement backend confirmé

`POST /api/v1/auth/refresh` :

1. rejette un corps absent ou sans refresh token ;
2. cherche le hash sous verrou pessimiste (`RefreshTokenService.java:92-94`) ;
3. rejette expiration ou révocation ;
4. vérifie l’éligibilité du compte ;
5. révoque l’ancien token ;
6. crée un nouveau refresh token ;
7. génère un nouvel access JWT (`AuthService.java:242-253`).

La réutilisation d’un token révoqué déclenche `REFRESH_TOKEN_REUSE_DETECTED` et supprime les tokens de l’utilisateur (`RefreshTokenService.java:95-98`). La protection est forte, mais une concurrence hors du chemin mobile peut invalider toutes les sessions.

### Comportement mobile confirmé

Dans `E:\Daryl\yeyamo-mobile\src\services\api\client.ts` :

- le Bearer est lu dans SecureStore pour chaque requête privée ;
- un `401` déclenche au plus un retry par requête (`_retry`) ;
- `refreshPromise` déduplique les refreshs simultanés au niveau du processus ;
- le refresh utilise une requête Axios séparée sans Bearer ;
- les deux tokens sont remplacés après `200` ;
- un échec du refresh efface la session et déclenche la redirection une seule fois.

### Risque de boucle / surcharge

**PROBABLE :** si toutes les requêtes métier sont refusées par le secret B, chaque requête reçoit un `401`, lance éventuellement un refresh qui réussit avec Auth A, puis rejoue la requête et obtient à nouveau `401`. Le mécanisme n’est pas une boucle infinie par requête, mais une rafale de requêtes peut produire de nombreux refreshs successifs. La limitation d’authentification de la Gateway peut alors retourner `RATE_LIMITED`.

Le refresh est donc un symptôme secondaire tant que la validation aval reste incohérente. Il ne faut pas augmenter le rate limiting ni désactiver la rotation pour masquer ce défaut.

## G. Analyse OTP

### Ce que fait réellement le backend

- `POST /auth/email/verification/request` génère un OTP de vérification d’adresse (`AuthService.java:303-313`).
- `POST /auth/email/verification/confirm` vérifie et consomme l’OTP, définit `emailVerifiedAt` et passe un compte `PENDING` à `ACTIVE` (`AuthService.java:315-327`).
- `POST /auth/password/forgot` et `/reset` utilisent un autre purpose OTP (`:329-353`).
- `POST /auth/login` refuse un compte `PENDING` par `EMAIL_NOT_VERIFIED` (`:192-195`).
- Les tokens ne sont créés que par `tokensFor()` après `assertSessionEligible()` (`:356-360`).

`OtpService` stocke uniquement un hash SHA-256 dans Redis, expire le code et consomme atomiquement le code valide. Le code est à usage unique et limité à trois tentatives selon la configuration centralisée.

### Explication du parcours observé

Le mobile traite `EMAIL_NOT_VERIFIED` dans `login.tsx` et navigue vers `verify-code`. Après succès, `verify-code.tsx` appelle `router.replace('/(auth)/login')`. Le backend ne renvoie pas de tokens depuis la confirmation OTP. Le second login est donc nécessaire avec le contrat actuel.

**Conclusion :** le backend ne démontre pas un contournement OTP. Il implémente une activation persistante de l’adresse e-mail, pas une MFA à chaque connexion. Une fois `emailVerifiedAt` enregistré, les connexions ultérieures n’exigent plus d’OTP. Si le produit exige réellement `identifiants → OTP → session` à chaque login, cette fonctionnalité n’existe pas dans le code inspecté et nécessite un nouveau contrat de session partielle, pas un simple réglage.

### Scénarios où une session peut sembler préexister

- un ancien access/refresh token reste dans SecureStore ;
- `hydrate()` restaure une session valide ou une session hors ligne mise en cache ;
- un mode `demo-user`/`demo-partner` est actif en environnement non production ;
- une autre requête mobile est envoyée avant la fin de la navigation.

Ces scénarios ne prouvent pas un bypass backend ; ils doivent être vérifiés avec les logs de session mobile et la corrélation serveur.

## H. Contrat mobile et WebSocket

### Session mobile

Le dépôt mobile est accessible. Les tokens sont stockés dans Expo SecureStore sous des clés dédiées, jamais dans le code source (`src/services/storage/secure-store.ts`). `persistSession()` refuse une réponse sans access token, refresh token ou utilisateur éligible (`src/features/auth/auth.service.ts`).

La restauration appelle `/auth/me` si le réseau est disponible. En cas de réseau absent, elle peut restaurer une identité cache si un refresh token et une identité éligible existent. Cette tolérance hors ligne ne doit pas être confondue avec une preuve d’acceptation backend.

### WebSocket

Le mobile ouvre `ENV.MESSAGING_WS_URL`, envoie le JWT dans la trame STOMP `CONNECT` et reconnecte avec backoff (`src/services/socket/reverb.client.ts`). La Gateway autorise seulement l’upgrade HTTP de `/ws/messaging`; le commentaire et la configuration indiquent que le JWT est vérifié sur le `CONNECT` par le messaging service (`api-gateway/.../SecurityConfig.java:78-81`).

Le hardening rejette toute origine non présente dans la liste autorisée (`SecurityHardeningFilter.java:51-55`, `:139-145`). Il ne rejette pas le JWT à cet endroit. `ORIGIN_NOT_ALLOWED` signifie donc d’abord que l’origine envoyée par le client n’est pas admise. La correction sûre est de déterminer l’origine réellement envoyée par la plateforme mobile et d’ajouter uniquement les origines nécessaires à la configuration autorisée ; il ne faut pas autoriser `*` ni désactiver le filtre.

## I. Audit des 19 microservices métier

Le tableau reprend les 19 services listés dans l’inventaire de production fourni. Tous déclarent le `security-hardening-starter` dans leur `pom.xml` local. La colonne « risque » distingue le code local de la configuration effective qui dépend des variables d’environnement et de Config Server.

| Service | Validation JWT | Source de configuration | Risque identifié | Preuve |
|---|---|---|---|---|
| `booking-service` | Oui, HMAC resource server, rôles | `cloud-conf-yeyamo/booking-service.properties`, `jwt.secret` | Secret aval différent | `infrastructure/security/SecurityConfig.java`, decoder HMAC |
| `catalog-service` | Oui, HMAC resource server, routes GET publiques et gestion protégée | `cloud-conf-yeyamo/catalog-service.properties`, `jwt.secret` | Secret aval différent ; routes GET/écriture à distinguer | `infrastructure/security/SecurityConfig.java` |
| `commerce-service` | Oui, HMAC resource server | `cloud-conf-yeyamo/commerce-service.properties`, `jwt.secret` | Secret aval différent | `config/SecurityConfig.java` |
| `content-service` | Oui, HMAC resource server | `cloud-conf-yeyamo/content-service.properties`, `jwt.secret` | Secret aval différent | `infrastructure/security/SecurityConfig.java` |
| `country-config-service` | Resource server configuré issuer/JWK ; hardening présent | `cloud-conf-yeyamo/country-config-service.properties` et application locale | Risque de mode JWK/issuer incompatible avec JWT HMAC ; vérifier surcharge effective | `config/SecurityConfig.java`, propriétés `spring.security.oauth2.resourceserver.jwt.*` |
| `culture-service` | Oui, HMAC resource server | `cloud-conf-yeyamo/culture-service.properties`, `jwt.secret` | Secret aval différent | `infrastructure/web/SecurityConfig.java` |
| `discovery-service` | HMAC decoder local, propriété issuer-uri également présente | `cloud-conf-yeyamo/discovery-service.properties` | Ambiguïté entre Auth HMAC et provider issuer | `infrastructure/security/SecurityConfig.java` |
| `event-service` | Oui, HMAC resource server | `cloud-conf-yeyamo/event-service.properties`, `jwt.secret` | Secret aval différent | `config/SecurityConfig.java` |
| `feed-service` | HMAC decoder local ; issuer/JWK également configurés | `cloud-conf-yeyamo/feed-service.properties` | Ambiguïté et dépendance possible à une URL de clés | `infrastructure/security/SecurityConfig.java` |
| `interaction-service` | Oui, HMAC resource server | `cloud-conf-yeyamo/interaction-service.properties`, `jwt.secret` | Secret aval différent | `infrastructure/security/SecurityConfig.java` |
| `media-service` | Oui, HMAC resource server, scope culture | `cloud-conf-yeyamo/media-service.properties`, `jwt.secret` | Secret aval différent ; scope peut donner `403` après un JWT valide | `infrastructure/security/SecurityConfig.java` |
| `messaging-service` | Oui, HMAC resource server ; STOMP séparé | `cloud-conf-yeyamo/messaging-service.properties`, `jwt.secret` | Secret aval différent ; origine WebSocket distincte | `infrastructure/web/SecurityConfig.java` |
| `notification-service` | Oui, HMAC resource server | `cloud-conf-yeyamo/notification-service.properties`, `jwt.secret` | Secret aval différent | `infrastructure/web/SecurityConfig.java` |
| `partner-service` | Oui, HMAC resource server | `cloud-conf-yeyamo/partner-service.properties`, `jwt.secret` | Secret aval différent | `infrastructure/security/SecurityConfig.java` |
| `payment-service` | Oui, HMAC resource server ; webhooks publics | `cloud-conf-yeyamo/payment-service.properties`, `jwt.secret` | Secret aval différent ; ne pas ajouter de retry non idempotent | `infrastructure/web/SecurityConfig.java` |
| `place-service` | Oui, HMAC resource server | `cloud-conf-yeyamo/place-service.properties`, `jwt.secret` | Secret aval différent ; GET public/privé dépend de la route | `config/SecurityConfig.java` |
| `registry-service` | Basic Auth/Eureka, pas JWT métier | `registry-service/src/main/resources/application.properties` | Le secret JWT de l’inventaire n’est pas une preuve de validation JWT | `SecurityConfig.java`, commentaire d’exclusion du hardening |
| `ticket-service` | Decoder HMAC local, propriétés issuer/JWK locales | `ticket-service/src/main/resources/application.properties` et env | Configuration possiblement incompatible ; opérations sensibles protégées par scopes | `infrastructure/config/SecurityConfig.java` |
| `user-service` | Oui, HMAC resource server | `cloud-conf-yeyamo/user-service.properties`, `jwt.secret` | Secret aval différent | `infrastructure/security/SecurityConfig.java` |

### Observation transversale

Le fait qu’un service possède `JWT_SECRET` ne suffit pas à prouver son decoder effectif. Le starter commun intercepte tout Bean `JwtDecoder` (`HardeningAutoConfiguration.java:24-38`) et peut sélectionner HMAC, clés précédentes ou JWK selon les propriétés résolues. L’audit de production doit donc capturer les propriétés non secrètes effectivement résolues : mode HMAC/JWK, issuer, audience, identifiant de configuration et empreinte non réversible de clé, sans capturer la clé.

## J. Vulnérabilités et risques classés

### P0 — Critique

1. **Divergence de secrets JWT entre signature et validation — CONFIRMÉE sous réserve de l’inventaire de production.** Impact : indisponibilité authentifiée de la majorité des fonctionnalités privées. Correction : alignement contrôlé de la configuration effective, puis rotation planifiée si nécessaire.
2. **Mélange HMAC / issuer-JWK — CONFIRMÉ dans les fichiers, effet effectif à confirmer.** Impact : refus systématique ou dépendance à un provider de clés qui n’est pas démontré dans l’Auth Service.

### P1 — Important

3. **Refresh après `401` aval — CONFIRMÉ dans le code, causalité de la rafale PROBABLE.** Impact : rotation excessive, rate-limit, réutilisation concurrente possible.
4. **Absence de MFA de connexion — CONFIRMÉE.** Impact : le comportement attendu « OTP à chaque login » n’est pas couvert ; le code couvre seulement la vérification e-mail persistante.
5. **Configuration effective non observable sans exposer les secrets — CONFIRMÉ comme manque opérationnel.** Impact : difficulté à distinguer signature, issuer, audience et JWK.

### P2 — Complémentaire

6. **Origine WebSocket refusée — PROBABLE.** Impact : messagerie temps réel indisponible, sans impliquer nécessairement la REST ou le JWT.
7. **Route stats sociales ambiguë — PROBABLE.** Impact : une URL sans UUID peut manquer la règle publique prévue.
8. **Absence de `jti` et révocation access token immédiate — CONFIRMÉE.** Le système révoque les refresh tokens ; un access token déjà émis reste cryptographiquement valable jusqu’à expiration sauf contrôle d’état aval. Ce point n’explique pas le `401` observé mais doit être pris en compte dans le modèle de menace.

## K. Plan de correction sécurisé

### P0 — Aligner et prouver la validation JWT

1. **Inventorier sans révéler les secrets** les variables résolues dans Auth, Gateway et chaque service : présence, longueur, empreinte SHA-256 tronquée, issuer, audience, mode HMAC/JWK et source de propriété.
2. **Choisir explicitement un seul modèle** pour les JWT applicatifs : HMAC partagé avec rotation contrôlée, ou OIDC/JWK réellement servi par un provider. Ne pas conserver un mélange implicite.
3. **Aligner Auth/Gateway/services** sur la même clé courante, issuer et audience. Conserver une ancienne clé uniquement pendant une fenêtre de rotation documentée.
4. **Tester une requête réelle non sensible** : Auth signe, Gateway valide, un service cible valide à son tour, puis un mauvais secret est rejeté. Les journaux doivent contenir seulement type d’erreur, service et corrélation.

**Critère d’acceptation :** un JWT valide est accepté par Auth, Gateway et le service cible ; un JWT avec mauvaise clé, issuer, audience ou expiration est refusé par tous les validateurs.

### P1 — Stabiliser refresh et contrat OTP

1. Ajouter une télémétrie non sensible sur `401` : origine du rejet (Gateway/aval), route, service, corrélation, sans token.
2. Côté mobile, ne déclencher le refresh automatique que pour un `401` clairement lié à l’expiration/invalidité du token, pas pour une réponse aval indiquant une configuration de clé incohérente. Cela nécessite un contrat d’erreur stable avant modification.
3. Tester concurrence, rotation, réutilisation et rate-limit sans augmenter les limites.
4. Décider explicitement si l’OTP est une activation d’e-mail ou une MFA de connexion. Si MFA, concevoir une session partielle et un token de challenge distinct avant émission de l’access token.

### P2 — WebSocket et observabilité

1. Capturer l’origine WebSocket réelle côté client et Gateway.
2. Autoriser uniquement les origines mobiles réellement nécessaires.
3. Vérifier le JWT sur STOMP `CONNECT` et limiter les reconnexions après `ORIGIN_NOT_ALLOWED` ou erreur d’authentification.

### Risques de régression et retour arrière

- Aligner les secrets peut invalider des tokens existants signés avec l’ancien groupe ; prévoir une fenêtre `previous-secrets` et une expiration contrôlée.
- Une correction partielle Gateway-only laisse les services métier en rejet aval.
- Un passage JWK sans endpoint JWKS réellement disponible peut empêcher le démarrage ou provoquer des erreurs de validation.
- Le retour arrière consiste à restaurer la configuration précédente validée, sans supprimer les données de sessions ni désactiver les validateurs ; les tokens émis pendant une rotation doivent être traités selon la fenêtre de compatibilité décidée.
- Aucun secret de production ne doit être écrit dans Git, dans les logs ou dans ce rapport.

## L. Tests de non-régression

### Tests à exécuter après correction, dans un environnement isolé

| Domaine | Scénarios indispensables | Résultat attendu |
|---|---|---|
| JWT | bonne clé, mauvaise clé, expiration, mauvais issuer, mauvaise audience, JWT malformé | `200` uniquement pour le premier ; `401` pour les autres |
| Claims | `token_type=refresh`, compte inactif, e-mail non vérifié, sujet invalide | `401`/`403` selon le contrat, jamais accès métier |
| Chaîne Gateway | Gateway accepte puis service cible accepte ; Gateway accepte puis service cible rejette | corrélation permettant d’identifier l’étage du rejet |
| Headers | Authorization présent, absent, modifié ; corrélation conservée | aucun secret en log ; comportement déterministe |
| Refresh | succès, expiration, révocation, réutilisation, appels concurrents | rotation unique ; réutilisation détectée ; pas de boucle |
| Mobile | plusieurs requêtes simultanées en `401`, refresh unique, échec refresh | pas de rafale non bornée ; session effacée seulement si refresh invalide |
| OTP | code valide, expiré, incorrect, déjà utilisé, compte PENDING puis ACTIVE | aucune émission de token avant vérification ; code consommé une fois |
| Gateway disponibilité | service rapide, lent, indisponible, circuit ouvert, rétablissement | distinguer `200`, timeout/`503`, fallback et `401` |
| WebSocket | origine autorisée/non autorisée, STOMP sans/avec Bearer | rejet de l’origine non autorisée ; auth au CONNECT |

### Tests présents mais non exécutés

Le dépôt contient notamment :

- `auth-service/src/test/.../JwtAuthenticationFilterSecurityTests.java` ;
- `auth-service/src/test/.../JwtServiceEligibilityTests.java` ;
- `auth-service/src/test/.../JwtServiceCountryClaimTest.java` ;
- `auth-service/src/test/.../OtpServiceAtomicTests.java` ;
- `auth-service/src/test/.../RefreshTokenServiceSecurityTests.java` ;
- `security-hardening-starter/src/test/.../StrictJwtDecodersTests.java` ;
- tests Gateway de contrats, fallback, corrélation et rate-limit sous `api-gateway/src/test`.

**Résultat de cet audit :** aucun test Maven, build, Docker Compose, service, endpoint local ou endpoint de production n’a été lancé. Cette décision respecte le mode lecture seule et l’interdiction de démarrer l’infrastructure. Les résultats ci-dessus sont des conclusions statiques et ne remplacent pas une preuve d’exécution avec les variables effectives.

## M. Informations manquantes pour clôturer la preuve production

1. Empreinte non réversible de `JWT_SECRET` réellement résolu dans Auth, Gateway et un service métier, produite indépendamment dans chaque conteneur sans divulguer la valeur.
2. Valeurs non secrètes réellement résolues pour `jwt.issuer`, `jwt.audience`, `jwt.jwk-set-uri` et `spring.security.oauth2.resourceserver.jwt.*`.
3. Pour un même `X-Correlation-ID`, logs Gateway puis service cible montrant l’étape exacte du `401`.
4. URL exacte de `/api/v1/users/social/stats`, notamment présence ou absence de l’identifiant utilisateur.
5. Code HTTP et corps normalisé du `401` avant et après refresh.
6. Origine WebSocket réellement envoyée par chaque plateforme mobile.
7. Confirmation que `CORS_ALLOWED_ORIGINS`, `JWT_ISSUER_URI`, `JWT_JWK_SET_URI` et les variables de secret ne sont pas surchargées par Dokploy/Config Server.

## N. Commandes de validation recommandées

À exécuter uniquement dans un environnement isolé après validation des prérequis et sans afficher de secrets :

```powershell
mvn -pl auth-service,api-gateway,security-hardening-starter -am test
mvn -pl api-gateway -am -DskipTests package
git diff --check
```

Puis exécuter un test d’intégration ciblé avec un seul service métier et des secrets de test dédiés. Ne pas lancer les 32 microservices, ne pas appeler la production, ne pas déployer et ne pas publier d’image.

## O. Réponse finale obligatoire

### Pourquoi le login peut-il finir mais les fonctionnalités renvoient-elles ensuite `401` ?

1. **Cause racine démontrée conditionnellement :** l’inventaire de production fourni montre le groupe A pour Auth/Gateway et le groupe B pour les microservices. Le code vérifie bien le JWT à plusieurs étages ; une signature produite avec A est rejetée par un decoder HMAC B.
2. **Causes secondaires :** le mobile rafraîchit après les `401` avals ; la rotation répond alors `200` sans corriger la clé aval, ce qui peut provoquer une rafale et `RATE_LIMITED`. Des configurations issuer/JWK coexistent avec le mode HMAC.
3. **Hypothèses non résolues :** valeurs effectives Config Server/Dokploy, issuer/audience réellement injectés, URL exacte de stats sociales, origine WebSocket, et éventuelle différence entre la version déployée et le dépôt local.
4. **Correction minimale :** aligner la clé de validation effective et les claims `iss`/`aud` entre Auth, Gateway et services, puis prouver la chaîne par corrélation ; ne pas désactiver Spring Security, le rate-limit ou le CircuitBreaker.
5. **Risque d’une correction partielle :** corriger seulement la Gateway laisse les services avals en `401`; corriger seulement Auth casse la Gateway ; changer uniquement le refresh masque le défaut cryptographique et peut invalider des sessions.
6. **Ordre recommandé :** (P0) inventaire résolu et alignement cryptographique ; (P1) tests Gateway → service et refresh/OTP ; (P2) WebSocket/origines et observabilité.
7. **Tests indispensables :** bonne/mauvaise clé, issuer/audience, expiration, propagation Authorization, refresh concurrent, OTP à usage unique, erreurs de disponibilité et rétablissement.

**Aucun fichier autre que ce rapport n’a été créé ou modifié pendant cette mission.**
