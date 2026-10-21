package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.WritingGrade;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Turns one graded essay into mastery outcomes. The question's knowledge points take the essay's pass. Each rubric
 * criterion that names a knowledge point is evidence for that point on its own, correct when its band reaches the
 * pass band. Judged sub knowledge points add evidence independently of the essay pass mark. A point that is both a
 * question point and a criterion point takes the criterion's result; two criteria on one point must both pass.
 */
public final class WritingEvidenceRule {

    /**
     * {@code correctByKnowledgePoint} lists question points first, then criterion points, then judged sub points;
     * a point appears once. {@code criterionKnowledgePoints} are points decided by a criterion band or sub judgment.
     */
    public record Outcome(Map<UUID, Boolean> correctByKnowledgePoint, Set<UUID> criterionKnowledgePoints,
                          String dominantErrorCode) {
        public Outcome {
            correctByKnowledgePoint = Collections.unmodifiableMap(new LinkedHashMap<>(correctByKnowledgePoint));
            criterionKnowledgePoints = Set.copyOf(criterionKnowledgePoints);
        }
    }

    public Outcome derive(Collection<UUID> questionKnowledgePoints, BigDecimal passBand, boolean passed,
                          WritingGrade grade) {
        Map<UUID, Boolean> correct = new LinkedHashMap<>();
        for (UUID kp : questionKnowledgePoints) {
            if (kp != null) correct.putIfAbsent(kp, passed);
        }
        Set<UUID> decidedByCriteria = new LinkedHashSet<>();
        if (passBand != null && grade != null && grade.criteria() != null) {
            for (WritingGrade.Criterion criterion : grade.criteria()) {
                if (criterion == null || criterion.knowledgePointId() == null || criterion.band() == null) continue;
                boolean reached = criterion.band().compareTo(passBand) >= 0;
                UUID kp = criterion.knowledgePointId();
                if (decidedByCriteria.add(kp)) correct.put(kp, reached);
                else correct.put(kp, correct.get(kp) && reached);
            }
        }
        Set<UUID> decidedByJudgments = new LinkedHashSet<>();
        if (grade != null && grade.criteria() != null) {
            for (WritingGrade.Criterion criterion : grade.criteria()) {
                if (criterion == null || criterion.knowledgePointJudgments() == null) continue;
                for (WritingGrade.KnowledgeJudgment judgment : criterion.knowledgePointJudgments()) {
                    if (judgment == null || judgment.knowledgePointId() == null
                            || judgment.judgment() == WritingGrade.Judgment.NOT_ASSESSED) continue;
                    UUID kp = judgment.knowledgePointId();
                    if (decidedByCriteria.contains(kp)) continue;
                    boolean right = judgment.judgment() == WritingGrade.Judgment.PASS;
                    if (decidedByJudgments.add(kp)) correct.put(kp, right);
                    else correct.put(kp, correct.get(kp) && right);
                }
            }
        }
        Set<UUID> decidedPoints = new LinkedHashSet<>(decidedByCriteria);
        decidedPoints.addAll(decidedByJudgments);
        return new Outcome(correct, decidedPoints, dominantErrorCode(grade));
    }

    /** The most frequent error code of the corrections; a tie goes to the alphabetically first code. */
    static String dominantErrorCode(WritingGrade grade) {
        if (grade == null || grade.corrections() == null) return null;
        Map<String, Integer> counts = new TreeMap<>();
        for (WritingGrade.Correction correction : grade.corrections()) {
            if (correction == null || correction.errorCode() == null || correction.errorCode().isBlank()) continue;
            counts.merge(correction.errorCode(), 1, Integer::sum);
        }
        String best = null;
        int bestCount = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                best = entry.getKey();
                bestCount = entry.getValue();
            }
        }
        return best;
    }
}
