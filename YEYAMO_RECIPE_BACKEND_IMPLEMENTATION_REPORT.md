# YEYAMO — Recipe backend implementation report

## 1. Résumé exécutif

La fiche recette enrichie a été implémentée dans `culture-service` sans modifier les routes REST existantes. Le modèle couvre désormais les temps, la difficulté, le rendement, les textes éditoriaux, les quantités structurées, les UUID stables, les étapes enrichies, les associations étape–ingrédient, le matériel, la galerie, les références média et les liens YouTube strictement validés.

Une structure minimale de révision conserve le contenu publié pendant la modération d'une modification. L'autorisation distante des médias reste volontairement bloquée en écriture, car le contrat public actuel de `media-service` ne permet pas de vérifier de façon sûre le propriétaire, le statut et le droit d'association. Aucun contrôle n'a été affaibli pour contourner cette absence.

## 2. État Git initial

- Branche : `main`.
- HEAD observé : `4f7620a resolution de conflit test 7A-B`.
- Des modifications préexistantes existaient dans `auth-service`, le sous-module `cloud-conf-yeyamo` et plusieurs rapports racine.
- Elles ont été préservées et ne font pas partie de cette implémentation.

## 3. Fichiers réellement modifiés

- `culture-service/src/main/java/com/yeyamo_mobile/api/culture_service/application/CultureContentService.java`
- `culture-service/src/main/java/com/yeyamo_mobile/api/culture_service/application/CultureDtos.java`
- `culture-service/src/main/java/com/yeyamo_mobile/api/culture_service/application/YouTubeUrlValidator.java`
- `culture-service/src/main/java/com/yeyamo_mobile/api/culture_service/domain/CultureContent.java`
- `culture-service/src/main/java/com/yeyamo_mobile/api/culture_service/domain/CultureEnums.java`
- `culture-service/src/main/java/com/yeyamo_mobile/api/culture_service/domain/CultureRecipeRevision.java`
- `culture-service/src/main/java/com/yeyamo_mobile/api/culture_service/domain/CultureTranslation.java`
- `culture-service/src/main/java/com/yeyamo_mobile/api/culture_service/domain/RecipeDetailsEntity.java`
- `culture-service/src/main/java/com/yeyamo_mobile/api/culture_service/infrastructure/persistence/CultureRepositories.java`
- `culture-service/src/main/resources/db/migration/V6__enrich_recipe_details.sql`
- `culture-service/src/test/java/com/yeyamo_mobile/api/culture_service/CulturePostgresIntegrationTest.java`
- `culture-service/src/test/java/com/yeyamo_mobile/api/culture_service/application/YouTubeUrlValidatorTest.java`
- `culture-service/src/test/java/com/yeyamo_mobile/api/culture_service/domain/RecipeDetailsEntityTest.java`
- `YEYAMO_RECIPE_BACKEND_IMPLEMENTATION_REPORT.md`

## 4. Migration créée

`V6__enrich_recipe_details.sql` est additive. Elle ajoute les colonnes recette, enrichit ingrédients et étapes, effectue un backfill UUID déterministe ne dépendant d'aucune extension PostgreSQL, crée matériel, médias, associations étape–ingrédient et révisions. Un trigger PostgreSQL interdit une association entre recettes différentes. V4 et V5 n'ont pas été modifiées.

La migration n'a pas été exécutée sur une base réelle ou de production.

## 5. Entités modifiées

`RecipeDetailsEntity` utilise maintenant des entités enfants à UUID stable au lieu de simples `@ElementCollection`. `CultureContent.synchronizeRecipe` synchronise les enfants par UUID, conserve les objets persistants lors d'un déplacement et applique les suppressions par `orphanRemoval`. `CultureTranslation` peut recevoir atomiquement la traduction d'une révision approuvée.

## 6. DTO modifiés

Les requests/responses exposent tous les nouveaux champs, les IDs, les nombres `BigDecimal`, les unités, les liens étape–ingrédient, le matériel et la galerie. Les anciens constructeurs Java minimaux restent disponibles. Les noms historiques (`recipeDetails`, `ingredients`, `steps`, `quantity`, `unit`, `instruction`, `prepTimeMinutes`, `servings`) sont conservés.

