package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.FailedWritingCriterion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Turns an examiner's standalone Writing review into mastery outcomes, the way {@link WritingEvidenceRule} does for the AI:
 * each criterion band that names a knowledge point is correct when it reaches the pass band, and each approved error code
 * that has a trap point is one wrong answer on that trap point. The examiner's names for criteria are mapped to rubric codes
 * here, so the rule never sees the examiner's wording. Pure: no I/O and no clock.
 */
public final class HumanReviewEvidenceRule {
    /** One criterion band, named as the examiner named it ({@code taskResponse}, {@code coherence}, {@code lexical}, {@code grammar}). */
    public record Mark(String criterion, BigDecimal band) { }

    /**
     * The examiner's overall band: the mean of the criterion bands rounded to the nearest half band, a quarter or three quarters
     * rounding up, the same rule as a Speaking test. Null when no criterion has a band.
     */
    public static BigDecimal overallBand(Collection<Mark> marks) {
        List<BigDecimal> bands = marks.stream().map(Mark::band).filter(Objects::nonNull).toList();
        if (bands.isEmpty()) return null;
        BigDecimal sum = bands.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mean = sum.divide(BigDecimal.valueOf(bands.size()), 3, RoundingMode.HALF_UP);
        return mean.multiply(BigDecimal.valueOf(2)).setScale(0, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(2), 1, RoundingMode.HALF_UP);
    }

    /** An error code and how often the examiner marked it; at least 1. */
    public record ErrorMark(String code, int count) { }

    /** One trap point to record as wrong, with the number of times the examiner marked its error. */
    public record TrapOutcome(String code, UUID trapKnowledgePointId, int count) { }

    /**
     * {@code correctByKnowledgePoint} and {@code criterionKnowledgePoints} come from criteria only. {@code failedCriteria} lists
     * the mapped criteria below the pass band. {@code traps} is ordered by count, most frequent first, then by code.
     */
    public record Outcome(Map<UUID, Boolean> correctByKnowledgePoint, Set<UUID> criterionKnowledgePoints,
                          List<FailedWritingCriterion> failedCriteria, List<TrapOutcome> traps) {
        public Outcome {
            correctByKnowledgePoint = Map.copyOf(correctByKnowledgePoint);
            criterionKnowledgePoints = Set.copyOf(criterionKnowledgePoints);
            failedCriteria = List.copyOf(failedCriteria);
            traps = List.copyOf(traps);
        }
    }

    /**
     * @param task                    {@code TASK_1} or {@code TASK_2}; decides what {@code taskResponse} means
     * @param passBand                the prompt's pass band; {@code null} means no criterion is judged
     * @param criterionPointByRubric  knowledge point of each rubric criterion code ({@code TA}/{@code TR}, {@code CC}, {@code LR}, {@code GRA})
     * @param trapPointByCode         trap point of each error code that has one
     * @param approvedCodes           error codes Content has approved; any other code is not shown or counted
     */
    public Outcome derive(String task, BigDecimal passBand, Collection<Mark> marks,
                          Map<String, UUID> criterionPointByRubric, Collection<ErrorMark> errors,
                          Map<String, UUID> trapPointByCode, Set<String> approvedCodes) {
        Map<UUID, Boolean> correct = new LinkedHashMap<>();
        List<FailedWritingCriterion> failed = new ArrayList<>();
        if (passBand != null) {
            for (Mark mark : marks) {
                String rubric = rubricCode(task, mark.criterion());
                UUID point = rubric == null ? null : criterionPointByRubric.get(rubric);
                if (point == null || mark.band() == null) continue;
                boolean reached = mark.band().compareTo(passBand) >= 0;
                if (!reached) failed.add(new FailedWritingCriterion(point, "DEMO_WRITING_CRITERION_" + rubric, mark.band()));
                correct.merge(point, reached, Boolean::logicalAnd);
            }
        }
        List<TrapOutcome> traps = errors.stream()
                .filter(error -> approvedCodes.contains(error.code()) && trapPointByCode.containsKey(error.code()))
                .map(error -> new TrapOutcome(error.code(), trapPointByCode.get(error.code()), error.count()))
                .sorted(Comparator.comparingInt(TrapOutcome::count).reversed().thenComparing(TrapOutcome::code))
                .toList();
        return new Outcome(correct, correct.keySet(), failed, traps);
    }

    /** The rubric code an examiner's criterion name stands for; {@code taskResponse} is TA for Task 1 and TR for Task 2. */
    static String rubricCode(String task, String criterion) {
        if (criterion == null) return null;
        return switch (criterion) {
            case "taskResponse" -> "TASK_1".equals(task) ? "TA" : "TR";
            case "coherence" -> "CC";
            case "lexical" -> "LR";
            case "grammar" -> "GRA";
            default -> null;
        };
    }
}
