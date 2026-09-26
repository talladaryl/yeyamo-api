# YeYamo — audit d'authentification administrateur de production

Date de l'audit : 2026-09-26
Périmètre : `yeyamo-api`, `yeyamo-admin`, `https://api.yeyamo.com`

## 1. Executive summary

Le mécanisme d'authentification réellement utilisé est porté par `auth-service`.
Un administrateur est un utilisateur de `yeyamo_auth.users` auquel est associé un
rôle dans `yeyamo_auth.user_roles`, en particulier `SUPER_ADMIN` ou `ADMIN`.
Les mots de passe sont encodés par le bean Spring `BCryptPasswordEncoder(12)` et
jamais stockés en clair par le flux normal.

Le dépôt contient un mécanisme prévu pour créer le premier administrateur :
`LocalAdminBootstrap`. Il est désactivé par défaut et crée/actualise un compte
`SUPER_ADMIN` avec les variables `YEYAMO_BOOTSTRAP_ADMIN_*` au démarrage de
`auth-service`.

Je n'ai créé aucun compte et je n'ai exécuté aucune écriture SQL. La composition
décrit bien la cible logique de production, mais cette session n'a ni accès au
contexte Dokploy de production, ni accès à son daemon Docker, ni variables de
déploiement permettant de prouver et d'atteindre la base concernée. Créer un
compte dans ces conditions risquerait de viser une base locale ou un mauvais
environnement.

`api.yeyamo.com` est joignable. Le vrai `POST /api/v1/auth/login` a atteint
l'application et a retourné `400` pour un corps volontairement invalide ; les
principales routes du dashboard ont retourné `401` sans Bearer token, ce qui est
le comportement attendu. Les tests avec un vrai administrateur ne peuvent pas
être exécutés sans provisionnement sûr préalable.

Le domaine de dashboard attendu par la composition, `https://admin.yeyamo.com`,
ne se résout pas publiquement au moment de l'audit. Même après création du
compte, ce déploiement DNS/application doit donc être disponible pour une
connexion navigateur réelle.

## 2. Backend authentication architecture

| Élément | Implémentation réelle |
| --- | --- |
| User entity | `auth-service/.../models/User.java` |
| User table | `yeyamo_auth.public.users` |
| Identifier de connexion | `LoginRequest.identifier`, recherché d'abord dans `email`, puis `phone` ; les deux sont normalisés en minuscules lors du login email/téléphone |
| Password storage | `users.password_hash` (`VARCHAR(255)`) |
| PasswordEncoder | bean `BCryptPasswordEncoder(12)` dans `auth-service/.../security/SecurityConfig.java` |
| Admin role | enum `Roles.ADMIN` et `Roles.SUPER_ADMIN` ; le bootstrap attribue `USER` + `SUPER_ADMIN` |
| Role persistence | `roles.code` et jointure `user_roles(user_id, role_id)` |
| Account status | enum `UserStatus`; le login exige un compte actif (un `PENDING` est refusé, un `INACTIVE` est réactivé seulement après vérification du mot de passe) |
| Login endpoint | `POST /api/v1/auth/login` |
| Refresh endpoint | `POST /api/v1/auth/refresh`, avec rotation de refresh token stocké sous hash dans `refresh_tokens.token_hash` |
| JWT admin claim/authority | claims `roles`, `scope`/`scopes`, `permissions`; les consommateurs convertissent `ADMIN` en `ROLE_ADMIN` et `SUPER_ADMIN` en `ROLE_SUPER_ADMIN` |
| Admin authorization mechanism | Spring Security au Gateway, puis Resource Server/Spring Method Security dans les services cibles |

Flux observé :

```text
yeyamo-admin
  -> POST /api/auth/login (route Next.js)
  -> POST https://api.yeyamo.com/api/v1/auth/login
  -> API Gateway (route auth-service)
  -> auth-service / AuthenticationManager / BCryptPasswordEncoder
  -> yeyamo_auth.users + user_roles
  -> JWT HS256 + refresh token
  -> cookies HttpOnly de yeyamo-admin
  -> /api/backend/* avec Authorization: Bearer <access token>
```

Le JWT est signé HS256, contient notamment `sub` (l'identifiant numérique de
`users`), `roles`, `scope`, `scopes`, `permissions`, `email`, `phone` et
`country`, ainsi que `iss` et `aud`. Aucun mot de passe ni hash n'est mis dans le
jeton.

