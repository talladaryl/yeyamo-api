# Audit d’architecture Culture et Artisanat YeYamo

## 1. Périmètre et règles structurantes

Cet audit précède toute modification métier. PostgreSQL reste la source transactionnelle, Kafka transporte les changements, OpenSearch porte les projections de découverte et Neo4j porte uniquement les relations calculées. Aucun profil utilisateur, partenaire ou contenu social ne doit être dupliqué.

Standards obligatoires : ISO 3166-1 alpha-2 (`countryCode`), BCP 47 (`languageCode`), ISO 4217 (`currencyCode`), `Instant` UTC en persistance, `BigDecimal` pour l’argent et coordonnées dans les bornes WGS84.

## 2. Architecture existante constatée

| Service | État utile au périmètre | Données/technologies réutilisables | Limites actuelles |
|---|---|---|---|
| `partner-service` | Profil partenaire, propriétaire utilisateur, KYC, documents, historique, outbox | PostgreSQL, Kafka/outbox | Aucun type artisan ni profil public artisan détaillé |
| `catalog-service` | Assets, collections, statuts éditoriaux, références, outbox | PostgreSQL, Kafka/outbox | `AssetType` limité à destination/place/experience/event; géographie en chaînes camerouno-centrées; pas de provenance/authenticité/traductions |
| `content-service` | Publications sociales | PostgreSQL/Kafka selon flux existants | Ne doit pas devenir source de vérité des œuvres ou savoirs culturels |
| `interaction-service` | Avis, commentaires et interactions | Persistance propre | Référence seulement les agrégats; aucune propriété Culture/Artisanat |
| `feed-service` | Projection de flux | Kafka, Cassandra/stockage de lecture selon configuration | Projection seulement |
| `discovery-service` / `search-service` | Recherche et découverte | OpenSearch | Les documents Culture/Artisanat et leur mapping n’existent pas |
| `recommendation-service` | Recommandations | Projections/événements | Manque les signaux artisan, langue, transmission et authenticité |
| `gamification-service` / `mission-reward-service` | XP, missions, récompenses | PostgreSQL/Kafka | Peut suivre apprentissage/défis, mais ne doit pas posséder les leçons culturelles |
| `moderation-trust-service` | Signalements, décisions, confiance | PostgreSQL/Kafka | Doit recevoir les nouveaux types de cible |
| `media-service` | Médias centralisés | Stockage objet et références sécurisées | Les nouveaux agrégats doivent stocker des références, pas des URL permanentes |
| `analytics-service` | KPI et événements analytiques | Kafka/projections | Taxonomie Culture/Artisanat absente |
| `commerce-service` | Promotions, commissions, ledger | PostgreSQL | Doit posséder offre commerciale/prix/inventaire, pas l’identité culturelle de l’œuvre |
| `payment-service` | Paiement/remboursement | PostgreSQL, idempotence | Consomme une commande/réservation; ne possède pas l’œuvre |
| `notification-service` | Notifications | Kafka | Nouveaux événements à mapper |
| `user-service` | Profil utilisateur | PostgreSQL | Référence auteur/contributeur uniquement; aucun profil dupliqué |
| `place-service` | Régions, villes, districts, lieux, PostGIS, outbox | PostgreSQL/PostGIS | Aucun pays, langue de pays ou niveaux administratifs génériques; régions globalement uniques |
| `ingestion-service` | Imports asynchrones | Kafka/stockage de jobs | Nouveaux formats Culture/Artisanat à ajouter ultérieurement |
| `graph-service` | Application vide, désactivée dans Compose | Neo4j prévu | À réactiver comme projection, jamais comme source transactionnelle |
| `api-gateway` | Routage par service | Eureka/Gateway | Les routes `/countries` devront être ajoutées à `place-service` |

## 3. Architecture cible et propriétaires

### 3.1 Décision de découpage

Aucun nouveau microservice n’est nécessaire pour le premier incrément. Ajouter un service `culture-service` ou `artisan-service` dupliquerait le catalogue, les partenaires, la modération et la recherche. Un module Java partagé de contrats de domaine non persistants est recommandé pour les value objects.

