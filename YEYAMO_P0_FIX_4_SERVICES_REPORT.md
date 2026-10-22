# YEYAMO API — Rapport de correction P0 des 4 services

Date de l’audit : 2026-10-22

Périmètre : `notification-service`, `partner-service`, `ticket-service`, OpenSearch

## Synthèse

Les corrections sont limitées aux causes P0 identifiées. Aucun déploiement, accès VPS,
publication d’image, commit, push, suppression de volume ou modification métier n’a été
effectué.

| Composant | Verdict | Cause traitée |
|---|---|---|
| Notification / Flyway | **CORRIGÉ — VALIDATION PRODUCTION REQUISE** | Collision de clé primaire dans V8 avec l’UUID déjà utilisé par V3 |
| Partner / documents | **CORRIGÉ — VALIDATION PRODUCTION REQUISE** | Répertoire relatif sous `/app` non inscriptible par `spring`, sans volume persistant |
| Ticket / readiness | **CORRIGÉ ET TESTÉ** | `/actuator/health/readiness` n’était pas couvert par le matcher public |
| OpenSearch | **CORRIGÉ — VALIDATION PRODUCTION REQUISE** | Override de nom nul et heap trop proche de la limite mémoire |

## Corrections par fichier

### Notification

Cause racine confirmée : `V3__password_changed_email_template.sql` réserve
`10000000-0000-0000-0000-000000000018` à `user.password_changed` / `EMAIL`, alors que
V8 réutilisait exactement cet UUID pour `social.followed` / `IN_APP`. PostgreSQL échoue
donc sur la clé primaire avant que la clé métier `(event_type, channel, locale)` ne
puisse jouer son rôle.

Correction dans
`notification-service/src/main/resources/db/migration/V8__social_follow_notification_templates.sql` :

```diff
-('10000000-0000-0000-0000-000000000018','social.followed','IN_APP','fr',...)
-('10000000-0000-0000-0000-000000000019','social.followed','PUSH','fr',...);
+('80000000-0000-0000-0000-000000000001','social.followed','IN_APP','fr',...),
+('80000000-0000-0000-0000-000000000002','social.followed','PUSH','fr',...)
+ON CONFLICT (event_type, channel, locale) DO NOTHING;
```

Les deux lignes métier sont conservées. L’UUID `...018` de V3 n’est ni supprimé ni
réécrit. L’audit statique des migrations compte 30 UUID et ne trouve aucun doublon.

**Prérequis production obligatoire :** cette correction modifie le contenu de V8. Avant
toute exécution dans une base existante, vérifier l’historique Flyway et les données,
sans lancer `flyway clean` ni `flyway repair` automatiquement :

```sql
SELECT installed_rank, version, description, checksum, success
FROM flyway_schema_history
WHERE version IN ('3', '8')
ORDER BY installed_rank;

SELECT id, event_type, channel, locale
FROM notification_templates
WHERE id IN (
    '10000000-0000-0000-0000-000000000018',
    '10000000-0000-0000-0000-000000000019',
    '80000000-0000-0000-0000-000000000001',
    '80000000-0000-0000-0000-000000000002'
)
   OR (event_type = 'social.followed' AND channel IN ('IN_APP', 'PUSH'));
```

Si V8 est déjà marquée `success=true` dans un environnement, ne pas remplacer son
checksum silencieusement : appliquer une procédure Flyway approuvée par le DBA, avec
sauvegarde et vérification de la compatibilité de l’historique. Si V8 est la migration
qui a échoué en production, la reprise doit également suivre la procédure de gestion des
migrations échouées de l’équipe DBA. Aucune donnée existante n’est supprimée par le
correctif fourni.

### Partner

Le stockage actif est `FilesystemDocumentStorageAdapter`; aucun adaptateur R2/S3 n’est
configuré pour Partner. La valeur par défaut `./data/partner-documents` devient
`/app/data/partner-documents` dans l’image de déploiement. `deploy/service.Dockerfile`
créait l’utilisateur non-root mais pas ce répertoire, ce qui provoquait l’accès refusé.

Corrections :

- `deploy/service.Dockerfile` crée `/app/data` et le donne à `spring:spring`, sans
  utiliser root au runtime ni `chmod 777`.
