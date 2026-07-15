# YeYamo API — état d'avancement

> Sécurité : voir [l'audit et le guide de durcissement](docs/SECURITY_HARDENING.md) pour les contrôles communs, la rotation JWT, le chiffrement des secrets, la matrice OWASP ASVS et les risques résiduels de déploiement.

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
| `analytics-service` | 90 % |
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
| `messaging-service` | 85 % |
| `mission-reward-service` | 88 % |
| `moderation-trust-service` | 88 % |
| `notification-service` | 85 % |
| `partner-service` | 85 % |
| `payment-service` | 88 % |
| `place-service` | 80 % |
| `recommendation-service` | 88 % |
| `referral-service` | 88 % |
| `registry-service` | 92 % |
| `search-service` | 0 % |
| `social-service` | 0 % |
| `user-service` | 85 % |

## Synthèse précise

- Avancement moyen des 26 modules actifs : **87 %**. Moyenne des 29 dossiers,
  anciens squelettes inclus : **78 %**.
- **26 services** ont un socle V1 ou infrastructure substantiel (80 % et plus).
- `analytics-service` utilise OpenSearch avec des projections quotidiennes
  idempotentes pour l'engagement utilisateur, l'activité régionale, la popularité
  des lieux, les partenaires et les KPI. Le pipeline Strategy consomme les topics
  V2, enrichit les événements via des dimensions catalogue/contenu/partenaire,
  applique retry exponentiel et DLT, et limite les tableaux de bord aux
  propriétaires ou administrateurs. `event-service` utilise désormais Flyway,
  JWT et une Outbox transactionnelle.
- `place-service` est une façade legacy dépréciée : ses écritures sont relayées
  par Outbox vers `catalog-service`, qui devient la source cible. Les anciens
  contrats restent disponibles pendant la transition.
- `payment-service` couvre maintenant la Saga de réservation, les autorisations,
  annulations, remboursements, webhooks signés, Inbox/Outbox et l'idempotence.
  L'adaptateur simulé doit être remplacé par un fournisseur réel en production.
- `messaging-service` couvre les conversations directes et de groupe, les membres,
  messages, références de médias, réponses, édition/suppression logique, lecture,
  pagination, idempotence, Cassandra, Kafka et WebSocket privé. Les notifications
  sont déléguées à `notification-service` et les signalements à
  `moderation-trust-service`. Il reste à réaliser les tests d'intégration réels
  Cassandra/Kafka et le routage WebSocket par l'ingress de production.
- `social-service` et `search-service` ne doivent pas être développés séparément :
  leurs responsabilités sont déjà couvertes par content/interaction/feed et
  discovery. `graph-service` reste expérimental et hors du plan V2 actuel.
- Une collision de ports par défaut reste à corriger avant un lancement global :
  `admin-service`/`gamification-service` sur `8096`. `analytics-service` utilise
  maintenant `8097`.
- Il manque encore une validation bout en bout commune avec PostgreSQL, Kafka,
  Redis, PostGIS, stockage objet et fournisseurs externes, ainsi qu'un Docker
  Compose global, du tracing distribué et des tests de charge.

## Services restant à implémenter

Aucun nouveau microservice autonome prévu par le périmètre V2 ne reste à créer.
`analytics-service` nécessite encore une validation d'intégration avec un cluster
OpenSearch et un broker Kafka réels, mais son socle fonctionnel est implémenté.

`place-service` ne doit plus recevoir de nouvelles fonctionnalités. Il reste à
migrer ses données historiques vers `catalog-service`, basculer ses consommateurs,
puis retirer progressivement ses routes legacy.

`graph-service`, `search-service` et `social-service` ne sont pas à implémenter
comme microservices autonomes dans l'architecture V2 actuelle : leurs fonctions
sont respectivement couvertes par les projections sociales/recommandations,
`discovery-service`, puis `content-service` + `interaction-service` + `feed-service`.

Le chantier restant est transversal : Docker Compose global,
tests contractuels et bout en bout, observabilité distribuée, gestion centralisée
des secrets, résilience des dépendances externes et tests de charge.
