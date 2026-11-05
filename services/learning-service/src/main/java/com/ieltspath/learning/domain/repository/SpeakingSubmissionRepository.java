package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;

import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

public interface SpeakingSubmissionRepository {
    Optional<SpeakingSubmission> findByRequestId(UUID requestId);

    Optional<SpeakingSubmission> findById(UUID id);

    Optional<SpeakingSubmission> findOwned(UUID id, UUID userId);

    /** Locks the row; call it inside a transaction that already holds the learner's lock. */
    Optional<SpeakingSubmission> findForUpdate(UUID id);

    /** Submissions of the learner still waiting for a grade. */
    List<SpeakingSubmission> findActive(UUID userId);

    /** Claims one queued or stale grading row atomically across service instances. */
    Optional<SpeakingSubmission> claimNext(Instant staleBefore, int maxAttempts);

    Optional<String> findAnalysis(UUID submissionId, UUID questionVersionId);

    void saveAnalysis(UUID submissionId, UUID questionVersionId, String analysisJson);

    List<SpeakingSubmission> findExpiredBefore(Instant cutoff, int limit);

    boolean deleteExpired(UUID id, Instant cutoff);

    /** Inserts a new submission with its empty recordings, or updates an existing one and its recordings. */
    void save(SpeakingSubmission submission);
}
