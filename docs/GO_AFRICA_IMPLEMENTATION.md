# YeYamo Go Africa implementation guide

This guide defines the compatible, Africa-ready model used by YeYamo services. It complements the execution-oriented [data migration report](../AFRICA_READY_DATA_MIGRATION_REPORT.md).

## Core contract

Every country-aware aggregate carries an ISO 3166-1 alpha-2 `countryCode`. Geographic detail is optional and is expressed with the country-config identifiers:

```text
countryCode -> adminLevel1Id -> adminLevel2Id -> cityId -> localityId
```

`languageCode` is a BCP 47/platform language code. `timezone` is an IANA timezone. Monetary amounts are `BigDecimal` plus a mandatory ISO-4217 `currencyCode`; the platform does not infer a currency from country and does not perform implicit FX conversion.

The country configuration service is the authority for country launch status, enabled features, administrative hierarchy, supported languages, default currency and timezone. A service must fail closed when an action requires country configuration and that configuration cannot be obtained.

## User and registration

`POST /api/v1/auth/register` accepts country-aware registration data:

```json
{
  "email": "person@example.com",
  "phone": "+237699000000",
  "countryCode": "CM",
  "cityId": "<optional-country-config-uuid>",
  "preferredLanguageCode": "fr-CM",
  "timezone": "Africa/Douala"
}
```

The phone format is E.164. Registration checks that the country exists, is registration-enabled and allows the requested launch status. A `COMING_SOON` country is accepted only when configuration explicitly enables it.

Authenticated profile routes are:

| Method | Route | Purpose |
|---|---|---|
| PATCH | `/api/v1/users/me/location` | principal country, administrative areas, city, locality and timezone |
| PATCH | `/api/v1/users/me/language` | preferred language and content languages |
| PATCH | `/api/v1/users/me/discovery-preferences` | countries to discover, local radius, Africa-content opt-in and preferred currency |

Profile events are `UserCountrySelected`, `UserLocationUpdated`, `UserLanguageUpdated` and `UserDiscoveryPreferencesUpdated`. They use an outbox and include a correlation id and occurrence time.

## Country-aware domain behaviour

| Area | Rule |
|---|---|
| Places | creating or publishing requires `contentPublishingEnabled` or `placePublishingEnabled` for the target country |
| Events | creation requires `contentPublishingEnabled` **and** `eventFeatureEnabled`; physical events retain country/city/coordinates |
| Culture/artworks | publishing follows `artisanCommerceEnabled` or `cultureModuleEnabled` as appropriate |
| Booking | creation is gated by `bookingEnabled`; booking and activity slot snapshot their country |
| Payments | payment initiation is gated by `paymentsEnabled`; routing selects an enabled provider by country, currency, method and operation |
| Partners | onboarding requires `partnerOnboardingEnabled`; KYC requirements are selected by country and partner type |
| Discovery | result documents carry country, admin area, city, language codes, coordinates, content type and launch visibility |

The shared country-config client uses a short local cache, bounded timeout, circuit breaker and strict fallback. Configuration failure never silently grants a country-gated action.

## Content, feed and discovery

Geolocated posts, stories, places, culture content, events, partners, artisans, artworks, challenges and editorial collections carry the applicable country/geography/language fields. Kafka events for those aggregates include `countryCode`, relevant `languageCode`, `occurredAt` and `correlationId`.

Discovery is exposed by `GET /api/v1/discovery/search`. Its useful territorial filters are `countryCode`, repeated/comma-separated `countries`, `cityId`, `languageCode`, `lat`, `lng`, `radiusKm`, `type` and `scope=LOCAL|COUNTRY|AFRICA`. In COUNTRY/LOCAL scope, when no explicit country is supplied, the JWT profile claim is used. Disabled-country private content is excluded; publicly configured heritage content may remain visible.

OpenSearch documents index original titles, translations, aliases and language plus geographic and launch fields. A physical distant event must not be presented as local: clients show its country and distance.

Feed and recommendations consume the same user country, optional current location, languages, follows, interests, viewed countries and travel mode. The feed mix is configuration-driven (`localCityWeight`, `countryWeight`, `africaDiscoveryWeight`, `followingWeight`, `cultureWeight`, `eventsWeight`, `artworkWeight`), not hard-coded. A Cameroon starting policy is 35% city, 35% country, 15% following and 15% Africa discovery. `FeedItemViewed` includes `viewerCountryCode`, `contentCountryCode` and `contentType`.

## Payments and commerce

`country_payment_providers` in country-config holds non-secret routing metadata: country, provider, method, currency, enabled flag, priority, amount limits and a configuration reference. Provider secrets remain in the provider secret store.

`PaymentProviderResolver` selects a provider from `(countryCode, currencyCode, paymentMethod, operationType)`. Supported methods are `MOBILE_MONEY`, `CARD`, `BANK_TRANSFER`, `WALLET`, `CASH_ON_DELIVERY` and `OTHER`. Cameroon only exposes providers actually integrated (for example MTN Mobile Money or Orange Money when their integration/configuration is enabled); it must not advertise simulated providers.

Webhooks are routed by provider and retain signature validation, rate limiting, idempotency and inbox processing. Commerce/payment/ticket records must snapshot country independently of their currency before the migration can be contracted.

## Partner KYC and administration

`country_kyc_requirements` selects an active rule by `(countryCode, partnerType, documentType)`, including whether it is required and its validity period. The administrable types are `IDENTITY_DOCUMENT`, `BUSINESS_REGISTRATION`, `TAX_CERTIFICATE`, `PROOF_OF_ADDRESS`, `ARTISAN_ASSOCIATION`, `WORKSHOP_PROOF`, `BANK_DETAILS`, `LICENSE` and `OTHER`.

`GET /api/v1/partners/onboarding/requirements?countryCode=CM&partnerType=ARTISAN` returns the effective onboarding requirement set. Administrative verification shows the configuration version, applied rules, received documents and missing documents.

Administrative access is role plus territorial scope, not a proliferation of country-specific roles. `admin_scopes` supports `GLOBAL`, `COUNTRY`, `ADMIN_AREA` and `CITY`. `SUPER_ADMIN` is global; every other administrative list, export and mutation must apply its scope in the backend. Audit records contain `actorScope` and `targetCountryCode`.

## Territorial analytics

Analytics events include, when applicable, `countryCode`, `adminLevel1Id`, `cityId`, `userCountryCode`, `contentCountryCode` and `currencyCode`. Daily aggregates are separated into `country_activity_daily`, `city_activity_daily`, `country_revenue_daily`, `culture_country_daily` and `artisan_country_daily`.

Country analytics routes require an authorized country scope:

- `GET /api/v1/analytics/countries`
- `GET /api/v1/analytics/countries/{countryCode}`
- `GET /api/v1/analytics/countries/{countryCode}/cities`
- `GET /api/v1/analytics/countries/{countryCode}/culture`
- `GET /api/v1/analytics/countries/{countryCode}/artisans`
- `GET /api/v1/analytics/countries/{countryCode}/revenue`

Revenue is returned per currency. Reporting conversion is permitted only when an explicit rate and reporting currency are supplied by a dedicated FX service.

## Safe rollout sequence

1. Deploy country configuration and nullable schema expansions.
2. Deploy double-read/double-write application releases and enriched event envelopes.
3. Run only evidence-backed backfills and record the batch evidence.
4. Measure fallback reads, data quality, consumer compatibility and index parity.
5. Enable country capabilities country by country.
6. Add constraints only after the readiness gates in the migration report pass.

The migration report is the release checklist and rollback authority. It intentionally keeps legacy fields until old clients, events and indexes have aged out.
