# YEYAMO — Audit de la fiche recette avant refonte

Date d'audit : 2026-10-16  
Mode : lecture seule du mobile, du monorepo backend et de la configuration Gateway.

## 1. Résumé exécutif
 
Le parcours réel existe de bout en bout : sélection d'un pays, liste des recettes, navigation par identifiant, lecture du contenu et des traductions, puis affichage de la fiche. La première rupture n'est donc pas une route absente, mais un contrat recette trop réduit et un écran qui n'exploite même pas toutes les données génériques disponibles.

Le backend possède déjà un sous-modèle structuré : ingrédients ordonnés (`name`, `quantity`, `unit`), étapes ordonnées (`instruction`), temps de préparation et portions. Il ne possède ni temps de cuisson/repos/total, ni durée par étape, difficulté, équipement, substitutions, conseils, sécurité, galerie ou référence vidéo. Les quantités étant des chaînes libres, le recalcul automatique des portions n'est pas fiable sans évolution additive.

La fiche mobile actuelle est fonctionnelle mais minimale : titre, type/catégorie, préparation, portions, ingrédients et étapes. Elle n'affiche ni résumé/corps/histoire, ni pays/région, ni image, ni actions. Le formulaire de contribution permet de créer les ingrédients et étapes mais envoie toujours `prepTimeMinutes: null` et `servings: null`; il ne gère aucun média.

La refonte doit étendre le modèle existant, pas créer un second modèle. Le pipeline média (image/vidéo), le lecteur `expo-video`, le sélecteur média, le système générique de favoris/commentaires et le composant de progression sont réutilisables. Une migration additive sera nécessaire pour une fiche complète et un mode guidé robuste.

## 2. Périmètre inspecté

- Mobile : `src/app/(tabs)/explore.tsx`, `src/app/(explore)/transmission.tsx`, `recipes.tsx`, `recipes/country/[code].tsx`, `recipes/[id].tsx`, `src/features/culture/**`, `src/app/(create)/culture-contribution.tsx`, primitives UI/formulaire/média/interactions.
- Backend : `culture-service` (contrôleurs, DTO, domaine, service, repositories, sécurité, migrations V1–V5), `media-service`, `interaction-service`.
- Routage : `cloud-conf-yeyamo/api-gateway.properties`.
- Hors périmètre : modification de code, exécution des services, base de données, déploiement et test sur appareil.

## 3. État Git

### `yeyamo-mobile`

- Branche : `daryl`.
- HEAD : `d287554 avancé yeyamo`.
- `git status --short` : propre au moment du contrôle.

### `yeyamo-api`

- Branche : `main`.
- HEAD : `4f7620a resolution de conflit test 7A-B`.
- Modifications non committées préexistantes dans `auth-service` et sous-module `cloud-conf-yeyamo`; fichiers de rapports non suivis. Elles ne concernent pas la recette et n'ont pas été touchées.
- `git diff --check` : uniquement des avertissements de conversion LF/CRLF sur les fichiers Auth préexistants.

### `cloud-conf-yeyamo`

- Branche : `daryl`.
- HEAD : `69617fc Correction new bug test8.`.
- `auth-service.properties` modifié avant l'audit; non touché.

Contrats Culture & Créateurs récemment présents à préserver : routes publiques Culture, contributions authentifiées, structure V4 et routage Gateway.

## 4. Parcours Cuisine complet

| Étape | Route/composant | Données et comportement |
|---|---|---|
| Explorer | `src/app/(tabs)/explore.tsx` | Entrée Culture/Créateurs et navigation vers les catalogues. |
| Culture | `src/app/(explore)/transmission.tsx` | Carte « Recettes » vers `/(explore)/recipes`. |
| Pays | `src/app/(explore)/recipes.tsx` | `useAvailableCountries()`; loading, erreur + retry, vide; grille 2 colonnes. |
| Recettes du pays | `src/app/(explore)/recipes/country/[code].tsx` | paramètre `code`; `useCultureContents({type:'RECIPE', countryCode: code, size:100})`; démo locale sinon API. |
| Détail | `src/app/(explore)/recipes/[id].tsx` | paramètre `id`; `useCultureContent(id)`; démo locale sinon deux GET parallèles. |
| Création/édition | `src/app/(create)/culture-contribution.tsx` | formulaire unique multi-type; POST/PUT de contribution, puis soumission. |

