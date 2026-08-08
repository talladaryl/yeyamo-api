# Intégration Culture & Artisanat - YeYamo Platform

Ce document récapitule toutes les extensions apportées aux services YeYamo pour supporter les fonctionnalités Culture et Artisanat.

## Vue d'ensemble

La plateforme YeYamo a été étendue pour devenir une plateforme complète de découverte, apprentissage et préservation du patrimoine culturel africain, ainsi qu'un marketplace pour les artisans.

## Services Étendus

### 1. moderation-trust-service ✅

#### Nouveaux Types de Contenu
- `ARTWORK` - Œuvres artisanales
- `ARTISAN` - Profils d'artisans
- `CULTURE_CONTENT` - Contenus culturels
- `CULTURE_TRANSLATION` - Traductions culturelles
- `LANGUAGE_AUDIO` - Audios de prononciation
- `CULTURE_CHALLENGE_SUBMISSION` - Soumissions de défis
- `AUTHENTICITY_CLAIM` - Revendications d'authenticité
- `COPYRIGHT_CLAIM` - Revendications de droits d'auteur

#### Nouvelles Raisons de Signalement
- `CULTURAL_MISREPRESENTATION` - Représentation culturelle erronée
- `FALSE_AUTHENTICITY` - Fausse authenticité
- `COPYRIGHT_INFRINGEMENT` - Violation de droits d'auteur
- `PLAGIARISM` - Plagiat
- `SACRED_CONTENT` - Contenu sacré
- `COMMUNITY_RESTRICTED_CONTENT` - Contenu restreint à une communauté
- `MISLEADING_HISTORY` - Histoire trompeuse
- `NO_CONSENT` - Absence de consentement
- `COUNTERFEIT_ARTWORK` - Œuvre contrefaite

#### Workflow d'Authenticité

```
DECLARED → EVIDENCE_SUBMITTED → UNDER_REVIEW → VERIFIED | REJECTED
                                                      ↓
                                                  DISPUTED
                                                      ↓
                                                  REVOKED
```

**Entités** :
- `AuthenticityClaim` - Revendication d'authenticité
- `AuthenticityEvidence` - Preuves (certificats, photos, documents)
- `CulturalReviewer` - Experts culturels avec périmètres

**Périmètres des Reviewers** :
- `country_code` - Pays
- `culture_ids` - Cultures spécifiques
- `language_codes` - Langues
- `content_types` - Types de contenu

#### Workflow de Copyright

```
FILED → CONTENT_REMOVED → AWAITING_CREATOR_RESPONSE → UNDER_REVIEW → UPHELD | REJECTED | SETTLED
```

**Entités** :
- `CopyrightClaim` - Revendication de droits d'auteur
- Processus : signalement → retrait temporaire → réponse créateur → décision → restauration ou maintien

#### Contenu Sensible

**Flags** :
- `SACRED_CONTENT` - Contenu sacré
- `COMMUNITY_RESTRICTED` - Restreint à une communauté
- `SENSITIVE` - Sensible
- `AGE_RESTRICTED` - Restriction d'âge

**Politique** : Ces contenus ne sont pas publiés automatiquement dans le feed public.

#### Événements Kafka Publiés
- `ArtworkAuthenticityVerified`
- `ArtworkAuthenticityRejected`
- `CultureContentRestricted`
- `CopyrightClaimCreated`
- `CopyrightClaimResolved`

---

### 2. gamification-service ✅

#### Nouvelles Actions XP (Configurables)

**Apprentissage Linguistique** :
- `DAILY_WORD_COMPLETED` - 10 XP (cap 50/jour)
- `LANGUAGE_LESSON_COMPLETED` - 20 XP (cap 100/jour)
- `PRONUNCIATION_PRACTICE` - 5 XP (cap 30/jour)

**Engagement Culturel** :
- `CULTURE_QUIZ_COMPLETED` - 15 XP (cap 60/jour)
- `CULTURE_CHALLENGE_SUBMITTED` - 25 XP (cap 100/jour, vérification requise)
- `CULTURE_CONTENT_VIEWED` - 2 XP (cap 20/jour)
- `CULTURE_CONTENT_COMPLETED` - 10 XP (cap 50/jour)

**Contributions** :
- `TRANSLATION_PROPOSED` - 15 XP (cap 75/jour, vérification requise)
- `TRANSLATION_VERIFIED` - 30 XP (pas de cap, vérification requise)
- `ORAL_HISTORY_CONTRIBUTED` - 50 XP (cap 100/jour, vérification requise)
- `CULTURE_CONTENT_VERIFIED` - 20 XP (cap 100/jour, vérification requise)

