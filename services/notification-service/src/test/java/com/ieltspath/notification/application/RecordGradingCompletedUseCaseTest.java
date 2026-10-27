package com.ieltspath.notification.application;

import com.ieltspath.events.assessment.GradingCompletedV1;
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

class RecordGradingCompletedUseCaseTest {
    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final RecordGradingCompletedUseCase useCase = new RecordGradingCompletedUseCase(notifications);
    private final GradingCompletedV1 event = new GradingCompletedV1(UUID.randomUUID(), GradingCompletedV1.EVENT_TYPE,
            Instant.parse("2026-10-26T13:00:00Z"), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "WRITING");

    @Test
    void theLearnerGetsANoticeThatPointsAtTheJob() {
        when(notifications.insertIfAbsent(any(), any())).thenReturn(true);

        var stored = useCase.execute(event, GradingCompletedV1.EVENT_TYPE, event.eventId().toString()).orElseThrow();

        var saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).insertIfAbsent(org.mockito.ArgumentMatchers.eq(event.eventId()), saved.capture());
        assertEquals(stored, saved.getValue());
        assertEquals(event.learnerId(), stored.recipientId());
        assertEquals(event.learnerId(), stored.actorId(), "the notice does not name the examiner");
        assertEquals(event.gradingJobId(), stored.gradingJobId());
        assertEquals(GradingCompletedV1.EVENT_TYPE, stored.eventType());
        assertNull(stored.postId());
        assertNull(stored.commentId());
        assertNull(stored.readAt());
    }

    @Test
    void aRepeatedEventStoresNothingNew() {
        when(notifications.insertIfAbsent(any(), any())).thenReturn(false);

        assertTrue(useCase.execute(event, GradingCompletedV1.EVENT_TYPE, event.eventId().toString()).isEmpty());
    }

    @Test
    void anEventThatBreaksTheContractIsRefusedAndNothingIsStored() {
        String id = event.eventId().toString();
        String type = GradingCompletedV1.EVENT_TYPE;
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, type, id));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(event, "assessment.grading-overdue.v1", id));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(event, type, UUID.randomUUID().toString()));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                new GradingCompletedV1(event.eventId(), "other", event.occurredAt(), event.learnerId(),
                        event.gradingJobId(), event.reviewSubmissionId(), "WRITING"), type, id));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                new GradingCompletedV1(event.eventId(), type, event.occurredAt(), null, event.gradingJobId(), event.reviewSubmissionId(), "WRITING"),
                        type, id));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                new GradingCompletedV1(event.eventId(), type, event.occurredAt(), event.learnerId(), null, event.reviewSubmissionId(), "WRITING"),
                        type, id));
        verifyNoInteractions(notifications);
    }
}
