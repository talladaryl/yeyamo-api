# Phase 1 — Audit global de l’intégration Yeyamo Mobile ↔ Backend

**Date de l’audit :** 2026-10-26
**Type :** audit statique uniquement
**Dépôt de rattachement du rapport :** `yeyamo-api`
**Verdict global :** architecture statiquement raccordée sur les parcours centraux, mais disponibilité réelle depuis Expo Go iOS et état runtime des profils/services non vérifiés.

## 1. Résumé exécutif

Les deux incidents signalés ne sont pas attribuables avec certitude à un défaut de route dans le code accessible.

- La connexion mobile appelle `POST /api/v1/auth/login`; le Gateway route `/api/v1/auth/**` vers `auth-service`, et le contrôleur expose bien `POST /api/v1/auth/login` comme endpoint public.
- Le chargement des pays appelle `GET /api/v1/countries`; le Gateway route ce chemin vers `country-config-service`, qui expose ce GET et possède une chaîne Spring Security dédiée `permitAll`.
- Les URLs de production déclarées côté mobile sont cohérentes : `https://api.yeyamo.com` et `wss://api.yeyamo.com/ws/messaging`.
- Le fallback mobile vers le host Expo/loopback reste une cause plausible pour Expo Go iOS si les variables `EXPO_PUBLIC_*` ne sont pas réellement injectées dans l’exécution observée.
- Les messages d’erreur mobiles masquent volontairement une partie de la cause technique, ce qui empêche de distinguer statiquement DNS/TLS, timeout, 502/503 Gateway et panne d’un microservice.
- Les profils Compose rendent plusieurs modules optionnels. Les appels vers gamification, analytics, recommandation, commerce et autres doivent être comparés au profil réellement activé en production; cette information n’est pas disponible dans l’audit statique.

**Comptage des anomalies retenues :**

| Priorité | Confirmées | Candidates / non vérifiées | Total suivi |
|---|---:|---:|---:|
| P0 | 0 | 1 | 1 |
| P1 | 1 | 4 | 5 |
| P2 | 0 | 4 | 4 |
| P3 | 0 | 2 | 2 |

Il n’y a donc **aucun défaut P0 confirmé par le code**. Le P0 retenu est un blocage de confirmation de disponibilité du chemin critique, pas une preuve de panne.

## 2. Périmètre réellement audité

Audit réalisé sans démarrer de service, sans appel HTTP métier, sans migration, sans redémarrage, sans build complet et sans modification de code/configuration.

- Dépôt backend `E:\Daryl\yeyamo-api`.
- Dépôt mobile `E:\Daryl\yeyamo-mobile`.
- Tous les fichiers TypeScript/TSX sous `mobile/src` liés à Axios, wrappers API, WebSocket, notifications et hooks de données.
- Contrôleurs Java et annotations de mapping des microservices accessibles.
- Routes Spring Cloud Gateway dans `cloud-conf-yeyamo/api-gateway.properties`.
- Configurations Spring Security Gateway, auth et country-config.
- Compose local, production et override d’images GHCR.
- Configuration Expo, `.env`, `eas.json`, client HTTP et client WebSocket.

Non audité dynamiquement : DNS public, certificat TLS, réseau iPhone réel, état Docker/Dokploy, registre, Eureka, Config Server actif, disponibilité réelle des services, contrats JSON observés sur le réseau et données métier.

## 3. Dépôts/fichiers accessibles

| Élément | État | Éléments principaux |
|---|---|---|
| `yeyamo-api` | Accessible | microservices Java, Gateway, `cloud-conf-yeyamo`, Compose, Dockerfiles |
| `yeyamo-mobile` | Accessible | Expo/React Native, `src/features`, `src/services/api`, `src/services/socket` |
| Git backend | Propre au début de l’audit | aucune modification source réalisée |
| Git mobile | Propre au début de l’audit | aucune modification réalisée |
| Rapport cible | Créé par cette phase | `PHASE_1_AUDIT_GLOBAL_INTEGRATION_MOBILE_BACKEND.md` |

Le fichier `yeyamo-mobile/AGENTS.md` impose de consulter la documentation Expo v56 avant d’écrire du code. Aucune écriture de code mobile n’a été effectuée.

## 4. Architecture communication

Le chemin HTTP principal est :

`Écran Expo → hook TanStack Query/service feature → apiClient Axios → ${API_BASE_URL}/api/v1 → API Gateway :8083 → route lb://service → contrôleur Spring Boot`.

Le client mobile ajoute :

- `Accept: application/json`;
- `X-Requested-With: XMLHttpRequest`;
- `Authorization: Bearer ...` pour les appels non publics;
- `X-Correlation-ID: mobile-<timestamp>-<random>`;
- timeout Axios général de 15 secondes;
- timeout upload de 120 secondes.

Le chemin temps réel est séparé :

`WebSocket(ENV.MESSAGING_WS_URL, STOMP 1.2/1.1) → Gateway /ws/messaging → messaging-service → CONNECT STOMP avec Bearer`.

