-- Analytics tables for Culture and Artisan features

-- ========================================
-- Culture Engagement Analytics
-- ========================================
CREATE TABLE culture_engagement_daily (
    id UUID PRIMARY KEY,
    aggregation_date DATE NOT NULL,
    country_code VARCHAR(2),
    culture_id VARCHAR(120),
    language_code VARCHAR(10),
    
    -- Views and engagement
    content_views BIGINT NOT NULL DEFAULT 0,
    unique_viewers BIGINT NOT NULL DEFAULT 0,
    avg_view_duration_seconds INTEGER,
    total_view_duration_seconds BIGINT NOT NULL DEFAULT 0,
    
    -- Completions
    content_completed BIGINT NOT NULL DEFAULT 0,
    completion_rate DECIMAL(5,2),
    
    -- Interactions
    likes BIGINT NOT NULL DEFAULT 0,
    shares BIGINT NOT NULL DEFAULT 0,
    saves BIGINT NOT NULL DEFAULT 0,
    comments BIGINT NOT NULL DEFAULT 0,
    
    -- Metadata
    created_at TIMESTAMPTZ NOT NULL,
    
    CONSTRAINT uk_culture_engagement_daily UNIQUE (aggregation_date, country_code, culture_id, language_code)
);

CREATE INDEX idx_culture_eng_date ON culture_engagement_daily(aggregation_date DESC);
CREATE INDEX idx_culture_eng_country ON culture_engagement_daily(country_code, aggregation_date DESC);
CREATE INDEX idx_culture_eng_culture ON culture_engagement_daily(culture_id, aggregation_date DESC);

COMMENT ON TABLE culture_engagement_daily IS 'Daily aggregated culture content engagement metrics';

-- ========================================
-- Language Learning Analytics
-- ========================================
CREATE TABLE language_learning_daily (
    id UUID PRIMARY KEY,
    aggregation_date DATE NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    country_code VARCHAR(2),
    
    -- Learners
    active_learners BIGINT NOT NULL DEFAULT 0,
    new_learners BIGINT NOT NULL DEFAULT 0,
    returning_learners BIGINT NOT NULL DEFAULT 0,
    
    -- Lessons
    lessons_started BIGINT NOT NULL DEFAULT 0,
    lessons_completed BIGINT NOT NULL DEFAULT 0,
    lesson_completion_rate DECIMAL(5,2),
    
    -- Words
    words_learned BIGINT NOT NULL DEFAULT 0,
    daily_words_completed BIGINT NOT NULL DEFAULT 0,
    pronunciation_practices BIGINT NOT NULL DEFAULT 0,
    
    -- Quizzes
    quizzes_taken BIGINT NOT NULL DEFAULT 0,
    quiz_avg_score DECIMAL(5,2),
    
    -- Engagement
    avg_session_duration_minutes INTEGER,
    total_study_time_minutes BIGINT NOT NULL DEFAULT 0,
    
    created_at TIMESTAMPTZ NOT NULL,
    
    CONSTRAINT uk_language_learning_daily UNIQUE (aggregation_date, language_code, country_code)
);

CREATE INDEX idx_lang_learning_date ON language_learning_daily(aggregation_date DESC);
CREATE INDEX idx_lang_learning_code ON language_learning_daily(language_code, aggregation_date DESC);

COMMENT ON TABLE language_learning_daily IS 'Daily language learning metrics and progress';

-- ========================================
-- Artwork Popularity Analytics
-- ========================================
CREATE TABLE artwork_popularity_daily (
    id UUID PRIMARY KEY,
    aggregation_date DATE NOT NULL,
    country_code VARCHAR(2),
    artwork_id VARCHAR(120) NOT NULL,
    artisan_id VARCHAR(120) NOT NULL,
    
    -- Views
    views BIGINT NOT NULL DEFAULT 0,
    unique_viewers BIGINT NOT NULL DEFAULT 0,
    
    -- Engagement
    likes BIGINT NOT NULL DEFAULT 0,
    shares BIGINT NOT NULL DEFAULT 0,
    saves BIGINT NOT NULL DEFAULT 0,
    story_views BIGINT NOT NULL DEFAULT 0,
    story_completed BIGINT NOT NULL DEFAULT 0,
    
    -- Commerce
    inquiries BIGINT NOT NULL DEFAULT 0,
    purchases BIGINT NOT NULL DEFAULT 0,
    revenue DECIMAL(12,2) DEFAULT 0,
    
    -- Reach
    reach_countries INTEGER DEFAULT 0,
    international_views BIGINT NOT NULL DEFAULT 0,
    
    created_at TIMESTAMPTZ NOT NULL,
    
    CONSTRAINT uk_artwork_daily UNIQUE (aggregation_date, artwork_id)
);

