# YEYAMO V1 — Final Domain Alignment

Audit de clôture du 12 septembre 2026. Les interfaces existantes ont été conservées. Lorsqu'une route ou une preuve métier manquait, l'interface reste accessible, signale explicitement la limite et le manque est documenté ci-dessous ; aucun faux succès ni jeu de données de démonstration n'a été ajouté.

## 1. Exec Summary

Les parcours principaux sont maintenant reliés à des contrats métier réels : suggestion de lieu, création de sortie publique authentifiée, créneaux/réservations d'expériences, statistiques artisan et avis vérifiés pour les lieux, expériences et événements.

Trois points empêchent une certification V1 stricte : liste d'attente de sortie absente, actions commentaires/partage propres à une sortie absentes, et preuve transactionnelle canonique absente pour publier un avis `ARTISAN`. Les interfaces concernées sont préservées et ne prétendent pas fonctionner quand le backend ne le permet pas.

## 2. Git Baseline

- Le dépôt principal, `cloud-conf-yeyamo` et `yeyamo-mobile` étaient déjà des arbres de travail modifiés.
- Aucun `reset`, `clean`, suppression destructive, commit ou push n'a été exécuté.
- Les modifications existantes ont été préservées. Une suppression locale de `create-review/[targetType]/[targetId]` a été détectée pendant la clôture : la route était suivie par Git et a été restaurée avec son enregistrement de navigation. Aucun écran historique n'est supprimé.

## 3. Existing Backend Domain Reuse

- `place-service` reste l'unique propriétaire des lieux canoniques ; les propositions publiques sont modérées avant toute création de lieu.
- `event-service` reste propriétaire des sorties, inscriptions, invitations et de l'éligibilité événementielle interne.
- `booking-service` reste propriétaire des créneaux, paiements, annulations et de l'éligibilité d'une expérience terminée.
- `culture-service` reste le catalogue canonique des expériences ; le partenaire ne fournit ni prix ni devise au moment de créer un créneau.
- `analytics-service` agrège les données culturelles et résout l'artisan à partir de l'identité partenaire authentifiée, jamais depuis un `partnerId` client.
- `interaction-service` centralise les avis, leur modération, leur signalement et leur suppression logique.

## 4. Place Suggestion

**État : GO.**

- `POST /api/v1/place-suggestions` enregistre une proposition authentifiée, avec coordonnées exactes, au lieu de créer directement un lieu public.
- Les doublons sont recherchés contre les lieux canoniques et les suggestions en attente dans un rayon d'environ 100 m.
- `GET /api/v1/place-suggestions/me` expose les propositions de l'utilisateur ; les routes d'administration permettent la liste, l'approbation ou le rejet avec motif.
- L'approbation crée ou réutilise un lieu canonique dans une transaction ; aucun passage ne contourne la modération.
- Les écrans de suggestion gardent la localisation réellement choisie et affichent l'état « en attente de modération ».

## 5. Public User Outing

**État : NO_GO strict, cœur de création fonctionnel.**

- La sortie est créée par un utilisateur authentifié, dont l'identité devient le propriétaire ; le client ne peut pas imposer un propriétaire ni publier directement.
- Le lieu est soit un UUID canonique, soit une localisation libre complète (nom, adresse, latitude, longitude). La localisation libre ne crée pas de lieu public.
- La sortie reste `PENDING` jusqu'à la modération. Visibilité, limite de participants, couverture, commentaires et partage sont persistés comme options.
- Les invitations sont disponibles uniquement au propriétaire ou à un administrateur ; leurs routes sont authentifiées.
- L'interface conserve le commutateur de liste d'attente et indique explicitement « route V1 absente » : aucune table, route ou logique de promotion de liste d'attente n'existe encore.
- Les options commentaires/partage sont stockées, mais aucun contrat de lecture/écriture de commentaire lié à une sortie ni action de partage événementielle n'a été identifié. Elles ne doivent pas être présentées comme des actions V1 terminées.

## 6. Experience Booking

**État : GO.**