Les occurrences `fetch()` trouvées dans le mobile concernent les ressources locales de splash/onboarding, pas un client HTTP backend concurrent. Les notifications utilisent Expo Notifications puis les endpoints push du module notifications.

## 5. Inventaire global appels frontend

L’extraction statique des appels `apiClient` et wrappers `apiGet/apiPost/apiPut/apiPatch/apiDelete` a produit **250 motifs d’appels uniques**. Les chemins dynamiques sont comptés comme un motif et non comme toutes leurs valeurs possibles.

| Module | Appels réellement présents, regroupés par chemin | Authentification attendue |
|---|---|---|
| `auth` | `/auth/login`, `/auth/register`, `/auth/logout`, `/auth/me`, `/auth/refresh`, vérification email, reset password, OAuth provider, password, account deactivate, sessions | public pour login/register/refresh/OAuth/reset; JWT pour le reste |
| `country` | `/countries`, `/countries/available`, `/countries/{code}/*`, `/administrative-areas`, `/cities`, `/cities/{id}/localities`, `/users/me/*` | pays/géographie GET public; profil JWT |
| `artisans` | `/artisan-specialties`, `/partners/me/artisan-profile`, `/artisans/{id}/profile` | lecture publique selon chemin; profil partenaire JWT |
| `artwork-orders` | `/artwork-orders/*`, `/artisan/orders/*` | JWT |
| `artworks` | `/artworks`, `/artworks/{id}`, `/artworks/{id}/history`, `/artworks/{id}/media`, `/artwork-offers*` | lecture selon endpoint; mutations JWT |
| `ads` | `/ads/impressions`, `/ads/clicks`, `/ads/conversions`, sélection publicitaire | JWT ou contexte applicatif selon service |
| `chat` | `/messaging/conversations*`, messages, read, outing group, conversation partner | JWT; WebSocket JWT au CONNECT |
| `collections` | `/collections*`, `/collections/places`, `/collections/{id}/places/{assetId}` | JWT |
| `culture` | daily word, categories, languages, contributions, challenges, lessons, contents, `/culture-graph/*` | lecture publique selon endpoint; mutations/JWT |
| `discovery` | `/discovery/search`, `/discovery/trending` | lecture publique ou JWT selon route |
| `events` | `/events`, upcoming, me, detail, register/unregister, invitations | GET public selon route; opérations JWT |
| `experiences` | `/catalog/assets/{id}` | lecture publique |
| `explore` | `/regions`, `/categories`, trending, adventure plans preview/create/get/delete/skip | lecture publique pour rails; plans JWT |
| `feed` | `/feed`, `/feed/sponsored`, posts, likes, favorites, comments, shares, views, summaries | feed authentifié; lectures publiques selon endpoint |
| `finance` | `/commerce/partners/{partnerId}/finance/summary`, transactions | rôle partenaire |
| `interactions` | feedback, favorites, generic interactions, comments, `/interactions/me/likes`, user reviews | JWT pour mutations et données personnelles |
| `interests` | `/users/me`, `/users/me/interests`, `/users/me/activity` | JWT |
| `maps` | `/maps/geocode`, reverse-geocode, route, `/places/directions` | dépend du Gateway/service |
| `media` | upload multipart, `/media/{id}`, batch, suppression | JWT pour upload/suppression |
| `notifications` | list/unread/count/preferences, read, push-token, device token | JWT |
| `partner` | `/partners/me`, create partner | JWT |
| `partner-dashboard` | partner CRUD/submit, `/analytics/partners/{id}/dashboard`, `/analytics/artisans/me` | rôle partenaire |
| `partner-staff` | staff invitations, assignments, roles, event staff | rôle partenaire |
| `places` | places, suggestions, duplicate check, activities, bookings, nearby, search, detail, availability, city, media | lecture mixte; mutations JWT |
| `post` | create/publish/delete posts | JWT |
| `profile` | posts me, likes, saves, events, social stats, author posts, reviews, booking cancel | JWT ou lecture publique selon endpoint |
| `promotions` | `/commerce/admin/promotions*`, `/commerce/partners/{id}/promotions*` | admin ou partenaire |
| `recommendations` | `/recommendations` | JWT/contexte utilisateur |
| `reviews` | create/list/aggregate/eligibility/delete/report | lecture partiellement publique; mutations JWT |
| `settings` | user profile/preferences/location/language/discovery, delete user | JWT |
| `social` | suggestions/activity/muted/blocked/settings/profile/identities, follow/block/mute | lecture mixte; mutations JWT |
| `social-graph` | `/me/badges`, xp, passport, rewards, missions, leaderboard, claim | JWT |
| `story` | stories list/create/detail/view | lecture mixte; mutations JWT |
| `ticketing` | hold/order/my tickets/scan, event types/configuration, analytics, ticket detail/QR | JWT et rôles métier |

Les appels peuvent être indirects via hooks TanStack Query et être déclenchés au montage d’écran, au refresh, après connexion, après sélection de pays, après mutation ou selon un feature flag. L’inventaire ci-dessus porte sur les appels effectivement construits dans les services API, pas sur de simples constantes non utilisées.

