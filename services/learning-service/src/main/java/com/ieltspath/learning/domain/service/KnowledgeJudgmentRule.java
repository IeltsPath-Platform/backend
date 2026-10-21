package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.WritingGrade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/** Validates optional model judgments and turns failed knowledge points into study recommendations. */
public final class KnowledgeJudgmentRule {
    public static final int MAX_TEXT_CHARS = 300;

    public record Candidate(String criterionCode, UUID knowledgePointId, String code, String name,
                            String description) {}
    public record Raw(String criterionCode, String code, String judgment, String excerpt, String reason) {}
    /** A validated correction whose error type is tied to a knowledge point. */
    public record CorrectionEvidence(UUID knowledgePointId, String excerpt, String reason) {}

    public List<WritingGrade.KnowledgeJudgment> judge(List<Candidate> candidates, List<Raw> raws, String essay,
                                                       List<CorrectionEvidence> corrections) {
        List<WritingGrade.KnowledgeJudgment> judgments = new ArrayList<>(candidates.size());
        for (Candidate candidate : candidates) {
            Raw raw = raws.stream().filter(Objects::nonNull)
                    .filter(item -> Objects.equals(item.criterionCode(), candidate.criterionCode())
                            && Objects.equals(item.code(), candidate.code()))
                    .findFirst().orElse(null);
            WritingGrade.Judgment judgment = parseJudgment(raw == null ? null : raw.judgment());
            String excerpt = null;
            String reason = null;
            if ((judgment == WritingGrade.Judgment.PASS || judgment == WritingGrade.Judgment.FAIL)
                    && raw.excerpt() != null) {
                String candidateExcerpt = raw.excerpt().trim();
                if (!candidateExcerpt.isEmpty() && essay != null && essay.contains(candidateExcerpt)) {
                    excerpt = truncate(candidateExcerpt);
                    reason = truncate(raw.reason());
                } else {
                    judgment = WritingGrade.Judgment.NOT_ASSESSED;
                }
            } else {
                judgment = WritingGrade.Judgment.NOT_ASSESSED;
            }

            CorrectionEvidence correction = corrections.stream().filter(Objects::nonNull)
                    .filter(item -> candidate.knowledgePointId().equals(item.knowledgePointId()))
                    .findFirst().orElse(null);
            if (correction != null && judgment == WritingGrade.Judgment.PASS) {
                judgment = WritingGrade.Judgment.NOT_ASSESSED;
                excerpt = null;
                reason = null;
            } else if (correction != null && judgment == WritingGrade.Judgment.NOT_ASSESSED) {
                judgment = WritingGrade.Judgment.FAIL;
                excerpt = truncate(correction.excerpt());
                reason = truncate(correction.reason());
            }
            judgments.add(new WritingGrade.KnowledgeJudgment(candidate.knowledgePointId(), candidate.code(),
                    candidate.name(), judgment, excerpt, reason));
        }
        return List.copyOf(judgments);
    }

    public List<WritingGrade.StudyPoint> studyPoints(List<WritingGrade.Criterion> criteria,
                                                     List<WritingGrade.StudyPoint> existing,
                                                     Function<WritingGrade.KnowledgeJudgment, UUID> practiceTarget) {
        Map<String, List<WritingGrade.StudyPoint>> failedByCriterion = new HashMap<>();
        List<WritingGrade.StudyPoint> failedInOrder = new ArrayList<>();
        for (WritingGrade.Criterion criterion : criteria) {
            if (criterion == null || criterion.knowledgePointJudgments() == null) continue;
            for (WritingGrade.KnowledgeJudgment judgment : criterion.knowledgePointJudgments()) {
                if (judgment == null || judgment.judgment() != WritingGrade.Judgment.FAIL) continue;
                var point = new WritingGrade.StudyPoint(WritingGrade.StudyPoint.KNOWLEDGE_POINT, criterion.code(),
                        criterion.band(), judgment.knowledgePointId(), judgment.code(), judgment.name(),
                        judgment.name(), judgment.reason(), judgment.excerpt(), practiceTarget.apply(judgment));
                failedByCriterion.computeIfAbsent(criterion.code(), ignored -> new ArrayList<>()).add(point);
                failedInOrder.add(point);
            }
        }

        List<WritingGrade.StudyPoint> ordered = new ArrayList<>();
        for (WritingGrade.StudyPoint point : existing) {
            if (WritingGrade.StudyPoint.CRITERION.equals(point.kind())) {
                ordered.add(point);
                ordered.addAll(failedByCriterion.getOrDefault(point.criterionCode(), List.of()));
            }
        }
        for (WritingGrade.StudyPoint point : failedInOrder) {
            boolean followsCriterion = existing.stream().anyMatch(existingPoint ->
                    WritingGrade.StudyPoint.CRITERION.equals(existingPoint.kind())
                            && Objects.equals(existingPoint.criterionCode(), point.criterionCode()));
            if (!followsCriterion) ordered.add(point);
        }
        existing.stream().filter(point -> !WritingGrade.StudyPoint.CRITERION.equals(point.kind()))
                .forEach(ordered::add);
        return List.copyOf(ordered);
    }

    private static WritingGrade.Judgment parseJudgment(String value) {
        if (value == null) return WritingGrade.Judgment.NOT_ASSESSED;
        return switch (value.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "PASS" -> WritingGrade.Judgment.PASS;
            case "FAIL" -> WritingGrade.Judgment.FAIL;
            default -> WritingGrade.Judgment.NOT_ASSESSED;
        };
    }

    private static String truncate(String value) {
        if (value == null) return null;
        return value.length() <= MAX_TEXT_CHARS ? value : value.substring(0, MAX_TEXT_CHARS);
    }
}
