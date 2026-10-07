package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.GateEssayJobStore;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.entity.AttemptItem;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AssessmentResultRepository;
import com.ieltspath.assessment.domain.repository.AttemptItemRepository;
import com.ieltspath.assessment.domain.vo.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GateResultAssemblerTest {
    private final AssessmentAttemptRepository attempts = mock(AssessmentAttemptRepository.class);
    private final AssessmentResultRepository results = mock(AssessmentResultRepository.class);
    private final AttemptItemRepository items = mock(AttemptItemRepository.class);
    private final GateEssayJobStore jobs = mock(GateEssayJobStore.class);
    private final AutoGradeAttemptService autoGrader = mock(AutoGradeAttemptService.class);
    private final UUID attemptId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();
    private final AssessmentAttempt attempt = attempt();

    @Test
    void essayAtPassBandCompletesWithEssayPassingAndKeepsObjectiveItemsForAutoGrader() {
        stubAttempt(new BigDecimal("5.5"));

        assertTrue(assembler().completeIfGraded(attemptId));

        verify(autoGrader).grade(attempt, java.util.Map.of(itemId, true));
    }

    @Test
    void essayBelowPassBandIsGivenZeroByTheAutoGrader() {
        stubAttempt(new BigDecimal("5.0"));

        assembler().completeIfGraded(attemptId);

        verify(autoGrader).grade(attempt, java.util.Map.of(itemId, false));
    }

    @Test
    void resultWaitsUntilEveryAiJobIsCompleted() {
        when(attempts.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(results.findLatestForUpdateByAttemptId(attemptId)).thenReturn(Optional.empty());
        when(jobs.aiJobs(attemptId)).thenReturn(List.of(new GateEssayJobStore.JobState(itemId, "PROCESSING", null)));

        assertFalse(assembler().completeIfGraded(attemptId));

        verifyNoInteractions(autoGrader);
    }

    private void stubAttempt(BigDecimal band) {
        when(attempts.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(results.findLatestForUpdateByAttemptId(attemptId)).thenReturn(Optional.empty());
        when(jobs.aiJobs(attemptId)).thenReturn(List.of(
                new GateEssayJobStore.JobState(itemId, "COMPLETED", band)));
        when(items.findByAttemptId(attemptId)).thenReturn(List.of(new AttemptItem(itemId, UUID.randomUUID(),
                UUID.randomUUID(), 0, "{\"stem\":\"Write an essay\"}",
                "{\"answerSpec\":{\"type\":\"ESSAY\",\"passBand\":5.5},\"maxScore\":1}", null)));
        when(autoGrader.grade(attempt, java.util.Map.of(itemId, band.compareTo(new BigDecimal("5.5")) >= 0)))
                .thenReturn(true);
    }

    private GateResultAssembler assembler() {
        return new GateResultAssembler(attempts, results, items, jobs, autoGrader, new ObjectMapper());
    }

    private AssessmentAttempt attempt() {
        Instant now = Instant.now();
        return new AssessmentAttempt(attemptId, UUID.randomUUID(), UUID.randomUUID(), AttemptType.TOPIC_GATE,
                AttemptMode.STANDARD, AttemptChannel.WEB, AttemptStatus.SUBMITTED, now, now, null, 1, now, now);
    }
}
