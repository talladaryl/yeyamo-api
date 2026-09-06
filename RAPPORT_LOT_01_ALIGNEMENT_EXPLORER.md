# Rapport Lot 01 — Alignement Explorer

Date : 4 septembre 2026  
Périmètre : `yeyamo-mobile` (Expo SDK 54) et vérification statique des contrats `yeyamo-api`.  
Docker n'a pas été démarré dans le cadre de ce lot.

## Objectif réalisé

Les flux Explorer de recherche, navigation Discovery, carte, proximité, régions et listes de lieux ont été alignés sur les contrats backend existants. Les sessions `demo-*` conservent leurs données de démonstration ; les sessions réelles ne reçoivent plus de valeurs inventées en remplacement d'une réponse API absente ou incomplète.

## Contrats backend vérifiés

| Flux | Route appelée par le mobile | Contrat statique vérifié |
|---|---|---|
| Recherche Discovery | `GET /api/v1/discovery/search` | `q`, `type`, `categoryCode`, `regionCode`, `lat`, `lng`, `radiusKm`, pagination et filtres culture sont acceptés par `DiscoveryController`. `radiusKm` est borné à `]0, 200]`. |
| Tendances Places | `GET /api/v1/discovery/trending?type=PLACE&regionCode=…` | `type`, `regionCode`, `countryCode`, pagination sont acceptés. |
| Lieux proches | `GET /api/v1/places/nearby?lat=…&lng=…&radiusKm=…&limit=20` | `PlaceController` exige `lat/lng` et impose `0,1 <= radiusKm <= 100`. |
| Détail d'un lieu | `GET /api/v1/places/{uuid}` | L'identifiant de chemin est un UUID. |
| Catégories | `GET /api/v1/categories` | La réponse expose `id`, `name`, `slug`, `icon`, enfants. |
| Régions | `GET /api/v1/regions` | Réponse consommée par l'écran Explorer. |
| Événements à venir | route existante consommée par `useUpcomingEvents` | Aucun contrat de filtre global saison/zone/date n'a été trouvé dans le flux utilisé. |

Le client Axios mobile préfixe ces routes avec `/api/v1` et injecte le Bearer token sauvegardé. Les endpoints Discovery sont protégés côté backend ; l'intercepteur existant renouvelle le token après un `401`, puis déconnecte la session réelle si le renouvellement échoue. Aucun fallback mock n'est appliqué dans ce cas.

## Modifications mobiles

### Identifiants et navigation Discovery

- Ajout de `src/features/discovery/discovery.navigation.ts`.
- `normalizeDiscoveryId(sourceId)` accepte un UUID tel quel ou un identifiant préfixé (`place:<uuid>`, `event:<uuid>`, etc.) et retourne l'identifiant cible.
- `discoveryHref` associe explicitement `PLACE`, `EVENT`, `ARTWORK`, `ARTISAN`, `CULTURE`/`CONTENT`/`TRADITION` et `LANGUAGE` à leurs routes existantes.
- `DESTINATION`, `EXPERIENCE` et tout type non pris en charge ne sont plus redirigés par défaut vers un lieu : l'utilisateur reçoit un état contrôlé « Contenu indisponible ».
- La normalisation est utilisée pour les tendances et les résultats Places issus de Discovery. Les remplacements de préfixe locaux ont été retirés des écrans Explorer et Search.

### Recherche et proximité

- `DiscoverySearchParams` contient maintenant `lat`, `lng` et `radiusKm`.
- Le raccourci « Près de moi » demande la permission de localisation, attend une position réelle et transmet ces trois paramètres à Discovery.
- Sans permission, sans position ou en cas d'échec, aucune recherche avec une position Cameroun fictive n'est envoyée : un écran contrôlé permet de réessayer.
- La clé React Query inclut l'objet de filtres, donc position, rayon, région, catégorie et autres paramètres participent au cache et au refetch.

### Carte

- Suppression de la requête invalide avec `radiusKm=1000`.
- La carte utilise un rayon sélectionnable parmi `10`, `25`, `50`, `100` km, tous compatibles avec Place Service.
- Le rayon et la position font partie de la clé `usePlaces`, donc un changement réexécute la requête.
- `CAMEROON_CENTER` reste uniquement le centre visuel initial et le contexte demo. Il ne sert plus de position utilisateur réelle.
- En session réelle sans géolocalisation, les marqueurs réels ne sont pas chargés et une action de réessai est affichée.

### Listes Places, régions, catégories et événements

