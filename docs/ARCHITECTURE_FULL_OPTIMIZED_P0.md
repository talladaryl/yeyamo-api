# YeYamo API — P0 Architecture FULL-OPTIMIZED

Date : 21 octobre 2026  
Périmètre : audit et corrections locales uniquement. Aucun déploiement, push, commit, publication d’image, migration de base existante ou accès VPS n’a été effectué.

## 1. Résumé exécutif

La décision produit est respectée : aucune fonctionnalité métier n’est supprimée, simulée ou désactivée pour économiser de la mémoire. Les 34 applications Java actives, Kafka/Redpanda, Redis, PostgreSQL/PostGIS, OpenSearch et Neo4j restent dans le périmètre FULL.

Les corrections locales réalisées sont limitées et réversibles :

- la projection PostGIS de `discovery-service` persiste désormais les champs culturels/artisans déjà prévus par Flyway ;
- les filtres culturels, territoriaux, disponibilité et vérification sont appliqués par PostGIS au lieu d’être ignorés ;
- la visibilité `PUBLIC`/`PUBLIC_HERITAGE`, la recherche préfixe et le classement par popularité sont alignés autant que possible avec OpenSearch ;
- une migration Flyway additive ajoute la visibilité, les alias et l’index trigramme sans supprimer de données ;
- sept valeurs par défaut de bases PostgreSQL sont alignées sur les noms créés par Compose ;
- les optimisations Maven/Docker/CI déjà présentes sont conservées et documentées, sans recompilation massive sur le VPS.

OpenSearch et Neo4j ne sont pas supprimés. Leur migration vers PostgreSQL nécessite une phase dual-run, un backfill, une comparaison de résultats et une bascule réversible qui dépassent une correction locale sûre.

## 2. État initial et baseline

### 2.1 Dépôt et chaîne de build

- Reactor Maven : 39 modules, dont 34 applications Java actives.
- Compose local : 34 applications, PostgreSQL/PostGIS, Redis, Redpanda, OpenSearch et Neo4j.
- Compose production : hérite des services locaux, retire les ports/montages locaux et prévoit un override d’images immuables.
- Modifications préexistantes conservées : `.env.production.example`, scripts `build-all*`, `docker-compose*.yml`, `deploy/*`, workflow GHCR, script de build d’images et `docs/BUILD_DEPLOYMENT_AUDIT.md`.
- Les fichiers `hs_err_pid*.log` et `replay_pid*.log` non suivis n’ont pas été modifiés.

### 2.2 Matrice fonctionnelle

