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
        if (!"HUMAN".equals(mode) || !java.util.Set.of("QUEUED","PROCESSING","COMPLETED","FAILED","CANCELLED").contains(status)
                || page<0 || size<1 || size>100) throw new IllegalArgumentException("Invalid queue filters or pagination (size 1..100)");
        return store.list(mode,status,page,size);
    }
}
