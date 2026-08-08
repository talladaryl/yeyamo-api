# Culture and Artisan Backend — implementation reference

Last updated: 2026-08-08

## Service ownership and ports

| Service | Port | Persistence | Responsibility |
|---|---:|---|---|
| `culture-service` | 8115 | PostgreSQL `yeyamo_culture` | Culture content, contributions, translations, language learning and challenges |
| `graph-service` | 8116 | Neo4j | Read-model of culture, artwork, artisan, place and event relations |
| `catalog-service` | 8088 | PostgreSQL `yeyamo_catalog` | Artwork catalogue, materials, techniques and artwork media/history |
| `partner-service` | 8087 | PostgreSQL `yeyamo_partner` | Artisan profiles, specialties and verification |
| `commerce-service` | 8113 | PostgreSQL `yeyamo_commerce` | Artwork offers, orders, payment state and fulfilment |
| `interaction-service` | 8091 | PostgreSQL | Artwork likes and artisan follows |
| `moderation-trust-service` | 8100 | PostgreSQL | Artwork authenticity claims and cultural moderation |
| `discovery-service` | 8093 | PostgreSQL + OpenSearch | Culture/artisan/artwork search projections |

The ports 8115 and 8116 were assigned after checking the Docker Compose matrix: 8110–8114 were already occupied by campaign, ads, ticket, commerce and support. `cloud-conf-yeyamo/culture-service.properties`, `graph-service.properties` and `commerce-service.properties` are Config Server sources. All services register with Eureka through the shared configuration.

## Gateway routes

| Pattern | Target | Notes |
|---|---|---|
| `/api/v1/culture/**` | `culture-service` | Public culture, contributions, learning and challenges |
| `/api/v1/admin/culture/**` | `culture-service` | Culture editorial/moderation endpoints |
| `/api/v1/culture-graph/**` | `graph-service` | Public graph queries |
| `/api/v1/admin/culture-graph/**` | `graph-service` | Graph rebuild operations |
| `/api/v1/artworks/**` | `catalog-service` | Artwork is catalogue-owned |
| `/api/v1/artisans/*/artworks` | `catalog-service` | More specific route than artisan profiles |
| `/api/v1/artisans/**`, `/api/v1/admin/artisans/**`, `/api/v1/artisan-specialties/**` | `partner-service` | Artisan profile ownership |
| `/api/v1/artwork-orders/**`, `/api/v1/artisan/orders/**` | `commerce-service` | Artwork order workflow |
| `/api/v1/artwork-offers/**` | `commerce-service` | Artwork offers |

The gateway uses explicit negative route priorities so specialised culture/artisan routes win over the generic `/api/v1/admin/**` route.

## Endpoint documentation

Every owner exposes OpenAPI v3 at `/v3/api-docs` and Swagger UI at `/swagger-ui.html`:

| Owner | Endpoint families |
|---|---|
| culture | `/api/v1/culture/contents`, `contributions`, `languages`, `language-lessons`, `language-progress`, `daily-word`, `challenges`, and `/api/v1/admin/culture/**` |
| graph | `/api/v1/culture-graph/{artworks,cultures,languages,artisans,places}/**`, `/discover`, `/api/v1/admin/culture-graph/rebuild` |
| catalogue | `/api/v1/artworks/**`, `/api/v1/artisans/{id}/artworks`, artwork material/technique references |
| partner | `/api/v1/partners/me/artisan-profile`, `/api/v1/artisans/**`, `/api/v1/admin/artisans/**` |
| commerce | `/api/v1/artwork-offers/**`, `/api/v1/artwork-orders/**`, `/api/v1/artisan/orders/**` |

OpenAPI dependencies/configuration are present in the culture, graph, catalogue, partner and commerce services. Gateway CORS admits and exposes `X-Correlation-ID` and rate-limit headers.

## Persistence and migrations

| Database | Core tables | Relevant migrations |
|---|---|---|
| `yeyamo_culture` | `culture_contents`, `culture_content_translations`, `culture_sources`, `language_*`, `culture_challenges`, `culture_outbox`, `culture_audit` | `V1__create_culture_schema.sql`, `V2__culture_challenges.sql`, `V3__culture_audit.sql` |
| `yeyamo_catalog` | `artworks`, `artwork_translations`, `artwork_media`, `artwork_history`, materials/techniques and `catalog_outbox` | `V9__artwork_catalog.sql` |
| `yeyamo_partner` | `artisan_profiles`, profile languages/specialties, KYC requirements and partner outbox | existing partner artisan migrations |
| `yeyamo_commerce` | `artwork_offers`, `artwork_orders`, `artwork_order_history`, `commerce_outbox`, `commerce_audit` | `V5__artwork_commerce.sql` |

