package com.yeyamo_mobile.api.user_service.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Représente une activité sociale récente (qui a suivi qui)
 * Utilisé pour le flux d'activité du réseau
 */
public record SocialActivity(
    UUID actorId,
    String actorDisplayName,
    String actorAvatarUrl,
    UUID targetId,
    String targetDisplayName,
    String targetAvatarUrl,
    String actionType, // "FOLLOWED"
    Instant occurredAt
) {
    public static SocialActivity followed(UserProfile actor, UserProfile target, Instant when) {
        return new SocialActivity(
            actor.getId(),
            actor.getDisplayName(),
            actor.getAvatarUrl(),
            target.getId(),
            target.getDisplayName(),
            target.getAvatarUrl(),
            "FOLLOWED",
            when
        );
    }
}
