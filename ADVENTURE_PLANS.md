# Adventure Plans — Backend Explorer

## Décision d'architecture

`AdventurePlan` est stocké dans PostgreSQL et appartient à `recommendation-service`.

Ce choix étend un service qui possède déjà les projections de candidats, le classement personnalisé, les signaux utilisateur et les migrations Flyway. Il évite un nouveau microservice et, surtout, évite de faire des appels HTTP par candidat ou des lectures de base de données d'un autre service.

Pipeline réel :

```text
Mobile (JWT)
  -> AdventurePlanController
  -> AdventurePlanService.normalize
  -> CountryConfigClient + RecommendationProjectionPort.activeCandidates(200)
  -> filtres stricts (pays, date/heure Event, budget, activité)
  -> RecommendationQueryService.rankForAdventure
  -> allocation déterministe par jour
  -> preview non persistant / persistance atomique du snapshot
```

## Routes

| Method | Route | Auth | Fonction |
|---|---|---|---|
| POST | `/api/v1/explore/adventure-plans/preview` | JWT obligatoire | Calcule un planning non persisté pour le profil connecté. |
| POST | `/api/v1/explore/adventure-plans` | JWT obligatoire | Recalcule et sauvegarde le planning côté serveur. |
| GET | `/api/v1/explore/adventure-plans?page=&size=` | JWT obligatoire | Liste paginée des plans du propriétaire. Taille plafonnée à 50. |
| GET | `/api/v1/explore/adventure-plans/{planId}` | JWT obligatoire | Retourne le détail seulement si le JWT est propriétaire. |
| DELETE | `/api/v1/explore/adventure-plans/{planId}` | JWT obligatoire | Supprime le plan propriétaire et renvoie `204`. |
| POST | `/api/v1/explore/adventure-plans/{planId}/recommendations/{recommendationId}/skip` | JWT obligatoire | Marque la proposition comme ignorée et cherche un remplacement compatible. |

Le gateway route `/api/v1/explore/**` vers `recommendation-service`. Une ressource d'un autre utilisateur répond comme inexistante (`404 ADVENTURE_PLAN_NOT_FOUND`) : elle ne révèle pas son propriétaire.

`preview` est volontairement authentifié : le classement réutilise les préférences et signaux du profil JWT.

## Requête réellement acceptée

```json
{
  "countryCode": "CM",
  "startDate": "2026-10-05",
  "endDate": "2026-10-20",
  "startTime": "09:00",
  "endTime": "17:00",
  "partyType": "SOLO",
  "interestCodes": ["culture", "events"],
  "budget": {
    "tier": "STANDARD",
    "minimumAmount": 5000,
    "maximumAmount": 20000,
    "currencyCode": "XAF"
  }
}
```

Valeurs `partyType` : `SOLO`, `FAMILY`, `FRIENDS`, `COUPLE`.
Valeurs `budget.tier` : `STANDARD`, `MEDIUM`, `PREMIUM`, `CUSTOM`.

Les dates sont inclusives : `2026-10-05` à `2026-10-05` crée un seul jour ; `05` à `07` crée `05`, `06`, `07`. Une demande est limitée à 31 jours. Si les heures sont présentes, elles doivent être présentes toutes les deux et `endTime` doit être strictement après `startTime`.

Le pays est vérifié auprès de `country-config-service` et doit être `LIVE` ou `BETA`. La devise est contrôlée contre `GET /api/v1/countries/{code}/currencies`, avec cache court dans `CountryConfigClient`. Aucun feature flag Explorer n'existe dans le contrat pays actuel : aucun nouveau flag n'a été inventé.

## Réponse preview réelle

`POST /preview` ne crée aucune ligne `adventure_plans`. Sa réponse est de la forme exacte suivante ; les valeurs des propositions dépendent exclusivement des projections réelles présentes :

```json
{
  "normalizedCriteria": {
    "countryCode": "CM",
    "startDate": "2026-10-05",
    "endDate": "2026-10-05",
    "startTime": "09:00",
    "endTime": "17:00",
    "partyType": "SOLO",
    "interestCodes": ["culture"],
    "budgetTier": "STANDARD",
    "minimumAmount": 5000,
    "maximumAmount": 20000,
    "currencyCode": "XAF"
  },
  "days": [
    {
      "date": "2026-10-05",
      "position": 0,
      "items": [
        {
          "recommendationId": "UUID",
          "targetType": "PLACE",
          "targetId": "real-target-id",
          "scheduledAt": null,
          "reasonCodes": ["COUNTRY_MATCH", "INTEREST_MATCH"],
          "title": "real projected title",
          "imageMediaId": null,
          "locationLabel": null,
          "startsAt": null,
          "endsAt": null,
          "price": null,
          "currencyCode": null,
          "availabilityStatus": "UNKNOWN"
        }
      ]
    }
  ],
  "warnings": [],
  "relaxations": [],
  "noResultsReason": null
}
```

