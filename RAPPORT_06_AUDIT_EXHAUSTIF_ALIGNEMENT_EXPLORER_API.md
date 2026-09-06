# RAPPORT_06 - Audit exhaustif statique Explorer <-> API

Date : 2026-09-04. Perimetre confirme : `E:\Daryl\yeyamo-api\yeyamo-mobile` (Expo SDK 54).

## 1. Synthese executive

Constat : Explorer est structure autour de React Query et Axios, avec des routes Gateway presentes pour les principaux domaines. Le chemin complet n'est pas aligne pour toutes les interactions visibles. Les problemes les plus concrets sont :

1. les listes de lieux issues de Discovery conservent `sourceId` prefixe puis naviguent vers un detail Place qui attend un UUID (G) ;
2. la carte passe `radiusKm=1000` a une route qui accepte au plus 100 (C) ;
3. Experiences, recettes, proverbes et categories artistiques restent tout ou partie alimentes localement en session reelle (B/E) ;
4. des filtres, favoris, partages et boutons visibles ne lancent aucune mutation ou aucun appel (F) ;
5. les DTO Event et Place ne fournissent pas une partie importante des champs affiches (D).

Les constats sont **constates** lorsqu'ils proviennent du code lu, **deduits** lorsqu'ils suivent directement de deux contrats compares, et **non verifiables statiquement** lorsqu'ils dependent de donnees, JWT ou services en execution. Docker n'a pas ete lance.

## 2. Score global

| Indicateur | Resultat |
|---|---:|
| Alignement statique UI <-> API | **51 / 100** |
| Validation runtime | **NON EVALUEE** |

Le score couvre route, parametres, DTO, mapper, navigation, mocks, filtres et actions. Il ne signifie pas qu'une route repond effectivement en local.

## 3. Statistiques

Unite de comptage : une ligne de comportement utilisateur dans la matrice ci-dessous ; les cartes repetitives de meme comportement sont groupees.

| Mesure | Nombre | Part |
|---|---:|---:|
| Ecrans audites | 34 | - |
| Interactions auditees | 47 | 100 % |
| Alignees (J) | 16 | 34 % |
| Partiellement alignees | 15 | 32 % |
| Non alignees | 16 | 34 % |
| Interactions avec mock/local en session reelle | 8 | 17 % |
| Actions UI inactives | 12 | 26 % |
| APIs existantes non consommees | 5 | - |
| Contrats DTO insuffisants | 6 | - |
| Problemes identifiant/navigation | 3 | - |

## 4. Architecture reelle observee

`explore.tsx` appelle les hooks Explorer, Discovery, Recommendations, Culture, Artworks, Artisans et Events. Les hooks utilisent `apiClient` (`src/services/api/client.ts`), dont la base est `EXPO_PUBLIC_API_BASE_URL` ou `http://<Metro-host>:8083`, suivie de `/api/v1`. Le Bearer JWT est ajoute depuis le secure store.

La Gateway route notamment : Place/Region/Category (route 3), Event (2), Catalog (8 et 32), Discovery (13), Recommendation (14), Culture (30), Graph (31), Artisan (34) et Ticket (24) dans `cloud-conf-yeyamo/api-gateway.properties`. Les controllers correspondants existent dans les services concernes. La Gateway autorise publiquement certains GET (places, regions, events, culture, artworks, artisans), mais pas Discovery ni Recommendations : ces deux derniers requierent donc un JWT au niveau Gateway.

## 5. Matrice exhaustive interfaces -> API

