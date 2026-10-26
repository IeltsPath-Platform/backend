package com.ieltspath.assessment.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Only blind projections leave the examiner queue adapter. */
public interface ExaminerQueueStore {
    QueuePage list(String mode, String status, int page, int size);
    default QueuePage listByReason(String mode, String status, int page, int size, String reason) {
        return list(mode, status, page, size);
    }
    /** The jobs the learners chose this examiner for. */
    QueuePage listFor(UUID examinerId, String mode, String status, int page, int size);
    /**
     * Quality-review jobs the reviewer claimed and has not yet submitted a review for, oldest claim first. The default
     * is empty so stores without this view stay valid.
     */
    default QueuePage claimedQaBy(UUID reviewerId, int page, int size) {
        return new QueuePage(List.of(), page, size, 0);
    }
    Optional<QueueEntry> find(UUID jobId);
    boolean claim(UUID jobId, UUID examinerId);
    Optional<Essay> essay(UUID jobId);
    List<QueueEntry> lockAttempt(UUID attemptId);
    void completeAttempt(UUID attemptId, UUID examinerId);
    void reopenCompleted(UUID attemptId);

    record QueuePage(List<QueueEntry> content, int page, int size, long totalElements) {}
    record QueueEntry(UUID id, UUID submissionId, UUID attemptId, UUID attemptItemId, String attemptType,
                      String source, String skill, String task, String status, String reason, UUID assignedExaminerId,
                      Instant claimedAt, Instant createdAt, long waitSeconds, boolean overdue,
                      UUID requestedExaminerId, boolean hasDraft) {}
    record Essay(UUID jobId, UUID attemptId, UUID attemptItemId, String prompt, String essay, String task,
                 Object rubric, String source) {
        @Override public String toString() { return "Essay[jobId=" + jobId + "]"; }
    }
}
