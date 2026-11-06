package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.Item;
import com.ieltspath.learning.application.port.LearningContentClient.KnowledgePointMapping;
import com.ieltspath.learning.application.port.LearningContentClient.PackageVersion;
import com.ieltspath.learning.application.port.LearningContentClient.Section;
import com.ieltspath.learning.application.port.QuestionEventLog;
import com.ieltspath.learning.application.service.ErrorTrapCatalog;
import com.ieltspath.learning.application.service.ErrorTrapEvidence;
import com.ieltspath.learning.application.service.FirstAttemptMistakes;
import com.ieltspath.learning.application.service.FreePracticeSubmission;
import com.ieltspath.learning.application.service.GamificationRecorder;
import com.ieltspath.learning.application.service.HeartsService;
import com.ieltspath.learning.application.service.MasteryProfileUpdater;
import com.ieltspath.learning.application.service.PracticeAccess;
import com.ieltspath.learning.application.service.PracticeProgress;
import com.ieltspath.learning.application.service.SpeakingSettingsFixture;
import com.ieltspath.learning.application.service.WritingCriterionReviews;
import com.ieltspath.learning.domain.aggregate.PracticeAttempt;
import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import com.ieltspath.learning.domain.vo.EvidenceSource;
import com.ieltspath.learning.domain.vo.GamificationSummary;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.FailedWritingCriterion;
import com.ieltspath.learning.domain.vo.PracticeOrigin;
import com.ieltspath.learning.domain.vo.PromotedReview;
import com.ieltspath.learning.domain.vo.ReviewStage;
import com.ieltspath.learning.domain.vo.SpeakingRecording;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The recorded answer of a lesson's practice set: it must be graded first and reaches mastery only through the attempt. */
class SubmitPracticeAttemptSpeakingTest {
    private final LearnerLock lock = mock(LearnerLock.class);
    private final PracticeAccess access = mock(PracticeAccess.class);
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final PracticeAttemptRepository attempts = mock(PracticeAttemptRepository.class);
    private final ReviewItemRepository reviews = mock(ReviewItemRepository.class);
    private final KnowledgeEvidenceRepository evidence = mock(KnowledgeEvidenceRepository.class);
    private final SpeakingSubmissionRepository speaking = mock(SpeakingSubmissionRepository.class);
    private final MasteryProfileUpdater mastery = mock(MasteryProfileUpdater.class);
    private final PracticeProgress progress = mock(PracticeProgress.class);
    private final GamificationRecorder gamification = mock(GamificationRecorder.class);
    private final HeartsService hearts = mock(HeartsService.class);
    private final WritingCriterionReviews criterionReviews = mock(WritingCriterionReviews.class);
    private final ErrorTrapEvidence errorTraps = mock(ErrorTrapEvidence.class);
    private final SubmitPracticeAttemptUseCase useCase = new SubmitPracticeAttemptUseCase(lock, access, content,
            attempts, reviews, evidence, progress, mock(FirstAttemptMistakes.class),
            mock(WritingSubmissionRepository.class), mock(QuestionEventLog.class), mastery,
            mock(FreePracticeSubmission.class), gamification, hearts, criterionReviews,
            errorTraps, speaking, SpeakingSettingsFixture.enabled(),
            mock(com.ieltspath.learning.domain.repository.HumanReviewOutcomeRepository.class));

    private final UUID userId = UUID.randomUUID(), lessonId = UUID.randomUUID(), attemptId = UUID.randomUUID();
    private final UUID packageId = UUID.randomUUID(), versionId = UUID.randomUUID(), questionId = UUID.randomUUID();
    private final UUID kp = UUID.randomUUID();
    private final UUID fcKp = UUID.randomUUID(), lrKp = UUID.randomUUID(), graKp = UUID.randomUUID(), prKp = UUID.randomUUID();
    private final UUID trapKp = UUID.randomUUID();

