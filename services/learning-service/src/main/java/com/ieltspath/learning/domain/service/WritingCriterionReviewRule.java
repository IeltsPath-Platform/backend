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

    public List<FailedWritingCriterion> select(Collection<FailedWritingCriterion> failedCriteria,
                                               Set<UUID> pendingPoints, Map<UUID, Integer> availability, int limit) {
        if (limit <= 0 || failedCriteria.isEmpty()) return List.of();
        Map<UUID, FailedWritingCriterion> unique = new HashMap<>();
        for (FailedWritingCriterion criterion : failedCriteria) {
            if (pendingPoints.contains(criterion.knowledgePointId())
                    || availability.getOrDefault(criterion.knowledgePointId(), 0) <= 0) continue;
            unique.merge(criterion.knowledgePointId(), criterion,
                    (first, next) -> WEAKEST_FIRST.compare(first, next) <= 0 ? first : next);
        }
        return unique.values().stream().sorted(WEAKEST_FIRST).limit(limit).toList();
    }
}
