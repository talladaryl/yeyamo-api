# YEYAMO — BOOKING / EVENT ALIGNMENT

## 1. Périmètre et constat initial

L’audit a porté sur `booking-service`, `event-service`, `ticket-service`,
`payment-service`, `notification-service`, `gamification-service`,
`catalog-service`, `place-service`, `recommendation-service`, les contrats
Kafka et la configuration gateway. Les changements précédents sur Feed,
Stories, lieux, interactions et recommandations sont conservés.

Avant cette passe, le dépôt disposait déjà de :

- créneaux `activity_slots`, réservation pessimiste, idempotence et outbox dans
  `booking-service` ;
- réservation d’expérience depuis le catalogue canonique ;
- paiements asynchrones `payment.commands` / `payment.events` ;
- inscription événement avec verrou pessimiste et contrainte unique
  `(event_id, user_id)` ;
- tickets et commandes de billets, dans un domaine séparé ;
- un outbox Event et des projections Recommendation.

Les manques réels étaient : des listes de créneaux incluaient des créneaux clos
ou complets, la liste des réservations utilisateur n’était pas paginée, les
annulations ordinaires demandaient encore un remboursement automatique, et les
topics réservation/événement n’étaient pas consommés par Notification.

## 2. Modèle réellement présent

| Sujet | Source de vérité | Données importantes |
|---|---|---|
| Lieu | `place-service` / projection locale booking | `placeId`, statut actif |
| Expérience réservable | `catalog-service` | asset `EXPERIENCE` publié, partenaire, prix, devise, capacités min/max, `placeId`, pays |
| Activité générique | `booking-service.activity_slots` | `activityId` texte, créneau, capacité, prix/devise du créneau |
| Disponibilité finale | `booking-service.activity_slots` | statut `OPEN`, `reservedCount`, capacité, dates |
| Réservation | `booking-service.bookings` | snapshot prix/devise/pays/quantité/créneau, états métier et paiement |
| Événement/sortie | `event-service.events` | propriétaire, lieu ou localisation libre, dates UTC, capacité, inscrits, visibilité, statut |
| Inscription événement | `event_registrations` | une ligne unique par événement/utilisateur, `CONFIRMED` ou `CANCELLED` |
| Invitation | `event_invitations` | événement, utilisateur ciblé, organisateur |
| Paiement réservation | `payment-service.payments` | `sourceType=BOOKING`, `sourceId=bookingId`, saga, montant/devise serveur, provider |
| Billet | `ticket-service` | hold, order, ticket type, ticket ; domaine séparé de Booking |
| XP | `gamification-service.EventXpPolicy` | politiques existantes consommant `booking.events` |

Relation effectivement raccordée : `PLACE -> EXPERIENCE (catalog) ->
EXPERIENCE SLOT (booking) -> BOOKING -> PAYMENT`. Les dates d’un créneau sont
des `Instant` UTC ; l’application mobile doit les afficher dans son fuseau
local. Aucun fuseau horaire IANA distinct n’est stocké dans `ActivitySlot`.

`EVENT -> registration/invitation` est indépendant de `BOOKING` et de
`TICKET`. Un événement public peut donc avoir des inscriptions gratuites sans
être une expérience réservable ou une vente de billets.

## 3. Routes réelles et évolution appliquée

### Booking et disponibilité

| Route | Méthode | Authentification / rôle | Contrat effectif |
|---|---|---|---|
| `/api/v1/activities` | GET | JWT | Page de `SlotView`, filtrable par `placeId`. Depuis cette passe, seulement créneaux futurs, `OPEN` et avec places disponibles. |
| `/api/v1/activities/{activityId}/availability` | GET | JWT | Liste des mêmes créneaux réellement réservables. |
| `/api/v1/bookings` | POST | JWT | `CreateBooking { slotId, quantity, operator?, phoneNumber? }`, en-tête obligatoire `Idempotency-Key`. Le prix, devise, utilisateur et statut ne viennent jamais du mobile. |
| `/api/v1/bookings/me` | GET | JWT | Page de `BookingView`, `page`, `size` (maximum 100), réservations du seul sujet JWT. C’est désormais paginé. |
| `/api/v1/bookings/{id}` | GET | propriétaire, propriétaire du créneau ou admin | Détail `BookingView`. |
| `/api/v1/bookings/{id}/history` | GET | mêmes règles | Historique persistant de la réservation. |
| `/api/v1/bookings/{id}/cancel` | POST | propriétaire, partenaire du créneau ou admin | `CancelBooking { reason }` et `Idempotency-Key`. Libère la capacité ; aucun remboursement automatique. |
| `/api/v1/bookings/{id}/complete` | POST | partenaire propriétaire du créneau ou admin | `Idempotency-Key`; seulement après la fin du créneau. |
| `/api/v1/bookings/partner/me` | GET | JWT | Page des réservations des créneaux du partenaire authentifié. |
| `/api/v1/booking-management/experience-slots` | POST | PARTNER/ADMIN | `CreateExperienceSlot { experienceId, startsAt, endsAt, capacity }`; tarif, devise, pays, lieu et propriétaire lus dans Catalog/Partner. |
| `/api/v1/booking-management/slots` | POST | PARTNER/ADMIN | Ancien créneau générique. Voir limitation `ACTIVITY_CATALOG_NOT_CONNECTED`. |
| `/api/v1/booking-management/slots/{id}/close` | POST | propriétaire ou admin | Ferme un créneau. |

