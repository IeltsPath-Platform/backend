package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.ErrorType;
import com.ieltspath.learning.application.port.LearningContentClient.KnowledgePoint;
import com.ieltspath.learning.application.port.LearningContentClient.RubricCriterion;
import com.ieltspath.learning.domain.service.KnowledgeJudgmentRule;
import com.ieltspath.learning.domain.vo.WritingGrade;
import com.ieltspath.learning.domain.vo.WritingTask;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Validates knowledge-point judgments from the same response as the essay's band scores. */
@Component
public class EssayKnowledgeJudge {
    public static final String VERSION = "kpj2";
    private static final String TEMPLATE = loadTemplate();

    private final KnowledgeJudgmentRule rule = new KnowledgeJudgmentRule();

    public WritingGrade judge(WritingTask task, String essay, WritingGrade grade,
                              List<RubricCriterion> rubricCriteria, Map<String, ErrorType> errorTypes,
                              JsonNode entries, Supplier<Map<UUID, KnowledgePoint>> catalog) {
        List<RubricCriterion> orderedCriteria = orderedCriteria(task, rubricCriteria);
        List<KnowledgeJudgmentRule.Candidate> candidates = orderedCriteria.stream()
                .flatMap(criterion -> criterion.subKnowledgePoints().stream()
                        .filter(point -> point != null && point.id() != null && point.code() != null)
                        .map(point -> new KnowledgeJudgmentRule.Candidate(criterion.code(), point.id(), point.code(),
                                point.name() == null ? point.code() : point.name(), point.description())))
                .toList();
        if (candidates.isEmpty()) return grade;

        // The prompt carried the extension for these points, so the version is recorded even when the model
        // returned no judgments.
        List<KnowledgeJudgmentRule.Raw> raws = List.of();
        if (entries != null && entries.isArray()) {
            List<KnowledgeJudgmentRule.Raw> parsed = new ArrayList<>();
            for (JsonNode entry : entries) {
                if (!entry.isObject()) continue;
                parsed.add(new KnowledgeJudgmentRule.Raw(text(entry, "criterion"), text(entry, "code"),
                        text(entry, "judgment"), text(entry, "excerpt"), text(entry, "reason")));
            }
            raws = List.copyOf(parsed);
        }

        List<KnowledgeJudgmentRule.CorrectionEvidence> corrections = new ArrayList<>();
        if (grade.corrections() != null) {
            for (WritingGrade.Correction correction : grade.corrections()) {
                if (correction == null || correction.errorCode() == null) continue;
                ErrorType error = errorTypes.get(correction.errorCode());
                if (error != null && error.knowledgePointId() != null) {
                    corrections.add(new KnowledgeJudgmentRule.CorrectionEvidence(error.knowledgePointId(),
                            correction.excerpt(), error.fixHint()));
                }
            }
        }

        List<WritingGrade.KnowledgeJudgment> judgments = rule.judge(candidates, raws, essay, corrections);
        Map<String, List<WritingGrade.KnowledgeJudgment>> byCriterion = judgments.stream()
                .collect(Collectors.groupingBy(judgment -> candidates.stream()
                        .filter(candidate -> candidate.knowledgePointId().equals(judgment.knowledgePointId())
                                && candidate.code().equals(judgment.code()))
                        .findFirst().map(KnowledgeJudgmentRule.Candidate::criterionCode).orElse("")));
        List<WritingGrade.Criterion> gradeCriteria = grade.criteria().stream().map(criterion ->
                criterion.withKnowledgePointJudgments(byCriterion.getOrDefault(criterion.code(), List.of()))).toList();

        boolean hasFailedJudgment = judgments.stream()
                .anyMatch(judgment -> judgment.judgment() == WritingGrade.Judgment.FAIL);
        Map<String, UUID> parentPoints = grade.criteria().stream().filter(Objects::nonNull)
                .filter(criterion -> criterion.code() != null && criterion.knowledgePointId() != null)
                .collect(Collectors.toMap(WritingGrade.Criterion::code, WritingGrade.Criterion::knowledgePointId,
                        (first, ignored) -> first));
        Supplier<Map<UUID, KnowledgePoint>> catalogIfNeeded = hasFailedJudgment ? catalog : Map::of;
        List<WritingGrade.StudyPoint> studyPoints = rule.studyPoints(gradeCriteria, grade.studyPoints(), judgment -> {
            Map<UUID, KnowledgePoint> points = catalogIfNeeded.get();
            KnowledgePoint point = points.get(judgment.knowledgePointId());
            if (point != null && point.hasPracticeSet()) return judgment.knowledgePointId();
            return parentPoints.get(judgmentCriterion(candidates, judgment));
        });
        return new WritingGrade(gradeCriteria, grade.corrections(), grade.summary(), grade.overallBand(), studyPoints,
                VERSION);
    }

    static String prompt(WritingTask task, List<RubricCriterion> criteria) {
        String points = orderedCriteria(task, criteria).stream().map(criterion -> {
            String heading = criterion.code() + " (" + criterion.name() + "):\n";
            String entries = criterion.subKnowledgePoints().stream()
                    .filter(point -> point != null && point.id() != null && point.code() != null)
                    .map(point -> "- " + point.code() + " — " + (point.name() == null ? point.code() : point.name())
                            + (point.description() == null || point.description().isBlank()
                            ? "" : ": " + point.description()))
                    .collect(Collectors.joining("\n"));
            return entries.isBlank() ? "" : heading + entries;
        }).filter(text -> !text.isBlank()).collect(Collectors.joining("\n"));
        return points.isBlank() ? "" : "\n\n" + TEMPLATE.replace("{{knowledge_points}}", points);
    }

    private static List<RubricCriterion> orderedCriteria(WritingTask task, List<RubricCriterion> criteria) {
        return criteria.stream().filter(Objects::nonNull)
                .filter(criterion -> task.criteria().contains(criterion.code()))
                .sorted(Comparator.comparingInt(RubricCriterion::sortOrder)).toList();
    }

    private static String judgmentCriterion(List<KnowledgeJudgmentRule.Candidate> candidates,
                                            WritingGrade.KnowledgeJudgment judgment) {
        return candidates.stream().filter(candidate -> candidate.knowledgePointId().equals(judgment.knowledgePointId())
                        && candidate.code().equals(judgment.code()))
                .findFirst().map(KnowledgeJudgmentRule.Candidate::criterionCode).orElse("");
    }

    private static String text(JsonNode node, String key) {
        JsonNode value = node.get(key);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private static String loadTemplate() {
        try (var input = EssayKnowledgeJudge.class.getResourceAsStream("/essay-kp-judgment-v2.txt")) {
            if (input == null) throw new IllegalStateException("Knowledge judgment prompt template missing");
            return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Knowledge judgment prompt template unreadable", exception);
        }
    }
}
