-- Stories : contenus éphémères (24h par défaut)
CREATE TABLE stories (
    id UUID PRIMARY KEY,
    author_id VARCHAR(100) NOT NULL,
    media_id UUID NOT NULL,
    caption TEXT,
    duration_seconds INTEGER NOT NULL DEFAULT 15,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ
);

-- Index pour récupérer les stories actives d'un auteur
CREATE INDEX idx_stories_author_active ON stories(author_id, created_at DESC) 
    WHERE expires_at > NOW() AND deleted_at IS NULL;

-- Index pour le job de nettoyage (stories expirées)
CREATE INDEX idx_stories_expired ON stories(expires_at) 
    WHERE deleted_at IS NULL AND expires_at <= NOW();

-- Vues des stories : qui a vu quelle story
CREATE TABLE story_views (
    story_id UUID NOT NULL REFERENCES stories(id) ON DELETE CASCADE,
    viewer_id VARCHAR(100) NOT NULL,
    viewed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (story_id, viewer_id)
);

-- Index pour compter les vues d'une story
CREATE INDEX idx_story_views_story ON story_views(story_id, viewed_at DESC);

-- Index pour voir l'historique de vues d'un utilisateur
CREATE INDEX idx_story_views_viewer ON story_views(viewer_id, viewed_at DESC);
