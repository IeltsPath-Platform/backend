package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.domain.repository.SubmissionHistoryRepository;
import com.ieltspath.learning.domain.repository.SubmissionHistoryRepository.PracticeTally;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetPracticeSinceEssayUseCaseTest {
    private final SubmissionHistoryRepository history = mock(SubmissionHistoryRepository.class);
    private final GetPracticeSinceEssayUseCase useCase = new GetPracticeSinceEssayUseCase(history);
    private final UUID user = UUID.randomUUID();
    private final UUID essay = UUID.randomUUID();
    private final Instant submitted = Instant.parse("2026-11-14T10:00:00Z");

    @Test
    void countsPracticeSinceTheEssayOnTheAskedPointsOnce() {
        UUID point = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        var tally = new PracticeTally(point, 2, UUID.randomUUID(), submitted.plusSeconds(60), 7, 10, true);
        when(history.essaySubmittedAt(user, essay)).thenReturn(Optional.of(submitted));
        when(history.tallyKnowledgePractice(user, new LinkedHashSet<>(List.of(point, other)), submitted)).thenReturn(List.of(tally));

        assertEquals(List.of(tally), useCase.execute(user, essay, Arrays.asList(point, other, point, null)));
    }

    @Test
    void anotherLearnersEssayIsNotFound() {
        when(history.essaySubmittedAt(user, essay)).thenReturn(Optional.empty());

        var error = assertThrows(LearningRequestException.class, () -> useCase.execute(user, essay, List.of(UUID.randomUUID())));

        assertEquals(404, error.getStatus());
        verify(history, never()).tallyKnowledgePractice(any(), anyCollection(), any());
    }

    @Test
    void noPointsAskedMeansNothingPractised() {
        when(history.essaySubmittedAt(user, essay)).thenReturn(Optional.of(submitted));

        assertTrue(useCase.execute(user, essay, null).isEmpty());
        verify(history, never()).tallyKnowledgePractice(any(), anyCollection(), any());
    }

    @Test
    void refusesTooManyPoints() {
        List<UUID> points = new ArrayList<>(IntStream.range(0, GetPracticeSinceEssayUseCase.MAX_POINTS + 1)
                .mapToObj(index -> UUID.randomUUID()).toList());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(user, essay, points));
    }
}
