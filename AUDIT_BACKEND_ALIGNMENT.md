# YEYAMO API — audit d’alignement Backend / Mobile

**Date :** 16 septembre 2026  
**Périmètre :** audit statique du monorepo `yeyamo-api` et rapprochement avec `yeyamo-mobile/AUDIT_TEST1.md`.  
**Aucun service, contrat, configuration, test, migration ou frontend n’a été modifié.**

## 1. Statuts employés

| Statut | Signification |
|---|---|
| `EXISTANT` | Route et logique serveur présentes. |
| `PARTIEL` | La route existe mais ne couvre pas le comportement demandé ou ne renvoie pas les données affichées. |
| `NON RACCORDÉ` | Des éléments existent, mais les événements/services nécessaires ne sont pas connectés. |
| `À CRÉER` | Aucun contrat correspondant n’existe. |
| `À CORRIGER` | Une implémentation présente a un défaut vérifiable dans le code. |

## 2. Architecture constatée

### Entrée API et routage

`api-gateway` utilise des routes Spring Cloud Gateway MVC vers les services enregistrés. Les routes utilisateur importantes sont déjà présentes :

| Domaine | Préfixe gateway | Service |
|---|---|---|
| Authentification | `/api/v1/auth/**` | `auth-service` |
| Utilisateurs et réseau | `/api/v1/users/**` | `user-service` |
| Pays | `/api/v1/countries/**`, `/api/v1/administrative-areas/**`, `/api/v1/localities/**` | `country-config-service` |
| Lieux / suggestions | `/api/v1/places/**`, `/api/v1/place-suggestions/**` | `place-service` |
| Sorties | `/api/v1/events/**` | `event-service` |
| Posts / Stories | `/api/v1/posts/**`, `/api/v1/stories/**` | `content-service` |
| Interactions / avis / sauvegardes | `/api/v1/interactions/**`, `/api/v1/reviews/**`, `/api/v1/saves/**` | `interaction-service` |
| Feed | `/api/v1/feed/**` | `feed-service` |
| Recherche / carte | `/api/v1/discovery/**`, `/api/v1/maps/**` | `discovery-service` |
| Recommandations | `/api/v1/recommendations/**` | `recommendation-service` |
| Collections | `/api/v1/collections/**` | `catalog-service` |
| Réservations | `/api/v1/activities/**`, `/api/v1/bookings/**` | `booking-service` |
| Billets | `/api/v1/tickets/**` | `ticket-service` |
| Culture | `/api/v1/culture/**` | `culture-service` |
| Media | `/api/v1/media/**` | `media-service` |
| Notifications | `/api/v1/notifications/**` | `notification-service` |
| XP / Passport | `/api/v1/me/xp/**`, `/api/v1/me/passport/**`, etc. | `gamification-service` |
| Signalements / confiance | `/api/v1/moderation/**`, `/api/v1/trust/**` | `moderation-trust-service` |

**Écart confirmé :** `PublicFeedController` expose `/api/v1/public/feed` dans `feed-service`, mais le gateway ne route que `/api/v1/feed/**`. Le chemin public retourne donc `404` via le gateway alors que le contrôleur existe. Le feed mobile actuel utilise bien le chemin authentifié `/api/v1/feed`.

### Responsabilités des services

| Groupe | Services et responsabilité |
|---|---|
| Identité | `auth-service`, `user-service`, `country-config-service` : compte, session, profil, préférences, réseau social, pays/feature flags. |
| Contenu social | `content-service`, `feed-service`, `interaction-service`, `media-service`, `notification-service`, `moderation-trust-service`. |
| Découverte | `place-service`, `catalog-service`, `discovery-service`, `recommendation-service`, `graph-service`. |
| Sorties et monétisation | `event-service`, `booking-service`, `ticket-service`, `payment-service`. |
| Culture / engagement | `culture-service`, `gamification-service`, `mission-reward-service`, `referral-service`. |
| Plateforme | `api-gateway`, `config-server`, `registry-service`, `event-contracts`, `shared-lib`, `security`. |

