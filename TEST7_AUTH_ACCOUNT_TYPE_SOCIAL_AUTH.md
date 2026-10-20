# TEST 7 — Auth, type de compte et authentification sociale

## 1. Architecture d'inscription existante

Le mobile utilise un store Zustand unique, SecureStore, `authService` et `/api/v1/auth`. Les quatre modes d'authentification convergent vers le même `AuthResponse`, le même stockage access/refresh token, `/auth/me` et le même nettoyage de session. Le dossier professionnel est un agrégat distinct créé par `POST /api/v1/partners`.

## 2. Parcours Explorer avant/après

- Avant : choix Explorer → formulaire monolithique `/(auth)/register` → `/auth/register` → vérification email → intérêts/application.
- Après : même logique répartie sur quatre étapes avec `YeyamoFormProgress` : identité, pays/préférences locales, sécurité, puis vérification. Le même schéma Zod, les mêmes hooks pays, Turnstile, le même payload et la même route post-inscription sont conservés. `accountType=EXPLORER` est explicite et persistant.

## 3. Parcours Partner avant/après

- Avant : choix Partner → `register-partner-multistep` → redirection forcée vers `register` → formulaire Explorer.
- Après : choix Partner → formulaire Partner réel en quatre étapes avec la progression globale Yeyamo → création de l'identité avec `accountType=PARTNER` → création du dossier via `/partners` → vérification email. Un login social Partner revient au formulaire professionnel pour compléter le dossier.

Le backend ne supporte qu'une activité principale. L'interface propose donc une sélection unique parmi les onze valeurs réelles de `BusinessType`; aucun faux multi-select n'a été ajouté.

## 4. Cause racine exacte

`register-partner-multistep.tsx` redirigeait vers `/(auth)/register` et `register-partner.tsx` faisait la même chose. Cette régression avait remplacé l'ancien écran Partner. L'ancien submit historique était cependant un mock (`setTimeout`, aucun endpoint), il n'a donc pas été restauré tel quel.

## 5. Représentation canonique

`AccountType { EXPLORER, PARTNER }` est persisté dans `users.account_type` et retourné par `/auth/me`. Il décrit le parcours produit, pas les autorisations. Le rôle `PARTNER` reste accordé uniquement après l'événement métier `partner.approved`.

## 6. Contrat backend d'inscription

`RegisterRequest` accepte désormais `accountType`; une valeur absente devient `EXPLORER`. Les seules valeurs acceptées sont celles de l'enum Java. Le formulaire Partner envoie uniquement les champs réellement supportés par `PartnerRequest`.

## 7. Contrat `/auth/me`

`UserResponse` expose `accountType`. Le mapper mobile utilise ce champ, avec compatibilité temporaire par rôle `PARTNER` pour les réponses anciennes.

## 8. Architecture Google existante

Expo AuthSession obtient un ID token Google, envoyé à `/auth/oauth/google`. Le backend valide signature, issuer, audience, expiration, subject et email vérifié avant de produire une session Yeyamo.

## 9. Non-régression Google

Le endpoint, le stockage de session et le validateur existants sont conservés. L'inscription sociale envoie maintenant explicitement le type de compte. Les tests Google existants passent.

## 10. Apple

`expo-apple-authentication`, `usesAppleSignIn` et le plugin étaient déjà présents. Le bouton « Continuer avec Apple » reste visible sur les écrans de connexion et d'inscription; sur une plateforme non compatible, un clic affiche l'indisponibilité au lieu de masquer l'option. Le backend Apple existant valide le token via les JWK Apple, l'issuer et l'audience. Le subject est la clé d'identité; email/nom ne sont pas requis lors des connexions suivantes.

## 11. Microsoft

Ajout d'un flux OIDC Authorization Code + PKCE via `expo-auth-session`. Aucun secret n'est embarqué. Le bouton reste cliquable même sans client ID et affiche alors l'erreur de configuration exacte. Une fois configuré, le code est échangé contre un ID token, puis envoyé à `/auth/oauth/microsoft`. Le backend vérifie signature JWK, issuer tenant, audience, expiration et subject.

## 12. Persistance des identités sociales

La table générique `oauth_accounts(provider, provider_user_id, user_id)` est réutilisée. Sa contrainte unique empêche une identité fournisseur d'être liée à plusieurs comptes.

## 13. Validation des tokens

