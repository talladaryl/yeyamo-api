CREATE INDEX IF NOT EXISTS idx_catalog_assets_status_created ON catalog_assets(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_catalog_assets_type_status ON catalog_assets(type, status);
CREATE INDEX IF NOT EXISTS idx_catalog_assets_source ON catalog_assets(source);
CREATE INDEX IF NOT EXISTS idx_catalog_assets_region_category ON catalog_assets(region_code, category_code);
