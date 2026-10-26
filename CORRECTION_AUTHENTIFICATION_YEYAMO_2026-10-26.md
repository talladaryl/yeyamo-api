# Correction complète et sécurisée de l’authentification Yeyamo

**Date :** 2026-10-26<br>
**Périmètres :** `yeyamo-api`, `yeyamo-mobile`<br>
**Mode :** corrections locales ciblées, aucun déploiement<br>
**Versions conservées :** Spring Boot `4.1.0`, Spring Cloud `2025.1.2`, Expo déclaré localement `~57.0.22`

## A. Résumé exécutif

Les corrections locales réalisées traitent les causes démontrées par l’audit :

- Les configurations JWT versionnées utilisent désormais une source explicite `JWT_SECRET`; les valeurs codées en dur du Compose local ont été remplacées par une variable obligatoire.
- La configuration HMAC applicative est explicite pour les services qui mélangeaient issuer/JWK et HMAC.
- Le starter commun échoue au démarrage si `jwt.secret` est absent au lieu de démarrer avec une validation ambiguë.
- La confirmation OTP d’un compte `PENDING` active maintenant le compte et crée une session complète dans la même transaction logique.
- Une confirmation d’e-mail sur un compte déjà actif ne crée jamais de session implicite.
- Le mobile persiste la session reçue après OTP et ne revient au login que si l’ancien backend ne fournit pas de session.
- Le mobile ne rafraîchit plus un JWT encore valide mais rejeté par la Gateway ou un service aval ; ce cas termine la session au lieu de provoquer une rafale de refresh.
- Un refus STOMP permanent arrête les reconnexions WebSocket automatiques.

La divergence des secrets réellement déployés dans Dokploy n’a pas été modifiée : aucun secret de production n’a été lu, changé ou publié. Le déploiement doit encore utiliser une valeur cohérente de `JWT_SECRET` pour Auth, Gateway et les services métier.

## B. Modifications backend

### Session après OTP

| Fichier | Modification | Justification |
|---|---|---|
| `auth-service/src/main/java/com/yeyamo_mobile/api/auth_service/dto/EmailVerificationResponse.java` | Nouveau DTO conservant `message` et ajoutant les champs de session facultatifs | Compatibilité avec l’ancien client et support de l’auto-session |
| `auth-service/src/main/java/com/yeyamo_mobile/api/auth_service/controller/AuthController.java` | La confirmation retourne `EmailVerificationResponse` | Le mobile peut recevoir les tokens sans changer l’endpoint |
| `auth-service/src/main/java/com/yeyamo_mobile/api/auth_service/service/AuthService.java` | `confirmEmailVerification` consomme l’OTP, active un compte `PENDING`, puis appelle `tokensFor` | La session n’est créée qu’après OTP valide et activation |
| `auth-service/src/test/java/com/yeyamo_mobile/api/auth_service/service/AuthServiceEmailVerificationTests.java` | Deux tests : activation/session et absence de session pour un compte actif | Protection contre le bypass de session implicite |

Le critère `PENDING` est la preuve locale du contexte d’inscription. L’OTP reste lié à l’e-mail et au purpose `email-verification` dans Redis ; il est temporaire, consommé atomiquement et limité par les règles existantes. Un compte déjà `ACTIVE` ne reçoit aucun access token ni refresh token via cette confirmation.

### Cohérence JWT et secrets

| Fichier | Modification | Justification |
|---|---|---|
| `cloud-conf-yeyamo/application.properties` | Ajout de `jwt.secret=${JWT_SECRET}` dans la configuration commune | Une propriété obligatoire et homogène pour les services |
| `cloud-conf-yeyamo/country-config-service.properties` | Suppression des defaults issuer/JWK legacy ; référence HMAC commune | Évite un provider OIDC implicite non servi par Auth |
| `cloud-conf-yeyamo/discovery-service.properties` | Remplacement de l’issuer URI legacy par la référence HMAC | Aligne le decoder local avec les JWT HS256 Auth |
| `cloud-conf-yeyamo/feed-service.properties` | Suppression des URI issuer/JWK legacy et référence HMAC | Évite le mélange de contrats de validation |
| `country-config-service/src/main/resources/application.properties` | Suppression des defaults Keycloak/local et référence `JWT_SECRET` | Pas de fallback OIDC local contradictoire |
| `country-config-service/src/main/java/com/yeyamo_mobile/api/country_config_service/config/SecurityConfig.java` | Ajout d’un `JwtDecoder` HMAC avec validation de longueur | Le service conserve son resource server sans dépendance JWKS implicite |
| `ticket-service/src/main/resources/application.properties` | Remplacement des URI issuer/JWK codées en dur par `jwt.secret=${JWT_SECRET}` | Supprime une configuration locale incompatible avec Auth HS256 |
| `graph-service/src/main/resources/application.properties` | Suppression du fallback `change-me...` | Un secret de développement dangereux ne peut plus être utilisé par défaut |
| `security-hardening-starter/src/main/java/com/yeyamo/security/hardening/HardeningAutoConfiguration.java` | `jwt.secret` devient requis lorsqu’un `JwtDecoder` est post-traité | Échec explicite au démarrage au lieu d’une configuration partielle |
| `docker-compose.yml` | Tous les `JWT_SECRET` de service utilisent `${JWT_SECRET:?JWT_SECRET is required}` | Une seule variable d’environnement est requise ; aucune clé locale codée en dur |
| `.env.example` | Documentation de `JWT_SECRET`, `JWT_PREVIOUS_SECRETS`, issuer, audience et JWK optionnel | Contrat local explicite sans valeur sensible |

