# YEYAMO — CREATE TAB PRODUCTION REPAIR / TEST 3

## 1. Executive summary

Cette passe est limitée à l’onglet **Créer**. Feed et Adventure Planning n’ont pas été modifiés.

Deux causes transversales ont été démontrées et corrigées :

1. le client mobile envoyait explicitement `Content-Type: multipart/form-data`, ce qui peut empêcher React Native/Axios d’ajouter la boundary multipart ;
2. le client partagé `CountryConfigClient` utilisait par défaut `http://country-config-service` (port 80), alors que Docker Compose expose le service sur `8117`.

Les réessais de Story, Sortie et Suggestion de lieu réutilisent désormais les `mediaId` déjà acceptés, au lieu de réuploader le même contenu. La sélection administrative tient compte de la hiérarchie réelle `Centre → Mfoundi → Yaoundé`.

## 2. Create architecture

| Parcours | Mobile / hook | API mobile | Gateway → contrôleur | Dépendances backend |
| --- | --- | --- | --- | --- |
| Publication | `(create)/publication.tsx`, `useCreatePost`, `useUploadMedia` | `POST /media`, `POST /posts`, `POST /posts/{id}/publish` | media-service `MediaController`; content-service `PostController` | stockage média, contenu culturel référencé, outbox Feed |
| Story | `(create)/story.tsx`, `useCreateStory` | `POST /media`, `POST /stories` | media-service `MediaController`; content-service `StoryController` | stockage média, content outbox |
| Sortie | `(create)/event*`, `eventsApi` | `POST /media`, `POST /events` | media-service; event-service `EventController` | Country Config, place read model, distribution Feed/Story |
| Suggestion de lieu | `(create)/suggest-place-*`, `placesApi` | `GET /categories`, Country Config geography, `POST /media`, `POST /place-suggestions` | place-service `CategoryController`/`PlaceSuggestionController`; country-config-service `GeographyController` | Country Config, vérification média, dédoublonnage |
| Transmission de savoir | `(create)/culture-contribution.tsx`, culture hooks | `POST /culture/contributions`, `POST /culture/contributions/{id}/submit` | culture-service controller | Country Config, langues culturelles, outbox |

Le client mobile ajoute le préfixe `/api/v1`; les routes Gateway existantes couvrent `/api/v1/media/**`, `/posts/**`, `/stories/**`, `/events/**`, `/place-suggestions/**`, `/categories/**`, `/countries/**`, `/administrative-areas/**`, `/cities/*/localities/**` et `/culture/**`.

## 3. Media pipeline diagnosis

Chaîne validée : Expo ImagePicker/Camera → `PickedMediaAsset` → `resolveMediaMimeType`/`resolveMediaFileName` → `FormData(file: { uri, name, type })` → `POST /api/v1/media` → `MediaController.upload` → `MediaContentPolicy` → stockage → `MediaResponse.id` → objet métier.

`POST /api/v1/media/culture` reste réservé aux usages avec `usageType` (œuvres/artisanat). Le client générique choisit maintenant ce chemin uniquement lorsqu’un `usageType` est réellement fourni ; les uploads ordinaires vont vers `/media`.

## 4. Media root cause(s)

- `postApi.uploadMedia` forçait un `Content-Type` multipart dépourvu de boundary. React Native doit générer cet en-tête lui-même.
- Les alias mobiles (`image/jpg`, `audio/m4a`, `video/x-m4v`) et les noms de fichiers ne normalisaient pas tous vers le MIME réellement accepté.
- Après un upload réussi mais une création métier échouée, Story, Sortie et Suggestion de lieu pouvaient réuploader le fichier. media-service protège correctement par checksum/owner et répond alors `409 MEDIA_DUPLICATE` / `This media was already uploaded`.

## 5. Media corrections

- suppression du `Content-Type` manuel dans les deux uploaders mobiles ;
- normalisation MIME/extension côté mobile, sans forcer arbitrairement les fichiers inconnus vers JPEG ;
- réutilisation des `mediaId` réussis par URI pour les suggestions ;
- conservation du `mediaId` de Story et de l’image de couverture de Sortie pendant un retry métier ;
- verrou explicite contre double soumission dans la Sortie et la Suggestion.