## 6. Inventaire endpoints backend

Le backend contient environ **40 répertoires de services détectés** par structure `src/main`; le périmètre applicatif déclaré dans la mission est d’environ 32 microservices actifs selon les profils.

Les routes backend ont été recherchées par annotations `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping` et `@DeleteMapping`, puis recoupées avec les routes Gateway. Principaux contrôleurs constatés :

- `auth-service`: `/api/v1/auth/*` pour login, register, refresh, OAuth, vérification email, password, logout et sessions.
- `country-config-service`: `/api/v1/countries*` et géographie `/api/v1/administrative-areas`, `/cities`, `/localities`.
- `place-service`: places, régions, villes, catégories, suggestions et directions.
- `event-service`: événements, inscriptions et invitations.
- `partner-service`: partenaires, artisans et profils artisan.
- `catalog-service`: assets, artworks, médias/catalogue et collections.
- `commerce-service`: orders, artwork commerce, finance partenaire et promotions.
- `content-service`: posts et stories.
- `interaction-service`: likes, favorites, comments, reviews, feedback et summaries.
- `feed-service`: feed personnel et feed public.
- `discovery-service`: recherche et cartes.
- `recommendation-service`: recommendations et adventure plans.
- `gamification-service`: xp, badges, passport, rewards et missions.
- `analytics-service`: dashboards, ticket analytics, artisan analytics et analytics partenaires.
- `messaging-service`: conversations, messages et groupes outing.
- `notification-service`: notifications, préférences et push devices.
- `media-service`: upload et consultation de médias.
- `ticket-service`: tickets, holds, orders, QR et configuration partenaire.
- `culture-service` et `graph-service`: culture et culture graph.

L’OpenAPI statique `api-gateway/src/main/resources/static/mobile-api/openapi.json` a été utilisé comme index complémentaire, mais il ne peut pas être considéré comme la preuve du runtime : plusieurs contrôleurs source récents existent alors que l’artefact OpenAPI ne reflète pas toutes les routes.

## 7. Matrice correspondance

### 7.1 Parcours critiques

| Appel mobile | Gateway | Contrôleur backend | Résultat statique |
|---|---|---|---|
| `POST /api/v1/auth/login` | route 0 → `auth-service` | `AuthController` | ALIGNÉ, public |
| `POST /api/v1/auth/register` | route 0 → `auth-service` | `AuthController` | ALIGNÉ, public |
| `GET /api/v1/countries` | route 39 → `country-config-service` | `CountryController` | ALIGNÉ, public par chaîne dédiée |
| `GET /api/v1/countries/available` | route 39 → `country-config-service` | `CountryController` | ALIGNÉ, public par chaîne dédiée |
| `GET /api/v1/cities/{id}/localities` | route 40 → `country-config-service` | `GeographyController` | ALIGNÉ, public GET |
| `GET /api/v1/feed` | route 12 → `feed-service` | `FeedController` | ALIGNÉ, JWT attendu |
| `GET /api/v1/posts/{id}` | route 10 → `content-service` | `PostController` | ALIGNÉ, lecture publique |
| `POST /api/v1/interactions/posts/summaries` | route 11 → `interaction-service` | `InteractionController` | ALIGNÉ dans le source; OpenAPI statique possiblement en retard |
| `GET /api/v1/messaging/outings/{id}/group` | route 23 → `messaging-service` | `MessagingController` | ALIGNÉ |
| `GET /api/v1/commerce/partners/{id}/finance/summary` | route 27 → `commerce-service` | `PartnerFinanceController` | ALIGNÉ |
| `GET /api/v1/me/passport/summary` | route 15 → `gamification-service` | `GamificationController` | ALIGNÉ si profil/service actif |
| `GET /api/v1/analytics/artisans/me` | route 5 → `analytics-service` | `ArtisanAnalyticsController` | ALIGNÉ, rôle PARTNER au Gateway |

### 7.2 Écarts OpenAPI versus source

La comparaison mécanique a donné 250 motifs frontend, 211 correspondances avec l’OpenAPI statique et 39 non-correspondances. Ce résultat **ne signifie pas 39 endpoints absents** : plusieurs non-correspondances sont prouvées dans les contrôleurs source, notamment artisans profile, invitations events, outing group, summaries d’interactions, finance, reviews, social, passport et adventure plans.

L’anomalie retenue est donc un risque de dérive d’artefact et non une suppression prouvée d’endpoint.

## 8. Auth/inscription

### Authentification mobile

`src/services/api/client.ts` construit la base `${API_BASE_URL}/api/v1`, lit les tokens SecureStore, ajoute le Bearer sauf pour les chemins publics, génère un `X-Correlation-ID`, puis tente une seule rotation sur 401 pour les appels authentifiés.

Le refresh utilise un client Axios brut sur `${apiBaseUrl}/auth/refresh`, évitant la récursion de l’intercepteur. En cas d’échec, la session est nettoyée et une seule redirection est déclenchée.

