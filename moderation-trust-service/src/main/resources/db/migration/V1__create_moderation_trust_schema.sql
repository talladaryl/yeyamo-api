CREATE TABLE moderation_reports (
 id UUID PRIMARY KEY,target_type VARCHAR(30) NOT NULL,target_id VARCHAR(120) NOT NULL,target_owner_id VARCHAR(120),reporter_id VARCHAR(120) NOT NULL,
 reason VARCHAR(40) NOT NULL,details TEXT,status VARCHAR(20) NOT NULL,assigned_to VARCHAR(120),resolution TEXT,
 created_at TIMESTAMPTZ NOT NULL,updated_at TIMESTAMPTZ NOT NULL,decided_at TIMESTAMPTZ,version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT ck_moderation_status CHECK(status IN('OPEN','REVIEW','APPROVED','REJECTED'))
);
CREATE UNIQUE INDEX uk_active_report_target_reporter ON moderation_reports(target_type,target_id,reporter_id) WHERE status IN('OPEN','REVIEW');
CREATE INDEX idx_moderation_queue ON moderation_reports(status,created_at);
CREATE INDEX idx_moderation_reporter ON moderation_reports(reporter_id,created_at DESC);

CREATE TABLE trust_scores(subject_id VARCHAR(120) PRIMARY KEY,score INTEGER NOT NULL DEFAULT 50,approved_reports INTEGER NOT NULL DEFAULT 0,rejected_reports INTEGER NOT NULL DEFAULT 0,updated_at TIMESTAMPTZ NOT NULL,version BIGINT NOT NULL DEFAULT 0,CONSTRAINT ck_trust_score CHECK(score BETWEEN 0 AND 100));

CREATE TABLE moderation_audit(id UUID PRIMARY KEY,action VARCHAR(100) NOT NULL,aggregate_type VARCHAR(60) NOT NULL,aggregate_id VARCHAR(120) NOT NULL,actor_id VARCHAR(120),correlation_id VARCHAR(120),details TEXT NOT NULL,occurred_at TIMESTAMPTZ NOT NULL);
CREATE INDEX idx_moderation_audit_time ON moderation_audit(occurred_at DESC);
CREATE OR REPLACE FUNCTION reject_moderation_audit_mutation() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'moderation_audit is append-only'; END $$;
CREATE TRIGGER trg_moderation_audit_no_update BEFORE UPDATE OR DELETE ON moderation_audit FOR EACH ROW EXECUTE FUNCTION reject_moderation_audit_mutation();

CREATE TABLE moderation_outbox(id UUID PRIMARY KEY,aggregate_id VARCHAR(120) NOT NULL,event_type VARCHAR(120) NOT NULL,payload TEXT NOT NULL,occurred_at TIMESTAMPTZ NOT NULL,published_at TIMESTAMPTZ,attempts INTEGER NOT NULL DEFAULT 0,last_error VARCHAR(1000));
CREATE INDEX idx_moderation_outbox_pending ON moderation_outbox(occurred_at) WHERE published_at IS NULL;
CREATE TABLE moderation_processed_events(event_id UUID PRIMARY KEY,event_type VARCHAR(120) NOT NULL,processed_at TIMESTAMPTZ NOT NULL);
