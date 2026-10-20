# Yeyamo Partner — sécurité et non-régression

## Correctifs confirmés

- IDOR Place fermé : l’identifiant utilisateur n’est jamais lu du corps ou des paramètres.
- BOLA Place : partenaire B ne peut pas modifier le lieu de A; réponse 404 anti-énumération.
- Explorateur sans profil partenaire : refusé.
- Partenaire suspendu/non approuvé : refusé pour mutation du lieu.
- Champs protégés : transfert `partnerId` et changement de statut refusés.
- Admin : autorisé uniquement via autorités établies dans le contexte Spring Security.
- Listing Event propriétaire : filtre JPA construit depuis `Authentication.getName()`.

## Couverture exécutée

| Cas | Résultat |
|---|---|
| propriétaire approuvé | PASS |
| autre partenaire | PASS |
| Explorateur | PASS |
| suspendu | PASS |
| transfert de propriété | PASS |
| publication via update général | PASS |
| admin | PASS |
| ressource absente sans fuite | PASS |
| liste événements de l’acteur | PASS |

L’accès non authentifié est rejeté par Spring Security sur les mutations `/api/v1/**`; il n’a pas fait l’objet d’un test MVC dédié dans cette passe. La concurrence Place n’est pas couverte par `@Version` sur cette entité et reste un risque documenté.

## Identité professionnelle

- Auteur technique : préservé dans les modèles existants.
- Propriétaire métier : préservé pour Place; Event reste actuellement propriétaire par utilisateur.
- Marque publique sur Posts/Stories : non implémentée.
- Validation fraîche du statut avant publication professionnelle : non implémentée dans Content/Story.
- Aucun contenu personnel Explorateur n’a été migré ou réattribué.

## Vulnérabilités/limites restantes

- Audit dynamique BOLA Posts, Stories, Booking, Tickets, Messages, Campaign, KYC, Analytics, Artisan et Media non exhaustif.
- Projection Partner Place peut devenir obsolète; les actions sensibles de publication doivent à terme effectuer une revalidation synchrone fail-closed.
- Absence de test token expiré et de matrice MVC complète.
- Absence de test de concurrence sur Place.

Conclusion sécurité : **PARTIAL**. Le P0 Place démontré est corrigé, mais l’affirmation « aucune BOLA connue sur tout le périmètre » n’est pas démontrable sans la campagne restante.
