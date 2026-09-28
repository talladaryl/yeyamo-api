# YEYAMO — Create Tab Test 4: diagnostic approfondi et réparation

## 1. Executive summary

Le pays manquant avait une cause backend démontrée : `GET /users/me` pouvait créer un profil vide avant que le consommateur Kafka de `user.created` ne traite le pays de l'inscription. Le consommateur ignorait ensuite le profil déjà présent. Le pays restait donc vide après connexion et relance.

La correction matérialise le claim JWT `country` dans le profil dès `GET /users/me` et complète uniquement les champs initiaux absents à la réception de l'événement. Le mobile attend désormais le profil canonique avant ses requêtes Create dépendantes du pays.

Le 415 média n'est pas attribué sans preuve à `/media`. Une trace DEV sûre, corrélée et un garde-fou de transport Axios ont été ajoutés. Le prochain essai sur appareil identifiera l'URL, le statut et le service exacts.

## 2. Test 4 runtime evidence

Symptômes fournis : média toujours en échec autour de `Content-Type`, pays absent dans Create après reconnexion/redémarrage, langues et listes dépendantes vides. Aucun log HTTP corrélé de l'appareil n'a été fourni ; le 415 ne peut donc pas être associé honnêtement à une URL précise.

Statut : `STILL_BROKEN` pour le constat runtime antérieur ; les corrections ci-dessous sont prêtes à être retestées sur appareil.

## 3. Create architecture

Les écrans standard utilisent `useCountryStore.selectedCountryCode` : sortie, suggestion de lieu, contribution culturelle et œuvre. Les uploads standard passent par `toMediaFormData` + `postApi.uploadMedia`; les œuvres utilisent `media.api.uploadMedia` et `/media/culture` avec un `usageType`.

## 4. Auth restoration architecture

`RootLayout` lance `authService.hydrate()`. Celui-ci relit les jetons sécurisés, appelle `GET /auth/me`, puis remplit `useAuthStore`. Une session backend déclenche ensuite `GET /users/me`, la source canonique du profil pays. Le JWT auth contient déjà le claim signé `country`.

## 5. Register vs Login vs Cold Start

| Contexte | Chaîne |
| --- | --- |
| Register | `POST /auth/register` enregistre `User.countryCode`, émet `user.created`, retourne un JWT contenant `country`; `GET /users/me` matérialise immédiatement ce pays. |
| Login | `POST /auth/login` recharge le `User` auth; son JWT contient `country`; `GET /users/me` complète seulement un profil historique incomplet. |
| Cold start | jeton restauré → `GET /auth/me` → `GET /users/me` → store pays → configuration et références Create. |

## 6. Profile country contract

`GET /api/v1/users/me` retourne `MyProfileResponse.countryCode`. Le client lit exactement `preferences.countryCode`, le normalise en majuscules et ne parse pas de champ d'écran ou de liste codée en dur.

## 7. Country persistence

Cause démontrée : `UserProfileService.getOrCreate()` créait un profil sans localisation ; plus tard `createFromIdentityWithLocation()` retournait ce profil existant sans le compléter. `createFromIdentityWithLocation()` complète maintenant seulement `countryCode`, `cityId`, `preferredLanguageCode` et `timezone` manquants, puis publie l'événement de localisation requis. Un choix ultérieur de l'utilisateur n'est jamais écrasé.

## 8. Create country resolver

Le store distingue `PROFILE_LOADING`, `COUNTRY_LOADING`, `COUNTRY_MISSING`, `COUNTRY_ERROR` et `COUNTRY_READY`. Pendant `PROFILE_LOADING`, les requêtes de références authentifiées restent désactivées. La configuration pays rend ensuite l'état `COUNTRY_READY`. La déconnexion efface la préférence locale pays : aucun pays du compte A ne sert au compte B.

## 9. React Query/cache analysis

