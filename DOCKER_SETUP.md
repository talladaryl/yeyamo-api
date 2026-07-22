# Guide de Démarrage Docker - YeYamo Backend

## Prérequis

- Docker Desktop installé et démarré
- Au moins 8 GB de RAM disponible pour Docker
- Au moins 20 GB d'espace disque libre
- **Maven installé** (commande `mvn` disponible dans le PATH)
  - Télécharger depuis : https://maven.apache.org/download.cgi
  - Vérifier l'installation : `mvn -version`

## Lancement du Backend Complet

### ÉTAPE 1 : Compiler tous les services

⚠️ **IMPORTANT** : Vous devez d'abord compiler tous les services Java avant de lancer Docker Compose.

**Vérifier que Maven est installé** :
```cmd
mvn -version
```

Si Maven n'est pas installé, téléchargez-le depuis https://maven.apache.org/download.cgi

**Compiler tous les services** :
```cmd
cd yeyamo-api
build-all.bat
```

Ce script va compiler tous les modules du projet reactor Maven (y compris `security-hardening-starter`) et créer les fichiers JAR dans les dossiers `target/` de chaque service.

### ÉTAPE 2 : Lancer tous les services avec Docker

```cmd
docker compose up -d --build
```

Cette commande va :
- Construire toutes les images Docker des services
- Démarrer tous les conteneurs en arrière-plan
- Créer le réseau et les volumes nécessaires

### 3. Vérifier le statut des services

```cmd
docker compose ps
```

### 4. Voir les logs

```cmd
# Tous les services
docker compose logs -f

# Un service spécifique
docker compose logs -f auth-service
docker compose logs -f api-gateway
```

### 5. Arrêter tous les services

```cmd
docker compose down
```

### 6. Arrêter et supprimer les volumes (ATTENTION: supprime les données)

```cmd
docker compose down -v
```

## Services et Ports

### Infrastructure
- **Config Server**: http://localhost:8080
- **Registry (Eureka)**: http://localhost:8081
- **API Gateway**: http://localhost:8083
- **PostgreSQL**: localhost:5432
- **Cassandra**: localhost:9042
- **Redis**: localhost:6379
- **OpenSearch**: http://localhost:9200
- **Neo4j**: http://localhost:7474 (Browser), localhost:7687 (Bolt)
- **Kafka**: localhost:9092

### Microservices
- **auth-service**: http://localhost:8082
- **place-service**: http://localhost:8084
- **event-service**: http://localhost:8085
- **user-service**: http://localhost:8086
- **partner-service**: http://localhost:8087
- **catalog-service**: http://localhost:8088
- **ingestion-service**: http://localhost:8089
- **content-service**: http://localhost:8090
- **interaction-service**: http://localhost:8091
- **feed-service**: http://localhost:8092
- **discovery-service**: http://localhost:8093
- **notification-service**: http://localhost:8094
- **recommendation-service**: http://localhost:8095
- **admin-service**: http://localhost:8096
- **analytics-service**: http://localhost:8097
- **mission-reward-service**: http://localhost:8098
- **referral-service**: http://localhost:8099
- **moderation-trust-service**: http://localhost:8100
- **media-service**: http://localhost:8101
- **booking-service**: http://localhost:8102
- **payment-service**: http://localhost:8103
- **messaging-service**: http://localhost:8104
- **gamification-service**: http://localhost:8105
- **search-service**: (pas de port exposé, interne)
- **graph-service**: (pas de port exposé, interne)
- **social-service**: (pas de port exposé, interne)

## Ordre de Démarrage

Les services démarrent automatiquement dans le bon ordre grâce aux `depends_on`:

1. **Bases de données** (postgres, cassandra, redis, opensearch, neo4j, kafka)
2. **Config Server**
3. **Registry Service (Eureka)**
4. **API Gateway**
5. **Tous les microservices**

## Tests de Santé

### Config Server
```cmd
curl http://localhost:8080/actuator/health
```

### Registry Service
```cmd
curl http://localhost:8081/actuator/health
```

