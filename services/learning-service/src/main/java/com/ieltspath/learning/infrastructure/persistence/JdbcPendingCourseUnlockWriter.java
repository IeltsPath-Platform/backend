package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.port.PendingCourseUnlockWriter;
import com.ieltspath.learning.domain.entity.LearnerCourseUnlock;
import com.ieltspath.learning.domain.repository.LearnerCourseUnlockRepository;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Commits a debit intent independently so it survives a later Access timeout or process failure. */
@Component
public class JdbcPendingCourseUnlockWriter implements PendingCourseUnlockWriter {
    private final LearnerCourseUnlockRepository unlocks;

    public JdbcPendingCourseUnlockWriter(LearnerCourseUnlockRepository unlocks) {
        this.unlocks = unlocks;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LearnerCourseUnlock createPending(UUID userId, UUID courseId, LearningSkill skill, int cost,
                                             UUID requestId) {
        return unlocks.createPending(userId, courseId, skill, cost, requestId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancel(UUID requestId) {
        unlocks.cancel(requestId);
    }
}
