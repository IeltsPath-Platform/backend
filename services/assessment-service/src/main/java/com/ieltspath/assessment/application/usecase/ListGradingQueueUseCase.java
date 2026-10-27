package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.ExaminerQueueStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class ListGradingQueueUseCase {
    private static final Set<String> STATUSES = Set.of("QUEUED", "PROCESSING", "COMPLETED", "FAILED", "CANCELLED");

    private final ExaminerQueueStore store;
    public ListGradingQueueUseCase(ExaminerQueueStore store) { this.store=store; }

    /** Every job, whoever it was chosen for: the view of an administrator. */
    @Transactional(readOnly=true)
    public ExaminerQueueStore.QueuePage execute(String mode,String status,int page,int size) {
        return execute(mode, status, page, size, null);
    }

    @Transactional(readOnly=true)
    public ExaminerQueueStore.QueuePage execute(String mode,String status,int page,int size,String reason) {
        validate(mode, status, reason, page, size);
        return reason == null ? store.list(mode,status,page,size) : store.listByReason(mode,status,page,size,reason);
    }

    /** An examiner's own queue: the jobs learners chose them for. */
    @Transactional(readOnly=true)
    public ExaminerQueueStore.QueuePage executeFor(UUID examiner,String mode,String status,int page,int size) {
        validate(mode, status, null, page, size);
        return store.listFor(examiner,mode,status,page,size);
    }

    /** The quality-review essays this reviewer claimed and has not yet reviewed. */
    @Transactional(readOnly=true)
    public ExaminerQueueStore.QueuePage claimedQaBy(UUID reviewerId,int page,int size) {
        if (page<0 || size<1 || size>100) throw new IllegalArgumentException("Invalid pagination (size 1..100)");
        return store.claimedQaBy(reviewerId,page,size);
    }

    private static void validate(String mode,String status,String reason,int page,int size) {
        if (!"HUMAN".equals(mode) || !STATUSES.contains(status)
                || (reason != null && !"QA_SAMPLE".equals(reason)) || page<0 || size<1 || size>100)
            throw new IllegalArgumentException("Invalid queue filters or pagination (size 1..100)");
    }
}
