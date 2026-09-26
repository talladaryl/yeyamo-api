# YEYAMO — Create Tab end-to-end repair

Date : 2026-09-26.

## 1. Executive summary

Le défaut Axios global `Content-Type: application/json` était transmis à chaque `FormData` et causait le 415 Media. Le client commun est corrigé. Les catégories et langues manquaient de données de référence, donc deux migrations idempotentes ont été ajoutées. La géographie accepte maintenant la relation indirecte Centre → Mfoundi → Yaoundé.

## 2. Runtime failures reproduced

Les erreurs fournies (415 publication, Story, sortie et suggestion) ont été reproduites au niveau du protocole : POST `/api/v1/media` JSON donne 415 avant le contrôleur. Aucun appareil authentifié ni runtime de production n’était accessible, donc `RUNTIME_VERIFIED = NO`.

## 3. Complete creation inventory

| Actor | Creation | Screen | State |
| --- | --- | --- | --- |
| User | Publication, Story, sortie, suggestion lieu, savoir | `(create)/*` | réel |
| Partner | Publication, Story, place, événement | `(partner)/*` | réel |
| Partner | Artwork et offre d’œuvre | `(create)/artwork/*` | réel |
| Partner | Offre générique | `(partner)/offer` | `ORPHAN_UI` |
| Partner | Promotion, campagne, ticket | `(partner-dashboard)/*-create` | réel, hors Create |

Il n’existe pas d’écran séparé de création partenaire pour établissement, expérience/activité, horaires/disponibilités ou configuration de réservation.

## 4. Complete backend endpoint inventory

| Flow | Endpoint | Service |
| --- | --- | --- |
| Media normal | `POST /api/v1/media` | media-service |
| Media artwork | `POST /api/v1/media/culture` | media-service |
| Publication | `POST /posts`, `POST /posts/{id}/publish` | content-service |
| Story | `POST /stories` | content-service |
| Sortie | `POST /events` | event-service |
| Suggestion | `POST /place-suggestions` | place-service |
| Partner place | `GET /partners/me`, `POST /places` | partner/place |
| Savoir | `POST /culture/contributions`, `POST /{id}/submit` | culture |

## 5. HTTP client architecture

`apiGet`, `apiPost`, `apiPatch`, `apiPut` et `apiDelete` passent par `src/services/api/client.ts`. Les uploads standard et Culture passent par cette même instance, sans client `fetch` parallèle.

## 6. Content-Type root cause

Avant correction, Axios possédait le défaut `Content-Type: application/json`. Lors de `apiPost('/media', formData)`, Spring rejetait la requête lors du matching `consumes = multipart/form-data`, avant `MediaController`. Gateway, `FormData.append` et Security ne sont pas la cause.

## 7. Media pipeline

```text
picker/camera → PickedMediaAsset → toMediaFormData(file) → POST /media → MediaResponse.id → POST métier
```

`/media/culture` est réservé aux assets avec `usageType`, notamment les œuvres.

## 8. Media corrections

- suppression du défaut JSON Axios ;
- détection du FormData React Native/Web et retrait de tout Content-Type JSON ;
- aucun Content-Type multipart manuel : React Native fournit la boundary ;
- réponse 415 structurée `UNSUPPORTED_MEDIA_TYPE` ;
- log serveur sûr du seul Content-Type rejeté ;
- intégration JPEG, PNG et MP4 à 201, JSON à 415.

## 9. Publication end-to-end

| Stage | Status |
| --- | --- |
| picker/camera/MIME | `STATICALLY_FIXED` |
| FormData | `UNIT_TESTED` |
| upload + mediaId | `INTEGRATION_TESTED` |
| create + publish post | `UNIT_TESTED` |
| retry after business failure | mediaId by URI |
| reset/navigation | implémenté |

Le texte seul ne passe pas par Media.

## 10. Story end-to-end

Story fait `FormData → /media → /stories`. `uploadedMediaId` est conservé après upload si la création Story échoue, puis supprimé après succès.

## 11. Outing end-to-end

La sortie retient `cover_media_id` puis transmet `coverMediaId` à `POST /events`. Les tests Event couvrent le contrat et la demande de distribution Feed/Story. Les tickets/prix restent un flux distinct.

## 12. Place categories

`GET /categories` retourne une liste directe depuis `CategoryController`; le parser n’attend pas d’enveloppe. La cause est `DATA_GAP`: aucune ligne active n’était seedée. V9 ajoute six catégories actives avec `ON CONFLICT (slug) DO NOTHING`.