- `POST /api/v1/booking-management/experience-slots` crée un créneau `EXPERIENCE` en relisant prix, devise, pays, lieu et contraintes de capacité dans le catalogue canonique.
- L'identité partenaire authentifiée est contrôlée par l'API interne partenaire ; aucune propriété, devise ou tarification n'est acceptée depuis le client mobile.
- L'application affiche les disponibilités et déclenche une réservation réelle avec le flux Mobile Money existant.
- Une annulation d'expérience réglée passe à `REFUND_NOT_REQUESTED` et n'émet ni demande de remboursement automatique ni annulation d'autorisation automatique. Une demande manuelle de support/remboursement reste nécessaire.
- L'éligibilité d'avis d'expérience exige une réservation `COMPLETED` par le même utilisateur.

## 7. Artisan Analytics

**État : GO.**

- `GET /api/v1/analytics/artisans/me?periodDays=7|30|90` est réservé au rôle `PARTNER`.
- L'artisan est résolu avec le jeton du partenaire connecté, puis les statistiques sont agrégées par jour et période 7/30/90.
- Une absence de source est rendue vide/null, jamais remplacée par de fausses valeurs à zéro.
- L'écran partenaire emploie ce contrat réel et ne charge plus de métriques de démonstration.

## 8. Verified Reviews

**État : NO_GO strict ; `PLACE`, `EXPERIENCE` et `EVENT` sont opérationnels.**

- Les avis utilisent `targetType` (`PLACE`, `EXPERIENCE`, `ARTISAN`, `EVENT`), une note 1–5, un commentaire limité à 5 000 caractères, une référence de preuve et une unicité par utilisateur/preuve.
- `PLACE` exige un check-in ; `EXPERIENCE` exige une réservation terminée ; `EVENT` exige une inscription confirmée à une sortie terminée.
- Les listes et agrégats publics ne lisent que les avis actifs. Modification, suppression logique, signalement, masquage/restauration et audit sont disponibles.
- Les écrans lieu, expérience et sortie utilisent ces listes/agrégats et le panneau de publication réel ; ils permettent aussi le signalement et la suppression de son propre avis.
- `ARTISAN` retourne explicitement `REVIEW_ELIGIBILITY_UNAVAILABLE`. L'audit n'a trouvé aucune preuve transactionnelle canonique suffisante (commande/prestation terminée reliée de manière fiable à un artisan) pour autoriser un avis vérifié. Cette publication est donc bloquée, non simulée.

## 9. Flyway Migrations

| Service | Migration | Objet |
| --- | --- | --- |
| place-service | `V7__add_public_place_suggestions.sql` | suggestions, modération et anti-doublon |
| event-service | `V8__add_public_outing_fields.sql` | propriétaire, visibilité, localisation libre, options et invitations |
| booking-service | `V8__add_experience_booking_type.sql` | type `EXPERIENCE` et comportement de remboursement |
| interaction-service | `V6__add_verified_review_targets.sql` | cibles d'avis, preuve, signalement et unicité |

## 10. Gateway/Security

- La configuration Cloud Config de la passerelle route les suggestions et les avis normalisés vers leurs services.
- `GET /api/v1/reviews/**` est public ; écrire, modifier, supprimer ou signaler exige une authentification.
- `POST /api/v1/place-suggestions` et les opérations d'invitation exigent une authentification.
- Le chemin partenaire `/api/v1/analytics/artisans/me` est contrôlé par `ROLE_PARTNER` avant la règle analytics réservée à l'administration.
- Les contrôleurs internes de réservation et d'événement exigent le jeton inter-services ; l'application mobile ne peut pas les appeler directement.

## 11. Backend Tests

| Commande | Résultat |
| --- | --- |
| `mvn -pl place-service test` | PASS — 11 tests |
| `mvn -pl event-service test` | PASS — 11 tests |
| `mvn -pl booking-service "-Dtest=BookingApplicationServiceTest,ExperienceBookingCancellationTest" test` | PASS — 12 tests, dont l'absence de remboursement automatique d'expérience |
| `mvn -pl analytics-service test` | PASS — 48 tests |
| `mvn -pl interaction-service "-Dtest=ReviewServiceTest" test` | PASS — 12 tests et recompilation des 74 sources de production |
| `mvn -pl api-gateway test` | PASS — 14 tests, dont le contrat mobile de sécurité |
| `mvn -pl media-service -am clean test` | PASS — 72 tests (contrôle R2 antérieur conservé) |
| `docker compose -f docker-compose.production.yml config --quiet` | PASS avec des valeurs de contrôle locales pour les variables obligatoires, y compris `yeyamo-public-media` et `yeyamo-private-med` |

