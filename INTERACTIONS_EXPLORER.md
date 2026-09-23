# Interactions Explorer — implementation report

## Matrix

| Action | Before | Final behaviour | Persistence |
|---|---|---|---|
| Like/comment/share | Existing post interaction flows | Unchanged | interaction-service |
| Favorite post | Existing post relation and `/api/v1/saves` | Unchanged; posts retain their historical source of truth | `relations` |
| Favorite Explorer target | No dedicated Explorer API | New generic route for PLACE, EVENT, ACTIVITY/EXPERIENCE and CULTURE_CONTENT | `generic_interactions` |
| Interested / not interested | Absent | Exclusive persisted feedback state, consumed by recommendations | interaction + recommendation |
| Block | Existing relation | Unchanged; remains stronger than mute | `blocks` |
| Mute | Absent | Independent social relation, filters personal feed | user + feed projection |
| Report | Client could send owner | Owner resolved server-side | moderation projection |

## MOBILE_CONTRACT

The existing gateway route predicates already cover `/api/v1/interactions/**`, `/api/v1/users/**`, and `/api/v1/moderation/**`; no new gateway route is required. The checked-in static mobile OpenAPI catalogue has not been regenerated in this pass, so generated clients must use service OpenAPI or the contracts below until that documentation build runs.

Feedback: `PUT /api/v1/interactions/feedback` with `{"targetType":"EVENT","targetId":"id","feedbackType":"INTERESTED"}`. Accepted types are POST, PLACE, EVENT, ACTIVITY, EXPERIENCE and CULTURE_CONTENT; accepted values are INTERESTED and NOT_INTERESTED. The JWT subject is always the actor. Same-state calls are idempotent; changing state retires the opposite state atomically. Undo: `DELETE /api/v1/interactions/feedback?targetType=EVENT&targetId=id`.

Favorites: posts continue to use `PUT/DELETE /api/v1/interactions/posts/{postId}/favorite` and `GET /api/v1/saves`. Non-post Explorer cards use `PUT/DELETE /api/v1/interactions/favorites/{targetType}/{targetId}`. `GET /api/v1/interactions/favorites?page=0&size=20` pages the persisted non-post favorites without one target request per row. Favorites never create INTERESTED feedback; catalog collections remain separate.

Mute: `PUT/DELETE /api/v1/users/social/{profileId}/mute`; self-mute is rejected and both operations are idempotent. `GET /api/v1/users/social/muted` lists muted profiles. Mute does not alter follows, block, search or notifications.

Report: `POST /api/v1/moderation/reports` now accepts `{"targetType":"STORY","targetId":"id","reason":"SPAM","details":"optional"}`. `targetOwnerId` is removed. STORY, EVENT, PLACE and PLACE_SUGGESTION are added report target types. Missing non-user target projections return `REPORT_TARGET_NOT_FOUND`. A place with no user owner is reportable but no partner id is fabricated as a trust subject.

## Events, cache and N+1

| Event | Consumer | Effect |
|---|---|---|
| `interaction.feedback.updated` | recommendation | Persists feedback; filters NOT_INTERESTED and applies INTERESTED with existing favorite signal weight 3 |
| `interaction.feedback.removed` | recommendation | Removes feedback and reverses a prior interest signal |
| `social.muted` / `social.unmuted` | feed | Updates viewer-to-author mute projection and invalidates feed cache |
| content post/story, event, place/suggestion events | moderation | Projects reportable target existence and known owner |

All consumers use processed-event receipts. Feed consumes both `user.events` and legacy `user-events`; an identical event id is processed once. Recommendation batches feedback for the candidate window, filters before ranking/pagination, and returns `viewerState.feedbackType`; its cache key is user-scoped and feedback invalidates the cache version. Feed loads muted authors once and filters before ranking. Public feed is unchanged.

Recommendation candidates do not currently carry an author id, so mute cannot safely filter recommendation cards until catalog/content/event candidate events and the candidate table expose one. This is documented rather than faked. Feed feedback is persisted but not a feed-ranking input in this pass. Interaction feedback/favorites validate supported target types but do not make per-card upstream calls; report creation requires a server target projection. Availability is still determined by existing candidate lifecycle events.

## Migrations

* `interaction-service/V7__explorer_feedback_and_generic_favorites.sql`
* `user-service/V6__create_mutes.sql`
* `feed-service/V7__feed_muted_authors.sql`
* `recommendation-service/V7__recommendation_feedback.sql`
* `moderation-trust-service/V6__reportable_target_projection.sql`

Before enabling the new report contract in production, replay retained content/event/place topics or backfill the reportable-target projection. This prevents historical records from relying on a client-supplied owner.

## Security and validation

Every mutation derives its actor from `Authentication.getName()`; no request supplies its acting user and moderation resolves owner internally.

`mvn -pl interaction-service,user-service,feed-service,recommendation-service,moderation-trust-service -am test-compile -DskipTests` passed. Targeted tests all passed: GenericInteractionServiceTest (2), RecommendationEventConsumerTest + RecommendationQueryServiceTest (9), SocialGraphAlignmentTest + SocialSettingsTest (3), ModerationServiceTests (2), and FeedQueryServiceTests + FeedEventConsumerTests (10). A previous full five-service `mvn test` exceeded the 120-second command limit while Spring contexts started; it was a timeout, not an assertion failure.

## MOBILE ACTION MAP

| UI action | Call | Result |
|---|---|---|
| Interested | feedback PUT / INTERESTED | Mark card; refresh if needed |
| Not interested/global skip | feedback PUT / NOT_INTERESTED | Remove card and prevent later recommendation reappearance |
| Undo | feedback DELETE | Clear viewer state |
| Save post | historical favorite route | Existing UI stays valid |
| Save place/event/activity/culture | generic favorite route | Toggle non-post favorite |
| Mute author | social mute route | Personal feed filters authored cards |
| Report story/event/place/suggestion | moderation report without owner | Surface duplicate/open or unavailable-target response |