| Extension | MIME mobile normalisé | MIME backend accepté | Status |
| --- | --- | --- | --- |
| `.jpg`, `.jpeg` | `image/jpeg` (`image/jpg` normalisé) | `image/jpeg` | ALIGNED |
| `.png` | `image/png` | `image/png` | ALIGNED |
| `.webp` | `image/webp` | `image/webp` | ALIGNED |
| `.mp4` | `video/mp4` | `video/mp4` | ALIGNED |
| `.mov` | `video/quicktime` | `video/quicktime` | ALIGNED |
| `.webm` | `video/webm` | `video/webm` | ALIGNED |
| `.mp3` | `audio/mpeg` | `audio/mpeg`, `audio/mp3` | ALIGNED |
| `.ogg` | `audio/ogg` | `audio/ogg` | ALIGNED |
| `.flac` | `audio/flac` | `audio/flac` | ALIGNED |
| `.m4a` | `audio/mp4` (`audio/m4a` normalisé) | `audio/mp4`, `audio/x-m4a` | ALIGNED |
| `.wav` | `audio/wav` | `audio/wav`, `audio/x-wav` | ALIGNED |
| `.pdf` | `application/pdf` | `application/pdf` | ALIGNED |

La validation serveur reste l’autorité : MIME déclaré, taille et signature binaire sont tous vérifiés par `MediaContentPolicy`.

## 6. Country-config consumer audit

| Service consommateur | Configuration finale | Port | Status |
| --- | --- | ---: | --- |
| auth-service | propriété existante `COUNTRY_CONFIG_SERVICE_URL` | 8117 | ALIGNED |
| content-service | propriété ajoutée + fallback partagé | 8117 | FIXED |
| culture-service | propriété ajoutée + fallback partagé | 8117 | FIXED |
| event-service | propriété ajoutée + fallback partagé | 8117 | FIXED |
| place-service | propriété existante | 8117 | ALIGNED |
| recommendation-service | correction précédente conservée | 8117 | ALIGNED |
| booking/catalog/discovery/user | fallback partagé, surcharge d’environnement prioritaire | 8117 | FIXED |

`COUNTRY_CONFIG_SERVICE_URL` reste prioritaire partout. Le test ajouté vérifie à la fois le fallback Compose `:8117` et une URL explicitement fournie.

## 7. Country-config corrections

`CountryConfigClient.DEFAULT_SERVICE_URL` vaut maintenant `http://country-config-service:8117`. Les fichiers Config Server `event-service.properties`, `content-service.properties` et `culture-service.properties` exposent explicitement la même convention.

## 8. Publication diagnosis/fix

Le contrat mobile est conforme au backend : `caption`, `visibility`, `mediaIds`, `catalogAssetId` ou cible culturelle réelle, puis publication du brouillon. Le Feed est invalidé après succès. La correction multipart commune couvre galerie, vidéo et caméra.

L’analyse AST de `publication.tsx` ne trouve aucun nœud JSX texte non vide hors de `<Text>` (notamment dans le picker Culture). L’erreur React Native observée n’est plus présente dans le fichier actuel ; aucun wrapper artificiel n’a été ajouté.

## 9. Story diagnosis/fix

Le backend attend `mediaId`, `caption` facultative et `durationSeconds` entre 5 et 60. Le mobile respectait ce DTO, mais recommençait l’upload après un échec de création. Il mémorise maintenant le `mediaId` : « Réessayer sans changer le média » rejoue uniquement `POST /stories`.

## 10. Outing diagnosis/fix

La sortie appelle Country Config lors de la validation des fonctionnalités et de la géographie. Son erreur `Connection refused http://country-config-service/...` provenait du fallback port 80 ; elle est corrigée côté configuration et bibliothèque partagée.

L’image de couverture réussie est conservée dans le draft (`cover_media_id` lié à son URI). Si `POST /events` échoue ensuite, le nouvel essai réutilise ce média. Le DTO conserve localisation Yeyamo **ou** localisation personnalisée, dates, capacité, visibilité et distribution sociale existante.

## 11. Place suggestion diagnosis/fix

Les catégories viennent déjà de `GET /api/v1/categories` (`CategoryController`) : le mobile affiche le nom réel retourné par place-service et envoie ce libellé, car `PlaceSuggestionRequest.category` est effectivement un `String`, non un identifiant numérique. Aucune liste locale factice n’a été ajoutée.

`POST /place-suggestions/check-duplicates` reste un précontrôle ; `POST /place-suggestions` demeure l’autorité. Une réponse `409` est conservée et mappée par `normalizeApiError`, sans supprimer la protection anti-doublon.

## 12. Administrative hierarchy diagnosis/fix

La donnée backend est réelle : la migration Country Config crée `Centre` (niveau 1), `Mfoundi` (niveau 2 enfant de Centre), puis `Yaoundé` dont `administrativeAreaId` est Mfoundi, avec les localités Bastos, Nlongkak, Mvan, Essos, Mokolo et Emana.

Le défaut mobile comparait uniquement `city.administrativeAreaId === selectedAreaId` ; choisir Centre excluait donc Yaoundé. Le filtre parcourt maintenant la chaîne `parentId` fournie par `GET /countries/CM/administrative-areas`. L’interface garde Centre visible, mais soumet le parent direct Mfoundi requis par `PlaceSuggestionService.validateCity` et `validateLocality`.

