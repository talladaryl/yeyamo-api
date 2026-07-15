package com.yeyamo_mobile.api.analytics_service.service.projection;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.yeyamo_mobile.api.analytics_service.event.AnalyticsDomainEvent;
import com.yeyamo_mobile.api.analytics_service.models.ContentDimension;
import com.yeyamo_mobile.api.analytics_service.models.PlacePopularity;
import com.yeyamo_mobile.api.analytics_service.repository.ContentDimensionRepository;
import com.yeyamo_mobile.api.analytics_service.repository.PlacePopularityRepository;

@Component
public class PlacePopularityProjection implements AnalyticsProjection {
    private final PlacePopularityRepository popularity;
    private final ContentDimensionRepository contents;

    public PlacePopularityProjection(PlacePopularityRepository popularity, ContentDimensionRepository contents) {
        this.popularity = popularity;
        this.contents = contents;
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public boolean supports(AnalyticsDomainEvent event) {
        return event.eventType().startsWith("content.post.")
                || event.eventType().startsWith("interaction.")
                || event.eventType().startsWith("booking.");
    }

    @Override
    public void project(AnalyticsDomainEvent event) {
        UUID placeId = resolvePlace(event);
        if (placeId == null) {
            return;
        }
        String type = event.eventType();
        int views = type.equals("interaction.post.viewed") ? 1 : 0;
        int likes = ProjectionSupport.direction(type, "interaction.like.added", "interaction.like.removed");
        int comments = type.equals("interaction.comment.created") ? 1
                : type.equals("interaction.comment.deleted") ? -1 : 0;
        int shares = type.equals("interaction.post.shared") ? 1 : 0;
        int checkIns = type.equals("interaction.checkin.created") ? 1 : 0;
        int bookings = type.equals("booking.confirmed") ? 1
                : type.equals("booking.cancelled") ? -1 : 0;
        int posts = type.equals("content.post.published") ? 1
                : (type.equals("content.post.archived") || type.equals("content.post.deleted"))
                        && ProjectionSupport.text(event.payload(), "publishedAt") != null ? -1 : 0;
        if (views + likes + comments + shares + checkIns + bookings + posts == 0) {
            return;
        }
        LocalDate date = ProjectionSupport.date(event);
        UUID id = ProjectionSupport.id("place-popularity", placeId, date);
        PlacePopularity projection = popularity.findById(id).orElseGet(PlacePopularity::new);
        projection.setId(id);
        projection.setPlaceId(placeId);
        projection.setStatDate(date);
        if (projection.apply(event.eventId(), views, likes, comments, shares, checkIns,
                bookings, posts, event.occurredAt())) {
            popularity.save(projection);
        }
    }

    private UUID resolvePlace(AnalyticsDomainEvent event) {
        UUID direct = ProjectionSupport.uuid(event.payload(), "catalogAssetId", "placeId", "activityId", "assetId");
        if (direct != null) {
            return direct;
        }
        UUID postId = ProjectionSupport.uuid(event.payload(), "postId");
        ContentDimension content = postId == null ? null : contents.findById(postId).orElse(null);
        return content == null ? null : content.getCatalogAssetId();
    }
}
