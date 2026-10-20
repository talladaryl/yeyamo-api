# YEYAMO — Audit Culture & Créateurs avant refonte

Date de l'audit statique : 2026-10-13  
Mode : lecture seule. Aucun fichier applicatif, endpoint, DTO, schéma ou jeu de données n'a été modifié.

## 1. Résumé exécutif

La section est déjà découpée en trois domaines exploitables : contenus culturels (`culture-service`), œuvres (`catalog-service`) et artisans/artistes (`partner-service`). Le frontend possède les routes, hooks et cartes de base. Une nouvelle architecture parallèle n'est donc pas justifiée.

Les trois causes principales sont prouvées :

- la grille du profil artiste est une `FlatList` verticale sans `numColumns`; chaque ligne enveloppe une `ArtworkCard` de largeur fixe `w-64`, ce qui produit une seule œuvre alignée à gauche (`yeyamo-mobile/src/app/(explore)/artisans/[id].tsx:39-42`, `src/features/artworks/components/ArtworkCard.tsx:7`);
- « Artisans suivis » est explicitement une `FlatList horizontal` et réemploie une carte fixe `w-64`; ce n'est pas une grille deux colonnes (`src/app/(profile)/followed-artisans.tsx:22`, `src/features/artisans/components/ArtisanCard.tsx:2`);
- « Voir tout » de la rail « Culture et créateurs » ouvre `/(explore)/culture`, dont le code rend exclusivement deux sections codées en dur : `PROVERB` et `RECIPE`. Artistes, œuvres, langues et traditions ont pourtant déjà des routes séparées (`src/app/(tabs)/explore.tsx:279-286`, `src/app/(explore)/culture.tsx:2`).

Un blocage transversal doit être traité avant ou avec la refonte : les noms de propriétés des DTO publics artisans/œuvres ne correspondent pas aux types mobiles et aucun mapper n'est appelé par `artisans.api.ts`/`artworks.api.ts`. Exemples : backend `PublicResponse.id`, `verified` contre mobile `partnerId`, `verificationStatus`; backend `PublicSummary.id`, `artisanId` contre mobile `assetId`, `artisanPartnerId` (`partner-service/.../ArtisanDtos.java:3`, `catalog-service/.../ArtworkDtos.java:18`, `yeyamo-mobile/src/features/artisans/artisans.types.ts`, `src/features/artworks/artworks.types.ts`).

## 2. Périmètre réellement inspecté

- Frontend : `E:/Daryl/yeyamo-mobile`, branche `daryl`, commit `cc98b7e`.
- Backend : `E:/Daryl/yeyamo-api`, services `culture-service`, `catalog-service`, `partner-service`, `graph-service`, `interaction-service` et `api-gateway`.
- Configuration gateway : `E:/Daryl/yeyamo-api/cloud-conf-yeyamo/api-gateway.properties`.
- L'état Git backend était déjà modifié dans `auth-service` et le sous-module `cloud-conf-yeyamo`; ces changements sans rapport ont été laissés intacts.
- Aucun service, build, test runtime ou déploiement n'a été lancé, conformément au prompt.

## 3. Inventaire des écrans

