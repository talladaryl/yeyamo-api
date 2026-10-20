# YEYAMO — Cuisine final alignment report

## 1. Résumé exécutif

La consolidation a renforcé `culture-service`, ajouté un test réel du workflow de révision, exposé l'état de révision uniquement aux réponses propriétaire/admin, aligné les types TypeScript sur les DTO Java, résolu les images réelles par `mediaId` et remplacé la page fixe de 100 recettes par une pagination progressive.

Deux conditions bloquent une validation de préproduction : la migration PostgreSQL V6 n'a pas pu être exécutée faute de Docker/PostgreSQL de test, et le dépôt ne possède pas d'identité interservice conforme aux exigences du prompt. Le `503 MEDIA_VALIDATION_UNAVAILABLE` reste donc volontairement fail-closed.

## 2. État Git initial

- Backend : branche `main`, HEAD `4f7620a`.
- Mobile : branche `daryl`, HEAD `837b43a`.
- Modifications Auth et configuration préexistantes préservées.
- Les quatre rapports de référence étaient présents et ont été consultés.

## 3. Inventaire des contrats existants

- Culture : routes publiques/contributions inchangées.
- Media public : metadata, batch, contenu, thumbnail et upload Culture.
- Interactions : favoris et commentaires génériques `CULTURE_CONTENT` existants.
- Sécurité interne : uniquement `X-Internal-Token`, secret partagé global, sans identité, audience ou scope du service appelant.

## 4. Matrice avant correction

| Domaine | Backend | Mobile avant |
|---|---|---|
| Difficulté | codes techniques | libellés français attendus directement |
| Médias | UUID | URLs fictives optionnelles |
| Matériel | objets | chaînes |
| Conseils/sécurité | texte | tableaux |
| Quantités/plages | structurées | minimum seulement |
| Pagination | Spring Page | `size: 100` |
| Révision | candidate interne | aucun état contributeur |

## 5. Modifications culture-service

- Modèle enrichi et synchronisation JPA par UUID.
- Révision candidate indépendante du contenu publié.
- Dates de soumission/décision et motif de rejet autorisé.
- Réponse additive `recipeRevision` pour propriétaire/admin ; GET public sans donnée de révision.
- Validation YouTube stricte.
- Fail-closed pour toute nouvelle association média tant que le contrat sûr est absent.

## 6. Modifications media-service

Aucune. Créer un endpoint protégé par le secret partagé existant aurait contrevenu à l'exigence d'identité interservice authentifiée. Le service ne possède par ailleurs pas de visibilité image/vidéo suffisamment explicite pour décider qu'un média privé peut devenir public dans une recette.

## 7. Migration et PostgreSQL

`V6__enrich_recipe_details.sql` contient les colonnes, backfills UUID, tables, contraintes, trigger inter-recettes et table de révision. Docker Desktop n'était pas démarré et aucun `psql` local isolé n'était disponible. Testcontainers a donc été préparé mais non exécuté.

## 8. Validation média

Le modèle et le mobile savent transporter/résoudre les UUID. L'écriture reste bloquée avec `MEDIA_VALIDATION_UNAVAILABLE`. Politique historique : une référence déjà persistée et inchangée est conservée ; toute nouvelle référence doit être validée.

## 9. Authentification interservice

Blocage : `InternalServiceTokenFilter` prouve seulement la connaissance d'un secret partagé. Contrat minimal à autoriser : jeton de service signé, audience `media-service`, sujet/service `culture-service`, scope `media:associate:culture`, expiration courte et rotation. Aucun changement Auth/infrastructure n'a été improvisé.

## 10. UUID et collections

Les UUID envoyés par les clients modernes sont contrôlés, synchronisés en place et conservés lors des réorganisations. Les relations inter-recettes sont refusées en service et par trigger SQL. La correspondance positionnelle des anciens clients demeure une compatibilité partielle et doit encore être rendue conservatrice pour les éditions ambiguës.

## 11. Modération et révisions

Le test couvre : publication existante, création candidate, lecture publique de l'ancienne version, soumission, approbation et remplacement atomique. Le contrôle de `baseVersion` bloque une candidate obsolète. Une seule révision active est imposée en SQL.

## 12. Compatibilité anciens clients

Lecture préservée. Les champs enrichis absents sont conservés. Omission et `null` explicite ne sont pas encore distinguables pour tous les textes du record Java ; les deux signifient actuellement « conserver » en édition.

## 13. Modifications mobile

- Types backend exacts pour difficulté, quantités, températures, matériel, galerie, médias et révision.
- Adaptateur explicite backend → libellés/structures UI.
- Résolution réelle des hero images via `media.api.ts`.
- Pagination progressive par pages de 20.
- `CatalogListScreen` accepte chargement suivant et footer de chargement.

## 14. Gestion démo/réel

Les clés de cache conservent `demo`/`backend`. Les fixtures ne sont utilisées qu'en session démo. Les erreurs backend restent visibles sans fallback. Les IDs `demo-recipe-*` ne sont pas transmis aux interactions réelles.

## 15. Images et vidéos

Hero réel : raccordé par `heroMediaId`. La vidéo Yeyamo, la galerie et les médias d'étape sont typés et préchargés par batch mais leur rendu complet reste partiel. L'éditeur média réel reste bloqué par la validation interservice.

## 16. YouTube

Backend validé et testé. L'ouverture/aperçu mobile explicite n'est pas encore finalisé : aucun auto-play ni injection WebView non validée n'a été ajouté.

## 17. Ingrédients et portions

Le contrat couvre minimum, maximum, unité canonique, texte historique, groupe et substitution. L'adaptateur préserve les textes ambigus. L'UI de recalcul avancée des plages reste partielle.

