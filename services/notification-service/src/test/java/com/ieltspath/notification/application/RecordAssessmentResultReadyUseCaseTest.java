package com.ieltspath.notification.application;

import com.ieltspath.events.assessment.AssessmentCompletedV2;
import com.ieltspath.events.assessment.AssessmentMessaging;
import com.ieltspath.notification.domain.repository.NotificationRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecordAssessmentResultReadyUseCaseTest {
    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final NotificationPreferenceService preferences = mock(NotificationPreferenceService.class);
    private final RecordAssessmentResultReadyUseCase useCase = new RecordAssessmentResultReadyUseCase(
            notifications, preferences, EnqueueNotificationPushUseCase.disabled());
    private final UUID user = UUID.randomUUID();
    private final AssessmentCompletedV2 event = event("PLACEMENT", 1);

    private AssessmentCompletedV2 event(String assessmentType, int version) {
        return new AssessmentCompletedV2(UUID.randomUUID(), AssessmentCompletedV2.EVENT_TYPE, Instant.now(),
                AssessmentCompletedV2.SOURCE, new AssessmentCompletedV2.Data(user, null, UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), version, assessmentType, "COMPLETED", Instant.now(), null, List.of()));
    }

    @Test
    void storesFirstPlacementAndMockResultsWhenEnabled() {
        when(preferences.isEnabled(user, NotificationEventTypes.RESULT_READY)).thenReturn(true);
        when(notifications.insertIfAbsent(any(), any())).thenReturn(true);
        assertTrue(useCase.execute(event, AssessmentMessaging.COMPLETED_ROUTING_KEY, event.eventId().toString()).isPresent());

        var mockResult = event("MOCK", 1);
        assertTrue(useCase.execute(mockResult, AssessmentMessaging.COMPLETED_ROUTING_KEY, mockResult.eventId().toString()).isPresent());
    }

    @Test
    void skipsRegradesOtherAssessmentTypesAndDisabledPreferences() {
        var regrade = event("MOCK", 2);
        assertTrue(useCase.execute(regrade, AssessmentMessaging.COMPLETED_ROUTING_KEY, regrade.eventId().toString()).isEmpty());
        var other = event("COURSE_FINAL", 1);
        assertTrue(useCase.execute(other, AssessmentMessaging.COMPLETED_ROUTING_KEY, other.eventId().toString()).isEmpty());
        when(preferences.isEnabled(user, NotificationEventTypes.RESULT_READY)).thenReturn(false);
        assertTrue(useCase.execute(event, AssessmentMessaging.COMPLETED_ROUTING_KEY, event.eventId().toString()).isEmpty());
        verify(notifications, never()).insertIfAbsent(any(), any());
    }

    @Test
    void rejectsMessagesThatBreakTheEventContract() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(event, "assessment.other.v2", event.eventId().toString()));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(event, event.eventType(), UUID.randomUUID().toString()));
    }
}