Les mécanismes existants de rotation (`jwt.previous-secrets`), issuer, audience, expiration et éligibilité de compte sont conservés. Le support JWK du starter n’est pas supprimé ; il reste disponible si un contrat OIDC/JWK est explicitement choisi et configuré, mais il n’est plus activé par les defaults legacy des services corrigés.

## C. Modifications mobile

| Fichier | Modification | Justification |
|---|---|---|
| `E:\Daryl\yeyamo-mobile\src\features\auth\types.ts` | Ajout du type `EmailVerificationResponse` | Décrit le contrat de confirmation avec session facultative |
| `E:\Daryl\yeyamo-mobile\src\features\auth\auth.api.ts` | Confirmation typée sur `EmailVerificationResponse` | Permet d’inspecter `authenticated` et les tokens |
| `E:\Daryl\yeyamo-mobile\src\features\auth\auth.service.ts` | Ajout de `verifyEmail`, réutilisant `persistSession` | Stockage SecureStore, store global, WebSocket et services dépendants restent centralisés |
| `E:\Daryl\yeyamo-mobile\src\features\auth\useAuth.ts` | `verifyCode` retourne le résultat d’authentification | L’écran peut choisir la route sans fabriquer de token |
| `E:\Daryl\yeyamo-mobile\src\app\(auth)\verify-code.tsx` | Navigation vers `/(tabs)` si session reçue, sinon fallback login | Supprime la reconnexion inutile après activation |
| `E:\Daryl\yeyamo-mobile\src\services\api\client.ts` | Détection d’un access token expiré, réutilisation du token déjà rafraîchi et échec terminal si le token est encore valide | Empêche les refreshs répétés sur une divergence de clé ou issuer aval |
| `E:\Daryl\yeyamo-mobile\src\services\socket\reverb.client.ts` | Arrêt des reconnexions après une trame STOMP `ERROR` | Évite les tentatives répétées après refus permanent |

Le parcours d’inscription reste : inscription → `PENDING` → OTP → confirmation → session persistée → route guard existant vers intérêts ou feed. Le code ne conserve pas le mot de passe et ne génère aucun token côté mobile.

## D. Sécurité JWT

### Contrat local après correction

```text
Auth Service       signe HS256 avec JWT_SECRET
API Gateway        valide avec JWT_SECRET + issuer + audience
Service métier     valide avec JWT_SECRET + issuer + audience
Ancienne clé       acceptée seulement via jwt.previous-secrets pendant rotation
Secret absent      démarrage refusé explicitement
```

Le Compose local ne porte plus de valeur par service. Il exige une variable `JWT_SECRET` unique au lancement. Cela empêche les divergences accidentelles causées par le fichier Compose local ; cela ne peut pas corriger une variable différente déjà injectée séparément par Dokploy.

### Production restant à configurer

À vérifier hors de ce dépôt, sans afficher les secrets :

1. empreinte SHA-256 tronquée de la valeur résolue dans Auth, Gateway et au moins un service métier ;
2. même `JWT_ISSUER` et `JWT_AUDIENCE` partout ;
3. `JWT_PREVIOUS_SECRETS` limité à la fenêtre de rotation ;
4. `JWT_JWK_SET_URI` vide pour le contrat HMAC, sauf décision OIDC explicite ;
5. aucune variable Dokploy ou Config Server ne réintroduit les anciennes valeurs par service.

La correction locale ne modifie aucune valeur de production et ne réalise aucune rotation.

## E. Nouveau parcours OTP

