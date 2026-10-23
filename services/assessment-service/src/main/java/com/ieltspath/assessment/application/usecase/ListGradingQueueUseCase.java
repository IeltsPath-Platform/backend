package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.ExaminerQueueStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListGradingQueueUseCase {
    private final ExaminerQueueStore store;
    public ListGradingQueueUseCase(ExaminerQueueStore store) { this.store=store; }
    @Transactional(readOnly=true)
    public ExaminerQueueStore.QueuePage execute(String mode,String status,int page,int size) {
        return execute(mode, status, page, size, null);
    }
    @Transactional(readOnly=true)
    public ExaminerQueueStore.QueuePage execute(String mode,String status,int page,int size,String reason) {
        if (!"HUMAN".equals(mode) || !java.util.Set.of("QUEUED","PROCESSING","COMPLETED","FAILED","CANCELLED").contains(status)
                || (reason != null && !"QA_SAMPLE".equals(reason)) || page<0 || size<1 || size>100)
            throw new IllegalArgumentException("Invalid queue filters or pagination (size 1..100)");
        return reason == null ? store.list(mode,status,page,size) : store.listByReason(mode,status,page,size,reason);
    }
    /** The quality-review essays this reviewer claimed and has not yet reviewed. */
    @Transactional(readOnly=true)
    public ExaminerQueueStore.QueuePage claimedQaBy(java.util.UUID reviewerId,int page,int size) {
        if (page<0 || size<1 || size>100) throw new IllegalArgumentException("Invalid pagination (size 1..100)");
        return store.claimedQaBy(reviewerId,page,size);
    }
}
