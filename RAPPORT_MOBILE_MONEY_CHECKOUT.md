# Rapport — checkout Mobile Money booking et ticket

## Partie A — audit préalable

### A.1 Pays d'inscription — RÉSOLU

Le pays d'inscription est porté par `auth-service`, sur `User.countryCode` :

```java
// auth-service/.../models/User.java:65-66
@Column(name = "country_code", length = 2)
private String countryCode;
```

Avant ce lot, le JWT ne portait pas ce champ. Il contient maintenant le claim
`country` :

```java
// auth-service/.../security/JwtService.java:77-81
.claim("email", user.getEmail())
.claim("phone", user.getPhone())
.claim("country", user.getCountryCode())
```

Le choix retenu est donc **un claim JWT**, et non un appel synchrone à
`user-service` pour chaque paiement. Le contrôleur booking lit ce claim à
`BookingController.java:113-118`; celui des tickets le lit à
`UserTicketController.java:219-224`.

Preuve automatisée :

```text
mvn test -pl auth-service -Dtest=JwtServiceCountryClaimTest
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### A.2 DTO réellement utilisé — RÉSOLU

- Booking : `BookingDtos.CreateBooking` était limité à `slotId` et
  `quantity`. Il accepte maintenant `operator` et `phoneNumber`
  (`BookingDtos.java:38-47`).
- Ticket : le endpoint actif est `POST /api/v1/tickets/orders` dans
  `interfaces/rest/UserTicketController`, et non le contrôleur legacy sous
  profil `legacy-ticket-api`. Son `CreateOrderRequest` accepte désormais les
  deux champs (`UserTicketController.java:212-217`).
- Recherche effectuée : `rg -n -i "phone" booking-service ticket-service`.
  Aucun champ de checkout mobile money n'était présent dans les DTO actifs.

## Partie B — booking-service

### B.1 Implémentation — RÉSOLU

- Pour une activité avec `isPaid=true`, l'application refuse l'absence ou
  l'invalidité de l'opérateur, du numéro E.164 ou du pays de compte.
  Les erreurs métier sont `PAYMENT_OPERATOR_REQUIRED`,
  `PAYMENT_PHONE_REQUIRED` et `PAYMENT_COUNTRY_REQUIRED`
  (`BookingApplicationService.java:153-160`, `368-386`).
- Les valeurs admises pour `operator` sont : `mtn`, `orange`, `moov`,
  `airtel`, `mpesa`, `wave`, `free`, `tmoney`, `afrimoney`.
- Les données de checkout sont persistées séparément de `countryCode` du
  lieu : `payment_operator`, `payment_phone_number`,
  `payment_country_code` dans
  `booking-service/src/main/resources/db/migration/V7__add_mobile_money_checkout_fields.sql`.
- Le message `payment.authorization.requested` contient désormais
  `operator`, `phoneNumber` et `country`
  (`BookingApplicationService.java:174-186`).
- Pour une activité gratuite, ces champs restent facultatifs et aucun appel
  paiement n'est produit.

### B.2 Vérification — RÉSOLU

```text
mvn test -pl booking-service -Dtest=BookingApplicationServiceTest
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Ce test couvre explicitement le rejet d'une activité payante sans checkout et
la présence des trois champs dans la commande d'outbox.

```text
mvn test -pl booking-service -Dtest=BookingPaymentCommandKafkaTest
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

`BookingPaymentCommandKafkaTest` démarre un Kafka embarqué, publie via
`BookingOutboxPublisher`, puis relit effectivement le message du topic
`payment.commands`.

## Partie C — ticket-service

### C.1 Implémentation — RÉSOLU

L'ordre de billets actif passe systématiquement à `AWAITING_PAYMENT` et
publie `payment.authorization.requested` dans `OrderService`; les champs de
checkout sont donc obligatoires pour toute commande de billet.

- `operator` et `phoneNumber` sont validés dès le DTO HTTP
  (`UserTicketController.java:212-217`) et validés à nouveau dans le service
  (`OrderService.java:224-242`).
- Le claim JWT `country` est transmis au service sans que le client puisse
  l'usurper.
- `TicketOrderEntity` et la migration
  `ticket-service/src/main/resources/db/migration/V3__add_mobile_money_checkout_fields.sql`
  conservent les trois valeurs de traçabilité.
- L'outbox publie `partnerId`, `operator`, `phoneNumber`, `country`, en plus
  du contrat existant (`OutboxService.java:49-73`).

### C.2 Vérification — RÉSOLU

```text
mvn test -pl ticket-service -Dtest=OrderServiceMobileMoneyTest
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

