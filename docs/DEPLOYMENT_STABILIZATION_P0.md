# Stabilisation P0 du déploiement Yeyamo Backend

## Périmètre et garde-fous

Audit et correction locaux uniquement. Aucun déploiement, accès VPS, `docker compose up/down`, suppression de volume, migration, publication d’image, commit ou push n’a été effectué.

Les services, contrats REST/Kafka/interservices, ports applicatifs, secrets, tokens, mots de passe et règles métier sont conservés. Le fichier local reste volontairement sans profils afin de ne pas casser le workflow de développement; la stratégie progressive est appliquée à `docker-compose.production.yml`, utilisé par Dokploy.

## Problèmes confirmés

1. La composition production ne déclarait aucun profil : un démarrage simple activait tous les microservices.
2. La surcharge PostgreSQL production utilisait `!reset` avec une valeur non vide; la configuration résolue ne montait donc pas `postgres-data`.
3. Redpanda n’avait aucun volume persistant déclaré.
4. Les images production basées sur `deploy/service.Dockerfile` installent `curl`, tandis que les healthchecks Compose de Config Server et Registry invoquaient `wget`.
5. Le budget Java de `512M` et `1 CPU` était appliqué uniformément, sans limitation CPU adaptée au VPS.
6. Le total des limites déclarées pour le profil ADVANCED complet dépasse la marge sûre d’un VPS de 12 Gio; il doit être activé par sous-ensemble de services.

## Corrections réalisées

### Profils production

Tous les services production sont maintenant explicitement associés à un profil. Les services sans profil ont été supprimés du modèle production résolu : il faut donc définir `COMPOSE_PROFILES` dans Dokploy.

| Profil | Services applicatifs | Infrastructure activée |
|---|---|---|
| `core` | `api-gateway`, `auth-service`, `user-service` | PostgreSQL, Redis, Redpanda, Config Server, Registry |
| `discovery` | `place-service`, `country-config-service`, `catalog-service`, `discovery-service`, `media-service` | Core infrastructure + OpenSearch |
| `social` | `content-service`, `feed-service`, `interaction-service`, `messaging-service`, `notification-service` | Core infrastructure |
| `experience` | `event-service`, `booking-service`, `ticket-service`, `payment-service`, `culture-service`, `partner-service`, `commerce-service` | Core infrastructure |
| `advanced` | `admin-service`, `campaign-service`, `ads-delivery-service`, `analytics-service`, `recommendation-service`, `gamification-service`, `graph-service`, `mission-reward-service`, `referral-service`, `moderation-trust-service`, `support-service`, `ingestion-service` | Core infrastructure + OpenSearch + Neo4j |

Les dépendances réelles restent celles du Compose local : chaque profil inclut les prérequis nécessaires (`config-server`, `registry-service`, `postgres-init`, `kafka`, et `redis` lorsque requis). `OpenSearch` n’est activé que pour Discovery/Advanced; `Neo4j` uniquement pour Advanced.

Sans profil, `docker compose ... config --services` ne retourne aucun service activable. C’est volontaire : Dokploy doit utiliser `COMPOSE_PROFILES=core` ou une sélection explicite de services.

### Persistance