`SlotView` contient `id`, `activityId`, `placeId`, `startsAt`, `endsAt`,
`capacity`, `reserved`, `available`, `unitPrice`, `currency`, `countryCode`,
`status`, `isPaid`, `amount` et `activityType`. `BookingView` renvoie le
snapshot de prix/devise, références, statuts, dates et motif d’annulation ; il
ne renvoie ni numéro de téléphone ni données de paiement sensibles.

### Événements

| Route | Méthode | Règle |
|---|---|---|
| `/api/v1/events` | POST | Crée une sortie `PENDING`; le propriétaire est le sujet JWT. |
| `/api/v1/events?placeId=` | GET | Événements publics publiés d’un lieu. |
| `/api/v1/events/upcoming` | GET | Événements publics publiés futurs. |
| `/api/v1/events/me` | GET | Événements auxquels le sujet est inscrit. |
| `/api/v1/events/{id}` | GET | Respecte la visibilité privée. |
| `/api/v1/events/{id}/register` | POST | Publication requise, date future, invitation si demandée, capacité atomique, aucun doublon. |
| `/api/v1/events/{id}/unregister` | DELETE | Annulation persistante de l’inscription et libération d’une place. |
| `/api/v1/events/{id}/invitations` | POST/GET | Création ou liste par propriétaire/admin. |
| `/api/v1/events/{id}/invitations/{userId}` | DELETE | Révocation par propriétaire/admin. |
| `/api/v1/events/{id}/status` | PATCH | Transitions vérifiées et publication/cancel/completion d’événements de domaine. |

`EventResponse` contient `capacity` et `registeredCount` ; le client peut
afficher une disponibilité indicative mais ne doit jamais conclure qu’une place
est réservée avant la réponse de `POST /register`.

### Paiement et billets

`payment-service` possède `GET /api/v1/payments/mine`,
`GET /api/v1/payments/{id}`, les remboursements de lecture, et
`POST /api/v1/payments/{id}/refunds` réservé aux administrateurs. Il traite des
commands Kafka réels et appelle le provider configuré ; aucun provider fictif
n’a été ajouté.

`ticket-service` possède son propre flux : `POST /api/v1/tickets/hold`,
`POST /api/v1/tickets/orders`, `GET /my-orders`, `GET /my-tickets` et types de
billets par événement. Ce flux n’est pas lié à `booking-service` et aucune
conversion artificielle réservation -> billet n’a été créée.

## 4. Validations, concurrence et idempotence

Une création de réservation verrouille le créneau en écriture (`PESSIMISTIC_WRITE`).
`reserve(quantity)` exige simultanément : statut `OPEN`, début futur,
`quantity >= 1` et `reservedCount + quantity <= capacity`. Deux derniers achats
concurrents ne peuvent donc pas vendre la même dernière place.

Le serveur vérifie le pays/la feature, le format mobile money pour une activité
payante, le créneau et sa capacité. `Idempotency-Key` est mémorisé par
utilisateur + opération + créneau ou réservation ; la répétition renvoie la
réservation existante au lieu de réexécuter la mutation.

Pour une expérience, l’endpoint partenaire appelle Catalog et Partner :

- l’asset doit être une expérience publiée lisible publiquement ;
- le partenaire authentifié doit en être gestionnaire ;
- le tarif, la devise, le pays et le lieu sont pris au catalogue ;
- la capacité du créneau respecte le minimum/maximum canonique.

Pour les événements, l’événement lui-même est verrouillé avant inscription.
La contrainte unique de base de données et le statut de registration empêchent
les doublons ; `registeredCount` est incrémenté/décrémenté dans la même
transaction.