`culture_audit` is written in the same transaction as the culture outbox record. Catalogue, partner and commerce retain the correlation value in their transactional outbox records.

## Kafka contracts and consumers

| Topic | Producer(s) | Key event families |
|---|---|---|
| `culture.events` | `culture-service` | Content lifecycle, contribution review, translations, learning, challenges |
| `catalog.events` | `catalog-service` | Artwork lifecycle and availability |
| `partner-events` | `partner-service` | Artisan profile lifecycle and verification |
| `commerce.events` | `commerce-service` | Artwork orders, order status, sales |
| `interaction.events` | `interaction-service` | Artwork likes and artisan follows |
| `moderation.events` | `moderation-trust-service` | Artwork authenticity decisions |

Contract details are versioned in:

- `docs/contracts/culture-events-v1.md`
- `docs/contracts/artwork-events-v1.md`
- `docs/contracts/artisan-events-v1.md`

Notifications map the following delivery types: `CULTURE_CONTRIBUTION_APPROVED`, `CULTURE_CONTRIBUTION_REJECTED`, `TRANSLATION_VERIFIED`, `CHALLENGE_STARTED`, `CHALLENGE_RESULT`, `ARTWORK_LIKED`, `ARTWORK_SOLD`, `ARTWORK_ORDER_CREATED`, `ARTWORK_ORDER_UPDATED`, `ARTISAN_FOLLOWED`, and `AUTHENTICITY_VERIFIED`.

Consumers validate `eventVersion=1`, deduplicate on `eventId`, and retain the envelope correlation id in MDC. Graph projections also persist the correlation id on their Neo4j `ProcessedEvent` audit node.

## Neo4j and OpenSearch

Neo4j is part of the Docker environment (`neo4j:5.24-community`, Bolt 7687, browser 7474). `graph-service` projects nodes for `Culture`, `Language`, `Tradition`, `Artwork`, `Artisan`, `Material`, `Technique`, `Place`, and `Event`, with validated relation types such as `CREATED`, `ORIGINATES_FROM`, `ASSOCIATED_WITH`, `USES_MATERIAL`, and `USES_TECHNIQUE`.

OpenSearch remains owned by `discovery-service`. Culture/artisan/artwork consumers update the discovery indices; no inactive `search-service` or `social-service` is started by Compose.

## Security, permissions and rate limits

- Gateway validates JWTs and converts `roles`, `scope/scopes`, and `permissions` claims.
- Culture administration: `EDITOR`, `MODERATOR`, `ADMIN`, `SUPER_ADMIN`; contribution and learning mutations require a user JWT.
- Graph rebuild: `ADMIN` or `SUPER_ADMIN`; graph queries are public.
- Artwork mutations require the owner partner claim or platform-admin role; artwork reads are public.
- Artisan profile mutations require the owning partner; verification requires `ADMIN` or `SUPER_ADMIN`.
- Artwork orders require a customer JWT plus `Idempotency-Key`; artisan fulfilment requires partner ownership.

Gateway limits are Redis-backed and independent per client/category per 60 seconds (environment-overridable):

| Category | Default |
|---|---:|
| Audio upload (`/api/v1/media/culture` audio usage) | 10 |
| Artwork create/upload | 20 |
| Culture contributions | 30 |
| Translation mutations | 50 |
| Language quiz attempts/completion | 20 |
| Artwork order mutations | 30 |
| Moderation reporting | 10 |

## Correlation propagation

1. Gateway accepts a valid `X-Correlation-ID` or generates one, returns it, and forwards it downstream.
2. Culture and graph HTTP filters store it in MDC. Catalogue, partner and commerce propagate the forwarded header into their owner commands/outbox rows.
3. The culture audit/outbox, catalogue outbox, partner outbox and commerce outbox retain the correlation id transactionally.
4. Kafka envelopes carry `correlationId`; graph, commerce payment and notification consumers restore it to MDC before processing.

## Docker and frontend dependencies

Docker Compose includes `culture-service`, `graph-service`, and Neo4j. Kafka initialization creates culture, catalogue, commerce, interaction, artisan and moderation topics. Search and social service definitions remain inactive/commented.

Frontend clients need:

- generated API clients from the owner service OpenAPI documents;
- `Authorization: Bearer <JWT>` for mutations and `Idempotency-Key` for `POST /api/v1/artwork-orders`;
- support for the response `X-Correlation-ID` and rate-limit headers;
- media uploads through `POST /api/v1/media/culture` with a controlled `usageType`;
- no direct Neo4j, Kafka or OpenSearch access.
