package com.group01.assessment.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.assessment.application.port.GateEssayJobStore;
import com.group01.assessment.domain.aggregate.AssessmentAttempt;
import com.group01.assessment.domain.entity.AttemptItem;
import com.group01.assessment.domain.repository.AttemptItemRepository;
import com.group01.assessment.domain.vo.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnqueueGateEssayGradingServiceTest {
    private static final String ESSAY = """
            {"answerSpec":{"type":"ESSAY","task":"TASK_2","minWords":250,"passBand":5.5},
             "explanation":"private","maxScore":1.0}
            """;
    private static final String OBJECTIVE =
            "{\"answerSpec\":{\"type\":\"CHOICE\",\"correct\":\"A\"},\"maxScore\":1.0}";

    @Mock AttemptItemRepository items;
    @Mock GateEssayJobStore jobs;
    @Mock AutoGradeAttemptService autoGrader;

    private final ObjectMapper json = new ObjectMapper();
    private final UUID userId = UUID.randomUUID();

    @Test
    void submittedGateEssayQueuesOnlyItsFreeAiJobAndDoesNotCompleteResult() {
        AssessmentAttempt attempt = attempt(AttemptType.TOPIC_GATE);
        UUID essayId = UUID.randomUUID();
        UUID objectiveId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        when(items.findByAttemptId(attempt.getId())).thenReturn(List.of(item(essayId, ESSAY), item(objectiveId, OBJECTIVE)));
        when(jobs.submittedEssays(List.of(essayId))).thenReturn(Map.of(essayId, submissionId));

        service().enqueueOrGrade(attempt);

        verify(jobs).enqueueAi(Map.of(essayId, submissionId), userId);
        verify(jobs, never()).enqueueHuman(any(), any(), any());
        verifyNoInteractions(autoGrader);
    }

    @Test
    void gateWithOmittedEssayScoresItZeroAndCompletesWithoutCreatingJob() {
        AssessmentAttempt attempt = attempt(AttemptType.COURSE_GATE);
        UUID essayId = UUID.randomUUID();
        UUID objectiveId = UUID.randomUUID();
        when(items.findByAttemptId(attempt.getId())).thenReturn(List.of(item(essayId, ESSAY), item(objectiveId, OBJECTIVE)));
        when(jobs.submittedEssays(List.of(essayId))).thenReturn(Map.of());

        service().enqueueOrGrade(attempt);

        verify(autoGrader).grade(attempt, Map.of(essayId, false));
        verify(jobs).submittedEssays(List.of(essayId));
        verify(jobs, never()).enqueueAi(anyMap(), any());
        verify(jobs, never()).enqueueHuman(any(), any(), any());
    }

    @Test
    void mockEssayDoesNotQueueAiOrChangeExistingHumanGradingPath() {
        AssessmentAttempt attempt = attempt(AttemptType.MOCK);
        service().enqueueOrGrade(attempt);
        verifyNoInteractions(items, jobs);
        verify(autoGrader).gradeIfObjective(attempt);
    }

    @Test
    void repeatedGateEnqueueUsesTheSameUniqueKeyForIdempotentAiJobs() {
        AssessmentAttempt attempt = attempt(AttemptType.TOPIC_GATE);
        UUID essayId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        when(items.findByAttemptId(attempt.getId())).thenReturn(List.of(item(essayId, ESSAY)));
        when(jobs.submittedEssays(List.of(essayId))).thenReturn(Map.of(essayId, submissionId));

        service().enqueueOrGrade(attempt);
        service().enqueueOrGrade(attempt);

        verify(jobs, times(2)).enqueueAi(Map.of(essayId, submissionId), userId);
    }

    private EnqueueGateEssayGradingService service() {
        return new EnqueueGateEssayGradingService(items, jobs, autoGrader, json);
    }

    private AttemptItem item(UUID id, String answer) {
        return new AttemptItem(id, UUID.randomUUID(), UUID.randomUUID(), 0,
                "{\"stem\":\"Write an essay\"}", answer, null);
    }

    private AssessmentAttempt attempt(AttemptType type) {
        Instant now = Instant.now();
        return new AssessmentAttempt(UUID.randomUUID(), userId, UUID.randomUUID(), type, AttemptMode.STANDARD,
                AttemptChannel.WEB, AttemptStatus.SUBMITTED, now, now, null, 1, now, now);
    }
}
