# RAPPORT_05 - Audit statique de l'onglet Explorer

Date : 2026-09-04

## Perimetre et methode

Audit realise sans demarrer Docker et sans modifier le code applicatif. Les verifications reposent sur :

- le mobile `yeyamo-mobile` present dans ce depot, notamment `src/app/(tabs)/explore.tsx` et `src/app/(explore)` ;
- les appels Axios et hooks React Query associes ;
- les controleurs Java, la configuration de la Gateway et `docker-compose.yml` de `yeyamo-api`.

Le projet mobile audite est bien en Expo SDK 54 (`expo ~54.0.0`). Le rapport ne conclut donc pas sur la disponibilite runtime : aucune requete HTTP ni conteneur n'a ete lance.

## Conclusion

L'onglet Explorer est riche et la majorite de ses domaines disposent d'une route backend et d'un routage Gateway. L'alignement n'est toutefois pas suffisamment fiable pour considerer le module entierement integre : certaines interfaces restent exclusivement mockees, plusieurs controles visibles ne modifient aucune requete, et deux chemins de lieux transmettent un identifiant `sourceId` non compatible avec le detail de lieu.

Evaluation statique de l'alignement interface/API : **58/100**.

Ce score mesure les ecrans et comportements visibles dans Explorer, pas la qualite fonctionnelle de l'ensemble du backend.

## Cartographie interface -> API

| Surface mobile | Appel mobile actuel | Route backend/Gateway trouvee | Etat statique |
|---|---|---|---|
| Chargement des regions | `GET /regions` | `place-service`, `GET /api/v1/regions` ; route Gateway 3 | Aligne, avec DTO mobile incomplet |
| Chargement des categories | `GET /categories` | `place-service`, `GET /api/v1/categories` ; route Gateway 3 | Aligne au niveau transport, semantique a normaliser |
| Rail "Pres de vous" | `GET /discovery/trending?type=PLACE&size=20` | `discovery-service`, `GET /api/v1/discovery/trending` ; route Gateway 13 | Route existante, identifiant detail incorrect dans le mapper |
| Rail "Pour vous" | `GET /recommendations` | `recommendation-service`, `GET /api/v1/recommendations` ; route Gateway 14 | Route existante, authentification requise |
| Rail / liste d'evenements | `GET /events/upcoming` | `event-service`, `GET /api/v1/events/upcoming` ; route Gateway 2 | Route existante, mais contrat visuel incomplet |
| Detail / inscription evenement | `GET /events/{id}`, `GET /events/me`, `POST /events/{id}/register`, `DELETE /events/{id}/unregister` | `event-service` expose ces quatre routes ; Gateway 2 | Aligne au niveau des routes |
| Recherche Explorer | `GET /discovery/search` | `discovery-service`, `GET /api/v1/discovery/search` ; route Gateway 13 | Route existante, filtres geo non transmis |
| Carte | `GET /places/nearby` | `place-service`, `GET /api/v1/places/nearby` ; route Gateway 3 | Appel present mais invalide : rayon mobile 1000 km, maximum backend 100 km |
| Detail de lieu | `GET /places/{id}` | `place-service`, `GET /api/v1/places/{id}` ; route Gateway 3 | Route existante ; certains parcours envoient un mauvais id |
| Culture, langues, lecons, defis | `/culture/**` et `/culture-graph/**` | `culture-service` et `graph-service` exposent les routes correspondantes ; routes Gateway 30 et 31 | Majoritairement aligne |
| Oeuvres et artisans | `/artworks/**`, `/artisans/**`, `/artisan-specialties/**` | `catalog-service` et `partner-service` ; routes Gateway 32 a 34 | Routes existantes |
| Experiences | aucun appel en session reelle | `catalog-service` expose `GET /api/v1/catalog/assets?type=EXPERIENCE` et Discovery accepte `type=EXPERIENCE` | Non consomme cote mobile |
| Recettes et proverbes | donnees `demoRecipes` / `demoProverbs` | `culture-service` sait filtrer `GET /api/v1/culture/contents?type=RECIPE` et `type=PROVERB` | Non consomme cote mobile |
| Categories artistiques | categorie locale + `GET /artworks` | routes oeuvres existantes | Filtrage mobile non garanti par le contrat backend |

