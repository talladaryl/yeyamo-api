-- Add support for cultural content moderation, authenticity claims, and copyright claims

-- ========================================
-- Authenticity Claims
-- ========================================
CREATE TABLE authenticity_claims (
    id UUID PRIMARY KEY,
    target_type VARCHAR(40) NOT NULL,
    target_id VARCHAR(120) NOT NULL,
    claimant_id VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DECLARED',
    claim_description TEXT,
    assigned_reviewer_id VARCHAR(120),
    reviewer_notes TEXT,
    verified_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT chk_auth_status CHECK (status IN ('DECLARED', 'EVIDENCE_SUBMITTED', 'UNDER_REVIEW', 'VERIFIED', 'REJECTED', 'DISPUTED', 'REVOKED'))
);

CREATE INDEX idx_auth_claim_target ON authenticity_claims(target_type, target_id);
CREATE INDEX idx_auth_claim_claimant ON authenticity_claims(claimant_id);
CREATE INDEX idx_auth_claim_status ON authenticity_claims(status);
CREATE INDEX idx_auth_claim_reviewer ON authenticity_claims(assigned_reviewer_id);

COMMENT ON TABLE authenticity_claims IS 'Claims that artwork or cultural content is authentic';

-- ========================================
-- Authenticity Evidence
-- ========================================
CREATE TABLE authenticity_evidence (
    id UUID PRIMARY KEY,
    claim_id VARCHAR(120) NOT NULL,
    evidence_type VARCHAR(50) NOT NULL,
    media_url VARCHAR(500) NOT NULL,
    description VARCHAR(1000),
    uploaded_by VARCHAR(120) NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_auth_evidence_claim ON authenticity_evidence(claim_id);

COMMENT ON TABLE authenticity_evidence IS 'Evidence supporting authenticity claims (certificates, photos, documents)';

-- ========================================
-- Copyright Claims
-- ========================================
CREATE TABLE copyright_claims (
    id UUID PRIMARY KEY,
    target_type VARCHAR(40) NOT NULL,
    target_id VARCHAR(120) NOT NULL,
    claimant_id VARCHAR(120) NOT NULL,
    content_creator_id VARCHAR(120),
    status VARCHAR(30) NOT NULL DEFAULT 'FILED',
    claim_details TEXT NOT NULL,
    proof_url VARCHAR(500),
    creator_response TEXT,
    creator_response_at TIMESTAMPTZ,
    resolution TEXT,
    resolved_at TIMESTAMPTZ,
    resolved_by VARCHAR(120),
    content_removed BOOLEAN NOT NULL DEFAULT FALSE,
    content_restored BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT chk_copyright_status CHECK (status IN ('FILED', 'CONTENT_REMOVED', 'AWAITING_CREATOR_RESPONSE', 'UNDER_REVIEW', 'UPHELD', 'REJECTED', 'SETTLED'))
);

CREATE INDEX idx_copyright_target ON copyright_claims(target_type, target_id);
CREATE INDEX idx_copyright_claimant ON copyright_claims(claimant_id);
CREATE INDEX idx_copyright_status ON copyright_claims(status);
CREATE INDEX idx_copyright_creator ON copyright_claims(content_creator_id);

COMMENT ON TABLE copyright_claims IS 'Copyright infringement claims following DMCA-like workflow';

-- ========================================
-- Cultural Reviewers
-- ========================================
CREATE TABLE cultural_reviewers (
    id UUID PRIMARY KEY,
    user_id VARCHAR(120) NOT NULL UNIQUE,
    role VARCHAR(30) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    credentials VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    
    CONSTRAINT chk_reviewer_role CHECK (role IN ('MODERATOR', 'CULTURAL_EXPERT', 'INSTITUTION_REVIEWER', 'ADMIN')),
    CONSTRAINT chk_reviewer_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED'))
);

CREATE UNIQUE INDEX idx_reviewer_user ON cultural_reviewers(user_id);
CREATE INDEX idx_reviewer_role ON cultural_reviewers(role);
CREATE INDEX idx_reviewer_status ON cultural_reviewers(status);

-- Reviewer scope tables
CREATE TABLE reviewer_country_scope (
    reviewer_id UUID NOT NULL REFERENCES cultural_reviewers(id) ON DELETE CASCADE,
    country_code VARCHAR(2) NOT NULL,
    PRIMARY KEY (reviewer_id, country_code)
);

CREATE TABLE reviewer_culture_scope (
    reviewer_id UUID NOT NULL REFERENCES cultural_reviewers(id) ON DELETE CASCADE,
    culture_id VARCHAR(120) NOT NULL,
    PRIMARY KEY (reviewer_id, culture_id)
);

CREATE TABLE reviewer_language_scope (
    reviewer_id UUID NOT NULL REFERENCES cultural_reviewers(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    PRIMARY KEY (reviewer_id, language_code)
);

CREATE TABLE reviewer_content_scope (
    reviewer_id UUID NOT NULL REFERENCES cultural_reviewers(id) ON DELETE CASCADE,
    content_type VARCHAR(40) NOT NULL,
    PRIMARY KEY (reviewer_id, content_type)
);

COMMENT ON TABLE cultural_reviewers IS 'Cultural experts and institution reviewers with scoped permissions';
COMMENT ON TABLE reviewer_country_scope IS 'Countries a reviewer can moderate';
COMMENT ON TABLE reviewer_culture_scope IS 'Cultures a reviewer can moderate';
COMMENT ON TABLE reviewer_language_scope IS 'Languages a reviewer can moderate';
COMMENT ON TABLE reviewer_content_scope IS 'Content types a reviewer can moderate';

-- ========================================
-- Sensitive Content Flags
-- ========================================
CREATE TABLE sensitive_content_flags (
    id UUID PRIMARY KEY,
    target_type VARCHAR(40) NOT NULL,
    target_id VARCHAR(120) NOT NULL,
    flag_type VARCHAR(40) NOT NULL,
    reason TEXT,
    flagged_by VARCHAR(120) NOT NULL,
    approved_by VARCHAR(120),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL,
    approved_at TIMESTAMPTZ,
    
    CONSTRAINT chk_flag_type CHECK (flag_type IN ('SACRED_CONTENT', 'COMMUNITY_RESTRICTED', 'SENSITIVE', 'AGE_RESTRICTED')),
    CONSTRAINT chk_flag_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_sensitive_flag_target ON sensitive_content_flags(target_type, target_id);
CREATE INDEX idx_sensitive_flag_status ON sensitive_content_flags(status);

COMMENT ON TABLE sensitive_content_flags IS 'Flags for sacred or community-restricted cultural content';
