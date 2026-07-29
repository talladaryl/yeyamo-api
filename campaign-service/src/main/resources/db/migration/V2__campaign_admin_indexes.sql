CREATE INDEX IF NOT EXISTS idx_campaigns_created_at ON campaigns(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_campaigns_objective ON campaigns(objective,status);
CREATE INDEX IF NOT EXISTS idx_campaigns_admin_schedule ON campaigns(start_at,end_at);