### Inscription initiale

1. `POST /api/v1/auth/register` crée un compte `PENDING` et envoie un OTP `email-verification`.
2. `POST /api/v1/auth/email/verification/confirm` vérifie l’e-mail et consomme le code une fois.
3. Si le compte était `PENDING`, il passe `ACTIVE` et le backend crée access token + refresh token opaque via le même chemin que le login.
4. La réponse conserve `message` et fournit `authenticated=true`, `accessToken`, `refreshToken`, `tokenType`, `expiresIn` et `user`.
5. Le mobile valide la présence d’une session complète, la persiste dans SecureStore, met à jour Zustand, reconnecte les services et navigue vers `/(tabs)`.
6. Le route guard existant décide ensuite si `/interests` est nécessaire ou si le feed est accessible.

### Cas non sessionné

Si le compte est déjà `ACTIVE`, la confirmation peut mettre à jour l’e-mail si le code est valide mais renvoie `authenticated=false` et aucun token. Cela évite qu’un simple endpoint de vérification d’adresse devienne une prise de contrôle implicite d’un compte actif.

Si le backend déployé est encore ancien et renvoie seulement `{message}`, le mobile détecte l’absence de session et revient au login. Il ne fabrique pas de session locale.

## F. Gestion des sessions

- Les access/refresh tokens restent stockés dans Expo SecureStore.
- Le refresh token reste opaque et n’est jamais placé dans l’en-tête `Authorization`.
- `refreshPromise` conserve la déduplication des refreshs concurrents.
- Si une requête échoue avec un ancien token alors qu’un autre refresh a déjà remplacé le token SecureStore, elle rejoue une fois avec le token courant sans effectuer une nouvelle rotation.
- Un refresh automatique n’est tenté que si le JWT possède un `exp` arrivé ou proche de l’expiration.
- Un JWT encore valide mais rejeté en `401` est traité comme un échec terminal de session ; cela évite de masquer une erreur de clé, issuer, audience ou session révoquée par des refreshs successifs.
- Un échec de refresh efface la session et utilise le handler de redirection unique existant.
- Les protections backend de hash, rotation transactionnelle, révocation et réutilisation restent inchangées.

## G. WebSocket

### Correction locale

Le client STOMP cesse les reconnexions après réception d’une trame `ERROR`. Le JWT reste obligatoire dans la trame `CONNECT` côté messaging service.

### Origine

Le filtre backend continue de refuser les origines non autorisées ; aucune origine wildcard n’a été ajoutée. Le dépôt ne permet pas de déduire une origine native stable pour toutes les plateformes React Native. La valeur exacte doit donc être configurée dans `CORS_ALLOWED_ORIGINS`/`yeyamo.security.allowed-origins` après observation non sensible de l’origine réellement envoyée par la plateforme cible.

Une connexion native qui ne fournit pas d’`Origin` n’est pas rejetée par le contrôle actuel, mais elle doit toujours réussir l’authentification STOMP. Une origine fournie et non autorisée reste refusée.

## H. Tests

### Tests ajoutés

- `AuthServiceEmailVerificationTests.activatesPendingAccountAndCreatesSessionAfterValidOtp`.
- `AuthServiceEmailVerificationTests.doesNotCreateImplicitSessionForAlreadyActiveAccount`.

### Tests exécutés et réussis

- `mvn -pl auth-service,country-config-service,security-hardening-starter -am -DskipTests compile` — BUILD SUCCESS.
- `mvn -pl auth-service,security-hardening-starter -am test` — 65 tests Auth, 19 tests hardening et tests shared-lib, 0 échec.
- `mvn -pl api-gateway -am -DskipTests compile` — BUILD SUCCESS.
- `mvn -pl api-gateway -am test` — 26 tests Gateway, 0 échec.
- Tests Gateway exécutés : disponibilité, LoadBalancer sans instance, timeout, CircuitBreaker/fallback, rétablissement, corrélation et authentification simulée.
- `npx tsc --noEmit` dans `yeyamo-mobile` — réussite.
- ESLint ciblé sur les fichiers mobile modifiés — réussite.
- `docker compose -f docker-compose.yml config --no-interpolate` — réussite.
- `docker compose -f docker-compose.production.yml -f docker-compose.production.images.yml config --no-interpolate` — réussite.
- `git diff --check` — réussi dans le dépôt backend, le sous-dépôt de configuration et le dépôt mobile ; aucun déploiement lancé.

### Tests non exécutés

