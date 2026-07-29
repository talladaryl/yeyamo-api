# Audit Kafka, performances admin et environnement local

## Synthèse

Date de l'audit : 2026-07-29.

Le backend dispose déjà d'un socle distribué sérieux : outbox transactionnelle sur les producteurs critiques, tables d'inbox/événements traités sur la majorité des consumers, pagination des principales listes admin, Spring Kafka avec DLT dans presque tous les services, Actuator/Micrometer et une stack Compose couvrant les dépendances techniques.

Les changements de cette passe renforcent la compatibilité des contrats Kafka, ferment le trou de reprise de `commerce-service`, alignent les index sur les filtres admin réellement exécutés, évitent un chargement N+1 dans Booking et rendent la stack locale vérifiable automatiquement.

## Kafka

### Contrat d'événement

Le module partagé `event-contracts` définit l'enveloppe :

| Champ | Règle |
| --- | --- |
| `eventId` | UUID obligatoire, clé d'idempotence |
| `eventType` | type métier stable |
| `version` | version canonique, entière et positive |
| `eventVersion` | alias temporaire émis pour compatibilité |
| `producer` | nom du microservice |
| `occurredAt` | instant UTC |
| `correlationId` | corrélation propagée ou `eventId` par défaut |
| `aggregateId` | clé métier utilisée également comme clé Kafka |
| `payload` | données métier versionnées |

`payment-service` utilise désormais ce contrat pour les nouveaux événements. Son consumer accepte `version` et l'ancien `eventVersion`. La migration des autres producteurs peut donc être progressive sans interruption.

### Idempotence observée

Les consumers critiques de Payment, Booking, Ticket, Partner, Admin, Notification, Commerce, Moderation et Mission Reward possèdent une inbox ou une table `processed_events` persistante. Le traitement métier et l'enregistrement de l'identifiant sont transactionnels dans les consumers inspectés.

Analytics utilise deux protections persistantes : `analytics_inbox_processed` pour les agrégats métier et `eventId` unique dans le journal d'ingestion. Une redelivery ne réapplique donc pas les agrégats.

### Outbox observée

Une outbox transactionnelle est présente notamment dans :

- `payment-service`
- `booking-service`
- `ticket-service`
- `partner-service`
- `admin-service`
- `auth-service`
- `commerce-service`
- `moderation-trust-service`
- `mission-reward-service`
- `catalog-service`

Les publishers publient par lots bornés et ne marquent `publishedAt` qu'après acquittement du broker. Les événements restent en base en cas d'échec.

### Retry et DLT

Les consumers critiques utilisent un `DefaultErrorHandler` avec retry borné. `commerce-service`, seul trou détecté dans le périmètre financier, possède maintenant un backoff exponentiel borné et une publication vers `<topic>.DLT`.

Le service Compose `kafka-init` précrée les DLT critiques :

- `payment.commands.DLT`
- `payment.events.DLT`
- `booking.events.DLT`
- `partner-events.DLT`
- `ticket.events.DLT`
- `admin.audit.events.DLT`

Les observations Kafka Spring sont activées globalement. Les métriques natives de listeners/templates sont exposées via Actuator/Prometheus. Les tableaux de bord doivent suivre au minimum débit, erreurs, retries, DLT et consumer lag.

## Performances des APIs admin

### Endpoints inspectés

- `GET /api/v1/admin/platform-users`
- `GET /api/v1/booking-management/bookings`
- `GET /api/v1/payments/admin`
- `GET /api/v1/analytics/admin/dashboard`
- `GET /api/v1/moderation/reports`

Toutes ces listes sont paginées côté stockage. La taille maximale est bornée à 200 dans Users, Booking et Payment. Aucun `findAll()` non paginé n'est utilisé sur ces parcours.

### Index ajoutés

| Service | Index | Justification |
| --- | --- | --- |
| Auth | trigram email/téléphone | recherche `%texte%` observée |
| Auth | `(status, created_at)` | filtre statut + tri récent |
| Auth | `(role_id, user_id)` | filtre rôle |
| Booking | trigram référence/activity/user | recherche multi-colonnes observée |
| Booking | `slot_id` | jointure vers créneau |
| Booking | `(status, payment_status, created_at)` | filtre console principal |
| Payment | `(status, created_at)` | file financière |
| Payment | provider id/idempotency | recherches exactes |
| Payment | trigram user/provider/idempotency | recherche globale observée |
| Moderation | `(status, priority, created_at)` | queue opérateur |
| Moderation | target/status/date | dossier cible |
| Moderation | assignee/priority/date | file assignée |