| Fonctionnalité | Service responsable | Dépendances principales | Contrats/API | Kafka ou projection | Risque P0 |
|---|---|---|---|---|---|
| Authentification, inscription, sessions | `auth-service` | PostgreSQL, Redis, Config, Eureka, Kafka | `/api/v1/auth/**` | user/partner/auth events | OTP, JWT, sécurité |
| Profils utilisateurs | `user-service` | PostgreSQL, Kafka, Redis | `/api/v1/users/**` | user events | cohérence profil |
| Pays, villes, régions | `country-config-service`, `place-service` | PostgreSQL, Redis, Config, Eureka | `/api/v1/countries/**`, `/api/v1/places/**` | country/place events | configuration territoriale |
| Lieux et suggestions | `place-service` | PostgreSQL, Redis, Kafka | places/suggestions/regions | place events | géolocalisation |
| Événements et sorties | `event-service` | PostgreSQL, Kafka, Config, Eureka | `/api/v1/events/**` | event events | publication |
| Publications, stories | `content-service` | PostgreSQL, Kafka, Config, Eureka | `/api/v1/posts/**`, `/api/v1/stories/**` | content events | modération/expiration |
| Interactions, reviews, check-ins | `interaction-service` | PostgreSQL, Redis, Kafka | interactions/reviews/check-ins | interaction events | idempotence |
| Feed | `feed-service` | PostgreSQL, Redis, Kafka | `/api/v1/feed/**` | feed events | classement/cache |
| Médias et stockage | `media-service` | PostgreSQL, stockage objet, Kafka | `/api/v1/media/**` | media events | secrets/stockage |
| Recherche et exploration | `discovery-service`, `recommendation-service` | PostgreSQL/PostGIS, Redis, Kafka, OpenSearch optionnel | discovery/maps/explore | discovery projection | parité des filtres |
| Culture, traditions, langues | `culture-service` | PostgreSQL, Kafka | culture/languages/traditions | culture events | contenu culturel |
| Œuvres et artisans | `catalog-service`, `partner-service` | PostgreSQL, Kafka | artworks/artisans/catalog | catalog/artisan/partner events | contrats partenaires |
| Administration | `admin-service`, `auth-service` | PostgreSQL, Redis, Kafka | `/api/v1/admin/**` spécialisé | admin/audit events | autorisation |
| Modération et confiance | `moderation-trust-service` | PostgreSQL, Kafka | moderation/trust | moderation events | contrôle obligatoire |
| Réservations | `booking-service` | PostgreSQL, Redis, Kafka | bookings/activities | booking/payment commands | états métier |
| Paiements et webhooks | `payment-service` | PostgreSQL, Kafka, fournisseur externe | payments/webhook | payment commands/events | aucun contournement |
| Billetterie | `ticket-service` | PostgreSQL, Redis/Redisson, Kafka | tickets/participants | ticket events | verrouillage inventaire |
| Commerce | `commerce-service` | PostgreSQL, Kafka | commerce/orders/offers | commerce/payment events | prix/commandes |
| Messagerie | `messaging-service` | PostgreSQL, Redis, Kafka/WebSocket | messaging/WebSocket | messaging events | connexions longues |
| Notifications | `notification-service` | PostgreSQL, Kafka, fournisseurs push/mail | notifications/newsletters | notification events | retries/delivery |
| Gamification | `gamification-service`, `mission-reward-service` | PostgreSQL, Redis, Kafka | XP/badges/missions/rewards | gamification/mission events | calculs idempotents |
| Parrainage | `referral-service` | PostgreSQL, Redis, Kafka | referrals | referral events | anti-fraude |
| Campagnes et publicité | `campaign-service`, `ads-delivery-service` | PostgreSQL, Redis, Kafka | campaigns/ads | campaign/ad events | budgets/caps |
| Analytics | `analytics-service` | PostgreSQL, OpenSearch, Redis, Kafka | analytics/dashboard/KPI | projections et event log | migration non prouvée |
| Ingestion | `ingestion-service` | PostgreSQL, Kafka | catalog imports | ingestion events | jobs longs |
| Graphe culturel | `graph-service` | Neo4j, Kafka | culture-graph | graph commands | parité graphe |
| Support | `support-service` | PostgreSQL, Config, Eureka | admin/support | support events | disponibilité |
| Discovery/runtime | `config-server`, `registry-service`, `api-gateway` | Config storage, Eureka, Redis | gateway/routes/health | service discovery | démarrage progressif |

`search-service` et `social-service` restent des modules du reactor mais ne sont pas activés dans Compose ; leur activation ne peut pas être déduite de la seule présence de classes d’application.

### 2.3 Écarts locale/production

- Compose injecte les URLs PostgreSQL canoniques `yeyamo_*` par service ; plusieurs valeurs par défaut du Config Server divergeaient lorsque Compose n’injectait pas la variable.
- La production exige les secrets et l’URI Git du Config Server ; le local utilise la configuration native montée en volume.
- La production utilise le même healthcheck applicatif dans les Dockerfiles, sans exposer les ports hôte.
- Les limites JVM/Docker sont des plafonds et non des mesures runtime.

## 3. Scénarios FULL-OPTIMIZED

| Scénario | RAM/CPU | Démarrage | Fiabilité | Sécurité | Migration | Décision |
|---|---|---|---|---|---|---|
| A — FULL actuelle optimisée | 34 JVM + 3 moteurs de données ; pression très élevée sur 11 GiB | améliorable par reactor et readiness | élevée si les dépendances restent disponibles | isolation actuelle | faible | référence immédiate |
| B — FULL avec moteurs rationalisés | gain potentiel si OpenSearch/Neo4j sont migrés, non mesurable avant parité | plus simple après bascule | risque élevé pendant dual-run | à vérifier pour analytics et graphe | élevée | étude uniquement |
| C — mutualisation de processus | quelques JVM économisables théoriquement | un processus plus lourd à redémarrer | blast radius accru | isolation réduite | très élevée | ne pas implémenter sans validation explicite |

Architecture retenue pour le lancement : scénario A, avec optimisations de build, projection PostGIS et démarrage progressif. Kafka et Redis sont conservés. Config Server et Eureka sont conservés initialement. Aucune fusion de microservices n’est implémentée.

## 4. Lot 2 — Recherche PostGIS

### Problème initial et preuves

