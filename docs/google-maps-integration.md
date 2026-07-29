# Intégration Google Maps YeYamo

## Architecture

- Le mobile utilise `react-native-maps` pour l'affichage et Expo Location pour le GPS.
- Le mobile appelle uniquement l'API YeYamo pour le geocoding, le reverse geocoding et les itinéraires.
- `discovery-service` appelle Geocoding API et Routes API avec la clé serveur.
- PostGIS reste la source de vérité des recherches de proximité internes.
- OpenSearch reste la source de vérité de la recherche YeYamo. Places API n'est pas utilisée actuellement.

## Configuration

Définir localement ou dans les secrets EAS :

- `EXPO_PUBLIC_GOOGLE_MAPS_ANDROID_API_KEY` : clé limitée au package Android et au SHA-1.
- `EXPO_PUBLIC_GOOGLE_MAPS_IOS_API_KEY` : clé limitée au bundle iOS.

Définir uniquement sur le serveur :

- `GOOGLE_MAPS_SERVER_API_KEY` : Geocoding API et Routes API uniquement.
- `GOOGLE_MAPS_BASE_URL` : `https://maps.googleapis.com`.
- `GOOGLE_ROUTES_BASE_URL` : `https://routes.googleapis.com`.

Ne jamais préfixer la clé serveur avec `EXPO_PUBLIC_` et ne jamais l'ajouter aux logs, Swagger ou DTO.

## Flux

La carte affiche les lieux YeYamo issus du backend. Expo Location fournit la position courante. Les itinéraires transitent par `POST /api/v1/maps/route`. Le geocoding utilise `GET /api/v1/maps/geocode` et le reverse geocoding `GET /api/v1/maps/reverse-geocode`.

## Résilience et coûts

Les erreurs Google sont normalisées en erreurs YeYamo. L'écran lieu reste utilisable si le calcul d'itinéraire échoue. Les réponses 429 et 5xx bénéficient d'un retry limité. Surveiller quotas, facturation et taux d'erreur dans Google Cloud. Aucun cache persistant de résultats Google n'est ajouté avant validation de la politique de stockage applicable.

## Tests

Android et iOS nécessitent un nouveau development build après modification des clés natives. Tester carte, marqueurs, permission accordée/refusée, recentrage, route et panne backend sur appareils réels. Utiliser `npx expo-doctor`, `npx tsc --noEmit` et `discovery-service/mvnw test`.

## Dépannage

- Carte grise Android : vérifier package, SHA-1 de la build development et restriction Maps SDK for Android.
- Carte vide iOS : vérifier bundle ID et restriction Maps SDK for iOS.
- Réponse 403 backend : vérifier que la clé serveur autorise Geocoding API et Routes API.
- Ne jamais contourner une restriction en réutilisant une clé mobile côté serveur.