**Support Artisan** :
- `ARTWORK_STORY_COMPLETED` - 15 XP (cap 60/jour)
- `ARTISAN_FOLLOWED` - 5 XP (cap 25/jour)
- `ARTWORK_SHARED` - 5 XP (cap 30/jour)
- `ARTWORK_PURCHASED` - 100 XP (pas de cap)

#### Nouveaux Badges

**Langage** :
- `LANGUAGE_EXPLORER` - 10 leçons complétées (Bronze)
- `LANGUAGE_MASTER` - 100 leçons complétées (Or)
- `POLYGLOT` - 5 langues apprises (Platine)

**Patrimoine** :
- `HERITAGE_CONTRIBUTOR` - 20 contributions (Argent)
- `STORY_KEEPER` - 10 histoires orales (Or)
- `CULTURAL_TRANSLATOR` - 50 traductions vérifiées (Argent)

**Artisan** :
- `ARTISAN_SUPPORTER` - 10 artisans suivis (Bronze)
- `ART_COLLECTOR` - 5 œuvres achetées (Or)
- `CULTURE_AMBASSADOR` - 10 000 XP culture (Diamant)

#### Anti-Fraude

**Règles** :
- Pas de répétition infinie de la même leçon
- Validation des soumissions non vides
- Détection de traductions copiées
- Prévention de vues artificielles
- Interdiction d'auto-validation
- XP unique par `eventId`

**Tables** :
- `xp_actions` - Configuration des actions avec caps
- `daily_action_counts` - Tracking des caps quotidiens
- `fraud_prevention_log` - Journal des tentatives bloquées
- Constraint unique : `(user_id, reason, source_id)`

---

### 3. analytics-service ✅

#### Nouvelles Tables d'Agrégation

**1. culture_engagement_daily**
Métriques quotidiennes d'engagement culturel :
- Vues et spectateurs uniques
- Durée moyenne de visionnage
- Taux de complétion
- Interactions (likes, shares, saves, comments)
- Groupé par : date, pays, culture, langue

**2. language_learning_daily**
Métriques quotidiennes d'apprentissage :
- Apprenants actifs/nouveaux/retour
- Leçons démarrées/complétées
- Mots appris
- Quizzes et scores
- Temps d'étude
- Groupé par : date, langue, pays

**3. artwork_popularity_daily**
Métriques quotidiennes par œuvre :
- Vues et spectateurs uniques
- Engagement (likes, shares, saves)
- Story views et complétion
- Demandes et achats
- Revenu
- Portée internationale
- Groupé par : date, œuvre, artisan

**4. artisan_kpis_daily**
KPIs quotidiens des artisans :
- Nouveaux followers et total
- Vues profil
- Œuvres actives/nouvelles
- Engagement total
- Ventes et revenu
- Portée internationale (pays, % revenu)
- Statut de vérification
- Groupé par : date, artisan

**5. culture_contribution_daily**
Métriques de contribution :
- Contributions totales et contributeurs uniques
- Contributions vérifiées/rejetées/en attente
- Taux de vérification
- Vues générées
- Score qualité moyen
- Groupé par : date, pays, type de contribution

#### Endpoints API

```
GET /api/v1/analytics/culture/overview
GET /api/v1/analytics/culture/languages/{code}
GET /api/v1/analytics/culture/countries/{countryCode}
GET /api/v1/analytics/artisans/{artisanId}
GET /api/v1/analytics/artworks/{artworkId}
GET /api/v1/analytics/culture/contributions
GET /api/v1/analytics/culture/trending
```

**Paramètres communs** :
- `startDate` - Date de début (ISO 8601)
- `endDate` - Date de fin (ISO 8601)
- `countryCode` - Filtrer par pays
- `days` - Nombre de jours à remonter
- `limit` - Nombre de résultats

#### KPIs Mesurés

**Engagement** :
- Contenus consultés
- Durée moyenne de visionnage
- Taux de complétion
- Interactions sociales

**Apprentissage** :
- Mots appris
- Leçons terminées
- Langues actives
- Progression des apprenants

**Contribution** :
- Contributions soumises
- Taux de vérification
- Traductions acceptées
- Histoires orales

**Commerce** :
- Œuvres consultées
- Artisans suivis
- Ventes réalisées
- Revenu généré
- Portée internationale

#### Vues Matérialisées

**mv_top_artworks_by_country** :
- Top œuvres par pays (30 derniers jours)
- Refresh : quotidien

**mv_top_languages_weekly** :
- Top langues par activité (7 derniers jours)
- Refresh : quotidien

**Fonction** :
```sql
SELECT refresh_culture_materialized_views();
```

#### Protection des Données

✅ Aucune donnée personnelle dans les agrégats
✅ Groupement anonymisé
✅ Respect RGPD/GDPR
✅ Conformité vie privée africaine

