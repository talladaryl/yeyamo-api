# Africa-ready data migration report

**Audit date:** 13 August 2026  
**Scope:** static audit of the Flyway migrations and application mappings in this repository.  
**Important:** no production database was accessed. Counts in this document are therefore explicitly marked *pending execution*; they must be produced in each service database before any `NOT NULL` constraint is considered.

## Decision

The backend can be migrated without destructive changes by using an **expand, observe, backfill, contract** rollout. `country_code` remains nullable during the rollout. Cameroon (`CM`) is inferred only where an immutable source proves it; the current repository contains one such rule: an E.164 telephone number beginning with `+237`.

No migration in this programme may use “the application used to be Cameroon-only” as proof that a row is Cameroonian. Rows without evidence stay `NULL`, are auditable, and are corrected through an operational review.

## Inventory

| Domain / relation | Legacy or implicit territorial data | Current Africa-ready state | Safe backfill source | Status |
|---|---|---|---|---|
| auth / `users` | `phone`, profile `language` | nullable `country_code`, `city_id`, `preferred_language_code`, `timezone` | E.164 `+237…` only | Implemented; audit table present |
| user / `user_profiles` | `preferred_region_id`, `language` | country, admin areas, city, locality, language, timezone, currency and discovery tables | verified user choice or verified auth country | Awaiting data review |
| country config / countries, languages and administrative areas | country, language and city reference data | canonical reference model | not a backfill target; validates other services | Authority for the programme |
| place / `regions`, `cities`, `districts`, `places` | `region_id`, `city_id`, `district_id`, phone | regions plus nullable city/district/place country snapshots (`V4`) | verified parent region after provenance check | Expand ready; provenance review pending |
| content / `content_posts`, `stories` | geographic fields were implicit | nullable country, admin areas, city, locality, coordinates, language | verified source/place/event reference | Audit present; no automatic CM fill |
| event / `events` | legacy `place_id` | nullable country, admin areas, `country_city_id`, locality, coordinates, language | verified place or event owner declaration | Audit present; no automatic CM fill |
| partner / `partners` | `contact_phone`, address-like profile data | `primary_country_code`, operating countries | verified registration/address | Audit present |
| booking / `activity_slots`, `bookings` | activity and order context implicit | nullable `country_code` (`V4`) | verified activity/place country snapshot | Audit present |
| catalog / `catalog_assets`, `collections` | language and collection targeting | assets have nullable country; collection targets are explicit | verified source asset/content | Audit present for assets |
| culture / `culture_contents`, translations, languages | country/language already first-class | `country_code` is already required for contents | not applicable to new rows | Ready; validate historical imports |
| feed / `feed_posts` | country/language projection absent historically | nullable country and language | verified originating content event | Audit present |
| recommendation / candidates and preferences | country/language previously implicit | nullable candidate country/language and preference country tables | verified source event or user profile | Audit present |
| analytics / `analytics_event_store` | country/language and monetary context | country, city, user/content country and currency dimensions | immutable event envelope | Audit present; aggregations are country/currency separated |
| admin / `admin_scopes`, `admin_audit_logs` | country/city access scope and audit target | country/city scopes and `target_country_code` | administrative assignment | Ready; enforce at every admin query |
| moderation / reviewer country and language scopes | country/language reviewer eligibility | explicit country and language scope relations | not a historical content backfill | Ready; validate consumer events |
| gamification and mission/reward | language appears in action/mission semantics, but no geographic business column | no country snapshot required unless a future reward becomes territorially scoped | source event only | No automatic backfill |
| commerce / orders, promotions, ledger, invoices | `currency`, partner/user references | nullable country snapshots added in `V6` | authoritative source order/partner snapshot | Expand ready; application double-write/backfill pending |
| payment / payments, refunds, webhook receipts | `currency`, provider transaction data | nullable payment/refund/webhook country snapshots added in `V3` | order country snapshot; never infer from currency | Expand ready; application double-write/backfill pending |
| ticket / sales, orders, tickets | `currency`, event reference | no country snapshot | authoritative event snapshot | Gap: expand migration required; duplicate baseline migration must be resolved first |
| campaign and ads delivery | `currency`, language targeting | nullable owner/viewer/conversion country snapshots added in `V2` | explicit campaign targeting only | Expand ready; projection/event propagation pending |

The scan also found `currency`, `language`, `phone` or `timezone` fields in the relations above. Amounts must continue to carry their own ISO-4217 code; a country must never be inferred from a currency. Language values must be validated as BCP 47 / registered platform language codes, and timezones as IANA identifiers.

## Evidence-based backfill policy

### Automatically eligible

`auth-service` migration `V5__backfill_verified_cameroon_users.sql` sets `users.country_code = 'CM'` only when `phone` matches `^\\+237[0-9]{6,12}$`. It records all remaining rows in `user_country_migration_audit` with `AMBIGUOUS_COUNTRY_NO_RELIABLE_E164_SIGNAL`.

For other services, a row may be filled only when a recorded, immutable relationship resolves to a country already verified in the country configuration service: for example, an event whose place has a verified country snapshot. The migration batch must retain the evidence relation and its identifier.

### Explicitly not eligible

Do not infer `CM` from a legacy `region_id`, a `city_id`, a currency (`XAF`), a French language, an `Africa/Douala` timezone, or the service's former launch market alone. These are review signals, not proof.

