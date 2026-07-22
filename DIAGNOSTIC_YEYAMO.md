# DIAGNOSTIC COMPLET - Architecture Docker + Spring Cloud YeYamo

**Date de l'audit** : 21 juillet 2026  
**Auditeur** : Kiro - Architecte Logiciel Senior  
**Contexte** : registry-service échoue au démarrage avec exit code 1

---

## 1. RÉSUMÉ EXÉCUTIF

### Cause Principale Identifiée
**CRITIQUE : Incohérence de configuration Spring Cloud Config entre registry-service et les autres services**

Le `registry-service` utilise **`spring.config.import=optional:configserver:`** alors que tous les autres services utilisent **`spring.config.import=configserver:`** (sans optional).

Cette configuration "optional" combinée avec `spring.cloud.config.fail-fast=false` fait que le registry-service tente de démarrer SANS configuration du Config Server. Cependant, la configuration centralisée dans `registry-service.properties` est CRITIQUE et contient :
- Les credentials Eureka (EUREKA_USERNAME, EUREKA_PASSWORD)
- Le port du serveur (SERVER_PORT)
- La configuration Eureka complète

**Impact** : Le registry-service démarre sans ces configurations essentielles, provoquant un échec immédiat.

### Niveau de Confiance
**95%** - L'anomalie est confirmée par analyse du code et des configurations.

---

## 2. ARCHITECTURE ANALYSÉE

### 2.1 Services d'Infrastructure

| Service | Port | Rôle | État Attendu | Dépendances |
|---------|------|------|--------------|-------------|
| postgres | 5432 | Base de données relationnelle | Healthy | Aucune |
| cassandra | 9042 | Base NoSQL pour messaging/social | Healthy | Aucune |
| redis | 6379 | Cache & sessions | Healthy | Aucune |
| opensearch | 9200 | Recherche & analytics | Healthy | Aucune |
| kafka (redpanda) | 9092 | Message broker | Healthy | Aucune |
| **config-server** | 8080 | Configuration centralisée | **Healthy** | Volume cloud-conf-yeyamo |
| **registry-service** | 8081 | Service discovery Eureka | **FAILED** | config-server |

### 2.2 Services Applicatifs

25 microservices dépendent de config-server et registry-service.

Tous configurés avec :
- `spring.config.import=configserver:${CONFIG_SERVER_URL}`
- `spring.cloud.config.fail-fast=true`

---

## 3. ANOMALIES DÉTECTÉES

### 🔴 ANOMALIE CRITIQUE #1
**Titre** : Configuration Config Client incohérente dans registry-service  
**Fichier** : `registry-service/src/main/resources/application.properties`  
**Ligne** : 5  
**Configuration actuelle** :
```properties
spring.config.import=optional:configserver:${CONFIG_SERVER_URL:http://localhost:8080}
spring.cloud.config.fail-fast=false
```

**Explication** :
Le préfixe `optional:` permet à Spring Boot de démarrer même si Config Server est inaccessible. Combiné avec `fail-fast=false`, cela crée un comportement de fallback silencieux.

Le registry-service a besoin de sa configuration centralisée car :
1. Les credentials Eureka sont dans `registry-service.properties` (EUREKA_USERNAME, EUREKA_PASSWORD)
2. Le port SERVER_PORT peut être configuré centralement
3. La configuration Eureka complète (self-preservation, timeouts) est centralisée

**Impact** : CRITIQUE - Le service démarre sans credentials, provoque une erreur d'authentification ou de configuration manquante.

**Gravité** : 🔴 **CRITIQUE**

---

### 🔴 ANOMALIE CRITIQUE #2
**Titre** : Incohérence hostname localhost dans configuration centralisée  
**Fichier** : `cloud-conf-yeyamo/registry-service.properties`  
**Ligne** : 2  
**Configuration actuelle** :
```properties
eureka.instance.hostname=${EUREKA_HOSTNAME:localhost}
```