| Besoin | Service propriétaire | Justification |
|---|---|---|
| Pays, niveaux administratifs, langues disponibles | `place-service` | Possède déjà le référentiel géographique et PostGIS |
| Profil artisan et compétences | `partner-service` | Un artisan est une spécialisation/capacité d’un partenaire rattaché à un utilisateur |
| Œuvre, provenance, authenticité, histoire | `catalog-service` | Agrégat culturel éditorial distinct d’un post et indépendant de l’offre commerciale |
| Offre, prix, stock, vente | `commerce-service` | Responsabilité commerciale; référence `artworkId` |
| Paiement | `payment-service` | Transaction financière existante |
| Publications parlant d’une œuvre | `content-service` | Stocke une référence `artworkId`, jamais une copie de l’œuvre |
| Langues, proverbes, contes, traditions, personnages, événements historiques, pratiques | `catalog-service` | Contenu culturel structuré, versionné et publiable |
| Mini-leçons et quiz | `catalog-service` pour le contenu; `gamification-service` pour progression/réponses | Séparation contenu de référence / état utilisateur |
| Défis et récompenses | `mission-reward-service` | Capacité existante |
| Contributions communautaires | `catalog-service` avec workflow éditorial; auteur référencé depuis `user-service` | Évite la duplication du profil et garantit la modération |
| Médias audio/vidéo/image | `media-service` | Références opaques et accès signé |
| Recherche | `discovery-service`/`search-service` | Projection OpenSearch alimentée par Kafka |
| Relations sémantiques | `graph-service` réactivé | Projection Neo4j alimentée par Kafka |

## 4. Modèles et migrations recommandés

### 4.1 Socle partagé

- `CountryReference(countryCode)`
- `AdministrativeAreaReference(countryCode, level, areaId)`
- `GeoReference(countryCode, adminLevel1Id, adminLevel2Id, cityId, localityId, latitude, longitude)`
- `LanguageReference(languageCode)`
- `LocalizedText(languageCode, title, description)`
- `Money(amount: BigDecimal, currencyCode)`
- `MediaReference(mediaId, mediaType, altText, displayOrder)`

Ces objets valident les formats mais ne font aucun appel réseau et ne deviennent pas des entités JPA partagées.

### 4.2 Référentiel pays (`place-service`)

Nouvelles tables :

- `countries(code, name, launch_status, default_currency_code, default_timezone, active, created_at, updated_at)`;
- `country_languages(country_code, language_code, display_name, official, primary_language)`;
- à terme `administrative_areas(id, country_code, parent_id, level, code, name, slug, active)`.

Extensions progressives :

- `regions.country_code` nullable initialement, backfill fiable en `CM`, puis contrainte `NOT NULL`;
- unicité future `(country_code, code)` et `(country_code, slug)` au lieu d’une unicité mondiale;
- `cities` et `districts` restent compatibles via leurs parents;
- les données ambiguës sont inscrites dans une table de journal de migration et ne sont pas forcées.

### 4.3 Artisan (`partner-service`)

Réutiliser `partners`, `partner_documents`, l’historique KYC et l’outbox. Ajouter :

- `partner_capabilities(partner_id, capability)` avec `ARTISAN`;
- `artisan_profiles(partner_id PK/FK, public_name, biography, country_code, admin_level1_id, city_id, locality_id, primary_language_code, international_visibility, shipping_policy, created_at, updated_at)`;
- `artisan_skills(partner_id, skill_code, experience_years)`;
- `artisan_languages(partner_id, language_code, proficiency)`.

Ne pas créer de table utilisateur artisan. `owner_user_id` reste la liaison d’identité.

### 4.4 Œuvres (`catalog-service`)

Étendre `AssetType` avec des types dédiés sans transformer une œuvre en `PLACE`. Ajouter un agrégat `Artwork` ou une extension 1:1 typée du catalogue :

- `artworks(asset_id PK/FK, artisan_partner_id, country_code, admin_level1_id, city_id, locality_id, creation_period, materials, technique, dimensions, authenticity_status, provenance_summary, visibility, sale_status, created_at, updated_at)`;
- `artwork_translations(asset_id, language_code, title, description, story)`;
- `artwork_provenance_entries(id, artwork_id, sequence, event_type, occurred_at, location, custodian_reference, evidence_media_id)` append-only;
- `artwork_media(artwork_id, media_id, purpose, display_order)`;
- `authenticity_records(id, artwork_id, verifier_type, verifier_id, status, evidence_media_id, issued_at, revoked_at)`;
- les collections existantes sont réutilisées avec ordre et publication.