CREATE INDEX idx_artwork_pop_date ON artwork_popularity_daily(aggregation_date DESC);
CREATE INDEX idx_artwork_pop_country ON artwork_popularity_daily(country_code, aggregation_date DESC);
CREATE INDEX idx_artwork_pop_id ON artwork_popularity_daily(artwork_id, aggregation_date DESC);
CREATE INDEX idx_artwork_pop_artisan ON artwork_popularity_daily(artisan_id, aggregation_date DESC);

COMMENT ON TABLE artwork_popularity_daily IS 'Daily artwork popularity and commerce metrics';

-- ========================================
-- Artisan KPIs
-- ========================================
CREATE TABLE artisan_kpis_daily (
    id UUID PRIMARY KEY,
    aggregation_date DATE NOT NULL,
    artisan_id VARCHAR(120) NOT NULL,
    
    -- Profile
    new_followers BIGINT NOT NULL DEFAULT 0,
    total_followers BIGINT NOT NULL DEFAULT 0,
    profile_views BIGINT NOT NULL DEFAULT 0,
    
    -- Content
    active_artworks INTEGER DEFAULT 0,
    new_artworks INTEGER DEFAULT 0,
    total_views BIGINT NOT NULL DEFAULT 0,
    avg_views_per_artwork DECIMAL(10,2),
    
    -- Engagement
    total_likes BIGINT NOT NULL DEFAULT 0,
    total_shares BIGINT NOT NULL DEFAULT 0,
    total_saves BIGINT NOT NULL DEFAULT 0,
    engagement_rate DECIMAL(5,2),
    
    -- Commerce
    inquiries BIGINT NOT NULL DEFAULT 0,
    sales BIGINT NOT NULL DEFAULT 0,
    revenue DECIMAL(12,2) DEFAULT 0,
    avg_sale_value DECIMAL(12,2),
    
    -- Reach
    reach_countries INTEGER DEFAULT 0,
    international_revenue_pct DECIMAL(5,2),
    
    -- Verification
    verification_status VARCHAR(30),
    authenticity_claims INTEGER DEFAULT 0,
    verified_artworks INTEGER DEFAULT 0,
    
    created_at TIMESTAMPTZ NOT NULL,
    
    CONSTRAINT uk_artisan_kpi_daily UNIQUE (aggregation_date, artisan_id)
);

CREATE INDEX idx_artisan_kpi_date ON artisan_kpis_daily(aggregation_date DESC);
CREATE INDEX idx_artisan_kpi_id ON artisan_kpis_daily(artisan_id, aggregation_date DESC);

COMMENT ON TABLE artisan_kpis_daily IS 'Daily artisan performance and business metrics';

-- ========================================
-- Culture Contribution Analytics
-- ========================================
CREATE TABLE culture_contribution_daily (
    id UUID PRIMARY KEY,
    aggregation_date DATE NOT NULL,
    country_code VARCHAR(2),
    contribution_type VARCHAR(50) NOT NULL,
    
    -- Contributions
    total_contributions BIGINT NOT NULL DEFAULT 0,
    unique_contributors BIGINT NOT NULL DEFAULT 0,
    
    -- Quality
    verified_contributions BIGINT NOT NULL DEFAULT 0,
    rejected_contributions BIGINT NOT NULL DEFAULT 0,
    pending_contributions BIGINT NOT NULL DEFAULT 0,
    verification_rate DECIMAL(5,2),
    
    -- Impact
    views_generated BIGINT NOT NULL DEFAULT 0,
    avg_quality_score DECIMAL(3,2),
    
    created_at TIMESTAMPTZ NOT NULL,
    
    CONSTRAINT uk_contribution_daily UNIQUE (aggregation_date, country_code, contribution_type),
    CONSTRAINT chk_contrib_type CHECK (contribution_type IN (
        'TRANSLATION', 'ORAL_HISTORY', 'CULTURE_CONTENT', 
        'AUDIO_PRONUNCIATION', 'CHALLENGE_SUBMISSION', 'VERIFICATION'
    ))
);