La configuration Gateway route bien les prefixes necessaires : `/places`, `/regions`, `/categories`, `/events`, `/catalog/assets`, `/discovery`, `/recommendations`, `/culture`, `/culture-graph`, `/artworks` et `/artisans`.

## Ecarts bloquants ou majeurs

1. **Identifiant de lieu incorrect apres une tendance ou une recherche de lieux.**

   `explore.api.ts` affecte `item.sourceId` a `TrendingPlace.id`. `places.api.ts` fait la meme chose pour les resultats Discovery. Or la navigation directe construit `/(places)/${id}`, tandis que `PlaceController.getById` attend un `UUID`. Le `sourceId` est de forme prefixee (par exemple `place:<uuid>`), comme le montre le fait que les autres parcours retirent explicitement le prefixe avec `replace(/^[^:]+:/, '')`. Le detail de lieu peut donc recevoir une valeur non UUID et echouer avant d'atteindre la ressource.

2. **La carte envoie un rayon refuse par le backend.**

   `map.tsx` appelle `usePlaces` avec `radius_km: 1_000`. Le mobile le convertit correctement en `radiusKm`, mais `PlaceController` impose `0.1 <= radiusKm <= 100`. Cette interface produira une validation 400 lorsque le backend est utilise.

3. **La fonctionnalite "Pres de moi" ne transmet pas la position.**

   Le filtre rapide ajoute `nearby=1` puis `SearchScreen` ne transmet que `regionCode`. Les types `DiscoverySearchParams` et le hook omettent `lat`, `lng` et `radiusKm`, bien que `DiscoveryController` les accepte. Le controle de distance affiche dans les filtres avances n'est lui non plus pas inclus dans l'objet passe a `useDiscoverySearch`.

4. **L'ecran Experiences est vide hors comptes demo.**

   `experiences.tsx` retourne explicitement `[]` lorsque la session n'est pas demo. Il existe pourtant deux contrats utilisables : `GET /catalog/assets?type=EXPERIENCE` et `GET /discovery/search?type=EXPERIENCE`.

5. **Les filtres et actions visibles de plusieurs listes ne sont pas fonctionnels.**

   - `places.tsx` conserve `activeFilter`, mais son calcul ne l'utilise pas ; "Populaire", "Nouveaux" et "Pres de moi" affichent donc la meme liste.
   - `events.tsx` affiche lieu et filtres saisonniers sans les appliquer a `GET /events/upcoming` ; sauvegarde et changement de lieu sont des `console.log`.
   - `experiences.tsx` a les memes actions locales non branchees.
   - Le bouton de filtre de la carte ne declenche aucune action.

6. **Le contrat evenement ne fournit pas les informations affichees.**

   `EventResponse` expose id, placeId, titre, description, dates, statut, capacite et compteur. Le mapper mobile insere `"Lieu associe"`, une ville et adresse vides, un organisateur de secours et une image absente. L'ecran fonctionne techniquement, mais ne peut pas fournir une fiche evenement complete sans enrichissement backend ou requetes de composition vers le lieu/utilisateur/media.

## Coherence UX/UI et interfaces redondantes

### Redondance fonctionnelle

- Le rail "Pres de vous", la page `/(explore)/places` et la carte representent tous des lieux, mais n'utilisent pas la meme source : tendance Discovery, tendance filtree localement et proximite Place Service. Une source canonique doit etre choisie par intention : Discovery pour classement/recherche, `places/nearby` pour rayon GPS, puis detail Place Service.
- La carte affiche deja une recherche et un bouton filtre, alors que l'ecran recherche porte les filtres avances. Soit la carte ouvre ce meme filtre avec des coordonnees, soit ses controles doivent etre retires pour eviter une interface inactive.
- La section Culture annonce un module "A transmettre" qui reunit proverbes, recettes, langues et arts, tandis que des routes paralleles gardent des catalogues locaux. Le regroupement est coherent visuellement, mais recettes/proverbes/categories d'art ne doivent pas coexister durablement en mock avec les contenus culturels backend equivalents.

