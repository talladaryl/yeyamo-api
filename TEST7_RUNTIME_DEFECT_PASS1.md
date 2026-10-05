# YEYAMO — Test 7 — Runtime defect pass 1

Date d'audit : 2026-10-05

Ce rapport distingue strictement la correction du code, les tests statiques et la validation sur iPhone réel. Un résultat HTTP 2xx ou une compilation ne constitue pas une preuve runtime complète.

## Synthèse

| Défaut | Sévérité | Cause trouvée | Code corrigé | Tests statiques | Retest runtime |
|---|---:|---|---|---|---|
| Navigation Feed → profil | P0 | Oui | Oui | PASS | Requis |
| Clavier commentaires iOS | P0 | Oui | Oui | PASS | Requis |
| Clavier avis iOS | P0 | Oui | Oui | PASS | Requis |
| Éligibilité avis | P1 | Oui | Non : règle correcte, exposition UI à compléter | Audit | Requis |
| Stabilité Like | P0 | Oui, concurrence client possible | Oui | PASS | Requis |
| Stories inter-comptes | P0 | Contrat actuel audité, première frontière runtime non reproduite | Non | Tests existants seulement | Requis |
| Miniatures profil | P1 | Correctif déjà présent dans la branche | Préservé | PASS TypeScript | Requis |
| Créateur membre du groupe | P0 | Invariant déjà présent dans `messaging-service` | Préservé | Audit | Requis |
| Compteur membres | P0 | Source canonique = membres actifs, pas de `+1` client | Préservé | Audit | Requis |
| Upload image téléphone | P0 | Instrumentation existante, échec runtime non reproduit | Non | Audit | Requis |
| Upload vidéo téléphone | P0 | Instrumentation existante, étape défaillante inconnue sans trace téléphone | Non | Audit | Requis |
| Traitement vidéo | P0 | FFmpeg production non prouvé | Non | Non applicable | Requis |
| UTF-8 Explorer/groupe | P1 | Littéraux source mojibake identifiés | Partiel/non terminé | Recherche statique | Requis |
| Suggestions à suivre | P1 | Algorithme limité au second degré | Oui | PASS backend | Requis |
| Configuration pays | P0 | Correctif précédent présent dans le worktree mobile | Préservé | PASS TypeScript | Requis |
| Écran blanc démarrage | P0 | Correctif précédent présent dans le worktree mobile | Préservé | PASS TypeScript | Requis |

## 1. Navigation Feed vers profil public

- Comportement observé : le tap sur l'avatar ouvre une route puis affiche « Profil introuvable ».
- Comportement attendu : ouvrir le profil public de l'auteur réel.
- Cause racine : `feed.api.ts` résout correctement `authorId` (AUTH_ID) en `profileId` (PROFILE_UUID), puis `VerticalFeedList` transmet ce PROFILE_UUID. L'écran `src/app/(profile)/[username].tsx` réutilisait cependant `useUserSearch()` et traitait le UUID comme une requête textuelle.
- Première frontière défaillante : paramètres de route PROFILE_UUID → hook de recherche textuelle.
- IDs : `BackendFeedItem.authorId=AUTH_ID`; `ContentAuthorIdentity.profileId=PROFILE_UUID`; route publique=PROFILE_UUID; publications publiques=AUTH_ID.
- API : ajout additif `GET /api/v1/users/social/profiles/{profileId}`.
- Persistance : lecture `user_profiles` par UUID, aucune migration.
- Cache : clé isolée par compte et PROFILE_UUID : `['social', mode, 'profile', viewer, profileId]`.
- Correctif : résolution directe par UUID, sans conversion heuristique. La recherche textuelle reste disponible pour les anciennes routes non UUID.
- Risque : nécessite le redéploiement de `user-service` avant le mobile corrigé.
- Tests : TypeScript PASS; test backend de résolution directe PASS.
- Retest : compte B ouvre depuis le Feed un post de A, vérifie identité, posts, suivi et retour; répéter depuis stories, followers, notifications.

## 2. Saisie des commentaires et clavier iOS

- Cause racine : le `KeyboardAvoidingView` était enfant d'un conteneur absolument positionné et déjà ancré sous la zone du clavier. Il ne pouvait pas déplacer la feuille elle-même.
- Première frontière défaillante : modal/overlay → layout clavier, avant le composant `CommentInput`.
- Correctif : la feuille est désormais rendue dans un `Modal`; un `KeyboardAvoidingView` plein écran contient et ancre la feuille au-dessus du clavier. La liste conserve `keyboardDismissMode="interactive"` et le composer reste dans le flux.
- API/persistance/cache : inchangés.
- Risque : hauteur de feuille à revalider sur petits écrans et clavier tiers.
- Tests : TypeScript et lint ciblé PASS.
- Retest : iPhone, ouvrir commentaires depuis Feed, taper plusieurs lignes, faire défiler, masquer/réouvrir le clavier, envoyer.

