CREATE TABLE admin_users (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL,
    permissions JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(20) NOT NULL,
    last_login_at TIMESTAMP,
    created_by UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE reports (
    id UUID PRIMARY KEY,
    report_type VARCHAR(50) NOT NULL,
    target_id UUID NOT NULL,
    reporter_id UUID NOT NULL,
    reason VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    assigned_to UUID,
    resolved_by UUID,
    resolution_comment TEXT,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE moderation_actions (
    id UUID PRIMARY KEY,
    admin_id UUID NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id UUID NOT NULL,
    previous_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    reason TEXT,
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE partner_validations (
    id UUID PRIMARY KEY,
    partner_id UUID NOT NULL,
    requester_id UUID,
    status VARCHAR(50) NOT NULL,
    kyc_document_urls JSONB NOT NULL DEFAULT '[]'::jsonb,
    kyc_document_types JSONB NOT NULL DEFAULT '[]'::jsonb,
    review_comment TEXT,
    risk_score INTEGER,
    validated_by UUID,
    validated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE place_validations (
    id UUID PRIMARY KEY,
    place_id UUID NOT NULL,
    submitted_by UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    reviewed_by UUID,
    review_comment TEXT,
    changes_requested JSONB NOT NULL DEFAULT '{}'::jsonb,
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE admin_audit_logs (
    id UUID PRIMARY KEY,
    admin_id UUID NOT NULL,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id UUID,
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    ip_address VARCHAR(45),
    user_agent TEXT,
    correlation_id UUID,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_admin_users_role_status ON admin_users(role, status);
CREATE INDEX idx_reports_status_created_at ON reports(status, created_at DESC);
CREATE INDEX idx_reports_target ON reports(report_type, target_id);
CREATE INDEX idx_moderation_actions_target ON moderation_actions(target_type, target_id);
CREATE INDEX idx_partner_validations_status ON partner_validations(status);
CREATE INDEX idx_place_validations_status ON place_validations(status);
CREATE INDEX idx_admin_audit_logs_created_at ON admin_audit_logs(created_at DESC);
