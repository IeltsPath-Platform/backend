package com.ieltspath.learning.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.assessment.HumanReviewCompletedV1;
import com.ieltspath.learning.application.command.HumanReviewResult;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Reads a {@code assessment.human-review-completed.v1} body. Anything outside the contract raises
 * {@link IllegalArgumentException}, so the message is dead-lettered instead of being retried.
 */
@Component
public class HumanReviewCompletedParser {
    private final ObjectMapper json;

    public HumanReviewCompletedParser(ObjectMapper json) {
        this.json = json;
    }

    public HumanReviewResult parse(byte[] body) {
        JsonNode root;
        try {
            root = json.readTree(body);
        } catch (IOException exception) {
            throw new IllegalArgumentException("body is not JSON", exception);
        }
        if (root == null || !root.isObject()) throw new IllegalArgumentException("body is not an object");
        if (!HumanReviewCompletedV1.EVENT_TYPE.equals(text(root, "eventType"))) {
            throw new IllegalArgumentException("unexpected eventType");
        }
        List<HumanReviewResult.Criterion> criteria = new ArrayList<>();
        for (JsonNode item : array(root, "criteria")) {
            JsonNode band = item.get("band");
            if (band == null || !band.isNumber()) throw new IllegalArgumentException("criterion band must be a number");
            criteria.add(new HumanReviewResult.Criterion(text(item, "criterion"), new BigDecimal(band.asText())));
        }
        List<HumanReviewResult.ErrorCount> codes = new ArrayList<>();
        for (JsonNode item : array(root, "errorCodes")) {
            JsonNode count = item.get("count");
            if (count == null || !count.isInt() || count.intValue() < 1) {
                throw new IllegalArgumentException("error count must be an integer of at least 1");
            }
            codes.add(new HumanReviewResult.ErrorCount(text(item, "code"), count.intValue()));
        }
        return new HumanReviewResult(uuid(root, "eventId"), uuid(root, "learnerId"), uuid(root, "reviewSubmissionId"),
                uuid(root, "sourceSubmissionId"), criteria, codes);
    }

    private static JsonNode array(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isArray()) throw new IllegalArgumentException(field + " must be an array");
        return value;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(field + " must be text");
        }
        return value.asText();
    }

    private static UUID uuid(JsonNode node, String field) {
        try {
            return UUID.fromString(text(node, field));
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException(field + " must be a UUID", invalid);
        }
    }
}
