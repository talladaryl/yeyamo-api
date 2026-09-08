# Rapport Phase 9 — sécurité Turnstile, Google OAuth et itinéraires

## A. Turnstile — RÉSOLU

### Audit préalable

Le mécanisme Cloudflare existait déjà :

- `CloudflareTurnstileVerifier` appelle `POST https://challenges.cloudflare.com/turnstile/v0/siteverify` en `application/x-www-form-urlencoded` ; il transmet uniquement `secret`, `response` et, si disponible, `remoteip`.
- `TurnstileHttpConfiguration` définit `connectTimeout` et `readTimeout` à partir de `security.turnstile.timeout`, qui vaut cinq secondes par défaut.
- `.env.example` contenait déjà `TURNSTILE_SECRET_KEY=` ; aucune valeur réelle n'a été lue ni ajoutée.

### Écarts corrigés

- `RegisterRequest.turnstileToken` et `LoginRequest.turnstileToken` sont maintenant `@NotBlank` : le contrat HTTP impose donc le jeton.
- `AuthService.login` appelle maintenant `antiBotVerifier.verify(..., LOGIN, ...)` avant la recherche utilisateur, le contrôle de mot de passe et la logique de limitation. L’ancienne condition « seulement après le seuil de tentatives » a été supprimée.
- Une réponse Cloudflare `success: false` retourne désormais `400` avec `TURNSTILE_VERIFICATION_FAILED`.
- Une défaillance technique Cloudflare (timeout, erreur HTTP, réponse vide) est journalisée comme alerte et laisse passer la requête. C’est le repli explicite choisi pour ne pas bloquer toutes les connexions/inscriptions lors d’un incident externe ; une configuration activée mais sans secret reste une erreur de disponibilité explicite.

### Tests

`CloudflareTurnstileVerifierTests` utilise `MockRestServiceServer` : succès, refus de vérification et erreur serveur sont couverts sans appel réseau.

```text
mvn test -pl auth-service
Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## B. Google OAuth — RÉSOLU (flux ID token)

### Audit

`POST /api/v1/auth/oauth/google` accepte le contrat existant suivant :

```json
{ "idToken": "<JWT Google signé>" }
```

`OAuthTokenVerifier` construit un `NimbusJwtDecoder` sur le JWKS officiel Google (`https://www.googleapis.com/oauth2/v3/certs`), puis vérifie la signature lors du décodage, l’émetteur (`https://accounts.google.com` ou `accounts.google.com`), l’audience `oauth.google.client-id` et, en audience multiple, `azp`.

Le backend est donc compatible avec `expo-auth-session` lorsqu’il fournit un **ID token**. Il ne consomme pas d’`authorizationCode` et n’implémente pas l’échange OAuth PKCE côté serveur : le mobile doit conserver le flux ID token pour cet endpoint existant.

### Tests

`OAuthTokenVerifierSecurityTests` couvre désormais : ID token valide, signature invalide simulée et émetteur non Google, en plus des contrôles existants d’audience et d’email.

```text
mvn test -pl auth-service
Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## C. Proxy OpenRouteService — RÉSOLU

### Choix et audit

Le proxy est placé dans `place-service`, responsable des lieux et de la géographie. La recherche dépôt sur `openrouteservice`, `directions` et `routing` n’a révélé aucun appel direct mobile vers OpenRouteService ; le seul adaptateur de routes existant est un adaptateur Google Maps interne de `discovery-service`.

La route Gateway existante couvre déjà `/api/v1/places/**`; aucune règle Gateway additionnelle n’était nécessaire.

### Implémentation

Nouvelle route : `GET /api/v1/places/directions`, protégée JWT par la règle spécifique `authenticated()` placée avant le `permitAll()` générique des lectures place.

Paramètres :

- `originLat`, `originLng`, `destLat`, `destLng` : bornés aux coordonnées géographiques valides ;
- `mode` : `driving-car` (défaut) ou `foot-walking`.

`ORS_API_KEY` est lu uniquement depuis la configuration de service (`yeyamo.routing.open-route-service.api-key=${ORS_API_KEY:}`) et ajouté vide à `.env.example`. Il n’est jamais retourné ni accessible au mobile.

`DirectionsService` appelle `OpenRouteService /v2/directions/{mode}` avec timeout de dix secondes et transforme la réponse en DTO stable. Un cache mémoire de cinq minutes, fondé sur les coordonnées arrondies à cinq décimales et le mode, évite les appels répétés. Le quota 429, un timeout, une erreur provider et une clé absente produisent `503 ROUTING_UNAVAILABLE`.

### Tests

`DirectionsServiceTests` emploie `MockRestServiceServer` : réponse réussie, 429, timeout et cache (une seule requête HTTP pour deux trajets égaux arrondis).

```text
mvn test -pl place-service
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## D. Vérification globale — RÉSOLU

```text
mvn compile -DskipTests
Reactor Build Order: 41 modules
Reactor Summary: 41 SUCCESS
BUILD SUCCESS
```

## CONTRAT MOBILE

### Inscription et connexion

Ajouter un jeton Turnstile non vide à chaque appel :

```json
{
  "email": "user@example.com",
  "password": "mot-de-passe-solide",
  "turnstileToken": "jeton-obtenu-par-le-widget"
}
```

Pour l’inscription, `turnstileToken` est le même champ, en complément des champs géographiques déjà requis par `RegisterRequest`. En cas de jeton refusé : HTTP 400, code `TURNSTILE_VERIFICATION_FAILED`.

### Google OAuth

```json
{ "idToken": "<JWT Google provenant du flux Expo configuré pour retourner un ID token>" }
```

`authorizationCode` n’est pas accepté par l’endpoint actuel.

### Itinéraire

```text
GET /api/v1/places/directions?originLat=3.848&originLng=11.502&destLat=3.866&destLng=11.516&mode=driving-car
Authorization: Bearer <access-token>
```

Réponse :

```json
{
  "distanceMeters": 1234.0,
  "durationSeconds": 321.0,
  "geometry": [
    { "longitude": 11.502, "latitude": 3.848 },
    { "longitude": 11.516, "latitude": 3.866 }
  ]
}
```

## Fichiers modifiés

- `auth-service/.../RegisterRequest.java`
- `auth-service/.../LoginRequest.java`
- `auth-service/.../AuthService.java`
- `auth-service/.../CloudflareTurnstileVerifier.java`
- `auth-service/.../CloudflareTurnstileVerifierTests.java`
- `auth-service/.../OAuthTokenVerifierSecurityTests.java`
- `place-service/.../OpenRouteServiceProperties.java`
- `place-service/.../OpenRouteServiceConfiguration.java`
- `place-service/.../DirectionsService.java`
- `place-service/.../DirectionsResponse.java`
- `place-service/.../DirectionsServiceTests.java`
- `place-service/.../PlaceController.java`
- `place-service/.../SecurityConfig.java`
- `place-service/.../PlaceServiceApplication.java`
- `cloud-conf-yeyamo/place-service.properties`
- `.env.example`

PHASE 9 BACKEND RÉSOLUE — TURNSTILE, GOOGLE OAUTH ET ROUTING PRÊTS POUR LE FRONT
