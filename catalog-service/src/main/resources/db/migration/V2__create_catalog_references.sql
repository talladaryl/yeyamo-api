CREATE TABLE catalog_references (
    id UUID PRIMARY KEY,
    type VARCHAR(24) NOT NULL,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(200) NOT NULL,
    parent_code VARCHAR(80),
    country_code VARCHAR(2),
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_catalog_reference_type_code UNIQUE(type, code),
    CONSTRAINT ck_catalog_reference_country CHECK(country_code IS NULL OR country_code ~ '^[A-Z]{2}$')
);
CREATE INDEX idx_catalog_reference_lookup ON catalog_references(type, active, name);
CREATE INDEX idx_catalog_reference_parent ON catalog_references(type, parent_code, active);

INSERT INTO catalog_references(id,type,code,name,country_code,active,created_at,updated_at)
SELECT gen_random_uuid(),'REGION',region_code,region_code,NULL,TRUE,now(),now()
FROM catalog_assets WHERE region_code IS NOT NULL
ON CONFLICT(type,code) DO NOTHING;
