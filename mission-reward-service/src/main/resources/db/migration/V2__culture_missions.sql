-- V2: Seed culture & artisan missions
-- These missions drive the mission-reward loop for cultural content.
-- Objectives use event_type to match Kafka events consumed by the mission engine.

-- Helper to avoid duplicate inserts on re-run
DO $$
BEGIN

-- =========================================================================
-- Mission 1: Language Explorer Starter
-- Complete 5 language lessons to earn the Language Explorer badge reward
-- =========================================================================
IF NOT EXISTS (SELECT 1 FROM mission_definitions WHERE code = 'LANGUAGE_EXPLORER_STARTER') THEN
    INSERT INTO mission_definitions
        (id, code, title, description, status, reward_code, reward_title, reward_amount,
         created_at, updated_at)
    VALUES
        (gen_random_uuid(),
         'LANGUAGE_EXPLORER_STARTER',
         'Language Explorer',
         'Complete 5 language lessons to unlock the Language Explorer journey',
         'ACTIVE',
         'BADGE_LANGUAGE_EXPLORER',
         'Language Explorer Badge',
         1,
         now(), now());

    INSERT INTO mission_objectives
        (id, mission_id, label, event_type, metric, target_value, position)
    VALUES
        (gen_random_uuid(),
         (SELECT id FROM mission_definitions WHERE code = 'LANGUAGE_EXPLORER_STARTER'),
         'Complete 5 language lessons',
         'LanguageLessonCompleted',
         'COUNT',
         5,
         0);
END IF;

-- =========================================================================
-- Mission 2: Heritage Contributor
-- Contribute 3 items (oral history or verified content)
-- =========================================================================
IF NOT EXISTS (SELECT 1 FROM mission_definitions WHERE code = 'HERITAGE_CONTRIBUTOR_MISSION') THEN
    INSERT INTO mission_definitions
        (id, code, title, description, status, reward_code, reward_title, reward_amount,
         created_at, updated_at)
    VALUES
        (gen_random_uuid(),
         'HERITAGE_CONTRIBUTOR_MISSION',
         'Heritage Contributor',
         'Contribute 3 cultural items to the YeYamo heritage collection',
         'ACTIVE',
         'BADGE_HERITAGE_CONTRIBUTOR',
         'Heritage Contributor Badge',
         1,
         now(), now());

    INSERT INTO mission_objectives
        (id, mission_id, label, event_type, metric, target_value, position)
    VALUES
        (gen_random_uuid(),
         (SELECT id FROM mission_definitions WHERE code = 'HERITAGE_CONTRIBUTOR_MISSION'),
         'Record 2 oral histories',
         'OralHistoryContributed',
         'COUNT',
         2,
         0),
        (gen_random_uuid(),
         (SELECT id FROM mission_definitions WHERE code = 'HERITAGE_CONTRIBUTOR_MISSION'),
         'Get 1 content verified',
         'CultureContentVerified',
         'COUNT',
         1,
         1);
END IF;

-- =========================================================================
-- Mission 3: Story Keeper
-- Record 3 oral histories
-- =========================================================================
IF NOT EXISTS (SELECT 1 FROM mission_definitions WHERE code = 'STORY_KEEPER_MISSION') THEN
    INSERT INTO mission_definitions
        (id, code, title, description, status, reward_code, reward_title, reward_amount,
         created_at, updated_at)
    VALUES
        (gen_random_uuid(),
         'STORY_KEEPER_MISSION',
         'Story Keeper',
         'Preserve cultural heritage by recording 3 oral histories',
         'ACTIVE',
         'BADGE_STORY_KEEPER',
         'Story Keeper Badge',
         1,
         now(), now());

    INSERT INTO mission_objectives
        (id, mission_id, label, event_type, metric, target_value, position)
    VALUES
        (gen_random_uuid(),
         (SELECT id FROM mission_definitions WHERE code = 'STORY_KEEPER_MISSION'),
         'Record 3 oral histories',
         'OralHistoryContributed',
         'COUNT',
         3,
         0);
END IF;

