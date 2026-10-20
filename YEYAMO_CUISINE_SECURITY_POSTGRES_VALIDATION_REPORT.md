# Yeyamo Cuisine — sécurité interservice et validation PostgreSQL

Date : 2026-10-19  
Dépôt : `yeyamo-api` — branche `main`, HEAD `4f7620a`

## 1. Architecture sécurité initiale

`culture-service` refusait toute nouvelle référence média avec `MEDIA_VALIDATION_UNAVAILABLE`. `media-service` ne proposait aucun contrat d'association Culture. Les JWT utilisateurs sont HMAC et le starter commun protège `/internal/**` avec un unique secret statique `X-Internal-Token`. Ce dernier ne porte ni identité de service, ni audience, ni scope, ni expiration : il n'est pas approprié comme autorisation Culture→Media.

| Élément | Avant | Risque | Après |
|---|---|---|---|
| Identité | secret déclaratif global | usurpation latérale | JWT signé `sub=culture-service` |
| Audience | absente | rejeu sur un autre service | `aud=media-service` |
| Scope | absent | privilèges excessifs | `media:associate:culture` |
| Expiration | absente | rejeu durable | 2 min, maximum 5 min |
| Rotation | remplacement brutal | interruption/compromission | clé courante d'émission + clé précédente de vérification |
| Acteur | identifiant de requête | substitution | claim signé `act`, comparé au contributeur |

## 2. Risques identifiés

- `X-Internal-Token` seul est insuffisant pour ce contrat.
- Le modèle média ne possède pas encore un état de modération générique ni une visibilité explicite persistée.
- Les anciens médias `usageType=null` ne peuvent pas être associés comme nouvelles références Cuisine : décision fail-closed.
- Le contrat direct `/service/**` doit rester inaccessible au Gateway et limité au réseau de services.
- La migration V6 n'a pas été exécutée sur PostgreSQL réel dans cette session (Docker indisponible).

## 3. Modifications effectuées

- Ajout d'un émetteur JWT dédié dans `culture-service`.
- Ajout d'un filtre de vérification dédié dans `media-service`.
- Ajout du contrat `POST /service/v1/media/culture/validate` (absent des routes Gateway).
- Ajout des usages contrôlés `RECIPE_HERO_IMAGE`, `RECIPE_GALLERY_IMAGE`, `RECIPE_STEP_IMAGE`, `RECIPE_VIDEO`.
- Raccordement de `heroMediaId`, `videoMediaId`, `gallery[].mediaId`, `steps[].mediaId` avant persistance/application d'une révision.
- Configuration uniquement par variables, sans secret en dur.
- Aucun changement dans `auth-service` : inutile pour une identité machine séparée des sessions utilisateurs.

## 4. Contrat d'identité interservice

En-tête : `X-Yeyamo-Service-Token`. Claims obligatoires : issuer et subject `culture-service`, audience `media-service`, scope `media:associate:culture`, `iat`, `exp`, `jti`, acteur `act`. Signature HS256 avec secret dédié à cette relation, minimum 32 octets. La clé précédente n'accepte que la vérification. Variables :

- `CULTURE_MEDIA_SERVICE_JWT_SECRET` — obligatoire sur les deux services ;
- `CULTURE_MEDIA_SERVICE_JWT_PREVIOUS_SECRET` — uniquement côté Media pendant rotation ;
- `CULTURE_MEDIA_SERVICE_JWT_TTL` — défaut `PT2M`, plafonné à cinq minutes ;
- `MEDIA_SERVICE_URL` — défaut Docker `http://media-service:8080`.

Sans clé, Culture reste fail-closed avec `MEDIA_VALIDATION_UNAVAILABLE`; Media rejette le contrat.

## 5. Contrat Media

Entrée : contributeur, indicateur de publication, liste `{mediaId, usage}`. Sortie par ID : `allowed`, `reason`, `publicationAllowed`. Contrôles : existence, propriétaire, état `READY`, non supprimé, MIME et type image/vidéo, consentement non refusé/non en attente, usage exact Cuisine, correspondance entre le claim signé `act` et le contributeur.

Les refus n'exposent ni propriétaire réel, ni chemin de stockage, ni URL signée.

## 6. Validation d'association

Toute nouvelle référence est validée avant synchronisation de l'agrégat recette. Une indisponibilité réseau ou une réponse incomplète devient 503 ; un refus métier devient 422. Aucun retry infini n'est introduit. La validation distante intervient avant la persistance finale et ne maintient pas volontairement une transaction SQL autour de retries.

