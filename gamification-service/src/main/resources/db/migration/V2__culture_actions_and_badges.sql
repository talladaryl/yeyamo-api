-- V2: Culture & Artisan gamification — configurable XP actions and badge definitions
--
-- Points are stored in configuration tables so they can be adjusted without code changes.
-- The EventXpPolicy reads from xp_action_rules at startup (or can be reloaded).

-- =========================================================================
-- XP Action Rules (configurable per-action points + caps)
-- =========================================================================
CREATE TABLE xp_action_rules (
    action_code         VARCHAR(80)  PRIMARY KEY,
    points              INTEGER      NOT NULL CHECK (points > 0),
    counter_type        VARCHAR(50),                 -- nullable = no counter increment
    daily_cap           INTEGER,                     -- max XP from this action per day (NULL = unlimited)
    requires_source_id  BOOLEAN      NOT NULL DEFAULT TRUE,
    description         VARCHAR(300) NOT NULL,
    enabled             BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO xp_action_rules(action_code, points, counter_type, daily_cap, description) VALUES
-- Language learning
('DAILY_WORD_COMPLETED',       10, 'DAILY_WORD',        50,  'Complete a daily vocabulary word'),
('LANGUAGE_LESSON_COMPLETED',  20, 'LESSON',           100,  'Complete a language lesson'),
-- Culture engagement
('CULTURE_QUIZ_COMPLETED',     15, 'QUIZ',              60,  'Complete a cultural quiz'),
('CULTURE_CHALLENGE_SUBMITTED',25, 'CHALLENGE',        100,  'Submit an entry to a culture challenge'),
-- Contributions (higher value, needs verification at source)
('TRANSLATION_PROPOSED',       15, 'TRANSLATION',       75,  'Propose a translation'),
('TRANSLATION_VERIFIED',       30, 'TRANSLATION_VFD', NULL,  'Have your translation verified by an expert'),
('ORAL_HISTORY_CONTRIBUTED',   50, 'ORAL_HISTORY',     100,  'Contribute an oral history recording'),
('CULTURE_CONTENT_VERIFIED',   20, 'CONTENT_VFD',      100,  'Have your cultural content verified'),
-- Artisan support
('ARTWORK_STORY_COMPLETED',    15, 'ARTWORK_STORY',     60,  'Complete the story of an artwork');

-- =========================================================================
-- Badge Definitions (configurable names/descriptions for i18n)
-- =========================================================================
CREATE TABLE badge_definitions_cfg (
    badge_code          VARCHAR(80)  PRIMARY KEY,
    name_key            VARCHAR(120) NOT NULL,   -- i18n key, e.g. badge.language_explorer.name
    default_name        VARCHAR(160) NOT NULL,
    description_key     VARCHAR(120) NOT NULL,
    default_description VARCHAR(500) NOT NULL,
    tier                VARCHAR(20),             -- BRONZE / SILVER / GOLD / PLATINUM / DIAMOND
    category            VARCHAR(40)  NOT NULL,
    enabled             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO badge_definitions_cfg(badge_code, name_key, default_name, description_key, default_description, tier, category) VALUES
('LANGUAGE_EXPLORER',    'badge.language_explorer.name',    'Language Explorer',    'badge.language_explorer.desc',    'Complete 10 language lessons',             'BRONZE',   'CULTURE'),
('HERITAGE_CONTRIBUTOR', 'badge.heritage_contributor.name', 'Heritage Contributor', 'badge.heritage_contributor.desc', 'Contribute 5 cultural items',              'SILVER',   'CONTRIBUTION'),
('STORY_KEEPER',         'badge.story_keeper.name',         'Story Keeper',         'badge.story_keeper.desc',         'Record 3 oral histories',                  'GOLD',     'CONTRIBUTION'),
('CULTURAL_TRANSLATOR',  'badge.cultural_translator.name',  'Cultural Translator',  'badge.cultural_translator.desc',  'Get 10 translations verified',             'SILVER',   'CONTRIBUTION'),
('ARTISAN_SUPPORTER',    'badge.artisan_supporter.name',    'Artisan Supporter',    'badge.artisan_supporter.desc',    'Complete 5 artwork stories',               'BRONZE',   'ARTISAN'),
('CULTURE_AMBASSADOR',   'badge.culture_ambassador.name',   'Culture Ambassador',   'badge.culture_ambassador.desc',   'Earn 500 XP from culture actions',         'GOLD',     'CULTURE');

-- =========================================================================
-- Anti-fraud: daily action counts (for cap enforcement)
-- =========================================================================
CREATE TABLE xp_daily_caps (
    user_id       VARCHAR(120) NOT NULL,
    action_code   VARCHAR(80)  NOT NULL,
    cap_date      DATE         NOT NULL,
    count_today   INTEGER      NOT NULL DEFAULT 0,
    xp_today      INTEGER      NOT NULL DEFAULT 0,
    updated_at    TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (user_id, action_code, cap_date)
);
CREATE INDEX idx_xp_daily_caps_cleanup ON xp_daily_caps(cap_date);

COMMENT ON TABLE xp_action_rules      IS 'Configurable XP points per culture/artisan action';
COMMENT ON TABLE badge_definitions_cfg IS 'Configurable badge names and descriptions (i18n-ready)';
COMMENT ON TABLE xp_daily_caps        IS 'Per-user daily XP cap enforcement for anti-fraud';
