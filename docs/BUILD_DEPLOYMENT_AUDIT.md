# Audit build, démarrage et disponibilité

Date de l’audit : 21 octobre 2026. Aucun déploiement, accès VPS, push ou publication d’image n’a été effectué.

## 1. Résumé exécutif

Le dépôt contient un reactor Maven de 39 modules, dont 34 applications Java actives dans Compose (Config Server, Eureka, Gateway et 31 services applicatifs). `search-service` et `social-service` existent dans le dépôt mais ne sont pas activés dans les Compose actuels ; leur activation n’a pas été inventée, car leur disponibilité frontend n’est pas démontrée par les contrats présents.

Les problèmes principaux confirmés étaient :

- les Dockerfiles applicatifs attendent le POM reactor racine, alors que le Compose local leur fournissait le sous-répertoire du service comme contexte ;
- chaque image multi-stage relançait `mvn -pl ... -am package`, donc un `up --build` pouvait recompiler les modules partagés pour chaque service ;
- le cache Maven BuildKit n’avait pas de partage explicite et les couches dépendances/source n’étaient pas séparées ;
- plusieurs services pouvaient démarrer sans Config Server ou avant la création des bases applicatives ;
- les healthchecks des images utilisaient `curl` sans l’installer explicitement et testaient l’endpoint global plutôt que la readiness.

Les hypothèses de cause des annulations Dokploy restent des hypothèses : aucune preuve VPS/OOM n’est disponible dans ce dépôt. Les fichiers `hs_err_pid*.log` et `replay_pid*.log` présents dans l’arbre sont non suivis et n’ont pas été modifiés ni attribués à Dokploy.

## 2. Inventaire des services actifs

Tous les services Java construits par `deploy/service.Dockerfile` ont un healthcheck HTTP sur `/actuator/health/readiness`, avec une fenêtre de démarrage de 60 secondes et 10 tentatives. Les ressources indiquées sont des limites de configuration, pas des mesures runtime.

| Service | Port | Dépendances Compose | Criticité | Risque principal |
|---|---:|---|---|---|
| postgres | 5432 | — | P0 | volume et mémoire PostgreSQL |
| postgres-init | — | postgres healthy | P0 | job bloqué si PostgreSQL n’est pas prêt |
| redis | 6379 | — | P0 | cache, rate limit et sessions |
| kafka | 9092 | — | P0 | événements et outbox |
| kafka-init | — | kafka healthy | P1 | job de topics |
| opensearch | 9200 | — | P1 | heap fixe 512 MiB |
| neo4j | 7687 | — | P1 | heap fixe 512 MiB |
| config-server | 8080 | — | P0 | point de départ de configuration |
| registry-service | 8761 | config-server healthy | P0 | découverte Eureka |
| api-gateway | 8083 | config-server, registry, redis | P0 | seul point d’entrée frontend |
| auth-service | 8082 | postgres-init, redis, kafka, config, registry | P0 | authentification |
| user-service | 8086 | postgres-init, kafka, redis, config, registry | P0 | profils utilisateurs |
| partner-service | 8087 | postgres-init, kafka, config, registry | P0 | partenaires et artisans |
| campaign-service | 8110 | postgres-init, kafka, config, registry | P1 | campagnes |
| ads-delivery-service | 8111 | postgres-init, kafka, redis, config, registry | P1 | publicité |
| ticket-service | 8112 | postgres-init, kafka, redis, config, registry | P0 | billetterie |
| commerce-service | 8113 | postgres-init, kafka, config, registry | P0 | commerce |
| admin-service | 8096 | postgres-init, kafka, redis, config, registry | P0 | administration |
| catalog-service | 8088 | postgres-init, kafka, config, registry | P1 | catalogue |
| place-service | 8084 | postgres-init, kafka, redis, config, registry | P0 | lieux et suggestions |
| content-service | 8090 | postgres-init, kafka, config, registry | P0 | publications et stories |
| interaction-service | 8091 | postgres-init, kafka, redis, config, registry | P1 | interactions |
| feed-service | 8092 | postgres-init, kafka, redis, config, registry | P0 | feed |
| media-service | 8101 | postgres-init, kafka, config, registry | P0 | médias |
| moderation-trust-service | 8100 | postgres-init, kafka, config, registry | P1 | modération et confiance |
| event-service | 8085 | postgres-init, kafka, config, registry | P0 | événements |
| booking-service | 8102 | postgres-init, kafka, config, registry | P0 | réservations |
| payment-service | 8103 | postgres-init, kafka, config, registry | P0 | paiements |
| messaging-service | 8104 | postgres-init, kafka, config, registry | P1 | messagerie/WebSocket |
| notification-service | 8094 | postgres-init, kafka, config, registry | P1 | notifications |
| support-service | 8114 | postgres-init, config, registry | P1 | support |
| culture-service | 8115 | postgres-init, kafka, config, registry | P1 | culture |
| gamification-service | 8105 | postgres-init, kafka, redis, config, registry | P1 | gamification |
| mission-reward-service | 8098 | postgres-init, kafka, config, registry | P1 | missions |
| recommendation-service | 8095 | postgres-init, kafka, redis, config, registry | P1 | exploration et recommandations |
| referral-service | 8099 | postgres-init, kafka, redis, config, registry | P1 | parrainage |
| analytics-service | 8097 | postgres-init, redis, OpenSearch, kafka, config, registry | P1 | analytics |
| ingestion-service | 8089 | postgres-init, kafka, config, registry | P1 | import catalogue |
| discovery-service | 8093 | postgres-init, OpenSearch, kafka, redis, config, registry | P0 | recherche/cartographie |
| graph-service | 8116 | Neo4j, kafka, config, registry | P1 | graphe culturel |