`V3__culture_artisan_support.sql` ajoutait les colonnes culturelles à `discovery_documents`, mais `DiscoveryDocumentEntity` ne les mappait pas et `PostgisDiscoverySearchAdapter` les ignorait. OpenSearch appliquait pourtant ces filtres. Cela créait une divergence fonctionnelle, pas seulement une divergence de performance.

### Modifications réalisées

- Mapping JPA des champs pays, zone, ville, traductions, langues, communauté, tags, matériaux, techniques, artisan, vérification, disponibilité, prix, popularité, visibilité et alias.
- Application des filtres `countryCode`, `countries`, `adminLevel1Id`, `cityId`, `languageCode`, `cultureType`, `materialId`, `techniqueId`, `availability` et `verified` en recherche normale et géographique.
- Filtre de visibilité publique aligné sur OpenSearch.
- Recherche texte conservée via `tsvector`/GIN et complétée par une recherche préfixe `ILIKE` sur titre/alias.
- Classement trends enrichi par la popularité, sans modifier le contrat HTTP.
- Migration `V4__discovery_visibility_and_fuzzy_search.sql` additive : `launch_visibility`, `aliases`, extension `pg_trgm` et index trigramme partiel.

### Limites

Le classement exact `ts_rank`/distance PostgreSQL n’est pas mathématiquement identique au `_score` OpenSearch. Une comparaison sur données représentatives reste nécessaire avant de rendre PostGIS seul obligatoire. OpenSearch reste donc activable et conservé.

### Tests

Le test unitaire ajouté vérifie la persistance de tous les champs culturels et le passage des filtres au repository. Le test d’équivalence contre deux moteurs réels nécessite PostgreSQL/PostGIS et OpenSearch avec un dataset contrôlé ; il n’a pas été exécuté localement.

Statut : **partiel — correction locale terminée, équivalence dual-engine à démontrer**.

## 5. Lot 3 — Étude OpenSearch vers PostgreSQL

| Index OpenSearch | Usage constaté | Équivalent PostgreSQL | Migration proposée | Risque |
|---|---|---|---|---|
| `user_engagement` | engagement utilisateur | table de faits journaliers + index utilisateur/date | backfill depuis événements/outbox | agrégations et rétention |
| `region_activity` | activité par région | faits journaliers région/date | projection Kafka + index composite | recomputation historique |
| `place_popularity` | popularité de lieux | table de scores ou vue matérialisée | double écriture puis comparaison | fraîcheur/classement |
| `analytics_partner_dimensions` | dimension partenaire | table dimensionnelle PostgreSQL | backfill idempotent | schéma d’attributs |
| `partner_analytics` | dashboard partenaire | faits partenaire/date/type | vues matérialisées ciblées | latence analytique |
| `kpi_history` | historique KPI | table temporelle partitionnée | projection + contrôle de somme | séries temporelles |
| `analytics_content_dimensions` | dimensions contenu | table dimensionnelle | projection des événements content | évolution JSON |
| `analytics_catalog_dimensions` | dimensions catalogue | table dimensionnelle | projection catalog/artisan | cohérence cross-service |
| `analytics_event_logs` | recherche d’événements | `analytics_event_store` + index JSONB/date | retention/backfill/requêtes SQL | recherche textuelle et volume |

Le service possède déjà plusieurs tables PostgreSQL pour faits, inbox, agrégats et analytics culturelles, mais conserve des `ElasticsearchRepository`, `ElasticsearchOperations` et un client OpenSearch pour les usages opérationnels. L’équivalence n’est donc pas démontrée.

### Plan non destructif

1. Ajouter les tables/index PostgreSQL sans toucher aux index OpenSearch.
2. Rejouer les événements depuis l’outbox/Kafka vers une projection PostgreSQL idempotente.
3. Comparer par fenêtre temporelle : cardinalité, sommes, top-N, latence et résultats paginés.
4. Exécuter un dual-read masqué ou shadow-read, sans modifier la réponse client.
5. Définir un seuil d’écart accepté par KPI et une durée d’observation.
6. Basculer par fonctionnalité avec feature flag réversible.
7. Conserver OpenSearch pendant la période de rollback ; aucune suppression automatique.

Statut : **bloqué pour bascule — étude et plan seulement**.

## 6. Lot 4 — Étude Neo4j vers PostgreSQL

`graph-service` utilise directement `Neo4jClient`, des requêtes Cypher, des contraintes de labels et un rebuild qui détruit puis reconstruit la projection Neo4j. Les contrats exposent les voisinages, parcours culturels et découvertes par pays/langue.

