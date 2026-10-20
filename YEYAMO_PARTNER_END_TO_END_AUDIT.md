# Yeyamo — audit statique end-to-end du parcours Partenaire

Date : 2026-10-19  
Mobile audité en lecture seule : `E:\Daryl\yeyamo-mobile`, branche `daryl`, HEAD `837b43a`  
Backend : `yeyamo-api`, branche `main`, HEAD `4f7620a`

## 1. Résumé exécutif

Le socle Partenaire est réel mais hétérogène. Inscription, profil partenaire, lieux, posts, stories, événements, campagnes, ticketing, finance et artisanat ont des contrats backend. En revanche plusieurs écrans du dashboard restent statiques, vides hors démo ou partiellement raccordés : établissements, événements, réservations, avis, notifications, statistiques, paramètres et offres. La navigation globale ne protège pas le groupe `(partner-dashboard)` selon le statut partenaire. Le contrôle d'approbation existe pour la création de lieu, mais n'est pas transversal. Une IDOR confirmée existe sur `PUT /api/v1/places/{id}` : le contrôleur ne transmet aucune identité et `PlaceService.update` n'effectue pas de contrôle propriétaire.

Verdict : le parcours n'est pas prêt pour des tests de production ni un lancement réel.

## 2. Inventaire des routes mobiles

| Route Expo | Composant | Accès observé | Hook/API | Backend | Statut |
|---|---|---|---|---|---|
| `(auth)/register-partner` | fichier homonyme | public/social | `useAuth`, `partnerApi.create` | auth + partner | PARTIAL |
| `(auth)/register-partner-multistep` | fichier homonyme | public | redirection/flow inscription | auth + partner | UNVERIFIED_RUNTIME |
| `(partner)/choice` | fichier homonyme | connecté | navigation | aucun | CONNECTED UI |
| `(partner)/publication` | fichier homonyme | connecté | upload + `postApi` | media/content/feed | PARTIAL |
| `(partner)/story` | alias Create Story | connecté | `storyApi` | content | CONNECTED statique |
| `(partner)/add-place-step1..4` | 4 fichiers | connecté | references, `partnerApi`, `placesApi` | partner/place | PARTIAL |
| `(partner)/add-event-step1..4` | 4 fichiers | connecté | `eventsApi`, upload | event/media | PARTIAL |
| `(partner)/offer` | fichier homonyme | connecté | store local seulement | absent | MOCK/MISSING_BACKEND |
| `(partner-dashboard)/dashboard` | `PartnerProfileDashboard` | connecté | profil + analytics | partner/analytics | PARTIAL |
| `establishments` | fichier homonyme | connecté | `mockData` seulement en démo | place disponible mais non utilisé | BROKEN réel |
| `events` | fichier homonyme | connecté | données mock | event/ticket partiel | BROKEN réel |
| `reservations` | fichier homonyme | connecté | `mockData` | booking non raccordé | MOCK |
| `reviews` | fichier homonyme | connecté | `mockData` | interaction non raccordé | MOCK |
| `statistics` | fichier homonyme | connecté | `mockData` partiel | analytics partiel | PARTIAL |
| `campaigns`, `campaign-create`, `campaign/[id]` | fichiers homonymes | connecté | `useCampaigns` | campaign + analytics | CONNECTED/UNVERIFIED_RUNTIME |
| `promotions`, `promotion-create` | fichiers homonymes | connecté | hooks promotion | commerce/ticket selon contrat | PARTIAL |
| `finance`, `transaction/[id]` | fichiers homonymes | connecté | hooks finance | commerce ledger | CONNECTED/UNVERIFIED_RUNTIME |
| `notifications` | fichier homonyme | connecté | `mockData` local | notification non utilisé | MOCK |
| `settings` | fichier homonyme | connecté | sections statiques | partiel | PARTIAL |
| `event/[id]/tickets`, create/orders/scans/staff/analytics | 6 fichiers | connecté | ticketing/staff | ticket/partner | PARTIAL |
| `artisan-profile`, `artisan-statistics`, `artworks/**`, `artwork-orders/**` | 7 fichiers | artisan | hooks réels | partner/catalog/commerce/analytics | PARTIAL |