### Routes publiques

Le Gateway rend publics login, register, refresh, OAuth Google/Apple, vérification email, forgot/reset password. Le contrôleur `auth-service` expose ces opérations.

Le client mobile rend aussi publics les appels pays et ces mêmes routes. Le commentaire du client indique explicitement que cela évite qu’un token expiré bloque l’inscription ou la sélection du pays.

### Incident login

La route et l’autorisation sont ALIGNÉES statiquement. Le message observé peut encore provenir de :

- URL effectivement chargée différente de celle du fichier `.env`;
- DNS/TLS iOS;
- Gateway indisponible ou circuit ouvert;
- `auth-service` indisponible malgré une readiness saine;
- dépendance auth (base, email, Turnstile, Redis) en erreur;
- réponse 5xx normalisée par le mobile.

Aucune de ces causes runtime n’est confirmée par cet audit.

### Incident inscription/pays

`GET /api/v1/countries` est aligné côté Gateway, contrôleur et règle `permitAll` dédiée de `country-config-service`. Une erreur sur ce chemin pointe donc prioritairement vers la connectivité réelle, le runtime Gateway/Config Server, une réponse 5xx du service ou une URL mobile non chargée, pas vers une absence statique de route.

## 9. Countries/géographie

Le Gateway envoie `/api/v1/countries/**` à `country-config-service`, et `/api/v1/cities/*/localities` à la même famille de service. Le `place-service` possède également des contrôleurs liés à la géographie, mais le routage Gateway détermine la destination effective des appels `/countries`.

La chaîne `publicCountryReadFilterChain` de `country-config-service` cible `/api/v1/countries` et `/api/v1/countries/**`, autorise tous les GET et refuse les autres méthodes. C’est cohérent avec la préinscription.

Les autres GET géographiques `/administrative-areas`, `/cities` et `/localities` sont également autorisés dans la chaîne générale. La présence du mapping ne prouve toutefois pas la présence de données, la connexion PostgreSQL ou l’état du cache en production.

## 10. Expo Go iOS

Configuration observée :

- `src/config/env.ts` utilise `EXPO_PUBLIC_API_BASE_URL` si elle existe;
- `auto` ou une valeur absente bascule vers le host Metro, puis `127.0.0.1` sur iOS;
- en production, l’application vérifie le protocole HTTPS, le hostname `api.yeyamo.com` et le chemin WebSocket `wss://api.yeyamo.com/ws/messaging`;
- `.env` et le profil EAS production déclarent les URLs de production attendues.

Risque spécifique Expo Go iOS : si l’application observée n’a pas reçu les variables publiques de production, `127.0.0.1` désigne l’iPhone et non le PC, ce qui produit un échec réseau générique pour login et pays. Ce point est **NON VÉRIFIÉ** sans inspecter la configuration effective Metro/Expo Go de la session incidentée.

Le certificat TLS, la résolution DNS IPv4/IPv6, les restrictions réseau iOS et les éventuels proxies ne sont pas vérifiables statiquement.

## 11. Gateway

Les routes métier HTTP principales sont déclarées dans `cloud-conf-yeyamo/api-gateway.properties` :

- auth route 0;
- events route 2;
- places route 3;
- analytics route 5;
- users route 6;
- partners route 7;
- catalog route 8;
- content route 10;
- interactions route 11;
- feed route 12;
- discovery/maps route 13;
- recommendation/explore route 14;
- gamification route 15;
- moderation route 18;
- media route 19;
- notifications route 20;
- booking route 21;
- payments route 22;
- messaging route 23;
- tickets route 24;
- ads route 26;
- commerce route 27;
- culture/graph routes 30–31;
- artworks/artisan/artwork orders routes 32–36;
- countries/geography routes 39–40.

Les routes utilisent `lb://...` et un filtre CircuitBreaker avec fallback. L’API Gateway est donc dépendante du Config Server, de la registry et de la résolution de service. Le fichier local de configuration ne permet pas de prouver que la configuration active sur Dokploy est identique.

La sécurité Gateway rend publics les GET countries, places, events, media, reviews, artworks, artisans, culture et certains posts/interactions. Les mutations restent authentifiées, ce qui correspond globalement aux appels mobiles.

## 12. Erreurs HTTP

`src/services/api/errors.ts` :

- conserve le statut, code backend, correlation ID, méthode et URL;
- protège contre la remontée de stacktrace, SQL, JDBC, noms de classes Java et détails de base;
- distingue erreurs réseau sans réponse et réponses HTTP;
- traduit certains codes métier tels que country service, OTP, email et database unavailable;
- normalise la plupart des 5xx en message serveur générique.

Cette protection est correcte pour ne pas exposer d’informations internes, mais elle réduit la visibilité pendant le diagnostic de login/pays. Une réponse 502 Gateway, 503 circuit breaker, timeout, certificat et DNS peut converger vers un message utilisateur proche. Le correlation ID est disponible si le backend le renvoie dans le body/header; la continuité de bout en bout n’est pas prouvée.