- L'onglet Places conserve uniquement des contrôles effectifs :
  - **Tous** : recherche Discovery avec région et catégorie éventuelles ;
  - **Populaire** : route Discovery trending ;
  - **Près de moi** : Place Service nearby avec position réelle.
- Le filtre décoratif « Nouveaux » a été retiré car aucun tri backend correspondant n'est exposé par le contrat vérifié.
- Les catégories et régions réelles viennent exclusivement du backend. Les définitions éditoriales restent cantonnées aux sessions `demo-*`.
- Une région choisie est désormais transmise aux tendances et le libellé fictif `Centre => Yaoundé` a été supprimé.
- Les filtres d'événements (saison, lieu et « plus tard ») et l'action de sauvegarde qui ne déclenchaient qu'un `console.log` ont été retirés. Ils ne prétendent plus filtrer ou modifier le backend.
- Les boutons de favoris inactifs dans les cartes Places/Events ne sont plus affichés sans callback réellement implémenté.

### Données réelles et mocks

- `useTrendingPlaces`, `usePlaces`, catégories et régions isolent leurs caches par mode `demo`/`backend`.
- En `demo-*`, les tableaux de démonstration existants sont conservés.
- En session backend, aucune image, note, distance, compteur ou adresse n'est fabriqué à partir de la réponse Discovery. Les cartes affichent un visuel neutre et « Informations à découvrir » lorsqu'une donnée n'est pas fournie par le contrat.
- Les erreurs API, `401/403`, listes vides et permissions refusées restent des états réels ; ils ne basculent pas vers des mocks.

## Fichiers modifiés

- `yeyamo-mobile/src/features/discovery/discovery.navigation.ts` (nouveau)
- `yeyamo-mobile/src/features/discovery/discovery.types.ts`
- `yeyamo-mobile/src/features/discovery/discovery.hooks.ts`
- `yeyamo-mobile/src/features/explore/explore.api.ts`
- `yeyamo-mobile/src/features/explore/useExplore.ts`
- `yeyamo-mobile/src/features/explore/types.ts`
- `yeyamo-mobile/src/features/places/places.api.ts`
- `yeyamo-mobile/src/features/places/types.ts`
- `yeyamo-mobile/src/features/places/usePlaces.ts`
- `yeyamo-mobile/src/app/(tabs)/explore.tsx`
- `yeyamo-mobile/src/app/(explore)/search.tsx`
- `yeyamo-mobile/src/app/(explore)/map.tsx`
- `yeyamo-mobile/src/app/(explore)/places.tsx`
- `yeyamo-mobile/src/app/(explore)/events.tsx`
- `yeyamo-mobile/src/components/explore/TrendingPlaceCard.tsx`
- `yeyamo-mobile/src/components/explore/PlaceListItem.tsx`
- `yeyamo-mobile/src/components/events/EventCard.tsx`

## Validation effectuée

Exécuté depuis `yeyamo-mobile` :

```powershell
.\node_modules\.bin\tsc.cmd --noEmit
npm run lint
```

Résultats :

- TypeScript : succès, aucune erreur.
- `expo lint` : succès, `0` erreur et `60` avertissements préexistants hors du flux modifié (le seul avertissement Explorer restant concerne `experiences.tsx`, hors du périmètre de ce lot). Aucun test automatisé dédié à Discovery/Explorer n'était déclaré dans les scripts npm ; le projet expose `lint`, `start`, `android`, `ios` et `web`.

## Limites restantes, volontairement non étendues dans ce lot

1. Les routes de détail mobile pour `EXPERIENCE` et `DESTINATION` n'existent pas. La navigation est donc explicitement bloquée avec un message, plutôt que redirigée vers une route erronée.
2. `GET /api/v1/events/upcoming` ne fournit pas, dans le contrat statiquement vérifié, de filtres globaux saison/zone/date. Les filtres d'interface ont été retirés ; leur extension demanderait une évolution de contrat distincte.
3. Le backend Categories publie un `slug`, tandis que Discovery filtre avec `categoryCode`. Le mobile utilise désormais la valeur backend `slug` comme valeur de filtre, sans slug UI hardcodé. La cohérence des valeurs réellement indexées dépend des données d'indexation ; elle nécessite un test d'intégration avec une base alimentée, non exécuté ici puisque Docker n'a pas été lancé.
4. La disponibilité runtime du Gateway, des index Discovery et des données métier n'est pas attestée par ce rapport : cette vérification requiert le démarrage des services et des appels HTTP authentifiés.

Le lot suivant n'a pas été commencé.
