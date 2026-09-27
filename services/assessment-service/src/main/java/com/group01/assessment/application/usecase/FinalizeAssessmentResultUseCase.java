package com.group01.assessment.application.usecase;

import com.group01.assessment.application.command.FinalizeAssessmentResultCommand;
import com.group01.assessment.application.event.AssessmentCompletedEventFactory;
import com.group01.assessment.application.event.AssessmentCompletedV2;
import com.group01.assessment.application.result.AssessmentResultResult;
import com.group01.assessment.domain.entity.AssessmentResult;
import com.group01.assessment.domain.entity.AttemptItem;
import com.group01.assessment.domain.entity.ItemResult;
import com.group01.assessment.domain.entity.OutboxEvent;
import com.group01.assessment.domain.exception.AssessmentNotFoundException;
import com.group01.assessment.domain.exception.InvalidAssessmentStateException;
import com.group01.assessment.domain.repository.*;
import com.group01.assessment.domain.vo.AttemptStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Makes one fully graded result version final. The COMPLETED transition and the AssessmentCompleted.v2 outbox
 * row commit in the same transaction, so the event exists if and only if the finalized result exists.
 */
@Service
public class FinalizeAssessmentResultUseCase {
    public static final String AGGREGATE_TYPE = "AssessmentResult";
    private static final Logger log = LoggerFactory.getLogger(FinalizeAssessmentResultUseCase.class);

    private final AssessmentResultRepository results;
    private final AssessmentAttemptRepository attempts;
    private final AttemptItemRepository attemptItems;
    private final ItemResultRepository itemResults;
    private final AttemptItemKnowledgePointRepository knowledgeSnapshot;
    private final ItemResultKnowledgeJudgmentRepository judgments;
    private final ErrorAnalysisItemRepository errors;
    private final OutboxEventRepository outbox;
    private final AssessmentCompletedEventFactory events;

    public FinalizeAssessmentResultUseCase(AssessmentResultRepository results,
                                           AssessmentAttemptRepository attempts,
                                           AttemptItemRepository attemptItems,
                                           ItemResultRepository itemResults,
                                           AttemptItemKnowledgePointRepository knowledgeSnapshot,
                                           ItemResultKnowledgeJudgmentRepository judgments,
                                           ErrorAnalysisItemRepository errors,
                                           OutboxEventRepository outbox,
                                           AssessmentCompletedEventFactory events) {
        this.results = results;
        this.attempts = attempts;
        this.attemptItems = attemptItems;
        this.itemResults = itemResults;
        this.knowledgeSnapshot = knowledgeSnapshot;
        this.judgments = judgments;
        this.errors = errors;
        this.outbox = outbox;
        this.events = events;
    }

    @Transactional
    public AssessmentResultResult execute(FinalizeAssessmentResultCommand command) {
        AssessmentResult result = results.findForUpdateById(command.resultId())
                .orElseThrow(() -> new AssessmentNotFoundException("Assessment result not found"));
        if (result.isCompleted()) {
            // Already final: its outbox row was written by the transaction that completed it.
            return toResult(result);
        }
        AssessmentResult latest = results.findLatestByAttemptId(result.attemptId()).orElse(result);
        if (latest.resultVersion() != result.resultVersion()) {
            throw new InvalidAssessmentStateException("Only the latest result version can be finalized");
        }
        var attempt = attempts.findById(result.attemptId())
                .orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));
        if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
            throw new InvalidAssessmentStateException("Only a submitted attempt can have a final result");
        }

        List<AttemptItem> items = attemptItems.findByAttemptId(attempt.getId());
        List<ItemResult> graded = itemResults.findByResultId(result.id());
        requireFullyGraded(items, graded);

        Instant now = Instant.now();
        AssessmentResult completed = results.save(result.complete(now));
        if (attempt.getLearningGoalId() == null) {
            log.info("Finalized result {} without AssessmentCompleted.v2: attempt {} has no learning goal",
                    completed.id(), attempt.getId());
            return toResult(completed);
        }

        List<UUID> itemIds = items.stream().map(AttemptItem::id).toList();
        List<UUID> itemResultIds = graded.stream().map(ItemResult::id).toList();
        UUID eventId = UUID.randomUUID();
        var event = events.create(eventId, now, attempt, completed, items, graded,
                knowledgeSnapshot.findByAttemptItemIds(itemIds), judgments.findByItemResultIds(itemResultIds),
                errors.findByResultId(completed.id()));
        outbox.save(OutboxEvent.pending(eventId, AGGREGATE_TYPE, completed.id().toString(),
                AssessmentCompletedV2.EVENT_TYPE, events.toJson(event), now));
        return toResult(completed);
    }

    private static void requireFullyGraded(List<AttemptItem> items, List<ItemResult> graded) {
        if (items.isEmpty()) {
            throw new InvalidAssessmentStateException("An attempt without items cannot be finalized");
        }
        Map<UUID, ItemResult> byItem = graded.stream()
                .collect(Collectors.toMap(ItemResult::attemptItemId, Function.identity()));
        for (AttemptItem item : items) {
            ItemResult itemResult = byItem.get(item.id());
            if (itemResult == null) {
                throw new InvalidAssessmentStateException("Every assessment item must be graded before finalization");
            }
            if (itemResult.score() == null || itemResult.maxScore() == null) {
                throw new InvalidAssessmentStateException("Every graded item must record its score and maximum score");
            }
        }
    }

    private static AssessmentResultResult toResult(AssessmentResult result) {
        return new AssessmentResultResult(result.id(), result.attemptId(), result.resultVersion(), result.status(),
                result.overallBand(), result.completedAt());
    }
}