Chaîne exacte : `CatalogListScreen` reçoit `detailBase="/(explore)/recipes"` → tap pousse `/(explore)/recipes/{id}` → `useLocalSearchParams` lit `id` → `useCultureContent` → `cultureApi.getContent` exécute `GET /culture/contents/{id}` et `GET /culture/contents/{id}/translations` → client ajoute `/api/v1` → Gateway route `/api/v1/culture/**` vers `culture-service` → `ContentResponse`/`TranslationResponse` → types `CultureContentDetail` → rendu.

Première rupture fonctionnelle : la liste backend fabrique son titre depuis `slug`, son image est `undefined`, et son temps vaut `0`; ensuite la fiche n'affiche qu'une fraction du contenu générique et du sous-modèle recette. La pagination est techniquement disponible dans l'API, mais l'écran pays demande une page unique de 100 éléments sans chargement progressif.

## 5. Écran de détail actuel

Fichier : `src/app/(explore)/recipes/[id].tsx`, composant `RecipeDetail`.

| Donnée/fonction | État | Preuve |
|---|---|---|
| Nom | DISPLAYED_AND_WORKING | première traduction ou `slug`; démo `demo.name`. |
| Image principale/galerie | MISSING | aucun champ média lu ni composant Image. |
| Description/histoire | AVAILABLE_NOT_DISPLAYED/PARTIAL | `summary`/`body` existent dans `CultureTranslation`; fallback body seulement si aucun détail structuré. |
| Pays/région/origine | AVAILABLE_NOT_DISPLAYED/PARTIAL | `countryCode`, niveaux administratifs, ville, communauté, source existent; pas d'origine narrative structurée. |
| Préparation | DISPLAYED_AND_WORKING | `recipeDetails.prepTimeMinutes`. |
| Cuisson/repos/total | MISSING | absents du contrat. |
| Portions | DISPLAYED_AND_WORKING backend; MISSING démo | `servings` affiché seulement hors démo. |
| Difficulté | MISSING | aucun champ. |
| Ingrédients | DISPLAYED_AND_WORKING | ordre de tableau, quantité/unité concaténées. |
| Étapes | DISPLAYED_AND_WORKING | numérotation par index. |
| Médias/vidéo | MISSING | aucune référence dans `RecipeDetails`. |
| Favori/partage/commentaire/avis | AVAILABLE_NOT_DISPLAYED ou MISSING | partage natif et interactions génériques existent ailleurs; aucune intégration recette; les avis vérifiés ne ciblent pas explicitement les recettes. |

UX prouvée : `SafeScreen`, thème global, `ScrollView`, bouton retour accessible, loading et introuvable. Lacunes : pas d'état d'erreur avec retry distinct de « introuvable », pas de hero, hiérarchie très plate, listes non interactives, aucune action, aucun mode guidé, padding bas fixe sans analyse explicite de la barre flottante. Responsive/dark mode reposent correctement sur les tokens. VoiceOver ne reçoit pas de structure sémantique pour sections/étapes. À confirmer sur appareil : chevauchement avec la barre flottante et comportement aux très grandes tailles de police.

## 6. Modèles frontend

`src/features/culture/culture.types.ts` :

- `CultureRecipeIngredient`: `name`, `quantity: string|null`, `unit: string|null`.
- `CultureRecipeStep`: `instruction` uniquement.
- `CultureRecipeDetails`: `ingredients[]`, `steps[]`, `prepTimeMinutes`, `servings`.
- `CultureContent`: métadonnées d'identité, localisation, provenance, vérification, sensibilité et dates, plus `recipeDetails` optionnel.
- `CultureTranslation`: `title`, `summary`, `body` et statut.
- `CultureContributionInput` réutilise exactement ce modèle.