| Ecran | Element UI | Action attendue | Hook / appel mobile | API backend | Parametres | Etat | Ecart | Correction recommandee |
|---|---|---|---|---|---|---|---|---|
| Accueil Explorer | chargement regions | afficher/choisir region | `useRegions` -> `GET /regions` | Place `RegionController` | aucun | J | DTO mobile met compteurs/coordonnees a 0 | etendre DTO si ces champs doivent etre reels |
| Accueil Explorer | choix region | changer le contexte visuel | etat local `selectedRegionId` | aucune | aucun | I | ne change ni pays store ni requetes deja chargees | relier le choix aux query keys/filtres |
| Accueil Explorer | categories | afficher categories | `useCategories` -> `GET /categories` | Place `CategoryController` | aucun | J | slugs editoriaux locaux non normalises | definir correspondance categorie UI/API |
| Accueil Explorer | carte categorie | ouvrir liste / domaine | navigation locale | aucune | id local | Partiel | `events`, culture, langues, oeuvres, artisans sont explicites ; le reste filtre seulement une liste locale | utiliser un filtre backend par categorie |
| Accueil Explorer | recherche | ouvrir recherche | navigation | aucune | aucun | J | - | - |
| Accueil Explorer | filtres avances | ouvrir sheet recherche | navigation | aucune | `filters=1` | J | - | - |
| Accueil Explorer | filtre rapide | ouvrir recherche type/proximite | navigation | Discovery plus loin | type/`nearby=1` | C | proximite ne transmet pas GPS/rayon | passer lat/lng/radiusKm |
| Accueil Explorer | bouton carte | ouvrir carte | navigation | aucune | aucun | J | - | - |
| Accueil Explorer | rail tendances lieux | charger et ouvrir detail | `useTrendingPlaces` -> `/discovery/trending?type=PLACE` | `DiscoveryController.trending` | type, size | G | mapper garde `sourceId` prefixe puis detail Place attend UUID | normaliser `sourceId` dans mapper |
| Accueil Explorer | rail recommandations | charger personnalisation | `useRecommendations` -> `/recommendations` | `RecommendationController` | languageCodes, page, size | J/H | actif seulement `sessionMode=backend`; JWT necessaire | conserver garde et afficher etat explicite |
| Accueil Explorer | rail evenements | charger evenements | `useUpcomingEvents` -> `/events/upcoming` | `EventController.upcoming` | aucun | D | UI demande image/lieu/ville/organisateur absents de `EventResponse` | enrichir DTO ou composer explicitement |
| Accueil Explorer | rail culture/createurs | ouvrir resultats Discovery | `useDiscoveryTrending` | `DiscoveryController.trending` | type/country/region | J/H | JWT requis ; demo affiche autre source | afficher erreur/auth si besoin |
| Liste lieux | tabs Tous/Populaire/Nouveaux/Pres de moi | filtrer liste | seulement `setActiveFilter` | aucune requete additionnelle | aucun | F | `activeFilter` n'est pas lu par `filteredPlaces` | modifier requete ou retirer tabs |
| Liste lieux | cartes | detail lieu | navigation avec `TrendingPlace.id` | Place detail | id | G | id est `sourceId` brut | normaliser avant navigation |
| Liste lieux | favori | sauvegarder lieu | `console.log` | aucune mutation | - | F | API non consommee | definir mutation interaction/favori |
| Liste lieux | carte FAB | carte | navigation | aucune | - | J | - | - |
| Carte Explorer | chargement marqueurs | trouver lieux proches | `usePlaces` -> `/places/nearby` | `PlaceController.nearby` | lat,lng,`radiusKm=1000` | C | validation backend max 100 | rayon <=100 ou Discovery geo <=200 |
| Carte Explorer | se recentrer | geolocaliser/carte | `useLocation`, action native | aucune API YeYamo | GPS | J | permission/runtime non evalue | test runtime separe |
| Carte Explorer | bouton recherche | ouvrir recherche | navigation | - | - | J | - | - |
| Carte Explorer | bouton filtre | filtrer carte | aucun `onPress` | aucune | - | F | bouton sans comportement | partager sheet de recherche |
| Carte Explorer | carte marqueur | ouvrir detail | navigation id Place nearby | Place detail | UUID | J | - | - |
| Recherche | texte/type/categorie | rechercher | `useDiscoverySearch` -> `/discovery/search` | `DiscoveryController.search` | q,type,country,region,city,categorie,langue,... | J/H | JWT Gateway requis | etat 401 explicite |
| Recherche | rayon/distance/Pres de moi | recherche geographique | pas dans `DiscoverySearchParams` final | Discovery accepte geo | aucun lat/lng/radiusKm | C/F | controle visuel sans effet HTTP | etendre types, hook, query key |
| Recherche | reset | reinitialiser recherche | etat local | aucune | - | J | - | - |
| Recherche | resultat | detail selon type | navigation avec prefixe retire | controllers Place/Event/Culture/Artwork/Artisan | UUID apres retrait | J | un type non traite tombe sur Place | switch exhaustif par type |
| Experiences | liste/filtres | lister experiences | `mockExperiences` si demo, `[]` sinon | Catalog et Discovery ont type EXPERIENCE | aucun | B/E | aucune consommation reelle | `catalog/assets?type=EXPERIENCE` ou Discovery |
| Experiences | changement lieu/favori | filtrer/sauvegarder | `console.log` | aucune | - | F | actions inactives | brancher API/retirer |
| Evenements | liste | charger evenements a venir | `useUpcomingEvents` | Event upcoming | aucun | D | contrat incomplet pour cartes | enrichir EventResponse |
| Evenements | saison/lieu | filtrer | etat local/`console.log` | Event upcoming sans filtre | aucun | F/C | aucun impact sur la requete | ajouter parametres backend ou supprimer UI |
| Evenements | favori | sauvegarder | `console.log` | aucune mutation utilisee | - | F | interaction inactive | utiliser API generique si validee |
| Detail evenement | charger detail/participation | consulter statut | `useEventDetail` -> detail + `/events/me` | Event controller | id, limit=100 | J/H | JWT requis pour `/events/me`; erreur absorbee en liste vide | propager etat auth approprie |
| Detail evenement | lieu | ouvrir place | navigation `place_id` | Place detail | UUID | J | location affichee est fallback | enrichir Event DTO |
| Detail evenement | favori/partage/ajout | sauver/partager/agenda | etat local ou boutons sans onPress | aucune | - | F | pas de persistance, deux boutons inactifs | mutation/Share/calendrier |
| Detail evenement | inscription/desinscription | participer | `useEventRegistration` -> POST/DELETE | Event controller | id + JWT | J/H | bouton reel dans ecran, runtime non evalue | tests 401/200 |
| Detail evenement | billets | charger types | `useEventTickets` -> `/tickets/events/{id}/types` | Ticket public types | eventId | J | `eventName` remplace par eventId dans mapper | enrichir reponse Ticket |
| Billets/checkout | choisir, hold, commander | achat billet | ticketing API hold/orders | Ticket controller | eventId,ticketTypeId,quantity, Idempotency-Key | J/H | authentification et paiement runtime non evalues | smoke test |
| Detail lieu | charger detail | afficher lieu | `usePlaceDetail` -> `/places/{id}` | Place controller | UUID | D | avis/evenements/prix/equipements absents DTO | composer endpoints ou enrichir DTO |
| Detail lieu | favori | sauvegarder | `setIsSaved` local | aucune mutation | - | F | aucun etat persiste | API interaction cible lieu |
| Detail lieu | partager | partager systeme | `Share.share` | aucune API | texte local | J | - | - |
| Detail lieu | reserver | reserver | Alert `BLOCKED_BY_BACKEND` | aucun contrat cible | - | A | pas de lien place -> activite reservable | definir contrat de composition |
| Detail lieu | itineraire | calculer route | Maps API `/maps/route` via `mapsApi` | `MapsController.route` | origin,destination,DRIVE | J/H | service externe/runtime non evalue | test cle Maps |
| Detail lieu | avis/evenements lies | consulter contenu | champs optionnels jamais remplis par mapper | APIs reviews/place-events existent | aucun appel | B/D | sections vides; reponse place ne les contient pas | requetes composees explicites |
| Culture | contenus/langues/defis/mot | consulter | hooks Culture | PublicCulture/Challenge | filtres, pays | J | mot du jour desactive sans pays en session reelle | UX etat vide |
| Culture | recettes/proverbes | cataloguer/detail/partager | tableaux `demo*` | Culture Content accepte RECIPE/PROVERB | aucun | B/E | mock en session reelle | mapper CultureContent |
| Culture | stories/traditions | lister | `CultureListScreen` | Culture contents | type STORY/TRADITION | J | - | - |
| Langues/lecons | liste/detail/demarrer | apprentissage | hooks Culture POST start | PublicCultureController | code/id + JWT pour mutations | J/H | JWT necessaire pour start/attempt/complete | gerer 401 |
| Quiz lecon | repondre/terminer | envoyer tentative/progression | POST attempts puis complete | PublicCultureController | lessonId, exerciseId, answer, score | C | reponse correcte suppose `options[0]` au lieu du champ backend | exposer correction ou contrat explicite |
| Defis | liste/detail/rejoindre | rejoindre et creer publication | Culture hooks POST join | CultureChallengeController | id + JWT | J/H | soumission du defi n'est pas faite ici | relier publication -> submission |
| Oeuvres | liste/detail/relations/commentaire | consulter et agir | Artworks hooks, offer; interaction publication | Catalog/Graph/Interaction | id/filtres | Partiel | boutons partage sans onPress, donnees atelier parfois Alert | completer actions |
| Artisans | liste/detail/filtre oeuvres | consulter | Artisan + Artworks hooks | Partner/Catalog | pays,id,page | J | filtre categorie est local sur contenu charge | pagination/filtres serveur si volume |
| Artisan detail | contacter | contacter artisan | Alert seulement | aucune route contact utilisee | - | F | CTA inactive | definir messagerie/contact |
| Categories artistiques | afficher et filtrer | explorer par savoir-faire | taxonomie demo + `useArtworks` | Catalog artworks | size=40, filtre local | B/E | categorie API non transmise ni prouvee compatible | exposer/mappper categorie canonique |

