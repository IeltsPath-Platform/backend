package com.group01.learning.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.group01.learning.application.command.AssessmentResult;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class AssessmentCompletedParserTest {
    private final ObjectMapper json = new ObjectMapper();
    private final AssessmentCompletedParser parser = new AssessmentCompletedParser(json);

    @Test
    void overallBandIsOptionalAndNormalizesNumericHalfBands() {
        assertNull(parse(event("")).overallBand());
        assertNull(parse(event(",\"overall_band\":null")).overallBand());
        for (String band : new String[]{"0.0", "6.00", "9.0"}) {
            assertEquals(new java.math.BigDecimal(band).setScale(1),
                    parse(event(",\"overall_band\":" + band)).overallBand());
        }
    }

    @Test
    void invalidOverallBandsAreContractViolations() {
        for (String value : new String[]{"9.5", "6.25", "-0.5", "6.000000000000000000001",
                "\"6.0\"", "true", "{}"}) {
            assertViolation(event(",\"overall_band\":" + value));
        }
    }

    static String event(String extraData) {
        return """
                {"event_id":"%s","event_type":"AssessmentCompleted.v2","occurred_at":"2026-10-01T09:10:00Z",
                 "source":"assessment-service","data":{"user_id":"%s","attempt_id":"%s","result_id":"%s",
                 "result_version":1,"assessment_type":"TOPIC_GATE","status":"COMPLETED",
                 "completed_at":"2026-10-01T09:10:00Z"%s,
                 "item_results":[{"item_result_id":"%s","question_version_id":"%s","is_correct":false,"score":0,
                 "max_score":1,"knowledge_point_mappings":[{"knowledge_point_id":"%s","weight":1.0,
                 "qualitative_judgment":null,"error_type":null}]}]}}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), extraData,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

    private AssessmentResult parse(String body) {
        return parser.parse(body.getBytes(StandardCharsets.UTF_8));
    }

    private String mutate(String body, Consumer<ObjectNode> change) throws Exception {
        ObjectNode root = (ObjectNode) json.readTree(body);
        change.accept(root);
        return json.writeValueAsString(root);
    }

    @Test
    void goalAndPackageVersionAreOptional() {
        AssessmentResult withoutBoth = parse(event(""));
        assertNull(withoutBoth.packageVersionId());
        assertEquals("TOPIC_GATE", withoutBoth.assessmentType());
        assertFalse(withoutBoth.items().getFirst().isCorrect());

        UUID version = UUID.randomUUID();
        AssessmentResult withNullGoal = parse(event(",\"learning_goal_id\":null,\"package_version_id\":\"" + version + "\""));
        assertEquals(version, withNullGoal.packageVersionId());
    }

    @Test
    void acceptsCourseGateAssessmentType() throws Exception {
        String courseGate = mutate(event(""), root ->
                ((ObjectNode) root.get("data")).put("assessment_type", "COURSE_GATE"));

        assertEquals("COURSE_GATE", parse(courseGate).assessmentType());
    }

    @Test
    void contractViolationsAreRejected() throws Exception {
        String valid = event("");
        assertViolation("not json");
        assertViolation(mutate(valid, root -> root.put("event_type", "AssessmentCompleted.v1")));
        assertViolation(mutate(valid, root -> ((ObjectNode) root.get("data")).put("status", "DRAFT")));
        assertViolation(mutate(valid, root -> ((ObjectNode) root.get("data")).put("user_id", "not-a-uuid")));
        assertViolation(mutate(valid, root -> ((ObjectNode) root.get("data")).put("result_version", 0)));
        assertViolation(mutate(valid, root -> ((ObjectNode) root.get("data").get("item_results").get(0))
                .put("score", 2)));
        assertViolation(mutate(valid, root -> ((ObjectNode) root.get("data").get("item_results").get(0)
                .get("knowledge_point_mappings").get(0)).put("qualitative_judgment", "MAYBE")));
        assertViolation(mutate(valid, root -> ((ObjectNode) root.get("data")).put("package_version_id", "x")));
    }

    private void assertViolation(String body) {
        assertThrows(AssessmentCompletedParser.ContractViolationException.class, () -> parse(body));
    }
}
