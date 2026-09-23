# Place Suggestions — backend completion

## Pipeline implemented

The former public flow had a `PENDING` entity and admin approve/reject routes, but no mandatory country, structured geography, media, decision event or notification. It now uses the existing bounded contexts only:

```text
Mobile -> upload existing media-service -> mediaId
Mobile -> POST /place-suggestions/check-duplicates (advisory)
Mobile -> POST /place-suggestions
       -> country-config validation + media ownership/status + duplicate check
       -> PENDING suggestion + ordered media references + transactional outbox
Moderator -> approve/reject under pessimistic row lock
approve -> existing canonical Place OR create canonical Place -> normal place.events pipeline
decision event -> notification-service -> in-app notification
```

`place-service` stores references only, never binary files. `place.suggestion.*` uses the existing `place_outbox` and topic `place.events`; catalog/event canonical-place consumers now explicitly ignore those suggestion events. Canonical `place.created` and `place.updated` continue through `catalog-service`, `catalog.events`, Discovery and Recommendation. No Feed post is created.

## Routes

| Method | Route | Auth/role | Function |
|---|---|---|---|
| POST | `/api/v1/place-suggestions/check-duplicates` | JWT | Advisory canonical and pending-match lookup; the later POST remains authoritative. |
| POST | `/api/v1/place-suggestions` | JWT | Creates an actual `PENDING` suggestion. |
| GET | `/api/v1/place-suggestions/me?page=&size=` | JWT | Owner-only paged list, size bounded to 1–100. |
| GET | `/api/v1/admin/place-suggestions?status=&page=&size=` | `MODERATOR`, `ADMIN`, `SUPER_ADMIN` | Moderation queue. |
| PATCH | `/api/v1/admin/place-suggestions/{id}/approve` | moderator role | Links a canonical Place or creates one. |
| PATCH | `/api/v1/admin/place-suggestions/{id}/reject` | moderator role | Rejects while retaining the reason and timestamp. |

The gateway already routes `/api/v1/place-suggestions/**` to `place-service`, so no new gateway predicate was required.

## Actual create contract

The original request DTO had `countryCode` optional and only free-text `region`. All previous fields remain compatible; country/geography/media were added.

```json
{
  "name": "Musée national",
  "address": "Boulevard du 20 mai, Yaoundé",
  "description": "Musée public",
  "category": "CULTURE",
  "placeType": "MUSEUM",
  "region": "Centre",
  "countryCode": "CM",
  "administrativeAreaId": "2d935f66-8c47-4d20-b918-e1d6d3d92a35",
  "cityId": "9c7978f3-8d8d-4367-95f5-08e6bfdf7d29",
  "localityId": "77e23e6d-5bb5-4c11-9a79-20e45d02a4ae",
  "mediaIds": ["030f0759-72a2-480a-b78e-6f906f8f1fea"],
  "latitude": 3.866,
  "longitude": 11.517
}
```

`name`, `address`, `countryCode`, `latitude`, and `longitude` are required. Coordinates are non-null and must be respectively in `[-90,90]` and `[-180,180]`. The optional ordered `mediaIds` are distinct and capped at eight.

`201` returns the genuine suggestion:

```json
{
  "id": "f45ec80a-7e7b-45db-89e2-6e2d131bf2a4",
  "submitterUserId": "user-42",
  "name": "Musée national",
  "address": "Boulevard du 20 mai, Yaoundé",
  "countryCode": "CM",
  "latitude": 3.866,
  "longitude": 11.517,
  "status": "PENDING",
  "canonicalPlaceId": null,
  "moderationReason": null,
  "reviewedAt": null,
  "createdAt": "2026-09-20T12:00:00Z",
  "updatedAt": "2026-09-20T12:00:00Z",
  "administrativeAreaId": "2d935f66-8c47-4d20-b918-e1d6d3d92a35",
  "cityId": "9c7978f3-8d8d-4367-95f5-08e6bfdf7d29",
  "localityId": "77e23e6d-5bb5-4c11-9a79-20e45d02a4ae",
  "media": [{
    "mediaId": "030f0759-72a2-480a-b78e-6f906f8f1fea",
    "type": "IMAGE",
    "contentType": "image/jpeg",
    "contentUrl": "/api/v1/media/030f0759-72a2-480a-b78e-6f906f8f1fea/content",
    "thumbnailUrl": "/api/v1/media/030f0759-72a2-480a-b78e-6f906f8f1fea/thumbnail",
    "displayOrder": 0
  }]
}
```