Les écrans sont déclarés dans `src/app/_layout.tsx`, mais aucun guard central ne vérifie `PARTNER`, `APPROVED`, `SUSPENDED` ou la propriété d'une ressource avant d'ouvrir les routes dashboard.

## 3. Inscription

`register-partner.tsx` crée d'abord le compte Auth puis appelle `POST /partners`. Le DTO mobile principal transmet nom légal/commercial, type, contact et pays. Le backend `PartnerRequest` accepte davantage de champs KYC. Le code distingue bien compte utilisateur et profil partenaire, mais le flow n'est pas transactionnel entre deux services : un succès Auth suivi d'un échec Partner laisse un compte utilisateur sans partenaire. Le traitement `PARTNER_ALREADY_EXISTS` redirige vers les tabs sans revalider le statut. Tests 400/401/403/409/500 E2E absents.

## 4. Onboarding

Le backend expose documents, exigences KYC et soumission. Le mobile d'inscription ne couvre pas complètement `documents → submit → attente de décision`. Des écrans « devenir artisan » existent côté Profil, distincts du partenaire générique. Le bootstrap global renvoie tous les comptes authentifiés vers `(tabs)` et expose les outils professionnels depuis Profil ; il n'impose pas la fin du KYC.

## 5. Statuts et rôles

Enum réel : `DRAFT`, `SUBMITTED`, `UNDER_REVIEW`, `NEEDS_INFO`, `REQUIRES_CHANGES`, `APPROVED`, `REJECTED`, `SUSPENDED`, `DELETED`. Le mobile de démo utilise aussi `VERIFIED`, valeur non canonique. `PartnerService` protège l'édition/soumission selon l'état et ne rend public qu'un partenaire approuvé. Le frontend masque certaines actions, mais les groupes de routes restent ouverts à tout utilisateur authentifié.

## 6. Dashboard

Le profil provient de `/partners/me`. Les statistiques appellent `/analytics/partners/{partnerId}/dashboard`, mais les métriques `publications` et `establishments` sont forcées à zéro. Plusieurs widgets importent `partner-dashboard/mockData.ts`. Les valeurs de production ne doivent donc pas être considérées fiables.

## 7. Profil public

`GET /partners/{uuid}` ne retourne que les partenaires publics/approuvés. Les profils artisan ont leurs routes publiques dédiées. À valider en runtime : séparation exacte des DTO public/privé et absence de contacts KYC. Le backend utilise un DTO public distinct, point positif.

## 8. Profil privé

`GET/PUT /partners/me`, documents et soumission utilisent l'identité du JWT. Les opérations staff utilisent `partnerId` plus une permission vérifiée en service. Les routes dashboard mobiles ne centralisent cependant ni statut ni permission.

## 9. Publications

Le partenaire réutilise le pipeline Explorateur : upload média, création brouillon `/posts`, publication `/posts/{id}/publish`. La distribution Feed dépend ensuite des événements content/feed. Aucun rattachement explicite `partnerId` n'est envoyé : l'auteur reste l'utilisateur Auth. Cela fonctionne pour une identité sociale, mais ne prouve pas l'affichage sous la marque Partenaire. Modération partenaire spécifique absente.

## 10. Stories

`(partner)/story` réexporte exactement l'écran Story commun. Image/vidéo, expiration et vues utilisent content-service. Le commentaire du code reconnaît l'absence d'association promotion/campagne. La visibilité finale Feed/profil partenaire n'est pas démontrée.

## 11. Événements

Le formulaire envoie `/events` avec le propriétaire utilisateur, lieu et statut client `PENDING` (le serveur garde la décision). Le backend peut publier immédiatement certaines « sorties publiques » selon sa logique propre et ne vérifie pas le statut du partenaire. Le dashboard événements réel repose encore sur mock. Billetterie est séparée et activée par feature flag.

## 12. Lieux

