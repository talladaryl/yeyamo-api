# YeYamo V1 — Alignment and Deployment Certification

**Assessment date:** 2026-09-11  
**Scope:** backend, gateway, payment/cash-in, booking, ticketing, Passport, mobile Android and production configuration.  
**Verdict:** **NO-GO — not certified for production deployment or Google Play release.**

## 1. Executive summary

The V1 code path is substantially more aligned than the baseline: simulated payment is blocked in production, the selected provider configuration is explicit, paid booking and ticket flows carry the required cash-in fields, Passport is connected to real APIs, the WebSocket has a gateway route, production CORS defaults fail closed, and Android has a validated Expo configuration and export.

This is not a deployment certificate. No production environment, credentials, DNS/reverse proxy, real payment account, webhook delivery, or signed AAB was available to validate. In addition, the selected provider adapter deliberately returns failed results for cancellation and refund, the artwork-commerce payment producer does not supply the provider-required cash-in fields, production mobile dependencies contain 6 high and 21 moderate audit findings, and many non-V1 mobile paths still contain mock/demo data or no-op CTAs.

## 2. Scope, method and limits

Reviewed source, compose and configuration under the API repository and the nested `yeyamo-mobile` repository. No secret value was read, printed, created, or changed. No payment, webhook, deployment, Play Console, EAS cloud build, or production endpoint was invoked.

Evidence is code/configuration and local test evidence only. A variable absent from the local command process does **not** prove it is absent from Dokploy/EAS; it means deployed-secret presence was not available for inspection.

## 3. Global V1 flow matrix

| Capability | Backend/API and gateway | Mobile path | Cash-in fields / real state | Evidence | Status |
|---|---|---|---|---|---|
| Provider selection | `payment.provider.name` is explicit; prod guard rejects empty/simulated | N/A | No production fallback | payment tests | PASS (code) |
| Cash-in initiation | HR-Skills request sends operator, country, phone, amount, currency, idempotency key | Ticket checkout collects operator/phone; active country supplies dialing code | Real provider credentials/webhook unproved | unit/integration tests only | BLOCKED |
| Cash-in webhook | Gateway public only for payment webhook paths; signature secret is canonicalized | N/A | Signature/timeout/replay proof requires provider sandbox | source/tests | BLOCKED |
| Cancellation | Command and state path exist | N/A | HR-Skills provider returns `FAILED` | adapter source | NO-GO |
| Refund | Admin/request state path exists | N/A | HR-Skills provider returns `FAILED` | adapter source | NO-GO |
| Paid booking | `/api/v1/bookings/**` routed | API-only activity hooks | backend persists operator/phone/country and emits them | 40 booking tests | PASS (code) |
| Ticket order | `/api/v1/tickets/**` routed | hold → order → status polling | backend validates E.164, operator and JWT country claim | 17 ticket tests | PASS (code) |
| Ticket confirmation | payment event consumer issues/updates tickets | confirmation only after server order status | no optimistic ticket issuance | ticket tests / mobile source | PASS (code) |
| Ticket QR/scan | ticket routes via gateway | owned ticket + QR + scan screens | server credential only; status gates QR | ticket tests | PASS (code) |
| Legacy event participation | Legacy deep link redirects to Ticket V1 checkout/list | no local “participation validated” success state remains | server remains authority | TypeScript/lint | PASS |
| Passport summary/badges | gamification routes via gateway | `/me/passport/summary`, `/me/badges` | no local Passport mock data | 36 gamification tests | PASS (code) |
| Passport missions/rewards/history | mission/gamification routes via gateway | real missions/rewards/xp/history/leaderboard queries | claim uses backend mutation | 17 mission tests | PASS (code) |
| WebSocket messaging | `lb:ws://messaging-service` route at `/ws/messaging` | `wss://api.yeyamo.com/ws/messaging` required in prod | STOMP CONNECT owns JWT authentication | gateway test/compose | PASS (code), live test pending |
| Gateway public surface | all business routes gatewayed | public API base required is `https://api.yeyamo.com` | docs now role-restricted and disabled in prod compose | 14 gateway tests | PASS (code) |
| Android native maps | EAS config injects native keys at build time | Android production config is explicit | keys must exist in EAS secrets | Expo Doctor/export | BLOCKED |
| Artwork commerce payment | commerce routes exist | artwork creation only; no aligned payment checkout | producer contract lacks operator/country/phone | source review | NO-GO |
| Global mock/no-op elimination | N/A | non-V1 areas still contain mock/demo files and no-op CTAs | 132 mock-bearing source files found | source audit | NO-GO for whole-app claim |

