package com.group01.assessment.application.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Grading jobs for the essays of topic and course tests. An AI job is keyed {@code gate-essay:{attemptItemId}:ai} and
 * the human job that replaces a failed one {@code gate-essay:{attemptItemId}:human}, so retries never duplicate them.
 */
public interface GateEssayJobStore {
    /** The newest SUBMITTED text submission of each attempt item that has one. */
    Map<UUID, UUID> submittedEssays(Collection<UUID> attemptItemIds);

    /** Queues free AI jobs for the submitted items unless their idempotency keys already exist. */
    void enqueueAi(Map<UUID, UUID> submissionByAttemptItem, UUID userId);

    /** Queues a job for a human examiner unless one with {@code idempotencyKey} exists. */
    void enqueueHuman(UUID submissionId, UUID userId, String idempotencyKey);

    /** Puts AI jobs that have been PROCESSING since before {@code startedBefore} back in the queue. */
    int requeueStuck(Instant startedBefore);

    /** Moves up to {@code limit} queued AI jobs to PROCESSING, skipping rows other workers hold, and returns them. */
    List<ClaimedJob> claim(int limit, Instant now);

    void complete(UUID jobId, BigDecimal band, Instant now);

    void fail(UUID jobId, Instant now);

    /** The AI jobs of the attempt's essays. */
    List<JobState> aiJobs(UUID attemptId);

    /** A claimed job with what grading needs: the essay and the item's question and answer snapshots. */
    record ClaimedJob(UUID jobId, UUID userId, UUID attemptId, UUID attemptItemId, UUID submissionId, String essay,
                      String questionSnapshot, String answerSnapshot) {}

    /** {@code status} is a grading job status; {@code band} is set once COMPLETED. */
    record JobState(UUID attemptItemId, String status, BigDecimal band) {}
}
