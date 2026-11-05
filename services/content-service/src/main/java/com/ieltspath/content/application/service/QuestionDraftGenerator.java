package com.ieltspath.content.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ieltspath.content.application.command.QuestionDraftRequest;
import com.ieltspath.content.application.port.AiDraftGenerator;
import com.ieltspath.content.application.port.ContentGenerationException;
import com.ieltspath.content.application.port.ContentGenerationPort;
import com.ieltspath.content.application.port.QuestionStemReader;
import com.ieltspath.content.application.result.GenerationOutcome;
import com.ieltspath.content.application.result.GenerationReply;
import com.ieltspath.content.domain.aggregate.ErrorType;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.repository.ErrorTypeRepository;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.infrastructure.config.ContentAiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Asks the model for questions on one knowledge point and keeps only the ones that pass the shape check. Rejected
 * items are recorded with their reason, so the author sees what was dropped and why.
 */
@Component
public class QuestionDraftGenerator implements AiDraftGenerator {

    private static final Logger log = LoggerFactory.getLogger(QuestionDraftGenerator.class);

    private final ContentGenerationPort model;
    private final KnowledgePointRepository knowledgePoints;
    private final ErrorTypeRepository errorTypes;
    private final QuestionStemReader stems;
    private final ObjectMapper json;
    private final ContentAiProperties properties;

    public QuestionDraftGenerator(ContentGenerationPort model, KnowledgePointRepository knowledgePoints,
                                  ErrorTypeRepository errorTypes, QuestionStemReader stems, ObjectMapper json,
                                  ContentAiProperties properties) {
        this.model = model;
        this.knowledgePoints = knowledgePoints;
        this.errorTypes = errorTypes;
        this.stems = stems;
        this.json = json;
        this.properties = properties;
    }

    @Override
    public DraftKind kind() {
        return DraftKind.QUESTIONS;
    }

    @Override
    public GenerationOutcome generate(AiDraft draft) {
        QuestionDraftRequest request = readRequest(draft.getRequestJson());
        KnowledgePoint point = knowledgePoints.findById(request.knowledgePointId())
                .filter(found -> found.getStatus() == ContentStatus.ACTIVE)
                .orElseThrow(() -> new ContentGenerationException(ContentGenerationException.FAILED,
                        "Knowledge point is not available"));

        List<ErrorType> codes = errorTypes.findApprovedActive();
        Set<String> activeCodes = codes.stream().map(ErrorType::getCode).collect(Collectors.toSet());
        GenerationReply reply = model.generate(QuestionDraftPromptBuilder.build(point, request, codes));
        JsonNode candidates = candidates(reply.text());
        // Stems the point already has, and then those kept from this reply, so neither is repeated.
        Set<String> knownStems = stems.stemsOf(point.getId()).stream().map(QuestionDraftItems::normalize)
                .collect(Collectors.toCollection(HashSet::new));

        ArrayNode accepted = json.createArrayNode();
        ArrayNode rejected = json.createArrayNode();
        int limit = Math.min(request.count(), properties.getAi().getMaxItemsPerRequest());
        for (int index = 0; index < candidates.size(); index++) {
            JsonNode candidate = withKnownDistractors(candidates.get(index), activeCodes);
            if (accepted.size() >= limit) {
                break;
            }
            String reason = QuestionDraftValidator.rejectionReason(candidate, request.questionType(), activeCodes,
                    knownStems);
            if (reason == null) {
                knownStems.add(QuestionDraftItems.normalize(candidate.path("stem").asText()));
                accepted.add(QuestionDraftItems.toItem(candidate, request.questionType(), request.difficulty()));
            } else {
                rejected.add(json.createObjectNode().put("index", index).put("reason", reason));
            }
        }
        // Only the reasons are logged, never the reply: it may echo the prompt.
        log.info("Question draft for {}: {} items usable, {} rejected {}", point.getId(), accepted.size(),
                rejected.size(), reasonCounts(rejected));
        if (accepted.isEmpty()) {
            throw new ContentGenerationException(ContentGenerationException.INVALID_REPLY,
                    "No usable question in the reply");
        }

        ObjectNode result = json.createObjectNode();
        result.set("items", accepted);
        result.set("rejected", rejected);
        return new GenerationOutcome(write(result), reply.model(), QuestionDraftPromptBuilder.VERSION);
    }

    /** Accepts a bare array or an object with "items", and tolerates code fences or prose around the JSON. */
    private JsonNode candidates(String text) {
        String body = text == null ? "" : text;
        int start = body.indexOf('{');
        int end = body.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new ContentGenerationException(ContentGenerationException.INVALID_REPLY, "Reply is not JSON");
        }
        try {
            JsonNode root = json.readTree(body.substring(start, end + 1));
            JsonNode items = root.path("items");
            if (!items.isArray()) {
                throw new ContentGenerationException(ContentGenerationException.INVALID_REPLY, "Reply has no items");
            }
            return items;
        } catch (JsonProcessingException invalid) {
            throw new ContentGenerationException(ContentGenerationException.INVALID_REPLY, "Reply is not JSON");
        }
    }

    /**
     * A wrong option that names a code outside the approved list loses that label and keeps its place: the question
     * itself is still sound, and the label only says what the learner's mistake is.
     */
    private ObjectNode withKnownDistractors(JsonNode candidate, Set<String> activeCodes) {
        ObjectNode copy = candidate.isObject() ? ((ObjectNode) candidate).deepCopy() : json.createObjectNode();
        JsonNode distractors = copy.path("distractors");
        if (!distractors.isObject()) {
            return copy;
        }
        ObjectNode known = json.createObjectNode();
        distractors.fields().forEachRemaining(entry -> {
            if (activeCodes.contains(entry.getValue().asText(""))) {
                known.set(entry.getKey(), entry.getValue());
            }
        });
        if (known.size() == distractors.size()) {
            return copy;
        }
        copy.remove("distractors");
        if (known.size() > 0) {
            copy.set("distractors", known);
        }
        return copy;
    }

    private static Map<String, Integer> reasonCounts(ArrayNode rejected) {
        Map<String, Integer> counts = new TreeMap<>();
        rejected.forEach(entry -> counts.merge(entry.path("reason").asText(), 1, Integer::sum));
        return counts;
    }

    private QuestionDraftRequest readRequest(String text) {
        try {
            return json.readValue(text, QuestionDraftRequest.class);
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