`PASS (code)` means the local implementation and tests support the flow; it is not proof that the external provider or production system has executed it.

## 4. Payment production selection

Changes made:

- `PAYMENT_PROVIDER_NAME` has no silent `simulated` fallback.
- `SimulatedPaymentProvider` is restricted to `local`, `dev`, and `test` profiles.
- `ProductionPaymentProviderGuard` fails startup on a missing provider, simulated provider, or missing matching external provider.
- `HrSkillsPayProperties` no longer has a real-provider URL fallback.
- `docker-compose.production.yml` makes the provider name, base URL, keys and webhook secret required.

The canonical payment contract is now:

| Variable | Consumed as | Production compose | Example files | Local process observed |
|---|---|---|---|---|
| `PAYMENT_PROVIDER_NAME` | `payment.provider.name` | required | `hr-skills-pay` | absent |
| `PAYMENT_AGGREGATOR_BASE_URL` | provider base URL | required | non-secret URL | absent |
| `PAYMENT_AGGREGATOR_KEY_A` | provider credential | required | blank | absent |
| `PAYMENT_AGGREGATOR_KEY_B` | provider credential | required | blank | absent |
| `PAYMENT_AGGREGATOR_WEBHOOK_SECRET` | generic webhook verifier and provider HMAC | required | blank | absent |

The local process also had no `JWT_SECRET`, `INTERNAL_SERVICE_TOKEN`, `CORS_ALLOWED_ORIGINS`, Android API base/WS URL, or Android Maps key. This is an evidence gap, not a claim about the deployed secret store.

## 5. Cash-in, webhook, cancellation and refund

`HrSkillsPayService` explicitly refuses a producer message missing `operator`, `country`, or `phone_number`/`phoneNumber`; the provider request carries all three fields plus amount, currency and an idempotency key. This is fail-closed and prevents an accidental generic payment request.

Paid booking and ticket producers supply those values. The ticket controller derives the country from the JWT country claim, validates E.164 phone numbers and allows the backend operator set. The mobile form now gets the active country dialing code from country configuration and disables submission when that configuration is absent; it no longer hardcodes `+237`.

The following is not production-complete:

- `HrSkillsPayProvider.cancel(...)` returns `FAILED` because no provider cancellation contract is implemented.
- `HrSkillsPayProvider.refund(...)` returns `FAILED` because no provider refund contract is implemented.
- `ArtworkCommerceService.OrderCommand` and its controller do not accept or emit operator/country/phone, so commerce cannot meet the provider cash-in contract. It must remain unavailable for paid checkout until that producer, UI and tests are added.
- Provider sandbox evidence is missing for success, failure, timeout, callback signature, replay and idempotency behavior.

## 6. Paid booking alignment

Booking migrations persist `payment_operator`, `payment_phone_number` and `payment_country_code`. The application service validates the operator, E.164 phone number and account country for paid slots, includes the fields in `payment.authorization.requested`, and waits for payment events to move the booking state.

Mobile `usePlaceActivities` no longer returns demo booking/availability data. The backend result remains the source of truth. The `BookingPaymentCommandKafkaTest` was stabilized to explicitly assign and seek the embedded Kafka partition; it now passes reliably.

## 7. Ticketing V1 alignment

The Ticket V1 checkout uses the real sequence:

```text
available ticket types → server hold → server order/cash-in request
→ poll server order status → payment event → issued ticket / QR credential
```

The displayed final amount is the order amount returned by the server. Failure, expiration and poll timeout remain non-success states. The previous legacy event-participation route is now a redirect to ticket listing/checkout, and the event CTA points directly to the V1 checkout.

