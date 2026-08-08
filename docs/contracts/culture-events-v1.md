# Culture event contracts — v1

Topic: `culture.events`  
Producer: `culture-service`

## Envelope

Every record is JSON and must contain the following fields. `eventVersion` is the wire-version field; culture events also contain `version: 1` because they are serialized from the shared `EventEnvelope`.

```json
{
  "eventId": "uuid",
  "eventType": "CultureContentPublished",
  "eventVersion": 1,
  "producer": "culture-service",
  "occurredAt": "2026-08-08T12:00:00Z",
  "correlationId": "request-or-generated-id",
  "aggregateId": "uuid",
  "payload": {}
}
```

`eventId` is the idempotency key for consumers. `correlationId` is mandatory and is copied from `X-Correlation-ID`, or generated at the service boundary. Consumers must retain it in their audit/log context.

## Event payloads

| Event type | Required payload fields | Main consumers |
|---|---|---|
| `CultureContentCreated` | `contentId`, `type`, `countryCode`, `contributorId` | analytics, graph |
| `CultureContentUpdated` | `contentId`, `contributorId` | graph, discovery, analytics |
| `CultureContentPublished` | `contentId`, `status`, `countryCode`, `contributorId` | graph, discovery, analytics |
| `CultureContentArchived` | `contentId`, `status`, `countryCode`, `contributorId` | discovery, analytics |
| `CultureContributionSubmitted` | `contentId`, `contributorId`, `userId` | moderation, analytics |
| `CultureContributionApproved` | `contentId`, `contributorId`, `reviewerId`, `reason` | notification (`CULTURE_CONTRIBUTION_APPROVED`), analytics |
| `CultureContributionRejected` | `contentId`, `contributorId`, `reviewerId`, `reason` | notification (`CULTURE_CONTRIBUTION_REJECTED`), analytics |
| `CultureTranslationAdded` | `contentId`, `languageCode`, `translatorId` | analytics |
| `CultureTranslationVerified` | `contentId`, `translationId`, `languageCode`, `translatorId`, `verifierId` | notification (`TRANSLATION_VERIFIED`), gamification, analytics |
| `LanguageLessonStarted` | `lessonId`, `userId` | analytics |
| `LanguageExerciseCompleted` | `lessonId`, `exerciseId`, `userId` | analytics |
| `LanguageLessonCompleted` | `lessonId`, `userId`, `score`, `idempotencyKey` | gamification, analytics |
| `CultureChallengeJoined` | `challengeId`, `userId` | notification (`CHALLENGE_STARTED`), analytics |
| `CultureChallengeSubmitted` | `challengeId`, `submissionId`, `userId`, `moderationRequired` | moderation, analytics |
| `CultureChallengeCompleted` | `challengeId`, `submissionId`, `userId`, `rewardDefinitionId` | notification (`CHALLENGE_RESULT`), gamification, analytics |

Unknown event types must be ignored safely by consumers that do not own them. Consumers reject an unsupported `eventVersion` before applying a projection.