Politique historique : les références déjà persistées et non renvoyées par un ancien client sont conservées par la logique d'enrichissement existante. Toute nouvelle valeur fournie doit passer le contrat.

## 7. Politique de visibilité

La validation n'altère jamais le média et ne le rend pas public. Elle vérifie seulement son aptitude à l'association/publication. Les recettes acceptent uniquement image/vidéo, actuellement stockées dans le compartiment public par `media-service`. Documents et certificats restent privés/signés et sont refusés.

Limitation : aucun champ de modération/visibilité générique n'existe dans `MediaAsset`; le contrôle complet « média approuvé » ne peut donc pas être déclaré PASS. Une migration/addition métier dédiée sera nécessaire avant préproduction.

## 8. Migration V6

Inspection statique : V6 crée/enrichit les UUID stables, équipements, galerie, relations étape–ingrédient et révisions avec timestamps de soumission/décision. Les tests Testcontainers existent pour Flyway/Hibernate et sont configurés pour PostgreSQL. V6 étant non suivie et non prouvée comme appliquée dans un environnement partagé, elle n'a pas été réécrite par cette passe.

## 9. Tests PostgreSQL

`docker info` échoue : daemon Docker Desktop absent. Les trois tests `CulturePostgresIntegrationTest` sont donc `SKIPPED`. Aucun `psql` local ni environnement de test fourni. Statut honnête : `POSTGRES_V6=NOT_RUN`, `HIBERNATE_MAPPING=NOT_RUN`.

## 10. Compatibilité historique

Les constructeurs de test historiques sont conservés. Les UUID existants, champs omis et enrichissements antérieurs restent gérés par la synchronisation actuelle. Les nouveaux usages média sont additifs. Les anciens médias sans `usageType` restent lisibles mais ne sont pas implicitement autorisés comme nouvelle association.

## 11. Tests de sécurité

- Compilation Culture + Media : PASS.
- Culture : 22 tests passés, 3 PostgreSQL ignorés faute de Docker.
- Tests ciblés Media : 9/9 PASS.
- Cas couverts : token absent, mauvaise audience, scope absent, expiration, token valide, propriétaire différent, acteur signé différent, média processing/supprimé, type/usage incorrects, association valide.
- Suite réacteur complète : interrompue par timeout à 120 s pendant les intégrations R2 après le succès Culture ; ce n'est pas présenté comme un échec fonctionnel des tests ciblés.

## 12. Limitations

- Modération et visibilité explicites absentes du modèle média.
- Pas de preuve réseau réelle Culture→Media dans Docker.
- Pas de test PostgreSQL V5→V6 exécuté.
- La révocation immédiate d'un jeton avant expiration repose sur la très courte TTL ; pas de registre distribué de `jti`.

## 13. Prérequis infrastructure

1. Générer un secret aléatoire dédié (jamais le JWT utilisateur ni `INTERNAL_SERVICE_TOKEN`).
2. Le distribuer aux deux services via le gestionnaire de secrets.
3. Restreindre `/service/v1/media/**` au réseau interne et confirmer son absence du Gateway.
4. Pour rotation : déployer ancienne+nouvelle clé sur Media, nouvelle sur Culture, puis retirer l'ancienne après la TTL maximale.
5. Exécuter V5→V6 sur PostgreSQL isolé et valider Hibernate avant déploiement.
6. Ajouter au modèle média une visibilité et une décision de modération explicites.

## 14. État Git

Les changements Auth, Culture et rapports déjà présents ont été préservés. Cette passe modifie uniquement Culture/Media et crée ce rapport. Aucun reset, commit, push ou déploiement.

## 15. Verdict

```text
CUISINE_SECURITY = PARTIAL
INTERSERVICE_IDENTITY = PASS
MEDIA_ASSOCIATION = PASS
MEDIA_VISIBILITY = PARTIAL
POSTGRES_V6 = NOT_RUN
HIBERNATE_MAPPING = NOT_RUN
LEGACY_COMPATIBILITY = PARTIAL
SECURITY_TESTS = PASS
CUISINE_PREPRODUCTION = NO_GO
```

Le NO_GO reste obligatoire tant que PostgreSQL V6/Hibernate et la politique de modération/visibilité média ne sont pas validés.
