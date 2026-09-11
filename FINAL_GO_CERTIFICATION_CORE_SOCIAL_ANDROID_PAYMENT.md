# Certification finale — Core, Social, Android et paiement

Date : 2026-09-11
Périmètre vérifié : `E:\Daryl\yeyamo-api` et `E:\Daryl\yeyamo-api\yeyamo-mobile`
Méthode : audit ciblé, corrections locales, tests, recompilation du réacteur puis ré-audit. Aucune transaction réelle n'a été effectuée et aucune valeur de secret n'est reproduite dans ce document.

## Décision globale

**CONDITIONAL_GO — uniquement pour les parcours gratuits et les services dont les prérequis de production sont injectés.**

La mise en production **ne doit pas activer le commerce payant** dans l'état observé. Les paiements sont correctement bloqués en profil `prod` lorsqu'un fournisseur réel ou ses identifiants ne sont pas configurés ; ce comportement est voulu. Il reste un écart fonctionnel réel : le fournisseur HR-Skills Pay ne fournit pas encore les opérations réelles d'annulation et de remboursement.

## Modifications apportées pendant l'audit

| Zone | Correction | Effet vérifié |
|---|---|---|
| Paiement | `payment.provider.name` n'a plus `simulated` comme valeur de repli dans la configuration partagée. | Un déploiement ne peut plus basculer silencieusement sur un débit simulé. |
| Paiement | Ajout du fournisseur sélectionnable `HrSkillsPayProvider` et du contrôle de démarrage production. | `prod` exige un fournisseur explicite, non simulé, disponible, avec des clés et un secret webhook présents, ainsi qu'une URL HTTPS. |
| Paiement | `HrSkillsPayService.isEnabled()` exige explicitement `payment.provider.name=hr-skills-pay`. | Aucun appel cash-in HR-Skills Pay n'est déclenché par simple présence accidentelle de clés. |
| Paiement | Tests de sélection de fournisseur et test d'intégration HR-Skills Pay alignés sur la configuration explicite. | 35 tests du module paiement, sans échec. |
| Mobile | Les permissions Android historiques de lecture/écriture de stockage ont été retirées. | Réduction des permissions sensibles déclarées. |
| Mobile | L'environnement de production exige une API HTTPS et un WebSocket WSS configurés ; la connexion démo est désactivée et ses boutons sont masqués en production. | Le build de production échoue de manière explicite si l'environnement critique est absent ; pas d'accès démo en production. |
| Mobile | Les règles de lint incompatibles avec les références impératives/Animated de React Native ont été limitées, sans désactiver les règles Hooks standard. | Lint exécutable sans erreur ; les avertissements existants restent visibles. |
| Mobile | Dépendances Expo remises sur les versions compatibles du SDK. | `expo-doctor` est intégralement vert. |

## Preuves d'exécution

| Contrôle | Résultat |
|---|---|
| Tests backend core : `config-server`, `registry-service`, `api-gateway`, `auth-service`, `user-service`, `media-service`, `partner-service` | **155 tests, 0 échec, 0 erreur, 0 ignoré** |
| Tests backend social : `content-service`, `interaction-service`, `feed-service`, `notification-service`, `messaging-service`, `social-service`, `graph-service` | **144 tests, 0 échec, 0 erreur, 1 ignoré** |
| Tests booking / payment / commerce / ticket | **108 tests, 0 échec, 0 erreur, 0 ignoré** |
| Tests paiement après les corrections fournisseur | **35 tests, 0 échec, 0 erreur, 0 ignoré** |
| Compilation réacteur Maven | `mvn compile -DskipTests` : **41/41 modules, BUILD SUCCESS** |
| TypeScript mobile | `npx tsc --noEmit` : **succès** |
| Lint mobile | **0 erreur, 67 avertissements**, code de sortie 0 |
| Santé Expo | `npx expo-doctor` : **21/21 contrôles réussis** |
| Export Android | `npx expo export --platform android` : **succès**, bundle généré (2 995 modules) |
| Audit npm de production | `npm audit --omit=dev --json` : **0 critique, 0 haute, 0 modérée, 0 basse** |
| Cohérence des diffs | `git diff --check` backend et mobile : **aucune erreur d'espace blanc** (seuls des avertissements LF/CRLF ont été émis) |