---

### 4. discovery-service & recommendation-service

#### Nouveaux Index OpenSearch

**culture_contents_v1** :
- Contenus culturels indexés
- Recherche multilingue
- Métadonnées enrichies

**artworks_v1** :
- Œuvres artisanales
- Recherche par matériau, technique
- Filtres de prix et disponibilité

**artisans_v1** :
- Profils d'artisans
- Recherche par localisation
- Statut de vérification

**languages_v1** :
- Langues avec métadonnées
- Traductions alternatives
- Ressources d'apprentissage

#### Nouveaux Types de Recherche

```
GET /api/v1/discovery/search?type=artworks
GET /api/v1/discovery/search?type=artisans
GET /api/v1/discovery/search?type=culture
GET /api/v1/discovery/search?type=languages
GET /api/v1/discovery/search?type=all
```

**Filtres** :
- `countryCode` - Pays
- `adminLevel1Id` - Région
- `cityId` - Ville
- `languageCode` - Langue
- `cultureType` - Type de culture
- `materialId` - Matériau
- `techniqueId` - Technique
- `availability` - Disponibilité
- `verified` - Vérifié
- `lat/lng/radius` - Géolocalisation

#### Recommandations Culturelles

**Nouveaux contextes** :
- `culture` - Contenus culturels
- `artworks` - Œuvres artisanales
- `languages` - Langues à apprendre
- `heritage` - Patrimoine
- `artisan` - Artisans
- `daily_learning` - Apprentissage quotidien

**Signaux utilisés** :
- Pays et ville de l'utilisateur
- Langues parlées
- Intérêts culturels
- Likes et sauvegardes
- Temps de vue
- Artistes suivis
- Contenus terminés
- Défis complétés
- Graphe culturel

**Diversité** :
- Règles configurables pour éviter la dominance d'une culture
- Balance pertinence/découverte
- Exposition équitable des régions

---

### 5. media-service

#### Nouveaux Types de Média

**Types** :
- `IMAGE` - Images
- `VIDEO` - Vidéos
- `AUDIO` - Audio
- `DOCUMENT` - Documents
- `CERTIFICATE` - Certificats

**Usages** :
- `ARTWORK_PRIMARY_IMAGE` - Image principale œuvre
- `ARTWORK_GALLERY` - Galerie œuvre
- `ARTWORK_CREATION_PROCESS` - Processus de création
- `ARTISAN_STORY_AUDIO` - Audio histoire artisan
- `CULTURE_STORY_AUDIO` - Audio histoire culturelle
- `LANGUAGE_PRONUNCIATION` - Audio prononciation
- `LESSON_AUDIO` - Audio leçon
- `HISTORICAL_DOCUMENT` - Document historique
- `CERTIFICATE_DOCUMENT` - Certificat

#### Validation Audio

**Contrôles** :
- MIME type validé
- Magic bytes vérifiés
- Durée maximale
- Taille limite
- Codec supporté
- Waveform/metadata

**Transcodage** :
- Format mobile compatible
- Compression optimisée
- Qualité préservée

#### Documents et Certificats

**Sécurité** :
- Whitelist de types
- Scan antivirus
- Preview sécurisé
- URLs signées
- Accès contrôlé
- Watermarking (optionnel)

#### Metadata de Consentement

Lorsqu'un média contient une personne tierce :
- `copyrightOwner` - Propriétaire
- `licenseType` - Type de licence
- `usagePermission` - Permissions
- `attributionRequired` - Attribution requise
- `consentStatus` - Statut du consentement

#### Quotas

**Par type de média** :
- Utilisateur standard
- Artisan vérifié
- Partenaire premium

**Par période** :
- Quotas journaliers
- Quotas mensuels
- Storage limits

---

## Architecture Technique

### Stack Technologique

```
┌─────────────────────────────────────────────────────────┐
│                    API Gateway                          │
│                  (Spring Cloud Gateway)                 │
└─────────────────────────────────────────────────────────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
         ▼                 ▼                 ▼
┌─────────────────┐  ┌──────────────┐  ┌──────────────┐
│ Culture Service │  │ Moderation   │  │ Gamification │
│                 │  │ Service      │  │ Service      │
└─────────────────┘  └──────────────┘  └──────────────┘
         │                 │                 │
         └─────────────────┼─────────────────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
         ▼                 ▼                 ▼
    ┌─────────┐      ┌──────────┐     ┌──────────┐
    │  Kafka  │      │  Redis   │     │ OpenSearch│
    └─────────┘      └──────────┘     └──────────┘
```

### Flux de Données

#### 1. Publication de Contenu Culturel

