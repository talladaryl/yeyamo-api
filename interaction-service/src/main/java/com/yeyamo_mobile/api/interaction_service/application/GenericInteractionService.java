package com.yeyamo_mobile.api.interaction_service.application;

import com.yeyamo_mobile.api.interaction_service.application.port.InteractionOutboxPort;
import com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.GenericInteractionEntity;
import com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.GenericInteractionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenericInteractionService {
    private final GenericInteractionRepository repo;
    private final InteractionOutboxPort outbox;

    public GenericInteractionService(GenericInteractionRepository repo, InteractionOutboxPort outbox) {
        this.repo = repo;
        this.outbox = outbox;
    }

    @Transactional
    public GenericInteractionEntity add(GenericInteractionEntity.TargetType target, String id, GenericInteractionEntity.Type type, String user, String body, String channel, String correlation) {
        if (type == GenericInteractionEntity.Type.LIKE || type == GenericInteractionEntity.Type.FAVORITE || type == GenericInteractionEntity.Type.FOLLOW) {
            var existing = repo.findByTargetTypeAndTargetIdAndUserIdAndInteractionType(target, id, user, type);
            if (existing.isPresent() && existing.get().status == GenericInteractionEntity.Status.ACTIVE) return existing.get();
        }
        if (type == GenericInteractionEntity.Type.COMMENT && (body == null || body.isBlank())) throw new IllegalArgumentException("Comment body required");
        if (type == GenericInteractionEntity.Type.REPORT && (body == null || body.isBlank())) throw new IllegalArgumentException("Report reason required");
        var interaction = new GenericInteractionEntity();
        interaction.id = UUID.randomUUID();
        interaction.targetType = target;
        interaction.targetId = id;
        interaction.userId = user;
        interaction.interactionType = type;
        interaction.body = body;
        interaction.channel = channel;
        interaction.status = type == GenericInteractionEntity.Type.REPORT ? GenericInteractionEntity.Status.PENDING_MODERATION : GenericInteractionEntity.Status.ACTIVE;
        repo.save(interaction);
        event(interaction, correlation);
        return interaction;
    }

    @Transactional
    public void remove(GenericInteractionEntity.TargetType target, String id, GenericInteractionEntity.Type type, String user, String correlation) {
        var interaction = repo.findByTargetTypeAndTargetIdAndUserIdAndInteractionType(target, id, user, type).orElseThrow();
        if (interaction.status == GenericInteractionEntity.Status.REMOVED) return;
        interaction.status = GenericInteractionEntity.Status.REMOVED;
        repo.save(interaction);
        event(interaction, correlation);
    }

    @Transactional(readOnly = true)
    public List<GenericInteractionEntity> comments(GenericInteractionEntity.TargetType target, String id) {
        return repo.findByTargetTypeAndTargetIdAndInteractionTypeAndStatusOrderByCreatedAtAsc(target, id, GenericInteractionEntity.Type.COMMENT, GenericInteractionEntity.Status.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Optional<GenericInteractionEntity> status(GenericInteractionEntity.TargetType target, String id, GenericInteractionEntity.Type type, String user) {
        return repo.findByTargetTypeAndTargetIdAndUserIdAndInteractionType(target, id, user, type)
                .filter(interaction -> interaction.status == GenericInteractionEntity.Status.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<GenericInteractionEntity> mine(GenericInteractionEntity.TargetType target, GenericInteractionEntity.Type type, String user, int limit) {
        return repo.findByTargetTypeAndUserIdAndInteractionTypeAndStatusOrderByCreatedAtDesc(target, user, type, GenericInteractionEntity.Status.ACTIVE)
                .stream().limit(Math.max(1, Math.min(limit, 100))).toList();
    }

    private void event(GenericInteractionEntity interaction, String correlation) {
        String prefix = switch (interaction.targetType) {
            case ARTWORK -> "Artwork";
            case CULTURE_CONTENT -> "CultureContent";
            case CHALLENGE_SUBMISSION, CULTURE_CHALLENGE -> "Challenge";
            case DAILY_WORD -> "DailyWord";
            case ARTISAN -> "Artisan";
            case PLACE -> "Place";
            case EVENT -> "Event";
            case EXPERIENCE -> "Experience";
            case POST -> "Post";
        };
        String action = switch (interaction.interactionType) {
            case LIKE -> interaction.status == GenericInteractionEntity.Status.REMOVED ? "LikeRemoved" : "Liked";
            case FAVORITE -> interaction.status == GenericInteractionEntity.Status.REMOVED ? "FavoriteRemoved" : "Favorited";
            case COMMENT -> "Commented";
            case SHARE -> "Shared";
            case REPORT -> "Reported";
            case VIEW -> "Viewed";
            case FOLLOW -> interaction.status == GenericInteractionEntity.Status.REMOVED ? "Unfollowed" : "Followed";
        };
        outbox.append(prefix + action, "interaction", interaction.id.toString(), interaction.userId, correlation,
                Map.of("targetType", interaction.targetType.name(), "targetId", interaction.targetId, "userId", interaction.userId, "interactionType", interaction.interactionType.name()));
    }
}