    @BeforeEach
    void setup() {
        PracticeAttempt attempt = mock(PracticeAttempt.class);
        when(attempts.findOwned(userId, attemptId)).thenReturn(Optional.of(attempt));
        when(attempt.acceptsAnswers(any())).thenReturn(true);
        when(attempt.id()).thenReturn(attemptId);
        when(attempt.lessonId()).thenReturn(lessonId);
        when(attempt.packageId()).thenReturn(packageId);
        when(attempt.packageVersionId()).thenReturn(versionId);
        when(attempt.origin()).thenReturn(PracticeOrigin.LESSON);
        when(attempt.skill()).thenReturn(LearningSkill.SPEAKING);
        when(attempts.revealedPackageIds(userId)).thenReturn(Set.of());
        when(content.getPackageVersion(versionId)).thenReturn(new PackageVersion(versionId, packageId, "PRACTICE_SET",
                null, Map.of(), List.of(new Section(UUID.randomUUID(), "Speak", "SPEAKING", null, 0, null,
                List.of(new Item(questionId, 0, "Do you like cooking?", null,
                        Map.of("type", "SPEAKING", "durationSeconds", 60), null, BigDecimal.ONE,
                        List.of(new KnowledgePointMapping(kp, BigDecimal.ONE))))))));
        when(gamification.recordCorrectAnswers(any(), any(), anyList())).thenReturn(new GamificationSummary(0, false, 0, 1, false));
        when(gamification.current(userId)).thenReturn(new GamificationSummary(0, false, 0, 1, false));
        when(hearts.lose(userId, 0)).thenReturn(new HeartsService.Snapshot(5, 5, null, 5));
    }

    /** A single answer has no overall band, as the grader gives one only for a full test: its band is in its criteria. */
    private SpeakingSubmission answer(SpeakingSubmissionStatus status, String band, boolean usable) {
        SpeakingSubmission answer = mock(SpeakingSubmission.class);
        when(answer.status()).thenReturn(status);
        when(answer.overallBand()).thenReturn(null);
        List<Map<String, Object>> criteria = List.of(Map.of("code", "FC", "band", new BigDecimal(band)),
                Map.of("code", "LR", "band", new BigDecimal(band)), Map.of("code", "GRA", "band", new BigDecimal(band)),
                Map.of("code", "PR", "band", new BigDecimal(band)));
        when(answer.result()).thenReturn(Map.of("display", Map.of("criteria", criteria),
                "signals", Map.of("usableAsEvidence", usable)));
        when(answer.recordingFor(questionId)).thenReturn(Optional.of(SpeakingRecording.expected(questionId,
                UUID.randomUUID(), 1, 1)));
        when(speaking.findLatestForAttempt(userId, attemptId)).thenReturn(Optional.of(answer));
        return answer;
    }

    /** A graded answer whose signals carry the criteria with their points and the errors the grader marked. */
    private SpeakingSubmission answerWithSignals(Map<String, String> bands, Map<String, UUID> points, List<String> errors,
                                                 boolean usable) {
        SpeakingSubmission answer = mock(SpeakingSubmission.class);
        when(answer.status()).thenReturn(SpeakingSubmissionStatus.GRADED);
        when(answer.overallBand()).thenReturn(null);
        List<Map<String, Object>> display = new java.util.ArrayList<>();
        List<Map<String, Object>> signalCriteria = new java.util.ArrayList<>();
        bands.forEach((code, band) -> {
            display.add(Map.of("code", code, "band", new BigDecimal(band)));
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("code", code);
            row.put("band", new BigDecimal(band));
            if (points.get(code) != null) row.put("kpId", points.get(code).toString());
            signalCriteria.add(row);
        });
        when(answer.result()).thenReturn(Map.of("display", Map.of("criteria", display), "signals", Map.of(
                "usableAsEvidence", usable, "criteria", signalCriteria,
                "errors", errors.stream().map(code -> Map.<String, Object>of("code", code, "partNo", 1, "excerpt", "x")).toList())));
        when(answer.recordingFor(questionId)).thenReturn(Optional.of(SpeakingRecording.expected(questionId,
                UUID.randomUUID(), 1, 1)));
        when(speaking.findLatestForAttempt(userId, attemptId)).thenReturn(Optional.of(answer));
        return answer;
    }