## 7. Services modifiés

`CultureContentService` valide les IDs appartenant à la recette, refuse les relations croisées, normalise YouTube, protège les enrichissements omis par un ancien client et synchronise les collections sans réattribuer les UUID fournis. Les routes REST existantes restent inchangées.

## 8. Repositories modifiés

Ajout de `RecipeRevisions` avec recherche de la dernière révision active par contenu et état. Les repositories des autres domaines sont inchangés.

## 9. Nouveaux modèles

- `CultureRecipeRevision` : contenu canonique, auteur, état, version de base, snapshot JSON, dates, modérateur et motif.
- `RecipeEquipment` et `RecipeMedia`.
- Enums : difficulté, unité de quantité, unité de température, rôle média et statut de révision.

## 10. Modèle ingrédients

Implémenté : UUID, groupe, optionnel, substitution, minimum/maximum `BigDecimal`, unité canonique et conservation des textes historiques. La contrainte Java et SQL impose `maximum >= minimum`.

## 11. Modèle étapes

Implémenté : UUID, titre, durée, température structurée, résultat attendu, conseil, sécurité et référence média. Les durées restent de simples métadonnées ; aucun minuteur serveur n'a été créé.

## 12. IDs stables

Le backfill historique est déterministe et unique. À l'édition, un ID fourni est contrôlé contre la recette, un élément existant est synchronisé en place et une réorganisation conserve son UUID. Sans ID (ancien client), une compatibilité positionnelle est appliquée ; seule une requête moderne portant les UUID peut exprimer sans ambiguïté l'identité lors d'une réorganisation.

## 13. Modèle médias

Le modèle de références est implémenté, sans URL signée persistée et sans copie de fichier. L'association de nouveaux `mediaId` renvoie `503 MEDIA_VALIDATION_UNAVAILABLE` tant que `media-service` ne fournit pas un contrat interservice authentifié couvrant existence, MIME/type, statut, visibilité, propriétaire et droit d'usage Culture.

## 14. Validation YouTube

Validation par `URI`, HTTPS uniquement, aucun identifiant de connexion, liste exacte de domaines, ID vidéo de 11 caractères, aucune requête réseau et normalisation vers `youtube-nocookie.com/embed/{id}`. Les domaines ressemblants, HTTP, schémas dangereux et IDs invalides sont refusés.

## 15. Matériel

Collection ordonnée facultative avec UUID, nom et caractère optionnel. Aucune déduction automatique n'est faite depuis les instructions.

## 16. Modération des modifications publiées

Une mise à jour d'une recette `PUBLISHED` écrit une révision candidate au lieu de muter le contenu canonique. `submit` soumet cette révision. `review` applique la candidate et sa traduction uniquement à l'approbation, dans la transaction courante, après contrôle de `baseVersion`. Un rejet ne modifie pas la version publique. Une seule révision active est autorisée par recette au niveau SQL.

Limite : l'état de révision n'a pas été ajouté à `ContentResponse`; les endpoints restent compatibles, mais un futur champ de réponse additif sera nécessaire pour afficher précisément cet état au contributeur.

## 17. Sécurité

- Propriété du contenu et de la révision contrôlée côté serveur.
- IDs étrangers refusés.
- Association étape–ingrédient inter-recettes refusée en service et en base.
- Révision concurrente détectée par version optimiste.
- Candidats jamais exposés par les GET publics.
- Médias en fail-closed.

## 18. Compatibilité anciens clients

Les champs ajoutés sont nullables et les champs enrichis absents sont conservés lors d'un PUT ancien. Une collection explicitement vide efface la collection ; une collection enrichie absente est conservée. Avec les records Jackson actuels, omission et `null` explicite ne sont pas distinguables pour chaque texte : les deux signifient « conserver » lors d'une édition existante.

## 19. Compatibilité anciennes recettes

Les colonnes historiques et leurs données sont conservées. Les UUID sont ajoutés sans remplacer la clé historique `(content_id, display_order)`. Le test PostgreSQL de migration a été préparé, mais n'a pas pu être exécuté faute de moteur Docker disponible.

## 20. Transactions