La création vérifie réellement le `partnerId`, son propriétaire et `APPROVED`, via une projection Kafka `PartnerReadModel`; elle échoue fermée si la projection manque. La liste `/places/me` existe. Pourtant l'écran établissements n'utilise pas cette API hors démo et affiche une liste vide. Défaut critique confirmé : `PUT /places/{id}` n'injecte pas `Authentication` et `PlaceService.update` est dépourvu de contrôle propriétaire, permettant la modification d'un lieu connu par ID par un utilisateur authentifié.

## 13. Expériences

Des routes Explorateur `(experiences)` et booking existent, mais aucun flow partenaire dédié de création/gestion d'expérience n'a été trouvé dans `(partner)` ou `(partner-dashboard)`. Statut : `MISSING_FRONTEND`; ne pas inventer de backend supplémentaire avant clarification produit.

## 14. Réservations

Le dashboard réservations lit `mockData`. Des APIs booking Explorateur existent, mais aucune requête « réservations reçues par mon partenaire » n'est raccordée à cet écran. Confirmation/annulation, capacité concurrente, paiement et remboursement ne sont donc pas validables de bout en bout.

## 15. Messagerie

Le système commun possède REST, WebSocket, pagination, lecture et pièces jointes. Aucun inbox séparé Partenaire n'a été trouvé : le partenaire conserve l'expérience sociale commune. L'ouverture depuis lieu/événement vers la bonne identité partenaire et les tests à deux comptes restent `UNVERIFIED_RUNTIME`.

## 16–17. Publicités et campagnes

Campagnes : contrats create/update/submit/pause/resume/cancel/list/detail réels et analytics réels. La feature flag peut désactiver toute mutation. Le mobile ne déclenche pas de paiement réel. Les impressions/clics/conversions de la liste sont initialisées à zéro et `performanceAvailable=false`; seules les pages analytics consomment les métriques. Promotions/offres sont deux concepts différents : l'écran `(partner)/offer` ne fait aucune requête et affiche un faux succès local.

## 18. Notifications

La couche commune `notifications.api.ts` est réelle (liste, unread, read-all, préférences, push). L'écran dashboard Partenaire utilise pourtant sa propre liste mock et ne consomme pas cette API. Les deep links couvrent messages/interactions/booking/events, mais pas toutes les décisions KYC/campagnes.

## 19. Distribution Feed/Kafka

| Origine | Émission | Projection/lecture | Conclusion |
|---|---|---|---|
| Post publié | content events | feed-service `/feed` | chaîne présente, runtime requis |
| Story | content-service | `/stories` | lecture directe, runtime requis |
| Event publié | event publisher, options feed/story | consumers sociaux | PARTIAL : statuts de distribution présents, partenaire non distingué |
| Place | place events | Explorer/carte direct place-service | publication/modération présente |
| Partner status | outbox `partner.*` | projection place-service | présent, dépendance Kafka critique |

La présence d'un publisher ne prouve ni consommation ni idempotence. La création de lieu dépend d'une projection éventuellement en retard. Aucun test E2E Kafka Partenaire n'a été exécuté.

## 20. Sécurité

- **P0 confirmé** : IDOR sur mise à jour de lieu.
- **P1 confirmé** : absence de guard mobile central et absence de politique partenaire approuvé pour événements/posts/stories.
- Les routes internes Partner apparaissent `permitAll` dans Spring Security ; le starter commun les protège par filtre `X-Internal-Token`. Cette défense dépend donc de la configuration correcte du filtre et reste moins explicite que `authenticated` au niveau de chaîne.
- Les routes `me` et documents dérivent correctement le propriétaire du JWT.
- Les campagnes doivent être revalidées par tests BOLA ; leur contrôleur fournit le userId au service, mais aucun test runtime n'a été exécuté ici.

## 21. Matrice API frontend/backend

