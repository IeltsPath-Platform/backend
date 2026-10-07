package com.group01.learning.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.learning.application.command.AssessmentResult;
import com.group01.learning.domain.vo.BandLevel;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Reads an {@code AssessmentCompleted.v2} body. Anything outside the contract raises
 * {@link ContractViolationException} so the message goes to the dead-letter queue instead of being retried.
 * {@code learning_goal_id} and {@code package_version_id} may be null or absent.
 */
@Component
public class AssessmentCompletedParser {
    static final String EVENT_TYPE = "AssessmentCompleted.v2";
    private static final Set<String> TYPES = Set.of("PLACEMENT", "OFFICIAL_PRACTICE", "MOCK", "TOPIC_GATE",
            "COURSE_GATE", "QUIZ");
    private static final Set<String> JUDGMENTS = Set.of("PASS", "FAIL", "NOT_ASSESSED");

    private final ObjectMapper json;

    public AssessmentCompletedParser(ObjectMapper json) {
        this.json = json;
    }

    public AssessmentResult parse(byte[] body) {
        JsonNode root;
        try {
            root = json.reader().with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(body);
        } catch (IOException exception) {
            throw new ContractViolationException("body is not JSON");
        }
        if (root == null || !root.isObject()) throw new ContractViolationException("body is not an object");
        if (!EVENT_TYPE.equals(text(root, "event_type"))) throw new ContractViolationException("unexpected event_type");
        JsonNode data = object(root, "data");
        if (!"COMPLETED".equals(text(data, "status"))) throw new ContractViolationException("status is not COMPLETED");
        String type = text(data, "assessment_type");
        if (!TYPES.contains(type)) throw new ContractViolationException("unsupported assessment_type");
        JsonNode version = data.get("result_version");
        if (version == null || !version.canConvertToInt() || !version.isIntegralNumber() || version.intValue() < 1) {
            throw new ContractViolationException("result_version must be an integer >= 1");
        }
        JsonNode items = data.get("item_results");
        if (items == null || !items.isArray()) throw new ContractViolationException("item_results must be an array");
        List<AssessmentResult.ItemResult> parsed = new ArrayList<>();
        for (JsonNode item : items) parsed.add(item(item));
        optionalUuid(data, "learning_goal_id");
        return new AssessmentResult(uuid(root, "event_id"), uuid(data, "user_id"),
                optionalUuid(data, "package_version_id"), uuid(data, "attempt_id"), uuid(data, "result_id"),
                version.intValue(), type, instant(data, "completed_at"), parsed, optionalBand(data));
    }

    private AssessmentResult.ItemResult item(JsonNode item) {
        if (!item.isObject()) throw new ContractViolationException("item_results[] must be objects");
        JsonNode correct = item.get("is_correct");
        if (correct != null && !correct.isNull() && !correct.isBoolean()) {
            throw new ContractViolationException("is_correct must be a boolean or null");
        }
        BigDecimal score = decimal(item, "score");
        BigDecimal max = decimal(item, "max_score");
        if (max.signum() <= 0 || score.signum() < 0 || score.compareTo(max) > 0) {
            throw new ContractViolationException("score must be within 0..max_score and max_score > 0");
        }
        JsonNode mappings = item.get("knowledge_point_mappings");
        if (mappings == null || !mappings.isArray()) {
            throw new ContractViolationException("knowledge_point_mappings must be an array");
        }
        List<AssessmentResult.KnowledgePointJudgment> judgments = new ArrayList<>();
        for (JsonNode mapping : mappings) {
            if (!mapping.isObject()) throw new ContractViolationException("knowledge_point_mappings[] must be objects");
            JsonNode weight = mapping.get("weight");
            if (weight != null && !weight.isNull() && (!weight.isNumber() || weight.decimalValue().signum() < 0)) {
                throw new ContractViolationException("weight must not be negative");
            }
            JsonNode judgment = mapping.get("qualitative_judgment");
            String value = judgment == null || judgment.isNull() ? null : judgment.asText();
            if (value != null && !JUDGMENTS.contains(value)) {
                throw new ContractViolationException("unsupported qualitative_judgment");
            }
            judgments.add(new AssessmentResult.KnowledgePointJudgment(uuid(mapping, "knowledge_point_id"), value));
        }
        return new AssessmentResult.ItemResult(uuid(item, "item_result_id"), uuid(item, "question_version_id"),
                correct == null || correct.isNull() ? null : correct.booleanValue(), score, max, judgments);
    }

    private static JsonNode object(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isObject()) throw new ContractViolationException(field + " must be an object");
        return value;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || !value.isTextual() ? null : value.asText();
    }

    private static UUID uuid(JsonNode node, String field) {
        UUID value = optionalUuid(node, field);
        if (value == null) throw new ContractViolationException(field + " is required");
        return value;
    }

    private static UUID optionalUuid(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        if (!value.isTextual()) throw new ContractViolationException(field + " must be a UUID");
        try {
            return UUID.fromString(value.asText());
        } catch (IllegalArgumentException exception) {
            throw new ContractViolationException(field + " must be a UUID");
        }
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isNumber()) throw new ContractViolationException(field + " must be a number");
        return value.decimalValue();
    }

    private static BigDecimal optionalBand(JsonNode data) {
        JsonNode value = data.get("overall_band");
        if (value == null || value.isNull()) return null;
        if (!value.isNumber()) throw new ContractViolationException("overall_band must be a number or null");
        try {
            return new BandLevel(value.decimalValue()).value();
        } catch (IllegalArgumentException exception) {
            throw new ContractViolationException("overall_band must be between 0 and 9 in half-band steps");
        }
    }

    private static Instant instant(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) throw new ContractViolationException(field + " is required");
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new ContractViolationException(field + " must be an ISO-8601 instant");
        }
    }

    /** The message breaks the event contract; retrying cannot fix it. */
    public static class ContractViolationException extends RuntimeException {
        public ContractViolationException(String reason) {
            super(reason);
        }
    }
}
