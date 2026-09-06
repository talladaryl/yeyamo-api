# Consolidation Phase 0/1/2 — état de livraison

## PARTIE A — RÉSOLU

### Audit

`booking-service` possédait déjà un flux payé opérationnel, contrairement à l’hypothèse « confirmation immédiate sans distinction » :

- `ActivitySlotEntity` portait `unitPrice`, `currency`, `placeId`, capacité et dates, mais pas le choix métier explicite `isPaid/amount`.
- `BookingEntity.pending` crée `PENDING` et `AUTHORIZATION_PENDING` pour un montant strictement positif ; les réservations à zéro sont confirmées dans la même transaction.
- `BookingApplicationService` publie déjà `payment.authorization.requested` sur le topic `payment.commands` pour les réservations payantes. `paymentAuthorized` fait transiter `PENDING` vers `CONFIRMED`; `paymentFailed` annule et libère le créneau.
- Le pattern ticket a des holds `ACTIVE/CONFIRMED/RELEASED/EXPIRED` et des commandes `AWAITING_PAYMENT/PAID/ISSUED`; il n’est pas copié tel quel parce que booking-service possède déjà une saga d’autorisation dédiée cohérente avec son modèle.

### Changements

| Fichier | Changement |
|---|---|
| `booking-service/.../ActivitySlotEntity.java` | Ajout des colonnes métier `isPaid` et `amount`; montant gratuit normalisé à `0.00`; validation `INVALID_ACTIVITY_PRICING`. |
| `booking-service/.../BookingDtos.java` | `CreateSlot` accepte `isPaid/amount` tout en gardant les constructeurs historiques; `SlotView` expose ces champs. |
| `booking-service/.../BookingApplicationService.java` | Validation de prix à la création et propagation vers l’entité/DTO. Le flux Kafka existant est conservé. |
| `booking-service/src/main/resources/db/migration/V6__add_activity_pricing.sql` | Ajout Flyway non destructif de `is_paid DEFAULT false` et `amount DEFAULT 0.00`. |
| `yeyamo-mobile/src/features/places/types.ts` | Types activités enrichis avec `isPaid` et `amount`. |
| `yeyamo-mobile/src/app/(bookings)/activity/[id].tsx` | Prix/« Gratuit » affiché; réponse `PENDING` présentée comme « En attente de paiement », sans formulaire de paiement. |

### Phase 3

La Phase 3 doit traiter l’événement/commande déjà existant : topic **`payment.commands`**, type **`payment.authorization.requested`**. La confirmation est déjà consommée par le flux `paymentAuthorized` de booking-service; une réservation payante n’est pas confirmée automatiquement dans ce lot.

### Validation

- `mvn compile -DskipTests -pl booking-service -am` : OK.
- `mvn test -pl booking-service` : OK, 34 tests, 0 échec, dont `PlaceActivityIntegrationTest` (5 tests).
- `node_modules/.bin/tsc.cmd --noEmit` dans `yeyamo-mobile` : OK, 0 erreur.
- Le `mvn compile -DskipTests` complet a d’abord révélé puis validé la correction de compatibilité `SlotView`; après correction, la compilation ciblée du module est verte. La relance complète des 41 modules reste à faire dans la validation finale.

## PARTIE B — PARTIEL

### Réalisé

- Audit de `InternalServiceTokenFilter` : il protège strictement `/internal/**`, bloque fail-closed sans token et émet `INTERNAL_UNAUTHORIZED` au format `ErrorResponse`.
- Audit de `PartnerInternalController` : un partenaire non vérifié renvoyait encore `PARTNER_NOT_VERIFIED`, fuite d’état contraire à l’obfuscation demandée.
- Correction : le cas non vérifié renvoie maintenant `PARTNER_NOT_FOUND`, comme le partenaire inexistant.
- Compilation : `mvn compile -DskipTests -pl security-hardening-starter,partner-service,messaging-service -am` : OK.

### Restant

- Inventaire exhaustif des routes racine de l’ensemble des microservices et comparaison exhaustive avec `api-gateway.properties`.
- Recherche complète des variantes `internal`, `inter-service`, `service-to-service`, `s2s`, `machine-to-machine` (la commande initiale a été interrompue par un chemin de handler inexistant, sans modification de sécurité supplémentaire).
- Comparaison de format avec le vrai `@RestControllerAdvice` partner-service, puis éventuelle unification du filtre et mise à jour de ses tests.
- Exécution de `mvn test -pl security-hardening-starter,partner-service,messaging-service`.

## PARTIE C — NON COMMENCÉE

Les dépendances de test H2 et Kafka embarqué sont déjà déclarées dans `booking-service`, et la suite actuelle démarre déjà une application avec H2 mémoire. Les nouveaux tests `@SpringBootTest(webEnvironment = RANDOM_PORT)`/`@EmbeddedKafka`, le test Gateway éventuel et le test HTTP partenaire/messaging ne sont pas encore écrits.

## Verdict actuel

Points restants : Partie B.1/B.2/B.3 complète et Partie C complète. Ne pas déclarer la consolidation terminée avant ces validations.
