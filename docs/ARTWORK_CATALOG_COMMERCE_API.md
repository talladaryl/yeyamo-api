# YeYamo Artwork Catalog and Commerce

## Ownership

- `catalog-service` owns artwork identity, narrative, provenance references, translations, media IDs, authenticity and availability.
- `media-service` remains the binary source of truth; only `mediaId` is stored.
- `partner-service` remains the artisan identity source of truth.
- `culture-service` remains the structured cultural-content source; artwork stores an optional `cultureContentId`.
- `commerce-service` owns offers, orders, inventory reservations, commission snapshots and payment saga state.
- `payment-service` authorizes/refunds money and never owns artwork inventory.

## Public APIs

- `GET /api/v1/artworks`, `GET /api/v1/artworks/{id}`
- `GET /api/v1/artworks/{id}/history|media|related`
- `GET /api/v1/artisans/{artisanId}/artworks`
- `GET /api/v1/artwork-materials`, `GET /api/v1/artwork-techniques`
- `GET /api/v1/artwork-offers/{artworkId}`

## Authenticated APIs

- Artwork create/update/delete, availability and history mutations validate the JWT `partnerId`/`partnerIds`, except administrators.
- Offer mutations validate the artisan partner claim.
- `POST /api/v1/artwork-orders` requires `Idempotency-Key`.
- Buyer and artisan order endpoints enforce ownership independently.

## Consistency

Inventory is reserved under a pessimistic database lock before `PaymentRequested`. Payment failure releases the reservation. Payment success atomically consumes reserved inventory and marks a zero-stock offer `SOLD_OUT`. Commission values come from the existing pricing and commission engine and are snapshotted with `BigDecimal` amounts and ISO currency.

`AUCTION_FUTURE` is modeled but cannot be activated in V1. International logistics are limited to an availability flag and delivery type; no unsupported carrier workflow is implied.
