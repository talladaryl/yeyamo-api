# Yeyamo Partner — rapport d’implémentation

Date : 2026-10-19

## 1. État Git initial

- Backend : branche `main`, HEAD `4f7620a`, arbre déjà modifié (Auth, Cuisine/Culture, Media, rapports et sous-module cloud-conf).
- Mobile : branche `daryl`, HEAD `837b43a`, modifications Cuisine/Culture déjà présentes.
- Tous ces changements ont été conservés. Aucun reset, clean, commit, push ou déploiement.

## 2. Anomalies vérifiées

| Anomalie | Preuve | Résultat |
|---|---|---|
| IDOR `PUT /api/v1/places/{id}` | aucune identité transmise au service avant correction | corrigé et testé |
| « Mes établissements » vide en session backend | écran utilisait `isDemo ? fixtures : []` | raccordé à `/places/me` |
| « Mes événements » vide en session backend | écran utilisait `isDemo ? fixtures : []`; `/events/me` signifie inscriptions | endpoint propriétaire distinct ajouté |
| Statistiques inconnues affichées à zéro | `publications: 0`, `establishments: 0` forcés | `null/—` pour inconnu; établissements comptés depuis l’API |
| Offre avec faux succès | `Alert` local sans persistance | publication désactivée hors démo |

## 3. Décisions métier appliquées

- L’identité de l’acteur provient exclusivement de `Authentication.getName()`.
- Une modification de lieu partenaire exige la propriété et le statut `APPROVED`.
- Le propriétaire et le statut d’un lieu ne sont pas modifiables via la route partenaire générale.
- Les événements organisés sont filtrés serveur par `ownerUserId`; aucun identifiant de partenaire client n’est accepté.
- Une donnée statistique absente n’est plus présentée comme égale à zéro.
- Aucun succès de publication d’offre n’est annoncé sans persistance backend.

## 4. Modifications par lot

### Lot 0 — sécurité

- `PlaceController.update` transmet l’acteur serveur et détecte uniquement les rôles Spring Security administratifs.
- `PlaceService.updateAuthorized` applique propriété, approbation et protection des champs sensibles.
- Politique anti-énumération : un acteur tiers reçoit `PLACE_NOT_FOUND`.
- 8 tests unitaires de propriété ajoutés; 13 tests Place ciblés passent.

Limite : l’audit BOLA transversal de tous les microservices n’est pas couvert dynamiquement; le lot reste `PARTIAL` malgré la fermeture du P0 démontré.

### Lot 1 — onboarding

Les routes KYC existantes ont été confirmées (`/partners/me/documents`, `/partners/me/submit`, exigences pays). Aucun changement risqué du parcours Auth/Partner n’a été introduit dans cette passe. Reprise idempotente et guard central mobile restent à finaliser.

### Lot 2 — dashboard

- Établissements réels via le hook existant et `/places/me`, avec chargement, erreur, retry, vide et navigation.
- Événements organisés via `GET /api/v1/events/organized/me?page=&size=` et écran réel.
- Mesures inconnues rendues indisponibles; nombre d’établissements issu de la pagination Place.
- Notifications : l’écran existant utilise déjà les hooks backend en session réelle; non réécrit.
- Réservations et avis restent mockés : aucun raccordement non vérifié n’a été inventé.

### Lot 3 — posts/stories

Non implémenté dans cette passe. Le modèle additif `authorUserId + partnerId + contexte professionnel` et sa résolution publique nécessitent une évolution coordonnée Content/Feed/Search/Notification. Les posts personnels n’ont pas été modifiés.

### Lot 4 — événements/lieux

- Endpoint organisé ajouté et testé.
- Contrôle propriétaire des lieux corrigé.
- Aucun modèle « expérience partenaire » inventé : `BLOCKED_PRODUCT_DECISION`.

### Lots 5 à 8

- Billetterie, messagerie, campagnes et pipelines Kafka existants ont été préservés.
- L’offre sans backend est désormais explicitement indisponible.
- Aucun paiement, message ou contenu public réel n’a été émis.

## 5. Fichiers modifiés par cette passe

Backend :

- `place-service/.../controller/PlaceController.java`
- `place-service/.../service/PlaceService.java`
- `place-service/.../service/PlaceOwnershipSecurityTest.java`
- `event-service/.../controller/EventController.java`
- `event-service/.../repository/EventRepository.java`
- `event-service/.../service/EventService.java`
- `event-service/.../service/OrganizedEventsOwnershipTest.java`

Mobile :

- `src/app/(partner)/offer.tsx`
- `src/app/(partner-dashboard)/establishments.tsx`
- `src/app/(partner-dashboard)/events.tsx`
- `src/features/events/events.api.ts`
- `src/features/events/useEvents.ts`
- `src/features/partner-dashboard/types.ts`
- `src/features/partner-dashboard/usePartnerDashboard.ts`

## 6. Routes adaptées/créées

- Adaptée : `PUT /api/v1/places/{id}` — acteur authentifié obligatoire, contrôle propriétaire/administrateur.
- Créée : `GET /api/v1/events/organized/me` — page d’événements dont `ownerUserId` est l’acteur authentifié.

## 7. DTO et migrations

- Aucun DTO backend historique cassé.
- Aucun changement de schéma ni migration.
- Le type mobile des métriques accepte `null` pour exprimer « indisponible ».

## 8. Tests

- Place : 13/13 PASS (`PlaceOwnershipSecurityTest`, création et listing partenaire).
- Event : 11/11 PASS (`OrganizedEventsOwnershipTest`, inscription et création/lieu).
- Mobile : `npx tsc --noEmit` PASS.
- `git diff --check` PASS sur les deux dépôts.
- Non exécutés : PostgreSQL/Testcontainers, Kafka, E2E multi-service, test iPhone, régression Explorateur complète.

## 9. Compatibilité

- Explorateur : aucune route personnelle Post/Story/Auth modifiée.
- Partenaire : contrats existants préservés; nouvelles données réelles utilisées sans fixtures en session backend.
- Admin : la route administrative existante et son update interne sont conservés.

## 10. Risques résiduels et verdict

- Identité de marque professionnelle non propagée de bout en bout.
- Publication professionnelle avec validation fraîche `APPROVED` non implémentée dans Content/Story.
- Réservations/avis dashboard non raccordés.
- KYC mobile/reprise idempotente non entièrement validés.
- Audit BOLA transversal et E2E distribué incomplets.

Verdict : **NO_GO pour tests publics partenaire**. Les correctifs Place/Event/Dashboard réalisés sont testables isolément, mais ne constituent pas le parcours partenaire complet demandé.

## 11. État Git final

Arbres volontairement non commités. Les modifications préexistantes Auth/Cuisine/Culture/Media sont conservées. Aucun commit, push ou déploiement.