### API Gateway
```cmd
curl http://localhost:8083/actuator/health
```

### PostgreSQL
```cmd
docker compose exec postgres psql -U postgres -l
```

### Cassandra
```cmd
docker compose exec cassandra cqlsh -e "DESCRIBE KEYSPACES"
```

### Neo4j
Accédez à http://localhost:7474 dans votre navigateur
- Username: `neo4j`
- Password: `neo4jpassword`

## Commandes Utiles

### Rebuild un service spécifique
```cmd
docker compose up -d --build auth-service
```

### Redémarrer un service
```cmd
docker compose restart auth-service
```

### Voir les ressources utilisées
```cmd
docker stats
```

### Nettoyer les images inutilisées
```cmd
docker system prune -a
```

## Troubleshooting

### Erreur "JAR not found" lors du build Docker
**Cause**: Les fichiers JAR n'ont pas été compilés avant de lancer Docker Compose.

**Solution**:
```cmd
# 1. Arrêter Docker Compose
docker compose down

# 2. Vérifier que Maven est installé
mvn -version

# 3. Compiler tous les services
build-all.bat

# 4. Relancer Docker Compose
docker compose up -d --build
```

### Erreur "Maven not found"
**Cause**: Maven n'est pas installé ou pas dans le PATH.

**Solution**:
1. Télécharger Maven depuis https://maven.apache.org/download.cgi
2. Installer Maven et ajouter le dossier `bin` à votre PATH
3. Vérifier l'installation : `mvn -version`
4. Redémarrer votre terminal
5. Relancer `build-all.bat`

### Service ne démarre pas
1. Vérifier les logs: `docker compose logs [service-name]`
2. Vérifier que les dépendances sont healthy: `docker compose ps`
3. Rebuilder: `docker compose up -d --build [service-name]`

### Manque de mémoire
```cmd
docker compose down
# Augmenter la RAM de Docker Desktop dans les paramètres
docker compose up -d
```

### Base de données non initialisée
```cmd
docker compose down -v
docker compose up -d
```

### Cassandra - Initialiser le keyspace messaging
```cmd
docker compose exec cassandra cqlsh -f /var/lib/cassandra/schema.cql
```

## Variables d'Environnement

Les variables par défaut dans docker-compose.yml sont pour le développement local.

Pour la production, créez un fichier `.env`:

```env
# JWT
JWT_SECRET=your-super-secret-jwt-key-change-me

# Database
POSTGRES_PASSWORD=strong-password

# Eureka
EUREKA_USERNAME=eureka
EUREKA_PASSWORD=strong-password

# Config Server
CONFIG_SERVER_USERNAME=admin
CONFIG_SERVER_PASSWORD=strong-password

# Payment
PAYMENT_WEBHOOK_SECRET=webhook-secret

# Neo4j
NEO4J_AUTH=neo4j/strong-password
```

Puis lancez:
```cmd
docker compose --env-file .env up -d
```

## Accès aux Interfaces Web

### Eureka Dashboard
URL: http://localhost:8081  
Username: `eureka`  
Password: `eureka123`

Vous verrez tous les microservices enregistrés.

### Neo4j Browser
URL: http://localhost:7474  
Username: `neo4j`  
Password: `neo4jpassword`

### OpenSearch
URL: http://localhost:9200

## Notes Importantes

⚠️ **Configuration actuelle pour DEV uniquement**
- Les mots de passe sont en clair
- JWT_SECRET est simple
- Pas de TLS/HTTPS
- Pas de scaling

Pour la production, voir `docs/SECURITY_HARDENING.md`

## Nouveaux Services Ajoutés

Les services suivants ont été ajoutés avec leurs Dockerfiles :
- ✅ **auth-service** (port 8082)
- ✅ **booking-service** (port 8102)
- ✅ **graph-service** (utilise Neo4j)
- ✅ **notification-service** (port 8094)
- ✅ **search-service** (utilise OpenSearch)
- ✅ **social-service** (utilise Cassandra)
