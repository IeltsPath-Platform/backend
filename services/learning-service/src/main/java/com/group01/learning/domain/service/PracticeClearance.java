package com.group01.learning.domain.service;

import com.group01.learning.domain.vo.LearningSkill;
import com.group01.learning.domain.vo.PracticePassReason;
import com.group01.learning.domain.vo.PracticeStatus;
import com.group01.learning.domain.vo.ReviewStatus;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Whether a learner has cleared a lesson's Practice. When the published sets name their skills, each objective skill
 * (Reading, Listening) is cleared on its own and the Practice passes once every one of them is; Writing sets are
 * optional. Sets without skills (older Content) are judged together as before.
 */
public final class PracticeClearance {
    private static final Set<LearningSkill> OPTIONAL = EnumSet.of(LearningSkill.WRITING);
    private static final List<PracticePassReason> STRENGTH = List.of(PracticePassReason.FIRST_SUBMISSION,
            PracticePassReason.REVIEW_FINISHED, PracticePassReason.ALL_SETS_ATTEMPTED);

    public Clearance derive(boolean lessonCompleted, Set<UUID> publishedPackages, List<AttemptFact> attempts,
                            Set<UUID> revealedPackages, List<ReviewFact> reviews, PracticePassReason stored) {
        return derive(lessonCompleted, publishedPackages.stream()
                        .collect(Collectors.toMap(id -> id, id -> Set.<LearningSkill>of())), attempts,
                revealedPackages, reviews, stored);
    }

    /** {@code publishedSets} maps each published set of the lesson to the skills of its questions. */
    public Clearance derive(boolean lessonCompleted, Map<UUID, Set<LearningSkill>> publishedSets,
                            List<AttemptFact> attempts, Set<UUID> revealedPackages, List<ReviewFact> reviews,
                            PracticePassReason stored) {
        if (stored != null) return new Clearance(PracticeStatus.PASSED, stored);
        if (!lessonCompleted) return new Clearance(PracticeStatus.LOCKED, null);
        if (publishedSets.values().stream().allMatch(Set::isEmpty)) {
            return wholeLesson(publishedSets.keySet(), attempts, revealedPackages, reviews);
        }
        Map<LearningSkill, Set<UUID>> setsBySkill = new HashMap<>();
        publishedSets.forEach((packageId, skills) -> skills.stream().filter(skill -> !OPTIONAL.contains(skill))
                .forEach(skill -> setsBySkill.computeIfAbsent(skill, ignored -> new HashSet<>())
                        .add(packageId)));
        if (setsBySkill.isEmpty()) return new Clearance(PracticeStatus.PASSED, PracticePassReason.NO_PRACTICE);
        PracticePassReason weakest = null;
        for (var entry : setsBySkill.entrySet()) {
            PracticePassReason reason = skillReason(entry.getKey(), entry.getValue(), attempts, revealedPackages,
                    reviews);
            if (reason == null) return new Clearance(PracticeStatus.REQUIRED, null);
            if (weakest == null || STRENGTH.indexOf(reason) > STRENGTH.indexOf(weakest)) weakest = reason;
        }
        return new Clearance(PracticeStatus.PASSED, weakest);
    }

    /** The reason one skill is cleared, or null when it is not yet. */
    private static PracticePassReason skillReason(LearningSkill skill, Set<UUID> sets, List<AttemptFact> attempts,
                                                  Set<UUID> revealed, List<ReviewFact> reviews) {
        if (attempts.stream().anyMatch(attempt -> attempt.countedAsEvidence() && attempt.passedSkills().contains(skill))) {
            return PracticePassReason.FIRST_SUBMISSION;
        }
        List<ReviewFact> ofSkill = reviews.stream()
                .filter(review -> review.skill() == null || review.skill() == skill).toList();
        if (ofSkill.stream().anyMatch(review -> review.status() != ReviewStatus.PENDING)) {
            return PracticePassReason.REVIEW_FINISHED;
        }
        if (revealed.containsAll(sets) && ofSkill.stream().noneMatch(review -> review.status() == ReviewStatus.PENDING)) {
            return PracticePassReason.ALL_SETS_ATTEMPTED;
        }
        return null;
    }

    private static Clearance wholeLesson(Set<UUID> publishedPackages, List<AttemptFact> attempts,
                                         Set<UUID> revealedPackages, List<ReviewFact> reviews) {
        if (attempts.stream().anyMatch(attempt -> attempt.passed() && attempt.countedAsEvidence())) {
            return new Clearance(PracticeStatus.PASSED, PracticePassReason.FIRST_SUBMISSION);
        }
        if (reviews.stream().anyMatch(review -> review.status() != ReviewStatus.PENDING)) {
            return new Clearance(PracticeStatus.PASSED, PracticePassReason.REVIEW_FINISHED);
        }
        if (!publishedPackages.isEmpty() && revealedPackages.containsAll(publishedPackages)
                && reviews.stream().noneMatch(review -> review.status() == ReviewStatus.PENDING)) {
            return new Clearance(PracticeStatus.PASSED, PracticePassReason.ALL_SETS_ATTEMPTED);
        }
        if (publishedPackages.isEmpty()) return new Clearance(PracticeStatus.PASSED, PracticePassReason.NO_PRACTICE);
        return new Clearance(PracticeStatus.REQUIRED, null);
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