`search-service` et `social-service` ne figurent pas dans cette table active : leurs services Compose sont commentés et leurs images runtime seules ne prouvent pas leur readiness ni leurs contrats opérationnels.

## 3. Routes et disponibilité frontend

La configuration locale `cloud-conf-yeyamo/api-gateway.properties` contient 33 groupes de routes métier/documentation et utilise `lb://<service-name>` avec Eureka. La matrice contractuelle principale est :

| Domaine | Service responsable |
|---|---|
| Authentification | auth-service |
| Profils | user-service |
| Partenaires/artisans | partner-service |
| Lieux, régions, suggestions | place-service |
| Événements | event-service |
| Catalogue, œuvres | catalog-service |
| Publications, stories | content-service |
| Interactions, reviews, check-ins | interaction-service |
| Feed | feed-service |
| Explorer, recommandations | recommendation-service |
| Discovery, cartes, recherche admin | discovery-service |
| Médias | media-service |
| Réservations | booking-service |
| Paiements et webhook | payment-service |
| Billetterie | ticket-service |
| Messagerie/WebSocket | messaging-service |
| Notifications | notification-service |
| Culture et graphe | culture-service, graph-service |
| Gamification, missions, referrals | gamification-service, mission-reward-service, referral-service |
| Campagnes, publicité | campaign-service, ads-delivery-service |
| Commerce | commerce-service |
| Analytics, import | analytics-service, ingestion-service |
| Administration et support | admin-service, support-service |

Les dépôts frontend ne sont pas présents dans ce workspace : les appels réels Expo/React Native/Next.js, les retries client et les écrans dépendants restent à vérifier avec leurs dépôts respectifs. Aucun scale-to-zero n’a été introduit.

## 4. Corrections réalisées

| Fichier | Problème corrigé | Effet attendu | Risque résiduel |
|---|---|---|---|
| `deploy/service.Dockerfile` | builds dispersés et couches peu réutilisables | POMs résolus dans une couche dédiée, cache Maven BuildKit `sharing=locked`, image JRE non root | chaque build Compose source peut encore compiler son module ; le chemin recommandé est le reactor CI |
| `deploy/runtime.Dockerfile` | impossible de construire une image depuis un JAR déjà packagé | image runtime sans Maven, healthcheck réel avec `curl` | nécessite un JAR produit par le reactor |
| `docker-compose.yml` | contextes locaux incompatibles avec `-pl ... -am`, dépendances incomplètes | contexte racine commun, args service/port, attente de Config Server, Eureka et `postgres-init`, Redis ajouté au ticket-service | `depends_on` ne remplace pas les retries runtime |
| `docker-compose.production.yml` | builds production divergents et limites non héritées | hérite du Dockerfile commun, redémarre les services privés, applique la limite CPU/mémoire configurée | les limites par défaut doivent être confrontées aux mesures VPS |
| `docker-compose.production.images.yml` | absence de voie rollback par image immuable | override optionnel GHCR par tag `sha-*`, sans build local | Dokploy doit être configuré manuellement pour utiliser cet override |
| `build-all.sh`, `build-all.bat`, `build-all-no-maven.bat` | `clean` et builds individuels redondants | reactor unique, `MAVEN_THREADS=1` par défaut, `-DskipTests` explicite | le full build/test n’est pas lancé automatiquement |
| `scripts/build-images.sh` | chaque image recompilait Maven | package unique puis images runtime séquentielles | Docker daemon requis ; non exécuté ici |
| `.github/workflows/build-images.yml` | pas de pipeline image reproductible | workflow manuel, Maven une fois, matrix limitée à 2 builds, publication GHCR seulement si explicitement activée | workflow non exécuté ; secrets/permissions GHCR à valider |

## 5. Diagnostic Maven/Docker

Le reactor racine référence les modules internes, mais les POM enfants héritent directement de Spring Boot. Le maintien de `-am` est donc nécessaire lorsqu’un service utilise `shared-lib`, `domain-foundation`, `event-contracts` ou `security-hardening-starter`. Le supprimer ferait dépendre le build d’artefacts déjà installés et casserait la reproductibilité.

