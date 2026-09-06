# Rapport Lot 03 — Zéro mock dans le parcours réel

Date : 4 septembre 2026  
Périmètre : Explorer mobile, Culture, Artworks, Artisans, Experiences, Discovery et Recommendations. Vérification statique ; Docker non démarré.

## Résultat

| Indicateur | Résultat |
|---|---:|
| Modules de données demo conservés dans le périmètre | 6 |
| Mocks techniques/UI acceptables | 3 |
| Mocks métier accessibles en `sessionMode=backend` | **0** |
| Fallbacks métier fictifs accessibles en `sessionMode=backend` | **0** |

## CERTIFICATION PARCOURS RÉEL

```text
Existe-t-il encore une donnée métier mockée accessible depuis sessionMode backend ?

NON
```

Cette certification couvre les parcours Explorer inspectés. Les modules demo restent présents dans le code et ne sont accessibles que lorsque `sessionMode` commence par `demo-`.

## Corrections apportées

### Experiences

- Avant : le parcours backend retournait systématiquement `[]` et n'avait donc aucune source réelle.
- Après : `app/(explore)/experiences.tsx` utilise `useDiscoverySearch('', 'EXPERIENCE')` en session backend. Discovery est la source de listing réelle et renvoie les types, titres et descriptions réellement indexés.
- En `demo-*`, `mockExperiences` est conservé et rendu avec la carte enrichie demo.
- Les notes, prix, images, lieux et favoris ne sont pas fabriqués depuis la réponse Discovery : la carte backend n'affiche que les champs fournis.

### Recettes et proverbes

- Avant : les listes, rails Culture et détails importaient directement `demoRecipes` et `demoProverbs`, indépendamment du mode de session.
- Après en backend :
  - listes : `GET /api/v1/culture/contents?type=RECIPE` ou `type=PROVERB` ;
  - détails : `GET /api/v1/culture/contents/{id}` et `/translations`.
- Le contenu publié n'invente ni image, ni durée, ni ingrédients, ni étapes. La description/traduction réellement fournie est affichée ; les ingrédients et étapes ne sont rendus que dans le parcours demo, car le DTO Culture actuel ne les structure pas.
- Les catégories backend sont dérivées de `CultureContent.type`; les catégories éditoriales restent demo-only.

### Catégories artistiques

- Avant : `demoArtCategories` était consommé par les écrans de catégories même en session backend.
- Après : les catégories backend sont dérivées des `category` effectivement retournés par `useArtworks`. Le catalogue demo n'est lu que sous `demo-*`.
- Il n'y a plus de fusion implicite entre la taxonomie éditoriale locale et les catégories de données réelles.

### Isolation déjà conforme, vérifiée

| Fichier / données | Classification | Condition d'isolation |
|---|---|---|
| `features/explore/mockData.ts` (`regions`, `categories`, `trendingPlaces`) | DEMO ONLY | `useExplore`: branche `isDemo ? Promise.resolve(...) : exploreApi...`; clés React Query séparées `demo`/`backend`. |
| `features/experiences/mockData.ts` (`mockExperiences`) | DEMO ONLY | `ExperiencesListScreen`: rendu uniquement dans le retour anticipé `isDemo`. |
| `features/culture/culturalCatalog.demo.ts` (`demoRecipes`, `demoProverbs`, `demoArtCategories`) | DEMO ONLY | écrans Culture/Recipes/Proverbs/Art categories : sélection explicite `sessionMode.startsWith('demo-')`. |
| `features/culture/culture.demo.ts` | DEMO ONLY | `culture.hooks.ts`: chaque query/mutation choisit la branche demo conditionnelle. Les queries Contents et Content utilisées ici séparent explicitement les clés `demo` et `backend`. |
| `features/artworks/artworks.demo.ts` | DEMO ONLY | `artworks.hooks.ts`: branche `demo ? ... : artworksApi...`. |
| `features/artisans/artisans.demo.ts` | DEMO ONLY | `artisans.hooks.ts`: branche `demo ? ... : artisansApi...`. |
| `HERO_FALLBACK_COLORS`, icônes, surfaces neutres | SHARED TECHNICAL | pure décoration/placeholder UI ; ne représentent ni ressource, ni lieu, ni utilisateur, ni note métier. |

## Fallbacks supprimés ou neutralisés

- Les écrans Recipe/Proverb backend n'utilisent plus le dataset de démonstration pour compléter une réponse API vide ou en erreur.
- Les cartes Catalog acceptent désormais une image absente. Aucun média d'une autre ressource n'est réutilisé.
- Les contenus sans champ métier affichable utilisent uniquement `Non renseigné` pour signaler explicitement l'absence de donnée.
- Les résultats API vides restent vides; ils ne basculent pas vers un tableau demo.

## Fichiers modifiés

- `yeyamo-mobile/src/app/(explore)/experiences.tsx`
- `yeyamo-mobile/src/app/(explore)/culture.tsx`
- `yeyamo-mobile/src/app/(explore)/recipes.tsx`
- `yeyamo-mobile/src/app/(explore)/recipes/[id].tsx`
- `yeyamo-mobile/src/app/(explore)/proverbs.tsx`
- `yeyamo-mobile/src/app/(explore)/proverbs/[id].tsx`
- `yeyamo-mobile/src/app/(explore)/art-categories.tsx`
- `yeyamo-mobile/src/app/(explore)/art-categories/[slug].tsx`
- `yeyamo-mobile/src/features/culture/components/CatalogListScreen.tsx`
- `yeyamo-mobile/src/features/culture/culture.hooks.ts`
- `yeyamo-mobile/src/components/experiences/ExperienceCard.tsx`

## Validation

```powershell
cd yeyamo-mobile
.\node_modules\.bin\tsc.cmd --noEmit
```

Résultat : succès, aucune erreur TypeScript.

## Limites réelles documentées

1. Le DTO Culture liste les métadonnées et les traductions, mais ne publie pas une structure d'ingrédients/étapes/médias pour une recette. L'interface backend affiche donc le contenu textuel disponible, sans compléter depuis demo.
2. Discovery fournit le listing Experiences; le détail mobile Experience et le détail Catalog Asset restent à concevoir comme un contrat distinct.
3. La catégorisation artistique backend repose sur les valeurs `category` effectivement remontées par Artwork. Si une taxonomie éditoriale enrichie est souhaitée, elle doit être publiée par une API, pas fusionnée localement.

Le lot suivant n'a pas été commencé.
