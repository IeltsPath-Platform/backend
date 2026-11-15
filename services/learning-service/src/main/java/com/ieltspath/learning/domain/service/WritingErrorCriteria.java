package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.WritingGrade;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.LinkedHashSet;

/**
 * Which rubric criterion an error belongs to, and which practice point a weak criterion should review. A grammar error is
 * a GRA error, except the word-level codes, which IELTS marks as lexical resource.
 */
public final class WritingErrorCriteria {
    private static final Set<String> WORD_LEVEL_CODES = Set.of("WORD_FORM", "NOUN_PLURAL", "WRITING_SPELLING");

    private WritingErrorCriteria() {}

    /** The criterion of a correction: LR, GRA, CC or TASK (TA in Task 1, TR in Task 2); null when it fits none. */
    static String criterionOf(WritingGrade.Correction correction) {
        if (correction.errorCode() != null && WORD_LEVEL_CODES.contains(correction.errorCode())) return "LR";
        if (correction.category() == null) return null;
        return switch (correction.category()) {
            case "VOCABULARY" -> "LR";
            case "GRAMMAR" -> "GRA";
            case "COHERENCE" -> "CC";
            case "TASK" -> "TASK";
            default -> null;
        };
    }

    private static boolean belongsTo(String errorCriterion, String criterionCode) {
        if (errorCriterion == null) return false;
        if ("TASK".equals(errorCriterion)) return "TA".equals(criterionCode) || "TR".equals(criterionCode);
        return errorCriterion.equals(criterionCode);
    }

    /**
     * The practice points of the errors of one criterion, best first: the code met most often, then the more severe, then
     * the code that comes first. Only errors, not suggestions of better wording, count, and only a code whose point can
     * be practised (the grade keeps no point for a code without a practice set). A point shared by two codes appears once.
     */
    public static List<UUID> reviewPoints(List<WritingGrade.Correction> corrections, String criterionCode) {
        if (corrections == null || criterionCode == null) return List.of();
        record Tally(int count, int severity, UUID point) {}
        Map<String, Tally> byCode = new TreeMap<>();
        for (WritingGrade.Correction correction : corrections) {
            if (correction == null || correction.suggestionOnly() || correction.errorCode() == null
                    || correction.practiceKnowledgePointId() == null
                    || !belongsTo(criterionOf(correction), criterionCode)) continue;
            int severity = correction.severity() == null ? 0 : correction.severity();
            byCode.merge(correction.errorCode(), new Tally(1, severity, correction.practiceKnowledgePointId()),
                    (first, next) -> new Tally(first.count() + 1, Math.max(first.severity(), next.severity()), first.point()));
        }
        return byCode.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Tally>>comparingInt(entry -> -entry.getValue().count())
                        .thenComparingInt(entry -> -entry.getValue().severity())
                        .thenComparing(Map.Entry::getKey))
                .map(entry -> entry.getValue().point())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)).stream().toList();
    }
}
