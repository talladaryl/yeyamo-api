package com.yeyamo_mobile.api.content_service.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.yeyamo_mobile.api.content_service.domain.model.Post;
import com.yeyamo_mobile.api.content_service.domain.model.PostReferenceType;
import com.yeyamo_mobile.api.content_service.domain.port.PostRepository;
import com.yeyamo_mobile.api.content_service.application.port.ReferenceVisibilityPort;
import com.yeyamo_mobile.api.content_service.infrastructure.client.EventSocialMediaValidator;
import com.yeyamo_mobile.api.content_service.infrastructure.client.EventSocialMediaValidator.MediaEligibility;
import com.yeyamo_mobile.api.content_service.infrastructure.outbox.ContentOutboxPort;
import com.yeyamo_mobile.api.content_service.infrastructure.persistence.EventSocialDistributionEntity;
import com.yeyamo_mobile.api.content_service.infrastructure.persistence.EventSocialDistributionRepository;
import com.yeyamo_mobile.api.content_service.infrastructure.persistence.StoryEntity;

class EventSocialDistributionTargetServiceTests {

    @Test
    void feedOnlyCreatesOnePublishedEventPostThroughPostApplicationService() {
        Harness harness = new Harness();
        when(harness.media.inspect(any(), anyString())).thenReturn(MediaEligibility.usable(harness.command.coverMediaId()));
        Post draft = post();
        Post published = post();
        when(harness.posts.createDraft(anyString(), any(PostCommand.class), anyString())).thenReturn(draft);
        when(harness.posts.publish(eq(draft.getId()), eq("organizer-1"), eq(false), anyString())).thenReturn(published);

        harness.targets.processFeed(harness.command(false));

        ArgumentCaptor<PostCommand> command = ArgumentCaptor.forClass(PostCommand.class);
        verify(harness.posts).createDraft(eq("organizer-1"), command.capture(), eq("corr-1"));
        verify(harness.posts).publish(draft.getId(), "organizer-1", false, "corr-1");
        verify(harness.stories, never()).create(anyString(), any(), anyString(), anyInt(), any(), any(), anyString(), anyString());
        assertEquals(PostReferenceType.EVENT, command.getValue().referenceType());
        assertEquals(harness.command.eventId().toString(), command.getValue().referenceId());
        assertEquals("PUBLIC", command.getValue().visibility().name());
        assertEquals("organizer-1", harness.state.get().getOrganizerUserId());
        assertEquals("PUBLISHED", harness.state.get().getFeedStatus());
    }

    @Test
    void storyOnlyCreatesReferencedExpiringStoryWithEligibleCover() {
        Harness harness = new Harness();
        when(harness.media.inspect(any(), anyString())).thenReturn(MediaEligibility.usable(harness.command.coverMediaId()));
        StoryEntity story = new StoryEntity();
        story.setId(UUID.randomUUID());
        when(harness.stories.create(eq("organizer-1"), eq(harness.command.coverMediaId()), anyString(), eq(15), any(),
                eq(PostReferenceType.EVENT), eq(harness.command.eventId().toString()), eq("corr-1"))).thenReturn(story);

        harness.targets.processStory(harness.command(true));

        verify(harness.posts, never()).createDraft(anyString(), any(), anyString());
        verify(harness.stories).create(eq("organizer-1"), eq(harness.command.coverMediaId()), anyString(), eq(15), any(),
                eq(PostReferenceType.EVENT), eq(harness.command.eventId().toString()), eq("corr-1"));
        assertEquals("PUBLISHED", harness.state.get().getStoryStatus());
        assertEquals(story.getId(), harness.state.get().getStoryId());
    }

    @Test
    void storyWithoutEligibleMediaIsSkippedWithoutCreatingAPlaceholder() {
        Harness harness = new Harness();
        when(harness.media.inspect(any(), anyString())).thenReturn(MediaEligibility.unavailableForStory());

        harness.targets.processStory(harness.command(true));

        verify(harness.stories, never()).create(anyString(), any(), anyString(), anyInt(), any(), any(), anyString(), anyString());
        assertEquals("SKIPPED_NO_MEDIA", harness.state.get().getStoryStatus());
        ArgumentCaptor<java.util.Map<String, String>> payload = ArgumentCaptor.forClass(java.util.Map.class);
        verify(harness.outbox).append(eq("content.event_social.distribution.updated"), eq(harness.command.eventId().toString()),
                eq("organizer-1"), eq("corr-1"), payload.capture());
        assertEquals("SKIPPED_NO_MEDIA", payload.getValue().get("status"));
    }

