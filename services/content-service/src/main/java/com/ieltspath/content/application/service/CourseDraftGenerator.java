package com.ieltspath.content.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ieltspath.content.application.command.CourseDraftRequest;
import com.ieltspath.content.application.port.AiDraftGenerator;
import com.ieltspath.content.application.port.ContentGenerationException;
import com.ieltspath.content.application.port.ContentGenerationPort;
import com.ieltspath.content.application.port.CourseCandidateReader;
import com.ieltspath.content.application.result.CourseCandidates;
import com.ieltspath.content.application.result.GenerationOutcome;
import com.ieltspath.content.application.result.GenerationReply;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.vo.CourseDraftAnalysis;
import com.ieltspath.content.domain.vo.CourseDraftStructure;
import com.ieltspath.content.domain.vo.DraftKind;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Plans a course from a request. The rules choose the candidates, the model only groups and names them, and the result
 * is cut back to the candidate set before it is stored with its analysis. Nothing is invented and nothing is generated
 * to fill a shortage: a shortage is reported as a gap.
 */
@Component
public class CourseDraftGenerator implements AiDraftGenerator {

    static final int ATTEMPTS = 2;

    private final CourseCandidateSelector selector;
    private final CourseCandidateReader candidateReader;
    private final ContentGenerationPort model;
    private final CourseDraftValidator validator;
    private final CourseDraftCodec codec;
    private final CourseDraftAnalysisService analysis;
    private final ObjectMapper json;

    public CourseDraftGenerator(CourseCandidateSelector selector, CourseCandidateReader candidateReader,
                                ContentGenerationPort model, CourseDraftValidator validator, CourseDraftCodec codec,
                                CourseDraftAnalysisService analysis, ObjectMapper json) {
        this.selector = selector;
        this.candidateReader = candidateReader;
        this.model = model;
        this.validator = validator;
        this.codec = codec;
        this.analysis = analysis;
        this.json = json;
    }

    @Override
    public DraftKind kind() {
        return DraftKind.COURSE;
    }

    @Override
    public GenerationOutcome generate(AiDraft draft) {
        CourseDraftRequest request = readRequest(draft.getRequestJson());
        CourseCandidates candidates = selector.select(request);
        if (candidates.points().isEmpty()) {
            throw new ContentGenerationException(ContentGenerationException.NO_CANDIDATES,
                    "The request matched no knowledge point");
        }
        String prompt = CoursePromptBuilder.build(request, candidates);
        int firstSortOrder = candidateReader.highestRootTopicSortOrder() + 1;

        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            GenerationReply reply = model.generate(prompt);
            CourseDraftValidator.Built built = tryBuild(request, candidates, reply.text(), firstSortOrder);
            if (built != null) {
                return outcome(built, candidates, reply.model());
            }
        }
        throw new ContentGenerationException(ContentGenerationException.INVALID_REPLY,
                "The reply could not be turned into a course plan");
    }

    /** {@code null} when the reply is not JSON, holds nothing usable, or yields a structure that is not well formed. */
    private CourseDraftValidator.Built tryBuild(CourseDraftRequest request, CourseCandidates candidates, String text,
                                                int firstSortOrder) {
        JsonNode reply = parse(text);
        if (reply == null) return null;
        CourseDraftValidator.Built built = validator.build(request, candidates, reply, firstSortOrder);
        if (built == null || !codec.problems(built.structure()).isEmpty()) return null;
        return built;
    }

    private GenerationOutcome outcome(CourseDraftValidator.Built built, CourseCandidates candidates, String modelName) {
        CourseDraftAnalysis checked = analysis.analyze(built.structure()).analysis();
        List<CourseDraftStructure.GapNote> gaps = checked.gaps().stream()
                .map(gap -> new CourseDraftStructure.GapNote(gap.knowledgePointId(), gap.reason())).toList();
        CourseDraftStructure structure = new CourseDraftStructure(built.structure().course(),
                built.structure().topics(), built.structure().courseTest(), built.structure().practiceTestMilestones(),
                gaps);

        ObjectNode result = (ObjectNode) json.valueToTree(structure);
        result.set("unmatchedTopics", json.valueToTree(candidates.unmatchedTopics()));
        result.set("warnings", json.valueToTree(built.warnings()));
        result.set("analysis", analysisNode(checked));
        ArrayNode hints = json.createArrayNode();
        checked.gaps().stream().map(gap -> gap.knowledgePointId()).distinct().forEach(id -> {
            String reasons = checked.gaps().stream().filter(gap -> gap.knowledgePointId().equals(id))
                    .map(CourseDraftAnalysis.Gap::reason).collect(Collectors.joining(","));
            hints.add(json.createObjectNode().put("knowledgePointId", id.toString()).put("reason", reasons)
                    .put("action", "REQUEST_QUESTION_DRAFT"));
        });
        result.set("gapHints", hints);
        return new GenerationOutcome(write(result), modelName, CoursePromptBuilder.VERSION);
    }

    private ObjectNode analysisNode(CourseDraftAnalysis checked) {
        ObjectNode node = json.createObjectNode();
        node.put("valid", checked.valid());
        node.set("errors", json.valueToTree(checked.errors()));
        node.set("warnings", json.valueToTree(checked.warnings()));
        node.set("gaps", json.valueToTree(checked.gaps()));
        node.set("coverage", json.valueToTree(checked.coverage()));
        return node;
    }

    /** Tolerates code fences or prose around the JSON object. */
    private JsonNode parse(String text) {
        String body = text == null ? "" : text;
        int start = body.indexOf('{');
        int end = body.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        try {
            return json.readTree(body.substring(start, end + 1));
        } catch (JsonProcessingException invalid) {
            return null;
        }
    }

    private CourseDraftRequest readRequest(String text) {
        try {
            return json.readValue(text, CourseDraftRequest.class);
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
