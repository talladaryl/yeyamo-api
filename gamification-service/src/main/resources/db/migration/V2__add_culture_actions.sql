-- Add culture and artisan related gamification features

-- ========================================
-- XP Action Configuration
-- ========================================
CREATE TABLE xp_actions (
    id UUID PRIMARY KEY,
    action_code VARCHAR(80) NOT NULL UNIQUE,
    display_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    points INTEGER NOT NULL CHECK (points >= 0),
    daily_cap INTEGER,
    weekly_cap INTEGER,
    requires_verification BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    
    CONSTRAINT chk_xp_caps CHECK (daily_cap IS NULL OR daily_cap > 0)
);

CREATE INDEX idx_xp_actions_enabled ON xp_actions(enabled) WHERE enabled = TRUE;

COMMENT ON TABLE xp_actions IS 'Configurable XP actions with caps and rules';

-- ========================================
-- Badge Definitions
-- ========================================
CREATE TABLE badge_definitions (
    id UUID PRIMARY KEY,
    badge_code VARCHAR(80) NOT NULL UNIQUE,
    name_key VARCHAR(100) NOT NULL,
    description_key VARCHAR(150) NOT NULL,
    icon_url VARCHAR(500),
    tier VARCHAR(30),
    category VARCHAR(50) NOT NULL,
    requirement_type VARCHAR(50) NOT NULL,
    requirement_value INTEGER,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    
    CONSTRAINT chk_badge_tier CHECK (tier IN ('BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND')),
    CONSTRAINT chk_badge_category CHECK (category IN ('SOCIAL', 'TRAVEL', 'CULTURE', 'COMMERCE', 'CONTRIBUTION', 'ARTISAN'))
);

CREATE INDEX idx_badge_category ON badge_definitions(category);
CREATE INDEX idx_badge_enabled ON badge_definitions(enabled) WHERE enabled = TRUE;

COMMENT ON TABLE badge_definitions IS 'Badge definitions with i18n keys for names';

-- ========================================
-- User Progress Tracking
-- ========================================
CREATE TABLE user_progress (
    id UUID PRIMARY KEY,
    user_id VARCHAR(120) NOT NULL,
    progress_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(120),
    current_value INTEGER NOT NULL DEFAULT 0,
    target_value INTEGER,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT uk_user_progress UNIQUE (user_id, progress_type, target_id)
);

CREATE INDEX idx_user_progress_user ON user_progress(user_id, completed);
CREATE INDEX idx_user_progress_type ON user_progress(progress_type);

COMMENT ON TABLE user_progress IS 'Track progress towards badges and achievements';