L'approbation d'une révision, les détails, la traduction et la décision sont dans la même transaction Spring. Toute exception annule l'ensemble. Aucun appel distant média n'est fait dans la transaction.

## 21. Tests exécutés

- `mvn -pl culture-service -am test` : PASS.
- Résultat Culture : 21 tests, 0 échec, 0 erreur, 3 tests PostgreSQL ignorés.
- Validation YouTube : 2 tests PASS.
- Synchronisation UUID/réorganisation et plage invalide : 2 tests PASS.
- `git diff --check -- culture-service` : PASS.

## 22. Tests non exécutés

Les tests Testcontainers PostgreSQL ont été explicitement tentés avec `RUN_TESTCONTAINERS=true`, mais l'environnement ne dispose pas d'un moteur Docker accessible (`Could not find a valid Docker environment`). La migration PostgreSQL et le mapping Hibernate sur le schéma V6 restent donc non vérifiés en exécution.

Les scénarios bout-en-bout de révision, média autorisé/refusé via un vrai service et concurrence réelle n'ont pas été exécutés.

## 23. Limitations

- Validation distante média bloquée faute de contrat sûr.
- État de révision non exposé dans la réponse publique/contributeur.
- Distinction fine omission/null pour les textes non disponible avec le DTO record actuel.
- Aucun endpoint de recalcul des portions, aucune progression/minuterie serveur, conformément aux décisions produit.

## 24. Risques résiduels

Le principal risque est la migration V6 non exécutée sur PostgreSQL/Testcontainers. Avant déploiement, il faut faire passer Flyway et les tests de mapping sur une copie représentative du schéma V5, puis vérifier conservation des comptes, ordres, traductions et UUID.

## 25. Ajustements frontend nécessaires

Le mobile devra consommer les nouveaux champs sous `recipeDetails`, envoyer les UUID lors des éditions/réorganisations, conserver localement progression et minuteurs, et traiter YouTube avec une politique de confidentialité explicite. Un raccordement ultérieur devra exposer/afficher l'état de révision.

## 26. Services impactés

- Modifié : `culture-service` uniquement.
- Consulté uniquement : `media-service`, `interaction-service`, frontend mobile.
- Non modifiés : gateway, auth, interaction, media, notification et autres microservices.
- Contrat manquant : endpoint interservice média authentifié de validation d'association Culture.

## 27. État Git final

Les changements Culture listés ci-dessus sont non commités. Les changements préexistants hors Culture sont toujours présents. Aucun commit, push ou déploiement n'a été réalisé.

BACKEND_IMPLEMENTATION = PARTIAL

CULTURE_SERVICE_ONLY = YES

DATABASE_MIGRATION_CREATED = YES

DATABASE_MIGRATION_EXECUTED = NO

LEGACY_RECIPES_PRESERVED = NOT_TESTED

LEGACY_CLIENT_COMPATIBILITY = PARTIAL

RECIPE_DETAILS_EXTENDED = PASS

RECIPE_DIFFICULTY_ENUM = PASS

INGREDIENT_STRUCTURED_QUANTITIES = PASS

INGREDIENT_STABLE_IDS = PARTIAL

STEP_STABLE_IDS = PARTIAL

STEP_ENRICHMENT = PASS

STEP_INGREDIENT_RELATIONS = PARTIAL

RECIPE_EQUIPMENT = PASS

RECIPE_MEDIA_REFERENCES = PARTIAL

YOUTUBE_VALIDATION = PASS

MEDIA_AUTHORIZATION = FAIL

PUBLISHED_RECIPE_REVISION_WORKFLOW = PARTIAL

PUBLIC_VERSION_PRESERVED_DURING_REVIEW = NOT_TESTED

COMMENTS_CONTRACT_PRESERVED = YES

GATEWAY_CHANGED = NO

MEDIA_SERVICE_CHANGED = NO

INTERACTION_SERVICE_CHANGED = NO

FRONTEND_CHANGED = NO

TARGETED_TESTS = PASS

POSTGRES_MIGRATION_TESTS = NOT_RUN

SECURITY_TESTS = PARTIAL

EXISTING_GIT_CHANGES_PRESERVED = YES

COMMIT_CREATED = NO

PUSH_PERFORMED = NO

DEPLOYMENT_PERFORMED = NO
