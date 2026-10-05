# YEYAMO — Test 7 — Runtime defect pass 2

Date : 2026-10-05

## 1. Executive summary

Les bloqueurs statiques restants de Pass 1 ont été fermés sans déploiement : mojibake mobile supprimé, préflight d’avis adossé à la règle POST, configuration inter-service des stories réparée, lecture des membres actifs corrigée, propriétaire historique réconciliable, traces média corrélées et FFmpeg installé dans l’image déployable. Un retest iPhone et un redéploiement contrôlé restent requis.

## 2. Pass 1 fixes preserved

Les diffs Feed → profil UUID, clavier commentaires, clavier avis, verrou Like, suggestions de suivi et bootstrap pays/session sont toujours présents. Aucun de ces fichiers n’a été rétabli à une version antérieure.

## 3. UTF-8 findings and fixes

Les chaînes utilisateur corrompues ont été corrigées dans l’écran événement, le Feed, les commentaires, l’onglet Feed et l’erreur HEIC. Le libellé attendu est désormais `Accéder au groupe`. Un second `rg` sur les motifs mojibake dans `src/**/*.ts(x)` retourne zéro occurrence. Les commentaires Java et fixtures historiques n’ont pas subi de conversion globale.

## 4. Review eligibility architecture

`VerifiedReviewService.eligibility()` est l’évaluateur canonique partagé. Il couvre EVENT, EXPERIENCE, PLACE et ARTISAN. `create()` rappelle ce même évaluateur et refuse toute décision non éligible : le préflight n’est donc jamais une autorisation.

## 5. Review preflight API

Ajout authentifié : `GET /api/v1/reviews/eligibility?targetType=...&targetId=...`. Réponse publique : `eligible`, `reasonCode`, `targetType`, `targetId`. La référence interne de booking/check-in n’est pas exposée. Les codes sont `ELIGIBLE`, `EVENT_NOT_COMPLETED`, `REGISTRATION_NOT_CONFIRMED`, `BOOKING_NOT_ELIGIBLE`, `CHECKIN_REQUIRED`, `VERIFICATION_NOT_SUPPORTED`, `TARGET_NOT_FOUND`. Le mobile charge cette décision avant d’afficher le formulaire et traduit localement le code stable en texte français.

## 6. Story cross-account root cause

Première rupture prouvée : `UserServiceClient` attendait `yeyamo.user-service.base-url`, absent de `content-service.properties`, puis retombait sur `localhost`. Dans le conteneur content-service, ce localhost ne désigne pas user-service. La configuration fournit maintenant `${USER_SERVICE_BASE_URL:http://user-service:8086}`.

## 7. Story integration tests

Le test user-service vérifie B → profil B → relation vers PROFILE_UUID A → profil A → `auth-a`, sans retourner `auth-b`. `StoryServiceTest` vérifie ensuite que la story active de A est lue pour B et que le bearer est propagé.

## 8. Story inter-service configuration audit

URL : `http://user-service:8086` par défaut conteneur, surchargeable par `USER_SERVICE_BASE_URL`. Endpoint : `/api/v1/users/social/following/content-author-ids`. Le bearer du viewer est propagé. Les logs `STORY_FOLLOWING_RESOLUTION` et `STORY_ACTIVE_RESOLVED` exposent seulement les comptes, statuts et volumes, jamais le token ni le graphe.

## 9. Group membership read path

Mobile détail groupe → `BackendConversationPayload.members/memberIds` → participants → `participants.length`. Le DTO détaillé backend incluait auparavant les lignes LEFT/REMOVED. `ConversationView.from` ne retourne maintenant que `MemberStatus.ACTIVE`, comme le read model inbox.

## 10. Group creator invariant

La création persiste toujours l’auteur une seule fois avec `OWNER/ACTIVE`. Tests : création, ajout, retrait et départ vérifient désormais 2 → 3 → 2 → 1 membres actifs. Le consumer Kafka conserve son idempotence.

## 11. Historical group reconciliation decision

Migration Flyway `V3__reconcile_outing_group_owners.sql` ajoutée. Elle insère le propriétaire manquant et réactive/corrige son unique ligne via la clé `(conversation_id,user_id)`. Elle est idempotente et ne duplique aucun propriétaire.

## 12. Image upload audit

JPEG/PNG restent inchangés. HEIC/HEIF passe réellement par `expo-image-manipulator` avec `SaveFormat.JPEG` avant que MIME et extension deviennent JPEG. La validation de taille est 10 MiB image et 100 MiB vidéo.

## 13. iOS URI audit

Le picker versionné Expo retourne une URI locale utilisable. La normalisation HEIC produit une URI de cache `file://`. Le pipeline ne suppose pas qu’un `ph://` peut être renommé : aucune relabellisation de bytes n’existe. Référence auditée : documentation Expo v56 `expo-image-picker` et `expo-file-system`.