### Représentation PostgreSQL candidate

- `graph_nodes(id, node_type, business_id, properties_jsonb, active, updated_at)` avec contrainte unique `(node_type, business_id)` ;
- `graph_edges(id, source_node_id, relation_type, target_node_id, properties_jsonb, updated_at)` avec contrainte unique sur les extrémités et le type ;
- index `(source_node_id, relation_type)`, `(target_node_id, relation_type)` et `(relation_type)` ;
- CTE récursives limitées à la profondeur contractuelle ;
- projection Kafka idempotente et job de rebuild par snapshot/version.

### Risques

Les relations orientées et les labels Neo4j ne sont pas automatiquement équivalents à des lignes SQL. Le rebuild actuel est particulièrement sensible : une migration naïve pourrait rendre le graphe vide ou perdre les relations pendant la reconstruction. Les performances des parcours multi-sauts, la cohérence avec les événements Kafka et la visibilité doivent être comparées.

### Plan

Créer les tables en parallèle, rejouer les mêmes événements, comparer les voisinages et parcours sur un dataset figé, mesurer les P50/P95, puis activer par endpoint derrière un flag. Neo4j reste la source opérationnelle jusqu’à validation et aucun nouveau provider PostgreSQL n’est activé automatiquement.

Statut : **bloqué pour bascule — conception non destructive uniquement**.

## 7. Lot 5 — Mémoire JVM

### État constaté

- défaut applicatif : `-Xms64m -Xmx256m -XX:+UseSerialGC` ; production ajoute `-XX:ActiveProcessorCount=1` ;
- les Dockerfiles utilisent `UseContainerSupport` et `ActiveProcessorCount=1` ;
- plafond Docker précédent : `512M` et `1.0` CPU par service ;
- pools Hikari généralement à 5, plusieurs `minimum-idle=0` ;
- analytics et graph ont des consommateurs/schedulers, tandis que ticket, messaging et media ont des buffers ou connexions spécifiques.

Aucun heap n’a été abaissé arbitrairement. Il manque les mesures RSS, committed heap, threads, connexions, buffers et GC par service ; une optimisation chiffrée n’est donc pas revendiquée.

### Profils recommandés

| Profil | Politique |
|---|---|
| Développement | valeur actuelle, services ciblés, logs lisibles |
| Production faible charge | `Xms` bas, `Xmx` par classe de service, `ActiveProcessorCount=1`, pools min-idle bas |
| Production normale | dimensionnement par mesure, aucun plafond inférieur au pic observé |

Les services critiques (`auth`, `booking`, `payment`, `ticket`, `gateway`, `discovery`) doivent être mesurés avant toute réduction. Si la FULL dépasse durablement 11 GiB, la conclusion correcte est d’augmenter la RAM ou de répartir les services, pas de forcer des heaps instables.

Statut : **partiel — garde-fous et instrumentation à compléter sur VPS**.

## 8. Lot 6 — PostgreSQL

Le cluster contient environ 32 bases logiques, `max_connections=300`, `shared_buffers=256MB` et des pools généralement limités à 5. La valeur 300 n’a pas été réduite sans mesure : le plafond théorique de 34 pools peut déjà dépasser 150 connexions, avant migrations, jobs et outils.

Correction réalisée : alignement des valeurs par défaut du Config Server sur les bases effectivement créées par Compose : `auth`, `booking`, `discovery`, `mission`, `partner`, `payment` et `referral`. Les URLs `SPRING_DATASOURCE_URL` injectées par Compose restent prioritaires.

PgBouncer est une option à évaluer avec les transactions Flyway, WebSocket et verrous ; il n’est pas introduit automatiquement. Aucun regroupement de bases et aucune migration existante n’ont été exécutés.

Statut : **partiel — incohérences corrigées, dimensionnement runtime restant à mesurer**.

## 9. Lot 7 — Disponibilité et démarrage

Ordre retenu : PostgreSQL/Redis/Kafka/OpenSearch/Neo4j → Config Server → Eureka → services métier → Gateway et contrôles publics. Compose utilise déjà `service_healthy` pour les dépendances critiques initiales, et les Dockerfiles communs exposent `/actuator/health/readiness` avec une fenêtre de démarrage.

Limites : `depends_on` ne garantit pas la disponibilité continue ; le Gateway ne doit pas être considéré comme preuve de disponibilité de tous les downstreams. Les retries Config/Kafka/Eureka et les politiques de redémarrage doivent rester actifs. Les services non critiques ne doivent pas bloquer indéfiniment les services cœur.

