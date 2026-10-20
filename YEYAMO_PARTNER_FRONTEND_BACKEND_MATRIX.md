# Yeyamo Partner — matrice frontend/backend

| Action | Écran / hook | API | Contrôleur / service / persistance | Permission/statut | Tests | État |
|---|---|---|---|---|---|---|
| Modifier son lieu | écran de modification existant | `PUT /places/{id}` | `PlaceController` → `PlaceService.updateAuthorized` → `PlaceRepository` | propriétaire + `APPROVED`, ou admin | 8 scénarios propriété | CONNECTED_TESTED |
| Lister ses lieux publiés | establishments / `useMyPartnerPlaces` | `GET /places/me` | Place → projection Partner → JPA paginé | rôle Partner + propriétaire + `APPROVED` | contrat Place + TypeScript | CONNECTED_TESTED |
| Lister ses événements organisés | events / `useMyOrganizedEvents` | `GET /events/organized/me` | EventController → EventService → requête `ownerUserId` paginée | acteur du token | test propriétaire + TypeScript | CONNECTED_TESTED |
| Événements auxquels je participe | détail Event | `GET /events/me` | inscriptions Event | utilisateur authentifié | tests inscription | CONNECTED_TESTED |
| Statistiques partenaire | dashboard / `usePartnerStatistics` | analytics Partner + `/places/me` | Partner analytics + Place | propriétaire | TypeScript | PARTIAL |
| Notifications partenaire | notifications / hooks communs | API notifications existante | Notification service | acteur du token | non rejoué ici | CONNECTED_UNTESTED |
| Créer/publier une offre | offer | aucun contrat confirmé | aucun propriétaire backend | action désactivée hors démo | TypeScript | BLOCKED |
| Réservations reçues | reservations | contrat propriétaire non confirmé | à identifier dans Booking | propriétaire offre | non exécuté | NOT_IMPLEMENTED |
| Avis sur ressources partenaire | reviews | contrat filtré partenaire non confirmé | Interaction/Review | propriétaire ressource | non exécuté | NOT_IMPLEMENTED |
| KYC documents | onboarding | `/partners/me/documents` | PartnerService + stockage document | propriétaire | existant, non rejoué | CONNECTED_UNTESTED |
| Soumettre dossier | onboarding | `/partners/me/submit` | PartnerService + KYC policy | propriétaire, statut compatible | existant, non rejoué | CONNECTED_UNTESTED |
| Publication sous marque | create post/story | évolution absente | Content + Partner + Feed | `APPROVED` requis | absent | NOT_IMPLEMENTED |
| Campagnes | écrans campagne | contrats existants à revalider | Campaign | propriétaire + flags | non exécuté | PARTIAL |
| Messagerie partenaire | chat commun | contrats communs | Message | participants conversation | non exécuté | PARTIAL |
| Expérience partenaire | aucun flow confirmé | aucun contrat confirmé | décision produit requise | indéterminé | absent | BLOCKED |
