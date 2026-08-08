ALTER TABLE catalog_assets ALTER COLUMN latitude DROP NOT NULL;
ALTER TABLE catalog_assets ALTER COLUMN longitude DROP NOT NULL;
ALTER TABLE catalog_assets ALTER COLUMN location DROP NOT NULL;

CREATE TABLE artworks (
    asset_id UUID PRIMARY KEY REFERENCES catalog_assets(id), artisan_partner_id UUID NOT NULL,
    title VARCHAR(240) NOT NULL, slug VARCHAR(260) NOT NULL UNIQUE, short_description TEXT,
    story TEXT, country_code VARCHAR(2) NOT NULL, admin_level_1_id VARCHAR(100), city_id VARCHAR(100),
    locality_id VARCHAR(100), culture_content_id UUID, cultural_community VARCHAR(180),
    year_created INTEGER, production_time VARCHAR(100), width NUMERIC(12,3), height NUMERIC(12,3),
    depth NUMERIC(12,3), weight NUMERIC(12,3), edition_type VARCHAR(32) NOT NULL,
    edition_size INTEGER, availability_status VARCHAR(32) NOT NULL,
    authenticity_status VARCHAR(32) NOT NULL, deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    CHECK (country_code = upper(country_code)), CHECK (edition_size IS NULL OR edition_size > 0)
);
CREATE INDEX idx_artworks_country_availability ON artworks(country_code, availability_status) WHERE deleted_at IS NULL;
CREATE INDEX idx_artworks_artisan ON artworks(artisan_partner_id, created_at DESC) WHERE deleted_at IS NULL;

CREATE TABLE materials (id UUID PRIMARY KEY, code VARCHAR(100) NOT NULL UNIQUE, active BOOLEAN NOT NULL DEFAULT TRUE);
CREATE TABLE material_translations (material_id UUID REFERENCES materials(id), language_code VARCHAR(35), name VARCHAR(160) NOT NULL, description TEXT, PRIMARY KEY(material_id, language_code));
CREATE TABLE techniques (id UUID PRIMARY KEY, code VARCHAR(100) NOT NULL UNIQUE, active BOOLEAN NOT NULL DEFAULT TRUE);
CREATE TABLE technique_translations (technique_id UUID REFERENCES techniques(id), language_code VARCHAR(35), name VARCHAR(160) NOT NULL, description TEXT, PRIMARY KEY(technique_id, language_code));
CREATE TABLE artwork_materials (artwork_id UUID REFERENCES artworks(asset_id), material_id UUID REFERENCES materials(id), PRIMARY KEY(artwork_id, material_id));
CREATE TABLE artwork_techniques (artwork_id UUID REFERENCES artworks(asset_id), technique_id UUID REFERENCES techniques(id), PRIMARY KEY(artwork_id, technique_id));
CREATE TABLE artwork_translations (id UUID PRIMARY KEY, artwork_id UUID NOT NULL REFERENCES artworks(asset_id), language_code VARCHAR(35) NOT NULL, title VARCHAR(240) NOT NULL, short_description TEXT, story TEXT, status VARCHAR(32) NOT NULL, translator_id VARCHAR(100), UNIQUE(artwork_id, language_code));
CREATE TABLE artwork_history_entries (id UUID PRIMARY KEY, artwork_id UUID NOT NULL REFERENCES artworks(asset_id), title VARCHAR(240) NOT NULL, narrative TEXT NOT NULL, language_code VARCHAR(35) NOT NULL, period VARCHAR(120), cultural_meaning TEXT, source TEXT, contributor_id VARCHAR(100), verification_status VARCHAR(32) NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0);
CREATE TABLE artwork_provenance (id UUID PRIMARY KEY, artwork_id UUID NOT NULL REFERENCES artworks(asset_id), entry_type VARCHAR(40) NOT NULL, creator VARCHAR(200), creation_location VARCHAR(240), narrative TEXT, occurred_at DATE, source_reference TEXT, verification_status VARCHAR(32) NOT NULL, created_at TIMESTAMPTZ NOT NULL);
CREATE TABLE artwork_media (artwork_id UUID REFERENCES artworks(asset_id), media_id UUID NOT NULL, media_type VARCHAR(32) NOT NULL, display_order INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(artwork_id, media_id));
