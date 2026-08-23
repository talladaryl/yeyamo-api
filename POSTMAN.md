# Tester les API YeYamo : Swagger et Postman

La documentation HTTP est disponible dans le navigateur et la collection Postman est générée depuis les contrôleurs Spring. Les deux utilisent l'API Gateway comme point d'entrée local : `http://localhost:8083`.

## Démarrer la plateforme

Depuis la racine du dépôt, construire les JAR puis démarrer les services :

```powershell
.\scripts\prepare-docker-stack.ps1
```

Vérifier que la gateway et les services nécessaires sont sains :

```powershell
.\scripts\verify-local-stack.ps1
```

Les routes sont alors appelées avec `http://localhost:8083/api/v1/...`. Les services continuent aussi d'exposer leur documentation directement sur leur port Docker, ce qui aide à diagnostiquer un service isolé.

## Swagger dans le navigateur

Ouvrir [Swagger UI de la gateway](http://localhost:8083/swagger-ui.html). Le sélecteur en haut de la page permet de consulter la gateway, les services d'authentification, les utilisateurs, les partenaires, le contenu, la culture, les évènements, le commerce, les paiements, l'administration, l'analytics et la configuration pays.

Chaque service référencé est aussi disponible à l'adresse `http://localhost:<port>/swagger-ui.html` et sa spécification brute à `http://localhost:<port>/v3/api-docs`. Les URL et les ports sont déclarés dans `cloud-conf-yeyamo/api-gateway.properties`.

Les spécifications sont accessibles sans JWT. Les opérations protégées demandent toutefois un Bearer JWT dans le bouton **Authorize**. Obtenir d'abord un jeton avec `POST /api/v1/auth/login`, puis saisir `Bearer <token>`.

## Importer et utiliser Postman

La collection et son environnement local sont versionnés ici :

- `docs/postman/YeYamo_API.postman_collection.json`
- `docs/postman/YeYamo_Local.postman_environment.json`

Dans Postman :

1. Cliquer sur **Import**, sélectionner les deux fichiers, puis choisir l'environnement **YeYamo local**.
2. Laisser `baseUrl` à `http://localhost:8083`, ou le remplacer par l'URL d'une gateway distante.
3. Appeler `POST /api/v1/auth/login` (ou `POST /api/v1/auth/register`) : le jeton JWT retourné est **automatiquement sauvegardé** dans la variable d'environnement `accessToken` via le script de test intégré.
4. Les variables `countryCode` (`CM`), `cityId`, `placeId`, `eventId`, `ticketId`, `partnerId`, `artisanId`, `conversationId` et `id` sont pré-configurées avec des identifiants valides.
5. Les requêtes d'écriture (`POST`, `PUT`, `PATCH`) sont pré-remplies avec des corps JSON réalistes conformes aux DTOs réels et incluent automatiquement les en-têtes `X-Correlation-ID` et `X-Idempotency-Key`.

La collection définit une authentification Bearer au niveau global. Les routes publiques sont explicitement configurées en `No Auth`. Ne jamais versionner un jeton réel dans l'environnement.

## Régénérer après une évolution d'API

Après avoir ajouté, supprimé ou modifié un contrôleur, exécuter :

```powershell
.\scripts\generate-postman-collection.ps1
```

Le générateur unifié analyse l'ensemble du code source Java des microservices, extrait les routes, DTOs, paramètres et sécurités, et synchronise à la fois :
1. La spécification **OpenAPI 3.1** (`api-gateway/src/main/resources/static/mobile-api/openapi.json`)
2. La collection **Postman v2.1.0** (`docs/postman/YeYamo_API.postman_collection.json`)
3. L'environnement **Postman** (`docs/postman/YeYamo_Local.postman_environment.json`)

## Couverture des APIs

La suite couvre **100% des 465 endpoints réels** répartis en 33 domaines fonctionnels (Authentication, Users, Places, Events, Culture, Commerce, Ticketing, Messaging, Bookings, Payments, Notifications, Partners, Admin, etc.).