## 5. Machines d’état

### Réservation

```text
création
  gratuit  -> CONFIRMED / NOT_REQUIRED
  payant   -> PENDING / AUTHORIZATION_PENDING

payment.authorized -> CONFIRMED / AUTHORIZED
payment.failed     -> CANCELLED / FAILED + capacité libérée
annulation         -> CANCELLED / REFUND_NOT_REQUESTED (ou annulation d'autorisation en cours)
fin du créneau     -> COMPLETED
remboursement      -> uniquement procédure manuelle Payment admin
```

Une autorisation arrivée après une annulation devient
`REFUND_NOT_REQUESTED` : elle est historisée et doit être traitée par le
processus de remboursement manuel. Cela évite toute promesse de remboursement
automatique au mobile.

### Événement

```text
DRAFT -> PENDING_REVIEW -> PUBLISHED -> CANCELLED
                              |            
                              +-> COMPLETED

PENDING/PENDING_REVIEW -> PUBLISHED | CANCELLED | REJECTED | ARCHIVED
SUSPENDED -> PUBLISHED | ARCHIVED
```

Une inscription est possible uniquement sur `PUBLISHED`, avant `startAt`, avec
une capacité restante et, si nécessaire, une invitation persistante.

## 6. Outbox, consommateurs et notifications

`booking-service` publie dans l’outbox `booking.events` uniquement des
transitions réelles : `booking.created`, `booking.confirmed`,
`booking.cancelled`, `booking.completed`, `booking.refunded` ainsi que les
événements de créneau. Les consommateurs paiements et gamification possèdent
un reçu d’événement pour ignorer les relectures.

`notification-service` consomme maintenant `booking.events` et `event.events`
en plus de ses topics existants. Les notifications créées sont :

- `BOOKING_CONFIRMED`, `BOOKING_CANCELLED`, `BOOKING_COMPLETED` pour le
  voyageur concerné ;
- `EVENT_REGISTRATION_RECEIVED` et `EVENT_REGISTRATION_CANCELLED` pour
  l’organisateur ;
- `EVENT_INVITATION` pour le destinataire exact ;
- `EVENT_CANCELLED` et `EVENT_COMPLETED` pour les seuls participants confirmés.

Les événements ciblés embarquent `recipientIds` calculés par `event-service`
dans sa transaction. Aucun consumer ne recherche « tous les utilisateurs ».
Le stockage Notification est idempotent par `(sourceEventId, recipientId)`.

## 7. XP et réputation

La politique XP déjà présente mappe `booking.confirmed` et
`booking.completed` vers ses activités XP existantes. Cette passe ne modifie ni
les valeurs, ni les raisons, ni les compteurs : elle ne crée donc pas de points
arbitraires.

Il n’existe pas de politique versionnée approuvée pour l’inscription,
l’annulation, l’invitation ou la complétion d’un événement. Ces points sont
explicitement : `XP_POLICY_NOT_DEFINED`. Aucun hook XP supplémentaire n’a été
ajouté.

## 8. Adventure Plan, prix, heure et disponibilité

Recommendation conserve une projection compacte, sans appel HTTP N+1. Les
événements publiés fournissent titre/date/lieu/visibilité ; les assets Catalog
peuvent fournir prix/devise. Les créneaux Booking, leur capacité en temps réel
et leur prix final ne sont pas projetés dans Recommendation.

La réponse de planning affiche donc désormais une disponibilité `UNKNOWN` pour
une recommandation tant qu’elle n’a pas été validée par le domaine final. Les
annulations et complétions d’événements propagent toujours les statuts
`CANCELLED` / `UNAVAILABLE` aux plans persistés.

Verdict : **PARTIELLEMENT CONNECTÉ**. Les dates et prix catalogués peuvent
orienter la découverte ; la disponibilité et le prix opposable restent
exclusivement ceux de `booking-service` au moment de la réservation. Aucun
statut `CONFIRMED` n’est fabriqué depuis Recommendation.

## 9. LIMITATIONS BLOQUANTES / contrats à prévoir

### ACTIVITY_CATALOG_NOT_CONNECTED

Le vieux endpoint `POST /api/v1/booking-management/slots` accepte un
`activityId` libre et le tarif du partenaire. Il n’existe pas de service ou de
contrat canonique « Activity » permettant de prouver son statut actif, son
propriétaire, son tarif, sa devise, son lieu et son fuseau.