## 13. Observabilité

Le mobile fournit déjà :

- `X-Correlation-ID` sur les appels Axios;
- traces DEV pour les requêtes business et médias;
- traces upload avec statut, timeout et type de transport sans token;
- traces social/feed/profile/messages dans les modules concernés;
- normalisation sans payload sensible dans les logs ciblés.

Le backend utilise des headers de correlation dans plusieurs contrôleurs et le Gateway accepte les chemins de fallback. L’audit ne prouve pas que le header est conservé par chaque hop, injecté dans MDC, renvoyé dans toutes les réponses d’erreur et indexé dans les logs de production.

Aucune nouvelle instrumentation n’est proposée en Phase 1. La Phase 2 doit exploiter la corrélation existante et ne jamais journaliser Bearer, refresh token, mot de passe, OTP, payload complet ou données personnelles non nécessaires.

## 14. Module par module

| Module | Compatibilité statique | Risque principal à confirmer |
|---|---|---|
| Auth/inscription | Alignée | URL effective, dépendances auth, 5xx masqué |
| Countries/géographie | Alignée | destination country-config, données/runtime |
| Explorer/discovery | Alignée sur Gateway/routes publiques | disponibilité discovery/recommendation |
| Places/maps | Alignée | clés/maps, permissions et service discovery |
| Feed/posts/stories | Alignée avec source | feed-service actif et réponses DTO |
| Social/interactions/reviews | Alignée avec contrôleurs source | OpenAPI obsolète, scopes JWT |
| Events/bookings | Alignée | profils experience et dépendances métier |
| Partner/artisans | Alignée | rôle partenaire et analytics |
| Finance/promotions/commerce | Alignée avec `commerce-service` | profil experience, permissions partenaire/admin |
| Culture/languages | Alignée | culture/graph service actif |
| Media | Alignée | stockage R2, multipart et timeout upload |
| Notifications/push | REST aligné; Expo runtime partiel | permission iOS, token push, provider |
| Chat/messaging | REST et route WS alignés | handshake STOMP, auth CONNECT, reconnect |
| Gamification/passport | Alignée avec source | `advanced` actif ou fonctionnalité volontairement désactivée |
| Ticketing | Routes présentes | service, QR, analytics et profil experience |
| Partner dashboard/staff | Routes présentes | rôle et `analytics-service` |

## 15. Profils Compose

`docker-compose.production.yml` utilise les profils `core`, `discovery`, `social`, `experience` et `advanced`.

- `core` : infrastructure, Gateway, auth et user.
- `discovery` : catalogue, places, discovery, media et country-config.
- `social` : content, interaction, feed, notifications et messaging.
- `experience` : partner, events, tickets, commerce, booking, payment et culture.
- `advanced` : opensearch/neo4j selon besoin, campagnes, ads, admin, recommendation, gamification, mission, referral, moderation, support, graph, analytics et ingestion.

Le fichier production contient environ 14 services explicitement `advanced`. Le mobile appelle effectivement plusieurs domaines concernés par ce profil, notamment passport/gamification, recommendations et analytics. Les feature flags peuvent empêcher certains appels, mais l’état effectif des flags et le profil activé par Dokploy sont **NON VÉRIFIÉS**.

L’override `docker-compose.production.images.yml` remplace les builds par des images `${IMAGE_REGISTRY}/${...}:${IMAGE_TAG}` et rend `IMAGE_REGISTRY`/`IMAGE_TAG` obligatoires. Sa structure couvre les services applicatifs listés, mais aucune résolution Compose réelle n’a été exécutée dans cette phase.

## 16. Régressions

Aucune modification source, configuration, Dockerfile ou contrat n’a été réalisée. Il n’existe donc pas de régression introduite par cette Phase 1.

Points à protéger lors de la Phase 2 :

- ne pas changer le préfixe `/api/v1`;
- ne pas modifier les contrats login/register/countries;
- conserver le Bearer et le refresh mono-retry;
- conserver `X-Correlation-ID` sans secret;
- préserver le timeout upload de 120 s et le timeout général de 15 s tant qu’aucune mesure ne justifie un changement;
- préserver le WebSocket `wss` production et le CONNECT STOMP authentifié;
- ne pas activer globalement `advanced` sur un VPS limité;
- ne pas modifier les profils métier pour masquer un défaut de disponibilité.

## 17. P0

### P0-NET-001 — Chemin mobile critique non confirmé depuis Expo Go iOS