- Google : validation existante conservée.
- Apple : JWK `appleid.apple.com`, issuer et audience.
- Microsoft : JWK Microsoft tenant/common, issuer v2 et client ID comme audience.
- Aucun token OAuth n'est journalisé.

## 14. Nouveau compte social

Le `accountType` demandé est appliqué uniquement lors de la création d'un nouvel utilisateur social. Partner mène ensuite à la complétion du dossier professionnel.

## 15. Compte social existant

Une identité `(provider, subject)` existante récupère toujours son utilisateur persistant; le type demandé par l'écran est ignoré. Une collision email sans lien existant retourne `OAUTH_ACCOUNT_CONFLICT`; aucun merge automatique par email n'est effectué.

## 16. Préservation du type

Un Explorer existant ne devient pas Partner et inversement lors d'un login social. L'approbation métier Partner synchronise `accountType=PARTNER` et le rôle `PARTNER`.

## 17. Convergence de session

Email, Google, Apple et Microsoft utilisent `persistSession`, SecureStore, le store canonique, refresh token et `/auth/me`.

## 18. Logout et changement de compte

Le logout, la révocation serveur, la déconnexion temps réel et le nettoyage local existants n'ont pas été modifiés. Les protections Test 7 de cache/viewer restent intactes.

## 19. Redirect/deep link

- Scheme conservé : `yeyamo`.
- Redirect Microsoft : `yeyamo://oauth/microsoft`.
- Les routes notifications/messages/stories ne sont pas modifiées.

## 20. Configuration requise

- Mobile : `EXPO_PUBLIC_MICROSOFT_CLIENT_ID`, `EXPO_PUBLIC_MICROSOFT_TENANT_ID`.
- Backend/config : `MICROSOFT_CLIENT_ID`, `MICROSOFT_TENANT_ID`, plus les Google/Apple IDs déjà requis.
- Enregistrer exactement les redirect URIs de chaque profil development/preview/production dans Microsoft Entra.
- Aucun secret client Microsoft, clé privée Apple ou secret backend dans le mobile.

## 21. Changements natifs

Aucune nouvelle dépendance ni modification native. Apple Authentication et sa capability étaient déjà configurés. Un binaire existant qui ne contient pas cette configuration historique doit être reconstruit, mais cette passe n'impose pas de rebuild supplémentaire.

## 22. Migration base de données

`V6__add_canonical_account_type.sql` ajoute `users.account_type`, backfill les détenteurs du rôle Partner et limite les valeurs à `EXPLORER|PARTNER`.

## 23. Fichiers mobile modifiés

- `.env.example`, `.env.production.example`
- `src/app/(auth)/account-type.tsx`
- `src/app/(auth)/login.tsx`
- `src/app/(auth)/register.tsx`
- `src/app/(auth)/register-partner.tsx`
- `src/app/(auth)/register-partner-multistep.tsx`
- `src/components/auth/SocialButton.tsx`
- `src/config/env.ts`
- `src/features/auth/auth.api.ts`
- `src/features/auth/auth.service.ts`
- `src/features/auth/types.ts`
- `src/features/auth/useMicrosoftIdToken.ts`

Les autres modifications mobiles préexistantes ont été conservées.

## 24. Fichiers backend modifiés

- `AuthController`, `RegisterRequest`, `OAuthLoginRequest`, `UserResponse`
- `AccountType`, `User`, `AuthService`, `OAuthTokenVerifier`
- `PartnerApprovedConsumer`
- `OAuthTokenVerifierSecurityTests`
- migration `V6__add_canonical_account_type.sql`

## 25. Configuration modifiée

`cloud-conf-yeyamo/auth-service.properties` référence les deux variables Microsoft sans valeur secrète.

## 26. Tests exécutés

- `npx tsc --noEmit` : PASS.
- ESLint ciblé : PASS avec un avertissement initial supprimé ensuite.
- Auth-service complet : 45 tests, 0 échec.
- OAuth/Partner ciblés après ajout des cas Microsoft : 9 tests, 0 échec.
- `git diff --check` backend/mobile : PASS (avertissements CRLF uniquement).
- Aucun test fournisseur réel : credentials et appareil requis.

## 27. Prérequis runtime restants

Configurer les client IDs/audiences réels, la capability Apple du compte Apple Developer, l'application Microsoft Entra et ses redirects. Tester sur iPhone réel. Désactiver le flag local `EXPO_PUBLIC_DEV_FORCE_CLEAN_SESSION` après le test de démarrage propre pour tester la persistance.