Un résultat vide reste un `200`, avec tous les jours présents, `items: []` et `noResultsReason: "NO_COMPATIBLE_CANDIDATES"`. Quand un budget chiffré est demandé, un prix inconnu est exclu et `warnings` contient `UNKNOWN_PRICE_EXCLUDED_BY_STRICT_BUDGET`; un prix inconnu n'est jamais assimilé à zéro.

## Réponse persistée réelle

`POST /adventure-plans` accepte uniquement les critères précédents, recalculés côté serveur (stratégie A). Le mobile ne peut donc pas imposer un `targetId`, prix, titre ou statut. Il retourne, comme `GET /{planId}` :

```json
{
  "id": "UUID",
  "criteria": {
    "countryCode": "CM",
    "startDate": "2026-10-05",
    "endDate": "2026-10-05",
    "startTime": "09:00",
    "endTime": "17:00",
    "partyType": "SOLO",
    "interestCodes": ["culture"],
    "budgetTier": "STANDARD",
    "minimumAmount": 5000,
    "maximumAmount": 20000,
    "currencyCode": "XAF"
  },
  "days": [
    {
      "date": "2026-10-05",
      "position": 0,
      "items": []
    }
  ],
  "createdAt": "2026-10-01T10:00:00Z",
  "updatedAt": "2026-10-01T10:00:00Z"
}
```

Le snapshot persisté contient aussi `sourceId` (interne), `targetType`, `targetId`, titre, image, lieu, date, prix, devise, raisons et disponibilité. Le détail n'exige aucun appel mobile supplémentaire pour afficher un jour.

## Sources, filtres et disponibilité

| Target type | Source réelle | Filtrage avant ranking | Disponibilité | Prix |
|---|---|---|---|---|
| `PLACE` | projection des `catalog.asset.*` de `catalog-service` | actif, pays exact, budget strict éventuel | `UNKNOWN` | prix/devise Catalog si publiés, sinon `null` |
| `ACTIVITY` | `CatalogAsset` de type `EXPERIENCE` | mêmes filtres | `UNKNOWN` | prix Catalog si présent |
| `CULTURE_CONTENT` | projections Catalog de type `CULTURE`, `LANGUAGE`, `TRADITION` | mêmes filtres | `UNKNOWN` | `null` ou prix réellement projeté |
| `EVENT` | projection compacte de `event.events` | `PUBLISHED`, `PUBLIC`, pays, date et heure exactes | `CONFIRMED` tant que publié, `CANCELLED`/`UNAVAILABLE` après événement | aucun prix inventé (`null`) |

`RecommendationEventConsumer` consomme `event.published`, `event.updated`, `event.cancelled` et `event.completed` sous le topic `event.events`, avec `recommendation_processed_events` pour l'idempotence. La projection contient seulement l'identifiant, titre, pays/langue, coordonnées, couverture, lieu et créneaux. Les événements annulés ou terminés deviennent inactifs pour les nouveaux previews. Un événement annulé/terminé met à jour en masse les items Event sauvegardés, sans relire Event au GET.

L'outbox Catalog publie désormais, de façon additive, ville/adresse, média, prix/devise et durée afin que les candidats puissent fournir un résumé sans N+1.

## Ranking, allocation et contraintes