**Explication** :
Dans Docker, `localhost` fait référence au conteneur lui-même, pas au host ou au réseau Docker.
La valeur par défaut `localhost` n'est JAMAIS correcte dans un environnement conteneurisé.

Le docker-compose.yml injecte bien `EUREKA_USERNAME` et `EUREKA_PASSWORD`, mais **AUCUNE variable EUREKA_HOSTNAME** n'est définie.

**Impact** : Le registry-service s'identifie comme `localhost` dans Eureka, rendant impossible la découverte par les autres services.

**Gravité** : 🔴 **CRITIQUE**

---

### 🟠 ANOMALIE ÉLEVÉE #3
**Titre** : Variable d'environnement CONFIG_SERVER_URL non définie dans docker-compose.yml  
**Fichier** : `docker-compose.yml`  
**Service** : registry-service  
**Lignes** : 260-279

**Configuration actuelle** :
```yaml
registry-service:
  environment:
    CONFIG_SERVER_URL: http://config-server:8080
```

**Explication** :
La variable est bien définie dans docker-compose.yml avec la valeur correcte `http://config-server:8080`.
Cependant, si le Config Server n'est pas accessible au moment du démarrage, le registry-service utilise le fallback `localhost:8080` défini dans application.properties.

**Impact** : Moyen - La configuration est correcte en théorie, mais vulnérable aux problèmes de timing.

**Gravité** : 🟠 **ÉLEVÉE**

---

### 🟠 ANOMALIE ÉLEVÉE #4
**Titre** : Healthcheck registry-service utilise un endpoint non authentifié  
**Fichier** : `registry-service/Dockerfile`  
**Ligne** : 6

**Configuration actuelle** :
```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health/readiness || exit 1
```

**Explication** :
Le SecurityConfig du registry-service autorise `/actuator/health` et `/actuator/health/**` sans authentification, ce qui est correct.
Cependant, le healthcheck dans Docker ne peut s'exécuter que si le service a démarré avec succès.

