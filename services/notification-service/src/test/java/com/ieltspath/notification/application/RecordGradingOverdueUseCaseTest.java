package com.ieltspath.notification.application;

import com.ieltspath.events.assessment.GradingOverdueV1;
import com.ieltspath.notification.domain.repository.NotificationRepository;
import com.ieltspath.notification.domain.vo.Notification;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecordGradingOverdueUseCaseTest {
    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final RecordGradingOverdueUseCase useCase = new RecordGradingOverdueUseCase(notifications);
    private final GradingOverdueV1 event = new GradingOverdueV1(UUID.randomUUID(), GradingOverdueV1.EVENT_TYPE,
            Instant.parse("2026-10-23T09:10:00Z"), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "SPEAKING",
            Instant.parse("2026-10-23T09:00:00Z"));

    @Test
    void theLearnerGetsANoticeThatPointsAtTheJobAndNamesTheExaminer() {
        when(notifications.insertIfAbsent(any(), any())).thenReturn(true);

        var stored = useCase.execute(event, GradingOverdueV1.EVENT_TYPE, event.eventId().toString()).orElseThrow();

        var saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).insertIfAbsent(org.mockito.ArgumentMatchers.eq(event.eventId()), saved.capture());
        assertEquals(stored, saved.getValue());
        assertEquals(event.learnerId(), stored.recipientId());
        assertEquals(event.examinerId(), stored.actorId());
        assertEquals(event.gradingJobId(), stored.gradingJobId());
        assertEquals(GradingOverdueV1.EVENT_TYPE, stored.eventType());
        assertNull(stored.postId());
        assertNull(stored.commentId());
        assertNull(stored.readAt());
    }

    @Test
    void aRepeatedEventStoresNothingNew() {
        when(notifications.insertIfAbsent(any(), any())).thenReturn(false);

        assertTrue(useCase.execute(event, GradingOverdueV1.EVENT_TYPE, event.eventId().toString()).isEmpty());
    }

    @Test
    void anEventThatBreaksTheContractIsRefusedAndNothingIsStored() {
        String id = event.eventId().toString();
        String type = GradingOverdueV1.EVENT_TYPE;
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, type, id));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(event, "assessment.completed.v2", id));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(event, type, UUID.randomUUID().toString()));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                new GradingOverdueV1(event.eventId(), "other", event.occurredAt(), event.learnerId(), event.examinerId(),
                        event.gradingJobId(), "WRITING", event.deadline()), type, id));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                new GradingOverdueV1(event.eventId(), type, event.occurredAt(), null, event.examinerId(),
                        event.gradingJobId(), "WRITING", event.deadline()), type, id));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                new GradingOverdueV1(event.eventId(), type, event.occurredAt(), event.learnerId(), event.examinerId(),
                        null, "WRITING", event.deadline()), type, id));
        verifyNoInteractions(notifications);
    }
}
