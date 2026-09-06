# Lot 4 — Actions utilisateur Explorer

Date : 2026-09-04. Périmètre : écrans Explorer et leurs détails. Docker n’a pas été démarré.

## Actions raccordées

| Action visible | État avant | Implémentation | Route |
|---|---|---|---|
| Favori lieu | `useState` local | lecture + mutation React Query | `GET/POST/DELETE /api/v1/interactions/PLACE/{id}/FAVORITE` |
| Favori événement | `useState` local | lecture + mutation React Query | `GET/POST/DELETE /api/v1/interactions/EVENT/{id}/FAVORITE` |
| Favori œuvre | bouton inactif | lecture + mutation React Query | `GET/POST/DELETE /api/v1/interactions/ARTWORK/{id}/FAVORITE` |
| Commentaires œuvre (réel) | deux commentaires codés en dur + ajout local | liste et création serveur | `GET /api/v1/interactions/ARTWORK/{id}/comments`, `POST /api/v1/interactions/ARTWORK/{id}/COMMENT` |
| Partage événement | deux boutons sans `onPress` | `Share.share` avec titre et date API | action native, sans donnée fictive |
| Calendrier événement | bouton sans `onPress` | ouverture d’un modèle calendrier fondé sur `start_date`, `end_date`, titre et description réels | URL calendrier, sans date fabriquée |

Les états sont isolés par cible et identifiant dans React Query. Une mutation invalide la clé concernée ; aucun favori réel n’est conservé seulement dans un composant.

## Backend ajouté

`interaction-service` accepte désormais les cibles `PLACE`, `EVENT` et `EXPERIENCE` (en plus des cibles existantes) et expose :

- `GET /api/v1/interactions/{targetType}/{targetId}/{type}/status` ;
- `GET /api/v1/interactions/{targetType}/{type}/me?limit=50`.

Les routes sont déjà couvertes par le prédicat Gateway `/api/v1/interactions/**`. Les favoris et commentaires émettent toujours l’événement outbox `interaction.events`.

Les avis Place possédaient déjà le CRUD backend complet : création, lecture par lieu/utilisateur, modification et suppression dans `InteractionController`. Aucun contrat d’avis n’a été inventé.

## Fichiers modifiés

- `interaction-service/.../GenericInteractionEntity.java`
- `interaction-service/.../GenericInteractionRepository.java`
- `interaction-service/.../GenericInteractionService.java`
- `interaction-service/.../GenericInteractionController.java`
- `yeyamo-mobile/src/features/interactions/generic-interactions.api.ts`
- `yeyamo-mobile/src/features/interactions/generic-interactions.hooks.ts`
- `yeyamo-mobile/src/app/(places)/[id].tsx`
- `yeyamo-mobile/src/app/(events)/[id].tsx`
- `yeyamo-mobile/src/app/(explore)/artworks/[id].tsx`

## CTA légitimes restant sans comportement backend complet

| CTA | Écart démontré | Action requise |
|---|---|---|
| Réserver un lieu | le service Booking ne publie que des créneaux par `activityId`; aucune relation Place → activité/créneau n’existe | définir puis exposer ce lien dans Booking/Place avant de créer le formulaire de réservation |
| Avis récents d’un lieu | le CRUD existe mais le détail Place ne compose pas encore `GET /interactions/places/{id}/reviews` | raccorder la liste et le formulaire d’avis, puis éventuellement un résumé agrégé |
| Événements liés au lieu | endpoint backend existant `GET /places/{placeId}/events`, mais non composé dans `placesApi.getPlace` | ajouter la composition côté client sans N+1 (un appel sur le détail) |
| Participants « Voir tout » | le détail Event ne publie pas une liste paginée de participants | ajouter le contrat Event avant d’ouvrir un écran de liste |
| Contact artisan | le détail Artwork affiche une alerte de localisation, sans canal de contact confirmé | exposer un contact public/une conversation autorisée par Partner/Messaging, puis remplacer l’alerte |

Nombre de CTA Explorer visibles avec action légitime encore incomplète : **5**. Ils sont conservés afin de ne pas supprimer une fonctionnalité produit ; le rapport ne les présente pas comme consommés.

## Validation exécutée

- `mvn -q -DskipTests package` dans `interaction-service` : succès ; JAR reconstruit.
- `npm exec tsc -- --noEmit` dans `yeyamo-mobile` : succès.
- `npm run lint` : succès, 0 erreur et 62 avertissements existants (dont deux imports inutilisés corrigés dans les écrans touchés).

## Dette restante

La réservation Place doit être modélisée, pas déduite d’un identifiant. Les avis et événements associés disposent déjà d’une route lisible, mais leur composition dans le détail Place reste à faire. Aucun test de réseau n’a été exécuté, Docker étant volontairement arrêté pour ce lot.
