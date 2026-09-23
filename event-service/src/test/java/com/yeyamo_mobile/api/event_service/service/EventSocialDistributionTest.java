package com.yeyamo_mobile.api.event_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.event_service.dto.EventRequest;
import com.yeyamo_mobile.api.event_service.dto.EventStatusRequest;
import com.yeyamo_mobile.api.event_service.dto.EventUpdateRequest;
import com.yeyamo_mobile.api.event_service.dto.SocialDistributionRequest;
import com.yeyamo_mobile.api.event_service.enums.EventStatus;
import com.yeyamo_mobile.api.event_service.enums.SocialDistributionStatus;
import com.yeyamo_mobile.api.event_service.event.EventPublisher;
import com.yeyamo_mobile.api.event_service.exception.ApiException;
import com.yeyamo_mobile.api.event_service.models.Event;
import com.yeyamo_mobile.api.event_service.repository.EventRegistrationRepository;
import com.yeyamo_mobile.api.event_service.repository.EventRepository;

class EventSocialDistributionTest {

    @Test
    void pendingCreationPersistsRequestedTargetsWithoutPublishingSocialContent() {
        EventRepository events = mock(EventRepository.class);
        EventPublisher publisher = mock(EventPublisher.class);
        when(events.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            return event;
        });
        EventService service = new EventService(events, mock(EventRegistrationRepository.class), publisher);

        EventRequest request = request();
        request.setSocialDistribution(new SocialDistributionRequest(true, true));

        var response = service.create(request, "corr-1", "organizer-1");

        assertEquals(EventStatus.PENDING, response.status());
        assertEquals(SocialDistributionStatus.PENDING_MODERATION, response.socialDistribution().feedStatus());
        assertEquals(SocialDistributionStatus.PENDING_MODERATION, response.socialDistribution().storyStatus());
        verify(publisher).publishCreated(any(Event.class), org.mockito.ArgumentMatchers.eq("corr-1"), org.mockito.ArgumentMatchers.eq("organizer-1"));
        verify(publisher, never()).publishPublished(any(), any(), any());
    }

    @Test
    void legacyCreateDefaultsToNoSocialDistribution() {
        EventRepository events = mock(EventRepository.class);
        when(events.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        EventService service = new EventService(events, mock(EventRegistrationRepository.class), mock(EventPublisher.class));

        var response = service.create(request(), "corr", "organizer-1");

        assertFalse(response.socialDistribution().publishToFeed());
        assertFalse(response.socialDistribution().publishToStory());
        assertEquals(SocialDistributionStatus.NOT_REQUESTED, response.socialDistribution().feedStatus());
        assertEquals(SocialDistributionStatus.NOT_REQUESTED, response.socialDistribution().storyStatus());
    }

    @Test
    void realPublishedTransitionEmitsExplicitPublishedEvent() {
        Event event = pendingEvent();
        EventRepository events = mock(EventRepository.class);
        EventPublisher publisher = mock(EventPublisher.class);
        when(events.findByIdForUpdate(event.getId())).thenReturn(Optional.of(event));
        when(events.save(event)).thenReturn(event);
        EventService service = new EventService(events, mock(EventRegistrationRepository.class), publisher);
        EventStatusRequest request = new EventStatusRequest();
        request.setStatus(EventStatus.PUBLISHED);

        service.updateStatus(event.getId(), request, "corr-published", "moderator", true);
        service.updateStatus(event.getId(), request, "corr-published", "moderator", true);

        assertEquals(EventStatus.PUBLISHED, event.getStatus());
        assertEquals(SocialDistributionStatus.PROCESSING, event.getFeedDistributionStatus());
        verify(publisher, times(1)).publishPublished(event, "corr-published", "moderator");
        verify(publisher, never()).publishUpdated(event, "corr-published", "moderator");
    }

    @Test
    void pendingOwnerCanChangeSocialIntentBeforeModeration() {
        Event event = pendingEvent();
        EventRepository events = mock(EventRepository.class);
        when(events.findById(event.getId())).thenReturn(Optional.of(event));
        when(events.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        EventService service = new EventService(events, mock(EventRegistrationRepository.class), mock(EventPublisher.class));
        EventUpdateRequest request = updateRequest();
        request.setSocialDistribution(new SocialDistributionRequest(false, true));

        var response = service.update(event.getId(), request, "corr", "organizer-1");

        assertFalse(response.socialDistribution().publishToFeed());
        assertEquals(SocialDistributionStatus.NOT_REQUESTED, response.socialDistribution().feedStatus());
        assertEquals(SocialDistributionStatus.PENDING_MODERATION, response.socialDistribution().storyStatus());
    }

    @Test
    void socialIntentIsLockedOnceEventIsPublished() {
        Event event = pendingEvent();
        event.setStatus(EventStatus.PUBLISHED);
        EventRepository events = mock(EventRepository.class);
        when(events.findById(event.getId())).thenReturn(Optional.of(event));
        EventService service = new EventService(events, mock(EventRegistrationRepository.class), mock(EventPublisher.class));
        EventUpdateRequest request = updateRequest();
        request.setSocialDistribution(new SocialDistributionRequest(false, true));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.update(event.getId(), request, "corr", "organizer-1"));

        assertEquals("EVENT_SOCIAL_DISTRIBUTION_LOCKED", exception.getCode());
    }

    private EventRequest request() {
        EventRequest request = new EventRequest();
        request.setVirtual(true);
        request.setTitle("Sortie sociale");
        request.setDescription("Description");
        request.setStartAt(Instant.now().plusSeconds(3600));
        request.setEndAt(Instant.now().plusSeconds(7200));
        request.setCapacity(10);
        return request;
    }

    private EventUpdateRequest updateRequest() {
        EventUpdateRequest request = new EventUpdateRequest();
        request.setTitle("Sortie sociale");
        request.setDescription("Description");
        request.setStartAt(Instant.now().plusSeconds(3600));
        request.setEndAt(Instant.now().plusSeconds(7200));
        request.setCapacity(10);
        return request;
    }

    private Event pendingEvent() {
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setOwnerUserId("organizer-1");
        event.setTitle("Sortie");
        event.setDescription("Description");
        event.setStartAt(Instant.now().plusSeconds(3600));
        event.setEndAt(Instant.now().plusSeconds(7200));
        event.setCapacity(10);
        event.setRegisteredCount(0);
        event.setStatus(EventStatus.PENDING);
        event.setPublishToFeed(true);
        event.setFeedDistributionStatus(SocialDistributionStatus.PENDING_MODERATION);
        return event;
    }
}