## 3. Admin role model

Les rôles auth disponibles sont : `USER`, `PARTNER`, `ADMIN`, `SUPER_ADMIN`,
`MODERATOR`, `EDITOR`, `SUPPORT`, `COMMERCIAL`.

`UserPrincipal` transforme les rôles en autorités Spring préfixées par `ROLE_`.
Le Gateway fait la même conversion depuis le claim JWT. Les principales
autorisations réelles sont :

| Backend endpoint | Required authority |
| --- | --- |
| `GET /api/v1/admin/platform-users` | `ADMIN`, `SUPER_ADMIN` ou `SUPPORT` |
| `PATCH /api/v1/admin/platform-users/{id}/status` | `ADMIN` ou `SUPER_ADMIN` |
| `PATCH /api/v1/admin/platform-users/{id}/roles` | `SUPER_ADMIN` (mais cette API refuse explicitement tout rôle autre que `USER` ou `PARTNER`) |
| `GET/POST /api/v1/admin/users` | lecture `ADMIN`/`SUPER_ADMIN`; création `SUPER_ADMIN` |
| `GET /api/v1/admin/audit-logs` | `ADMIN` ou `SUPER_ADMIN` |
| `GET/PATCH /api/v1/admin/reports/**` | `MODERATOR`, `ADMIN` ou `SUPER_ADMIN` |
| `GET /api/v1/admin/events/**` | `EDITOR`, `ADMIN` ou `SUPER_ADMIN` |
| `GET /api/v1/admin/partners/**` | `SUPPORT`, `ADMIN` ou `SUPER_ADMIN` (KYC : `ADMIN`/`SUPER_ADMIN`) |
| `GET /api/v1/admin/campaigns/**` | JWT scope `campaign:approve` ou `campaign:reject` selon l'action |
| `GET /api/v1/analytics/**` | Gateway : `ADMIN`, `SUPER_ADMIN` ou `MODERATOR`; les contrôleurs restreignent certaines opérations à `ADMIN`/`SUPER_ADMIN` |

`ADMIN` reçoit les permissions `admin:read` et `users:suspend`. `SUPER_ADMIN`
reçoit en plus `admin:manage` et `finance:adjust`. Les deux reçoivent les scopes
de campagne nécessaires à la lecture, création, mise à jour, soumission, pause,
approbation et rejet.

### Attention : deux modèles « administrateur » différents

`admin-service.admin_users` est une entité de métadonnées/gouvernance distincte.
Elle utilise un `user_id UUID` et un enum `AdminRole`; elle n'est ni lue par
`auth-service` pendant le login, ni incluse dans le JWT. À l'inverse,
`auth-service.users.id` est un `Long` et ses rôles sont dans `user_roles`.

Par conséquent, `POST /api/v1/admin/users` ne doit **pas** être utilisé pour
bootstrapper un compte qui doit se connecter. L'écran « Administrateurs » du
dashboard appelle actuellement cette API de métadonnées et ne peut pas, à lui
seul, rendre un utilisateur authentifiable comme administrateur. C'est un
écart structurel à traiter dans une passe dédiée ; il ne peut pas être corrigé
par une simple insertion de rôle sans définir une source d'identité unique.

## 4. yeyamo-admin authentication flow

| Étape | Fichier | Comportement |
| --- | --- | --- |
| Page login | `app/(auth)/admin/login/page.tsx` | affiche le formulaire administrateur |
| Formulaire | `components/admin-login-form.tsx` | envoie `identifier` et `password` vers `/api/auth/login` |
| Route serveur login | `app/api/auth/login/route.ts` | appelle `${backendBaseUrl()}/api/v1/auth/login` en `POST` |
| Contrôle d'accès | même route | refuse la session si `auth.user.roles` ne contient aucun rôle privilégié |
| Rôles admis côté dashboard | `lib/server/backend.ts` | `SUPER_ADMIN`, `ADMIN`, `MODERATOR`, `EDITOR`, `SUPPORT`, `COMMERCIAL` |
| Cookies | `lib/server/backend.ts` | access token et refresh token `HttpOnly`, `Secure` en production, `SameSite=Lax`/`Strict`, `Path=/` |
| Session | `app/api/auth/session/route.ts` | restitue uniquement l'identité/rôles/permissions/scopes conservés dans le cookie HttpOnly |
| Proxy API | `app/api/backend/[...path]/route.ts` | lit le cookie access token et transfère `Authorization: Bearer ...` au backend |
| 401 | `lib/api/client.ts` | tente une fois `/api/auth/refresh`, puis redirige vers `/admin/login` |
| 403 | `lib/api/client.ts` | remonte l'erreur API; aucun contournement de rôle |