Le mobile consomme le DTO détaillé existant, pas un DTO résumé distinct. Toutefois `CultureContent` omet actuellement côté TypeScript `adminLevel2Id`, `localityId`, `createdBy`, `contributorType` et `version` présents dans `ContentResponse` Java.

## 7. Modèles backend

Sources : `CultureContent.java`, `RecipeDetailsEntity.java`, `CultureTranslation.java`, `CultureDtos.java`.

| Propriété | Classification |
|---|---|
| titre, résumé, histoire/contexte (`body`) | PERSISTED_STRUCTURED dans `culture_content_translations` (texte libre pour le contenu) |
| pays, région IDs, ville, localité, communauté, source, vérification | PERSISTED_STRUCTURED dans `culture_contents` |
| préparation, portions | PERSISTED_STRUCTURED dans `culture_recipe_details` |
| nom/quantité/unité ingrédient | PERSISTED_STRUCTURED; quantité/unité sémantiquement libres |
| ordre ingrédient | PERSISTED_STRUCTURED via `display_order`/`@OrderColumn` |
| instruction étape | PERSISTED_STRUCTURED comme texte |
| ordre étape | PERSISTED_STRUCTURED via `display_order`/`@OrderColumn` |
| cuisson, repos, total, difficulté, rendement | MISSING |
| optionnel, groupe, substitution, lien ingrédient-étape | MISSING |
| ID stable d'ingrédient/étape | MISSING; clé DB = `(content_id, display_order)` |
| titre/durée/température/matériel/média/résultat/sécurité d'étape | MISSING |
| média principal, galerie, vidéo recette | MISSING dans le domaine recette |
| équipement, préparation préalable, astuces, variantes, allergies, conservation, réchauffage, dressage | MISSING comme champs dédiés; peut seulement être noyé dans `body` (PERSISTED_FREE_TEXT). |

## 8. Contrats API

| Service | Méthode/route publique | Auth | DTO/persistance | Mobile |
|---|---|---|---|---|
| culture | GET `/api/v1/culture/contents` | publique | `Page<ContentResponse>`; filtres type/pays/région/ville/langue/communauté/vérifié/recherche/page | `cultureApi.listContents` |
| culture | GET `/api/v1/culture/contents/{id}` | publique | `ContentResponse`, seulement publié/public/non sacré | `cultureApi.getContent` |
| culture | GET `/api/v1/culture/contents/{id}/translations` | publique | traductions vérifiées | `cultureApi.getContent` |
| culture | POST `/api/v1/culture/contributions` | authentifiée | `ContentRequest` → brouillon | `createContribution` |
| culture | PUT `/api/v1/culture/contributions/{id}` | propriétaire | même DTO | `updateContribution` |
| culture | POST `/api/v1/culture/contributions/{id}/submit` | propriétaire | transition de modération | `submitContribution` |
| media | POST `/api/v1/media/culture` | authentifiée | multipart, `usageType`, metadata | `uploadMedia(...usageType)` |
| media | GET `/api/v1/media/{id}`, `/content`, `/thumbnail`, `/batch` | selon politique média | metadata/contenu | `media.api.ts` |
| interaction | PUT/DELETE `/api/v1/interactions/favorites/CULTURE_CONTENT/{id}` | authentifiée | `GenericInteraction` | hooks génériques disponibles |
| interaction | GET/POST `/api/v1/interactions/CULTURE_CONTENT/{id}/comments` | lecture/création selon sécurité | `GenericInteraction` | hooks génériques disponibles |

Le Gateway couvre Culture, Media et Interactions. Aucun endpoint recette spécialisé n'est requis pour lire la structure actuelle. Les extensions doivent rester dans `ContentRequest/Response.recipeDetails` afin de préserver le flux.

