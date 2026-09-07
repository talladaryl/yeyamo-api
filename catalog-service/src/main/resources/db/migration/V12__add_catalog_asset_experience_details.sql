ALTER TABLE catalog_assets
    ADD COLUMN duration_minutes INTEGER,
    ADD COLUMN difficulty_level VARCHAR(32),
    ADD COLUMN price NUMERIC(19,2),
    ADD COLUMN currency VARCHAR(3),
    ADD COLUMN capacity_min INTEGER,
    ADD COLUMN capacity_max INTEGER,
    ADD COLUMN place_id UUID;

CREATE TABLE catalog_asset_media (
    catalog_asset_id UUID NOT NULL REFERENCES catalog_assets(id) ON DELETE CASCADE,
    display_order INTEGER NOT NULL,
    media_id UUID NOT NULL,
    PRIMARY KEY (catalog_asset_id, display_order),
    CONSTRAINT uk_catalog_asset_media UNIQUE (catalog_asset_id, media_id)
);

CREATE TABLE catalog_asset_included_items (
    catalog_asset_id UUID NOT NULL REFERENCES catalog_assets(id) ON DELETE CASCADE,
    display_order INTEGER NOT NULL,
    item VARCHAR(500) NOT NULL,
    PRIMARY KEY (catalog_asset_id, display_order)
);

CREATE TABLE catalog_asset_excluded_items (
    catalog_asset_id UUID NOT NULL REFERENCES catalog_assets(id) ON DELETE CASCADE,
    display_order INTEGER NOT NULL,
    item VARCHAR(500) NOT NULL,
    PRIMARY KEY (catalog_asset_id, display_order)
);

ALTER TABLE catalog_assets
    ADD CONSTRAINT chk_catalog_asset_duration_positive CHECK (duration_minutes IS NULL OR duration_minutes >= 0),
    ADD CONSTRAINT chk_catalog_asset_price_positive CHECK (price IS NULL OR price > 0),
    ADD CONSTRAINT chk_catalog_asset_capacity_min_positive CHECK (capacity_min IS NULL OR capacity_min >= 0),
    ADD CONSTRAINT chk_catalog_asset_capacity_max_positive CHECK (capacity_max IS NULL OR capacity_max >= 0),
    ADD CONSTRAINT chk_catalog_asset_capacity_range CHECK (capacity_min IS NULL OR capacity_max IS NULL OR capacity_min <= capacity_max);
