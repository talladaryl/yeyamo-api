# YEYAMO — Proposition backend pour la fiche recette

> Proposition uniquement. Aucune modification backend ou base de données n'a été appliquée.

## 1. Modèles concernés

- `culture-service/.../domain/RecipeDetailsEntity.java`
- `culture-service/.../application/CultureDtos.java`
- `culture-service/.../application/CultureContentService.java`
- repositories Culture directement liés si de nouvelles entités sont retenues.
- `media-service` et `interaction-service` restent inchangés; leurs contrats actuels doivent être réutilisés.

## 2. Champs à ajouter

Dans les détails recette, tous nullables : `cookTimeMinutes`, `restTimeMinutes`, `difficulty`, `yieldText`, `heroMediaId`, `videoMediaId`, `preparationAhead`, `plating`, `storage`, `reheating`, `tips`, `variants`, `accompaniments`, `safetyNotes`.

Étape enrichie : UUID stable, titre, durée minutes, température valeur/unité ou texte, résultat attendu, conseil, précaution, mediaId. Ingrédient enrichi : UUID stable, groupe, optionnel, substitution, quantité numérique/plage optionnelle et unité canonique, tout en conservant quantité/unité texte.

## 3. Tables à modifier

Ajouter les colonnes simples nullables à `culture_recipe_details`. Ne pas modifier V4; créer une nouvelle migration. Ne pas supprimer `prep_time_minutes`, `servings` ni les colonnes historiques.

## 4. Tables éventuelles à créer

- `culture_recipe_media(id, content_id, media_id, role, caption, alt_text, display_order)`.
- `culture_recipe_equipment(id, content_id, name, optional, display_order)`.
- Tables de conseils typés si les champs texte/tableaux ne suffisent pas.
- Liaison `culture_recipe_step_ingredients(step_id, ingredient_id)` si exigée par le mode guidé.

## 5. Stratégie des IDs stables

Ajouter un UUID aux lignes ingrédients/étapes tout en conservant la contrainte d'ordre historique. Backfill UUID pour chaque ligne existante. Garder une contrainte unique `(content_id, display_order)` pendant la transition. Les DTO acceptent un ID absent en création et le serveur le génère; en édition, l'ID existant stabilise la progression.

## 6. Stratégie des médias

Stocker seulement les UUID de `media-service`, jamais les URLs signées. Valider existence, type, usage Culture et propriété/droit d'utilisation lors de l'écriture. Exposer les IDs; le mobile résout metadata, thumbnail et contenu avec les endpoints actuels. Aucun nouveau service vidéo.

## 7. DTO request/response

Étendre `RecipeDetailsRequest/Response`, `RecipeIngredientRequest/Response` et `RecipeStepRequest/Response`. Champs nouveaux facultatifs. Conserver les noms/champs actuels. Les réponses peuvent ajouter des champs JSON sans casser le mobile; les requêtes anciennes restent valides.

## 8. Compatibilité anciennes recettes

Toutes les colonnes nouvelles sont nullables. Les recettes V4 gardent leurs listes et ordres. Les sections frontend restent conditionnelles. Aucun backfill métier inventé pour temps, températures ou sécurité.

## 9. Compatibilité anciens clients

Ne rendre aucun nouveau champ obligatoire. Conserver les endpoints `/culture/contents`, `/contents/{id}`, `/translations`, `/contributions` et les UUID. Ignorer uniquement les champs absents; ne jamais modifier la sémantique de `prepTimeMinutes`/`servings`.

## 10. Migrations Flyway

Créer V6 ou prochaine version réellement disponible après revalidation. Étapes : colonnes nullables; UUIDs temporaires; backfill; contraintes NOT NULL uniquement sur ID technique après backfill; nouvelles tables/index; contraintes FK. Ne pas toucher `V4__structured_culture_content.sql`.

## 11. Validation

- Durées entières positives avec limites raisonnables.
- Portions positives.
- Enum difficulté versionné.
- Quantité numérique facultative, précision documentée, unité canonique facultative.
- Media UUID/type/ownership.
- Ordres uniques et collections plafonnées.
- HTTPS/allowlist uniquement si une URL externe est finalement autorisée.

## 12. Sécurité

Préserver GET public filtré par publication/visibilité/sensibilité. Écriture authentifiée propriétaire/admin. Ne pas exposer d'URL privée brute. Vérifier les médias avant association et empêcher l'association d'un média non autorisé.

## 13. Modération

Les nouveaux textes et médias suivent le workflow Culture existant. Les précautions alimentaires doivent être saisies et revues; aucune génération automatique. Une modification d'une recette publiée doit respecter le workflow éditorial choisi.

## 14. Tests requis

- Migration sur schéma V4 contenant des recettes historiques.
- Sérialisation ancien/nouveau payload.
- création/édition minimale et riche.
- validation des limites/UUID média.
- ordre stable et conservation IDs après édition.
- permissions propriétaire/admin et lecture publique.
- publication/sensibilité/traductions.
- rollback de transaction sur média invalide.

## 15. Risques

Perte d'ordre pendant migration, duplication d'IDs, recalcul erroné des quantités, payloads trop volumineux, références médias orphelines, incompatibilité client si un champ devient obligatoire et confusion entre texte historique et valeur normalisée.

## 16. Ordre d'implémentation

1. Décisions produit et schéma.
2. Migration additive et entités.
3. DTO/mapping/service.
4. validations média et sécurité.
5. tests backend.
6. types/API mobile puis éditeur.
7. activation UI conditionnelle.
8. tests de non-régression et déploiement progressif.

## 17. Services à redéployer ultérieurement

- Obligatoire : `culture-service`.
- Gateway : non, sauf changement de routage non recommandé.
- `media-service`/`interaction-service` : non si leurs contrats existants sont réutilisés.
- Mobile : nouvelle version après disponibilité du contrat.

## 18. Décisions du propriétaire

1. Médias exclusivement Yeyamo ou URLs YouTube externes autorisées ?
2. Difficulté sous forme d'enum global ou vocabulaire éditorial configurable ?
3. Timer avec notification et restauration après fermeture ?
4. Les quantités doivent-elles gérer fractions et plages ?
5. Commentaires génériques uniquement ou notation culinaire distincte ?
6. Une modification de recette publiée repasse-t-elle systématiquement en modération ?

Statut : `BACKEND_PROPOSAL_READY = YES`, `BACKEND_CHANGES_APPLIED = NO`, `DATABASE_MIGRATION_APPLIED = NO`.