| Écran | Chemin | Responsabilité / données | Enfants / API | Statut |
|---|---|---|---|---|
| Explorer | `src/app/(tabs)/explore.tsx` | Agrège discovery, culture, œuvres, artisans par pays | `DiscoveryRail`, `CultureContentCard`, `ArtworkCard`, `ArtisanCard`; discovery + `/culture/contents`, `/artworks`, `/artisans` | PARTIAL |
| Catalogue culturel | `src/app/(explore)/culture.tsx` | Aperçu proverbes et recettes, 6 éléments | `Section`, `Rail`; deux `useCultureContents` | PARTIAL |
| Proverbes | `src/app/(explore)/proverbs.tsx` | Liste/recherche/filtre local | `CatalogListScreen`; `type=PROVERB,size=100` | PARTIAL |
| Recettes | `src/app/(explore)/recipes.tsx` | Liste/recherche/filtre local | `CatalogListScreen`; `type=RECIPE,size=100` | PARTIAL |
| Traditions | `src/app/(explore)/traditions.tsx` | Liste de type `TRADITION` | `CultureListScreen` | WORKING |
| Langues | `src/app/(explore)/languages.tsx` | Langues publiées et accès aux détails | `LanguageCard`; `/culture/languages` | WORKING |
| Artisans | `src/app/(explore)/artisans.tsx` | Liste publique d'artisans | `ArtisanCard`; `/artisans` | PARTIAL |
| Profil artisan/artiste | `src/app/(explore)/artisans/[id].tsx` | Profil, filtres d'œuvres, contact | `ArtworkCard`; `/artisans/{id}/profile`, `/artisans/{id}/artworks` | PARTIAL |
| Œuvres | `src/app/(explore)/artworks.tsx` | Liste publique | `ArtworkCard`; `/artworks` | PARTIAL |
| Détail œuvre | `src/app/(explore)/artworks/[id].tsx` | Média, récit, favoris, commentaires, offre | `/artworks/{id}`, history, offers, interactions | PARTIAL |
| Artisans suivis | `src/app/(profile)/followed-artisans.tsx` | Résout les FOLLOW puis charge chaque profil | `ArtisanCard`; interactions + N appels profils | PARTIAL |
| Catégories artistiques | `src/app/(explore)/art-categories.tsx` | Catégories dérivées localement des œuvres | `/artworks?size=100` | PARTIAL |
| Détail catégorie artistique | `src/app/(explore)/art-categories/[slug].tsx` | Filtre local des œuvres | `/artworks?size=40` | PARTIAL |

## 4. Inventaire des composants réutilisables

| Composant | Chemin | Props principales | Réutilisation / limite | Statut |
|---|---|---|---|---|
| `ArtisanCard` | `src/features/artisans/components/ArtisanCard.tsx` | `artisan`, `onPress` | Déjà Explorer, liste artisans, suivis; largeur `w-64` imposée, aucune variante grille | PARTIAL |
| `ArtworkCard` | `src/features/artworks/components/ArtworkCard.tsx` | `artwork`, `offer?`, `onPress` | Déjà listes/profil; largeur `w-64` imposée | PARTIAL |
| `CultureContentCard` | `src/features/culture/components/CultureContentCard.tsx` | contenu + navigation | Réutilisable pour listes culturelles | WORKING |
| `CatalogListScreen` | `src/features/culture/components/CatalogListScreen.tsx` | titre, items, catégories, base détail | Recherche et chips locaux; liste une colonne, image URI supposée | PARTIAL |
| `CultureListScreen` | `src/features/culture/components/CultureListScreen.tsx` | titre, type | Loading/empty; pas d'état erreur ni pagination | PARTIAL |
| `LanguageCard` | `src/features/culture/components/LanguageCard.tsx` | langue + navigation | Convient au domaine langues | WORKING |
| `CategoryCard` | `src/components/explore/CategoryCard.tsx` | catégorie Explorer | Candidat pour cartes illustrées après vérification visuelle; contrat différent d'une catégorie culturelle | PARTIAL |
| `SafeScreen` | `src/components/ui/SafeScreen.tsx` | conteneur | Déjà utilisé sur tous les écrans ciblés | REUSE_AS_IS |

Conclusion anti-duplication : adapter `ArtisanCard` et `ArtworkCard` avec un mode de largeur piloté par le parent; composer le catalogue avec les écrans/hooks existants. Ne créer une carte de catégorie culturelle que si `CategoryCard` ne peut recevoir proprement titre, image et route.

## 5. Inventaire des hooks

- `useCultureContents`, `useCultureContent`, `useCultureLanguages`, `useLanguage`, `useLanguageLessons`, `useCultureGraphRelations` dans `src/features/culture/culture.hooks.ts`.
- `useArtisans`, `useArtisan`, `useArtisanSpecialties`, `useArtisanArtworks`, `useFollowedArtisans` dans `src/features/artisans/artisans.hooks.ts`.
- `useArtworks`, `useArtwork`, `useArtworkHistory`, `useRelatedArtworks`, `useSavedArtworks` dans `src/features/artworks/artworks.hooks.ts`.
- `useFollowedArtisans` charge d'abord jusqu'à 100 interactions `ARTISAN/FOLLOW`, puis fait un `Promise.all` d'un profil par cible : risque N+1 et échec global si un seul profil échoue.
- Les listes culturelles ne sont pas infinies : tailles fixes 30 ou 100; `hasNextPage` et `fetchNextPage` sont absents.

