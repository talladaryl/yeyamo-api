-- ============================================================================
-- V5__add_city_id_uuid.sql
-- Migration: Unify City.id to UUID across place-service (Expand Phase)
-- Strategy: Expand/Contract safe migration - keeps legacy city_id intact
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Cities table: Add UUID identifier
ALTER TABLE cities ADD COLUMN IF NOT EXISTS city_id_uuid UUID DEFAULT gen_random_uuid();
UPDATE cities SET city_id_uuid = gen_random_uuid() WHERE city_id_uuid IS NULL;
ALTER TABLE cities ALTER COLUMN city_id_uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_cities_city_id_uuid ON cities(city_id_uuid);

-- 2. Districts table: Add city_id_uuid and backfill from cities
ALTER TABLE districts ADD COLUMN IF NOT EXISTS city_id_uuid UUID;
UPDATE districts d 
SET city_id_uuid = c.city_id_uuid 
FROM cities c 
WHERE d.city_id = c.id AND d.city_id_uuid IS NULL;

CREATE INDEX IF NOT EXISTS idx_districts_city_id_uuid ON districts(city_id_uuid);
ALTER TABLE districts ALTER COLUMN city_id DROP NOT NULL;

-- 3. Places table: Add city_id_uuid and backfill from cities
ALTER TABLE places ADD COLUMN IF NOT EXISTS city_id_uuid UUID;
UPDATE places p 
SET city_id_uuid = c.city_id_uuid 
FROM cities c 
WHERE p.city_id = c.id AND p.city_id_uuid IS NULL;

CREATE INDEX IF NOT EXISTS idx_places_city_id_uuid_status ON places(city_id_uuid, status);
ALTER TABLE places ALTER COLUMN city_id DROP NOT NULL;

-- 4. Foreign key constraints referencing cities(city_id_uuid)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_districts_city_id_uuid' AND table_name = 'districts'
    ) THEN
        ALTER TABLE districts 
        ADD CONSTRAINT fk_districts_city_id_uuid 
        FOREIGN KEY (city_id_uuid) REFERENCES cities(city_id_uuid);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_places_city_id_uuid' AND table_name = 'places'
    ) THEN
        ALTER TABLE places 
        ADD CONSTRAINT fk_places_city_id_uuid 
        FOREIGN KEY (city_id_uuid) REFERENCES cities(city_id_uuid);
    END IF;
END $$;
