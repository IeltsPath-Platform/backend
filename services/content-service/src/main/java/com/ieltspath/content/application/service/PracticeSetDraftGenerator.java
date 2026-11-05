package com.ieltspath.content.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ieltspath.content.application.command.PracticeSetDraftRequest;
import com.ieltspath.content.application.port.AiDraftGenerator;
import com.ieltspath.content.application.port.ContentGenerationException;
import com.ieltspath.content.application.port.ContentGenerationPort;
import com.ieltspath.content.application.port.QuestionStemReader;
import com.ieltspath.content.application.result.GenerationOutcome;
import com.ieltspath.content.application.result.GenerationReply;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.QuestionType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Asks the model for one practice set on the knowledge points of a lesson: a passage and the questions about it. Only
 * questions that pass the shape check are kept, and the stored items carry an answer spec built here, never copied from
 * the model. Questions the knowledge points already have, or that repeat inside the reply, are dropped.
 */
@Component
public class PracticeSetDraftGenerator implements AiDraftGenerator {

    static final int MIN_ITEMS = 3;
    static final int MAX_PASSAGE = 6000;
    static final int MAX_TITLE = 255;
    static final int MAX_INSTRUCTIONS = 1000;
    private static final Set<String> TRUE_FALSE_KEYS = Set.of("TRUE", "FALSE", "NOT_GIVEN");

    private final ContentGenerationPort model;
    private final KnowledgePointRepository knowledgePoints;
    private final QuestionStemReader stems;
    private final ObjectMapper json;

    public PracticeSetDraftGenerator(ContentGenerationPort model, KnowledgePointRepository knowledgePoints,
                                     QuestionStemReader stems, ObjectMapper json) {
        this.model = model;
        this.knowledgePoints = knowledgePoints;
        this.stems = stems;
        this.json = json;
    }

    @Override
    public DraftKind kind() {
        return DraftKind.PRACTICE_SET;
    }

    @Override
    public GenerationOutcome generate(AiDraft draft) {
        PracticeSetDraftRequest request = readRequest(draft.getRequestJson());
        List<KnowledgePoint> points = activePoints(request.knowledgePointIds());

        GenerationReply reply = model.generate(PracticeSetDraftPromptBuilder.build(points, request));
        JsonNode root = parse(reply.text());
        String passage = root.path("passage").asText("").trim();
        if (passage.isEmpty() || passage.length() > MAX_PASSAGE) {
            throw new ContentGenerationException(ContentGenerationException.INVALID_REPLY, "Reply has no usable passage");
        }
        JsonNode candidates = root.path("items");
        if (!candidates.isArray()) {
            throw new ContentGenerationException(ContentGenerationException.INVALID_REPLY, "Reply has no items");
        }

        Set<String> knownStems = new HashSet<>();
        for (KnowledgePoint point : points) {
            stems.stemsOf(point.getId()).forEach(stem -> knownStems.add(QuestionDraftItems.normalize(stem)));
        }
        ArrayNode accepted = json.createArrayNode();
        ArrayNode rejected = json.createArrayNode();
        for (int index = 0; index < candidates.size() && accepted.size() < request.count(); index++) {
            JsonNode candidate = withoutDistractors(candidates.get(index));
            String reason = rejection(candidate, request.questionType(), knownStems);
            if (reason == null) {
                knownStems.add(QuestionDraftItems.normalize(candidate.path("stem").asText()));
                ObjectNode item = QuestionDraftItems.toItem(candidate, request.questionType(), request.difficulty());
                ArrayNode itemPoints = item.putArray("knowledgePointIds");
                taughtBy(candidate, points).forEach(id -> itemPoints.add(id.toString()));
                accepted.add(item);
            } else {
                rejected.add(json.createObjectNode().put("index", index).put("reason", reason));
            }
        }
        if (accepted.size() < MIN_ITEMS) {
            throw new ContentGenerationException(ContentGenerationException.INVALID_REPLY,
                    "Fewer than " + MIN_ITEMS + " usable questions in the reply");
        }

        ObjectNode result = json.createObjectNode();
        result.put("title", clip(root.path("title").asText(""), MAX_TITLE, "Practice set"));
        result.put("instructions", clip(root.path("instructions").asText(""), MAX_INSTRUCTIONS, ""));
        result.put("passage", passage);
        result.put("questionType", request.questionType().name());
        ArrayNode pointIds = result.putArray("knowledgePointIds");
        points.forEach(point -> pointIds.add(point.getId().toString()));
        result.set("items", accepted);
        result.set("rejected", rejected);
        return new GenerationOutcome(write(result), reply.model(), PracticeSetDraftPromptBuilder.VERSION);
    }

