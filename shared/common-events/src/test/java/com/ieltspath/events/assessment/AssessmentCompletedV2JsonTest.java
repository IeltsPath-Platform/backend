package com.ieltspath.events.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AssessmentCompletedV2JsonTest {
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void preservesTheDocumentedSampleAndTheExistingOptionalNullFields() throws Exception {
        try (var sample = getClass().getResourceAsStream("/assessment-completed-v2-sample.json")) {
            assertNotNull(sample);
            var expected = json.readTree(sample);
            // The contract example predates these optional fields, which the producer already includes as null.
            var item = (ObjectNode) expected.path("data").path("item_results").get(0);
            item.putNull("duration_ms");
            item.putNull("error_code");
            var event = sampleEvent(false);
            assertEquals(expected, json.readTree(json.writeValueAsBytes(event)));
            assertEquals(event, json.readValue(json.writeValueAsBytes(event), AssessmentCompletedV2.class));
        }
    }

    @Test
    void keepsNullableCorrectnessAndAllItemMetadata() throws Exception {
        var original = sampleEvent(false);
        var data = original.data();
        var nullableItem = new AssessmentCompletedV2.ItemResult(id(999), id(16), null,
                BigDecimal.ZERO, BigDecimal.ONE, List.of(new AssessmentCompletedV2.KnowledgePointMapping(
                id(7), BigDecimal.ONE, "NOT_ASSESSED", null)), 4200L, null);
        var event = new AssessmentCompletedV2(original.eventId(), original.eventType(), original.occurredAt(),
                original.source(), new AssessmentCompletedV2.Data(data.userId(), data.learningGoalId(),
                data.packageVersionId(), data.attemptId(), data.resultId(), data.resultVersion(), data.assessmentType(),
                data.status(), data.completedAt(), data.overallBand(), List.of(data.itemResults().get(0), nullableItem)));
        var tree = json.readTree(json.writeValueAsBytes(event));
        var item = tree.path("data").path("item_results").get(1);
        assertEquals(2, tree.path("data").path("item_results").size());
        assertFalse(tree.path("data").path("item_results").get(0).get("is_correct").asBoolean());
        assertTrue(item.get("is_correct").isNull());
        assertEquals(1, item.path("knowledge_point_mappings").size());
        assertEquals(4200L, item.get("duration_ms").asLong());
        assertTrue(item.get("error_code").isNull());
        assertTrue(tree.path("data").get("learning_goal_id").isNull());
        assertTrue(tree.path("data").get("overall_band").isNull());
    }

    private AssessmentCompletedV2 sampleEvent(Boolean correct) {
        Instant at = Instant.parse("2026-10-01T09:10:00Z");
        return new AssessmentCompletedV2(id(991), AssessmentCompletedV2.EVENT_TYPE, at, AssessmentCompletedV2.SOURCE,
                new AssessmentCompletedV2.Data(id(981), null, id(401), id(971), id(975), 1, "TOPIC_GATE", "COMPLETED",
                        at, null, List.of(new AssessmentCompletedV2.ItemResult(id(998), id(15), correct,
                        BigDecimal.ZERO, BigDecimal.ONE, List.of(new AssessmentCompletedV2.KnowledgePointMapping(
                        id(6), new BigDecimal("1.0"), null, null))))));
    }

    private static UUID id(int suffix) {
        return UUID.fromString("20000000-0000-4000-8000-" + String.format("%012d", suffix));
    }
}
