-- Social Graph Schema: follows, blocks
-- Contraintes pour empêcher auto-follow et garantir l'intégrité

CREATE TABLE follows (
    follower_id UUID NOT NULL,
    followee_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_id, followee_id),
    CONSTRAINT fk_follows_follower FOREIGN KEY (follower_id) REFERENCES user_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_follows_followee FOREIGN KEY (followee_id) REFERENCES user_profiles(id) ON DELETE CASCADE,
    CONSTRAINT chk_no_self_follow CHECK (follower_id <> followee_id)
);

CREATE INDEX idx_follows_follower ON follows(follower_id, created_at DESC);
CREATE INDEX idx_follows_followee ON follows(followee_id, created_at DESC);

CREATE TABLE blocks (
    blocker_id UUID NOT NULL,
    blocked_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (blocker_id, blocked_id),
    CONSTRAINT fk_blocks_blocker FOREIGN KEY (blocker_id) REFERENCES user_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_blocks_blocked FOREIGN KEY (blocked_id) REFERENCES user_profiles(id) ON DELETE CASCADE,
    CONSTRAINT chk_no_self_block CHECK (blocker_id <> blocked_id)
);

CREATE INDEX idx_blocks_blocker ON blocks(blocker_id);
CREATE INDEX idx_blocks_blocked ON blocks(blocked_id);

COMMENT ON TABLE follows IS 'Relations de suivi entre utilisateurs (follower suit followee)';
COMMENT ON TABLE blocks IS 'Relations de blocage entre utilisateurs (blocker bloque blocked)';
COMMENT ON CONSTRAINT chk_no_self_follow ON follows IS 'Empêche un utilisateur de se suivre lui-même';
COMMENT ON CONSTRAINT chk_no_self_block ON blocks IS 'Empêche un utilisateur de se bloquer lui-même';