Les écritures asynchrones importantes utilisent un outbox transactionnel puis Kafka. Les topics observés sont notamment `content.events`, `interaction.events`, `event.events`, `culture.events`, `booking.events`, `feed.events`, `catalog.events` et `user.events`.

## 3. Contrats existants utiles au mobile

| Parcours | Contrats constatés | État et limite principale |
|---|---|---|
| Pays | `GET /countries`, `/countries/available`, `/{code}/configuration`, `/{code}/features`, `/{code}/languages`, `/{code}/currencies`, `/{code}/timezones` | `EXISTANT`. Les pays viennent bien de `country-config-service` et non d’une liste mobile. |
| Profil / préférences Explorer | `GET/PUT /users/me`, `PATCH /users/me/location`, `/language`, `/discovery-preferences` | `EXISTANT`. Pays, langues de contenu, rayon, contenu africain et devise préférée sont déjà persistables. |
| Post | `POST /posts`, `PUT /posts/{id}`, `POST /posts/{id}/publish`, visibilité, archive, suppression, lectures publiques et personnelles | `EXISTANT`. Un post peut référencer `EVENT`, `PLACE`, `ARTWORK`, `CULTURE_CONTENT`, etc. |
| Story | `POST/GET /stories`, `GET /stories/{id}`, `POST /stories/{id}/view`, `DELETE /stories/{id}` | `PARTIEL`. Pas de visibilité/audience ni de référence vers une sortie. |
| Sortie | `POST /events`, `GET /events/upcoming`, `GET /events?placeId`, `GET /events/me`, détail, statut, inscription, invitations | `PARTIEL`. Pas de diffusion sociale ni de recherche d’événements par contraintes d’aventure. |
| Suggestion de lieu | `POST /place-suggestions`, `GET /place-suggestions/me`; modération admin approve/reject | `PARTIEL`. Pas de médias, de ville structurée ni d’événements/notifications. |
| Explorer | `GET /discovery/search`, `/trending`; `GET /recommendations` | `PARTIEL`. La recommandation ne reçoit ni dates, ni plages horaires, ni budget, ni participants, ni intérêts. |
| Interactions Feed | like, favorite, commentaire, share, summary, commentaires, sauvegardes | `EXISTANT` pour les posts. Aucun feedback durable « intéressé / pas intéressé ». |
| Réseau social | follow, block, followers, following, recherche, suggestions, activité, settings | `EXISTANT`. Aucun mute/masquage discret. |
| Collections | CRUD collections et éléments de collection | `EXISTANT`. Une collection n’est pas un favori. |
| Réservations / tickets | slots, activités, bookings, annulation; hold/order/tickets/QR | `EXISTANT`, mais les réponses de réservation n’ont pas de résumé de lieu/activité affichable. |
| Avis | `/reviews/**`, `/interactions/places/{placeId}/reviews`, agrégat et avis utilisateur | `EXISTANT`; deux surfaces REST à harmoniser à terme. |
| Signalement | `POST /moderation/reports`, `GET /moderation/reports/me` | `EXISTANT` pour les cibles supportées. Ce n’est **pas** `/reports`. |
| Passport / XP | XP, historique, badges, Passport, récompenses, streaks, leaderboard | `EXISTANT`. Les règles XP dépendent des événements effectivement consommés. |
| Contribution culturelle | brouillon, édition, soumission, suivi personnel et lecture publique | `EXISTANT`, sans publication sociale automatique après validation. |

## 4. P0 — diagnostic du Feed

### Chaîne réellement implémentée

```text
POST /api/v1/posts/{id}/publish
  -> content-service / table content_outbox
  -> Kafka topic content.events
  -> FeedEventConsumer
  -> table feed_posts + métriques + cache Feed
  -> GET /api/v1/feed?page&size
```

Un post est présentable dans le Feed seulement avec :