### Problemes de coherence

- La localisation affichee est partiellement figee : la region `Centre` devient toujours `Yaounde` dans l'en-tete Explorer, et la carte est centree sur le Cameroun avec des etiquettes regionales statiques. Cela ne correspond pas au pays choisi dans `country.store` ni a la geolocalisation utilisateur.
- La liste de categories melange des slugs editoriaux locaux (`attractions`, `restaurants`, `hotels`) et des categories renvoyees par Place Service. Elle ne definit pas de table de correspondance contractuelle. Les categories qui ne sont pas explicitement reconnues ouvrent une liste de lieux filtree localement par egalite de chaines ; ce resultat depend donc des valeurs indexees par Discovery.
- Les textes Explorer contiennent des caracteres corrompus dans les fichiers inspectes (exemples visibles : `YaoundÃ©`, `Ã‰venements`, `PrÃ¨s`). C'est un defaut de rendu utilisateur a corriger independamment de l'API.

## Etat des mocks

Les deux modes demo du produit sont preserves par les hooks Explorer : regions, categories et tendances utilisent les mocks uniquement lorsque `sessionMode` commence par `demo-`.

En revanche, les mocks suivants ne sont pas limites au mode demo et empechent une consommation reelle complete :

- Experiences : aucune source backend hors demo.
- Recettes et proverbes : 100 entrees locales dans `culturalCatalog.demo.ts`.
- Categories artistiques : taxonomie et visuels locaux ; les oeuvres sont bien chargees, mais le filtrage par categorie est local.

## Verification TypeScript

La commande suivante a reussi sans erreur :

```powershell
E:\Daryl\yeyamo-api\yeyamo-mobile\node_modules\.bin\tsc.cmd --noEmit
```

Il n'y a donc pas d'erreur TypeScript bloquante dans l'etat audite. Cette reussite ne detecte pas les incoherences de contrat HTTP, les actions `console.log`, les identifiants prefixees ou les valeurs de rayon invalides ci-dessus.

## Ajustements recommandes, dans l'ordre

1. Normaliser les identifiants Discovery avant toute navigation vers un detail et centraliser cette regle dans un adaptateur.
2. Corriger la carte : coordonnees reelles, rayon au plus egal a 100 km pour Place Service, ou utiliser Discovery avec un rayon au plus egal a 200 km.
3. Faire passer `lat`, `lng` et `radiusKm` dans `DiscoverySearchParams`, le hook et `SearchScreen`; ne pas presenter "Pres de moi" tant que ces donnees ne sont pas appliquees.
4. Remplacer Experiences hors demo par une requete catalogue/Discovery `type=EXPERIENCE`, puis relier filtres, lieu et sauvegarde a des comportements reels.
5. Choisir une strategie editoriale pour recettes, proverbes et arts : conserver explicitement les mocks demo ou mapper ces ecrans vers `culture/contents` avec les types `RECIPE` et `PROVERB`.
6. Completer le contrat evenement (lieu, ville, adresse, media, organisateur) ou documenter une composition mobile explicite depuis `placeId`.
7. Supprimer les controles inactifs, ou les relier a des requetes effectives avant mise en production.
8. Corriger l'encodage UTF-8 des fichiers Explorer et supprimer les libelles/localisations fixes.

## Limite explicite de cet audit

L'existence des routes et leur routage Gateway sont verifies dans le code. Leur reponse reelle, les donnees indexees dans Discovery, les droits JWT et l'etat de chaque service doivent etre verifies par un smoke test separe lorsque Docker sera demarre. Aucun resultat runtime n'est affirme dans ce rapport.