## 13. Administrative hierarchy

V4 Country Config contient 10 régions, 29 aires administratives niveau 1/2, cinq villes et 13 localités. Yaoundé dépend de Mfoundi, lui-même de Centre. `isAdministrativeDescendant` remonte les parents au lieu de n’accepter qu’un lien direct.

## 14. Place suggestion end-to-end

| Stage | Status |
| --- | --- |
| catégories | `STATICALLY_FIXED` |
| Centre → Mfoundi → Yaoundé | `INTEGRATION_TESTED` |
| localités Yaoundé | `INTEGRATION_TESTED` |
| preflight doublon | `UNIT_TESTED` |
| upload média | `INTEGRATION_TESTED` |
| POST suggestion | `UNIT_TESTED` |
| retry upload | mediaId by URI |

La suggestion sans média est supportée; avec média elle envoie des IDs Media réels.

## 15. Culture language source

`GET /api/v1/culture/languages` passe par `PublicCultureController`/`LanguageLearningService`. La table `languages` était vide. V5 ajoute `fr/Français` et `en/English` pour `CM` sans fallback mobile.

## 16. Knowledge contribution end-to-end

`toCultureLanguageOptions` transforme la réponse réelle en options. Le flux crée à `POST /culture/contributions`, puis soumet à `POST /culture/contributions/{id}/submit`. V5 doit encore être appliquée en PostgreSQL réel.

## 17. Partner creation inventory

Publication partenaire, Story, place, événement, œuvre/offre d’œuvre, promotion, campagne et ticket ont été inspectés. Promotions, campagnes et tickets sont réels mais hors de l’onglet Create.

## 18. Partner place creation

Le flux vérifie le partenaire APPROVED, puis appelle `POST /places` avec les références et coordonnées du contrat. Security impose aussi PARTNER. Il n’y a aucun picker média dans cette interface : Media Partner Place est `NOT_APPLICABLE`. Les réseaux sociaux et e-mails secondaires restent non persistés car absents de `PlaceRequest`.

## 19. Partner event creation

L’image choisie était uniquement locale. Elle suit maintenant `picker → POST /media → cover_media_id → coverMediaId dans POST /events`. L’ID est conservé en cas d’échec du POST Event; `isPublishing` bloque le double tap. La billetterie reste séparée.

## 20. Other partner creation flows

| Flow | Contract | Status |
| --- | --- | --- |
| publication partenaire | Media → posts | `UNIT_TESTED` |
| Story partenaire | Media → stories | `UNIT_TESTED` |
| artwork | `/media/culture` → artworks → artwork-offers | `STATICALLY_FIXED` |
| offre œuvre | `/artwork-offers` + Idempotency-Key | `STATICALLY_FIXED` |
| promotion/campagne/ticket | contrats JSON existants | `STATICALLY_FIXED` |
| offre générique | aucun POST | `STILL_BROKEN` / `ORPHAN_UI` |

L’offre générique ne doit pas être raccordée arbitrairement à une promotion ou une offre d’œuvre.

## 21. Gateway alignment

Routes vérifiées : Media 19, Content 10, Event 2, Place 3, Culture 30, Country Config 39 et `/cities/*/localities` route 40. Aucune ne modifie un multipart; Gateway n’est pas la source du 415.

## 22. Security alignment

POST Media, posts, stories, events, suggestions et contribution sont authentifiés. POST places demande PARTNER. Lecture catégories/géographie est publique. Aucune règle `permitAll` n’a été ajoutée.

## 23. Reference-data audit

| Data | Endpoint | HTTP | Backend count after migration | Mobile parsed | UI count | Root cause |
| --- | --- | --- | ---: | ---: | ---: | --- |
| Categories | `/categories` | GET | 6* | 6 testés | non runtime | `DATA_GAP` |
| Administrative areas | `/countries/CM/administrative-areas` | GET | 29* | hiérarchie testée | non runtime | V4 + lien indirect |
| Cities | `/countries/CM/cities` | GET | 5* | Yaoundé testée | non runtime | parent indirect |
| Localities | `/cities/{id}/localities` | GET | 13* | six Yaoundé testées | non runtime | V4 + ID réel |
| Languages | `/culture/languages` | GET | 2* | 2 testées | non runtime | `DATA_GAP` |

`*` est le nombre attendu après Flyway PostgreSQL, pas un comptage de production.

## 24. Retry/idempotency