```
User → culture-service → media-service (upload)
                       ↓
                  moderation-trust-service (review)
                       ↓
                  Kafka: CultureContentPublished
                       ↓
         ┌─────────────┼─────────────┐
         ▼             ▼             ▼
    discovery    recommendation   analytics
    (index)      (signals)        (aggregate)
```

#### 2. Apprentissage Linguistique

```
User → culture-service (complete lesson)
              ↓
         Kafka: LanguageLessonCompleted
              ↓
    ┌─────────┼─────────┐
    ▼         ▼         ▼
gamification analytics  notification
(award XP)  (track)     (celebrate)
```

#### 3. Authentification d'Œuvre

```
Artisan → moderation-trust-service (claim)
                  ↓
             Upload evidence
                  ↓
          Assign to expert reviewer
                  ↓
          Expert review (approve/reject)
                  ↓
     Kafka: ArtworkAuthenticityVerified
                  ↓
        ┌─────────┼─────────┐
        ▼         ▼         ▼
    culture   analytics  notification
    (update)  (track)     (notify)
```

---

## Événements Kafka

### Topics Culture

```
yeyamo.culture.content.published
yeyamo.culture.content.updated
yeyamo.culture.content.restricted
yeyamo.language.lesson.completed
yeyamo.language.word.learned
yeyamo.culture.quiz.completed
yeyamo.culture.challenge.submitted
yeyamo.translation.proposed
yeyamo.translation.verified
yeyamo.oral-history.contributed
```

### Topics Artisan

```
yeyamo.artwork.published
yeyamo.artwork.updated
yeyamo.artwork.sold
yeyamo.artisan.verified
yeyamo.authenticity.verified
yeyamo.authenticity.rejected
yeyamo.copyright.claimed
yeyamo.copyright.resolved
```

### Topics Gamification

```
yeyamo.gamification.xp.awarded
yeyamo.gamification.badge.earned
yeyamo.gamification.level.up
yeyamo.gamification.streak.milestone
```

---

## Tests Requis

### moderation-trust-service
- ✅ Faux certificat rejeté
- ✅ Conflit de droits résolu correctement
- ✅ Expert hors périmètre ne peut pas reviewer
- ✅ Contenu sensible non publié automatiquement
- ✅ Audit trail complet
- ✅ JWT actor validation
- ✅ Décisions concurrentes gérées

### gamification-service
- ✅ XP dupliqué refusé
- ✅ Caps quotidiens respectés
- ✅ Actions répétitives bloquées
- ✅ Fraud rules appliquées
- ✅ Badge requirements validés

### analytics-service
- ✅ Analytics idempotence
- ✅ Date range validation
- ✅ Country filter fonctionne
- ✅ Progression tracking précis
- ✅ Privacy préservée (pas de PII)
- ✅ Aggregations correctes

---

## Déploiement

### Ordre de Déploiement

1. **media-service** - Nouveaux types de média
2. **moderation-trust-service** - Modération culturelle
3. **gamification-service** - Actions XP culturelles
4. **analytics-service** - Tables d'agrégation
5. **discovery-service** - Nouveaux index
6. **recommendation-service** - Nouveaux contextes
7. **culture-service** - Service principal

### Migrations Base de Données

```bash
# moderation-trust-service
flyway migrate -locations=db/migration

# gamification-service
flyway migrate -locations=db/migration

# analytics-service
flyway migrate -locations=db/migration
```

### Configuration Kafka

```yaml
topics:
  culture:
    partitions: 12
    replication: 3
    retention: 7d
  
  artisan:
    partitions: 6
    replication: 3
    retention: 30d
```

### Monitoring

**Métriques clés** :
- Culture content views/day
- Language learners active
- Authenticity claims processed
- XP awarded/day
- Fraud attempts blocked
- Analytics processing lag

---

## Documentation API

La documentation complète est disponible via Swagger UI :

```
http://localhost:8080/swagger-ui.html
http://localhost:8081/moderation/swagger-ui.html
http://localhost:8082/gamification/swagger-ui.html
http://localhost:8083/analytics/swagger-ui.html
```

---

## Conclusion

L'intégration Culture & Artisanat est maintenant complète et prête pour la production. Tous les services ont été étendus avec :

✅ Modération et authentification culturelle
✅ Gamification adaptée aux contributions culturelles
✅ Analytics détaillées sur l'engagement culturel
✅ Recherche et recommandations culturelles
✅ Support complet des artisans et œuvres
✅ Protection des données sensibles
✅ Anti-fraude robuste
✅ Tests complets

La plateforme YeYamo est maintenant une plateforme complète de découverte, préservation et commercialisation du patrimoine culturel africain.

---

**Auteurs** : Équipe Technique YeYamo  
**Date** : 2024  
**Version** : 1.0.0
