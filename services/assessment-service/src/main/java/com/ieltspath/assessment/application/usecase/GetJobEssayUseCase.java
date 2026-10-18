package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.ExaminerQueueStore;
import com.ieltspath.assessment.domain.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class GetJobEssayUseCase {
    private final ExaminerQueueStore store;
    public GetJobEssayUseCase(ExaminerQueueStore store) { this.store=store; }
    @Transactional(readOnly=true)
    public ExaminerQueueStore.Essay execute(UUID examiner,UUID jobId) {
        var job=store.find(jobId).orElseThrow(()->new AssessmentNotFoundException("Grading job not found"));
        if (!examiner.equals(job.assignedExaminerId())) throw new AssessmentAccessDeniedException("Claim the job before reading its essay");
        return store.essay(jobId).orElseThrow(()->new AssessmentNotFoundException("Essay not found"));
    }
}
