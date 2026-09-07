# Déploiement production Dokploy

## Architecture

Cloudflare envoie `api.yeyamo.com` vers Traefik/Dokploy. Traefik joint
uniquement `api-gateway:8083` via le réseau Docker externe `dokploy-network`.
Tous les autres services, PostgreSQL, Redis, Redpanda, OpenSearch, Neo4j,
Config Server et Eureka restent sur `yeyamo-network` sans publication de port
hôte.

## Config Server

Le développement conserve `SPRING_PROFILES_ACTIVE=native` et le montage local
du sous-module `cloud-conf-yeyamo`. La production utilise
`SPRING_PROFILES_ACTIVE=prod` et le backend Git :

- dépôt : `CONFIG_GIT_URI` ;
- branche par défaut : `CONFIG_GIT_BRANCH=daryl` ;
- authentification HTTPS facultative : `CONFIG_GIT_USERNAME` et
  `CONFIG_GIT_PASSWORD`.

La branche de référence est `cloud-conf-yeyamo:daryl`. Aucun PAT ne doit être
commité ; stockez-le seulement dans les variables Dokploy si le dépôt est privé.

Le keystore de chiffrement n’est pas présent dans l’image et n’a pas été
inventé. Fournissez-le via un fichier secret/montage géré par Dokploy, puis
définissez `CONFIG_ENCRYPT_KEYSTORE_LOCATION` sur son chemin absolu dans le
conteneur, par exemple `file:/run/secrets/config-encrypt.p12`, ainsi que les
trois variables associées. Vérifiez que l’utilisateur `spring` (UID 10001)
peut le lire.

## Préparation Dokploy

1. Créez une application Compose depuis ce dépôt.
2. Renseignez les variables de `.env.production.example` dans Dokploy, sans
   importer ce fichier avec des valeurs réelles dans Git.
3. Configurez la commande de build précédant Compose :
   `mvn -DskipTests package`.
   Les Dockerfiles actuels sont des images runtime et copient
   `target/<service>.jar`; un clone propre ne contient volontairement aucun
   JAR. Cette étape est obligatoire tant que les Dockerfiles ne sont pas
   convertis en multi-stage builds.
4. Utilisez `docker-compose.production.yml` comme fichier Compose.
5. Attachez le service Dokploy `api-gateway` au domaine `api.yeyamo.com`, port
   conteneur `8083`. Ne créez aucune route Dokploy pour les autres services.
6. Créez/assurez l’existence du réseau Docker externe `dokploy-network`.

## Volumes persistants

- `postgres-data`
- `redis-data`
- `opensearch-data`
- `neo4j-data`
- `media-storage`

L’image `deploy/postgres-init.Dockerfile` embarque le script idempotent de
création des bases. Ainsi, la production n’utilise aucun bind mount runtime
depuis le checkout Dokploy. `postgres-init` et `kafka-init` sont des jobs
one-shot sans boucle de redémarrage.

## Cloudflare

Créez un enregistrement DNS proxifié pour `api.yeyamo.com` vers le serveur
Dokploy, puis configurez le domaine dans Dokploy/Traefik. Activez Full
(strict) uniquement après émission du certificat par Traefik. Les CORS
`CORS_ALLOWED_ORIGINS` et `CORS_ORIGINS` doivent contenir uniquement les
origines front-end de production en HTTPS.

## Démarrage et diagnostic

Ordre : infrastructure, Config Server, Eureka, API Gateway, puis services
métier. Les `depends_on` et healthchecks existants sont conservés.

```bash
docker compose -f docker-compose.production.yml ps
docker compose -f docker-compose.production.yml logs -f config-server registry-service api-gateway
docker stats --no-stream
docker ps
free -h
df -h
```

Validez le rendu avant déploiement avec des variables Dokploy renseignées :

```bash
docker compose -f docker-compose.production.yml config --quiet
```

## Rollback

Conservez le dernier déploiement Dokploy fonctionnel. En cas d’échec, revenez
au commit Git précédent dans Dokploy et redéployez avec les mêmes volumes. Ne
supprimez pas les volumes PostgreSQL/OpenSearch/Neo4j/Redis pendant un rollback.