    private List<KnowledgePoint> activePoints(List<UUID> ids) {
        List<KnowledgePoint> points = new ArrayList<>();
        for (UUID id : ids) {
            points.add(knowledgePoints.findById(id).filter(found -> found.getStatus() == ContentStatus.ACTIVE)
                    .orElseThrow(() -> new ContentGenerationException(ContentGenerationException.FAILED,
                            "Knowledge point is not available")));
        }
        return points;
    }

    /** The shared shape check, plus the three fixed answers a true/false/not-given question must offer. */
    private static String rejection(JsonNode candidate, QuestionType type, Set<String> knownStems) {
        String reason = QuestionDraftValidator.rejectionReason(candidate, type, Set.of(), knownStems);
        if (reason != null || type != QuestionType.TRUE_FALSE_NOT_GIVEN) {
            return reason;
        }
        Set<String> keys = StreamSupport.stream(candidate.path("options").spliterator(), false)
                .map(option -> option.path("optionKey").asText()).collect(Collectors.toSet());
        return keys.equals(TRUE_FALSE_KEYS) ? null : "BAD_OPTIONS";
    }

    /**
     * The points a question tests, from the K labels the model gave it. A question with no label the model knew is
     * linked to every point of the set, so a question is never left without a point.
     */
    private static List<UUID> taughtBy(JsonNode candidate, List<KnowledgePoint> points) {
        Set<UUID> named = new LinkedHashSet<>();
        for (JsonNode label : candidate.path("knowledge")) {
            for (int index = 0; index < points.size(); index++) {
                if (PracticeSetDraftPromptBuilder.label(index).equalsIgnoreCase(label.asText("").trim())) {
                    named.add(points.get(index).getId());
                }
            }
        }
        return named.isEmpty() ? points.stream().map(KnowledgePoint::getId).toList() : List.copyOf(named);
    }

    /** Error codes are not offered to the model for a set, so any it writes anyway would only be rejected. */
    private static JsonNode withoutDistractors(JsonNode candidate) {
        if (candidate == null || !candidate.isObject()) {
            return candidate;
        }
        ObjectNode copy = candidate.deepCopy();
        copy.remove("distractors");
        return copy;
    }

    private static String clip(String text, int max, String fallback) {
        String value = text == null ? "" : text.strip();
        if (value.isEmpty()) {
            return fallback;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    /** Tolerates code fences or prose around the JSON object. */
    private JsonNode parse(String text) {
        String body = text == null ? "" : text;
        int start = body.indexOf('{');
        int end = body.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new ContentGenerationException(ContentGenerationException.INVALID_REPLY, "Reply is not JSON");
        }
        try {
            return json.readTree(body.substring(start, end + 1));
        } catch (JsonProcessingException invalid) {
            throw new ContentGenerationException(ContentGenerationException.INVALID_REPLY, "Reply is not JSON");
        }
    }

    private PracticeSetDraftRequest readRequest(String text) {
        try {
            return json.readValue(text, PracticeSetDraftRequest.class);
        } catch (JsonProcessingException invalid) {
            throw new ContentGenerationException(ContentGenerationException.FAILED, "Stored request is unreadable");
        }
    }

    private String write(JsonNode node) {
        try {
            return json.writeValueAsString(node);
        } catch (JsonProcessingException invalid) {
            throw new ContentGenerationException(ContentGenerationException.FAILED, "Result could not be stored");
        }
    }
}
