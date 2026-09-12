# Production remaining services audit

Audit et correctifs ciblés pour `ads-delivery-service`, `culture-service` et `media-service`. Aucun microservice, migration, secret, commit ou push n'a été ajouté.

## 1. ADS-DELIVERY-SERVICE

### Cause racine

- Compose injectait déjà `SERVER_PORT=8111`, mais le Dockerfile contenait `ENV SERVER_PORT=8095` et un healthcheck sur `localhost:8095/actuator/health`.
- Le service ne définissait aucune `SecurityFilterChain` locale. La sécurité OAuth2 par défaut renvoyait donc `401` pour les probes Actuator.
- Les probes Spring Boot n'étaient pas explicitement activées dans la configuration du service.

### Fichiers modifiés

- `ads-delivery-service/Dockerfile`
- `ads-delivery-service/src/main/resources/application.properties`
- `ads-delivery-service/src/main/java/com/yeyamo_mobile/api/ads_delivery_service/infrastructure/security/SecurityConfig.java`
- `ads-delivery-service/src/test/java/com/yeyamo_mobile/api/ads_delivery_service/AdsDeliveryServiceApplicationTests.java`
- `ads-delivery-service/src/test/resources/application.properties`

### Correction

- Le port applicatif par défaut et celui de l'image sont maintenant `8111`.
- Le healthcheck Docker appelle `curl --fail --silent http://localhost:8111/actuator/health/readiness`.
- L'image installe explicitement `curl`; elle continue de tourner avec l'utilisateur non-root `spring`.
- `management.endpoint.health.probes.enabled=true` rend disponibles `liveness` et `readiness`.
- Une chaîne de sécurité stateless conserve l'authentification JWT sur `/api/v1/ads/**` et sur toute autre route.

### Sécurité Actuator

Seuls les chemins suivants sont publics :

- `/actuator/health`
- `/actuator/health/**`

Il n'existe aucun `permitAll()` sur `/actuator/**`, ni sur les routes métier. Le décodeur JWT utilise le même `jwt.secret` et le même starter de validation stricte que les autres services HMAC du dépôt.

### Résultat tests

`mvn -pl ads-delivery-service -am clean test` : **PASS — 18 tests**.

Le test d'intégration vérifie sans authentification : `health`, `health/readiness` et `health/liveness`; il vérifie aussi qu'un `POST /api/v1/ads/select` reste `401`. Dans le contexte de test Redis est volontairement indisponible, donc le health global peut être `503`; la readiness utilisée par Docker reste `200`.

## 2. CULTURE-SERVICE

### Cause racine RestClient.Builder

`CultureServiceApplication` importait correctement `CountryConfigClient`, mais ne fournissait pas le bean `RestClient.Builder` requis par son constructeur. Dans cette combinaison Spring Boot 4.1 / WebMVC, aucun builder n'était auto-configuré pour ce contexte.

### Fichiers modifiés

- `culture-service/src/main/java/com/yeyamo_mobile/api/culture_service/CultureServiceApplication.java`
- `culture-service/src/test/java/com/yeyamo_mobile/api/culture_service/CultureRestClientConfigurationTest.java`
- `culture-service/src/test/java/com/yeyamo_mobile/api/culture_service/CulturePostgresIntegrationTest.java`

### Correction retenue

- Ajout d'un unique bean `RestClient.Builder` avec `@ConditionalOnMissingBean` et `RestClient.builder()`.
- Ce choix reprend le pattern existant dans `analytics-service`, `booking-service` et `catalog-service`, tout en évitant un second bean si une configuration commune est ajoutée ultérieurement.
- `CountryConfigClient` est inchangé : URL de service, timeouts de connexion/lecture de deux secondes, cache et circuit breaker sont conservés. Il ne définissait pas de header interne dans son contrat actuel, donc aucun header n'a été supprimé ou inventé.

### Résultat tests

`mvn -pl culture-service -am clean test` : **PASS — 16 tests, 2 ignorés**.

Le nouveau test ciblé démarre un `ApplicationContext`, fournit le builder réellement issu de l'application et confirme l'instanciation de `CountryConfigClient`. Le test Postgres/Testcontainers, lorsqu'il est activé par `RUN_TESTCONTAINERS=true`, vérifie désormais aussi que le contexte complet fournit le builder et le client.

## 3. MEDIA-SERVICE

### Cause racine `/app/storage`

`LocalObjectStorageAdapter` était un `@Component` inconditionnel. Son constructeur créait immédiatement son répertoire configuré, donc il tentait `Files.createDirectories("/app/storage")` au démarrage même lorsque R2 était le backend de production.

### Rôle de LocalObjectStorageAdapter

Il sert uniquement au stockage local explicite et à la compatibilité de lecture/suppression de clés locales héritées. Les nouvelles écritures passent par `RoutedObjectStorageAdapter` vers R2.

### Stratégie retenue

