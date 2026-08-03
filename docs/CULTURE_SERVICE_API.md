# Culture Service YeYamo

## Responsabilité

`culture-service` est la source de vérité PostgreSQL des contenus culturels structurés et de l’apprentissage linguistique léger. Les profils restent dans `user-service`/`partner-service`, les binaires dans `media-service`, les interactions dans `interaction-service`, la recherche dans OpenSearch via projections et les paiements hors de ce service.

## Infrastructure

- Application : `culture-service`, port local `8115`.
- Base : `yeyamo_culture`.
- Kafka : topic `culture.events.v1` et outbox `culture_outbox`.
- Gateway : `/api/v1/culture/**` et `/api/v1/admin/culture/**`.
- Swagger : `/swagger-ui/index.html`; contrat JSON : `/v3/api-docs`.
- Les timestamps sont UTC, les pays ISO 3166-1 alpha-2 et les langues BCP 47.

## API publique

| Méthode | Endpoint | Authentification | Description |
|---|---|---|---|
| GET | `/api/v1/culture/contents` | publique | Contenus publiés; filtres type, pays, zone, ville, langue, communauté, vérification, recherche et pagination |
| GET | `/api/v1/culture/contents/{id}` | publique | Contenu public non restreint |
| GET | `/api/v1/culture/contents/{id}/translations` | publique | Traductions vérifiées |
| GET | `/api/v1/culture/languages` | publique | Langues actives ou beta |
| GET | `/api/v1/culture/languages/{code}` | publique | Langue disponible |
| GET | `/api/v1/culture/languages/{code}/content` | publique | Contenus publiés de la langue |
| GET | `/api/v1/culture/languages/{code}/lessons` | publique | Leçons publiées |
| GET | `/api/v1/culture/language-lessons/{id}` | publique | Leçon, items et exercices sans réponse correcte |
| GET | `/api/v1/culture/daily-word` | publique | Sélection persistée stable par date UTC, pays et langue |
| GET | `/api/v1/culture/daily`, `/trending`, `/categories` | publique | Sélections et taxonomie |

## Contribution

Toutes les routes exigent un JWT; l’acteur est le `sub` JWT.

- `POST /api/v1/culture/contributions`
- `GET /api/v1/culture/contributions/me`
- `GET|PUT|DELETE /api/v1/culture/contributions/{id}`
- `POST /api/v1/culture/contributions/{id}/submit`

Une suppression n’est autorisée qu’en `DRAFT`. Workflow : `DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED/CORRECTIONS_REQUIRED/REJECTED → PUBLISHED/ARCHIVED`.

## Administration

Rôles : `EDITOR`, `MODERATOR`, `ADMIN`, `SUPER_ADMIN` selon mutation.

- `GET|POST /api/v1/admin/culture/contents`
- `GET|PUT /api/v1/admin/culture/contents/{id}`
- `PATCH /api/v1/admin/culture/contents/{id}/status`
- `GET /api/v1/admin/culture/contributions`
- `POST /api/v1/admin/culture/contributions/{id}/review`
- `PATCH /api/v1/admin/culture/translations/{id}/verify`

Les contenus `SACRED` et `COMMUNITY_RESTRICTED` nécessitent `verificationStatus=VERIFIED` avant publication. Ils ne sont jamais retournés par les endpoints publics.

## Apprentissage linguistique

JWT requis pour :

- `POST /api/v1/culture/language-lessons/{id}/start`
- `POST /api/v1/culture/language-lessons/{id}/attempts`
- `POST /api/v1/culture/language-lessons/{id}/complete`
- `GET /api/v1/culture/language-progress/me[/{languageCode}]`

Une progression est unique par utilisateur/leçon et conserve `startedAt`, `completedAt`, score et nombre de tentatives. La complétion est idempotente : un deuxième appel ne modifie ni score ni date et ne republie pas l’événement XP. Une soumission de prononciation exige un `mediaId`; aucune notation automatique n’est fabriquée et `manualReviewRequired=true` est retourné.

## Événements

`CultureContentCreated`, `CultureContentUpdated`, `CultureContentPublished`, `CultureContentArchived`, `CultureContributionSubmitted`, `CultureContributionApproved`, `CultureTranslationAdded`, `CultureTranslationVerified`, `LanguageLessonStarted`, `LanguageLessonCompleted`, `LanguageExerciseCompleted`, `DailyWordViewed`, `PronunciationSubmitted`.

Chaque événement utilise `EventEnvelope`, est écrit dans la transaction via l’outbox, puis relayé vers Kafka. Les consumers Analytics, Gamification, Discovery et Graph doivent dédupliquer par `eventId`.

Le test PostgreSQL/Flyway Testcontainers est activable explicitement avec `RUN_TESTCONTAINERS=true`; il reste désactivé par défaut afin de ne pas imposer Docker aux tests unitaires.

## Médias et données personnelles

Seuls les UUID `audioMediaId`, `imageMediaId` et `mediaId` sont conservés. Le service ne stocke ni fichier ni URL signée. Les identités de témoins sont facultatives; leur consentement est explicite et les endpoints publics n’exposent pas directement `culture_sources`.
