# Rapport de vérification — paiement HR-Skills Pay

Date : 2026-09-06  
Périmètre : `payment-service`, `booking-service`, `ticket-service`, API Gateway.  
Exécution Docker : aucune.

## Statut honnête

Le code d'intégration HR-Skills Pay est présent et les tests locaux sont verts. Il ne peut toutefois pas être déclaré opérationnel pour les parcours Booking et Ticket réels : leurs commandes Kafka ne contiennent pas les trois données obligatoires du Cash-In mobile-money (`operator`, `country`, `phone_number`). Aucune valeur n'a été inventée.

## Partie 0 — restitution Tâches 1 à 4

**Statut : BLOQUÉ.** Les preuves exhaustives demandées (tableau complet des routes, cinq recherches brutes, trois extraits de gestionnaire/filtre d'erreurs, fichier de test complet et sortie de la suite 41 modules pour chacune des quatre tâches) ne sont pas reconstituées dans ce rapport. Les affirmations de versions/commits/PR de l'ancien rapport ont donc été retirées : elles n'étaient pas vérifiables depuis le code courant.

## Contrats réellement vérifiés

| Producteur | Publication | Topic | Champs constatés | Champs Cash-In absents |
| --- | --- | --- | --- | --- |
| Booking | `BookingApplicationService.java:160` | `payment.commands` | `sagaId`, `bookingId`, `userId`, `partnerId`, `amount`, `currency`, `idempotencyKey` | `operator`, `country`, `phone_number` |
| Ticket | `infrastructure/outbox/OutboxService.java:57-65` | `payment.commands` | `sagaId`, `bookingId` (= order), `userId`, `amount`, `currency`, `idempotencyKey` | `operator`, `country`, `phone_number` |

Les deux consommateurs de résultat lisent `payment.events` :

- Booking : `booking-service/.../PaymentEventConsumer.java:2`, événements `payment.authorized` et `payment.failed`, identifiant `payload.bookingId`.
- Ticket : `ticket-service/.../PaymentEventConsumer.java:36-54`, événements `payment.authorized`/`payment.confirmed` et `payment.failed`, identifiant `payload.bookingId` (qui représente l'orderId Ticket).

## Implémentation vérifiée/corrigée

- `HrSkillsPayTokenManager` appelle bien `POST {base}/v1/auth/transaction-token`, conserve le token en mémoire, le renouvelle avec une marge de 300 secondes et protège le renouvellement par `ReentrantLock`.
- `HrSkillsPayClient` appelle bien `POST {base}/api/v1/payin/mobile-money`, avec timeout de 10 secondes, `Authorization`, `X-Transaction-Token` et `Idempotency-Key`.
- `PaymentAttemptEntity` persiste `reference`, `transactionId`, source, montant, devise, clé d'idempotence et état (`INITIATED`, `SUCCESS`, `FAILED`, `HOLD`).
- `PaymentCommandConsumer` écoute `payment.commands` et ne bascule sur HR-Skills Pay que lorsque les deux clés d'agrégateur sont configurées.
- `HrSkillsPayWebhookController` expose `POST /api/webhook/payment`; `SecurityConfig` l'autorise sans JWT. La vérification HMAC-SHA256 porte sur les octets bruts et utilise `MessageDigest.isEqual`.
- Le webhook est idempotent pour les états terminaux et traite `payment.hold` comme une alerte manuelle, sans confirmer ni annuler automatiquement.
- Correction appliquée pendant cette vérification : les événements `payment.authorized` et `payment.failed` passent désormais par `PaymentOutboxPort`. Ils sont donc enregistrés dans l'outbox transactionnelle avant publication Kafka, au lieu d'être envoyés directement via `KafkaTemplate`.
- En cas d'échec d'initiation ou de contrat incomplet, l'échec est inscrit dans l'outbox et la commande peut être marquée traitée ; cela évite les doublons dus aux retries Kafka.

## Configuration

Variables présentes sans secrets dans `.env.example` :

```dotenv
PAYMENT_AGGREGATOR_BASE_URL=https://api.hrskills-pay.com
PAYMENT_AGGREGATOR_KEY_A=
PAYMENT_AGGREGATOR_KEY_B=
PAYMENT_AGGREGATOR_WEBHOOK_SECRET=
```

`cloud-conf-yeyamo/payment-service.properties:34-37` les mappe vers `payment.aggregator.*`.

## Validation exécutée

Commande :

```text
mvn test -pl payment-service
```

Sortie de synthèse brute :

```text
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  01:28 min
[INFO] Finished at: 2026-09-06T16:40:56+01:00
```

La classe `HrSkillsPayIntegrationTest` exécute 7 scénarios sur H2, `@EmbeddedKafka` et WireMock, sans appel réseau HR-Skills Pay : cache/renouvellement token, Cash-In, signature invalide, succès, échec et webhook dupliqué.

Commande :

```text
mvn compile -DskipTests
```

Sortie de synthèse brute :

```text
[INFO] Reactor Summary:
[INFO] YeYamo API Reactor 1.0.0-SNAPSHOT .................. SUCCESS
[INFO] BUILD SUCCESS
[INFO] Total time:  01:24 min
[INFO] Finished at: 2026-09-06T16:42:31+01:00
```

Le reactor compilé contient 41 modules.

## Blocage produit restant

Pour rendre le Cash-In réellement appelable, le parcours qui crée une réservation ou une commande Ticket doit recueillir puis publier, après validation métier :

```text
operator
country
phone_number
```

Le code actuel refuse explicitement une commande incomplète et produit `payment.failed`; il ne choisit ni opérateur, ni pays, ni numéro par défaut. Le modèle source de ces données (profil utilisateur vérifié, moyen de paiement enregistré ou formulaire de paiement) doit être validé avant de modifier les producteurs.

## Checklist

- [ ] Partie 0, tâches 1-4 : toutes RÉSOLU avec preuve ? **NON**
- [x] Payloads et topics de retour identifiés pour Booking et Ticket
- [x] Token avec renouvellement et verrou concurrent implémenté et testé
- [x] HMAC sur corps brut avec comparaison en temps constant
- [x] Idempotence webhook couverte par test
- [ ] Sept scénarios avec preuve de publication Kafka consommée de bout en bout ? **NON** — les scénarios appellent le consumer et vérifient l'état/outbox ; ils ne lisent pas encore le message produit depuis `payment.events`.
- [x] Aucune clé ou secret réel dans ce rapport

## Conclusion

TRAVAIL INCOMPLET

Points manquants : restitution probante de la Partie 0, ajout validé des données mobile-money aux contrats producteurs, et test de consommation de l'événement réellement publié par l'outbox. La phrase « PHASE 3 IMPLÉMENTÉE — PAIEMENT RÉEL OPÉRATIONNEL EN SANDBOX » n'est donc pas applicable.