`201` means the suggestion exists, never that a Place has already been published. `GET /me` returns Spring `Page<PlaceSuggestionResponse>` with this same response as `content`; its media preview is local data, so it makes no per-row Media/Country/Place calls (**NO_N_PLUS_ONE**).

## Geography and media validation

| Field | Source | Validation | Required |
|---|---|---|---|
| `countryCode` | country-config-service | known, `LIVE`/`BETA`, `PLACE_PUBLISHING` enabled | yes |
| `administrativeAreaId` | country-config administrative area | active and same country | no |
| country-config `cityId` | country-config city | active, same country and selected area | no |
| `localityId` | country-config locality | active, same country/city/area | no |
| `address` | user input | nonblank, max 500 | yes |
| coordinates | map selection | bounds above | yes |

Historic database rows keep nullable `country_code`; V8 does not make that legacy column `NOT NULL`. New API calls require it. The approval DTO retains its existing legacy `regionId`/`cityId`/`districtId`, which are distinct from country-config geographic IDs; no false automatic mapping was invented.

Mobile uploads through existing authenticated `POST /api/v1/media` then submits IDs. For each ID, place-service reads the existing media metadata and requires JWT ownership, `READY`, and `IMAGE` or `VIDEO`. Therefore unavailable, `FAILED` and `DELETED` media cannot attach; current media-service has no `REJECTED` enum. The snapshot keeps ID, order, type and existing content/thumbnail URLs. On creation of a new canonical Place, the same IDs are written to `place_media.media_id`; an already existing canonical Place and its cover/gallery are never replaced.

## Duplicates, moderation and safety

Names and addresses are trimmed, lowercased, accent-folded and whitespace-normalized. Canonical candidates use existing PostGIS proximity within 250m. Pending candidates use country plus normalized name or address. Only same normalized name **and** address within 100m is a `certain` duplicate and blocks POST as `409 CERTAIN_DUPLICATE`; ambiguous matches are returned to mobile only.

`POST /check-duplicates` accepts `name`, `address`, `countryCode`, optional `cityId`, `latitude`, `longitude` and returns `200`:

```json
{"possibleDuplicates":[{"kind":"CANONICAL_PLACE","id":"UUID","name":"Musée national","address":"...","distanceMeters":62.4,"certain":true}]}
```

POST repeats the check. V8 gives **new** submissions a rounded natural dedupe key with a partial unique index; historic rows leave it null, so old duplicate data cannot make the migration fail. An index race is also converted to `CERTAIN_DUPLICATE`.

Approve locks the suggestion pessimistically. `PENDING -> APPROVED` either links a certain existing Place or creates one transactionally. Repeated approve returns the already-approved row, creates neither a second Place nor a second decision event. `PENDING -> REJECTED` preserves the mandatory reason. Other cross-decision transitions return `409 INVALID_SUGGESTION_STATUS`.

## Events, notification, XP and migrations

| Event | Topic | Producer | Consumer | Effect |
|---|---|---|---|---|
| `place.suggestion.created` | `place.events` | place-service outbox | none | durable lifecycle audit |
| `place.suggestion.approved` | `place.events` | place-service outbox | notification-service | one `PLACE_SUGGESTION_APPROVED` in-app notification |
| `place.suggestion.rejected` | `place.events` | place-service outbox | notification-service | one `PLACE_SUGGESTION_REJECTED` in-app notification |
| `place.created` / `place.updated` | `place.events` | place-service outbox | catalog/event services | canonical Place projection |
| `catalog.asset.synchronized` | `catalog.events` | catalog-service outbox | Discovery, Recommendation | normal searchable/recommendable projection |

