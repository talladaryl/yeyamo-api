# Generic Geography Model - Multi-Country Support

## 📋 Vue d'ensemble

Le **modèle géographique générique** remplace l'approche centrée sur le Cameroun (région/département/arrondissement/quartier) par une architecture flexible supportant **toutes les structures administratives africaines**.

### Problème Résolu

❌ **Ancien modèle** : Spécifique au Cameroun
- `regions` → Régions (Littoral, Centre...)
- `departments` → Départements (Wouri, Mfoundi...)
- `districts` → Arrondissements
- `quarters` → Quartiers

✅ **Nouveau modèle** : Générique et extensible
- `administrative_areas` → Hiérarchie basée sur des niveaux (1, 2, 3...)
- `administrative_level_labels` → Labels spécifiques par pays
- `cities` → Villes majeures
- `localities` → Unités locales (quartiers, villages, wards...)

---

## 🏗️ Architecture

### Tables Principales

#### 1. `administrative_areas` (Zones Administratives)

Structure hiérarchique générique basée sur des **niveaux** plutôt que des noms locaux.

```sql
CREATE TABLE administrative_areas (
    id UUID PRIMARY KEY,
    country_code VARCHAR(2) NOT NULL,
    parent_id UUID,  -- NULL pour le niveau supérieur
    level INTEGER NOT NULL,  -- 1, 2, 3...
    type_code VARCHAR(50),
    name VARCHAR(200) NOT NULL,
    localized_names JSONB,
    official_code VARCHAR(20),
    slug VARCHAR(250) NOT NULL,
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    active BOOLEAN NOT NULL DEFAULT true
);
```

**Exemples de hiérarchie:**

| Pays | Niveau 1 | Niveau 2 | Niveau 3 |
|------|----------|----------|----------|
| **Cameroun** | Région | Département | Arrondissement |
| **Nigeria** | State | Local Government Area | - |
| **Kenya** | County | Sub-County | Ward |
| **South Africa** | Province | District Municipality | Local Municipality |

#### 2. `administrative_level_labels` (Labels par Pays)

Configuration des terminologies spécifiques à chaque pays.

```sql
CREATE TABLE administrative_level_labels (
    id UUID PRIMARY KEY,
    country_code VARCHAR(2) NOT NULL,
    level INTEGER NOT NULL,
    label VARCHAR(100) NOT NULL,
    label_plural VARCHAR(100),
    localized_labels JSONB,
    display_order INTEGER NOT NULL,
    UNIQUE (country_code, level)
);
```

**Exemples:**

```sql
-- Cameroun
INSERT INTO administrative_level_labels VALUES
    ('CM', 1, 'Région', 'Régions', '{"fr": "Région", "en": "Region"}', 1),
    ('CM', 2, 'Département', 'Départements', '{"fr": "Département", "en": "Department"}', 2),
    ('CM', 3, 'Arrondissement', 'Arrondissements', '{"fr": "Arrondissement", "en": "District"}', 3);

-- Nigeria
INSERT INTO administrative_level_labels VALUES
    ('NG', 1, 'State', 'States', '{"en": "State"}', 1),
    ('NG', 2, 'Local Government Area', 'Local Government Areas', '{"en": "LGA"}', 2);

-- Kenya
INSERT INTO administrative_level_labels VALUES
    ('KE', 1, 'County', 'Counties', '{"en": "County", "sw": "Kaunti"}', 1),
    ('KE', 2, 'Sub-County', 'Sub-Counties', '{"en": "Sub-County"}', 2),
    ('KE', 3, 'Ward', 'Wards', '{"en": "Ward"}', 3);
```

#### 3. `cities` (Villes Majeures)

```sql
CREATE TABLE cities (
    id UUID PRIMARY KEY,
    country_code VARCHAR(2) NOT NULL,
    administrative_area_id UUID NOT NULL,  -- Parent admin area
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(250) NOT NULL,
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    population BIGINT,
    active BOOLEAN NOT NULL DEFAULT true
);
```

#### 4. `localities` (Unités Locales)

Niveau le plus fin : quartiers, villages, wards, zones, suburbs...

```sql
CREATE TABLE localities (
    id UUID PRIMARY KEY,
    country_code VARCHAR(2) NOT NULL,
    city_id UUID,  -- Si urbain
    administrative_area_id UUID,  -- Si rural
    locality_type VARCHAR(50) NOT NULL,  -- quartier, village, ward, suburb
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(250) NOT NULL,
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    active BOOLEAN NOT NULL DEFAULT true
);
```

