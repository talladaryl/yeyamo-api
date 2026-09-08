# Rapport — alignement JPA de `ticket-service`

Date : 2026-09-08  
Périmètre : `ticket-service` uniquement. Aucun Docker, commit, push ou déploiement n'a été exécuté.

## Statut

**RÉSOLU.** Le démarrage de l'application avec Flyway actif puis
`spring.jpa.hibernate.ddl-auto=validate` est vert sur une base H2 vide. La
persistance active ne contient plus de double mapping String/UUID.

## 1. Audit des entités et des mappings

### Ancien modèle — exclu du scan JPA par défaut

| Classe | Table | ID Java | Repository historique | Usage constaté | Statut |
|---|---|---:|---|---|---|
| `domain.model.TicketSaleConfiguration` | `ticket_sale_configurations` | `String` | `TicketSaleConfigurationRepository` | ancien `TicketService` | hors scan par défaut |
| `domain.model.TicketType` | `ticket_types` | `String` | `TicketTypeRepository` | ancien `TicketService` / inventaire | hors scan par défaut |
| `domain.model.TicketHold` | `ticket_holds` | `String` | `TicketHoldRepository` | ancien `TicketService` / inventaire | hors scan par défaut |
| `domain.model.TicketOrder` | `ticket_orders` | `String` | `TicketOrderRepository` | ancien `TicketService` | hors scan par défaut |
| `domain.model.Ticket` | `tickets` | `String` | `TicketRepository` | ancien `TicketService` / scan | hors scan par défaut |
| `domain.model.TicketQrCredential` | `ticket_qr_credentials` | `String` | `TicketQrCredentialRepository` | ancien `TicketService` | hors scan par défaut |
| `domain.model.EventStaffAssignment` | `event_staff_assignments` | `String` | `EventStaffAssignmentRepository` | ancien `TicketService` | hors scan par défaut |
| `domain.model.TicketScan` | `ticket_scans` | `String` | `TicketScanRepository` | ancien `TicketScanService` | hors scan par défaut |
| `domain.model.OutboxEvent` | `outbox_events` | `String` | `OutboxEventRepository` | ancien `OutboxService` | hors scan par défaut ; table retirée de V1 |

Les composants qui instancient encore ce graphe historique sont protégés par
`@Profile("legacy-ticket-api")` : `TicketService`, `TicketInventoryService`,
`TicketScanService`, `OutboxService`, `PartnerTicketController` et le
`UserTicketController` historique. Ce profil n'est pas un parcours supporté
par le schéma UUID V1–V3 ; il ne fait donc pas partie du démarrage normal.

### Modèle canonique — seul modèle JPA actif

| Classe | Table | ID Java | Repository |
|---|---|---:|---|
| `TicketSaleConfigurationEntity` | `ticket_sale_configurations` | `UUID` | `SpringTicketSaleConfigurationRepository` |
| `TicketTypeEntity` | `ticket_types` | `UUID` | `SpringTicketTypeRepository` |
| `TicketHoldEntity` | `ticket_holds` | `UUID` | `SpringTicketHoldRepository` |
| `TicketOrderEntity` | `ticket_orders` | `UUID` | `SpringTicketOrderRepository` |
| `TicketEntity` | `tickets` | `UUID` | `SpringTicketRepository` |
| `TicketQrCredentialEntity` | `ticket_qr_credentials` | `UUID` | `SpringTicketQrCredentialRepository` |
| `EventStaffAssignmentEntity` | `event_staff_assignments` | `UUID` | `SpringEventStaffAssignmentRepository` |
| `TicketScanEntity` | `ticket_scans` | `UUID` | `SpringTicketScanRepository` |
| `TicketOutboxEntity` | `ticket_outbox` | `UUID` | `SpringOutboxRepository` |
| `TicketProcessedEvent` | `ticket_processed_events` | `UUID` | `TicketProcessedEventRepository` |

Tables anciennement mappées deux fois : `ticket_sale_configurations`,
`ticket_types`, `ticket_holds`, `ticket_orders`, `tickets`,
`ticket_qr_credentials`, `event_staff_assignments`, `ticket_scans`.

Preuve statique : les annotations concurrentes existent encore dans le code
historique, mais `TicketServiceApplication.java:22-30` limite explicitement
le scan aux classes canoniques. Le démarrage de test confirme :

```text
Finished Spring Data repository scanning in 951 ms. Found 10 JPA repository interfaces.
```

Les neuf repositories historiques ne sont donc pas enregistrés dans le
contexte par défaut.

## 2. Architecture retenue et traitement

`TicketServiceApplication` importe le `EntityScan` de Spring Boot 4
(`org.springframework.boot.persistence.autoconfigure.EntityScan`) et définit :

```java
@EntityScan(basePackageClasses = {
    TicketSaleConfigurationEntity.class,
    TicketOutboxEntity.class,
    TicketProcessedEvent.class
})
@EnableJpaRepositories(basePackageClasses = {
    SpringTicketSaleConfigurationRepository.class,
    SpringOutboxRepository.class,
    TicketProcessedEventRepository.class
})
```