- `docker-compose.production.yml` fixe explicitement
  `PARTNER_DOCUMENT_ROOT=/app/data/partner-documents`.
- Un volume nommé dédié `partner-documents:/app/data` est ajouté en production; aucun
  volume existant n’est supprimé ou réutilisé pour un autre service.
- Le contrôle de chemin existant contre la traversée (`startsWith(root)`) est conservé.

Le volume doit être nouveau ou vérifié avant reprise. Si un volume du même nom existe
déjà avec un propriétaire incorrect, l’équipe d’exploitation doit effectuer une
vérification et une correction de propriétaire contrôlée, avec sauvegarde préalable;
aucune commande destructive n’est fournie ni exécutée ici.

### Ticket

`SecurityConfig` autorisait `/actuator/health` mais pas les sous-chemins. Le healthcheck
commun cible `/actuator/health/readiness`, qui tombait donc dans
`.anyRequest().authenticated()` et renvoyait 401.

Correction dans
`ticket-service/src/main/java/com/yeyamo_mobile/api/ticket_service/infrastructure/config/SecurityConfig.java` :

```diff
-.requestMatchers("/actuator/health", "/actuator/info").permitAll()
+.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
```

Les autres endpoints Actuator restent protégés par `.anyRequest().authenticated()`.

### OpenSearch

L’image `opensearchproject/opensearch:3.2.0` et le volume
`opensearch-data:/usr/share/opensearch/data` sont conservés. L’override production
`container_name: !reset null` est remplacé par `container_name: yeyamo-opensearch`.

La limite production reste configurable et par défaut à `768M`, mais le heap par défaut
est aligné à 384 MiB (`-Xms384m -Xmx384m`) au lieu de 512 MiB. Cela laisse davantage de
mémoire native au processus OpenSearch et ne touche pas aux index persistés. Cette valeur
ne garantit pas à elle seule la capacité d’un workload de production : la charge et les
profils activés doivent être vérifiés sur l’hôte.

Avec environ 11 GiB disponibles pour l’ensemble de la plateforme, l’activation
simultanée de tous les profils ne peut pas être considérée comme garantie. Activer les
profils progressivement et conserver OpenSearch dans le profil réellement nécessaire.

## Diff fonctionnel exact

```diff
diff --git a/notification-service/src/main/resources/db/migration/V8__social_follow_notification_templates.sql b/notification-service/src/main/resources/db/migration/V8__social_follow_notification_templates.sql
@@
-('10000000-0000-0000-0000-000000000018','social.followed','IN_APP','fr','Nouvel abonné','Une personne vient de s’abonner à votre profil.'),
-('10000000-0000-0000-0000-000000000019','social.followed','PUSH','fr','Nouvel abonné YeYamo','Une personne vient de s’abonner à votre profil.');
+('80000000-0000-0000-0000-000000000001','social.followed','IN_APP','fr','Nouvel abonné','Une personne vient de s’abonner à votre profil.'),
+('80000000-0000-0000-0000-000000000002','social.followed','PUSH','fr','Nouvel abonné YeYamo','Une personne vient de s’abonner à votre profil.')
+ON CONFLICT (event_type, channel, locale) DO NOTHING;

diff --git a/ticket-service/src/main/java/com/yeyamo_mobile/api/ticket_service/infrastructure/config/SecurityConfig.java b/ticket-service/src/main/java/com/yeyamo_mobile/api/ticket_service/infrastructure/config/SecurityConfig.java
@@
-.requestMatchers("/actuator/health", "/actuator/info").permitAll()
+.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()

diff --git a/deploy/service.Dockerfile b/deploy/service.Dockerfile
@@
-    && useradd --system --uid 10001 --create-home spring
+    && useradd --system --uid 10001 --create-home spring \
+    && mkdir -p /app/data \
+    && chown -R spring:spring /app/data

diff --git a/docker-compose.production.yml b/docker-compose.production.yml
@@
-    container_name: !reset null
+    container_name: yeyamo-opensearch
+    environment:
+      OPENSEARCH_JAVA_OPTS: "${OPENSEARCH_JAVA_OPTS:--Xms384m -Xmx384m}"
@@
     <<: *private-service
+    environment:
+      <<: *production-java-env
+      PARTNER_DOCUMENT_ROOT: /app/data/partner-documents
+    volumes:
+      - partner-documents:/app/data
@@
   media-storage:
+  partner-documents:
```