## 28. Services à redéployer

`auth-service` et la configuration servie par config-server. `partner-service` n'a pas été modifié mais doit rester disponible pendant l'inscription Partner.

## 29. Rebuild mobile

Pas de nouvelle dépendance native et pas de changement `app.json` dans cette passe : aucun rebuild natif supplémentaire requis pour Microsoft. Apple exige naturellement un binaire contenant la capability déjà déclarée.

## 30. Plan ordonné de test iPhone

1. Désactiver le force-clean après le premier écran login et redémarrer Metro.
2. Tester Explorer email, vérification, restart, logout.
3. Tester Partner email, vérifier l'ouverture du formulaire Partner et la création du dossier.
4. Tester Google existant puis nouveau Explorer/Partner.
5. Tester Apple existant puis nouveau Explorer/Partner; annuler une fois.
6. Tester Microsoft existant puis nouveau Explorer/Partner; annuler une fois.
7. Vérifier les deux invariants croisés de type de compte.
8. Tester double tap, erreur réseau, collision email et restauration après restart.
9. Tester A logout → B login et vérifier l'absence de données A.

## Statut final

```text
PARTNER_SELECTION_ROOT_CAUSE_FOUND = YES
EXPLORER_FORM_ROUTING = CODE_FIXED_RUNTIME_RETEST_REQUIRED
PARTNER_FORM_ROUTING = CODE_FIXED_RUNTIME_RETEST_REQUIRED
CANONICAL_ACCOUNT_TYPE_CONTRACT = PASS
PARTNER_BACKEND_REGISTRATION = CODE_FIXED_RUNTIME_RETEST_REQUIRED
EXPLORER_BACKEND_REGISTRATION = CODE_FIXED_RUNTIME_RETEST_REQUIRED
AUTH_ME_ACCOUNT_TYPE = PASS
GOOGLE_AUTH_PRESERVED = CODE_FIXED_RUNTIME_RETEST_REQUIRED
APPLE_AUTH_IMPLEMENTED = YES
APPLE_AUTH_STATIC_VALIDATION = PASS
MICROSOFT_AUTH_IMPLEMENTED = YES
MICROSOFT_AUTH_STATIC_VALIDATION = PASS
SOCIAL_AUTH_SHARED_ARCHITECTURE = PASS
NEW_SOCIAL_ACCOUNT_TYPE_PRESERVED = PASS
EXISTING_ACCOUNT_TYPE_IMMUTABLE_ON_SOCIAL_LOGIN = PASS
PROVIDER_IDENTITY_UNIQUENESS = PASS
SERVER_SIDE_PROVIDER_VALIDATION = PASS
EMAIL_PASSWORD_AUTH_PRESERVED = PASS
SESSION_PERSISTENCE_PRESERVED = PASS
LOGOUT_PRESERVED = PASS
ACCOUNT_SWITCH_CACHE_ISOLATION = PASS
OAUTH_SECRETS_IN_MOBILE = NONE
OAUTH_TOKENS_LOGGED = NO
DATABASE_MIGRATION_REQUIRED = YES
CONFIG_CHANGE_REQUIRED = YES
NEW_NATIVE_DEPENDENCY = NO
IOS_CAPABILITY_CHANGE_REQUIRED = NO
MOBILE_NATIVE_REBUILD_REQUIRED = NO
BACKEND_SERVICES_TO_REDEPLOY = auth-service, config-server configuration
MOBILE_FILES_MODIFIED = 12 auth/config files
BACKEND_FILES_MODIFIED = 11 auth files including migration/test
CONFIG_FILES_MODIFIED = cloud-conf-yeyamo/auth-service.properties, mobile env examples
MOBILE_TYPESCRIPT = PASS
MOBILE_LINT = PASS
BACKEND_TARGETED_TESTS = PASS
GIT_DIFF_CHECK = PASS
P0_CODE_BLOCKERS = NONE
P1_CODE_BLOCKERS = Real provider configuration and iPhone runtime verification
RUNTIME_PREREQUISITES = Google/Apple/Microsoft IDs, Apple capability, Microsoft redirect registrations, deployed auth migration
STATIC_FIX_READY = YES
RUNTIME_RETEST_REQUIRED = YES
READY_FOR_AUTH_RUNTIME_RETEST = YES
```
