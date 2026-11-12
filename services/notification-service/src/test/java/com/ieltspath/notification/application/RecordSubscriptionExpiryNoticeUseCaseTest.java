package com.ieltspath.notification.application;

import com.ieltspath.events.access.SubscriptionExpiryNoticeV1;
import com.ieltspath.notification.domain.repository.NotificationRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecordSubscriptionExpiryNoticeUseCaseTest {
    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final NotificationPreferenceService preferences = mock(NotificationPreferenceService.class);
    private final RecordSubscriptionExpiryNoticeUseCase useCase = new RecordSubscriptionExpiryNoticeUseCase(
            notifications, preferences, EnqueueNotificationPushUseCase.disabled());
    private final UUID userId = UUID.randomUUID();
    private final SubscriptionExpiryNoticeV1 event = new SubscriptionExpiryNoticeV1(UUID.randomUUID(),
            SubscriptionExpiryNoticeV1.EVENT_TYPE, Instant.parse("2026-11-01T00:00:00Z"), userId,
            UUID.randomUUID(), "EXPIRED", Instant.parse("2026-10-31T00:00:00Z"), "redacted@example.invalid", "Learner");

    @Test
    void storesExpiryNoticeWhenEnabled() {
        when(preferences.isEnabled(userId, event.eventType())).thenReturn(true);
        when(notifications.insertIfAbsent(any(), any())).thenReturn(true);
        assertTrue(useCase.execute(event, event.eventType(), event.eventId().toString()).isPresent());
        verify(notifications).insertIfAbsent(any(), any());
    }

    @Test
    void skipsNoticeWhenPreferenceIsDisabled() {
        when(preferences.isEnabled(userId, event.eventType())).thenReturn(false);
        assertTrue(useCase.execute(event, event.eventType(), event.eventId().toString()).isEmpty());
        verify(notifications, never()).insertIfAbsent(any(), any());
    }

    @Test
    void rejectsUnsupportedStage() {
        var invalid = new SubscriptionExpiryNoticeV1(event.eventId(), event.eventType(), event.occurredAt(), userId,
                event.subscriptionId(), "TWO_DAYS", event.endsAt(), event.email(), event.displayName());
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(invalid, invalid.eventType(), invalid.eventId().toString()));
    }
}
