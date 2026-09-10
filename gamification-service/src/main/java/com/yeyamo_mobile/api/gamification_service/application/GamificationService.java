package com.yeyamo_mobile.api.gamification_service.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.gamification_service.application.badge.BadgeCatalog;
import com.yeyamo_mobile.api.gamification_service.application.badge.BadgeRule;
import com.yeyamo_mobile.api.gamification_service.application.port.GamificationCachePort;
import com.yeyamo_mobile.api.gamification_service.application.port.GamificationOutboxPort;
import com.yeyamo_mobile.api.gamification_service.application.port.GamificationRepositoryPort;
import com.yeyamo_mobile.api.gamification_service.domain.AwardResult;
import com.yeyamo_mobile.api.gamification_service.domain.Badge;
import com.yeyamo_mobile.api.gamification_service.domain.BadgeDefinition;
import com.yeyamo_mobile.api.gamification_service.domain.Progress;
import com.yeyamo_mobile.api.gamification_service.domain.Reward;
import com.yeyamo_mobile.api.gamification_service.domain.XpActivity;
import com.yeyamo_mobile.api.gamification_service.domain.XpHistoryEntry;
import com.yeyamo_mobile.api.gamification_service.domain.RewardStatus;

@Service
public class GamificationService {
    private final GamificationRepositoryPort repo;
    private final GamificationCachePort cache;
    private final GamificationOutboxPort outbox;
    private final List<BadgeRule> rules;

    public GamificationService(GamificationRepositoryPort repo, GamificationCachePort cache,
            GamificationOutboxPort outbox, List<BadgeRule> rules) {
        this.repo = repo;
        this.cache = cache;
        this.outbox = outbox;
        this.rules = List.copyOf(rules);
    }

    @Transactional
    public void apply(XpActivity activity, String correlation) {
        AwardResult result = repo.award(activity);
        if (!result.created()) {
            return;
        }
        if (activity.counterType() != null) {
            repo.incrementCounter(activity.userId(), activity.counterType());
        }
        outbox.append("gamification.xp.awarded", activity.userId(), correlation,
                Map.of("userId", activity.userId(), "points", activity.points(),
                        "reason", activity.reason(), "totalXp", result.after().totalXp()));
        if (activity.destinationId() != null
                && repo.stamp(activity.userId(), activity.destinationId(), activity.eventId())) {
            outbox.append("gamification.passport.stamped", activity.userId(), correlation,
                    Map.of("userId", activity.userId(), "destinationId", activity.destinationId()));
        }
        if (result.before().level() != result.after().level()) {
            outbox.append("gamification.level.changed", activity.userId(), correlation,
                    Map.of("userId", activity.userId(), "level", result.after().level()));
            grantReward(activity.userId(), "LEVEL_" + result.after().level(),
                    "Récompense niveau " + result.after().level(),
                    "level:" + result.after().level(), correlation);
        }
        Map<String, Long> counters = repo.counters(activity.userId());
        for (BadgeRule rule : rules) {
            for (BadgeDefinition badge : rule.evaluate(result.after(), counters)) {
                if (repo.grantBadge(activity.userId(), badge, activity.eventId())) {
                    outbox.append("gamification.badge.earned", activity.userId(), correlation,
                            Map.of("userId", activity.userId(), "badgeCode", badge.code()));
                    grantReward(activity.userId(), "BADGE_" + badge.code(),
                            "Récompense " + badge.name(), "badge:" + badge.code(), correlation);
                }
            }
        }
        cache.evict(activity.userId());
    }

    @Transactional(readOnly = true)
    public GamificationView view(String user) {
        return cache.get(user).orElseGet(() -> {
            GamificationView view = new GamificationView(
                    repo.progress(user), repo.badges(user), repo.passport(user), repo.rewards(user));
            cache.put(user, view);
            return view;
        });
    }

    @Transactional(readOnly = true)
    public List<BadgeCatalogEntry> catalog(String user) {
        Map<String, Badge> earned = repo.badges(user).stream()
                .collect(Collectors.toMap(Badge::code, badge -> badge, (left, right) -> left));
        return BadgeCatalog.all().stream().map(definition -> {
            Badge badge = earned.get(definition.code());
            return new BadgeCatalogEntry(definition.code(), definition.name(),
                    definition.description(), badge != null, badge == null ? null : badge.earnedAt());
        }).toList();
    }

    @Transactional(readOnly = true)
    public BadgeCatalogEntry badge(String user, String code) {
        BadgeDefinition definition = BadgeCatalog.required(code);
        Badge earned = repo.badges(user).stream()
                .filter(badge -> badge.code().equals(definition.code()))
                .findFirst().orElse(null);
        return new BadgeCatalogEntry(definition.code(), definition.name(), definition.description(),
                earned != null, earned == null ? null : earned.earnedAt());
    }

    @Transactional(readOnly = true)
    public BadgeStats stats(String user) {
        Progress progress = repo.progress(user);
        return new BadgeStats(repo.badges(user).size(), BadgeCatalog.all().size(),
                progress.totalXp(), progress.level(), repo.rank(user));
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> leaderboard(int limit) {
        AtomicLong rank = new AtomicLong();
        return repo.leaderboard(limit).stream()
                .map(progress -> new LeaderboardEntry(rank.incrementAndGet(),
                        progress.userId(), progress.totalXp(), progress.level()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PassportSummary passportSummary(String user) {
        GamificationView view = view(user);
        Progress progress = view.progress();
        long currentThreshold = (long) (progress.level() - 1) * (progress.level() - 1) * 100;
        long nextThreshold = progress.xpForNextLevel();
        return new PassportSummary(progress.totalXp(), progress.level(), currentThreshold, nextThreshold,
                Math.max(0, progress.totalXp() - currentThreshold),
                Math.max(0, nextThreshold - progress.totalXp()), view.badges().size(), view.passport().size(),
                (int) view.rewards().stream().filter(reward -> reward.status() == RewardStatus.AVAILABLE).count(),
                progress.currentStreak(), progress.longestStreak(), progress.lastActivityDate(), progress.updatedAt());
    }

    @Transactional(readOnly = true)
    public Page<XpHistoryEntry> history(String user, Pageable pageable) {
        return repo.history(user, pageable);
    }

    @Transactional
    public Reward claim(String user, UUID id, String correlation) {
        Reward reward = repo.claimReward(user, id);
        outbox.append("gamification.reward.claimed", user, correlation,
                Map.of("userId", user, "rewardId", id, "rewardCode", reward.code()));
        cache.evict(user);
        return reward;
    }

    private void grantReward(String user, String code, String title,
            String source, String correlation) {
        repo.grantReward(user, code, title, source).ifPresent(reward ->
                outbox.append("gamification.reward.granted", user, correlation,
                        Map.of("userId", user, "rewardCode", code, "rewardId", reward.id())));
    }
}