    private Map<String, UUID> criterionPoints() {
        return Map.of("FC", fcKp, "LR", lrKp, "GRA", graKp, "PR", prKp);
    }

    @SuppressWarnings("unchecked")
    private List<KnowledgeEvidence> appended() {
        ArgumentCaptor<List<KnowledgeEvidence>> rows = ArgumentCaptor.forClass(List.class);
        verify(evidence).append(eq(userId), rows.capture());
        return rows.getValue();
    }

    @SuppressWarnings("unchecked")
    private Collection<FailedWritingCriterion> failedForSpeaking() {
        ArgumentCaptor<Collection<FailedWritingCriterion>> failed = ArgumentCaptor.forClass(Collection.class);
        verify(criterionReviews).createForSpeaking(eq(userId), eq(lessonId), failed.capture());
        return failed.getValue();
    }

    private SubmitExerciseCommand command() {
        return new SubmitExerciseCommand(UUID.randomUUID(), List.of());
    }

    @Test
    void anAttemptWithAnUngradedRecordingCannotBeSubmitted() {
        answer(SpeakingSubmissionStatus.QUEUED, "6.5", true);

        assertThatThrownBy(() -> useCase.execute(userId, attemptId, command()))
                .isInstanceOfSatisfying(LearningRequestException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("SPEAKING_NOT_GRADED"));
        verify(evidence, never()).append(any(), anyList());
    }