- **Module :** réseau global, auth, countries.
- **Symptôme :** login indisponible et liste des pays indisponible.
- **Cause :** candidate non confirmée : URL effective Expo Go, DNS/TLS, Gateway ou service downstream.
- **Preuve :** le code statique aligne les deux chemins; `env.ts` possède toutefois un fallback loopback si les variables publiques ne sont pas injectées.
- **Fichiers/lignes :** `yeyamo-mobile/src/config/env.ts:4-16`, `yeyamo-mobile/src/services/api/client.ts:130-175`, `cloud-conf-yeyamo/api-gateway.properties:24-29`, `cloud-conf-yeyamo/api-gateway.properties:39-52`.
- **Impact :** bloque les parcours d’entrée de l’application.
- **Test de confirmation :** depuis le même iPhone/Expo Go, afficher la base URL effective non secrète, résoudre `api.yeyamo.com`, vérifier TLS puis appeler uniquement les GET publics et `POST /auth/login` avec un compte de test non destructif.
- **Correction proposée :** aucune correction Phase 1; confirmer d’abord l’environnement réellement chargé.
- **Risque de régression :** modifier l’URL sans distinguer développement, Expo Go et production peut casser les simulateurs.
- **Certitude :** symptôme confirmé par le demandeur; cause NON VÉRIFIÉE.

## 18. P1

### P1-CFG-001 — Fallback loopback dangereux sur appareil iOS réel

- **Module :** configuration Expo.
- **Symptôme :** toutes les requêtes échouent si `.env`/EAS n’est pas injecté.
- **Cause :** `env.ts` utilise `127.0.0.1` sur iOS quand la variable est absente ou `auto`.
- **Preuve :** `yeyamo-mobile/src/config/env.ts:4-16`.
- **Impact :** faux diagnostic de panne backend.
- **Test :** inspecter la valeur effective pendant la session Expo Go et la commande de lancement.
- **Correction proposée :** décider en Phase 2 si Expo Go doit exiger une URL explicite; ne pas modifier sans confirmer les environnements supportés.
- **Risque :** casser le développement local si le fallback est supprimé.
- **Certitude :** comportement confirmé; lien avec l’incident NON VÉRIFIÉ.

### P1-ERR-001 — Normalisation qui masque la classe exacte de panne

- **Module :** erreurs HTTP/mobile.
- **Symptôme :** message utilisateur générique pour réseau et plusieurs 5xx.
- **Cause :** `normalizeApiError` regroupe les erreurs sans réponse et une partie des erreurs serveur; le code est sécurisé mais peu discriminant.
- **Preuve :** `yeyamo-mobile/src/services/api/errors.ts:32-60`.
- **Impact :** diagnostic lent de 502/503/timeout/TLS/DNS et support incapable de confirmer l’origine.
- **Test :** rejouer avec erreurs réseau, 502, 503, 504 et body de fallback, puis comparer `status`, `axiosCode`, `correlationId` et message.
- **Correction proposée :** conserver les détails techniques hors UI, exposer au support une catégorie non sensible et le correlation ID.
- **Risque :** divulgation accidentelle si les champs backend sont affichés directement.
- **Certitude :** comportement confirmé.

### P1-GW-001 — Configuration Gateway active non prouvée

- **Module :** Gateway/Config Server/registry.
- **Symptôme :** une route statiquement valide peut rester indisponible au runtime.
- **Cause :** le fichier local `cloud-conf-yeyamo` n’est pas la preuve de la configuration effectivement chargée par le Config Server de production.
- **Preuve :** routes `lb://...` et CircuitBreaker dans `cloud-conf-yeyamo/api-gateway.properties:23-52`; Compose exige un Config Server et une registry.
- **Impact :** login/pays peuvent échouer malgré des contrôleurs sains.
- **Test :** lire la configuration active du déploiement sans secret, vérifier route IDs, service registry et état circuit breaker.
- **Correction proposée :** aucune avant preuve; comparer version/branche/config active.
- **Risque :** modifier le fichier local sans corriger la source réellement chargée.
- **Certitude :** risque non vérifié.

### P1-PROFILE-001 — Fonctionnalités mobiles dépendantes de profils optionnels

- **Module :** gamification, analytics, recommendations, commerce, advanced.
- **Symptôme :** écran fonctionnel mais endpoint indisponible ou circuit ouvert.
- **Cause :** services appelés par le mobile répartis dans `experience`, `discovery` et `advanced`.
- **Preuve :** `docker-compose.production.yml:254-390`; contrôleurs source finance, promotions, passport, analytics et recommendations présents.
- **Impact :** modules secondaires non fonctionnels si le profil voulu n’est pas activé.
- **Test :** comparer les profils réellement lancés avec les feature flags mobiles et les appels déclenchés par écran.
- **Correction proposée :** aligner le catalogue de fonctionnalités sur les profils déjà autorisés; ne pas activer `advanced` globalement sans capacité.
- **Risque :** OOM et dépendances supplémentaires sur le VPS.
- **Certitude :** dépendance statique confirmée; état runtime non vérifié.

### P1-CONTRACT-001 — OpenAPI mobile statique en dérive possible