Les clés sont `['countries','profile','backend']`, `['countries','configuration',country]`, `['countries','administrative-areas',country]`, `['countries','cities',country]` et `['countries','localities',city]`. Les langues deviennent `['culture','languages',country]` pour la contribution. À la déconnexion, le store pays est réinitialisé et les queries protégées sont supprimées par le layout.

## 10. Reference data pipeline

Chaque query DEV journalise sa clé, son URL, sa dépendance pays et les comptes brut/parsés, sans données de profil. Les données restent fournies par les services backend, sans mock ni fallback mobile.

## 11. Categories

Suggestion de lieu : `GET /api/v1/categories` via `place-service`. La migration existante `V9__seed_core_place_categories.sql` fournit six catégories actives. Elles sont indépendantes du pays dans le contrat actuel.

## 12. Administrative areas

`GET /api/v1/countries/{countryCode}/administrative-areas` via `country-config-service`. La query est activée seulement après le profil canonique. La migration CM structure régions et départements.

## 13. Cities

`GET /api/v1/countries/{countryCode}/cities` via `country-config-service`. Les villes restent filtrées côté mobile par l'ascendance de la zone sélectionnée, en conservant les IDs backend.

## 14. Localities

`GET /api/v1/cities/{cityId}/localities` via `country-config-service`. La query est désactivée jusqu'à la sélection d'une ville et jusqu'à la fin de `PROFILE_LOADING` pour une session backend.

## 15. Culture languages

`GET /api/v1/culture/languages` est filtré par le champ backend `countryCodes` de chaque langue pour l'écran de contribution. La query de contribution attend `COUNTRY_READY`; les écrans Explorer qui demandent toutes les langues conservent le comportement global.

## 16. Media runtime root cause

`ROOT_CAUSE_PROVEN = NO`. Le runtime signale un 415, mais sans URL/statut/correlation ID il est impossible d'affirmer qu'il vient encore de `/media`. L'audit a cependant démontré un risque de transport : Axios 1.18 peut appliquer son transformeur JSON si sa détection interne ne reconnaît pas une forme de `FormData` React Native.

## 17. Axios final transport

Le client commun reste l'unique `axios.create`. Pour tout FormData reconnu par la détection React Native (`instanceof`, `getParts` ou `_parts`), le transformeur final :

- retire `Content-Type` et `content-type` ;
- renvoie le FormData sans passer par le transformeur JSON Axios ;
- trace `HTTP_MEDIA_TRANSPORT_READY` avec `isFormData=true` et `explicitContentType=null`.

Le transport natif React Native ajoute alors la boundary multipart. Les requêtes non multipart passent toujours la chaîne Axios par défaut.

## 18. Publication

Chemin : picker → normalisation → `POST /media` → `POST /posts` → `POST /posts/{id}/publish`. Les étapes upload et métier sont maintenant tracées. `postApi` est le chemin canonique.

## 19. Story

Chemin : picker → `POST /media` → `POST /stories`. La Story partenaire réutilise exactement cet écran et ce contrat.

## 20. Outing

Chemin : couverture optionnelle → `POST /media` → `POST /events`. Le pays requis provient du profil canonique. La diffusion Feed/Story reste transmise dans `socialDistribution` au backend, sans création locale de post/story.

## 21. Place suggestion

Chemin : contrôle doublons → upload de chaque média par `POST /media` → `POST /place-suggestions`. Pays, zones, ville et localité conservent les IDs renvoyés par Country Config.

## 22. Knowledge contribution

La contribution en cours ne téléverse pas de binaire dans ce formulaire ; elle fait `POST /culture/contributions`, puis `POST /culture/contributions/{id}/submit`. Les langues sont maintenant dépendantes du pays prêt.

## 23. Partner publication

Chemin identique à Publication avec préfixe de trace `partner-publication` : `POST /media` puis post/publish. Aucun client HTTP partenaire distinct n'a été trouvé.

## 24. Partner Story

`(partner)/story.tsx` réutilise le composant Story standard. Même client, même FormData, même endpoint `/media`, puis `/stories`.

## 25. Partner Place

