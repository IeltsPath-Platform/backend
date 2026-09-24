package com.group01.assessment.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.group01.assessment.application.command.FinalizeAssessmentResultCommand;
import com.group01.assessment.application.event.AssessmentCompletedEventFactory;
import com.group01.assessment.domain.aggregate.AssessmentAttempt;
import com.group01.assessment.domain.entity.*;
import com.group01.assessment.domain.exception.InvalidAssessmentStateException;
import com.group01.assessment.domain.repository.*;
import com.group01.assessment.domain.vo.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinalizeAssessmentResultUseCaseTest {
    @Mock AssessmentResultRepository results;
    @Mock AssessmentAttemptRepository attempts;
    @Mock AttemptItemRepository attemptItems;
    @Mock ItemResultRepository itemResults;
    @Mock AttemptItemKnowledgePointRepository knowledgeSnapshot;
    @Mock ItemResultKnowledgeJudgmentRepository judgments;
    @Mock ErrorAnalysisItemRepository errors;
    @Mock OutboxEventRepository outbox;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
    private final UUID userId = UUID.randomUUID();
    private final UUID goalId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();
    private final UUID resultId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();
    private final UUID itemResultId = UUID.randomUUID();
    private final UUID questionVersionId = UUID.randomUUID();
    private final UUID memoryPoint = UUID.randomUUID();
    private final UUID conceptPoint = UUID.randomUUID();
    private final AssessmentResult draft = new AssessmentResult(resultId, attemptId, 1, AssessmentResult.DRAFT, 6.5, null);

    @Test
    void finalizesGradedResultAndRecordsSelfContainedEventInTheSameUnitOfWork() throws Exception {
        stubGradedAttempt(goalId);
        when(knowledgeSnapshot.findByAttemptItemIds(List.of(itemId))).thenReturn(List.of(
                new AttemptItemKnowledgePoint(itemId, memoryPoint, new BigDecimal("0.60")),
                new AttemptItemKnowledgePoint(itemId, conceptPoint, new BigDecimal("0.40"))));
        when(judgments.findByItemResultIds(List.of(itemResultId))).thenReturn(List.of(
                new ItemResultKnowledgeJudgment(itemResultId, conceptPoint, QualitativeJudgment.PASS)));
        when(errors.findByResultId(resultId)).thenReturn(List.of());

        var finalized = useCase().execute(new FinalizeAssessmentResultCommand(resultId));

        assertEquals(AssessmentResult.COMPLETED, finalized.status());
        assertNotNull(finalized.completedAt());
        InOrder order = inOrder(results, outbox);
        order.verify(results).save(argThat(AssessmentResult::isCompleted));
        ArgumentCaptor<OutboxEvent> event = ArgumentCaptor.forClass(OutboxEvent.class);
        order.verify(outbox).save(event.capture());
        assertEquals("AssessmentCompleted.v2", event.getValue().eventType());
        assertEquals(resultId.toString(), event.getValue().aggregateId());

        JsonNode json = objectMapper.readTree(event.getValue().payload());
        assertEquals(event.getValue().id().toString(), json.get("event_id").asText());
        assertEquals("AssessmentCompleted.v2", json.get("event_type").asText());
        assertEquals("assessment-service", json.get("source").asText());
        JsonNode data = json.get("data");
        assertEquals(userId.toString(), data.get("user_id").asText());
        assertEquals(goalId.toString(), data.get("learning_goal_id").asText());
        assertEquals(attemptId.toString(), data.get("attempt_id").asText());
        assertEquals(resultId.toString(), data.get("result_id").asText());
        assertEquals(1, data.get("result_version").asInt());
        assertEquals("MOCK", data.get("assessment_type").asText());
        assertEquals("COMPLETED", data.get("status").asText());
        JsonNode item = data.get("item_results").get(0);
        assertEquals(itemResultId.toString(), item.get("item_result_id").asText());
        assertEquals(questionVersionId.toString(), item.get("question_version_id").asText());
        assertTrue(item.get("is_correct").asBoolean());
        assertEquals(1.0, item.get("score").asDouble());
        assertEquals(1.0, item.get("max_score").asDouble());
        JsonNode mappings = item.get("knowledge_point_mappings");
        assertEquals(2, mappings.size());
        assertEquals(memoryPoint.toString(), mappings.get(0).get("knowledge_point_id").asText());
        assertEquals(0.60, mappings.get(0).get("weight").asDouble());
        assertTrue(mappings.get(0).get("qualitative_judgment").isNull());
        assertEquals("PASS", mappings.get(1).get("qualitative_judgment").asText());
        assertFalse(json.toString().contains("correct_answer"));
    }

    @Test
    void partiallyGradedResultIsNeitherFinalizedNorAnnounced() {
        when(results.findForUpdateById(resultId)).thenReturn(Optional.of(draft));
        when(results.findLatestByAttemptId(attemptId)).thenReturn(Optional.of(draft));
        when(attempts.findById(attemptId)).thenReturn(Optional.of(attempt(goalId)));
        when(attemptItems.findByAttemptId(attemptId)).thenReturn(List.of(item()));
        when(itemResults.findByResultId(resultId)).thenReturn(List.of());

        assertThrows(InvalidAssessmentStateException.class,
                () -> useCase().execute(new FinalizeAssessmentResultCommand(resultId)));

        verify(results, never()).save(any());
        verifyNoInteractions(outbox);
    }

    @Test
    void itemWithoutMaximumScoreBlocksFinalization() {
        when(results.findForUpdateById(resultId)).thenReturn(Optional.of(draft));
        when(results.findLatestByAttemptId(attemptId)).thenReturn(Optional.of(draft));
        when(attempts.findById(attemptId)).thenReturn(Optional.of(attempt(goalId)));
        when(attemptItems.findByAttemptId(attemptId)).thenReturn(List.of(item()));
        when(itemResults.findByResultId(resultId)).thenReturn(List.of(
                new ItemResult(itemResultId, resultId, itemId, 1.0, null, true, 900L, "{}")));

        assertThrows(InvalidAssessmentStateException.class,
                () -> useCase().execute(new FinalizeAssessmentResultCommand(resultId)));

        verifyNoInteractions(outbox);
    }

    @Test
    void completedResultIsNotAnnouncedTwice() {
        AssessmentResult completed = new AssessmentResult(resultId, attemptId, 1, AssessmentResult.COMPLETED, 6.5,
                Instant.now());
        when(results.findForUpdateById(resultId)).thenReturn(Optional.of(completed));

        var result = useCase().execute(new FinalizeAssessmentResultCommand(resultId));

        assertEquals(AssessmentResult.COMPLETED, result.status());
        verify(results, never()).save(any());
        verifyNoInteractions(outbox);
    }

    @Test
    void attemptWithoutLearningGoalIsFinalizedButNotAttributedToAnyPath() {
        stubGradedAttempt(null);

        var result = useCase().execute(new FinalizeAssessmentResultCommand(resultId));

        assertEquals(AssessmentResult.COMPLETED, result.status());
        verify(results).save(argThat(AssessmentResult::isCompleted));
        verifyNoInteractions(outbox);
    }

    @Test
    void supersededVersionCannotBeFinalized() {
        when(results.findForUpdateById(resultId)).thenReturn(Optional.of(draft));
        when(results.findLatestByAttemptId(attemptId)).thenReturn(Optional.of(
                new AssessmentResult(UUID.randomUUID(), attemptId, 2, AssessmentResult.DRAFT, null, null)));

        assertThrows(InvalidAssessmentStateException.class,
                () -> useCase().execute(new FinalizeAssessmentResultCommand(resultId)));

        verifyNoInteractions(outbox);
    }

    private void stubGradedAttempt(UUID learningGoalId) {
        when(results.findForUpdateById(resultId)).thenReturn(Optional.of(draft));
        when(results.findLatestByAttemptId(attemptId)).thenReturn(Optional.of(draft));
        when(attempts.findById(attemptId)).thenReturn(Optional.of(attempt(learningGoalId)));
        when(attemptItems.findByAttemptId(attemptId)).thenReturn(List.of(item()));
        when(itemResults.findByResultId(resultId)).thenReturn(List.of(
                new ItemResult(itemResultId, resultId, itemId, 1.0, 1.0, true, 900L, "{}")));
        when(results.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private AttemptItem item() {
        return new AttemptItem(itemId, UUID.randomUUID(), questionVersionId, 0, "{}", null, null);
    }

    private AssessmentAttempt attempt(UUID learningGoalId) {
        Instant now = Instant.now();
        return new AssessmentAttempt(attemptId, userId, UUID.randomUUID(), AttemptType.MOCK, AttemptMode.STANDARD,
                AttemptChannel.WEB, AttemptStatus.SUBMITTED, now, now, null, 1, now, now, learningGoalId);
    }

    private FinalizeAssessmentResultUseCase useCase() {
        return new FinalizeAssessmentResultUseCase(results, attempts, attemptItems, itemResults, knowledgeSnapshot,
                judgments, errors, outbox, new AssessmentCompletedEventFactory(objectMapper));
    }
}
