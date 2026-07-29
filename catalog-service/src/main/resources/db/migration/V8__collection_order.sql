ALTER TABLE collection_places ADD COLUMN IF NOT EXISTS display_order INTEGER NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_collection_places_order ON collection_places(collection_id,display_order,added_at);