Le parcours conforme pour la V1 est `CreateExperienceSlot`, qui s’appuie sur
Catalog/Partner. Pour rendre l’activité générique aussi sûre, le backend devra
exposer un read-model/versionné, par exemple :

```text
GET /internal/activities/{activityId}
-> { id, status: ACTIVE, ownerId, placeId, countryCode,
     price, currency, capacityMin, capacityMax, timezone }
```

`booking-service` devra alors créer un snapshot depuis cette réponse, jamais
depuis une valeur mobile. Ce contrat n’a pas été inventé ici.

### Absences restantes

- pas de lien métier Booking <-> Ticket <-> Event ;
- pas de fuseau IANA associé au créneau ; les timestamps sont UTC ;
- pas de tarif/stock de créneau dans la projection Adventure Plan ;
- pas de politique XP approuvée pour les actions événementielles
  (`XP_POLICY_NOT_DEFINED`) ;
- le remboursement est manuel et admin ; aucune règle métier de montant ou de
  délai de remboursement n’est exposée au client.

## MOBILE_CONTRACT

### Règles communes

- Toujours envoyer le bearer JWT ; l’identité vient exclusivement du token.
- Envoyer `Idempotency-Key` (UUID ou clé opaque stable) sur création,
  annulation et complétion de réservation.
- Ne jamais envoyer ni recalculer `amount`, `currency`, `userId`,
  `BookingStatus`, `PaymentStatus`, capacité ou disponibilité.
- Afficher la réponse serveur après chaque mutation, et ne pas déduire un état
  depuis le bouton appuyé.
- En cas de `409 SLOT_UNAVAILABLE`, `EVENT_FULL`, `ALREADY_REGISTERED` ou
  transition invalide, rafraîchir la ressource correspondante et afficher le
  message métier retourné.

## MOBILE BOOKING FLOW

1. Charger `GET /api/v1/activities/{activityId}/availability` ou la page
   `/api/v1/activities?placeId=...`.
2. Afficher seulement les `SlotView` reçus et leur `available` serveur.
3. Créer la réservation avec le `slotId`, la quantité et une clé
   d’idempotence. Pour une réservation payante, fournir l’opérateur et le
   numéro E.164 ; le pays provient du JWT.
4. Afficher la carte depuis `BookingView` : référence, créneau, quantité,
   snapshot prix/devise, `status`, `paymentStatus`, dates et motif éventuel.
5. Poller ou rafraîchir `GET /api/v1/bookings/{id}` si la réponse est
   `PENDING / AUTHORIZATION_PENDING`; attendre `CONFIRMED / AUTHORIZED` ou
   `CANCELLED / FAILED`.
6. L’annulation appelle `/cancel` avec un motif. La carte doit indiquer que le
   remboursement n’est pas automatique (`REFUND_NOT_REQUESTED`) ; ne jamais
   promettre de remboursement.
7. L’organisateur/partenaire termine seulement une réservation confirmée après
   `endsAt`, avec `/complete`.

## MOBILE EVENT FLOW

1. Créer la sortie ; elle reste dans son statut serveur jusqu’à la revue et la
   publication autorisée.
2. Avant inscription, lire l’événement et afficher `capacity - registeredCount`
   uniquement comme indication.
3. Appeler `/register` ; seul le `200 EventResponse` confirme la place.
4. Pour une sortie privée sans accès libre, afficher l’inscription seulement si
   le serveur a conservé une invitation ; un `403 EVENT_INVITATION_REQUIRED`
   n’est pas contournable côté mobile.
5. Appeler `/unregister` pour quitter ; la capacité est libérée par serveur.
6. L’organisateur utilise les routes invitations ; les destinataires et les
   participants reçoivent les notifications ciblées des changements réels.
7. Les billets éventuels suivent le flux Ticket séparé et ne valent pas une
   inscription Event ou une réservation Booking sans contrat futur explicite.

## 10. Tests et validation

Exécutés avec succès :

```text
mvn -pl booking-service,event-service,notification-service,recommendation-service -am test-compile -DskipTests
mvn -pl booking-service,event-service,notification-service,recommendation-service test \
  "-Dtest=BookingApplicationServiceTest,ActivitySlotEntityTest,EventRegistrationAlignmentTest,EventNotificationPolicyTest,RecommendationEventConsumerTest"
git diff --check
```

Les tests couvrent notamment la réservation payante, l’idempotence existante,
la capacité, l’annulation sans remboursement automatique, la notification de
réservation, le ciblage sans broadcast des participants et la projection
Recommendation. Aucun build mobile ni déploiement n’a été lancé.