    @Test
    void replayDoesNotCreateAnotherAutomaticPost() {
        Harness harness = new Harness();
        when(harness.media.inspect(any(), anyString())).thenReturn(MediaEligibility.unavailableForStory());
        Post draft = post();
        Post published = post();
        when(harness.posts.createDraft(anyString(), any(PostCommand.class), anyString())).thenReturn(draft);
        when(harness.posts.publish(any(), anyString(), eq(false), anyString())).thenReturn(published);

        harness.targets.processFeed(harness.command(false));
        harness.targets.processFeed(harness.command(false));

        verify(harness.posts, times(1)).createDraft(anyString(), any(PostCommand.class), anyString());
        verify(harness.posts, times(1)).publish(any(), anyString(), eq(false), anyString());
    }

    @Test
    void bothRequestedTargetsCreateOnePostAndOneStory() {
        Harness harness = new Harness();
        when(harness.media.inspect(any(), anyString())).thenReturn(MediaEligibility.usable(harness.command.coverMediaId()));
        Post draft = post();
        Post published = post();
        StoryEntity story = new StoryEntity();
        story.setId(UUID.randomUUID());
        when(harness.posts.createDraft(anyString(), any(PostCommand.class), anyString())).thenReturn(draft);
        when(harness.posts.publish(any(), anyString(), eq(false), anyString())).thenReturn(published);
        when(harness.stories.create(anyString(), any(), anyString(), anyInt(), any(), any(), anyString(), anyString()))
                .thenReturn(story);

        harness.targets.processFeed(harness.command);
        harness.targets.processStory(harness.command);

        assertEquals("PUBLISHED", harness.state.get().getFeedStatus());
        assertEquals("PUBLISHED", harness.state.get().getStoryStatus());
        verify(harness.posts, times(1)).createDraft(anyString(), any(PostCommand.class), anyString());
        verify(harness.stories, times(1)).create(anyString(), any(), anyString(), anyInt(), any(), any(), anyString(), anyString());
    }

    @Test
    void privateEventIsSkippedWithoutCreatingPublicPost() {
        Harness harness = new Harness();

        harness.targets.processFeed(harness.privateCommand(true, false));

        verify(harness.posts, never()).createDraft(anyString(), any(PostCommand.class), anyString());
        assertEquals("SKIPPED_PRIVATE_EVENT", harness.state.get().getFeedStatus());
        assertEquals("EVENT_NOT_PUBLIC", harness.state.get().getFeedError());
    }

