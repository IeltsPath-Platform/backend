package com.group01.assessment.application.usecase;

import com.group01.assessment.application.command.ItemResultInput;
import com.group01.assessment.application.command.KnowledgeJudgmentInput;
import com.group01.assessment.application.command.SaveAssessmentResultDetailsCommand;
import com.group01.assessment.application.command.SaveGradingDetailsCommand;
import com.group01.assessment.domain.aggregate.AssessmentAttempt;
import com.group01.assessment.domain.entity.AssessmentResult;
import com.group01.assessment.domain.entity.AttemptItemKnowledgePoint;
import com.group01.assessment.domain.entity.ItemResult;
import com.group01.assessment.domain.exception.AssessmentNotFoundException;
import com.group01.assessment.domain.exception.InvalidAssessmentStateException;
import com.group01.assessment.domain.repository.*;
import com.group01.assessment.domain.vo.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaveAssessmentResultDetailsUseCaseTest {
    @Mock AssessmentAttemptRepository attempts;
    @Mock AssessmentResultRepository results;
    @Mock AttemptItemRepository attemptItems;
    @Mock SkillScoreRepository skills;
    @Mock ItemResultRepository items;
    @Mock ErrorAnalysisItemRepository errors;
    @Mock AttemptItemKnowledgePointRepository knowledgeSnapshot;
    @Mock ItemResultKnowledgeJudgmentRepository judgments;

    private final UUID userId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();
    private final UUID resultId = UUID.randomUUID();
    private final UUID attemptItemId = UUID.randomUUID();

    @Test
    void finalizedResultIsImmutable() {
        stubAttempt();
        when(results.findLatestForUpdateByAttemptId(attemptId)).thenReturn(Optional.of(
                new AssessmentResult(resultId, attemptId, 1, AssessmentResult.COMPLETED, 6.0, Instant.now())));

        assertThrows(InvalidAssessmentStateException.class, () -> useCase().execute(command(
                List.of(new ItemResultInput(attemptItemId, 1.0, 1.0, true, null, "{}")), null)));

        verifyNoInteractions(items, judgments);
    }

    @Test
    void awardedScoreCannotExceedMaximumScore() {
        stubAttempt();
        stubDraft();
        when(attemptItems.findExistingIdsByAttemptId(Set.of(attemptItemId), attemptId)).thenReturn(Set.of(attemptItemId));

        assertThrows(InvalidAssessmentStateException.class, () -> useCase().execute(command(
                List.of(new ItemResultInput(attemptItemId, 3.0, 2.0, true, null, "{}")), null)));

        verify(items, never()).saveAll(any());
    }

    @Test
    void judgmentMustTargetAKnowledgePointSnapshottedOnTheItem() {
        stubAttempt();
        stubDraft();
        UUID snapshotted = UUID.randomUUID();
        when(items.findByResultId(resultId)).thenReturn(List.of(
                new ItemResult(UUID.randomUUID(), resultId, attemptItemId, 5.0, 9.0, null, null, "{}")));
        when(knowledgeSnapshot.findByAttemptItemIds(Set.of(attemptItemId))).thenReturn(List.of(
                new AttemptItemKnowledgePoint(attemptItemId, snapshotted, BigDecimal.ONE)));

        assertThrows(InvalidAssessmentStateException.class, () -> useCase().execute(command(null, List.of(
                new KnowledgeJudgmentInput(attemptItemId, UUID.randomUUID(), QualitativeJudgment.PASS)))));

        verify(judgments, never()).saveAll(any());
    }

    @Test
    void explicitJudgmentForSnapshottedKnowledgePointIsStored() {
        stubAttempt();
        stubDraft();
        UUID knowledgePointId = UUID.randomUUID();
        UUID itemResultId = UUID.randomUUID();
        when(items.findByResultId(resultId)).thenReturn(List.of(
                new ItemResult(itemResultId, resultId, attemptItemId, 5.0, 9.0, null, null, "{}")));
        when(knowledgeSnapshot.findByAttemptItemIds(Set.of(attemptItemId))).thenReturn(List.of(
                new AttemptItemKnowledgePoint(attemptItemId, knowledgePointId, BigDecimal.ONE)));

        useCase().execute(command(null, List.of(
                new KnowledgeJudgmentInput(attemptItemId, knowledgePointId, QualitativeJudgment.FAIL))));

        verify(judgments).saveAll(List.of(new com.group01.assessment.domain.entity.ItemResultKnowledgeJudgment(
                itemResultId, knowledgePointId, QualitativeJudgment.FAIL)));
    }

    @Test
    void learnerCannotSaveDetailsForAnotherLearnersAttempt() {
        when(attempts.findByIdAndUserId(attemptId, userId)).thenReturn(Optional.empty());

        assertThrows(AssessmentNotFoundException.class, () -> useCase().execute(command(
                List.of(new ItemResultInput(attemptItemId, 1.0, 1.0, true, null, "{}")), null)));

        verifyNoInteractions(results, items, judgments);
    }

    @Test
    void learnerFlowKeepsTheBandOfTheResult() {
        stubAttempt();
        when(results.findLatestForUpdateByAttemptId(attemptId)).thenReturn(Optional.of(
                new AssessmentResult(resultId, attemptId, 1, AssessmentResult.DRAFT, 9.0, null)));
        when(attemptItems.findExistingIdsByAttemptId(Set.of(attemptItemId), attemptId)).thenReturn(Set.of(attemptItemId));

        useCase().execute(command(List.of(new ItemResultInput(attemptItemId, 1.0, 1.0, true, null, "{}")), null));

        verify(results, never()).save(any());
    }

    @Test
    void graderSavesDetailsByResultWithoutALearnerAndOverwritesTheSelfDeclaredBand() {
        stubGraderResult(AssessmentResult.DRAFT, 9.0, 1);
        when(attemptItems.findExistingIdsByAttemptId(Set.of(attemptItemId), attemptId)).thenReturn(Set.of(attemptItemId));
        when(results.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase().executeForGrader(graderCommand(6.5,
                List.of(new ItemResultInput(attemptItemId, 1.0, 1.0, true, null, "{}")), null));

        verify(items).saveAll(any());
        verify(results).save(new AssessmentResult(resultId, attemptId, 1, AssessmentResult.DRAFT, 6.5, null));
        verify(attempts, never()).findByIdAndUserId(any(), any());
    }

    @Test
    void graderSendingNoBandClearsTheBand() {
        stubGraderResult(AssessmentResult.PROCESSING, 9.0, 1);
        when(results.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase().executeForGrader(graderCommand(null, null, null));

        verify(results).save(new AssessmentResult(resultId, attemptId, 1, AssessmentResult.PROCESSING, null, null));
    }

    @Test
    void graderCannotChangeAFinalizedResult() {
        stubGraderResult(AssessmentResult.COMPLETED, 6.0, 1);

        assertThrows(InvalidAssessmentStateException.class, () -> useCase().executeForGrader(graderCommand(7.0,
                List.of(new ItemResultInput(attemptItemId, 1.0, 1.0, true, null, "{}")), null)));

        verify(results, never()).save(any());
        verifyNoInteractions(items);
    }

    @Test
    void graderCanOnlyGradeTheLatestVersion() {
        when(results.findForUpdateById(resultId)).thenReturn(Optional.of(
                new AssessmentResult(resultId, attemptId, 1, AssessmentResult.DRAFT, null, null)));
        when(results.findLatestByAttemptId(attemptId)).thenReturn(Optional.of(
                new AssessmentResult(UUID.randomUUID(), attemptId, 2, AssessmentResult.DRAFT, null, null)));

        assertThrows(InvalidAssessmentStateException.class, () -> useCase().executeForGrader(graderCommand(7.0, null, null)));

        verify(results, never()).save(any());
    }

    @Test
    void graderSavingAnUnknownResultIsNotFound() {
        when(results.findForUpdateById(resultId)).thenReturn(Optional.empty());

        assertThrows(AssessmentNotFoundException.class, () -> useCase().executeForGrader(graderCommand(7.0, null, null)));
    }

    @Test
    void graderDetailsUseTheSameScoreValidation() {
        stubGraderResult(AssessmentResult.DRAFT, null, 1);
        when(attemptItems.findExistingIdsByAttemptId(Set.of(attemptItemId), attemptId)).thenReturn(Set.of(attemptItemId));

        assertThrows(InvalidAssessmentStateException.class, () -> useCase().executeForGrader(graderCommand(7.0,
                List.of(new ItemResultInput(attemptItemId, 3.0, 2.0, true, null, "{}")), null)));

        verify(results, never()).save(any());
    }

    private void stubGraderResult(String status, Double band, int version) {
        AssessmentResult result = new AssessmentResult(resultId, attemptId, version, status, band,
                AssessmentResult.COMPLETED.equals(status) ? Instant.now() : null);
        when(results.findForUpdateById(resultId)).thenReturn(Optional.of(result));
        lenient().when(results.findLatestByAttemptId(attemptId)).thenReturn(Optional.of(result));
    }

    private SaveGradingDetailsCommand graderCommand(Double overallBand, List<ItemResultInput> itemResults,
                                                    List<KnowledgeJudgmentInput> knowledgeJudgments) {
        return new SaveGradingDetailsCommand(resultId, overallBand, null, itemResults, null, knowledgeJudgments);
    }

    private void stubAttempt() {
        Instant now = Instant.now();
        when(attempts.findByIdAndUserId(attemptId, userId)).thenReturn(Optional.of(new AssessmentAttempt(attemptId,
                userId, UUID.randomUUID(), AttemptType.MOCK, AttemptMode.STANDARD, AttemptChannel.WEB,
                AttemptStatus.SUBMITTED, now, now, null, 1, now, now, UUID.randomUUID())));
    }

    private void stubDraft() {
        when(results.findLatestForUpdateByAttemptId(attemptId)).thenReturn(Optional.of(
                new AssessmentResult(resultId, attemptId, 1, AssessmentResult.DRAFT, null, null)));
    }

    private SaveAssessmentResultDetailsCommand command(List<ItemResultInput> itemResults,
                                                       List<KnowledgeJudgmentInput> knowledgeJudgments) {
        return new SaveAssessmentResultDetailsCommand(userId, attemptId, null, itemResults, null, knowledgeJudgments);
    }

    private SaveAssessmentResultDetailsUseCase useCase() {
        return new SaveAssessmentResultDetailsUseCase(attempts, results, attemptItems, skills, items, errors,
                knowledgeSnapshot, judgments);
    }
}
