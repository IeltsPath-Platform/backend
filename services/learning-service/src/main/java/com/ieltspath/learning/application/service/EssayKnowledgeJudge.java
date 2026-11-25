package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltspath.learning.application.port.LearningContentClient.ErrorType;
import com.ieltspath.learning.application.port.LearningContentClient.KnowledgePoint;
import com.ieltspath.learning.domain.service.KnowledgeJudgmentRule;
import com.ieltspath.learning.domain.vo.EssayPrompt;
import com.ieltspath.learning.domain.vo.WritingGrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Judges the knowledge points of the question itself, in the same response as the essay's band scores. The model is
 * asked about no other points. A question without knowledge points gets no judgments block and no judgment version.
 */
@Component
public class EssayKnowledgeJudge {
    private static final Logger log = LoggerFactory.getLogger(EssayKnowledgeJudge.class);

    /** Stored with a grade whose prompt asked about the question's own knowledge points. */
    public static final String VERSION = "kpj3";
    /** At most this many of the question's points go into the prompt; the rest take the essay's pass mark. */
    public static final int MAX_POINTS = 4;
    private static final String TEMPLATE = loadTemplate("essay-kp-judgment-v3.txt");

    private final KnowledgeJudgmentRule rule = new KnowledgeJudgmentRule();

    /** A knowledge point of the question as the prompt shows it. */
    public record TaskPoint(UUID id, String code, String name, String description, boolean hasPracticeSet) {}

    /**
     * The question's knowledge points that the prompt will ask about, in the question's order and without repeats. A
     * point the catalog does not know cannot be described to the model, so it is left out. The catalog is read only
     * when the question has points.
     */
    public static List<TaskPoint> taskPoints(EssayPrompt prompt, Supplier<Map<UUID, KnowledgePoint>> catalog) {
        if (prompt == null || prompt.knowledgePointIds() == null) return List.of();
        List<UUID> ids = prompt.knowledgePointIds().stream().filter(id -> id != null)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)).stream().toList();
        if (ids.isEmpty()) return List.of();
        Map<UUID, KnowledgePoint> known = catalog.get();
        List<TaskPoint> points = new ArrayList<>();
        for (UUID id : ids) {
            KnowledgePoint point = known.get(id);
            if (point == null || point.code() == null) {
                log.info("Question knowledge point left out of the essay prompt: questionVersionId={}, knowledgePointId={}",
                        prompt.questionVersionId(), id);
                continue;
            }
            points.add(new TaskPoint(id, point.code(), point.name() == null ? point.code() : point.name(),
                    point.description(), point.hasPracticeSet()));
        }
        if (points.size() > MAX_POINTS) {
            log.info("Question has {} knowledge points, only the first {} are judged: questionVersionId={}",
                    points.size(), MAX_POINTS, prompt.questionVersionId());
            return List.copyOf(points.subList(0, MAX_POINTS));
        }
        return List.copyOf(points);
    }

    /** Adds the judgments of the question's points to a grade; a grade for a question without points stays as it is. */
    public WritingGrade judge(String essay, WritingGrade grade, List<TaskPoint> points,
                              Map<String, ErrorType> errorTypes, JsonNode entries) {
        if (points.isEmpty()) return grade;
        List<KnowledgeJudgmentRule.Candidate> candidates = points.stream()
                .map(point -> new KnowledgeJudgmentRule.Candidate(null, point.id(), point.code(), point.name(),
                        point.description())).toList();

        List<KnowledgeJudgmentRule.Raw> raws = new ArrayList<>();
        if (entries != null && entries.isArray()) {
            for (JsonNode entry : entries) {
                if (!entry.isObject()) continue;
                raws.add(new KnowledgeJudgmentRule.Raw(null, text(entry, "code"), text(entry, "judgment"),
                        text(entry, "excerpt"), text(entry, "reason")));
            }
        }

        List<KnowledgeJudgmentRule.CorrectionEvidence> corrections = new ArrayList<>();
        if (grade.corrections() != null) {
            for (WritingGrade.Correction correction : grade.corrections()) {
                // A suggestion is acceptable text, so it never turns a knowledge point into a FAIL.
                if (correction == null || correction.errorCode() == null || correction.suggestionOnly()) continue;
                ErrorType error = errorTypes.get(correction.errorCode());
                if (error != null && error.knowledgePointId() != null) {
                    corrections.add(new KnowledgeJudgmentRule.CorrectionEvidence(error.knowledgePointId(),
                            correction.excerpt(), error.fixHint()));
                }
            }
        }

        List<WritingGrade.KnowledgeJudgment> judgments = rule.judgeByCode(candidates, raws, essay, corrections);
        Map<UUID, Boolean> practice = new java.util.HashMap<>();
        points.forEach(point -> practice.put(point.id(), point.hasPracticeSet()));
        // A point with a practice set sends the learner to it; one without sends nowhere, never to a criterion's point.
        List<WritingGrade.StudyPoint> studyPoints = rule.withTaskStudyPoints(grade.studyPoints(), judgments,
                judgment -> Boolean.TRUE.equals(practice.get(judgment.knowledgePointId())) ? judgment.knowledgePointId() : null);
        return grade.withTaskKnowledgePointJudgments(judgments, studyPoints, VERSION);
    }

    /** The prompt extension that asks for the judgments; empty when there are no points to ask about. */
    static String prompt(List<TaskPoint> points) {
        if (points == null || points.isEmpty()) return "";
        String lines = points.stream().map(point -> "- " + point.code() + " — " + point.name()
                        + (point.description() == null || point.description().isBlank() ? "" : ": " + point.description()))
                .collect(java.util.stream.Collectors.joining("\n"));
        return "\n\n" + TEMPLATE.replace("{{knowledge_points}}", lines);
    }

    private static String text(JsonNode node, String key) {
        JsonNode value = node.get(key);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private static String loadTemplate(String resource) {
        try (var input = EssayKnowledgeJudge.class.getResourceAsStream("/" + resource)) {
            if (input == null) throw new IllegalStateException("Knowledge judgment prompt template missing");
            return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .replace("\r\n", "\n").replace('\r', '\n');
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Knowledge judgment prompt template unreadable", exception);
        }
    }
}