## 6. Inventaire des services API

`src/features/culture/culture.api.ts` couvre contenus, traductions, catégories, langues, leçons, progrès, challenges, contributions et graphe. `src/features/artisans/artisans.api.ts` couvre recherche, profil, spécialités et profil propre. `src/features/artworks/artworks.api.ts` couvre listes/détails/médias/historique/relations/création.

Faits importants :

- `cultureApi.categories()` existe mais n'est pas utilisée par `culture.tsx`;
- le filtre `CultureFilters` expose type, pays, région administrative, ville, langue, communauté, vérification, recherche et pagination;
- le mobile déclare `verified` dans `ArtisanFilters`, mais `ArtisanController.search` ne possède pas ce paramètre; il force déjà uniquement les profils `VERIFIED`;
- les API artisans/œuvres retournent directement les DTO sans mapper de renommage, d'où l'écart contractuel précité.

## 7. Inventaire des routes Expo Router

Flux réels :

- `/(tabs)/explore` → `/(explore)/culture` via « Voir tout »;
- culture → `/proverbs`, `/recipes`; chaque item → `/proverbs/[id]` ou `/recipes/[id]`;
- Explorer expose séparément `/artisans`, `/artworks`, `/languages`, `/traditions`, `/art-categories`;
- artiste → `/artisans/[id]` → `/artworks/[id]`;
- profil → `/(profile)/followed-artisans` → `/artisans/[id]`.

Le retour utilise `router.back()`. Le détail culturel générique fabrique un deep link `yeyamo://explore/culture/{id}` (`src/app/(explore)/culture/[id].tsx:26`). Aucun deep link de catalogue quatre catégories n'est identifié.

## 8. Inventaire des routes backend/gateway

| Route gateway | Propriétaire / contrôleur | Auth | Filtres / pagination | Consommateur mobile | Compatibilité |
|---|---|---|---|---|---|
| `GET /api/v1/culture/contents` | culture / `PublicCultureController.contents` | public | type, country, area, city, language, community, verified, search, `Pageable` | `listContents` | EXISTING |
| `GET /culture/contents/{id}` + translations | culture | public | UUID culture | `getContent` | EXISTING |
| `GET /culture/categories` | culture | public | aucun; renvoie tous les `ContentType` | API mobile seulement | EXISTING_BUT_UNUSED |
| `GET /culture/languages...` | culture | public pour GET | code, pages contenu | hooks langues | EXISTING |
| `GET /artworks` | catalog / `ArtworkController.list` | public | country, artisanId, availability, search, pageable | `artworksApi.list` | PARTIAL: noms DTO |
| `GET /artisans/{id}/artworks` | catalog | public | UUID partenaire, pageable | profil artiste | PARTIAL: noms DTO |
| `GET /artisans` | partner / `ArtisanController.search` | public | country, area, city, specialtyId, custom, shipping, search, pageable | `artisansApi.list` | PARTIAL: noms DTO |
| `GET /artisans/{id}/profile` | partner | public | UUID partenaire | profil/suivis | PARTIAL: noms DTO |
| `GET /artisan-specialties` | partner | public | spécialités actives | `useArtisanSpecialties` | EXISTING |
| `GET /culture-graph/**` | graph / `CultureGraphController` | public selon gateway | relations, limit | détail culture | EXISTING |

Routage prouvé dans `cloud-conf-yeyamo/api-gateway.properties:252-295` : culture → `culture-service`, graphe → `graph-service`, œuvres → `catalog-service`, profils artisans → `partner-service`.

## 9. Diagnostic grille des œuvres

Cause racine : `ArtisanDetail` rend la collection dans une `FlatList` sans `numColumns`; `renderItem` ajoute `px-5` puis `ArtworkCard` impose `w-64`. Il n'existe ni calcul de largeur disponible, ni `columnWrapperStyle`, ni variante compacte. La liste reste correctement virtualisée et les filtres sont dans `ListHeaderComponent`.

