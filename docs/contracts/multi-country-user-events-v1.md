# Multi-Country User Events Contract v1

This document defines the event schema for multi-country user management events.

## Event Types

### 1. profile.location_updated

Emitted when a user updates their geographic location (country, administrative areas, city, timezone).

**Topic**: `user-events`

**Schema**:
```json
{
  "eventId": "uuid",
  "eventType": "profile.location_updated",
  "eventVersion": 1,
  "occurredAt": "2026-08-13T10:30:00Z",
  "producer": "user-service",
  "correlationId": "uuid",
  "actorId": "userId",
  "payload": {
    "profileId": "uuid",
    "authUserId": "userId",
    "countryCode": "CM",
    "adminLevel1Id": "uuid",
    "adminLevel2Id": "uuid",
    "cityId": "uuid",
    "localityId": "uuid",
    "timezone": "Africa/Douala"
  }
}
```

**Field Descriptions**:
- `countryCode`: ISO 3166-1 alpha-2 country code
- `adminLevel1Id`: First-level administrative area (e.g., Region in Cameroon)
- `adminLevel2Id`: Second-level administrative area (e.g., Department)
- `cityId`: City UUID from country-config-service
- `localityId`: Locality UUID (neighborhood, quarter)
- `timezone`: IANA timezone identifier

---

### 2. profile.language_updated

Emitted when a user updates their language preferences.

**Topic**: `user-events`

**Schema**:
```json
{
  "eventId": "uuid",
  "eventType": "profile.language_updated",
  "eventVersion": 1,
  "occurredAt": "2026-08-13T10:30:00Z",
  "producer": "user-service",
  "correlationId": "uuid",
  "actorId": "userId",
  "payload": {
    "profileId": "uuid",
    "authUserId": "userId",
    "preferredLanguageCode": "fr",
    "contentLanguages": ["fr", "en", "bam"]
  }
}
```

**Field Descriptions**:
- `preferredLanguageCode`: User's primary language (ISO 639-1 or BCP 47)
- `contentLanguages`: Languages the user wants to see content in

---

### 3. profile.discovery_preferences_updated

Emitted when a user updates their content discovery preferences.

**Topic**: `user-events`

**Schema**:
```json
{
  "eventId": "uuid",
  "eventType": "profile.discovery_preferences_updated",
  "eventVersion": 1,
  "occurredAt": "2026-08-13T10:30:00Z",
  "producer": "user-service",
  "correlationId": "uuid",
  "actorId": "userId",
  "payload": {
    "profileId": "uuid",
    "authUserId": "userId",
    "contentCountries": ["CM", "SN", "CI"],
    "localRadiusKm": 50,
    "discoverAfricanContent": true,
    "preferredCurrencyCode": "XAF"
  }
}
```

**Field Descriptions**:
- `contentCountries`: Countries the user wants to discover content from
- `localRadiusKm`: Radius in km for local content discovery (1-500)
- `discoverAfricanContent`: Whether to show African content from all countries
- `preferredCurrencyCode`: ISO 4217 currency code for pricing display

---

### 4. user.country_selected (from auth-service)

Emitted during registration when a user selects their country.

**Topic**: `user-events`

**Schema**:
```json
{
  "eventId": "uuid",
  "eventType": "user.created",
  "eventVersion": 1,
  "occurredAt": "2026-08-13T10:30:00Z",
  "producer": "auth-service",
  "correlationId": "uuid",
  "actorId": "userId",
  "payload": {
    "userId": "42",
    "email": "user@example.com",
    "phone": "+237699123456",
    "status": "PENDING",
    "countryCode": "CM",
    "cityId": "uuid",
    "preferredLanguageCode": "fr",
    "timezone": "Africa/Douala"
  }
}
```

**Field Descriptions**:
- `countryCode`: Country selected during registration
- `cityId`: Optional city selected during registration
- `preferredLanguageCode`: Language preference (defaults to country default)
- `timezone`: Timezone (defaults to country default)

---

## Consumer Guidelines

### 1. Recommendation Service
- Consume `profile.location_updated` to adjust location-based recommendations
- Consume `profile.language_updated` to filter content by language
- Consume `profile.discovery_preferences_updated` to personalize content discovery

### 2. Analytics Service
- Track country distribution via `user.created` events
- Track language preferences for content analytics
- Track discovery preferences for engagement metrics

### 3. Notification Service
- Use `preferredLanguageCode` for notification language
- Use `timezone` for optimal notification timing

### 4. Discovery Service
- Filter content by `contentCountries` and `localRadiusKm`
- Apply African content filter based on `discoverAfricanContent`

---

## Event Evolution

When updating event schemas:
1. Increment `eventVersion` in the event
2. Document changes in this file
3. Maintain backward compatibility for existing consumers
4. Coordinate with consuming services before breaking changes

---

## Validation Rules

### Country Code
- Must be ISO 3166-1 alpha-2 (2 uppercase letters)
- Must exist in country-config-service
- Must have `registrationEnabled = true` for new registrations

### Language Code
- Must be ISO 639-1 (2 letters) or BCP 47 (e.g., "fr-CM")
- Maximum 10 characters

### Timezone
- Must be valid IANA timezone identifier
- Maximum 50 characters

### Currency Code
- Must be ISO 4217 (3 uppercase letters)

### Local Radius
- Must be between 1 and 500 km