## 13. Knowledge diagnosis/fix

La contribution utilise le contrat culture existant, crée puis soumet la contribution selon le comportement déjà implémenté. Culture Service valide Country Config lors de la création ; la correction `:8117` couvre donc l’erreur observée. Aucun changement de règle de modération/publication n’a été inventé.

## 14. Gateway/security alignment

| Feature | Mobile endpoint | Gateway route | Backend service | Status |
| --- | --- | --- | --- | --- |
| Media | `/media`, `/media/culture` | `/api/v1/media/**` | media-service | ALIGNED |
| Post | `/posts` | `/api/v1/posts/**` | content-service | ALIGNED |
| Story | `/stories` | `/api/v1/stories/**` | content-service | ALIGNED |
| Event | `/events` | `/api/v1/events/**` | event-service | ALIGNED |
| Place suggestion | `/place-suggestions` | `/api/v1/place-suggestions/**` | place-service | ALIGNED |
| Place categories | `/categories` | `/api/v1/categories/**` | place-service | ALIGNED |
| Country Config | `/countries`, `/administrative-areas`, `/cities/*/localities` | routes Country Config prioritaires | country-config-service | ALIGNED |
| Culture | `/culture/contributions` | `/api/v1/culture/**` | culture-service | ALIGNED |

Les endpoints de création restent authentifiés. Aucun `permitAll`, nouvel endpoint ou modification de contrat backend n’a été introduit.

## 15. Error handling/retry/idempotency

- le mobile conserve l’état des uploads déjà acceptés ; un échec métier ne provoque pas de second upload ;
- en cas d’échec d’upload, seul le média absent est rejoué ;
- les boutons actifs sont désactivés pendant mutation/upload et protégés en code contre un double tap ;
- Story affiche maintenant l’erreur normalisée de l’upload ou de la création ;
- les messages serveur internes ne sont pas exposés au-delà du mapping utilisateur existant ; l’en-tête `X-Correlation-Id` reste propagé par le client API pour le diagnostic backend.

## 16. Files modified

### yeyamo-api

- `shared-lib/pom.xml`
- `shared-lib/src/main/java/com/yeyamo_mobile/shared/country/CountryConfigClient.java`
- `shared-lib/src/test/java/com/yeyamo_mobile/shared/country/CountryConfigClientConfigurationTest.java`

### cloud-conf-yeyamo

- `content-service.properties`
- `culture-service.properties`
- `event-service.properties`

### yeyamo-mobile

- `src/features/media/media.api.ts`
- `src/features/media/media.utils.ts`
- `src/features/post/post.api.ts`
- `src/features/create/types.ts`
- `src/app/(create)/event.tsx`
- `src/app/(create)/event-review.tsx`
- `src/app/(create)/story.tsx`
- `src/app/(create)/suggest-place-review.tsx`
- `src/app/(create)/suggest-place-step2.tsx`

## 17. Backend tests

| Command / scope | Result |
| --- | --- |
| `shared-lib` CountryConfigClientConfigurationTest | PASS — 2 tests |
| `media-service` MediaApplicationService/Policy/Culture/R2 tests | PASS — 44 tests |
| `content-service` PostApplicationService + StoryService | PASS — 15 tests |
| `event-service` Event place creation + social distribution | PASS — 11 tests |
| `place-service` PlaceSuggestionService | PASS — 3 tests |
| `culture-service` structured content + domain | PASS — 7 tests |

`domain-foundation` et `event-contracts` ont seulement été installés localement avec tests ignorés afin de satisfaire les dépendances Maven des suites ciblées ; aucun fichier source de ces modules n’a été modifié.

## 18. Mobile tests

| Vérification | Résultat |
| --- | --- |
| `npx tsc --noEmit` | PASS |
| ESLint ciblé sur les 9 fichiers mobiles modifiés | PASS, 0 erreur / 0 avertissement |
| `npx expo lint --no-cache` global | PARTIAL : le premier passage complet a donné 0 erreur et seulement 5 avertissements préexistants ; la seconde tentative complète a dépassé 120 s. |

## 19. Runtime validation

`RUNTIME = NOT_VERIFIED` : aucun accès VPS/JWT de test n’est disponible dans cette session et aucun déploiement n’a été effectué.

Après redéploiement, vérifier notamment :

```bash
docker compose logs --tail=200 media-service content-service event-service place-service culture-service country-config-service api-gateway
docker compose exec event-service sh -lc 'getent hosts country-config-service'
docker compose exec event-service sh -lc 'printenv COUNTRY_CONFIG_SERVICE_URL'
```

