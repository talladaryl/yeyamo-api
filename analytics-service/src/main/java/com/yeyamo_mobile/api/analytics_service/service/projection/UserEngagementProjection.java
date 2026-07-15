package com.yeyamo_mobile.api.analytics_service.service.projection;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.yeyamo_mobile.api.analytics_service.event.AnalyticsDomainEvent;
import com.yeyamo_mobile.api.analytics_service.models.ContentDimension;
import com.yeyamo_mobile.api.analytics_service.models.UserEngagement;
import com.yeyamo_mobile.api.analytics_service.repository.ContentDimensionRepository;
import com.yeyamo_mobile.api.analytics_service.repository.UserEngagementRepository;

@Component
public class UserEngagementProjection implements AnalyticsProjection {
    private final UserEngagementRepository engagements;
    private final ContentDimensionRepository contents;

    public UserEngagementProjection(UserEngagementRepository engagements, ContentDimensionRepository contents) {
        this.engagements = engagements;
        this.contents = contents;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public boolean supports(AnalyticsDomainEvent event) {
        String type = event.eventType();
        return type.equals("content.post.published") || type.equals("content.post.archived")
                || type.equals("content.post.deleted") || type.startsWith("interaction.")
                || type.equals("booking.completed");
    }

    @Override
    public void project(AnalyticsDomainEvent event) {
        String type = event.eventType();
        String actor = ProjectionSupport.text(event.payload(), "userId", "authorId");
        if (actor == null) {
            actor = event.actorId();
        }
        int posts = type.equals("content.post.published") ? 1
                : (type.equals("content.post.archived") || type.equals("content.post.deleted"))
                        && ProjectionSupport.text(event.payload(), "publishedAt") != null ? -1 : 0;
        int likes = ProjectionSupport.direction(type, "interaction.like.added", "interaction.like.removed");
        int comments = type.equals("interaction.comment.created") ? 1
                : type.equals("interaction.comment.deleted") ? -1 : 0;
        int shares = type.equals("interaction.post.shared") ? 1 : 0;
        int checkIns = type.equals("interaction.checkin.created") ? 1 : 0;
        int bookings = type.equals("booking.completed") ? 1 : 0;
        if (actor != null && posts + likes + comments + shares + checkIns + bookings != 0) {
            apply(event, actor, posts, likes, 0, comments, shares, checkIns, bookings);
        }

        if (likes != 0) {
            UUID postId = ProjectionSupport.uuid(event.payload(), "postId");
            ContentDimension content = postId == null ? null : contents.findById(postId).orElse(null);
            if (content != null && content.getAuthorId() != null && !content.getAuthorId().equals(actor)) {
                apply(event, content.getAuthorId(), 0, 0, likes, 0, 0, 0, 0);
            }
        }
    }

    private void apply(AnalyticsDomainEvent event, String userId, int posts, int likesGiven,
            int likesReceived, int comments, int shares, int checkIns, int bookings) {
        LocalDate date = ProjectionSupport.date(event);
        UUID id = ProjectionSupport.id("user-engagement", userId, date);
        UserEngagement projection = engagements.findById(id).orElseGet(UserEngagement::new);
        projection.setId(id);
        projection.setUserId(userId);
        projection.setEngagementDate(date);
        if (projection.apply(event.eventId(), posts, likesGiven, likesReceived, comments, shares, checkIns, bookings)) {
            engagements.save(projection);
        }
    }
}
