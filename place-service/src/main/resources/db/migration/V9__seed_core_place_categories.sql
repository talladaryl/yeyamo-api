-- Canonical categories already used by the Yeyamo partner place flow.
-- They are reference data, not a mobile fallback: the public categories API
-- remains the sole source for every non-demo selector.
INSERT INTO place_categories (name, slug, icon, active) VALUES
    ('Hôtel', 'hotel', 'bed', TRUE),
    ('Restaurant', 'restaurant', 'restaurant', TRUE),
    ('Culture', 'culture', 'color-palette', TRUE),
    ('Loisir', 'loisir', 'sparkles', TRUE),
    ('Commerce', 'commerce', 'storefront', TRUE),
    ('Nature', 'nature', 'leaf', TRUE)
ON CONFLICT (slug) DO NOTHING;