La suite complète d'`interaction-service` avait dépassé la fenêtre de contrôle précédente ; le test ciblé et la compilation ont été relancés après les derniers changements. Ce n'est pas une preuve de suite complète postérieure aux derniers changements.

## 12. Mobile Tests

| Commande | Résultat |
| --- | --- |
| `npx tsc --noEmit` | PASS |
| `npx eslint` sur les écrans et le composant modifiés | PASS |
| `npx expo-doctor` | PASS — 21/21 contrôles |
| `npx expo export --platform android` | PASS — bundle Android final de 2 991 modules exporté dans `dist` |

Le lint Expo complet avait dépassé la fenêtre de 120 secondes avant ce contrôle ; le lint ciblé passe. Les erreurs TypeScript rencontrées pendant la clôture (balise JSX manquante, conversion d'identifiant média) ont été corrigées puis le contrôle complet a réussi.

## 13. Matrix

| Domaine | Contrat réel | Mobile | État |
| --- | --- | --- | --- |
| Suggestion de lieu | création, déduplication, modération | parcours de suggestion conservé | GO |
| Sortie publique | création, invitation, modération, lieu libre | création et réglages réels ; liste d'attente signalée absente | NO_GO strict |
| Réservation expérience | créneau canonique, Mobile Money, annulation sans remboursement auto | détail/réservation réels | GO |
| Statistiques artisan | `/artisans/me`, périodes 7/30/90 | tableau réel sans chiffres fictifs | GO |
| Avis vérifiés | place/expérience/événement vérifiés ; artisan explicitement indisponible | lecture, création, signalement, suppression | NO_GO strict |

## 14. Metrics

| Indicateur | Avant clôture | Après clôture |
| --- | ---: | ---: |
| Routes mobiles de production | 166 après suppression locale détectée | 167 après restauration |
| Écrans historiques supprimés | 1 détecté localement | 0 |
| Domaines ciblés certifiés GO | 0/5 | 3/5 |
| Domaines ciblés avec intégration réelle mais limite déclarée | 0 | 2 (sorties, avis) |
| Types de cible d'avis effectivement vérifiables | 0/4 | 3/4 |
| Jeux de données fictifs ajoutés dans les parcours ciblés | 0 | 0 |
| Interfaces avec route V1 absente explicitement signalée | 0 | 1 (liste d'attente) |

## 15. Remaining gaps

1. **Liste d'attente de sortie** — il faut une migration `event_waitlist`, des commandes rejoindre/quitter, une promotion transactionnelle lors d'une libération de place et des routes authentifiées. L'interface doit ensuite appeler ces routes sans changer sa navigation.
2. **Commentaires/partage de sortie** — il faut définir un propriétaire de domaine et des routes événementielles (ou un contrat interaction-social explicitement lié à `EVENT`) pour lister/écrire les commentaires et produire un lien de partage. Les options déjà persistées pourront alors contrôler ces actions.
3. **Avis vérifié d'artisan** — il faut une source de preuve canonique, par exemple une prestation/commande terminée avec `artisanId`, `userId`, statut final et référence immuable, puis un endpoint interne d'éligibilité. Tant qu'elle n'existe pas, refuser l'avis est le comportement correct.
4. **Suite interaction complète post-changement** — relancer `mvn -pl interaction-service test` dans une fenêtre CI sans limite courte et publier le résultat ; le ciblé/compilation est vert mais ne remplace pas ce run complet.

## 16. Final verdict

La fermeture est **partiellement livrée sans suppression de parcours**. Les trois parcours totalement vérifiés sont prêts ; deux domaines restent volontairement en `NO_GO` pour éviter de présenter une liste d'attente, des commentaires/partages événementiels ou un avis artisan comme fonctionnels sans logique serveur et preuve métier correspondantes.

```text
YES — 167 production mobile screens reachable
BACKEND_ROUTE_ALIGNMENT : NO_GO
MOBILE_API_ALIGNMENT : NO_GO
ORPHAN_SCREENS : 0
FINAL VERDICT : NO_GO
```
