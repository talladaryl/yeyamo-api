package com.yeyamo_mobile.api.gamification_service.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.yeyamo_mobile.api.gamification_service.application.port.GamificationRepositoryPort;
import com.yeyamo_mobile.api.gamification_service.domain.AwardResult;
import com.yeyamo_mobile.api.gamification_service.domain.Badge;
import com.yeyamo_mobile.api.gamification_service.domain.BadgeDefinition;
import com.yeyamo_mobile.api.gamification_service.domain.PassportStamp;
import com.yeyamo_mobile.api.gamification_service.domain.Progress;
import com.yeyamo_mobile.api.gamification_service.domain.Reward;
import com.yeyamo_mobile.api.gamification_service.domain.RewardStatus;
import com.yeyamo_mobile.api.gamification_service.domain.XpActivity;
import com.yeyamo_mobile.api.gamification_service.domain.XpHistoryEntry;

@Component
public class JpaGamificationAdapter implements GamificationRepositoryPort {
    private final ProfileRepository profiles;
    private final XpLedgerRepository ledger;
    private final CounterRepository counters;
    private final BadgeRepository badges;
    private final StampRepository stamps;
    private final RewardRepository rewards;

    public JpaGamificationAdapter(ProfileRepository profiles, XpLedgerRepository ledger,
            CounterRepository counters, BadgeRepository badges, StampRepository stamps,
            RewardRepository rewards) {
        this.profiles = profiles;
        this.ledger = ledger;
        this.counters = counters;
        this.badges = badges;
        this.stamps = stamps;
        this.rewards = rewards;
    }

    @Override
    public AwardResult award(XpActivity activity) {
        ProfileEntity profile = profiles.findLockedByUserId(activity.userId())
                .orElseGet(() -> newProfile(activity.userId()));
        Progress before = progress(profile);
        if (ledger.existsByEventId(activity.eventId())
                || ledger.existsByUserIdAndReasonAndSourceId(
                        activity.userId(), activity.reason(), activity.sourceId())) {
            return new AwardResult(false, before, before);
        }
        XpLedgerEntity entry = new XpLedgerEntity();
        entry.id = UUID.randomUUID();
        entry.eventId = activity.eventId();
        entry.userId = activity.userId();
        entry.points = activity.points();
        entry.reason = activity.reason();
        entry.sourceId = activity.sourceId();
        entry.occurredAt = activity.occurredAt();
        entry.createdAt = Instant.now();
        ledger.save(entry);

        profile.totalXp += activity.points();
        profile.level = Progress.levelFor(profile.totalXp);
        LocalDate date = activity.occurredAt().atZone(ZoneOffset.UTC).toLocalDate();
        if (profile.lastActivityDate == null) {
            profile.currentStreak = 1;
        } else if (date.equals(profile.lastActivityDate.plusDays(1))) {
            profile.currentStreak++;
        } else if (!date.equals(profile.lastActivityDate)) {
            profile.currentStreak = 1;
        }
        if (profile.lastActivityDate == null || date.isAfter(profile.lastActivityDate)) {
            profile.lastActivityDate = date;
        }
        profile.longestStreak = Math.max(profile.longestStreak, profile.currentStreak);
        profile.updatedAt = Instant.now();
        profiles.save(profile);
        return new AwardResult(true, before, progress(profile));
    }

    @Override
    public long incrementCounter(String user, String type) {
        CounterId id = new CounterId(user, type);
        CounterEntity counter = counters.findById(id).orElseGet(() -> {
            CounterEntity created = new CounterEntity();
            created.id = id;
            return created;
        });
        counter.value++;
        counter.updatedAt = Instant.now();
        return counters.save(counter).value;
    }

    @Override
    public Map<String, Long> counters(String user) {
        Map<String, Long> result = new HashMap<>();
        counters.findByIdUserId(user).forEach(counter -> result.put(counter.id.type, counter.value));
        return result;
    }

