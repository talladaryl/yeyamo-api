# Rapport Flyway — ticket-service

## Statut

**BLOQUÉ pour une mise en production avec `ddl-auto=validate`.** La séquence
Flyway est reconstruite et s'applique de V1 à V3 sur une base vide, mais le
service conserve deux ensembles d'entités JPA actives qui mappent les mêmes
tables avec des types d'identifiants incompatibles (UUID et `String`).

## Preuve de base vierge

La preuve fournie pour `yeyamo_ticket` indique : `flyway_schema_history`
absent, `\dt` sans relation, et absence de `ticket_orders` et
`ticket_outbox`. Aucune connexion à la base ou au VPS n'a été effectuée dans
ce lot, conformément au périmètre.

## Ancien problème et correction appliquée

| Avant | Après |
|---|---|
| Deux schémas initiaux concurrents : `V1__create_base_schema.sql` (IDs VARCHAR) et l'ancien schéma UUID déplacé en V4. | Un seul schéma initial : `V1__create_ticket_tables.sql`, fondé sur le modèle UUID. |
| V2 modifiait `ticket_outbox`, créée trop tard en V4. | `ticket_outbox` est créée par V1, avant V2. |
| V3 ajoutait trois colonnes mobile money dans une seule instruction `ALTER TABLE`. | V3 conserve son rôle mais contient trois `ALTER TABLE` indépendants, portables dans le test H2. |

Fichiers modifiés :

- supprimé : `ticket-service/src/main/resources/db/migration/V1__create_base_schema.sql` ;
- reconstruit : `ticket-service/src/main/resources/db/migration/V1__create_ticket_tables.sql` ;
- ajusté : `ticket-service/src/main/resources/db/migration/V2__ticket_reliability.sql` ;
- ajusté : `ticket-service/src/main/resources/db/migration/V3__add_mobile_money_checkout_fields.sql`.

V1 crée, dans l'ordre nécessaire aux clés étrangères :
`ticket_sale_configurations`, `ticket_types`, `ticket_holds`,
`ticket_orders`, `tickets`, `ticket_qr_credentials`, `ticket_scans`,
`event_staff_assignments`, `ticket_outbox` et `outbox_events` (ce dernier est
maintenu car une entité historique active le mappe encore). La FK différée de
`ticket_scans.staff_assignment_id` est ajoutée après
`event_staff_assignments`.

Les ajustements de compatibilité SQL effectués pendant la validation sont
sans changement de contrat métier :

- `UUID DEFAULT gen_random_uuid() PRIMARY KEY` au lieu de l'ordre non
  portable `UUID PRIMARY KEY DEFAULT ...` ;
- index de hold sur `(status, expires_at)` au lieu d'un index partiel H2 non
  supporté ;
- index unique de scan sur `(scanner_user_id, offline_reference)`. PostgreSQL
  autorise plusieurs valeurs NULL dans un index unique : cela conserve le
  comportement attendu pour une référence hors-ligne non renseignée ;
- `TIMESTAMP WITH TIME ZONE` (type SQL standard) au lieu de l'alias
  PostgreSQL `TIMESTAMPTZ` dans V2.

## Séquence finale

| Version | Fichier | Rôle | Dépendances |
|---|---|---|---|
| V1 | `V1__create_ticket_tables.sql` | Schéma Ticket UUID complet, FKs, contraintes et index. | Base vide. |
| V2 | `V2__ticket_reliability.sql` | `ticket_processed_events`, tentatives/erreur de l'outbox, unicité scan. | `ticket_outbox` et `ticket_scans` créées en V1. |
| V3 | `V3__add_mobile_money_checkout_fields.sql` | `payment_operator`, `payment_phone_number`, `payment_country_code`. | `ticket_orders` créée en V1. |

Contrôle des versions effectué avec :

```text
V1__create_ticket_tables.sql
V2__ticket_reliability.sql
V3__add_mobile_money_checkout_fields.sql
```

Il ne reste aucune seconde migration V1 ni migration V4 contenant le schéma
initial.

## Alignement JPA / SQL

Les entités actuelles sous `infrastructure.persistence` et
`infrastructure.outbox` utilisent des IDs UUID ; V1/V2/V3 leur fournissent
les tables, champs mobile-money et `ticket_outbox` correspondants.

Cependant, le package `domain.model` est toujours automatiquement détecté
par JPA et contient des entités historiques utilisant `String` sur les mêmes
tables. Preuve runtime après migration réussie :

```text
Schema validation: wrong column type encountered in column [id] in table
[event_staff_assignments]; found [uuid (Types#BINARY)], but expecting
[varchar(255) (Types#VARCHAR)]
```

Ce résultat provient de la commande suivante, exécutée après `clean` :

```powershell
mvn -B -pl ticket-service -am clean test -Dtest=QrTokenSecurityTest \
  '-Dsurefire.failIfNoSpecifiedTests=false' \
  '-Dspring.flyway.enabled=true' \
  '-Dspring.jpa.hibernate.ddl-auto=validate'
```

La sortie a confirmé avant cet échec :

```text
Successfully validated 3 migrations
Successfully applied 3 migrations to schema "PUBLIC", now at version v3
```

Le schéma UUID ne doit pas être régressé vers VARCHAR : cela contredirait les
entités UUID utilisées par le parcours actuel. La résolution requiert de
retirer de l'exécution les entités/repositories/contrôleurs historiques ou de
les migrer complètement vers UUID ; ce refactoring dépasse la reconstruction
Flyway seule et n'a pas été masqué par une modification du schéma.

## Validation Maven et qualité

| Commande | Résultat |
|---|---|
| `mvn -B -pl ticket-service -am test -DskipTests` | **BUILD SUCCESS** ; `security-hardening-starter` et `ticket-service` compilés. |
| `mvn -B -pl ticket-service -am test` | **BUILD SUCCESS** ; 16 tests du starter et 19 tests Ticket, 0 échec, 0 erreur. |
| Test Flyway + Hibernate validate ci-dessus | Flyway V1, V2, V3 appliquées ; **échec attendu et prouvé** de validation sur le mapping JPA historique String. |
| `git diff --check` | Succès, aucune erreur d'espacement signalée. |

Les tests usuels utilisent `src/test/resources/application-test.properties`,
qui fixe `spring.flyway.enabled=false` et `ddl-auto=create-drop`; ils ne
valident donc pas les migrations par défaut. Le contrôle Flyway explicite
ci-dessus a été ajouté pour cette raison.

## Actions non effectuées

Aucun Docker, déploiement, connexion VPS, commit ou push Git n'a été effectué.
Les autres changements non liés présents dans le dépôt ont été préservés.

TICKET_FLYWAY_READY=NO