- Aucun microservice complet n’a été démarré.
- Aucun Docker Compose n’a été lancé ; seules les configurations ont été parsées.
- Aucun endpoint local ou de production n’a été appelé.
- Aucun test E2E mobile sur appareil/simulateur n’a été exécuté.
- Aucun test de la vraie origine WebSocket native n’a été exécuté.
- Aucun test de la valeur effective des secrets Dokploy n’est possible sans accès production, qui n’a pas été utilisé.

## I. Risques résiduels

1. **Production :** la divergence de `JWT_SECRET` doit encore être corrigée dans l’injection de configuration. Le code local ne peut pas imposer une valeur identique entre conteneurs indépendants.
2. **WebSocket :** l’origine native exacte n’est pas prouvée localement ; une valeur autorisée doit être définie explicitement par environnement.
3. **Refresh :** un `401` causé par une configuration serveur incohérente déconnecte désormais l’utilisateur au lieu de boucler. C’est volontaire et plus sûr, mais cela rend visible l’incident de configuration.
4. **MFA :** l’OTP reste une activation d’e-mail, pas une MFA à chaque connexion. Les connexions ultérieures sans OTP correspondent à la décision produit fournie.
5. **Partenaire :** le parcours partenaire possède une création de profil avant validation OTP ; il doit être validé séparément si ce flux appelle une route privée avant la session auto-émise.
6. **Compatibilité :** le mobile déclare Expo SDK 57 alors que l’instruction historique du dépôt mobile mentionne SDK 56 ; aucune dépendance n’a été changée. Les modifications utilisent uniquement les API TypeScript/Expo existantes.

## J. Préparation théorique du déploiement

Ce plan n’a pas été exécuté.

### Variables à vérifier

- `JWT_SECRET` identique pour Auth, Gateway et tous les services métier concernés.
- `JWT_ISSUER` identique.
- `JWT_AUDIENCE` identique.
- `JWT_PREVIOUS_SECRETS` uniquement pendant la rotation.
- `JWT_JWK_SET_URI` vide pour le mode HMAC, sauf contrat OIDC documenté.
- `CORS_ALLOWED_ORIGINS` contenant uniquement les origines Web/mobile réellement nécessaires.
- `INTERNAL_SERVICE_TOKEN` inchangé et configuré pour les endpoints internes.

### Ordre recommandé

1. Vérifier les empreintes non réversibles et les propriétés résolues sans exposer les secrets.
2. Déployer d’abord une configuration compatible avec la clé courante et l’ancienne clé si une rotation est nécessaire.
3. Déployer les bibliothèques/configurations de validation.
4. Déployer Auth Service, puis Gateway, puis les services métier par groupes contrôlés.
5. Déployer le mobile après confirmation du contrat de réponse OTP.
6. Vérifier login, confirmation OTP, profil, feed, messaging REST et une connexion STOMP.

### Risques et retour arrière

- Une clé différente invalide les tokens existants ; une fenêtre `previous-secrets` doit être planifiée.
- Un rollback partiel peut recréer le défaut Gateway valide / service aval `401`.
- Le retour arrière doit restaurer la dernière configuration cohérente, sans supprimer les sessions ni désactiver les validateurs.
- En cas d’augmentation immédiate des `401`, arrêter le déploiement du groupe suivant et corréler les logs par `X-Correlation-ID`.

## K. État Git et interdictions respectées

- Aucun commit créé.
- Aucun push effectué.
- Aucun déploiement ou redémarrage effectué.
- Aucun secret de production modifié.
- Aucune base de données ou migration exécutée.
- Aucun fichier de production Dokploy modifié.
- Les modifications de `cloud-conf-yeyamo` restent des modifications locales de son dépôt imbriqué ; aucun commit n’a été créé dans ce dépôt.

## L. Critères d’acceptation

| Critère | Statut local |
|---|---|
| OTP valide active un compte `PENDING` | Réussi par test unitaire |
| Session auto-créée après OTP | Réussi par test unitaire et contrat mobile |
| Compte actif ne reçoit pas de session via confirmation e-mail | Réussi par test unitaire |
| Compte non vérifié reste bloqué | Conservé par `SessionEligibilityPolicy` et tests existants |
| JWT HMAC cohérent localement | Configuration normalisée ; secrets production à vérifier |
| Mauvais JWT refusé | Tests hardening/JWT existants réussis |
| Refresh excessif limité | Correction mobile validée par TypeScript/ESLint ; E2E non exécuté |
| WebSocket refus permanent ne boucle pas côté client | Correction STOMP locale ; handshake d’origine non testé |
| Routes et protections métier conservées | Tests Gateway/Auth ciblés réussis |
