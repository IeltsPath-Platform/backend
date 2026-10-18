package com.ieltspath.assessment.application.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.entity.AssessmentResult;
import com.ieltspath.assessment.domain.entity.AttemptItem;
import com.ieltspath.assessment.domain.entity.AttemptItemKnowledgePoint;
import com.ieltspath.assessment.domain.entity.ErrorAnalysisItem;
import com.ieltspath.assessment.domain.entity.ItemResult;
import com.ieltspath.assessment.domain.vo.AttemptChannel;
import com.ieltspath.assessment.domain.vo.AttemptMode;
import com.ieltspath.assessment.domain.vo.AttemptStatus;
import com.ieltspath.assessment.domain.vo.AttemptType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssessmentCompletedEventFactoryTest {
    // Same module set as the application's mapper, so Instant fields serialize.
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final AssessmentCompletedEventFactory factory = new AssessmentCompletedEventFactory(json);

    @Test
    void itemsCarryTheReportedDurationAndTheErrorTypeOfAWrongAnswer() throws Exception {
        UUID userId = UUID.randomUUID();
        AssessmentAttempt attempt = AssessmentAttempt.start(userId, UUID.randomUUID(), AttemptType.QUIZ,
                AttemptMode.STANDARD, AttemptChannel.WEB, null);
        UUID timedItem = UUID.randomUUID();
        UUID untimedItem = UUID.randomUUID();
        UUID timedResult = UUID.randomUUID();
        UUID untimedResult = UUID.randomUUID();
        UUID kp = UUID.randomUUID();
        List<AttemptItem> items = List.of(
                new AttemptItem(timedItem, UUID.randomUUID(), UUID.randomUUID(), 1, "{}", null, null),
                new AttemptItem(untimedItem, UUID.randomUUID(), UUID.randomUUID(), 2, "{}", null, null));
        List<ItemResult> results = List.of(
                new ItemResult(timedResult, UUID.randomUUID(), timedItem, 0.0, 1.0, false, 4200L, "{}"),
                new ItemResult(untimedResult, UUID.randomUUID(), untimedItem, 1.0, 1.0, true, null, "{}"));
        List<AttemptItemKnowledgePoint> snapshot = List.of(
                new AttemptItemKnowledgePoint(timedItem, kp, BigDecimal.ONE));
        List<ErrorAnalysisItem> errors = List.of(
                new ErrorAnalysisItem(UUID.randomUUID(), timedResult, kp, "PARAPHRASE_TRAP", null));
        AssessmentResult result = new AssessmentResult(UUID.randomUUID(), attempt.getId(), 1, AssessmentResult.COMPLETED,
                6.5, Instant.parse("2026-10-16T10:00:00Z"));

        AssessmentCompletedV2 event = factory.create(UUID.randomUUID(), Instant.now(), attempt, result, items, results,
                snapshot, List.of(), errors);

        AssessmentCompletedV2.ItemResult timed = event.data().itemResults().get(0);
        assertEquals(4200L, timed.durationMs());
        assertEquals("PARAPHRASE_TRAP", timed.errorCode());

        AssessmentCompletedV2.ItemResult untimed = event.data().itemResults().get(1);
        assertNull(untimed.durationMs());
        assertNull(untimed.errorCode());

        JsonNode payload = json.readTree(factory.toJson(event)).get("data").get("item_results").get(0);
        assertEquals(4200, payload.get("duration_ms").asInt());
        assertEquals("PARAPHRASE_TRAP", payload.get("error_code").asText());
    }

    @Test
    void anUntimedItemSerializesItsDurationAsNull() throws Exception {
        UUID userId = UUID.randomUUID();
        AssessmentAttempt attempt = new AssessmentAttempt(UUID.randomUUID(), userId, UUID.randomUUID(), AttemptType.QUIZ,
                AttemptMode.STANDARD, AttemptChannel.WEB, AttemptStatus.SUBMITTED, Instant.now(), Instant.now(), null, 1,
                Instant.now(), Instant.now());
        UUID item = UUID.randomUUID();
        AssessmentResult result = new AssessmentResult(UUID.randomUUID(), attempt.getId(), 1, AssessmentResult.COMPLETED,
                null, Instant.now());

        AssessmentCompletedV2 event = factory.create(UUID.randomUUID(), Instant.now(), attempt, result,
                List.of(new AttemptItem(item, UUID.randomUUID(), UUID.randomUUID(), 1, "{}", null, null)),
                List.of(new ItemResult(UUID.randomUUID(), result.id(), item, 0.0, 1.0, false, null, "{}")),
                List.of(), List.of(), List.of());

        JsonNode payload = json.readTree(factory.toJson(event)).get("data").get("item_results").get(0);
        assertTrue(payload.get("duration_ms").isNull());
        assertTrue(payload.get("error_code").isNull());
    }
}