Les cookies ne définissent pas de `Domain`, donc ils restent host-only, ce qui
est adapté à l'application admin. Le dashboard ne met pas le JWT dans le
JavaScript client : le proxy Next.js l'utilise depuis un cookie `HttpOnly`.

## 5. Production API configuration

La résolution backend du dashboard est, par ordre de priorité :

```text
API_BASE_URL
API_URL
NEXT_PUBLIC_API_URL
NEXT_PUBLIC_API_BASE_URL
http://localhost:8083 (fallback de développement uniquement)
```

Constats :

| Emplacement | Valeur/effet constaté | Classification |
| --- | --- | --- |
| `yeyamo-admin/.env` local | `API_BASE_URL=https://api.yeyamo.com` et `NEXT_PUBLIC_API_BASE_URL=https://api.yeyamo.com` | DEV local aligné production |
| `yeyamo-admin/docker-compose.production.yml` | `API_BASE_URL` et `NEXT_PUBLIC_API_URL` par défaut à `https://api.yeyamo.com` | PRODUCTION_ACTIVE par défaut |
| même composition | `NEXT_PUBLIC_APP_URL` par défaut à `https://admin.yeyamo.com` | PRODUCTION_ACTIVE par défaut |
| `lib/server/backend.ts` | fallback `http://localhost:8083` si toutes les variables sont absentes | DEV_ONLY fallback |

Conclusion de configuration :

```text
YEYAMO_ADMIN_PRODUCTION_API =
https://api.yeyamo.com
```

Cette valeur peut néanmoins être remplacée au runtime par une variable du
déploiement. Les variables du déploiement de `yeyamo-admin` ne sont pas
accessibles dans cette session; le code et les valeurs par défaut sont alignés.

Le test DNS public de `admin.yeyamo.com` a échoué (nom non résolu). Ce domaine
doit être créé/pointé et le conteneur admin déployé avant de pouvoir tester la
page de login dans un navigateur réel.

## 6. Database target

```text
TARGET_ENVIRONMENT = production composition intended by docker-compose.production.yml,
                     but runtime deployment not accessible from this session
TARGET_DATABASE    = PostgreSQL / yeyamo_auth
TARGET_SCHEMA      = public
TARGET_USER_TABLE  = users
ADMIN_ROLE_STORAGE = roles + user_roles
DATABASE_WRITE     = NOT_EXECUTED
```

La composition de production relie `auth-service` à `postgres:5432/yeyamo_auth`
par le réseau Docker interne. PostgreSQL n'expose aucun port public dans cette
composition. Le daemon Docker local est indisponible et aucun contexte Docker
ou accès Dokploy distant n'a été fourni; il serait donc incorrect de prétendre
que cette session vise la base de production.

Les migrations pertinentes sont :

* `auth-service/src/main/resources/db/migration/V1__create_auth_schema.sql`
  (`users`, `roles`, `user_roles`, `refresh_tokens`),
* `V2__admin_platform_users.sql` et `V3__admin_user_search_indexes.sql` pour la
  consultation des comptes par le dashboard.

## 7. Admin provisioning method

La méthode correcte existante est
`auth-service/.../config/LocalAdminBootstrap.java` :

* elle est conditionnée par `yeyamo.bootstrap-admin.enabled=true`;
* Docker la mappe sur `YEYAMO_BOOTSTRAP_ADMIN_ENABLED`;
* elle requiert un email et un mot de passe d'au moins 12 caractères;
* elle utilise le vrai bean `PasswordEncoder` (`BCryptPasswordEncoder(12)`);
* elle marque le compte `ACTIVE`, vérifie son email, et ajoute `USER` et
  `SUPER_ADMIN` depuis la table `roles`;
* elle est exécutée après `RoleSeeder`, qui garantit les rôles requis.

Procédure à exécuter **dans le projet Dokploy/API réellement associé à
api.yeyamo.com**, jamais dans un fichier versionné :