- `status = PUBLISHED` ;
- `visibility = PUBLIC` ;
- `publishedAt` non nul.

Le Feed personnalisé renvoie `FeedPage(userId, page, size, items, generatedAt)`. Une ligne organique contient seulement IDs/auteur brut, caption, médias, hashtags, compteurs et score. Elle ne contient ni `hasNext`, ni total, ni résumé d’auteur, ni état du lecteur (`likedByViewer`, `savedByViewer`, `followingByViewer`).

### Défauts et risques vérifiables

| Priorité | Constat | Effet |
|---|---|---|
| P0 | `PublicFeedController` est injoignable derrière le gateway. | `GET /api/v1/public/feed` retourne 404 via l’entrée publique malgré son contrôleur. |
| P0 | `FeedEventConsumer` consomme **tout** `content.events` et exige `payload.postId`. Les Stories publient sur le même topic avec `storyId`, sans `postId`. | Une Story fait lever une erreur avant le receipt de déduplication ; selon la politique Kafka, elle peut être relue et bloquer/retarder la partition. |
| P0 | Une sortie émet `event.events`, mais `feed-service` n’écoute pas ce topic. | Une sortie ne peut pas devenir une carte Feed automatiquement. |
| P1 | Le cache du Feed contient la page organique avant injection ; un cache hit retourne immédiatement cette page. | L’injection de publicité n’est pas rejouée sur cache hit, contrairement au commentaire du code. |
| P1 | Le DTO ne donne pas l’état du lecteur ni les données d’auteur. | Le mobile doit inventer nom/avatar/état initial ou multiplier les appels. |
| P1 | Le Feed personnel ne renvoie pas `hasNext`. | La pagination ne peut pas être correcte lorsque le nombre d’items varie après injection publicitaire. |

### Correction cible du Feed

1. Ajouter `/api/v1/public/feed/**` au prédicat gateway **si** ce feed est un produit public réel. Sinon supprimer son exposition interne ou la documenter comme non publiée ; ne pas laisser le client croire que la route est disponible.
2. Filtrer explicitement `content.post.*` dans `FeedEventConsumer`, ou séparer les topics post/story. Les événements Story doivent être reçus, ignorés avec receipt, ou traités par un consommateur Story dédié ; ils ne doivent jamais être parsés comme un post.
3. Étendre **la route existante** `GET /api/v1/feed` plutôt que créer une seconde route :

```json
{
  "page": 0,
  "size": 20,
  "hasNext": true,
  "items": [{
    "itemType": "ORGANIC",
    "postId": "uuid",
    "author": {
      "id": "uuid", "displayName": "…", "avatarUrl": "…",
      "verified": false, "followingByViewer": false
    },
    "interactions": {
      "likes": 12, "comments": 2, "shares": 1,
      "likedByViewer": false, "savedByViewer": false
    }
  }]
}
```

4. Tester le parcours avec un vrai Bearer token, un post `PUBLIC/PUBLISHED`, puis observer : `content_outbox.published_at`, consumer group `feed-service`, projection `feed_posts`, cache Redis et la réponse Feed. Un `401` sans token sur `/api/v1/feed` est normal ; il ne démontre pas une absence de route.

## 5. Sorties : événement, Feed, Story, notifications et XP

### Comportement actuel

`POST /api/v1/events` crée une sortie au statut `PENDING`. Un utilisateur standard peut modifier sa sortie mais ne peut pas la passer à `PUBLISHED` : la publication requiert le chemin privilégié de modération. `event-service` écrit des événements `event.created`, `event.updated`, `event.cancelled`, `event.completed` dans `event_outbox`, puis dans `event.events`.

Le payload sortant contient uniquement : `eventId`, `placeId`, `title`, `startAt`, `status`, `countryCode`, `languageCode`.

La recherche du monorepo montre qu’actuellement **seul `graph-service` consomme `event.events`**. Ni Feed, ni notifications utilisateur, ni gamification, ni recommandations ne le consomment.

