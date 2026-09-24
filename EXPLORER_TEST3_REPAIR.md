# YEYAMO — EXPLORER / FEED PRODUCTION REPAIR (TEST3)

Date : 24 septembre 2026  
Périmètre : lecture Feed authentifiée et prévisualisation d’aventure. L’onglet Créer, l’upload, les Stories, les suggestions de lieu et `country-config-service` n’ont pas été modifiés.

## 1. Executive summary

Deux défauts backend indépendants empêchaient les parcours observés :

1. **Feed** — l’entité `FeedPostEntity` sélectionne `city_id`, `latitude` et `longitude`, mais les migrations `feed_posts` ne créaient aucune de ces colonnes. Sur une base construite par Flyway, la première requête JPA Feed échoue donc avec une erreur PostgreSQL de colonne inexistante et devient un HTTP 500.
2. **Aventure** — `recommendation-service` importait `CountryConfigClient` sans propriété d’URL. Il utilisait alors le fallback `http://country-config-service` (port 80), alors que le Docker Compose de production expose le service interne sur le port **8117**. C’est exactement le chemin à l’origine du `Connection refused` rapporté.

Les deux corrections sont backend ; aucune donnée fictive, aucun fallback vers le Feed public et aucune modification de l’onglet Créer n’ont été introduits.

## 2. Feed mobile call chain

```text
src/app/(tabs)/index.tsx
  → useFeed()
  → features/feed/feed.api.ts::feedApi.getFeed(page)
  → services/api/client.ts::apiGet()
  → GET {EXPO_PUBLIC_API_BASE_URL}/api/v1/feed?page=<n>&size=20
```

- L’URL est construite une seule fois : le client ajoute `/api/v1`, puis le Feed appelle `/feed`.
- Une build production impose `https://api.yeyamo.com` comme `EXPO_PUBLIC_API_BASE_URL`; l’URL attendue est donc `https://api.yeyamo.com/api/v1/feed`.
- `useFeed` est activé seulement après `isHydrated && isAuthenticated`.
- L’intercepteur lit le token depuis SecureStore et ajoute `Authorization: Bearer <token>` pour `/feed`.
- Un 401 déclenche un seul refresh, puis rejoue la requête avec le nouveau token.
- La query key sépare bien backend et sessions `demo-*`; les mocks ne sont jamais utilisés dans une session backend.
- `getNextPageParam` utilise `hasNext` et calcule `page + 1`, jamais la page courante.
- `Réessayer` appelle réellement `feed.refetch()`.
- Les états UI sont distincts : chargement, erreur HTTP, vide réel `200 + items=[]`, puis liste.

Le mobile ne masquait donc ni un double préfixe API, ni une requête prématurée `Bearer undefined`, ni un faux retry.

## 3. Feed backend call chain

```text
api-gateway (/api/v1/feed)
  → feed-service FeedController.feed(page, size, Authentication)
  → FeedQueryService.feed(user, page, size)
  → RedisFeedCacheAdapter (échec de cache toléré)
  → JpaFeedProjectionAdapter
  → feed_posts + métriques + signaux + auteurs mutés
  → FeedPage JSON
```

- `page` est borné à `>= 0` et `size` à `1..50`.
- Le cache Redis est versionné sous `feed:v2:<version>:<user>:<page>:<size>` ; erreur de lecture/désérialisation Redis = cache miss, pas HTTP 500.
- `FeedEventConsumer` filtre les événements non-Post avant de lire `payload.postId`. Les Stories ne sont donc pas désérialisées comme des Posts : **ALREADY_FIXED**, aucune modification dans cette passe.
- Une indisponibilité de l’ad service retourne le Feed organique ; elle ne fait pas échouer la page.

## 4. Actual HTTP failure observability

Un appel sans credential a été exécuté le 24 septembre 2026 :

```text
GET https://api.yeyamo.com/api/v1/feed?page=0&size=20
→ 401 Unauthorized
→ X-Correlation-Id: 89ec3b57-102b-491a-a2bf-e9a16187c4cf
→ corps vide
```