1. Générer localement un mot de passe temporaire aléatoire de 24 caractères ou
   plus, sans le mettre dans Git, dans un rapport ou dans `.env.example`.
2. Dans les variables runtime du déploiement de production, définir
   `YEYAMO_BOOTSTRAP_ADMIN_ENABLED=true`,
   `YEYAMO_BOOTSTRAP_ADMIN_EMAIL=<email-admin-dédié>`, et
   `YEYAMO_BOOTSTRAP_ADMIN_PASSWORD=<mot-de-passe-temporaire>`.
3. Redéployer/redémarrer seulement `auth-service`. Le runner créera ou mettra à
   jour ce compte dans **la base du conteneur auth-service réellement déployé**.
4. Vérifier un login par `POST https://api.yeyamo.com/api/v1/auth/login`, puis
   vérifier un endpoint admin avec le Bearer token retourné.
5. Retirer immédiatement les trois variables ou remettre
   `YEYAMO_BOOTSTRAP_ADMIN_ENABLED=false`, puis redémarrer le service. Cette
   dernière étape est obligatoire : tant que le bootstrap reste activé, un
   nouveau démarrage réencode et remplace le mot de passe du compte ciblé.

Une insertion SQL directe n'est ni nécessaire ni recommandée ici. Elle
contournerait le mécanisme applicatif testé et nécessiterait de gérer à la main
le hash BCrypt, les rôles et les contraintes de la base.

## 8. Account creation result

```text
ADMIN ACCOUNT NOT CREATED
```

Raison : aucune preuve d'accès à la base/au runtime qui sert
`https://api.yeyamo.com`. Les variables de bootstrap de la composition sont
présentes mais restent désactivées par défaut; leurs valeurs effectives dans
Dokploy ne sont pas accessibles. Aucun identifiant n'est donc inventé et aucun
mot de passe temporaire n'est exposé.

## 9. Authentication test

Tests HTTPS exécutés sans identifiants ni écriture :

| Cible | Résultat | Interprétation |
| --- | --- | --- |
| `GET https://api.yeyamo.com/actuator/health` | `200` | Gateway joignable |
| `POST https://api.yeyamo.com/api/v1/auth/login` avec `{}` | `400` | endpoint réel joignable; validation du DTO exécutée avant toute authentification |
| `GET https://api.yeyamo.com/api/v1/auth/login` | `503` | méthode non utilisée par le dashboard; ne remplace pas le test `POST` concluant |

Un vrai login, la rotation refresh, le contenu `roles` du JWT et le hash d'un
compte de production ne sont pas testables sans compte créé dans la cible.

## 10. Authorization test

Les appels sans Bearer token ci-dessous ont tous retourné `401`, résultat
attendu pour des routes protégées :

```text
/api/v1/admin/platform-users
/api/v1/admin/users
/api/v1/admin/events
/api/v1/admin/partners
/api/v1/admin/support/conversations
/api/v1/admin/notifications
/api/v1/admin/newsletters
/api/v1/admin/campaigns
/api/v1/admin/culture/contents
/api/v1/analytics/admin/dashboard
/api/v1/booking-management/bookings
/api/v1/payments/admin
/api/v1/commerce/admin/promotions
/api/v1/catalog/assets
```

Cela confirme que le Gateway ne rend pas ces APIs publiques. Cela ne prouve pas
une autorisation admin positive, qui exige un jeton `ADMIN` ou `SUPER_ADMIN`
valide. Aucun `403`, `404` ou `5xx` n'a été masqué comme un succès.

## 11. Dashboard API matrix

