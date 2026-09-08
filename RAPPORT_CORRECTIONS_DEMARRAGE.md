# Rapport de correction des démarrages

## Résumé exécutif

Les corrections ciblées de JPA, SQL PostgreSQL, permissions de fichiers et alignement Hibernate ont été appliquées sans Docker, déploiement, commit ni push.

La compilation Maven ciblée est verte. Un blocage structurel subsiste dans `ticket-service` : son renommage V1 → V4 ne rend pas les migrations exécutables, car V1 et V4 créent les mêmes tables et V2 dépend d'une table créée par V4. Ce point est volontairement non réécrit sans l'historique Flyway/une base de référence.

## Services corrigés

| Service | Cause racine | Correction | Risque résiduel |
|---|---|---|---|
| ads-delivery-service | Lettuce pouvait être masqué par une dépendance explicite de test. | Audit : aucune dépendance `lettuce-core` explicite en test ne subsiste. | Aucun changement de code. |
| analytics-service | Ancien doublon Flyway V2. | Renommage déjà présent dans l’arbre : `V2__analytics_rebuild_jobs.sql` → `V5__analytics_rebuild_jobs.sql`. | À valider contre l'historique Flyway d’une base déjà existante. |
| gamification-service | Ancien doublon Flyway V2. | Renommage déjà présent : `V2__culture_actions_and_badges.sql` → `V3__culture_actions_and_badges.sql`. | Même réserve Flyway historique. |
| mission-reward-service | Ancien doublon Flyway V2. | Renommage déjà présent : `V2__gamification_admin_definitions.sql` → `V3__gamification_admin_definitions.sql`. | Même réserve Flyway historique. |
| commerce-service | Le scan JPA ne couvrait pas `messaging`, donc `CommerceOutboxRepository` n’était pas créé. | `ArtworkCommerceJpaConfig` scanne le package racine `com.yeyamo_mobile.api.commerce_service`. | Aucun scan JPA doublé ajouté. |
| culture-service | Le scan JPA ne couvrait pas `infrastructure.outbox`, donc `CultureOutboxRepository` n’était pas créé. | `JpaConfig` scanne `com.yeyamo_mobile.api.culture_service`. | Aucun scan JPA doublé ajouté. |
| content-service | Les colonnes géographiques sont `DECIMAL`, le domaine Java est `Double`. | Nouvelle migration V6 convertissant les coordonnées de `content_posts` et `stories` en `DOUBLE PRECISION`. | Exécution PostgreSQL à faire au déploiement. |
| event-service | Même divergence `DECIMAL` / `Double`. | Nouvelle migration V7 convertissant `events.latitude` et `events.longitude`. | Exécution PostgreSQL à faire au déploiement. |
| country-config-service | Deux inserts `cities` avaient sept colonnes déclarées pour huit valeurs. | Ajout de `population` dans les insertions Bafoussam/Foumban et Ngaoundéré. | Le fichier ne contient pas de séquences `Ã` ou `Â` selon le contrôle `rg`; aucune réécriture d'encodage n’a été faite. |
| media-service | `ADD CONSTRAINT IF NOT EXISTS` n'est pas valide en PostgreSQL. | V2 supprime la contrainte nommée si elle existe, puis la recrée. | V2 est corrigée directement car elle ne pouvait pas réussir avec sa syntaxe initiale. |
| moderation-trust-service | Les identifiants culturels SQL UUID sont mappés en `String`. | Nouvelle V5 convertissant les IDs culturels et les FK de scopes reviewers en `VARCHAR(36)`. | Les tables/modèles de modération classique déjà en UUID ne sont pas modifiés. |
| partner-service | L'utilisateur `spring` ne peut pas créer `/app/data/partner-documents`. | Dockerfile : création de `/app/data` et propriété `spring:spring` avant `USER spring`. | Aucun volume de production ne monte `/app/data`; la persistance n’est donc pas retirée ou modifiée. |
| payment-service | Secret webhook absent/trop court au runtime. | Audit uniquement : validation Java `>= 24` conservée, compose conserve `${PAYMENT_WEBHOOK_SECRET:-}`, exemple reste vide. | Configuration Dokploy obligatoire. |

## Ticket-service — BLOQUÉ

Le renommage annoncé vers `V4__create_ticket_tables.sql` est présent, mais il n’est pas cohérent :