## 14. HEIC/JPEG contract

Contrat : bytes HEIC → conversion locale réelle → bytes JPEG → `image/jpeg` → `.jpg`. En cas d’échec, aucun upload n’est tenté et une erreur utilisateur explicite est retournée.

## 15. Video MOV/MP4 contract

`.mov` reste `video/quicktime`; `.mp4` reste `video/mp4`. Aucune conversion fictive MOV → MP4 et aucun changement de MIME sans conversion.

## 16. Multipart/gateway size audit

React Native/FormData reste propriétaire de la boundary. L’interceptor retire tout `Content-Type: application/json` pour FormData et ne construit aucune boundary. Media-service autorise JPEG, PNG, WebP, MP4, WebM et QuickTime; limites Spring : 100 MB fichier, 101 MB requête. Les limites du reverse proxy restent à confirmer dans l’environnement déployé.

## 17. Media runtime tracing

Un `traceId` mémoire corrèle `MEDIA_PICKED`, `MEDIA_VALIDATION_START/OK`, `MEDIA_NORMALIZATION_START/OK`, `MEDIA_FORMDATA_READY`, `MEDIA_UPLOAD_START`, `MEDIA_UPLOAD_HTTP_RESULT`, `MEDIA_CREATED`, `MEDIA_PROCESSING_STATUS`, `MEDIA_ASSOCIATION_START/RESULT` et `MEDIA_UPLOAD_ERROR`. Sont journalisés : type, extension, MIME déclaré/normalisé, taille/bucket, statut HTTP et codes sûrs. Aucun octet, token, URI locale ou URL R2 signée.

## 18. FFmpeg Docker/runtime audit

L’ancienne image `eclipse-temurin:21-jre-jammy` n’installait ni FFmpeg ni ffprobe. Le Dockerfile installe maintenant `ffmpeg`, vérifie `ffmpeg -version` et `ffprobe -version` au build, puis supprime les listes apt. `media-service` doit être reconstruit/redéployé. Aucun téléchargement au démarrage.

## 19. Profile thumbnail verification

Le contrat existant est préservé : résolution batch des médias, choix `thumbnail_url`, puis `url`, puis fallback content. `PublicationGrid` distingue média, vidéo et carte texte. Aucun N+1 ni redesign ajouté.

## 20. Identity audit

Stories et contenus persistent `AUTH_ID`. Le graphe social persiste `PROFILE_UUID`. La conversion se fait exclusivement dans user-service par lecture de profils; aucun cast, aucune heuristique de longueur et aucun mélange des identités.

## 21. Cache/non-regression audit

Les clés viewer-scoped de Feed, profil, stories, suggestions et avis sont inchangées. Le préflight utilise une clé targetType/targetId et un stale time court de 30 secondes; le POST reste autoritatif.

## 22. Mobile files modified

- `src/app/(events)/[id].tsx`
- `src/app/(tabs)/index.tsx`
- `src/components/comments/CommentsThread.tsx`
- `src/components/feed/VerticalFeedList.tsx`
- `src/components/reviews/CreateVerifiedReviewSheet.tsx`
- `src/features/media/media.api.ts`
- `src/features/media/media.runtime-trace.ts`
- `src/features/media/media.utils.ts`
- `src/features/reviews/reviews.api.ts`
- `src/services/api/client.ts`

Les autres fichiers mobiles sales appartiennent à Pass 1 et ont été préservés.

## 23. Backend files modified

- contrôleurs internes d’éligibilité event/booking
- service et contrôleur des avis interaction
- client user-service de content-service
- DTO et test messaging
- test social user-service
- test d’éligibilité interaction
- Dockerfile media-service

## 24. Config files modified

- `cloud-conf-yeyamo/content-service.properties` : `yeyamo.user-service.base-url`.

## 25. Migration files added

- `messaging-service/src/main/resources/db/migration/V3__reconcile_outing_group_owners.sql`.

## 26. Native dependencies added

Aucune.

## 27. Tests executed with exact counts

- Mobile TypeScript : PASS.
- Mobile ESLint ciblé : PASS, zéro erreur.
- user-service : 25 tests, 0 échec.
- content-service : 34 tests, 0 échec.
- interaction-service : suite 31 tests PASS, plus 3 nouveaux tests d’éligibilité PASS.
- messaging-service : 45 tests, 0 échec.
- media-service : 77 tests, 0 échec.
- event-service : 22 tests, 0 échec.
- booking-service : 41 tests, 1 erreur préexistante dans `PlaceActivityIntegrationTest` (mock `findBookableByPlaceIdAfter` retournant null), sans lien avec le DTO reasonCode. Compilation et contexte de l’endpoint modifié réussis.
- `git diff --check` : PASS dans les deux dépôts et le sous-dépôt config.