## 6. Analyse ecran par ecran

- **Accueil Explorer** : bonnes bases de chargement remote ; le changement de region est seulement local. Les rails ont trois sources differentes (Discovery, Event, Recommendation).
- **Places/Search/Map** : ce sont les ecrans les plus exposes aux incoherences de filtre et d'identifiant. `sourceId` est correctement nettoye dans Search, pas dans les mappers des listes de lieux.
- **Experiences** : ecran present mais pas connecte hors demo ; c'est le trou fonctionnel principal.
- **Event detail et tickets** : tickets et commandes suivent une chaine API reelle. La presentation d'evenement est plus riche que `EventResponse` et complete donc avec des valeurs fabriquees par `events.api.ts`.
- **Place detail** : detail principal remote, mais sections avis, evenements lies et similaires n'ont aucun appel de composition dans ce screen.
- **Culture** : langues, lecons, contenus, histoires, traditions et defis sont relies. Recettes/proverbes/categories artistiques restent locaux en session reelle.
- **Artwork/Artisan** : les listings/details sont relies ; contact, partage et certains filtres ne le sont pas.

## 7. DTO champ par champ

| Ressource / champs UI | Source API presente | Transformation mobile | Deficit constate |
|---|---|---|---|
| Place : id, nom, adresse, coordonnees, categorie, telephone, site, medias, horaires | `PlaceResponse` | mappe dans `places.api.ts` | rating, reviews, prix, equipements, avis, events lies/similaires ne sont pas fournis |
| Event : id, titre, description, dates, capacite, compteur, placeId | `EventResponse` | mappe | image, location, city, address, organisateur, prix, programme, tickets sont remplis vides/fallback ou absents |
| Experience : type, nom, description, geo | `CatalogAssetResponse` et Discovery existent | aucun mapper ecran | ecran non consomme |
| Artwork : liste/detail/media/relations | Controllers Catalog/Graph | hooks existants | categorie editoriale locale non contractuelle |
| Artisan : profil, pays, ville, specialites, oeuvres | Partner/Artwork controllers | hooks existants | contact public non fourni/utilise |
| Culture Content : contenu, traduction, type, langue | `ContentResponse` | hooks Culture | recettes/proverbes affichent autre modele demo (ingredients/etapes/usage) |
| Recommendation : id cible, kind, titre, categorie, region, geo, score | `RecommendationPage` | conversion Discovery | pays, ville, description, media absents pour cartes riches |
| Discovery : id, sourceId, type, titre, geo, meta | `DiscoveryDocument` | direct/mappers | `sourceId` doit etre normalise avant detail Place |
| Region : id, nom, slug, code, description, cover | Region response | compteurs/coordonnees definis a zero | UI affiche donc des valeurs non backend |
| Category : id, nom, slug, icon, enfants | Category response | fusion avec definitions locales | taxonomie locale peut diverger du backend |