| Effet produit attendu | État actuel | Écart |
|---|---|---|
| Sortie visible par son détail / lieu | `EXISTANT` après publication | Les vues `upcoming` et `byPlace` ne renvoient que les sorties `PUBLISHED/PUBLIC`. |
| Publication Feed liée à la sortie | `NON RACCORDÉ` | Aucun post `referenceType=EVENT` n’est créé. |
| Story de sortie | `NON RACCORDÉ` | Aucun appel/story event ne lie une Story à `eventId`. |
| Notification d’invitation / inscription / annulation | `NON RACCORDÉ` | `notification-service` ne consomme pas `event.events`. |
| XP événement | `NON RACCORDÉ` | `gamification-service` ne consomme pas `event.events` et son `EventXpPolicy` n’a pas ces règles. |
| Recommandations d’événements | `NON RACCORDÉ` | `recommendation-service` ne consomme pas `event.events`. |

### Contrat recommandé : diffusion sociale atomique

Étendre `POST /api/v1/events` et l’update concernée avec une intention explicite :

```json
"socialDistribution": {
  "publishToFeed": true,
  "publishToStory": true
}
```

La réponse de création peut être acceptée alors que la diffusion est en attente de modération ; elle doit alors donner des états réels, jamais simuler un succès :

```json
{
  "eventId": "uuid",
  "status": "PENDING",
  "distribution": {
    "feed": "PENDING_MODERATION",
    "story": "PENDING_MODERATION"
  }
}
```

Après transition contrôlée vers `PUBLISHED`, un orchestrateur transactionnel/outbox doit :

1. créer un post `PUBLIC`, `referenceType=EVENT`, `referenceId=eventId` et rattacher le cover media si autorisé ;
2. créer une Story seulement si `publishToStory=true` **et** un média publiable existe ; sinon retourner/émettre `SKIPPED_NO_MEDIA` ;
3. publier des événements dédiés et idempotents pour Feed, notification, XP et recommandation ;
4. conserver l’état de distribution et l’erreur éventuelle afin de rejouer sans doublon.

Le mobile ne doit pas effectuer trois écritures indépendantes `event + post + story` : il ne pourrait pas compenser une réussite partielle.

Les groupes, contraintes d’âge, liste d’attente et vente de tickets de groupe n’existent pas dans le modèle d’événement : il y a une capacité unique, des inscriptions et des invitations. Ils restent à concevoir séparément.

## 6. Suggestions de lieu

`POST /api/v1/place-suggestions` crée une suggestion `PENDING`; `GET /api/v1/place-suggestions/me` la rend à son auteur. La modération admin peut approuver (création ou rapprochement d’un lieu canonique) ou rejeter. Les réponses exposent bien statut, lieu canonique, motif et dates.

| Besoin | État | Décision backend attendue |
|---|---|---|
| Pays | `PARTIEL` | `countryCode` est accepté mais optionnel. Si chaque suggestion appartient obligatoirement à un pays, le rendre obligatoire et valider le code avec `country-config-service`. |
| Ville / découpage administratif | `À CRÉER` | Le DTO ne transporte qu’`address` et une chaîne `region`; ajouter identifiants ville/zone ou un contrat d’adresse géocodée cohérent. |
| Photos / vidéos | `À CRÉER` | Ajouter `POST /place-suggestions/{id}/media` et `DELETE /.../media/{mediaId}`, ou un `mediaIds` contrôlé. Vérifier propriétaire, ordre, droits, antivirus/modération et ne pas rendre ces médias publics avant approbation. |
| Notification de décision | `NON RACCORDÉ` | `PlaceSuggestionService` n’écrit pas d’outbox ; aucune notification/XP n’est possible. |
| Feed après approbation | `NON RACCORDÉ` | La création de lieu canonique ne produit pas de post ni d’événement de découverte exploitable par le Feed. |

## 7. Explorer et plannings d’aventure