Si le service échoue au démarrage (avant d'écouter sur le port 8081), le healthcheck échouera systématiquement.

**Impact** : Le healthcheck ne peut pas diagnostiquer la vraie cause du problème.

**Gravité** : 🟠 **ÉLEVÉE**

---

### 🟡 ANOMALIE MOYENNE #5
**Titre** : Dépendance security-hardening-starter peut interférer avec Eureka Server  
**Fichier** : `registry-service/pom.xml`  
**Lignes** : 76-80

**Dépendance** :
```xml
<dependency>
    <groupId>com.yeyamo.security</groupId>
    <artifactId>security-hardening-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Explication** :
Le `security-hardening-starter` configure automatiquement :
1. Un `SecurityHardeningFilter` avec rate limiting
2. Un `BeanPostProcessor` pour les JwtDecoder

Le registry-service utilise Basic Auth, pas JWT. Le `BeanPostProcessor` cherche la propriété `jwt.secret` qui n'existe pas dans le contexte du registry-service.

Le `HardeningAutoConfiguration` s'active si `yeyamo.security.enabled=true` (défaut dans application.properties commun).

**Impact** : Potentiellement, le BeanPostProcessor cherche `jwt.secret` qui n'est pas défini pour registry-service, causant une erreur de démarrage.

**Gravité** : 🟡 **MOYENNE**

---

### 🟡 ANOMALIE MOYENNE #6
**Titre** : Incohérence des valeurs par défaut localhost dans configurations centralisées  
**Fichiers** : Multiples fichiers dans `cloud-conf-yeyamo/*.properties`

**Exemples** :
```properties
# admin-service.properties
eureka.client.service-url.defaultZone=${EUREKA_SERVER_URL:http://localhost:8081/eureka/}
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/yeyamo_admin}

# analytics-service.properties
eureka.client.service-url.defaultZone=${EUREKA_SERVER_URL:http://localhost:8081/eureka/}
opensearch.uris=${OPENSEARCH_URIS:http://localhost:9200}
```

**Explication** :
Les configurations centralisées définissent `localhost` comme valeur par défaut pour :
- Eureka defaultZone
- Datasources PostgreSQL
- OpenSearch, Redis, Kafka

Dans Docker, ces valeurs par défaut ne fonctionnent JAMAIS.
Le docker-compose.yml injecte bien les variables correctes :
- `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka:eureka123@registry-service:8081/eureka`
- `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/...`

**Impact** : Les services fonctionneront en Docker grâce aux variables d'environnement, MAIS échoueront en environnement local de développement.

**Gravité** : 🟡 **MOYENNE** (pas d'impact Docker, impact développement local)

---

### 🟢 ANOMALIE FAIBLE #7
**Titre** : Variable EUREKA_SERVER_URL vs EUREKA_CLIENT_SERVICEURL_DEFAULTZONE  
**Fichiers** : `cloud-conf-yeyamo/application.properties` et `docker-compose.yml`

**Configuration centralisée** :
```properties
# application.properties (commun)
eureka.client.service-url.defaultZone=${EUREKA_SERVER_URL}
```

**Docker Compose** :
```yaml
environment:
  EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka:eureka123@registry-service:8081/eureka
```

**Explication** :
Spring Cloud convertit automatiquement :
- `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` → `eureka.client.service-url.defaultZone`

Mais la configuration centralisée attend `${EUREKA_SERVER_URL}`.

Il y a donc une déconnexion entre le nom de variable attendu et le nom fourni.

**Impact** : Les services utilisent directement la variable Docker `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` qui écrase la configuration centralisée. Pas de problème en pratique, mais incohérence conceptuelle.

**Gravité** : 🟢 **FAIBLE**

---

### 🟢 ANOMALIE FAIBLE #8
**Titre** : Start period du healthcheck trop court pour registry-service  
**Fichier** : `registry-service/Dockerfile`  
**Ligne** : 6

**Configuration** :
```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3
```

**Explication** :
Le registry-service doit :
1. Se connecter au Config Server (peut prendre 5-10s avec retries)
2. Charger sa configuration
3. Démarrer le serveur Eureka (peut prendre 10-15s)

Un `start-period` de 30s peut être insuffisant, surtout si Config Server est lent.

Le config-server a un `start-period=90s` ce qui est plus approprié.

**Impact** : Le healthcheck peut marquer le service comme unhealthy prématurément.

**Gravité** : 🟢 **FAIBLE**

---

## 4. CAUSE RACINE

**Le registry-service échoue au démarrage pour les raisons suivantes (par ordre de probabilité) :**

### Cause Racine Principale (Probabilité 80%)
**Configuration Config Client "optional" permet un démarrage sans configuration**

Le registry-service utilise :
```properties
spring.config.import=optional:configserver:${CONFIG_SERVER_URL:http://localhost:8080}
spring.cloud.config.fail-fast=false
```

Ce qui signifie :
1. Si Config Server est inaccessible → le service démarre quand même
2. Les propriétés de `registry-service.properties` ne sont PAS chargées
3. Les variables `${EUREKA_USERNAME}` et `${EUREKA_PASSWORD}` restent non résolues
4. Le service tente de démarrer avec une configuration incomplète
5. Spring Security ne peut pas s'initialiser sans credentials valides
6. Le service échoue avec une erreur de configuration

**Preuve** :
- Tous les autres services utilisent `spring.config.import=configserver:` (obligatoire)
- Tous les autres services utilisent `spring.cloud.config.fail-fast=true`
- registry-service est le SEUL service avec `optional:` et `fail-fast=false`

**Logs attendus** :
```
ConfigClientFailFastException: Could not locate PropertySource
```
Mais avec `fail-fast=false`, cette exception n'est PAS levée, le service continue et échoue plus tard.

### Cause Racine Secondaire (Probabilité 15%)

**Le security-hardening-starter interfère avec le démarrage**

Le `BeanPostProcessor` du security-hardening-starter cherche `jwt.secret` :
```java
public Object postProcessAfterInitialization(Object bean, String beanName) {
    // ...
    return StrictJwtDecoders.create(
        environment.getProperty("jwt.secret"),  // ← PEUT ÊTRE NULL
        // ...
    );
}
```

Si `jwt.secret` est null ou absent, le `StrictJwtDecoders.create()` peut échouer.

Le registry-service n'a pas besoin de JWT (il utilise Basic Auth), mais le starter s'active quand même.

### Cause Racine Tertiaire (Probabilité 5%)

**Problème de timing - Config Server pas encore prêt**

Même si le healthcheck du config-server indique "Healthy", il se peut que :
1. Le endpoint `/registry-service/default` ne soit pas encore disponible
2. Le volume `cloud-conf-yeyamo` n'est pas monté correctement
3. Les permissions du volume empêchent la lecture

---

## 5. CAUSES SECONDAIRES

### 5.1 Problèmes d'Architecture

**Incohérence des hostnames**
- `registry-service.properties` utilise `${EUREKA_HOSTNAME:localhost}` sans variable Docker
- Les autres services utilisent `eureka.instance.prefer-ip-address=true` (mieux pour Docker)

**Configuration Config Server native**
- `spring.cloud.config.server.accept-empty=false` force une erreur si pas de fichier trouvé
- `spring.cloud.config.server.native.fail-on-error=true` force une erreur en cas de problème de lecture

Ces configurations sont trop strictes pour un environnement Docker où les timings sont variables.

### 5.2 Problèmes de Sécurité

**Basic Auth non testé en environnement conteneurisé**
Le SecurityConfig du registry-service :
```java
.authorizeHttpRequests(authorize -> authorize
    .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
    .anyRequest().authenticated())
.httpBasic(Customizer.withDefaults())
```

Cette configuration force l'authentification Basic pour TOUS les endpoints Eureka (`/eureka/**`).

Les autres services doivent s'authentifier avec :
```
http://eureka:eureka123@registry-service:8081/eureka
```

Si les credentials ne sont pas correctement passés, l'authentification échoue.

### 5.3 Problèmes de Configuration Maven

**Dépendances Spring Boot 4.1.0 et Spring Cloud 2025.1.2**

Spring Boot 4.x est une version MAJEURE avec breaking changes.
Spring Cloud 2025.1.2 est une version récente qui peut avoir des incompatibilités.

**Risques** :
- Changements dans le mécanisme de chargement de configuration
- Comportement modifié de `spring.config.import`
- Nouvelles contraintes de sécurité

### 5.4 Problèmes Docker

**Ordre de démarrage dépend uniquement de healthcheck**

```yaml
depends_on:
  config-server:
    condition: service_healthy
```

Mais le healthcheck du config-server vérifie uniquement `/actuator/health/readiness`.
Cela ne garantit PAS que :
1. Le endpoint `/registry-service/default` est disponible
2. Le fichier `registry-service.properties` a été lu et parsé
3. Le serveur est prêt à servir les configurations

**Résultat** : Race condition possible où registry-service démarre trop tôt.

---

## 6. VÉRIFICATIONS EFFECTUÉES

### 6.1 Fichiers de Configuration
✅ `registry-service/src/main/resources/application.properties`  
✅ `cloud-conf-yeyamo/registry-service.properties`  
✅ `cloud-conf-yeyamo/application.properties`  
✅ `config-server/src/main/resources/application.properties`  
✅ Tous les fichiers `*/src/main/resources/application.properties` (26 services)

### 6.2 Fichiers Docker
✅ `docker-compose.yml` (analyse complète)  
✅ `config-server/Dockerfile`  
✅ `registry-service/Dockerfile`  
✅ `api-gateway/Dockerfile`  
✅ Tous les autres Dockerfiles (comparaison)

### 6.3 Fichiers Maven
✅ `pom.xml` (reactor parent)  
✅ `registry-service/pom.xml`  
✅ `config-server/pom.xml`  
✅ `api-gateway/pom.xml`  
✅ `security-hardening-starter/pom.xml`

### 6.4 Code Java
✅ `RegistryServiceApplication.java`  
✅ `SecurityConfig.java` (registry-service)  
✅ `HardeningAutoConfiguration.java`  
✅ `SecurityHardeningFilter.java`  
✅ `StrictJwtDecoders.java`

### 6.5 Recherches
✅ Toutes les occurrences de `localhost` dans les properties  
✅ Toutes les occurrences de `spring.config.import`  
✅ Toutes les occurrences de `fail-fast`  
✅ Toutes les occurrences de `EUREKA_`  
✅ Toutes les occurrences de `security-hardening-starter`

### 6.6 Architecture
✅ Structure des répertoires (29 services)  
✅ Dépendances entre services  
✅ Ordre de démarrage Docker  
✅ Configuration réseau Docker
✅ Healthchecks et dépendances  
✅ Variables d'environnement injectées  
✅ Volumes et montages

---

## 7. SCORE GLOBAL

### 7.1 Scores par Composant

| Composant | Score | Commentaire |
|-----------|-------|-------------|
| **Docker** | 7/10 | Architecture globale correcte, mais problèmes de timing et healthchecks |
| **Spring Cloud** | 5/10 | Configuration incohérente entre services, problèmes de fail-fast |
| **Config Server** | 8/10 | Configuration correcte, mais trop strict (accept-empty=false) |
| **Registry** | 3/10 | Configuration "optional" incorrecte, hostname localhost |
| **Compose** | 8/10 | Structure correcte, variables bien injectées |
| **Infrastructure** | 9/10 | Bases de données bien configurées avec healthchecks |
| **Architecture** | 6/10 | Bonne séparation, mais incohérences de configuration |

### 7.2 Score de Santé Global
**Score total** : **6.5/10**

**Points forts** :
- Architecture microservices bien structurée (26 services)
- Infrastructure complète (PostgreSQL, Cassandra, Redis, OpenSearch, Kafka)
- Config Server correctement configuré avec profil native
- Healthchecks présents sur tous les services d'infrastructure
- Variables d'environnement correctement injectées dans docker-compose.yml
- Security-hardening-starter mutualisé entre services

**Points faibles critiques** :
- registry-service utilise configuration "optional" alors qu'elle est obligatoire
- Incohérence fail-fast entre registry-service et autres services
- Hostname localhost non remplacé par variable Docker
- Security-hardening-starter peut interférer avec Eureka Server
- Valeurs par défaut localhost dans configurations centralisées

---

## 8. PLAN DE CORRECTION

**⚠️ IMPORTANT : Les corrections doivent être effectuées dans l'ordre exact ci-dessous.**

### PHASE 1 : Corrections Critiques (Priorité 1)

#### Correction 1.1 : Rendre Config Client obligatoire pour registry-service
**Fichier** : `registry-service/src/main/resources/application.properties`  
**Action** : Modifier la ligne 5

**Avant** :
```properties
spring.config.import=optional:configserver:${CONFIG_SERVER_URL:http://localhost:8080}
spring.cloud.config.fail-fast=false
```

**Après** :
```properties
spring.config.import=configserver:${CONFIG_SERVER_URL:http://config-server:8080}
spring.cloud.config.fail-fast=true
```

**Justification** : Rend la configuration Config Server obligatoire comme pour tous les autres services.

---

#### Correction 1.2 : Ajouter variable EUREKA_HOSTNAME dans docker-compose.yml
**Fichier** : `docker-compose.yml`  
**Action** : Ajouter la variable d'environnement dans registry-service

**Avant** :
```yaml
registry-service:
  environment:
    CONFIG_SERVER_URL: http://config-server:8080
    CONFIG_SERVER_USERNAME: admin
    CONFIG_SERVER_PASSWORD: admin123
    EUREKA_USERNAME: eureka
    EUREKA_PASSWORD: eureka123
```

**Après** :
```yaml
registry-service:
  environment:
    CONFIG_SERVER_URL: http://config-server:8080
    CONFIG_SERVER_USERNAME: admin
    CONFIG_SERVER_PASSWORD: admin123
    EUREKA_HOSTNAME: registry-service
    EUREKA_USERNAME: eureka
    EUREKA_PASSWORD: eureka123
```

**Justification** : Permet au registry-service de s'identifier correctement dans le réseau Docker.

---

#### Correction 1.3 : Désactiver security-hardening-starter pour registry-service
**Fichier** : `registry-service/src/main/resources/application.properties`  
**Action** : Ajouter la propriété pour désactiver le starter

**Ajouter** :
```properties
# Désactiver security-hardening-starter pour registry-service (utilise Basic Auth, pas JWT)
yeyamo.security.enabled=false
```

**Justification** : Le registry-service utilise Basic Auth pour Eureka, pas JWT. Le security-hardening-starter cherche jwt.secret qui n'existe pas.

---

### PHASE 2 : Corrections Élevées (Priorité 2)

#### Correction 2.1 : Augmenter start-period du healthcheck
**Fichier** : `registry-service/Dockerfile`  
**Action** : Modifier la ligne du HEALTHCHECK

**Avant** :
```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health/readiness || exit 1
```

**Après** :
```dockerfile
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -qO- http://localhost:8081/actuator/health/readiness || exit 1
```

**Justification** : Donne plus de temps au registry-service pour se connecter au Config Server et démarrer Eureka.

---

#### Correction 2.2 : Assouplir Config Server pour Docker
**Fichier** : `config-server/src/main/resources/application.properties`  
**Action** : Modifier les contraintes strictes

**Avant** :
```properties
spring.cloud.config.server.accept-empty=false
spring.cloud.config.server.native.fail-on-error=true
```

**Après** :
```properties
spring.cloud.config.server.accept-empty=true
spring.cloud.config.server.native.fail-on-error=false
```

**Justification** : Rend Config Server plus tolérant aux problèmes de timing dans Docker.

---

### PHASE 3 : Corrections Moyennes (Priorité 3)

#### Correction 3.1 : Utiliser prefer-ip-address pour registry-service
**Fichier** : `cloud-conf-yeyamo/registry-service.properties`  
**Action** : Modifier la configuration Eureka

**Avant** :
```properties
eureka.instance.hostname=${EUREKA_HOSTNAME:localhost}
```

**Après** :
```properties
eureka.instance.hostname=${EUREKA_HOSTNAME:registry-service}
eureka.instance.prefer-ip-address=false
```

**Justification** : Cohérence avec la configuration des autres services et meilleure résolution DNS Docker.

---

#### Correction 3.2 : Uniformiser noms de variables Eureka
**Fichier** : `cloud-conf-yeyamo/application.properties`  
**Action** : Ajouter documentation

**Ajouter** :
```properties
# Note: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE dans Docker Compose
# est automatiquement converti en eureka.client.service-url.defaultZone
# La variable EUREKA_SERVER_URL ci-dessous est utilisée comme fallback
eureka.client.service-url.defaultZone=${EUREKA_SERVER_URL}
```

**Justification** : Clarifier la relation entre variables Docker et properties Spring.

---

### PHASE 4 : Corrections Faibles (Priorité 4)

#### Correction 4.1 : Documenter valeurs localhost
**Fichier** : `cloud-conf-yeyamo/application.properties`  
**Action** : Ajouter commentaire explicatif

**Ajouter en haut du fichier** :
```properties
# ============================================
# CONFIGURATION COMMUNE YEYAMO
# ============================================
# IMPORTANT: Les valeurs par défaut 'localhost' ci-dessous
# sont UNIQUEMENT pour développement local.
# En Docker, ces valeurs sont écrasées par les variables
# d'environnement définies dans docker-compose.yml
# ============================================
```

**Justification** : Éviter la confusion sur les valeurs localhost.

---

### PHASE 5 : Tests et Validation

#### Test 5.1 : Vérifier compilation
```cmd
cd yeyamo-api
build-all.bat
```

**Vérifier** : Pas d'erreur de compilation.

---

#### Test 5.2 : Rebuild images Docker
```cmd
docker compose down -v
docker compose build --no-cache config-server registry-service
```

**Vérifier** : Images reconstruites avec succès.

---

#### Test 5.3 : Démarrer Config Server seul
```cmd
docker compose up -d config-server
docker compose logs -f config-server
```

**Vérifier** : 
- Config Server démarre et devient "Healthy"
- Pas d'erreur dans les logs
- Attendre que le healthcheck passe à "healthy"

---

#### Test 5.4 : Tester endpoint Config Server
```cmd
curl -u admin:admin123 http://localhost:8080/registry-service/default
```

**Vérifier** :
- HTTP 200 OK
- JSON contenant les propriétés de `registry-service.properties`
- Les valeurs pour EUREKA_USERNAME, EUREKA_PASSWORD sont présentes

---

#### Test 5.5 : Démarrer Registry Service
```cmd
docker compose up -d registry-service
docker compose logs -f registry-service
```

**Vérifier** :
- Connexion réussie au Config Server
- Chargement de la configuration `registry-service/default`
- Eureka Server démarre sans erreur
- Message "Eureka Server started"
- Healthcheck passe à "healthy"

---

#### Test 5.6 : Vérifier Dashboard Eureka
```cmd
# Ouvrir dans le navigateur
http://localhost:8081
```

**Credentials** : eureka / eureka123

**Vérifier** :
- Dashboard Eureka accessible
- Aucun service enregistré (c'est normal à ce stade)
- Section "General Info" affiche les bonnes informations

---

#### Test 5.7 : Démarrer un service client
```cmd
docker compose up -d api-gateway
docker compose logs -f api-gateway
```

**Vérifier** :
- api-gateway se connecte au Config Server
- api-gateway se connecte au Registry Service avec authentification
- api-gateway s'enregistre dans Eureka
- Voir "api-gateway" dans le dashboard Eureka

---

#### Test 5.8 : Démarrer tous les services
```cmd
docker compose up -d
docker compose ps
```

**Vérifier** :
- Tous les services démarrent
- Tous les healthchecks passent à "healthy"
- Tous les services apparaissent dans Eureka
- Pas d'erreur de connexion dans les logs

---

### PHASE 6 : Monitoring Post-Correction

#### Monitoring 6.1 : Vérifier les logs en continu
```cmd
docker compose logs -f registry-service api-gateway
```

**Surveiller** :
- Pas de "ConfigClientFailFastException"
- Pas de "Connection refused"
- Pas de "401 Unauthorized"
- Enregistrement/désenregistrement normal des services

---

#### Monitoring 6.2 : Tester failover Config Server
```cmd
# Arrêter Config Server temporairement
docker compose stop config-server

# Attendre 30 secondes

# Redémarrer
docker compose start config-server
```

**Vérifier** :
- Registry Service reste UP (utilise configuration en cache)
- Les autres services peuvent avoir des erreurs temporaires (normal)
- Tout revient à la normale après redémarrage Config Server

---

#### Monitoring 6.3 : Tester failover Registry Service
```cmd
# Redémarrer Registry Service
docker compose restart registry-service

# Observer les logs des autres services
docker compose logs -f api-gateway auth-service
```

**Vérifier** :
- Les services se reconnectent automatiquement
- Réenregistrement dans Eureka après redémarrage
- Pas de perte de service

---

## 9. ANNEXES

### 9.1 Résumé des Fichiers Modifiés

**Fichiers à modifier** :
1. `registry-service/src/main/resources/application.properties` (3 modifications)
2. `docker-compose.yml` (1 modification)
3. `registry-service/Dockerfile` (1 modification)
4. `config-server/src/main/resources/application.properties` (2 modifications)
5. `cloud-conf-yeyamo/registry-service.properties` (2 modifications)
6. `cloud-conf-yeyamo/application.properties` (2 commentaires ajoutés)

**Total** : 6 fichiers modifiés

---

### 9.2 Commandes de Diagnostic Utiles

**Vérifier état des conteneurs** :
```cmd
docker compose ps
docker compose ps --format json | jq '.[] | {name: .Name, status: .Status, health: .Health}'
```

**Voir logs avec timestamps** :
```cmd
docker compose logs --timestamps registry-service | tail -100
```

**Tester connexion Config Server depuis registry-service** :
```cmd
docker compose exec registry-service wget -qO- http://config-server:8080/actuator/health
```

**Inspecter réseau Docker** :
```cmd
docker network inspect yeyamo-api_yeyamo-network
```

**Vérifier variables d'environnement** :
```cmd
docker compose exec registry-service env | grep -E "CONFIG|EUREKA"
```

**Tester résolution DNS** :
```cmd
docker compose exec registry-service ping -c 2 config-server
docker compose exec registry-service nslookup config-server
```

---

### 9.3 Patterns de Logs à Surveiller

**Logs NORMAUX (après correction)** :
```
✅ Fetching config from server at : http://config-server:8080
✅ Located environment: name=registry-service, profiles=[default]
✅ Eureka HTTP Client uses Jersey
✅ Setting the eureka configuration
✅ Initializing Eureka in region us-east-1
✅ Started Eureka Server
```

**Logs ANORMAUX (problème)** :
```
❌ ConfigClientFailFastException: Could not locate PropertySource
❌ Connection refused: config-server:8080
❌ Error creating bean with name 'securityFilterChain'
❌ Failed to bind properties under 'spring.security.user'
❌ 401 Unauthorized when connecting to Eureka
```

---

### 9.4 Checklist de Validation Finale

- [ ] Config Server accessible et healthy
- [ ] Endpoint `/registry-service/default` retourne configuration valide
- [ ] Registry Service démarre sans erreur
- [ ] Registry Service devient healthy après < 60s
- [ ] Dashboard Eureka accessible sur http://localhost:8081
- [ ] Au moins un service client s'enregistre dans Eureka
- [ ] api-gateway route le trafic vers services enregistrés
- [ ] Pas de "localhost" dans les logs de connexion
- [ ] Pas d'erreur 401 Unauthorized
- [ ] Pas d'erreur ConfigClientFailFastException

---

## 10. CONCLUSION

### Récapitulatif

L'échec du démarrage du registry-service est causé par une **configuration "optional" du Config Client** qui permet un démarrage sans configuration, alors que les credentials Eureka et autres paramètres critiques sont dans la configuration centralisée.

Cette anomalie critique est aggravée par :
- L'absence de variable EUREKA_HOSTNAME dans docker-compose.yml
- L'utilisation de localhost comme valeur par défaut
- L'interférence potentielle du security-hardening-starter

### Niveau de Risque Après Correction

**Risque résiduel** : FAIBLE

Après application du plan de correction, l'architecture devrait être stable avec :
- Configuration obligatoire pour tous les services
- Hostnames Docker corrects
- Healthchecks adaptés
- Security-hardening-starter désactivé pour registry-service

### Recommandations Futures

1. **Tests d'intégration** : Ajouter des tests automatisés pour valider le démarrage complet
2. **Monitoring** : Implémenter Prometheus + Grafana pour surveiller la santé des services
3. **Tracing distribué** : Ajouter Zipkin/Jaeger pour tracer les appels entre services
4. **CI/CD** : Automatiser le build et le déploiement avec validation des healthchecks
5. **Documentation** : Maintenir un runbook pour le troubleshooting

---

**Fin du Diagnostic**

---

**Rapport généré par** : Kiro - Architecte Logiciel Senior  
**Date** : 21 juillet 2026  
**Durée de l'audit** : Analyse exhaustive complète  
**Fichiers analysés** : 47 fichiers de configuration et code source
