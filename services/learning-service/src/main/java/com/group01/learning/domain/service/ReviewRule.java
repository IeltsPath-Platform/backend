package com.group01.learning.domain.service;

import com.group01.learning.domain.vo.LearningSkill;
import com.group01.learning.domain.vo.ReviewStage;
import com.group01.learning.domain.vo.TheoryReason;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class ReviewRule {
    /** The failure that created the review is the first; the second sends to theory, the third skips the review. */
    public static final int MAX_FAILED_REVIEW_SETS = 2;
    /** Below this share of correct answers a learner reads the theory before any practice set. */
    public static final double LOW_SCORE = 0.40;

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
        return reevaluate(consideredKps, wrongKps, mastery, lessonsByKp, kpsWithPracticeSet, pendingKps, Map.of());
    }

    /**
     * {@code kpSkills} gives each review the skill of its knowledge point, so a review from a lesson teaching several
     * skills blocks only that skill; a knowledge point without one takes the skill of its lesson.
     */
    public List<ReviewCandidate> reevaluate(Set<UUID> consideredKps, Set<UUID> wrongKps,
                                           Map<UUID, Double> mastery,
                                           Map<UUID, List<CompletedLesson>> lessonsByKp,
                                           Set<UUID> kpsWithPracticeSet, Set<UUID> pendingKps,
                                           Map<UUID, LearningSkill> kpSkills) {
        List<ReviewCandidate> candidates = new ArrayList<>();
        List<UUID> orderedKps = consideredKps.stream().sorted(Comparator.comparing(UUID::toString)).toList();
        for (UUID kp : orderedKps) {
            if (!wrongKps.contains(kp) || !kpsWithPracticeSet.contains(kp) || pendingKps.contains(kp)
                    || !(mastery.getOrDefault(kp, 0.0) < threshold)) {
                continue;
            }
            lessonsByKp.getOrDefault(kp, List.of()).stream().min(LESSON_ORDER)
                    .ifPresent(lesson -> candidates.add(new ReviewCandidate(kp, lesson.lessonId(),
                            kpSkills.getOrDefault(kp, lesson.skill()))));
        }
        return List.copyOf(candidates);
    }

    /**
     * Stage of a new review: a knowledge point scored below {@link #LOW_SCORE} in the practice attempt that created
     * it, or answered wrong on the first submission of the lesson's exercises, starts with the theory.
     * {@code kpPercent} is null for reviews not created by a practice attempt.
     */
    public static Start initialStage(Double kpPercent, boolean wrongInLesson) {
        if (kpPercent != null && kpPercent < LOW_SCORE) return new Start(ReviewStage.THEORY, TheoryReason.LOW_SCORE);
        if (wrongInLesson) return new Start(ReviewStage.THEORY, TheoryReason.WRONG_IN_LESSON);
        return new Start(ReviewStage.PRACTICE, null);
    }

    public record Start(ReviewStage stage, TheoryReason theoryReason) {
        public Start {
            Objects.requireNonNull(stage, "stage");
        }
    }

    /** Each entry represents a lesson whose completed timestamp has already been recorded. */
    public record CompletedLesson(UUID lessonId, Integer sequenceOrder, int lessonSortOrder, LearningSkill skill) {
        public CompletedLesson(UUID lessonId, Integer sequenceOrder, int lessonSortOrder) {
            this(lessonId, sequenceOrder, lessonSortOrder, null);
        }
        public CompletedLesson {
            Objects.requireNonNull(lessonId, "lessonId");
        }
    }

    public record ReviewCandidate(UUID knowledgePointId, UUID lessonId, LearningSkill skill) {
        public ReviewCandidate(UUID knowledgePointId, UUID lessonId) {
            this(knowledgePointId, lessonId, null);
        }
        public ReviewCandidate {
            Objects.requireNonNull(knowledgePointId, "knowledgePointId");
            Objects.requireNonNull(lessonId, "lessonId");
        }
    }
}
