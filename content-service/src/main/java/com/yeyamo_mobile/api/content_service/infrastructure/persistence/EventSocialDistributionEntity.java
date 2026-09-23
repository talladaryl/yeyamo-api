package com.yeyamo_mobile.api.content_service.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Content-service execution state for a published Event. The primary key and
 * unique source event ID protect the automatic Post/Story fan-out from Kafka
 * replays and concurrent consumers.
 */
@Entity
@Table(name = "content_event_social_distributions")
public class EventSocialDistributionEntity {
    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "source_event_id", nullable = false, unique = true)
    private UUID sourceEventId;

    @Column(name = "organizer_user_id", nullable = false, length = 100)
    private String organizerUserId;

    @Column(name = "feed_requested", nullable = false)
    private boolean feedRequested;

    @Column(name = "story_requested", nullable = false)
    private boolean storyRequested;

    @Column(name = "feed_status", nullable = false, length = 40)
    private String feedStatus;

    @Column(name = "story_status", nullable = false, length = 40)
    private String storyStatus;

    @Column(name = "post_id")
    private UUID postId;

    @Column(name = "story_id")
    private UUID storyId;

    @Column(name = "feed_error", length = 1000)
    private String feedError;

    @Column(name = "story_error", length = 1000)
    private String storyError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public UUID getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(UUID sourceEventId) { this.sourceEventId = sourceEventId; }
    public String getOrganizerUserId() { return organizerUserId; }
    public void setOrganizerUserId(String organizerUserId) { this.organizerUserId = organizerUserId; }
    public boolean isFeedRequested() { return feedRequested; }
    public void setFeedRequested(boolean feedRequested) { this.feedRequested = feedRequested; }
    public boolean isStoryRequested() { return storyRequested; }
    public void setStoryRequested(boolean storyRequested) { this.storyRequested = storyRequested; }
    public String getFeedStatus() { return feedStatus; }
    public void setFeedStatus(String feedStatus) { this.feedStatus = feedStatus; }
    public String getStoryStatus() { return storyStatus; }
    public void setStoryStatus(String storyStatus) { this.storyStatus = storyStatus; }
    public UUID getPostId() { return postId; }
    public void setPostId(UUID postId) { this.postId = postId; }
    public UUID getStoryId() { return storyId; }
    public void setStoryId(UUID storyId) { this.storyId = storyId; }
    public String getFeedError() { return feedError; }
    public void setFeedError(String feedError) { this.feedError = feedError; }
    public String getStoryError() { return storyError; }
    public void setStoryError(String storyError) { this.storyError = storyError; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