Les hard filters exécutés avant le score sont : pays exact, candidat actif, type réellement supporté, exclusion du skip, créneau Event inclus dans les dates et heures demandées, budget strict (devise identique et prix connu dans l'intervalle).

Le score réutilise les stratégies existantes de `RecommendationQueryService` (popularité, proximité lorsqu'elle est disponible, préférences et historique) puis ajoute `adventure_interest` uniquement lorsque `categoryCode` correspond à un code d'intérêt normalisé. `partyType` est stocké mais ne modifie pas encore le score : aucune métadonnée fiable de capacité/famille/couple n'est projetée. Le budget est un filtre, non un bonus de score.

L'allocation est déterministe, sans optimisation de trajet : les Events restent sur leur date réelle, puis les autres candidats sont distribués en round-robin. Limites : 200 candidats lus, 31 jours, 3 items par jour. Aucune durée de trajet ni horaires d'ouverture ne sont inventés. Si une plage horaire est demandée avec un contenu non Event, la réponse signale `OPENING_HOURS_UNKNOWN`.

Verdict appels inter-services : **NO_N_PLUS_ONE**. Un preview lit une projection locale en batch et utilise le profil déjà local. La validation pays/devise est au plus un appel pays et un appel devises, avec cache court, jamais un appel par cible. `booking-service` n'est pas utilisé car il n'offre pas de projection batch de disponibilité/prix ; aucune réservation, ticket, paiement, XP, Feed, partage ou notification n'est créé.

## Skip

Un item ignoré reçoit `skipped_at`, et le plan conserve `replaced_by_recommendation_id`. Le remplacement est recalculé côté serveur avec exclusion des `sourceId` et des couples `targetType:targetId` déjà présents — y compris la cible ignorée. S'il n'existe aucun candidat compatible, la réponse est :

```json
{
  "skippedRecommendationId": "UUID",
  "replacement": null,
  "reasonCode": "NO_COMPATIBLE_REPLACEMENT"
}
```

Le skip est limité au plan courant ; il ne devient pas un signal global Feed.

## Base de données et migrations

| Migration | Contenu |
|---|---|
| `V4__extend_candidates_for_adventure_plans.sql` | prix, devise, image, lieu, horaires sur `recommendation_candidates` et index Event/budget. |
| `V5__create_adventure_plans.sql` | tables `adventure_plans`, `adventure_plan_interests`, `adventure_plan_days`, `adventure_plan_items`, FK, checks et index ownership/plan/date/target. |
| `V6__add_adventure_plan_timezone.sql` | timezone pays persistée pour conserver la sémantique locale lors d'un skip ultérieur. |

La création est transactionnelle : plan, jours et snapshots sont sauvegardés ensemble. `adventure_plans.user_id` indexé et toutes les queries de lecture/modification incluent `userId`.

## Tests exécutés

| Commande | Module | Résultat |
|---|---|---|
| `mvn -pl shared-lib,event-service,catalog-service,recommendation-service -am test -DskipTests` | dépendances touchées | PASS — compilation complète. |
| `mvn -pl recommendation-service "-Dtest=AdventurePlanServiceTest,RecommendationEventConsumerTest" test` | recommendation-service | PASS — 12 tests. |
| `mvn -pl recommendation-service "-Dtest=RecommendationServiceApplicationTests" test` | recommendation-service | PASS — contexte Spring démarre avec CountryConfigClient et RestClient. |
| `mvn -pl catalog-service "-Dtest=JpaCatalogOutboxAdapterTests" test` | catalog-service | PASS — 1 test du producteur Catalog. |
| `mvn -pl event-service test` | event-service | PASS — 20 tests, dont le contrat Event publié et le démarrage du service. |
| `mvn -pl recommendation-service test` | recommendation-service | TIMEOUT à 60 s — toutes les suites unitaires (dont Adventure, RecommendationQuery, scoring, domaine, consumer et outbox) ont passé avant le test de contexte ; ce contexte a été exécuté séparément avec succès ci-dessus. |
| `mvn -pl recommendation-service "-Dtest=RecommendationServiceApplicationTests" "-Dspring.flyway.enabled=true" test` | recommendation-service | NON PASS — échec antérieur dans `V1__recommendation_schema.sql`: H2 ne reconnaît pas le type PostgreSQL `TIMESTAMPTZ`. Les migrations de test sont normalement désactivées et ce n'est pas introduit par V4–V6. |

Les tests Explorer couvrent : même jour, trois jours inclusifs, Event hors plage/inactif exclu, budget avec prix inconnu exclu, dates/heures invalides, ownership, skip/remplacement et replay Kafka Event.

## Limites explicites

- Les horaires d'ouverture des Places/activités ne sont pas projetés ; ils restent `UNKNOWN`.
- `booking-service` n'expose pas une projection batch de créneaux/prix : disponibilité de réservation non intégrée.
- Les Events n'ont pas de prix dans leur contrat réel actuel.
- `partyType` est conservé mais sans effet de ranking faute de métadonnées fiables.
- Aucun temps de trajet, itinéraire ou optimisation géographique n'est calculé.
- Aucun relâchement silencieux : `relaxations` est aujourd'hui vide ; un résultat vide est explicite.