| Dashboard feature | Client admin | HTTP | Backend controller/service | Gateway | Production test |
| --- | --- | --- | --- | --- | --- |
| Login/refresh/logout | routes `app/api/auth/*` | POST | `AuthController` / auth-service | `auth-service` | Login invalide `400`; vrai login `NOT_TESTED` |
| Platform users | `platformUsersApi` | GET/PATCH/POST | `AdminPlatformUserController` / auth-service | `auth-service-admin-platform-users` (ordre `-110`) | `401_EXPECTED` |
| Administrators metadata | `administratorsApi` | GET/POST/PUT | `AdminUserController` / admin-service | `admin-service` | `401_EXPECTED`; ne provisionne pas l'identité auth |
| Audit, reports, validations | `adminApi`, settings/partners features | GET/POST/PATCH | `AuditController`, `ReportController`, `ValidationController` / admin-service | `admin-service` | routes protégées; non testées avec admin |
| Events | `eventsApi` | GET/POST/PUT/PATCH | `AdminEventController` / event-service | `event-service` (ordre `-100`) | `401_EXPECTED` |
| Partners/KYC | `partnersApi` | GET | `AdminPartnerController` / partner-service | `partner-service` (ordre `-100`) | `401_EXPECTED` |
| Support | `supportApi` | GET/POST/PATCH | `SupportAdminController` / support-service | `support-service` (ordre `-100`) | `401_EXPECTED` |
| Places/geography | `geographyApi` | GET/POST/PUT | place, region, city, district, category controllers / place-service | `place-service` (ordre `-100` sur paths admin) | accès protégé/public selon méthode; non testé avec admin |
| Catalog/collections/import | `catalogApi` | GET/POST/PUT/PATCH/DELETE | catalog + ingestion controllers | `catalog-service`, `ingestion-service` | `401_EXPECTED` sur `catalog/assets` via proxy admin |
| Moderation/trust | `moderationApi` | GET/POST | moderation-trust controllers | `moderation-trust-service` | non testé avec admin |
| Notifications/newsletter | features dédiées | GET/POST/PATCH/PUT | notification admin controllers | `notification-service` (ordre `-100`) | `401_EXPECTED` |
| Campaigns | `campaignsApi` | GET/POST | `AdminCampaignController` / campaign-service | `campaign-service` (ordre `-100`) | `401_EXPECTED` |
| Reservations | `reservationsApi` | GET/POST | booking controllers | `booking-service` | `401_EXPECTED` |
| Payments/commerce | `financeApi` | GET/POST/PUT | payment and commerce admin controllers | `payment-service`, `commerce-service` | `401_EXPECTED` |
| Analytics | `analyticsApi` | GET/POST | analytics controllers | `analytics-service` | `401_EXPECTED` |
| Missions/gamification | feature APIs | GET/POST | mission-reward and gamification controllers | corresponding Gateway routes | non testé avec admin |

## 12. Gateway matrix

| Route | Gateway | Service | Controller | Result |
| --- | --- | --- | --- | --- |
| `/api/v1/auth/**` | `auth-service` | auth-service | `AuthController` | `POST /login` reached in production (`400` invalid payload) |
| `/api/v1/admin/platform-users/**` | `auth-service-admin-platform-users`, order `-110` | auth-service | `AdminPlatformUserController` | Static alignment; `401_EXPECTED` without token |
| `/api/v1/admin/events/**` | `event-service`, order `-100` | event-service | `AdminEventController` | Static alignment; `401_EXPECTED` |
| `/api/v1/admin/partners/**` | `partner-service`, order `-100` | partner-service | `AdminPartnerController` | Static alignment; `401_EXPECTED` |
| `/api/v1/admin/support/**` | `support-service`, order `-100` | support-service | `SupportAdminController` | Static alignment; `401_EXPECTED` |
| `/api/v1/admin/notifications/**`, `/newsletters/**` | `notification-service`, order `-100` | notification-service | admin notification/newsletter controllers | Static alignment; `401_EXPECTED` |
| `/api/v1/admin/campaigns/**` | `campaign-service`, order `-100` | campaign-service | `AdminCampaignController` | Static alignment; `401_EXPECTED` |
| Other `/api/v1/admin/**` | `admin-service` | admin-service | governance, users, reports, moderation, validations | Static alignment; `401_EXPECTED` |

La règle générale `admin-service` n'écrase pas les routes spécialisées : elles
ont explicitement des ordres négatifs (`-100` à `-120`, `-110` pour
`platform-users`) tandis que la règle générale est à l'ordre par défaut. Aucune
correction de Gateway n'est donc justifiée sur ce point.

## 13. Production api.yeyamo.com tests

| Classement | Résultat |
| --- | --- |
| `PASS` | Gateway health `200`; CORS preflight de login et d'admin `204`; login invalide `400` |
| `401_EXPECTED` | les principales routes dashboard listées en section 10 sans token |
| `403` | aucun observé dans les tests sans token |
| `404` | aucun observé dans les tests sans token |
| `CONTRACT_MISMATCH` | écran Administrateurs/admin-service ne crée pas une identité auth loggable |
| `SERVER_ERROR` | aucun pour les méthodes réellement utilisées; `GET /api/v1/auth/login` a donné `503`, mais cette méthode n'est pas le contrat de login |
| `NOT_TESTED` | routes avec Bearer `ADMIN`/`SUPER_ADMIN`, faute de compte production provisionné de manière sûre |

