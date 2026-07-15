# YeYamo API — état d'avancement

Évaluation mise à jour le 15 juillet 2026 à partir du code présent, des
configurations centralisées et des architectures V1/V2.

Le pourcentage mesure l'achèvement d'une **V1 exploitable**, pas la préparation
à la production. Grille utilisée : métier/API 35 %, persistance et Flyway 15 %,
sécurité/configuration 15 %, fiabilité Kafka et idempotence 15 %, tests 10 %,
conteneurisation et exploitation 10 %. Pour les services d'infrastructure, la
grille est adaptée à leur responsabilité réelle.

| Service | Pourcentage effectué |
|---|---:|
| `admin-service` | 82 % |
| `analytics-service` | 78 % |
| `api-gateway` | 88 % |
| `auth-service` | 85 % |
| `booking-service` | 88 % |
| `catalog-service` | 90 % |
| `config-server` | 92 % |
| `content-service` | 86 % |
| `discovery-service` | 85 % |
| `event-service` | 85 % |
| `feed-service` | 88 % |
| `gamification-service` | 86 % |
| `graph-service` | 0 % |
| `ingestion-service` | 88 % |
| `interaction-service` | 90 % |
| `media-service` | 85 % |
| `messaging-service` | 5 % |
| `mission-reward-service` | 88 % |
| `moderation-trust-service` | 88 % |
| `notification-service` | 85 % |
| `partner-service` | 85 % |
| `payment-service` | 5 % |
| `place-service` | 80 % |
| `recommendation-service` | 88 % |
| `referral-service` | 88 % |
| `registry-service` | 92 % |
| `search-service` | 0 % |
| `social-service` | 0 % |
| `user-service` | 85 % |

## Synthèse précise

- Avancement moyen des 26 modules actifs : **80 %**. Moyenne des 29 dossiers,
  anciens squelettes inclus : **72 %**.
- **23 services** ont un socle V1 ou infrastructure substantiel (80 % et plus).
- `analytics-service` utilise OpenSearch pour ses projections, avec consommation
  idempotente, retry exponentiel et DLT. `event-service` utilise désormais
  Flyway, JWT et une Outbox transactionnelle.
- `place-service` est une façade legacy dépréciée : ses écritures sont relayées
  par Outbox vers `catalog-service`, qui devient la source cible. Les anciens
  contrats restent disponibles pendant la transition.
- `payment-service` est le blocage fonctionnel principal : sans lui, la Saga de
  `booking-service` ne peut pas confirmer les réservations payantes en réel.
- `messaging-service` reste un squelette. `social-service` et `search-service`
  ne doivent pas être développés séparément : leurs responsabilités sont déjà
  couvertes par content/interaction/feed et discovery. `graph-service` reste
  expérimental et hors du plan V2 actuel.
- Une collision de ports par défaut reste à corriger avant un lancement global :
  `admin-service`/`gamification-service` sur `8096`. `analytics-service` utilise
  maintenant `8097`.
- Il manque encore une validation bout en bout commune avec PostgreSQL, Kafka,
  Redis, PostGIS, stockage objet et fournisseurs externes, ainsi qu'un Docker
  Compose global, du tracing distribué et des tests de charge.