    @Override
    public boolean grantBadge(String user, BadgeDefinition definition, UUID eventId) {
        if (badges.existsByUserIdAndBadgeCode(user, definition.code())) {
            return false;
        }
        BadgeEntity entity = new BadgeEntity();
        entity.id = UUID.randomUUID();
        entity.userId = user;
        entity.badgeCode = definition.code();
        entity.name = definition.name();
        entity.description = definition.description();
        entity.earnedAt = Instant.now();
        entity.sourceEventId = eventId;
        badges.save(entity);
        return true;
    }

    @Override
    public boolean stamp(String user, String destination, UUID eventId) {
        if (stamps.existsByUserIdAndDestinationId(user, destination)) {
            return false;
        }
        StampEntity entity = new StampEntity();
        entity.id = UUID.randomUUID();
        entity.userId = user;
        entity.destinationId = destination;
        entity.stampedAt = Instant.now();
        entity.sourceEventId = eventId;
        stamps.save(entity);
        return true;
    }

    @Override
    public Optional<Reward> grantReward(String user, String code, String title, String source) {
        if (rewards.existsByUserIdAndRewardCodeAndSource(user, code, source)) {
            return Optional.empty();
        }
        RewardEntity entity = new RewardEntity();
        entity.id = UUID.randomUUID();
        entity.userId = user;
        entity.rewardCode = code;
        entity.title = title;
        entity.status = RewardStatus.AVAILABLE;
        entity.grantedAt = Instant.now();
        entity.source = source;
        return Optional.of(reward(rewards.save(entity)));
    }

    @Override
    public Reward claimReward(String user, UUID id) {
        RewardEntity entity = rewards.findById(id)
                .filter(reward -> reward.userId.equals(user))
                .orElseThrow(() -> new NoSuchElementException("Reward not found"));
        if (entity.status != RewardStatus.AVAILABLE) {
            throw new IllegalStateException("Reward is not available");
        }
        entity.status = RewardStatus.CLAIMED;
        entity.claimedAt = Instant.now();
        return reward(rewards.save(entity));
    }

    @Override
    public Progress progress(String user) {
        return profiles.findById(user).map(this::progress)
                .orElse(new Progress(user, 0, 1, 0, 0, null, Instant.now()));
    }

    @Override
    public List<Badge> badges(String user) {
        return badges.findByUserIdOrderByEarnedAtDesc(user).stream()
                .map(entity -> new Badge(entity.id, entity.userId, entity.badgeCode,
                        entity.name, entity.description, entity.earnedAt))
                .toList();
    }

    @Override
    public List<PassportStamp> passport(String user) {
        return stamps.findByUserIdOrderByStampedAtDesc(user).stream()
                .map(entity -> new PassportStamp(
                        entity.id, entity.userId, entity.destinationId, entity.stampedAt))
                .toList();
    }

    @Override
    public List<Reward> rewards(String user) {
        return rewards.findByUserIdOrderByGrantedAtDesc(user).stream().map(this::reward).toList();
    }

    @Override
    public Page<XpHistoryEntry> history(String user, Pageable pageable) {
        return ledger.findByUserId(user, pageable).map(entry -> new XpHistoryEntry(
                entry.getId(), entry.getPoints(), entry.getReason(), entry.getSourceId(), entry.getOccurredAt()));
    }

    @Override
    public List<Progress> leaderboard(int limit) {
        return profiles.findAllByOrderByTotalXpDescUserIdAsc(
                        PageRequest.of(0, Math.max(1, Math.min(100, limit))))
                .stream().map(this::progress).toList();
    }

    @Override
    public long rank(String user) {
        Progress current = progress(user);
        return profiles.countByTotalXpGreaterThan(current.totalXp()) + 1;
    }

    private ProfileEntity newProfile(String user) {
        ProfileEntity profile = new ProfileEntity();
        profile.userId = user;
        profile.level = 1;
        profile.updatedAt = Instant.now();
        return profile;
    }

    private Progress progress(ProfileEntity profile) {
        return new Progress(profile.userId, profile.totalXp, profile.level,
                profile.currentStreak, profile.longestStreak,
                profile.lastActivityDate, profile.updatedAt);
    }

    private Reward reward(RewardEntity entity) {
        return new Reward(entity.id, entity.userId, entity.rewardCode, entity.title,
                entity.status, entity.grantedAt, entity.claimedAt, entity.source);
    }
}
