package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.port.*;
import com.ieltspath.learning.application.port.LearningContentClient.*;
import com.ieltspath.learning.application.service.*;
import com.ieltspath.learning.domain.aggregate.PracticeAttempt;
import com.ieltspath.learning.domain.aggregate.WritingSubmission;
import com.ieltspath.learning.domain.repository.*;
import com.ieltspath.learning.domain.service.ReviewRule.ReviewCandidate;
import com.ieltspath.learning.domain.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SubmitPracticeAttemptUseCaseTest {
    private final LearnerLock lock = mock(LearnerLock.class);
    private final PracticeAccess access = mock(PracticeAccess.class);
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final PracticeAttemptRepository attempts = mock(PracticeAttemptRepository.class);
    private final ReviewItemRepository reviews = mock(ReviewItemRepository.class);
    private final KnowledgeEvidenceRepository evidence = mock(KnowledgeEvidenceRepository.class);
    private final PracticeProgress progress = mock(PracticeProgress.class);
    private final FirstAttemptMistakes mistakes = mock(FirstAttemptMistakes.class);
    private final WritingSubmissionRepository essays = mock(WritingSubmissionRepository.class);
    private final QuestionEventLog events = mock(QuestionEventLog.class);
    private final MasteryProfileUpdater mastery = mock(MasteryProfileUpdater.class);
    private final FreePracticeSubmission freePractice = mock(FreePracticeSubmission.class);
    private final GamificationRecorder gamification = mock(GamificationRecorder.class);
    private final HeartsService hearts = mock(HeartsService.class);
    private final WritingCriterionReviews criterionReviews = spy(new WritingCriterionReviews(reviews, attempts, content, 2));
    private final ErrorTrapEvidence errorTrapEvidence = mock(ErrorTrapEvidence.class);
    private final SubmitPracticeAttemptUseCase useCase = new SubmitPracticeAttemptUseCase(lock, access, content,
            attempts, reviews, evidence, progress, mistakes, essays, events, mastery, freePractice, gamification,
            hearts, criterionReviews, errorTrapEvidence);
    private final UUID userId = UUID.randomUUID(), lessonId = UUID.randomUUID(), attemptId = UUID.randomUUID();
    private final UUID packageId = UUID.randomUUID(), versionId = UUID.randomUUID(), essayId = UUID.randomUUID();
    private final UUID ta = UUID.randomUUID(), cc = UUID.randomUUID(), lr = UUID.randomUUID(), gra = UUID.randomUUID();

    @BeforeEach void setup() {
        PracticeAttempt attempt = mock(PracticeAttempt.class);
        when(attempts.findOwned(userId, attemptId)).thenReturn(Optional.of(attempt));
        when(attempt.acceptsAnswers(any())).thenReturn(true);
        when(attempt.id()).thenReturn(attemptId);
        when(attempt.lessonId()).thenReturn(lessonId);
        when(attempt.packageId()).thenReturn(packageId);
        when(attempt.packageVersionId()).thenReturn(versionId);
        when(attempt.origin()).thenReturn(PracticeOrigin.LESSON);
        when(attempt.skill()).thenReturn(LearningSkill.WRITING);
        when(attempts.revealedPackageIds(userId)).thenReturn(Set.of());
        when(content.getPackageVersion(versionId)).thenReturn(new PackageVersion(versionId, packageId,
                "PRACTICE_SET", null, Map.of(), List.of(new Section(UUID.randomUUID(), "Essay", "WRITING", null,
                0, null, List.of(new Item(essayId, 0, "Prompt", null, Map.of("type", "ESSAY"), null,
                BigDecimal.ONE, List.of()))))));
        WritingSubmission essay = mock(WritingSubmission.class);
        when(essay.status()).thenReturn(WritingSubmissionStatus.GRADED);
        when(essay.passed()).thenReturn(false);
        when(essay.prompt()).thenReturn(new EssayPrompt(essayId, "Prompt", "TASK_1", 150, new BigDecimal("5.5"),
                "Chart facts", null, List.of(), List.of()));
        when(essay.grade()).thenReturn(new WritingGrade(List.of(criterion("TA", ta, "5.5"),
                criterion("CC", cc, "5.5"), criterion("LR", lr, "5.0"), criterion("GRA", gra, "4.5")),
                List.of(), "Summary", new BigDecimal("5.0")));
        when(essays.latestForPractice(userId, attemptId)).thenReturn(Map.of(essayId, essay));
        when(content.practiceSetAvailability(anyList(), anyList(), eq(3))).thenReturn(Map.of(lr, 3, gra, 3));
        when(gamification.recordCorrectAnswers(any(), any(), anyList())).thenReturn(new GamificationSummary(0, false, 0, 1, false));
        when(gamification.current(userId)).thenReturn(new GamificationSummary(0, false, 0, 1, false));
        when(hearts.lose(userId, 0)).thenReturn(new HeartsService.Snapshot(5, 5, null, 5));
    }

    @Test void countedEssayCreatesOnlyTheFailedCriteriaAsOptionalReviews() {
        var result = useCase.execute(userId, attemptId, new SubmitExerciseCommand(UUID.randomUUID(), List.of()));
        assertTrue(result.countedAsEvidence());
        verify(reviews).insertPending(userId, List.of(new ReviewCandidate(gra, lessonId, LearningSkill.WRITING),
                new ReviewCandidate(lr, lessonId, LearningSkill.WRITING)), false, "WRITING_CRITERION");
        verify(errorTrapEvidence, never()).errorTexts(anyCollection());
    }

    @Test void revealedPackageWritesNoNewCriterionReviews() {
        when(attempts.revealedPackageIds(userId)).thenReturn(Set.of(packageId));
        var result = useCase.execute(userId, attemptId, new SubmitExerciseCommand(UUID.randomUUID(), List.of()));
        assertFalse(result.countedAsEvidence());
        verify(criterionReviews, never()).create(any(), any(), anyCollection());
        verify(reviews, never()).insertPending(any(), anyList(), anyBoolean(), anyString());
    }

    private WritingGrade.Criterion criterion(String code, UUID kp, String band) {
        return new WritingGrade.Criterion(code, new BigDecimal(band), List.of(), List.of(), kp);
    }
}