## 8. Analyse des filtres

| Filtre | Etat React / query key | HTTP | Backend | Verdict |
|---|---|---|---|---|
| Region accueil | etat local seulement | non | - | F/I |
| Region recherche | inclus dans `filters` | `regionCode` | accepte Discovery | J |
| Pays recherche | inclus | `countryCode` | accepte Discovery | J |
| Texte/type/categorie/langue/culture/verified | inclus | transmis | acceptes Discovery | J |
| Proximite/distance | visible | non transmis | Discovery accepte lat/lng/radiusKm | C/F |
| Place populaire/nouveau/proche | `activeFilter` change | non | pas appele | F |
| Event saison/lieu | etat local | non | Event upcoming ne prend pas ces filtres | F/C |
| Experience categorie/lieu | etat local/mock | non | Catalog/Discovery supportent type et filtres | B/E |
| Artisan categorie oeuvre | filtre contenu deja charge | non | Catalog pagine | Partiel |

## 9. Navigation et identifiants

| Flux | ID API | ID mobile/navigation | Etat |
|---|---|---|---|
| Recherche Discovery -> Place | `sourceId=place:<uuid>` | prefixe retire avant navigation | J |
| Tendance Discovery -> Place | `sourceId=place:<uuid>` | conserve dans `TrendingPlace.id` | G |
| `placesApi.getPlaces` via Discovery -> detail | `sourceId` prefixe | conserve comme `Place.id` | G |
| Event -> Place | `placeId` UUID | passe directement | J |
| Artwork/Artisan/Culture -> details | UUID/id service | passe directement | J sous reserve des donnees runtime |