Publication, Story, suggestion, sortie, événement partenaire et publication partenaire conservent les mediaIds obtenus jusqu’au succès métier. Event et Place ne gèrent pas encore `Idempotency-Key` serveur pour un timeout ambigu : backlog backend réel, non masqué.

## 25. Error handling

Les écrans distinguent picker, upload, create et publish. Media renvoie désormais `UNSUPPORTED_MEDIA_TYPE` en 415. Chaque requête mobile porte `X-Correlation-ID`.

## 26. Files modified

Mobile : `client.ts`, `multipart.ts`, `multipart.test.mts`, `place.categories.ts`, `place.categories.test.mts`, `country.geography.ts`, `country.geography.test.mts`, `culture.language-options.ts`, `culture.language-options.test.mts`, les écrans Suggest Place, contribution Culture, publication partenaire et événement partenaire, ainsi que `partner/types.ts`.

API : GlobalExceptionHandler et test R2 Media, V9 catégories Place, V5 langues Culture et test Geography Country Config. `ADMIN_AUTH_PRODUCTION_AUDIT.md` est une modification hors périmètre déjà présente, préservée.

## 27. Backend tests

| Command | Result |
| --- | --- |
| Media R2 JPEG/PNG/MP4 + JSON | PASS, 2 |
| `mvn -pl content-service test` | PASS, 31 |
| `mvn -pl event-service test` | PASS, 21 |
| `mvn -pl place-service test` | PASS, 14 |
| `mvn -pl culture-service test` | PASS, 16; 2 PostgreSQL skipped, Docker absent |
| Country controller/geography tests | PASS, 15 |

## 28. Mobile tests

`multipart.test.mts`, `place.categories.test.mts`, `country.geography.test.mts` et `culture.language-options.test.mts` passent. `npx tsc --noEmit` passe. `npx expo lint --no-cache` donne 0 erreur et 2 warnings préexistants hors Create.

## 29. Integration tests

`R2StorageIntegrationTest` utilise Media + H2 + WireMock R2 pour accepter JPEG/PNG/MP4 à 201 et refuser JSON à 415. `GeographyControllerIntegrationTest` appelle les endpoints de la hiérarchie Centre, Mfoundi, Yaoundé et localités.

## 30. Runtime limitations

Docker local, PostgreSQL production, R2 production et un token/test device n’étaient pas disponibles. Le test Expo → Gateway → R2, la modération et la distribution sortie → Feed/Story n’ont donc pas été vérifiés runtime.

## 31. Deployment requirements

```text
MOBILE_REBUILD = YES
API_REDEPLOY = YES
CONFIG_REDEPLOY = YES
SERVICES_TO_REDEPLOY = media-service, place-service, culture-service, country-config-service, config-server, content-service, event-service
```

V9 et V5 doivent être appliquées par Flyway. Country Config doit disposer de V4. Content/Event doivent être rechargés si `COUNTRY_CONFIG_SERVICE_URL` passe à 8117.

## 32. Manual production retest

```bash
MEDIA=$(sudo docker ps --filter "name=media-service" --format '{{.Names}}' | head -1)
CONTENT=$(sudo docker ps --filter "name=content-service" --format '{{.Names}}' | head -1)
EVENT=$(sudo docker ps --filter "name=event-service" --format '{{.Names}}' | head -1)
PLACE=$(sudo docker ps --filter "name=place-service" --format '{{.Names}}' | head -1)
CULTURE=$(sudo docker ps --filter "name=culture-service" --format '{{.Names}}' | head -1)
COUNTRY=$(sudo docker ps --filter "name=country-config-service" --format '{{.Names}}' | head -1)
GATEWAY=$(sudo docker ps --filter "name=api-gateway" --format '{{.Names}}' | head -1)
sudo docker logs --tail 200 "$MEDIA"
curl -i -X POST https://api.yeyamo.com/api/v1/media -H "Authorization: Bearer $TOKEN" -F "file=@./image.jpg;type=image/jpeg"
curl -fsS https://api.yeyamo.com/api/v1/categories
curl -fsS https://api.yeyamo.com/api/v1/countries/CM/administrative-areas
curl -fsS https://api.yeyamo.com/api/v1/countries/CM/cities
curl -fsS https://api.yeyamo.com/api/v1/culture/languages
```

T1 publication image; T2 vidéo; T3 caméra; T4 multi-média; T5 Story image; T6 Story vidéo; T7/T8/T9 sortie sans/avec/retry; T10 catégories; T11 Centre→Yaoundé; T12 localités; T13/T14/T15 suggestion sans/avec/doublon; T16 langues; T17 contribution; T18/T19/T20/T21 publication/Story/place/event partenaire; T22 artwork; T23 offre générique; T24 promotion; T25 campagne; T26 ticket.