La cible `graph-service` comporte un test ignoré, lié à une dépendance d'environnement de test ; elle ne dispose donc pas d'une couverture d'intégration complète dans cette exécution.

## Paiement : état exact

### Ce qui est opérationnel dans le code

- Cash-in HR-Skills Pay : client HTTP, gestion de jeton, idempotence de tentative et vérification HMAC du webhook existent.
- La paire de clés fournisseur, le secret webhook et l'URL HTTPS sont vérifiés au démarrage en profil `prod`.
- Le fournisseur simulé est limité aux profils `local`, `dev` et `test` ; il est interdit en production.
- Les événements de paiement, idempotence, outbox et les parcours Booking/Commerce/Ticket sont testés localement.
- En cas de champ cash-in manquant (`operator`, `country`, `phone_number`), le consommateur publie un échec de contrat au lieu de débiter ou d'autoriser un paiement incohérent.

### Écarts bloquants pour le paiement réel

1. **Identifiants de production absents dans le processus audité.** `PAYMENT_PROVIDER_NAME`, les clés fournisseur et le secret webhook sont `MISSING`. Le garde de démarrage interrompt donc correctement un service paiement `prod`.
2. **Contrat producteur insuffisant pour un cash-in réel.** Les commandes Booking/Commerce/Ticket doivent fournir `operator`, `country` et `phone_number`. Sans ces champs, le service refuse l'opération avec `CONTRACT_GAP_MISSING_PAYMENT_DETAILS`.
3. **Annulation et remboursement HR-Skills Pay non implémentés côté réel.** `HrSkillsPayProvider.cancel()` et `.refund()` renvoient actuellement un échec explicite car aucune opération fournisseur réelle ne leur est raccordée. Les scénarios simulés sont testés, mais ne prouvent pas ces actions en production.
4. **Aucun test sandbox fournisseur n'a été effectué**, volontairement : il n'y a ni secret injecté ni autorisation de débit réel dans cet environnement.

Conséquence : **désactiver les achats/encaissements réels** jusqu'à la fermeture des points 1 à 3. Les flux gratuits peuvent être évalués séparément.

## Android : état exact

### Éléments vérifiés

- Identifiant Android `com.yeyamo.mobile`, icône/adaptive icon et splash configurés.
- `eas.json` prévoit des builds Android AAB de production avec incrémentation distante.
- La configuration mobile locale contient les noms des trois variables publiques nécessaires (`EXPO_PUBLIC_API_BASE_URL`, `EXPO_PUBLIC_MESSAGING_WS_URL`, `EXPO_PUBLIC_APP_ENV`) sans exposer leurs valeurs dans ce rapport.
- Ces variables, ainsi que les variables backend critiques, ne sont **pas présentes dans le processus de contrôle** : l'injection par EAS/CI ou par l'environnement de déploiement reste à prouver.
- La signature effective de l'AAB, l'installation sur appareil Android, les deep links, le mode offline et les push notifications n'ont pas été exécutés ici.

### Points restant à corriger avant une validation Android complète

- 67 avertissements ESLint subsistent ; ils ne cassent pas le build mais doivent être triés avant une certification de qualité stricte.
- Des écrans/payloads de démonstration persistent, notamment autour du passeport et de certains CTA sociaux/partenaires. Les chemins qui ne disposent pas d'API réelle doivent être masqués derrière une feature flag de production ou finalisés côté API.
- Les clés de cartes publiques ne sont pas disponibles dans le processus vérifié ; les parcours cartographiques de production demandent une validation sur build signé.

## Vérification configuration et secrets

Les contrôles ont uniquement enregistré `SET`/`MISSING`, jamais les valeurs.