- R2 est conditionné par `r2.enabled=true` ; la configuration de production l'impose.
- Le backend local est conditionné par `media.storage.local.enabled=true` et est explicitement désactivé en production.
- Le routeur reçoit le backend local de façon optionnelle. Si une ancienne clé locale est rencontrée alors que ce backend est désactivé, il renvoie `LEGACY_LOCAL_STORAGE_UNAVAILABLE`.
- Aucun accès à `/app/storage` n'est donc requis lorsque R2 est actif. Aucune permission large, exécution root, ni création de répertoire local de production n'a été ajoutée.
- En local/dev, l'adaptateur local reste disponible en activant explicitement `media.storage.local.enabled=true` avec un répertoire inscriptible ; `r2.enabled=false` sélectionne alors ce backend seul.

### Fichiers modifiés

- `media-service/src/main/java/com/yeyamo_mobile/api/media_service/infrastructure/storage/LocalObjectStorageAdapter.java`
- `media-service/src/main/java/com/yeyamo_mobile/api/media_service/infrastructure/storage/R2StorageConfiguration.java`
- `media-service/src/main/java/com/yeyamo_mobile/api/media_service/infrastructure/storage/R2StorageAdapter.java`
- `media-service/src/main/java/com/yeyamo_mobile/api/media_service/infrastructure/storage/RoutedObjectStorageAdapter.java`
- `media-service/src/test/java/com/yeyamo_mobile/api/media_service/MediaServiceApplicationTests.java`
- `media-service/src/test/java/com/yeyamo_mobile/api/media_service/infrastructure/storage/LocalObjectStorageConditionTests.java`
- `media-service/src/test/java/com/yeyamo_mobile/api/media_service/infrastructure/storage/RoutedObjectStorageAdapterTests.java`
- `media-service/src/test/resources/application.properties`
- `cloud-conf-yeyamo/media-service.properties`
- `docker-compose.production.yml`
- `.env.example`

### Comportement R2 public/private

- `r2.public-bucket-name=${R2_PUBLIC_BUCKET_NAME}` et `r2.private-bucket-name=${R2_PRIVATE_BUCKET_NAME}` remplacent entièrement l'ancien nom unique dans la configuration active.
- Les clés `public/...` vont exclusivement dans le bucket public et les clés `private/...` exclusivement dans le bucket privé.
- Une clé non classifiée est refusée par `R2StorageAdapter`; une clé locale héritée ne peut ni tomber dans le bucket public ni être redirigée vers R2 sans configuration locale.
- Les flux upload/get/delete restent routés par le même port de stockage. La logique de contenu protégé et de signed URL existante n'a pas été modifiée.

### Résultat tests

`mvn -pl media-service -am clean test` : **PASS — 75 tests**.

Les tests couvrent la sélection des deux buckets pour upload/read/delete, l'absence de fallback privé-vers-public, R2 actif sans instanciation du backend local, le refus explicite de clés locales en production et l'activation locale explicite.

## 4. Docker Compose

- Le service Ads résout `SERVER_PORT=8111` dans `docker-compose.production.yml` (hérité de `docker-compose.yml`). Son healthcheck final est fourni par l'image et cible `localhost:8111/actuator/health/readiness`.
- Le service Media résout `R2_ENABLED=true` et `MEDIA_STORAGE_LOCAL_ENABLED=false` en production.
- Les deux noms de buckets requis restent `R2_PUBLIC_BUCKET_NAME` et `R2_PRIVATE_BUCKET_NAME`; aucune valeur secrète n'a été ajoutée au dépôt.

## 5. Commandes de validation

| Commande | Résultat |
| --- | --- |
| `mvn -pl ads-delivery-service -am clean test` | PASS — 18 tests |
| `mvn -pl culture-service -am clean test` | PASS — 16 tests, 2 ignorés par condition Testcontainers |
| `mvn -pl media-service -am clean test` | PASS — 75 tests |
| `docker compose -f docker-compose.production.yml config --quiet` | PASS avec valeurs de contrôle locales non secrètes |
| résolution Compose | Ads `SERVER_PORT=8111`, Media `R2_ENABLED=true`, `MEDIA_STORAGE_LOCAL_ENABLED=false` |
| `git diff --check` (dépôt principal et Cloud Config) | PASS |

Recherches de régression : aucune occurrence du healthcheck Ads `8095/actuator/health` ne subsiste ; l'occurrence de port 8095 restante appartient à `recommendation-service`, hors périmètre. Aucune occurrence de `R2_BUCKET_NAME` ne reste dans la configuration active.

## 6. Risques restants éventuels

- Le daemon Docker local n'était pas disponible, donc l'image Ads n'a pas été exécutée localement. Le Dockerfile installe explicitement `curl`, le healthcheck est statiquement exact et les endpoints sont couverts par le test Spring.
- Un redéploiement des images Ads/Culture/Media et de la configuration Cloud Config est nécessaire pour appliquer ces correctifs à l'environnement déjà en ligne.
- Le health global Ads peut refléter une dépendance indisponible, comme Redis en test. Docker utilise volontairement la readiness Spring Boot, qui est la probe adéquate pour déterminer si le processus peut recevoir du trafic.

PRODUCTION_REMAINING_SERVICES_READY=YES