Correction future minimale : `ADAPT_EXISTING`. Ajouter une variante grille à `ArtworkCard`, rendre le parent responsable de la largeur, passer la liste à deux colonnes avec espacement et préserver `keyExtractor=assetId`, filtres et navigation. Éviter une `ScrollView` verticale autour de la liste.

## 10. Diagnostic cartes Artisans suivis

Cause racine : `FlatList horizontal` + `ArtisanCard w-64`. La carte elle-même n'a ni `height:100%`, ni `flex:1`, ni hauteur fenêtre; l'impression de carte très haute vient de sa composition couverture `h-32` + avatar débordant + contenu et du fait qu'une rail horizontale occupe seule l'écran. Le besoin deux colonnes impose de changer la liste, puis de rendre la largeur de la carte adaptable. Les écrans `artisans.tsx` et Explorer réutilisent la même carte et seraient affectés par une suppression brute de `w-64`.

## 11. Diagnostic Culture & Créateurs

La rail Explorer agrège les résultats trending `CULTURE`, `ARTWORK`, `ARTISAN`, puis charge aussi des aperçus directs. Son bouton global route néanmoins vers `culture.tsx`. Cet écran ignore les artistes/œuvres/langues/traditions et construit explicitement deux requêtes et deux sections. La visibilité limitée à Proverbes/Cuisine est donc d'abord une décision frontend codée en dur, pas une absence globale de routes backend.

Le titre « Cuisine camerounaise » est lui aussi codé en dur et n'est pas calculé depuis `countryCode`. Les appels de cette page n'envoient aucun pays; ils peuvent donc retourner plusieurs pays tandis que le titre affirme Cameroun.

## 12. Diagnostic du bouton Voir plus

Source : `src/app/(tabs)/explore.tsx:286`. Handler : `router.push('/(explore)/culture')`. Aucun paramètre n'est transmis. Destination : `CultureExplore`. Retour : `router.back()`.

Choix futur recommandé : **C — extension mineure de l'écran existant**, en composant les quatre entrées vers les routes déjà présentes. Aucun nouvel écran n'est nécessaire pour le catalogue principal. Les pages spécifiques actuelles doivent rester les destinations.

## 13. État réel des Proverbes

- Modèle canonique : `CultureContent` de type `PROVERB` + `ProverbDetailsEntity`.
- Contrat : `/culture/contents?type=PROVERB`, détail et traductions.
- Champs structurés : traduction littérale, sens, langue d'origine, audio (`CultureDtos.java:5,10`).
- Filtres backend : langue primaire, pays/région/ville, communauté, recherche, vérifié. Pas de champ canonique peuple/thème.
- UI production : catégorie ramenée à `item.type`, donc tous les éléments ont « PROVERB »; les catégories riches n'existent que dans les données de démonstration.

## 14. État réel de la Cuisine

- Modèle canonique : `CultureContent` de type `RECIPE` + détails recette.
- Champs structurés : ingrédients, étapes, temps de préparation, portions (`CultureDtos.java:6-13`).
- Aucun type de plat/spécialité culinaire normalisé n'est présent dans ce contrat. Région et communauté reposent sur les champs généraux de localisation.
- UI production : filtre local unique « RECIPE »; les catégories culinaires visibles en démo ne sont pas un contrat production.

## 15. État réel des Artistes

- Modèle canonique profil : `ArtisanProfileEntity`, distinguant `BusinessType` `ARTISAN`/`ARTIST` à la création, mais la recherche publique ne filtre pas ce type.
- Sous-catégories réutilisables : spécialités actives normalisées par UUID via `/artisan-specialties`; filtre backend `specialtyId` existant.
- Modèle œuvre : `ArtworkEntity`/asset catalogue. Les filtres publics ne comprennent pas de discipline/catégorie; le champ `category?` mobile n'est pas dans `PublicSummary` backend.
- Abonnement : interaction générique cible `ARTISAN`, pas une collection propriétaire dédiée.

## 16. État réel des Langues & Culture

