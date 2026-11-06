package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.FailedWritingCriterion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Turns one graded recorded answer into mastery outcomes, as {@link WritingEvidenceRule} does for an essay. The question's
 * knowledge points take the answer's pass. Each rubric criterion that names a knowledge point is evidence for that point on
 * its own, correct when its band reaches the pass band; a point that is both a question point and a criterion point takes
 * the criterion's result. Each error the grader marked that has a trap point is evidence against that point.
 */
public final class SpeakingEvidenceRule {

    /** One criterion of the graded answer: its code, the knowledge point it maps to (null when none) and its band. */
    public record Criterion(String code, UUID knowledgePointId, BigDecimal band) { }

    /**
     * {@code correctByKnowledgePoint} lists question points first, then criterion points; a point appears once.
     * {@code criterionKnowledgePoints} are the points decided by a criterion band. {@code failedCriteria} are the mapped
     * criteria below the pass band. {@code trapKnowledgePoints} are the trap points of the marked errors, each once.
     */
    public record Outcome(Map<UUID, Boolean> correctByKnowledgePoint, Set<UUID> criterionKnowledgePoints,
                          List<FailedWritingCriterion> failedCriteria, Set<UUID> trapKnowledgePoints) {
        public Outcome {
            correctByKnowledgePoint = Collections.unmodifiableMap(new LinkedHashMap<>(correctByKnowledgePoint));
            criterionKnowledgePoints = Set.copyOf(criterionKnowledgePoints);
            failedCriteria = List.copyOf(failedCriteria);
            trapKnowledgePoints = Collections.unmodifiableSet(new LinkedHashSet<>(trapKnowledgePoints));
        }
    }

    public Outcome derive(Collection<UUID> questionKnowledgePoints, BigDecimal passBand, boolean passed,
                          List<Criterion> criteria, Collection<String> errorCodes, Map<String, UUID> trapKpByCode) {
        Map<UUID, Boolean> correct = new LinkedHashMap<>();
        for (UUID kp : questionKnowledgePoints) {
            if (kp != null) correct.putIfAbsent(kp, passed);
        }
        Set<UUID> decidedByCriteria = new LinkedHashSet<>();
        List<FailedWritingCriterion> failed = new ArrayList<>();
        if (passBand != null && criteria != null) {
            for (Criterion criterion : criteria) {
                if (criterion == null || criterion.knowledgePointId() == null || criterion.band() == null
                        || criterion.code() == null) continue;
                boolean reached = criterion.band().compareTo(passBand) >= 0;
                UUID kp = criterion.knowledgePointId();
                if (!reached) failed.add(new FailedWritingCriterion(kp, criterion.code(), criterion.band()));
                if (decidedByCriteria.add(kp)) correct.put(kp, reached);
                else correct.put(kp, correct.get(kp) && reached);
            }
        }
        Set<UUID> traps = new LinkedHashSet<>();
        if (errorCodes != null && trapKpByCode != null) {
            for (String code : errorCodes) {
                UUID trap = code == null ? null : trapKpByCode.get(code);
                if (trap != null) traps.add(trap);
            }
        }
        return new Outcome(correct, decidedByCriteria, failed, traps);
    }
}
