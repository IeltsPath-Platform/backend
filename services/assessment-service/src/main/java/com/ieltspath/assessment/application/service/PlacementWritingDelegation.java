package com.ieltspath.assessment.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.PlacementWritingGradingStore;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.events.assessment.PlacementWritingGradingRequestedV1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Asks Learning to grade placement essays with the grader of course and free practice. */
@Component
public class PlacementWritingDelegation {
    private final PlacementWritingGradingStore store;
    private final ObjectMapper json;
    private final boolean enabled;

    public PlacementWritingDelegation(PlacementWritingGradingStore store, ObjectMapper json,
                                      @Value("${assessment.placement.writing-grading.enabled:false}") boolean enabled) {
        this.store = store;
        this.json = json;
        this.enabled = enabled;
    }

    /** Call after the essay jobs are queued; essays whose prompt Learning cannot use stay with the local grader. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void delegate(AssessmentAttempt attempt) {
        if (!enabled) return;
        List<PlacementWritingGradingRequestedV1.Item> items = new ArrayList<>();
        for (var essay : store.queued(attempt.getId(), attempt.getUserId())) {
            var item = item(essay);
            if (item != null) items.add(item);
        }
        if (items.isEmpty()) return;
        Instant now = Instant.now();
        store.delegate(new PlacementWritingGradingRequestedV1(UUID.randomUUID(),
                PlacementWritingGradingRequestedV1.EVENT_TYPE, now, PlacementWritingGradingRequestedV1.SOURCE,
                new PlacementWritingGradingRequestedV1.Data(attempt.getId(), attempt.getUserId(), items)), now);
    }

    private PlacementWritingGradingRequestedV1.Item item(PlacementWritingGradingStore.Essay essay) {
        try {
            JsonNode question = json.readTree(essay.questionSnapshot());
            JsonNode spec = json.readTree(essay.answerSnapshot()).path("answerSpec");
            String stem = question.path("stem").asText(null);
            String task = spec.path("task").asText(null);
            String chartFacts = spec.path("chartFacts").asText(null);
            if (stem == null || task == null || "TASK_1".equals(task) && (chartFacts == null || chartFacts.isBlank())) {
                return null;
            }
            return new PlacementWritingGradingRequestedV1.Item(essay.attemptItemId(), essay.questionVersionId(), stem,
                    task, spec.path("minWords").isNumber() ? spec.get("minWords").asInt() : null,
                    spec.path("passBand").isNumber() ? new BigDecimal(spec.get("passBand").asText()) : null,
                    chartFacts, essay.knowledgePointIds(), essay.text());
        } catch (Exception unreadable) {
            return null;
        }
    }
}
