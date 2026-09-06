# Rapport Lot 02 — Contrats DTO et composition

Date : 4 septembre 2026  
Périmètre : `event-service`, `place-service`, `recommendation-service`, `ticketing-service` et `yeyamo-mobile` (Expo SDK 54). Docker n'a pas été lancé.

## Décision d'architecture

Les données ne sont pas complétées par des valeurs métier fictives. Lorsqu'une donnée n'appartient pas au DTO principal, le choix est explicite :

- **Event → Place sur le détail** : composition mobile ponctuelle avec `GET /places/{placeId}` ; pas de N+1 dans les listes Event.
- **Ticket → Event sur le détail** : composition mobile ponctuelle avec `GET /events/{eventId}`.
- **Événements liés à un lieu** : ajout d'une route backend dédiée `GET /api/v1/events?placeId={uuid}`, qui ne retourne que les événements publiés. Elle évite de créer artificiellement cette relation dans le client.
- les champs sans source métier actuellement exposée restent absents de la représentation réelle ; les composants les masquent ou utilisent un visuel neutre non informationnel.

## Matrice des données réelles

| Ressource / champ UI | Source backend | Mapper / composition | Fallback réel | Verdict |
|---|---|---|---|---|
| Event titre, dates, description, capacité, inscrits | `EventResponse`, `EventSummaryResponse` | `events.api.ts` | aucun | conforme |
| Event lieu, adresse, ville | `EventResponse.placeId` + `GET /places/{id}` | `usePlaceDetail` dans le détail Event | section masquée tant que Place n'est pas disponible | conforme |
| Event image, organisateur, prix, devise | absents de `EventResponse` | aucun | non affichés / visuel neutre | conforme, dette backend |
| Event participants détaillés | `GET /events/{id}/participants` existe | pas encore branché dans le détail | avatars non alimentés | dette explicitement identifiée |
| Événements liés à un Place | nouveau `GET /events?placeId=` | route Event Service ajoutée | liste vide réelle | route créée, branchement UI restant |
| Place nom, adresse, coordonnées, catégorie, médias, horaires, contact | `PlaceResponse` | `places.api.ts` | aucun | conforme |
| Place note, avis, prix, équipements, posts, similaires | absents de `PlaceResponse` et aucune route vérifiée | aucun | champs optionnels, aucune valeur `0` inventée | dette backend |
| Recommendation titre, type, catégorie, région, coordonnées, score | `RecommendationItem` | `recommendations.types.ts` | aucun | conforme |
| Recommendation ville, pays, description détaillée, média | absents de `RecommendationItem` | aucun | non inférés | conforme |
| Ticket titre d'événement | `TicketDetailResponse.eventId` + `GET /events/{id}` | `useEventDetail` dans détail Ticket | libellé générique, jamais l'ID comme nom | conforme |
| Ticket liste publique, monnaie, disponibilité | `GET /tickets/events/{id}/types` | `ticketing.api.ts` | aucun | conforme |

## Anomalies corrigées

| Champ | Avant | Source réelle / solution retenue | Après | Frontend | Backend |
|---|---|---|---|---|---|
| Event location | `"Lieu associé"` inventé | `placeId` puis `GET /places/{id}` sur le détail | affiché seulement si le Place est chargé | `events.api.ts`, `app/(events)/[id].tsx` | `PlaceController` existant |
| Event organizer | `fallbackUser('organizer')` | aucun champ nominatif dans `EventResponse` | champ absent | `events.api.ts`, `events/types.ts` | `EventResponse` |
| Event address/city/currency | chaînes vides ou `XAF` inventé | non fournis par EventResponse | optionnels et non affichés | mêmes fichiers | `EventResponse` |
| Event image | URI vide rendue comme image | média absent du contrat | visuel neutre, non une image métier | cartes Event et détail | `EventResponse` |
| Place category | `"Autre"` | `categoryName` de Place | `null` si absent | `places.api.ts`, `places/types.ts` | `PlaceResponse` |
| Place city/address/counts | `''` / `0` | champs source ou absence | `null`/absence, non déguisée en donnée | `places.api.ts`, types et cartes | `PlaceResponse` |
| Carte Place | note `0`, image `''`, `105 avis` | Place nearby ne fournit pas ces données | marqueur et aperçu sans métriques fictives | `app/(explore)/map.tsx` | `PlaceSummaryResponse` |
| Billet Event name | `eventName = eventId`; « Événement {id} » | composition Event sur le détail | titre Event réel ou « Billet d’événement » | ticket API, écran profil, carte | `EventController` existant |
| Événements d'un lieu | aucune route publique dédiée | route listant les événements publiés d'un `placeId` | contrat backend disponible | à brancher dans détail Place | `EventController.java` |

