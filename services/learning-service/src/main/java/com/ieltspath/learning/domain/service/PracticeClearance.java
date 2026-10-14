package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PracticePassReason;
import com.ieltspath.learning.domain.vo.PracticeStatus;
import com.ieltspath.learning.domain.vo.ReviewStatus;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Whether a learner has cleared a lesson's Practice: every required set has been submitted at least once and no
 * review it created is still pending. Required sets are those with an objective skill (Reading, Listening) or no
 * skill (older Content); Writing sets are optional. A lesson without required sets is {@code NO_PRACTICE} even before
 * it opens; otherwise its Practice is LOCKED until the lesson opens. A PASSED Practice is what completes a lesson that
 * has required sets.
 */
public final class PracticeClearance {
    private static final Set<LearningSkill> OPTIONAL = EnumSet.of(LearningSkill.WRITING);

    public Clearance derive(boolean lessonOpen, Set<UUID> publishedPackages, List<AttemptFact> attempts,
                            Set<UUID> revealedPackages, List<ReviewFact> reviews, PracticePassReason stored) {
        return derive(lessonOpen, publishedPackages.stream()
                        .collect(Collectors.toMap(id -> id, id -> Set.<LearningSkill>of())), attempts,
                revealedPackages, reviews, stored);
    }

    /**
     * {@code publishedSets} maps each published set of the lesson to the skills of its questions. The reason is
     * {@code FIRST_SUBMISSION} when every required set passed at its first submission and no review was needed,
     * {@code REVIEW_FINISHED} when reviews were needed and are all finished, else {@code ALL_SETS_ATTEMPTED}.
     */
    public Clearance derive(boolean lessonOpen, Map<UUID, Set<LearningSkill>> publishedSets,
                            List<AttemptFact> attempts, Set<UUID> revealedPackages, List<ReviewFact> reviews,
                            PracticePassReason stored) {
        if (stored != null) return new Clearance(PracticeStatus.PASSED, stored);
        Map<UUID, Set<LearningSkill>> required = new HashMap<>();
        publishedSets.forEach((packageId, skills) -> {
            Set<LearningSkill> objective = skills.stream().filter(skill -> !OPTIONAL.contains(skill))
                    .collect(Collectors.toSet());
            if (skills.isEmpty() || !objective.isEmpty()) required.put(packageId, objective);
        });
        if (required.isEmpty()) return new Clearance(PracticeStatus.PASSED, PracticePassReason.NO_PRACTICE);
        if (!lessonOpen) return new Clearance(PracticeStatus.LOCKED, null);
        boolean pending = reviews.stream().anyMatch(review -> review.status() == ReviewStatus.PENDING);
        if (pending || !revealedPackages.containsAll(required.keySet())) {
            return new Clearance(PracticeStatus.REQUIRED, null);
        }
        if (!reviews.isEmpty()) return new Clearance(PracticeStatus.PASSED, PracticePassReason.REVIEW_FINISHED);
        return new Clearance(PracticeStatus.PASSED, everySetPassedFirst(required, attempts)
                ? PracticePassReason.FIRST_SUBMISSION : PracticePassReason.ALL_SETS_ATTEMPTED);
    }

    private static boolean everySetPassedFirst(Map<UUID, Set<LearningSkill>> required, List<AttemptFact> attempts) {
        return required.entrySet().stream().allMatch(set -> attempts.stream().anyMatch(attempt ->
                attempt.packageId().equals(set.getKey()) && attempt.countedAsEvidence()
                        && (set.getValue().isEmpty() ? attempt.passed()
                        : attempt.passedSkills().containsAll(set.getValue()))));
    }

    /** {@code passedSkills} are the skills that reached the pass mark in that submission. */
    public record AttemptFact(UUID packageId, boolean passed, boolean countedAsEvidence, Set<LearningSkill> passedSkills) {
        public AttemptFact(UUID packageId, boolean passed, boolean countedAsEvidence) {
            this(packageId, passed, countedAsEvidence, Set.of());
        }
    }

    /** {@code skill} is the skill of the review's knowledge point; null for reviews created before skills. */
    public record ReviewFact(ReviewStatus status, LearningSkill skill) {
        public ReviewFact(ReviewStatus status) {
            this(status, null);
        }
    }

    public record Clearance(PracticeStatus status, PracticePassReason reason) {}
}