    @Test
    void anAttemptWithNoRecordedAnswerAtAllCannotBeSubmitted() {
        when(speaking.findLatestForAttempt(userId, attemptId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(userId, attemptId, command()))
                .isInstanceOfSatisfying(LearningRequestException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("SPEAKING_NOT_GRADED"));
    }

    @Test
    void aGradedAnswerWritesLessonSpeakingEvidenceOnTheItemsKnowledgePointAtTheBandItReached() {
        answer(SpeakingSubmissionStatus.GRADED, "6.5", true);

        var result = useCase.execute(userId, attemptId, command());

        assertThat(result.countedAsEvidence()).isTrue();
        @SuppressWarnings("unchecked") ArgumentCaptor<List<KnowledgeEvidence>> rows = ArgumentCaptor.forClass(List.class);
        verify(evidence).append(eq(userId), rows.capture());
        assertThat(rows.getValue()).singleElement().satisfies(row -> {
            assertThat(row.knowledgePointId()).isEqualTo(kp);
            assertThat(row.source()).isEqualTo(EvidenceSource.LESSON_SPEAKING);
            assertThat(row.correct()).isTrue();
        });
        verify(mastery).recordEvidence(eq(userId), anyList(), any());
    }

    @Test
    void aBandBelowThePassBandIsEvidenceOfAWeakPointButCostsNeitherAHeartNorAReview() {
        answer(SpeakingSubmissionStatus.GRADED, "4.5", true);

        var result = useCase.execute(userId, attemptId, command());

        assertThat(result.passed()).isFalse();
        @SuppressWarnings("unchecked") ArgumentCaptor<List<KnowledgeEvidence>> rows = ArgumentCaptor.forClass(List.class);
        verify(evidence).append(eq(userId), rows.capture());
        assertThat(rows.getValue()).singleElement().satisfies(row -> assertThat(row.correct()).isFalse());
        verify(hearts).lose(userId, 0);
        verify(reviews, never()).insertPending(any(), anyList(), org.mockito.ArgumentMatchers.anyBoolean(), anyString());
    }

    @Test
    void speechTheGraderCouldNotTrustMovesNoMastery() {
        answer(SpeakingSubmissionStatus.GRADED, "6.5", false);

        useCase.execute(userId, attemptId, command());

        @SuppressWarnings("unchecked") ArgumentCaptor<List<KnowledgeEvidence>> rows = ArgumentCaptor.forClass(List.class);
        verify(evidence).append(eq(userId), rows.capture());
        assertThat(rows.getValue()).isEmpty();
    }

    @Test
    void anAnswerOfAnotherAttemptDoesNotCount() {
        SpeakingSubmission other = mock(SpeakingSubmission.class);
        when(other.status()).thenReturn(SpeakingSubmissionStatus.GRADED);
        when(other.recordingFor(questionId)).thenReturn(Optional.empty());
        when(speaking.findLatestForAttempt(userId, attemptId)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> useCase.execute(userId, attemptId, command()))
                .isInstanceOf(LearningRequestException.class);
    }

    @Test
    void theSubmissionOfAnAttemptKeepsItsLessonAndAttemptSoOnlyThatGradeIsEverApplied() {
        SpeakingSubmission created = SpeakingSubmission.start(UUID.randomUUID(), userId, UUID.randomUUID(), versionId,
                SpeakingScope.FULL, 5, Instant.now(), List.of(SpeakingRecording.expected(questionId, UUID.randomUUID(), 1, 1)),
                Instant.now()).forPracticeAttempt(lessonId, attemptId);

        assertThat(created.lessonId()).isEqualTo(lessonId);
        assertThat(created.practiceAttemptId()).isEqualTo(attemptId);
        assertThat(SpeakingSubmission.start(UUID.randomUUID(), userId, UUID.randomUUID(), versionId, SpeakingScope.FULL, 5,
                Instant.now(), List.of(SpeakingRecording.expected(questionId, UUID.randomUUID(), 1, 1)), Instant.now())
                .practiceAttemptId()).isNull();
    }

    @Test
    void eachCriterionIsEvidenceForItsOwnPointAtTheItemsPassBand() {
        answerWithSignals(Map.of("FC", "6.5", "LR", "6.0", "GRA", "5.5", "PR", "7.0"), criterionPoints(), List.of(), true);

        useCase.execute(userId, attemptId, command());

        Map<UUID, Boolean> byPoint = new java.util.HashMap<>();
        appended().forEach(row -> {
            assertThat(row.source()).isEqualTo(EvidenceSource.LESSON_SPEAKING);
            byPoint.put(row.knowledgePointId(), row.correct());
        });
        assertThat(byPoint).containsEntry(fcKp, true).containsEntry(lrKp, true).containsEntry(graKp, false)
                .containsEntry(prKp, true).containsEntry(kp, true).hasSize(5);
    }

    @Test
    void theCriteriaBelowThePassBandAreOfferedForOptionalReviewsAndTheOnesThatPassedAreNot() {
        answerWithSignals(Map.of("FC", "4.5", "LR", "6.5", "GRA", "5.0", "PR", "6.0"), criterionPoints(), List.of(), true);

        useCase.execute(userId, attemptId, command());

        assertThat(failedForSpeaking()).extracting(FailedWritingCriterion::knowledgePointId, FailedWritingCriterion::code)
                .containsExactlyInAnyOrder(org.assertj.core.groups.Tuple.tuple(fcKp, "FC"),
                        org.assertj.core.groups.Tuple.tuple(graKp, "GRA"));
    }

    @Test
    void aCriterionWeakAgainWhileItsReviewWaitsStaysOptionalBecauseSpeakingNeverEscalates() {
        answerWithSignals(Map.of("FC", "4.5", "LR", "6.5", "GRA", "6.0", "PR", "6.0"), criterionPoints(), List.of(), true);

        var result = useCase.execute(userId, attemptId, command());

        assertThat(failedForSpeaking()).extracting(FailedWritingCriterion::knowledgePointId).containsExactly(fcKp);
        verify(reviews, never()).promoteCriterionReviewsToBlocking(any(), any(), any());
        assertThat(result.reviewsCreated()).isEmpty();
    }

    @Test
    void anAnswerThatPassesEveryCriterionTouchesNoReview() {
        answerWithSignals(Map.of("FC", "6.0", "LR", "6.0", "GRA", "6.0", "PR", "6.0"), criterionPoints(), List.of(), true);

        useCase.execute(userId, attemptId, command());

        assertThat(failedForSpeaking()).isEmpty();
        verify(reviews, never()).promoteCriterionReviewsToBlocking(any(), any(), any());
    }

    @Test
    void aCriterionWithNoPointInTheRubricIsNeitherEvidenceNorReview() {
        answerWithSignals(Map.of("FC", "4.0", "PR", "4.0"), Map.of("PR", prKp), List.of(), true);

        useCase.execute(userId, attemptId, command());

        assertThat(appended()).extracting(KnowledgeEvidence::knowledgePointId).containsExactlyInAnyOrder(kp, prKp);
        assertThat(failedForSpeaking()).extracting(FailedWritingCriterion::knowledgePointId).containsExactly(prKp);
    }

    @Test
    void speechTheGraderCouldNotTrustGivesNeitherCriterionEvidenceNorReviewsNorTraps() {
        when(errorTraps.loadIfNeeded(anyBoolean())).thenReturn(
                new ErrorTrapCatalog.ErrorTraps(Map.of("SPK_NO_REASON", trapKp), Map.of(), Map.of()));
        answerWithSignals(Map.of("FC", "4.0", "LR", "4.0", "GRA", "4.0", "PR", "4.0"), criterionPoints(),
                List.of("SPK_NO_REASON"), false);

        useCase.execute(userId, attemptId, command());

        assertThat(appended()).isEmpty();
        assertThat(failedForSpeaking()).isEmpty();
        verify(reviews, never()).promoteCriterionReviewsToBlocking(any(), any(), any());
    }

    @Test
    void aSecondSubmissionOfACountedSetMakesNoEvidenceAndNoReview() {
        when(attempts.revealedPackageIds(userId)).thenReturn(Set.of(packageId));
        answerWithSignals(Map.of("FC", "4.0", "LR", "4.0", "GRA", "4.0", "PR", "4.0"), criterionPoints(),
                List.of("SPK_NO_REASON"), true);

        var result = useCase.execute(userId, attemptId, command());

        assertThat(result.countedAsEvidence()).isFalse();
        assertThat(appended()).isEmpty();
        verify(criterionReviews, never()).createForSpeaking(any(), any(), any());
        verify(reviews, never()).promoteCriterionReviewsToBlocking(any(), any(), any());
    }

    @Test
    void eachMarkedErrorWithATrapPointIsEvidenceAgainstThatPointOnceAndTheRestAreIgnored() {
        UUID other = UUID.randomUUID();
        when(errorTraps.loadIfNeeded(anyBoolean())).thenReturn(new ErrorTrapCatalog.ErrorTraps(
                Map.of("SPK_NO_REASON", trapKp, "SPK_NO_LINKER", other), Map.of(), Map.of()));
        answerWithSignals(Map.of("FC", "6.0"), criterionPoints(),
                List.of("SPK_NO_REASON", "SPK_NO_REASON", "SPK_UNKNOWN", "SPK_NO_LINKER"), true);

        useCase.execute(userId, attemptId, command());

        var traps = appended().stream().filter(row -> row.source() == EvidenceSource.ERROR_TRAP).toList();
        assertThat(traps).extracting(KnowledgeEvidence::knowledgePointId).containsExactlyInAnyOrder(trapKp, other);
        assertThat(traps).allSatisfy(row -> assertThat(row.correct()).isFalse());
        verify(errorTraps).createReviews(eq(userId), eq(lessonId), anyList());
    }

    @Test
    void anAnswerWithNoErrorsOrCriteriaInItsSignalsStillWorksAsBefore() {
        answer(SpeakingSubmissionStatus.GRADED, "6.5", true);

        useCase.execute(userId, attemptId, command());

        assertThat(appended()).singleElement().satisfies(row -> assertThat(row.knowledgePointId()).isEqualTo(kp));
        assertThat(failedForSpeaking()).isEmpty();
    }
}
