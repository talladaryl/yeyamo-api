CREATE TABLE search_synonyms (
 id UUID PRIMARY KEY, terms_json TEXT NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 created_by VARCHAR(120) NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE search_ranking_versions (
 id UUID PRIMARY KEY, version INTEGER NOT NULL UNIQUE, config_json TEXT NOT NULL,
 active BOOLEAN NOT NULL, created_by VARCHAR(120) NOT NULL, created_at TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX uk_search_ranking_active ON search_ranking_versions(active) WHERE active;
CREATE TABLE search_reindex_jobs (
 id UUID PRIMARY KEY, status VARCHAR(30) NOT NULL, requested_by VARCHAR(120) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, started_at TIMESTAMPTZ, completed_at TIMESTAMPTZ, error_code VARCHAR(120)
);
CREATE UNIQUE INDEX uk_search_reindex_running ON search_reindex_jobs(status) WHERE status IN ('QUEUED','RUNNING');
CREATE TABLE search_zero_results (
 query_hash VARCHAR(64) PRIMARY KEY, normalized_query VARCHAR(300) NOT NULL, region_code VARCHAR(80),
 occurrences BIGINT NOT NULL DEFAULT 1, first_seen_at TIMESTAMPTZ NOT NULL, last_seen_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_search_zero_last_seen ON search_zero_results(last_seen_at DESC);
