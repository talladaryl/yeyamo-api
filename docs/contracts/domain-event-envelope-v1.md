# Contrat événementiel YeYamo V1

Tous les événements interservices utilisent une enveloppe JSON stable. Les
champs de l'enveloppe ne doivent jamais être renommés ou supprimés dans une
version existante.

| Champ | Type | Règle |
|---|---|---|
| `eventId` | UUID | Identifiant global utilisé pour l'idempotence |
| `eventType` | string | Nom passé au format `domaine.action` |
| `eventVersion` | integer | Version du payload, supérieure ou égale à 1 |
| `occurredAt` | ISO-8601 UTC | Date de l'événement métier |
| `producer` | string | Nom Eureka du service producteur |
| `aggregateType` | string | Type de l'agrégat source |
| `aggregateId` | string | Identifiant stable de l'agrégat |
| `correlationId` | string | Corrélation du flux entrant ou UUID généré |
| `actorId` | string | Utilisateur/service auteur, `system` par défaut |
| `payload` | object | Données versionnées propres à l'événement |

Compatibilité :

- un producteur ajoute des champs de façon additive dans une même version ;
- une rupture de structure incrémente `eventVersion` ;
- un consommateur ignore les champs inconnus et refuse les versions futures ;
- chaque consommateur persiste `eventId` avant de confirmer le message ;
- les échecs permanents sont publiés dans `<topic>.DLT` ;
- toute écriture métier suivie d'une publication utilise une outbox
  transactionnelle.

Topics canoniques V2 :

- `place.events` : transition temporaire place vers catalog ;
- `event.events` : cycle de vie des événements touristiques ;
- `catalog.events` : source canonique des actifs touristiques ;
- `admin.events`, `partner-events`, `user-events` : contrats V1 existants.