Tickets, QR credentials, scans, holds, orders and partner ticket administration use existing gateway ticket routes. The local tests cover inventory/order, payment-event handling, QR token security and REST behavior; a real handheld scan against deployed infrastructure remains required.

## 8. Passport V1 alignment

Passport UI is now API driven. It calls:

- `/me/passport/summary`, `/me/badges`, `/me/passport`
- `/me/missions`, `/me/rewards`, `/me/rewards/{id}/claim`
- `/me/xp/history?size=20`, `/me/leaderboard?limit=50`

The Passport page presents only backend-confirmed content and handles loading/error/empty states. The unused `passport.mockData.ts`, `passport.badges.ts` and social-graph mock dataset were removed. No V1 Passport mock reference remains.

## 9. Gateway, routing and real-time messaging

The API gateway now has a dedicated WebSocket route:

```properties
Path=/ws/messaging → lb:ws://messaging-service
```

The HTTP GET upgrade is permitted so the socket can be established; the messaging service must authenticate the STOMP CONNECT frame. This must be proven with an actual authenticated device session in staging/production.

Production CORS defaults no longer include localhost, `127.0.0.1`, emulator or LAN wildcards in global, auth, gateway or messaging config. The production compose file requires `CORS_ALLOWED_ORIGINS` and keeps only the API gateway exposed to the Dokploy network; all other compose service ports are reset. Reverse-proxy/DNS/TLS policy still needs deployment evidence.

Gateway Swagger/OpenAPI endpoints are role restricted in the security chain and disabled in production compose through `SPRINGDOC_API_DOCS_ENABLED=false` and `SPRINGDOC_SWAGGER_UI_ENABLED=false`.

## 10. Production configuration and environment hygiene

`.env.example` and `.env.production.example` now use the canonical payment variable names and contain no secret values. The production compose configuration parses successfully with `docker compose ... config --no-interpolate`.

`docker-compose.production.yml` is a deployment definition, not evidence that Dokploy actually supplies its required variables. Before a release, compare the rendered Dokploy environment by **presence only** against:

- payment provider variables above;
- `JWT_SECRET`, `INTERNAL_SERVICE_TOKEN`, `CORS_ALLOWED_ORIGINS`;
- database, Kafka, Redis, config-server and Eureka credentials;
- ticket QR keys if QR signing is enabled;
- media, mail, Maps, Turnstile and push credentials where their features are enabled.

No localhost fallback is present in the production mobile API/WS contract. Localhost in `src/config/env.ts` is development-only and is rejected for production URLs.

## 11. Android and Play Store readiness

Implemented/validated:

- Android package is `com.yeyamo.mobile`; app scheme, icons, splash and required runtime permissions are present.
- Storage permissions were removed; camera, microphone and location declarations remain feature-aligned.
- `app.config.ts` injects Google Maps Android/iOS native keys at build time without committing a key.
- `eas.json` production requires `https://api.yeyamo.com` and `wss://api.yeyamo.com/ws/messaging`.
- Added `.env.production.example` listing public build-time variables without values.
- `npx expo-doctor`: 21/21 checks passed.
- `npx expo export --platform android`: passed, bundle created successfully.

Not completed or not evidenced:

- No `eas build --platform android --profile production` was run, so there is no signed AAB, signing proof, install proof or Play Console upload.
- EAS secret presence for Maps, OAuth, Turnstile and production URLs was not inspectable.
- `npm audit --omit=dev --json` reports **6 high, 21 moderate, 0 critical** vulnerabilities in the production dependency graph. Notably, vulnerable transitive packages include `js-yaml`, `postcss` and `shell-quote`; remediation needs a compatibility-tested dependency upgrade rather than `npm audit fix --force`.

## 12. Mocks, demo logic and dead CTAs

V1 Passport, ticketing and place-activity flows were converted to real API behavior, and their legacy mock sources were removed. The V1 ticket success path no longer locally declares participation or payment success.

The application as a whole is not mock-free. A source search found **130** mobile files containing mock/demo/fake/fixture/placeholder references. The report does not treat those matches as automatically production-reachable; they require route-by-route review. Known no-op/disabled user interactions include feed comment/share buttons, partner-dashboard reservation/event/establishment cards, and an intentionally disabled Apple login control. These must either be implemented, removed, or explicitly feature-gated before claiming full-app production readiness.