---

## 🌍 Exemples Concrets

### Cameroun

```
Pays: Cameroon (CM)
└─ Niveau 1: Région "Littoral"
   └─ Niveau 2: Département "Wouri"
      └─ Ville: Douala
         ├─ Locality (quartier): Akwa
         ├─ Locality (quartier): Bonanjo
         └─ Locality (quartier): Bonabéri
```

```json
// GET /api/v1/countries/CM/administrative-labels
[
  {"level": 1, "label": "Région", "labelPlural": "Régions"},
  {"level": 2, "label": "Département", "labelPlural": "Départements"},
  {"level": 3, "label": "Arrondissement", "labelPlural": "Arrondissements"}
]

// GET /api/v1/countries/CM/administrative-areas/top-level
[
  {
    "id": "uuid",
    "countryCode": "CM",
    "level": 1,
    "name": "Littoral",
    "slug": "littoral",
    "parentId": null
  }
]

// GET /api/v1/administrative-areas/{littoral-id}/children
[
  {
    "id": "uuid",
    "countryCode": "CM",
    "level": 2,
    "name": "Wouri",
    "slug": "wouri",
    "parentId": "littoral-uuid"
  }
]
```

### Nigeria

```
Pays: Nigeria (NG)
└─ Niveau 1: State "Lagos"
   └─ Niveau 2: LGA "Ikeja"
      └─ Ville: Lagos City
         ├─ Locality (area): Victoria Island
         ├─ Locality (area): Ikoyi
         └─ Locality (area): Lekki
```

```json
// GET /api/v1/countries/NG/administrative-labels
[
  {"level": 1, "label": "State", "labelPlural": "States"},
  {"level": 2, "label": "Local Government Area", "labelPlural": "Local Government Areas"}
]

// GET /api/v1/countries/NG/administrative-areas/top-level
[
  {
    "id": "uuid",
    "countryCode": "NG",
    "level": 1,
    "name": "Lagos",
    "slug": "lagos",
    "parentId": null
  }
]
```

---

## 🔌 API Endpoints

### Administrative Areas

| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/countries/{code}/administrative-areas` | Toutes les zones admin d'un pays |
| `GET /api/v1/countries/{code}/administrative-areas/top-level` | Zones de niveau 1 uniquement |
| `GET /api/v1/administrative-areas/{id}` | Détails d'une zone |
| `GET /api/v1/administrative-areas/{id}/children` | Sous-divisions |
| `GET /api/v1/countries/{code}/administrative-labels` | Labels spécifiques au pays |

### Cities

| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/countries/{code}/cities` | Toutes les villes d'un pays |
| `GET /api/v1/cities/{id}` | Détails d'une ville |

### Localities

| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/cities/{id}/localities` | Quartiers/zones d'une ville |
| `GET /api/v1/localities/{id}` | Détails d'une localité |

---

## 📊 Migrations

### V3: Schéma Générique

Création des tables `administrative_areas`, `administrative_level_labels`, `cities`, `localities`.

### V4: Seed Cameroun

Structure administrative complète du Cameroun :
- 10 régions (Littoral, Centre, Ouest...)
- Départements (Wouri, Mfoundi, Mifi...)
- Villes (Douala, Yaoundé, Bafoussam...)
- Quartiers (Akwa, Bonanjo, Bastos...)

### V5: Seed Nigeria

Structure administrative du Nigeria :
- States (Lagos, Abuja FCT, Kano, Rivers...)
- LGAs (Lagos Island, Ikeja, Abuja Municipal...)
- Villes (Lagos, Abuja, Port Harcourt, Kano...)
- Areas/Districts (Victoria Island, Ikoyi, Maitama...)

---

## 💡 Avantages

### ✅ Flexibilité

- Supporte **n'importe quelle structure administrative**
- Pas de dépendance aux concepts camerounais
- Extensible à tous les pays africains

### ✅ Clarté

- API indépendante de la terminologie locale
- Les clients interrogent par **niveau** (1, 2, 3...)
- Les labels sont fournis séparément pour l'affichage UI

### ✅ Maintenance

- Ajout d'un pays = configuration des labels + seed des données
- Pas de modification du code
- Pas de duplication de logique

### ✅ I18n

- Support natif du multi-langues via `localized_names` et `localized_labels`
- Chaque zone peut avoir des noms dans plusieurs langues

---

## 🔄 Migration depuis l'Ancien Modèle

Si des tables `regions`, `departments`, `districts`, `quarters` existaient :

### Étape 1: Créer le Nouveau Modèle (V3)
Tables génériques créées sans toucher aux anciennes.

### Étape 2: Migrer les Données
```sql
-- Migrer regions → administrative_areas (level 1)
INSERT INTO administrative_areas (country_code, parent_id, level, name, slug, ...)
SELECT 'CM', NULL, 1, name, slug, ... FROM regions;

