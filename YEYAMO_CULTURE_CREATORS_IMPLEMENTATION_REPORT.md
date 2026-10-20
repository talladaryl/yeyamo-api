# YEYAMO — Culture & Créateurs — Rapport d'implémentation

Date : 2026-10-13

## 1. Résumé des modifications

La passe a été réalisée de manière additive dans `yeyamo-mobile`. Aucun backend, endpoint, schéma ou contrat public existant n'a été renommé.

- Normalisation explicite des DTO publics artisans et œuvres à la frontière API.
- Variante `grid` ajoutée aux cartes existantes sans changer leur variante `rail` par défaut.
- Profil artiste et artisans suivis convertis en grilles verticales de deux colonnes.
- Catalogue Culture & Créateurs existant étendu à quatre catégories.
- Filtre par spécialité backend ajouté à la découverte des créateurs.
- Accès direct aux traditions ajouté dans « Langues & Culture ».
- N+1 des artisans suivis rendu tolérant aux profils indisponibles.
- Navigation hiérarchique ajoutée : Cuisine → pays → recettes, Créateurs → discipline → profils, Proverbes → catégorie/langue → contenus.
- Toutes les pages intermédiaires et les listes de langues utilisent désormais des grilles de deux colonnes.

## 2. Causes racines traitées

- Profil artiste : `FlatList` mono-colonne et `ArtworkCard` à largeur fixe `w-64`.
- Artisans suivis : `FlatList horizontal` et `ArtisanCard` à largeur fixe.
- Culture : écran `culture.tsx` codé pour ne rendre que `PROVERB` et `RECIPE`.
- Contrats : backend `id`/`artisanId` et `id`/`verified`, mobile `assetId`/`artisanPartnerId` et `partnerId`/`verificationStatus`.

## 3. Composants réutilisés

- `ArtworkCard`
- `ArtisanCard`
- `LanguageCard`
- `SafeScreen`
- `Icon`
- Hooks React Query culture, artisans et œuvres existants

## 4. Composants adaptés

`ArtworkCard` et `ArtisanCard` acceptent désormais `variant="rail" | "grid"`. La valeur par défaut est `rail`, ce qui préserve les rails Explorer. En grille, la largeur appartient au parent, les images conservent un ratio stable et les textes essentiels sont bornés.

## 5. Nouveaux composants réellement nécessaires

Aucun nouveau composant visuel parallèle. Seul un mapper de frontière artisan a été ajouté. Le mapper œuvres existant a été étendu.

## 6. Modifications des grilles

`src/app/(explore)/artisans/[id].tsx` utilise maintenant `numColumns={2}`, `columnWrapperStyle` et `ArtworkCard variant="grid"`. Le header, les chips de catégories, le footer de contact, la virtualisation et la navigation vers le détail sont conservés.

## 7. Artisans suivis

`src/app/(profile)/followed-artisans.tsx` utilise une grille verticale à deux colonnes et `ArtisanCard variant="grid"`. Les états loading/error/empty et la navigation vers le profil sont conservés.

`useFollowedArtisans` utilise `Promise.allSettled` : un profil supprimé ou devenu indisponible ne fait plus échouer toute la liste. Le batch backend n'a pas été ajouté afin d'éviter une extension disproportionnée; le nombre initial d'appels reste N+1.

## 8. Catalogue des quatre catégories

`src/app/(explore)/culture.tsx` présente maintenant deux cartes par ligne :

1. Proverbes → `/(explore)/proverbs`
2. Cuisine → `/(explore)/recipes`
3. Artistes & Créateurs → `/(explore)/artisans`
4. Langues & Culture → `/(explore)/languages`

Le bouton « Voir tout » de l'Explorer principal continue d'ouvrir ce même écran. Les catégories restent accessibles même si leur contenu est vide.

## 9. Images de catégories

Aucune image canonique validée n'existe. Les cartes utilisent donc un placeholder neutre thématique basé sur les icônes et couleurs du thème. Aucune image de démonstration ni image sans rapport n'a été introduite. Le statut reste partiel jusqu'à la disponibilité d'un média de catégorie configuré.

## 10. Filtres par domaine

- Proverbes : filtres existants conservés; la taxonomie thème/peuple n'a pas été inventée.
- Cuisine : filtres existants conservés; aucun type de plat fictif ajouté.
- Artistes : filtre `specialtyId` réellement supporté ajouté, alimenté par `/artisan-specialties`.
- Langues & Culture : langues existantes conservées; accès aux contenus `TRADITION` ajouté.

Les filtres horizontaux restent uniquement dans les écrans finaux de résultats. Ils ne remplacent plus la sélection initiale en cartes illustrées.

Routes additives créées sans conflit avec les détails existants :

- `/(explore)/recipes/country/[code]`
- `/(explore)/artisans/specialty/[id]`
- `/(explore)/proverbs/category/[slug]`

## 11. DTO et mappers

- Nouveau `src/features/artisans/artisans.mappers.ts` : `id → partnerId`, booléen `verified → verificationStatus`, sans fabriquer de média ou de date absente.
- `src/features/artworks/artworks.mappers.ts` : `id → assetId`, `artisanId → artisanPartnerId`, adaptation des médias publics `id/type/contentUrl` et construction du détail mobile.
- Les champs privés déjà alignés (`partnerId`, `verificationStatus`) restent acceptés.
- Un identifiant canonique absent provoque une erreur explicite au lieu de fabriquer une valeur.

## 12. Endpoints existants réutilisés

- `GET /api/v1/artisans`
- `GET /api/v1/artisans/{id}/profile`
- `GET /api/v1/artisan-specialties`
- `GET /api/v1/artisans/{id}/artworks`
- `GET /api/v1/artworks`
- `GET /api/v1/artworks/{id}`
- `GET /api/v1/culture/contents`
- `GET /api/v1/culture/languages`

