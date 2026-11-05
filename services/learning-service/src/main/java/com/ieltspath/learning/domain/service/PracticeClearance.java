package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PracticePassReason;
import com.ieltspath.learning.domain.vo.PracticeStatus;
import com.ieltspath.learning.domain.vo.ReviewStatus;

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
 *
 * <p>A skill (or the whole lesson) cleared through reviews needs at least one review of it and none still PENDING:
 * a finished review next to an open one does not clear the Practice. A DONE and a SKIPPED review (no package left)
 * both count as finished. A counted failed attempt with no pending blocking review clears softly as
 * {@code ATTEMPT_NOT_BLOCKED}; that reason is derived on each read and is never stored.
 */
public final class PracticeClearance {
    private static final Set<LearningSkill> OPTIONAL = EnumSet.of(LearningSkill.WRITING);
    private static final List<PracticePassReason> STRENGTH = List.of(PracticePassReason.FIRST_SUBMISSION,
            PracticePassReason.REVIEW_FINISHED, PracticePassReason.ALL_SETS_ATTEMPTED,
            PracticePassReason.ATTEMPT_NOT_BLOCKED);

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
        boolean nonePending = ofSkill.stream().noneMatch(review -> review.status() == ReviewStatus.PENDING);
        if (!ofSkill.isEmpty() && nonePending) return PracticePassReason.REVIEW_FINISHED;
        if (revealed.containsAll(sets) && nonePending) return PracticePassReason.ALL_SETS_ATTEMPTED;
        boolean noBlockingPending = ofSkill.stream()
                .noneMatch(review -> review.status() == ReviewStatus.PENDING && review.blocking());
        boolean failedHere = attempts.stream().anyMatch(attempt -> attempt.countedAsEvidence()
                && sets.contains(attempt.packageId()) && !attempt.passedSkills().contains(skill));
        // A failed first try with nothing blocking lets the learner go on; suggested reviews do not hold it back.
        if (failedHere && noBlockingPending) return PracticePassReason.ATTEMPT_NOT_BLOCKED;
        return null;
    }

    private static Clearance wholeLesson(Set<UUID> publishedPackages, List<AttemptFact> attempts,
                                         Set<UUID> revealedPackages, List<ReviewFact> reviews) {
        if (attempts.stream().anyMatch(attempt -> attempt.passed() && attempt.countedAsEvidence())) {
            return new Clearance(PracticeStatus.PASSED, PracticePassReason.FIRST_SUBMISSION);
        }
        boolean nonePending = reviews.stream().noneMatch(review -> review.status() == ReviewStatus.PENDING);
        if (!reviews.isEmpty() && nonePending) {
            return new Clearance(PracticeStatus.PASSED, PracticePassReason.REVIEW_FINISHED);
        }
        if (!publishedPackages.isEmpty() && revealedPackages.containsAll(publishedPackages) && nonePending) {
            return new Clearance(PracticeStatus.PASSED, PracticePassReason.ALL_SETS_ATTEMPTED);
        }
        if (publishedPackages.isEmpty()) return new Clearance(PracticeStatus.PASSED, PracticePassReason.NO_PRACTICE);
        boolean noBlockingPending = reviews.stream()
                .noneMatch(review -> review.status() == ReviewStatus.PENDING && review.blocking());
        if (noBlockingPending && attempts.stream().anyMatch(attempt -> attempt.countedAsEvidence() && !attempt.passed())) {
            return new Clearance(PracticeStatus.PASSED, PracticePassReason.ATTEMPT_NOT_BLOCKED);
        }
        return new Clearance(PracticeStatus.REQUIRED, null);
    }

    /** {@code passedSkills} are the skills that reached the pass mark in that submission. */
    public record AttemptFact(UUID packageId, boolean passed, boolean countedAsEvidence, Set<LearningSkill> passedSkills) {
        public AttemptFact(UUID packageId, boolean passed, boolean countedAsEvidence) {
            this(packageId, passed, countedAsEvidence, Set.of());
        }
    }

    /** {@code skill} is the skill of the review's knowledge point; null for reviews created before skills. */
    public record ReviewFact(ReviewStatus status, LearningSkill skill, boolean blocking) {
        public ReviewFact(ReviewStatus status, LearningSkill skill) { this(status, skill, true); }
        public ReviewFact(ReviewStatus status) {
            this(status, null, true);
        }
    }

    public record Clearance(PracticeStatus status, PracticePassReason reason) {}
}
