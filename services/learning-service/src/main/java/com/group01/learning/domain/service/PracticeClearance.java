package com.group01.learning.domain.service;

import com.group01.learning.domain.vo.PracticePassReason;
import com.group01.learning.domain.vo.PracticeStatus;
import com.group01.learning.domain.vo.ReviewStatus;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PracticeClearance {
    public Clearance derive(boolean lessonCompleted, Set<UUID> publishedPackages, List<AttemptFact> attempts,
                            Set<UUID> revealedPackages, List<ReviewFact> reviews, PracticePassReason stored) {
        if (stored != null) return new Clearance(PracticeStatus.PASSED, stored);
        if (!lessonCompleted) return new Clearance(PracticeStatus.LOCKED, null);
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

    public record AttemptFact(UUID packageId, boolean passed, boolean countedAsEvidence) {}
    public record ReviewFact(ReviewStatus status) {}
    public record Clearance(PracticeStatus status, PracticePassReason reason) {}
}