## 13. Endpoints étendus

Aucun.

## 14. Modifications backend

Aucune. Les routes et filtres nécessaires à cette passe existaient déjà.

## 15. Migrations

Aucune migration créée. Les taxonomies types de plats/thèmes ne sont pas implémentées sans décision produit et référentiel canonique.

## 16. Pagination

La pagination Spring existante est préservée et les filtres restent dans les query keys. Les écrans concernés utilisent encore une première page bornée; aucune fausse pagination infinie n'a été déclarée. Une passe ultérieure pourra employer `useInfiniteQuery` sans modifier les contrats.

## 17. Identifiants canoniques

- Profil créateur : UUID partenaire.
- Œuvre : UUID asset catalogue.
- Contenu culturel : UUID culture.
- Aucun AUTH_ID n'est converti en identifiant partenaire ou œuvre.

## 18. Gestion des médias

Les médias de détail œuvre utilisent `PublicMedia.contentUrl` et `absoluteApiUrl`. Aucun appel média par carte n'a été ajouté. Les résumés d'œuvre backend ne contenant pas d'image, la carte affiche son placeholder existant en liste et l'image lorsque le détail/mode démo la fournit.

## 19. Tests exécutés

| Test | Résultat |
|---|---|
| `npx tsc --noEmit` | PASS |
| ESLint ciblé sur tous les fichiers modifiés | PASS, 0 erreur et 0 avertissement après correction |
| `npm run lint` | WARNING : 0 erreur, 1 avertissement préexistant hors périmètre dans `src/features/partner-dashboard/partner.api.ts:38` |
| `node --experimental-strip-types src/features/artisans/artisans.mappers.test.mts` | PASS |
| `node --experimental-strip-types src/features/artworks/artworks.contract.test.mts` | PASS |
| `git diff --check` mobile | PASS; seulement avertissements de normalisation LF/CRLF |
| Tests backend | NOT_REQUIRED : backend non modifié |
| Test physique iPhone/Android | NON EXÉCUTÉ |

## 20. Régressions détectées et corrigées

Pendant l'implémentation, la forme réelle des médias backend a été revérifiée (`id`, `type`, `contentUrl`) et le mapper a été corrigé avant validation. Les variantes rail restent le comportement par défaut afin de ne pas modifier les rails Explorer.

## 21. Limitations restantes

- N+1 des artisans suivis seulement mitigé, pas supprimé.
- Pagination mobile encore limitée à une page bornée.
- Pas d'images canoniques de catégories.
- Pas de taxonomie persistée pour types de plats, thèmes de proverbes ou disciplines d'œuvres.
- Les quatre grilles doivent être contrôlées sur appareil réel et avec grande taille de texte.

## 22. Services à redéployer

Aucun service backend. Pour publier la modification, seul le bundle mobile doit être livré par le processus habituel; aucune dépendance native n'a changé.

## 23. Plan de retest iPhone/Android

1. Explorer → Culture & Créateurs : vérifier centrage, deux colonnes et quatre routes.
2. Tester chacune des quatre catégories avec catalogue rempli et vide.
3. Artistes & Créateurs : sélectionner plusieurs spécialités et vérifier les résultats serveur.
4. Profil d'un artiste : filtrer puis ouvrir chaque œuvre dans la grille.
5. Artisans suivis : vérifier 0, 1, 2 et plusieurs profils, y compris un profil indisponible.
6. Vérifier light/dark, petits écrans, grande police et retour arrière.
7. Vérifier que les rails Explorer sont inchangés.
8. Vérifier favoris/commentaires/détail œuvre après normalisation des UUID.

ARTWORK_TWO_COLUMN_GRID = RUNTIME_RETEST_REQUIRED

FOLLOWED_ARTISANS_GRID = RUNTIME_RETEST_REQUIRED

ARTISAN_CARD_REUSED = YES

ARTWORK_CARD_REUSED = YES

EXPLORER_RAILS_PRESERVED = PASS

CULTURE_FOUR_CATEGORIES = PASS

CULTURE_CATEGORY_IMAGES = PARTIAL

PROVERBS_FILTERS = PARTIAL

CUISINE_FILTERS = PARTIAL

ARTISTS_SPECIALTY_FILTERS = PASS

LANGUAGES_CULTURE_FILTERS = PARTIAL

DTO_CONTRACT_MAPPING = PASS

FOLLOWED_ARTISANS_N_PLUS_ONE = MITIGATED

PAGINATION = PARTIAL

AUTHORIZATION_PRESERVED = PASS

BACKWARD_API_COMPATIBILITY = PASS

DUPLICATE_DOMAIN_CREATED = NO

DUPLICATE_COMPONENTS_CREATED = NO

BACKEND_MODIFIED = NO

BACKEND_SERVICES_MODIFIED = NONE

DATABASE_MIGRATION_REQUIRED = NO

MIGRATION_FILES = NONE

CONFIG_CHANGE_REQUIRED = NO

NEW_NATIVE_DEPENDENCY = NO

MOBILE_NATIVE_REBUILD_REQUIRED = NO

MOBILE_TYPESCRIPT = PASS

MOBILE_LINT = WARNING

BACKEND_TARGETED_TESTS = NOT_REQUIRED

GIT_DIFF_CHECK = PASS

P0_BLOCKERS = NONE

P1_BLOCKERS = NONE

STATIC_IMPLEMENTATION_READY = YES

RUNTIME_RETEST_REQUIRED = YES

READY_FOR_DEVICE_RETEST = YES