## 14. CORS/token findings

Le Gateway configure `security.cors.allowed-origins` et `allowCredentials=true`
dans son code. En production, les preflights depuis `https://admin.yeyamo.com`
ont renvoyé `204` avec :

```text
Access-Control-Allow-Origin: https://admin.yeyamo.com
```

Le header `Access-Control-Allow-Credentials` n'était pas présent dans la réponse
observée. Le dashboard admin appelle toutefois l'API depuis ses routes serveur
Next.js avec un Bearer token, pas directement depuis le navigateur; cette
absence ne bloque pas le flux actuel proxyfié, mais doit être vérifiée au niveau
du reverse proxy si un appel navigateur avec credentials est prévu.

Le token est stocké dans un cookie `HttpOnly` côté dashboard et transmis par le
proxy avec `Authorization: Bearer ...`. Le refresh traite le `401`, réévalue les
rôles privilégiés, et efface les cookies en cas d'échec. Aucun bypass de rôle,
`permitAll`, ni rôle codé en dur n'a été ajouté.

## 15. Corrections performed

Aucune modification métier ou de sécurité n'a été appliquée :

* l'URL de production est déjà correctement câblée dans le fichier d'environnement
  local et dans les défauts de la composition de production;
* les routes Gateway spécialisées ont déjà une priorité correcte;
* créer un pseudo-compte dans `admin-service.admin_users` ou insérer un hash SQL
  aurait créé un compte non authentifiable, donc cette action a été refusée;
* ce rapport est le seul fichier créé par cet audit.

## 16. Remaining blockers

1. Accès au runtime Dokploy/production absent : impossible de lancer le
   bootstrap contre la base réellement servie par `api.yeyamo.com`.
2. `admin.yeyamo.com` ne résout pas au moment du test : aucun login dashboard
   navigateur réel ne peut être validé.
3. La gestion des administrateurs de `admin-service` n'est pas la source des
   rôles de `auth-service`; elle ne peut pas provisionner un compte de connexion.
4. Les tests avec un vrai Bearer token ne peuvent pas être faits avant le
   bootstrap. Après bootstrap, ils doivent inclure `GET /api/v1/auth/me` et
   `GET /api/v1/admin/platform-users`.
5. Le déploiement de `admin.yeyamo.com` et le provisionnement restent nécessaires
   pour exécuter les tests avec un vrai Bearer token de production.

### Validation locale complémentaire

Le 2026-09-26, les tests ciblés suivants ont été exécutés avec succès :

```text
mvn -pl auth-service \
  -Dtest=RoleAuthoritiesTests,AdminPlatformUserServiceTests,JwtServiceCountryClaimTest test
```

Résultat : `9 tests`, `0 failures`, `0 errors`, `BUILD SUCCESS`.
Ils valident notamment la génération du claim JWT `country`, les scopes et
permissions de rôles, ainsi que les protections de l'API de gestion des comptes
plateforme. Ils ne remplacent pas un test de connexion contre le runtime de
production.

## 17. Final verdict

```text
ADMIN_ACCOUNT =
NOT_CREATED

ADMIN_PASSWORD_HASH =
NOT_VERIFIED

ADMIN_LOGIN_API =
NOT_TESTED

ADMIN_AUTHORIZATION =
NOT_TESTED

YEYAMO_ADMIN_API_BASE_URL =
https://api.yeyamo.com

API_YEYAMO_COM_REACHABLE =
YES

ADMIN_API_GATEWAY_ALIGNMENT =
PARTIAL

ADMIN_FRONTEND_BACKEND_ALIGNMENT =
PARTIAL

CORS =
PARTIAL

READY_TO_LOGIN_ADMIN_DASHBOARD =
NO
```

La chaîne de code attendue est bien identifiée :

```text
auth users/user_roles -> BCrypt authentication -> JWT roles -> Gateway/service role checks
-> yeyamo-admin HttpOnly session -> protected dashboard API
```

Elle ne peut pas encore être prouvée de bout en bout en production car le
compte n'a volontairement pas été créé sans accès prouvé à la cible, et le
dashboard public attendu ne résout pas.
