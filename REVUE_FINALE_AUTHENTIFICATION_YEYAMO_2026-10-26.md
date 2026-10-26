# Revue finale de l'authentification Yeyamo — 2026-10-26

## A. Résumé exécutif

Revue locale uniquement des dépôts \`yeyamo-api\` et \`yeyamo-mobile\`. Aucun déploiement, redémarrage, accès production, accès à un secret réel, push ou commit n'a été effectué.

La revue a confirmé trois défauts résiduels et les a corrigés :

1. Le parcours partenaire appelait \`/partners\` avant la confirmation OTP, alors que le compte \`PENDING\` ne possédait pas encore de session.
2. Le client mobile effaçait la session pour toute erreur de refresh, y compris une coupure réseau temporaire.
3. Le décodeur JWT interservices acceptait un jeton signé sans claim \`sub\`.

Les validations ciblées sont vertes. La décision reste \`INCOMPLETE\` : les tests E2E mobile, les tests WebSocket réels et un build Docker réel n'ont pas été exécutés.

## B. Anomalies découvertes

| Gravité | Fichier / emplacement | Cause | Conséquence | Preuve | État |
|---|---|---|---|---|---|
| P1 | \`yeyamo-mobile/src/app/(auth)/register-partner.tsx:78\` | Création du profil partenaire avant la vérification e-mail et avant persistance des tokens | Appel privé sans session, inscription partenaire incomplète | \`authService.register\` efface la session pour \`verificationRequired\`; \`partnerApi.create\` exige une authentification dans \`partner-service\` | Corrigé |
| P1 | \`yeyamo-mobile/src/services/api/client.ts:257\` | Le \`catch\` du refresh traitait réseau, 5xx et 401 comme une expiration terminale | Déconnexion injustifiée et risque de perte de session lors d'une panne temporaire | Le refresh utilise Axios hors de l'intercepteur; l'absence de réponse n'est pas une révocation | Corrigé |
| P1 | \`security-hardening-starter/src/main/java/com/yeyamo/security/hardening/StrictJwtDecoders.java:79\` | Aucun validateur explicite du sujet JWT | Un JWT signé avec issuer/audience/expiration valides mais sans \`sub\` pouvait être accepté | Test ajouté, échec initial puis succès après correction | Corrigé |

Aucune faille supplémentaire n'a été démontrée dans la consommation OTP : le script Redis consomme le challenge atomiquement et la création de session reste limitée à l'état \`PENDING\` avant confirmation.

## C. Corrections supplémentaires

Corrections apportées pendant cette revue :

- \`security-hardening-starter/src/main/java/com/yeyamo/security/hardening/StrictJwtDecoders.java\` : validation obligatoire du \`sub\`.
- \`security-hardening-starter/src/test/java/com/yeyamo/security/hardening/StrictJwtDecodersTests.java\` : tests issuer, sujet et claims de confiance manquants.
- \`auth-service/src/test/java/com/yeyamo_mobile/api/auth_service/service/AuthServiceEmailVerificationTests.java\` : OTP invalide, expiré, consommé et compte suspendu.
- \`E:\Daryl\yeyamo-mobile\src\services\api\client.ts\` : absence de déconnexion sur erreur réseau/5xx du refresh et invalidation générationnelle des refreshs en vol.
- \`E:\Daryl\yeyamo-mobile\src\services\storage\secure-store.ts\` : clé de brouillon partenaire.
- \`E:\Daryl\yeyamo-mobile\src\features\partner\partner-registration-draft.ts\` : stockage borné à 24 heures et finalisation après session.
- \`E:\Daryl\yeyamo-mobile\src\app\(auth)\register-partner.tsx\` : report de la création privée après OTP pour le parcours mot de passe.
- \`E:\Daryl\yeyamo-mobile\src\features\auth\auth.service.ts\` : finalisation du dossier partenaire après login, OTP ou restauration de session.

Les autres fichiers modifiés présents dans l'arbre de travail proviennent de la correction précédente et ont été préservés intégralement.

### Diff exact des corrections de cette revue

\`\`\`diff
// client mobile
let refreshGeneration = 0;
export function invalidatePendingRefresh(): void {
  refreshGeneration += 1;
  refreshPromise = null;
}

// seuls les 401/403 du endpoint refresh sont terminaux
if (isAxiosError(refreshError)
        && [401, 403].includes(refreshError.response?.status ?? 0)) {
    await handleTerminalAuthenticationFailure();
}

// empêche une réponse de refresh arrivée après logout de réécrire SecureStore
if (generation !== refreshGeneration) throw new Error('SESSION_CHANGED');
\`\`\`

\`\`\`diff
// parcours partenaire mobile
- await partnerApi.create(partnerInput);
+ if (socialAuthenticated) await partnerApi.create(partnerInput);
+ else await savePartnerRegistrationDraft(partnerInput);
\`\`\`

\`\`\`diff
// validation JWT interservices
+ OAuth2TokenValidator<Jwt> subjectValidator = jwt -> hasText(jwt.getSubject())
+         ? OAuth2TokenValidatorResult.success()
+         : OAuth2TokenValidatorResult.failure(new OAuth2Error(
+                 "invalid_token", "Required JWT subject is missing", null));
\`\`\`

## D. Sécurité OTP

- \`AuthService.confirmEmailVerification\` capture l'état \`wasPending\` avant consommation, puis n'appelle \`tokensFor\` que pour un compte initialement \`PENDING\`.
- Un compte déjà \`ACTIVE\`, \`SUSPENDED\`, \`BLOCKED\` ou autre statut non actif ne reçoit pas de session implicite.
- \`OtpService\` utilise un script Redis atomique : compteur de tentatives, expiration, comparaison du hash et suppression du challenge se font dans une seule exécution Redis.
- Un OTP incorrect ou expiré ne modifie pas l'utilisateur et n'émet aucun token.
- Une seconde confirmation échoue après consommation et ne crée pas de seconde session.
- La connexion automatique après activation est donc autorisée uniquement par possession de la boîte e-mail et pour le contexte d'un compte \`PENDING\`. Aucun challenge d'inscription supplémentaire n'est introduit, car le contrat actuel lie déjà le code au but \`email-verification\` et à l'adresse.

Limite : la concurrence réelle avec Redis et PostgreSQL n'a pas été lancée. La consommation atomique Redis est testée par script simulé; l'atomicité transactionnelle de la base n'est pas prouvée par un test multi-processus.

## E. Refresh et sessions

Politique finale mobile :

- Un \`401\` sur un token différent de celui actuellement stocké est rejoué une fois avec le token courant.
- Un token d'accès expiré ou malformé déclenche au plus un refresh partagé par \`refreshPromise\`.
- Un \`401\` avec token encore valide est considéré comme rejet d'authentification et termine la session; il n'est pas transformé en boucle de refresh.
- Une réponse refresh \`401/403\` termine la session, car elle correspond au contrat d'un refresh invalide, expiré ou révoqué.
- Une erreur sans réponse réseau, un timeout ou une réponse serveur \`5xx\` ne supprime plus la session.
- Une déconnexion ou une nouvelle session invalide les refreshs en vol; une réponse tardive ne peut pas réécrire les tokens.
- La rotation serveur reste conservée et le refresh token n'est jamais envoyé comme access token.

Il n'existe pas de suite Jest/React Native dédiée dans le dépôt mobile. Le typage et ESLint sont donc validés, mais les scénarios runtime Axios (réseau, concurrence, logout pendant refresh) restent à couvrir par tests unitaires mobiles ou E2E.

## F. JWT interservices

Configuration examinée :

- \`api-gateway/pom.xml:8\` et \`auth-service/pom.xml:8\` confirment Spring Boot \`4.1.0\`.
- \`api-gateway/pom.xml:31\` et \`auth-service/pom.xml:31\` confirment Spring Cloud \`2025.1.2\`.
- Le contrat applicatif reste HS256 avec une clé HMAC UTF-8 d'au moins 32 octets.
- \`JWT_SECRET\`, \`JWT_ISSUER\` et \`JWT_AUDIENCE\` sont centralisés via Config Server sans valeur secrète écrite dans le dépôt.
- \`jwt.previous-secrets\` conserve la rotation contrôlée.
- Le starter valide signature, timestamps, issuer, audience, sujet et éligibilité du compte.
- Les tokens d'accès générés par Auth portent \`token_type=access\`, \`account_status\` et \`email_verified\`. Les tokens refresh restent opaques.

Résultats :

- JWT HS256 valide : accepté.
- Mauvaise signature : refusée.
- JWT expiré : refusé.
- Mauvais issuer : refusé.
- Mauvaise audience : refusée.
- Sujet absent : refusé après correction.
- Ancienne clé : acceptée uniquement lorsqu'elle est explicitement configurée.
- Compte e-mail non vérifié ou statut inactif : refusé par le validateur d'éligibilité.

La validation complète Auth → Gateway → User avec trois applications démarrées n'a pas été exécutée. Les preuves disponibles sont les tests du service Auth, du starter et de la Gateway avec secrets fictifs.

## G. Parcours partenaire

Le défaut de séquencement est corrigé :

\`\`\`text
register PENDING
  → brouillon local sans mot de passe
  → confirmation OTP
  → session backend persistée
  → POST /partners authentifié
  → tableau de bord partenaire
\`\`\`

Pour un backend ancien retournant une confirmation OTP sans session, le brouillon reste disponible et la finalisation est retentée après le login ou la restauration d'une session. Un compte \`PENDING\` n'est jamais autorisé à accéder directement à \`/partners\`.

Le service partenaire conserve les règles existantes : création en \`DRAFT\`, modification avant soumission, publication publique seulement après \`APPROVED\`, et permissions métier inchangées.

## H. Navigation mobile

Le contrat courant permet :

- Inscription e-mail : écran OTP, persistance des tokens, puis \`/(tabs)\`.
- Ancien backend sans session : écran login après OTP.
- Login suivant : aucun OTP supplémentaire.
- Session restaurée : appel \`/auth/me\`, validation d'éligibilité puis hydratation Zustand/SecureStore.
- Onboarding et activité feed : déclenchés après persistance de la session.

\`npx tsc --noEmit\` est vert et ESLint ciblé est vert. Cela ne constitue pas une preuve E2E de navigation Expo Router, d'hydratation réelle SecureStore ou d'un démarrage Android.

## I. WebSocket

Analyse statique :

- Les frames STOMP \`ERROR\` ferment le socket et désactivent les reconnexions automatiques.
- Une fermeture réseau ordinaire conserve la reconnexion exponentielle bornée à huit tentatives.
- \`disconnect()\` désactive explicitement toute reconnexion et efface les handlers.
- Le Gateway conserve une liste CORS explicite via \`security.cors.allowed-origins\`; aucune autorisation globale \`*\` n'a été ajoutée.
- L'authentification STOMP reste aval au service messaging; la route Gateway d'upgrade ne désactive pas ce contrôle.

Les cas origine autorisée/interdite, JWT STOMP valide/invalide et retour réseau n'ont pas été exécutés contre un serveur WebSocket réel. Aucun secret ou token n'est journalisé par les modifications examinées.

## J. Tests

| Test | Commande ou scénario | Résultat | Preuve |
|---|---|---|---|
| OTP/Auth | \`mvn -pl auth-service,security-hardening-starter -am test\` | Réussi | 69 tests, 0 échec |
| OTP ciblé | Activation, OTP invalide, expiré, réutilisé, compte suspendu | Réussi | \`AuthServiceEmailVerificationTests\`, 6 tests |
| JWT ciblé | \`mvn -pl security-hardening-starter -am '-Dtest=StrictJwtDecodersTests' '-Dsurefire.failIfNoSpecifiedTests=false' test\` | Réussi | 5 tests |
| Gateway | \`mvn -pl api-gateway -am test\` | Réussi | 26 tests, dont fallback disponible/lent/indisponible/rétablissement/circuit |
| Partenaire | \`mvn -pl partner-service -am test\` | Réussi | 16 tests |
| Mobile TypeScript | \`npx tsc --noEmit\` | Réussi | code retour 0 |
| Mobile ESLint | \`npx eslint\` sur les fichiers modifiés | Réussi | 0 erreur, 0 avertissement après correction |
| Compose local | \`docker compose -f docker-compose.yml config --no-interpolate\` | Réussi | code retour 0 |
| Compose production | \`docker compose -f docker-compose.production.yml -f docker-compose.production.images.yml config --no-interpolate\` | Réussi | code retour 0 |
| Diff whitespace | \`git diff --check\` backend, cloud config, mobile | Réussi | trois codes retour 0 |
| Build Docker réel | \`docker build\` | Non exécuté | aucun conteneur ni image créé |
| E2E mobile/WebSocket | Android/Expo/WebSocket réel | Non exécuté | infrastructure runtime non démarrée |
| Production | endpoints, VPS, Dokploy | Interdit et non exécuté | aucune connexion production |

Une première tentative du test Maven ciblé a été bloquée par l'interprétation PowerShell de l'option \`-D\`; elle a ensuite été relancée correctement et réussie.

## K. Risques résiduels

- La concurrence OTP réelle Redis/PostgreSQL et les échecs de commit réseau restent à tester en environnement isolé.
- Le refresh mobile n'a pas de tests unitaires runtime dédiés; le comportement est démontré par analyse statique et compilation.
- Le parcours mobile complet n'a pas été exécuté sur Android/iOS.
- Le handshake WebSocket et la configuration d'origine effective doivent être vérifiés en préproduction avec une origine autorisée réelle et une origine interdite.
- La validation cryptographique interservices n'a pas été exécutée avec Gateway et User Service simultanément; elle est couverte par composants avec secrets fictifs.
- Le brouillon partenaire est conservé au maximum 24 heures; un échec durable de création est retenté lors d'une session authentifiée ultérieure.

## L. État Git

Branche backend : \`main\`. Branche du dépôt imbriqué \`cloud-conf-yeyamo\` : \`daryl\`. Branche mobile : \`daryl\`.

Fichiers modifiés ou ajoutés avant cette revue et préservés :

- Backend : \`.env.example\`, Auth, country-config, \`docker-compose.yml\`, graph, ticket, starter de sécurité, rapports précédents et DTO/test OTP.
- \`cloud-conf-yeyamo\` : \`application.properties\`, \`country-config-service.properties\`, \`discovery-service.properties\`, \`feed-service.properties\`.
- Mobile : flux OTP, API/auth types, service auth, client Axios et client STOMP.

Fichiers touchés ou ajoutés pendant cette revue :

- \`security-hardening-starter/src/main/java/com/yeyamo/security/hardening/StrictJwtDecoders.java\`
- \`security-hardening-starter/src/test/java/com/yeyamo/security/hardening/StrictJwtDecodersTests.java\`
- \`auth-service/src/test/java/com/yeyamo_mobile/api/auth_service/service/AuthServiceEmailVerificationTests.java\`
- \`E:\Daryl\yeyamo-mobile\src\services\api\client.ts\`
- \`E:\Daryl\yeyamo-mobile\src\services\storage\secure-store.ts\`
- \`E:\Daryl\yeyamo-mobile\src\features\partner\partner-registration-draft.ts\`
- \`E:\Daryl\yeyamo-mobile\src\features\auth\auth.service.ts\`
- \`E:\Daryl\yeyamo-mobile\src\app\(auth)\register-partner.tsx\`

Aucun commit, push, reset ou nettoyage Git n'a été effectué.

## M. Préparation du déploiement

### Décision : \`INCOMPLETE\`

Les corrections locales démontrées sont terminées et les suites ciblées sont vertes. La version peut être proposée pour une préproduction isolée après validation manuelle des points résiduels, mais elle ne doit pas être déclarée prête pour production sur la seule base de ces tests.

### Réponses obligatoires

1. **Connexion automatique après OTP sécurisée ?** Oui pour \`PENDING\` avec OTP consommable une seule fois; démontré par tests unitaires et analyse Redis.
2. **Connexion sans ressaisie après activation ?** Oui avec le contrat courant; démontré par le test Auth et le flux mobile compilé.
3. **Connexions suivantes sans OTP ?** Oui pour un compte \`ACTIVE\` et vérifié; démontré par \`SessionEligibilityPolicy\` et les tests JWT/Auth.
4. **JWT cohérents ?** Partiellement démontré : signature, issuer, audience, expiration, sujet et éligibilité sont testés avec secrets fictifs; le trajet multi-service réel reste non vérifié.
5. **Refreshs excessifs maîtrisés ?** Démontré par analyse statique : single-flight, retry unique et invalidation générationnelle; tests runtime mobile non disponibles.
6. **Parcours partenaire préservé ?** Oui par analyse et tests partenaire; le parcours mobile réel reste à valider sur appareil.
7. **Navigation mobile cohérente ?** Probable et typée; non démontrée E2E.
8. **WebSocket sécurisé ?** Démontré par analyse statique des contrôles et de l'arrêt sur erreur permanente; handshake réel non vérifié.
9. **Tests indispensables restants ?** E2E Android/iOS OTP→session→onboarding, refresh réseau/concurrent, WebSocket origine/JWT, et test interservices avec instances isolées.
10. **Prêt pour préproduction isolée ?** Oui sous réserve d'une validation manuelle contrôlée; la classification formelle reste \`INCOMPLETE\` jusqu'à l'exécution des tests listés.

### Procédure recommandée de préproduction

1. Injecter uniquement des secrets fictifs propres à la préproduction et vérifier l'égalité \`JWT_SECRET\`, issuer et audience entre Auth, Gateway et services aval.
2. Construire une image représentative avec \`docker build\` sans la publier, puis vérifier le healthcheck \`/actuator/health/readiness\` sous utilisateur non-root.
3. Exécuter les scénarios OTP et partenaire avec Redis/PostgreSQL de préproduction isolés.
4. Exécuter les scénarios refresh réseau, révocation, rotation et logout concurrent.
5. Tester WebSocket avec origines autorisée/interdite et JWT fictifs.

### Retour arrière

Revenir uniquement aux fichiers modifiés par cette revue dans la même révision de travail, après arrêt contrôlé de la préproduction. Ne pas restaurer de secret depuis Git et ne pas annuler les modifications antérieures sans comparaison de leurs diffs. Aucun retour arrière n'a été exécuté pendant cette mission.