-- =========================================================================
-- Mission 4: Cultural Translator
-- Get 5 translations verified by experts
-- =========================================================================
IF NOT EXISTS (SELECT 1 FROM mission_definitions WHERE code = 'CULTURAL_TRANSLATOR_MISSION') THEN
    INSERT INTO mission_definitions
        (id, code, title, description, status, reward_code, reward_title, reward_amount,
         created_at, updated_at)
    VALUES
        (gen_random_uuid(),
         'CULTURAL_TRANSLATOR_MISSION',
         'Cultural Translator',
         'Help bridge cultures — get 5 translations verified by cultural experts',
         'ACTIVE',
         'BADGE_CULTURAL_TRANSLATOR',
         'Cultural Translator Badge',
         1,
         now(), now());

    INSERT INTO mission_objectives
        (id, mission_id, label, event_type, metric, target_value, position)
    VALUES
        (gen_random_uuid(),
         (SELECT id FROM mission_definitions WHERE code = 'CULTURAL_TRANSLATOR_MISSION'),
         'Propose 10 translations',
         'TranslationProposed',
         'COUNT',
         10,
         0),
        (gen_random_uuid(),
         (SELECT id FROM mission_definitions WHERE code = 'CULTURAL_TRANSLATOR_MISSION'),
         'Get 5 translations verified',
         'TranslationVerified',
         'COUNT',
         5,
         1);
END IF;

-- =========================================================================
-- Mission 5: Artisan Supporter
-- Complete 5 artwork stories
-- =========================================================================
IF NOT EXISTS (SELECT 1 FROM mission_definitions WHERE code = 'ARTISAN_SUPPORTER_MISSION') THEN
    INSERT INTO mission_definitions
        (id, code, title, description, status, reward_code, reward_title, reward_amount,
         created_at, updated_at)
    VALUES
        (gen_random_uuid(),
         'ARTISAN_SUPPORTER_MISSION',
         'Artisan Supporter',
         'Discover the story behind 5 artisan creations',
         'ACTIVE',
         'BADGE_ARTISAN_SUPPORTER',
         'Artisan Supporter Badge',
         1,
         now(), now());

    INSERT INTO mission_objectives
        (id, mission_id, label, event_type, metric, target_value, position)
    VALUES
        (gen_random_uuid(),
         (SELECT id FROM mission_definitions WHERE code = 'ARTISAN_SUPPORTER_MISSION'),
         'Complete 5 artwork stories',
         'ArtworkStoryCompleted',
         'COUNT',
         5,
         0);
END IF;

-- =========================================================================
-- Mission 6: Culture Ambassador (advanced)
-- Complete all 5 culture missions above → earn the Culture Ambassador badge
-- =========================================================================
IF NOT EXISTS (SELECT 1 FROM mission_definitions WHERE code = 'CULTURE_AMBASSADOR_MISSION') THEN
    INSERT INTO mission_definitions
        (id, code, title, description, status, reward_code, reward_title, reward_amount,
         created_at, updated_at)
    VALUES
        (gen_random_uuid(),
         'CULTURE_AMBASSADOR_MISSION',
         'Culture Ambassador',
         'Master all aspects of YeYamo culture — become a Culture Ambassador',
         'ACTIVE',
         'BADGE_CULTURE_AMBASSADOR',
         'Culture Ambassador Badge',
         1,
         now(), now());

    -- This mission tracks completion of the other culture missions via gamification.xp.awarded
    -- with XP threshold (gamification integration) — tracked via total XP milestone
    INSERT INTO mission_objectives
        (id, mission_id, label, event_type, metric, target_value, position)
    VALUES
        (gen_random_uuid(),
         (SELECT id FROM mission_definitions WHERE code = 'CULTURE_AMBASSADOR_MISSION'),
         'Complete Language Explorer Starter',
         'mission.reward.granted',
         'COUNT',
         1,
         0),
        (gen_random_uuid(),
         (SELECT id FROM mission_definitions WHERE code = 'CULTURE_AMBASSADOR_MISSION'),
         'Complete Heritage Contributor mission',
         'mission.reward.granted',
         'COUNT',
         1,
         1),
        (gen_random_uuid(),
         (SELECT id FROM mission_definitions WHERE code = 'CULTURE_AMBASSADOR_MISSION'),
         'Complete Artisan Supporter mission',
         'mission.reward.granted',
         'COUNT',
         1,
         2);
END IF;

END $$;