- **Module :** contrat API/documentation.
- **Symptôme :** 39 non-correspondances mécaniques entre motifs frontend et OpenAPI.
- **Cause :** OpenAPI ne reflète pas certains contrôleurs présents dans le source ou les chemins dynamiques.
- **Preuve :** contrôleurs `InteractionController`, `MessagingController`, `GamificationController`, `PartnerFinanceController`, `AdventurePlanController` et autres.
- **Impact :** générateurs, audits et intégrateurs peuvent conclure à tort qu’un endpoint manque.
- **Test :** régénérer l’OpenAPI à partir des applications ou comparer chaque chemin source au document publié.
- **Correction proposée :** traiter la dérive documentaire en Phase 2, sans changer les routes métier.
- **Risque :** régénération peut révéler des contrats JSON incompatibles; validation nécessaire.
- **Certitude :** dérive probable; absence d’endpoint non prouvée.

## 19. P2

### P2-WS-001 — Handshake WebSocket non validé dynamiquement

- **Module :** messaging.
- **Symptôme :** chat REST potentiellement sain mais temps réel non connecté.
- **Cause :** compatibilité TLS, upgrade Gateway, sous-protocole STOMP, auth CONNECT et reconnexion non testées depuis iOS.
- **Preuve :** `reverb.client.ts:69-99`; Gateway route `/ws/messaging` et autorise l’upgrade avant auth STOMP.
- **Impact :** messages temps réel, compteurs et notifications de chat retardés.
- **Test :** connexion iOS avec un compte de test, vérifier HTTP 101 puis frame STOMP CONNECTED et abonnement.
- **Correction proposée :** aucune avant capture contrôlée.
- **Risque :** changement de sous-protocole ou proxy incompatible.
- **Certitude :** non vérifié.

### P2-DTO-001 — Contrats JSON non observés sur le réseau

- **Module :** tous les modules.
- **Symptôme :** un mapping de chemin peut réussir mais le rendu mobile échouer sur la forme du payload.
- **Cause :** audit statique des signatures/types, sans réponse runtime réelle.
- **Preuve :** 250 motifs frontend et contrôleurs source; aucun trafic capturé dans cette phase.
- **Impact :** erreurs de parsing, écran vide ou fallback UI malgré HTTP 2xx.
- **Test :** capturer les réponses non sensibles de login, countries, feed, profile, media et messaging puis comparer aux types mobiles.
- **Correction proposée :** établir une matrice DTO versionnée en Phase 2.
- **Risque :** exposer des données personnelles dans les captures; anonymiser.
- **Certitude :** risque structurel non vérifié.

### P2-TIMEOUT-001 — Timeouts homogènes non mesurés sur iOS

- **Module :** transport HTTP/media.
- **Symptôme :** timeout générique sur réseau mobile lent ou endpoint lourd.
- **Cause :** 15 s globales, 120 s upload; latences réelles inconnues.
- **Preuve :** `client.ts:131-133` et `media.api.ts:102`.
- **Impact :** faux échec sur recherche, feed, login lent ou media.
- **Test :** mesurer p50/p95/p99 depuis iPhone et Gateway par endpoint, sans charge massive.
- **Correction proposée :** ajuster uniquement sur mesures et par famille d’endpoint.
- **Risque :** augmenter les connexions simultanées et la pression backend.
- **Certitude :** configuration confirmée; criticité non vérifiée.

### P2-OBS-001 — Corrélation bout en bout non démontrée

- **Module :** observabilité.
- **Symptôme :** correlation ID généré côté mobile mais recherche backend potentiellement incomplète.
- **Cause :** conservation par Gateway, forwarding interservices, MDC et retour header non testés.
- **Preuve :** génération mobile `client.ts:174-175`; headers de correlation présents dans plusieurs contrôleurs.
- **Impact :** incident difficile à suivre sur plusieurs services.
- **Test :** envoyer un ID de test sur un GET public, retrouver exactement la même valeur dans Gateway et service downstream.
- **Correction proposée :** compléter la propagation existante seulement si un hop est perdu.
- **Risque :** logs trop verbeux ou fuite de données si mal filtrés.
- **Certitude :** non vérifié.

## 20. P3

### P3-UX-001 — Messages de reprise parfois trop génériques

- **Module :** écrans mobile.
- **Symptôme :** plusieurs écrans affichent uniquement « indisponible » ou « réessayer ».
- **Cause :** séparation stricte entre message sécurisé et détails techniques.
- **Preuve :** écrans feed, pays, conversations, notifications, profile et erreurs normalisées.
- **Impact :** diagnostic utilisateur limité, sans impact direct sur le contrat backend.
- **Test :** revue UX des catégories réseau, auth, permissions et service.
- **Correction proposée :** ajouter des catégories fonctionnelles non sensibles et le code de référence.
- **Risque :** messages incohérents entre modules.
- **Certitude :** comportement confirmé.

### P3-DOC-001 — Inventaires API répartis entre source, OpenAPI et services mobiles

- **Module :** gouvernance du contrat.
- **Symptôme :** même endpoint peut être documenté dans plusieurs endroits avec des états différents.
- **Cause :** génération/documentation non vérifiée dans cette phase.
- **Preuve :** 39 écarts mécaniques et chemins dynamiques.
- **Impact :** maintenance et audit futurs plus coûteux.
- **Test :** générer une vue consolidée source → Gateway → OpenAPI → mobile.
- **Correction proposée :** automatiser une vérification de dérive en CI, sans modifier les contrats existants.
- **Risque :** bruit CI si les routes dynamiques sont mal normalisées.
- **Certitude :** risque probable.