La recherche `GET /discovery/search` supporte pays, plusieurs pays, zone, ville, type, catégorie, position/rayon, langue, scope et filtres culturels. `GET /recommendations` n’accepte que `lat/lng`, `languageCodes`, `ctx`, `page`, `size`; ses candidates proviennent des topics catalogue, contenu, interactions, Feed et utilisateur. Les événements et les disponibilités/budgets de réservation ne l’alimentent pas.

Le planning mobile observé est donc nécessairement local aujourd’hui : aucun modèle ni contrôleur `Adventure`, `Itinerary` ou `Planning` n’existe dans le backend.

### API à créer — source de vérité des plannings

| Méthode / chemin | Service conseillé | Comportement exigé |
|---|---|---|
| `POST /api/v1/explore/adventure-plans/preview` | nouveau `adventure-planning-service` ou extension explicitement isolée de `recommendation-service` | Génère un aperçu non persistant. Retourne jours inclusifs, propositions réelles et les critères normalisés. |
| `POST /api/v1/explore/adventure-plans` | même service | Persiste le plan du JWT connecté et le détail journalier validé. Retourne `201`, id et plan. |
| `GET /api/v1/explore/adventure-plans?page&size` | même | Liste les plans de l’utilisateur connecté, paginée. |
| `GET /api/v1/explore/adventure-plans/{planId}` | même | Retourne un plan propriétaire avec jours, propositions, choix et états. |
| `DELETE /api/v1/explore/adventure-plans/{planId}` | même | Suppression propriétaire, idempotence définie (204 ou 404 documenté). |
| `POST /api/v1/explore/adventure-plans/{planId}/recommendations/{id}/skip` | même | Mémorise l’exclusion et retourne un remplacement réel ou `replacement: null`. |

Requête minimale :

```json
{
  "countryCode": "CM",
  "startDate": "2026-10-05",
  "endDate": "2026-10-20",
  "startTime": "09:00",
  "endTime": "17:00",
  "partyType": "SOLO",
  "interestCodes": ["culture", "events"],
  "budget": { "tier": "STANDARD", "minimumAmount": 5000, "maximumAmount": 20000, "currencyCode": "XAF" }
}
```

Règles : date début/fin inclusives (même date = un jour), heure de fin après début, `partyType` limité à `SOLO|FAMILY|FRIENDS|COUPLE`; filtrage pays/date/horaires/disponibilité/budget/intérêts avant ranking. En cas d’absence de résultat, répondre `200` avec une raison et des alternatives relâchées explicitement, jamais avec de faux contenus.

## 8. Interactions, favoris, feedback et signalements

### Ce qui peut déjà être câblé sans nouvelle route

- Like : `PUT/DELETE /api/v1/interactions/posts/{postId}/like`.
- Favori de post : `PUT/DELETE /api/v1/interactions/posts/{postId}/favorite`; lecture `GET /api/v1/saves`.
- Commentaires : `POST /api/v1/interactions/posts/{postId}/comments`, lecture et édition/suppression.
- Partage : `POST /api/v1/interactions/posts/{postId}/shares`.
- Blocage : `POST/DELETE /api/v1/users/social/{userId}/block`.
- Signalement : `POST /api/v1/moderation/reports` avec `targetType`, `targetId`, `targetOwnerId`, `reason`, `details`.

La route de signalement existante couvre `POST`, `COMMENT`, `MEDIA`, `MESSAGE`, `USER`, `PARTNER`, `CATALOG_ASSET` et des cibles culturelles. Elle **ne couvre pas** `STORY`, `EVENT`, `PLACE` ou `PLACE_SUGGESTION`. `GenericInteractionEntity.Type.REPORT` est une interaction générique, pas un dossier de modération : ne pas l’utiliser pour remplacer `moderation/reports`.

### API / extensions à prévoir

