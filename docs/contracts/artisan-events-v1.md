# Artisan event contracts — v1

All messages use the version-1 JSON envelope (`eventId`, `eventType`, `eventVersion`, `producer`, `occurredAt`, `correlationId`, `payload`; partner events also include `actorId`). `correlationId` is required and is retained by Kafka consumers in their audit/log context.

## `partner-events` — `partner-service`

| Event type | Required payload fields | Consumers |
|---|---|---|
| `ArtisanProfileCreated` | `partnerId`, `countryCode`, `verificationStatus` | graph, discovery, analytics |
| `ArtisanProfileUpdated` | `partnerId`, `countryCode`, `verificationStatus` | graph, discovery, analytics |
| `ArtisanVerified` | `partnerId`, `countryCode`, `verificationStatus=VERIFIED` | graph, discovery, notification (`AUTHENTICITY_VERIFIED`) |
| `ArtisanSuspended` | `partnerId`, `countryCode`, `verificationStatus=SUSPENDED` | graph, discovery, analytics |

## `interaction.events` — `interaction-service`

| Event type | Required payload fields | Consumers |
|---|---|---|
| `ArtisanFollowed` | `targetType=ARTISAN`, `targetId` (artisan/partner id), `userId` (follower) | notification (`ARTISAN_FOLLOWED`), recommendation, analytics |
| `ArtisanUnfollowed` | `targetType=ARTISAN`, `targetId`, `userId` | recommendation, analytics |

## `moderation.events` — `moderation-trust-service`

| Event type | Required payload fields | Consumers |
|---|---|---|
| `ArtworkAuthenticityVerified` | `claimId`, `targetType=ARTWORK`, `targetId`, `claimantId`, `reviewerId`, `status=VERIFIED` | notification (`AUTHENTICITY_VERIFIED`), discovery, analytics |
| `ArtworkAuthenticityRejected` | `claimId`, `targetType=ARTWORK`, `targetId`, `claimantId`, `reviewerId`, `status=REJECTED` | notification, analytics |

The catalog owns artwork metadata, partner owns artisan profiles, and moderation owns authenticity decisions. Events are integration facts, never a replacement for the owner service API.