## 9. Ingrédients

Stockage : table dédiée `culture_recipe_ingredients`, objets JPA `@ElementCollection`, ordre explicite. La quantité et l'unité sont des `VARCHAR(80)`; elles ne garantissent ni nombre décimal ni unité canonique. Conséquences :

- checklist locale : faisable sans backend;
- groupes, optionalité, substitutions, lien aux étapes : non disponibles;
- liste de courses : affichage possible, agrégation fiable impossible;
- recalcul de portions : seulement partiel et dangereux avec « une pincée », fractions/locales ou quantité vide;
- aucun composant checklist culinaire dédié trouvé; le pattern checkbox accessible de `ReviewStep` est adaptable, pas réutilisable tel quel.

## 10. Étapes

Elles sont ordonnées mais sans identifiant stable. Chaque suppression/réorganisation change la clé logique `display_order`. Seule l'instruction existe. Pas de durée, température, médias, ingrédients référencés, matériel, sous-étapes, résultat attendu, préparation préalable ou sécurité. Un mode pas-à-pas basique peut afficher une instruction après l'autre depuis la liste existante; un mode riche et restaurable exige des IDs stables et champs additifs. Il ne faut pas dupliquer les étapes dans un second modèle.

## 11. Durées

- Préparation : YES, minutes entières positives, mais l'éditeur envoie actuellement `null`.
- Cuisson/repos/total : absents.
- Étape : absente.
- Démo : `preparationMinutes` seulement.

Le total devra être calculé avec une règle documentée ou persisté; des temps parallèles rendent une simple somme potentiellement fausse.

## 12. Médias et vidéo

La recette ne référence aucun média. En revanche :

- `media-service` accepte IMAGE (10 MiB par défaut) et VIDEO MP4/WebM/QuickTime (100 MiB), stockage routé public/privé, thumbnails et métadonnées de durée;
- `src/features/media/media.api.ts` sait uploader et résoudre metadata/URLs;
- `useYeyamoMediaPicker` gère galerie, caméra photo/vidéo et permissions;
- `VideoCard.tsx` prouve l'usage d'`expo-video`; `expo-image` est installé.

YouTube/URL externe : aucun contrat recette ni composant dédié/validation observé. Recommandation : privilégier des `mediaId` Yeyamo; n'ajouter une URL externe que si le produit la valide explicitement (allowlist HTTPS, consentement et confidentialité). Galerie et média par étape exigent des relations structurées. La vidéo doit être chargée à la demande, jamais automatiquement à l'ouverture.

## 13. Portions

`servings` est un entier positif nullable. Il n'existe ni rendement ni unité de rendement. Le multiplicateur UI est faisable uniquement après normalisation optionnelle des quantités (`quantityValue`, fraction, plage, unité canonique) avec fallback conservant `quantityText`. Ne jamais transformer silencieusement les recettes historiques.

## 14. Matériel

Aucun champ ni table ne décrit les ustensiles. Extension additive recommandée : collection ordonnée avec ID stable, nom et indicateur facultatif. Aucun matériel ne doit être inféré des instructions historiques.

## 15. Préparation préalable

Trempage, lavage, découpe et marinade ne sont pas structurés. Ils peuvent exister seulement dans `body` ou l'instruction. Prévoir soit une section textuelle dédiée, soit des étapes typées; éviter deux listes concurrentes.

## 16. Conseils et sécurité

Aucun champ dédié pour astuces, variantes, substitutions, allergies, conservation, réchauffage, températures, accompagnement ou dressage. Les métadonnées de vérification du contenu existent (`verificationStatus`, sensibilité, workflow de modération), mais ne certifient pas individuellement une consigne de sécurité. Toute donnée sanitaire doit être saisie et modérée, jamais générée ou déduite automatiquement.

## 17. Mode guidé