## 10. Mocks

| Domaine | Fichier | Mock | Demo seulement ? | Equivalent API | A remplacer en reel ? |
|---|---|---|---:|---|---:|
| regions/categories/tendances Explorer | `features/explore/mockData.ts` | listes locales | oui | Place/Discovery | non |
| events | `features/events/mockData.ts` | events locaux | oui | Event | non |
| places | `features/places/mockData.ts` | lieux locaux | oui | Place/Discovery | non |
| culture/langues/lecons/defis | `features/culture/culture.demo.ts` | contenu local | oui | Culture | non |
| experiences | `features/experiences/mockData.ts` | liste complete | **non, ecran vide hors demo** | Catalog/Discovery | oui |
| recettes/proverbes/categories art | `culturalCatalog.demo.ts` | 100 recettes/proverbes/taxonomie | **non** | Culture/Artwork | oui |
| defaults geo carte | `CAMEROON_CENTER`, `DEFAULT_ORIGIN` | centre/origine | non | GPS/Maps | oui comme fallback documente |

## 11. Actions UI inactives

Constats explicites : favoris Place et Event (`setIsSaved` local), favoris listes Place/Event/Experience (`console.log`), changement lieu Event/Experience (`console.log`), filtres Place non appliques, filtre carte sans `onPress`, partage/ajout Event sans `onPress`, contact Artisan via `Alert`, reservation Place via `Alert`, et categories artistiques locales. Ces actions sont classees F, sauf reservation Place (A : blocage assume faute de contrat).

## 12. Authentification

- GET publics Gateway : Places, Regions, Categories, Events publics, Culture, Graph, Artworks, Artisans, Media selon `SecurityConfig.isPublicMobileRequest`.
- JWT obligatoire a la Gateway : Discovery et Recommendations. `useRecommendations` est donc correctement limite a `sessionMode === 'backend'`; `useDiscoveryTrending` retourne un vide local en demo. Les sessions reelles dependront du token injecte par Axios.
- JWT obligatoire dans les services : inscriptions Event, parcours de lecon, defis, ticket hold/order et mutations. L'injection Bearer est constatee ; les reponses 401/403 ne sont pas verifiables statiquement.