Le flow partenaire étudié appelle `POST /places` sans sélecteur ni upload média. Il n'est pas concerné par FormData ; les références catégorie/région/ville restent côté backend.

## 26. Partner Event

Chemin : couverture optionnelle → `POST /media` → `POST /events`. Il passe le même `postApi` et le même transformeur multipart que la sortie grand public.

## 27. Artwork

Chemin : upload `/media/culture` avec `usageType=ARTWORK_PRIMARY_IMAGE` ou `ARTWORK_GALLERY` → `POST /artworks` → offre optionnelle `POST /artwork-offers`. C'est le seul chemin qui utilise le client média étendu ; il passe aussi le client Axios partagé.

## 28. Gateway

La route Gateway `/api/v1/users/**` cible `user-service`. La route média existante ne transforme pas de corps. Aucun changement Gateway n'est requis par cette passe.

## 29. Security

Les traces excluent Authorization, JWT, cookie, URI locale, binaire, caption et réponse complète. Elles ne conservent que méthode, URL, statut, code/message serveur, type du corps, Content-Type explicite et correlation ID.

## 30. Database/migrations

Aucune migration nouvelle n'est nécessaire pour cette réparation. Les migrations de références déjà présentes doivent toutefois avoir été appliquées dans l'environnement : Country Config V4, Culture V5 et Place V9.

## 31. Files modified

| Fichier | Raison / cause adressée | Couverture |
| --- | --- | --- |
| `user-service/.../UserProfileService.java` | complète le profil créé avant Kafka sans écrasement | `UserProfileMultiCountryTests` |
| `user-service/.../UserProfileController.java` | matérialise le pays signé au premier `/users/me` | tests service + claim JWT |
| `user-service/.../UserProfileMultiCountryTests.java` | régression profil vide/race et non-écrasement | Maven ciblé |
| `src/app/_layout.tsx` | chaîne pays profil + états + invalidation | TypeScript/lint |
| `src/features/country/{store,types,hooks}.ts` | états explicites, queries retardées, logout propre | TypeScript/tests géographie |
| `src/features/country/country.runtime-trace.ts` | observabilité références DEV | TypeScript |
| `src/features/auth/auth.service.ts` | efface pays à la déconnexion | TypeScript/lint |
| `src/features/culture/{hooks,query-keys}.ts` | langues de contribution filtrées après pays prêt | test options langues |
| `src/app/(create)/culture-contribution.tsx` | transmet le pays canonique au hook langues | TypeScript/lint |
| `src/features/media/{media.utils,media.api,media.runtime-trace}.ts` | normalisation/FormData/traces DEV | test multipart + TypeScript |
| `src/components/media/useYeyamoMediaPicker.ts` | étape picker tracée sans URI/binaire | TypeScript/lint |
| `src/services/api/client.ts` | garde-fou transformeur multipart final, traces HTTP | test multipart + TypeScript |
| `src/features/{post,story,events,places,culture,artworks}/*.api.ts` | traces des appels métier Create réels | TypeScript/lint |

## 32. Backend tests

- `AuthServiceMultiCountryTests` : `UNIT_TESTED`.
- `JwtServiceCountryClaimTest` : `UNIT_TESTED`.
- `UserProfileMultiCountryTests` : `UNIT_TESTED`, dont profil vide pré-Kafka complété avec `CM`.

## 33. Mobile tests

- `node --experimental-strip-types src/services/api/multipart.test.mts` : `UNIT_TESTED`.
- `node --experimental-strip-types src/features/country/country.geography.test.mts` : `UNIT_TESTED`.
- `node --experimental-strip-types src/features/culture/culture.language-options.test.mts` : `UNIT_TESTED`.
- `node --experimental-strip-types src/features/places/place.categories.test.mts` : `UNIT_TESTED`.
- `npx tsc --noEmit --pretty false` : `UNIT_TESTED`.
- `npx expo lint --no-cache` : `UNIT_TESTED` avec 0 erreur et 2 avertissements préexistants hors périmètre.

## 34. Integration tests

