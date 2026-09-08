CREATE TABLE analytics_rebuild_jobs(id UUID PRIMARY KEY,status VARCHAR(20) NOT NULL,requested_by VARCHAR(120) NOT NULL,created_at TIMESTAMPTZ NOT NULL,started_at TIMESTAMPTZ,completed_at TIMESTAMPTZ,events_replayed BIGINT,failure_reason VARCHAR(255));
CREATE INDEX idx_analytics_rebuild_jobs_created ON analytics_rebuild_jobs(created_at DESC);
