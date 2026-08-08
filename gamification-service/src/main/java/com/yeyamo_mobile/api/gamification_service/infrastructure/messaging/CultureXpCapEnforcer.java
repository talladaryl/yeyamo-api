package com.yeyamo_mobile.api.gamification_service.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Enforces per-user daily XP caps for culture actions.
 *
 * <p>Algorithm (atomic upsert):
 * <ol>
 *   <li>INSERT OR UPDATE the daily cap row for (userId, actionCode, today).</li>
 *   <li>If {@code count_today} already reached the cap from the rules table,
 *       return {@code false} (the caller should skip this XpActivity).</li>
 *   <li>Otherwise increment and return {@code true}.</li>
 * </ol>
 * </p>
 *
 * <p>A cap of {@code NULL} means unlimited.</p>
 */
@Component
public class CultureXpCapEnforcer {

    private static final Logger log = LoggerFactory.getLogger(CultureXpCapEnforcer.class);

    private final JdbcTemplate jdbc;

    public CultureXpCapEnforcer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Check whether the user has not yet hit the daily cap for this action,
     * and if so increment the counter.
     *
     * @return {@code true} if the activity is allowed and counter was incremented,
     *         {@code false} if the daily cap is already reached.
     */
    public boolean allowAndRecord(String userId, String actionCode, int points) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        // Fetch the cap from the rules table (null = unlimited)
        Integer cap = jdbc.queryForObject(
            "SELECT daily_cap FROM xp_action_rules WHERE action_code = ? AND enabled = true",
            Integer.class, actionCode);

        if (cap == null) {
            // Unlimited — record and allow
            upsertDailyCap(userId, actionCode, today, points);
            return true;
        }

        // Fetch current daily XP for this action
        Integer currentXp = jdbc.queryForObject(
            """
            SELECT COALESCE(xp_today, 0)
            FROM   xp_daily_caps
            WHERE  user_id = ? AND action_code = ? AND cap_date = ?
            """,
            Integer.class, userId, actionCode, today);

        if (currentXp == null) currentXp = 0;

        if (currentXp + points > cap) {
            log.debug("Daily cap reached for user={} action={} ({}+{} > {})",
                      userId, actionCode, currentXp, points, cap);
            return false;
        }

        upsertDailyCap(userId, actionCode, today, points);
        return true;
    }

    private void upsertDailyCap(String userId, String actionCode, LocalDate today, int points) {
        jdbc.update("""
            INSERT INTO xp_daily_caps (user_id, action_code, cap_date, count_today, xp_today, updated_at)
            VALUES (?, ?, ?, 1, ?, now())
            ON CONFLICT (user_id, action_code, cap_date)
            DO UPDATE SET count_today = xp_daily_caps.count_today + 1,
                          xp_today    = xp_daily_caps.xp_today    + EXCLUDED.xp_today,
                          updated_at  = now()
            """,
            userId, actionCode, today, points);
    }
}
