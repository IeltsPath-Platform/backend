package com.group01.learning.domain.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class ReviewRule {
    public static final int MAX_FAILED_REVIEW_SETS = 3;

    private static final Comparator<CompletedLesson> LESSON_ORDER = Comparator
            .comparing(CompletedLesson::sequenceOrder, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparingInt(CompletedLesson::lessonSortOrder)
            .thenComparing(lesson -> lesson.lessonId().toString());

    private final double threshold;

    public ReviewRule(double threshold) {
        if (!Double.isFinite(threshold) || threshold <= 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Review threshold must be greater than zero and at most one.");
        }
        this.threshold = threshold;
    }

    public List<ReviewCandidate> reevaluate(Set<UUID> consideredKps, Set<UUID> wrongKps,
                                           Map<UUID, Double> mastery,
                                           Map<UUID, List<CompletedLesson>> lessonsByKp,
                                           Set<UUID> kpsWithPracticeSet, Set<UUID> pendingKps) {
        List<ReviewCandidate> candidates = new ArrayList<>();
        List<UUID> orderedKps = consideredKps.stream().sorted(Comparator.comparing(UUID::toString)).toList();
        for (UUID kp : orderedKps) {
            if (!wrongKps.contains(kp) || !kpsWithPracticeSet.contains(kp) || pendingKps.contains(kp)
                    || !(mastery.getOrDefault(kp, 0.0) < threshold)) {
                continue;
            }
            lessonsByKp.getOrDefault(kp, List.of()).stream().min(LESSON_ORDER)
                    .ifPresent(lesson -> candidates.add(new ReviewCandidate(kp, lesson.lessonId())));
        }
        return List.copyOf(candidates);
    }

    /** Each entry represents a lesson whose completed timestamp has already been recorded. */
    public record CompletedLesson(UUID lessonId, Integer sequenceOrder, int lessonSortOrder) {
        public CompletedLesson {
            Objects.requireNonNull(lessonId, "lessonId");
        }
    }

    public record ReviewCandidate(UUID knowledgePointId, UUID lessonId) {
        public ReviewCandidate {
            Objects.requireNonNull(knowledgePointId, "knowledgePointId");
            Objects.requireNonNull(lessonId, "lessonId");
        }
    }
}
