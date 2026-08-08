package com.yeyamo_mobile.api.gamification_service.application.badge;

import com.yeyamo_mobile.api.gamification_service.domain.BadgeDefinition;
import com.yeyamo_mobile.api.gamification_service.domain.Progress;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Badge rules for Culture & Artisan features.
 *
 * <p>All thresholds are defined here and will be replaced by DB-driven config
 * in a future iteration (badge_definitions_cfg already contains the names/descriptions).
 * Counter types match the {@code counter_type} column in {@code xp_action_rules}.</p>
 *
 * <p>Badges and their counters:
 * <ul>
 *   <li>LANGUAGE_EXPLORER   — 10 lessons completed   (LESSON)</li>
 *   <li>HERITAGE_CONTRIBUTOR— 5 contributions         (ORAL_HISTORY + CONTENT_VFD ≥ 5)</li>
 *   <li>STORY_KEEPER        — 3 oral histories         (ORAL_HISTORY)</li>
 *   <li>CULTURAL_TRANSLATOR — 10 verified translations (TRANSLATION_VFD)</li>
 *   <li>ARTISAN_SUPPORTER   — 5 artwork stories        (ARTWORK_STORY)</li>
 *   <li>CULTURE_AMBASSADOR  — 500 XP total             (totalXp)</li>
 * </ul>
 * </p>
 */
@Component
public class CultureBadgeRule implements BadgeRule {

    @Override
    public List<BadgeDefinition> evaluate(Progress progress, Map<String, Long> counters) {
        List<BadgeDefinition> earned = new ArrayList<>();

        long lessons     = counters.getOrDefault("LESSON",          0L);
        long oralHistory = counters.getOrDefault("ORAL_HISTORY",    0L);
        long contentVfd  = counters.getOrDefault("CONTENT_VFD",     0L);
        long transVfd    = counters.getOrDefault("TRANSLATION_VFD", 0L);
        long artworkStory= counters.getOrDefault("ARTWORK_STORY",   0L);

        // LANGUAGE_EXPLORER — 10 lessons
        if (lessons >= 10)
            earned.add(new BadgeDefinition(
                "LANGUAGE_EXPLORER",
                "Language Explorer",
                "Complete 10 language lessons"));

        // HERITAGE_CONTRIBUTOR — 5 total cultural contributions
        if (oralHistory + contentVfd >= 5)
            earned.add(new BadgeDefinition(
                "HERITAGE_CONTRIBUTOR",
                "Heritage Contributor",
                "Contribute 5 cultural items"));

        // STORY_KEEPER — 3 oral histories
        if (oralHistory >= 3)
            earned.add(new BadgeDefinition(
                "STORY_KEEPER",
                "Story Keeper",
                "Record 3 oral histories"));

        // CULTURAL_TRANSLATOR — 10 verified translations
        if (transVfd >= 10)
            earned.add(new BadgeDefinition(
                "CULTURAL_TRANSLATOR",
                "Cultural Translator",
                "Get 10 translations verified"));

        // ARTISAN_SUPPORTER — 5 artwork stories
        if (artworkStory >= 5)
            earned.add(new BadgeDefinition(
                "ARTISAN_SUPPORTER",
                "Artisan Supporter",
                "Complete 5 artwork stories"));

        // CULTURE_AMBASSADOR — 500 XP total
        if (progress.totalXp() >= 500)
            earned.add(new BadgeDefinition(
                "CULTURE_AMBASSADOR",
                "Culture Ambassador",
                "Earn 500 XP from culture actions"));

        return earned;
    }
}
