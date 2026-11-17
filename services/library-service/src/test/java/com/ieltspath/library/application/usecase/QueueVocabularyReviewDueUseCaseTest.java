package com.ieltspath.library.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ieltspath.events.library.VocabularyReviewDueV1;
import com.ieltspath.library.application.port.ReviewDueReminders;
import com.ieltspath.library.application.port.ReviewDueReminders.DueLearner;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueueVocabularyReviewDueUseCaseTest {
    /** 08:00 on 16 Nov in Ho Chi Minh City. */
    private static final Instant NOW = Instant.parse("2026-11-16T01:00:00Z");
    private static final UUID NOBODY = new UUID(0L, 0L);

    private final ReviewDueReminders reminders = mock(ReviewDueReminders.class);
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final QueueVocabularyReviewDueUseCase useCase =
            new QueueVocabularyReviewDueUseCase(reminders, mapper, "Asia/Ho_Chi_Minh", 5);

    @Test
    void queuesOneEventPerLearnerCarryingTheDayAndTheCount() throws Exception {
        UUID learner = UUID.randomUUID();
        when(reminders.findDueLearners(NOW, 5, NOBODY, 500)).thenReturn(List.of(new DueLearner(learner, 12)));
        when(reminders.queue(any(), any(), any(), any())).thenReturn(true);

        assertEquals(1, useCase.execute(NOW));

        var eventId = ArgumentCaptor.forClass(UUID.class);
        var payload = ArgumentCaptor.forClass(String.class);
        verify(reminders).queue(eventId.capture(), eq(learner), payload.capture(), eq(NOW));
        VocabularyReviewDueV1 event = mapper.readValue(payload.getValue(), VocabularyReviewDueV1.class);
        assertEquals(eventId.getValue(), event.eventId());
        assertEquals(VocabularyReviewDueV1.EVENT_TYPE, event.eventType());
        assertEquals(learner, event.userId());
        assertEquals(LocalDate.of(2026, 11, 16), event.reviewDate());
        assertEquals(12, event.dueCount());
    }

    @Test
    void theDayIsTheCalendarDayInTheConfiguredZoneNotInUtc() throws Exception {
        UUID learner = UUID.randomUUID();
        Instant lateEvening = Instant.parse("2026-11-16T18:00:00Z"); // 01:00 on 17 Nov in Ho Chi Minh City
        when(reminders.findDueLearners(lateEvening, 5, NOBODY, 500)).thenReturn(List.of(new DueLearner(learner, 5)));
        when(reminders.queue(any(), any(), any(), any())).thenReturn(true);

        useCase.execute(lateEvening);

        var payload = ArgumentCaptor.forClass(String.class);
        verify(reminders).queue(any(), eq(learner), payload.capture(), any());
        assertEquals(LocalDate.of(2026, 11, 17), mapper.readValue(payload.getValue(), VocabularyReviewDueV1.class).reviewDate());
    }

    @Test
    void theEventIdIsStableForALearnerAndDayAndDiffersAcrossDaysAndLearners() {
        UUID learner = UUID.randomUUID();
        LocalDate day = LocalDate.of(2026, 11, 16);

        assertEquals(QueueVocabularyReviewDueUseCase.eventId(learner, day), QueueVocabularyReviewDueUseCase.eventId(learner, day));
        assertNotEquals(QueueVocabularyReviewDueUseCase.eventId(learner, day), QueueVocabularyReviewDueUseCase.eventId(learner, day.plusDays(1)));
        assertNotEquals(QueueVocabularyReviewDueUseCase.eventId(learner, day), QueueVocabularyReviewDueUseCase.eventId(UUID.randomUUID(), day));
    }

    @Test
    void aLearnerAlreadyQueuedTodayIsNotCounted() {
        when(reminders.findDueLearners(NOW, 5, NOBODY, 500)).thenReturn(List.of(new DueLearner(UUID.randomUUID(), 6)));
        when(reminders.queue(any(), any(), any(), any())).thenReturn(false);

        assertEquals(0, useCase.execute(NOW));
    }

    @Test
    void readsEveryPageUntilOneComesBackShort() {
        List<DueLearner> full = java.util.stream.IntStream.range(0, 500)
                .mapToObj(i -> new DueLearner(new UUID(1L, i), 5)).toList();
        UUID last = full.get(499).userId();
        when(reminders.findDueLearners(NOW, 5, NOBODY, 500)).thenReturn(full);
        when(reminders.findDueLearners(NOW, 5, last, 500)).thenReturn(List.of(new DueLearner(new UUID(2L, 0), 5)));
        when(reminders.queue(any(), any(), any(), any())).thenReturn(true);

        assertEquals(501, useCase.execute(NOW));
        verify(reminders, times(2)).findDueLearners(any(), anyInt(), any(), eq(500));
    }

    @Test
    void aMinimumBelowOneIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new QueueVocabularyReviewDueUseCase(reminders, mapper, "Asia/Ho_Chi_Minh", 0));
    }
}
