# YEYAMO V1 — remédiation finale et décision GO

Date : 11 septembre 2026  
Périmètre : backend Maven multi-modules, `yeyamo-mobile`, configuration de production et contrat d’encaissement mobile.

## Décision exécutable

| Décision | Statut | Portée exacte |
| --- | --- | --- |
| Déploiement backend | **GO** | Les flux de paiement ne simulent plus un succès en production ; une capacité de paiement non configurée échoue fermement. |
| Bundle Android Expo | **GO — contrôle statique** | Lint, TypeScript et export Android ont abouti. Un build signé EAS et un essai sur appareil restent des étapes de livraison, pas des correctifs de code. |
| Activation réelle du cash-in | **PENDING EXTERNAL** | Elle exige les identifiants, URL, webhook/callback et capacités documentées du prestataire. Aucun opérateur ou endpoint réel n’a été inventé. |
| Annulation / remboursement prestataire | **NON ACTIVÉ** | Le fournisseur générique répond par une erreur explicite ; l’interface ne promet plus un remboursement automatique. Contrat fournisseur requis. |

Il ne reste aucun blocage de code local P0. La mise en production ne doit pas annoncer le remboursement automatique ni activer le cash-in réel avant validation avec le prestataire.

## Correctifs livrés

### Cash-in commerce : prix serveur et coordonnées de paiement

- La création de commande commerce accepte désormais `operator`, `country` et un numéro E.164 validé, uniquement pour les commandes payantes.
- Le prix, la devise et l’identifiant de commande envoyés au paiement restent canoniques côté serveur. Une valeur fournie par le client ne peut pas déterminer le montant encaissé.
- Les coordonnées mobile-money sont persistées pour l’exécution, mais masquées des réponses JSON de commande afin de ne pas exposer le numéro de téléphone.
- Le contrôleur de commande d’œuvre déduit le pays du claim JWT : le client ne choisit pas le pays de paiement.
- La migration `commerce-service/src/main/resources/db/migration/V7__add_commerce_cash_in_details.sql` ajoute les champs requis.
- Les tests couvrent le prix canonique, les coordonnées absentes/invalides, le message de paiement émis et l’idempotence d’une commande dupliquée.

Fichiers principaux :

- `commerce-service/.../application/CommerceService.java`
- `commerce-service/.../application/ArtworkCommerceService.java`
- `commerce-service/.../infrastructure/rest/ArtworkCommerceController.java`
- `commerce-service/.../domain/CommerceOrder.java`

### Capacité de paiement : configuration explicite et échec fermé

- `PAYMENT_CASH_IN_CAPABILITIES` est désormais requis pour sélectionner des opérateurs en production. Son format est un JSON de type `{"XX":["provider-operator-id"]}`.
- Une capacité absente, un JSON invalide ou un opérateur non autorisé produit `EXTERNAL_PROVIDER_CAPABILITY_UNCONFIRMED`; aucune requête HTTP vers le prestataire n’est alors faite.
- Le profil production refuse de démarrer sans cette configuration de capacités.
- Les valeurs de capacités côté application mobile sont entièrement fournies par `EXPO_PUBLIC_CASH_IN_OPERATOR_CONFIG`. Aucune liste Cameroun/MTN/Orange ni aucune revendication de support opérateur n’est codée par défaut.
- Les identifiants d’opérateurs exposés par le mobile doivent correspondre exactement à ceux de `PAYMENT_CASH_IN_CAPABILITIES`. La configuration mobile ne constitue jamais une autorisation serveur.

Fichiers principaux :

- `payment-service/.../application/HrSkillsPayService.java`
- `payment-service/.../application/HrSkillsPayProvider.java`
- `payment-service/.../config/HrSkillsPayProperties.java`
- `cloud-conf-yeyamo/payment-service.properties`
- `docker-compose.production.yml`
- `yeyamo-mobile/src/features/payments/cash-in.ts`
- `yeyamo-mobile/src/components/payments/MobileMoneyForm.tsx`
- `yeyamo-mobile/.env.mobile.example`

### Annulation et remboursement : comportement honnête

- Le port de paiement générique ne prétend plus savoir annuler ou rembourser via un fournisseur dont le contrat ne fournit pas ces capacités.
- La réponse est un échec explicite, conservé dans le flux de paiement ; elle ne devient jamais un remboursement réussi artificiel.
- L’écran de commande d’œuvre indique qu’une demande est envoyée et qu’aucun remboursement n’est confirmé avant la réponse du prestataire.

Condition de clôture externe : documentation des endpoints d’annulation/remboursement, authentification, règle d’idempotence et sandbox du prestataire.

### Démos, mocks et CTA

- Les faux comptes ne sont pas hydratés en production et `loginDemo` échoue en production.
- Les contenus fictifs restants sont explicitement limités aux sessions `demo-*`; ils ne constituent pas une donnée de production.
- Les actions de partage et commentaires de `VideoCard` sont devenues fonctionnelles (partage natif et route réelle des commentaires).
- Le bouton social Apple inactif a été retiré de l’inscription.
- Les cartes partenaire sans route/API sont passives : elles ne simulent plus une action. Les actions de billetterie ne sont proposées que hors jeu de démonstration.
- Le profil public ne retombe plus sur un auteur ou un compteur fictif hors démonstration.

Classification résiduelle :

