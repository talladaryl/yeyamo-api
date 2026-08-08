# Culture graph and social integration

## Graph ownership

`graph-service` is a disposable Neo4j projection. PostgreSQL services remain authoritative. It consumes versioned Kafka envelopes and records every `eventId` as a unique `ProcessedEvent` node before acknowledging processing. Domain nodes expose identifiers and non-sensitive indexing attributes only.

The admin rebuild endpoint deletes projection nodes asynchronously, preserves the rebuild job, and publishes `CultureGraphRebuildRequested`. Source services must replay their published projections; graph-service does not scrape transactional databases.

## Social references

Posts retain their social body and now store only `referenceType` plus `referenceId`. `catalogAssetId` remains for backward compatibility and old rows are migrated to `PLACE`. New references are checked against their owning service's public endpoint before persistence.

Generic interactions use an additive table and endpoint family under `/api/v1/interactions/{targetType}/{targetId}`. Existing post-centric endpoints and tables remain unchanged.

Feed projections carry the structured reference and expose stable card types: `SOCIAL`, `ARTWORK`, `CULTURE_CONTENT`, `CULTURE_CHALLENGE`, and `ARTISAN_SPOTLIGHT`. Mix weights are environment/config-server properties, not hardcoded product policy.

## Remaining integration dependency

Country/language-aware ranking requires a privacy-reviewed user preference projection containing country, administrative area, city and preferred language codes. The current feed contract does not expose that projection, so no location or language value is guessed. Blocked-user filtering continues to require the existing social graph/block projection to be made available to feed-service.
