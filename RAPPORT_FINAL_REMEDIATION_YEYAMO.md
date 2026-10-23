# Rapport final de remédiation Yeyamo

Date : 2026-10-23  
Périmètre : `E:\Daryl\yeyamo-api` et validations locales ciblées de `E:\Daryl\yeyamo-mobile`

## 1. Synthèse exécutive

La remédiation locale couvre les contrôles P0 d’authentification, JWT, OTP, refresh token, Graph, Ads/Feed, session mobile, recommandations et chaîne Docker/CI. Aucun déploiement, accès VPS, publication d’image, migration de base, commit ou push n’a été effectué.

Le blocage Docker confirmé n’est plus présent dans les Dockerfiles actifs : `deploy/service.Dockerfile:76` et `deploy/runtime.Dockerfile:20` utilisent la syntaxe Dockerfile `CMD`, et non la syntaxe Compose `CMD-SHELL`.

## 2. Baseline Git et conservation

- Le dépôt backend était déjà sale avant cette passe : suppressions documentaires, modifications de services/tests, artefacts d’audits et script GHCR existants ont été conservés.
- Les modifications mobile préexistantes ont été conservées ; seules les corrections de session/recommandation de cette mission ont été maintenues en plus.
- Aucun rapport existant n’a été écrasé ni supprimé. Ce fichier est le seul nouveau rapport documentaire demandé.
- `git diff --check` backend et mobile : succès.

## 3. Corrections Auth / JWT

- `auth-service/src/main/java/com/yeyamo_mobile/api/auth_service/service/AuthService.java:363` centralise l’éligibilité : compte `ACTIVE` obligatoire ; un compte avec email exige `emailVerifiedAt` ; le compte téléphone seul reste compatible ; OAuth conserve sa confiance fournisseur.
- `auth-service/src/main/java/com/yeyamo_mobile/api/auth_service/security/JwtService.java:98` valide la signature/claims parsés, le sujet numérique, le type `access`, le statut `ACTIVE`, ainsi que `email_verified` pour les tokens email.
- `auth-service/src/main/java/com/yeyamo_mobile/api/auth_service/security/JwtAuthenticationFilter.java:40` recharge l’utilisateur depuis la base et refuse un compte désactivé, non actif ou email non vérifié avant de créer le `SecurityContext`.
- Les réponses d’erreur restent génériques côté filtre ; aucun détail interne n’est exposé.

## 4. Matrice JWT

| Cas | Résultat |
|---|---|
| Email + `ACTIVE` + vérifié | accepté |
| Email + non vérifié | refusé |
| Email + statut non `ACTIVE` | refusé |
| Téléphone seul, ancien token sans claims email | accepté si compte live actif |
| Type de token différent de `access` | refusé |
| Signature/issuer/audience/expiration invalides | refusé par le parseur strict |

Le starter commun applique aussi `JwtAccountEligibilityValidator` (`security-hardening-starter/src/main/java/com/yeyamo/security/hardening/JwtAccountEligibilityValidator.java:15`) aux ressources JWT concernées.

## 5. OTP et refresh

- `auth-service/src/main/java/com/yeyamo_mobile/api/auth_service/service/OtpService.java:21` remplace les opérations Redis séparées par un script Lua atomique : incrément, TTL des tentatives, contrôle du hash, suppression one-shot et limite d’essais.
- `auth-service/src/main/java/com/yeyamo_mobile/api/auth_service/service/RefreshTokenService.java:92` reçoit le contrôle d’éligibilité et l’exécute avant révocation/création du nouveau token (`:104`), supprimant le risque de refresh orphelin.
- Tests dédiés : `OtpServiceAtomicTests` couvre succès, OTP invalide, expiré et limite ; `RefreshTokenServiceSecurityTests` couvre l’absence de sauvegarde avant rejet.

## 6. Graph et contrats Ads/Feed

- `graph-service/pom.xml` dépend du starter commun.
- `graph-service/src/main/java/com/yeyamo_mobile/api/graph_service/config/GraphSecurityConfig.java:39` utilise `StrictJwtDecoders` sans modifier les routes publiques ni les rôles admin.
- `ads-delivery-service/src/main/java/com/yeyamo_mobile/api/ads_delivery_service/application/AdDeliveryService.java:327` normalise le creative JSON avec Jackson ; les JSON invalides ou incomplets retournent une structure contrôlée `media=[]`, `caption=null`, `ctaLabel=Découvrir`, sans champ `{raw}`.
- Le `partnerId` est propagé de la campagne vers la réponse Ads, puis Feed et mobile, sans remplacer le flux organique.
- `SponsoredCreativeContractTests` valide la normalisation et le fallback contrôlé.

## 7. Session mobile et résilience

- `E:\Daryl\yeyamo-mobile\src\features\auth\auth.service.ts:38` exige désormais une réponse serveur éligible avant persistance ; le cache offline ne restaure pas un utilisateur email non vérifié.
- `E:\Daryl\yeyamo-mobile\src\app\_layout.tsx:107` invalide la session backend devenue inéligible et protège les routes privées avec la même condition.
- Les recommandations restent optionnelles : `E:\Daryl\yeyamo-mobile\src\features\recommendations\recommendations.api.ts:28` trace uniquement un événement DEV sûr, retourne un fallback vide et `recommendations.hooks.ts:12` désactive les retries automatiques.
- Aucun nouveau SDK ni changement de contrat API n’a été introduit.