## 21. Hypothèses non confirmées

- `api.yeyamo.com` résout correctement depuis le réseau de l’iPhone.
- Le certificat TLS est accepté par la version iOS/Expo Go utilisée.
- Expo Go charge bien les valeurs production plutôt que le fallback local.
- La configuration active du Config Server correspond à `cloud-conf-yeyamo` dans la branche effectivement déployée.
- Eureka/registry contient les instances correspondant aux `lb://...`.
- Les circuits Gateway ne sont pas ouverts.
- `auth-service` et `country-config-service` sont joignables sur leurs routes métier, pas seulement `actuator/health/readiness`.
- Les profils Compose nécessaires sont actifs et les profils `advanced` non requis ne sont pas volontairement désactivés.
- Les feature flags mobiles ne déclenchent pas un endpoint d’un service absent.
- Les DTO JSON observés correspondent exactement aux types TypeScript.
- Le WebSocket termine correctement l’upgrade et accepte STOMP CONNECT.
- Les réponses d’erreur conservent le correlation ID sur tous les hops.

## 22. Vérifications dynamiques

À exécuter en Phase 2, dans cet ordre et avec un compte de test :

1. Depuis le même iPhone/Expo Go, relever l’URL effective non secrète et vérifier DNS/TLS de `api.yeyamo.com`.
2. Appeler `GET /api/v1/countries` puis `GET /api/v1/countries/available`; relever statut, headers et correlation ID.
3. Appeler `POST /api/v1/auth/login` avec des identifiants de test; relever 2xx/4xx/5xx et dépendances visibles.
4. Vérifier un appel avec token expiré pour confirmer le refresh mono-retry et l’absence de boucle.
5. Vérifier une route inexistante afin de distinguer 404 Gateway, fallback et 5xx downstream.
6. Comparer les logs Gateway, auth et country-config avec le même `X-Correlation-ID`.
7. Vérifier les profils Compose réellement actifs et les services enregistrés dans Eureka, sans activer globalement `advanced`.
8. Tester une lecture feed, une lecture profile et un appel public post pour séparer problème global et problème de module.
9. Tester WebSocket : upgrade, STOMP CONNECTED, abonnement, réception d’un événement non destructif.
10. Tester une réponse multipart/media uniquement si les étapes critiques sont saines.
11. Capturer les DTO anonymisés de countries, login, feed et profile et comparer aux types mobiles.
12. Vérifier les latences sans charge de masse et conserver les résultats par endpoint.

Tests non exécutés dans cette Phase 1 : build Maven complet, build Docker, `docker compose config`, démarrage de services, appels HTTP de production, test iOS réel, test WebSocket réel, validation Dokploy, vérification dynamique GHCR et interrogation VPS.

## 23. Plan phase 2

### Étape 1 — Confirmer le transport

Valider URL effective, DNS, TLS, reachability Gateway et headers de correlation depuis le terminal Expo Go iOS concerné.

### Étape 2 — Reproduire les deux incidents

Rejouer countries puis login avec la même version mobile et un compte de test. Conserver statut, `axiosCode`, correlation ID et timestamp.

### Étape 3 — Tracer le chemin Gateway

Comparer route active, CircuitBreaker, registry et logs downstream pour auth/country-config. Vérifier la branche/configuration réellement déployée.

### Étape 4 — Vérifier les profils

Comparer les écrans réellement activés aux profils Compose actifs; vérifier en priorité les appels analytics, gamification, recommendation et commerce.

### Étape 5 — Vérifier les contrats

Comparer DTO runtime anonymisés, types TypeScript et OpenAPI; documenter les écarts sans changer les routes par défaut.

### Étape 6 — Corriger seulement après preuve

Priorité à la configuration effective Expo/production ou au routage runtime si confirmé. Toute correction doit rester minimale, réversible et couverte par un test de non-régression.

## 24. Conclusion / niveau de confiance

**Niveau de confiance sur les correspondances statiques : élevé** pour les parcours auth, countries, Gateway, feed, messaging, finance, promotions, passport et principales routes métier, car les chemins ont été recoupés avec les contrôleurs source et la configuration Gateway.

**Niveau de confiance sur la cause des incidents : faible à moyen**, car aucune requête réelle depuis Expo Go iOS, aucun log de production et aucune configuration active Dokploy n’ont été consultés.

La conclusion opérationnelle est : **ne pas corriger les routes métier sur la seule base du message utilisateur**. Commencer par prouver l’URL effective Expo Go, la reachability HTTPS, le chemin Gateway et le correlation ID pour `countries` et `login`. Le code accessible ne montre pas d’absence statique de ces deux endpoints.

**Phase 1 terminée sans modification source/configuration et sans déploiement.**
