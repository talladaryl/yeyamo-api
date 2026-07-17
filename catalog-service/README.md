# Catalog Service

Service de gestion du catalogue YeYamo : assets (lieux, expériences, destinations) et références (régions, villes, catégories).

## 📦 Fonctionnalités

### Assets Catalog
- Gestion des lieux, expériences et destinations
- Recherche par proximité géographique (PostGIS)
- Recherche textuelle et filtres (région, catégorie)
- Statuts de workflow (DRAFT, PENDING_REVIEW, PUBLISHED, ARCHIVED, DELETED)
- Synchronisation avec le legacy place-service

### References
- Taxonomie : régions, villes, catégories
- Hiérarchie parent/enfant
- Activation/désactivation

### Collections ✨ *Nouveau*
- Listes personnalisées d'assets (ex: "Mes restaurants préférés", "Voyage à Paris")
- Collections publiques ou privées
- Image de couverture personnalisable
- Ajout/retrait de lieux
- Gestion de la propriété avec protection IDOR

## 🗄️ Base de données

### Tables principales
- `catalog_assets` - Assets du catalogue (PostGIS)
- `catalog_references` - Taxonomie (régions, villes, catégories)
- `collections` - Collections d'utilisateurs ✨
- `collection_places` - Association collection ↔ assets ✨
- `catalog_outbox` - Pattern Outbox pour événements Kafka
- `catalog_processed_events` - Idempotence des événements consommés

### Migrations Flyway
- `V1__create_catalog_schema.sql` - Schéma assets + outbox
- `V2__create_catalog_references.sql` - Taxonomie
- `V3__create_collections.sql` - Collections ✨

## 🔌 API Endpoints

### Assets (`/api/v1/catalog/assets`)
- `GET /{id}` - Récupérer un asset publié
- `GET /slug/{slug}` - Récupérer par slug
- `GET /manage/{id}` - Récupérer n'importe quel statut (management)
- `GET ?type&regionCode&categoryCode&q&limit` - Recherche
- `GET /nearby?lat&lng&radiusKm&type&categoryCode&limit` - Recherche géographique
- `POST` - Créer un asset 🔒
- `PUT /{id}` - Modifier un asset 🔒
- `PATCH /{id}/status` - Changer le statut 🔒
- `DELETE /{id}` - Soft-delete 🔒

### References (`/api/v1/catalog/{kind}`)
- `GET` - Lister (kind: regions, cities, categories)
- `GET /{code}` - Récupérer par code
- `POST` - Créer une référence 🔒
- `PUT /{code}` - Modifier 🔒
- `PATCH /{code}/active` - Activer/désactiver 🔒

### Collections (`/api/v1/collections`) ✨
- `GET` - Mes collections (paginé)
- `GET /public` - Collections publiques (paginé)
- `GET /{id}` - Détail collection + lieux (404 si privée et non-propriétaire)
- `GET /summaries` - Version allégée (pour dropdowns)
- `POST` - Créer une collection 🔒
- `PUT /{id}` - Modifier (propriétaire uniquement) 🔒
- `DELETE /{id}` - Supprimer (propriétaire uniquement) 🔒
- `POST /places` - Ajouter un lieu (idempotent) 🔒
- `DELETE /{id}/places/{assetId}` - Retirer un lieu 🔒

🔒 = Authentification JWT requise

## 📡 Événements Kafka

**Topic:** `catalog.events` (configurable via `yeyamo.kafka.topics.catalog-events`)

### Événements Assets
- `catalog.asset.created`
- `catalog.asset.updated`
- `catalog.asset.status_changed`
- `catalog.asset.deleted`
- `catalog.asset.synchronized` (legacy sync)

### Événements References
- `catalog.reference.created`
- `catalog.reference.updated`
- `catalog.reference.activated`
- `catalog.reference.deactivated`

### Événements Collections ✨
- `catalog.collection.created`
- `catalog.collection.updated`
- `catalog.collection.deleted`
- `catalog.collection.place_added`
- `catalog.collection.place_removed`

**Pattern:** Outbox avec publication automatique toutes les 1000ms (configurable)

## 🔒 Sécurité

### JWT Bearer Token
- Extraction user ID depuis `Authentication.getName()`
- Tous les endpoints mutations requièrent l'authentification

### Protection IDOR (Collections)
- ✅ Vérification propriété sur `PUT`, `DELETE`, `addPlace`, `removePlace`
- ✅ 404 (pas 403) pour collection privée consultée par non-propriétaire
- ✅ Idempotence sur ajout de lieu existant

### Validation
- Bean Validation sur tous les DTOs
- Contraintes base de données (UNIQUE, CHECK, FK)
- Taille limite : title (120 chars), description (2000 chars)

## 🧪 Tests

### Tests unitaires
- `CollectionServiceTest` - Tests complets du service Collections
  - Création, modification, suppression
  - Protection IDOR (utilisateur B ne peut pas modifier collection de A)
  - 404 (pas 403) sur collection privée pour non-propriétaire
  - Idempotence ajout lieu existant
  - Validation assets invalides

### Exécution
```bash
./mvnw test
./mvnw test -Dtest=CollectionServiceTest
```

## 🚀 Configuration

### Variables d'environnement
```bash
SERVER_PORT=8088
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/yeyamo_catalog
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Optionnel
YEYAMO_KAFKA_TOPICS_CATALOG_EVENTS=catalog.events
YEYAMO_OUTBOX_ENABLED=true
YEYAMO_OUTBOX_PUBLISH_DELAY_MS=1000
```

## 📚 Documentation API

Swagger UI disponible sur : `http://localhost:8088/swagger-ui.html`

## 🔗 Intégrations futures (à documenter uniquement)

### recommendation-service
**Consommation potentielle des événements Collections:**
- `catalog.collection.place_added` → Améliorer recommandations basées sur les collections populaires
- `catalog.collection.created` → Suggérer collections similaires aux utilisateurs

**Implémentation:** Non réalisée dans catalog-service (responsabilité de recommendation-service)

## 📝 Commit History

1. **Schéma BDD** - Migration V3 collections + collection_places
2. **Entités JPA** - CollectionEntity, CollectionPlaceEntity, Repositories
3. **Service métier** - CollectionService avec sécurité IDOR
4. **Endpoints REST** - CollectionController + DTOs
5. **Tests** - Tests unitaires complets
6. **Documentation** - README + Swagger

## 🏗️ Architecture

```
catalog-service/
├── domain/
│   ├── model/          # CatalogAsset, CatalogReference
│   └── port/           # Repositories interfaces
├── application/
│   ├── CatalogAssetService.java
│   ├── CatalogReferenceService.java
│   └── CollectionService.java ✨
├── infrastructure/
│   ├── persistence/    # JPA entities, Spring Data repos
│   ├── outbox/         # Kafka outbox pattern
│   ├── messaging/      # Event consumers
│   └── security/       # JWT config
└── interfaces/
    └── rest/           # Controllers, DTOs, Exception handlers
```

**Date:** 17 juillet 2026  
**Auteur:** Senior Backend Engineer