- `V1__create_base_schema.sql` crée déjà `ticket_sale_configurations`, `ticket_types`, `ticket_holds`, `ticket_orders`, `tickets`, `ticket_qr_credentials`, `event_staff_assignments` et `ticket_scans`.
- `V4__create_ticket_tables.sql` recrée ces mêmes tables avec des IDs UUID, et crée `ticket_outbox`.
- `V2__ticket_reliability.sql` exécute `ALTER TABLE ticket_outbox`, donc dépend du schéma déplacé en V4 tout en étant exécutée avant V4.
- Les entités JPA actuelles (`TicketSaleConfigurationEntity`, `TicketOrderEntity`, `TicketOutboxEntity`) utilisent des IDs UUID et correspondent au schéma de l’ancien `create_ticket_tables`, pas au V1 historique en `VARCHAR`.

Une correction sûre exige de savoir quel V1 a été appliqué dans les bases existantes. Réordonner, supprimer ou remplacer ces migrations sans cette preuve modifierait l’historique Flyway et peut rendre une base existante incohérente. Aucune modification Ticket supplémentaire n’a donc été appliquée.

## Migrations renommées ou créées

### Renommages déjà présents à l’audit

- analytics : `V2__analytics_rebuild_jobs.sql` → `V5__analytics_rebuild_jobs.sql`
- gamification : `V2__culture_actions_and_badges.sql` → `V3__culture_actions_and_badges.sql`
- mission-reward : `V2__gamification_admin_definitions.sql` → `V3__gamification_admin_definitions.sql`
- ticket : `V1__create_ticket_tables.sql` → `V4__create_ticket_tables.sql` (**non validé ; voir blocage**)

### Nouvelles migrations créées

- `content-service/.../V6__fix_geography_coordinate_types.sql`
- `event-service/.../V7__fix_event_geography_coordinate_types.sql`
- `moderation-trust-service/.../V5__align_cultural_identifier_types.sql`

## Contrôle Flyway final

| Service | Versions | Doublon de version |
|---|---|---|
| analytics-service | 1, 2, 3, 4, 5 | non |
| gamification-service | 1, 2, 3 | non |
| mission-reward-service | 1, 2, 3 | non |
| ticket-service | 1, 2, 3, 4 | non, mais ordre SQL incohérent |
| content-service | 1, 2, 3, 4, 5, 6 | non |
| event-service | 1, 2, 3, 4, 5, 6, 7 | non |
| country-config-service | 1, 2, 3, 4, 5, 6, 7 | non |
| media-service | 1, 2 | non |
| moderation-trust-service | 1, 2, 3, 4, 5 | non |

## Validation

### Lettuce

Commande exécutée :

```text
mvn -B dependency:tree -pl ads-delivery-service '-Dincludes=io.lettuce:lettuce-core'
```

Résultat : `io.lettuce:lettuce-core:7.5.2.RELEASE:compile`, via `spring-boot-starter-data-redis`. Build SUCCESS.

### Compilation Maven ciblée

Commande exécutée :

```text
mvn -B -pl culture-service,content-service,event-service,country-config-service,media-service,moderation-trust-service,partner-service,ads-delivery-service -am test -DskipTests
```

Résultat : BUILD SUCCESS. Les modules `partner-service`, `ads-delivery-service`, `culture-service`, `country-config-service`, `media-service`, `content-service`, `moderation-trust-service` et `event-service` compilent, ainsi que leurs dépendances de reactor. `commerce-service` a aussi compilé avec succès via sa commande ciblée.

### Tests réels

Une commande de tests réels groupés a été démarrée puis interrompue après plus de quatre minutes sans sortie supplémentaire exploitable. Elle ne constitue pas un succès de test. La compilation est validée ; les tests restent à relancer dans un environnement Maven non bloqué.

### Contrôles statiques

- `git diff --check` : succès (code retour 0).
- Recherche de motifs de secrets dans les fichiers suivis : aucune valeur ajoutée n’a été affichée ni écrite par cette intervention.
- Aucun commit, push, Docker ou déploiement exécuté.

## Actions manuelles Dokploy requises

1. Définir `PAYMENT_WEBHOOK_SECRET` avec au moins 24 caractères avant le démarrage de `payment-service`.
2. Fournir les autres secrets obligatoires déjà référencés par `docker-compose.production.yml` via Dokploy ; aucune valeur ne doit être ajoutée au dépôt.
3. Pour `ticket-service`, examiner la table `flyway_schema_history` de chaque environnement et choisir le schéma initial canonique avant de réécrire la séquence V1–V4.
4. Relancer les tests Maven complets dans un environnement où les suites Spring peuvent terminer.

## PRÊT POUR PUSH : NON

Blockers :

1. `ticket-service` contient une séquence Flyway logiquement impossible, non résolue sans l’historique de migration des bases existantes.
2. Les tests Maven réels du groupe n’ont pas terminé ; seule la compilation ciblée est validée.

