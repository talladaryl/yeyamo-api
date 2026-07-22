# Dockerfiles Ajoutés - Résumé

## Services sans Dockerfile (Avant)

Les 6 services suivants n'avaient pas de Dockerfile :
- ❌ auth-service
- ❌ booking-service
- ❌ graph-service
- ❌ notification-service
- ❌ search-service
- ❌ social-service

## Dockerfiles Créés

Tous les Dockerfiles ont été créés avec le format standard du projet :

### ✅ auth-service/Dockerfile
- Port: 8082
- Base image: eclipse-temurin:21-jre-jammy
- JAR: auth-service-0.0.1-SNAPSHOT.jar
- Healthcheck: curl sur /actuator/health

### ✅ booking-service/Dockerfile
- Port: 8102
- Base image: eclipse-temurin:21-jre-jammy
- JAR: booking-service-0.0.1-SNAPSHOT.jar
- Healthcheck: curl sur /actuator/health

### ✅ graph-service/Dockerfile
- Port: 8080 (interne)
- Base image: eclipse-temurin:21-jre-jammy
- JAR: graph-service-0.0.1-SNAPSHOT.jar
- Dépendance: Neo4j
- Healthcheck: curl sur /actuator/health

### ✅ notification-service/Dockerfile
- Port: 8094
- Base image: eclipse-temurin:21-jre-jammy
- JAR: notification-service-0.0.1-SNAPSHOT.jar
- Healthcheck: curl sur /actuator/health

### ✅ search-service/Dockerfile
- Port: 8080 (interne)
- Base image: eclipse-temurin:21-jre-jammy
- JAR: search-service-0.0.1-SNAPSHOT.jar
- Dépendance: OpenSearch
- Healthcheck: curl sur /actuator/health

### ✅ social-service/Dockerfile
- Port: 8080 (interne)
- Base image: eclipse-temurin:21-jre-jammy
- JAR: social-service-0.0.1-SNAPSHOT.jar
- Dépendance: Cassandra
- Healthcheck: curl sur /actuator/health

## Modifications du docker-compose.yml

### Services ajoutés au docker-compose.yml
1. **search-service** - Service de recherche utilisant OpenSearch
2. **graph-service** - Service de graphe utilisant Neo4j
3. **social-service** - Service social utilisant Cassandra

### Infrastructure ajoutée
- **Neo4j** (ports 7474, 7687) - Base de données graphe pour graph-service
  - Volume: neo4j-data
  - Healthcheck configuré
  - Auth: neo4j/neo4jpassword

### Autres modifications
- Suppression de l'attribut `version` obsolète du docker-compose.yml
- Ajout du volume `neo4j-data` dans la section volumes

## Scripts de Build Créés

### build-all.bat (Windows)
Script pour compiler tous les services avant Docker :
1. Compile security-hardening-starter (dépendance commune)
2. Compile tous les microservices avec Maven
3. Crée les fichiers JAR dans target/

### build-all.sh (Linux/Mac)
Version Unix du script de build avec les mêmes fonctionnalités.

## Documentation Mise à Jour

### DOCKER_SETUP.md
- Ajout de l'ÉTAPE 1 : Compilation obligatoire avant Docker
- Instructions détaillées pour utiliser build-all.bat/sh
- Ajout de Neo4j dans la liste des services d'infrastructure
- Section troubleshooting avec l'erreur "JAR not found"
- Documentation des 3 nouveaux services (search, graph, social)
- Informations d'accès Neo4j Browser

## Format Standard des Dockerfiles

Tous les Dockerfiles suivent le même pattern :

```dockerfile
FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home spring
WORKDIR /app
COPY --chown=spring:spring target/<service-name>-0.0.1-SNAPSHOT.jar app.jar
USER spring
ENV SERVER_PORT=<port>
EXPOSE <port>
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 CMD curl --fail --silent http://localhost:<port>/actuator/health >/dev/null || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
```

## Commandes pour Lancer le Projet

### Sur Windows
```cmd
cd yeyamo-api
build-all.bat
docker compose up -d --build
```

### Sur Linux/Mac
```bash
cd yeyamo-api
chmod +x build-all.sh
./build-all.sh
docker compose up -d --build
```

## Vérification

Tous les services ont maintenant un Dockerfile :

```cmd
# Vérifier la présence des Dockerfiles
dir *-service\Dockerfile /s /b
```

Résultat attendu : 27 Dockerfiles (tous les services)

## État Final

✅ Tous les services ont un Dockerfile
✅ Tous les services sont dans docker-compose.yml
✅ Scripts de build créés (Windows + Unix)
✅ Documentation complète mise à jour
✅ Infrastructure complète (PostgreSQL, Redis, Cassandra, OpenSearch, Neo4j, Kafka)
✅ Prêt pour `docker compose up -d --build` après compilation
