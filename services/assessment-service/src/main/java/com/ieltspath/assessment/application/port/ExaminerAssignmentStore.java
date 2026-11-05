package com.ieltspath.assessment.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Who a human grading job is for: the examiner the learner chose, and how far that choice got. */
public interface ExaminerAssignmentStore {
    /** Locks a human job (not a QA sample) for the rest of the transaction. */
    Optional<JobRef> lockJob(UUID jobId);

    /** The open jobs of the same attempt (the job itself when it belongs to no attempt), locked. */
    List<JobRef> lockOpenGroup(JobRef job);

    /** Serialises the capacity check of one examiner. Held until the transaction ends. */
    void lockExaminer(UUID examinerId);

    /** The human jobs handed to the examiner since {@code since}, whatever their state now. */
    int receivedSince(UUID examinerId, Instant since);

    /** How many of the given examiners' human jobs were handed to each since {@code since}; absent means none. */
    java.util.Map<UUID, Integer> receivedSince(java.util.Collection<UUID> examinerIds, Instant since);

    /** How many open human jobs the examiner was chosen for at or before {@code overdueBefore}: the ones already late. */
    int overdueOpen(UUID examinerId, Instant overdueBefore);

    /** The same count for several examiners at once; absent means none. */
    java.util.Map<UUID, Integer> overdueOpen(java.util.Collection<UUID> examinerIds, Instant overdueBefore);

    /** Closes an open job the learner gave up on and drops the unfinished review, so nobody can grade it any more. */
    void cancel(UUID jobId);

    /**
     * Hands the jobs to the examiner and restarts the work: the claim, the unfinished review and the deadline clock are
     * reset. {@code replacing} counts the change when the jobs already had an examiner.
     */
    void assign(List<UUID> jobIds, UUID examinerId, boolean replacing);

    List<MyJob> listMine(UUID learnerId);

    record JobRef(UUID id, UUID learnerId, String status, UUID attemptId, UUID requestedExaminerId,
                  Instant requestedAt, int examinerChanges, String skill) {}

    /** {@code reviewSubmissionId} is set for a standalone review (Practice, Lesson): how Learning finds the learner's job. */
    record MyJob(UUID id, UUID reviewSubmissionId, String skill, String status, String source, UUID requestedExaminerId,
                 Instant requestedAt, int examinerChanges, Instant createdAt, java.math.BigDecimal band,
                 Instant completedAt) {}
}
