package com.group01.assessment.application.usecase;

import com.group01.assessment.application.event.AssessmentCompletedEventFactory;
import com.group01.assessment.application.event.AssessmentCompletedV2;
import com.group01.assessment.domain.aggregate.AssessmentAttempt;
import com.group01.assessment.domain.entity.AssessmentResult;
import com.group01.assessment.domain.entity.AttemptItem;
import com.group01.assessment.domain.entity.ItemResult;
import com.group01.assessment.domain.entity.OutboxEvent;
import com.group01.assessment.domain.exception.InvalidAssessmentStateException;
import com.group01.assessment.domain.repository.AssessmentResultRepository;
import com.group01.assessment.domain.repository.AttemptItemKnowledgePointRepository;
import com.group01.assessment.domain.repository.ErrorAnalysisItemRepository;
import com.group01.assessment.domain.repository.ItemResultKnowledgeJudgmentRepository;
import com.group01.assessment.domain.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Makes a fully graded result version COMPLETED and records its AssessmentCompleted.v2 outbox row. It joins the
 * caller's transaction, so the event exists if and only if the completed result exists. Used by both the grader's
 * finalize and the auto-grader at submit.
 */
@Component
public class AssessmentResultCompleter {
    public static final String AGGREGATE_TYPE = "AssessmentResult";

    private final AssessmentResultRepository results;
    private final AttemptItemKnowledgePointRepository knowledgeSnapshot;
    private final ItemResultKnowledgeJudgmentRepository judgments;
    private final ErrorAnalysisItemRepository errors;
    private final OutboxEventRepository outbox;
    private final AssessmentCompletedEventFactory events;

    public AssessmentResultCompleter(AssessmentResultRepository results,
                                     AttemptItemKnowledgePointRepository knowledgeSnapshot,
                                     ItemResultKnowledgeJudgmentRepository judgments,
                                     ErrorAnalysisItemRepository errors,
                                     OutboxEventRepository outbox,
                                     AssessmentCompletedEventFactory events) {
        this.results = results;
        this.knowledgeSnapshot = knowledgeSnapshot;
        this.judgments = judgments;
        this.errors = errors;
        this.outbox = outbox;
        this.events = events;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AssessmentResult complete(AssessmentAttempt attempt, AssessmentResult result, List<AttemptItem> items,
                                     List<ItemResult> graded) {
        requireFullyGraded(items, graded);
        Instant now = Instant.now();
        AssessmentResult completed = results.save(result.complete(now));

        List<UUID> itemIds = items.stream().map(AttemptItem::id).toList();
        List<UUID> itemResultIds = graded.stream().map(ItemResult::id).toList();
        UUID eventId = UUID.randomUUID();
        var event = events.create(eventId, now, attempt, completed, items, graded,
                knowledgeSnapshot.findByAttemptItemIds(itemIds), judgments.findByItemResultIds(itemResultIds),
                errors.findByResultId(completed.id()));
        outbox.save(OutboxEvent.pending(eventId, AGGREGATE_TYPE, completed.id().toString(),
                AssessmentCompletedV2.EVENT_TYPE, events.toJson(event), now));
        return completed;
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
}