Faisabilité actuelle : PARTIAL. On peut construire une navigation locale indexée sur `steps[]`, une progression et précédent/suivant. On ne peut pas restaurer sûrement une progression après édition sans ID stable, ni proposer média/minuteur/ingrédients par étape. Source unique impérative : le même `recipeDetails.steps` alimente fiche et mode guidé. `YeyamoFormProgress` est adaptable visuellement; `Stepper` est son alias déprécié. Aucun Carousel culinaire trouvé.

## 18. Minuteurs

Aucun composant minuterie partagé trouvé; le timer de vérification Auth et l'intervalle Story ne sont pas des abstractions réutilisables. Aucun champ `stepDuration` n'existe. Un timer JS local ne nécessite pas de dépendance native pour pause/reprise/reset et calcul par timestamp, mais les notifications fiables en arrière-plan peuvent utiliser `expo-notifications`, déjà installé/configuré. La restauration exige une persistance locale (`recipeId`, `stepId`, échéance, pause), avec décision produit sur notifications.

## 19. Création et édition

`culture-contribution.tsx` est le formulaire unique et doit être étendu, pas remplacé. Il utilise React Hook Form + Zod, `useFieldArray`, cinq étapes, `YeyamoFormScreen`, `YeyamoFormProgress` et le workflow brouillon → soumission → modération.

Présent : titre, langue/pays/communauté, résumé, contexte, ingrédients nom/quantité/unité, instructions. Absent dans l'UI : préparation et portions malgré leur contrat; cuisson/repos/difficulté; médias/vidéo; groupes, optionalité/substitutions; métadonnées d'étape; matériel; astuces/sécurité. `publish()` force actuellement préparation et portions à `null`. Une route PUT existe dans l'API mobile, mais aucun écran d'édition recette dédié n'a été trouvé dans le parcours inspecté.

## 20. Interactions existantes

- Favoris : backend et mobile acceptent `CULTURE_CONTENT`; non branché sur la fiche.
- Commentaires : hooks/API génériques acceptent `CULTURE_CONTENT`; non branchés.
- Partage : `Share.share` est déjà utilisé par `culture/[id].tsx`; adaptable.
- Signalement : `CULTURE_CONTENT` existe dans `moderation.api.ts`; branchement fiche à confirmer.
- Avis/notation : le système `reviews.api.ts` est vérifié et typé par cibles; aucune preuve que RECIPE/CULTURE_CONTENT soit une cible autorisée. Ne pas le détourner.
- Modification/modération : propriétaire via contributions; administration via routes culture; états DRAFT/SUBMITTED/UNDER_REVIEW/APPROVED/PUBLISHED, etc.

## 21. Inventaire des composants réutilisables

- Structure : `SafeScreen`, `YeyamoFormScreen`, thème global.
- Images : `expo-image`, `AccessibleImage`, `MediaGrid` (adaptation requise).
- Vidéo : logique `VideoCard`/`expo-video` (extraire un lecteur non-Feed).
- Sélection/upload : `useYeyamoMediaPicker`, `media.api.ts`.
- Progression : `YeyamoFormProgress`; `Stepper` redirige vers lui.
- Actions : `Button`, `AccessibleButton`, `Icon`.
- États : `AppErrorScreen` et états locaux; consolidation souhaitable.
- Modale : `YeyamoModal`, `FilterBottomSheet` comme patterns.
- Interactions : hooks génériques favoris/commentaires, `Share.share`.

## 22. Matrice anti-duplication

