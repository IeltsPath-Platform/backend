package com.ieltspath.notification.application;

import com.ieltspath.events.assessment.GradingRequestedV1;
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

class RecordGradingRequestedUseCaseTest {
    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final RecordGradingRequestedUseCase useCase = new RecordGradingRequestedUseCase(notifications);
    private final GradingRequestedV1 event = new GradingRequestedV1(UUID.randomUUID(), GradingRequestedV1.EVENT_TYPE,
            Instant.parse("2026-10-23T21:10:00Z"), UUID.randomUUID(), UUID.randomUUID(), "WRITING",
            Instant.parse("2026-10-24T09:00:00Z"));

    @Test
    void theExaminerGetsANoticeThatPointsAtTheJob() {
        when(notifications.insertIfAbsent(any(), any())).thenReturn(true);

        var stored = useCase.execute(event, GradingRequestedV1.EVENT_TYPE, event.eventId().toString()).orElseThrow();

        var saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).insertIfAbsent(org.mockito.ArgumentMatchers.eq(event.eventId()), saved.capture());
        assertEquals(stored, saved.getValue());
        assertEquals(event.examinerId(), stored.recipientId());
        assertEquals(event.examinerId(), stored.actorId(), "there is no learner to name: grading is blind");
        assertEquals(event.gradingJobId(), stored.gradingJobId());
        assertEquals(GradingRequestedV1.EVENT_TYPE, stored.eventType());
        assertNull(stored.postId());
        assertNull(stored.commentId());
        assertNull(stored.readAt());
    }

    @Test
    void aRepeatedEventStoresNothingNew() {
        when(notifications.insertIfAbsent(any(), any())).thenReturn(false);

        assertTrue(useCase.execute(event, GradingRequestedV1.EVENT_TYPE, event.eventId().toString()).isEmpty());
    }

    @Test
    void anEventThatBreaksTheContractIsRefusedAndNothingIsStored() {
        String id = event.eventId().toString();
        String type = GradingRequestedV1.EVENT_TYPE;
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, type, id));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(event, "assessment.grading-overdue.v1", id));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(event, type, UUID.randomUUID().toString()));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                new GradingRequestedV1(event.eventId(), "other", event.occurredAt(), event.examinerId(),
                        event.gradingJobId(), "WRITING", event.deadline()), type, id));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                new GradingRequestedV1(event.eventId(), type, event.occurredAt(), null, event.gradingJobId(), "WRITING",
                        event.deadline()), type, id));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                new GradingRequestedV1(event.eventId(), type, event.occurredAt(), event.examinerId(), null, "WRITING",
                        event.deadline()), type, id));
        verifyNoInteractions(notifications);
    }
}
