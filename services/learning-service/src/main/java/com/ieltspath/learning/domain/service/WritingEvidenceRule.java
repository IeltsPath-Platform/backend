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
 * pass band. A point that is both a question point and a criterion point takes the criterion's result; two criteria
 * on one point must both reach the pass band.
 */
public final class WritingEvidenceRule {

    /**
     * {@code correctByKnowledgePoint} lists the question's points first, then the criterion points; a point appears
     * once. {@code criterionKnowledgePoints} are the points that a criterion decided.
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
        return new Outcome(correct, decidedByCriteria, dominantErrorCode(grade));
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