| Besoin | Existant | Décision | Adaptation |
|---|---|---|---|
| Image principale | `expo-image`, `AccessibleImage` | ADAPT_EXISTING | hero, placeholder, alt text |
| Lecteur vidéo | `VideoCard` + `expo-video` | EXTEND_EXISTING | lecteur à la demande hors Feed |
| Galerie | `MediaGrid` | ADAPT_EXISTING | lecture seule et plein écran |
| Checklist | pattern checkbox accessible | NEW_ONLY_IF_REQUIRED | composant recette léger |
| Minuteur | aucun composant générique | NEW_ONLY_IF_REQUIRED | timestamp/pause/restauration |
| Étape guidée | aucun | NEW_ONLY_IF_REQUIRED | vue du même modèle d'étape |
| Progression | `YeyamoFormProgress` | ADAPT_EXISTING | variante non formulaire |
| Favoris | interactions génériques | REUSE_AS_IS | cible `CULTURE_CONTENT` |
| Partage | `Share.share` Culture | REUSE_AS_IS | deep link recette |
| Avis | avis vérifiés non prouvés pour recette | NO_CHANGE | ne pas brancher sans contrat |
| Commentaires | interactions génériques | REUSE_AS_IS | intégrer hooks existants |
| Formulaire recette | `culture-contribution.tsx` | EXTEND_EXISTING | champs et médias additifs |

## 23. Migrations existantes

Dernière migration Culture : `V5__seed_cameroon_configured_languages.sql`. La structure recette vient de `V4__structured_culture_content.sql` :

- `culture_recipe_details(content_id PK/FK, prep_time_minutes CHECK >0, servings CHECK >0)`;
- `culture_recipe_ingredients(content_id, display_order, name, quantity, unit)`, PK composite;
- `culture_recipe_steps(content_id, display_order, instruction)`, PK composite.

Aucun JSON recette. Pas d'index secondaire, ce qui reste acceptable pour les collections chargées par `content_id`. Future stratégie : migrations V6+ additives, colonnes nullables et/ou tables filles avec IDs UUID; backfill non destructif; lecture compatible quand les nouveaux champs sont nuls. Préserver les PK/ordres historiques ou migrer vers IDs stables sans perdre l'ordre.

## 24. Performances

- La fiche lance deux requêtes parallèles; acceptable, mais un endpoint détail agrégé pourrait être envisagé seulement si mesuré nécessaire.
- La liste `size:100` sans pagination est le principal risque immédiat.
- Images : thumbnails, dimensions, cache `expo-image`, chargement progressif.
- Vidéo : poster d'abord, initialisation au tap, pause hors écran, qualité adaptée; aucune prélecture du fichier complet.
- Étapes : une liste courte peut rester dans ScrollView; utiliser une liste virtualisée seulement pour recettes réellement longues.
- Offline : React Query peut conserver le cache en mémoire, mais aucune garantie de persistance recette n'a été prouvée.

## 25. Accessibilité

Conserver zones tactiles ≥44 pt, labels explicites, ordre de lecture, contrastes du thème et taille dynamique. Ajouter titres accessibles par section, libellés complets pour quantités, état `checked` des ingrédients, valeur de progression du mode guidé, commandes vidéo/minuteur nommées, alternatives textuelles et sous-titres. Ne pas coder l'information uniquement par couleur. Tester VoiceOver/TalkBack, texte XXL et réduction des animations. Le bouton retour actuel possède un label; les simples `Text` d'étapes n'ont pas de sémantique structurée.

## 26. Matrice des écarts

| Fonctionnalité | État | Priorité/écart | Cause racine | Correction | Backend | Risque |
|---|---|---|---|---|---|---|
| Photo principale | absente | P0 | aucun mediaId | hero + media principal | oui | moyen |
| Description/origine | disponible non affichée | P0 | UI minimale | afficher translation/localisation | non | faible |
| Vidéo | absente | P1 | aucune référence | média Yeyamo à la demande | oui | moyen |
| Temps cuisson/repos | absent | P0 | contrat incomplet | champs minutes nullables | oui | faible |
| Ingrédients | structurés minimaux | P0 | quantité texte | checklist + modèle compatible | partiel | moyen |
| Portions | présent mais non saisi | P0 | éditeur incomplet | ajouter saisie; scaling prudent | UI oui; scaling backend conseillé | moyen |
| Étapes détaillées | instruction seule | P0 | contrat incomplet | IDs + champs additifs | oui | élevé |
| Minuteur | absent | P1 | durée étape absente | timer local après contrat | oui pour durée | moyen |
| Mode guidé | partiel | P1 | IDs/données absents | vue du modèle unique | oui | élevé |
| Matériel/préalable | absent | P1 | modèle absent | sections facultatives | oui | faible |
| Astuces/sécurité | absent | P1 | modèle absent | champs éditoriaux modérés | oui | moyen |
| Favori/commentaire/partage | disponible ailleurs | P1 | non intégré | réutiliser interactions | non | faible |
| Édition riche | absente | P0 | formulaire minimal | étendre l'unique formulaire | oui | élevé |
| Pagination liste | page unique 100 | P2 | écran simplifié | infinite query/list | non | faible |