| Groupe | État observé dans le processus |
|---|---|
| Fournisseur de paiement, clés et secret webhook | `MISSING` |
| JWT, CORS, jeton interservices | `MISSING` |
| Config Server et Eureka | `MISSING` |
| API/mobile WebSocket et environnement Expo publics | `MISSING` dans le processus ; noms présents dans `.env` mobile |
| Clé Maps publique | `MISSING` |

Plusieurs configurations partagées conservent aussi des valeurs de développement `localhost`. Elles doivent être surchargées par des variables/secrets de l'environnement cible et faire l'objet d'un contrôle de démarrage production. Aucune valeur sensible n'a été affichée, journalisée ou copiée.

## Verdicts requis

| Surface | Verdict | Justification concise |
|---|---|---|
| BACKEND_CORE | **CONDITIONAL_GO** | 155 tests et compilation verte ; variables critiques non injectées et valeurs `localhost` encore présentes dans les fichiers partagés. |
| BACKEND_SOCIAL | **GO_WITH_WARNINGS** | 144 tests sans erreur ; 1 test Graph ignoré et validation runtime Kafka/Neo4j de production encore requise. |
| BACKEND_COMMERCE | **CONDITIONAL_GO** | 108 tests Booking/Payment/Commerce/Ticket verts ; commerce payant dépend des blocages fournisseur ci-dessous. |
| PAYMENT_CORE | **GO_WITH_WARNINGS** | Idempotence, outbox, webhooks et tests locaux passants ; des avertissements portent sur la mise en production fournisseur. |
| PAYMENT_PROVIDER | **NO_GO** | Aucune configuration réelle dans l'environnement, contrat cash-in incomplet et annulation/remboursement réels absents. |
| PAID_COMMERCE | **DISABLED_FOR_RELEASE** | Doit rester désactivé tant que `PAYMENT_PROVIDER` est `NO_GO`. |
| ANDROID_CODE | **GO_WITH_WARNINGS** | Typecheck, lint sans erreur, Expo Doctor et export Android réussissent ; 67 avertissements à résorber. |
| ANDROID_API_ALIGNMENT | **NO_GO** | Mocks/passeport et CTA sans backend réel encore accessibles ; alignement complet non démontré. |
| ANDROID_RUNTIME | **REQUIRES_EXTERNAL_SMOKE_TEST** | Nécessite AAB signé, appareil réel, API cible, deep links, offline et push. |
| ANDROID_PACKAGE | **CONDITIONAL_GO** | Configuration et export OK ; signature EAS et installation d'un AAB de production restent à prouver. |
| PUBLIC_WEB | **NO_GO** | Aucun projet/parcours web public n'a été audité dans cette exécution. |
| USER_WEB | **NO_GO** | Aucun parcours web utilisateur n'a été audité dans cette exécution. |
| PARTNER_WEB | **NO_GO** | Aucun parcours web partenaire n'a été audité dans cette exécution. |
| ADMIN_WEB | **NO_GO** | Le dépôt/admin existe mais n'a pas été audité ni testé dans cette exécution. |

## Conditions de levée des blocages

1. Injecter les secrets et URLs de production dans un coffre/CI, définir explicitement `payment.provider.name=hr-skills-pay`, puis effectuer un test sandbox sans débit réel.
2. Compléter le contrat de commande avec les champs cash-in requis et couvrir Booking, Commerce et Ticket par des tests de contrat.
3. Implémenter et tester les opérations réelles HR-Skills Pay d'annulation et de remboursement avec idempotence et webhooks associés.
4. Construire un AAB signé via EAS/CI avec les variables publiques de production, puis exécuter un smoke test sur appareil physique.
5. Retirer/masquer les écrans et CTA de démonstration en production, traiter les 67 avertissements ESLint, puis relancer l'audit Android.
6. Élargir l'audit aux trois surfaces web et à l'administration avant toute certification globale de la plateforme.

## État du dépôt

Le répertoire de travail était déjà modifié avant et pendant l'audit. Les modifications existantes ont été préservées ; aucun reset, nettoyage destructif, commit, déploiement ni appel de paiement réel n'a été réalisé.