| Action | Frontend | Gateway/contrôleur | Service/table | État |
|---|---|---|---|---|
| créer profil | `partnerApi.create` POST `/partners` | partner-service | partners | CONNECTED |
| profil privé | GET/PUT `/partners/me` | partner-service | partners | CONNECTED |
| KYC docs/submit | absent/incomplet | `/partners/me/documents`, `/submit` | documents/outbox | MISSING_FRONTEND |
| dashboard analytics | `partnerApi.analytics` | analytics-service | projections analytics | PARTIAL |
| établissements | aucune API en écran | `/places/me` existe | places | MISSING_FRONTEND |
| créer lieu | `placesApi.createPlace` | place-service | places | CONNECTED |
| modifier lieu | non central | `PUT /places/{id}` | places | BROKEN sécurité |
| créer événement | `eventsApi.createEvent` | event-service | events/outbox | PARTIAL |
| liste événements dashboard | mock | routes event existent | events | MISSING_FRONTEND |
| post partenaire | `postApi` | content-service | posts/outbox | PARTIAL |
| story partenaire | `storyApi` | content-service | stories | PARTIAL |
| offre simple | store local | aucun appel | aucune persistance | MOCK |
| réservations reçues | mock | booking à inventorier par propriétaire | bookings | MISSING_FRONTEND |
| avis reçus | mock | interaction existe | reviews | MISSING_FRONTEND |
| notifications dashboard | mock | notification API existe | notifications | MISSING_FRONTEND |
| campagne | `campaignsApi` | campaign-service | campaigns | CONNECTED |
| campagne analytics | `campaignsApi` | analytics-service | metrics | CONNECTED/UNVERIFIED_RUNTIME |
| finance | hooks finance | commerce ledger | ledger | CONNECTED/UNVERIFIED_RUNTIME |
| messagerie | couche commune | messaging REST/WS | conversations/messages | UNVERIFIED_RUNTIME |

## 22. Comparaison Explorateur / Partenaire

| Domaine | Explorateur | Partenaire | Écart | Réutilisation |
|---|---|---|---|---|
| Auth | testé plus largement | compte + profil en deux phases | reprise partielle | auth commun |
| Profil | social complet | professionnel partiel | KYC/status | primitives profil |
| Publication | pipeline réel | même pipeline sans marque | identité business | post/media commun |
| Story | pipeline réel | alias exact | promo absente | story commun |
| Événement | création réelle | formulaire distinct, dashboard mock | modération/statut | API event commune |
| Lieu | Explorer lecture | création sécurisée mais liste UI vide | dashboard | `placesApi.myPlaces` |
| Réservation | client présent | réception mock | majeur | modèles booking |
| Messagerie | commune | commune non prouvée | deep links | chat commun |
| Notifications | API commune | écran dashboard mock | duplication | notifications hooks |

## 23. Anomalies classées

| ID | Priorité | Preuve/état | Anomalie | Correction recommandée | Complexité |
|---|---|---|---|---|---|
| PAR-P0-01 | P0 | CONFIRMED `PlaceController`/`PlaceService` | PUT lieu sans contrôle propriétaire | injecter acteur, vérifier propriétaire/admin, tests IDOR | M |
| PAR-P1-01 | P1 | CONFIRMED | dashboard établissements vide hors démo | raccorder `/places/me`, pagination/états | S |
| PAR-P1-02 | P1 | CONFIRMED | réservations dashboard mock | endpoint propriétaire + hook réel | M/L |
| PAR-P1-03 | P1 | CONFIRMED | offre affiche succès sans persistance | désactiver honnêtement ou créer contrat autorisé | M |
| PAR-P1-04 | P1 | CONFIRMED | dashboard événements mock | endpoint « mes événements créés » distinct de `/events/me` (inscriptions) | M |
| PAR-P1-05 | P1 | PROBABLE | non-approuvé peut appeler event/post/story | politique backend par action | L |
| PAR-P1-06 | P1 | CONFIRMED | notifications partenaire mock malgré API commune | réutiliser hook réel + deep links | S |
| PAR-P1-07 | P1 | CONFIRMED | KYC onboarding incomplet | flow documents/soumission/statut | M |
| PAR-P2-01 | P2 | CONFIRMED | métriques dashboard partiellement forcées à 0 | enrichir analytics ou masquer valeurs | M |
| PAR-P2-02 | P2 | CONFIRMED | `VERIFIED` démo ≠ enum backend | aligner fixture sur `APPROVED` | S |
| PAR-P2-03 | P2 | TO_VERIFY | identité sociale utilisateur vs marque partenaire | définir auteur business | L |
| PAR-P2-04 | P2 | TO_VERIFY | retards/duplications projections Kafka | outbox/idempotence/monitoring | L |
| PAR-P3-01 | P3 | CONFIRMED | composants Stepper anciens et styles locaux | harmoniser sans toucher métier | S |