Statut : **partiel — chaîne locale alignée, test de redémarrage/chaos non exécuté**.

## 10. Lot 8 — Fiabilité Kafka/Redis

Kafka/Redpanda est conservé comme backbone événementiel. Les outbox, `acks=all`, idempotence producteur, consommateurs sans auto-commit, retries, DLT et déduplication observés ne sont pas remplacés par des appels HTTP synchrones.

Redis est conservé pour OTP, rate limiting, caches, anti-fraude et verrous de billetterie. Aucun contrôle de paiement, de modération, de sécurité ou d’idempotence n’a été retiré.

Statut : **terminé pour le périmètre d’optimisation — tests d’infrastructure non exécutés**.

## 11. Lot 9 — Gateway et sécurité

La route générique `/api/v1/admin/**` est à l’ordre par défaut, tandis que les routes spécialisées vérifiées utilisent des ordres négatifs (`-100` à `-200`). Les tests existants couvrent notamment les campagnes, platform-users, gamification, culture/artisans et feed.

Aucun conflit supplémentaire démontré n’a justifié une modification des routes. Changer l’ordre ou retirer un chemin sans matrice frontend aurait pu casser un contrat. Les services internes restent derrière la discovery/Gateway ; les règles JWT, autorisations et CORS ne sont pas affaiblies.

Statut : **partiel — audit et tests contractuels présents, validation runtime Gateway/Eureka à faire**.

## 12. Lot 10 — CI/CD et images Docker

Les corrections de la mission précédente sont conservées : reactor Maven unique, Dockerfiles `deploy/service.Dockerfile` et `deploy/runtime.Dockerfile`, cache BuildKit Maven verrouillé, workflow manuel GHCR, tags `sha-*`, Compose production et override d’images.

Le flux cible reste : GitHub Actions → Maven → images runtime → GHCR → Dokploy. Cette mission n’a publié aucune image et n’a activé aucun déploiement. Le rollback consiste à revenir à un tag immuable précédent et à conserver les volumes.

Statut : **terminé pour les fichiers locaux précédemment modifiés — exécution CI distante non réalisée**.

## 13. Budget mémoire et besoins matériels

Les estimations précédentes restent les seules valeurs disponibles : FULL actuelle environ `8,5–16,5 GiB`, FULL optimisée sans refonte profonde environ `7,5–14,5 GiB`, hors incertitude RSS, système, Traefik, frontend et Dokploy.

Les corrections de cette mission n’autorisent pas à annoncer un gain mémoire mesuré : elles améliorent surtout la cohérence et évitent des résultats incomplets. Tant que les 34 JVM et les trois moteurs persistent, le VPS de 11 GiB RAM + 4 GiB swap n’est pas démontré suffisant pour la charge simultanée complète.

Besoin minimal prudent : mesurer d’abord sur le VPS ; prévoir une capacité supérieure à 11 GiB pour la FULL simultanée, ou répartir les JVM et moteurs sur plusieurs machines. Le swap ne doit pas être compté comme RAM disponible pour la latence.

## 14. Tests exécutés

- `docker compose -f docker-compose.yml config --quiet` avec variables locales de validation : succès.
- `docker compose -f docker-compose.production.yml config --quiet` avec variables factices non secrètes : succès.
- `mvn -B -ntp -pl discovery-service -am -DskipTests -Dcheckstyle.skip=true package` : succès.
- `mvn -B -ntp -pl discovery-service -Dtest=PostgisDiscoverySearchAdapterTest,CultureDiscoverySearchTest test` : 11 tests, 0 échec.
- `mvn -B -ntp -pl discovery-service test` : 36 tests, 0 échec.
- `mvn -B -ntp -pl api-gateway -Dtest=AdminCampaignGatewayContractTest,CultureArtisanGatewayContractTest,FeedGatewayContractTest,MobileIntegrationContractTest test` : 10 tests, 0 échec.
- Les tests Gateway existants ont été inspectés ; aucun changement de route n’a été nécessaire.

## 15. Tests non exécutés et risques résiduels

