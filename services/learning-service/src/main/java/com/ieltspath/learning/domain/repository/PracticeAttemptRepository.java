package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.aggregate.PracticeAttempt;
import com.ieltspath.learning.domain.vo.LearningSkill;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PracticeAttemptRepository {
    Optional<PracticeAttempt> findOwned(UUID userId, UUID attemptId);
    Optional<PracticeAttempt> findOpen(UUID userId, UUID packageId);
    Optional<PracticeAttempt> findByRequestId(UUID requestId);
    List<PracticeAttempt> findByLessons(UUID userId, Collection<UUID> lessonIds);
    TopicAttempts findForTopic(UUID userId, Collection<UUID> lessonIds, Collection<UUID> packageIds);
    List<PackageSummary> summarizeForLesson(UUID userId, UUID lessonId, Collection<UUID> packageIds);
    Set<UUID> revealedPackageIds(UUID userId);
    Set<UUID> revealedPackageIds(UUID userId, Collection<UUID> packageIds);
    void insert(PracticeAttempt attempt);
    void saveResult(PracticeAttempt attempt);

    /** A counted first submission that passed the set or at least one of its skills. */
    record FirstPass(UUID lessonId, UUID packageId, boolean passed, Set<LearningSkill> passedSkills) {
        public FirstPass(UUID lessonId, UUID packageId) {
            this(lessonId, packageId, true, Set.of());
        }
    }
    record TopicAttempts(List<FirstPass> firstPasses, Set<UUID> revealedPackageIds) {}
    record PackageSummary(UUID packageId, UUID lastAttemptId, boolean open, boolean passed,
                          boolean attempted, Double bestPercent) {}
}