L’offre commerciale (`price`, `currency`, stock, expédition) reste dans `commerce-service` et référence l’œuvre.

### 4.5 Culture et transmission (`catalog-service`)

Nouvelles tables structurées :

- `cultural_items(id/asset_id, kind, country_code, admin_level1_id, admin_level2_id, city_id, locality_id, source_language_code, period_start, period_end, sensitivity_level, status)`;
- `cultural_item_translations(item_id, language_code, title, summary, body)`;
- `cultural_item_media(item_id, media_id, purpose, transcript_language_code)`;
- `cultural_item_sources(id, item_id, source_type, citation, contributor_user_id, verification_status)`;
- `learning_lessons(id, cultural_item_id, level, estimated_minutes, status)`;
- `quiz_questions`, `quiz_options` dans le catalogue; réponses/progression dans `gamification-service`;
- `community_contributions(id, target_type, target_id, contributor_user_id, payload, status, moderation_reference, created_at)`.

Kinds recommandés : `LANGUAGE_EXPRESSION`, `PROVERB`, `TALE`, `TRADITION`, `HISTORICAL_PERSON`, `HISTORICAL_EVENT`, `CULTURAL_PRACTICE`, `HERITAGE`, `GASTRONOMY`. Ce sont des enums versionnés côté domaine, pas des textes libres.

## 5. APIs cibles

### Référentiel public

- `GET /api/v1/countries`
- `GET /api/v1/countries/{countryCode}`
- `GET /api/v1/countries/{countryCode}/administrative-areas`
- `GET /api/v1/countries/{countryCode}/languages`

### Artisans

- `GET /api/v1/artisans`
- `GET /api/v1/artisans/{partnerId}`
- `PUT /api/v1/partners/{partnerId}/artisan-profile`
- `GET /api/v1/artisans/{partnerId}/artworks`
- endpoints admin de validation via les contrôleurs partenaire existants.

### Œuvres

- `GET /api/v1/artworks`, `GET /api/v1/artworks/{id}`
- `POST /api/v1/artworks`, `PUT /api/v1/artworks/{id}`
- `POST /api/v1/artworks/{id}/submit`, endpoints admin approve/reject/authenticity
- `GET /api/v1/artworks/{id}/provenance`

### Culture et apprentissage

- `GET /api/v1/culture/items`, `GET /api/v1/culture/items/{id}`
- endpoints de contribution et workflow éditorial sous `/api/v1/culture/contributions`
- `GET /api/v1/culture/lessons`, `GET /api/v1/culture/lessons/{id}`
- soumission de quiz/progression dans `gamification-service`.

Toutes les listes sont paginées, filtrables par `countryCode`, niveaux administratifs, `languageCode`, type et statut.

## 6. Événements Kafka et projections

Utiliser l’enveloppe commune existante (`eventId`, `eventType`, `version`, `producer`, `occurredAt`, `correlationId`, `aggregateId`, `payload`) et l’outbox transactionnelle déjà présente dans `partner-service`, `catalog-service` et `place-service`.

Événements :

- `CountryEnabled`, `CountryConfigurationUpdated`, `LanguageReferenceUpdated`;
- `ArtisanProfilePublished`, `ArtisanProfileUpdated`;
- `ArtworkCreated`, `ArtworkPublished`, `ArtworkAvailabilityChanged`, `ArtworkAuthenticityChanged`, `ArtworkProvenanceAppended`;
- `CulturalItemPublished`, `CulturalItemUpdated`, `CulturalContributionSubmitted`, `CulturalContributionApproved`;
- `LessonPublished`, `QuizCompleted`.

Consumers critiques enregistrent `eventId`; retry borné et DLQ selon la convention existante.

## 7. OpenSearch

Indexes/projections recommandés :