- Test de comparaison PostGIS/OpenSearch sur un dataset réel : nécessite les deux moteurs et des fixtures représentatives.
- Test de migration Flyway contre PostgreSQL/PostGIS existant : volontairement non exécuté pour respecter l’interdiction de migration de données existantes.
- Dual-run Analytics et comparaison Neo4j/PostgreSQL : non implémentés, donc non exécutables.
- Build Docker et démarrage FULL : Dockerfile/Compose validés statiquement ; aucune publication ni démarrage de production effectué.
- Tests de charge, OOM, restart storm, coupure Kafka/Redis/Eureka et rollback : non exécutés.

Risques résiduels : classement différent entre moteurs, charge cumulée sur 11 GiB, ordre de démarrage sous panne persistante, pression OpenSearch/Neo4j, taille des outbox et tables analytics, et configuration Dokploy/Traefik distante non observable.

## 16. Plan de migration non destructif

1. Mesurer la FULL actuelle sur un environnement de préproduction équivalent.
2. Appliquer les migrations additives sur une base de test restaurée, jamais sur la base de production depuis cette mission.
3. Backfill par lots idempotents avec compteurs et checksum.
4. Activer les projections dual-write/dual-read masquées.
5. Comparer fonctionnellement et en performance sur des fenêtres contrôlées.
6. Basculer une capacité à la fois avec feature flag et période d’observation.
7. Conserver l’ancien moteur et les offsets/backups jusqu’à validation.

## 17. Rollback

- Code : revert local des commits futurs ou restauration des fichiers modifiés avant déploiement ; aucun commit n’a été créé par cette mission.
- Recherche : désactiver le flag PostGIS/OpenSearch concerné et conserver `discovery.search.engine=opensearch` si la parité échoue.
- Flyway : ne jamais supprimer les colonnes additives ; corriger le code puis restaurer une version compatible. Une migration déjà appliquée ne doit pas être éditée.
- Analytics/graph : aucune bascule effectuée ; le rollback consiste à ne pas activer les nouveaux readers.
- Images : remettre le tag GHCR immuable précédent dans l’override Dokploy.
- Données : ne supprimer aucun volume ni table ; restaurer uniquement via la procédure de backup validée.

## 18. Actions manuelles avant déploiement

- obtenir RSS/heap/threads/pools/GC de chaque JVM et la consommation des moteurs ;
- vérifier les noms de bases, secrets, URI Config Server, credentials Eureka et permissions GHCR ;
- exécuter Flyway V4 sur une base restaurée de staging ;
- exécuter le dataset de comparaison PostGIS/OpenSearch ;
- valider les routes Gateway avec frontend Next.js/Expo et un Eureka réel ;
- tester les flux auth, modération, réservation, paiement, webhook, ticket et notifications ;
- lancer le workflow CI en `publish=false`, puis publier uniquement après approbation ;
- configurer Dokploy/Traefik et le rollback d’image sans supprimer les volumes ;
- décider explicitement si le VPS doit être agrandi ou si la FULL doit être répartie.

## 19. Statut par lot

| Lot | Statut | Justification |
|---|---|---|
| 0 Baseline | terminé | Git, Maven, Compose, rapports et tests inventoriés |
| 1 Architecture | terminé | scénarios comparés, aucune fusion activée |
| 2 PostGIS | partiel | correction locale et tests unitaires ; parité runtime restante |
| 3 Analytics | bloqué pour migration | OpenSearch encore utilisé directement |
| 4 Neo4j | bloqué pour migration | Cypher/rebuild spécifiques, dual-run requis |
| 5 JVM | partiel | garde-fous existants, mesures VPS absentes |
| 6 PostgreSQL | partiel | noms de bases corrigés, tuning non arbitraire |
| 7 Disponibilité | partiel | healthchecks/retries présents, chaos non testé |
| 8 Flux métier | terminé pour ce périmètre | Kafka/Redis/outbox/sécurité préservés |
| 9 Gateway | partiel | conflits vérifiés, tests runtime restant |
| 10 CI/CD | terminé pour les fichiers locaux | chaîne précédente conservée, pas de publication |
| 11 Validation | partiel | compilation et tests ciblés réussis |
| 12 Rapport | terminé | ce document |

## Conclusion

La FULL-OPTIMIZED sûre consiste aujourd’hui à conserver tous les domaines et moteurs, corriger les incohérences prouvées, construire une seule fois via Maven/CI, démarrer progressivement et mesurer avant de réduire. La suppression d’OpenSearch, Neo4j ou de microservices n’est pas justifiée par les preuves disponibles et n’a pas été effectuée. La capacité simultanée sur 11 GiB reste une hypothèse non démontrée et doit être traitée par mesure puis par augmentation ou répartition matérielle si nécessaire.