- `mvn -q -pl country-config-service,place-service,culture-service test` : `INTEGRATION_TESTED`.
- `mvn -q -pl media-service -Dtest=R2StorageIntegrationTest test` : `INTEGRATION_TESTED`; multipart JPEG/PNG/MP4 201, JSON 415 attendu.

## 35. Runtime limitations

Pas d'émulateur/appareil iOS ou Android connecté dans cet environnement. La boundary réellement ajoutée par le transport natif et l'URL exacte du 415 restent donc à confirmer par la trace DEV sur appareil. Aucun statut `RUNTIME_VERIFIED` n'est revendiqué.

## 36. Deployment requirements

Déployer `user-service` puis reconstruire/redéployer le mobile. Aucune modification Gateway ou Config Server n'est requise. Vérifier que les migrations Country Config V4, Culture V5 et Place V9 sont déjà appliquées dans la base cible.

## 37. Production retest plan

1. Inscrire un utilisateur avec `CM`, puis lire `/users/me` et vérifier `countryCode=CM`.
2. Se déconnecter/reconnecter, puis redémarrer l'application : Create doit afficher `CM`.
3. Vérifier catégories, zones, villes, localités et langues dans les logs `YEYAMO_COUNTRY_TRACE`.
4. Publier une image unique et relever la séquence `YEYAMO_MEDIA_TRACE` : PICK, NORMALIZE, FORM_DATA, HTTP_MEDIA_TRANSPORT_READY, réponse upload, création, publication.
5. Si échec, relever exactement `url`, `status`, `serverCode`, `serverMessage` et `correlationId`, puis chercher ce dernier dans Gateway/service concerné.

## 38. Remaining blockers

- La preuve runtime du pipeline média est `BLOCKED` par l'absence de trace d'un appareil réel.
- Une instance de production qui n'aurait pas appliqué les migrations de références doit les appliquer avant le test.
- La confirmation finale des réponses business Create requiert le prochain essai mobile.

## 39. Messaging next-audit note

`NEXT_AUDIT = MESSAGING`.

Les symptômes Messaging précédemment signalés sont hors périmètre de Test 4. Aucun fichier Messaging n'a été modifié.

## 40. Final verdict

### Matrice pays

| Context | Source | Raw field | Normalized country | Status |
| --- | --- | --- | --- | --- |
| Register | user auth + JWT | `User.countryCode` / claim `country` | majuscule ISO-2 | `UNIT_TESTED` |
| Login | user auth + JWT | claim `country` | majuscule ISO-2 | `STATICALLY_FIXED` |
| Cold restore | JWT → `/users/me` | `MyProfileResponse.countryCode` | majuscule ISO-2 | `STATICALLY_FIXED` |
| Create root | profil backend | `preferences.countryCode` | `selectedCountryCode` | `STATICALLY_FIXED` |
| Suggest Place | country store | `selectedCountryCode` | ISO-2 | `STATICALLY_FIXED` |
| Knowledge | country store | `selectedCountryCode` | ISO-2 | `STATICALLY_FIXED` |
| Outing | country store | `selectedCountryCode` | ISO-2 | `STATICALLY_FIXED` |
| Partner Place | références partenaire | pays non porté par cet écran | `NOT_APPLICABLE` | `NOT_APPLICABLE` |

### Matrice références

| Data | Country dependency | HTTP | Raw count | Parsed | UI | Status |
| --- | --- | --- | --- | --- | --- |
| Categories | non dans contrat actuel | `GET /categories` | 6 seeds V9 | options actives | suggestion lieu | `INTEGRATION_TESTED` |
| Areas | pays prêt | `GET /countries/CM/administrative-areas` | 29 seeds V4 | DTO direct | suggestion lieu | `INTEGRATION_TESTED` |
| Cities | pays prêt | `GET /countries/CM/cities` | 5 seeds V4 | DTO direct | suggestion lieu | `INTEGRATION_TESTED` |
| Localities | ville + pays prêt | `GET /cities/{id}/localities` | 13 seeds V4 | DTO direct | suggestion lieu | `INTEGRATION_TESTED` |
| Languages | pays prêt en contribution | `GET /culture/languages` | 2 seeds V5 CM | filtre `countryCodes` | contribution | `STATICALLY_FIXED` |