- Langues : modèle canonique et routes complets (liste, détail, contenu, leçons, progression).
- Traditions et patrimoine : `ContentType` contient `TRADITION`, `RITUAL`, `FESTIVAL`, `HISTORICAL_EVENT`, `HISTORICAL_FIGURE`, `DANCE`, `SONG`, etc. Ce sont des types de contenu, pas une taxonomie hiérarchique « Langues & Culture ».
- Peuples/groupes culturels : seulement `communityName` texte libre; aucune entité canonique observée dans le périmètre.

## 17. Taxonomie et sous-catégories existantes

| Domaine | État | Preuve |
|---|---|---|
| Types culturels | EXISTING_AND_USABLE mais plats | enum `ContentType`, `/culture/categories` |
| Hiérarchie quatre catégories | MISSING | aucun parent/enfant ni catégorie illustrée |
| Spécialités artisans | EXISTING_AND_USABLE | UUID + code + nom, `/artisan-specialties` |
| Disciplines œuvres | EXISTING_BUT_INCOMPLETE | `category?` seulement côté mobile, absent du DTO public backend |
| Types de plats | MISSING | `RecipeDetails` sans catégorie |
| Langues | EXISTING_AND_USABLE | code stable, pays, nom natif |
| Communautés/peuples | EXISTING_BUT_INCOMPLETE | `communityName` libre |
| Pays/régions/villes | EXISTING_AND_USABLE | champs structurés et filtres |

## 18. Filtres disponibles et manquants

Disponibles côté backend mais sous-exploités dans l'UI : pays, région, ville, langue, communauté, vérification pour culture; spécialité, commandes personnalisées, livraison, recherche pour artisans; disponibilité et recherche pour œuvres.

Manquants comme contrats canoniques : thème/peuple pour proverbes, type de plat, discipline d'œuvre, filtre `ARTIST` contre `ARTISAN`, regroupement principal quatre catégories. Les chips actuels Proverbes/Recettes filtrent uniquement les 100 éléments déjà chargés, pas le backend.

## 19. Inventaire des médias de catégories

- `CultureContentResponse` ne contient aucune image de couverture (`CultureDtos.java:14`).
- Les détails proverbe peuvent contenir un audio; les recettes n'ont pas de média.
- Les œuvres ont une table média et une URL de contenu, mais le résumé public backend ne fournit pas d'image.
- Les profils publics artisans n'exposent ni avatar ni couverture, alors que la carte mobile les attend optionnellement.
- Les images `demoProverbs`/`demoRecipes` sont des données de démonstration et ne doivent pas illustrer la production.
- Aucun asset local culturel représentatif validé n'a été trouvé dans l'inventaire ciblé. Prévoir d'abord une source backend/configurée; sinon un placeholder neutre du design system.

## 20. Contrats d'identité

- `CULTURE_CONTENT_ID` : UUID `CultureContent.id`, route `/culture/contents/{id}`.
- `ARTWORK_ID` : UUID asset catalogue. Backend l'appelle `id`, mobile `assetId`.
- `ARTIST_ID`/artisan public : UUID partenaire. Backend public l'appelle `id`, backend privé `partnerId`, mobile attend toujours `partnerId`.
- `AUTH_ID` : `Principal/Jwt.subject`, utilisé pour contributions/profil propre, jamais à substituer au partenaire UUID.
- `PROFILE_UUID` utilisateur n'est pas requis par les routes publiques auditées.

Aucune conversion implicite entre ces namespaces ne doit être ajoutée. Le correctif futur doit normaliser les noms à la frontière API.

## 21. Pagination et performances

- Backend : `Pageable` sur contenus, œuvres, artisans.
- Mobile : `useQuery`, pas `useInfiniteQuery`; tailles fixes et aucun chargement suivant.
- `CultureExplore` exécute deux requêtes parallèles; l'Explorer principal en exécute au moins six pour culture/œuvres/artisans + trends.
- `followed-artisans` a un N+1 et aucune tolérance à un profil supprimé/non vérifié.
- Les listes principales utilisent `FlatList`; le catalogue culturel utilise aussi des rails `ScrollView` horizontales. Aucune imbrication verticale problématique n'est présente sur le profil artiste.