-- Migrer departments → administrative_areas (level 2)
INSERT INTO administrative_areas (country_code, parent_id, level, name, slug, ...)
SELECT 'CM', region_new_id, 2, name, slug, ... FROM departments;
```

### Étape 3: Valider
- Tests d'intégration passent
- API fonctionne avec les nouvelles tables
- Clients mis à jour

### Étape 4: Supprimer les Anciennes Tables
```sql
DROP TABLE quarters CASCADE;
DROP TABLE districts CASCADE;
DROP TABLE departments CASCADE;
DROP TABLE regions CASCADE;
```

---

## 🧪 Tests

### Couverture

✅ Hiérarchie (parent-child)  
✅ Cohérence pays (country consistency)  
✅ Labels par pays  
✅ Villes et localités  
✅ Pagination  
✅ Cas d'erreur (404, invalid country)  
✅ Cameroun complet  
✅ Nigeria complet  

### Exemple de Test

```java
@Test
void shouldReturnTopLevelAreasOnly() throws Exception {
    mockMvc.perform(get("/api/v1/countries/CM/administrative-areas/top-level"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(10)))  // 10 régions
            .andExpect(jsonPath("$[*].level", everyItem(is(1))))
            .andExpect(jsonPath("$[*].parentId", everyItem(nullValue())));
}
```

---

## 📚 Utilisation dans les Services

### Exemple: Filtrage Géographique

```java
// Dans campaign-service ou discovery-service
@Service
public class GeographicFilterService {
    
    private final RestTemplate restTemplate;
    
    public List<AdministrativeAreaDto> getRegionsForCountry(String countryCode) {
        return restTemplate.exchange(
            "http://country-config-service/api/v1/countries/{code}/administrative-areas/top-level",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<AdministrativeAreaDto>>() {},
            countryCode
        ).getBody();
    }
    
    public List<CityDto> getCitiesInArea(UUID adminAreaId) {
        // Interroger via admin area ID
        return ...;
    }
}
```

### Exemple: Sélecteur UI Dynamique

```typescript
// Frontend: Construction de sélecteurs géographiques
async function loadGeographicHierarchy(countryCode: string) {
  // 1. Récupérer les labels
  const labels = await fetch(`/api/v1/countries/${countryCode}/administrative-labels`);
  // labels = [{level: 1, label: "Région"}, {level: 2, label: "Département"}]
  
  // 2. Charger le niveau 1
  const level1 = await fetch(`/api/v1/countries/${countryCode}/administrative-areas/top-level`);
  
  // 3. Quand l'utilisateur sélectionne une région, charger les départements
  const level2 = await fetch(`/api/v1/administrative-areas/${selectedRegionId}/children`);
  
  // 4. UI s'adapte automatiquement:
  // - Cameroun: "Région" → "Département"
  // - Nigeria: "State" → "LGA"
  // - Kenya: "County" → "Sub-County"
}
```

---

## 🚀 Roadmap

### Phase 1: Implémentation de Base ✅
- [x] Schéma générique
- [x] Seed Cameroun
- [x] Seed Nigeria
- [x] API REST
- [x] Tests

### Phase 2: Extension (Q3 2026)
- [ ] Seed Kenya, South Africa, Ghana, Sénégal
- [ ] Import en masse (CSV, JSON)
- [ ] API Admin pour gestion dynamique

### Phase 3: Enrichissement (Q4 2026)
- [ ] Données géospatiales (polygones)
- [ ] Population, superficie
- [ ] Codes postaux
- [ ] Données économiques

---

## 📖 Documentation API Complète

Swagger UI disponible à: `http://localhost:8090/swagger-ui.html`

---

**Version**: 1.0.0  
**Date**: 13 août 2026  
**Auteur**: YeYamo Backend Team
