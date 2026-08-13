-- Generic Administrative Model for Multi-Country Support
-- Replaces Cameroon-centric model (regions, departments, districts)
-- Supports diverse African administrative structures

-- Administrative areas (generic hierarchy)
CREATE TABLE administrative_areas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code VARCHAR(2) NOT NULL,
    parent_id UUID REFERENCES administrative_areas(id) ON DELETE CASCADE,
    level INTEGER NOT NULL CHECK (level >= 1 AND level <= 10),
    type_code VARCHAR(50),
    name VARCHAR(200) NOT NULL,
    localized_names JSONB,
    official_code VARCHAR(20),
    slug VARCHAR(250) NOT NULL,
    latitude DECIMAL(10, 7) CHECK (latitude >= -90 AND latitude <= 90),
    longitude DECIMAL(10, 7) CHECK (longitude >= -180 AND longitude <= 180),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_country_code_fmt CHECK (country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT uq_admin_area_country_slug UNIQUE (country_code, slug)
);

CREATE INDEX idx_admin_area_country ON administrative_areas(country_code);
CREATE INDEX idx_admin_area_parent ON administrative_areas(parent_id);
CREATE INDEX idx_admin_area_level ON administrative_areas(level);
CREATE INDEX idx_admin_area_slug ON administrative_areas(slug);
CREATE INDEX idx_admin_area_country_level ON administrative_areas(country_code, level);
CREATE INDEX idx_admin_area_country_parent ON administrative_areas(country_code, parent_id);

COMMENT ON TABLE administrative_areas IS 'Generic administrative hierarchy supporting diverse African structures';
COMMENT ON COLUMN administrative_areas.level IS 'Hierarchical level: 1=top (Region/State/Province), 2, 3...';
COMMENT ON COLUMN administrative_areas.type_code IS 'Optional type: urban_commune, rural_district, autonomous_city';
COMMENT ON COLUMN administrative_areas.localized_names IS 'Names in multiple languages: {"fr": "Littoral", "en": "Littoral"}';

-- Administrative level labels per country
CREATE TABLE administrative_level_labels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code VARCHAR(2) NOT NULL,
    level INTEGER NOT NULL CHECK (level >= 1 AND level <= 10),
    label VARCHAR(100) NOT NULL,
    label_plural VARCHAR(100),
    localized_labels JSONB,
    display_order INTEGER NOT NULL,
    
    CONSTRAINT chk_label_country_code CHECK (country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT uq_admin_label_country_level UNIQUE (country_code, level)
);

CREATE INDEX idx_admin_label_country ON administrative_level_labels(country_code);

COMMENT ON TABLE administrative_level_labels IS 'Country-specific labels for administrative levels';
COMMENT ON COLUMN administrative_level_labels.label IS 'Label in default language: Région, State, Province';
COMMENT ON COLUMN administrative_level_labels.localized_labels IS 'Labels in multiple languages';

-- Cities (major urban centers)
CREATE TABLE cities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code VARCHAR(2) NOT NULL,
    administrative_area_id UUID NOT NULL REFERENCES administrative_areas(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(250) NOT NULL,
    latitude DECIMAL(10, 7) CHECK (latitude >= -90 AND latitude <= 90),
    longitude DECIMAL(10, 7) CHECK (longitude >= -180 AND longitude <= 180),
    active BOOLEAN NOT NULL DEFAULT true,
    population BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_city_country_code CHECK (country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT uq_city_country_slug UNIQUE (country_code, slug)
);

CREATE INDEX idx_city_country ON cities(country_code);
CREATE INDEX idx_city_admin_area ON cities(administrative_area_id);
CREATE INDEX idx_city_slug ON cities(slug);
CREATE INDEX idx_city_active ON cities(active);

COMMENT ON TABLE cities IS 'Major urban centers within administrative areas';

-- Localities (neighborhoods, villages, zones)
CREATE TABLE localities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code VARCHAR(2) NOT NULL,
    city_id UUID REFERENCES cities(id) ON DELETE CASCADE,
    administrative_area_id UUID REFERENCES administrative_areas(id) ON DELETE CASCADE,
    locality_type VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(250) NOT NULL,
    latitude DECIMAL(10, 7) CHECK (latitude >= -90 AND latitude <= 90),
    longitude DECIMAL(10, 7) CHECK (longitude >= -180 AND longitude <= 180),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_locality_country_code CHECK (country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT chk_locality_parent CHECK (city_id IS NOT NULL OR administrative_area_id IS NOT NULL),
    CONSTRAINT uq_locality_country_slug UNIQUE (country_code, slug)
);

CREATE INDEX idx_locality_country ON localities(country_code);
CREATE INDEX idx_locality_city ON localities(city_id);
CREATE INDEX idx_locality_admin_area ON localities(administrative_area_id);
CREATE INDEX idx_locality_slug ON localities(slug);
CREATE INDEX idx_locality_active ON localities(active);

COMMENT ON TABLE localities IS 'Smallest administrative units: neighborhoods, villages, wards, zones';
COMMENT ON COLUMN localities.locality_type IS 'Type: quartier, village, ward, suburb, zone';
COMMENT ON COLUMN localities.city_id IS 'Parent city (for urban localities)';
COMMENT ON COLUMN localities.administrative_area_id IS 'Parent admin area (for rural localities)';
