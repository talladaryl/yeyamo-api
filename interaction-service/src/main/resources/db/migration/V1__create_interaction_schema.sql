CREATE TABLE interaction_relations (
 id UUID PRIMARY KEY, post_id UUID NOT NULL, user_id VARCHAR(100) NOT NULL, type VARCHAR(20) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, CONSTRAINT uk_relation_post_user_type UNIQUE(post_id,user_id,type)
);
CREATE INDEX idx_relation_post_type ON interaction_relations(post_id,type);
CREATE INDEX idx_relation_user_favorites ON interaction_relations(user_id,created_at DESC) WHERE type='FAVORITE';

CREATE TABLE interaction_comments (
 id UUID PRIMARY KEY, post_id UUID NOT NULL, parent_id UUID, author_id VARCHAR(100) NOT NULL,
 body TEXT, status VARCHAR(20) NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 deleted_at TIMESTAMPTZ, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_comment_parent FOREIGN KEY(parent_id) REFERENCES interaction_comments(id)
);
CREATE INDEX idx_comments_post_active ON interaction_comments(post_id,created_at) WHERE status='ACTIVE';

CREATE TABLE interaction_shares (
 id UUID PRIMARY KEY, post_id UUID NOT NULL, user_id VARCHAR(100) NOT NULL,
 channel VARCHAR(40) NOT NULL, created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_shares_post ON interaction_shares(post_id);

CREATE TABLE interaction_checkins (
 id UUID PRIMARY KEY, catalog_asset_id UUID NOT NULL, user_id VARCHAR(100) NOT NULL,
 latitude DOUBLE PRECISION, longitude DOUBLE PRECISION, visible BOOLEAN NOT NULL,
 occurred_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT ck_checkin_lat CHECK(latitude IS NULL OR latitude BETWEEN -90 AND 90),
 CONSTRAINT ck_checkin_lng CHECK(longitude IS NULL OR longitude BETWEEN -180 AND 180)
);
CREATE INDEX idx_checkins_user ON interaction_checkins(user_id,occurred_at DESC);
CREATE INDEX idx_checkins_asset_visible ON interaction_checkins(catalog_asset_id,occurred_at DESC) WHERE visible=true;

CREATE TABLE interaction_command_receipts (
 id UUID PRIMARY KEY, idempotency_key VARCHAR(200) NOT NULL, actor_id VARCHAR(100) NOT NULL,
 operation VARCHAR(200) NOT NULL, result_id UUID NOT NULL, changed BOOLEAN NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uk_command_actor_key_operation UNIQUE(actor_id,idempotency_key,operation)
);

CREATE TABLE interaction_outbox (
 id UUID PRIMARY KEY, aggregate_id VARCHAR(100) NOT NULL, event_type VARCHAR(120) NOT NULL,
 payload TEXT NOT NULL, occurred_at TIMESTAMPTZ NOT NULL, published_at TIMESTAMPTZ,
 attempts INTEGER NOT NULL DEFAULT 0, last_error VARCHAR(1000)
);
CREATE INDEX idx_interaction_outbox_pending ON interaction_outbox(occurred_at) WHERE published_at IS NULL;