## DTO et mappers modifiés

- `Event` mobile : les champs réellement absents du DTO (`location`, `address`, `city`, `organizer`, `participants`, `currency`, `is_saved`) sont désormais optionnels.
- `Place` mobile : coordonnées, localisation, catégorie, compteurs et état de favori peuvent être absents. Le mapper ne fabrique plus `Autre`, `0` ni faux booléen de favori.
- `TrendingPlace`, `MapPlace` et `UpcomingEvent` acceptent les médias, notes, distances et lieux absents.
- `PublicEventTickets.eventName` est optionnel ; le mapper ne lui assigne plus `eventId`.

## Fichiers modifiés dans ce lot

- `event-service/src/main/java/.../controller/EventController.java`
- `yeyamo-mobile/src/features/events/events.api.ts`
- `yeyamo-mobile/src/features/events/types.ts`
- `yeyamo-mobile/src/features/events/useEvents.ts`
- `yeyamo-mobile/src/features/places/places.api.ts`
- `yeyamo-mobile/src/features/places/types.ts`
- `yeyamo-mobile/src/features/explore/types.ts`
- `yeyamo-mobile/src/features/ticketing/ticketing.api.ts`
- `yeyamo-mobile/src/features/ticketing/types.ts`
- `yeyamo-mobile/src/app/(events)/[id].tsx`
- `yeyamo-mobile/src/app/(profile)/ticket/[id].tsx`
- `yeyamo-mobile/src/app/(explore)/map.tsx`
- `yeyamo-mobile/src/components/events/EventCard.tsx`
- `yeyamo-mobile/src/components/explore/EventCard.tsx`
- `yeyamo-mobile/src/components/explore/PlaceListItem.tsx`
- `yeyamo-mobile/src/components/profile/TicketCard.tsx`

## Validation

Exécuté :

```powershell
# yeyamo-mobile
.\node_modules\.bin\tsc.cmd --noEmit

# event-service
mvn test
```

- TypeScript : succès, aucune erreur.
- Maven : la compilation Java et les tests unitaires ciblés passent (`EventOutboxRelayTests`, `EventRegistrationAlignmentTest`). Le test de contexte `EventServiceApplicationTests` échoue avant l'exécution métier avec `scale has no meaning for SQL floating point types`, lors de l'initialisation Hibernate de `GeographicFields`. Cette erreur de mapping JPA préexistante bloque le test de contexte ; elle n'est pas causée par la route ajoutée, dont la compilation est validée.

## Dette restante, non transformée en donnée fictive

1. Ajouter au contrat Event une stratégie métier pour média, organisateur public et prix si ces sections doivent devenir renseignées sans composition.
2. Brancher `GET /events?placeId=` dans le détail Place pour alimenter la section d'événements liés avec de vraies données.
3. Exposer/implémenter les contrats Reviews, rating agrégé, prix, équipements et recommandations de lieux si les sections correspondantes doivent être affichées.
4. Ajouter un endpoint ou une composition batch pour les titres d'événements dans les listes de billets, afin de ne pas lancer une requête Event par carte.
5. Corriger le mapping Hibernate de `GeographicFields` afin de rétablir le test de contexte Event Service.

Le lot suivant n'a pas été commencé.