## 24. Tests exécutés

- Recherche statique routes Expo, hooks, appels API, contrôleurs, DTO, Gateway et consommateurs ciblés.
- Aucun code Partenaire modifié.
- Compilation/tests globaux mobile non lancés : l'audit est lecture seule et le dépôt mobile porte déjà des changements Cuisine.

## 25. Tests non exécutés

Pas de comptes réels, publication, message, réservation, paiement, campagne payante, WebSocket, Kafka ou push. Aucun environnement E2E isolé/identifiants n'a été fourni.

## 26. Scénarios manuels prioritaires

| Rôle | Précondition | Étapes | Attendu | Priorité |
|---|---|---|---|---|
| non connecté | session vide | ouvrir dashboard | login, aucune donnée | P0 |
| explorateur | JWT Explorateur | ouvrir route partenaire directe | refus/CTA devenir partenaire | P0 |
| partenaire DRAFT | profil créé | créer lieu/event/post | lieu refusé; autres selon politique explicite | P0 |
| partenaire SUBMITTED | KYC soumis | modifier docs/publier | restrictions cohérentes | P1 |
| partenaire APPROVED | projection Kafka à jour | lieu→event→feed→booking | visible de bout en bout | P0 |
| partenaire SUSPENDED | ressources existantes | update/publish/campaign | refus backend | P0 |
| autre partenaire | ID ressource tiers | PUT/DELETE/analytics | 403/404 sans fuite | P0 |
| explorateur | offre publiée | réserver/message/commenter | état visible aux deux comptes | P1 |
| admin | contenu soumis | approuver/rejeter | statut + notification + projection | P1 |

## 27. Plan de correction

Voir `YEYAMO_PARTNER_CORRECTION_ROADMAP.md`. Commencer par IDOR et autorisations, puis KYC/navigation, puis remplacer les écrans mock par les contrats réels.

## 28. Risques de régression

La mutualisation Post/Story/Messaging peut affecter l'Explorateur ; conserver les contrats communs et ajouter le contexte business de façon additive. Les projections Kafka de statut Partner doivent être idempotentes. Ne jamais faire reposer l'autorisation sur le seul masquage mobile.

## 29. État Git final

Le dépôt mobile n'a pas été modifié. Ses six changements Cuisine préexistants sont conservés. Backend Partenaire non modifié. Aucun commit/push/déploiement.

## 30. Verdict

```text
PARTNER_AUDIT = COMPLETE
PARTNER_REGISTRATION = PARTIAL
PARTNER_ONBOARDING = BROKEN
PARTNER_DASHBOARD = BROKEN
PARTNER_PROFILE = PARTIAL
PARTNER_POSTS = PARTIAL
PARTNER_STORIES = PARTIAL
PARTNER_EVENTS = PARTIAL
PARTNER_PLACES = PARTIAL
PARTNER_BOOKINGS = BROKEN
PARTNER_MESSAGES = UNVERIFIED_RUNTIME
PARTNER_CAMPAIGNS = UNVERIFIED_RUNTIME
PARTNER_FEED_DISTRIBUTION = UNVERIFIED_RUNTIME
PARTNER_SECURITY = FAIL
PARTNER_E2E_TESTS = NOT_RUN
PARTNER_PRODUCTION_READINESS = NO_GO

EXPLORER_MODIFIED = NO
PARTNER_CODE_MODIFIED = NO
UNRELATED_SERVICES_MODIFIED = NO
EXISTING_GIT_CHANGES_PRESERVED = YES
COMMIT_CREATED = NO
PUSH_PERFORMED = NO
DEPLOYMENT_PERFORMED = NO
```
