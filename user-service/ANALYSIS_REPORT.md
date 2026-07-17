# Analyse Préalable - user-service (Étape 0)

**Date:** 17 juillet 2026  
**Objectif:** Audit complet avant ajout Social Graph + Agrégateur Profil

---

## 📋 Tableau de Constat

| Élément recherché | Statut trouvé | Action décidée |
|---|---|---|
| **Table `follows`** | ❌ **ABSENT** | ✅ **CRÉER** avec migration V2 |
| **Table `blocks`** | ❌ **ABSENT** | ✅ **CRÉER** avec migration V2 |
| **Table `suggestions`** (cache) | ❌ **ABSENT** | ⚠️ **NE PAS CRÉER** - Calculer à la volée |
| **Entité JPA `FollowEntity`** | ❌ **ABSENT** | ✅ **CRÉER** |
| **Entité JPA `BlockEntity`** | ❌ **ABSENT** | ✅ **CRÉER** |
| **Contrôleur `/api/v1/social/*`** | ❌ **ABSENT** | ✅ **CRÉER** `SocialGraphController` |
| **Endpoint `GET /social/following`** | ❌ **ABSENT** | ✅ **CRÉER** |
| **Endpoint `GET /social/followers`** | ❌ **ABSENT** | ✅ **CRÉER** |
| **Endpoint `GET /social/suggestions`** | ❌ **ABSENT** | ✅ **CRÉER** |
| **Endpoint `POST /social/users/{id}/follow`** | ❌ **ABSENT** | ✅ **CRÉER** |
| **Endpoint `DELETE /social/users/{id}/follow`** | ❌ **ABSENT** | ✅ **CRÉER** |
| **Endpoint `DELETE /social/followers/{id}`** | ❌ **ABSENT** | ✅ **CRÉER** |
| **Endpoint `GET /social/activity`** | ❌ **ABSENT** | ✅ **CRÉER** |
| **Endpoint `GET /social/settings`** | ❌ **ABSENT** | ✅ **CRÉER** (réutilise `visibility`) |
| **Endpoint `PUT /social/settings`** | ❌ **ABSENT** | ✅ **CRÉER** (réutilise `visibility`) |
| **Contrôleur `/api/v1/profile/*`** | ❌ **ABSENT** | ✅ **CRÉER** `ProfileAggregatorController` |
| **Endpoint `GET /profile/publications`** | ❌ **ABSENT** | ✅ **CRÉER** (agrégateur) |
| **Endpoint `GET /profile/favorites`** | ❌ **ABSENT** | ✅ **CRÉER** (agrégateur) |
| **Endpoint `GET /profile/events`** | ❌ **ABSENT** | ✅ **CRÉER** (agrégateur) |
| **Endpoint `GET /profile/reservations`** | ❌ **ABSENT** | ✅ **CRÉER** (agrégateur) |
| **Endpoint `GET /profile/reviews`** | ❌ **ABSENT** | ✅ **CRÉER** (agrégateur) |
| **Endpoint `GET /profile/stats`** | ❌ **ABSENT** | ✅ **CRÉER** (agrégateur) |
| **Événement Kafka `UserFollowed`** | ❌ **ABSENT** | ✅ **CRÉER** |
| **Événement Kafka `UserUnfollowed`** | ❌ **ABSENT** | ✅ **CRÉER** |
| **Dépendance Resilience4j** | ❌ **ABSENT** | ✅ **AJOUTER** pour circuit breaker |
| **Dépendance Spring Data Redis** | ❌ **ABSENT** | ✅ **AJOUTER** pour cache agrégateur |

---

## ✅ Éléments Existants (À Réutiliser)

### 1. Migrations Flyway
- **V1__create_user_profile_schema.sql** ✅ Existant
  - Tables: `user_profiles`, `outbox_events`, `processed_events`

### 2. Entités JPA
- **UserProfileEntity** ✅ Existant
  - Champs: `id`, `auth_user_id`, `display_name`, `avatar_url`, `bio`, `language`, `visibility`, `status`, etc.
  - Champ **`visibility`** (ProfileVisibility enum) ✅ **À RÉUTILISER pour settings**
- **OutboxEventEntity** ✅ Existant
- **ProcessedEventEntity** ✅ Existant