## 8. Docker et readiness

- `deploy/service.Dockerfile:65` et `deploy/runtime.Dockerfile:11` installent effectivement `curl` avant passage à l’utilisateur `spring`.
- `deploy/service.Dockerfile:74` / `deploy/runtime.Dockerfile:18` définissent `SERVER_PORT` depuis `SERVICE_PORT`; `EXPOSE`, entrypoint, JVM, mémoire et ports sont conservés.
- Les healthchecks utilisent `CMD curl ... /actuator/health/readiness` avec les valeurs inchangées : intervalle 15 s, timeout 5 s, start-period 60 s, retries 10.
- Scan statique : 46 Dockerfiles, 43 healthchecks, 0 occurrence `HEALTHCHECK ... CMD-SHELL`.
- Aucun des 36 Dockerfiles historiques n’a été massivement modifié.

## 9. CI, Compose et GHCR

- `docker-compose.yml`, `docker-compose.production.yml` et leur override images passent `docker compose config -q` avec variables factices non sensibles.
- La matrice `.github/workflows/build-images.yml:45` contient 34 services uniques ; `docker-compose.production.images.yml:7` contient les mêmes 34 services uniques : aucune omission, addition ou duplication détectée.
- Le script `scripts/verify-image-contract.sh` est cohérent avec cette comparaison.
- Le script Bash n’a pas pu être exécuté dans cet environnement Windows/WSL : `Bash/Service/CreateInstance` retourne `E_ACCESSDENIED`.

## 10. Tests exécutés

- Starter sécurité : 19 tests passés lors de la passe Maven ciblée.
- Auth : 61 tests passés lors de la passe Maven ciblée, incluant JWT, filtre, session et refresh.
- OTP + Ads creative : 4 tests ciblés passés.
- Graph strict decoder : 4 tests ciblés passés.
- Feed : compilation Maven passée ; aucune modification de logique organique détectée.
- API Gateway et User : compilation Maven passée avec JVM bornée (`MAVEN_OPTS`, compilateur non forké).
- Mobile TypeScript : `npm exec -- tsc --noEmit` passé.
- Mobile ESLint : passe complète initiale terminée avec 0 erreur et 3 warnings ; le fichier mobile modifié passe aussi le lint ciblé. Une seconde passe complète a atteint une limite mémoire native.
- `git diff --check` backend/mobile : passé.

## 11. Tests non exécutés ou limités

- Build Docker réel et exécution d’un healthcheck : non validés, le daemon Docker Linux est indisponible (`dockerDesktopLinuxEngine` absent) et BuildKit est bloqué par un verrou local inaccessible.
- Le contexte Spring Ads complet n’a pas terminé dans la fenêtre de validation ; la compilation et les tests unitaires ciblés sont passés.
- Le script Bash GHCR n’a pas été exécuté pour la raison Windows/WSL indiquée ci-dessus ; le contrôle équivalent 34/34 a été exécuté en PowerShell.
- Aucun test mobile E2E ou appareil réel n’a été lancé.

## 12. Matrice de non-régression et risques résiduels

| Surface | Statut |
|---|---|
| Compilation Maven ciblée | OK |
| Copie JAR / `app.jar` | inchangée, syntaxiquement conservée |
| ENTRYPOINT / JVM | inchangé |
| Ports / `SERVER_PORT` | conservés |
| Utilisateur non-root | conservé |
| Readiness Spring Boot | endpoint conservé |
| Variables et chemins | conservés |
| Contrats API / métier / DB | non modifiés |
| Dokploy / build Docker réel | à confirmer par CI ou daemon Linux |

Risque résiduel principal : la construction Docker et le healthcheck doivent encore être validés dans un daemon Linux ou la CI Dokploy. Les tests d’intégration dépendant de services externes, du registry, de Redis réel ou d’un appareil mobile restent hors périmètre local.

## 13. Décision et checklist pré-déploiement

**GO local pour la correction et la revue de code. NO-GO pour un déploiement immédiat tant que la validation Docker Linux et le script Bash CI ne sont pas passés.**

Avant déploiement contrôlé :

1. Lancer `docker compose -f docker-compose.production.yml -f docker-compose.production.images.yml config -q` dans l’environnement CI avec secrets injectés.
2. Lancer `bash scripts/verify-image-contract.sh` dans Linux/CI.
3. Construire une image représentative avec `deploy/service.Dockerfile` et vérifier `/actuator/health/readiness` avec l’utilisateur `spring`.
4. Vérifier les variables `SERVICE_MODULE`, `SERVICE_PORT`, `JWT_SECRET`, issuer/audience et secrets registry dans l’environnement cible.
5. Rejouer les tests backend ciblés et les contrôles mobile dans la CI avant toute promotion.

**Aucun déploiement — aucun commit — aucun push — aucune publication d’image effectués.**
