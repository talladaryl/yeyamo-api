# Contrats communs de domaine YeYamo

## Objectif

Le module Maven `domain-foundation` fournit des value objects Java 21 immuables et sans dépendance Spring/JPA. Il peut être consommé par les services Culture, Artisanat, Catalogue, Commerce et Géographie sans partager leurs entités ni leurs tables.

## Standards validés

| Objet | Contrat | Validation |
|---|---|---|
| `CountryReference` | `countryCode` | ISO 3166-1 alpha-2, normalisé en majuscules |
| `AdministrativeAreaReference` | pays, niveau 1 à 3, identifiant | Pays valide et identifiant non vide |
| `GeoReference` | pays, niveaux administratifs, ville, localité, latitude/longitude | Coordonnées fournies ensemble; latitude `[-90,90]`, longitude `[-180,180]` |
| `LanguageReference` | `languageCode` | BCP 47, normalisé via `Locale.toLanguageTag()` |
| `LocalizedText` | langue, titre, description | Langue valide et titre obligatoire |
| `Money` | `BigDecimal amount`, devise | ISO 4217 et précision compatible avec la devise |
| `MediaReference` | UUID média, type, texte alternatif, ordre | Référence obligatoire, ordre positif ou nul; aucune URL persistée |

Les timestamps métier sont des `Instant` UTC. Les fuseaux ne sont appliqués qu’aux frontières d’affichage ou de planification.

## API du référentiel

Propriétaire : `place-service`.

| Méthode | Route | Réponse |
|---|---|---|
| `GET` | `/api/v1/countries` | Pays actifs, y compris `COMING_SOON`, avec statut de lancement |
| `GET` | `/api/v1/countries/{countryCode}` | Pays demandé |
| `GET` | `/api/v1/countries/{countryCode}/administrative-areas` | Régions actives du pays (niveau 1 dans le modèle actuel) |
| `GET` | `/api/v1/countries/{countryCode}/languages` | Langues référencées, langue principale en premier |

Erreurs : `COUNTRY_CODE_INVALID` (`400`) et `COUNTRY_NOT_FOUND` (`404`).

## Migration

`V3__country_reference_foundation.sql` crée le référentiel, marque le Cameroun `LIVE`, prépare neuf pays `COMING_SOON`, ajoute `regions.country_code` et conserve les clés historiques. Le backfill `CM` est appliqué aux régions du jeu historique, réputé camerounais. La colonne reste nullable dans cet incrément afin de permettre l’import et l’audit de données ambiguës avant une future contrainte `NOT NULL`.

La table `geographic_migration_issues` journalise les futures ambiguïtés sans les corriger arbitrairement.

`RegionRequest.countryCode` est désormais accepté. Pour préserver les anciens clients pendant la migration, une création qui omet encore ce champ reçoit temporairement `CM`; une mise à jour qui l’omet conserve la valeur existante. Ce fallback de compatibilité doit être retiré après migration des clients et activation d’une contrainte obligatoire.

## Événements

Les payloads communs sont dans `event-contracts` :

- `CountryEnabledPayload`;
- `CountryConfigurationUpdatedPayload`;
- `LanguageReferenceUpdatedPayload`.

Ils doivent être enveloppés dans `EventEnvelope` et écrits dans l’outbox de `place-service` lors de l’ajout futur des mutations administratives. Les endpoints de cet incrément sont en lecture seule et n’émettent donc aucun faux événement.
