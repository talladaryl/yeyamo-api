package com.yeyamo_mobile.api.notification_service.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.notification_service.application.port.DeliveryRepositoryPort;
import com.yeyamo_mobile.api.notification_service.application.port.NotificationRepositoryPort;
import com.yeyamo_mobile.api.notification_service.application.port.PreferenceRepositoryPort;
import com.yeyamo_mobile.api.notification_service.application.port.TemplatePort;
import com.yeyamo_mobile.api.notification_service.domain.Notification;

class NotificationAlignmentTest {
    private NotificationRepositoryPort notifications;
    private NotificationApplicationService service;

    @BeforeEach
    void setUp() {
        notifications = mock(NotificationRepositoryPort.class);
        service = new NotificationApplicationService(notifications, mock(PreferenceRepositoryPort.class),
                mock(TemplatePort.class), mock(DeliveryRepositoryPort.class));
    }

    @Test
    void exposesUnreadNotificationsAndCount() {
        NotificationSlice slice = new NotificationSlice(0, 20, false, List.of());
        when(notifications.findUnreadByRecipient("42", 0, 20)).thenReturn(slice);
        when(notifications.countUnread("42")).thenReturn(3L);

        assertEquals(slice, service.unread("42", 0, 20));
        assertEquals(3L, service.unreadCount("42"));
    }

    @Test
    void deletesOnlyAnOwnedNotification() {
        UUID id = UUID.randomUUID();
        Notification notification = new Notification(id, UUID.randomUUID(), "message.sent", "42",
                "Title", "Body", "{}", Instant.now(), null);
        when(notifications.findById(id)).thenReturn(Optional.of(notification));

        service.delete("42", id);

        verify(notifications).delete(id);
    }

    @Test
    void hidesAnotherUsersNotificationDuringDelete() {
        UUID id = UUID.randomUUID();
        Notification notification = new Notification(id, UUID.randomUUID(), "message.sent", "99",
                "Title", "Body", "{}", Instant.now(), null);
        when(notifications.findById(id)).thenReturn(Optional.of(notification));

        assertThrows(NoSuchElementException.class, () -> service.delete("42", id));
    }
}