## 27. Architecture UX cible

| Section | Disponible | Manquant/adaptation | Priorité |
|---|---|---|---|
| A Hero photo/titre/origine | titre/localisation | mediaId + composant hero | P0 |
| B Résumé durée/difficulté/portions | préparation/portions | cuisson/repos/difficulté | P0 |
| C Actions | services partage/favori | branchement et vidéo | P1 |
| D Présentation/histoire | summary/body | rendu structuré | P0 |
| E Ingrédients | liste minimale | checklist/groupes/scaling prudent | P0 |
| F Matériel | rien | modèle + éditeur facultatif | P1 |
| G Préparation préalable | body libre possible | champ/étapes typées | P1 |
| H Étapes détaillées | instruction ordonnée | ID/durée/média/etc. | P0 |
| I Mode guidé | progression réutilisable | écran local basé sur mêmes étapes | P1 |
| J Conseils/sécurité | rien | données modérées facultatives | P1 |
| K Dressage/conservation | rien | champs facultatifs | P2 |
| L Interactions | backend/hooks existants | composition dans la fiche | P1 |

La fiche existante peut être décomposée en sections sans réécriture du flux de navigation ni second domaine recette.

## 28. Extensions backend nécessaires

Proposition additive à confirmer avec produit :

- `culture_recipe_details` : `cook_time_minutes`, `rest_time_minutes`, `difficulty`, `yield_text`, `hero_media_id`, `video_media_id`, `equipment_notes`, `make_ahead_notes`, `tips`, `safety_notes`, `serving_notes`, `storage_notes`.
- Galerie : table ordonnée `culture_recipe_media` avec UUID, `media_id`, rôle, légende et alt text.
- Ingrédients : ID UUID stable; conserver `quantity` texte, ajouter éventuellement valeur numérique/plage et unité canonique, groupe, optionnel, substitution.
- Étapes : ID UUID stable, titre, durée, température/ unité, résultat attendu, sécurité, mediaId; table de liaison ingrédients-étapes si nécessaire.
- DTO request/response : champs nullables et collections vides; validation par type; compatibilité des anciens clients.
- Validation des mediaId/ownership et usage Culture via media-service, sans copie de média.

Ne pas rendre obligatoires les nouveaux champs pour les recettes historiques.

## 29. Plan d'implémentation futur

### Phase A — Contrats et mapping
Étendre migrations, entités, DTO Java, types TS et fixtures. Tests migration, validation, sérialisation et compatibilité anciens payloads. Acceptation : anciennes recettes toujours lisibles.

### Phase B — Structure UX
Décomposer `recipes/[id].tsx` avec primitives existantes, états erreur/retry et sections conditionnelles. Aucun changement des routes précédentes. Tests rendu light/dark/texte long.

### Phase C — Médias
Réutiliser media picker/API/`expo-image`/`expo-video`; ajouter références contrôlées au contrat et éditeur. Tests image, vidéo, erreur, connexion lente. Acceptation : aucune lecture automatique.

### Phase D — Ingrédients et portions
Checklist locale, saisie portions, stratégie quantité compatible. Tests unités ambiguës et fallback texte. Aucun scaling automatique avant données fiables.

### Phase E — Étapes enrichies
IDs stables et champs facultatifs; éditeur `useFieldArray` enrichi. Tests ordre, ajout/suppression, anciennes étapes.