## 28. Remaining environment-only checks

Test A/B réel des stories après redéploiement; uploads iPhone JPEG/HEIC/MOV/MP4; limites Nginx/ingress; état R2; génération miniature par le conteneur reconstruit; affichage profil image/vidéo/texte; application contrôlée de la migration.

## 29. Services requiring redeployment

`user-service` (Pass 1), `interaction-service`, `event-service`, `booking-service`, `content-service`, `messaging-service`, `media-service`, ainsi que la configuration centralisée/config-server. Le mobile doit être redistribué mais aucune reconstruction native n’est requise.

## 30. Ordered runtime retest plan

1. Publier config puis redéployer user/content/interaction/event/booking/messaging/media.
2. Vérifier migrations et santé; confirmer FFmpeg/ffprobe dans media-service.
3. B suit A; A publie une story; vérifier les deux logs de résolution puis la story chez B.
4. Tester les trois états EVENT du préflight et la création éligible.
5. Créer un groupe neuf : 1; rejoindre : 2; quitter : 1; rejouer Kafka : inchangé.
6. Sur iPhone, envoyer JPEG, HEIC, MOV, MP4 et conserver chaque `traceId` jusqu’à l’association.
7. Vérifier les miniatures profil et toutes les régressions Pass 1.

PASS1_FEED_PROFILE_FIX_PRESERVED = PASS

PASS1_COMMENT_KEYBOARD_FIX_PRESERVED = PASS

PASS1_REVIEW_KEYBOARD_FIX_PRESERVED = PASS

PASS1_LIKE_FIX_PRESERVED = PASS

PASS1_FOLLOW_SUGGESTIONS_FIX_PRESERVED = PASS

EXPLORER_UTF8 = PASS

UTF8_REMAINING_USER_VISIBLE_MOJIBAKE = NONE

REVIEW_ELIGIBILITY_DOMAIN_RULE = PASS

REVIEW_ELIGIBILITY_PREFLIGHT = PASS

REVIEW_POST_REVALIDATION = PASS

STORY_FOLLOWING_AUTH_ID_RESOLUTION = PASS

STORY_CROSS_ACCOUNT_BACKEND_TEST = PASS

STORY_INTERSERVICE_CONFIG = PASS

CROSS_ACCOUNT_STORY_VISIBILITY = CODE_FIXED_RUNTIME_RETEST_REQUIRED

GROUP_OWNER_PERSISTED_MEMBER = PASS

GROUP_MEMBER_COUNT_SOURCE = PASS

GROUP_HISTORICAL_RECONCILIATION = IMPLEMENTED

PHONE_IMAGE_UPLOAD_CODE_CONTRACT = PASS

IOS_MEDIA_URI_HANDLING = PASS

HEIC_REAL_CONVERSION = PASS

PHONE_VIDEO_UPLOAD_CODE_CONTRACT = PASS

MOV_MIME_CONTRACT = PASS

MP4_MIME_CONTRACT = PASS

MULTIPART_CONTRACT = PASS

MEDIA_RUNTIME_TRACE_READY = YES

FFMPEG_PRESENT_IN_DEPLOYABLE_IMAGE = YES

VIDEO_PROCESSING = CODE_READY_RUNTIME_RETEST_REQUIRED

PROFILE_MEDIA_THUMBNAIL_CONTRACT = PASS

COUNTRY_STARTUP_FIX_PRESERVED = PASS

AUTH_ID_PROFILE_UUID_AUDIT = PASS

MOBILE_TYPESCRIPT = PASS

MOBILE_LINT = PASS

GIT_DIFF_CHECK = PASS

USER_SERVICE_TESTS = PASS

CONTENT_SERVICE_TESTS = PASS

INTERACTION_SERVICE_TESTS = PASS

MESSAGING_SERVICE_TESTS = PASS

MEDIA_SERVICE_TESTS = PASS

DATABASE_MIGRATION_REQUIRED = YES

CONFIG_CHANGE_REQUIRED = YES

NEW_NATIVE_DEPENDENCY = NO

MOBILE_NATIVE_REBUILD_REQUIRED = NO

BACKEND_SERVICES_TO_REDEPLOY = user-service, interaction-service, event-service, booking-service, content-service, messaging-service, media-service, config-server/config

P0_CODE_BLOCKERS = NONE

P1_CODE_BLOCKERS = booking-service pre-existing unrelated test fixture failure

ENVIRONMENT_RUNTIME_BLOCKERS = iPhone media execution, deployed inter-service routing, reverse-proxy upload limit, R2, rebuilt FFmpeg image, controlled migration

STATIC_FIX_READY = YES

RUNTIME_RETEST_REQUIRED = YES

READY_FOR_TEST7_RUNTIME_RETEST = YES