`place-service/V3__country_reference_foundation.sql` contains a historical unconditional `UPDATE regions SET country_code = 'CM'`. It is a data-quality risk under this stricter policy. Do not alter an already-applied Flyway migration; instead, export and validate those regions against their source records before treating them as authoritative parents or tightening their constraint.

## Migration procedure

1. **Preflight and backup.** Take a tested, point-in-time restorable backup of each service database. Record Flyway version, row count, and checksum. Run the accompanying `scripts/africa-ready-data-quality.sql` separately against every PostgreSQL service database.
2. **Expand.** Apply only additive Flyway migrations: nullable `country_code` columns, indexes, audit tables and new event fields. Do not rename/drop legacy `region_id`, `city_id`, `district_id`, `currency`, `language`, `phone` or `timezone` fields.
3. **Compatibility release.** Readers resolve `new countryCode -> verified legacy parent -> no country`. Every fallback is logged with aggregate type, aggregate id, source field and correlation id. Writers persist the new country field and keep legacy location references while old clients exist.
4. **Backfill in batches.** Write a batch identifier and evidence into an audit table before updating rows. Apply only the eligible rule above; leave ambiguity as `NULL`. Throttle and observe replication lag, error rates and Kafka consumer lag.
5. **Validate.** Re-run the quality script, reconcile counts with audit tables, sample records per country, replay representative Kafka events and reindex OpenSearch. Investigate all cross-country city/parent inconsistencies.
6. **Contract.** A per-table `NOT NULL` migration is permitted only after the readiness gates below are green for the agreed observation period. It must be an append-only Flyway migration, never an edit of a previously released file.

## Double-read and double-write contract

During the compatibility window, all services use this deterministic order:

1. validated `countryCode` on the aggregate or event;
2. verified country from an immutable, local parent snapshot;
3. no country (`NULL` / unresolved), with structured `africa_ready.country_fallback` telemetry.

Do not fall back to a broad default of `CM`. On writes, persist both `countryCode` and the legacy location identifier when the latter is still part of the public contract. Outbox messages must include `countryCode`, `occurredAt` and `correlationId`, plus `languageCode` where content is linguistic. Consumers must remain tolerant of older events that do not contain the new fields and must emit a fallback metric.

## Quality report to capture at execution time

The SQL script writes aggregate measurements to `africa_ready_migration_measurements`. The release record must include the following values per database and per relation:

| Measure | Initial result | Acceptance rule |
|---|---:|---|
| Rows scanned | Pending execution | equals source table count |
| Rows migrated with evidence | Pending execution | equals matching audit/evidence rows |
| Ambiguous rows | Pending execution | no silent default; operational queue created |
| Rejected rows | Pending execution | reason and source retained |
| Invalid country codes | Pending execution | 0 before constraint |
| Geographic inconsistencies | Pending execution | 0 unresolved for rows to be constrained |
| Missing/invalid currencies | Pending execution | 0 for monetary rows in scope |
| Invalid languages | Pending execution | 0 for rows in scope |
| Fallback reads | Pending execution | decreasing; 0 for the target table before contract |

## Constraint readiness gates

Do not add `NOT NULL`, foreign keys, or a country/city consistency check until all of these are true:

- the backfill batch is reconciled and its rollback snapshot exists;
- the quality report is attached to the release;
- fallback-read rate is zero for the table for at least one full business cycle;
- old client versions that omit `countryCode` are below the agreed retirement threshold;
- Kafka consumers have processed both old and enriched envelopes successfully;
- the relevant OpenSearch index has been rebuilt and count-checked;
- country availability and feature flag tests are green.

## Rollback

This plan is intentionally reversible during the expand/backfill phase:

- Disable the feature flag or writer first; legacy fields are still intact.
- Revert the application release to the double-read version. Do **not** drop new columns.
- Restore a specific backfill from its batch audit snapshot (`previous_country_code`, evidence and update timestamp). A generic `UPDATE ... SET country_code = NULL` is unsafe because it can erase a legitimate post-migration user edit.
- If a database rollback is required, restore the tested point-in-time backup and replay only idempotent, correlation-id keyed events.

Physical column drops and destructive Flyway “undo” scripts are out of scope until the contract phase has completed. Flyway migrations already deployed must not be edited.

## Required test evidence

| Scenario | Required verification |
|---|---|
| Historical record | unresolved records remain null and audited; proved `+237` users become CM |
| Rollback | batch snapshot restores only the batch's values |
| Old client | legacy location write/read remains valid and produces a fallback metric |
| New client | country, city, language and timezone survive round trip |
| Kafka | enriched and legacy envelopes are both consumed; correlation id preserved |
| OpenSearch | new country/language fields are indexed; disabled-country visibility is respected |
| Finance | amount and currency are preserved; no cross-currency aggregation/inference |
| Geography | city/locality belongs to supplied country; invalid links are rejected |

## Blocking observations

- Flyway version collisions still exist in `analytics-service` (`V2`), `gamification-service` (`V2`), `mission-reward-service` (`V2`) and `ticket-service` (`V1`). The country migrations introduced by this work were numbered after their respective existing versions; the remaining historical collisions must be made unambiguous before a clean-environment global release.
- The additive schema migrations for commerce, payment, campaign and ads delivery are prepared, but their writers, consumers and evidence-based backfills still need to be enabled before those domains can be declared Africa-ready.
- Runtime data counts cannot be claimed from a repository audit. The SQL measurement output is the authoritative completion record.

See [the Go Africa implementation guide](docs/GO_AFRICA_IMPLEMENTATION.md) for the runtime architecture and API contract.