    @Test
    void automaticEventPostUsesTheNormalContentPublishedOutboxPipeline() {
        ContentOutboxPort normalPostOutbox = mock(ContentOutboxPort.class);
        PostRepository postRepository = mock(PostRepository.class);
        AtomicReference<Post> storedPost = new AtomicReference<>();
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            storedPost.set(saved);
            return saved;
        });
        when(postRepository.findById(any())).thenAnswer(invocation -> Optional.ofNullable(storedPost.get()));
        PostApplicationService normalPosts = new PostApplicationService(
                postRepository,
                normalPostOutbox,
                mock(ReferenceVisibilityPort.class));
        Harness harness = new Harness(normalPosts);
        when(harness.media.inspect(any(), anyString())).thenReturn(MediaEligibility.usable(harness.command.coverMediaId()));

        harness.targets.processFeed(harness.command(false));

        ArgumentCaptor<Post> published = ArgumentCaptor.forClass(Post.class);
        verify(normalPostOutbox).append(eq("content.post.published"), published.capture(), eq("corr-1"), eq("organizer-1"));
        assertEquals(PostReferenceType.EVENT, published.getValue().getReferenceType());
        assertEquals(harness.command.eventId().toString(), published.getValue().getReferenceId());
        assertEquals("PUBLISHED", published.getValue().getStatus().name());
    }

    @Test
    void partialFailureIsRecordedAndRetryCanContinueWithoutRollingBackFeed() {
        EventSocialDistributionTargetService targets = mock(EventSocialDistributionTargetService.class);
        com.yeyamo_mobile.api.content_service.infrastructure.persistence.ContentProcessedEventReceiptRepository receipts = mock(com.yeyamo_mobile.api.content_service.infrastructure.persistence.ContentProcessedEventReceiptRepository.class);
        EventSocialDistributionService service = new EventSocialDistributionService(targets, receipts);
        EventPublishedSocialCommand command = new Harness().commandWithTargets(true, true);
        org.mockito.Mockito.doThrow(new ContentException("STORY_FAILURE", "temporary story failure"))
                .doNothing()
                .when(targets).processStory(command);

        assertThrows(ContentException.class, () -> service.distribute(command));
        verify(targets).processFeed(command);
        verify(targets).markFailed(eq(command), eq("STORY"), any(ContentException.class));

        service.distribute(command);
        verify(targets, times(2)).processFeed(command);
        verify(targets, times(2)).processStory(command);
        verify(receipts).save(any());
    }

    private static Post post() {
        Post post = new Post();
        post.setId(UUID.randomUUID());
        return post;
    }

    private static final class Harness {
        private final EventSocialDistributionRepository distributions = mock(EventSocialDistributionRepository.class);
        private final PostApplicationService posts;
        private final StoryService stories = mock(StoryService.class);
        private final EventSocialMediaValidator media = mock(EventSocialMediaValidator.class);
        private final ContentOutboxPort outbox = mock(ContentOutboxPort.class);
        private final AtomicReference<EventSocialDistributionEntity> state = new AtomicReference<>();
        private final UUID eventId = UUID.randomUUID();
        private final EventPublishedSocialCommand command = new EventPublishedSocialCommand(
                UUID.randomUUID(), eventId, "organizer-1", "Sortie", "Description", Instant.now(), Instant.now().plusSeconds(3600),
                true, UUID.randomUUID(), true, true, null, null, "corr-1");
        private final EventSocialDistributionTargetService targets;

        private Harness() {
            this(mock(PostApplicationService.class));
        }

        private Harness(PostApplicationService posts) {
            this.posts = posts;
            when(distributions.findByEventIdForUpdate(eventId)).thenAnswer(invocation -> Optional.ofNullable(state.get()));
            when(distributions.saveAndFlush(any(EventSocialDistributionEntity.class))).thenAnswer(invocation -> {
                EventSocialDistributionEntity saved = invocation.getArgument(0);
                state.set(saved);
                return saved;
            });
            when(distributions.save(any(EventSocialDistributionEntity.class))).thenAnswer(invocation -> {
                EventSocialDistributionEntity saved = invocation.getArgument(0);
                state.set(saved);
                return saved;
            });
            targets = new EventSocialDistributionTargetService(distributions, posts, stories, media, outbox);
        }

        private EventPublishedSocialCommand command(boolean storyOnly) {
            return commandWithTargets(!storyOnly, storyOnly);
        }

        private EventPublishedSocialCommand commandWithTargets(boolean publishToFeed, boolean publishToStory) {
            return new EventPublishedSocialCommand(
                    command.sourceEventId(), command.eventId(), command.organizerUserId(), command.title(), command.description(),
                    command.startAt(), command.endAt(), command.publicEvent(), command.coverMediaId(), publishToFeed, publishToStory,
                    command.countryCode(), command.languageCode(), command.correlationId());
        }

        private EventPublishedSocialCommand privateCommand(boolean publishToFeed, boolean publishToStory) {
            return new EventPublishedSocialCommand(
                    command.sourceEventId(), command.eventId(), command.organizerUserId(), command.title(), command.description(),
                    command.startAt(), command.endAt(), false, command.coverMediaId(), publishToFeed, publishToStory,
                    command.countryCode(), command.languageCode(), command.correlationId());
        }
    }
}