| Besoin | Contrat recommandé | Règle métier |
|---|---|---|
| Intéressé / pas intéressé | `POST /api/v1/feed/items/{postId}/feedback` | Corps `{"type":"INTERESTED"|"NOT_INTERESTED"}`; idempotent par utilisateur/post/type, audit, invalide le cache du lecteur et alimente le ranking. |
| Masquer un compte sans le bloquer | `POST/DELETE /api/v1/users/social/{userId}/mute` | Masquage personnel durable, distinct d’un blocage. Les Feed/recherche/notifications doivent l’appliquer. |
| Signaler Story / sortie / suggestion | Extension de `TargetType` sur **la route existante** `/moderation/reports` | Ajouter `STORY`, `EVENT`, `PLACE`, `PLACE_SUGGESTION` selon le produit, avec vérification de cible/propriétaire. |
| Favoris multi-types pour Profil | `GET /api/v1/me/favorites?targetType=...` ou lecture agrégée dans `interaction-service` | Ne pas remplacer les collections. Renvoyer type, cible, résumé affichable, date de sauvegarde. |

## 9. Notifications, culture, réservation, Passport

| Domaine | Constat | Écart concret |
|---|---|---|
| Notifications | Le service consomme auth, partner, moderation, messaging, culture, catalog, commerce, interaction et artisan. | Il ne consomme pas `event.events` ni `booking.events` côté notifications utilisateur. De plus sa policy ne mappe ni like/comment/share génériques, ni événements de sortie. |
| Culture | Contribution : draft → submit → modération/publication. `culture.events` est consommé par Feed pour les seuls types `PROVERB`/`RECIPE`, par recommandation et par XP culture. | Aucune création de post Feed liée à l’auteur après validation ; seuls liens de contenu culturel sont projetés. |
| Réservations | Les `BookingView` retournent montant, statut, dates, slot/activité IDs et devise. | Le mobile ne peut pas afficher de façon robuste nom, image, adresse et créneau contextualisés sans jointures/additions au DTO. |
| Billets | Hold, commande, billets, QR et types de tickets existent. | Aucun contrat de groupe lié à la création d’une sortie n’existe. |
| Passport / XP | `EventXpPolicy` récompense posts, interactions, check-ins et booking confirmé/terminé; `CultureEventXpPolicy` gère culture/artisan. | Aucun XP constaté pour création, publication, inscription ou participation à une sortie ; le service ne consomme pas `event.events`. |

## 10. Matrice des événements essentiels

| Producteur | Topic | Consommateurs constatés | Écart important |
|---|---|---|---|
| `content-service` post | `content.events` | Feed, recommandation, gamification | Notifications utilisateur absentes pour post/like/comment selon policy. |
| `content-service` Story | `content.events` | Feed, recommandation, gamification | Feed attend `postId` : défaut P0. Recommandation/XP ignorent fonctionnellement la Story. |
| `interaction-service` | `interaction.events` | Feed, recommandation, gamification, notification | Feed/rec/XP sont câblés pour les interactions post; policy notification ne les transforme pas en notification. |
| `event-service` | `event.events` | `graph-service` uniquement | Feed, Story, notifications, XP et recommandation absents. |
| `place-service` suggestion | aucun événement trouvé | aucun | Aucune notification ni traçabilité inter-service de décision. |
| `booking-service` | `booking.events` | gamification, notification admin | Notification utilisateur booking non observée. |
| `culture-service` | `culture.events` | Feed (partiel), recommandation, gamification, notifications | Diffusion auteur dans Feed non créée. |

## 11. Tests existants et lacunes

Inventaire statique de fichiers `*Test.java` : Feed 2, Content 1, Event 2, Place 3, Recommendation 7, Interaction 9, User 2, Catalog 2, Booking 10, Ticket 9, Notification 10, Culture 7, Gamification 6, Moderation 1, Auth 1, Country-config 6, Discovery 9.

Les tests Feed présents couvrent notamment post replay, interaction replay, lien Culture et Feed public. Ils ne couvrent pas :