-- ========================================
-- Anti-Fraud Rules
-- ========================================
CREATE TABLE fraud_prevention_log (
    id UUID PRIMARY KEY,
    user_id VARCHAR(120) NOT NULL,
    action_code VARCHAR(80) NOT NULL,
    source_id VARCHAR(160) NOT NULL,
    event_id UUID NOT NULL,
    blocked_reason VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_fraud_log_user ON fraud_prevention_log(user_id, created_at DESC);
CREATE INDEX idx_fraud_log_action ON fraud_prevention_log(action_code, created_at DESC);

COMMENT ON TABLE fraud_prevention_log IS 'Log of blocked XP attempts for fraud detection';

-- ========================================
-- Insert Default Culture Actions
-- ========================================
INSERT INTO xp_actions (id, action_code, display_name, description, points, daily_cap, requires_verification, enabled, created_at, updated_at) VALUES
-- Language Learning
(gen_random_uuid(), 'DAILY_WORD_COMPLETED', 'Daily Word Completed', 'Complete daily word challenge', 10, 50, FALSE, TRUE, NOW(), NOW()),
(gen_random_uuid(), 'LANGUAGE_LESSON_COMPLETED', 'Language Lesson Completed', 'Complete a language lesson', 20, 100, FALSE, TRUE, NOW(), NOW()),
(gen_random_uuid(), 'PRONUNCIATION_PRACTICE', 'Pronunciation Practice', 'Practice pronunciation', 5, 30, FALSE, TRUE, NOW(), NOW()),

-- Culture Engagement
(gen_random_uuid(), 'CULTURE_QUIZ_COMPLETED', 'Culture Quiz Completed', 'Complete a cultural quiz', 15, 60, FALSE, TRUE, NOW(), NOW()),
(gen_random_uuid(), 'CULTURE_CHALLENGE_SUBMITTED', 'Culture Challenge Submitted', 'Submit a culture challenge entry', 25, 100, TRUE, TRUE, NOW(), NOW()),
(gen_random_uuid(), 'CULTURE_CONTENT_VIEWED', 'Culture Content Viewed', 'View cultural content', 2, 20, FALSE, TRUE, NOW(), NOW()),
(gen_random_uuid(), 'CULTURE_CONTENT_COMPLETED', 'Culture Content Completed', 'Complete cultural content', 10, 50, FALSE, TRUE, NOW(), NOW()),

-- Contributions
(gen_random_uuid(), 'TRANSLATION_PROPOSED', 'Translation Proposed', 'Propose a translation', 15, 75, TRUE, TRUE, NOW(), NOW()),
(gen_random_uuid(), 'TRANSLATION_VERIFIED', 'Translation Verified', 'Have your translation verified', 30, NULL, TRUE, TRUE, NOW(), NOW()),
(gen_random_uuid(), 'ORAL_HISTORY_CONTRIBUTED', 'Oral History Contributed', 'Contribute oral history', 50, 100, TRUE, TRUE, NOW(), NOW()),
(gen_random_uuid(), 'CULTURE_CONTENT_VERIFIED', 'Culture Content Verified', 'Verify cultural content', 20, 100, TRUE, TRUE, NOW(), NOW()),

-- Artisan Support
(gen_random_uuid(), 'ARTWORK_STORY_COMPLETED', 'Artwork Story Completed', 'Complete an artwork story', 15, 60, FALSE, TRUE, NOW(), NOW()),
(gen_random_uuid(), 'ARTISAN_FOLLOWED', 'Artisan Followed', 'Follow an artisan', 5, 25, FALSE, TRUE, NOW(), NOW()),
(gen_random_uuid(), 'ARTWORK_SHARED', 'Artwork Shared', 'Share an artwork', 5, 30, FALSE, TRUE, NOW(), NOW()),
(gen_random_uuid(), 'ARTWORK_PURCHASED', 'Artwork Purchased', 'Purchase an artwork', 100, NULL, FALSE, TRUE, NOW(), NOW());

-- ========================================
-- Insert Badge Definitions
-- ========================================
INSERT INTO badge_definitions (id, badge_code, name_key, description_key, tier, category, requirement_type, requirement_value, enabled, created_at, updated_at) VALUES
-- Language Badges
(gen_random_uuid(), 'LANGUAGE_EXPLORER', 'badge.language_explorer.name', 'badge.language_explorer.desc', 'BRONZE', 'CULTURE', 'LESSONS_COMPLETED', 10, TRUE, NOW(), NOW()),
(gen_random_uuid(), 'LANGUAGE_MASTER', 'badge.language_master.name', 'badge.language_master.desc', 'GOLD', 'CULTURE', 'LESSONS_COMPLETED', 100, TRUE, NOW(), NOW()),
(gen_random_uuid(), 'POLYGLOT', 'badge.polyglot.name', 'badge.polyglot.desc', 'PLATINUM', 'CULTURE', 'LANGUAGES_LEARNED', 5, TRUE, NOW(), NOW()),

-- Heritage Badges
(gen_random_uuid(), 'HERITAGE_CONTRIBUTOR', 'badge.heritage_contributor.name', 'badge.heritage_contributor.desc', 'SILVER', 'CONTRIBUTION', 'CONTRIBUTIONS_MADE', 20, TRUE, NOW(), NOW()),
(gen_random_uuid(), 'STORY_KEEPER', 'badge.story_keeper.name', 'badge.story_keeper.desc', 'GOLD', 'CONTRIBUTION', 'ORAL_HISTORIES', 10, TRUE, NOW(), NOW()),
(gen_random_uuid(), 'CULTURAL_TRANSLATOR', 'badge.cultural_translator.name', 'badge.cultural_translator.desc', 'SILVER', 'CONTRIBUTION', 'TRANSLATIONS_VERIFIED', 50, TRUE, NOW(), NOW()),

-- Artisan Badges
(gen_random_uuid(), 'ARTISAN_SUPPORTER', 'badge.artisan_supporter.name', 'badge.artisan_supporter.desc', 'BRONZE', 'ARTISAN', 'ARTISANS_FOLLOWED', 10, TRUE, NOW(), NOW()),
(gen_random_uuid(), 'ART_COLLECTOR', 'badge.art_collector.name', 'badge.art_collector.desc', 'GOLD', 'ARTISAN', 'ARTWORKS_PURCHASED', 5, TRUE, NOW(), NOW()),
(gen_random_uuid(), 'CULTURE_AMBASSADOR', 'badge.culture_ambassador.name', 'badge.culture_ambassador.desc', 'DIAMOND', 'CULTURE', 'TOTAL_CULTURE_XP', 10000, TRUE, NOW(), NOW());

-- ========================================
-- Daily Cap Tracking
-- ========================================
CREATE TABLE daily_action_counts (
    user_id VARCHAR(120) NOT NULL,
    action_code VARCHAR(80) NOT NULL,
    action_date DATE NOT NULL,
    count INTEGER NOT NULL DEFAULT 0,
    total_points INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    
    PRIMARY KEY (user_id, action_code, action_date)
);

CREATE INDEX idx_daily_counts_date ON daily_action_counts(action_date);

COMMENT ON TABLE daily_action_counts IS 'Track daily action counts for cap enforcement';
