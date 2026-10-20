# YEYAMO — Rapport d'implémentation frontend Cuisine

## 1. Résumé

La refonte compatible avec le contrat actuel est implémentée dans `E:\Daryl\yeyamo-mobile`. Elle sépare strictement les fixtures locales des réponses réelles, remplace l'appel backend des pays de Cuisine par un catalogue ISO local, enrichit la fiche, ajoute checklist, mode guidé et minuteurs de session, et corrige l'éditeur pour transmettre réellement préparation et portions. Aucun backend n'a été modifié.

## 2. État Git initial

- Branche mobile : `daryl`.
- HEAD initial : `d287554 avancé yeyamo`.
- Arbre mobile propre au début de cette passe.
- Les modifications préexistantes du monorepo backend/Auth et de `cloud-conf-yeyamo` ont été préservées.

## 3. Fichiers modifiés

- `src/app/(explore)/recipes.tsx`
- `src/app/(explore)/recipes/country/[code].tsx`
- `src/app/(explore)/recipes/[id].tsx`
- `src/app/(create)/culture-contribution.tsx`
- `src/features/culture/culture.types.ts`
- `src/app/(explore)/languages.tsx` : correctif de typage minimal requis par le contrôle TypeScript, sans changement fonctionnel.

Fichiers créés :

- `src/features/country/recipe-country-catalog.ts`
- `src/features/culture/recipe.types.ts`
- `src/features/culture/recipe.demo.ts`
- `src/features/culture/components/RecipeGuidedCooking.tsx`
- `src/features/culture/recipe.test.mts`

## 4. Composants réutilisés

`SafeScreen`, `Icon`, `Button`, `CultureCategoryCard`, `CatalogListScreen`, `YeyamoFormScreen`, `YeyamoFormStep`, `YeyamoFormProgress`, React Query, interactions génériques `CULTURE_CONTENT`, `expo-image` et partage natif.

## 5. Composants créés

- `RecipeGuidedCooking` : modal pas-à-pas, progression, précédent/suivant, état terminé et minuteur local lorsque la durée existe.
- Modèle adaptateur `RichRecipeDetails` : extension TypeScript optionnelle, sans envoyer de propriétés inconnues au backend.

## 6. Changements UX

- Hero image conditionnel ou placeholder explicite.
- Titre, pays/région/communauté, résumé et histoire.
- Badges préparation, cuisson, repos, total, difficulté et portions selon disponibilité.
- Actions favori, partage et mode pas-à-pas.
- Sections conditionnelles : ingrédients, matériel, préparation préalable, étapes, conseils, dressage, conservation et précautions.
- Erreur réseau réelle avec retry; absence de recette distincte.
- Aucun champ réel absent n'est inventé.

## 7. Catalogue local des pays

`recipe-country-catalog.ts` contient les codes ISO 3166-1 alpha-2, génère les drapeaux localement et résout les noms français avec fallback local. L'écran Cuisine offre une recherche locale sur nom/code et n'appelle plus Country Config. Les autres parcours géographiques sont inchangés.

## 8. Fixtures démo

`recipe.demo.ts` contient des IDs `demo-recipe-*` et couvre : recette riche, sans vidéo, sans image, minimale, nombreuses étapes et quantité ambiguë. Les données sont étiquetées « contenu fictif local ». Les interactions démo ne sont pas envoyées au backend; le favori est local à la session.

## 9. Source des données réelles

Les comptes réels conservent `useCultureContents({type:'RECIPE', countryCode})` et `useCultureContent(id)`, donc les GET Culture existants et les UUID. Une erreur backend reste une erreur : aucun fallback fixture.

## 10. Fiche recette

La fiche rend les champs enrichis uniquement s'ils existent. Les recettes historiques `instruction` seule et ingrédients minimaux restent compatibles. Les nouveaux champs TypeScript sont optionnels jusqu'à évolution du serveur.

## 11. Mode guidé

Fonctionnel sur les fixtures et sur toute réponse contenant des étapes. Il utilise la même liste `recipeDetails.steps`. Les étapes sans ID ont une progression de session seulement; aucune restauration risquée n'est tentée.

## 12. Minuteurs