## 22. Gestion des caches

Les clés React Query sont séparées par domaine et filtres (`culture.query-keys.ts`, `artisans.query-keys.ts`, `artworks.query-keys.ts`). Le mode démo est inclus explicitement dans certaines clés culturelles, mais les clés artisans/œuvres ne montrent pas cette séparation dans les appels inspectés : risque de collision de données après bascule démo/réelle à confirmer runtime. Les clés de listes incorporent les objets filtre, ce qui préserve les sélections par pays.

## 23. Matrice des écarts

| Fonctionnalité | Existant | Écart / cause | Réutilisation | Intervention | Priorité/risque |
|---|---|---|---|---|---|
| Œuvres deux colonnes | Liste + carte | liste 1 colonne, carte fixe | oui | adapter liste/carte | P1 / moyen |
| Artistes suivis | rail horizontal | pas de grille, N+1 | oui | adapter liste/carte puis requête | P1 / moyen |
| Culture Voir plus | écran existant | seulement 2 sections codées | oui | étendre composition | P1 / faible |
| Proverbes | modèle/route | filtres UI fictivement plats en prod | oui | brancher filtres réels | P2 / moyen |
| Cuisine | modèle recette | aucun type de plat canonique | partiel | UI sur filtres existants; backend si taxonomie validée | P2 / élevé |
| Artistes | profils/spécialités | type public et contrats DTO incomplets | oui | aligner frontière API | P1 / élevé |
| Langues & Culture | langues + types | pas de regroupement/taxonomie parent | oui | composition frontend | P2 / faible |
| Images catégories | médias dispersés | aucun média catégorie canonique | partiel | placeholder puis contrat validé | P2 / moyen |

## 24. Matrice de réutilisation des composants

| Besoin | Composant existant | Réutilisable ? | Adaptation nécessaire |
|---|---|---|---|
| Carte artiste | `ArtisanCard` | oui | largeur/variante compacte, média réellement fourni |
| Carte œuvre | `ArtworkCard` | oui | largeur fluide, média résumé mappé |
| Carte catégorie illustrée | `CategoryCard` | à confirmer visuellement | adapter titre/image/route sans spécialiser abusivement |
| Grille deux colonnes | patterns `FlatList numColumns=2` dans `art-categories.tsx` | oui | extraire seulement si répétition justifiée |
| Filtres horizontaux | chips dans `CatalogListScreen` et profil artisan | oui | alimenter depuis contrats réels |
| Chargement/pagination | React Query + `FlatList` | partiel | infinite queries ou pagination explicite |

## 25. Risques de régression

- casser les rails Explorer si les largeurs fixes sont supprimées globalement;
- casser navigation/favoris/commentaires en changeant les identifiants sans mapper unique;
- afficher des catégories vides en inventant une taxonomie frontend;
- perdre les filtres du profil si le header de `FlatList` est remplacé;
- augmenter le nombre de requêtes avec quatre catégories chargées simultanément;
- utiliser des images de démo ou d'œuvres comme couvertures trompeuses;
- masquer contenu sacré/restreint sans respecter les règles de `CultureContentService.publicList`.

## 26. Architecture cible minimale proposée

Conserver les domaines et routes. Faire de `culture.tsx` un catalogue visuel de quatre entrées configurées vers les capacités existantes, sans charger tous leurs contenus. Chaque destination charge ensuite uniquement son domaine et ses filtres backend. Garder une seule `ArtisanCard` et une seule `ArtworkCard` avec variantes de layout explicites (`rail`/`grid`), pilotées par le parent. Normaliser les DTO à la frontière API via mappers avant rendu.

## 27. Fichiers qui devraient être modifiés ultérieurement

- Mobile : `src/app/(explore)/artisans/[id].tsx`, `src/app/(profile)/followed-artisans.tsx`, `src/app/(explore)/culture.tsx`, `src/features/artisans/components/ArtisanCard.tsx`, `src/features/artworks/components/ArtworkCard.tsx`, APIs/mappers/types artisans et œuvres, éventuellement `CatalogListScreen.tsx`.
- Backend à confirmer après choix produit : `ArtisanDtos.java`, `ArtworkDtos.java` ou uniquement mappers mobiles; éventuellement modèle/migration uniquement si une vraie taxonomie types de plats/disciplines et médias de catégories est validée.
- Gateway : aucun changement requis pour les routes actuelles.

