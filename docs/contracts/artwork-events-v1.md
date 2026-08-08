# Artwork event contracts — v1

Artwork behaviour is owned by three services. A consumer subscribes only to the topics it needs; it must deduplicate with `eventId` and preserve `correlationId`.

## Common envelope

```json
{
  "eventId": "uuid",
  "eventType": "ArtworkOrderCreated",
  "eventVersion": 1,
  "producer": "commerce-service",
  "occurredAt": "2026-08-08T12:00:00Z",
  "correlationId": "request-or-generated-id",
  "actorId": "user-or-service-id",
  "payload": {}
}
```

## `catalog.events` — `catalog-service`

| Event type | Required payload fields | Consumers |
|---|---|---|
| `ArtworkCreated` | `artworkId`, `artisanPartnerId`, `availability`, `countryCode` | graph, discovery, analytics |
| `ArtworkUpdated` | `artworkId`, `artisanPartnerId`, `availability`, `countryCode` | graph, discovery, analytics |
| `ArtworkArchived` | `artworkId`, `artisanPartnerId`, `availability`, `countryCode` | discovery, analytics |
| `ArtworkAvailabilityChanged` | `artworkId`, `artisanPartnerId`, `availability`, `countryCode` | discovery, analytics |
| `ArtworkSold` | `artworkId`, `artisanPartnerId`, `availability`, `countryCode` | notification (`ARTWORK_SOLD`), analytics |

## `commerce.events` — `commerce-service`

| Event type | Required payload fields | Consumers |
|---|---|---|
| `ArtworkOrderCreated` | `orderId`, `artworkOfferId`, `artworkId`, `buyerUserId`, `artisanPartnerId`, `status`, `amount`, `currency` | notification (`ARTWORK_ORDER_CREATED`), analytics |
| `ArtworkOrderPaid`, `ArtworkOrderAccepted`, `ArtworkOrderIn_production`, `ArtworkOrderReady`, `ArtworkOrderShipped`, `ArtworkOrderDelivered`, `ArtworkOrderCancelled`, `ArtworkOrderRefunded` | `orderId`, `artworkOfferId`, `buyerUserId`, `artisanPartnerId`, `status`, `amount`, `currency` | notification (`ARTWORK_ORDER_UPDATED`), analytics |
| `ArtworkSold` | `orderId`, `artworkId`, `buyerUserId`, `artisanPartnerId`, `amount`, `currency` | notification (`ARTWORK_SOLD`), analytics |

## `interaction.events` — `interaction-service`

| Event type | Required payload fields | Consumers |
|---|---|---|
| `ArtworkLiked` | `targetType=ARTWORK`, `targetId` (artwork id), `userId` | notification (`ARTWORK_LIKED`), recommendation, analytics |

The interaction producer must additionally provide the artwork owner as `artisanPartnerId` when a notification consumer needs a recipient; it must never infer a recipient from the artwork id.
