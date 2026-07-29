UPDATE ingestion_jobs SET status='PARTIAL' WHERE status='COMPLETED_WITH_ERRORS';
CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_status_created ON ingestion_jobs(status,created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_source_created ON ingestion_jobs(source_type,created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ingestion_records_job_status ON ingestion_records(job_id,status,created_at);