- `yeyamo-artisans-v1`: identité publique, compétences, langues, géographie et visibilité;
- `yeyamo-artworks-v1`: traductions, matériaux, technique, artisan, provenance résumée, authenticité, disponibilité, pays;
- `yeyamo-culture-v1`: kind, traductions, langue source, géographie, périodes, tags, médias/transcriptions;
- alias sans version pour lecture et reindexation atomique.

Ne jamais indexer documents KYC, contacts privés, preuves d’authenticité sensibles ou URLs signées.

## 8. Neo4j (`graph-service` à réactiver)

`graph-service` est actuellement une coquille désactivée dans Compose. Il doit devenir un consumer de projections Kafka.

Nœuds : `User`, `Partner`, `Artisan`, `Artwork`, `CulturalItem`, `Language`, `Country`, `AdministrativeArea`, `HistoricalPerson`, `HistoricalEvent`, `Topic`.

Relations : `OWNS_PROFILE`, `CREATED`, `ORIGINATES_FROM`, `SPEAKS`, `TRANSLATED_IN`, `MENTIONS`, `RELATED_TO`, `PART_OF_COLLECTION`, `INFLUENCED`, `PARTICIPATED_IN`, `LEARNED`, `CONTRIBUTED_TO`.

Seuls identifiants, relations et attributs de projection sont stockés. Toute reconstruction doit être possible depuis PostgreSQL/Kafka.

## 9. Permissions

- Lecture publique : contenus `PUBLISHED` et artisans visibles.
- Écriture artisan : propriétaire du partenaire ou membre autorisé (`ARTISAN_CONTENT_WRITE`).
- Contribution : utilisateur authentifié (`CULTURE_CONTRIBUTE`).
- Édition/validation : `CULTURE_EDITOR`, `CULTURE_MODERATOR`, `ARTWORK_REVIEW`.
- Authenticité : permission dédiée `ARTWORK_AUTHENTICITY_VERIFY`.
- Administration pays : `SUPER_ADMIN` ou `COUNTRY_CONFIGURATION_MANAGE`.
- Les acteurs proviennent toujours du JWT; aucun `actorId` client n’est digne de confiance.

## 10. Risques de duplication et protections

- **Profil artisan dupliqué** : lier 1:1 au partenaire existant et conserver `owner_user_id`.
- **Œuvre copiée dans les posts** : stocker uniquement `artworkId` dans `content-service`.
- **Prix dans le catalogue** : interdire; `commerce-service` possède l’offre.
- **Géographie textuelle divergente** : migrer vers références et garder les anciens champs seulement pendant transition.
- **Traductions en colonnes** : utiliser tables de traductions avec unicité `(aggregate_id, language_code)`.
- **Neo4j comme source** : proscrire toute mutation métier directe du graphe.
- **URLs média permanentes** : conserver `mediaId`; générer les accès signés à la bordure.
- **Double traitement Kafka** : processed-event store par consumer.

## 11. Ordre d’implémentation

1. Module partagé de value objects et validations ISO/BCP47.
2. Référentiel pays/langues dans `place-service`, API et routage Gateway.
3. Migration progressive de `regions` avec backfill Cameroun et journal d’ambiguïtés.
4. Extension artisan de `partner-service`.
5. Agrégats œuvres et contenus culturels dans `catalog-service`.
6. Offre commerciale référencée dans `commerce-service`.
7. Projections OpenSearch dans Discovery/Search.
8. Réactivation de `graph-service` comme projection Neo4j.
9. Contributions, modération, apprentissage et gamification.
10. Analytics, notifications, ingestion et administration complète.

## 12. Risques de livraison

- Les services utilisent des versions Spring Boot hétérogènes/récentes; le module partagé doit rester Java pur.
- Les contraintes uniques actuelles de `regions` sont globales et doivent être remplacées avec prudence.
- Les identifiants géographiques existants sont des `Long`; la migration multi-pays doit préserver les URLs et clés étrangères.
- `graph-service` n’a actuellement ni modèle ni consumer ni configuration opérationnelle.
- La recherche est répartie entre `discovery-service` et `search-service`; un seul propriétaire d’écriture OpenSearch devra être retenu avant implémentation des projections.
- L’attribution automatique de `CM` ne doit viser que les données dont la provenance camerounaise est certaine.