## 18. Étapes et mode guidé

Les IDs, durées, température et associations sont alignés. Le mode guidé existant reste fonctionnel mais sa restauration durable par compte/recette/version n'a pas été implémentée dans cette passe.

## 19. Minuteurs et notifications

Minuteurs de session existants préservés. Échéance persistante, reprise après fermeture et notification locale restent partielles/non raccordées.

## 20. Commentaires et favoris

Favoris réels/démo préservés. Le contrat générique de commentaires existe mais son UI intégrée à la fiche recette n'a pas été ajoutée.

## 21. Pagination

La liste réelle utilise maintenant `useInfiniteQuery`, `page` et `size: 20`, avec chargement au bas de la liste. La démo reste locale.

## 22. Tests exécutés

- `mvn -pl culture-service -am test` : PASS, 22 tests Culture, 0 échec, 3 PostgreSQL ignorés.
- Test révision publiée : PASS.
- Tests YouTube et UUID : PASS.
- `npx tsc --noEmit` : PASS.
- `recipe.test.mts` : PASS.
- ESLint ciblé des 6 fichiers Cuisine : PASS, 0 erreur/avertissement.
- `git diff --check` backend/mobile : PASS.

## 23. Tests non exécutés

- PostgreSQL V6/Testcontainers : Docker indisponible.
- Tests sur appareil physique : non exécutés.
- Lecture vidéo, YouTube, notifications et restauration runtime : non exécutées.
- Lint frontend complet : commande tentée mais délai de 60 secondes dépassé ; lint ciblé réussi.

## 24. Vulnérabilités corrigées

- Candidate publiée non exposée par GET public.
- Réponse de première candidate corrigée (`recipeRevision` n'est plus nul).
- Conflit de version avant approbation.
- Domaines YouTube ressemblants/HTTP/credentials refusés.
- Relations étape–ingrédient inter-recettes refusées.
- Média non validable refusé par défaut.

## 25. Risques résiduels

- Migration V6 non exécutée.
- Authentification interservice insuffisante.
- Politique de visibilité image/vidéo incomplète dans Media.
- Sémantique PUT omission/null partielle.
- Ancien client sans UUID ambigu lors d'une réorganisation.
- N+1 possible sur l'état de révision dans la liste propriétaire.

## 26. Fonctionnalités bloquées

- Association réelle de nouveaux médias.
- Vidéo/galerie/éditeur média complets.
- Notifications locales et restauration du minuteur.
- GO préproduction.

## 27. Matrice finale

| Backend | Mobile | État |
|---|---|---|
| `heroMediaId` | résolution `media.api` | raccordé |
| `videoMediaId`, `gallery`, step media | types + batch | rendu partiel |
| difficulté | adaptateur libellé | raccordé |
| matériel/textes | adaptateur | raccordé lecture |
| UUID ingrédients/étapes | types | raccordé lecture, édition partielle |
| quantité min/max/unité | types | recalcul partiel |
| révision | réponse propriétaire | backend raccordé |
| pagination | infinite query | raccordé |

## 28. État Git final

Backend modifié uniquement dans `culture-service` et ce rapport. Mobile modifié uniquement dans six fichiers Cuisine/partagés directement concernés. `media-service`, Auth, Gateway, Interaction, Notification et configuration n'ont pas été modifiés. Aucun commit, push ou déploiement.

## 29. Recommandation

**NO-GO préproduction.** Autoriser d'abord une identité interservice signée/scopée, implémenter et tester le contrat Media, démarrer Docker/PostgreSQL isolé et exécuter V1→V6 avec données historiques, puis compléter les tests runtime mobile.

FRONTEND_BACKEND_ALIGNMENT = PARTIAL

CULTURE_SERVICE_SECURITY = PARTIAL

MEDIA_SERVICE_SECURITY = PARTIAL

MEDIA_INTERSERVICE_AUTH = BLOCKED

MEDIA_ASSOCIATION_VALIDATION = BLOCKED

MEDIA_PRIVATE_ACCESS_PROTECTION = PARTIAL

POSTGRES_V6_MIGRATION = NOT_RUN

HIBERNATE_MAPPING = NOT_RUN

LEGACY_RECIPES_COMPATIBILITY = NOT_RUN

LEGACY_CLIENT_COMPATIBILITY = PARTIAL

STABLE_INGREDIENT_IDS = PARTIAL

STABLE_STEP_IDS = PARTIAL

RECIPE_REVISION_WORKFLOW = PASS

PUBLIC_VERSION_PRESERVATION = PASS

DEMO_REAL_ISOLATION = PASS

REAL_RECIPES_BACKEND_ONLY = YES

COUNTRY_CATALOG_LOCAL = YES

RECIPE_HERO_MEDIA = PARTIAL

RECIPE_VIDEO = BLOCKED

YOUTUBE_INTEGRATION = PARTIAL

RECIPE_EDITOR = PARTIAL

PORTION_SCALING = PARTIAL

GUIDED_COOKING = PASS

TIMER_RESTORATION = PARTIAL

LOCAL_NOTIFICATIONS = BLOCKED

RECIPE_COMMENTS = PARTIAL

RECIPE_PAGINATION = PASS

BACKEND_TESTS = PASS

FRONTEND_TESTS = PARTIAL

SECURITY_TESTS = PARTIAL

PHYSICAL_DEVICE_TESTS = NOT_RUN

UNRELATED_SERVICES_MODIFIED = NO

EXISTING_GIT_CHANGES_PRESERVED = YES

COMMIT_CREATED = NO

PUSH_PERFORMED = NO

DEPLOYMENT_PERFORMED = NO

PRODUCTION_READINESS = NO_GO