| Classe | État | Conséquence production |
| --- | --- | --- |
| `PROD_BLOCKED_DEMO` | Jeux de données experience/feed/profile/places de démonstration | Inaccessibles via les sessions de production. |
| `INTENTIONALLY_NON_INTERACTIVE` | Cartes partenaire sans endpoint réel | Pas de faux CTA ; elles deviennent actives quand le backend correspondant est livré. |
| `LOCAL_ONLY` | URLs de développement, exemples `.env`, tests, Postman et health checks Docker | Ne doivent pas être utilisés comme configuration de déploiement. |
| `PRODUCTION_CONFIG` | URL API/WSS, secrets et capacités définis au déploiement | Valeurs obligatoires à injecter par l’environnement cible. |

Une recherche finale ne trouve plus de callback vide de type `onPress={() => {}}` ou `() => undefined` dans le mobile.

## Sécurité dépendances

Un nouvel audit `npm audit --json` a été exécuté après mise à jour contrôlée du lockfile et des overrides compatibles.

| Mesure | Avant | Après |
| --- | ---: | ---: |
| Critiques | 0 | 0 |
| Hautes | 6 | 1 |
| Modérées | 22 | 21 |
| Total | 28 | 22 |

Les overrides appliqués mettent notamment à jour `browserslist`, `js-yaml`, `postcss`, `shell-quote` et les deux branches vulnérables de `brace-expansion` sans forcer une mise à niveau Expo incompatible.

Le seul niveau **high** restant est `@xmldom/xmldom`, transitif via la chaîne Expo/config (`@expo/plist` / `plist`). La version corrigée nécessite une migration de cette chaîne pré-1.0 qui n’est pas compatible sémantiquement avec le graphe actuel. Elle est donc classée **UPSTREAM_OR_MAJOR_UPGRADE_REVIEW**, et non masquée par un override risqué.

Les 21 modérées restantes doivent être traitées lors d’une montée de version Expo/React Native planifiée et testée. `npm audit` ne doit pas être présenté comme vert tant que ce travail n’est pas fait.

## Validation exécutée

| Contrôle | Résultat |
| --- | --- |
| `mvn compile -DskipTests` à la racine | Réussi : 41 modules. |
| Tests backend ciblés initiaux | booking 40, payment 35, ticket 17, gateway 14, gamification 36, mission 17 : tous réussis. |
| `mvn -pl commerce-service -am test` | Réussi : security-hardening 16 + commerce 18 tests. |
| `mvn -pl payment-service -am test` | Réussi : event-contracts 2 + security-hardening 16 + payment 36 tests. |
| Avertissement hôte Maven | Espace disque insuffisant signalé durant l’arrêt Surefire, malgré `BUILD SUCCESS`. Libérer de l’espace avant un nouveau run Maven complet sur l’hôte de release. |
| `npm run lint` | Réussi, 0 erreur et 0 avertissement. |
| `npx tsc --noEmit` | Réussi. |
| `npx expo-doctor` | Réussi : 21 contrôles sur 21. |
| `npx expo export --platform android` | Bundle Android généré : `index-753b21947e3ab90238521654d4c06a06.hbc`, 8,4 Mo. Metro est resté ouvert jusqu’au timeout après l’affichage `Exported`; les fichiers produits ont été vérifiés puis le répertoire temporaire `.expo-go-check` a été supprimé. |
| `git diff --check` | Réussi : aucune erreur d’espace blanc (avertissements CRLF non bloquants). |

## Configuration release à fournir

Avant activation réelle, le déploiement doit injecter et vérifier :

1. Les URL publiques API et WebSocket, sans `localhost` dans le compose ou l’exemple de production.
2. Les secrets/identifiants et l’URL contractuelle du prestataire de paiement.
3. `PAYMENT_CASH_IN_CAPABILITIES` avec les seuls pays et identifiants d’opérateurs effectivement homologués.
4. `EXPO_PUBLIC_CASH_IN_OPERATOR_CONFIG` correspondant aux mêmes identifiants, avec libellés destinés à l’interface.
5. Les URLs de callback/webhook, TLS, DNS, politiques CORS et un essai de bout en bout sandbox puis production contrôlée.
6. Le protocole documenté d’annulation/remboursement avant de proposer cette promesse dans l’interface.

La vérification des fichiers de compose et d’environnement de production ne contient pas de `localhost`. Les occurrences restantes dans le dépôt sont des fichiers de développement, tests, documentation ou health checks internes ; chaque conteneur de release doit néanmoins être contrôlé avec son environnement réellement injecté.

## Risques restants et responsabilités

| Priorité | Risque | Propriétaire / sortie |
| --- | --- | --- |
| P1 | Une haute vulnérabilité transitive `@xmldom/xmldom` demeure. | Planifier l’upgrade Expo/config et repasser audit, doctor et export. |
| P1 | Capacité cash-in et annulation/remboursement non attestées par un contrat prestataire. | Obtenir documentation, accès sandbox, secrets et valider les parcours réels. |
| P1 | Avertissement de capacité disque sur l’hôte Maven. | Libérer de l’espace et rejouer le contrôle backend complet dans l’environnement de release. |
| P2 | Export Expo local non signé. | Générer l’artefact signé EAS/CI et tester sur appareil avant publication store. |

## Verdict final

**GO pour déployer le backend et préparer le bundle Android.** Les parcours sensibles échouent désormais fermement en l’absence de capacité externe confirmée et l’UI ne promet pas de résultat fictif.

**GO conditionnel pour activer les paiements mobile-money réels :** seulement après injection et validation des paramètres prestataire ci-dessus.  
**NO-GO pour promettre annulation/remboursement automatique :** jusqu’à réception et intégration du contrat fournisseur correspondant.