## 13. APIs existantes mais non consommees

1. `GET /catalog/assets?type=EXPERIENCE` ou Discovery `type=EXPERIENCE` : ecran Experiences.
2. `GET /culture/contents?type=RECIPE` : recettes.
3. `GET /culture/contents?type=PROVERB` : proverbes.
4. `GET /places/{placeId}/events`, reviews Place : sections detail Place.
5. routes interactions generiques : aucun branchement demontre pour favoris Place/Event.

## 14. Redondances et sources canoniques recommandees

- **Listing/recherche multi-domaine** : Discovery (`/discovery/search` et `/trending`).
- **Proximite GPS de lieux operationnels** : Place Service (`/places/nearby`), rayon <=100 km.
- **Detail lieu** : Place Service (`/places/{id}`), complete par reviews/events si l'UI les conserve.
- **Experiences** : Discovery pour recherche/classement, Catalog Assets pour detail/editing selon le DTO retenu.
- **Evenements** : Event Service pour detail/inscription ; Ticket Service pour types et commandes.
- **Contenu culturel** : Culture Service par `ContentType`; les tableaux locaux ne doivent rester qu'en demo.

## 15. Ecarts classes A -> J

| Classe | Occurrences constatees | Exemple |
|---|---|---|
| A API absente | 1 | reservation Place vers activite reservable |
| B API existante non consommee | 5 | Experiences, recettes, proverbes, avis/evenements Place |
| C parametres incorrects/incomplets | 4 | rayon carte, proximite Search, quiz, filtres Event |
| D Response insuffisante | 6 | Event, Place, Recommendation, Region, Category, Culture recipe |
| E mock/local | 8 | Experiences, recettes, proverbes, categories art |
| F UI inactive | 12 | favoris, filtres, contact, boutons carte/Event |
| G navigation/ID | 3 | `sourceId` non nettoye dans deux flux, fallback type |
| H auth incompatible/non prouvee | 5 | Discovery, recommendations et mutations JWT |
| I UX sans API directe | 3 | regions/carte statiques, encodage |
| J alignement complet | 16 | recherche textuelle, details normalises, culture connectee, tickets |

## 16. Priorites P0 -> P3

- **P0 - Front/mapper** : nettoyer `sourceId` avant navigation Place ; corriger rayon carte ; connecter Experiences hors demo.
- **P1 - Front + DTO** : propager GPS/distance dans Discovery ; completer Event/Place ou composer les requetes ; rendre actifs les filtres visibles.
- **P2 - Front + backend selon choix produit** : remplacer recettes/proverbes/categories art reellement par Culture/Artwork ; connecter favoris/contact/avis/evenements lies.
- **P3 - Front** : supprimer localisation figee et corriger les textes mal encodes ; rationaliser les sources redondantes.

## 17. Plan d'alignement recommande

1. Definir des adaptateurs canoniques `DiscoveryItem -> detail id` et `Catalog/Place -> card` (frontend/mapper).
2. Choisir la source de chaque cas d'usage selon la section 14, puis supprimer les filtres UI non supportes ou ajouter leurs parametres backend.
3. Enrichir ou composer les DTO Event et Place avant de promettre images, lieux, avis, prix, organisateurs et evenements similaires.
4. Remplacer les mocks reellement visibles hors demo par les routes listees en section 13, tout en gardant les demos conditionnels.
5. Implementer les mutations qui correspondent aux CTA conserves ; sinon retirer/neutraliser explicitement les actions inactives.
6. Executer ensuite des smoke tests Gateway avec et sans JWT ; cette etape est hors du present audit.

## 18. Limites de l'audit

La presence de code Controller, Gateway et DTO est constatee. La disponibilite de Docker, les enregistrements Eureka, le contenu des index Discovery, les statuts HTTP, les permissions effectives et les donnees medias ne sont **pas verifies runtime**. Les chiffres comptent les comportements groupes, pas chaque repetition visuelle d'une meme carte dans une FlatList.