### Matrice média

| Flow | HTTP client | FormData | Final Content-Type | Upload HTTP | Business HTTP |
| --- | --- | --- | --- | --- | --- |
| Publication | `apiClient` / `postApi` | `toMediaFormData` | explicite absent avant adaptateur | `/media` | `/posts` puis publish |
| Story | `apiClient` / `postApi` | `toMediaFormData` | explicite absent avant adaptateur | `/media` | `/stories` |
| Outing | `apiClient` / `postApi` | couverture optionnelle | explicite absent avant adaptateur | `/media` | `/events` |
| Suggestion | `apiClient` / `postApi` | chaque média | explicite absent avant adaptateur | `/media` | `/place-suggestions` |
| Partner Publication | `apiClient` / `postApi` | `toMediaFormData` | explicite absent avant adaptateur | `/media` | `/posts` puis publish |
| Partner Story | composant Story partagé | `toMediaFormData` | explicite absent avant adaptateur | `/media` | `/stories` |
| Partner Event | `apiClient` / `postApi` | couverture optionnelle | explicite absent avant adaptateur | `/media` | `/events` |
| Artwork | `apiClient` / `media.api` | FormData étendu | explicite absent avant adaptateur | `/media/culture` | `/artworks`, offre optionnelle |

```text
PROFILE_COUNTRY_ROOT_CAUSE = ROOT_CAUSE_PROVEN — GET /users/me pouvait créer un profil vide avant user.created ; l'événement ne le complétait pas.

COUNTRY_PERSISTENCE = UNIT_TESTED — profil vide complété, choix existant préservé.

COUNTRY_AFTER_REGISTER = UNIT_TESTED

COUNTRY_AFTER_LOGIN = STATICALLY_FIXED

COUNTRY_AFTER_COLD_START = STATICALLY_FIXED

CREATE_COUNTRY_RESOLUTION = STATICALLY_FIXED

REFERENCE_DATA_QUERY_CHAIN = STATICALLY_FIXED

PLACE_CATEGORIES = INTEGRATION_TESTED

PLACE_ADMINISTRATIVE_AREAS = INTEGRATION_TESTED

PLACE_CITIES = INTEGRATION_TESTED

PLACE_LOCALITIES = INTEGRATION_TESTED

CULTURE_LANGUAGES = STATICALLY_FIXED

MEDIA_RUNTIME_ROOT_CAUSE = BLOCKED — un 415 utilisateur n'a pas encore de trace URL/statut/correlation ID.

MEDIA_FINAL_CONTENT_TYPE = STATICALLY_FIXED — null explicite avant adaptateur, FormData préservé.

MEDIA_UPLOAD = INTEGRATION_TESTED

PUBLICATION = STATICALLY_FIXED

STORY = STATICALLY_FIXED

OUTING = STATICALLY_FIXED

PLACE_SUGGESTION = STATICALLY_FIXED

KNOWLEDGE_CONTRIBUTION = STATICALLY_FIXED

PARTNER_PUBLICATION = STATICALLY_FIXED

PARTNER_STORY = STATICALLY_FIXED

PARTNER_PLACE = NOT_APPLICABLE

PARTNER_EVENT = STATICALLY_FIXED

ARTWORK = STATICALLY_FIXED

CREATE_TAB_GLOBAL_STATUS = STILL_BROKEN — la validation runtime sur appareil est requise avant une conclusion production.

MOBILE_REBUILD = YES

API_REDEPLOY = YES

CONFIG_REDEPLOY = NO

DATABASE_MIGRATION_REQUIRED = NO

SERVICES_TO_REDEPLOY = user-service; yeyamo-mobile

READY_FOR_PRODUCTION_RETEST = NO

MESSAGING = OUT_OF_SCOPE_NEXT_AUDIT
```