`BookingRepository.findAll(Specification, Pageable)` applique maintenant un `EntityGraph` sur `slot`, évitant une requête supplémentaire par réservation lors du mapping de la page.

### EXPLAIN et p95

Les plans réels et p95 ne sont pas inventés : ils nécessitent une base peuplée représentative et une stack saine. Le scénario `tests/load/admin-api.k6.js` couvre les cinq endpoints prioritaires et impose :

- taux d'erreur inférieur à 1 % ;
- p95 inférieur à 750 ms ;
- propagation d'un `X-Correlation-Id`.

Commande :

```powershell
$env:ADMIN_TOKEN="<jwt-admin>"
$env:BASE_URL="http://localhost:8083"
k6 run tests/load/admin-api.k6.js
```

Pour les plans SQL, activer temporairement `auto_explain` ou extraire les requêtes Hibernate avec leurs paramètres sur un jeu de données anonymisé, puis exécuter `EXPLAIN (ANALYZE, BUFFERS)`. Les valeurs avant/après doivent être archivées avec le volume de lignes, la distribution des statuts et la machine de test.

## Tests

Couverture existante constatée :

- statut/rôles/sessions des utilisateurs ;
- transitions Booking et concurrence via verrou pessimiste/version ;
- paiement, refund, idempotence webhook/commands et outbox ;
- KYC et contrat d'approbation partenaire ;
- modération, audit, sanctions et redelivery ;
- attribution Mission/XP et doublons ;
- approbation Campaign et scopes ;
- QR/ticketing et inventaire ;
- routage Gateway Campaign/Admin et correlation ID.

Ajouts :

- sérialisation et validation de l'enveloppe commune ;
- compatibilité `version`/`eventVersion` ;
- consumer Payment avec version canonique ;
- configuration DLT Commerce ;
- scénario de charge admin.

Les tests Testcontainers sont déjà déclarés dans Campaign, Ads et Ticket. Leur extension à toutes les bases ne doit pas dupliquer des tests unitaires : elle doit cibler migrations, contraintes d'unicité, verrous et vraie redelivery Kafka.

## Environnement local

### Couverture Compose

La stack contient Gateway, Config Server, Eureka, les services admin et métier demandés, PostgreSQL/PostGIS, Redis, Kafka/Redpanda, Cassandra et OpenSearch. Neo4j est commenté car le graphe actif n'en dépend pas dans la configuration courante.

Les communications inter-conteneurs utilisent les noms DNS Compose (`postgres`, `redis`, `kafka`, `registry-service`, `config-server`) et non `localhost`.

Les dépendances d'infrastructure utilisent des healthchecks. `api-gateway`, `config-server`, `registry-service` et `support-service` s'exécutent maintenant avec un utilisateur non-root ; les trois derniers possèdent également un healthcheck d'image.

La majorité des Dockerfiles copie un JAR déjà construit dans une image JRE minimale. Ce choix n'est pas un multi-stage autonome, mais évite Maven dans les images finales et permet au build reactor de résoudre les modules partagés une seule fois. `support-service` conserve un multi-stage autonome.

### Commandes

Validation de la configuration :

```powershell
docker compose config --quiet
.\scripts\verify-local-stack.ps1
```

Construction reactor puis démarrage :

```powershell
.\mvnw.cmd clean package
docker compose up --build -d
.\scripts\verify-local-stack.ps1
```

Si aucun wrapper Maven n'est présent :

```powershell
mvn clean package
docker compose up --build -d
```

Diagnostic :

```powershell
docker compose ps
docker compose logs -f postgres kafka config-server registry-service api-gateway
```

Accès local :

- Gateway : `http://127.0.0.1:8083`
- Config Server health : `http://127.0.0.1:8080/actuator/health`
- Eureka : `http://127.0.0.1:8761`
- OpenSearch : `http://127.0.0.1:9200`

## Risques et travaux suivants

1. Migrer progressivement tous les producteurs vers `event-contracts`, puis retirer `eventVersion` après une fenêtre de compatibilité documentée.
2. Ajouter une politique de rétention et une procédure de replay contrôlé pour chaque DLT.
3. Ajouter un dashboard consumer lag et des alertes DLT non vides.
4. Exécuter les plans SQL et le test k6 avec des volumes proches de la production ; renseigner les p95 mesurés.
5. Remplacer les valeurs locales par variables/secrets CI hors Git en staging et production. Les valeurs Compose par défaut sont uniquement des identifiants de développement.
6. Étendre les tests Testcontainers aux contraintes financières et aux migrations Kafka/PostgreSQL les plus critiques.
