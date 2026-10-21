# Rapport P0 — Authentification Registry Service ↔ Config Server

## Résultat

La propagation des credentials Config Server est maintenant explicite et cohérente entre le serveur, `registry-service` et les autres clients Spring Cloud Config.

Aucun déploiement, push, commit, accès VPS ou publication d’image n’a été effectué.

## Cause racine

La configuration utilisait plusieurs conventions d’environnement : les applications lisaient `CONFIG_SERVER_USERNAME/PASSWORD`, tandis que la convention Spring Cloud Config attend aussi `SPRING_CLOUD_CONFIG_USERNAME/PASSWORD`. En parallèle, le Compose local contenait un couple de credentials statique et le Config Server possédait le même défaut codé en dur.

Cette configuration pouvait fonctionner lorsque Dokploy injectait exactement les variables attendues, mais elle ne garantissait pas une chaîne bootstrap unique et fail-fast. Les logs VPS n’étant pas disponibles, une ancienne réponse HTTP 401 ne peut pas être attribuée à une valeur Dokploy précise; la fragilité de propagation est toutefois confirmée par l’audit.

## Corrections appliquées

### Diff fonctionnel exact

```diff
- spring.security.user.name=${CONFIG_SERVER_USERNAME:<default-statique>}
- spring.security.user.password=${CONFIG_SERVER_PASSWORD:<default-statique>}
+ spring.security.user.name=${CONFIG_SERVER_USERNAME}
+ spring.security.user.password=${CONFIG_SERVER_PASSWORD}
```

Le Config Server n’a plus de secret par défaut et échoue explicitement si ses credentials ne sont pas injectés.

```diff
- spring.cloud.config.username=${CONFIG_SERVER_USERNAME}
- spring.cloud.config.password=${CONFIG_SERVER_PASSWORD}
+ spring.cloud.config.username=${SPRING_CLOUD_CONFIG_USERNAME:${CONFIG_SERVER_USERNAME}}
+ spring.cloud.config.password=${SPRING_CLOUD_CONFIG_PASSWORD:${CONFIG_SERVER_PASSWORD}}
```

`registry-service` accepte la convention Spring canonique avec repli compatible sur la convention historique.

Les ancres Compose locale et production injectent désormais :

```yaml
CONFIG_SERVER_URL: "${CONFIG_SERVER_URL:-http://config-server:8080}"
CONFIG_SERVER_USERNAME: "${CONFIG_SERVER_USERNAME:?CONFIG_SERVER_USERNAME is required}"
CONFIG_SERVER_PASSWORD: "${CONFIG_SERVER_PASSWORD:?CONFIG_SERVER_PASSWORD is required}"
SPRING_CLOUD_CONFIG_USERNAME: "${CONFIG_SERVER_USERNAME:?CONFIG_SERVER_USERNAME is required}"
SPRING_CLOUD_CONFIG_PASSWORD: "${CONFIG_SERVER_PASSWORD:?CONFIG_SERVER_PASSWORD is required}"
```

Les services locaux qui redéfinissaient le couple statique utilisent maintenant les mêmes variables obligatoires. Les variables `EUREKA_USERNAME/PASSWORD`, qui authentifient Registry/Eureka et non Config Server, restent séparées et inchangées fonctionnellement.

Les deux exemples `.env` documentent `CONFIG_SERVER_URL`, `CONFIG_SERVER_USERNAME` et `CONFIG_SERVER_PASSWORD` sans valeur secrète réelle.

## Audit Docker

- `deploy/service.Dockerfile` et `deploy/runtime.Dockerfile` utilisent une syntaxe Dockerfile valide : `HEALTHCHECK ... CMD ...`, jamais `CMD-SHELL`.
- `curl` est installé dans les images runtime avant le passage à `USER spring`.
- `SERVER_PORT` est défini puis utilisé par `EXPOSE` et le healthcheck.
- Les healthchecks des Dockerfiles de déploiement ciblent `/actuator/health/readiness` et conservent leurs intervalles, timeouts, périodes de démarrage et retries.
- La recherche de tous les Dockerfiles ne trouve aucune instruction `HEALTHCHECK` avec `CMD-SHELL`.
- Les entrypoints, ports, paramètres JVM, limites mémoire et contrats applicatifs n’ont pas été modifiés dans cette correction.

## Matrice d’environnement

| Variable | Usage | Obligatoire |
|---|---|---:|
| `CONFIG_SERVER_URL` | URL bootstrap des clients Config | Oui en déploiement, défaut Compose interne conservé |
| `CONFIG_SERVER_USERNAME` | Basic Auth du Config Server et repli client | Oui |
| `CONFIG_SERVER_PASSWORD` | Basic Auth du Config Server et repli client | Oui |
| `SPRING_CLOUD_CONFIG_USERNAME/PASSWORD` | Convention canonique du client Config | Injectées depuis `CONFIG_SERVER_*` |
| `CONFIG_GIT_URI` | Backend Git du Config Server en production | Oui en production |
| `CONFIG_GIT_USERNAME/PASSWORD` | Authentification Git du Config Server | Selon le dépôt Git |
| `EUREKA_USERNAME/PASSWORD` | Authentification Registry/Eureka | Oui en production |

## Validations

Réussies :

- `docker compose -f docker-compose.yml config --quiet`
- `docker compose -f docker-compose.production.yml config --quiet`
- `docker compose -f docker-compose.production.yml -f docker-compose.production.images.yml config --quiet`
- Vérification de rendu production : credentials Config Server présents, identiques côté serveur/client, URL `http://config-server:8080`, 34 services clients contrôlés, aucun client sans variables canoniques.
- `mvn -B -ntp -pl config-server -am test` — succès.
- `mvn -B -ntp -pl registry-service -am test` — succès avec cache Maven temporaire isolé.
- `git diff --check` — succès.
- Audit textuel de tous les Dockerfiles — aucune occurrence `CMD-SHELL`.

Non exécutées :

- Build Docker réel et exécution d’un healthcheck : daemon Docker Linux indisponible dans l’environnement local.
- Test d’intégration Registry → Config Server authentifié : nécessite deux conteneurs démarrés et les valeurs Dokploy réelles.
- Déploiement Dokploy/VPS : explicitement exclu de la mission.

## Procédure de validation recommandée

Dans Dokploy, renseigner les mêmes valeurs non vides pour `CONFIG_SERVER_USERNAME` et `CONFIG_SERVER_PASSWORD` sur Config Server et tous les clients, puis vérifier le rendu Compose avant redémarrage. Démarrer dans l’ordre Config Server, Registry, puis les autres services et contrôler les logs de bootstrap sans afficher les secrets.

## Risques résiduels

- Une valeur erronée ou absente dans Dokploy provoquera maintenant un échec explicite au lieu d’un fallback implicite.
- Le healthcheck Config Server vérifie sa readiness actuator, pas l’accès authentifié à une configuration applicative; ce contrôle reste à effectuer dans l’environnement d’exécution.
- La disponibilité du backend Git, de ses credentials et de la clé de chiffrement reste indépendante de cette correction.
