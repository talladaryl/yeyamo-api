CREATE TABLE referral_codes (
 id UUID PRIMARY KEY, code VARCHAR(24) NOT NULL UNIQUE, owner_user_id VARCHAR(120) NOT NULL,
 status VARCHAR(20) NOT NULL, max_uses INTEGER NOT NULL CHECK(max_uses > 0), usage_count INTEGER NOT NULL DEFAULT 0 CHECK(usage_count >= 0),
 expires_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_referral_codes_owner ON referral_codes(owner_user_id,created_at DESC);

CREATE TABLE referral_invitations (
 id UUID PRIMARY KEY, code_id UUID NOT NULL REFERENCES referral_codes(id), inviter_user_id VARCHAR(120) NOT NULL,
 email_hash VARCHAR(64) NOT NULL, status VARCHAR(30) NOT NULL, invited_user_id VARCHAR(120),
 created_at TIMESTAMPTZ NOT NULL, accepted_at TIMESTAMPTZ, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_referral_invitation UNIQUE(inviter_user_id,email_hash)
);
CREATE INDEX idx_invitation_email_status ON referral_invitations(email_hash,status);

CREATE TABLE referral_attributions (
 id UUID PRIMARY KEY, code_id UUID NOT NULL REFERENCES referral_codes(id), invitation_id UUID REFERENCES referral_invitations(id),
 referrer_user_id VARCHAR(120) NOT NULL, referred_user_id VARCHAR(120) NOT NULL UNIQUE,
 source VARCHAR(30) NOT NULL, status VARCHAR(40) NOT NULL, device_hash VARCHAR(64),
 qualifying_event_id UUID, attributed_at TIMESTAMPTZ NOT NULL, qualified_at TIMESTAMPTZ, rewarded_at TIMESTAMPTZ,
 updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_attribution_referrer ON referral_attributions(referrer_user_id,attributed_at DESC);
CREATE INDEX idx_attribution_device ON referral_attributions(device_hash) WHERE device_hash IS NOT NULL;

CREATE TABLE referral_rewards (
 id UUID PRIMARY KEY, attribution_id UUID NOT NULL REFERENCES referral_attributions(id), beneficiary_user_id VARCHAR(120) NOT NULL,
 beneficiary_type VARCHAR(20) NOT NULL, reward_code VARCHAR(100) NOT NULL, title VARCHAR(200) NOT NULL,
 amount INTEGER NOT NULL CHECK(amount > 0), status VARCHAR(20) NOT NULL, created_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uk_referral_reward UNIQUE(attribution_id,beneficiary_type)
);
CREATE INDEX idx_referral_rewards_user ON referral_rewards(beneficiary_user_id,created_at DESC);

CREATE TABLE referral_history (
 id UUID PRIMARY KEY, attribution_id UUID, actor_user_id VARCHAR(120), action VARCHAR(80) NOT NULL,
 details TEXT NOT NULL, occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_referral_history_attribution ON referral_history(attribution_id,occurred_at);
CREATE OR REPLACE FUNCTION prevent_referral_history_mutation() RETURNS trigger AS $$ BEGIN RAISE EXCEPTION 'referral_history is append-only'; END; $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_referral_history_immutable BEFORE UPDATE OR DELETE ON referral_history FOR EACH ROW EXECUTE FUNCTION prevent_referral_history_mutation();

CREATE TABLE referral_processed_events(event_id UUID PRIMARY KEY,event_type VARCHAR(140) NOT NULL,processed_at TIMESTAMPTZ NOT NULL);
CREATE TABLE referral_outbox(id UUID PRIMARY KEY,aggregate_id VARCHAR(120) NOT NULL,event_type VARCHAR(140) NOT NULL,payload TEXT NOT NULL,occurred_at TIMESTAMPTZ NOT NULL,published_at TIMESTAMPTZ,attempts INTEGER NOT NULL DEFAULT 0,last_error VARCHAR(1000));
CREATE INDEX idx_referral_outbox_pending ON referral_outbox(occurred_at) WHERE published_at IS NULL;