- un événement `content.story.*` sur `content.events` ;
- le gateway `/api/v1/public/feed/**` ;
- une publication réelle outbox → Kafka → projection → cache → réponse authentifiée ;
- la réinjection des publicités sur cache hit ;
- une sortie publiée entraînant un post Feed/Story/notification/XP (chaîne absente aujourd’hui).

**Tests non exécutés dans cet audit :** aucun test Maven/Gradle n’a été lancé, conformément au périmètre audit sans modification. Avant toute livraison backend, ajouter les tests ci-dessus puis exécuter au minimum les modules affectés et les tests d’intégration Kafka avec conteneurs/infra autorisés.

## 12. Roadmap recommandée

### P0 — avant les tests produit globaux

1. Corriger le routage public Feed ou retirer l’API publique non publiée.
2. Corriger le consommateur Feed pour les événements Story sur `content.events`.
3. Vérifier de bout en bout une publication publique authentifiée et traiter les outbox/consumer/cache qui échouent.
4. Décider et implémenter l’orchestration de diffusion d’une sortie publiée vers Feed et Story.
5. Créer la persistance des plannings d’aventure, car les interfaces « Enregistrer » / « Gérer mes plannings » ne peuvent pas être une source de vérité locale.

### P1 — cohérence des parcours déjà affichés

1. Étendre le DTO Feed (auteur, états lecteur, `hasNext`) et câbler favoris/partage sur les routes existantes.
2. Ajouter feedback Feed, mute et cibles de signalement manquantes.
3. Ajouter médias, géographie structurée et événements de décision pour suggestions de lieux.
4. Ajouter listeners/policies notification, XP et recommandation pour les événements de sortie décidés par le produit.
5. Enrichir les résumés de sorties, réservations et favoris pour les écrans Profil.

### P2 — consolidation

1. Harmoniser les deux surfaces d’avis et les lectures favoris/collections.
2. Définir groupes, liste d’attente, âge et ticketing de sortie si ces interfaces deviennent actives.
3. Concevoir les audiences Story et les références Story ↔ événement, plutôt que les inférer côté mobile.
4. Remplacer les libellés de carte statiques par des données géographiques administrées lorsque le provider de carte est configuré.

## 13. Risques à ne pas masquer par le frontend

- Ne pas créer de faux posts ou de faux plannings quand l’API est absente.
- Ne pas faire croire qu’une sortie est publiée socialement tant que la modération/distribution n’a pas confirmé les effets.
- Ne pas confondre collection, favori, feedback de ranking, block et mute : ce sont cinq persistances différentes.
- Ne pas créer une nouvelle route `/reports` : câbler/étendre `/api/v1/moderation/reports`.
- Ne pas attribuer l’écran de carte noir à une route backend sans vérifier le provider natif, la clé Maps et les permissions ; les API de recherche/places ne suffisent pas à rendre une carte.
- Les deux fichiers de configuration `content-service.properties` et `country-config-service.properties` indiquent tous deux le port par défaut 8090. En déploiement distribué cela peut être compensé par `SERVER_PORT`; en exécution locale conjointe c’est un risque de collision à documenter avant un test complet.

## 14. Checklist de validation après implémentation

1. Publier un post public avec média puis lire `GET /api/v1/feed` avec un token réel ; vérifier projection, auteur, états lecteur et pagination.
2. Créer/lire/supprimer une Story et vérifier que `feed-service` ne met pas la partition `content.events` en échec.
3. Créer une sortie, la modérer puis vérifier les états Event/Feed/Story et l’absence de duplication lors d’un replay Kafka.
4. Tester invitation, inscription, annulation et la notification/XP seulement pour les règles explicitement implémentées.
5. Soumettre une suggestion avec pays/média, puis approuver/rejeter et vérifier la notification, la visibilité auteur et le lieu canonique.
6. Prévisualiser un plan d’un jour et un plan multi-jours, l’enregistrer, le relire sur une autre session, le supprimer et skipper une proposition.
7. Tester comme deux utilisateurs distincts : like, commentaire, save, feedback, block/mute, report et leurs effets après rafraîchissement.