## Dockerfiles et syntaxe Compose

- Tous les `HEALTHCHECK` trouvés dans les Dockerfiles utilisent maintenant la forme
  Dockerfile `CMD`; aucun `HEALTHCHECK.*CMD-SHELL` n’est présent.
- `deploy/service.Dockerfile:76` et `deploy/runtime.Dockerfile:20` installent effectivement
  `curl`, utilisent `SERVER_PORT` et ciblent `/actuator/health/readiness`.
- Le runtime reste non-root (`USER spring`). Une requête HTTP loopback ne nécessite pas
  de privilège particulier.
- `CMD-SHELL` reste correct dans Docker Compose, par exemple pour `healthcheck.test` sous
  forme ` ["CMD-SHELL", "..."]`; cette syntaxe n’est pas interchangeable avec
  l’instruction `HEALTHCHECK` d’un Dockerfile.
- Les ports, entrypoints, paramètres JVM, limites et intervalles/timeout/start-period/
  retries des healthchecks n’ont pas été modifiés.

## Tests et validations

### Réussis

- `git diff --check` : succès.
- Audit statique de tous les Dockerfiles : aucun `HEALTHCHECK` avec `CMD-SHELL`.
- Audit statique des UUID Flyway Notification : 30 UUID, aucun doublon.
- `docker compose -f docker-compose.yml config -q` avec variables de validation : succès.
- `docker compose -f docker-compose.production.yml config -q` avec variables de validation : succès.
- Configuration production résolue : `yeyamo-opensearch`, heap `-Xms384m -Xmx384m`,
  `partner-documents:/app/data`, `/app/data/partner-documents`.
- `mvn -B -ntp -pl notification-service,partner-service,ticket-service -am -DskipTests package` : succès.
- `mvn -B -ntp -pl partner-service -Dtest=FilesystemDocumentStorageAdapterTest test` : 1 test,
  0 échec.
- `mvn -B -ntp -pl ticket-service -Dtest=ActuatorSecurityTest test` : 1 test,
  0 échec; readiness 200 et `/actuator/metrics` anonyme 401.

### Non exécutés / limites

- Le daemon Docker Linux n’était pas disponible (`dockerDesktopLinuxEngine` absent).
- Aucun build Docker réel et aucun démarrage de conteneur n’a donc été exécuté.
- Aucune migration n’a été appliquée à une base PostgreSQL réelle.
- Le comportement de volume et la pression mémoire OpenSearch restent à confirmer sur
  l’environnement de production après revue des données et des profils activés.

## Procédure de déploiement recommandée — non exécutée

1. Sauvegarder les bases et vérifier les deux requêtes Flyway en lecture seule.
2. Vérifier que `partner-documents` n’existe pas déjà avec un contenu inattendu; ne pas
   supprimer de volume.
3. Valider la configuration avec les vraies variables obligatoires :

   ```bash
   docker compose --profile core --profile discovery --profile social \
     --profile experience --profile advanced \
     -f docker-compose.production.yml config -q
   ```

4. Avec un daemon Linux disponible, construire une image représentative :

   ```bash
   docker build --progress=plain \
     --build-arg SERVICE_MODULE=ticket-service \
     --build-arg SERVICE_PORT=8112 \
     -f deploy/service.Dockerfile .
   ```

5. Inspecter la configuration healthcheck de l’image puis tester uniquement le conteneur
   représentatif, sans lancer l’ensemble de la plateforme.
6. Déployer selon la procédure Dokploy approuvée, puis vérifier : readiness Ticket,
   écriture Partner, état Flyway Notification, nom/heap/health OpenSearch et absence de
   croissance anormale du GC.

## Rollback

- Revenir à l’image applicative précédente sans supprimer de volume ni de données.
- Ne pas restaurer V8 à sa version collisionnelle dans une base où V3 existe.
- Pour Notification, tout rollback de migration doit passer par le DBA et l’historique
  Flyway; ne pas utiliser `clean` ou `repair` comme automatisme.
- Conserver `partner-documents` et `opensearch-data` pendant toute opération de rollback.
- Si la heap OpenSearch doit être ajustée, modifier uniquement la variable contrôlée après
  observation de l’usage et du GC; ne pas supprimer les index.