## 3. Saisie d'avis et clavier iOS

- Cause racine : `Modal` alignée en bas sans conteneur d'évitement clavier.
- Correctif : même structure clavier sûre (`KeyboardAvoidingView` plein écran, feuille en bas, ScrollView interactive), sans padding arbitraire.
- API/persistance/cache : inchangés.
- Tests : TypeScript et lint ciblé PASS.
- Retest : ouvrir « Laisser un avis », saisir un commentaire long, vérifier étoiles et bouton au-dessus du clavier.

## 4. Éligibilité des avis

- Règle canonique trouvée : pour `EVENT`, l'événement doit être `COMPLETED` et l'utilisateur doit avoir une inscription `CONFIRMED`. Pour `EXPERIENCE`, la preuve vient de booking-service; pour `PLACE`, un check-in canonique est requis; `ARTISAN` n'a pas encore de preuve exposée.
- Première frontière potentielle : l'UI affiche actuellement « Laisser un avis » sans préflight d'éligibilité. Le POST vérifie correctement côté `interaction-service` et ne doit pas être assoupli.
- API : `POST /api/v1/reviews`; appel interne event-service `/internal/review-eligibility/events/{eventId}/users/{authId}`.
- IDs : cible=EVENT_ID; acteur=AUTH_ID; preuve=EVENT_ID pour un événement terminé/confirmé.
- Correctif non appliqué dans cette passe : aucun endpoint public de préflight n'existe. Masquer le bouton sans contrat fiable créerait une autre règle côté client.
- Diagnostics nécessaires au retest : statut/code backend, type/id cible et motif; jamais de jeton interne.
- Risque : l'utilisateur continue de découvrir l'inéligibilité au submit.
- Retest : événement futur, terminé sans inscription, terminé avec inscription confirmée.

## 5. Stabilité Like

- Cause trouvée : le bouton autorisait plusieurs taps pendant une mutation; deux commandes opposées pouvaient partir avec le même état rendu avant réconciliation.
- Première frontière défaillante : interaction UI → mutation optimistic.
- Correctif : verrou par POST_ID jusqu'à `onSettled`; les autres posts restent interactifs. Le rollback existant reste unique et le refetch autoritatif est conservé.
- IDs : POST_ID et viewer AUTH_ID.
- Cache : toutes les variantes `['feed', viewer, audience]` sont mises à jour; isolation par viewer déjà présente.
- Risque : un service/projection distant réellement en retard reste à vérifier par les traces `LIKE_REFETCH_RESULT`.
- Tests : TypeScript/lint PASS.
- Retest : tap rapide répété, changement d'onglet Feed, refresh, changement A→B→A.

## 6. Visibilité Story inter-comptes

- Contrat audité : story persistée avec `authorId=AUTH_ID`; content-service demande à user-service les AUTH_ID suivis via `/users/social/following/content-author-ids`; la requête active inclut viewer + suivis; cache mobile inclut l'AUTH_ID du viewer.
- Première frontière runtime non prouvée : réponse de user-service à content-service ou configuration inter-service/bearer en environnement déployé.
- Persistance : `stories.author_id=AUTH_ID`; follow graph stocke des PROFILE_UUID mais la sortie interne doit convertir vers AUTH_ID.
- Aucun élargissement public de l'audience n'a été fait.
- Retest : capturer `STORY_ACTIVE_RESOLVED authorCount/storyCount` pour B après follow de A et la réponse du endpoint content-author-ids.

## 7. Miniatures de publications profil

- État actuel préservé : `profile.api.ts` résout les médias en batch, choisit `thumbnail_url`, puis `url`, sans N+1. `PublicationGrid` utilise une image `cover`, un indicateur vidéo/carrousel et une carte texte seulement quand aucun aperçu n'existe ou que le média échoue.
- IDs : POST_ID → MEDIA_ID.
- Risque : une vidéo sans thumbnail READY utilisera son URL média; le runtime doit confirmer que le backend fournit bien une miniature et que le client ne télécharge pas le fichier vidéo.
- Retest : image, vidéo avec thumbnail, vidéo en échec thumbnail, texte, carrousel.

## 8. Créateur et compteur du groupe de sortie

