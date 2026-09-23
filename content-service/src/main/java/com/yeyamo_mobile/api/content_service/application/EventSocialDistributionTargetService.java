package com.yeyamo_mobile.api.content_service.application;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.content_service.domain.model.Post;
import com.yeyamo_mobile.api.content_service.domain.model.PostReferenceType;
import com.yeyamo_mobile.api.content_service.domain.model.PostVisibility;
import com.yeyamo_mobile.api.content_service.infrastructure.client.EventSocialMediaValidator;
import com.yeyamo_mobile.api.content_service.infrastructure.client.EventSocialMediaValidator.MediaEligibility;
import com.yeyamo_mobile.api.content_service.infrastructure.outbox.ContentOutboxPort;
import com.yeyamo_mobile.api.content_service.infrastructure.persistence.EventSocialDistributionEntity;
import com.yeyamo_mobile.api.content_service.infrastructure.persistence.EventSocialDistributionRepository;
import com.yeyamo_mobile.shared.geography.GeographicFields;

/** Executes each social target in its own transaction so one target can retry independently. */
@Service
public class EventSocialDistributionTargetService {
    private static final Logger log = LoggerFactory.getLogger(EventSocialDistributionTargetService.class);
    static final String NOT_REQUESTED = "NOT_REQUESTED";
    static final String PENDING = "PENDING_MODERATION";
    static final String PROCESSING = "PROCESSING";
    static final String PUBLISHED = "PUBLISHED";
    static final String SKIPPED_NO_MEDIA = "SKIPPED_NO_MEDIA";
    static final String SKIPPED_PRIVATE_EVENT = "SKIPPED_PRIVATE_EVENT";
    static final String FAILED = "FAILED";

    private final EventSocialDistributionRepository distributions;
    private final PostApplicationService posts;
    private final StoryService stories;
    private final EventSocialMediaValidator media;
    private final ContentOutboxPort outbox;