### Phase F — Guidé/minuteurs
Nouvel écran ou modal basé sur les mêmes données; persistance locale par ID; notifications seulement si décision produit. Tests arrière-plan, pause, navigation et édition concurrente.

### Phase G — Conseils/finalisation
Matériel, préalable, conseils, sécurité, conservation, accessibilité et modération. Sections masquées quand vides.

### Phase H — Éditeur
Étendre `culture-contribution.tsx`, sans second formulaire; brouillon et soumission existants préservés. Tester payload complet et minimal.

### Phase I — Non-régression
Tests du parcours pays → liste → détail, démo/backend, contributions, Gateway, media, interactions, iOS/Android, offline dégradé et performances.

## 30. Risques de régression

- rendre les nouveaux champs obligatoires casserait anciens clients/recettes;
- changer les clés `(content_id, display_order)` sans migration sûre pourrait réordonner/perdre des données;
- confondre recette et avis de lieu créerait un second contrat incohérent;
- charger vidéo/galerie immédiatement dégraderait réseau et mémoire;
- recalculer des quantités textuelles afficherait des valeurs fausses;
- remplacer le formulaire de contribution casserait modération et propriété;
- modifier `CatalogListScreen`, les interactions ou composants Feed globalement pourrait impacter d'autres parcours;
- masquer le contenu générique quand `recipeDetails` existe reproduirait le défaut actuel;
- une barre flottante absolue peut recouvrir les dernières sections : test appareil requis.

Contrats de non-régression : routes Expo existantes, ID UUID transmis, endpoints publics Culture, visibilité/publication/sensibilité, traductions vérifiées, workflow contribution, cibles génériques `CULTURE_CONTENT`, route Gateway et anciens payloads V4.

## 31. Questions bloquantes avant Prompt 2

1. Les médias recette doivent-ils être exclusivement hébergés par Yeyamo, ou YouTube/externe est-il autorisé ?
2. Le mode guidé doit-il survivre à la fermeture de l'application et envoyer une notification de fin de minuteur ?
3. Les portions doivent-elles recalculer seulement les nouvelles quantités normalisées, en laissant les quantités historiques inchangées ?
4. Les commentaires génériques suffisent-ils, ou le produit souhaite-t-il des avis culinaires notés distincts (ce qui exige un contrat métier explicite) ?

Ces questions n'empêchent pas la Phase A additive, mais conditionnent vidéo, timers, scaling et notation.

## Statut final

```text
AUDIT_MODE = READ_ONLY
APPLICATION_FILES_MODIFIED = NO
BACKEND_FILES_MODIFIED = NO
DATABASE_MODIFIED = NO
RECIPE_DETAIL_ROUTE_FOUND = YES
RECIPE_DETAIL_API_FOUND = YES
RECIPE_DETAILS_MODEL = PARTIAL
STRUCTURED_INGREDIENTS = PARTIAL
STRUCTURED_STEPS = PARTIAL
PREPARATION_TIME = YES
COOKING_TIME = NO
STEP_DURATION = NO
RECIPE_VIDEO_SUPPORT = PARTIAL
RECIPE_MEDIA_SUPPORT = PARTIAL
PORTION_SCALING_FEASIBLE = PARTIAL
GUIDED_COOKING_FEASIBLE = PARTIAL
STEP_TIMER_FEASIBLE = PARTIAL
RECIPE_EDITOR_FOUND = YES
RECIPE_EDITOR_EXTENSION_REQUIRED = YES
REUSABLE_COMPONENTS_IDENTIFIED = YES
BACKEND_EXTENSION_REQUIRED = YES
DATABASE_MIGRATION_REQUIRED = YES
NATIVE_DEPENDENCY_REQUIRED = NO
NON_REGRESSION_CONTRACTS_IDENTIFIED = YES
AUDIT_REPORT_READY = YES
READY_FOR_REFACTOR_PROMPT = YES
```