Avant correction, les Dockerfiles faisaient tous `COPY . .` puis `mvn -pl <service> -am package`. Dans un `docker compose up --build`, cela autorise plusieurs compilations concurrentes et recompile les modules partagés. La nouvelle chaîne distingue :

1. Maven reactor unique (`build-all*`, CI ou `scripts/build-images.sh`) ;
2. images runtime sans Maven (`runtime.Dockerfile`) ;
3. fallback Compose source (`service.Dockerfile`) avec cache verrouillé et couche POM dédiée.

## 6. Budget mémoire et CPU

Le budget ci-dessous est une estimation de configuration, pas une mesure VPS :

- 34 JVM applicatives avec `-Xmx256m` par défaut représentent environ 8,5 GiB de heap maximale cumulée ;
- métadonnées JVM, threads, buffers natifs et bibliothèques ajoutent typiquement une marge significative ;
- OpenSearch et Neo4j réservent chacun 512 MiB de heap ; Redpanda est configuré à 512 MiB ; PostgreSQL utilise `shared_buffers=256MB` et `max_connections=300` ;
- Dokploy, Traefik, le frontend Next.js, le système et les volumes ne sont pas inclus dans ces 8,5 GiB.

Conclusion : le fonctionnement simultané de toute la plateforme sur 11 GiB n’est pas démontré et paraît très contraint. Les valeurs `JAVA_MEMORY_LIMIT=512M`, `JAVA_CPU_LIMIT=1.0` sont des plafonds de conteneur configurables, pas une garantie de capacité globale. Le swap de 4 GiB ne doit pas être compté comme de la RAM. Avant production, mesurer RSS, heap committed/max, threads, pools JDBC, OpenSearch, Neo4j, Redpanda et Next.js sous charge représentative.

## 7. Sévérité et risques résiduels

- **P0 confirmé** : ancien contexte Docker local incompatible avec les Dockerfiles reactor ; dépendances de démarrage manquantes pour plusieurs clients Config ; base applicative pouvant ne pas exister avant Flyway.
- **P1 confirmé** : builds Docker répétés par service ; healthcheck `curl` non garanti dans l’image runtime ; absence de pipeline d’images immuables.
- **P1 hypothèse** : annulations Dokploy dues à CPU, concurrence BuildKit, timeouts ou pression mémoire ; aucune preuve corrélée n’est disponible.
- **P2 confirmé** : `search-service` et `social-service` non activés dans Compose ; à traiter seulement après validation de leurs routes/contrats.
- **P2 non vérifié** : configuration distante Git du Config Server, readiness des dépendances externes, comportement Eureka sous redémarrage réel et récupération des consommateurs Kafka.

## 8. Validations effectuées

- `docker compose -f docker-compose.yml config --quiet` : succès.
- `docker compose -f docker-compose.production.yml config --quiet` avec variables de test : succès.
- Compose production + `docker-compose.production.images.yml` avec `IMAGE_TAG=sha-test` : succès.
- `git diff --check` : succès.
- `mvn -B -ntp -pl api-gateway -am -DskipTests validate` : succès.
- `mvn -B -ntp -pl auth-service -am -DskipTests validate` : succès.
- `mvn -B -ntp -pl auth-service,api-gateway -am -DskipTests package` : succès ; shared-lib et security-hardening-starter construits une seule fois dans le reactor.
- `mvn -B -ntp -pl security-hardening-starter -am test` : 16 tests, 0 échec.
- Docker build/run non exécuté : Docker Desktop n’avait pas de daemon Linux disponible dans l’environnement.
- Tests unitaires/intégration complets non exécutés ; aucun secret, service externe ou VPS n’était disponible.

## 9. Mise en production progressive et rollback

1. Exécuter manuellement le workflow avec `publish=false` et vérifier les artefacts ; lancer ensuite une publication autorisée avec `publish=true`.
2. Vérifier les images `ghcr.io/<owner>/yeyamo-<service>:sha-<commit>` et conserver le tag précédent.
3. Configurer Dokploy pour charger `docker-compose.production.yml` puis l’override `docker-compose.production.images.yml`, avec toutes les variables requises.
4. Déployer d’abord l’infrastructure, Config Server, Eureka, Gateway, auth/user/place/event, puis les autres services par groupes fonctionnels.
5. Vérifier les healthchecks readiness, l’enregistrement Eureka, les routes Gateway, les logs et les flux Kafka avant chaque groupe suivant.
6. Pour rollback, remettre le précédent `IMAGE_TAG` immuable et relancer le groupe concerné ; ne supprimer aucun volume.

Les actions manuelles restent : validation des ressources VPS, configuration Dokploy/Traefik, permissions GHCR, variables/secrets de production, test frontend réel et test de restauration des volumes.
