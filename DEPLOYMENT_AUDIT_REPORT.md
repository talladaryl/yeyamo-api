# Audit de préparation Dokploy

## A. État initial

Le Compose local contient 41 services et expose les infrastructures, Config
Server, Eureka et les microservices sur l’hôte. Le Config Server était limité
au profil `native` avec le montage `./cloud-conf-yeyamo:/config:ro`.

## B. Problèmes identifiés

- des identifiants de développement étaient codés dans `docker-compose.yml` ;
- le mode native ne convient pas à un checkout Dokploy éphémère ;
- les Dockerfiles copient des JAR dans `target/`, désormais non versionnés ;
- le keystore de chiffrement n’existe ni dans le dépôt ni dans l’image ;
- OpenSearch désactive son plugin de sécurité. Il reste privé dans la
  topologie production, mais ce point doit être durci ultérieurement.

## C. Config Server

`application.properties` utilise désormais `spring.profiles.default=native`.
`application-native.properties` contient la configuration locale existante.
`application-prod.properties` active le backend Git, la branche `daryl`, le
clone au démarrage et le force-pull, avec identifiants Git uniquement via
variables d’environnement. Le chiffrement existant est conservé.

## D. Compose production

`docker-compose.production.yml` étend les 41 services réellement déclarés par
le Compose local. Il conserve les healthchecks, dépendances, limites Java et
volumes ; il retire les ports hôte et le montage local du sous-module. Seul
`api-gateway` rejoint `dokploy-network` et expose le port conteneur 8083.

## E. Ports hôte supprimés

Tous les bindings hôte du Compose local sont réinitialisés en production,
notamment 5432, 6379, 9092, 9200, 9600, 7474, 7687, 8080, 8761, 8082-8117 et
8083. Les communications restent par noms de services Docker.

## F. Volumes persistants

`postgres-data`, `redis-data`, `opensearch-data`, `neo4j-data` et
`media-storage` sont conservés.

## G. Variables requises

La liste sans valeur est dans `.env.production.example` : infrastructure,
Config Git/chiffrement, Eureka/JWT/CORS, SMTP/Expo, Google, R2, paiement,
ticket, publicité et bootstrap admin.

## H. Secrets détectés et remplacés

Des valeurs de développement ont été détectées dans `docker-compose.yml`
(PostgreSQL, Config Server, Eureka, JWT, Neo4j et jetons associés). Elles ne
sont pas copiées dans le Compose production : celui-ci exige des variables
Dokploy. Le sous-module contient des propriétés de secrets sous forme de
placeholders ; aucune valeur n’est reproduite dans ce rapport.

## I. Services publics

Uniquement `api-gateway:8083`, via Traefik/Dokploy sur `dokploy-network`.

## J. Services privés

Toutes les infrastructures, Config Server, Eureka et les 40 autres services
restent sur `yeyamo-network` sans port hôte publié.

## K. Validation Compose

`docker compose -f docker-compose.production.yml config --quiet` est valide
avec des valeurs de validation en mémoire et résout 41 services. Les variables
réelles restent obligatoires dans Dokploy.

## L. Validation Git

`git diff --check` est valide. `mvn test -pl config-server` est également
valide : 1 test exécuté, 0 échec, 0 erreur. Aucun commit ni push n’est effectué
par cette intervention.

## M. Risques restants

- le keystore doit être fourni hors Git par Dokploy ;
- le premier build doit produire les JAR avant Compose ;
- OpenSearch conserve son plugin de sécurité désactivé, compensé seulement par
  l’absence de port public.

## N. Actions Dokploy

Configurer les variables, le fichier secret keystore, le réseau
`dokploy-network`, la commande Maven de build et le domaine de l’API.

## O. Actions Cloudflare

Configurer le DNS proxifié, le mode TLS strict après certificat Traefik et les
origines CORS de production.

## P. Verdict

## Q. Audit final pré-Dokploy

- Dockerfiles applicatifs audités : 34 ; convertis en multi-stage Maven/Java
  21 : 34 ; dépendance restante à un JAR `target/` du checkout : 0.
- Les builds production utilisent désormais le contexte racine du reactor et
  `mvn -pl <service> -am -DskipTests package` dans le stage Maven. Le cache
  Maven BuildKit est utilisé ; `.env`, le mobile, le sous-module config et les
  anciens `target/` sont exclus du contexte.
- `{cipher}` dans `cloud-conf-yeyamo` : aucune occurrence. Le keystore reste
  supporté mais `CONFIG_ENCRYPT_ENABLED=false` par défaut ; il ne bloque plus
  le premier démarrage. Pour l’activer, provisionner un fichier secret Dokploy
  et définir les variables `CONFIG_ENCRYPT_*` puis `CONFIG_ENCRYPT_ENABLED=true`.
- Docker Compose : v5.1.4 ; `config --quiet` valide ; 41 services résolus.
- Builds représentatifs demandés (Config Server, Registry, Gateway, Auth,
  Payment, Media) : non évalués localement. Docker Buildx a échoué avant la
  construction sur un refus d’accès au verrou utilisateur
  `.docker/buildx/.lock`; aucun conteneur n’a été démarré et ce n’est pas une
  erreur Maven/Dockerfile. Relancer après correction des droits Docker locaux.
- `git diff --check` : valide. Aucun secret réel n’a été ajouté ; les valeurs
  de production restent exclusivement des variables Dokploy.

**NOT READY FOR FIRST DOKPLOY DEPLOYMENT** : la configuration est prête, mais
les six builds représentatifs ne sont pas encore prouvés à cause du blocage
Buildx local. Lever ce verrou, exécuter ces builds sans démarrer les services,
puis réévaluer ce verdict.
