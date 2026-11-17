package com.ieltspath.notification.application;

import com.ieltspath.events.library.VocabularyReviewDueV1;
import com.ieltspath.notification.domain.repository.NotificationRepository;
import com.ieltspath.notification.domain.vo.Notification;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecordVocabularyReviewDueUseCaseTest {
    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final NotificationPreferenceService preferences = mock(NotificationPreferenceService.class);
    private final EnqueueNotificationPushUseCase push = mock(EnqueueNotificationPushUseCase.class);
    private final RecordVocabularyReviewDueUseCase useCase =
            new RecordVocabularyReviewDueUseCase(notifications, preferences, push);
    private final VocabularyReviewDueV1 event = new VocabularyReviewDueV1(UUID.randomUUID(),
            VocabularyReviewDueV1.EVENT_TYPE, Instant.parse("2026-11-16T01:00:00Z"), UUID.randomUUID(),
            LocalDate.of(2026, 11, 16), 9);

    @Test
    void theLearnerGetsANoticeAndItIsQueuedForPush() {
        when(preferences.isEnabled(event.userId(), NotificationEventTypes.VOCABULARY_REVIEW_DUE)).thenReturn(true);
        when(notifications.insertIfAbsent(any(), any())).thenReturn(true);

        var stored = useCase.execute(event, VocabularyReviewDueV1.EVENT_TYPE, event.eventId().toString()).orElseThrow();

        var saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).insertIfAbsent(eq(event.eventId()), saved.capture());
        assertEquals(stored, saved.getValue());
        assertEquals(event.userId(), stored.recipientId());
        assertEquals(NotificationEventTypes.VOCABULARY_REVIEW_DUE, stored.eventType());
        assertEquals(event.occurredAt(), stored.createdAt());
        assertNull(stored.postId());
        assertNull(stored.readAt());
        verify(push).execute(stored);
    }

    @Test
    void aLearnerWhoSwitchedTheNoticeOffGetsNothing() {
        when(preferences.isEnabled(event.userId(), NotificationEventTypes.VOCABULARY_REVIEW_DUE)).thenReturn(false);

        assertTrue(useCase.execute(event, VocabularyReviewDueV1.EVENT_TYPE, event.eventId().toString()).isEmpty());

        verifyNoInteractions(notifications, push);
    }

    @Test
    void aRepeatedEventStoresAndPushesNothingNew() {
        when(preferences.isEnabled(any(), any())).thenReturn(true);
        when(notifications.insertIfAbsent(any(), any())).thenReturn(false);

        assertTrue(useCase.execute(event, VocabularyReviewDueV1.EVENT_TYPE, event.eventId().toString()).isEmpty());

        verifyNoInteractions(push);
    }

    @Test
    void anEventThatBreaksTheContractIsRefusedAndNothingIsStored() {
        String id = event.eventId().toString();
        String type = VocabularyReviewDueV1.EVENT_TYPE;
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, type, id));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(event, "library.other.v1", id));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(event, type, UUID.randomUUID().toString()));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new VocabularyReviewDueV1(event.eventId(),
                "other", event.occurredAt(), event.userId(), event.reviewDate(), 9), type, id));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new VocabularyReviewDueV1(event.eventId(),
                type, event.occurredAt(), null, event.reviewDate(), 9), type, id));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new VocabularyReviewDueV1(event.eventId(),
                type, event.occurredAt(), event.userId(), null, 9), type, id));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new VocabularyReviewDueV1(event.eventId(),
                type, event.occurredAt(), event.userId(), event.reviewDate(), 0), type, id));
        verifyNoInteractions(notifications, preferences, push);
    }
}