### 3. Contrôleurs REST
- **UserProfileController** ✅ Existant
  - Endpoints:
    - `GET /api/v1/users/me`
    - `PUT /api/v1/users/me`
    - `PATCH /api/v1/users/me/preferences`
    - `DELETE /api/v1/users/me`
    - `GET /api/v1/users/{id}`
    - `GET /api/v1/users?q={query}` (recherche publique)

### 4. Enum ProfileVisibility
```java
public enum ProfileVisibility {
    PUBLIC,
    FOLLOWERS_ONLY,
    PRIVATE
}
```
✅ **RÉUTILISER** pour `/api/v1/social/settings`

### 5. Pattern Outbox
- **OutboxPort** ✅ Existant (interface)
- **JpaOutboxAdapter** ✅ Existant (implémentation)
- **OutboxPublisher** ✅ Existant (scheduler Kafka)
- **Topic Kafka:** `user-events` ✅ Existant

### 6. Dépendances Maven
✅ Présentes:
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-kafka`
- `spring-boot-starter-security-oauth2-resource-server`
- `spring-boot-starter-validation`
- `spring-cloud-starter-netflix-eureka-client`
- `caffeine` (cache local)

❌ Absentes (à ajouter):
- `spring-boot-starter-data-redis`
- `spring-cloud-starter-circuitbreaker-resilience4j`
- `resilience4j-reactor` (optionnel)

---

## 🎯 Décision Finale

**AUCUN élément social graph ou agrégateur profil n'existe dans le code actuel.**

✅ **Autorisation de procéder à l'étape 1** (création Social Graph)  
✅ **Autorisation de procéder à l'étape 2** (création Agrégateur Profil)

### Points d'Attention
1. ⚠️ **Pas de duplication** : Réutiliser `visibility` existant au lieu de créer un nouveau champ
2. ⚠️ **Pattern Outbox** : Utiliser le mécanisme existant (`OutboxPort`)
3. ⚠️ **Topic Kafka** : Publier sur `user-events` déjà existant
4. ⚠️ **Pagination** : Adopter le style cursor déjà utilisé dans feed-service

---

## 📦 Structure Cible Après Implémentation

```
user-service/
├── src/main/resources/db/migration/
│   ├── V1__create_user_profile_schema.sql       [EXISTANT]
│   └── V2__create_social_graph_schema.sql       [NOUVEAU]
│
├── domain/model/
│   ├── UserProfile.java                         [EXISTANT]
│   ├── Follow.java                              [NOUVEAU]
│   ├── Block.java                               [NOUVEAU]
│   └── SocialActivity.java                      [NOUVEAU]
│
├── domain/port/
│   ├── UserProfileRepository.java               [EXISTANT]
│   ├── FollowRepository.java                    [NOUVEAU]
│   └── BlockRepository.java                     [NOUVEAU]
│
├── application/
│   ├── UserProfileService.java                  [EXISTANT]
│   ├── SocialGraphService.java                  [NOUVEAU]
│   └── ProfileAggregatorService.java            [NOUVEAU]
│
├── infrastructure/persistence/
│   ├── UserProfileEntity.java                   [EXISTANT]
│   ├── FollowEntity.java                        [NOUVEAU]
│   ├── BlockEntity.java                         [NOUVEAU]
│   ├── SpringDataFollowRepository.java          [NOUVEAU]
│   └── SpringDataBlockRepository.java           [NOUVEAU]
│
├── infrastructure/external/
│   ├── ContentServiceClient.java                [NOUVEAU]
│   ├── InteractionServiceClient.java            [NOUVEAU]
│   ├── EventServiceClient.java                  [NOUVEAU]
│   ├── BookingServiceClient.java                [NOUVEAU]
│   └── GamificationServiceClient.java           [NOUVEAU]
│
├── interfaces/rest/
│   ├── UserProfileController.java               [EXISTANT]
│   ├── SocialGraphController.java               [NOUVEAU]
│   └── ProfileAggregatorController.java         [NOUVEAU]
│
└── interfaces/rest/dto/
    ├── MyProfileResponse.java                   [EXISTANT]
    ├── PublicProfileResponse.java               [EXISTANT]
    ├── FollowResponse.java                      [NOUVEAU]
    ├── SocialActivityResponse.java              [NOUVEAU]
    ├── SocialSettingsResponse.java              [NOUVEAU]
    ├── AggregatedProfileResponse.java           [NOUVEAU]
    └── ServiceUnavailableInfo.java              [NOUVEAU]
```

---

**✅ CONSTAT VALIDÉ - PRÊT POUR IMPLÉMENTATION**
