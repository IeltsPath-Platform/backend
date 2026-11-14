package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.FailedWritingCriterion;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Selects optional criterion reviews from failed bands and the available, unseen practice sets. */
public final class WritingCriterionReviewRule {
    private static final Comparator<FailedWritingCriterion> WEAKEST_FIRST = Comparator
            .comparing(FailedWritingCriterion::band)
            .thenComparing(FailedWritingCriterion::code)
            .thenComparing(criterion -> criterion.knowledgePointId().toString());

    /**
     * The weakest criteria that can be reviewed, at most {@code limit}. A criterion reviews the first point of its
     * {@code reviewPoints} that no waiting review has and that still has an unseen practice set, and falls back to its own
     * point on the same terms. One that has neither is left out. A waiting review of the criterion's own point also
     * stops the criterion.
     */
    public List<FailedWritingCriterion> select(Collection<FailedWritingCriterion> failedCriteria,
                                               Set<UUID> pendingPoints, Map<UUID, Integer> availability, int limit) {
        if (limit <= 0 || failedCriteria.isEmpty()) return List.of();
        Map<UUID, FailedWritingCriterion> unique = new HashMap<>();
        for (FailedWritingCriterion criterion : failedCriteria) {
            if (pendingPoints.contains(criterion.knowledgePointId())) continue;
            List<UUID> candidates = new java.util.ArrayList<>(criterion.reviewPoints());
            candidates.add(criterion.knowledgePointId());
            UUID chosen = candidates.stream()
                    .filter(point -> !pendingPoints.contains(point) && availability.getOrDefault(point, 0) > 0)
                    .findFirst().orElse(null);
            if (chosen == null) continue;
            FailedWritingCriterion reviewed = criterion.reviewing(chosen.equals(criterion.knowledgePointId()) ? null : chosen);
            unique.merge(criterion.knowledgePointId(), reviewed,
                    (first, next) -> WEAKEST_FIRST.compare(first, next) <= 0 ? first : next);
        }
        return unique.values().stream().sorted(WEAKEST_FIRST).limit(limit).toList();
    }
}