- Volumes existants conservés : `postgres-data`, `redis-data`, `neo4j-data`, `opensearch-data`, `media-storage`.
- Nouveau volume : `redpanda-data:/var/lib/redpanda/data`.
- Le chemin Redpanda correspond au chemin de données documenté dans le [quickstart officiel Redpanda 24.3](https://docs.redpanda.com/streaming/24.3/get-started/quick-start/).
- Le volume Redpanda est nouveau : il ne restaure pas les messages historiques absents avant cette correction.
- La surcharge production PostgreSQL utilise maintenant `!override` avec `postgres-data`, ce qui conserve le montage persistant tout en supprimant le montage d’initialisation local.

### Ressources

Les valeurs Java existantes sont préservées : `-Xms64m -Xmx256m`, avec une limite conteneur par défaut de `512M`. La limite CPU Java production passe de `1.0` à `0.5` par JVM; elle reste configurable par `JAVA_CPU_LIMIT`.

| Composant | Limite mémoire | Limite CPU | Justification |
|---|---:|---:|---|
| JVM applicative | 512M | 0.5 | Heap 256M conservé; marge native et contention réduite |
| Config Server / Registry | 512M | 0.5 | Bootstrap Spring et Eureka, faible charge permanente |
| PostgreSQL | 1024M | 1.0 | Base multi-schémas; ne pas descendre sans métriques |
| PostgreSQL init | 256M | 0.25 | Job ponctuel |
| Redis | 256M | 0.25 | Cache et files légères |
| Redpanda | 768M | 1.0 | Limite cohérente avec `--memory 512M` et mémoire native |
| Kafka init | 128M | 0.25 | Création idempotente de topics |
| OpenSearch | 768M | 1.0 | Heap existant 512M + mémoire native |
| Neo4j | 768M | 0.75 | Heap existant 512M + mémoire native |

Les limites déclarées par profil sont environ : `core` 4.88 Gio, `discovery` 6.63 Gio, `social` 5.88 Gio, `experience` 6.88 Gio, `advanced` 10.88 Gio. Avec une réserve opérationnelle de 2.5 Gio pour Linux, Docker, Dokploy, Traefik et les pics, Advanced complet n’est pas recommandé sur 12 Gio. Il doit être lancé par sous-ensemble explicite, par exemple Analytics seul ou Graph seul.

### PostgreSQL et pools

Constats conservés sans modification destructive :

- `max_connections=300` et `shared_buffers=256MB` restent inchangés faute de métriques VPS.
- La majorité des services Compose fixe Hikari à `maximumPoolSize=5`, `minimumIdle=0`.
- `country-config-service` utilise `10/2`.
- `ticket-service` utilise `20/5`, identifié comme exception à surveiller.
- Les autres services peuvent utiliser les défauts Spring/Hikari provenant de leur configuration distante.

Valeur de travail recommandée après observation du profil réellement actif : viser environ 180 connexions PostgreSQL et `shared_buffers` autour de 192MB, puis ajuster avec les métriques de connexions, latence et OOM. Cette proposition n’est pas appliquée dans ce patch pour éviter une régression non vérifiée.

### Healthchecks et exposition

- Config Server et Registry utilisent maintenant `curl`, déjà installé dans l’image de déploiement.
- Les endpoints readiness, intervalles, timeouts, retries et périodes de démarrage sont inchangés.
- La production ne publie pas les ports des bases, brokers, Config Server ou Registry vers l’hôte; seul `api-gateway` est exposé au réseau Dokploy via `expose`.
- Les healthchecks d’image Spring Boot restent non authentifiés sur l’endpoint actuator readiness, ce qui est adapté au contrôle interne mais ne prouve pas l’accès Config authentifié.

## Procédure Dokploy recommandée

1. Renseigner `COMPOSE_PROFILES=core` dans l’environnement Dokploy; ne pas afficher les autres secrets lors des contrôles.
2. Valider le rendu avec `docker compose -f docker-compose.production.yml config --quiet`.
3. Démarrer le profil Core uniquement.
4. Vérifier Config Server, Registry, PostgreSQL, Redis, Redpanda, Gateway, Auth et User.
5. Ajouter un profil après l’autre; pour Advanced, sélectionner les services explicitement au lieu de démarrer tout le groupe.
6. Contrôler l’état des healthchecks, la mémoire RSS, le swap, les connexions PostgreSQL et les logs JVM avant d’élargir le périmètre.

Exemples de sélection, à exécuter uniquement dans Dokploy après validation manuelle :

```text
COMPOSE_PROFILES=core
docker compose -f docker-compose.production.yml up -d

COMPOSE_PROFILES=discovery
docker compose -f docker-compose.production.yml up -d place-service catalog-service discovery-service

COMPOSE_PROFILES=advanced
docker compose -f docker-compose.production.yml up -d analytics-service
```

Ces commandes sont documentaires et n’ont pas été exécutées ici.

## Sauvegarde préalable

À préparer avant redéploiement, sans utiliser `down -v` :

- PostgreSQL : sauvegarder les rôles avec `pg_dumpall --globals-only`, puis chaque base avec `pg_dump -Fc`; vérifier les listes `pg_restore --list` et les sommes SHA-256.
- Redis : produire un RDB ou une copie cohérente du volume selon la politique de persistance; vérifier le fichier hors du VPS.
- Neo4j : utiliser `neo4j-admin database dump` avec la base arrêtée ou en fenêtre cohérente.
- OpenSearch : utiliser un repository snapshot vérifié, pas une copie brute pendant les écritures.
- Media : copier `media-storage` vers un stockage externe avec vérification des fichiers.
- Redpanda : sauvegarder le volume hors ligne ou utiliser une stratégie Redpanda adaptée; ne jamais prétendre restaurer les messages historiques non sauvegardés.

Aucune sauvegarde n’a été exécutée par Codex.

## Rollback

En cas de régression : remettre la version précédente de `docker-compose.production.yml` et de l’environnement Dokploy, conserver les volumes nommés, désactiver le profil ajouté et redémarrer uniquement le périmètre déjà validé. Ne pas supprimer les volumes et ne pas recréer PostgreSQL avec un autre nom de volume.

## Validations

Réussies après correction :

- `docker compose -f docker-compose.production.yml config --quiet`.
- Validation des profils `core`, `discovery`, `social`, `experience` et `advanced`.
- Vérification de l’absence de service activable sans profil.
- Vérification du montage `postgres-data` résolu vers `/var/lib/postgresql/data`.
- Vérification du montage Redpanda et de la déclaration `redpanda-data`.
- Vérification de la présence des volumes persistants conservés.
- Audit statique des healthchecks et des Dockerfiles de déploiement.
- `git diff --check`.

Non exécutées :

- Build Docker réel : daemon Docker Linux indisponible.
- Démarrage complet ou partiel : explicitement interdit par la mission.
- Test de charge, mesure RSS/swap, métriques Hikari/PostgreSQL et restauration réelle : nécessite le VPS ou un environnement de préproduction.
- Déploiement Dokploy : non effectué.

## Risques résiduels

- Les limites sont des plafonds Docker, pas une mesure de consommation réelle.
- Le profil Advanced complet reste trop proche de la capacité du VPS; l’activation par sous-ensemble est obligatoire tant que les métriques ne sont pas disponibles.
- Les valeurs Hikari et PostgreSQL n’ont pas été abaissées sans observation de production.
- Le nouveau volume Redpanda protège les nouveaux déploiements, mais ne récupère aucune donnée historique déjà perdue.
- Le succès des healthchecks ne remplace pas une vérification fonctionnelle des dépendances et de l’authentification Config Server.
