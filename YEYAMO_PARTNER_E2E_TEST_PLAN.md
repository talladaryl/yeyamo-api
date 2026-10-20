# Yeyamo Partner — plan E2E reproductible

## Préconditions

- Environnement isolé avec Gateway, Auth, Partner, Place, Event, Content, Feed, Booking, Notification, Message et Kafka.
- PostgreSQL de test et topics dédiés; aucun secret/paiement/compte de production.
- Comptes synthétiques : Explorateur E, partenaires A (`DRAFT`), B (`APPROVED`), C (`SUSPENDED`), Admin autorisé.
- Capturer correlation-id, réponses HTTP, lignes DB et messages Kafka; nettoyer uniquement les données synthétiques par leurs identifiants.

## A — inscription et reprise

1. Inscrire un partenaire; interrompre le réseau après création Auth.
2. Reprendre avec les mêmes identifiants.
3. Vérifier un seul compte Auth et un seul profil Partner.
4. Uploader uniquement les documents exigés, soumettre, puis approuver via Admin.

Attendus : aucun faux succès, aucun doublon, statuts canoniques visibles. Endpoints : Auth register/login, `/partners/me`, documents, submit, Admin review.

## B — sécurité Place

1. B crée un lieu synthétique.
2. A, C et E tentent `PUT /places/{id}`.
3. B modifie le nom, tente ensuite de changer `partnerId` et `status`.
4. Admin autorisé effectue une mutation administrative.

Attendus : B normal = succès; tiers = 404/403 sans fuite; suspendu = refus; champs protégés = 400; admin conforme aux permissions.

## C — dashboard réel

1. Créer deux lieux publiés et deux événements avec B.
2. Ouvrir « Mes établissements » puis « Mes événements ».
3. Paginer, rafraîchir, simuler 503, puis compte vide.

Attendus : données serveur exactes, aucun mock, états erreur/vide explicites. Endpoints : `/places/me`, `/events/organized/me`.

## D — distinction événements

1. B crée un événement; E s’y inscrit.
2. Vérifier `/events/organized/me` avec B et `/events/me` avec E.

Attendu : le premier contient les événements créés, le second les inscriptions; aucune contamination inter-compte.

## E — publication professionnelle (bloquant avant exécution)

À exécuter seulement après implémentation du contexte professionnel : brouillon DRAFT autorisé, publication refusée avant approbation, publication sous marque après approbation, auteur technique conservé, feed/story/search/notification cohérents.

## F — offre

En session backend, ouvrir l’écran Offre. Attendu actuel : action désactivée et message « indisponible », aucune requête réseau, aucun succès. En démo : simulation clairement étiquetée.

## G — Explorateur non-régression

Connexion, profil, post personnel, story personnelle, feed, recherche, événement, inscription, réservation, message et notifications. Attendu : aucune attribution à une marque partenaire et comportements historiques conservés.

## H — preuves et validation

- Conserver logs HTTP/correlation-id, captures UI, assertions DB, offsets/payloads Kafka anonymisés.
- PASS exige résultats réels, absence de doublon et contrôle négatif tiers.
- Un test non exécuté reste `NOT_RUN`; un 200 seul ne prouve pas la distribution Kafka.
