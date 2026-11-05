package com.ieltspath.learning.application.port;

import com.ieltspath.learning.domain.entity.LearnerCourseUnlock;
import com.ieltspath.learning.domain.vo.LearningSkill;

import java.util.UUID;

/** Commits unlock intents independently from the transaction that calls Access. */
public interface PendingCourseUnlockWriter {
    LearnerCourseUnlock createPending(UUID userId, UUID courseId, LearningSkill skill, int cost, UUID requestId);
    void cancel(UUID requestId);
}