mvn test -pl ticket-service -Dtest=TicketPaymentCommandKafkaTest
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Le second test utilise `@EmbeddedKafka`, appelle `OutboxPublisher`, puis
consomme le message effectivement produit sur `payment.commands`.

Une exécution complète du module avant l'ajout du test Kafka a également été
verte :

```text
mvn test -pl ticket-service
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Contrat mobile

Les valeurs client ne doivent jamais contenir `country` : il vient du JWT
émis après connexion. Les numéros sont au format E.164. `operator` est en
minuscules et doit appartenir à la liste ci-dessus.

### Créer une réservation d'activité

`POST /api/v1/bookings`

Headers :

```text
Authorization: Bearer <access-token>
Idempotency-Key: <clé-unique>
Content-Type: application/json
```

Activité payante :

```json
{
  "slotId": "0d537b0c-3c89-4f3d-93d2-0d7aa4e0885e",
  "quantity": 2,
  "operator": "mtn",
  "phoneNumber": "+237690123456"
}
```

Pour une activité gratuite, `operator` et `phoneNumber` peuvent être omis.

### Créer une commande de billets

`POST /api/v1/tickets/orders`

Headers :

```text
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "holdId": "8f3a23e1-73f5-4fb9-aefc-8f1c8fa1fc34",
  "promotionCode": "OPTIONNEL",
  "operator": "orange",
  "phoneNumber": "+2250701234567"
}
```

## Compatibilité avec HR-Skills Pay

Le contrat interne Kafka conserve le nom Java/API `phoneNumber`. Le
`payment-service` accepte explicitement `phoneNumber` puis transmet le champ
exigé par HR-Skills Pay sous le nom `phone_number`
(`HrSkillsPayService.java:53-58`, `HrSkillsPayClient.java:69-73`).

## Partie D — vérification globale

### Compilation des modules modifiés — RÉSOLU

```text
mvn test -pl auth-service -Dtest=JwtServiceCountryClaimTest
BUILD SUCCESS

mvn test -pl booking-service -Dtest=BookingApplicationServiceTest
BUILD SUCCESS

mvn test -pl ticket-service -Dtest=OrderServiceMobileMoneyTest
BUILD SUCCESS
```

### Reactor complet — BLOQUÉ PAR LA LIMITE D'EXÉCUTION

Les commandes exactes ont été lancées :

```text
mvn test
mvn compile -DskipTests
mvn compile -DskipTests -rf :interaction-service
```

L'environnement d'exécution interrompt chaque commande au bout de 60 secondes.
La compilation n'a signalé aucune erreur avant interruption : le premier
passage a atteint `interaction-service` (21/41), le second a atteint
`analytics-service` après avoir compilé `booking-service`, `payment-service`
et `ticket-service`. Ce n'est donc pas une preuve de reactor complet vert ;
la vérification globale doit être relancée localement sans cette limite.

Commande à exécuter depuis `yeyamo-api` :

```powershell
mvn test
mvn compile -DskipTests
```

## Conclusion

Les tâches fonctionnelles A, B et C sont **RÉSOLUES** : le checkout mobile
fournit maintenant les données requises, le pays vient du compte via JWT, les
valeurs sont persistées et les deux producteurs publient un message réel sur
`payment.commands` vérifié par Kafka embarqué.

La seule vérification non soldée est l'exécution complète des 41 modules,
bloquée par la limite de 60 secondes de cet environnement, et non par une
erreur Maven observée.