Notification-service persists processed Kafka event IDs and notification source IDs, so replay does not duplicate a decision notification. V7 adds real French in-app templates. No new push, email or SMS provider is claimed.

**XP_POLICY_NOT_DEFINED**: gamification-service contains no existing approved-place-suggestion policy, so no arbitrary XP is granted. The existing authenticated gateway mutation rate-limit remains in effect; no parallel anti-fraud system was introduced.

| Migration | Service | Change |
|---|---|---|
| `V8__harden_place_suggestions.sql` | place-service | geography IDs, safe dedupe key/index, ordered suggestion media, canonical Place media ID. |
| `V7__place_suggestion_notification_templates.sql` | notification-service | French in-app approval/rejection templates. |

## Error codes

`VALIDATION_ERROR` (400), `INVALID_COUNTRY` (400), `INVALID_LOCATION` (400), `DUPLICATE_MEDIA_ID`/`MEDIA_LIMIT_EXCEEDED` (400), `MEDIA_NOT_FOUND` (404), `MEDIA_NOT_OWNED` (403), `MEDIA_NOT_USABLE` (400/409), `CERTAIN_DUPLICATE` (409), `SUGGESTION_NOT_FOUND` (404), `INVALID_SUGGESTION_STATUS` (409), and `COUNTRY_VALIDATION_UNAVAILABLE`/`LOCATION_VALIDATION_UNAVAILABLE`/`MEDIA_VALIDATION_UNAVAILABLE` (503) follow the existing error envelope.

## Tests actually executed

| Command | Module | Result |
|---|---|---|
| `mvn -pl place-service -am test -DskipTests` | shared-lib + place dependencies | PASS — compilation. |
| `mvn -pl notification-service,catalog-service,event-service -am test -DskipTests` | affected consumer modules | PASS — compilation. |
| `mvn -pl place-service -am test -Dtest=PlaceSuggestionServiceTest` | place-service | PASS — 3 tests: PENDING creation, certain pending duplicate, double approve idempotence. |
| `mvn -pl notification-service test -Dtest=EventNotificationPolicyTest` | notification-service | PASS — 7 tests, including decision recipient/type. |

No live PostgreSQL concurrent race, Kafka broker or remote country/media service was launched; the PostgreSQL index and unit tests cover the implemented logic. Reverse geocoding, fuzzy matching, file scanning/moderation, Feed publication, booking and ticketing remain deliberately out of scope.

# MOBILE_CONTRACT

| METHOD / PATH | AUTH | REQUEST | RESPONSE | STATUS / ERROR CODES | OLD MOBILE CALL | LOCAL STATE |
|---|---|---|---|---|---|---|
| `POST /api/v1/place-suggestions/check-duplicates` | JWT | fields described above | `possibleDuplicates[]` | `200`, validation/country/location errors | **ADD** as advisory preflight | **CACHE_ONLY** while editing; never source of truth |
| `POST /api/v1/place-suggestions` | JWT | create JSON above; mandatory country, optional geo/media IDs | `PlaceSuggestionResponse` | `201`, duplicate/country/media errors above | **MODIFY** | **REMOVE** fake local Place; cache only returned PENDING row |
| `GET /api/v1/place-suggestions/me?page=&size=` | JWT | query pagination | `Page<PlaceSuggestionResponse>` | `200` | **KEEP**, parse new geography/media fields | **CACHE_ONLY**, backend owns status |
| `GET /api/v1/admin/place-suggestions` | moderator | optional status/page/size | page | `200`, `403` | **KEEP** for moderator app | **CACHE_ONLY** |
| `PATCH /api/v1/admin/place-suggestions/{id}/approve` | moderator | existing moderation DTO | APPROVED response with canonical ID | `200`, `404`, `409` | **KEEP** | **REMOVE** optimistic canonical Place fabrication |
| `PATCH /api/v1/admin/place-suggestions/{id}/reject` | moderator | `{ "reason": "..." }` | REJECTED response | `200`, `404`, `409` | **KEEP** | **CACHE_ONLY** after response |

The mobile must upload first, preserve the `mediaIds` order, and never send bytes to place-service, infer media ownership, set final moderation state, or create a local canonical Place.
