package com.group01.assessment.application.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.assessment.domain.aggregate.AssessmentAttempt;
import com.group01.assessment.domain.entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** Builds the AssessmentCompleted.v2 payload from the finalized result and the attempt's stored snapshot. */
@Component
public class AssessmentCompletedEventFactory {
    private final ObjectMapper objectMapper;

    public AssessmentCompletedEventFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AssessmentCompletedV2 create(UUID eventId, Instant occurredAt, AssessmentAttempt attempt,
                                        AssessmentResult result, List<AttemptItem> items,
                                        List<ItemResult> itemResults,
                                        List<AttemptItemKnowledgePoint> snapshot,
                                        List<ItemResultKnowledgeJudgment> judgments,
                                        List<ErrorAnalysisItem> errors) {
        Objects.requireNonNull(attempt.getLearningGoalId(), "An attempt without a learning goal cannot be announced");
        Map<UUID, ItemResult> resultByItem = itemResults.stream()
                .collect(Collectors.toMap(ItemResult::attemptItemId, value -> value));
        Map<UUID, List<AttemptItemKnowledgePoint>> snapshotByItem = snapshot.stream()
                .collect(Collectors.groupingBy(AttemptItemKnowledgePoint::attemptItemId));
        Map<List<UUID>, String> judgmentByItemAndPoint = judgments.stream()
                .collect(Collectors.toMap(value -> List.of(value.itemResultId(), value.knowledgePointId()),
                        value -> value.judgment().name()));
        Map<List<UUID>, String> errorByItemAndPoint = new HashMap<>();
        for (ErrorAnalysisItem error : errors) {
            if (error.knowledgePointId() != null) {
                errorByItemAndPoint.putIfAbsent(List.of(error.itemResultId(), error.knowledgePointId()), error.errorType());
            }
        }

        List<AssessmentCompletedV2.ItemResult> eventItems = new ArrayList<>();
        for (AttemptItem item : items) {
            ItemResult graded = resultByItem.get(item.id());
            if (graded == null) {
                throw new IllegalStateException("Every assessment item must be graded before it is announced");
            }
            List<AssessmentCompletedV2.KnowledgePointMapping> mappings = snapshotByItem
                    .getOrDefault(item.id(), List.of()).stream()
                    .map(mapping -> {
                        List<UUID> key = List.of(graded.id(), mapping.knowledgePointId());
                        return new AssessmentCompletedV2.KnowledgePointMapping(mapping.knowledgePointId(),
                                mapping.weight(), judgmentByItemAndPoint.get(key), errorByItemAndPoint.get(key));
                    })
                    .toList();
            eventItems.add(new AssessmentCompletedV2.ItemResult(graded.id(), item.questionVersionId(), graded.correct(),
                    decimal(graded.score()), decimal(graded.maxScore()), mappings));
        }

        return new AssessmentCompletedV2(eventId, AssessmentCompletedV2.EVENT_TYPE, occurredAt,
                AssessmentCompletedV2.SOURCE, new AssessmentCompletedV2.Data(attempt.getUserId(),
                attempt.getLearningGoalId(), attempt.getId(), result.id(), result.resultVersion(),
                attempt.getAttemptType().name(), result.status(), result.completedAt(), decimal(result.overallBand()),
                List.copyOf(eventItems)));
    }

    public String toJson(AssessmentCompletedV2 event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AssessmentCompleted.v2 could not be serialized", exception);
        }
    }

    private static BigDecimal decimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