Cette réponse confirme que l’URL publique déployée et la protection Bearer sont atteignables. Elle ne reproduit volontairement pas le 500 d’un utilisateur connecté, car aucun JWT de test n’a été fourni ou lu depuis un poste utilisateur.

Le message observé dans l’application est un message normalisé 5xx. Le code mobile ne disposait pas d’un correlation id visible dans ce cas. Le défaut de schéma ci-dessous est toutefois démontré indépendamment du JWT : Hibernate sélectionne les colonnes de l’entité, alors que Flyway ne les créait pas.

Un appel exploratoire au Feed public a également reçu 401 (`X-Correlation-Id: 49be6695-8677-46c9-ba51-dd56193500c5`) malgré la règle publique présente dans le code source Gateway. Il n’est **pas** utilisé comme fallback. Cela doit être revalidé après déploiement comme possible écart de version Gateway, mais n’est pas la correction du Feed personnel authentifié.

## 5. Root cause Feed

`FeedPostEntity` déclare :

```text
city_id
latitude
longitude
```

Les migrations présentes avant cette passe étaient `V1`, `V2`, `V3`, `V4` et `V7` : elles créaient notamment `country_code` et `language_code`, mais jamais ces trois colonnes. Toute requête repository qui hydrate `FeedPostEntity` peut donc échouer avant la construction de `FeedPage` avec une erreur de type :

```text
ERROR: column feed_posts.city_id does not exist
```

La panne est un **drift entité JPA / schéma Flyway**, et non un faux état vide mobile, une erreur de pagination ou un échec Redis.

## 6. Feed corrections

Ajout de la migration idempotente :

```text
feed-service/src/main/resources/db/migration/V8__add_feed_projection_location_columns.sql
```

Elle ajoute `city_id`, `latitude` et `longitude`, puis impose que latitude et longitude soient toutes deux nulles ou toutes deux renseignées. Les données historiques restent lisibles : les trois nouveaux champs sont facultatifs.

Ajout du test `FeedProjectionLocationMigrationTest`, qui applique V8 sur une table Feed existante, vérifie les trois colonnes et la contrainte de coordonnées.

## 7. Feed contract matrix

| Champ | Backend actuel | Mobile actuel | Compatible |
|---|---|---|---|
| `page` | `FeedPage.page` | converti dans `meta.current_page` | Oui |
| `size` | `FeedPage.size` | converti dans `meta.per_page` | Oui |
| `hasNext` | `FeedPage.hasNext` | converti dans `links.next` et pagination infinie | Oui |
| `items` | liste de `FeedItem` organiques et éventuellement sponsorisés | filtre les sponsorisés avant le mapping des posts | Oui |
| `itemType` | `ORGANIC` / `SPONSORED` | protège le parser d’un item sponsorisé sans `postId` | Oui |
| `postId` | UUID | id de `FeedPost` | Oui |
| auteur | `authorId` | utilisateur de secours déterministe | Partiel : pas de displayName/avatar enrichi dans ce DTO |
| média | `mediaIds` | métadonnées demandées séparément ; fallback sur l’URL réelle du média en cas d’échec | Oui |
| interactions | `likes`, `comments`, `shares` | compteurs affichés | Oui |
| viewer state | absent | `is_liked` / `is_saved` initialisés à `false` sur la page Feed | Partiel : ne cause pas le 500 mais reste un enrichissement à planifier |
| timestamps | `publishedAt`, `generatedAt` | `publishedAt` devient `created_at` | Oui |

## 8. Feed runtime validation après déploiement

1. Déployer `feed-service` avec Flyway V8 et vérifier :

```sql
SELECT version, success FROM flyway_schema_history WHERE version = '8';
SELECT column_name
FROM information_schema.columns
WHERE table_name = 'feed_posts'
  AND column_name IN ('city_id', 'latitude', 'longitude');
```

2. Avec un token de test non journalisé :

