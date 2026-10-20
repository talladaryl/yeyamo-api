# Yeyamo — feuille de route de correction Partenaire

Ce document propose les corrections ; aucune n'a été appliquée dans cette passe.

## Lot 0 — sécurité et intégrité

- Anomalies : `PAR-P0-01`, contrôles transversaux de statut, BOLA campagnes/tickets/messages/statistiques.
- Fichiers : `PlaceController`, `PlaceService`, configurations Security et services propriétaires concernés.
- Dépendances : identité JWT, modèle de propriété Partner, projection de statut.
- Correction : passer systématiquement l'acteur, vérifier propriétaire/permission/statut au service, jamais uniquement au contrôleur/mobile.
- Tests : propriétaire/autre utilisateur/admin, DRAFT/APPROVED/SUSPENDED, ID inconnu, concurrence.
- Acceptation : aucune mutation/lecture privée tierce ; réponses sans fuite.
- Risque : régression admin/modération. Ordre : premier, bloquant.

## Lot 1 — inscription et onboarding

- Anomalies : compte Auth orphelin, KYC incomplet, statut non canonique.
- Fichiers : écrans `register-partner*`, hooks partner, documents/submit, navigation.
- Correction : reprise idempotente après création Auth, étapes profil→documents→soumission→attente, statut canonique.
- Tests : nouveau/existant, 400/409/500, reprise réseau, OAuth, rejet/corrections.
- Acceptation : aucun faux succès, reprise sans doublon, redirection déterministe.
- Risque : ne pas casser l'inscription Explorateur.

## Lot 2 — dashboard et profil

- Anomalies : widgets zéro/mock, routes accessibles sans contexte partenaire.
- Fichiers : `partner-dashboard/*`, `usePartnerDashboard`, guard de navigation.
- Correction : guard UX selon statut/permissions ; métriques réelles ou état « indisponible » ; séparation DTO public/privé.
- Tests : chaque statut et petits écrans, cache par compte.
- Acceptation : aucune donnée fictive en session backend.

## Lot 3 — publications et stories

- Anomalies : identité de marque non définie, modération partenaire non homogène, story promotionnelle absente.
- Fichiers : écrans partner publication/story, content/feed contracts.
- Correction : décision produit sur auteur user/partner, contexte additif signé/validé, statuts de distribution visibles.
- Tests : post/story créé, publié, feed/profil, suppression, média tiers refusé.
- Risque : fort sur parcours Explorateur ; réutiliser les primitives, pas dupliquer.

## Lot 4 — événements, lieux, expériences

- Anomalies : dashboard mock, `/events/me` signifie inscrit et non créé, expériences sans gestion partenaire, IDOR lieu.
- Fichiers : partner steps/dashboard, event/place services.
- Correction : endpoint propriétaire paginé pour événements créés, raccorder `/places/me`, politique APPROVED, décider le périmètre expériences.
- Tests : lieu publié requis, timezone, capacité, modération, feed/story, doublons géographiques.
- Acceptation : ressources visibles et gérables uniquement par leur propriétaire.

## Lot 5 — réservations

- Anomalies : écran mock, absence d'inbox de réservations partenaire raccordée.
- Fichiers : dashboard reservations, booking service/API/hooks.
- Correction : liste paginée et transitions autorisées côté propriétaire de l'offre ; prix/devise serveur.
- Tests : double réservation, capacité, annulation, fuseau, paiement/refus, autre partenaire.
- Acceptation : état identique Explorateur/Partenaire/backend.

## Lot 6 — messagerie

- Anomalies : parcours commun non prouvé pour profil business/deep links.
- Fichiers : chat hooks/routes, points d'entrée lieu/event/partner.
- Correction : résoudre destinataire canonique, conserver autorisations de conversation, pagination/reconnexion.
- Tests : deux comptes, tiers interdit, hors ligne, pièce jointe, accusé lecture.

## Lot 7 — campagnes, publicité et offres

- Anomalies : offre locale factice, métriques liste forcées à zéro, feature flag.
- Fichiers : `(partner)/offer`, campagnes/promotions, campaign/commerce/analytics.
- Correction : distinguer offre/promotion/campagne ; supprimer tout faux succès ; persistance uniquement après contrat autorisé.
- Tests : budget positif/devise/statuts/propriété ; aucun paiement réel en test.
- Acceptation : état serveur confirmé avant message de succès.

## Lot 8 — feed, Kafka et notifications

- Anomalies : distribution non prouvée, projections retardées, écran notifications mock.
- Fichiers : outbox/consumers content-event-place-partner-feed-notification ; mobile notifications.
- Correction : schémas versionnés, clés idempotentes, DLQ/observabilité, statuts de projection, réutiliser l'API notifications réelle.
- Tests : duplicate/retry/out-of-order, post/event/place approuvé, deep links valides.
- Acceptation : une action produit au plus une projection visible et une notification pertinente.

## Lot 9 — E2E et non-régression

- Matrice : non connecté, Explorateur, partenaire DRAFT/SUBMITTED/APPROVED/REJECTED/SUSPENDED, admin.
- Environnement : PostgreSQL/Kafka/R2 isolés, comptes synthétiques, aucun paiement ou contenu public réel.
- Suites : contrats frontend/backend, Testcontainers, parcours Detox/Maestro selon outillage présent, sécurité BOLA, cache multi-compte.
- Gate : zéro P0/P1 ouvert, données mock interdites en session backend, événements/projections observables, rollback documenté.

## Ordre recommandé

`Lot 0 → Lot 1 → Lot 2 → Lots 3/4 → Lots 5/6 → Lot 7 → Lot 8 → Lot 9`.

Ne pas démarrer les refontes UI avant la fermeture du Lot 0 : elles pourraient masquer des autorisations backend incorrectes.