## 28. Backend : modifications nécessaires ou non

Pour les grilles et le catalogue quatre entrées : **aucune nouvelle route requise**. Pour les filtres actuellement supportés : aucune route requise. Un alignement DTO est nécessaire, mais peut être résolu côté mobile sans casser les contrats publics.

Une évolution backend n'est requise que si le produit exige des taxonomies persistées de types de plats, thèmes/peuples, disciplines d'œuvres ou images administrables de catégories. Cette décision implique modèle et potentiellement migration; elle ne peut pas être déduite comme obligatoire pour la refonte UI minimale.

## 29. Plan d'implémentation proposé

1. `ADAPT_EXISTING` — créer les mappers de frontière artisans/œuvres et tests de contrat; préserver UUID/routes.
2. `ADAPT_EXISTING` — variantes compactes de `ArtworkCard`/`ArtisanCard`; tests petits écrans et accessibilité.
3. `ADAPT_EXISTING` — deux colonnes sur profil artiste et suivis; préserver filtres, navigation, empty/error.
4. `EXTEND_EXISTING` — transformer `culture.tsx` en quatre cartes routant vers écrans existants, sans requêtes anticipées inutiles.
5. `ADAPT_EXISTING` — brancher uniquement les filtres backend existants et paginer.
6. `NEW_ONLY_IF_REQUIRED` — taxonomies/médias backend après validation produit et données.
7. Tests : contrats DTO, navigation/deep links, light/dark, iPhone/Android, pagination, follow/unfollow, détail œuvre et listes vides.

## 30. Questions bloquantes

1. Les quatre cartes doivent-elles être affichées même lorsqu'une catégorie ne contient aucun contenu publié, ou désactivées avec un état explicite ?
2. « Artistes et créateurs » doit-il inclure les deux `BusinessType` ou uniquement `ARTIST` ? La route publique ne permet pas actuellement ce filtre.
3. Les images des quatre catégories seront-elles administrables côté backend ou validées comme assets statiques produit ?
4. Les taxonomies de démonstration pour recettes/proverbes sont-elles validées métier ? Le code production ne les persiste pas.

AUDIT_MODE = READ_ONLY

APPLICATION_FILES_MODIFIED = NO

BACKEND_FILES_MODIFIED = NO

DATABASE_MODIFIED = NO

NEW_COMPONENTS_CREATED = NO

NEW_ENDPOINTS_CREATED = NO

ARTIST_ARTWORK_GRID_ROOT_CAUSE = FOUND

FOLLOWED_ARTISTS_CARD_ROOT_CAUSE = FOUND

CULTURE_CATEGORY_VISIBILITY_ROOT_CAUSE = FOUND

CULTURE_VIEW_MORE_FLOW_MAPPED = YES

PROVERBS_BACKEND_CONTRACT = EXISTING

CUISINE_BACKEND_CONTRACT = EXISTING

ARTISTS_BACKEND_CONTRACT = PARTIAL

LANGUAGES_CULTURE_BACKEND_CONTRACT = PARTIAL

EXISTING_CATEGORY_TAXONOMY = PARTIAL

EXISTING_SUBCATEGORY_FILTERS = PARTIAL

EXISTING_CATEGORY_IMAGES = MISSING

REUSABLE_COMPONENTS_IDENTIFIED = YES

DUPLICATION_RISK_IDENTIFIED = YES

NEW_BACKEND_ENDPOINTS_REQUIRED = NO

DATABASE_MIGRATION_REQUIRED = TO_CONFIRM

PROPOSED_IMPLEMENTATION_PHASES = CONTRACT_MAPPING, RESPONSIVE_CARDS, TWO_COLUMN_GRIDS, CULTURE_CATALOG_COMPOSITION, REAL_FILTERS_AND_PAGINATION, OPTIONAL_TAXONOMY

AUDIT_REPORT_READY = YES

READY_FOR_TARGETED_IMPLEMENTATION_PROMPT = YES
