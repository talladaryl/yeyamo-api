ALTER TABLE collections ADD COLUMN IF NOT EXISTS scope VARCHAR(24) NOT NULL DEFAULT 'GLOBAL';
CREATE TABLE IF NOT EXISTS collection_target_countries (
  collection_id UUID NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
  country_code VARCHAR(2) NOT NULL,
  PRIMARY KEY (collection_id, country_code)
);
CREATE TABLE IF NOT EXISTS collection_target_languages (
  collection_id UUID NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
  language_code VARCHAR(10) NOT NULL,
  PRIMARY KEY (collection_id, language_code)
);
CREATE INDEX IF NOT EXISTS idx_collections_scope ON collections(scope);
