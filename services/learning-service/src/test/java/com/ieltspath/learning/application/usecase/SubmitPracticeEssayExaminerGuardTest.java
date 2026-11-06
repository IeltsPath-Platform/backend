package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.service.EssayPrompts;
import com.ieltspath.learning.application.service.EssaySubmissionFlow;
import com.ieltspath.learning.application.service.PracticeAccess;
import com.ieltspath.learning.application.service.WritingSubmissionViewAssembler;
import com.ieltspath.learning.domain.aggregate.PracticeAttempt;
import com.ieltspath.learning.domain.aggregate.WritingSubmission;
import com.ieltspath.learning.domain.repository.HumanReviewRequestRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The AI does not grade a question of a practice attempt whose essay is already with an examiner: one essay, one grade. */
class SubmitPracticeEssayExaminerGuardTest {
    private static final UUID USER = UUID.randomUUID(), ATTEMPT = UUID.randomUUID(), LESSON = UUID.randomUUID();
    private static final UUID VERSION = UUID.randomUUID(), QUESTION = UUID.randomUUID();

    private final PracticeAttemptRepository attempts = mock(PracticeAttemptRepository.class);
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final WritingSubmissionRepository essays = mock(WritingSubmissionRepository.class);
    private final HumanReviewRequestRepository requests = mock(HumanReviewRequestRepository.class);
    private final EssaySubmissionFlow flow = mock(EssaySubmissionFlow.class);
    private SubmitPracticeEssayUseCase useCase;
    private LearningContentClient.Item item;

    @BeforeEach
    void setUp() {
        useCase = new SubmitPracticeEssayUseCase(attempts, content, mock(PracticeAccess.class), mock(LearnerLock.class),
                essays, flow, mock(PlatformTransactionManager.class), mock(WritingSubmissionViewAssembler.class), requests);
        item = new LearningContentClient.Item(QUESTION, 1, "Should cities plant trees?", null,
                Map.of("type", "ESSAY", "task", "TASK_2", "passBand", 6.0), null, null, BigDecimal.ONE, List.of(), null, List.of());
        when(flow.validate(any())).thenReturn(120);
        when(essays.findByRequestId(any())).thenReturn(Optional.empty());
        when(attempts.findOwned(USER, ATTEMPT)).thenReturn(Optional.of(PracticeAttempt.start(ATTEMPT, USER, LESSON,
                LearningSkill.WRITING, UUID.randomUUID(), VERSION, Instant.now())));
        when(content.getPackageVersion(VERSION)).thenReturn(new LearningContentClient.PackageVersion(VERSION,
                UUID.randomUUID(), "PRACTICE_SET", null, Map.of(),
                List.of(new LearningContentClient.Section(UUID.randomUUID(), "Write", "WRITING", "", 1, null, List.of(item)))));
    }

    private WritingSubmission sentToExaminer() {
        return WritingSubmission.startPracticeForExaminer(UUID.randomUUID(), USER, LESSON, ATTEMPT, UUID.randomUUID(),
                "An essay", 120, EssayPrompts.of(item), Instant.now());
    }

    @Test
    void theAiRefusesAQuestionWhoseEssayAnExaminerIsGrading() {
        WritingSubmission waiting = sentToExaminer();
        when(essays.latestForPractice(USER, ATTEMPT)).thenReturn(Map.of(QUESTION, waiting));
        when(requests.findReviewId(waiting.id())).thenReturn(Optional.of(UUID.randomUUID()));

        var refused = assertThrows(LearningRequestException.class,
                () -> useCase.execute(USER, ATTEMPT, QUESTION, UUID.randomUUID(), "A new essay"));

        assertEquals(409, refused.getStatus());
        assertEquals("EXAMINER_REVIEW_PENDING", refused.getCode());
        assertEquals(waiting.id(), refused.getSubmissionId());
        verify(flow, never()).grade(any());
        verify(essays, never()).save(any());
    }

    @Test
    void anEssayWhoseExaminerRequestNeverWentThroughDoesNotHoldTheQuestion() {
        WritingSubmission orphan = sentToExaminer();
        when(essays.latestForPractice(USER, ATTEMPT)).thenReturn(Map.of(QUESTION, orphan));
        when(requests.findReviewId(orphan.id())).thenReturn(Optional.empty());
        when(essays.findGradingForPractice(USER, ATTEMPT, QUESTION)).thenReturn(List.of());

        // Past the guard the AI flow goes on to store and grade; the stub stops it there.
        when(essays.save(any())).thenThrow(new IllegalStateException("stored"));
        assertThrows(IllegalStateException.class, () -> useCase.execute(USER, ATTEMPT, QUESTION, UUID.randomUUID(), "A new essay"));
    }
}