- État backend constaté : `OutingGroupConsumer` crée le groupe avec `ownerId`; `MessagingApplicationService.create()` persiste ce propriétaire avec le rôle `OWNER`. Les participants sont ajoutés de façon idempotente et le créateur est explicitement ignoré lors de la synchronisation pour éviter un doublon.
- Première frontière à vérifier pour l'anomalie runtime : données historiques créées avant cette logique ou projection/UI qui compte seulement les inscriptions événement au lieu des membres actifs de conversation.
- Aucun `+1` frontend n'a été ajouté.
- Stratégie historique recommandée : reconciliation idempotente `outing_group_links.owner_user_id` → membre actif OWNER si absent, avec contrainte unique conversation/user.
- Retest : nouveau groupe count=1, participant count=2, annulation count=1, replay Kafka inchangé.

## 9. Upload image/vidéo téléphone

- Pipeline audité : picker → normalisation métadonnées/MIME → FormData → gateway → media-service → R2 → statut de traitement → association contenu.
- Les traces structurées existantes dans `media.runtime-trace.ts` sont conservées. Aucun octet, credential ou URL signée ne doit être journalisé.
- Première frontière réelle inconnue sans les traces du prochain essai téléphone.
- Il faut distinguer `UPLOAD_FAILURE`, `PROCESSING_FAILED` et `THUMBNAIL_FAILED`.
- Aucun contournement Expo Go et aucune dépendance native nouvelle n'ont été ajoutés.
- Retest : une image HEIC/JPEG et une vidéo MOV/MP4; relever type, extension, MIME déclaré/normalisé, taille, HTTP, mediaId et processingStatus.

## 10. UTF-8

- Cause : plusieurs littéraux TS/TSX et certains commentaires Java contiennent déjà du mojibake dans les sources; ce n'est pas une réponse API unique.
- Source exacte visible pour le défaut signalé : `src/app/(events)/[id].tsx`, libellé « Accéder au groupe » corrompu.
- Des motifs voisins subsistent (`Ãƒ`, `Ã¢â‚¬`, etc.). Une conversion globale aveugle risquerait de modifier des données ou fichiers déjà corrects; elle n'a pas été exécutée.
- Statut : FAIL tant que les littéraux utilisateur affectés ne sont pas corrigés et vérifiés fichier par fichier.

## 11. Suggestions à suivre

- Cause racine : le backend ne proposait que des relations de second degré. Un graphe neuf ou peu connecté retourne légitimement zéro candidat malgré plusieurs comptes éligibles.
- Correctif : conserver les candidats de second degré en priorité puis compléter avec les profils publics actifs, en excluant le viewer, les comptes déjà suivis, bloqués, bloqueurs et doublons.
- IDs : toutes les comparaisons sociales utilisent PROFILE_UUID.
- Cache mobile : clé suggestions isolée par AUTH_ID du viewer.
- Tests : test backend candidat/exclusions de base PASS dans `SocialGraphAlignmentTest`.
- Retest : A reçoit B/C, jamais A, jamais un déjà suivi ou bloqué.

## 12. Configuration pays et écran blanc

- Les modifications antérieures non commitées de `src/app/_layout.tsx`, `src/services/api/errors.ts` et `src/types/api.types.ts` ont été préservées.
- Le bootstrap ne doit jamais supprimer une session valide sur erreur réseau/5xx de configuration pays; l'état doit devenir explicite/réessayable ou non bloquant selon la dépendance réelle.
- Tests de cette passe : compilation TypeScript PASS. Le comportement iPhone reste à retester avec `/users/me=200` et configuration pays=503.

## Audit ciblé des identifiants

| Flux | Identifiant entrant | Identifiant canonique suivant |
|---|---|---|
| Feed ownership/content | AUTH_ID | résolution batch vers PROFILE_UUID |
| Route profil public | PROFILE_UUID | lecture directe social profile |
| Posts d'un profil | AUTH_ID | `/posts/authors/{authId}` |
| Follow/block/mute | PROFILE_UUID | table sociale |
| Story auteur/persistance | AUTH_ID | suivi converti PROFILE_UUID → AUTH_ID côté user-service |
| Like/comment/review acteur | AUTH_ID | issu du JWT |
| Like | POST_ID | interaction-service |
| Story | STORY_ID | content-service |
| Groupe | GROUP_ID/conversation UUID | messaging-service |
| Sortie | OUTING_ID/EVENT_ID | event-service et durable outing-group link |

Aucune conversion basée sur la forme, la longueur ou un cast entre AUTH_ID et PROFILE_UUID n'est utilisée comme pont métier. La détection syntaxique UUID dans la route sert seulement à choisir le contrat de lecture direct ou l'ancien contrat username; elle ne convertit pas l'identité.

## Audit ciblé des caches