## 33. Remaining blockers

1. Offre générique partenaire sans contrat (`ORPHAN_UI`).
2. POST Event et Place sans idempotence métier serveur pour timeout ambigu.
3. V9/V5 et V4 doivent être vérifiées en production.
4. Gateway + PostgreSQL + R2 + Expo réel reste à tester.

## 34. Final matrix

| Request | Body | Before transport | Backend expected | Status |
| --- | --- | --- | --- | --- |
| Media publication | FormData | JSON before fix, no explicit type after | multipart boundary RN | `UNIT_TESTED` |
| Media Story | FormData | idem | multipart boundary RN | `UNIT_TESTED` |
| Media sortie | FormData | idem | multipart boundary RN | `UNIT_TESTED` |
| Media suggestion | FormData | idem | multipart boundary RN | `UNIT_TESTED` |
| Media Partner Place | — | `NOT_APPLICABLE` | `NOT_APPLICABLE` | `NOT_APPLICABLE` |
| Media Partner Event | FormData | idem | multipart boundary RN | `UNIT_TESTED` |

| Actor | Creation | Media | Reference data | Business POST | Retry | Status |
| --- | --- | --- | --- | --- | --- | --- |
| User | Publication | oui | N/A | posts/publish | URI | `UNIT_TESTED` |
| User | Story | oui | N/A | stories | mediaId | `UNIT_TESTED` |
| User | Sortie | oui | pays/lieu | events | mediaId | `UNIT_TESTED` |
| User | Suggestion | oui | cat./géo | suggestion | URI | `UNIT_TESTED` |
| User | Savoir | non actuel | langues | contribution/submit | mutation | `UNIT_TESTED` |
| Partner | Place | N/A | cat./région/city | places | no server key | `UNIT_TESTED` |
| Partner | Event | oui | places publiés | events | mediaId | `UNIT_TESTED` |
| Partner | Offre générique | absent | N/A | absent | N/A | `STILL_BROKEN` |

## 35. Final verdict

CONTENT_TYPE_ROOT_CAUSE =
The shared Axios instance forced Content-Type application/json onto FormData uploads, so Spring rejected POST /api/v1/media during consumes matching before MediaController was called.

SHARED_HTTP_CLIENT_MULTIPART =
UNIT_TESTED

MEDIA_UPLOAD =
INTEGRATION_TESTED

PUBLICATION =
UNIT_TESTED

STORY =
UNIT_TESTED

OUTING =
UNIT_TESTED

PLACE_CATEGORIES_ROOT_CAUSE =
DATA_GAP: place_categories had no active seed rows; the direct-list parser was correct.

PLACE_CATEGORIES =
STATICALLY_FIXED

PLACE_CITIES_ROOT_CAUSE =
The selected administrative area can be an ancestor (Centre) of the city area (Mfoundi); deployed Country Config data must also include migration V4.

PLACE_CITIES =
INTEGRATION_TESTED

PLACE_LOCALITIES =
INTEGRATION_TESTED

PLACE_SUGGESTION =
UNIT_TESTED

CULTURE_LANGUAGES_ROOT_CAUSE =
DATA_GAP: the Culture languages table had no active Cameroon language seed rows.

CULTURE_LANGUAGES =
STATICALLY_FIXED

KNOWLEDGE_CONTRIBUTION =
UNIT_TESTED

PARTNER_PLACE =
UNIT_TESTED

PARTNER_EVENT =
UNIT_TESTED

OTHER_PARTNER_CREATIONS =
Publication UNIT_TESTED; Story UNIT_TESTED; Artwork STATICALLY_FIXED; artwork offer STATICALLY_FIXED; campaign STATICALLY_FIXED; promotion STATICALLY_FIXED; ticket STATICALLY_FIXED; generic offer STILL_BROKEN (ORPHAN_UI).

REFERENCE_DATA =
STATICALLY_FIXED

CREATE_TAB_GLOBAL_STATUS =
STILL_BROKEN

MOBILE_REBUILD =
YES

API_REDEPLOY =
YES

CONFIG_REDEPLOY =
YES

SERVICES_TO_REDEPLOY =
media-service, place-service, culture-service, country-config-service, config-server, content-service, event-service

READY_FOR_PRODUCTION_RETEST =
NO