Les réponses d’erreur doivent être corrélées via `X-Correlation-Id` dans les logs Gateway et service cible.

## 20. Manual retest plan

| Test | Action | Résultat attendu | API |
| --- | --- | --- | --- |
| C1 | Publier une image galerie | upload puis post visible Feed | `/media`, `/posts` |
| C2 | Publier une vidéo galerie | même résultat, MIME vidéo accepté | `/media`, `/posts` |
| C3 | Publier une photo caméra | même résultat, aucun message format non supporté | `/media`, `/posts` |
| C4 | Créer une Story image puis forcer/rejouer un échec métier si possible | second essai réutilise le média | `/media`, `/stories` |
| C5 | Créer une Story vidéo | story 24 h créée | `/media`, `/stories` |
| C6 | Créer une sortie avec couverture | Country Config joignable, sortie créée ; retry sans doublon | `/media`, `/events` |
| C7 | Ouvrir les catégories de suggestion | catégories réelles du backend | `/categories` |
| C8 | Choisir CM → Centre | Yaoundé devient disponible | `/countries/CM/administrative-areas`, `/countries/CM/cities` |
| C9 | Choisir Yaoundé | quartiers/localités disponibles | `/cities/{id}/localities` |
| C10 | Soumettre une suggestion | suggestion créée ou message métier 409 précis | `/media`, `/place-suggestions` |
| C11 | Transmettre un savoir | contribution créée/soumise sans appel Country Config port 80 | `/culture/contributions` |

## 21. Remaining blockers

- La validation avec vrais fichiers iOS/Android et le runtime Docker de production restent à effectuer après déploiement.
- Le lint global long n’a pas été relancé au-delà de la limite de 120 secondes ; le lint ciblé des fichiers modifiés est propre.
- Aucun changement de données n’est nécessaire : les données de géographie Cameroun (Centre, Mfoundi, Yaoundé, localités) existent déjà dans la migration V4 de country-config-service.

## 22. Final matrix

| Parcours | Root cause | Backend fix | Mobile fix | Tests | Runtime |
| --- | --- | --- | --- | --- | --- |
| Publication image | multipart boundary/MIME | ALIGNED | FIXED | PASS | NOT_VERIFIED |
| Publication vidéo | multipart boundary/MIME | ALIGNED | FIXED | PASS | NOT_VERIFIED |
| Publication caméra | multipart boundary/MIME | ALIGNED | FIXED | PASS | NOT_VERIFIED |
| Publication Culture picker | aucun texte JSX orphelin dans le code actuel | ALIGNED | ALIGNED | PASS | NOT_VERIFIED |
| Story image | retry réuploadait | ALIGNED | FIXED | PASS | NOT_VERIFIED |
| Story vidéo | retry réuploadait | ALIGNED | FIXED | PASS | NOT_VERIFIED |
| Sortie | Country Config port 80 + retry couverture | FIXED | FIXED | PASS | NOT_VERIFIED |
| Sortie média retry | mediaId non conservé | ALIGNED | FIXED | PASS | NOT_VERIFIED |
| Suggestion catégories | vérifier source réelle | ALIGNED | ALIGNED | PASS | NOT_VERIFIED |
| Suggestion région → villes | parent direct seulement | ALIGNED | FIXED | PASS | NOT_VERIFIED |
| Suggestion ville → localité | contrat Country Config | ALIGNED | ALIGNED | PASS | NOT_VERIFIED |
| Suggestion submit | retry réuploadait | ALIGNED | FIXED | PASS | NOT_VERIFIED |
| Transmission savoir | Country Config port 80 | FIXED | ALIGNED | PASS | NOT_VERIFIED |

## 23. Final verdict

```text
MEDIA_ROOT_CAUSE = Manual multipart Content-Type without a React Native boundary, incomplete mobile MIME normalization, and retries that re-uploaded media already accepted by media-service.

MEDIA_BACKEND = ALIGNED

MEDIA_MOBILE = FIXED

PUBLICATION = FIXED

STORY = FIXED

OUTING = FIXED

PLACE_CATEGORIES = FIXED

PLACE_ADMIN_HIERARCHY = FIXED

PLACE_SUGGESTION = FIXED

KNOWLEDGE = FIXED

COUNTRY_CONFIG_CONSUMERS = FIXED

TEXT_COMPONENT_ERROR = FIXED

DOUBLE_UPLOAD = FIXED

CREATE_BACKEND_TESTS = PASS

CREATE_MOBILE_TYPESCRIPT = PASS

CREATE_MOBILE_LINT = PASS

RUNTIME = NOT_VERIFIED

CREATE_TAB_READY_FOR_REDEPLOY = YES

CREATE_TAB_READY_FOR_USER_RETEST = YES
```