- Feed : clé par viewer et audience; les Likes mettent à jour toutes les variantes du viewer.
- Profil public : nouvelle clé par viewer + PROFILE_UUID.
- Stories : clé par viewer AUTH_ID; changement de compte ne réutilise pas la liste A.
- Suggestions : clé par viewer AUTH_ID.
- Avis : clés par type + target ID; invalidation ciblée après création.
- Groupes : pas de cache global partagé identifié dans la frontière modifiée.
- Aucun purge globale après chaque mutation n'a été introduite.

## Fichiers modifiés dans cette passe

### Mobile

- `src/app/(profile)/[username].tsx`
- `src/components/feed/VerticalFeedList.tsx`
- `src/components/reviews/CreateVerifiedReviewSheet.tsx`
- `src/features/social/social.api.ts`
- `src/features/social/useSocial.ts`

Les quatre modifications mobiles antérieures (`_layout.tsx`, `VerticalFeedItem.tsx`, `errors.ts`, `api.types.ts`) et `TEST7_RUNTIME_COUNTRY_WHITE_SCREEN.md` ont été conservées, pas réécrites.

### Backend

- `user-service/.../application/SocialGraphService.java`
- `user-service/.../interfaces/rest/SocialGraphController.java`
- `user-service/.../application/SocialGraphAlignmentTest.java`

## Tests exécutés

- `npx tsc --noEmit` : PASS.
- ESLint ciblé sur les cinq fichiers mobile modifiés : PASS.
- `mvn -pl user-service -am -Dtest=SocialGraphAlignmentTest ... test` : PASS, 7 tests.
- Validation physique iPhone : non exécutable dans cette session; requise.

## Risques et ordre de retest

1. Redéployer `user-service`, puis tester Feed → profil et suggestions.
2. Retester commentaires et avis sur iPhone avec clavier natif et clavier tiers.
3. Tester Like avec taps rapides et refresh.
4. Capturer les frontières Stories A/B et upload média sans secrets.
5. Traiter séparément les littéraux UTF-8 restants et le préflight d'éligibilité des avis.

FEED_PROFILE_NAVIGATION = CODE_FIXED_RUNTIME_RETEST_REQUIRED

COMMENT_KEYBOARD = CODE_FIXED_RUNTIME_RETEST_REQUIRED

REVIEW_KEYBOARD = CODE_FIXED_RUNTIME_RETEST_REQUIRED

REVIEW_ELIGIBILITY = FAIL

LIKE_STABILITY = CODE_FIXED_RUNTIME_RETEST_REQUIRED

CROSS_ACCOUNT_STORY_VISIBILITY = FAIL

PROFILE_MEDIA_THUMBNAILS = CODE_FIXED_RUNTIME_RETEST_REQUIRED

GROUP_CREATOR_MEMBERSHIP = CODE_FIXED_RUNTIME_RETEST_REQUIRED

GROUP_MEMBER_COUNT = CODE_FIXED_RUNTIME_RETEST_REQUIRED

PHONE_IMAGE_UPLOAD = FAIL

PHONE_VIDEO_UPLOAD = FAIL

VIDEO_PROCESSING = BLOCKED_FFMPEG

EXPLORER_UTF8 = FAIL

FOLLOW_SUGGESTIONS = CODE_FIXED_RUNTIME_RETEST_REQUIRED

COUNTRY_CONFIGURATION = CODE_FIXED_RUNTIME_RETEST_REQUIRED

STARTUP_WHITE_SCREEN = CODE_FIXED_RUNTIME_RETEST_REQUIRED

AUTH_ID_PROFILE_UUID_AFFECTED_FLOW_AUDIT = PASS

ACCOUNT_SWITCH_CACHE_AUDIT = PASS

MOBILE_TYPESCRIPT = PASS

MOBILE_LINT = PASS

BACKEND_TARGETED_TESTS = PASS

DATABASE_MIGRATION_REQUIRED = NO

CONFIG_CHANGE_REQUIRED = NO

NEW_NATIVE_DEPENDENCY = NO

MOBILE_NATIVE_REBUILD_REQUIRED = NO

BACKEND_SERVICES_TO_REDEPLOY = user-service

MOBILE_FILES_MODIFIED = src/app/(profile)/[username].tsx, src/components/feed/VerticalFeedList.tsx, src/components/reviews/CreateVerifiedReviewSheet.tsx, src/features/social/social.api.ts, src/features/social/useSocial.ts

P0_BLOCKERS = Cross-account Story visibility, phone image/video upload, production video processing/FFmpeg

P1_BLOCKERS = Review eligibility preflight, remaining UTF-8 mojibake

STATIC_FIX_READY = NO

RUNTIME_RETEST_REQUIRED = YES

READY_FOR_TEST7_RUNTIME_RETEST = NO