    public EventSocialDistributionTargetService(
            EventSocialDistributionRepository distributions,
            PostApplicationService posts,
            StoryService stories,
            EventSocialMediaValidator media,
            ContentOutboxPort outbox) {
        this.distributions = distributions;
        this.posts = posts;
        this.stories = stories;
        this.media = media;
        this.outbox = outbox;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processFeed(EventPublishedSocialCommand command) {
        EventSocialDistributionEntity distribution = distributionForUpdate(command);
        if (!distribution.isFeedRequested() || terminal(distribution.getFeedStatus())) {
            return;
        }
        if (!command.publicEvent()) {
            distribution.setFeedStatus(SKIPPED_PRIVATE_EVENT);
            distribution.setFeedError("EVENT_NOT_PUBLIC");
            touch(distribution);
            distributions.save(distribution);
            publishResult(command, "FEED", SKIPPED_PRIVATE_EVENT, null, "EVENT_NOT_PUBLIC");
            return;
        }

        distribution.setFeedStatus(PROCESSING);
        MediaEligibility cover = media.inspect(command.coverMediaId(), command.organizerUserId());
        Post draft = posts.createDraft(
                command.organizerUserId(),
                postCommand(command, cover),
                command.correlationId());
        Post published = posts.publish(draft.getId(), command.organizerUserId(), false, command.correlationId());

        distribution.setFeedStatus(PUBLISHED);
        distribution.setPostId(published.getId());
        distribution.setFeedError(null);
        touch(distribution);
        distributions.save(distribution);
        publishResult(command, "FEED", PUBLISHED, published.getId(), null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processStory(EventPublishedSocialCommand command) {
        EventSocialDistributionEntity distribution = distributionForUpdate(command);
        if (!distribution.isStoryRequested() || terminal(distribution.getStoryStatus())) {
            return;
        }
        if (!command.publicEvent()) {
            distribution.setStoryStatus(SKIPPED_PRIVATE_EVENT);
            distribution.setStoryError("EVENT_NOT_PUBLIC");
            touch(distribution);
            distributions.save(distribution);
            publishResult(command, "STORY", SKIPPED_PRIVATE_EVENT, null, "EVENT_NOT_PUBLIC");
            return;
        }

        distribution.setStoryStatus(PROCESSING);
        MediaEligibility cover = media.inspect(command.coverMediaId(), command.organizerUserId());
        if (!cover.usableForSocialContent()) {
            distribution.setStoryStatus(SKIPPED_NO_MEDIA);
            distribution.setStoryError("NO_ELIGIBLE_COVER_MEDIA");
            touch(distribution);
            distributions.save(distribution);
            publishResult(command, "STORY", SKIPPED_NO_MEDIA, null, "NO_ELIGIBLE_COVER_MEDIA");
            return;
        }

        var story = stories.create(
                command.organizerUserId(),
                cover.mediaId(),
                command.socialCaption(),
                15,
                geography(command),
                PostReferenceType.EVENT,
                command.eventId().toString(),
                command.correlationId());

        distribution.setStoryStatus(PUBLISHED);
        distribution.setStoryId(story.getId());
        distribution.setStoryError(null);
        touch(distribution);
        distributions.save(distribution);
        publishResult(command, "STORY", PUBLISHED, story.getId(), null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(EventPublishedSocialCommand command, String target, RuntimeException failure) {
        EventSocialDistributionEntity distribution = distributionForUpdate(command);
        String reason = reason(failure);
        if ("FEED".equals(target) && !terminal(distribution.getFeedStatus())) {
            distribution.setFeedStatus(FAILED);
            distribution.setFeedError(reason);
            touch(distribution);
            distributions.save(distribution);
            publishResult(command, target, FAILED, null, reason);
        }
        if ("STORY".equals(target) && !terminal(distribution.getStoryStatus())) {
            distribution.setStoryStatus(FAILED);
            distribution.setStoryError(reason);
            touch(distribution);
            distributions.save(distribution);
            publishResult(command, target, FAILED, null, reason);
        }
    }

    private EventSocialDistributionEntity distributionForUpdate(EventPublishedSocialCommand command) {
        return distributions.findByEventIdForUpdate(command.eventId()).orElseGet(() -> createDistribution(command));
    }

    private EventSocialDistributionEntity createDistribution(EventPublishedSocialCommand command) {
        EventSocialDistributionEntity distribution = new EventSocialDistributionEntity();
        distribution.setEventId(command.eventId());
        distribution.setSourceEventId(command.sourceEventId());
        distribution.setOrganizerUserId(command.organizerUserId());
        distribution.setFeedRequested(command.publishToFeed());
        distribution.setStoryRequested(command.publishToStory());
        distribution.setFeedStatus(command.publishToFeed() ? PENDING : NOT_REQUESTED);
        distribution.setStoryStatus(command.publishToStory() ? PENDING : NOT_REQUESTED);
        Instant now = Instant.now();
        distribution.setCreatedAt(now);
        distribution.setUpdatedAt(now);
        try {
            return distributions.saveAndFlush(distribution);
        } catch (DataIntegrityViolationException duplicate) {
            // A concurrent delivery owns the row. Rolling this transaction back
            // is safe; Kafka will retry and see its final target state.
            throw duplicate;
        }
    }

    private PostCommand postCommand(EventPublishedSocialCommand command, MediaEligibility cover) {
        return new PostCommand(
                command.socialCaption(),
                PostVisibility.PUBLIC,
                null,
                cover.usableForSocialContent() ? List.of(cover.mediaId()) : List.of(),
                Set.of(),
                PostReferenceType.EVENT,
                command.eventId().toString(),
                command.countryCode(),
                null,
                null,
                null,
                null,
                null,
                null,
                command.languageCode(),
                null,
                null);
    }

    private GeographicFields geography(EventPublishedSocialCommand command) {
        if (command.countryCode() == null || command.countryCode().isBlank()) {
            return null;
        }
        GeographicFields geography = new GeographicFields(command.countryCode());
        geography.setLanguageCode(command.languageCode());
        return geography;
    }

    private boolean terminal(String status) {
        return PUBLISHED.equals(status) || SKIPPED_NO_MEDIA.equals(status) || SKIPPED_PRIVATE_EVENT.equals(status);
    }

    private void touch(EventSocialDistributionEntity distribution) {
        distribution.setUpdatedAt(Instant.now());
    }

    private void publishResult(EventPublishedSocialCommand command, String target, String status, UUID contentId, String reason) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("eventId", command.eventId().toString());
        payload.put("sourceEventId", command.sourceEventId().toString());
        payload.put("target", target);
        payload.put("status", status);
        if (contentId != null) payload.put("contentId", contentId.toString());
        if (reason != null) payload.put("reason", reason);
        outbox.append(
                "content.event_social.distribution.updated",
                command.eventId().toString(),
                command.organizerUserId(),
                command.correlationId(),
                payload);
        log.debug("Event social distribution result: eventId={}, sourceEventId={}, target={}, status={}, contentId={}",
                command.eventId(), command.sourceEventId(), target, status, contentId);
    }

    private String reason(RuntimeException failure) {
        if (failure instanceof ContentException contentFailure) {
            return contentFailure.getCode();
        }
        return "CONTENT_DISTRIBUTION_FAILED";
    }
}