CREATE INDEX idx_contrib_date ON culture_contribution_daily(aggregation_date DESC);
CREATE INDEX idx_contrib_country ON culture_contribution_daily(country_code, aggregation_date DESC);
CREATE INDEX idx_contrib_type ON culture_contribution_daily(contribution_type, aggregation_date DESC);

COMMENT ON TABLE culture_contribution_daily IS 'Daily contribution metrics by type and region';

-- ========================================
-- Event Stream Processing State
-- ========================================
CREATE TABLE analytics_processing_state (
    processor_name VARCHAR(100) PRIMARY KEY,
    last_processed_offset BIGINT NOT NULL,
    last_processed_timestamp TIMESTAMPTZ NOT NULL,
    last_event_id UUID,
    status VARCHAR(30) NOT NULL,
    error_count INTEGER DEFAULT 0,
    last_error TEXT,
    updated_at TIMESTAMPTZ NOT NULL,
    
    CONSTRAINT chk_processor_status CHECK (status IN ('RUNNING', 'PAUSED', 'ERROR', 'STOPPED'))
);

COMMENT ON TABLE analytics_processing_state IS 'Track Kafka stream processing state for idempotence';

-- ========================================
-- Real-time Metrics Cache
-- ========================================
CREATE TABLE realtime_culture_metrics (
    metric_key VARCHAR(200) PRIMARY KEY,
    metric_value BIGINT NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    
    CONSTRAINT chk_metric_type CHECK (metric_type IN (
        'COUNTER', 'GAUGE', 'RATE', 'PERCENTAGE'
    ))
);

CREATE INDEX idx_realtime_updated ON realtime_culture_metrics(updated_at DESC);

COMMENT ON TABLE realtime_culture_metrics IS 'Cache for frequently accessed real-time metrics';

-- ========================================
-- Materialized Views for Performance
-- ========================================

-- Top Artworks by Country (Last 30 Days)
CREATE MATERIALIZED VIEW mv_top_artworks_by_country AS
SELECT 
    country_code,
    artwork_id,
    artisan_id,
    SUM(views) as total_views,
    SUM(likes) as total_likes,
    SUM(purchases) as total_purchases,
    SUM(revenue) as total_revenue,
    MAX(aggregation_date) as last_updated
FROM (
    SELECT 
        COALESCE(a.country_code, 'GLOBAL') as country_code,
        a.artwork_id,
        a.artisan_id,
        a.views,
        a.likes,
        a.purchases,
        a.revenue,
        a.aggregation_date
    FROM artwork_popularity_daily a
    WHERE a.aggregation_date >= CURRENT_DATE - INTERVAL '30 days'
) sub
GROUP BY country_code, artwork_id, artisan_id;

CREATE UNIQUE INDEX idx_mv_top_artworks ON mv_top_artworks_by_country(country_code, total_views DESC);

-- Top Languages by Activity (Last 7 Days)
CREATE MATERIALIZED VIEW mv_top_languages_weekly AS
SELECT 
    language_code,
    SUM(active_learners) as total_active_learners,
    SUM(lessons_completed) as total_lessons_completed,
    AVG(lesson_completion_rate) as avg_completion_rate,
    SUM(words_learned) as total_words_learned,
    MAX(aggregation_date) as last_updated
FROM language_learning_daily
WHERE aggregation_date >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY language_code;

CREATE UNIQUE INDEX idx_mv_top_languages ON mv_top_languages_weekly(total_active_learners DESC);

COMMENT ON MATERIALIZED VIEW mv_top_artworks_by_country IS 'Top artworks by country for last 30 days - refresh daily';
COMMENT ON MATERIALIZED VIEW mv_top_languages_weekly IS 'Top languages by activity for last 7 days - refresh daily';

-- ========================================
-- Functions for Refresh
-- ========================================
CREATE OR REPLACE FUNCTION refresh_culture_materialized_views()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_top_artworks_by_country;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_top_languages_weekly;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION refresh_culture_materialized_views IS 'Refresh all culture-related materialized views';