Un minuteur par étape apparaît seulement avec `durationMinutes`. Pause, reprise automatique du décompte et réinitialisation sont locales. Aucune notification native, aucune persistance en arrière-plan et aucune dépendance ajoutée.

## 13. Éditeur

Le formulaire unique est conservé. `prepTimeMinutes` et `servings` sont maintenant saisissables, validés comme entiers positifs, affichés au récapitulatif et envoyés au DTO actuel. Les champs non supportés sont signalés comme en attente et ne sont jamais envoyés.

## 14. Interactions

Favoris réels : hooks génériques `CULTURE_CONTENT`. Démo : état local. Partage : API native. Commentaires et signalement ne sont pas intégrés à cette passe afin de ne pas fabriquer une UX incomplète; leurs contrats existants restent réutilisables.

## 15. Cache et isolation

Les hooks Culture incluent déjà `demo` ou `backend`, l'ID et les filtres dans leurs clés. Le pays fait partie du filtre de liste. Les interactions sont désactivées côté service pour la démo. Les contenus publics réels ne dépendent pas de l'identité utilisateur; les interactions restent gérées par leur cache existant et le bootstrap global de changement de compte.

## 16. Tests

- `npx tsc --noEmit` : PASS.
- `node --experimental-strip-types src/features/culture/recipe.test.mts` : PASS.
- `npm run lint` : PASS avec 0 erreur; 1 avertissement préexistant hors Cuisine dans `partner.api.ts` après correction des avertissements introduits.
- `git diff --check` : PASS hors avertissements de conversion LF/CRLF Windows.
- Appareil physique iOS/Android : NOT_RUN.
- Navigation/runtime API : à retester manuellement sur appareil.

## 17. Limitations

- Les images de fixture utilisent des URLs publiques explicitement locales au jeu de démonstration, pas des mediaId backend.
- L'ajustement visuel des portions n'est pas exposé dans cette passe; la logique sûre est prête et testée, mais les quantités backend restent textuelles.
- La vidéo réelle, la galerie, les commentaires intégrés et la persistance du mode guidé attendent les décisions/contrats détaillés.
- La liste réelle demande toujours une page de taille 100 via le flux existant; pagination progressive à prévoir.

## 18. Fonctionnalités bloquées par le backend

`BLOCKED_BY_BACKEND` : média principal/vidéo/galerie par IDs, cuisson/repos/difficulté réelle, durées et IDs stables d'étape, matériel, préparation préalable, conseils/sécurité/conservation structurés, quantités numériques fiables et restauration durable du mode guidé.

## 19. Risques de régression

- Les futurs champs Java doivent rester optionnels.
- Les étapes historiques sans ID ne doivent pas recevoir une progression persistée par index.
- Les quantités textuelles ne doivent pas être recalculées.
- Le correctif `languages.tsx` ne change que l'union de type de la liste.
- Le rendu sur petits écrans, VoiceOver/TalkBack et la barre flottante doivent être vérifiés sur appareil.

## 20. État Git final

Seuls les fichiers frontend listés ci-dessus sont modifiés/créés. Aucun commit, push, build natif ou déploiement n'a été effectué.

```text
FRONTEND_REFACTOR = PARTIAL
DEMO_DATA_SOURCE = LOCAL_FIXTURES
REAL_DATA_SOURCE = CULTURE_SERVICE
COUNTRY_CATALOG = LOCAL
COUNTRY_FLAGS = PASS
DEMO_REAL_ISOLATION = PASS
RECIPE_DETAIL_UX = PASS
RECIPE_MEDIA = BLOCKED_BY_BACKEND
RECIPE_VIDEO = BLOCKED_BY_BACKEND
INGREDIENT_CHECKLIST = PASS
PORTION_SCALING = PARTIAL
GUIDED_COOKING = PASS
STEP_TIMERS = PARTIAL
RECIPE_EDITOR = PASS
REAL_RECIPE_BACKEND_CONTRACT = PRESERVED
BACKEND_CHANGES_APPLIED = NO
BACKEND_PROPOSAL_READY = YES
DATABASE_MIGRATION_APPLIED = NO
EXISTING_GIT_CHANGES_PRESERVED = YES
REGRESSION_TESTS = PARTIAL
PHYSICAL_DEVICE_TESTS = NOT_RUN
NO_DEPLOYMENT = YES
```

