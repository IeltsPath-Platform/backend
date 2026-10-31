package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.entity.LearnerCourseUnlock;
import com.ieltspath.learning.domain.vo.LearningSkill;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface LearnerCourseUnlockRepository {
    Optional<LearnerCourseUnlock> findByRequestId(UUID requestId);
    List<LearnerCourseUnlock> findByUser(UUID userId);
    List<LearnerCourseUnlock> findPending(UUID userId);
    List<LearnerCourseUnlock> findActive(UUID userId, UUID courseId);
    LearnerCourseUnlock createPending(UUID userId, UUID courseId, LearningSkill skill, int pointCost, UUID requestId);
    void activate(UUID requestId, UUID ledgerEntryId);
    void cancel(UUID requestId);

    default Map<LearningSkill, Integer> activeSkillPayments(UUID userId, UUID courseId) {
        return findActive(userId, courseId).stream().filter(unlock -> unlock.skill() != null)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        LearnerCourseUnlock::skill, LearnerCourseUnlock::pointCost));
    }
}