```powershell
$env:YEYAMO_TEST_TOKEN = '<token de test>'
curl.exe -i `
  -H "Authorization: Bearer $env:YEYAMO_TEST_TOKEN" `
  -H "X-Correlation-ID: feed-test3-$([guid]::NewGuid())" `
  'https://api.yeyamo.com/api/v1/feed?page=0&size=20'
```

Résultat attendu : `200`, `Content-Type: application/json`, JSON contenant `page`, `size`, `hasNext`, `items` et soit des items soit une liste vide réelle.

## 9. Adventure call chain

```text
src/app/(explore)/adventure.tsx
  → usePreviewAdventurePlan()
  → adventureApi.previewAdventurePlan(request)
  → POST /api/v1/explore/adventure-plans/preview
  → Gateway /explore/**
  → recommendation-service AdventurePlanController
  → AdventurePlanService.normalize()
  → CountryConfigClient + candidate projections
  → AdventurePlanPreviewResponse
```

Le mobile envoie les champs correspondant au DTO backend : `countryCode`, dates ISO, heures `HH:mm`, `partyType`, intérêts, tier/min/max/currency budget. Il n’invente pas de planning si la requête échoue.

## 10. Adventure actual HTTP failure

Le diagnostic est fourni par l’erreur observée :

```text
Failed to fetch country config
I/O error on GET request for http://country-config-service/api/countries/cm:
Connection refused
```

Cette URL sans port correspond exactement au fallback de `CountryConfigClient`. Le service déployé est interne sur `country-config-service:8117`; l’erreur se produit pendant `AdventurePlanService.normalize()` avant la génération des candidats. Aucun JWT réel n’étant disponible dans l’environnement, la requête POST de production n’a pas été rejouée ici.

## 11. Adventure root cause

`recommendation-service` importe `CountryConfigClient` mais n’avait pas la propriété :

```text
yeyamo.services.country-config.url
```

Le fallback partagé était `http://country-config-service`, qui résout le port 80. Le Compose définit pourtant :

```text
country-config-service
SERVER_PORT=8117
```

Le client pays tente donc une connexion que le service n’écoute pas. C’est une cause racine de configuration inter-service, distincte du Feed.

## 12. Adventure corrections

Ajout dans la configuration servie à `recommendation-service` :

```properties
yeyamo.services.country-config.url=${COUNTRY_CONFIG_SERVICE_URL:http://country-config-service:8117}
```

La variable d’environnement reste prioritaire pour tout déploiement qui utilise une URL différente. Aucune recommandation locale ou faux planning n’a été ajouté ; une absence de candidat continue à retourner la réponse métier contrôlée `NO_COMPATIBLE_CANDIDATES`.

## 13. Country-config / service-discovery findings

`COUNTRY_CONFIG_RELATED_TO_ADVENTURE = YES`.

La communication n’utilisait ni Eureka ni un client load-balanced : `CountryConfigClient` utilise un `RestClient` avec une URL explicite. L’URL doit donc comprendre le port réel Docker. La correction est placée dans la configuration de `recommendation-service`, sans modifier le service pays ni les écrans Créer.

## 14. Files modified

- `feed-service/src/main/resources/db/migration/V8__add_feed_projection_location_columns.sql`
- `feed-service/src/test/java/com/yeyamo_mobile/api/feed_service/infrastructure/persistence/FeedProjectionLocationMigrationTest.java`
- `cloud-conf-yeyamo/recommendation-service.properties`
- `EXPLORER_TEST3_REPAIR.md`

Les modifications mobiles préexistantes ont été conservées. Aucun fichier mobile et aucun fichier de l’onglet Créer n’a été modifié dans cette passe.

## 15. Tests

| Commande | Résultat |
|---|---|
| `mvn -pl feed-service -Dtest=FeedProjectionLocationMigrationTest,FeedQueryServiceTests,RedisFeedCacheTests,FeedEventConsumerTests,FeedEventConsumerRobustnessTests test` | 14 tests réussis |
| `mvn -pl recommendation-service -Dtest=AdventurePlanServiceTest,RecommendationQueryServiceTest,RecommendationEventConsumerTest test` | 15 tests réussis |
| `npx tsc --noEmit --pretty false` dans mobile | terminé sans sortie d’erreur ; la première exécution interactive avait atteint sa limite de temps, l’exécution complète en arrière-plan s’est terminée sans erreur enregistrée |
| `npm run lint` dans mobile | 0 erreur, 5 avertissements préexistants hors périmètre |
| `git diff --check` API, config et mobile | réussi |

## 16. Runtime tests

- Feed anonyme réel : `401` attendu, correlation id capturé ; ce n’est pas le test du Feed personnel.
- Feed authentifié réel : non exécuté, faute de token de test.
- Adventure preview authentifié réel : non exécuté, faute de token de test.
- Docker/logs locaux : non disponibles ; le daemon Docker local n’est pas démarré.

Pour retrouver les logs après déploiement sans figer de nom de conteneur :

```powershell
$containers = docker ps --format '{{.Names}}' |
  Select-String 'api-gateway|feed-service|recommendation-service|country-config-service' |
  ForEach-Object Line
$containers | ForEach-Object { docker logs --since 15m $_ }
```

Rechercher le `X-Correlation-ID` envoyé par le curl dans Gateway, Feed et Recommendation ; ne jamais enregistrer le JWT dans le rapport ou les logs applicatifs.

## 17. Remaining blockers

1. Déployer les trois fichiers de correction, y compris la configuration fournie par config-server, puis laisser Flyway appliquer V8.
2. Rejouer les deux appels authentifiés avec un compte test et conserver uniquement le statut, le corps non sensible et le correlation id.
3. Revalider la règle publique `/api/v1/public/feed`, qui renvoie actuellement 401 sur le domaine déployé malgré le code source Gateway. Ce point ne doit pas devenir un fallback du Feed personnel.
4. Planifier l’enrichissement `viewerState`/auteur du DTO Feed si l’UX doit refléter les likes/favoris préexistants dès la première page.

## 18. Final matrix

| Feature | Before | Root cause | Fix | Runtime status |
|---|---|---|---|---|
| Feed initial load | BROKEN | dérive `FeedPostEntity` / migrations | migration Flyway V8 | NOT_VERIFIED après déploiement |
| Feed retry | BROKEN | retry réel mais même 500 backend | même migration ; `refetch()` déjà correct | NOT_VERIFIED |
| Feed pagination | ALIGNED | aucune | aucun changement | NOT_VERIFIED |
| Feed empty state | ALIGNED | aucune | aucun changement | NOT_VERIFIED |
| Feed author/media | PARTIAL | DTO volontairement non enrichi, sans lien avec le 500 | aucun faux fallback | NOT_VERIFIED |
| Explorer discovery | ALIGNED | aucune | aucun changement | NOT_VERIFIED non-régression |
| Recommendations | ALIGNED | aucune | aucun changement | NOT_VERIFIED non-régression |
| Adventure preview | BROKEN | URL country-config interne sans port | URL `country-config-service:8117` | NOT_VERIFIED après déploiement |
| Adventure save | BLOCKED_BY_PREVIEW | preview échouait avant la création | débloqué par la même correction | NOT_VERIFIED |
| Adventure list/detail | ALIGNED | aucune | aucun changement | NOT_VERIFIED non-régression |

```text
FEED_ROOT_CAUSE = FeedPostEntity / feed_posts Flyway schema drift: city_id, latitude and longitude were mapped but never migrated.

FEED_BACKEND = FIXED

FEED_MOBILE = ALIGNED

FEED_RUNTIME = NOT_VERIFIED

ADVENTURE_ROOT_CAUSE = recommendation-service used CountryConfigClient fallback http://country-config-service (port 80) while Docker Compose exposes country-config-service on port 8117.

ADVENTURE_BACKEND = FIXED

ADVENTURE_MOBILE = ALIGNED

ADVENTURE_RUNTIME = NOT_VERIFIED

COUNTRY_CONFIG_RELATED_TO_ADVENTURE = YES

EXPLORER_READY_FOR_RETEST = YES

CREATE_TAB_TOUCHED = NO
```