## 13. Test and build evidence

| Command / module | Result |
|---|---|
| `mvn compile -DskipTests` | PASS — 41/41 reactor modules, 1m46s |
| booking-service tests | PASS — 40 tests, 0 failures/errors |
| payment-service tests | PASS — 35 tests, 0 failures/errors |
| ticket-service tests | PASS — 17 tests, 0 failures/errors |
| api-gateway tests | PASS — 14 tests, 0 failures/errors |
| gamification-service tests | PASS — 36 tests, 0 failures/errors |
| mission-reward-service tests | PASS — 17 tests, 0 failures/errors |
| `npx tsc --noEmit` | PASS |
| `npm run lint` | PASS — 0 errors, 62 pre-existing/non-blocking warnings |
| `npx expo-doctor` | PASS — 21/21 checks |
| `npx expo export --platform android` | PASS |
| production dependency audit | FAIL for release gate — 6 high, 21 moderate |
| production compose parse | PASS — syntax/render contract only |

No test result substitutes for sandbox payment, webhook, real database migration, real WebSocket, reverse-proxy or Android device evidence.

## 14. External validation required

The following requires the payment provider, Dokploy, EAS or Play Console and was not fabricated:

1. Provider sandbox credentials and supported operator/country mapping.
2. Cash-in success, rejection, timeout, duplicate idempotency key and delayed callback tests.
3. Signed webhook success, bad-signature rejection, expired timestamp and replay rejection.
4. Documented cancellation/refund endpoint implementation and sandbox proof.
5. Rendered Dokploy environment presence, migrations, health checks, TLS/DNS and public-port scan.
6. Authenticated `wss://api.yeyamo.com/ws/messaging` handshake and STOMP authorization test.
7. EAS production AAB build, physical Android smoke test, Maps/OAuth/notifications tests and Play Console pre-launch report.

## 15. Risk register and remediation order

| Priority | Finding | Required outcome |
|---|---|---|
| P0 | Production payment refund/cancel are explicitly unsupported | Implement provider-specific contract, idempotency and sandbox tests; do not market refunds before this |
| P0 | Artwork-commerce payment command lacks cash-in fields | Add operator/country/phone to API/domain/event/UI and contract tests, or hard-disable paid commerce |
| P0 | No external provider/webhook/Dokploy evidence | Complete the external validation sequence above |
| P0 | Android production graph has 6 high audit findings | Upgrade/pin compatible dependencies, regenerate lockfile, re-run Doctor/audit/export/AAB tests |
| P1 | Whole app still carries demo/mock/no-op surfaces | Route-by-route remove, implement or feature-gate; add production-mode tests |
| P1 | Mobile operator picker exposes only MTN/Orange while backend supports a wider set | Drive supported operators from country/provider configuration before multi-country launch |
| P1 | No real WSS device test | Run authenticated, reconnect and authorization tests behind the live proxy |
| P2 | 62 lint warnings | Clean warnings and enforce zero-warning policy gradually |
| P2 | Production docs configuration depends on compose variables | Add a production startup/integration test asserting docs are disabled/restricted |

## 16. Final certification and operator checklist

### Backend deployment

**NO-GO.** Source-level protections and tests are good enough to proceed to a controlled staging validation, but P0 payment/refund/commerce gaps and absent deployment/provider evidence prevent a production certification.

### Android / Google Play

**NO-GO.** The Expo config and Android export are valid, but there is no signed AAB or Play evidence and the production dependency audit contains high-severity findings.

### Immediate checklist before a new review

1. Supply provider sandbox credentials outside source control and execute the complete cash-in/webhook/refund test matrix.
2. Either finish cash-in propagation for artwork commerce or keep paid commerce disabled end-to-end.
3. Remediate the dependency audit with tested compatible package versions.
4. Render Dokploy production configuration, deploy staging, run migrations and test TLS, CORS, WSS and public exposure.
5. Build the EAS production AAB, test on a physical Android device and attach Play pre-launch results.
6. Attach the resulting logs/screenshots (with secrets redacted) to a fresh certification review.

**Forced GO was not used.**