Cette approche est additive : les classes historiques ne sont pas supprimées
sans migration de leurs usages, mais elles ne peuvent plus concurrencer le
modèle UUID dans le contexte normal.

| Fonction métier | Repositories/entités utilisés par le parcours courant | Modèle |
|---|---|---|
| configuration de vente et types | `SpringTicketSaleConfigurationRepository`, `SpringTicketTypeRepository` | UUID canonique |
| hold / inventaire | `SpringTicketHoldRepository`, `TicketHoldEntity` | UUID canonique |
| commande et mobile money | `SpringTicketOrderRepository`, `TicketOrderEntity` | UUID canonique |
| émission de ticket / QR | `SpringTicketRepository`, `SpringTicketQrCredentialRepository` | UUID canonique |
| scan QR et staff | `SpringTicketScanRepository`, `SpringEventStaffAssignmentRepository` | UUID canonique |
| publication fiable | `SpringOutboxRepository`, `TicketOutboxEntity` | UUID canonique |

Deux tests d'intégration uniquement liés aux services String historiques ont
été retirés : `TicketInventoryConcurrencyTest` et
`TicketScanDoubleScanTest`. Les tests du parcours canonique restent présents
et s'exécutent dans la suite (`InventoryServiceTest`,
`OrderServiceMobileMoneyTest`, test Kafka, QR et contrôleurs).

## 3. Cas `outbox_events`

`outbox_events` était exclusivement reliée à `domain.model.OutboxEvent`, son
repository et son `OutboxService` historiques. Le mécanisme actuel est
`ticket_outbox` via `TicketOutboxEntity` / `SpringOutboxRepository`.

La création de `outbox_events` a donc été retirée de
`V1__create_ticket_tables.sql`. La base de production ayant été confirmée
vide, aucun script correctif de suppression n'est nécessaire. V1 crée
désormais exactement les neuf tables canoniques, dont `ticket_outbox`.

## 4. Vérifications exécutées

### Flyway puis Hibernate `validate`

Test ajouté :
`ticket-service/src/test/java/com/yeyamo_mobile/api/ticket_service/FlywaySchemaValidationTest.java`.
Il démarre une base H2 vide avec :

```text
spring.flyway.enabled=true
spring.jpa.hibernate.ddl-auto=validate
```

Extrait brut de `mvn -B -pl ticket-service -am clean test` :

```text
Successfully validated 3 migrations (execution time 00:00.070s)
Migrating schema "PUBLIC" to version "1 - create ticket tables"
Migrating schema "PUBLIC" to version "2 - ticket reliability"
Migrating schema "PUBLIC" to version "3 - add mobile money checkout fields"
Successfully applied 3 migrations to schema "PUBLIC", now at version v3 (execution time 00:00.324s)
Initialized JPA EntityManagerFactory for persistence unit 'default'
Started FlywaySchemaValidationTest in 50.801 seconds
```

Aucune `SchemaManagementException` ni divergence UUID/VARCHAR n'a été
produite.

### Suite Maven obligatoire

Commande :

```text
mvn -B -pl ticket-service -am clean test
```

Sortie finale brute :

```text
security-hardening-starter 1.0.0-SNAPSHOT .......... SUCCESS [ 26.260 s]
ticket-service 0.0.1-SNAPSHOT ...................... SUCCESS [01:48 min]
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time:  02:19 min
```

Les avertissements restants (Lombok `@Builder`, API QR/Kafka dépréciées,
nettoyage de fichier temporaire Kafka sous Windows) n'ont entraîné aucun
échec de compilation ou de test et sont hors de cet écart JPA.

### Contrôle statique

Commande :

```text
git diff --check
```

Résultat : code de sortie `0`. Les seuls messages affichés sont des
avertissements CRLF portant aussi sur des fichiers hors périmètre ; aucune
erreur de whitespace n'a été signalée.

## 5. Fichiers modifiés dans ce lot

- `ticket-service/src/main/java/.../TicketServiceApplication.java` : scan
  JPA/repositories explicite du modèle canonique UUID.
- `ticket-service/src/main/java/.../application/service/{TicketService,TicketInventoryService,TicketScanService,OutboxService}.java` et
  `presentation/controller/PartnerTicketController.java` : profil historique
  explicite.
- `ticket-service/src/main/resources/db/migration/V1__create_ticket_tables.sql` : suppression du reliquat `outbox_events`.
- `ticket-service/src/test/java/.../FlywaySchemaValidationTest.java` : preuve
  automatique Flyway + Hibernate validate.
- Les deux tests spécifiques au graphe historique String ont été supprimés,
  car ce graphe n'est plus une persistance active.

## Blockers

Aucun pour le démarrage et les parcours canoniques de `ticket-service`.

TICKET_JPA_READY=YES
