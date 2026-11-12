package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.vo.SpeakingScope;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * What a learner has finished in the practice-test catalog for the skills whose answers live in this service. Only graded
 * answers count, and only answers to a catalog test: a lesson's own practice sets are left out. Each list holds at most
 * the 500 most recent entries, which is as many keys as the catalog accepts in one filter.
 */
public interface PracticeProgressRepository {
    int LIMIT = 500;

    /** The latest graded Speaking submission of each package version and part (the part is null for a full test). */
    List<GradedSpeaking> gradedSpeaking(UUID userId, SpeakingScope scope);

    /** The latest graded free essay of each Writing question. */
    List<GradedWriting> gradedFreeWriting(UUID userId);

    record GradedSpeaking(UUID submissionId, UUID packageVersionId, Integer partNo, Instant doneAt) {}

    record GradedWriting(UUID submissionId, UUID questionVersionId, Instant doneAt) {}
}
