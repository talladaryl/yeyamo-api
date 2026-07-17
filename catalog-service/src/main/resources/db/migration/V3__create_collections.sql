-- Collections : listes personnalisables d'assets (lieux, expériences, etc.)
CREATE TABLE collections (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(120) NOT NULL,
    description TEXT,
    is_public BOOLEAN NOT NULL DEFAULT false,
    cover_asset_id UUID REFERENCES catalog_assets(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

-- Index pour lister les collections d'un utilisateur (tri par updated_at DESC)
CREATE INDEX idx_collections_user ON collections(user_id, updated_at DESC);

-- Index pour lister les collections publiques
CREATE INDEX idx_collections_public ON collections(is_public, updated_at DESC) WHERE is_public = true;

-- Association collection <-> assets (lieux dans une collection)
CREATE TABLE collection_places (
    collection_id UUID NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    asset_id UUID NOT NULL REFERENCES catalog_assets(id) ON DELETE CASCADE,
    added_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (collection_id, asset_id)
);

-- Index pour récupérer les lieux d'une collection
CREATE INDEX idx_collection_places_collection ON collection_places(collection_id, added_at DESC);

-- Index pour trouver les collections contenant un lieu donné
CREATE INDEX idx_collection_places_asset ON collection_places(asset_id);
